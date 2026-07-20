using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关缺陷查询响应（`Result` 字段是 `List<DefectRecordDto>` 的 JSON 字符串或对象）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKResponseDTO`。
/// 我们这里把 `Result` 留成 `JsonElement` 形态，让调用方按需反序列化，
/// 避免编译期绑定 PSM 端 Result 的具体类型（PSM 端 Result 是 `Object`）。
/// </summary>
public record class YkDefectQueryResponse
{
    /// <summary>是否成功。</summary>
    [JsonPropertyName("Success")]
    public bool? Success { get; init; }

    /// <summary>英科网关返回的消息（错误时含原因）。</summary>
    [JsonPropertyName("Message")]
    public string? Message { get; init; }

    /// <summary>结果载荷（PSM 端类型为 Object，保留为字符串由调用方解析）。</summary>
    [JsonPropertyName("Result")]
    public string? Result { get; init; }
}
