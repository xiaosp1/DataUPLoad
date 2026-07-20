using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using Xunit;

namespace IntcoEdge.Tests.Repository;

/// <summary>
/// AlarmRecordRepository 仓储测试（W-A4）。
/// </summary>
public class AlarmRecordRepositoryTests : IDisposable
{
    private readonly string _dbPath;

    public AlarmRecordRepositoryTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"intco-alarm-test-{Guid.NewGuid():N}.db");
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
CREATE TABLE alarm_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid         TEXT    NOT NULL,
    ""time""       TEXT    NOT NULL,
    type         INTEGER NOT NULL,
    line_no      TEXT    NOT NULL,
    face_no      TEXT    NOT NULL,
    level        INTEGER NOT NULL,
    message      TEXT    NOT NULL,
    solve        INTEGER NOT NULL DEFAULT 2,
    reason       INTEGER,
    update_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time  TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    defect_name  TEXT
);
CREATE UNIQUE INDEX idx_alarm_record_uuid ON alarm_record(uuid);";
        ddl.ExecuteNonQuery();
    }

    private SqliteConnectionFactory NewFactory()
        => new SqliteConnectionFactory(_dbPath, baseDirectory: Path.GetTempPath());

    private static AlarmRecordInput MakeAlarm(string uuid = "uuid-001", string message = "底面破损")
        => new AlarmRecordInput(
            Uuid: uuid,
            Time: "2026-07-20 10:00:00",
            Type: 1,                          // 缺陷
            LineNo: "L01",
            FaceNo: "A1",
            Level: 2,                         // 警告
            Message: message,
            Solve: 2,                         // 未解决
            Reason: 1,
            DefectName: "001");

    [Fact]
    public void InsertOrIgnore_FirstCall_ReturnsOne()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        var rows = repo.InsertOrIgnore(MakeAlarm());

        Assert.Equal(1, rows);
    }

    [Fact]
    public void InsertOrIgnore_DuplicateUuid_ReturnsZero()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        repo.InsertOrIgnore(MakeAlarm("dup-uuid"));
        var rows = repo.InsertOrIgnore(MakeAlarm("dup-uuid", "different message"));

        Assert.Equal(0, rows);
    }

    [Fact]
    public void Exists_AfterInsert_True()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        repo.InsertOrIgnore(MakeAlarm("uuid-x"));
        Assert.True(repo.Exists("uuid-x"));
        Assert.False(repo.Exists("uuid-y"));
    }

    [Fact]
    public void InsertOrIgnore_EmptyUuid_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        Assert.Throws<ArgumentException>(() => repo.InsertOrIgnore(MakeAlarm("")));
    }

    [Fact]
    public void InsertOrIgnore_InvalidType_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        var bad = MakeAlarm("uuid-bad") with { Type = 5 };
        Assert.Throws<ArgumentException>(() => repo.InsertOrIgnore(bad));
    }

    [Fact]
    public void InsertOrIgnore_InvalidLevel_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        var bad = MakeAlarm("uuid-bad") with { Level = 99 };
        Assert.Throws<ArgumentException>(() => repo.InsertOrIgnore(bad));
    }

    [Fact]
    public void InsertOrIgnore_OptionalFieldsDefaultToDefaults()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        var minimal = new AlarmRecordInput(
            Uuid: "min-1",
            Time: "2026-07-20 10:00:00",
            Type: 1,
            LineNo: "L01",
            FaceNo: "A1",
            Level: 1,
            Message: "test");

        var rows = repo.InsertOrIgnore(minimal);

        Assert.Equal(1, rows);

        // 验证 reason/defect_name 落库为 NULL
        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT reason, defect_name, solve FROM alarm_record WHERE uuid = 'min-1'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.True(rd.IsDBNull(0)); // reason
        Assert.True(rd.IsDBNull(1)); // defect_name
        Assert.Equal(2, rd.GetInt32(2)); // solve 默认 2（未解决）
    }
}
