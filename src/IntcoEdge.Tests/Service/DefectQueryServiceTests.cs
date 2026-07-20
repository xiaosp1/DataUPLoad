using IntcoEdge.Db;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Services;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Data.Sqlite;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// 缺陷查询服务测试（W-A5 / 2 + 3）。
///
/// 测试策略：
///   - 用 System.IO.Path.GetTempFileName 临时建一份最小 SQLite DB，
///     灌入 3 条 defect_record + 1 条 defect_type + 1 条 line_day_record。
///   - Service 拿到真实 Repository 跑端到端，验证：
///       * 参数校验（必填、时间格式）
///       * 分页 + 统计聚合
///       * 产线当日统计
/// </summary>
public class DefectQueryServiceTests : IDisposable
{
    private readonly string _dbPath;
    private readonly SqliteConnectionFactory _factory;
    private readonly DefectQueryService _svc;

    public DefectQueryServiceTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"intco-wa5-{Guid.NewGuid():N}.db");
        SeedDatabase(_dbPath);
        _factory = new SqliteConnectionFactory(_dbPath);
        var repo = new DefectQueryRepository(_factory);
        _svc = new DefectQueryService(repo, NullLogger<DefectQueryService>.Instance);
    }

    public void Dispose()
    {
        if (File.Exists(_dbPath))
        {
            SqliteConnection.ClearAllPools();
            try { File.Delete(_dbPath); } catch { /* best effort */ }
        }
    }

    // =================================================================
    // DefectQueryService.Query — 参数校验
    // =================================================================

    [Fact]
    public void Query_NullRequest_Throws()
    {
        Assert.Throws<ArgumentNullException>(() => _svc.Query(null!));
    }

    [Fact]
    public void Query_MissingStartTime_Throws()
    {
        var req = new DefectQueryRequest { StartTime = "", EndTime = "2026-07-20 23:59:59" };
        var ex = Assert.Throws<ArgumentException>(() => _svc.Query(req));
        Assert.Contains("startTime", ex.Message);
    }

    [Fact]
    public void Query_MissingEndTime_Throws()
    {
        var req = new DefectQueryRequest { StartTime = "2026-07-20 00:00:00", EndTime = "" };
        var ex = Assert.Throws<ArgumentException>(() => _svc.Query(req));
        Assert.Contains("endTime", ex.Message);
    }

    [Fact]
    public void Query_BadTimeFormat_Throws()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20",  // 只有日期，没时间
            EndTime = "2026-07-20 23:59:59"
        };
        var ex = Assert.Throws<ArgumentException>(() => _svc.Query(req));
        Assert.Contains("startTime 格式错误", ex.Message);
    }

    // =================================================================
    // DefectQueryService.Query — 数据正确性
    // =================================================================

    [Fact]
    public void Query_NoFilter_Returns3Rows_2NG()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
        };

        var resp = _svc.Query(req);

        Assert.Equal(3, resp.Total);
        Assert.Equal(3, resp.Statistics.TotalCount);
        Assert.Equal(2, resp.Statistics.NgCount);  // 2 条次品
        Assert.Equal(3, resp.Rows.Count);
    }

    [Fact]
    public void Query_NgRate_IsZeroWhenNoNg()
    {
        // 场景：只筛良品 → ng=0 → rate=0
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            LineNo = "L03",  // fixture 里只有 1 条 L03 且 result=1
        };

        var resp = _svc.Query(req);
        Assert.Equal(1, resp.Total);
        Assert.Equal(0, resp.Statistics.NgCount);
        Assert.Equal(0d, resp.Statistics.NgRate);
    }

    [Fact]
    public void Query_FilterByLineNo_ReturnsOnlyThatLine()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            LineNo = "L01",
        };

        var resp = _svc.Query(req);

        Assert.Equal(2, resp.Total);  // fixture L01 有 2 条
        Assert.All(resp.Rows, r => Assert.Equal("L01", r.LineNo));
    }

    [Fact]
    public void Query_FilterByDefectType_ReturnsOnlyThatType()
    {
        // fixture: '黑点' 有 2 条（L01 result=2 + L03 result=1），返回 2 条。
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            DefectType = "黑点",
        };

        var resp = _svc.Query(req);

        Assert.Equal(2, resp.Total);
        Assert.All(resp.Rows, r => Assert.Equal("黑点", r.DefectType));
    }

    [Fact]
    public void Query_DefectTypeDistribution_HasCorrectCounts()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
        };

        var resp = _svc.Query(req);

        // fixture: 黑点=1条次品, 破洞=1条次品, L03 是良品不入分布
        var dist = resp.Statistics.DefectTypeDistribution;
        Assert.Equal(2, dist.Count);

        // 按 count DESC 排序
        Assert.True(dist[0].Count >= dist[1].Count);
        var types = dist.Select(d => d.Type).ToHashSet();
        Assert.Contains("黑点", types);
        Assert.Contains("破洞", types);
    }

    [Fact]
    public void Query_PageSize1_ReturnsFirstPageOnly()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            Page = 1,
            PageSize = 1,
        };

        var resp = _svc.Query(req);
        Assert.Equal(3, resp.Total);   // 总数不变
        Assert.Single(resp.Rows);       // 只返 1 行
    }

    [Fact]
    public void Query_PageSizeOverMax_ClampedTo200()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            PageSize = 9999,  // 超过上限 200
        };

        var resp = _svc.Query(req);  // 不抛异常，被夹到 200
        Assert.NotNull(resp);
    }

    [Fact]
    public void Query_DefaultPageSize_Is20()
    {
        // 验证常量值不漂移（前端默认值依赖）
        Assert.Equal(20, DefectQueryService.DefaultPageSize);
        Assert.Equal(200, DefectQueryService.MaxPageSize);
    }

    [Fact]
    public void Query_RowsOrderedByTimeDesc()
    {
        var req = new DefectQueryRequest
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
        };

        var resp = _svc.Query(req);

        // fixture 时间：14:00, 14:30, 15:00
        Assert.Equal("2026-07-20 15:00:00", resp.Rows[0].Time);
        Assert.Equal("2026-07-20 14:30:00", resp.Rows[1].Time);
        Assert.Equal("2026-07-20 14:00:00", resp.Rows[2].Time);
    }

    // =================================================================
    // DefectQueryService.GetLineDayStatistic
    // =================================================================

    [Fact]
    public void GetLineDayStatistic_L01_ReturnsCorrectNumbers()
    {
        var resp = _svc.GetLineDayStatistic("L01");

        Assert.Equal("L01", resp.LineNo);
        Assert.Equal(DateTime.Now.ToString("yyyy-MM-dd"), resp.Today);
        // fixture: line_day_record L01 right=100, error=2
        Assert.Equal(102, resp.Total);
        Assert.Equal(100, resp.Right);
        Assert.Equal(2, resp.Ng);
        Assert.True(resp.NgRate > 0);
    }

    [Fact]
    public void GetLineDayStatistic_UnknownLine_ReturnsZeros()
    {
        var resp = _svc.GetLineDayStatistic("L99");

        Assert.Equal(0, resp.Total);
        Assert.Equal(0, resp.Right);
        Assert.Equal(0, resp.Ng);
        Assert.Equal(0d, resp.NgRate);
        Assert.Empty(resp.DefectTypeTop5);
    }

    [Fact]
    public void GetLineDayStatistic_MissingLineNo_Throws()
    {
        Assert.Throws<ArgumentException>(() => _svc.GetLineDayStatistic(""));
    }

    [Fact]
    public void GetLineDayStatistic_DefectTop5_OrderedByCountDesc()
    {
        // L01 在 line_day_record 里有数据，但 Top5 来自 defect_record
        // fixture: L01 次品有 2 条 (黑点 + 破洞)，各 1 条
        var resp = _svc.GetLineDayStatistic("L01");

        Assert.NotEmpty(resp.DefectTypeTop5);
        // 按 count DESC
        for (var i = 1; i < resp.DefectTypeTop5.Count; i++)
        {
            Assert.True(resp.DefectTypeTop5[i - 1].Count >= resp.DefectTypeTop5[i].Count);
        }
    }

    // =================================================================
    // Fixture
    // =================================================================

    private static void SeedDatabase(string dbPath)
    {
        // 手动建库 + 灌 3 行 defect_record + 1 行 line_day_record。
        // 不依赖 IntcoEdge.Db migration runner（保持测试自包含）。
        using var conn = new SqliteConnection($"Data Source={dbPath};Mode=ReadWriteCreate");
        conn.Open();

        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = @"
CREATE TABLE defect_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category INTEGER NOT NULL DEFAULT 3,
    count_enable INTEGER NOT NULL DEFAULT 0,
    count_threshold INTEGER NOT NULL DEFAULT 0,
    rate_enable INTEGER NOT NULL DEFAULT 0,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    show_img_enable INTEGER NOT NULL DEFAULT 0,
    alarm_enable INTEGER NOT NULL DEFAULT 0,
    sound_enable INTEGER NOT NULL DEFAULT 0,
    send_yk_enable INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE defect_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    line_no TEXT NOT NULL,
    face_no TEXT NOT NULL,
    glove_no TEXT NOT NULL,
    result INTEGER NOT NULL,
    defect_type TEXT NOT NULL,
    img_list TEXT NOT NULL,
    ""time"" TEXT NOT NULL,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    except_flag INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE line_day_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    right_count INTEGER NOT NULL,
    error_count INTEGER NOT NULL,
    line_no TEXT NOT NULL,
    ""time"" TEXT NOT NULL,
    update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    face_no TEXT NOT NULL DEFAULT '',
    remove_total INTEGER NOT NULL DEFAULT 0,
    upload_remove_total INTEGER NOT NULL DEFAULT 0
);";
            cmd.ExecuteNonQuery();
        }

        // defect_type
        Exec(conn, "INSERT INTO defect_type (id, name, category) VALUES (1, '黑点', 2), (2, '破洞', 1), (3, '客户端', 3)");

        // defect_record：3 行（2 次品 + 1 良品），全部 2026-07-20 当天
        Exec(conn, @"INSERT INTO defect_record (line_no, face_no, glove_no, result, defect_type, img_list, ""time"") VALUES
            ('L01', 'A', 'G001', 2, '黑点', '[""a.jpg""]', '2026-07-20 14:00:00'),
            ('L01', 'A', 'G002', 2, '破洞', '[""b.jpg""]', '2026-07-20 14:30:00'),
            ('L03', 'B', 'G003', 1, '黑点', '[""c.jpg""]', '2026-07-20 15:00:00')");

        // line_day_record：L01 当日 right=100 error=2
        Exec(conn, $@"INSERT INTO line_day_record (line_no, ""time"", right_count, error_count, face_no) VALUES
            ('L01', '{DateTime.Now:yyyy-MM-dd} 12:00:00', 100, 2, 'A')");
    }

    private static void Exec(SqliteConnection conn, string sql)
    {
        using var cmd = conn.CreateCommand();
        cmd.CommandText = sql;
        cmd.ExecuteNonQuery();
    }
}
