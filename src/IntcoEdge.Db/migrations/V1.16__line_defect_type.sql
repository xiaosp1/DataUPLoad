-- V1.16__line_defect_type.sql
-- PSM Flyway V1.16: 创建 line_defect_type 表（产线-缺陷关联）。

CREATE TABLE line_defect_type (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,                                              -- 缺陷名称
    line_no      TEXT    NOT NULL,                                              -- 产线编号（PG 原 varchar(128)）
    face_no      TEXT    NOT NULL,                                              -- 面编号（PG 原 varchar(128)）
    show_flag    INTEGER NOT NULL DEFAULT 0,                                    -- 0:不展示 1:展示
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
