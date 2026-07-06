-- IntcoEdge MES 上传本地队列 Schema（SQLite）

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS mes_outbox (
    event_id       TEXT NOT NULL PRIMARY KEY,
    source_event_id TEXT,
    machine_id     TEXT NOT NULL,
    line_id        TEXT,
    workshop_id    TEXT,
    shift_id       TEXT,
    event_type     INTEGER NOT NULL,
    event_time     TEXT NOT NULL,
    collected_at   TEXT NOT NULL,
    payload        TEXT NOT NULL,
    extra_props    TEXT,
    status         TEXT NOT NULL,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    last_error     TEXT,
    trace_id       TEXT,
    http_status    INTEGER,
    next_retry_at  TEXT,
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status      ON mes_outbox(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_machine_time ON mes_outbox(machine_id, event_time);
CREATE INDEX IF NOT EXISTS idx_outbox_sent         ON mes_outbox(status, updated_at);

CREATE TABLE IF NOT EXISTS mes_audit_log (
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    event_id     TEXT NOT NULL,
    action       TEXT NOT NULL,
    http_status  INTEGER,
    error        TEXT,
    trace_id     TEXT,
    retry_count  INTEGER,
    created_at   TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_event ON mes_audit_log(event_id, created_at);
