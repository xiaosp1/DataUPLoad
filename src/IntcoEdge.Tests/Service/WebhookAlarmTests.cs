using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Xunit;

namespace IntcoEdge.Tests.Service;

/// <summary>
/// W-A7-S Webhook 接入集成测试。
///
/// 测试策略：
///   - 自定义 WebApplicationFactory&lt;Program&gt; 子类，重写 ConfigureWebHost，
///     把 IAlarmService 换成 <see cref="FakeAlarmService"/>（纯内存，不打英科网关）。
///   - 不监听真实端口，DB 不需要（AlarmService.HandleAlarmAsync 已被 Fake 替换）。
///   - 其它服务（SqliteConnectionFactory / YingkeGatewayClient / IYingkeService 等）
///     不被调用，所以不需要替换。
///
/// 覆盖：
///   - /api/webhook/health             GET 200 + ok
///   - /api/webhook/alarm              完整入参 → 200 + 落到 FakeAlarmService
///   - /api/webhook/alarm              缺必填 / 时间格式错 / 级别越界 → 400
///   - /api/webhook/alarm              空 body → 400
///   - ToAlarmInputDto 转换：AlarmId 缺省时透传给 Service 留空；
///                            ImgList 计入 message；Type=1 webhook 默认；Solve=2 默认。
/// </summary>
public class WebhookAlarmTests : IClassFixture<WebhookAlarmTests.WebhookFactory>
{
    private readonly WebhookFactory _factory;

    public WebhookAlarmTests(WebhookFactory factory)
    {
        _factory = factory;
        _factory.Reset();  // 每次新测试都清空 Fake 调用记录
    }

    // =================================================================
    // /api/webhook/health
    // =================================================================

    [Fact]
    public async Task Health_Returns200_Ok()
    {
        using var client = _factory.CreateClient();
        var resp = await client.GetAsync("/api/webhook/health");

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal(0, body.GetProperty("code").GetInt32());
        Assert.Equal("ok", body.GetProperty("message").GetString());
    }

    // =================================================================
    // /api/webhook/alarm — 正常路径
    // =================================================================

    [Fact]
    public async Task PushAlarm_ValidPayload_Returns200_AndCallsService()
    {
        using var client = _factory.CreateClient();

        var payload = new
        {
            AlarmId = "alarm-abc-123",
            LineNo = "L01",
            FaceNo = "A1",
            DeviceNo = "CAM-007",
            GloveNo = "G001",
            DefectType = "黑点",
            Severity = "3",
            Time = "2026-07-20 14:55:00",
            ImgList = "[\"a.jpg\",\"b.jpg\",\"c.jpg\"]",
        };

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", payload);

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal(0, body.GetProperty("code").GetInt32());
        Assert.Equal("ok", body.GetProperty("message").GetString());

        // data 段是 AlarmHandleResult JSON 形式
        Assert.True(body.TryGetProperty("data", out var data));
        Assert.Equal("alarm-abc-123", data.GetProperty("alarmId").GetString());

        // Fake 收到的入参核验（走 AlarmInputDto overload）
        var arg = Assert.Single(_factory.Fake.InputCalls);
        Assert.Equal("alarm-abc-123", arg.AlarmId);          // AlarmId 透传
        Assert.Equal("alarm-abc-123", arg.Uuid);             // Uuid 同步透传（保幂等）
        Assert.Equal("L01", arg.LineNo);
        Assert.Equal("A1", arg.FaceNo);
        Assert.Equal(3, arg.Level);                          // Severity 字符串 → 3
        Assert.Equal(1, arg.Type);                           // webhook 默认 type=1
        Assert.Equal("2026-07-20 14:55:00", arg.Time);
        Assert.Equal("黑点", arg.DefectName);
        Assert.Equal(2, arg.Solve);                          // 默认未解决
        Assert.Contains("黑点", arg.Message);
        Assert.Contains("[device=CAM-007]", arg.Message);
        Assert.Contains("[glove=G001]", arg.Message);
        Assert.Contains("[imgs=3]", arg.Message);
    }

    [Fact]
    public async Task PushAlarm_MissingAlarmId_PassesNullToService()
    {
        using var client = _factory.CreateClient();

        var payload = new
        {
            LineNo = "L02",
            FaceNo = "B",
            DefectType = "破洞",
            Severity = 2,
            Time = "2026-07-20 10:00:00",
        };

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", payload);

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var arg = _factory.Fake.InputCalls.Single();
        // AlarmId 缺省 → webhook 透传 null，Service 负责生成 Guid
        Assert.Null(arg.AlarmId);
        Assert.Null(arg.Uuid);
        Assert.Equal("L02", arg.LineNo);
        Assert.Equal("破洞", arg.DefectName);
    }

    [Fact]
    public async Task PushAlarm_NumericSeverity_Works()
    {
        using var client = _factory.CreateClient();

        var payload = new
        {
            AlarmId = "alarm-num",
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "破损",
            Severity = 4,           // 数字而非字符串
            Time = "2026-07-20 10:00:00",
        };

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", payload);

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        Assert.Equal(4, _factory.Fake.InputCalls.Single().Level);
    }

    // =================================================================
    // /api/webhook/alarm — 校验失败 400
    // =================================================================

    [Fact]
    public async Task PushAlarm_EmptyBody_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new { });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        Assert.Empty(_factory.Fake.InputCalls);  // 服务层不该被调用
    }

    [Fact]
    public async Task PushAlarm_MissingLineNo_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            FaceNo = "A",
            DefectType = "黑点",
            Severity = "2",
            Time = "2026-07-20 10:00:00",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Contains("LineNo", body.GetProperty("message").GetString());
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_MissingFaceNo_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            LineNo = "L01",
            DefectType = "黑点",
            Severity = "2",
            Time = "2026-07-20 10:00:00",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_MissingDefectType_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            LineNo = "L01",
            FaceNo = "A",
            Severity = "2",
            Time = "2026-07-20 10:00:00",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_MissingSeverity_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "黑点",
            Time = "2026-07-20 10:00:00",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_MissingTime_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "黑点",
            Severity = "2",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_BadTimeFormat_Returns400()
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            AlarmId = "alarm-bad-time",
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "黑点",
            Severity = "2",
            Time = "2026/07/20 14:55",  // 格式错
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Contains("Time", body.GetProperty("message").GetString());
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Theory]
    [InlineData("0")]   // 越界
    [InlineData("5")]   // 越界
    [InlineData("abc")] // 非数字
    public async Task PushAlarm_InvalidSeverity_Returns400(string severity)
    {
        using var client = _factory.CreateClient();

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", new
        {
            LineNo = "L01",
            FaceNo = "A",
            DefectType = "黑点",
            Severity = severity,
            Time = "2026-07-20 10:00:00",
        });

        Assert.Equal(HttpStatusCode.BadRequest, resp.StatusCode);
        var body = await resp.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Contains("Severity", body.GetProperty("message").GetString());
        Assert.Empty(_factory.Fake.InputCalls);
    }

    [Fact]
    public async Task PushAlarm_OptionalFieldsAbsent_Still200()
    {
        using var client = _factory.CreateClient();

        // AlarmId / DeviceNo / GloveNo / ImgList 都不传
        var payload = new
        {
            LineNo = "L99",
            FaceNo = "Z",
            DefectType = "其他",
            Severity = "1",
            Time = "2026-07-20 11:11:11",
        };

        var resp = await client.PostAsJsonAsync("/api/webhook/alarm", payload);

        Assert.Equal(HttpStatusCode.OK, resp.StatusCode);
        var arg = _factory.Fake.InputCalls.Single();
        Assert.Null(arg.AlarmId);                            // 透传 null，Service 生成
        Assert.DoesNotContain("[device=", arg.Message);      // 可选字段没拼接
        Assert.DoesNotContain("[glove=", arg.Message);
        Assert.DoesNotContain("[imgs=", arg.Message);
        Assert.Equal("其他", arg.DefectName);
    }

    // =================================================================
    // WebhookFactory — 替换 IAlarmService 为 Fake
    // =================================================================

    public class WebhookFactory : WebApplicationFactory<Program>
    {
        // W-A7-M 兼容修复：原为 public FakeAlarmService Fake，但 FakeAlarmService 是 private 嵌套类，
        // C# 禁止 public 成员暴露 private 类型 (CS0053)。改为 internal 与 FakeAlarmService 一致即可。
        internal FakeAlarmService Fake { get; } = new();

        public WebhookFactory() { }

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.ConfigureServices(services =>
            {
                // 移除 Program.cs 里注册的 IAlarmService → AlarmService
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IAlarmService));
                if (descriptor != null)
                {
                    services.Remove(descriptor);
                }

                // 替换成 Fake（Singleton，整个测试进程共享调用记录）
                services.AddSingleton<IAlarmService>(Fake);
            });
        }

        public void Reset()
        {
            Fake.InputCalls.Clear();
            Fake.RecordCalls.Clear();
        }
    }

    /// <summary>
    /// 假的 AlarmService — 只记录入参，不打 YK / 不入库。
    /// 返 Always-OK 结果，模拟 "入库成功 + 已推送英科"。
    /// 实现两个 overload：
    ///   - HandleAlarmAsync(AlarmRecordDto) — W-A4 老路径，本测试不会触发，留空记录。
    ///   - HandleAlarmAsync(AlarmInputDto)  — W-A7-M 新路径，本测试实际走这条。
    /// </summary>
    public class FakeAlarmService : IAlarmService
    {
        public List<AlarmInputDto> InputCalls { get; } = new List<AlarmInputDto>();
        public List<AlarmRecordDto> RecordCalls { get; } = new List<AlarmRecordDto>();

        public Task<AlarmHandleResult> HandleAlarmAsync(AlarmRecordDto alarm, CancellationToken ct = default)
        {
            RecordCalls.Add(alarm);
            return Task.FromResult(new AlarmHandleResult
            {
                AlarmId = alarm.Uuid ?? string.Empty,
                SendStatus = "pushed",
                YkCode = 200,
                ErrorMsg = null,
                Persisted = true,
            });
        }

        public Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputDto input, CancellationToken ct = default)
        {
            InputCalls.Add(input);
            // 返回一个稳定的 AlarmHandleResult（用固定 alarmId 方便测试断言）
            var alarmId = string.IsNullOrWhiteSpace(input.AlarmId)
                ? "generated-alarm-id"
                : input.AlarmId!;
            return Task.FromResult(new AlarmHandleResult
            {
                AlarmId = alarmId,
                SendStatus = "pushed",
                YkCode = 200,
                ErrorMsg = null,
                Persisted = true,
            });
        }
    }
}
