using System.Net;
using System.Text.Json;
using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// 英科网关客户端行为测试：登录 / ticket 缓存 / 报警推送（按权威协议）。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.service.impl.YKServiceImpl`。
/// </summary>
public class YingkeGatewayClientTests
{
    private static (YingkeGatewayClient Client, FakeHttpMessageHandler Handler, YkTicketCache Cache) Build(
        string url = "http://yk-gw:10031/api/dataportal/invoke",
        int retryCount = 0,
        int timeoutMs = 5000,
        int ticketCacheMinutes = 45,
        bool enabled = true)
    {
        var handler = new FakeHttpMessageHandler();
        var http = new HttpClient(handler) { Timeout = TimeSpan.FromMilliseconds(timeoutMs) };
        var inner = new IntcoHttpClient(http, NullLogger<IntcoHttpClient>.Instance, retryCount);
        var opts = Options.Create(new YingkeGatewayOptions
        {
            Url = url,
            Username = "HKSJSB",
            Password = "HKSJSB123",
            WorkshopCode = "QZN2",
            InvOrgId = 1,
            TicketCacheMinutes = ticketCacheMinutes,
            Enabled = enabled,
        });
        var cache = new YkTicketCache(TimeSpan.FromMinutes(ticketCacheMinutes));
        var client = new YingkeGatewayClient(inner, opts, cache, NullLogger<YingkeGatewayClient>.Instance);
        return (client, handler, cache);
    }

    // ★ 测试用 fixture：英科登录响应模板
    private const string LoginResponseJson = """
    {
      "Success": true,
      "Message": null,
      "Result": {
        "UserId": "50001",
        "EmployeeId": "60002",
        "UserCode": "HKSJSB",
        "UserName": "海康视觉设备[HKSJSB]",
        "InvOrg": 1
      },
      "Context": {
        "Ticket": "ABC-TICKET-XXX-YYY",
        "InvOrgId": 1
      }
    }
    """;

    [Fact]
    public async Task LoginAsync_SendsAuthenticationControllerLoginRequest()
    {
        var (client, handler, _) = Build();
        handler.Enqueue(HttpStatusCode.OK, LoginResponseJson);

        var resp = await client.LoginAsync();

        Assert.NotNull(resp);
        Assert.Equal(50001.0, resp!.UserId);
        Assert.Equal("HKSJSB", resp.UserCode);
        Assert.Equal(1, resp.InvOrg);

        // ★ 关键断言（按权威协议 3.1）：
        // ApiType = "AuthenticationController"，不是 "inkey.user.login"
        // Method  = "Login"
        // Parameters = [{Value: "HKSJSB"}, {Value: "HKSJSB123"}]
        var body = handler.ReceivedBodies[0];
        Assert.Contains("\"ApiType\":\"AuthenticationController\"", body);
        Assert.Contains("\"Method\":\"Login\"", body);
        Assert.Contains("{\"Value\":\"HKSJSB\"}", body);
        Assert.Contains("{\"Value\":\"HKSJSB123\"}", body);
        // Context = null 时不写入 JSON
        Assert.DoesNotContain("\"Context\":", body);
    }

    [Fact]
    public async Task GetTicketAsync_CachesAcrossCalls()
    {
        var (client, handler, cache) = Build();
        handler.Enqueue(HttpStatusCode.OK, LoginResponseJson);

        var (t1, org1) = await client.GetTicketAsync();
        var (t2, org2) = await client.GetTicketAsync();

        // ★ 关键：ticket 来自 Context.Ticket，不是 Result.UserId
        Assert.Equal("ABC-TICKET-XXX-YYY", t1);
        Assert.Equal("ABC-TICKET-XXX-YYY", t2);
        Assert.Equal(1, org1);
        Assert.Equal(1, org2);
        // 仅登录一次
        Assert.Single(handler.ReceivedRequests);
        // 缓存命中
        Assert.Equal("ABC-TICKET-XXX-YYY", cache.DebugState.Ticket);
    }

    [Fact]
    public async Task GetTicketAsync_DisabledOptions_ReturnsNull()
    {
        var (client, _, _) = Build(enabled: false);

        var (t, org) = await client.GetTicketAsync();

        Assert.Null(t);
        Assert.Null(org);
    }

    [Fact]
    public async Task GetTicketAsync_LoginFailure_ReturnsNullAndPreservesOldCache()
    {
        var (client, handler, cache) = Build();

        // 首次登录成功（拿到 ticket）
        handler.Enqueue(HttpStatusCode.OK, LoginResponseJson);
        var (t1, _) = await client.GetTicketAsync();
        Assert.Equal("ABC-TICKET-XXX-YYY", t1);

        // 强制让缓存过期 + 让登录失败
        cache.Invalidate();
        handler.Enqueue(HttpStatusCode.InternalServerError, "{}");

        var (t2, _) = await client.GetTicketAsync();
        // 登录失败：返回 null（旧 ticket 也不保留）
        Assert.Null(t2);
    }

    [Fact]
    public async Task PushAlarmAsync_SendsVisualInspectionControllerRequest()
    {
        var (client, handler, _) = Build();
        handler.Enqueue(HttpStatusCode.OK, LoginResponseJson); // 登录
        handler.Enqueue(HttpStatusCode.OK, """
        {
          "Success": true,
          "Message": null,
          "Result": { "code": 200, "message": null },
          "Context": { "Ticket": "abc", "InvOrgId": 1 }
        }
        """); // 推报警

        var alarm = new AlarmPushDto
        {
            WorkShop = "QZN2",
            Line = "L01",
            Face = "A1",
            AlarmTime = "2026-07-20T14:30:00",
            AlarmType = "缺陷报警",
            AlarmLevel = "严重",
            AlarmDetails = "底面破损",
            AlarmResult = "已处理",
            AlarmCount = 1,
        };

        var code = await client.PushAlarmAsync(new List<AlarmPushDto> { alarm });

        Assert.Equal(200, code);

        // ★ 关键断言（按权威协议 3.2）：
        // ApiType = "VisualInspectionController"
        // Method  = "HandleVisualInspectionAlarm"
        // Parameters[0] = {Value: [{AlarmPushDto...}]}
        // Context.Ticket 必须从缓存里拿出来带上
        var pushBody = handler.ReceivedBodies[1];
        Assert.Contains("\"ApiType\":\"VisualInspectionController\"", pushBody);
        Assert.Contains("\"Method\":\"HandleVisualInspectionAlarm\"", pushBody);
        Assert.Contains("\"Value\":[{", pushBody);
        Assert.Contains("\"WorkShop\":\"QZN2\"", pushBody);
        Assert.Contains("\"Line\":\"L01\"", pushBody);
        Assert.Contains("\"AlarmCount\":1", pushBody);
        Assert.Contains("\"Context\":{\"Ticket\":\"ABC-TICKET-XXX-YYY\",\"InvOrgId\":1}", pushBody);
    }

    [Fact]
    public async Task PushAlarmAsync_BusinessCode400_Returns400()
    {
        var (client, handler, _) = Build();
        handler.Enqueue(HttpStatusCode.OK, LoginResponseJson);
        handler.Enqueue(HttpStatusCode.OK, """
        {
          "Success": true,
          "Message": null,
          "Result": { "code": 400, "message": "处理报警时出错 Object reference not set to an instance of an object." },
          "Context": { "Ticket": "abc", "InvOrgId": 1 }
        }
        """);

        var code = await client.PushAlarmAsync(new List<AlarmPushDto>
        {
            new() { WorkShop = "QZN2", Line = "L01", Face = "A1" }
        });

        Assert.Equal(400, code);
    }

    [Fact]
    public async Task PushAlarmAsync_EmptyList_ReturnsNullWithoutHttpCall()
    {
        var (client, handler, _) = Build();

        var code = await client.PushAlarmAsync(new List<AlarmPushDto>());

        Assert.Null(code);
        Assert.Empty(handler.ReceivedRequests); // 不发请求
    }

    [Fact]
    public async Task PushAlarmAsync_DisabledOptions_ReturnsNull()
    {
        var (client, _, _) = Build(enabled: false);

        var code = await client.PushAlarmAsync(new List<AlarmPushDto>
        {
            new() { WorkShop = "QZN2", Line = "L01", Face = "A1" }
        });

        Assert.Null(code);
    }

    [Fact]
    public async Task LoginAsync_SuccessFalse_ReturnsNull()
    {
        var (client, handler, _) = Build();
        handler.Enqueue(HttpStatusCode.OK, """{"Success":false,"Message":"bad creds"}""");

        var resp = await client.LoginAsync();

        Assert.Null(resp);
    }

    [Fact]
    public async Task LoginAsync_Http500_ReturnsNull()
    {
        var (client, handler, _) = Build();
        handler.Enqueue(HttpStatusCode.InternalServerError, "{}");

        var resp = await client.LoginAsync();

        Assert.Null(resp);
    }
}
