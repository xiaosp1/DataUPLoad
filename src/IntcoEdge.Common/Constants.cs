namespace IntcoEdge.Common;

/// <summary>
/// 全局常量（端口、路径、协议版本等）。
/// 老板 16:12 拍板：REST API 端口 = 5288（沿用现场老 EdgeHost）。
/// </summary>
public static class Constants
{
    /// <summary>EdgeHost REST API 监听端口。</summary>
    public const int EdgeHostPort = 5288;

    /// <summary>健康检查端点路径。</summary>
    public const string HealthPath = "/health";

    /// <summary>SQLite 数据库默认相对路径（相对 EdgeHost 工作目录）。</summary>
    public const string DefaultDbPath = "data/intco.db";

    /// <summary>Flyway 风格迁移脚本目录。</summary>
    public const string MigrationsPath = "migrations";

    /// <summary>PSM 协议版本（视觉软件推送格式 v1+2）。</summary>
    public const string PsmProtocolVersion = "1.0";

    /// <summary>英科统一网关调用路径。</summary>
    public const string YkInvokePath = "/api/dataportal/invoke";
}
