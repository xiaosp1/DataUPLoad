using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关侧缺陷记录查询请求 DTO。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO`。
/// ⚠️ 重要：`lindGroup` 字段是 PSM/Java 端的拼写错误（line + d），**必须原样保留**，
/// 否则 PSM 反序列化找不到字段，会用 null 处理导致过滤失效。
/// </summary>
public record class SearchDefectRecordDto
{
    /// <summary>起始时间，格式 `yyyy-MM-dd HH:mm:ss`。@NotBlank。</summary>
    [JsonPropertyName("startTime")]
    public string? StartTime { get; init; }

    /// <summary>结束时间，格式 `yyyy-MM-dd HH:mm:ss`。可选。</summary>
    [JsonPropertyName("endTime")]
    public string? EndTime { get; init; }

    /// <summary>
    /// 产线编号过滤集合。
    /// ⚠️ PSM 拼写错误：原 Java 字段叫 `lindGroup`（非 lineGroup），JSON 字段名必须保留 typo。
    /// </summary>
    [JsonPropertyName("lindGroup")]
    public List<string>? LindGroup { get; init; }

    /// <summary>缺陷类型过滤集合。</summary>
    [JsonPropertyName("defectGroup")]
    public List<string>? DefectGroup { get; init; }

    /// <summary>面编号过滤集合。</summary>
    [JsonPropertyName("faceGroup")]
    public List<string>? FaceGroup { get; init; }
}
