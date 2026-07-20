using IntcoEdge.Common;
using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// LineRecordService 单元测试（W-A4）。
/// 用真实 SQLite 临时库验证：参数校验、DTO → POJO 映射、写入行数语义。
/// </summary>
public class LineRecordServiceTests : IDisposable
{
    private readonly string _dbPath;

    public LineRecordServiceTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"intco-svc-test-{Guid.NewGuid():N}.db");
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
CREATE TABLE line_day_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    right_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    line_no TEXT NOT NULL,
    ""time"" TEXT NOT NULL,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    face_no TEXT,
    remove_total INTEGER NOT NULL DEFAULT 0,
    upload_remove_total INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE status_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ""time"" TEXT NOT NULL,
    type INTEGER NOT NULL,
    line_no TEXT NOT NULL,
    face_no TEXT NOT NULL,
    status INTEGER NOT NULL DEFAULT 1,
    device_no TEXT NOT NULL,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_name TEXT,
    line_id INTEGER
);
-- 不加 UNIQUE INDEX，匹配生产 schema，逼仓储走显式 upsert。";
        ddl.ExecuteNonQuery();
    }

    private LineRecordService NewService()
    {
        var factory = new SqliteConnectionFactory(_dbPath, baseDirectory: Path.GetTempPath());
        var repo = new LineRecordRepository(factory);
        var defectRepo = new DefectRecordRepository(factory);
        var defectConv = new DefectConversion(NullLogger<DefectConversion>.Instance);
        return new LineRecordService(NullLogger<LineRecordService>.Instance, repo, defectRepo, defectConv);
    }

    [Fact]
    public async Task HandleDetectDataAsync_ValidPayload_WritesTwoRows()
    {
        var svc = NewService();

        var data = new DetectDataDto
        {
            LineNo = "L01",
            FaceNo = "A1",
            TodayData = new LineDayRecordDto
            {
                TotalNum = 100,
                NgNum = 5,
                StatisticTime = "2026-07-20 10:00:00",
            },
            RealTimeData = new RealtimeDataDto
            {
                Total = 50,
                NgCount = 3,
                StartTime = "2026-07-20 10:00:00",
            },
        };

        var rows = await svc.HandleDetectDataAsync(data);

        Assert.Equal(2, rows); // line_day_record + status_record 都新插
    }

    [Fact]
    public async Task HandleDetectDataAsync_SamePayloadTwice_LineIsUpdated()
    {
        var svc = NewService();

        var data = new DetectDataDto
        {
            LineNo = "L01",
            FaceNo = "A1",
            TodayData = new LineDayRecordDto
            {
                TotalNum = 100,
                NgNum = 5,
                StatisticTime = "2026-07-20 10:00:00",
            },
            RealTimeData = new RealtimeDataDto
            {
                Total = 50,
                NgCount = 3,
                StartTime = "2026-07-20 10:00:00",
            },
        };

        await svc.HandleDetectDataAsync(data);
        var rows2 = await svc.HandleDetectDataAsync(data);

        // 第二次：line_day_record 命中 UPDATE（0 新增），status_record 仍是新增（1）
        Assert.Equal(1, rows2);
    }

    [Fact]
    public async Task HandleDetectDataAsync_NullDto_Throws()
    {
        var svc = NewService();
        await Assert.ThrowsAsync<ArgumentNullException>(() => svc.HandleDetectDataAsync(null!));
    }

    [Fact]
    public async Task HandleDetectDataAsync_EmptyLineNo_Throws()
    {
        var svc = NewService();
        var data = new DetectDataDto { LineNo = "", FaceNo = "A1", TodayData = new(), RealTimeData = new() };
        await Assert.ThrowsAsync<ArgumentException>(() => svc.HandleDetectDataAsync(data));
    }

    [Fact]
    public async Task HandleDetectDataAsync_NullToday_Throws()
    {
        var svc = NewService();
        var data = new DetectDataDto { LineNo = "L01", FaceNo = "A1", TodayData = null, RealTimeData = new() };
        await Assert.ThrowsAsync<ArgumentException>(() => svc.HandleDetectDataAsync(data));
    }

    [Fact]
    public async Task HandleDetectDataAsync_EndToEnd_VerifySqliteRowCount()
    {
        // 冒烟测试风格：调一次 service，写完直接查库确认。
        var svc = NewService();
        var data = new DetectDataDto
        {
            LineNo = "L99",
            FaceNo = "B2",
            TodayData = new LineDayRecordDto
            {
                TotalNum = 200,
                NgNum = 8,
                StatisticTime = "2026-07-20 11:30:00",
            },
            RealTimeData = new RealtimeDataDto
            {
                Total = 100,
                NgCount = 4,
                StartTime = "2026-07-20 11:30:00",
            },
        };

        await svc.HandleDetectDataAsync(data);

        using var conn = new Microsoft.Data.Sqlite.SqliteConnection($"Data Source={_dbPath};Mode=ReadOnly;Cache=Shared");
        conn.Open();
        using var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT COUNT(*) FROM line_day_record WHERE line_no = 'L99'";
        var countObj = cmd.ExecuteScalar();
        Assert.Equal(1L, Convert.ToInt64(countObj));

        cmd.CommandText = "SELECT right_count, error_count, face_no FROM line_day_record WHERE line_no = 'L99'";
        using var rd = cmd.ExecuteReader();
        Assert.True(rd.Read());
        Assert.Equal(192, rd.GetInt32(0)); // 200 - 8
        Assert.Equal(8, rd.GetInt32(1));
        Assert.Equal("B2", rd.GetString(2));
    }
}
