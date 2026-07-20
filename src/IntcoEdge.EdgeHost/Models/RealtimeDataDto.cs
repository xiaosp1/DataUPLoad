using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 实时检测数据 DTO（PSM `DetectDataUploadDTO.realTimeData` 的嵌套对象）。
/// 反编译来源：`com.hikrobotics.solution.module.line.dto.RealTimeDetectData`。
/// ⚠️ `efficiency` / `totalNgRate` / `occupancyRate` 在 Java 端是 Double，**不要**误用整数。
/// </summary>
public record class RealtimeDataDto
{
    /// <summary>累计检测总数。@NotNull @Min(0)。</summary>
    [JsonPropertyName("total")]
    public int? Total { get; init; }

    /// <summary>累计不良数（NG）。@NotNull @Min(0)。</summary>
    [JsonPropertyName("ngCount")]
    public int? NgCount { get; init; }

    /// <summary>累计剔除总数。@NotNull @Min(0)。</summary>
    [JsonPropertyName("removeTotal")]
    public int? RemoveTotal { get; init; }

    /// <summary>累计剔除失败数。@NotNull @Min(0)。</summary>
    [JsonPropertyName("removeFail")]
    public int? RemoveFail { get; init; }

    /// <summary>综合良率/效率（百分比，0~100）。@NotNull @Min(0) Double。</summary>
    [JsonPropertyName("efficiency")]
    public double? Efficiency { get; init; }

    /// <summary>总不良率（百分比）。@NotNull @Min(0) Double。</summary>
    [JsonPropertyName("totalNgRate")]
    public double? TotalNgRate { get; init; }

    /// <summary>占位（占用次数）。@NotNull @Min(0)。</summary>
    [JsonPropertyName("occupancy")]
    public int? Occupancy { get; init; }

    /// <summary>占用率（百分比）。@NotNull @Min(0) Double。</summary>
    [JsonPropertyName("occupancyRate")]
    public double? OccupancyRate { get; init; }

    /// <summary>实时区间起始时间，格式 `yyyy-MM-dd HH:mm:ss`。@NotBlank。</summary>
    [JsonPropertyName("startTime")]
    public string? StartTime { get; init; }

    /// <summary>缺陷计数明细列表。可选。</summary>
    [JsonPropertyName("defects")]
    public List<DefectCountDto>? Defects { get; init; }
}
