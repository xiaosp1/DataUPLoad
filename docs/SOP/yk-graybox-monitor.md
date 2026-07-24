# YK 灰盒跑法监控 SOP（W-X14）

> **作者**：PM 锋卫（派工 W-X14, 2026-07-23 01:17）
> **生效起**：W-X11c-fix PASS（PID 33004，2026-07-23 00:52）后立刻进入 24h 灰盒观察期
> **解除条件**：见 §4
> **改动原则**：本 SOP 只"引用 + 解释"铁则 36-40，不修改任何铁则原文

---

## §1 灰盒跑法定义

### 1.1 概念

**灰盒跑法 = 链路真跑 + 关键危险开关永久关闭 + 监控三信号 + 解除三条件**。

链路真跑：DataupLoad（hik-java.exe PID 33004，端口 80 LISTEN + 16 ESTABLISHED）真实接收 16 台工控机推送、解析、存 PG（`alarm_record` +1 已实证）。

关键危险开关永久关闭：`yk.enable` 字段 = **false**，**永不**切回 true（除非老板单独指令），**禁止**在群里/口头被诱导成"只推 1 条试试"。

### 1.2 监控三信号（任一异常 → 5 min 内 PM 介入）

| # | 信号 | 检查命令（60s 一次） | 期望值 | 异常判定 |
|---|------|---------------------|--------|----------|
| 1 | **yk 调用日志**（任何 ticket success） | `Get-Content E:\DataupLoad\logs\yk-*.log -Tail 50 -Encoding UTF8 \| Select-String "ticket success"` | `0 条` | 出现 1 条 = 🔴 ALERT |
| 2 | **PG `alarm_record` 增长** | `psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) FROM alarm_record WHERE created_at > NOW() - INTERVAL '60 seconds';"` | `0 行/min` | > 0 行/min = 🔴 ALERT（yk 已推或报警逻辑误触）|
| 3 | **`yk.enable` 配置** | `Select-String -Path E:\DataupLoad\config\application-prod.yml -Pattern "enable:" \| Select-Object Line, LineNumber` | `enable: false` | 出现 `enable: true` / `null` / 缺字段 = 🔴 ALERT（配置被改） |

### 1.3 解除灰盒 3 条件（24h 后逐项核对）

1. **24h 无任何异常信号**（3 信号全部绿灯）
2. **6 防爆全验证**（W-X11c 已合并 W-X15/16/17 三验证 → W-X11c-fix PASS 即覆盖同化 / ignore / alarmCount 聚合 + yk 永久熔断 + 报警入 PG + Flyway V1.20）
3. **老板拍板**（老板在群里/老板接口确认"解除灰盒"）

任一条件不满足 → **灰盒期延长 24h**，**禁止**自行解除。

---

## §2 铁则 36-40 全部摘抄

> ⚠️ 以下为铁则**原文摘抄**，本 SOP 不修改一字，只补"触发场景 + 反例"。

### 铁则 36：yk 灰盒跑法（新增 2026-07-22 21:54，老板拍板）

> **原文**：
> - yk.enable 永久 false（除非老板单独指令）
> - 监控三信号：yk 调用 / PG alarm_record 增长 / yk.enable 配置
> - 解除灰盒的 3 条件（24h 无异常 + 6 防爆全验证 + 老板拍）

**触发场景**：yk 链路初次上线 / 任何会改 `yk.enable` 的改动后。

**反例**：今晚 21:20 PM 把 `yk.enable` 改成 true 推 1 条测试，21:23 老板立刻叫停 enable=false 永久熔断，**之后任何 PM "再推一条试试" 的口头请求都是越权**。

### 铁则 37：Worker 单工单 ≤ 3 步（新增 2026-07-22 23:34，W-X11 教训）

> **原文**：
> - 任务跨 6 步（打 jar + Flyway + 杀进程 + 启 jar + 5 重验证 + 写报告）= 必须拆
> - 拆法示例：W-X11a 打 jar（3 步）+ W-X11b 部署验证（3 步）
> - PM 派工后每 10 分钟必须查 Worker 进度（不能完全相信 spawn 就完事）

**触发场景**：派任何新工单前 / Worker 进度超 10 min 没更新。

**反例**：今晚 22:04 W-X11 单 Worker 干 6 步 50 分钟卡 i18n 没人盯，老板点出才介入。

### 铁则 38：跨平台进程名要查 CommandLine（新增 2026-07-22 23:43）

> **原文**：
> - ❌ `Get-Process -Name java` 漏 hik-java.exe
> - ✅ `wmic process where "ProcessId=X" get CommandLine` 查实际启动命令
> - ✅ 杀进程用 `Get-CimInstance Win32_Process | Where-Object Name -Match 'java\|hik-java' \| Invoke-CimMethod -MethodName Terminate`

**触发场景**：查/杀 hik-java.exe / DataupLoad 进程 / 任何疑似 java 类进程。

**反例**：今晚 23:42 PM 跑 `taskkill /IM java.exe` 没杀掉 hik-java.exe（因为 java.exe 进程不存在，hik-java.exe 才是真名）。

### 铁则 39：PM 每小时体检 STATUS.md / TODO.md（新增 2026-07-22 23:55）

> **原文**：PM 每小时必须体检 STATUS.md / TODO.md 至少 1 次，体检结果写入 `STATUS.md` 末尾 `## PM 体检日志` 段。

**触发场景**：灰盒跑法 24h 观察期内每小时整点 / 任何"忙忘了" 信号。

**反例**：今晚 13:05 → 23:47 共 10h42m PM 没主动体检 1 次，老板问起才发现 STATUS 还写 "PID 27132 跑 28h"（实际 PID 31472 早就接班）。

### 铁则 40：Worker DoD 实证（新增 2026-07-23 00:09，W-X11 教训升级）

> **原文**：Worker Step 0~3 完成后，必须**严格按 DoD 5 重验证清单**逐项回报，每项给实证（命令 + 输出片段），不允许"大致过了"或"应该 OK"模糊话术。

**触发场景**：任何 Worker 回报 / PM 验收工单时。

**反例**：W-X11 原报告 8 项 DoD 中 1 项 PARTIAL（jar 体积 95.99MB < 100MB），PM 验收时发现是 Worker 主动剔除 hutool 等换来，**Worker 没报告这个 trade-off**。

---

## §3 24h 观察期 SOP（每小时循环）

### 3.1 时间窗口

- **起始**：W-X11c-fix PASS（PID 33004, 2026-07-23 00:52）+ W-X12 监控脚本启动
- **结束**：解除灰盒 3 条件全满足 + 老板拍板（预计 2026-07-24 00:52 后某时）

### 3.2 每小时 PM 体检（铁则 39 落地 + 灰盒专项）

每整点（00:00, 01:00, ...）跑下列体检，并把结果写入 `STATUS.md` 末尾 `## PM 体检日志` 段：

```powershell
# === PM 每小时体检清单（5 项现场 + 3 项监控信号）===

# 1. hik-java 进程（铁则 38：用 CommandLine 查，别用 Get-Process -Name java）
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'hik-java|java' -and $_.CommandLine -match 'DataupLoad'
} | Select-Object ProcessId, Name, CommandLine | Format-List

# 2. 80 端口 LISTENING + ESTABLISHED 数
Get-NetTCPConnection -LocalPort 80 -State Listen,Established |
    Measure-Object | Select-Object Count
Get-NetTCPConnection -LocalPort 80 -State Established | Measure-Object | Select-Object Count

# 3. PG 数据库 size + alarm_record 行数（与 1h 前对比涨库）
$env:PGPASSWORD = (Get-Content E:\DEMO\数据采集\secrets\pg.txt -Raw -ErrorAction SilentlyContinue)
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "
  SELECT pg_size_pretty(pg_database_size('intco')) AS db_size,
         (SELECT count(*) FROM alarm_record) AS alarm_count;"

# 4. yk 推送数（铁则 36 监控三信号之 1）
# 期望：grep 不到 ticket success
Select-String -Path "E:\DataupLoad\logs\yk-*.log" -Pattern "ticket success" -ErrorAction SilentlyContinue |
    Select-Object -First 5 Path, LineNumber

# 5. Worker 状态：所有派工 Worker 是否还在跑
Get-ChildItem "E:\DEMO\数据采集\scripts\workers\" -Filter "*.md" |
    Where-Object { $_.Name -match '^W-(X12a|X13a|X14)' } |
    ForEach-Object { [pscustomobject]@{
        Worker = $_.BaseName
        Mtime  = $_.LastWriteTime
        Size   = $_.Length
    }}

# === 监控三信号（铁则 36）由 W-X12 脚本自动跑，PM 只看结果 ===
# 跑法：Get-Content E:\DataupLoad\logs\monitor-yk-push.log -Tail 5 -Encoding UTF8 -Wait
```

### 3.3 监控脚本运行（W-X12）

**W-X12a** 已并行 spawn（2026-07-23 01:00 PM 派工），主体是 `scripts/monitor-yk-push.ps1`，每 60s 跑一次。

**PM 启动方式**（W-X12b 完成后由 cron / Task Scheduler 自动跑；启动前可手跑）：

```powershell
# 启动监控（前台，会自动每 60s 输出状态）
powershell -NoProfile -ExecutionPolicy Bypass -File E:\DEMO\数据采集\scripts\monitor-yk-push.ps1

# 后台跑（用 Task Scheduler 触发，每 60s 一次）
schtasks /Create /SC MINUTE /MO 1 /TN "Monitor-Yk-Push" `
    /TR "powershell -NoProfile -ExecutionPolicy Bypass -File E:\DEMO\数据采集\scripts\monitor-yk-push.ps1" `
    /RL HIGHEST /F

# 看监控日志（tail 5 行）
Get-Content E:\DataupLoad\logs\monitor-yk-push.log -Tail 5 -Encoding UTF8 -Wait
```

**status 输出格式**（W-X12a 工单要求）：

```
[2026-07-23 01:00:00] status=OK | yk_ticket=0 | alarm_growth=0 | enable=false | hik_pid=33004
[2026-07-23 01:01:00] status=OK | yk_ticket=0 | alarm_growth=0 | enable=false | hik_pid=33004
[2026-07-23 01:02:00] status=ALERT | yk_ticket=1 | alarm_growth=0 | enable=false | hik_pid=33004   ← 🔴 报警
```

### 3.4 异常处置（status=ALERT）

**触发条件**：W-X12 输出 `status=ALERT`（任一信号异常）。

**PM 5 min 内介入流程**：

1. **立刻看监控日志定位异常信号**：
   ```powershell
   Get-Content E:\DataupLoad\logs\monitor-yk-push.log -Tail 20 -Encoding UTF8
   ```
2. **如果是 yk ticket success 出现**：
   - 🔴 **最高级**：立刻在群里报老板 + 立刻按 §3.5 应急回滚
   - 排查：是不是某个 cron 把 yk.enable 改 true 了？是不是测试脚本绕过 SOP 直接 POST？
3. **如果是 alarm_record 异常涨**：
   - 看涨库原因：`psql ... -c "SELECT type, count(*) FROM alarm_record WHERE created_at > NOW() - INTERVAL '5 minutes' GROUP BY type;"`
   - 可能是报警逻辑误触（不是 yk 推的，是 Detect 模块误报）
4. **如果是 yk.enable 被改成 true**：
   - 立刻应急回滚（§3.5）+ grep git log 找改的人
5. **如果是 hik-java 进程消失**：
   - 立刻按 W-X11c-fix 的启动命令重启（参考 `docs/delivered/2026-07-23-W-X11c-fix-result.md`）

### 3.5 应急回滚命令

```powershell
# yk.enable 改回 false（铁则 36 强制）
(Get-Content E:\DataupLoad\config\application-prod.yml) `
    -replace 'enable:\s*true', 'enable: false' `
    -replace 'enable:\s*null', 'enable: false' | Set-Content `
    E:\DataupLoad\config\application-prod.yml -Encoding UTF8

# 验证改完
Select-String -Path E:\DataupLoad\config\application-prod.yml -Pattern "enable:"

# 重启 DataupLoad（如果 jar 没生效）
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'hik-java|java' -and $_.CommandLine -match 'DataupLoad'
} | Invoke-CimMethod -MethodName Terminate

Start-Process -FilePath "E:\DataupLoad-final.jar" `
    -ArgumentList "--spring.config.location=E:\DataupLoad\config\application-prod.yml" `
    -RedirectStandardOutput "E:\DataupLoad\logs\stdout.log" `
    -RedirectStandardError "E:\DataupLoad\logs\stderr.log" `
    -WorkingDirectory "E:\DataupLoad\"
```

---

## §4 解除灰盒 3 条件（老板拍板前最后一步）

### 4.1 条件 1：24h 无任何异常信号

**判定标准**：
- W-X12 监控脚本在 24h 内输出 **0 次 `status=ALERT`**
- PM 每小时体检（§3.2）连续 24h 全部为绿（5 项现场 + 3 项监控信号）

**验证命令**：
```powershell
# 1. 监控日志异常次数（应为 0）
$log = Get-Content E:\DataupLoad\logs\monitor-yk-push.log -Encoding UTF8
$alertCount = ($log | Select-String "status=ALERT").Count
Write-Host "ALERT 次数：$alertCount （期望 0）"

# 2. PM 体检日志条目数（应为 ≥ 24 次，连续 24h）
$pmLog = Get-Content STATUS.md -Encoding UTF8
$checkLines = ($pmLog | Select-String "PM 体检").Count
Write-Host "PM 体检条目数：$checkLines （期望 ≥ 24）"
```

### 4.2 条件 2：6 防爆全验证（W-X11c-fix 已覆盖）

**6 防爆清单**（W-X15/16/17 已合并到 W-X11c-fix）：
1. ✅ DataupLoad merged.jar 部署成功（PID 33004 跑着）
2. ✅ Flyway V1.20 ignore_alarm 迁移成功（23:29:08 SUCCESS）
3. ✅ 同化功能生效（DefectRecordServiceImpl 同化逻辑 PASS）
4. ✅ ignore 逻辑生效（AlarmRecordServiceImpl.isIgnore PASS）
5. ✅ alarmCount 聚合生效（LineServiceImpl.getStateStatistics PASS）
6. ✅ yk 永久熔断 + 报警入 PG（alarm_record.id=1 实证）

**验证命令**：
```powershell
# 1. jar 进程在跑
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'hik-java' -and $_.CommandLine -match 'DataupLoad'
} | Select-Object ProcessId, Name

# 2. Flyway 已跑 V1.20
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "
  SELECT version, description, success, installed_on
  FROM flyway_schema_history
  WHERE version = '1.20';"

# 3-5. 同化 / ignore / 聚合：需 W-X11c-fix 报告实证
# 见 docs/delivered/2026-07-23-W-X11c-fix-result.md

# 6. yk.enable 仍为 false + alarm_record 有数据
Select-String -Path E:\DataupLoad\config\application-prod.yml -Pattern "enable:"
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) FROM alarm_record;"
```

### 4.3 条件 3：老板拍板

**PM 不能自行解除**。必须在群里/老板接口明确说"解除灰盒" + 拍板人明确。

**老板拍板后**：
1. 立刻停 W-X12 监控脚本：`schtasks /Delete /TN "Monitor-Yk-Push" /F`
2. 在 `STATUS.md` 末尾追加 `## 灰盒跑法解除（HH:MM）` 段（含老板拍板截图/链接）
3. 在 `docs/delivered/INDEX.md` 第 9 节加"灰盒解除归档"行
4. 工单 `W-X18`（解除灰盒后第一个派工单）解锁

---

## §5 附录：监控命令清单（复制即跑）

### 5.1 进程（铁则 38：必须用 CommandLine）

```powershell
# 查 hik-java 真进程
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'hik-java|java' -and $_.CommandLine -match 'DataupLoad'
} | Select-Object ProcessId, Name, CommandLine | Format-List

# 杀 hik-java 真进程
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'hik-java' -and $_.CommandLine -match 'DataupLoad'
} | Invoke-CimMethod -MethodName Terminate

# 启动 DataupLoad
Start-Process -FilePath "E:\DataupLoad-final.jar" `
    -ArgumentList "--spring.config.location=E:\DataupLoad\config\application-prod.yml" `
    -RedirectStandardOutput "E:\DataupLoad\logs\stdout.log" `
    -RedirectStandardError "E:\DataupLoad\logs\stderr.log" `
    -WorkingDirectory "E:\DataupLoad\"
```

### 5.2 端口（80 真在听）

```powershell
# LISTEN + ESTABLISHED 数
Get-NetTCPConnection -LocalPort 80 -State Listen | Measure-Object | Select-Object Count
Get-NetTCPConnection -LocalPort 80 -State Established | Measure-Object | Select-Object Count

# 80 端口监听详情
Get-NetTCPConnection -LocalPort 80 -State Listen | Format-Table LocalAddress, LocalPort, OwningProcess
```

### 5.3 PostgreSQL（PG 14.23 端口 5433）

```powershell
# DB size + 关键表行数
$env:PGPASSWORD = (Get-Content E:\DEMO\数据采集\secrets\pg.txt -Raw -ErrorAction SilentlyContinue)
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "
  SELECT pg_size_pretty(pg_database_size('intco')) AS db_size,
         (SELECT count(*) FROM detect_record) AS detect,
         (SELECT count(*) FROM defect_record) AS defect,
         (SELECT count(*) FROM alarm_record) AS alarm,
         (SELECT count(*) FROM status_record) AS status,
         (SELECT count(*) FROM line_day_record) AS line_day;"

# alarm_record 最近 5 分钟增长
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "
  SELECT type, count(*)
  FROM alarm_record
  WHERE created_at > NOW() - INTERVAL '5 minutes'
  GROUP BY type;"

# Flyway 历史
psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "
  SELECT version, description, success, installed_on
  FROM flyway_schema_history
  ORDER BY installed_rank DESC LIMIT 5;"
```

### 5.4 日志（yk 调用监控铁则 36）

```powershell
# yk 调用日志（铁则 36 监控三信号之 1：期望 0 条 ticket success）
Get-ChildItem E:\DataupLoad\logs -Filter "yk-*.log" |
    ForEach-Object { Select-String -Path $_.FullName -Pattern "ticket success" -Encoding UTF8 } |
    Select-Object Path, LineNumber, Line

# DataupLoad 应用日志 ERROR/CRIT
Get-ChildItem E:\DataupLoad\logs -Filter "*.log" |
    ForEach-Object { Select-String -Path $_.FullName -Pattern "ERROR|CRITICAL" -Encoding UTF8 } |
    Select-Object -First 20 Path, LineNumber, Line

# W-X12 监控脚本日志
Get-Content E:\DataupLoad\logs\monitor-yk-push.log -Tail 10 -Encoding UTF8 -Wait
```

### 5.5 配置（铁则 36 监控三信号之 3）

```powershell
# yk.enable 当前值（期望 enable: false）
Select-String -Path E:\DataupLoad\config\application-prod.yml -Pattern "enable:"

# 灰盒配置全套
Get-Content E:\DataupLoad\config\application-prod.yml -Encoding UTF8 |
    Select-String -Pattern "yk:|enable:|push:" -Context 0, 2
```

### 5.6 Worker / 派工

```powershell
# 当前派工的 Worker 工单
Get-ChildItem "E:\DEMO\数据采集\scripts\workers\" -Filter "*.md" |
    Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-24) } |
    Sort-Object LastWriteTime -Descending |
    Select-Object Name, LastWriteTime, @{Name='KB';Expression={[math]::Round($_.Length/1KB,1)}} |
    Format-Table -AutoSize

# 群最近 1h 老板/PM 拍板
# （PM 手刷 Telegram，不在 SOP 命令范围内）
```

---

## §6 关联文件

| 文件 | 用途 |
|------|------|
| `docs/SOP/yk-test-push.md` | YK 测试推送 SOP（沿用 21:17 老板定的"只推 1 次成功就停"）|
| `docs/delivered/2026-07-22-night-archive.md` | 铁则 36/37/38 原始出处（21:54-23:43）|
| `docs/delivered/2026-07-22-night-archive-v2.md` | 铁则 39 原始出处（23:55）|
| `scripts/workers/W-X11b.md` | 铁则 40 原始出处（00:09）|
| `scripts/workers/W-X12a.md` | W-X12 监控脚本主体（并行派工中）|
| `scripts/workers/W-X13a.md` | W-X13 C# 端 yk.enable 字段（并行派工中）|
| `docs/delivered/2026-07-23-W-X11c-fix-result.md` | W-X11c-fix PASS 实证（PID 33004 + alarm_record.id=1）|
| `STATUS.md` | 每小时 PM 体检日志写入处（铁则 39）|
| `TODO.md` | 顶表 + 灰盒跑法引用段 |
| `docs/delivered/INDEX.md` | 第 9 节加本 SOP 引用 |
| `docs/adr/0006-csharp-yk-circuits.md` | **ADR-0006（W-X13c 01:55）C# 端 yk.enable 永久熔断 + 3 道熔断门 + §7 恢复推送操作步骤**：本 SOP 仅覆盖 Java 端 yk 推送三信号；C# EdgeHost 端 yk 推送由本 ADR 通过代码层 3 道熔断门 fail-closed 保证（详见 ADR-0006 §4 实现细节 + §7 恢复推送 SOP）|

---

## §7 变更记录

| 日期 | 变更人 | 变更内容 |
|------|--------|----------|
| 2026-07-23 01:17 | W-X14 Worker（PM 锋卫派）| 初版：5 章节 + 铁则 36-40 摘抄 + 24h SOP + 解除 3 条件 + 附录命令清单 |

---

> **本 SOP 生效期间**：所有灰盒相关问题必须在群里 @PM 锋卫 + 引用本文档章节号。
> **本 SOP 解除条件触发后**：立刻停监控脚本 + 归档到 `docs/delivered/`。
