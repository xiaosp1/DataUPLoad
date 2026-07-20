-- V1.17__remove_total_and_config_rebuild.sql
-- PSM Flyway V1.17:
--   - line_day_record 加 remove_total int default 0 not null
--   - truncate table system_config; 然后重新插入 4 条（含 sound_play_count 下划线版）
--   - ignore_alarm 加唯一索引 (type, line_no, face_no, defect_name)

ALTER TABLE line_day_record ADD COLUMN remove_total INTEGER NOT NULL DEFAULT 0;  -- 剔除总数

-- SQLite 没有 TRUNCATE；DELETE 即可（flyway_schema_history 不在 system_config，无关）。
DELETE FROM system_config;

INSERT INTO system_config (config_name, config_key, config_value, update_time, create_time) VALUES
    ('设备报警音频', 'device_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46'),
    ('缺陷报警音频', 'defect_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46'),
    ('系统报警音频', 'system_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46'),
    ('重复播报次数', 'sound_play_count',       '1',                       '2025-04-15 15:27:46', '2025-04-15 15:27:46');

CREATE UNIQUE INDEX ignore_alarm_type_idx ON ignore_alarm(type, line_no, face_no, defect_name);
