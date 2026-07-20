namespace IntcoEdge.EdgeHost.Clients;

/// <summary>
/// 英科统一网关配置（绑定 `appsettings.json` 的 `IntcoEdge:YingkeGateway` 节）。
/// </summary>
public class YingkeGatewayOptions
{
    /// <summary>配置节名（用于 `IConfiguration.GetSection(...)`）。</summary>
    public const string SectionName = "IntcoEdge:YingkeGateway";

    /// <summary>英科网关 base URL（含 `/api/dataportal/invoke` 完整路径）。</summary>
    public string Url { get; set; } = "http://192.168.80.33:10031/api/dataportal/invoke";

    /// <summary>默认 ApiType：边缘数据上传。</summary>
    public string ApiType { get; set; } = "inkey.edge.dataTrans";

    /// <summary>单次 HTTP 调用超时（毫秒）。</summary>
    public int TimeoutMs { get; set; } = 5000;

    /// <summary>失败重试次数（不含首次）。</summary>
    public int RetryCount { get; set; } = 2;
}
