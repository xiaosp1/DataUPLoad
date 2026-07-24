# W-X11c — 灰盒跑法 3 验证（拆细·步骤 3/3）

> **任务**：W-X11b PASS 后做灰盒跑法最终验证：同化逻辑 / ignore 过滤 / alarmCount 聚合
>
> **派工时间**：2026-07-23 00:35（W-X11b PASS 后立刻派）
> **预计耗时**：45 min
> **执行人**：Worker（PM 严盯，每 10 min 查进度）
> **依赖**：W-X11b PASS ✅（hik-java PID 22296 / 80 LISTEN / 5 重验证全过 / yk 永久熔断）
>
> ---
>
> ## 背景（老板 21:54 灰盒跑法理论框架）

老板 21:54 #7798 拍板：
- PSM 防爆全套搬上来 + **yk.enable 永久 false** + 本地监控上传行为
- 解除灰盒 3 条件：24h 无异常 + 6 防爆全验证 + 老板拍
- 6 防爆（PM 拆 3 类可立刻验证）：**同化 / ignore / alarmCount**

**W-X11c 任务**：验证前 3 个防爆逻辑真在工作（用真实数据 + 推测试数据触发，不推 yk）。
**W-X12~W-X17 后续工单**：监控脚本 / SOP / 24h 观察 / 解除灰盒（不在本工单范围）。
>
> ---
>
> ## DoD（3 步）
>
> ### Step 1：同化逻辑验证（15 min）
>
> **目的**：推送同一条 line_no+face_no+glove_no 的 defect_record，验证第 2 条**同化**（合并）进第 1 条，不再新增 defect_record 行。
>
> 步骤：
> - [ ] 查 `psql -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT MAX(id), MAX(time) FROM defect_record;"` 拿到当前 max id
> - [ ] POST `/client/data/defect` 推 2 条同 line_no/face_no/glove_no 但 result 不同（NG vs OK）的 defect 记录，时间间隔 5 秒
>   ```json
>   {"line_no":"line1B","face_no":"B1","glove_no":"G-TEST-001","result":1,"defect_type":"脏污","img_list":"[]"}
>   ```
> - [ ] 验证：`defect_record` 表应该只有 1 条（不是 2 条）OR 第 2 条 result 覆盖第 1 条
> - [ ] 验证：`api_log` 表有 2 条 POST /client/data/defect 记录（推送是 2 条）
> - [ ] **如果同化没生效**：进 PM 介入，不要自己改 `DefectRecordServiceImpl`（铁则 40）
>
> ### Step 2：ignore 过滤验证（15 min）
>
> **目的**：把某条 alarm 加进 `ignore_alarm` 表，验证新触发的同类 alarm **不入库**（或不被推 yk，虽然 yk 已熔断）。
>
> 步骤：
> - [ ] INSERT 1 条 ignore_alarm：`psql ... -c "INSERT INTO ignore_alarm (line_no, face_no, defect_type, ignore_all, start_time, end_time) VALUES ('line1B', 'B1', '脏污', false, NOW(), NOW() + INTERVAL '1 hour');"`
> - [ ] POST 2 条 line1B/B1/脏污 的 defect 记录（10 秒间隔）
> - [ ] 验证：`alarm_record` 表 **不应该** 新增 line1B/B1/脏污 的报警
> - [ ] 验证：`api_log` 有 2 条 POST 记录（推送仍是 2 条，但被 ignore 拦了）
> - [ ] 清理：DELETE ignore_alarm 那条测试数据
>
> ### Step 3：alarmCount 聚合验证（15 min）
>
> **目的**：连续推 N 条同 line_no+face_no 的报警，验证 `alarmCount` 字段**累加**，不是每条都单独入库。
>
> 步骤：
> - [ ] `psql -c "SELECT MAX(id), MAX(alarm_count) FROM alarm_record WHERE line_no='line1B' AND face_no='B2';"` 拿当前基线
> - [ ] POST 3 条 line1B/B2 同类报警（间隔 5 秒）
> - [ ] 验证：`alarm_record` 表 line1B/B2 行数增加，但 alarm_count **累加**（如 alarm_count=3 + 1=4 或类似聚合逻辑）
> - [ ] 验证：`api_log` 有 3 条 POST 记录
> - [ ] 验证：`alarm_record.id` 可能只有 1 行但 alarm_count = N（聚合），不是 3 行（每条独立）
>
> ---
>
> ## 验收命令（PM 跑）
> ```powershell
> # 1. 链路活的
> Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Select-Object ProcessId
> Get-NetTCPConnection -LocalPort 80 -State Listen
> # 2. alarm_record 变化
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT id, line_no, face_no, message, alarm_count, create_time FROM alarm_record ORDER BY id DESC LIMIT 5;"
> # 3. defect_record 同化验证
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT id, line_no, face_no, glove_no, result, time FROM defect_record ORDER BY id DESC LIMIT 5;"
> # 4. ignore_alarm 状态
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT * FROM ignore_alarm;"
> # 5. api_log 推送次数
> & 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT endpoint, COUNT(*) FROM api_log WHERE create_time > NOW() - INTERVAL '5 minutes' GROUP BY endpoint ORDER BY COUNT(*) DESC;"
> ```
>
> ## 严禁
> - ❌ 不要改 `yk.enable=false`（老板 21:23 拍永久熔断）
> - ❌ 不要在 ignore_alarm 留测试数据（Step 2 末尾必须 DELETE）
> - ❌ 不要推真实 line_no 避免污染车间数据（用 `line1B / G-TEST-*` 测试命名）
> - ❌ 不要碰 `DefectRecordServiceImpl / AlarmRecordServiceImpl` 源码（铁则 40：本工单只验证不修复）
> - ❌ 不要在 Step 失败时擅自重写代码，必须进 PM 介入
>
> ## 报告输出
> `docs/delivered/2026-07-22-W-X11c-result.md`（≥ 3 KB，含 3 步实证 + 6 防爆当前进度 + 建议后续）
>
> ## 后续
> W-X11c PASS 后：
> - **W-X12**：灰盒监控脚本 `monitor-yk-push.ps1`（监控 yk 调用 / PG alarm_record 增长 / yk.enable 配置）
> - **W-X14**：灰盒跑法 SOP `yk-graybox-monitor.md`（把铁则 36/37/40 固化）
> - **24h 观察期**（PM 自动监控，老板不需介入）
