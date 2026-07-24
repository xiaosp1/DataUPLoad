# W-X13c — ADR-0006 C# 端 yk.enable + 3 道熔断 + 恢复推送操作步骤

> **任务**：写 `docs/adr/0006-csharp-yk-circuits.md`（C# 端 yk.enable + 3 道熔断 ADR）+ 完整恢复推送操作步骤
>
> **派工时间**：2026-07-23 01:13（W-X13b PASS 后立刻派）
> **预计耗时**：20 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X13b PASS ✅（3 道熔断 + 10 单测 + build 0 warning）
>
> ---
>
> ## ⚠️ 关键：恢复推送操作步骤必须写清楚

W-X13b Worker 反馈：**老板如要恢复 YK 推送（`yk.enable=true`），必须同时配置 `IntcoEdge:YingkeGateway:WhiteList` 白名单数组，否则熔断 3 fail-closed（这是 by design 防意外开）**。

本 ADR 必须把"恢复推送的完整 3 步操作"写得**老板能照着做**，避免将来误开。
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：写 ADR-0006 主体（12 min）
>
> 章节结构（不要漏）：
>
> - **§1 决策**：C# EdgeHost 加 yk.enable=false 永久熔断 + 3 道熔断（配置门 / 票据门 / 白名单门）
> - **§2 背景**：W-X11 系列 Java 端已熔断（hik-java yk.enable=false）；C# 端有同样 YK 推送代码（YingkeServiceImpl.cs），如果不加熔断，将来有人重启 C# EdgeHost 会立即往 yk 推 → 触发老板 21:23 拍的红线
> - **§3 老板拍板**：
>   - 21:23 yk.enable=false 永久熔断（Java 端，铁则 36）
>   - 23:54 #7917 W-X13 启动 C# 端熔断补完
>   - 00:46 #7923 W-X13b 3 道熔断代码落地
> - **§4 实现细节**：
>   - yk.enable 默认值 false（appsettings.json）
>   - 熔断 1（配置门）：line 135 `YingkeServiceImpl.HandleOneAsync`，读 `_configuration["yk:enable"]`，false/0/空 → return
>   - 熔断 2（票据门）：line 148 `_ykClient.TicketCache.DebugState.Ticket` null → return（用现有 API，等价于 `_yingkeClient.CurrentTicket`）
>   - 熔断 3（白名单门）：line 160 解析 `YingkeGateway.Url` host，对比 `IntcoEdge:YingkeGateway:WhiteList` 数组；不在/缺失/空 → return（fail-closed）
> - **§5 测试覆盖**：258 test 全过（248 原有 + 10 新单测），新单测文件 `src/IntcoEdge.Tests/Service/YingkeServiceImplCircuitBreakerTests.cs`
> - **§6 拒绝的方案**：
>   - 方案 A：在构造函数拦截（拒绝原因：构造函数无法访问推送参数如 IP/票据状态）
>   - 方案 B：用 try/catch 包推送（拒绝原因：推成功后才报错，老板已禁止任何推送）
>   - 方案 C：删 YK 推送代码（拒绝原因：将来如需恢复会丢代码，PM 反对"硬删除"）
> - **§7 恢复推送操作步骤（老板专用，照着做）**：
>
>   ```powershell
>   # ⚠️ 老板专用：恢复 YK 推送必须 3 步同时做，缺一熔断 3 fail-closed
>   # Step A：配白名单（必须先做，否则下面两步开了也会被熔断 3 拦）
>   # 编辑 E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\appsettings.json
>   # 在 IntcoEdge:YingkeGateway 段加 WhiteList 数组：
>   #   "IntcoEdge": { "YingkeGateway": { "WhiteList": ["192.168.135.150", "10.x.x.x"] } }
>   # 里面填你允许推 YK 的目标 IP（按 YingkeGateway.Url 的 host 填）
>
>   # Step B：开 yk.enable
>   # 同文件 yk 段改 enable: true
>
>   # Step C：重启 C# EdgeHost（pm 自己派工单重启，不写在这里）
>   ```
>
> - **§8 验证清单**：熔断 1/2/3 全部命中 + 单测 258 pass + build 0 warning + yk.enable=false 默认未动
>
> ### Step 2：交叉引用（3 min）
> - [ ] STATUS.md 末尾加 "## ADR 引用" 段 → 链 ADR-0006
> - [ ] docs/delivered/INDEX.md 第 9 节加引用
> - [ ] docs/SOP/yk-graybox-monitor.md（W-X14 已写）加引用
> - [ ] 在 ADR-0005 后面追加 "## 后续 ADR" 段引 ADR-0006
>
> ### Step 3：PM 体检（5 min）
> - [ ] ADR 文件 ≥ 4 KB
> - [ ] 8 个章节齐全（§1-§8）
> - [ ] §7 恢复推送 3 步操作可用（老板能照着做）
> - [ ] 4 处交叉引用都在
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. ADR 文件存在
> Test-Path E:\DEMO\数据采集\docs\adr\0006-csharp-yk-circuits.md
> # 2. 字数
> (Get-Content E:\DEMO\数据采集\docs\adr\0006-csharp-yk-circuits.md -Encoding UTF8 | Measure-Object -Word).Words
> # 3. 8 章节齐全
> Select-String -Path E:\DEMO\数据采集\docs\adr\0006-csharp-yk-circuits.md -Pattern '^## §'
> # 4. 交叉引用都在
> Select-String -Path E:\DEMO\数据采集\STATUS.md -Pattern '0006-csharp-yk-circuits'
> Select-String -Path E:\DEMO\数据采集\docs\delivered\INDEX.md -Pattern '0006-csharp-yk-circuits'
> Select-String -Path E:\DEMO\数据采集\docs\SOP\yk-graybox-monitor.md -Pattern '0006-csharp-yk-circuits'
> ```
>
> ## 严禁
> - ❌ 不要漏写 §7 恢复推送操作步骤（老板将来用）
> - ❌ 不要在 ADR 里改铁则原文（只能引用 + 解释）
> - ❌ 不要试图加新功能（只写 ADR + 交叉引用）
> - ❌ 不要碰 C# 代码（已 PASS，W-X13c 范围外）
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X13c-result.md`（≥ 1.5 KB，含 3 步实证 + ADR 字数 + 4 处交叉引用截图）
