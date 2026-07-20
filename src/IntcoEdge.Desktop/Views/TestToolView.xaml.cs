using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using IntcoEdge.Desktop.Services;

namespace IntcoEdge.Desktop.Views;

/// <summary>
/// TestToolView.xaml 的交互逻辑。
///
/// 三个核心测试：
/// ① POST /client/data/detect    模拟视觉软件推检测数据
/// ② GET  /client/yk/line-defect 拉字典
/// ③ POST /client/yk/defect-record 拉缺陷记录
/// </summary>
public partial class TestToolView : UserControl
{
    private readonly EdgeHttpClient _http = new();
    private static readonly JsonSerializerOptions _json = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
    };

    public TestToolView()
    {
        InitializeComponent();
        Loaded += (_, _) =>
        {
            BuildDetectPayload();
            BuildDefectRecordPayload();
            FooterText.Text = "就绪。修改参数后点「POST 发送」测试 EdgeHost 接口。";
        };
    }

    // -------------------- 通用 --------------------

    private void OnApplyBaseUrl(object sender, RoutedEventArgs e)
    {
        var url = BaseUrlBox.Text?.Trim() ?? "http://127.0.0.1:5288";
        _http.UpdateBaseUrl(url);
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] Base URL 已更新：{_http.BaseUrl}");
        FooterText.Text = $"Base URL = {_http.BaseUrl}";
    }

    private async void OnPing(object sender, RoutedEventArgs e)
    {
        _http.UpdateBaseUrl(BaseUrlBox.Text);
        PingStatusText.Text = "测试中...";
        var (ok, msg) = await _http.PingAsync();
        PingStatusText.Text = ok ? "✅ 通" : "❌ 不通";
        PingStatusText.Foreground = ok ? System.Windows.Media.Brushes.Green : System.Windows.Media.Brushes.Red;
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] Ping {_http.BaseUrl} → {msg}");
    }

    private void OnClearResponse(object sender, RoutedEventArgs e)
    {
        ResponseBox.Clear();
        RespStatusText.Text = "—";
    }

    private void OnCopyResponse(object sender, RoutedEventArgs e)
    {
        try
        {
            if (!string.IsNullOrEmpty(ResponseBox.Text))
            {
                Clipboard.SetText(ResponseBox.Text);
                FooterText.Text = "响应已复制到剪贴板";
            }
        }
        catch (Exception ex)
        {
            FooterText.Text = $"复制失败：{ex.Message}";
        }
    }

    private void ShowResponse(int status, string body)
    {
        RespStatusText.Text = status == 0 ? "ERR" : status.ToString();
        RespStatusText.Foreground = status >= 200 && status < 300
            ? System.Windows.Media.Brushes.Green
            : (status == 0 ? System.Windows.Media.Brushes.Red : System.Windows.Media.Brushes.DarkOrange);
        ResponseBox.Text = body ?? "";
        FooterText.Text = $"完成 → HTTP {status}";
    }

    private void AppendResponse(string line)
    {
        ResponseBox.AppendText(line + Environment.NewLine);
        ResponseBox.ScrollToEnd();
    }

    // -------------------- ① POST /client/data/detect --------------------

    private void OnBuildDetectPayload(object? sender = null, RoutedEventArgs? e = null)
    {
        BuildDetectPayload();
    }

    private void BuildDetectPayload()
    {
        var payload = new
        {
            faceNo = (DetectFaceNo.Text ?? "").Trim(),
            lineNo = (DetectLineNo.Text ?? "").Trim(),
            todayData = new
            {
                detectCount = int.TryParse(DetectTodayCount.Text, out var dc) ? dc : 0,
                defectCount = int.TryParse(DetectTodayDefect.Text, out var dd) ? dd : 0,
            },
            realTimeData = new
            {
                defectName = (DetectRealtimeDefect.Text ?? "").Trim(),
                count = int.TryParse(DetectRealtimeCount.Text, out var rc) ? rc : 1,
                timestamp = (DetectTimestamp.Text ?? "").Trim(),
            },
        };
        DetectPayloadPreview.Text = JsonSerializer.Serialize(payload, _json);
    }

    private async void OnPostDetect(object sender, RoutedEventArgs e)
    {
        _http.UpdateBaseUrl(BaseUrlBox.Text);
        BuildDetectPayload();
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] POST {_http.BaseUrl}/client/data/detect → ...");
        var (status, body) = await _http.PostRawJsonAsync("/client/data/detect", DetectPayloadPreview.Text);
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] ← HTTP {status}");
        ShowResponse(status, body);
    }

    private void OnCheckDbResult(object sender, RoutedEventArgs e)
    {
        // 提示用户去 PG 客户端 Tab 查表
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] 💡 切到「PG 客户端」Tab，打开数据库查 today_count / defect_count 表验证入库结果。");
        FooterText.Text = "请切到「PG 客户端」Tab 验证入库结果";
    }

    // -------------------- ② GET /client/yk/line-defect --------------------

    private async void OnGetLineDefect(object sender, RoutedEventArgs e)
    {
        _http.UpdateBaseUrl(BaseUrlBox.Text);
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] GET {_http.BaseUrl}/client/yk/line-defect → ...");
        var (status, body) = await _http.GetAsync("/client/yk/line-defect");
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] ← HTTP {status}");
        ShowResponse(status, body);
    }

    // -------------------- ③ POST /client/yk/defect-record --------------------

    private void OnBuildDefectRecordPayload(object? sender = null, RoutedEventArgs? e = null)
    {
        BuildDefectRecordPayload();
    }

    private void BuildDefectRecordPayload()
    {
        var payload = new Dictionary<string, object>
        {
            ["time"] = (DrTime.Text ?? "").Trim(),
        };

        var lindGroup = ParseCsv(DrLindGroup.Text);
        if (lindGroup.Count > 0) payload["lindGroup"] = lindGroup;

        var defectGroup = ParseCsv(DrDefectGroup.Text);
        if (defectGroup.Count > 0) payload["defectGroup"] = defectGroup;

        var faceGroup = ParseCsv(DrFaceGroup.Text);
        if (faceGroup.Count > 0) payload["faceGroup"] = faceGroup;

        DrPayloadPreview.Text = JsonSerializer.Serialize(payload, _json);
    }

    private static List<string> ParseCsv(string? csv)
    {
        var result = new List<string>();
        if (string.IsNullOrWhiteSpace(csv)) return result;
        foreach (var part in csv.Split(','))
        {
            var trimmed = part.Trim();
            if (!string.IsNullOrEmpty(trimmed)) result.Add(trimmed);
        }
        return result;
    }

    private async void OnPostDefectRecord(object sender, RoutedEventArgs e)
    {
        _http.UpdateBaseUrl(BaseUrlBox.Text);
        BuildDefectRecordPayload();
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] POST {_http.BaseUrl}/client/yk/defect-record → ...");
        var (status, body) = await _http.PostRawJsonAsync("/client/yk/defect-record", DrPayloadPreview.Text);
        AppendResponse($"[{DateTime.Now:HH:mm:ss}] ← HTTP {status}");
        ShowResponse(status, body);
    }
}
