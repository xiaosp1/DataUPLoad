# W-X13a — EdgeHost C# 端 yk.enable 字段（拆细·步骤 1/3）

> **任务**：在 `src/IntcoEdge/appsettings.json` 加 `yk.enable` 字段（默认 false）
>
> **派工时间**：W-X11c PASS 后立刻派（与 W-X12a / W-X14 并行）
> **预计耗时**：15 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：无（独立）
>
> ---
>
> ## 背景

C# EdgeHost 已退役（intco.db 94.68 MB 保留），但代码还在仓库。W-X11b 把 yk 永久熔断落地到 Java 端（hik-java），C# 端没有对应熔断。

老板 23:23 拍"yk.enable=false 永久熔断"是 Java 端决定，但 C# 端有同样的 yk 推送代码（YKService.cs），如果不加熔断，将来如果重启 C# EdgeHost 会立即往 yk 推 → 触发老板叫停的熔断红线。

W-X13 三步给 C# 端补完整熔断：
- **W-X13a**（本工单）：appsettings.json 加字段
- **W-X13b**：YKService.cs 加 3 道熔断
- **W-X13c**：写 ADR-0006 + 单测 + 部署包验证
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：读现有 appsettings.json（3 min）
> - [ ] 读 `src/IntcoEdge/appsettings.json` 当前内容
> - [ ] 找现有的"yk" 或 "ykservice" 相关字段（如果已存在直接复用）
> - [ ] 报告当前 yk 配置结构
>
> ### Step 2：加 yk.enable 字段（5 min）
> - [ ] 在 `"yk"` 段加 `"enable": false`（如果没有 "yk" 段就新建）
> - [ ] 保留所有现有字段不动（不能让 C# 项目编译失败）
> - [ ] 写注释：`"// yk.enable = false: 老板 2026-07-22 23:23 拍板永久熔断，禁止推 yk"`
>
> ### Step 3：build 验证（7 min）
> - [ ] `dotnet build src/IntcoEdge/IntcoEdge.csproj -c Release` 必须 BUILD SUCCESS
> - [ ] 跑现有 unit test：`dotnet test src/IntcoEdge.Tests/IntcoEdge.Tests.csproj`（如果有）必须 0 fail
> - [ ] 报告 build 输出最后 20 行 + test 结果
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. yk.enable 字段存在
> Select-String -Path E:\DEMO\数据采集\src\IntcoEdge\appsettings.json -Pattern 'yk.*enable'
> # 2. build
> dotnet build E:\DEMO\数据采集\src\IntcoEdge\IntcoEdge.csproj -c Release | Select-Object -Last 10
> # 3. dotnet 版本
> dotnet --version
> ```
>
> ## 严禁
> - ❌ 不要删现有 yk 配置字段（只加不删）
> - ❌ 不要改 yk.enable 默认值 true（必须 false，老板拍）
> - ❌ 不要碰其他配置文件（appsettings.Development.json 之类）
> - ❌ 不要试图启动 C# EdgeHost（已退役，跑不起来，避免动到 intco.db）
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X13a-result.md`（≥ 1 KB，含 3 步实证 + appsettings.json diff + build 输出）
>
> ## 后续工单（不在本单范围）
> - **W-X13b**：YKService.cs 加 3 道熔断（enable=false / ticket null / 白名单外 IP）
> - **W-X13c**：ADR-0006 + 单测 + 部署包验证
