using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using Xunit;

namespace IntcoEdge.Tests.Repository;

/// <summary>
/// DefectRecordRepository 仓储测试（W-A4）。
/// </summary>
public class DefectRecordRepositoryTests : IDisposable
{
    private readonly string _dbPath;

    public DefectRecordRepositoryTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"intco-defect-test-{Guid.NewGuid():N}.db");
        InitSchema(_dbPath);
    }

    public void Dispose()
    {
        try { File.Delete(_dbPath); } catch { /* ignore */ }
    }

    private static void InitSchema(string dbPath)
    {
        var connStr = $"Data Source={dbPath};Mode=ReadWriteCreate;Cache=Shared;Pooling=true;Foreign Keys=false";
        using var conn = new Microsoft.Data.Sqlite.SqliteConnection(connStr);
        conn.Open();
        using var ddl = conn.CreateCommand();
        ddl.CommandText = @"
CREATE TABLE defect_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    glove_no     TEXT    NOT NULL,
    result       INTEGER NOT NULL,
    defect_type  TEXT    NOT NULL,
    img_list     TEXT    NOT NULL,
    ""time""       TEXT    NOT NULL,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    except_flag  INTEGER NOT NULL DEFAULT 1
);";
        ddl.ExecuteNonQuery();
    }

    private SqliteConnectionFactory NewFactory()
        => new SqliteConnectionFactory(_dbPath, baseDirectory: Path.GetTempPath());

    private static DefectRecordInput MakeDefect(string gloveNo = "G-001", int result = 2, string type = "001")
        => new DefectRecordInput(
            LineNo: "L01",
            FaceNo: "A1",
            GloveNo: gloveNo,
            Result: result,
            DefectType: type,
            ImgList: "[\"img1.jpg\"]",
            Time: "2026-07-20 10:00:00",
            ExceptFlag: 1);

    [Fact]
    public void Insert_ReturnsPositiveId()
    {
        var repo = new DefectRecordRepository(NewFactory());

        var id = repo.Insert(MakeDefect());

        Assert.True(id > 0);
    }

    [Fact]
    public void GetById_AfterInsert_ReturnsRow()
    {
        var repo = new DefectRecordRepository(NewFactory());

        var id = repo.Insert(MakeDefect("G-xyz", result: 1, type: "002"));
        var row = repo.GetById(id);

        Assert.NotNull(row);
        Assert.Equal("L01", row!.LineNo);
        Assert.Equal("G-xyz", row.GloveNo);
        Assert.Equal(1, row.Result);
        Assert.Equal("002", row.DefectType);
        Assert.Equal(1, row.ExceptFlag);
    }

    [Fact]
    public void InsertBatch_AllSucceed_ReturnsCount()
    {
        var repo = new DefectRecordRepository(NewFactory());

        var batch = new[]
        {
            MakeDefect("G-1"),
            MakeDefect("G-2"),
            MakeDefect("G-3"),
        };

        var count = repo.InsertBatch(batch);

        Assert.Equal(3, count);
    }

    [Fact]
    public void InsertBatch_EmptyList_ReturnsZero()
    {
        var repo = new DefectRecordRepository(NewFactory());

        var count = repo.InsertBatch(Array.Empty<DefectRecordInput>());

        Assert.Equal(0, count);
    }

    [Fact]
    public void Insert_InvalidResult_Throws()
    {
        var repo = new DefectRecordRepository(NewFactory());
        var bad = MakeDefect() with { Result = 99 };
        Assert.Throws<ArgumentException>(() => repo.Insert(bad));
    }

    [Fact]
    public void Insert_DuplicateGloveNo_AllowedNotUniqueIndex()
    {
        var repo = new DefectRecordRepository(NewFactory());

        // 没有唯一索引 → 允许重复（PSM 端不主动去重，保留原始事件流）
        repo.Insert(MakeDefect("G-same"));
        repo.Insert(MakeDefect("G-same"));

        // 两行都进库
        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT COUNT(*) FROM defect_record WHERE glove_no = 'G-same'";
        Assert.Equal(2L, Convert.ToInt64(cmd.ExecuteScalar()));
    }

    [Fact]
    public void Insert_DefaultExceptFlag_IsOne()
    {
        var repo = new DefectRecordRepository(NewFactory());

        var id = repo.Insert(MakeDefect());
        var row = repo.GetById(id);

        Assert.Equal(1, row!.ExceptFlag);
    }
}
