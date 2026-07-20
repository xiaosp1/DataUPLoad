using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db.Repository;

// =====================================================================
// 报警记录仓储（W-A4）
//
// 负责把视觉软件推送的报警落到 SQLite `alarm_record` 表：
//   - 按 `uuid` 幂等（视觉软件会去重，但 PSM 端再保险一层）。
//   - 时间格式：DB 存 TEXT 'yyyy-MM-dd HH:mm:ss'。
//   - solve 默认 2（未解决），可被入站 payload 覆盖。
// =====================================================================

/// <summary>
/// alarm_record 入参 POJO（与 EdgeHost DTO 解耦）。
///
/// 字段说明：
///   - Uuid       视觉软件/上游系统给的原始 UUID（底层幂等键，W-A4/W-A7-M 老路径仍用）。
///   - AlarmId    A7 业务幂等键（报警生命周期 ID，由 Service 层生成）。
///                老代码/老 DTO 走 InsertOrIgnore 时可填空，UpsertByAlarmId 时必填。
///   - 其余字段  落 alarm_record 业务列（type/level/message/solve 等）。
///
/// 注：A7 引入的 `alarm_id` / `send_status` / `yk_code` / `error_msg` 列
/// 当前 schema（V1.0~V1.19）尚未迁移到位（PM 铁则 7：仓库层先到位，
/// migration 由 PM/后续 W 拍板）；测试 schema 会先把这些列补上以跑通仓储逻辑。
/// </summary>
public record class AlarmRecordInput(
    string Uuid,
    string Time,
    int Type,
    string LineNo,
    string FaceNo,
    int Level,
    string Message,
    int Solve = 2 /* 未解决 */,
    int? Reason = null,
    string? DefectName = null,
    string? AlarmId = null,
    string SendStatus = "pending" /* pending / sent / failed */,
    int YkCode = 0,
    string? ErrorMsg = null);

/// <summary>
/// 报警记录仓储接口。
/// </summary>
public interface IAlarmRecordRepository
{
    /// <summary>
    /// 写入报警记录（按 uuid 幂等，老 W-A4 路径）。返回新增行数（0 = uuid 已存在 / 1 = 新增）。
    /// </summary>
    int InsertOrIgnore(AlarmRecordInput alarm);

    /// <summary>
    /// 查询 uuid 是否已存在（用于去重决策 / 排错）。
    /// </summary>
    bool Exists(string uuid);

    /// <summary>
    /// 按 alarmId 幂等 upsert 报警记录（W-A7 新路径）。
    /// 命中已有 alarmId → 仅 UPDATE 可变字段（message/defect_name/send_status/yk_code/error_msg/update_time）。
    /// 未命中          → INSERT 一条新行（uuid 留空，alarmId 必填）。
    /// 返回新增行数（0 = UPDATE 命中已有 / 1 = 新插入）。
    /// </summary>
    int UpsertByAlarmId(AlarmRecordInput alarm);

    /// <summary>
    /// 把报警推送结果回写到 alarm_record.send_status / yk_code / error_msg。
    /// 用 alarmId 定位。返回受影响行数（0 = alarmId 不存在 / 1 = 已更新）。
    /// </summary>
    int UpdateSendStatus(string alarmId, string sendStatus, int ykCode, string? errorMsg);
}

public class AlarmRecordRepository : IAlarmRecordRepository
{
    private readonly SqliteConnectionFactory _factory;

    public AlarmRecordRepository(SqliteConnectionFactory factory)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
    }

    public int InsertOrIgnore(AlarmRecordInput alarm)
    {
        if (alarm == null) throw new ArgumentNullException(nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.Uuid)) throw new ArgumentException("uuid 必填", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.Time)) throw new ArgumentException("time 必填", nameof(alarm));
        if (alarm.Type < 1 || alarm.Type > 3) throw new ArgumentException("type 必须为 1/2/3", nameof(alarm));
        if (alarm.Level < 1 || alarm.Level > 4) throw new ArgumentException("level 必须为 1..4", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.LineNo)) throw new ArgumentException("lineNo 必填", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.FaceNo)) throw new ArgumentException("faceNo 必填", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.Message)) throw new ArgumentException("message 必填", nameof(alarm));

        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        using var conn = _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
INSERT OR IGNORE INTO alarm_record
    (uuid, time, type, line_no, face_no, level, message, solve,
     reason, update_time, create_time, defect_name)
VALUES
    ($uuid, $time, $type, $lineNo, $faceNo, $level, $message, $solve,
     $reason, $now, $now, $defectName);";
        cmd.Parameters.AddWithValue("$uuid", alarm.Uuid);
        cmd.Parameters.AddWithValue("$time", alarm.Time);
        cmd.Parameters.AddWithValue("$type", alarm.Type);
        cmd.Parameters.AddWithValue("$lineNo", alarm.LineNo);
        cmd.Parameters.AddWithValue("$faceNo", alarm.FaceNo);
        cmd.Parameters.AddWithValue("$level", alarm.Level);
        cmd.Parameters.AddWithValue("$message", alarm.Message);
        cmd.Parameters.AddWithValue("$solve", alarm.Solve);
        cmd.Parameters.AddWithValue("$reason", (object?)alarm.Reason ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$defectName", (object?)alarm.DefectName ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$now", now);

        // INSERT OR IGNORE：changes() 返回实际写入的行数（命中冲突时 = 0）
        return cmd.ExecuteNonQuery();
    }

    public bool Exists(string uuid)
    {
        if (string.IsNullOrWhiteSpace(uuid)) return false;
        using var conn = _factory.OpenReadOnly();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT 1 FROM alarm_record WHERE uuid = $uuid LIMIT 1;";
        cmd.Parameters.AddWithValue("$uuid", uuid);
        var result = cmd.ExecuteScalar();
        return result is not null && result != DBNull.Value;
    }

    // ============================================================
    // W-A7-R 新增方法
    //
    // 设计要点：
    //   1. alarmId 是 A7 业务幂等键，由 Service 层（AlarmService / Webhook 入站）
    //      在落库前生成（Guid 或 yk 入站 ID）。
    //   2. UpsertByAlarmId 走"先 SELECT，再 INSERT/UPDATE"的显式 upsert
    //      （同 LineRecordRepository 的 UpsertLineDayRecord 实现套路），
    //      因为生产 schema 上暂时没有 UNIQUE INDEX on alarm_id。
    //   3. UpdateSendStatus 假设 alarmId 列已存在；当前生产 schema 还未迁移到位，
    //      单元测试 schema 会补 alarm_id / send_status / yk_code / error_msg 列。
    // ============================================================

    public int UpsertByAlarmId(AlarmRecordInput alarm)
    {
        if (alarm == null) throw new ArgumentNullException(nameof(alarm));
        ValidateAlarm(alarm);
        if (string.IsNullOrWhiteSpace(alarm.AlarmId))
        {
            throw new ArgumentException("alarmId 必填（A7 业务幂等键）", nameof(alarm));
        }
        if (string.IsNullOrWhiteSpace(alarm.SendStatus))
        {
            throw new ArgumentException("sendStatus 必填", nameof(alarm));
        }
        if (alarm.SendStatus is not ("pending" or "pushed" or "failed"))
        {
            throw new ArgumentException("sendStatus 必须为 pending/pushed/failed", nameof(alarm));
        }

        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        using var conn = _factory.Open();

        // 1) 探测：是否已有 alarm_id = $alarmId 的记录？
        using (var probe = conn.CreateCommand())
        {
            probe.CommandText = "SELECT id FROM alarm_record WHERE alarm_id = $alarmId LIMIT 1;";
            probe.Parameters.AddWithValue("$alarmId", alarm.AlarmId);
            var existing = probe.ExecuteScalar();
            if (existing != null && existing != DBNull.Value)
            {
                // 2a) 命中 → UPDATE 可变字段。uuid 不动（保留历史去重键）。
                using var upd = conn.CreateCommand();
                upd.CommandText = @"
UPDATE alarm_record
SET time        = $time,
    type        = $type,
    line_no     = $lineNo,
    face_no     = $faceNo,
    level       = $level,
    message     = $message,
    solve       = $solve,
    reason      = $reason,
    defect_name = $defectName,
    send_status = $sendStatus,
    yk_code     = $ykCode,
    error_msg   = $errorMsg,
    update_time = $now
WHERE alarm_id = $alarmId;";
                upd.Parameters.AddWithValue("$time", alarm.Time);
                upd.Parameters.AddWithValue("$type", alarm.Type);
                upd.Parameters.AddWithValue("$lineNo", alarm.LineNo);
                upd.Parameters.AddWithValue("$faceNo", alarm.FaceNo);
                upd.Parameters.AddWithValue("$level", alarm.Level);
                upd.Parameters.AddWithValue("$message", alarm.Message);
                upd.Parameters.AddWithValue("$solve", alarm.Solve);
                upd.Parameters.AddWithValue("$reason", (object?)alarm.Reason ?? DBNull.Value);
                upd.Parameters.AddWithValue("$defectName", (object?)alarm.DefectName ?? DBNull.Value);
                upd.Parameters.AddWithValue("$sendStatus", alarm.SendStatus);
                upd.Parameters.AddWithValue("$ykCode", alarm.YkCode);
                upd.Parameters.AddWithValue("$errorMsg", (object?)alarm.ErrorMsg ?? DBNull.Value);
                upd.Parameters.AddWithValue("$now", now);
                upd.Parameters.AddWithValue("$alarmId", alarm.AlarmId);
                upd.ExecuteNonQuery();
                return 0; // 命中已有，只 UPDATE，不算新增
            }
        }

        // 2b) 未命中 → INSERT
        using (var ins = conn.CreateCommand())
        {
            ins.CommandText = @"
INSERT INTO alarm_record
    (uuid, alarm_id, time, type, line_no, face_no, level, message, solve,
     reason, update_time, create_time, defect_name,
     send_status, yk_code, error_msg)
VALUES
    ($uuid, $alarmId, $time, $type, $lineNo, $faceNo, $level, $message, $solve,
     $reason, $now, $now, $defectName,
     $sendStatus, $ykCode, $errorMsg);";
            ins.Parameters.AddWithValue("$uuid", (object?)alarm.Uuid ?? DBNull.Value);
            ins.Parameters.AddWithValue("$alarmId", alarm.AlarmId);
            ins.Parameters.AddWithValue("$time", alarm.Time);
            ins.Parameters.AddWithValue("$type", alarm.Type);
            ins.Parameters.AddWithValue("$lineNo", alarm.LineNo);
            ins.Parameters.AddWithValue("$faceNo", alarm.FaceNo);
            ins.Parameters.AddWithValue("$level", alarm.Level);
            ins.Parameters.AddWithValue("$message", alarm.Message);
            ins.Parameters.AddWithValue("$solve", alarm.Solve);
            ins.Parameters.AddWithValue("$reason", (object?)alarm.Reason ?? DBNull.Value);
            ins.Parameters.AddWithValue("$defectName", (object?)alarm.DefectName ?? DBNull.Value);
            ins.Parameters.AddWithValue("$sendStatus", alarm.SendStatus);
            ins.Parameters.AddWithValue("$ykCode", alarm.YkCode);
            ins.Parameters.AddWithValue("$errorMsg", (object?)alarm.ErrorMsg ?? DBNull.Value);
            ins.Parameters.AddWithValue("$now", now);
            ins.ExecuteNonQuery();
            return 1; // 新增
        }
    }

    public int UpdateSendStatus(string alarmId, string sendStatus, int ykCode, string? errorMsg)
    {
        if (string.IsNullOrWhiteSpace(alarmId)) throw new ArgumentException("alarmId 必填", nameof(alarmId));
        if (string.IsNullOrWhiteSpace(sendStatus)) throw new ArgumentException("sendStatus 必填", nameof(sendStatus));
        if (sendStatus is not ("pending" or "pushed" or "failed"))
        {
            throw new ArgumentException("sendStatus 必须为 pending/pushed/failed", nameof(sendStatus));
        }

        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        using var conn = _factory.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
UPDATE alarm_record
SET send_status = $sendStatus,
    yk_code     = $ykCode,
    error_msg   = $errorMsg,
    update_time = $now
WHERE alarm_id = $alarmId;";
        cmd.Parameters.AddWithValue("$sendStatus", sendStatus);
        cmd.Parameters.AddWithValue("$ykCode", ykCode);
        cmd.Parameters.AddWithValue("$errorMsg", (object?)errorMsg ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$now", now);
        cmd.Parameters.AddWithValue("$alarmId", alarmId);
        return cmd.ExecuteNonQuery(); // 0 = alarmId 不存在，1 = 已更新
    }

    // 共享入参校验（老 InsertOrIgnore 也复用：抽出来避免两边规则漂移）。
    private static void ValidateAlarm(AlarmRecordInput alarm)
    {
        if (string.IsNullOrWhiteSpace(alarm.Time)) throw new ArgumentException("time 必填", nameof(alarm));
        if (alarm.Type < 1 || alarm.Type > 3) throw new ArgumentException("type 必须为 1/2/3", nameof(alarm));
        if (alarm.Level < 1 || alarm.Level > 4) throw new ArgumentException("level 必须为 1..4", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.LineNo)) throw new ArgumentException("lineNo 必填", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.FaceNo)) throw new ArgumentException("faceNo 必填", nameof(alarm));
        if (string.IsNullOrWhiteSpace(alarm.Message)) throw new ArgumentException("message 必填", nameof(alarm));
    }
}
