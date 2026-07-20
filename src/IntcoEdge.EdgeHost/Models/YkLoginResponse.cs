using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关登录返回（嵌套在 `YKResponseDTO.Result` 中）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.LoginResultDTO`。
/// ⚠️ 字段名首字母**大写**（PSM 显式 `@JsonProperty(value="Xxx")`），必须原样保留。
/// </summary>
public record class YkLoginResponse
{
    /// <summary>用户 ID（英科 MES 用户主键，英科网关实际返回 Number 类型如 36056.0，PM 17:32 实测修正）。</summary>
    [JsonPropertyName("UserId")]
    public double? UserId { get; init; }

    /// <summary>员工 ID（英科网关实际返回 Number）。</summary>
    [JsonPropertyName("EmployeeId")]
    public double? EmployeeId { get; init; }

    /// <summary>用户编码（登录名）。</summary>
    [JsonPropertyName("UserCode")]
    public string? UserCode { get; init; }

    /// <summary>用户姓名。</summary>
    [JsonPropertyName("UserName")]
    public string? UserName { get; init; }

    /// <summary>库存组织 ID（PSM 部署在 InvOrg=1 下）。</summary>
    [JsonPropertyName("InvOrg")]
    public int? InvOrg { get; init; }
}
