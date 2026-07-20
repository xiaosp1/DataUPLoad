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
    alarm_id     TEXT,
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
    defect_name  TEXT,
    send_status  TEXT    NOT NULL DEFAULT 'pending',
    yk_code      INTEGER NOT NULL DEFAULT 0,
    error_msg    TEXT
);
CREATE UNIQUE INDEX idx_alarm_record_uuid ON alarm_record(uuid);
CREATE UNIQUE INDEX idx_alarm_record_alarm_id ON alarm_record(alarm_id);";
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

    // =====================================================================
    // W-A7-R 新增方法测试
    //
    // DoD：
    //   - UpsertByAlarmId 同 alarmId 调 2 次只插 1 行
    //   - UpdateSendStatus 成功更新字段
    //
    // 测试 schema 在 InitSchema 里加了 alarm_id / send_status / yk_code / error_msg 列。
    // =====================================================================

    private static AlarmRecordInput MakeAlarmA7(
        string alarmId = "alarm-001",
        string message = "底面破损",
        string sendStatus = "pending",
        int ykCode = 0,
        string? errorMsg = null)
        => new AlarmRecordInput(
            Uuid: $"uuid-{alarmId}",
            Time: "2026-07-20 10:00:00",
            Type: 1,
            LineNo: "L01",
            FaceNo: "A1",
            Level: 2,
            Message: message,
            Solve: 2,
            Reason: 1,
            DefectName: "001",
            AlarmId: alarmId,
            SendStatus: sendStatus,
            YkCode: ykCode,
            ErrorMsg: errorMsg);

    [Fact]
    public void UpsertByAlarmId_FirstCall_ReturnsOne()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        var rows = repo.UpsertByAlarmId(MakeAlarmA7("alarm-first"));

        Assert.Equal(1, rows);

        // 落库字段核验：alarm_id / send_status / yk_code 都要对得上
        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT alarm_id, send_status, yk_code, message FROM alarm_record WHERE alarm_id = 'alarm-first'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal("alarm-first", rd.GetString(0));
        Assert.Equal("pending", rd.GetString(1));
        Assert.Equal(0, rd.GetInt32(2));
        Assert.Equal("底面破损", rd.GetString(3));
    }

    [Fact]
    public void UpsertByAlarmId_SameAlarmIdTwice_OnlyOneRowInserted()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        // 第一次：INSERT → 1
        var first = repo.UpsertByAlarmId(MakeAlarmA7("alarm-dup", message: "first"));
        Assert.Equal(1, first);

        // 第二次：UPDATE → 0（不算新增）
        var second = repo.UpsertByAlarmId(MakeAlarmA7("alarm-dup", message: "second-updated"));
        Assert.Equal(0, second);

        // 验证：表里只有 1 行，且 message 已被更新为 second-updated
        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT COUNT(*), MAX(message) FROM alarm_record WHERE alarm_id = 'alarm-dup'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal(1, rd.GetInt32(0));   // 只有 1 行
        Assert.Equal("second-updated", rd.GetString(1)); // UPDATE 命中
    }

    [Fact]
    public void UpsertByAlarmId_MissingAlarmId_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        var bad = MakeAlarmA7(alarmId: "");
        Assert.Throws<ArgumentException>(() => repo.UpsertByAlarmId(bad));
    }

    [Fact]
    public void UpsertByAlarmId_InvalidSendStatus_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        var bad = MakeAlarmA7(alarmId: "alarm-x", sendStatus: "bogus");
        Assert.Throws<ArgumentException>(() => repo.UpsertByAlarmId(bad));
    }

    [Fact]
    public void UpsertByAlarmId_InvalidType_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        var bad = MakeAlarmA7("alarm-bad") with { Type = 99 };
        Assert.Throws<ArgumentException>(() => repo.UpsertByAlarmId(bad));
    }

    [Fact]
    public void UpsertByAlarmId_SecondCall_UpdatesSendStatusAndYkCode()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        // 第一次：pending
        repo.UpsertByAlarmId(MakeAlarmA7("alarm-status", sendStatus: "pending", ykCode: 0));
        // 第二次：sent + 200
        var rows = repo.UpsertByAlarmId(MakeAlarmA7("alarm-status", sendStatus: "pushed", ykCode: 200));
        Assert.Equal(0, rows);

        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT send_status, yk_code, error_msg FROM alarm_record WHERE alarm_id = 'alarm-status'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal("pushed", rd.GetString(0));
        Assert.Equal(200, rd.GetInt32(1));
        Assert.True(rd.IsDBNull(2)); // 第二次没传 errorMsg
    }

    [Fact]
    public void UpdateSendStatus_ExistingAlarm_UpdatesAllFields()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        repo.UpsertByAlarmId(MakeAlarmA7("alarm-yksend", sendStatus: "pending"));

        var rows = repo.UpdateSendStatus("alarm-yksend", "failed", 500, "yk gateway 5xx");

        Assert.Equal(1, rows);

        // 字段回写核验
        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT send_status, yk_code, error_msg FROM alarm_record WHERE alarm_id = 'alarm-yksend'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal("failed", rd.GetString(0));
        Assert.Equal(500, rd.GetInt32(1));
        Assert.Equal("yk gateway 5xx", rd.GetString(2));
    }

    [Fact]
    public void UpdateSendStatus_NotExistingAlarm_ReturnsZero()
    {
        var repo = new AlarmRecordRepository(NewFactory());

        var rows = repo.UpdateSendStatus("alarm-ghost", "pushed", 200, null);

        Assert.Equal(0, rows);
    }

    [Fact]
    public void UpdateSendStatus_EmptyAlarmId_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        Assert.Throws<ArgumentException>(() => repo.UpdateSendStatus("", "pushed", 200, null));
    }

    [Fact]
    public void UpdateSendStatus_InvalidSendStatus_Throws()
    {
        var repo = new AlarmRecordRepository(NewFactory());
        Assert.Throws<ArgumentException>(() => repo.UpdateSendStatus("alarm-x", "weird", 200, null));
    }

    [Fact]
    public void UpdateSendStatus_AfterSent_CanResetToFailed()
    {
        // 模拟：报警推成功后，PM 重试流程发现下游还是失败，把状态改回 failed。
        var repo = new AlarmRecordRepository(NewFactory());
        repo.UpsertByAlarmId(MakeAlarmA7("alarm-flip", sendStatus: "pending"));
        repo.UpdateSendStatus("alarm-flip", "pushed", 200, null);

        var rows = repo.UpdateSendStatus("alarm-flip", "failed", 502, "upstream down");

        Assert.Equal(1, rows);

        using var conn = NewFactory().Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT send_status, yk_code, error_msg FROM alarm_record WHERE alarm_id = 'alarm-flip'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal("failed", rd.GetString(0));
        Assert.Equal(502, rd.GetInt32(1));
        Assert.Equal("upstream down", rd.GetString(2));
    }
}
