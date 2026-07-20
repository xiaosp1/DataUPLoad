using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关统一响应包装体。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKResponseDTO`。
/// `Result` 字段是 `Object`，由 `ApiType` 决定具体类型，调用方按需反序列化。
/// </summary>
public record class YkResponseDto
{
    /// <summary>是否成功。</summary>
    [JsonPropertyName("Success")]
    public bool? Success { get; init; }

    /// <summary>英科网关返回的消息（错误时含原因）。</summary>
    [JsonPropertyName("Message")]
    public string? Message { get; init; }

    /// <summary>结果载荷（PSM 端类型为 Object，按需反序列化）。</summary>
    [JsonPropertyName("Result")]
    public System.Text.Json.JsonElement? Result { get; init; }

    /// <summary>调用上下文（响应里也带回，方便调试）。</summary>
    [JsonPropertyName("Context")]
    public YkContextDto? Context { get; init; }
}
