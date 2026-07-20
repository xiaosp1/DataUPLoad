using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

// =====================================================================
// 产线当日统计 DTO（W-A5 / 3）
// 对接 B1 大屏 Dashboard 视图：顶部 4 个 KPI 数字 + Top5 缺陷柱图 +
// 24h 时间轴曲线。
// =====================================================================

/// <summary>
/// 缺陷 Top5 条目（dashboard.defectTypeTop5 数组元素）。
/// </summary>
public record class DefectTopDto
{
    [JsonPropertyName("type")]
    public string Type { get; init; } = string.Empty;

    [JsonPropertyName("count")]
    public int Count { get; init; }
}

/// <summary>
/// 产线当日时间轴上的一个点（dashboard.timeline 数组元素）。
/// 时间桶粒度为 1 小时，前端拿去做折线图。
/// </summary>
public record class LineTimelinePointDto
{
    /// <summary>桶起始时间，格式 `yyyy-MM-dd HH:00:00`。</summary>
    [JsonPropertyName("time")]
    public string Time { get; init; } = string.Empty;

    /// <summary>该小时内的总检测数。</summary>
    [JsonPropertyName("total")]
    public int Total { get; init; }

    /// <summary>该小时内的次品数。</summary>
    [JsonPropertyName("ng")]
    public int Ng { get; init; }
}

/// <summary>
/// 产线当日统计响应体（GET /api/line/statistic 返回的 data 字段）。
/// </summary>
public record class LineStatisticResponse
{
    /// <summary>产线编号（入参 lineNo 原样回传，便于前端校验）。</summary>
    [JsonPropertyName("lineNo")]
    public string LineNo { get; init; } = string.Empty;

    /// <summary>当日日期，格式 `yyyy-MM-dd`。</summary>
    [JsonPropertyName("today")]
    public string Today { get; init; } = string.Empty;

    /// <summary>当日累计检测总数（含良品 + 次品）。</summary>
    [JsonPropertyName("total")]
    public int Total { get; init; }

    /// <summary>当日良品数（result=1）。</summary>
    [JsonPropertyName("right")]
    public int Right { get; init; }

    /// <summary>当日次品数（result=2）。</summary>
    [JsonPropertyName("ng")]
    public int Ng { get; init; }

    /// <summary>不良率（0~1，比如 0.0123 = 1.23%）。total=0 时返回 0。</summary>
    [JsonPropertyName("ngRate")]
    public double NgRate { get; init; }

    /// <summary>当日缺陷类型 Top5（按缺陷数降序）。</summary>
    [JsonPropertyName("defectTypeTop5")]
    public List<DefectTopDto> DefectTypeTop5 { get; init; } = new();

    /// <summary>当日 24 小时时间轴（按小时聚合 total/ng）。</summary>
    [JsonPropertyName("timeline")]
    public List<LineTimelinePointDto> Timeline { get; init; } = new();
}
