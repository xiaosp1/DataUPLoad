using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关调用上下文（请求/响应都用到）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.ContextDTO`。
/// </summary>
public record class YkContextDto
{
    /// <summary>调用凭证。</summary>
    [JsonPropertyName("Ticket")]
    public string? Ticket { get; init; }

    /// <summary>库存组织 ID。</summary>
    [JsonPropertyName("InvOrgId")]
    public int? InvOrgId { get; init; }
}
