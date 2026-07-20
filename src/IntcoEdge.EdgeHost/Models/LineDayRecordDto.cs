using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 当日检测统计 DTO（PSM `DetectDataUploadDTO.todayData` 的嵌套对象）。
/// 反编译来源：`com.hikrobotics.solution.module.line.dto.TodayDetectDataDTO`。
/// ⚠️ `statisticTime` 是字符串格式 `yyyy-MM-dd HH:mm:ss`，PSM 端用 `HikDateUtil.transformTime()`
/// 转成 `LocalDateTime`，所以我们保持字符串形态原样上送。
/// </summary>
public record class LineDayRecordDto
{
    /// <summary>当日检测总数。@NotNull @Min(0)。</summary>
    [JsonPropertyName("totalNum")]
    public int? TotalNum { get; init; }

    /// <summary>当日不良数（NG）。@NotNull @Min(0)。</summary>
    [JsonPropertyName("ngNum")]
    public int? NgNum { get; init; }

    /// <summary>统计时间，格式 `yyyy-MM-dd HH:mm:ss`。@NotBlank。</summary>
    [JsonPropertyName("statisticTime")]
    public string? StatisticTime { get; init; }

    /// <summary>缺陷计数明细列表（按 type 分组的缺陷数）。可选。</summary>
    [JsonPropertyName("defects")]
    public List<DefectCountDto>? Defects { get; init; }
}
