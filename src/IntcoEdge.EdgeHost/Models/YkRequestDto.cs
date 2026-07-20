using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关统一请求包装体。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKRequestDTO`。
/// 所有调用英科网关的请求都必须装在这个壳里：`ApiType` + `Parameters` + `Method` + `Context`。
/// </summary>
public record class YkRequestDto<T>
{
    /// <summary>API 类型（路由键），如 `inkey.edge.dataTrans`。</summary>
    [JsonPropertyName("ApiType")]
    public string? ApiType { get; init; }

    /// <summary>参数列表（PSM 端用 List 承载，列表长度通常为 1）。</summary>
    [JsonPropertyName("Parameters")]
    public List<T>? Parameters { get; init; }

    /// <summary>方法名（可选，部分 ApiType 不需要）。</summary>
    [JsonPropertyName("Method")]
    public string? Method { get; init; }

    /// <summary>调用上下文（带 Ticket）。</summary>
    [JsonPropertyName("Context")]
    public YkContextDto? Context { get; init; }
}
