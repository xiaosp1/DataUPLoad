-- V1.18__upload_remove_total.sql
-- PSM Flyway V1.18: line_day_record 加 upload_remove_total。

ALTER TABLE line_day_record ADD COLUMN upload_remove_total INTEGER NOT NULL DEFAULT 0;  -- 已上传剔除总数
