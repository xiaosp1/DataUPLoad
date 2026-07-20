-- V1.6__defect_record_backup.sql
-- PSM Flyway V1.6: 创建 defect_record_backup 表（结构与 defect_record 一致 + except_flag）。

CREATE TABLE defect_record_backup (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    glove_no     TEXT    NOT NULL,
    result       INTEGER NOT NULL,                          -- 1=良品 2=次品
    defect_type  TEXT    NOT NULL,
    img_list     TEXT    NOT NULL,
    "time"       TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    except_flag  INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX index_join_backup ON defect_record_backup("time", line_no, defect_type, result, face_no);
