using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 报警记录 DTO（本地 SQLite `alarm_record` 表的内存形态 + 入站请求体）。
/// 反编译来源：`com.hikrobotics.solution.module.alarm.dto.AlarmDTO`（请求体）
/// + `com.hikrobotics.solution.module.alarm.model.AlarmRecordPO`（持久化对象）。
/// 视觉软件通过 `POST /client/data/alarm` 推送到 PSM，字段名直接对应 PO。
/// </summary>
public record class AlarmRecordDto
{
    /// <summary>报警唯一 ID（PSM 用它去重）。@NotEmpty。</summary>
    [JsonPropertyName("uuid")]
    public string? Uuid { get; init; }

    /// <summary>报警时间，格式 `yyyy-MM-dd HH:mm:ss`。@NotEmpty。</summary>
    [JsonPropertyName("time")]
    public string? Time { get; init; }

    /// <summary>报警类型：1=defect（缺陷）/ 2=system（系统）/ 3=device（设备）。@Range(1,3)。</summary>
    [JsonPropertyName("type")]
    public int? Type { get; init; }

    /// <summary>产线编号。@NotEmpty。</summary>
    [JsonPropertyName("lineNo")]
    public string? LineNo { get; init; }

    /// <summary>面编号。@NotEmpty。</summary>
    [JsonPropertyName("faceNo")]
    public string? FaceNo { get; init; }

    /// <summary>报警级别：1=提示 / 2=警告 / 3=严重 / 4=紧急。@NotNull。</summary>
    [JsonPropertyName("level")]
    public int? Level { get; init; }

    /// <summary>报警描述。@NotEmpty。</summary>
    [JsonPropertyName("message")]
    public string? Message { get; init; }

    /// <summary>处理状态：1=已解决 / 2=未解决（默认 2）。可选。</summary>
    [JsonPropertyName("solve")]
    public int? Solve { get; init; }

    /// <summary>报警原因（详见 AlarmReasonEnum，可选）。</summary>
    [JsonPropertyName("reason")]
    public int? Reason { get; init; }

    /// <summary>关联缺陷名称（用于按缺陷聚合报警）。可选。</summary>
    [JsonPropertyName("defectName")]
    public string? DefectName { get; init; }
}
