-- V1.7__white_ip_init.sql
-- PSM Flyway V1.7: white_ip 默认放行 '*.*.*.*'。
-- PG 原 SQL: INSERT INTO public.white_ip(id, ip, create_time, update_time) VALUES(default, '*.*.*.*', ...);
-- SQLite: id 列 INTEGER PRIMARY KEY AUTOINCREMENT 不用 default 关键字，省略即可。

INSERT INTO white_ip (ip, create_time, update_time)
VALUES ('*.*.*.*', '2024-07-23 14:22:22', '2024-07-23 14:22:22');
