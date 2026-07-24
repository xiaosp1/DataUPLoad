# W-X12a — 灰盒监控脚本主体（拆细·步骤 1/3）

> **任务**：写 `scripts/monitor-yk-push.ps1` 监控脚本，监控 4 项关键指标
>
> **派工时间**：W-X11c PASS 后立刻派（2026-07-23 预计 00:54 左右）
> **预计耗时**：20 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度，老板 00:45 拍时间紧）
> **依赖**：W-X11c PASS
>
> ---
>
> ## 背景

老板 21:54 #7798 灰盒跑法理论框架：
- yk.enable 永久 false（铁则 36）
- 监控三信号：yk 调用 / PG alarm_record 增长 / yk.enable 配置
- 解除灰盒 3 条件：24h 无异常 + 6 防爆全验证 + 老板拍

W-X12 系列任务：把监控脚本化，让 PM 不用 24h 一直盯着。
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：监控 4 项指标函数（10 min）
> - [ ] 写 `scripts/monitor-yk-push.ps1` 包含 4 个独立函数：
>   - `Test-YkEnable` — 读 `DataupLoad/src/main/resources/application-prod.yml`，解析 `yk.enable` 字段
>   - `Test-YkTicketCall` — 扫 `DataupLoad/log/DataupLoad/*.log` 最近 1 min，grep `ticket success` 或 `updateTicket:8[0-9] success`
>   - `Test-PgAlarmGrowth` — 连 PG 5433，对比当前 `alarm_record` 行数 vs 5 min 前行数，涨速 > 10 行/min 报警
>   - `Test-HikJavaAlive` — `Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java'`，PID 不存在或端口 80 不 LISTEN 报警
>
> ### Step 2：返回结构化 JSON 结果（5 min）
> - [ ] 主函数 `Invoke-Monitor` 调 4 个函数 + 输出 JSON：
>   ```json
>   {
>     "timestamp": "2026-07-23T00:54:32",
>     "yk_enable": false,
>     "yk_ticket_call_count_1m": 0,
>     "alarm_record_count": 42,
>     "alarm_record_growth_5m": 3,
>     "hik_java_pid": 22296,
>     "port_80_listen": true,
>     "status": "OK"
>   }
>   ```
> - [ ] 任何一项异常 → `"status": "ALERT"` + `alert_reason` 字段
>
> ### Step 3：手动跑 2 次验证（5 min）
> - [ ] 跑 1 次：`.\scripts\monitor-yk-push.ps1`，应输出 JSON + `status: OK`
> - [ ] 故意改 yk.enable=true → 跑第 2 次 → 应 `status: ALERT, alert_reason: yk.enable changed`
> - [ ] 立刻改回 yk.enable=false（保留老板熔断状态）
> - [ ] 输出 2 次 JSON 到 `tmp/monitor-w-x12a-test-1.json` + `tmp/monitor-w-x12a-test-2.json`
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. 脚本存在
> Test-Path E:\DEMO\数据采集\scripts\monitor-yk-push.ps1
> # 2. 手动跑 1 次
> & E:\DEMO\数据采集\scripts\monitor-yk-push.ps1 | ConvertFrom-Json
> # 3. JSON 输出文件
> Test-Path E:\DEMO\数据采集\tmp\monitor-w-x12a-test-1.json
> Test-Path E:\DEMO\数据采集\tmp\monitor-w-x12a-test-2.json
> ```
>
> ## 严禁
> - ❌ 不要在脚本里改 yk.enable（只读不写）
> - ❌ 不要在监控脚本里写告警触发器（W-X12c 才做）
> - ❌ 不要把 PG 密码写死在脚本里（用 $env:PGPASSWORD）
> - ❌ 不要用 `Get-Process -Name java`（漏 hik-java.exe，铁则 38）
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X12a-result.md`（≥ 1.5 KB，含 3 步实证 + 2 次 JSON 输出）
>
> ## 后续工单（不在本单范围）
> - **W-X12b**：跑 cron / Task Scheduler + 测试（PM 拆细 2 步）
> - **W-X12c**：异常时给老板发 Telegram（PM 拆细 2 步）
