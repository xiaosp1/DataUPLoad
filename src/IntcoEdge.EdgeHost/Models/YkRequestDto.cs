using System.Text.Json;
using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关统一请求包装体。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.YKRequestDTO`。
///
/// 调用英科网关的所有请求都必须装在这个壳里：
/// <code>
/// {
///   "ApiType": "AuthenticationController",     // 控制器名（不是 HTTP 动词）
///   "Method":  "Login",                        // 方法名
///   "Parameters": [                            // 参数列表（按顺序对应方法形参）
///     { "Value": "HKSJSB" },
///     { "Value": "HKSJSB123" }
///   ],
///   "Context": { "Ticket": "...", "InvOrgId": 1 }
/// }
/// </code>
///
/// ⚠️ 关键事实（PM 反编译 + 权威 docx 确认）：
///   - `Parameters` 是**数组**，每个元素是 `{Value: ...}` 包装器
///   - 字符串参数（如登录的用户名/密码）用 <see cref="YkLoginRequest"/> 包装（= StringParamDTO）
///   - 列表型参数（如报警数组）用 <see cref="YkListParam{T}"/> 包装（= ListParamsDTO）
///   - `ApiType` 是**控制器类名**（`AuthenticationController`），不是 `inkey.user.login`
///   - `Method` 是**方法名**（`Login`），不是 HTTP 动词
/// </summary>
public record class YkRequestDto
{
    /// <summary>控制器类名（如 `AuthenticationController` / `VisualInspectionController`）。</summary>
    [JsonPropertyName("ApiType")]
    public string? ApiType { get; init; }

    /// <summary>
    /// 参数列表。每个元素是 `{Value: ...}` 包装器，按顺序对应方法形参。
    /// 留空 `null` 时不写入 JSON（避免英科网关收到空数组）。
    /// 使用 <see cref="object"/> 是因为：登录用 `StringParamDTO`、报警推送用 `ListParamsDTO`，
    /// 两种包装器类型不同。
    /// </summary>
    [JsonPropertyName("Parameters")]
    public List<object>? Parameters { get; init; }

    /// <summary>方法名（如 `Login` / `HandleVisualInspectionAlarm`）。</summary>
    [JsonPropertyName("Method")]
    public string? Method { get; init; }

    /// <summary>调用上下文（带 Ticket）。登录时为 `null`，调用业务接口时必填。</summary>
    [JsonPropertyName("Context")]
    public YkContextDto? Context { get; init; }
}
