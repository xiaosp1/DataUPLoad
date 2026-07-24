# W-X11b — 重启 hik-java + Flyway baseline 升级 + 5 重验证（拆细·步骤 2/3）

> **任务**：用 W-X11a 修好的 `E:\DataupLoad-final.jar`（**54.4 MB / i18n 已含**）重启 hik-java（PID 31472 退役） + Flyway baseline 1.19 → 1.20 + 5 重验证
>
> **派工时间**：2026-07-23 00:09（W-X11a PASS 后立刻派）
> **预计耗时**：60 min（多 15 min 因 W-X11a 发现的 2 个新阻塞）
> **执行人**：Worker（PM 严盯，每 10 min 查进度）
> **依赖**：W-X11a PASS ✅
>
> ---
>
> ## ⚠️ Step 0（必须先做）：修 2 个 W-X11a 发现的新阻塞
>
> **W-X11a smoke 实证**：
> 1. **`DefectRecordServiceImpl.java` 被排除编译**（W-B04 历史决策）→ `DetectDataController` 注入 `IDefectRecordService` 找不到实现 → Spring 启动失败
> 2. **datasource URL 写 `localhost:5432`**，但 PG 14 实际跑在 **5433**（老板 23:54 #7874 第 2 条指令修正）
>
> **本工单 Step 0 必须做**：
> - [ ] 检查 `DataupLoad/pom.xml` 看 `<excludes>` 排除了哪些文件 → 报告
> - [ ] **修 1（detect 服务类）**：
>   - 方案 A：`<excludes>` 改成 `<includes>` 或删整段，让 `DefectRecordServiceImpl.java` 进编译
>   - 方案 B：如果 `<excludes>` 是合理配置（文件本身有编译错误），补一个 `DefectRecordServiceImpl` 桩类实现 `IDefectRecordService`（PM 警告：选 B 等于砍掉 detect 功能，要 PM 介入）
> - [ ] **修 2（datasource URL）**：`application-prod.yml` 的 `spring.datasource.url` 从 `localhost:5432` 改成 `127.0.0.1:5433`
>   - 严禁改 `yk.enable=false`（老板 21:23 拍永久熔断）
> - [ ] 跑 `mvn clean package -DskipTests` 重新打 jar → `E:\DataupLoad-final.jar`（备份旧 jar 到 `.bak-W-X11b-pre`）
> - [ ] `jar tf` 验证 i18n 仍在（不能修一个坏一个）+ 服务类已编译
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：备份 + 杀旧进程 + 启动新 jar（10 min）
> - [ ] `Copy-Item E:\DataupLoad-final.jar E:\DataupLoad-final.jar.bak-wx11b -Force`
> - [ ] 用 `Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Invoke-CimMethod -MethodName Terminate` 杀 PID 31472（**铁则 38：不能用 ProcessName=java，会漏 hik-java.exe**）
> - [ ] 启动新 jar：
>   ```powershell
>   Start-Process -FilePath 'E:\DataupLoad-final.jar' -RedirectStandardOutput 'E:\DataupLoad-startup.log' -PassThru | Tee-Object -Variable p
>   ```
> - [ ] 等 30 秒 + `curl http://127.0.0.1/health` → **必须 200 OK**
>   - 如果还是 FAIL：**立刻停手进 PM 介入**，不要自己改 Spring 配置（铁则 40）
>
> ### Step 2：Flyway baseline 升级 + V1.20 自动跑（10 min）
> - [ ] `psql -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"`
> - [ ] 验证 **V1.19 baseline** + **V1.20 ignore_alarm** SUCCESS（如果还没自动跑就手动 psql + INSERT schema_history，按 W-X11 老 workaround）
> - [ ] 检查 `ignore_alarm` 表 11 列（含 ignore_all / start_time / end_time）
>
> ### Step 3：5 重验证（25 min）
> - [ ] **验证 1：yk 熔断** — log 无 `get ticket success`，有 `ticket is null`
> - [ ] **验证 2：报警入 PG** — curl `/client/data/alarm` 推 1 条 → `SELECT id FROM alarm_record` 看到新行
> - [ ] **验证 3：白名单定时** — log 有 `white ip list refresh over, count=2`
> - [ ] **验证 4：i18n 修复** — curl 触发 20204 异常 → log 无 `NoSuchMessageException`
> - [ ] **验证 5：现场数据** — 80 端口 ESTABLISHED ≥ 1 + `status_record` 表有新数据
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> $env:PGPASSWORD='postgres'
> # 1. 新 PID
> Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Select-Object ProcessId, CommandLine
> # 2. 端口 LISTEN
> Get-NetTCPConnection -LocalPort 80 -State Listen
> # 3. Flyway
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"
> # 4. 报警落 PG
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT id, line_no, face_no, message, create_time FROM alarm_record ORDER BY id DESC LIMIT 3;"
> # 5. i18n 修复
> Select-String -Path 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log' -Pattern 'NoSuchMessage' | Select-Object -Last 5
> ```
>
> ## 严禁
> - ❌ 不要改 `yk.enable=false`（老板 21:23 拍永久熔断）
> - ❌ 不要用 `Get-Process -Name java`（漏 hik-java.exe，铁则 38）
> - ❌ 不要在生产链路还没起稳时推测试数据（等 /health 200 后再 curl）
> - ❌ 不要碰 `application-prod.yml` 的 yk.enable（可以改 datasource URL 5432→5433，那个 PG 路径本来就错）
> - ❌ 不要在 Step 1 `/health` 200 后偷偷改更多东西（铁则 40：交付按 DoD 来，不擅自扩展）
>
> ## 报告输出
> `docs/delivered/2026-07-22-W-X11b-result.md`（≥ 3 KB，含 Step 0 实证 + 5 重验证全部）
>
> ## 后续
> W-X11b PASS 后 W-X11c（灰盒跑法 3 验证：W-X15 同化 / W-X16 ignore / W-X17 alarmCount）解锁。

---

## 🆕 铁则 40（新增 2026-07-23 00:09）

> **铁则 40**：Worker Step 0~3 完成后，必须**严格按 DoD 5 重验证清单**逐项回报，每项给实证（命令 + 输出片段），不允许"大致过了"或"应该 OK"模糊话术。
>
> **反例**：W-X11 原报告 8 项 DoD 中 1 项 PARTIAL（jar 体积 95.99MB < 100MB），PM 验收时发现是 Worker 主动剔除 hutool 等换来，**Worker 没报告这个 trade-off**。
>
> **改进**：Worker 报告必须包含每项 DoD 的"做了什么 + 看到什么 + 与预期差多少"，不写"OK"两字。
