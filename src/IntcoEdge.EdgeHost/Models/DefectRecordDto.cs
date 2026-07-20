using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 缺陷记录 DTO（本地 SQLite `defect_record` 表的内存形态）。
/// 字段映射来源：`com.hikrobotics.solution.module.defect.dto.ChangeLineDefectResult`
/// + `com.hikrobotics.solution.module.line.model.DefectRecordPO`（推断）。
/// 用于英科网关推来的 `POST /client/yk/defect-record` 反序列化结果。
/// </summary>
public record class DefectRecordDto
{
    /// <summary>主键 ID（本地自增，可选；新增时为 null）。</summary>
    [JsonPropertyName("id")]
    public long? Id { get; init; }

    /// <summary>产线编号。</summary>
    [JsonPropertyName("lineNo")]
    public string? LineNo { get; init; }

    /// <summary>面编号。</summary>
    [JsonPropertyName("faceNo")]
    public string? FaceNo { get; init; }

    /// <summary>手套编号（产线下唯一流水号）。</summary>
    [JsonPropertyName("gloveNo")]
    public string? GloveNo { get; init; }

    /// <summary>检测结果：1=良品 / 2=次品。</summary>
    [JsonPropertyName("result")]
    public int? Result { get; init; }

    /// <summary>缺陷类型（与 `defect_type.code` 对齐）。</summary>
    [JsonPropertyName("defectType")]
    public string? DefectType { get; init; }

    /// <summary>缺陷图片路径列表（JSON 字符串或数组）。</summary>
    [JsonPropertyName("imgList")]
    public string? ImgList { get; init; }

    /// <summary>检测时间，ISO 8601 字符串。</summary>
    [JsonPropertyName("time")]
    public string? Time { get; init; }
}
