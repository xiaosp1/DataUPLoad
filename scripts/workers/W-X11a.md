# W-X11a — 修 i18n 资源漏打进 jar（拆细·步骤 1/3）

> **任务**：把 `src/main/resources/i18n/messages_zh_CN.properties`（含 error code 10500 / 20204 / 41900 等所有 PSM 异常码翻译）打进 `E:\DataupLoad-final.jar`
>
> **拆细原因**（铁则 37）：W-X11 原任务 6 步，Worker 卡 i18n 50 分钟没人介入。本次拆 3 步，本单只做"补资源+验证打进 jar"。
>
> **派工时间**：2026-07-22 23:58（老板 23:54 #7874 第 1 条指令）
> **预计耗时**：30 min
> **执行人**：Worker（PM 严盯，每 10 min 查进度）
> **依赖**：无（独立前置）
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：定位 i18n 源文件（5 min）
> - [ ] 找出 PSM 反编译产物里 `messages_zh_CN.properties`（或在 framework-starter jar 里）
>   - 提示：`docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/i18n/messages*.properties`
>   - 或 `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/lib/*.jar` 里
> - [ ] 拷贝到 `DataupLoad/src/main/resources/i18n/messages_zh_CN.properties`
> - [ ] 列文件大小 + key 数 → 报告
>
> ### Step 2：补 build 配置（10 min）
> - [ ] 打开 `DataupLoad/pom.xml`
> - [ ] 确认 `<resources>` 块包含 `src/main/resources/i18n/**`（或 `<filtering>` 配置正确）
> - [ ] 如果用 spring-boot-maven-plugin，确认 include/exclude 规则不漏 i18n
> - [ ] 跑 `mvn clean package -DskipTests` 出新 jar
>
> ### Step 3：验证打进 jar（15 min）
> - [ ] `jar tf E:\DataupLoad-final.jar | grep -i "i18n/messages"` 必须 ≥1 行
> - [ ] `jar xf E:\DataupLoad-final.jar BOOT-INF/classes/i18n/messages_zh_CN.properties` + `file` 命令确认 UTF-8
> - [ ] 起 30 秒冒烟：`java -jar E:\DataupLoad-final.jar` + curl `/client/data/detect` 触发 20204 异常 → 看日志 `NoSuchMessageException` 应消失
> - [ ] 立刻 kill（不起长，避免干扰生产链路）
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> $env:PGPASSWORD='postgres'
> # 1. jar 内是否含 i18n
> & 'C:\Program Files\Java\jdk-17\bin\jar.exe' tf E:\DataupLoad-final.jar | Select-String -Pattern 'i18n/messages'
> # 2. 启动 + 冒烟 + kill（30 秒）
> Start-Process -FilePath 'E:\DataupLoad-final.jar' -ArgumentList '--server.port=18080' -PassThru | Tee-Object -Variable p
> Start-Sleep 15
> try { Invoke-WebRequest -Uri 'http://127.0.0.1:18080/client/data/detect' -Method POST -ContentType 'application/json' -Body '{}' } catch {}
> Get-Content 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log' -Tail 10 | Select-String -Pattern 'NoSuchMessage'
> Stop-Process -Id $p.Id -Force
> ```
>
> ## 严禁
> - ❌ 不要动 application-prod.yml（老板 23:23 拍 yk.enable=false 永久熔断，不能动）
> - ❌ 不要清 target/classes
> - ❌ 不要替换 production jar（用 18080 端口临时验证）
> - ❌ 不要跳过任意 Step 直接打 jar
>
> ## 报告输出
> `docs/delivered/2026-07-22-W-X11a-result.md`（≥ 2 KB，含 3 步实证 + jar 大小 + 冒烟日志片段）
>
> ## 后续工单（不在本单范围）
> - **W-X11b**：用修好的 jar + Flyway baseline 1.19→1.20 + 重启 hik-java + 5 重验证
> - **W-X11c**：W-X11b PASS 后灰盒跑法验证（W-X15/16/17 解锁）
