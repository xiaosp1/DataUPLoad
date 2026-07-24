# W-X26 yk 推送上线 SOP（含 Rollback）

**生效时间**：2026-07-24 09:14 GMT+8（老板 09:03 拍板"开推送，随时准备关掉"）
**负责**：PM 锋卫
**当前 PID**：hik-java.exe 19516
**应用层硬开关**：`yk.uploadEnabled`（W-X13d 双开关 / 铁则 42）
**目标**：
- ✅ 推送成功（verify MES 接收）
- ✅ 1 分钟内可回滚（关掉推送）

---

## 一、推送开启（Forward）

### Step 1：备份当前配置
```powershell
Copy-Item "E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml" "E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml.bak-w-x26-$(Get-Date -Format 'HHmmss')"
```

### Step 2：改 yk.uploadEnabled
```yaml
yk:
  loginEnabled: true
  uploadEnabled: true   # ← false 改 true
```

### Step 3：重启 hik-java
```powershell
# 优雅停
Stop-Process -Id 19516 -Force
Start-Sleep -Seconds 3

# 启动新（同 cp 模式）
Start-Process -FilePath "E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe" `
  -ArgumentList "-cp", "E:\DEMO\数据采集\DataupLoad\lib\*;E:\DEMO\数据采集\DataupLoad\target\classes", `
                "-Dfile.encoding=UTF-8", `
                "-Dspring.config.location=classpath:/,file:E:/DEMO/数据采集/DataupLoad/config/", `
                "-Dspring.config.name=application", `
                "-Dserver.port=80", `
                "com.hikrobotics.solution.Application" `
  -RedirectStandardOutput "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\stdout-w-x26.log" `
  -RedirectStandardError  "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\stderr-w-x26.log"
```

### Step 4：等启动（30-60s）
- 端口 80 LISTENING
- ticket 拿到（`Update-Ticket-Thread-1 success to get ticket` 日志）
- 第一次推送（`push alarm to yk` INFO 日志）

### Step 5：监控推送
```powershell
# 看推送日志
Select-String -Path "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\DataupLoad.log" -Pattern "yk upload|push alarm to yk" -Context 0,3 | Select-Object -Last 10

# 看 MES 端是否收到（对照 alarm_record）
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:PGPASSWORD='postgres'
& "E:\DEMO\数据采集\docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\postgres\postgres\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, uuid, defect_name, solve, update_time FROM alarm_record WHERE update_time > now() - interval '5 min' AND defect_name IS NOT NULL ORDER BY update_time DESC LIMIT 10;"
```

---

## 二、回滚（Rollback）—— 1 分钟内

### 触发条件（任一）
- ❌ pushAlarm2YK ERROR > 5 次/分钟
- ❌ ticket 拿不到（连续 2 次 50min 周期失败）
- ❌ MES 接口 5xx 返回率 > 50%
- ❌ 老板一句话（"关"）

### Rollback 步骤（30s 内完成）
```powershell
# Step A：改配置回 false
$ykkPath = "E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml"
(Get-Content $ykkPath) -replace 'uploadEnabled: true', 'uploadEnabled: false' | Set-Content $ykkPath

# Step B：找 hik-java PID 并停
$pid = (Get-Process hik-java -ErrorAction SilentlyContinue).Id
if ($pid) {
    Stop-Process -Id $pid -Force
    Write-Host "Stopped hik-java PID $pid at $(Get-Date -Format 'HH:mm:ss')"
}
Start-Sleep -Seconds 3

# Step C：启动新进程（同 forward step 3）
Start-Process -FilePath "E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe" `
  -ArgumentList "-cp", "E:\DEMO\数据采集\DataupLoad\lib\*;E:\DEMO\数据采集\DataupLoad\target\classes", `
                "-Dfile.encoding=UTF-8", `
                "-Dspring.config.location=classpath:/,file:E:/DEMO/数据采集/DataupLoad/config/", `
                "-Dspring.config.name=application", `
                "-Dserver.port=80", `
                "com.hikrobotics.solution.Application"

# Step D：等启动（30-60s）
Start-Sleep -Seconds 45
Get-NetTCPConnection -LocalPort 80 -State Listen
```

### Rollback 后验证
```powershell
# 端口在听？
Get-NetTCPConnection -LocalPort 80 -State Listen

# hik-java alive？
Get-Process hik-java

# yk.uploadEnabled=false 已生效？
Select-String -Path "E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml" -Pattern "uploadEnabled"

# 推送是否已停？报警继续入 PG（solve=2 不应变化）
Select-String -Path "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\DataupLoad.log" -Pattern "yk upload disabled" | Select-Object -Last 5
```

---

## 三、推送监控（30min 灰度）

### 时间表
| 时间 | 检查项 | PM 行动 |
|---|---|---|
| T+0min | 启动完成 + 端口 80 LISTEN | — |
| T+2min | ticket 拿到（Update-Ticket-Thread 日志）| 没拿到 → 等 |
| T+5min | 第一条推送（`yk upload enabled` 或 `success receive alarm event`）| 没有 → 报警太少，正常 |
| T+15min | 推送成功率（pushAlarm2YK ERROR 占比）| >5% → 准备回滚 |
| T+30min | 推送成功率稳定 + MES 端 alarm 记录到达 | 推老板 |

### 关键日志关键字
| 关键字 | 含义 |
|---|---|
| `yk upload disabled, skip push` | uploadEnabled=false（应不应该出现？开推送后**不应该**）|
| `success receive alarm event` | pushAlarm2YK 入口收到事件 |
| `push alarm to yk error` | 推送失败（**触发 rollback 监控**）|
| `get ticket from yk system failed` | ticket 拿不到（影响推送）|
| `Update-Ticket-Thread-1 success to get ticket` | ticket 续约成功 |

---

## 四、铁则 42（yk.uploadEnabled 红线）

> ⚠️ 推送开启属于"老板单独指令"例外，铁则 42 不变：
> - 默认值 `yk.uploadEnabled=false`（灰盒）
> - 上线期间 = true（当前状态）
> - 老板随时一句话改回 false（rollback 步骤 30s 内完成）
> - 推送关掉期间，报警继续入 PG（alarm_record 不受影响）

---

## 五、附：当前 Push 链路核心配置

```yaml
yk:
  loginEnabled: true    # ticket 续约
  uploadEnabled: true   # 推送开关（false→true 本次变更）
  workshop: QZN2
  username: HKSJSB
  password: HKSJSB123
  login-interval: 50    # ticket 续约间隔（分钟）
  url: http://192.168.80.33:10031/api/dataportal/invoke

alarm:
  global-enabled: true  # 全局报警入口（铁则新）
```

**链接**：
- W-X13d 双开关：`docs/delivered/2026-07-23-w-x13d-recovery-success.md`
- W-X24 PSM 1:1 对比：`docs/delivered/2026-07-24-W-X24-psm-alignment-matrix.md`
- W-X23 INSERT：`docs/delivered/2026-07-24-W-X23-defect-type-seed-result.md`
