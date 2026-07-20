using IntcoEdge.Common;

namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 英科统一网关配置（绑定 `appsettings.json` 的 `IntcoEdge:YingkeGateway` 节）。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.config.YKConfig`（PSM `yk.*` 配置）。
/// </summary>
public class YingkeGatewayOptions
{
    /// <summary>配置节名（用于 `IConfiguration.GetSection(...)`）。</summary>
    public const string SectionName = "IntcoEdge:YingkeGateway";

    /// <summary>英科网关完整 URL（含 `/api/dataportal/invoke` 路径）。</summary>
    public string Url { get; set; } = "http://192.168.80.33:10031/api/dataportal/invoke";

    /// <summary>登录用户名（英科系统账号，PSM 固定用 `HKSJSB`）。</summary>
    public string Username { get; set; } = "HKSJSB";

    /// <summary>登录密码（PSM 固定用 `HKSJSB123`）。</summary>
    public string Password { get; set; } = "HKSJSB123";

    /// <summary>车间代码（PSM 端 `yk.workshop`，如 `QZN2`）。</summary>
    public string WorkshopCode { get; set; } = "QZN2";

    /// <summary>单次 HTTP 调用超时（毫秒）。</summary>
    public int TimeoutMs { get; set; } = 5000;

    /// <summary>失败重试次数（不含首次）。</summary>
    public int RetryCount { get; set; } = 3;

    /// <summary>ticket 缓存 TTL（分钟）。PSM 是 50 分钟重登，缓存比它短一些。</summary>
    public int TicketCacheMinutes { get; set; } = Constants.YkTicketLoginIntervalMinutes - 5; // 45 min

    /// <summary>是否启用英科网关对接。false 时所有调英科方法直接返回 null/不推送。</summary>
    public bool Enabled { get; set; } = true;

    /// <summary>英科网关库存组织 ID（PSM 端是 1）。</summary>
    public int InvOrgId { get; set; } = 1;
}
