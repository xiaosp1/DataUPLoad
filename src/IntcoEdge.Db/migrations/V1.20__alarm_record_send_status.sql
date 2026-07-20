-- V1.20__alarm_record_send_status.sql
-- W-A7: alarm_record 加报警业务幂等键 + 推送状态字段。
--   - alarm_id:    业务幂等键（A7-M 报警入库时生成/透传）
--   - send_status: 'pending' / 'pushed' / 'failed'
--   - yk_code:     英科网关业务 code（200/400/...）
--   - error_msg:   失败时的错误信息
-- 幂等键：alarm_id 单独建唯一索引（保 NULL 不冲突；SQLite NULL 视为不同）。

ALTER TABLE alarm_record ADD COLUMN alarm_id    TEXT;
ALTER TABLE alarm_record ADD COLUMN send_status TEXT NOT NULL DEFAULT 'pending';
ALTER TABLE alarm_record ADD COLUMN yk_code     INTEGER NOT NULL DEFAULT 0;
ALTER TABLE alarm_record ADD COLUMN error_msg   TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_alarm_record_alarm_id
  ON alarm_record(alarm_id)
  WHERE alarm_id IS NOT NULL;

-- 历史数据回填（已有 alarm_record.uuid → alarm_id，保证 UpsertByAlarmId 不撞键）
UPDATE alarm_record
   SET alarm_id = uuid
 WHERE alarm_id IS NULL;