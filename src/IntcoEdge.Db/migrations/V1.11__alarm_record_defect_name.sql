-- V1.11__alarm_record_defect_name.sql
-- PSM Flyway V1.11: alarm_record 加 defect_name（关联 defect_type.name）。

ALTER TABLE alarm_record ADD COLUMN defect_name TEXT;         -- varchar(128) in PG; 冗余字段方便按缺陷名搜索
