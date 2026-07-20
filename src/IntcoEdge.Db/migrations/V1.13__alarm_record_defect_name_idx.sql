-- V1.13__alarm_record_defect_name_idx.sql
-- PSM Flyway V1.13: 创建 alarm_record_defect_name_idx 索引。
-- PG 原 SQL: CREATE INDEX ... USING btree (defect_name, solve, line_no, face_no);

CREATE INDEX alarm_record_defect_name_idx ON alarm_record(defect_name, solve, line_no, face_no);
