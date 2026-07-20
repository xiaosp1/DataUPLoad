-- V1.9__line_realtime_data.sql
-- PSM Flyway V1.9: line 加 realtime_data (text JSON)。

ALTER TABLE line ADD COLUMN realtime_data TEXT;               -- 产线实时数据（JSON 字符串）
