using System.Text.Json;
using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关统一响应包装体。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKResponseDTO`。
/// `Result` 字段在 PSM/Java 端是 `Object`，按需反序列化为强类型。
/// </summary>
public record class YkResponseDto
{
    /// <summary>是否成功（顶层 boolean）。</summary>
    [JsonPropertyName("Success")]
    public bool? Success { get; init; }

    /// <summary>英科网关返回的消息（错误时含原因）。</summary>
    [JsonPropertyName("Message")]
    public string? Message { get; init; }

    /// <summary>结果载荷（PSM 端类型为 Object，按需反序列化）。</summary>
    [JsonPropertyName("Result")]
    public JsonElement? Result { get; init; }

    /// <summary>调用上下文（响应里也带回新 ticket + InvOrgId）。</summary>
    [JsonPropertyName("Context")]
    public YkContextDto? Context { get; init; }

    /// <summary>把 <see cref="Result"/> 反序列化为指定类型（Result 为 null 时返回 default）。</summary>
    public T? DeserializeResult<T>(JsonSerializerOptions? options = null)
    {
        if (Result is null) return default;
        return Result.Value.Deserialize<T>(options ?? IntcoEdge.EdgeHost.Clients.IntcoHttpClient.DefaultJsonOptions);
    }
}
