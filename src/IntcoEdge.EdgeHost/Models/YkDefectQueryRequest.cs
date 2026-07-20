using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关缺陷查询请求参数（包装 `SearchDefectRecordDto`）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKRequestDTO` + `SearchDefectRecordDTO`。
/// ⚠️ `Parameters` 是 List，因为 PSM 端 `YKRequestDTO` 用 `List<T>` 装参数。
/// </summary>
public record class YkDefectQueryRequest
{
    /// <summary>缺陷查询参数集合（PSM 端 List 形态，通常只塞一个）。</summary>
    [JsonPropertyName("Parameters")]
    public List<SearchDefectRecordDto>? Parameters { get; init; }
}
