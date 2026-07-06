using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using IntcoEdge.Common.Contracts;
using IntcoEdge.Common.Models;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace IntcoEdge.MesUpload.Clients;

/// <summary>
/// MES HTTP 客户端（Day1 骨架）：JWT 登录/缓存、Send 错误分类、批量逐条兜底。
/// </summary>
public sealed class HttpMesClient : IMesUploadClient, IDisposable
{
    private readonly IHttpClientFactory _httpFactory;
    private readonly MesServerOptions _server;
    private readonly UploadPolicy _policy;
    private readonly ILogger<HttpMesClient>? _logger;
    private readonly JsonSerializerOptions _jsonOpts = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull
    };
    private readonly SemaphoreSlim _loginLock = new(1, 1);
    private string? _token;
    private DateTimeOffset _tokenExpireAt = DateTimeOffset.MinValue;

    public HttpMesClient(
        IHttpClientFactory httpFactory,
        IOptions<MesServerOptions> serverOpts,
        IOptions<UploadPolicy> policy,
        ILogger<HttpMesClient>? logger = null)
    {
        _httpFactory = httpFactory ?? throw new ArgumentNullException(nameof(httpFactory));
        _server = serverOpts?.Value ?? throw new ArgumentNullException(nameof(serverOpts));
        _policy = policy?.Value ?? throw new ArgumentNullException(nameof(policy));
        _logger = logger;
    }

    public async Task<MesLoginResult> LoginAsync(CancellationToken ct)
    {
        if (_server.AuthType == MesAuthType.None) return new MesLoginResult { Success = true };

        await _loginLock.WaitAsync(ct).ConfigureAwait(false);
        try
        {
            if (!TokenNeedsRefresh) return MesLoginResult.OkJwt(_token!, _tokenExpireAt);

            var client = CreateClient();
            var url = $"{NormalizeBase(_server.BaseUrl)}{TrimSlash(_server.ApiPrefix)}/auth/login";
            var body = new { username = _server.UserName, password = _server.Password, appKey = _server.AppKey };
            using var content = new StringContent(JsonSerializer.Serialize(body, _jsonOpts), Encoding.UTF8, "application/json");
            using var resp = await client.PostAsync(url, content, ct).ConfigureAwait(false);
            var text = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);

            if (!resp.IsSuccessStatusCode)
            {
                _logger?.LogWarning("MES 登录失败: {Code} {Body}", resp.StatusCode, text);
                return MesLoginResult.Fail($"MES 登录失败: {resp.StatusCode} {text}");
            }

            using var doc = JsonDocument.Parse(text);
            var token = doc.RootElement.TryGetProperty("token", out var t) ? t.GetString() :
                        doc.RootElement.TryGetProperty("accessToken", out var at) ? at.GetString() : null;
            var expireSeconds = doc.RootElement.TryGetProperty("expiresIn", out var ei) && ei.TryGetInt32(out var s) ? s : 3600;
            if (string.IsNullOrWhiteSpace(token))
                return MesLoginResult.Fail("MES 登录返回缺少 token/accessToken 字段");

            _token = token;
            _tokenExpireAt = DateTimeOffset.UtcNow.AddSeconds(Math.Max(30, expireSeconds - 60));
            _logger?.LogInformation("MES 登录成功，token 到期 {ExpireAt}", _tokenExpireAt);
            return MesLoginResult.OkJwt(_token, _tokenExpireAt);
        }
        finally { _loginLock.Release(); }
    }

    public async Task<UploadResult> SendAsync(MesEvent evt, CancellationToken ct)
    {
        if (evt is null) throw new ArgumentNullException(nameof(evt));

        if (_server.AuthType != MesAuthType.None && TokenNeedsRefresh)
        {
            var login = await LoginAsync(ct).ConfigureAwait(false);
            if (!login.Success)
                return UploadResult.Fail(evt.EventId, HttpStatusCode.Unauthorized, login.Error ?? "登录失败", retryable: true);
        }

        using var req = BuildRequest(evt);
        var client = CreateClient();
        HttpResponseMessage resp;
        try { resp = await client.SendAsync(req, ct).ConfigureAwait(false); }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
        catch (Exception ex)
        {
            _logger?.LogWarning(ex, "MES 发送网络异常: {EventId}", evt.EventId);
            return UploadResult.Fail(evt.EventId, (HttpStatusCode)0, "网络异常: " + ex.Message, retryable: true);
        }

        using (resp)
        {
            var text = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            var traceId = resp.Headers.TryGetValues("X-Trace-Id", out var vs) ? string.Join(",", vs) : null;

            if (resp.StatusCode == HttpStatusCode.Unauthorized)
            {
                _token = null;
                _tokenExpireAt = DateTimeOffset.MinValue;
                return UploadResult.Fail(evt.EventId, resp.StatusCode, "未授权，将重登后重试", retryable: true);
            }
            if (resp.StatusCode == HttpStatusCode.RequestTimeout ||
                (int)resp.StatusCode == 429 || (int)resp.StatusCode >= 500)
            {
                TimeSpan? retryAfter = null;
                if (resp.Headers.RetryAfter?.Delta is TimeSpan d) retryAfter = d;
                return UploadResult.Fail(evt.EventId, resp.StatusCode, text, retryable: true, retryAfter);
            }
            if ((int)resp.StatusCode >= 400 && (int)resp.StatusCode < 500)
                return UploadResult.Fail(evt.EventId, resp.StatusCode, text, retryable: false);

            bool duplicate = false;
            if (!string.IsNullOrWhiteSpace(text))
            {
                try
                {
                    using var doc = JsonDocument.Parse(text);
                    if (doc.RootElement.TryGetProperty("duplicate", out var d) && d.ValueKind == JsonValueKind.True)
                        duplicate = true;
                }
                catch { /* ignore */ }
            }
            if (duplicate) return UploadResult.Duplicated(evt.EventId, traceId ?? Guid.NewGuid().ToString("N"));
            return UploadResult.Ok(evt.EventId, traceId ?? Guid.NewGuid().ToString("N"), text);
        }
    }

    public async Task<IReadOnlyList<UploadResult>> SendBatchAsync(IEnumerable<MesEvent> evts, CancellationToken ct)
    {
        var list = evts == null ? throw new ArgumentNullException(nameof(evts)) : new List<MesEvent>(evts);
        var results = new List<UploadResult>(list.Count);
        foreach (var e in list)
        {
            ct.ThrowIfCancellationRequested();
            results.Add(await SendAsync(e, ct).ConfigureAwait(false));
        }
        return results;
    }

    private HttpRequestMessage BuildRequest(MesEvent evt)
    {
        var url = $"{NormalizeBase(_server.BaseUrl)}{TrimSlash(_server.ApiPrefix)}/messages/{evt.EventType}";
        var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Headers.Add("X-Event-Id", evt.EventId.ToString());
        if (_server.AuthType == MesAuthType.Jwt && !string.IsNullOrEmpty(_token))
            req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _token);
        var json = JsonSerializer.Serialize(evt, _jsonOpts);
        req.Content = new StringContent(json, Encoding.UTF8, "application/json");
        return req;
    }

    private HttpClient CreateClient()
    {
        var client = _httpFactory.CreateClient("IntcoEdge.MesUpload");
        client.Timeout = TimeSpan.FromMilliseconds(_policy.RequestTimeoutMs > 0 ? _policy.RequestTimeoutMs : _server.TimeoutMs);
        return client;
    }

    private bool TokenNeedsRefresh =>
        _server.AuthType == MesAuthType.Jwt &&
        (string.IsNullOrEmpty(_token) || _tokenExpireAt <= DateTimeOffset.UtcNow);

    private static string NormalizeBase(string baseUrl) =>
        string.IsNullOrWhiteSpace(baseUrl) ? "http://127.0.0.1/" :
        baseUrl.EndsWith('/') ? baseUrl : baseUrl + "/";

    private static string TrimSlash(string? s) => (s ?? string.Empty).TrimEnd('/');

    public void Dispose() { _loginLock.Dispose(); }
}
