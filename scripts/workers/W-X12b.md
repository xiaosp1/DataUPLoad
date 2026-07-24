# W-X12b — 监控脚本 cron 测试（拆细·步骤 2/3）

> **任务**：把 `scripts/monitor-yk-push.ps1` 接入 Windows Task Scheduler，每 60s 跑一次，验证 5 min 持续运行 OK
>
> **派工时间**：2026-07-23 01:04（W-X12a PASS 后立刻派）
> **预计耗时**：20 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X12a PASS ✅（脚本 12.5 KB / 4 函数 / JSON 输出 / yk.enable=false 已 byte-byte 恢复）
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：建 Task Scheduler 任务（8 min）
> - [ ] 用 `schtasks` 创建任务：
>   ```powershell
>   schtasks /Create /SC ONCE /ST 00:00 /TN "PM_Monitor_Yk_Push" /TR "powershell -ExecutionPolicy Bypass -File E:\DEMO\数据采集\scripts\monitor-yk-push.ps1" /RU SYSTEM /F
>   schtasks /Create /SC MINUTE /MO 1 /TN "PM_Monitor_Yk_Push_Loop" /TR "powershell -ExecutionPolicy Bypass -Command \"& 'E:\DEMO\数据采集\scripts\monitor-yk-push.ps1' | Out-File -Append E:\DEMO\数据采集\tmp\monitor-loop.log\"" /RU SYSTEM /F
>   ```
> - [ ] 验证任务存在：`schtasks /Query /TN "PM_Monitor_Yk_Push_Loop" /V /FO LIST | Select-String "Status\|Next Run Time\|Last Run Time"`
> - [ ] 不要启用 GUI 弹窗（脚本无 UI，silent OK）
>
> ### Step 2：跑 5 min 验证（10 min）
> - [ ] 启动任务 + 等 5 min（这步要 wait，可后台跑）
> - [ ] 检查日志：`Get-Content 'E:\DEMO\数据采集\tmp\monitor-loop.log' -Tail 5` 必须 ≥ 5 行 JSON（每分钟 1 行）
> - [ ] 验证每条 JSON `status: "OK"`，无 `ALERT`
> - [ ] 检查没有 yk.enable 配置被改（铁则 36）
>
> ### Step 3：手动触发 1 次 ALERT 验证（2 min）
> - [ ] 故意改 yk.enable=true → 等下一个 cron tick → 看日志应该有 `ALERT` 行
> - [ ] 立刻改回 yk.enable=false（**必须 byte-byte 恢复**）
> - [ ] 等下一个 tick → 看日志又 `OK`
> - [ ] 报告：log 截图 + ALERT/OK 时间戳
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. 任务存在
> schtasks /Query /TN "PM_Monitor_Yk_Push_Loop" /FO LIST
> # 2. 日志行数
> (Get-Content 'E:\DEMO\数据采集\tmp\monitor-loop.log' -ErrorAction SilentlyContinue | Measure-Object).Count
> # 3. 最近 5 行状态
> Get-Content 'E:\DEMO\数据采集\tmp\monitor-loop.log' -Tail 5
> # 4. yk.enable 状态（铁则 36）
> Select-String -Path 'E:\DEMO\数据采集\DataupLoad\src\main\resources\application-prod.yml' -Pattern 'yk:.*enable'
> ```
>
> ## 严禁
> - ❌ 不要改 yk.enable=false（铁则 36，必须 byte-byte 保留）
> - ❌ 不要把监控间隔改成 < 60s（PM 监控是辅助，避免过分抢占资源）
> - ❌ 不要删 / 停已经跑着的 cron 任务（W-X12c 才做告警升级）
> - ❌ 不要在监控脚本里硬编码任何密码
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X12b-result.md`（≥ 1.5 KB，含 3 步实证 + log 截图 + yk.enable 状态）
>
> ## 后续
> W-X12b PASS 后 W-X12c（异常时给老板发 Telegram）。
