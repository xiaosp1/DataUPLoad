-- V1.19__state_change.sql
-- PSM Flyway V1.19:
--   - 创建 state_change 表（设备状态变更）
--   - 创建 state_statistic 表（设备状态统计）+ 唯一索引
--   - status_record 加 line_id int NULL

-- state_change
CREATE TABLE state_change (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id      INTEGER NOT NULL,
    type         INTEGER NOT NULL,                                                -- 0:下线 1:上线
    change_time  TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- state_statistic
CREATE TABLE state_statistic (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id         INTEGER NOT NULL,
    ok_time         INTEGER NOT NULL DEFAULT 0,                                  -- OK 时长（PG int8 -> INTEGER）
    error_time      INTEGER NOT NULL DEFAULT 0,                                  -- 异常时长
    statistic_time  TEXT    NOT NULL,
    update_time     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time     TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX statistic_idx ON state_statistic(line_id, statistic_time);

-- status_record 加 line_id
ALTER TABLE status_record ADD COLUMN line_id INTEGER;                             -- 产线 ID（可空）
