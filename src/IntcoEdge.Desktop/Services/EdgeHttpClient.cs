using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace IntcoEdge.Desktop.Services;

/// <summary>
/// EdgeHost HTTP 客户端封装。
/// 测试工具 Tab 用它来打 EdgeHost REST API（本地端口 5288）。
/// </summary>
public class EdgeHttpClient
{
    private readonly HttpClient _http;
    private static readonly JsonSerializerOptions _json = new()
    {
        PropertyNamingPolicy = null,             // PSM 字段是小驼峰，我们这边手写
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
    };

    public string BaseUrl { get; set; } = "http://127.0.0.1:5288";
    public int TimeoutSeconds { get; set; } = 10;

    public EdgeHttpClient()
    {
        _http = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(10),
        };
    }

    public void UpdateBaseUrl(string url)
    {
        BaseUrl = url?.TrimEnd('/') ?? "http://127.0.0.1:5288";
    }

    /// <summary>
    /// GET 请求，返回响应原文 + 状态码。
    /// </summary>
    public async Task<(int status, string body)> GetAsync(string path, CancellationToken ct = default)
    {
        var url = $"{BaseUrl}{path}";
        try
        {
            using var resp = await _http.GetAsync(url, ct);
            var body = await resp.Content.ReadAsStringAsync(ct);
            return ((int)resp.StatusCode, body);
        }
        catch (Exception ex)
        {
            return (0, $"❌ GET {url} 失败：{ex.Message}");
        }
    }

    /// <summary>
    /// POST JSON 请求。
    /// </summary>
    public async Task<(int status, string body)> PostJsonAsync(string path, object payload, CancellationToken ct = default)
    {
        var url = $"{BaseUrl}{path}";
        try
        {
            var json = JsonSerializer.Serialize(payload, _json);
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            using var resp = await _http.PostAsync(url, content, ct);
            var body = await resp.Content.ReadAsStringAsync(ct);
            return ((int)resp.StatusCode, body);
        }
        catch (Exception ex)
        {
            return (0, $"❌ POST {url} 失败：{ex.Message}");
        }
    }

    /// <summary>
    /// POST 原始 JSON 字符串（用于直接发测试人员手写的 JSON）。
    /// </summary>
    public async Task<(int status, string body)> PostRawJsonAsync(string path, string rawJson, CancellationToken ct = default)
    {
        var url = $"{BaseUrl}{path}";
        try
        {
            var content = new StringContent(rawJson ?? "{}", Encoding.UTF8, "application/json");
            using var resp = await _http.PostAsync(url, content, ct);
            var body = await resp.Content.ReadAsStringAsync(ct);
            return ((int)resp.StatusCode, body);
        }
        catch (Exception ex)
        {
            return (0, $"❌ POST {url} 失败：{ex.Message}");
        }
    }

    /// <summary>
    /// 测通：先打 /health（不强制要求存在），看端口活不活。
    /// </summary>
    public async Task<(bool ok, string msg)> PingAsync()
    {
        try
        {
            using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(3));
            using var resp = await _http.GetAsync(BaseUrl + "/", cts.Token);
            // 任何 HTTP 响应都算"端口通"
            return (true, $"端口可达（HTTP {(int)resp.StatusCode}）");
        }
        catch (Exception ex)
        {
            return (false, $"端口不可达：{ex.Message}");
        }
    }
}
