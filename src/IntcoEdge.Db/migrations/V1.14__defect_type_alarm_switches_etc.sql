-- V1.14__defect_type_alarm_switches_etc.sql
-- PSM Flyway V1.14:
--   - defect_type 增加 alarm_enable / sound_enable / send_yk_enable 三个开关
--   - line 增加 color
--   - defect_type 默认数据：客户端（推送英科）
--   - 创建 ignore_alarm 表
--   - 创建 system_config 表 + 默认 3 个配置

-- defect_type 三个开关
ALTER TABLE defect_type ADD COLUMN alarm_enable   INTEGER NOT NULL DEFAULT 0;  -- 1:报警 0:不报警
ALTER TABLE defect_type ADD COLUMN sound_enable   INTEGER NOT NULL DEFAULT 0;  -- 1:是 0:否（是否播音）
ALTER TABLE defect_type ADD COLUMN send_yk_enable INTEGER NOT NULL DEFAULT 0;  -- 1:是 0:否（是否推英科）

-- line 加颜色
ALTER TABLE line ADD COLUMN color TEXT;                                        -- 产线颜色

-- defect_type 默认数据：客户端（推送英科）
INSERT INTO defect_type (name, category, count_enable, count_threshold, rate_enable,
                         show_img_enable, alarm_enable, sound_enable, send_yk_enable,
                         update_time, create_time)
VALUES ('客户端', 3, 1, 0, 0, 0, 1, 1, 1,
        '2025-01-08 10:36:40', '2025-01-08 10:36:40');

-- ignore_alarm 表（忽略报警）
CREATE TABLE ignore_alarm (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    defect_name  TEXT    NOT NULL,
    type         INTEGER NOT NULL,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    ignore_time  TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- system_config 表（系统配置）
CREATE TABLE system_config (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    config_name  TEXT    NOT NULL,                                              -- varchar(128)
    config_key   TEXT    NOT NULL,                                              -- varchar(128)
    config_value TEXT    NOT NULL,                                              -- varchar(128)
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 默认配置（V1.14 原始值）
INSERT INTO system_config (config_name, config_key, config_value, update_time, create_time) VALUES
    ('设备报警音频', 'device_alarm_sound_uri',  '/data/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46'),
    ('缺陷报警音频', 'defect_alarm_sound_uri',  '/data/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46'),
    ('系统报警音频', 'system_alarm_sound_uri',  '/data/default.mp3', '2025-04-15 15:27:46', '2025-04-15 15:27:46');
