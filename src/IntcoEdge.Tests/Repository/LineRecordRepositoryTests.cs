using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using Xunit;

namespace IntcoEdge.Tests.Repository;

/// <summary>
/// LineRecordRepository 仓储测试（W-A4）。
/// 用内存 SQLite（Data Source=:memory:）隔离每个测试，避免污染文件库。
///
/// ⚠️ 注意：SQLite 内存库 + Microsoft.Data.Sqlite 行为：
///   - OpenReadOnly() 需要文件存在；这里统一用 Open() 走读写模式。
///   - 多个 Open() 调用共享同一连接字符串（Data Source=:memory:），
///     但 Microsoft.Data.Sqlite 会为每个连接创建独立内存库（除非用 Shared Cache）。
///   - 我们的 SqliteConnectionFactory 默认 Cache=Shared，所以共享同一内存库。
/// </summary>
public class LineRecordRepositoryTests : IDisposable
{
    private readonly string _dbPath;

    public LineRecordRepositoryTests()
    {
        // 用临时文件 + Cache=Shared，行为最接近真实生产库。
        _dbPath = Path.Combine(Path.GetTempPath(), $"intco-test-{Guid.NewGuid():N}.db");
        InitSchema(_dbPath);
    }

    public void Dispose()
    {
        try { File.Delete(_dbPath); } catch { /* ignore */ }
    }

    private static void InitSchema(string dbPath)
    {
        // 偷懒：直接用 IntcoEdge.Db 跑 migrate
        // 这里手动写 line_day_record + status_record 的 DDL，避免依赖 migrate runner。
        var connStr = $"Data Source={dbPath};Mode=ReadWriteCreate;Cache=Shared;Pooling=true;Foreign Keys=false";
        using var conn = new Microsoft.Data.Sqlite.SqliteConnection(connStr);
        conn.Open();
        using (var pragma = conn.CreateCommand())
        {
            pragma.CommandText = "PRAGMA busy_timeout = 5000;";
            pragma.ExecuteNonQuery();
        }
        using var ddl = conn.CreateCommand();
        ddl.CommandText = @"
CREATE TABLE line_day_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    right_count  INTEGER NOT NULL DEFAULT 0,
    error_count  INTEGER NOT NULL DEFAULT 0,
    line_no      TEXT    NOT NULL,
    ""time""       TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    face_no      TEXT,
    remove_total INTEGER NOT NULL DEFAULT 0,
    upload_remove_total INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE status_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    ""time""       TEXT    NOT NULL,
    type         INTEGER NOT NULL,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    status       INTEGER NOT NULL DEFAULT 1,
    device_no    TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_name  TEXT,
    line_id      INTEGER
);
-- 注意：生产环境 line_day_record 没有 UNIQUE(line_no, time)，仓储走 SELECT-then-INSERT/UPDATE。
-- 测试 schema 故意保持一致（不加 UNIQUE INDEX），逼仓储走显式 upsert 路径。";
        ddl.ExecuteNonQuery();
    }

    private SqliteConnectionFactory NewFactory()
        => new SqliteConnectionFactory(_dbPath, baseDirectory: Path.GetTempPath());

    [Fact]
    public void UpsertLineDayRecord_FirstInsert_ReturnsOne()
    {
        var repo = new LineRecordRepository(NewFactory());

        var rows = repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 100,
            NgNum: 5,
            StatisticTime: "2026-07-20 10:00:00",
            LineNo: "L01",
            FaceNo: "A1"));

        Assert.Equal(1, rows);
    }

    [Fact]
    public void UpsertLineDayRecord_SameLineAndTime_UpdatesInsteadOfInsert()
    {
        var repo = new LineRecordRepository(NewFactory());

        repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 100, NgNum: 5, StatisticTime: "2026-07-20 10:00:00", LineNo: "L01", FaceNo: "A1"));

        // 第二次推送：相同 (line_no, time)，不同数据 → 应命中 UPDATE，返回 0
        var rows2 = repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 200, NgNum: 20, StatisticTime: "2026-07-20 10:00:00", LineNo: "L01", FaceNo: "A1"));

        Assert.Equal(0, rows2);
    }

    [Fact]
    public void UpsertLineDayRecord_DifferentTime_InsertsNewRow()
    {
        var repo = new LineRecordRepository(NewFactory());

        repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 100, NgNum: 5, StatisticTime: "2026-07-20 10:00:00", LineNo: "L01", FaceNo: "A1"));

        var rows2 = repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 150, NgNum: 8, StatisticTime: "2026-07-20 10:05:00", LineNo: "L01", FaceNo: "A1"));

        Assert.Equal(1, rows2);
    }

    [Fact]
    public void UpsertLineDayRecord_RightCount_CalculatedFromTotalMinusNg()
    {
        var repo = new LineRecordRepository(NewFactory());

        repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 100, NgNum: 12, StatisticTime: "2026-07-20 10:00:00", LineNo: "L01", FaceNo: "A1"));

        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT right_count, error_count FROM line_day_record WHERE line_no = 'L01'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal(88, rd.GetInt32(0)); // right = 100 - 12
        Assert.Equal(12, rd.GetInt32(1));
    }

    [Fact]
    public void UpsertLineDayRecord_NgGreaterThanTotal_ClampsRightToZero()
    {
        var repo = new LineRecordRepository(NewFactory());

        // 异常数据：ng > total → right 不能为负
        repo.UpsertLineDayRecord(new LineDayRecordInput(
            TotalNum: 10, NgNum: 99, StatisticTime: "2026-07-20 10:00:00", LineNo: "L01", FaceNo: "A1"));

        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT right_count FROM line_day_record WHERE line_no = 'L01'";
        var rightObj = cmd.ExecuteScalar();
        Assert.NotNull(rightObj);
        Assert.Equal(0, Convert.ToInt32(rightObj));
    }

    [Fact]
    public void InsertStatus_ReturnsOne()
    {
        var repo = new LineRecordRepository(NewFactory());

        var rows = repo.InsertStatus(new StatusRecordInput(
            Time: "2026-07-20 10:00:00",
            LineNo: "L01",
            FaceNo: "A1",
            DeviceNo: "L01-A1-client",
            DeviceName: "产线 L01 面 A1 客户端"));

        Assert.Equal(1, rows);
    }

    [Fact]
    public void UpsertLineAndStatus_BothInserted_ReturnsTwo()
    {
        var repo = new LineRecordRepository(NewFactory());

        var rows = repo.UpsertLineAndStatus(
            today: new LineDayRecordInput(100, 5, "2026-07-20 10:00:00", "L01", "A1"),
            status: new StatusRecordInput("2026-07-20 10:00:00", "L01", "A1", "L01-A1-client", "client"));

        Assert.Equal(2, rows);
    }

    [Fact]
    public void UpsertLineAndStatus_SecondCall_UpdatesLineInsertsStatus()
    {
        var repo = new LineRecordRepository(NewFactory());

        repo.UpsertLineAndStatus(
            new LineDayRecordInput(100, 5, "2026-07-20 10:00:00", "L01", "A1"),
            new StatusRecordInput("2026-07-20 10:00:00", "L01", "A1", "L01-A1-client", "client"));

        // 第二次：line_day_record 应被 UPDATE（返回 0），status_record 仍是新行（返回 1）
        var rows = repo.UpsertLineAndStatus(
            new LineDayRecordInput(200, 20, "2026-07-20 10:00:00", "L01", "A1"),
            new StatusRecordInput("2026-07-20 10:01:00", "L01", "A1", "L01-A1-client", "client"));

        Assert.Equal(1, rows); // 只 status 是新的
    }

    [Fact]
    public void UpsertLineDayRecord_EmptyLineNo_Throws()
    {
        var repo = new LineRecordRepository(NewFactory());
        Assert.Throws<ArgumentException>(() => repo.UpsertLineDayRecord(
            new LineDayRecordInput(1, 0, "2026-07-20 10:00:00", "", "A1")));
    }
}
