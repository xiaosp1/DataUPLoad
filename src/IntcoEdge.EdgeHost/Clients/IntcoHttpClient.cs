using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 英科 EdgeHost 统一 HTTP 客户端。
/// 封装：
///   1. JSON 序列化（System.Text.Json，camelCase 兼容 PSM）
///   2. 超时控制（默认 5s，可配置）
///   3. 重试（指数退避，仅对 5xx / 网络错误重试）
///   4. 结构化日志
/// 不依赖 Polly：保持依赖最小化，方便 PM 直接审核。
/// </summary>
public class IntcoHttpClient
{
    /// <summary>默认 JSON 选项：忽略 null、忽略大小写、支持中文字符。</summary>
    public static readonly JsonSerializerOptions DefaultJsonOptions = new(JsonSerializerDefaults.Web)
    {
        DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull,
        WriteIndented = false,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
    };

    private readonly HttpClient _http;
    private readonly ILogger<IntcoHttpClient> _logger;
    private readonly int _retryCount;
    private readonly TimeSpan _retryBaseDelay = TimeSpan.FromMilliseconds(200);

    /// <summary>构造：注入 HttpClient（HttpClientFactory 管生命周期）。</summary>
    public IntcoHttpClient(HttpClient http, ILogger<IntcoHttpClient> logger, int retryCount = 2)
    {
        _http = http ?? throw new ArgumentNullException(nameof(http));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _retryCount = Math.Max(0, retryCount);
    }

    /// <summary>POST JSON 到指定 URL，返回强类型响应对象。</summary>
    /// <typeparam name="TRequest">请求体类型。</typeparam>
    /// <typeparam name="TResponse">响应体类型。</typeparam>
    /// <param name="url">完整 URL（不要用 base URL 拼接，调用方自己负责）。</param>
    /// <param name="payload">请求体（会被序列化成 JSON）。</param>
    /// <param name="ct">取消令牌。</param>
    public async Task<TResponse?> PostJsonAsync<TRequest, TResponse>(
        string url,
        TRequest payload,
        CancellationToken ct = default)
    {
        var (status, body) = await PostRawAsync(url, payload, ct).ConfigureAwait(false);
        if (string.IsNullOrEmpty(body))
        {
            return default;
        }
        try
        {
            return JsonSerializer.Deserialize<TResponse>(body, DefaultJsonOptions);
        }
        catch (JsonException ex)
        {
            _logger.LogError(ex, "PostJsonAsync 反序列化失败 url={Url} body={Body}", url, body);
            throw;
        }
    }

    /// <summary>POST JSON 到指定 URL，返回 (HTTP 状态码, 响应体字符串)。</summary>
    public async Task<(HttpStatusCode Status, string Body)> PostRawAsync<TRequest>(
        string url,
        TRequest payload,
        CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(url))
        {
            throw new ArgumentException("url 不能为空", nameof(url));
        }

        var json = JsonSerializer.Serialize(payload, DefaultJsonOptions);
        using var content = new StringContent(json, Encoding.UTF8);
        content.Headers.ContentType = new MediaTypeHeaderValue("application/json") { CharSet = "utf-8" };

        var attempt = 0;
        while (true)
        {
            attempt++;
            try
            {
                _logger.LogDebug("POST {Url} attempt={Attempt} bodyLen={Len}", url, attempt, json.Length);
                using var resp = await _http.PostAsync(url, content, ct).ConfigureAwait(false);
                var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);

                if (IsTransient(resp.StatusCode) && attempt <= _retryCount + 1)
                {
                    _logger.LogWarning("POST {Url} 状态 {Status}（瞬态），第 {Attempt} 次重试", url, (int)resp.StatusCode, attempt);
                    await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
                    continue;
                }

                _logger.LogInformation("POST {Url} -> {Status} bodyLen={BodyLen}", url, (int)resp.StatusCode, body.Length);
                return (resp.StatusCode, body);
            }
            catch (HttpRequestException ex) when (attempt <= _retryCount + 1)
            {
                _logger.LogWarning(ex, "POST {Url} 网络异常，第 {Attempt} 次重试", url, attempt);
                await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
            }
            catch (TaskCanceledException ex) when (!ct.IsCancellationRequested && attempt <= _retryCount + 1)
            {
                // Timeout 走 HttpClient.Timeout，超时也按瞬态处理
                _logger.LogWarning(ex, "POST {Url} 超时，第 {Attempt} 次重试", url, attempt);
                await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
            }
        }
    }

    /// <summary>GET 请求，返回响应体字符串。</summary>
    public async Task<(HttpStatusCode Status, string Body)> GetRawAsync(string url, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(url))
        {
            throw new ArgumentException("url 不能为空", nameof(url));
        }

        var attempt = 0;
        while (true)
        {
            attempt++;
            try
            {
                using var resp = await _http.GetAsync(url, ct).ConfigureAwait(false);
                var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
                if (IsTransient(resp.StatusCode) && attempt <= _retryCount + 1)
                {
                    _logger.LogWarning("GET {Url} 状态 {Status}（瞬态），第 {Attempt} 次重试", url, (int)resp.StatusCode, attempt);
                    await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
                    continue;
                }
                _logger.LogInformation("GET {Url} -> {Status} bodyLen={BodyLen}", url, (int)resp.StatusCode, body.Length);
                return (resp.StatusCode, body);
            }
            catch (HttpRequestException ex) when (attempt <= _retryCount + 1)
            {
                _logger.LogWarning(ex, "GET {Url} 网络异常，第 {Attempt} 次重试", url, attempt);
                await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
            }
            catch (TaskCanceledException ex) when (!ct.IsCancellationRequested && attempt <= _retryCount + 1)
            {
                _logger.LogWarning(ex, "GET {Url} 超时，第 {Attempt} 次重试", url, attempt);
                await DelayBeforeRetryAsync(attempt, ct).ConfigureAwait(false);
            }
        }
    }

    /// <summary>判断是否为瞬态错误（需要重试）。5xx + 408 + 429 都按瞬态处理。</summary>
    private static bool IsTransient(HttpStatusCode code)
    {
        var n = (int)code;
        return n >= 500 || code == HttpStatusCode.RequestTimeout || code == HttpStatusCode.TooManyRequests;
    }

    /// <summary>指数退避：200ms / 400ms / 800ms / ...</summary>
    private Task DelayBeforeRetryAsync(int attempt, CancellationToken ct)
    {
        var delay = TimeSpan.FromMilliseconds(_retryBaseDelay.TotalMilliseconds * Math.Pow(2, attempt - 1));
        return Task.Delay(delay, ct);
    }
}
