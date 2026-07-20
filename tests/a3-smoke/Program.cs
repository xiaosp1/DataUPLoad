using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

/// <summary>
/// IntcoEdge W-A3 冒烟测试：
/// 1. 编译后先启动 EdgeHost（dotnet run --project src/IntcoEdge.EdgeHost）
/// 2. 再运行本程序：POST /client/data/detect → 检测连接是否正常
/// </summary>
var baseUrl = args.Length > 0 ? args[0].TrimEnd('/') : "http://127.0.0.1:5288";
var url = $"{baseUrl}/client/data/detect";

Console.WriteLine($"=== A3 Smoke Test ===");
Console.WriteLine($"Target: POST {url}");
Console.WriteLine();

// 构造最小化请求体
var payload = new
{
    faceNo = "A1",
    lineNo = "L01",
    todayData = new
    {
        totalNum = 100,
        ngNum = 5,
        statisticTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
        defects = (object[]?)null
    },
    realTimeData = new
    {
        total = 50,
        ngCount = 3,
        removeTotal = 3,
        removeFail = 0,
        efficiency = 98.5,
        totalNgRate = 6.0,
        occupancy = 25,
        occupancyRate = 50.0,
        startTime = DateTime.Now.AddMinutes(-5).ToString("yyyy-MM-dd HH:mm:ss"),
        defects = (object[]?)null
    }
};

var json = JsonSerializer.Serialize(payload, new JsonSerializerOptions
{
    DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull,
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
    WriteIndented = false
});

Console.WriteLine("Request body:");
Console.WriteLine(JsonSerializer.Serialize(payload, new JsonSerializerOptions { WriteIndented = true }));
Console.WriteLine();

using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
using var content = new StringContent(json, Encoding.UTF8);
content.Headers.ContentType = new MediaTypeHeaderValue("application/json") { CharSet = "utf-8" };

try
{
    using var resp = await http.PostAsync(url, content);
    var body = await resp.Content.ReadAsStringAsync();

    Console.WriteLine($"HTTP Status: {(int)resp.StatusCode} {resp.StatusCode}");
    Console.WriteLine($"Response:");
    Console.WriteLine(body);
    Console.WriteLine();

    if (resp.IsSuccessStatusCode)
    {
        Console.WriteLine("✓ SMOKE TEST PASSED");
        return 0;
    }
    else
    {
        Console.WriteLine("✗ SMOKE TEST FAILED (non-2xx)");
        return 1;
    }
}
catch (HttpRequestException ex)
{
    Console.WriteLine($"✗ SMOKE TEST FAILED (connection error): {ex.Message}");
    Console.WriteLine();
    Console.WriteLine("Hint: Run EdgeHost first:");
    Console.WriteLine("  cd src/IntcoEdge.EdgeHost && dotnet run");
    return 2;
}
catch (TaskCanceledException)
{
    Console.WriteLine("✗ SMOKE TEST FAILED (timeout after 5s)");
    Console.WriteLine("Hint: Is EdgeHost running on 5288?");
    return 3;
}
