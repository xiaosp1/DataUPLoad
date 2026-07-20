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

    /// <summary>视觉软件推送检测数据的端点（PSM 兼容路径）。</summary>
    public const string DetectPushPath = "/client/data/detect";

    /// <summary>PSM 报警推送端点（视觉软件推）。</summary>
    public const string AlarmPushPath = "/client/data/alarm";

    /// <summary>PSM 处理报警端点。</summary>
    public const string AlarmDealPath = "/client/data/deal-alarm";

    /// <summary>PSM 状态推送端点（视觉软件推）。</summary>
    public const string StatusPushPath = "/client/data/status";

    /// <summary>英科网关推缺陷记录到 PSM 的端点。</summary>
    public const string YkDefectPushPath = "/client/yk/defect-record";

    /// <summary>英科网关查产线-缺陷字典端点。</summary>
    public const string YkLineDefectPath = "/client/yk/line-defect";

    // ============ W-A6：英科网关 ApiType/Method 常量（按 PSM 反编译精确字符串） ============
    // 反编译来源：`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/yingke/service/impl/YKServiceImpl.java`
    //
    // ⚠️ 这些 ApiType 是**控制器类名**，不是自定义的 `inkey.*` 路由键。
    // PSM 调英科网关的所有 API 都按以下格式包装：
    //   { ApiType: <控制器类名>, Method: <方法名>, Parameters: [...], Context: {Ticket, InvOrgId} }

    /// <summary>英科网关 ApiType：登录控制器（取 ticket）。</summary>
    public const string YkApiTypeAuth = "AuthenticationController";

    /// <summary>英科网关 ApiType：视觉检测报警控制器。</summary>
    public const string YkApiTypeVisualInspection = "VisualInspectionController";

    /// <summary>英科网关 Method：登录。</summary>
    public const string YkMethodLogin = "Login";

    /// <summary>英科网关 Method：上传视觉检测报警（支持批量）。</summary>
    public const string YkMethodHandleVisualInspectionAlarm = "HandleVisualInspectionAlarm";

    /// <summary>PSM 端 ticket 重登周期（分钟）。</summary>
    public const int YkTicketLoginIntervalMinutes = 50;
}
