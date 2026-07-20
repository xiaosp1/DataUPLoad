using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关登录请求参数（单字符串包装）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.StringParamDTO`。
/// ⚠️ 字段名首字母**大写**（PSM 显式 `@JsonProperty(value="Value")`），必须原样保留。
/// 在 `YkRequestDto<T>.Parameters` 里，每个元素都是一个 `{ "Value": "..." }` 包装器。
/// </summary>
public record class YkLoginRequest
{
    /// <summary>参数值（PSM `StringParamDTO.Value`）。</summary>
    [JsonPropertyName("Value")]
    public string? Value { get; init; }

    public static YkLoginRequest Wrap(string value) => new() { Value = value };
}
