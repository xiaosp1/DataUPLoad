using System.Net;
using System.Text.Json;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// 英科网关客户端行为测试：登录 / ticket 缓存 / 缺陷查询 / 报警推送。
/// </summary>
public class YingkeGatewayClientTests
{
    private static (YingkeGatewayClient Client, FakeHttpMessageHandler Handler) Build(
        string url = "http://yk-gw:10031/api/dataportal/invoke",
        int retryCount = 0,
        int timeoutMs = 5000)
    {
        var handler = new FakeHttpMessageHandler();
        var http = new HttpClient(handler) { Timeout = TimeSpan.FromMilliseconds(timeoutMs) };
        var inner = new IntcoHttpClient(http, NullLogger<IntcoHttpClient>.Instance, retryCount);
        var opts = Options.Create(new YingkeGatewayOptions { Url = url, ApiType = "inkey.edge.dataTrans" });
        var client = new YingkeGatewayClient(inner, opts, NullLogger<YingkeGatewayClient>.Instance);
        return (client, handler);
    }

    [Fact]
    public async Task LoginAsync_ReturnsLoginResponse()
    {
        var (client, handler) = Build();
        // PSM 端 Result 是 Object，Jackson 默认序列化内嵌对象（不是 JSON 字符串）
        var respJson = "{\"Success\":true,\"Message\":\"ok\",\"Result\":{\"UserId\":\"u-001\",\"UserCode\":\"admin\",\"UserName\":\"管理员\",\"InvOrg\":100}}";
        handler.Enqueue(HttpStatusCode.OK, respJson);

        var resp = await client.LoginAsync("WS01");

        Assert.NotNull(resp);
        Assert.Equal("u-001", resp!.UserId);
        Assert.Equal("admin", resp.UserCode);
        Assert.Equal(100, resp.InvOrg);

        // 验证请求体里 ApiType 是 inkey.user.login
        Assert.Contains("\"ApiType\":\"inkey.user.login\"", handler.ReceivedBodies[0]);
        Assert.Contains("\"WorkShopCode\":\"WS01\"", handler.ReceivedBodies[0]);
    }

    [Fact]
    public async Task GetTicketAsync_CachesAcrossCalls()
    {
        var (client, handler) = Build();
        var respJson = "{\"Success\":true,\"Message\":\"ok\",\"Result\":{\"UserId\":\"u-ticket-001\",\"UserCode\":\"admin\",\"InvOrg\":100}}";
        handler.Enqueue(HttpStatusCode.OK, respJson);

        var t1 = await client.GetTicketAsync("WS01");
        var t2 = await client.GetTicketAsync("WS01");

        Assert.Equal("u-ticket-001", t1);
        Assert.Equal("u-ticket-001", t2);
        // 仅登录一次
        Assert.Single(handler.ReceivedRequests);
    }

    [Fact]
    public async Task GetTicketAsync_DifferentWorkshops_TriggersSeparateLogins()
    {
        var (client, handler) = Build();
        handler.Enqueue(HttpStatusCode.OK, "{\"Success\":true,\"Result\":{\"UserId\":\"t-WS01\"}}");
        handler.Enqueue(HttpStatusCode.OK, "{\"Success\":true,\"Result\":{\"UserId\":\"t-WS02\"}}");

        var t1 = await client.GetTicketAsync("WS01");
        var t2 = await client.GetTicketAsync("WS02");

        Assert.Equal("t-WS01", t1);
        Assert.Equal("t-WS02", t2);
        Assert.Equal(2, handler.ReceivedRequests.Count);
    }

    [Fact]
    public async Task QueryDefectAsync_PreservesLindGroupTypo()
    {
        var (client, handler) = Build();
        handler.Enqueue(HttpStatusCode.OK,
            "{\"Success\":true,\"Message\":\"\",\"Result\":\"[]\"}");

        var query = new SearchDefectRecordDto
        {
            StartTime = "2026-07-20 00:00:00",
            EndTime = "2026-07-20 23:59:59",
            LindGroup = new List<string> { "L01" },
            DefectGroup = new List<string> { "001" },
            FaceGroup = new List<string> { "A1" }
        };

        var resp = await client.QueryDefectAsync(new YkDefectQueryRequest
        {
            Parameters = new List<SearchDefectRecordDto> { query }
        });

        Assert.NotNull(resp);
        Assert.True(resp!.Success);
        // 关键断言：lindGroup typo 必须保留
        Assert.Contains("\"lindGroup\":[\"L01\"]", handler.ReceivedBodies[0]);
    }

    [Fact]
    public async Task PushAlarmAsync_PostsPascalCaseFields()
    {
        var (client, handler) = Build();
        handler.Enqueue(HttpStatusCode.OK, "");

        var alarm = new AlarmPushDto
        {
            WorkShop = "WS01",
            Line = "L01",
            Face = "A1",
            AlarmTime = "2026-07-20 14:55:00",
            AlarmType = "defect",
            AlarmLevel = "严重",
            AlarmDetails = "底面破损",
            AlarmCount = 1
        };

        var status = await client.PushAlarmAsync(alarm);

        Assert.Equal(HttpStatusCode.OK, status);
        var body = handler.ReceivedBodies[0];
        Assert.Contains("\"WorkShop\":\"WS01\"", body);
        Assert.Contains("\"Line\":\"L01\"", body);
        Assert.Contains("\"AlarmDetails\":\"底面破损\"", body);
        Assert.Contains("\"AlarmCount\":1", body);
    }

    [Fact]
    public async Task LoginAsync_NullResponse_ReturnsNull()
    {
        var (client, handler) = Build();
        handler.Enqueue(HttpStatusCode.InternalServerError, "{}");

        var resp = await client.LoginAsync("WS01");

        Assert.Null(resp);
    }
}
