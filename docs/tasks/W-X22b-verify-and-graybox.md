# W-X22b — 验证 W-C05 白名单 + 启动 1h 灰盒实测（🟡 P1 / 实测）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 16:58
**优先级**：🟡 P1（老板 16:41 指令："D+B 一块跑... 我最终就要一个一小时推送几个"）
**前置依赖**：W-C05 已 PASS（4 条 PSM 默认白名单已 INSERT + W-X15-restore id=37 保留）

---

## 🎯 任务目标

**验证 W-C05 白名单写入成功 → 重启 hik-java 加载 W-X17a/W-X15a/W-X15b 三轮 fix → 跑 1h 灰盒 → 给老板一个"1 小时推送几次"的真实数字。**

老板指令硬约束（**重申**）：
- ✅ **yk.uploadEnabled 维持 false**（不要真的推到 MES）
- ✅ **alarm.global-enabled 维持 true**（让报警正常落 PG 走全链路）
- ❌ 不能改 yml
- ❌ 不能改代码
- ❌ 不能删 ignore_alarm 数据

---

## 📋 任务清单（5 步）

### 1. 验证 W-C05 白名单（前置）

读 `E:\DEMO\数据采集\docs\delivered\2026-07-23-W-C05-psm-whitelist-seed-result.md`

PSQL 验证：
```bash
"C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, defect_name, type, line_no, face_no, ignore_all, end_time FROM ignore_alarm ORDER BY id"
```

**期望**：5 条（id=37 W-X15-restore + id=38-41 PSM 默认白名单），全部 end_time='2099-12-31 23:59:59'

### 2. 重启 hik-java 加载新 class

当前 hik-java 实际是 **PID 24588**（W-C05 报告中提到 16:54:55 启动——非本工单触发，可能是 W-X22 第一次启动尝试或其他人）

**先检查实际状态**：
```powershell
Get-Process hik-java -ErrorAction SilentlyContinue | Select-Object Id, StartTime
Get-NetTCPConnection -State Established | Where-Object {$_.LocalAddress -in @("0.0.0.0","127.0.0.1") -or $_.LocalPort -lt 1000} | Measure-Object
```

**步骤**：

```powershell
# 1. 确认 cp 启动脚本位置
Test-Path "E:\DEMO\数据采集\scripts\start-app.bat"
Get-Content "E:\DEMO\数据采集\scripts\start-app.bat" -ErrorAction SilentlyContinue

# 2. 停现有 hik-java
$hik = Get-Process hik-java -ErrorAction SilentlyContinue
if ($hik) {
    Stop-Process -Id $hik.Id -Force
    Start-Sleep -Seconds 5
}

# 3. cp 模式启动（参考 scripts/start-app.bat）
cd E:\DEMO\数据采集
# 项目标准 cp 模式 = hik-java.exe -cp "DataupLoad\lib\*;DataupLoad\target\classes"
Start-Process -FilePath "hik-java.exe" `
    -ArgumentList '-cp', 'DataupLoad\lib\*;DataupLoad\target\classes' `
    -WorkingDirectory 'E:\DEMO\数据采集\DataupLoad' `
    -RedirectStandardOutput 'E:\DEMO\数据采集\logs\dataupload.out.log' `
    -RedirectStandardError 'E:\DEMO\数据采集\logs\dataupload.err.log' `
    -NoNewWindow
# 或用 scripts\start-app.bat

Start-Sleep -Seconds 30

# 4. 验证 4 项
Get-Process hik-java -ErrorAction SilentlyContinue | Select-Object Id, StartTime
Get-NetTCPConnection -State Established | Measure-Object
Get-Content 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log' -Tail 50 | Select-String "ticket|ESTABLISHED|started"
```

**启动后必须验证**：
1. ✅ hik-java 进程存活（new PID）
2. ✅ 38 相机 ESTABLISHED（≥30）
3. ✅ yk ticket 拿到（`success to get ticket from yk`）
4. ✅ alarm.global-enabled=true（yml 确认）

### 3. 跑 1h 灰盒（不真推 yk）

**快照点**：0min / 15min / 30min / 45min / 60min（共 5 次）

**每次快照收集**（最近 5min delta）：

| 指标 | 来源 |
|---|---|
| receive alarm 计数 | DataupLoad.log `receive alarm` |
| not interesting defect 计数 | DataupLoad.log `not interesting defect` |
| isIgnore 命中数 | DataupLoad.log `isIgnore` |
| yk push 调用数 | DataupLoad.log `pushAlarm2YK` / `yk.*push` |
| BadSqlGrammarException | error.log 关键字 |
| alarm_record 入库 delta | `SELECT count(*) FROM alarm_record` |
| ignore_alarm 命中数 | DataupLoad.log 忽略命中 |

**写一个 ps 脚本自动收 5 次快照**（参考 W-X18 monitor），输出 `E:\DEMO\数据采集\logs\w-x22-snapshot-*.log`

### 4. 写报告 `E:\DEMO\数据采集\docs\delivered\2026-07-23-W-X22b-restart-1h-graybox-result.md`

必须包含：
- W-C05 验证结果（5 条 ignore_alarm）
- 重启时间戳（new PID / start time / ESTABLISHED 恢复时长）
- 1h 内 5 次快照表
- **老板要的最终数字：1h 平均推送 yk 次数 = X 次**（uploadEnabled=false 下应该是 0，但要实测确认）
- 报警处理漏斗（receive → 模板过滤 → 同类去重 → isIgnore 白名单 → PG 入库 → yk push）
- 任何 BadSqlGrammarException 残留（应该 = 0）

### 5. 严守红线 + 回滚预案

**红线**：
- ❌ yk.uploadEnabled 不能改成 true（**老板硬约束**）
- ❌ alarm.global-enabled 不能改成 false
- ❌ 不能改任何代码
- ❌ 不能删 ignore_alarm 数据
- ❌ 不能动 PSM 端

**回滚**：
- 如果 38 相机 ESTABLISHED 5min 内 < 30 → 立即停进程 → 回滚老 PID 启动命令
- 如果 yk ticket 5min 内拿不到 → 同上
- 超过 5min 仍异常 → PM 再派 W-X22c 排查

---

## 🚫 严禁

- ❌ 改 yml / 改代码 / 删数据 / 改 PSM
- ❌ yk.uploadEnabled 改 true
- ❌ alarm.global-enabled 改 false

---

## 🎯 PM 验收标准

1. ✅ W-C05 白名单已抄完且验证
2. ✅ hik-java 重启成功，new PID alive
3. ✅ 38 相机 ESTABLISHED 在 60s 内恢复 ≥ 30
4. ✅ yk ticket 拿到
5. ✅ 1h 跑完 5 次快照
6. ✅ **老板要的"1h 推送数"具体数字 + 完整证据链**
7. ✅ 报告含 `docs/delivered/2026-07-23-W-X22b-restart-1h-graybox-result.md`

完成后在群内回复 + 报告路径 + **老板要的数字（1h 推送 X 次）**。
