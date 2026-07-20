using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关登录请求参数。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.StringParamDTO` / `ListParamsDTO`。
/// ⚠️ 字段名首字母**大写**（PSM 显式 `@JsonProperty`），必须原样保留。
/// </summary>
public record class YkLoginRequest
{
    /// <summary>车间编码（workshopCode），英科 MES 用于路由到具体工厂/车间。</summary>
    [JsonPropertyName("WorkShopCode")]
    public string? WorkShopCode { get; init; }
}
