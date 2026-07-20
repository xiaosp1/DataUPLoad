-- V1.8__add_face_no_to_day_records.sql
-- PSM Flyway V1.8: defect_day_record + line_day_record 加 face_no 字段。

ALTER TABLE defect_day_record ADD COLUMN face_no TEXT;        -- varchar(100) in PG
ALTER TABLE line_day_record   ADD COLUMN face_no TEXT;        -- varchar(100) in PG
