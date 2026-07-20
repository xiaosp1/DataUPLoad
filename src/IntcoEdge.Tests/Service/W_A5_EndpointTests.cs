using System.Net;
using System.Net.Http.Json;
using IntcoEdge.EdgeHost.Models;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// W-A5 新增端点的集成测试：用 WebApplicationFactory 启动内存中的 EdgeHost，
/// 不监听真实端口（不会与运行中的实例冲突），但走完整 ASP.NET Core 管道。
///
/// 端点覆盖：
///   - GET  /api/dict/defect-type
///   - GET  /api/dict/defect-group
///   - GET  /api/dict/face-group
///   - POST /api/defect/query (空集 + 400)
///   - GET  /api/line/statistic?lineNo=...
///
/// 历史端点 W-A3 不动：
///   - POST /client/yk/defect-record 仍然 OK
///   - POST /client/yk/defect-records 仍然 OK
/// </summary>
public class W_A5_EndpointTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public W_A5_EndpointTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    // =================================================================
    // 字典
    // =================================================================

    [Fact]
    public async Task Dict_DefectType_Returns200_WithDataArray()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/dict/defect-type");
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<List<DefectTypeDictDto>>>();
        Assert.NotNull(body);
        Assert.Equal(0, body!.code);
        Assert.NotNull(body.data);
    }

    [Fact]
    public async Task Dict_DefectGroup_Returns200_FourItems()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/dict/defect-group");
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<List<DefectGroupDictDto>>>();
        Assert.NotNull(body);
        Assert.Equal(0, body!.code);
        Assert.Equal(4, body.data!.Count);
    }

    [Fact]
    public async Task Dict_FaceGroup_Returns200_TwoItems()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/dict/face-group");
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<List<FaceGroupDictDto>>>();
        Assert.NotNull(body);
        Assert.Equal(0, body!.code);
        Assert.Equal(2, body.data!.Count);
        Assert.Equal("A", body.data[0].Code);
        Assert.Equal("B", body.data[1].Code);
    }

    // =================================================================
    // 缺陷查询
    // =================================================================

    [Fact]
    public async Task DefectQuery_NullBody_Returns400()
    {
        using var client = _factory.CreateClient();
        var resp = await client.PostAsJsonAsync("/api/defect/query", new { });
        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
    }

    [Fact]
    public async Task DefectQuery_MissingStartTime_Returns400()
    {
        using var client = _factory.CreateClient();
        var resp = await client.PostAsJsonAsync("/api/defect/query", new
        {
            endTime = "2026-07-20 23:59:59"
        });
        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
    }

    [Fact]
    public async Task DefectQuery_BadTimeFormat_Returns400()
    {
        using var client = _factory.CreateClient();
        var resp = await client.PostAsJsonAsync("/api/defect/query", new
        {
            startTime = "2026/07/20",
            endTime = "2026-07-20 23:59:59"
        });
        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
    }

    [Fact]
    public async Task DefectQuery_EmptyDb_Returns200_WithZeroTotal()
    {
        using var client = _factory.CreateClient();
        // 用一个独特的 lineNo 过滤，避免跟生产 DB 的真实数据冲突
        // (PM 20:35 bug fix 后 defect_record 会被写入)
        var uniqueLine = $"W-A5-EMPTY-{Guid.NewGuid():N}";
        var resp = await client.PostAsJsonAsync("/api/defect/query", new
        {
            lineNo = uniqueLine,
            startTime = "2026-07-20 00:00:00",
            endTime = "2026-07-20 23:59:59"
        });

        // 独特 lineNo 下应该 0 行，controller 不应 5xx
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<DefectQueryResponse>>();
        Assert.NotNull(body);
        Assert.Equal(0, body!.code);
        Assert.Equal(0, body.data!.Total);
        Assert.Empty(body.data.Rows);
    }

    // =================================================================
    // 产线统计
    // =================================================================

    [Fact]
    public async Task LineStatistic_MissingLineNo_Returns400()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/line/statistic");
        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
    }

    [Fact]
    public async Task LineStatistic_UnknownLine_Returns200_WithZeros()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/line/statistic?lineNo=L99");
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<LineStatisticResponse>>();
        Assert.NotNull(body);
        Assert.Equal(0, body!.code);
        Assert.Equal("L99", body.data!.LineNo);
        Assert.Equal(0, body.data.Total);
        Assert.Equal(0d, body.data.NgRate);
    }

    [Fact]
    public async Task LineStatistic_TodayField_IsYyyyMmDd()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/line/statistic?lineNo=L01");
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<ApiResponse<LineStatisticResponse>>();
        var today = body!.data!.Today;
        Assert.Matches(@"^\d{4}-\d{2}-\d{2}$", today);
    }

    // =================================================================
    // W-A3 历史端点回归（W-A5 不破坏签名）
    // =================================================================

    [Fact]
    public async Task Legacy_DefectRecord_StillResponds()
    {
        using var client = _factory.CreateClient();
        var resp = await client.PostAsJsonAsync("/client/yk/defect-record", new
        {
            lineNo = "L01", faceNo = "A", gloveNo = "G1", result = 2,
            defectType = "黑点", imgList = "[]", time = "2026-07-20 14:00:00"
        });
        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
    }

    [Fact]
    public async Task Legacy_DefectRecords_EmptyList_Returns400()
    {
        using var client = _factory.CreateClient();
        var resp = await client.PostAsJsonAsync("/client/yk/defect-records", new List<object>());
        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
    }

    // =================================================================
    // 通用响应外壳
    // =================================================================

    private record class ApiResponse<T>(int code, T? data);
}
