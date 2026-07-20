using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db.Repository;

// =====================================================================
// 单条缺陷记录仓储（W-A4）
//
// 负责把英科网关推来的 DefectRecordDto 落到 SQLite `defect_record` 表：
//   - 默认 except_flag=1（参与剔除）
//   - 不做幂等：英科网关可能重推，但 PSM 端不主动去重（保留原始事件流）
//   - 批量 INSERT 用事务；失败整体回滚
// =====================================================================

/// <summary>
/// defect_record 入参 POJO（与 EdgeHost DTO 解耦）。
/// 注：读取结果 DefectRecordRow 由 W-A5 DefectQueryRepository 定义，
/// 这里直接复用，避免重复声明。
/// </summary>
public record class DefectRecordInput(
    string LineNo,
    string FaceNo,
    string GloveNo,
    int Result,                // 1=良品 2=次品
    string DefectType,
    string ImgList,
    string Time,
    int ExceptFlag = 1);

/// <summary>
/// 缺陷记录仓储接口。
/// </summary>
public interface IDefectRecordRepository
{
    /// <summary>单条插入。返回新行的 id（rowid）。</summary>
    long Insert(DefectRecordInput defect);

    /// <summary>批量插入（同一事务）。返回实际写入的行数。</summary>
    int InsertBatch(IReadOnlyList<DefectRecordInput> defects);

    /// <summary>按 id 查询一行（用于冒烟测试）。</summary>
    DefectQueryRow? GetById(long id);
}

public class DefectRecordRepository : IDefectRecordRepository
{
    private readonly SqliteConnectionFactory _factory;

    public DefectRecordRepository(SqliteConnectionFactory factory)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
    }

    public long Insert(DefectRecordInput defect)
    {
        Validate(defect);
        using var conn = _factory.Open();
        return InsertCore(conn, tx: null, defect);
    }

    public int InsertBatch(IReadOnlyList<DefectRecordInput> defects)
    {
        if (defects == null) throw new ArgumentNullException(nameof(defects));
        if (defects.Count == 0) return 0;
        foreach (var d in defects) Validate(d);

        using var conn = _factory.Open();
        using var tx = conn.BeginTransaction();
        try
        {
            int count = 0;
            foreach (var d in defects)
            {
                var id = InsertCore(conn, tx, d);
                if (id > 0) count++;
            }
            tx.Commit();
            return count;
        }
        catch
        {
            try { tx.Rollback(); } catch { /* ignore */ }
            throw;
        }
    }

    public DefectQueryRow? GetById(long id)
    {
        using var conn = _factory.OpenReadOnly();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = @"
SELECT id, line_no, face_no, glove_no, result, defect_type, img_list, time, except_flag
FROM defect_record WHERE id = $id LIMIT 1;";
        cmd.Parameters.AddWithValue("$id", id);
        using var rd = cmd.ExecuteReader();
        if (!rd.Read()) return null;
        return new DefectQueryRow(
            Id: rd.GetInt64(0),
            LineNo: rd.GetString(1),
            FaceNo: rd.GetString(2),
            GloveNo: rd.GetString(3),
            Result: rd.GetInt32(4),
            DefectType: rd.GetString(5),
            ImgList: rd.GetString(6),
            Time: rd.GetString(7),
            ExceptFlag: rd.GetInt32(8));
    }

    private static void Validate(DefectRecordInput defect)
    {
        if (defect == null) throw new ArgumentNullException(nameof(defect));
        if (string.IsNullOrWhiteSpace(defect.LineNo)) throw new ArgumentException("lineNo 必填", nameof(defect));
        if (string.IsNullOrWhiteSpace(defect.FaceNo)) throw new ArgumentException("faceNo 必填", nameof(defect));
        if (string.IsNullOrWhiteSpace(defect.GloveNo)) throw new ArgumentException("gloveNo 必填", nameof(defect));
        if (defect.Result < 1 || defect.Result > 2) throw new ArgumentException("result 必须为 1/2", nameof(defect));
        if (string.IsNullOrWhiteSpace(defect.DefectType)) throw new ArgumentException("defectType 必填", nameof(defect));
        if (defect.ImgList is null) throw new ArgumentException("imgList 必填（可空字符串）", nameof(defect));
        if (string.IsNullOrWhiteSpace(defect.Time)) throw new ArgumentException("time 必填", nameof(defect));
    }

    private static long InsertCore(SqliteConnection conn, SqliteTransaction? tx, DefectRecordInput defect)
    {
        var now = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
        using var cmd = conn.CreateCommand();
        if (tx != null) cmd.Transaction = tx;
        cmd.CommandText = @"
INSERT INTO defect_record
    (line_no, face_no, glove_no, result, defect_type, img_list, time, update_time, create_time, except_flag)
VALUES
    ($lineNo, $faceNo, $gloveNo, $result, $defectType, $imgList, $time, $now, $now, $exceptFlag);
SELECT last_insert_rowid();";
        cmd.Parameters.AddWithValue("$lineNo", defect.LineNo);
        cmd.Parameters.AddWithValue("$faceNo", defect.FaceNo);
        cmd.Parameters.AddWithValue("$gloveNo", defect.GloveNo);
        cmd.Parameters.AddWithValue("$result", defect.Result);
        cmd.Parameters.AddWithValue("$defectType", defect.DefectType);
        cmd.Parameters.AddWithValue("$imgList", defect.ImgList);
        cmd.Parameters.AddWithValue("$time", defect.Time);
        cmd.Parameters.AddWithValue("$exceptFlag", defect.ExceptFlag);
        cmd.Parameters.AddWithValue("$now", now);
        var result = cmd.ExecuteScalar();
        return Convert.ToInt64(result ?? 0L);
    }
}
