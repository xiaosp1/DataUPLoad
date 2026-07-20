using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db.Repository;

// =====================================================================
// 产线当日汇总仓储（W-A4）
//
// 负责把视觉软件推送的当日统计 + 实时状态落到 SQLite：
//   - line_day_record: 按 (line_no, time) 复合主键幂等 upsert
//   - status_record:   设备实时状态上报（推断为 type=3 客户端在线心跳）
//
// 设计要点：
//   1. 幂等性：line_day_record 用 (line_no, time) ON CONFLICT DO UPDATE，
//      同一条线同一分钟的多次推送只产生一行（最末次数据覆盖）。
//   2. 写入行数：返回 *新增* 行数（不计 ON CONFLICT 触发的 UPDATE）。
//      用 last_insert_rowid() 跨调用对比：INSERT 后 rowid 变化，UPDATE 不变。
//   3. 时间格式：DB 存 TEXT 'yyyy-MM-dd HH:mm:ss'，与应用层保持一致。
//   4. ADO.NET 直接写：不引 ORM，保持 SQL 透明、便于后续做 EXPLAIN。
//   5. POJO 接口：仓储层用自有 Row 类型，不依赖 EdgeHost DTO（避免循环依赖）。
// =====================================================================

/// <summary>
/// line_day_record 入参 POJO（与 EdgeHost.Models.LineDayRecordDto 解耦）。
/// </summary>
public record class LineDayRecordInput(
    int TotalNum,
    int NgNum,
    string StatisticTime,
    string LineNo,
    string FaceNo)
{
    /// <summary>right = total - ng，最小 0（避免 negative）。</summary>
    public int RightCount => Math.Max(0, TotalNum - NgNum);
    public int ErrorCount => Math.Max(0, NgNum);
}

/// <summary>
/// status_record 入参 POJO（与 EdgeHost DTO 解耦）。
/// </summary>
public record class StatusRecordInput(
    string Time,
    string LineNo,
    string FaceNo,
    string DeviceNo,
    string? DeviceName,
    int Status = 1,
    int Type = 3 /* 客户端 */);

/// <summary>
/// 产线当日汇总仓储接口（便于 Service 层注入 + 测试时 mock）。
/// </summary>
public interface ILineRecordRepository
{
    /// <summary>
    /// 写入或更新 line_day_record（按 (line_no, time) 幂等）。
    /// 返回新增的行数（0 = 命中已有行，仅 UPDATE；1 = 新插入）。
    /// </summary>
    int UpsertLineDayRecord(LineDayRecordInput today);

    /// <summary>
    /// 写入 status_record 一条设备状态。
    /// 返回新增的行数（0/1）。
    /// </summary>
    int InsertStatus(StatusRecordInput status);

    /// <summary>
    /// 一次事务里把 today + status 一起写入。
    /// 返回值 = 新增的行数（line_day_record + status_record 新增之和）。
    /// </summary>
    int UpsertLineAndStatus(LineDayRecordInput today, StatusRecordInput status);
}

/// <summary>
/// ADO.NET 实现的产线当日汇总仓储。
/// </summary>
public class LineRecordRepository : ILineRecordRepository
{
    private readonly SqliteConnectionFactory _factory;

    public LineRecordRepository(SqliteConnectionFactory factory)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
    }

    public int UpsertLineDayRecord(LineDayRecordInput today)
    {
        ValidateLineDayRecord(today);
        using var conn = _factory.Open();
        return UpsertLineDayRecordCore(conn, tx: null, today);
    }

    public int InsertStatus(StatusRecordInput status)
    {
        ValidateStatus(status);
        using var conn = _factory.Open();
        return InsertStatusCore(conn, tx: null, status);
    }

    public int UpsertLineAndStatus(LineDayRecordInput today, StatusRecordInput status)
    {
        ValidateLineDayRecord(today);
        ValidateStatus(status);
        using var conn = _factory.Open();
        using var tx = conn.BeginTransaction();
        try
        {
            var newRows = 0;
            newRows += UpsertLineDayRecordCore(conn, tx, today);
            newRows += InsertStatusCore(conn, tx, status);
            tx.Commit();
            return newRows;
        }
        catch
        {
            try { tx.Rollback(); } catch { /* ignore */ }
            throw;
        }
    }

    // ---- 入参校验 ----

    private static void ValidateLineDayRecord(LineDayRecordInput r)
    {
        if (r == null) throw new ArgumentNullException(nameof(r));
        if (string.IsNullOrWhiteSpace(r.LineNo)) throw new ArgumentException("lineNo 必填", nameof(r));
        if (string.IsNullOrWhiteSpace(r.FaceNo)) throw new ArgumentException("faceNo 必填", nameof(r));
        if (string.IsNullOrWhiteSpace(r.StatisticTime))
        {
            throw new ArgumentException("statisticTime 必填", nameof(r));
        }
        if (r.TotalNum < 0) throw new ArgumentException("totalNum 不能为负", nameof(r));
        if (r.NgNum < 0) throw new ArgumentException("ngNum 不能为负", nameof(r));
    }

    private static void ValidateStatus(StatusRecordInput r)
    {
        if (r == null) throw new ArgumentNullException(nameof(r));
        if (string.IsNullOrWhiteSpace(r.LineNo)) throw new ArgumentException("lineNo 必填", nameof(r));
        if (string.IsNullOrWhiteSpace(r.FaceNo)) throw new ArgumentException("faceNo 必填", nameof(r));
        if (string.IsNullOrWhiteSpace(r.DeviceNo)) throw new ArgumentException("deviceNo 必填", nameof(r));
        if (string.IsNullOrWhiteSpace(r.Time)) throw new ArgumentException("time 必填", nameof(r));
    }

    // ---- 核心 SQL ----

    private static int UpsertLineDayRecordCore(SqliteConnection conn, SqliteTransaction? tx, LineDayRecordInput today)
    {
        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

        // PSM SQLite schema（22 表迁移到位后）line_day_record 上没有 UNIQUE(line_no, time)，
        // 所以 ON CONFLICT (line_no, time) DO UPDATE 不可用。
        // 这里走"先 SELECT，再决定 INSERT/UPDATE"的显式 upsert 路径：
        //   命中已有行 → UPDATE（返回 0）
        //   未命中       → INSERT（返回 1）
        // 配合事务保证原子性。

        using (var probe = conn.CreateCommand())
        {
            if (tx != null) probe.Transaction = tx;
            probe.CommandText = "SELECT id FROM line_day_record WHERE line_no = $lineNo AND \"time\" = $time LIMIT 1;";
            probe.Parameters.AddWithValue("$lineNo", today.LineNo);
            probe.Parameters.AddWithValue("$time", today.StatisticTime);
            var existing = probe.ExecuteScalar();
            if (existing != null && existing != DBNull.Value)
            {
                // UPDATE
                using var upd = conn.CreateCommand();
                if (tx != null) upd.Transaction = tx;
                upd.CommandText = @"
UPDATE line_day_record
SET right_count = $right,
    error_count = $error,
    face_no     = $faceNo,
    update_time = $now
WHERE line_no = $lineNo AND ""time"" = $time;";
                upd.Parameters.AddWithValue("$right", today.RightCount);
                upd.Parameters.AddWithValue("$error", today.ErrorCount);
                upd.Parameters.AddWithValue("$faceNo", today.FaceNo);
                upd.Parameters.AddWithValue("$now", now);
                upd.Parameters.AddWithValue("$lineNo", today.LineNo);
                upd.Parameters.AddWithValue("$time", today.StatisticTime);
                upd.ExecuteNonQuery();
                return 0; // UPDATE 命中，不算新增
            }
        }

        // INSERT
        using (var ins = conn.CreateCommand())
        {
            if (tx != null) ins.Transaction = tx;
            ins.CommandText = @"
INSERT INTO line_day_record
    (right_count, error_count, line_no, time, update_time, create_time, face_no)
VALUES
    ($right, $error, $lineNo, $time, $now, $now, $faceNo);";
            ins.Parameters.AddWithValue("$right", today.RightCount);
            ins.Parameters.AddWithValue("$error", today.ErrorCount);
            ins.Parameters.AddWithValue("$lineNo", today.LineNo);
            ins.Parameters.AddWithValue("$time", today.StatisticTime);
            ins.Parameters.AddWithValue("$faceNo", today.FaceNo);
            ins.Parameters.AddWithValue("$now", now);
            ins.ExecuteNonQuery();
            return 1; // 新增
        }
    }

    private static int InsertStatusCore(SqliteConnection conn, SqliteTransaction? tx, StatusRecordInput status)
    {
        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        using var cmd = conn.CreateCommand();
        if (tx != null) cmd.Transaction = tx;
        cmd.CommandText = @"
INSERT INTO status_record
    (time, type, line_no, face_no, status, device_no, update_time, create_time, device_name)
VALUES
    ($time, $type, $lineNo, $faceNo, $status, $deviceNo, $now, $now, $deviceName);";
        cmd.Parameters.AddWithValue("$time", status.Time);
        cmd.Parameters.AddWithValue("$type", status.Type);
        cmd.Parameters.AddWithValue("$lineNo", status.LineNo);
        cmd.Parameters.AddWithValue("$faceNo", status.FaceNo);
        cmd.Parameters.AddWithValue("$status", status.Status);
        cmd.Parameters.AddWithValue("$deviceNo", status.DeviceNo);
        cmd.Parameters.AddWithValue("$deviceName", (object?)status.DeviceName ?? DBNull.Value);
        cmd.Parameters.AddWithValue("$now", now);
        return cmd.ExecuteNonQuery();
    }
}
