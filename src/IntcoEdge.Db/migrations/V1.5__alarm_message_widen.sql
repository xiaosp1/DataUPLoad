-- V1.5__alarm_message_widen.sql
-- PSM Flyway V1.5: alarm_record.message 由 varchar(50) -> varchar(1000)。
-- SQLite 中 TEXT 无长度限制，无需 ALTER；记此脚本以保持迁移版本对齐。

-- intentionally empty: SQLite TEXT already unlimited length.
SELECT 1;
