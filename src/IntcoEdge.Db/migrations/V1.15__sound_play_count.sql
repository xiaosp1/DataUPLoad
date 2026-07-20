-- V1.15__sound_play_count.sql
-- PSM Flyway V1.15: system_config 增加 sound-play-count。

INSERT INTO system_config (config_name, config_key, config_value, update_time, create_time)
VALUES ('重复播报次数', 'sound-play-count', '1', '2025-04-15 15:27:46', '2025-04-15 15:27:46');
