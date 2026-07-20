using IntcoEdge.EdgeHost.Models;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 报警服务层归一化输入。
///
/// 设计目的：
///   - 入站请求体（<see cref="AlarmPushDto"/> / <see cref="AlarmRecordDto"/>）字段扁平且
///     与英科网关/视觉软件协议绑定；服务层不应该把外部协议字段直接写进仓储 POJO。
///   - 这层多了一个 <see cref="AlarmId"/> 字段：A7 业务幂等键，Service 在落库前生成。
///     入站若自带（前端/Webhook 重推）则透传，否则由 Service 用 Guid 生成。
///
/// W-A7-M：Controller → Service 之间的内部数据载体，**不入英科网关协议**，
/// 也不暴露到 Swagger（外层还是 <see cref="AlarmPushDto"/>）。
/// </summary>
public record class AlarmInputDto
{
    /// <summary>报警唯一 ID（A7 业务幂等键）。若为 null/空，由 Service 生成 Guid。</summary>
    public string? AlarmId { get; init; }

    /// <summary>上游原始 UUID（视觉软件 / 老 PSM 协议）。可选。</summary>
    public string? Uuid { get; init; }

    /// <summary>报警时间，格式 `yyyy-MM-dd HH:mm:ss`。</summary>
    public string Time { get; init; } = string.Empty;

    /// <summary>报警类型：1=defect / 2=system / 3=device。</summary>
    public int Type { get; init; }

    /// <summary>产线编号。</summary>
    public string LineNo { get; init; } = string.Empty;

    /// <summary>面编号。</summary>
    public string FaceNo { get; init; } = string.Empty;

    /// <summary>报警级别：1=提示 / 2=警告 / 3=严重 / 4=紧急。</summary>
    public int Level { get; init; }

    /// <summary>报警内容明细。</summary>
    public string Message { get; init; } = string.Empty;

    /// <summary>处理状态：1=已解决 / 2=未解决。</summary>
    public int Solve { get; init; } = 2;

    /// <summary>报警原因（可选）。</summary>
    public int? Reason { get; init; }

    /// <summary>关联缺陷名称（可选）。</summary>
    public string? DefectName { get; init; }

    /// <summary>车间编号（英科 MES 组织编码，可选）。</summary>
    public string? WorkShop { get; init; }

    /// <summary>报警类型描述（PSM 端 AlarmTypeEnum.getDescription，可选）。</summary>
    public string? AlarmTypeDesc { get; init; }

    /// <summary>报警级别描述（PSM 端 AlarmLevelEnum.getLevel，可选）。</summary>
    public string? AlarmLevelDesc { get; init; }

    /// <summary>处理结果（已处理/未处理 等，可选）。</summary>
    public string? AlarmResult { get; init; }

    /// <summary>报警累计次数（用于英科侧聚合展示，可选）。</summary>
    public int? AlarmCount { get; init; }
}

/// <summary>
/// Service / Controller 处理结果（业务侧）。
///
/// 字段设计——兼容 W-A4 老路径（<c>Persisted</c>/<c>PushedToYk</c> 两个 bool）
/// + 扩展 W-A7-M 新路径（<c>AlarmId</c>/<c>SendStatus</c>/<c>YkCode</c>/<c>ErrorMsg</c>）。
///   - <see cref="PushedToYk"/> 等价于 <c>SendStatus == "pushed"</c>，保 W-A7-S WebhookController
///     现有调用 <c>result.PushedToYk</c> 不需要改动。
///   - <see cref="SendStatus"/> 用字符串而不是 bool：覆盖 pending/sent/failed 三态，
///     避免失败时丢"已经尝试过"的信息（重试 / 排错需要）。
/// </summary>
public record class AlarmHandleResult
{
    /// <summary>报警唯一 ID（与入参 AlarmId 一致；Service 自动生成时回填）。</summary>
    public string AlarmId { get; init; } = string.Empty;

    /// <summary>推送状态：<c>"pushed"</c> / <c>"failed"</c> / <c>"pending"</c>。</summary>
    /// <remarks>
    ///   - <c>pushed</c>  = 英科网关业务 code 200 入库完成
    ///   - <c>failed</c>  = 调用英科网关失败（网络/通道/业务 code 非 200）
    ///   - <c>pending</c> = 未尝试推送（英科网关禁用 / 入参被去重跳过 / 不在推送条件）
    /// </remarks>
    public string SendStatus { get; init; } = "pending";

    /// <summary>英科网关业务 code（200/400）。通道失败时为 null。</summary>
    public int? YkCode { get; init; }

    /// <summary>失败时的错误信息（成功/pending 时为 null）。</summary>
    public string? ErrorMsg { get; init; }

    /// <summary>是否新插入到 alarm_record（false = 命中幂等更新）。</summary>
    public bool Persisted { get; init; }

    /// <summary>W-A4 老路径兼容：英科推送是否成功（等价于 <c>SendStatus == "pushed"</c>）。</summary>
    public bool PushedToYk => SendStatus == "pushed";

    /// <summary>W-A4 老路径兼容：跳过的原因（本期保留为 null，留给未来扩展）。</summary>
    public string? SkipReason { get; init; }

    /// <summary>
    /// W-A4 老路径 / W-A7-S 测试兼容的便捷构造器。
    /// 调用示例：<c>new AlarmHandleResult(Persisted: true, PushedToYk: true, SkipReason: null)</c>。
    /// </summary>
    public AlarmHandleResult() { }

    /// <summary>
    /// W-A4 老路径 / W-A7-S 测试兼容的位置参数构造器。
    /// 注意：这里仅取 Persisted/PushedToYk/SkipReason 三个值。
    /// AlarmId 留空、SendStatus 根据 PushedToYk 推断、YkCode/ErrorMsg 留默认。
    /// </summary>
    public AlarmHandleResult(bool Persisted, bool PushedToYk, string? SkipReason)
    {
        this.Persisted = Persisted;
        this.SendStatus = PushedToYk ? "pushed" : "failed";
        this.SkipReason = SkipReason;
    }
}

/// <summary>
/// 入站 DTO → 内部 AlarmInputDto 转换契约。
/// 把"外部协议字段名"和"业务层字段名"解耦：
///   - <see cref="FromPushDto"/>：来自英科侧推送格式（<see cref="AlarmPushDto"/>）。
///   - <see cref="FromRecordDto"/>：来自视觉软件 / 老 PSM 格式（<see cref="AlarmRecordDto"/>）。
///
/// W-A7-M：单独抽出接口便于 Controller 测试 mock，避免 Service 直绑 ASP.NET DTO。
/// </summary>
public interface IAlarmConversion
{
    /// <summary>英科推送 DTO → 内部入参。</summary>
    AlarmInputDto FromPushDto(AlarmPushDto dto);

    /// <summary>视觉软件 / 老 PSM 报警记录 DTO → 内部入参。</summary>
    AlarmInputDto FromRecordDto(AlarmRecordDto dto);
}

public class AlarmConversion : IAlarmConversion
{
    private readonly ILogger<AlarmConversion> _logger;

    public AlarmConversion(ILogger<AlarmConversion> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public AlarmInputDto FromPushDto(AlarmPushDto dto)
    {
        if (dto == null) throw new ArgumentNullException(nameof(dto));

        // AlarmPushDto 是英科侧的"推送格式"，字段值是英文/中文混合描述。
        // 把它映射到 alarm_record 时需要给 type/level 一个合理默认值：
        //   - type=2（system，PSM AlarmTypeEnum.SYSTEM 的 fallback）
        //   - level=2（warning，PSM AlarmLevelEnum.getLevel("警告") 的 fallback）
        // 真要做严格枚举映射，需要 AlarmTypeEnum/AlarmLevelEnum 反编译回来后再细化。
        return new AlarmInputDto
        {
            AlarmId = null,           // 由 Service 生成
            Uuid = null,              // 英科推送不带 PSM uuid
            Time = dto.AlarmTime ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
            Type = 2,                 // system（占位）
            LineNo = dto.Line ?? string.Empty,
            FaceNo = dto.Face ?? string.Empty,
            Level = 2,                // warning（占位）
            Message = dto.AlarmDetails ?? string.Empty,
            Solve = 2,                // 未解决
            Reason = null,
            DefectName = null,
            WorkShop = dto.WorkShop,
            AlarmTypeDesc = dto.AlarmType,
            AlarmLevelDesc = dto.AlarmLevel,
            AlarmResult = dto.AlarmResult,
            AlarmCount = dto.AlarmCount ?? 1,
        };
    }

    public AlarmInputDto FromRecordDto(AlarmRecordDto dto)
    {
        if (dto == null) throw new ArgumentNullException(nameof(dto));

        return new AlarmInputDto
        {
            AlarmId = dto.Uuid,       // 视觉软件的 uuid 直接复用为 alarmId（保幂等）
            Uuid = dto.Uuid,
            Time = dto.Time ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
            Type = dto.Type ?? 2,
            LineNo = dto.LineNo ?? string.Empty,
            FaceNo = dto.FaceNo ?? string.Empty,
            Level = dto.Level ?? 2,
            Message = dto.Message ?? string.Empty,
            Solve = dto.Solve ?? 2,
            Reason = dto.Reason,
            DefectName = dto.DefectName,
            WorkShop = null,
            AlarmTypeDesc = null,
            AlarmLevelDesc = null,
            AlarmResult = null,
            AlarmCount = 1,
        };
    }
}
