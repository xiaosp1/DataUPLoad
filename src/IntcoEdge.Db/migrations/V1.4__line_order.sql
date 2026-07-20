-- V1.4__line_order.sql
-- PSM Flyway V1.4: 创建 line_order 表（产线顺序）。

CREATE TABLE line_order (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id      INTEGER NOT NULL,
    order_value  INTEGER NOT NULL,                          -- PG 用了 "order_value"（避开 SQL 关键字 order）
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
