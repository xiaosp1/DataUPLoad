using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// PSM `POST /client/data/detect` 请求体。
/// 反编译来源：`com.hikrobotics.solution.module.line.dto.DetectDataUploadDTO`。
/// 视觉软件定时把"当日统计 + 实时"打包推给 PSM。
/// </summary>
public record class DetectDataDto
{
    /// <summary>面编号（产线下分多个面）。@NotBlank，必填。</summary>
    [JsonPropertyName("faceNo")]
    public string? FaceNo { get; init; }

    /// <summary>产线编号。@NotBlank，必填。</summary>
    [JsonPropertyName("lineNo")]
    public string? LineNo { get; init; }

    /// <summary>当日统计数据（嵌套 DTO）。@NotNull，必填。</summary>
    [JsonPropertyName("todayData")]
    public LineDayRecordDto? TodayData { get; init; }

    /// <summary>实时数据（嵌套 DTO）。@NotNull，必填。</summary>
    [JsonPropertyName("realTimeData")]
    public RealtimeDataDto? RealTimeData { get; init; }
}
