-- V1.2__defect_type.sql
-- PSM Flyway V1.2: 创建 defect_type 表 + defect_day_record 上加索引。
-- 原始 PG：
--   create table public.defect_type (
--       id serial primary key, "name" varchar(32) not null, category int not null default 3,
--       count_enable bool not null default false, count_threshold int4 not null default 0,
--       rate_enable bool not null default false,
--       update_time timestamp not null default current_timestamp,
--       create_time timestamp not null default current_timestamp);
--   create index defect_day_record_time_idx on public.defect_day_record ("time");

CREATE TABLE defect_type (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,                       -- 缺陷名称（"name" 在 PG 是关键字，SQLite 用普通列名即可，但保留语义）
    category        INTEGER NOT NULL DEFAULT 3,             -- 1=破损 2=脏污 3=其他
    count_enable    INTEGER NOT NULL DEFAULT 0,             -- bool: 是否统计缺陷计数
    count_threshold INTEGER NOT NULL DEFAULT 0,             -- 缺陷计数阈值
    rate_enable     INTEGER NOT NULL DEFAULT 0,             -- bool: 是否统计缺陷率
    update_time     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX defect_day_record_time_idx ON defect_day_record("time");
