# W-X13b — C# YK 推送 3 道熔断

> **任务**：在 `src/IntcoEdge.EdgeHost/Services/Yingke/YingkeServiceImpl.cs` 加 3 道熔断（yk.enable=false / ticket null / 白名单外 IP），顺手清 W-X13a 发现的 CS0162 warning
>
> **派工时间**：2026-07-23 00:58（W-X13a PASS 后立刻派）
> **预计耗时**：25 min（多 10 min 修 CS0162 warning）
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X13a PASS ✅
>
> ---
>
> ## ⚠️ 路径修正（Worker W-X13a 提醒）

工单 W-X13a 写的 `src/IntcoEdge/appsettings.json` 实际不存在，正确路径是 **`src/IntcoEdge.EdgeHost/appsettings.json`**。本工单沿用 W-X13a 修正后的正确路径。
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：定位 YK 推送入口（5 min）
> - [ ] 找 `YingkeServiceImpl.cs` 的推送方法（`SendAlarmToYingke` / `PushToYingke` / 类似名字）
> - [ ] 看 W-X13a 报告里提到的 line 133 附近（CS0162 unreachable code 警告位置）
> - [ ] 报告：列出 YK 推送的 3 个入口方法（如果有多个）
>
> ### Step 2：加 3 道熔断（10 min）
> - [ ] **熔断 1（配置门）**：方法最前面读 `Configuration["yk:enable"]`，false 直接 return + log info
> - [ ] **熔断 2（票据门）**：检查 `_yingkeClient.CurrentTicket` 或类似字段，null 直接 return + log warn
> - [ ] **熔断 3（白名单门）**：检查推送目标 IP 在白名单（`Configuration.GetSection("IntcoEdge:YingkeGateway:WhiteList")`），不在白名单直接 return + log warn
> - [ ] 顺手清 CS0162 warning：把 line 133 后 unreachable code 删除（如果是合法 unreachable，例如 `if(false)` 调试代码就删整段）
>
> ### Step 3：build + test（10 min）
> - [ ] `dotnet build E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\IntcoEdge.EdgeHost.csproj -c Release` → 0 error 0 warning（CS0162 也要清掉）
> - [ ] `dotnet test` 248 全过（如果新增熔断导致测试 fail，加 3 个新单测验证熔断生效）
> - [ ] 报告：build 输出最后 10 行 + test 结果 + 3 道熔断的代码位置（行号 + diff）
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. build 干净
> dotnet build E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\IntcoEdge.EdgeHost.csproj -c Release | Select-Object -Last 15
> # 2. test 全过
> dotnet test E:\DEMO\数据采集\src\IntcoEdge.EdgeHost.Tests\IntcoEdge.EdgeHost.Tests.csproj 2>&1 | Select-Object -Last 20
> # 3. 3 道熔断都在代码里
> Select-String -Path 'E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\Services\Yingke\YingkeServiceImpl.cs' -Pattern 'yk:enable|CurrentTicket|WhiteList'
> ```
>
> ## 严禁
> - ❌ 不要改 `yk.enable` 默认值（必须 false）
> - ❌ 不要启 C# EdgeHost（避免动到 intco.db）
> - ❌ 不要改 appsettings.json 之外的其他配置
> - ❌ 不要把 3 道熔断加在错误位置（比如 Service 构造函数而非推送方法）
> - ❌ 不要试图加新功能（只加 3 道熔断 + 清 CS0162 warning）
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X13b-result.md`（≥ 2 KB，含 3 道熔断代码 diff + build 0 warning 实证 + test 结果）
>
> ## 后续
> W-X13b PASS 后 W-X13c（ADR-0006 + 部署包验证）。
