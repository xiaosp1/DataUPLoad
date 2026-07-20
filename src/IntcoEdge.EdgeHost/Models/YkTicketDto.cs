using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关 ticket 凭证。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.ContextDTO`（字段命名规则一致）。
/// ticket 是英科网关的调用凭证，调用其它 ApiType 时放进 `Context.Ticket`。
/// </summary>
public record class YkTicketDto
{
    /// <summary>调用凭证，登录后由英科网关颁发。</summary>
    [JsonPropertyName("Ticket")]
    public string? Ticket { get; init; }

    /// <summary>库存组织 ID。</summary>
    [JsonPropertyName("InvOrgId")]
    public int? InvOrgId { get; init; }
}
