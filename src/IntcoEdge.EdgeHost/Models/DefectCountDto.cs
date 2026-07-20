using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 缺陷计数条目（`TodayDetectDataDTO.defects` / `RealTimeDetectData.defects` 的元素）。
/// 反编译来源：`com.hikrobotics.solution.module.line.dto.DefectCountDTO`。
/// </summary>
public record class DefectCountDto
{
    /// <summary>缺陷数量。默认 0。</summary>
    [JsonPropertyName("count")]
    public int? Count { get; init; }

    /// <summary>发生时间，格式 `yyyy-MM-dd HH:mm:ss` 或区间标记。</summary>
    [JsonPropertyName("time")]
    public string? Time { get; init; }

    /// <summary>缺陷类型（与 PSM `defect_type` 表 `code` 对齐）。</summary>
    [JsonPropertyName("type")]
    public string? Type { get; init; }

    /// <summary>是否在大屏上显示（1=显示，0=隐藏）。可选。</summary>
    [JsonPropertyName("showFlag")]
    public int? ShowFlag { get; init; }
}
