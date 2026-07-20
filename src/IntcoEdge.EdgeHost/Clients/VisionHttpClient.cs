using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 视觉软件 HTTP 客户端。
/// 设计定位：**不主动调**，但提供一个对应的 outbound POST 入口（用于测试 / 重放 / 调度）。
/// 现场场景下视觉软件是主动推数据到 EdgeHost，所以生产代码主要走 inbound Controllers。
/// </summary>
public class VisionHttpClient
{
    private readonly HttpClient _http;
    private readonly ILogger<VisionHttpClient> _logger;
    private readonly string _baseUrl;

    /// <summary>默认 JSON 选项（与 IntcoHttpClient 保持一致）。</summary>
    public static readonly JsonSerializerOptions JsonOptions = IntcoHttpClient.DefaultJsonOptions;

    public VisionHttpClient(HttpClient http, ILogger<VisionHttpClient> logger, string baseUrl = "http://127.0.0.1:5288")
    {
        _http = http ?? throw new ArgumentNullException(nameof(http));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _baseUrl = baseUrl?.TrimEnd('/') ?? throw new ArgumentNullException(nameof(baseUrl));
    }

    /// <summary>POST 检测数据到本机 EdgeHost（默认 `http://127.0.0.1:5288/client/data/detect`）。</summary>
    public async Task<(HttpStatusCode Status, string Body)> PushDetectAsync(
        DetectDataDto payload,
        CancellationToken ct = default)
    {
        return await PostJsonAsync("/client/data/detect", payload, ct).ConfigureAwait(false);
    }

    /// <summary>POST 报警到本机 EdgeHost（默认 `/client/data/alarm`）。</summary>
    public async Task<(HttpStatusCode Status, string Body)> PushAlarmAsync(
        AlarmRecordDto payload,
        CancellationToken ct = default)
    {
        return await PostJsonAsync("/client/data/alarm", payload, ct).ConfigureAwait(false);
    }

    /// <summary>POST 状态到本机 EdgeHost（默认 `/client/data/status`）。</summary>
    public async Task<(HttpStatusCode Status, string Body)> PushStatusAsync(
        object payload,
        CancellationToken ct = default)
    {
        return await PostJsonAsync("/client/data/status", payload, ct).ConfigureAwait(false);
    }

    private async Task<(HttpStatusCode Status, string Body)> PostJsonAsync(
        string path,
        object payload,
        CancellationToken ct)
    {
        var url = _baseUrl + path;
        var json = JsonSerializer.Serialize(payload, JsonOptions);
        using var content = new StringContent(json, Encoding.UTF8);
        content.Headers.ContentType = new MediaTypeHeaderValue("application/json") { CharSet = "utf-8" };

        try
        {
            using var resp = await _http.PostAsync(url, content, ct).ConfigureAwait(false);
            var body = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            _logger.LogInformation("VisionHttpClient POST {Url} -> {Status} bodyLen={Len}", url, (int)resp.StatusCode, body.Length);
            return (resp.StatusCode, body);
        }
        catch (HttpRequestException ex)
        {
            _logger.LogError(ex, "VisionHttpClient POST {Url} 网络异常", url);
            throw;
        }
    }
}
