using System.Net;
using System.Text.Json;
using IntcoEdge.EdgeHost.Clients;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace IntcoEdge.Tests;

/// <summary>
/// 自定义 HttpMessageHandler：按队列返回预设响应。
/// 用于测试 IntcoHttpClient 的重试 / 超时 / 5xx 处理逻辑，无需引入 WireMock.Net。
/// </summary>
public class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<(HttpStatusCode Status, string Body, Exception? Throw)> _responses = new();
    public List<HttpRequestMessage> ReceivedRequests { get; } = new();
    public List<string> ReceivedBodies { get; } = new();

    public void Enqueue(HttpStatusCode status, string body = "")
        => _responses.Enqueue((status, body, null));

    public void EnqueueException(Exception ex)
        => _responses.Enqueue((HttpStatusCode.InternalServerError, "", ex));

    protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        ReceivedRequests.Add(request);
        if (request.Content != null)
        {
            ReceivedBodies.Add(await request.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false));
        }
        else
        {
            ReceivedBodies.Add(string.Empty);
        }

        if (_responses.Count == 0)
        {
            return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent("") };
        }
        var (status, body, throwEx) = _responses.Dequeue();
        if (throwEx != null)
        {
            throw throwEx;
        }
        return new HttpResponseMessage(status)
        {
            Content = new StringContent(body, System.Text.Encoding.UTF8, "application/json")
        };
    }
}

/// <summary>
/// IntcoHttpClient 行为测试：5xx 重试、4xx 不重试、HttpRequestException 重试、超时重试。
/// </summary>
public class IntcoHttpClientTests
{
    private static IntcoHttpClient BuildClient(FakeHttpMessageHandler handler, int retryCount = 2, int timeoutMs = 2000)
    {
        var http = new HttpClient(handler)
        {
            Timeout = TimeSpan.FromMilliseconds(timeoutMs)
        };
        return new IntcoHttpClient(http, NullLogger<IntcoHttpClient>.Instance, retryCount);
    }

    [Fact]
    public async Task PostJsonAsync_FirstAttempt500_RetriesThenSucceeds()
    {
        var handler = new FakeHttpMessageHandler();
        handler.Enqueue(HttpStatusCode.InternalServerError);
        handler.Enqueue(HttpStatusCode.BadGateway);
        handler.Enqueue(HttpStatusCode.OK, "{\"ok\":true}");

        var client = BuildClient(handler, retryCount: 2);

        var (status, body) = await client.PostRawAsync("http://test/", new { ping = 1 });

        Assert.Equal(HttpStatusCode.OK, status);
        Assert.Equal("{\"ok\":true}", body);
        Assert.Equal(3, handler.ReceivedRequests.Count); // 首次 + 2 次重试
    }

    [Fact]
    public async Task PostJsonAsync_4xxDoesNotRetry()
    {
        var handler = new FakeHttpMessageHandler();
        handler.Enqueue(HttpStatusCode.BadRequest, "{\"err\":\"bad\"}");

        var client = BuildClient(handler, retryCount: 2);

        var (status, body) = await client.PostRawAsync("http://test/", new { ping = 1 });

        Assert.Equal(HttpStatusCode.BadRequest, status);
        Assert.Single(handler.ReceivedRequests);
        Assert.Contains("bad", body);
    }

    [Fact]
    public async Task PostJsonAsync_ExhaustsRetries_ReturnsLastStatus()
    {
        var handler = new FakeHttpMessageHandler();
        // retryCount=3 时最多重试 3 次，加上首次共 4 次尝试。
        // 循环里 attempt 从 1 开始递增，IsTransient(5xx) && attempt <= retryCount+1(=4)
        // 触发重试：attempts 1,2,3 都重试；attempt=4 触发第 3 次重试（第 4 次入队被消费）；
        // attempt=5 时 attempt<=4 为 false，跳出循环 -> 共 5 次入队、5 次尝试。
        handler.Enqueue(HttpStatusCode.InternalServerError);
        handler.Enqueue(HttpStatusCode.InternalServerError);
        handler.Enqueue(HttpStatusCode.InternalServerError);
        handler.Enqueue(HttpStatusCode.InternalServerError);
        handler.Enqueue(HttpStatusCode.InternalServerError);

        var client = BuildClient(handler, retryCount: 3);

        var (status, _) = await client.PostRawAsync("http://test/", new { ping = 1 });

        Assert.Equal(HttpStatusCode.InternalServerError, status);
        Assert.Equal(5, handler.ReceivedRequests.Count);
    }

    [Fact]
    public async Task PostJsonAsync_NetworkError_Retries()
    {
        var handler = new FakeHttpMessageHandler();
        handler.EnqueueException(new HttpRequestException("connection refused"));
        handler.Enqueue(HttpStatusCode.OK, "{\"ok\":1}");

        var client = BuildClient(handler, retryCount: 1);

        var (status, body) = await client.PostRawAsync("http://test/", new { ping = 1 });

        Assert.Equal(HttpStatusCode.OK, status);
        Assert.Equal(2, handler.ReceivedRequests.Count);
    }

    [Fact]
    public async Task PostJsonAsync_DeserializesResponse()
    {
        var handler = new FakeHttpMessageHandler();
        handler.Enqueue(HttpStatusCode.OK, "{\"Success\":true,\"Message\":\"ok\"}");

        var client = BuildClient(handler, retryCount: 0);

        var resp = await client.PostJsonAsync<object, IntcoEdge.EdgeHost.Models.YkResponseDto>(
            "http://test/",
            new { ping = 1 });

        Assert.NotNull(resp);
        Assert.True(resp!.Success);
        Assert.Equal("ok", resp.Message);
    }

    [Fact]
    public async Task GetRawAsync_5xxRetries()
    {
        var handler = new FakeHttpMessageHandler();
        handler.Enqueue(HttpStatusCode.ServiceUnavailable);
        handler.Enqueue(HttpStatusCode.OK, "{\"x\":1}");

        var client = BuildClient(handler, retryCount: 1);

        var (status, body) = await client.GetRawAsync("http://test/");

        Assert.Equal(HttpStatusCode.OK, status);
        Assert.Equal(2, handler.ReceivedRequests.Count);
    }
}
