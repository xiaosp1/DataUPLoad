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
    string? DefectName = null);

/// <summary>
/// 报警记录仓储接口。
/// </summary>
public interface IAlarmRecordRepository
{
    /// <summary>
    /// 写入报警记录（按 uuid 幂等）。返回新增行数（0 = uuid 已存在 / 1 = 新增）。
    /// </summary>
    int InsertOrIgnore(AlarmRecordInput alarm);

    /// <summary>
    /// 查询 uuid 是否已存在（用于去重决策 / 排错）。
    /// </summary>
    bool Exists(string uuid);
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
}
