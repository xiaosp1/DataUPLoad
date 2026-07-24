# W-X11c-fix-ignore — 修 ignore 字段名错位 bug（拆细·紧急修复）

> **任务**：修 `IgnoreAlarmServiceImpl.isIgnore()` 第 45 行字段名错位 bug（ignore_time → end_time）
>
> **派工时间**：2026-07-23 00:54（PM 夜班计划，W-X11c FAIL 后立刻派）
> **预计耗时**：15 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X11c FAIL 报告已归档
>
> ---
>
> ## 🔴 紧急程度

W-X11c 灰盒验证发现 ignore 过滤**完全失效**，任何 ignore_alarm 行都拦不住报警。yk 永久熔断下没爆出来，但**灰盒跑法核心防线 1/3 失效**。
>
> ---
>
> ## 根因（PM 体检）

`IgnoreAlarmServiceImpl.isIgnore()` 第 45 行查 `IgnoreAlarm::getIgnoreTime`，MyBatis-Plus 默认下划线驼峰映射到 `ignore_time` 列，但 Flyway V1.20 建的表里**根本没有 `ignore_time` 列**（实际是 `end_time`），`IgnoreAlarm` 实体没有 `@TableField("end_time")` 注解修正。
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：定位 + 修代码（5 min）
> - [ ] 读 `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/IgnoreAlarmServiceImpl.java`
> - [ ] 把 `getIgnoreTime()` 调用改为 `getEndTime()`
> - [ ] 如果 `IgnoreAlarm.java` 实体类也没 `endTime` 字段：加 `@TableField("end_time") private LocalDateTime endTime;` + getter/setter
> - [ ] 同样查 `start_time` 是否也错位（如果用 `getStartTime()` 也要确认字段名一致）
>
> ### Step 2：重打 jar + 重启（5 min）
> - [ ] `mvn clean package -DskipTests` 出新 jar → `E:\DataupLoad-final.jar`（备份旧 jar 到 `.bak-wx11c-fix-pre`）
> - [ ] 用 `Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Invoke-CimMethod -MethodName Terminate` 杀 PID 22296（铁则 38）
> - [ ] 启新 jar：`Start-Process -FilePath 'E:\DataupLoad-final.jar' -PassThru`
> - [ ] 等 30 秒 + `curl http://127.0.0.1/health` → 必须 200 OK
>
> ### Step 3：复测 ignore 过滤（5 min）
> - [ ] INSERT ignore_alarm：`INSERT INTO ignore_alarm (line_no, face_no, defect_type, ignore_all, start_time, end_time) VALUES ('line1B', 'B1', '脏污', false, NOW(), NOW() + INTERVAL '1 hour');`
> - [ ] POST 1 条 line1B/B1/脏污 的 alarm → 看返回 `{"code":0}`（拦截成功）OR `{"code":10500}`（没拦）
> - [ ] 检查 PG `alarm_record` 表：line1B/B1 不应新增（如果数据库有 record_time 在 ignore 范围内）
> - [ ] DELETE ignore_alarm 那行（铁则）
> - [ ] 报告：拦截前后 alarm_record 行数对比 + HTTP 返回码
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> $env:PGPASSWORD='***'
> # 1. 进程活的
> Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Select-Object ProcessId
> # 2. 端口
> Get-NetTCPConnection -LocalPort 80 -State Listen
> # 3. ignore_alarm 验证（拦截后行数不变）
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT COUNT(*) FROM alarm_record WHERE line_no='line1B' AND face_no='B1';"
> # 4. 看 Worker 报告里的 fix diff
> Select-String -Path 'E:\DEMO\数据采集\docs\delivered\2026-07-23-W-X11c-fix-result.md' -Pattern 'isIgnore|getEndTime|@TableField'
> ```
>
> ## 严禁
> - ❌ 不要改 `yk.enable=false`（老板 21:23 拍永久熔断）
> - ❌ 不要清 target/classes（Maven clean 是构建标准，可以）
> - ❌ 不要在 ignore_alarm 留测试数据（Step 3 末尾必须 DELETE）
> - ❌ 不要碰 application-prod.yml 任何字段
> - ❌ 不要把 `getStartTime()` / `getEndTime()` 之外的其他字段一起"修"（只修 ignore_time → end_time 这一处）
>
> ## 报告输出
> `docs/delivered/2026-07-23-W-X11c-fix-result.md`（≥ 1.5 KB，含代码 diff + 重启 200 OK 实证 + ignore 拦截前后 alarm_record 行数对比）
>
> ## 后续
> W-X11c-fix PASS 后 W-X11c 灰盒验证**完整 PASS**（1/3 FAIL → 3/3 PASS），可进入 W-X12a / W-X14 / W-X13a 派工。
