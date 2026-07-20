using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// PSM 推给英科网关的报警 DTO。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.AlarmDTO`（英科模块）。
/// ⚠️ 与 `AlarmRecordDto` 的关键差异：PSM 这边 JSON 字段首字母**大写**（"WorkShop"/"Line"/"Face"/...），
/// 是 PSM 显式打了 `@JsonProperty(value="Xxx")` 的结果，调用方必须保留大小写。
/// </summary>
public record class AlarmPushDto
{
    /// <summary>车间编号（英科 MES 组织编码）。</summary>
    [JsonPropertyName("WorkShop")]
    public string? WorkShop { get; init; }

    /// <summary>产线编号。</summary>
    [JsonPropertyName("Line")]
    public string? Line { get; init; }

    /// <summary>面编号。</summary>
    [JsonPropertyName("Face")]
    public string? Face { get; init; }

    /// <summary>报警时间，格式 `yyyy-MM-dd HH:mm:ss`。</summary>
    [JsonPropertyName("AlarmTime")]
    public string? AlarmTime { get; init; }

    /// <summary>报警类型描述（中文/英文皆可，PSM 端用 `AlarmTypeEnum.getDescription()` 填）。</summary>
    [JsonPropertyName("AlarmType")]
    public string? AlarmType { get; init; }

    /// <summary>报警级别描述（PSM 端用 `AlarmLevelEnum.getLevel()` 填，如"严重"）。</summary>
    [JsonPropertyName("AlarmLevel")]
    public string? AlarmLevel { get; init; }

    /// <summary>报警内容明细。</summary>
    [JsonPropertyName("AlarmDetails")]
    public string? AlarmDetails { get; init; }

    /// <summary>处理结果（已处理/未处理 等）。</summary>
    [JsonPropertyName("AlarmResult")]
    public string? AlarmResult { get; init; }

    /// <summary>报警累计次数（用于英科侧聚合展示）。可选。</summary>
    [JsonPropertyName("AlarmCount")]
    public int? AlarmCount { get; init; }
}
