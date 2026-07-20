-- V1.0__init.sql
-- PSM Flyway V1.0: create core detection / alarm / status / line / plan tables.
-- SQLite translation of the original PostgreSQL script (docs/domain/海康大屏逆向/PSM/server/sql/V1.0__create_db.sql).
--
-- Mapping rules (per PM directive):
--   serial           -> INTEGER PRIMARY KEY AUTOINCREMENT
--   int / int4 / int8-> INTEGER
--   varchar(N)       -> TEXT   (length ignored; SQLite has no VARCHAR(N) enforcement)
--   text             -> TEXT
--   bool             -> INTEGER (0/1)
--   timestamp        -> TEXT   (ISO 8601)
--   default current_timestamp -> default CURRENT_TIMESTAMP  (UTC text per SQLite docs)
--   comment on table / column  -> dropped (SQLite has no comment syntax)
--   create trigger ... upd_timestamp() -> dropped (update_time handled in application layer)
--   public.<x>      -> <x>     (no schema namespace in SQLite)
--   create index ... USING btree -> create index ... (SQLite uses btree by default)
--   foreign keys     -> none (per PM: SQLite skips FK; validation in application layer)
--
-- Pre-Flyway PSM tables role / user / white_ip have no DDL in the V* scripts.
-- PSM said "Flyway 之前的脚本建的，SQL 没看到". We reconstruct them here in V1.0
-- (the "create_db" entry point) so the 19-script contract holds and referential
-- targets for V1.1 / V1.7 exist. Fields inferred from V1.1 (role.permission,
-- role.role) and V1.7 (white_ip.ip, create_time, update_time) usage.

-- ============= pre-Flyway reconstructed tables =============

CREATE TABLE role (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    "role"      TEXT    NOT NULL,
    permission  TEXT,
    create_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "user" (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL,
    password    TEXT    NOT NULL,
    role_id     INTEGER,
    create_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE white_ip (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    ip          TEXT    NOT NULL,
    create_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============= V1.0 core tables (verbatim translation) =============

-- defect_record (缺陷记录表) -- largest, holds every detection event
CREATE TABLE defect_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    glove_no     TEXT    NOT NULL,
    result       INTEGER NOT NULL,                 -- 1=良品 2=次品
    defect_type  TEXT    NOT NULL,
    img_list     TEXT    NOT NULL,                 -- JSON 字符串，图片路径列表
    "time"       TEXT    NOT NULL,                 -- 检测时间（ISO 8601）
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX index_join ON defect_record("time", line_no, defect_type, result, face_no);

-- workshop_day_record (车间单日检测数据汇总表)
CREATE TABLE workshop_day_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    right_count  INTEGER NOT NULL DEFAULT 0,
    error_count  INTEGER NOT NULL DEFAULT 0,
    need_count   INTEGER NOT NULL DEFAULT 0,
    "time"       TEXT    NOT NULL,                 -- 汇总日期 'yyyy-MM-dd'
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- line_day_record (产线单日检测数据汇总表)
CREATE TABLE line_day_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    right_count  INTEGER NOT NULL DEFAULT 0,
    error_count  INTEGER NOT NULL DEFAULT 0,
    line_no      TEXT    NOT NULL,
    "time"       TEXT    NOT NULL,                 -- 汇总日期
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- defect_day_record (每日缺陷数量汇总表)
CREATE TABLE defect_day_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    count        INTEGER NOT NULL DEFAULT 0,
    "time"       TEXT    NOT NULL,                 -- 汇总日期
    line_no      TEXT    NOT NULL,
    type         TEXT    NOT NULL,                 -- 缺陷类型
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- alarm_record (报警记录表)
CREATE TABLE alarm_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid         TEXT    NOT NULL,                 -- 报警唯一标识（去重用）
    "time"       TEXT    NOT NULL,                 -- 报警时间 'yyyy-MM-dd HH:mm:ss'
    type         INTEGER NOT NULL,                 -- 1=缺陷 2=系统 3=设备
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    level        INTEGER NOT NULL,                 -- 1=一般 2=严重
    message      TEXT    NOT NULL,
    solve        INTEGER NOT NULL DEFAULT 2,       -- 1=已解决 2=未解决
    reason       INTEGER,                          -- 1=客户端掉线 等
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- status_record (设备状态记录表)
CREATE TABLE status_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    "time"       TEXT    NOT NULL,                 -- 上报时间 'yyyy-MM-dd HH:mm:ss'
    type         INTEGER NOT NULL,                 -- 1=相机 2=剔除机 3=客户端
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    status       INTEGER NOT NULL DEFAULT 1,       -- 1=在线 2=掉线
    device_no    TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- plan (方案/配方表)
CREATE TABLE plan (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    uri          TEXT    NOT NULL,
    description  TEXT,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- line (产线表)
CREATE TABLE line (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    client_no    TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- plan_to_line (方案与产线关联表)
CREATE TABLE plan_to_line (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_id      INTEGER NOT NULL,
    plan_id      INTEGER NOT NULL,
    status       INTEGER NOT NULL DEFAULT 2,       -- 1=启用 2=禁用
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
