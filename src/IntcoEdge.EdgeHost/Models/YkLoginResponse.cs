using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关登录返回（嵌套在 `YKResponseDTO.Result` 中）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.LoginResultDTO`。
/// 关键字段是 `UserId` + `UserCode`，后续接口用 `UserId` 换 ticket。
/// </summary>
public record class YkLoginResponse
{
    /// <summary>用户 ID（英科 MES 用户主键）。</summary>
    [JsonPropertyName("UserId")]
    public string? UserId { get; init; }

    /// <summary>员工 ID（HR 系统员工号）。</summary>
    [JsonPropertyName("EmployeeId")]
    public string? EmployeeId { get; init; }

    /// <summary>用户编码（登录名）。</summary>
    [JsonPropertyName("UserCode")]
    public string? UserCode { get; init; }

    /// <summary>用户姓名。</summary>
    [JsonPropertyName("UserName")]
    public string? UserName { get; init; }

    /// <summary>库存组织 ID。</summary>
    [JsonPropertyName("InvOrg")]
    public int? InvOrg { get; init; }
}
