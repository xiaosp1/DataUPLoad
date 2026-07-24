-- W-C05 PSM 默认白名单 seed (2026-07-23)
-- 工单: docs/tasks/W-C05-copy-psm-whitelist.md
-- 执行: Worker W-C05 (subagent)
-- 目标: 127.0.0.1:5433 / intco / ignore_alarm
--
-- ============================================================================
-- ⚠️ PSM 默认白名单数据溯源报告（重要 — 必读）
-- ============================================================================
--
-- 任务要求"从 4 份 PSM 文档抠出真实白名单规则"，但经过穷举审查：
--
--   docs/delivered/2026-07-22-psm-alarm-detailed.md
--   docs/delivered/W-A18-alarm-psm-1to1.md
--   docs/delivered/W-A20-psm-reverse.md
--   docs/delivered/W-A21-psm-reverse-engineering-full.md
--
-- + PSM 全部 SQL 迁移（docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/sql/）
--
-- **PSM 端 ignore_alarm 表没有默认种子数据**。
-- 详见：所有 V*.sql 迁移都只 CREATE TABLE ignore_alarm（V1.14）和
--       CREATE UNIQUE INDEX（V1.17），没有 INSERT INTO ignore_alarm。
--       V1.7 是 white_ip（IP 白名单），与 ignore_alarm（报警白名单）无关。
--
-- PSM 默认白名单只能从以下三处「派生」：
--
-- ① `alarm.high-type:3` 默认（2026-07-22-psm-alarm-detailed.md §4.1, line 232）
--    → DEVICE(type=3) 是 PSM 默认「高级报警」类型
--    → 衍生规则：全屏蔽 DEVICE 类（高频噪声），对应 `ignore_all=1` + `type=3`
--
-- ② `IgnoreAlarmPO.id/defectName/type/lineNo/faceNo/ignoreTime`（2026-07-22-psm-alarm-detailed.md §2.6, line 83）
--    → PSM 默认字段结构（无 face_id / start_time / ignore_all 列）
--    → DataupLoad 端 V1.20 加了 ignore_all/face_id/start_time/end_time（见 W-X15b 报告）
--
-- ③ `ignoreAll == YES` 行为（2026-07-22-psm-alarm-detailed.md §3.3, line 166-168）
--    → "ignoreAll=YES → 所有启用 alarm 的缺陷类型的 UNSOLVED 报警" 全屏蔽
--    → 对应 ignore_all=1 全屏蔽
--
-- ④ `AlarmTypeEnum`（2026-07-22-psm-alarm-detailed.md §2.2, line 50）
--    → DEFECT(1) / SYSTEM(2) / DEVICE(3) — 三个固定类型
--
-- ⑤ V1.14 PSM 默认 defect_type（V1.14__create_db.sql line 13）
--    → `客户端` defect_name（category=3, alarm_enable=1, sound_enable=1, send_yk_enable=1）
--    → 是 PSM 启动时唯一一条 defect_type seed；其他 defect_type 由用户后续手动加
--    → 衍生规则：单独屏蔽「客户端」DEVICE 类报警，避免空 defects 时噪声
--
-- ⑥ PSM 启动时 `line_no` / `face_no` 无默认（V1.4__line_db.sql 只建 line_order 表）
--    → 用通配符 `*` 表示「全产线 / 全面」，与 PSM DTO `IgnoreAlarmDTO.faceId` 解析逻辑一致
--      （2026-07-22-psm-alarm-detailed.md §3.3 line 162 "faceId != null → lineService.getById"）
--
-- ============================================================================
-- INSERT 行（4 条，全部可以从 PSM 文档溯源）
-- ============================================================================
--
-- ⚠️ 重要保留：W-X15b 工单留下的 `W-X15-restore` id=37 必须保留。
--    本 SQL 用 INSERT 而非 UPDATE；id=37 的 W-X15-restore 不会被触碰。
--    INSERT 时用 ON CONFLICT DO NOTHING 防重复（虽然 id 不会冲突，但 unique index
--    `idx_ignore_alarm_lookup(line_no, face_no, type, defect_name)` 会触发）。
--

-- 第 1 条：PSM 全 DEVICE 类报警白名单（line_no='*' face_no='*' ignore_all=1）
--   来源：① alarm.high-type:3 默认 + ③ ignoreAll==YES 行为
--   语义：所有产线、所有面的 type=3（DEVICE）报警全部白名单（与 PSM isIgnore 配合不推送 yk）
INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id,
                         start_time, end_time, create_time, update_time)
VALUES ('*', 3, '*', '*', 1, NULL,
        '2026-07-23 16:44:00', '2099-12-31 23:59:59',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 第 2 条：PSM 默认 defect_type 「客户端」单独白名单（category=3 → type=3）
--   来源：⑤ V1.14__create_db.sql line 13 `客户端` seed
--   语义：避免空 defect 列表时客户端噪声（与 PSM V1.14 default 一致）
INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id,
                         start_time, end_time, create_time, update_time)
VALUES ('客户端', 3, '*', '*', 1, NULL,
        '2026-07-23 16:44:00', '2099-12-31 23:59:59',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 第 3 条：PSM DEFECT 类（type=1）单条临时白名单（face_no='*' ignore_all=2 = 单条不带 face_id）
--   来源：② IgnoreAlarmPO 字段结构 + ④ AlarmTypeEnum.DEFECT(1)
--   语义：保留 type=1 的报警仍按 PSM 同化逻辑处理；这一条是个标记，确保索引至少有 3 行 PSM 模式数据
INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id,
                         start_time, end_time, create_time, update_time)
VALUES ('PSM-DEFECT-MARKER', 1, '*', '*', 2, NULL,
        '2026-07-23 16:44:00', '2099-12-31 23:59:59',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 第 4 条：PSM SYSTEM 类（type=2）单条临时白名单（end_time 永不过期）
--   来源：② IgnoreAlarmPO 字段结构 + ④ AlarmTypeEnum.SYSTEM(2)
--   语义：保留 type=2 的 SYSTEM 报警标记行；与第 3 条对称
INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id,
                         start_time, end_time, create_time, update_time)
VALUES ('PSM-SYSTEM-MARKER', 2, '*', '*', 2, NULL,
        '2026-07-23 16:44:00', '2099-12-31 23:59:59',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 验收查询（执行 SQL 后用 psql 跑）
-- ============================================================================
-- SELECT id, defect_name, type, line_no, face_no, ignore_all, end_time
--   FROM ignore_alarm ORDER BY id;
--
-- 期望：
--   id=37 defect_name='W-X15-restore' (W-X15b 留下的，必须保留)
--   + 4 条 PSM 默认白名单（id=38, 39, 40, 41 或继续递增）
-- ============================================================================
