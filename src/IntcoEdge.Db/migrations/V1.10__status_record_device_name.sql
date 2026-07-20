-- V1.10__status_record_device_name.sql
-- PSM Flyway V1.10: status_record 加 device_name。

ALTER TABLE status_record ADD COLUMN device_name TEXT;        -- varchar(128) in PG
