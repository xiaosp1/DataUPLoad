using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 报警服务：把入站报警入库，并按需推给英科网关。
///
/// 历史路径（W-A4）：<see cref="HandleAlarmAsync(AlarmRecordDto, CancellationToken)"/>
/// 视觉软件 → /client/data/alarm → AlarmRecordDto → 写入 alarm_record（按 uuid 幂等）。
///
/// 新路径（W-A7-M）：<see cref="HandleAlarmAsync(AlarmInputDto, CancellationToken)"/>
///   1. 入参归一化（AlarmInputDto，携带业务幂等键 alarmId）
///   2. 仓储 UpsertByAlarmId（按 alarmId 幂等 → INSERT 或 UPDATE 可变字段）
///   3. 调 IYingkeService.PushAlarmAsync 推英科网关（自动管 ticket + 批量）
///   4. 仓储 UpdateSendStatus 把推送结果（pushed/failed + ykCode + errorMsg）回写
///   5. 返回 AlarmHandleResult 给 Controller / 上游
///
/// ★ Idempotency 约定：
///   - 同一 alarmId 第二次进来，仓储走 UPDATE，不重新推英科
///     （PSM 端 AlarmRecordServiceImpl 也是这个语义）。
///   - 若上游明确要求"重推"，由调用方在 AlarmInputDto.AlarmId 之外再加 forcePush 标志
///     （本期未实现，留 TODO）。
/// </summary>
public interface IAlarmService
{
    /// <summary>W-A4 老路径：处理视觉软件 / 老 PSM 推送的 AlarmRecordDto。</summary>
    /// <returns>已落库 + 已推送的状态摘要（简化三元组）。</returns>
    Task<AlarmHandleResult> HandleAlarmAsync(AlarmRecordDto alarm, CancellationToken ct = default);

    /// <summary>
    /// W-A7-M 新路径：处理归一化 AlarmInputDto，按 alarmId 幂等 upsert + 推英科 + 回写 send_status。
    /// 默认实现：转 AlarmRecordDto 走老路径（保 W-A4 兼容 + 让老 fake/test 不需要重写）。
    /// AlarmService 重写该方法走完整 A7-M 路径。
    /// </summary>
    /// <returns>包含 AlarmId / SendStatus / YkCode / ErrorMsg 的处理结果。</returns>
    Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputDto input, CancellationToken ct = default)
    {
        // 默认 fallback：让老 fake / 老实现不需要重写也能编过
        // （AlarmService 会重写，走完整逻辑）
        var fallback = new AlarmRecordDto
        {
            Uuid = input.Uuid ?? input.AlarmId,
            Time = input.Time,
            Type = input.Type,
            LineNo = input.LineNo,
            FaceNo = input.FaceNo,
            Level = input.Level,
            Message = input.Message,
            Solve = input.Solve,
            Reason = input.Reason,
            DefectName = input.DefectName,
        };
        return HandleAlarmAsync(fallback, ct);
    }
}

// AlarmHandleResult 的形状见 Services/AlarmConversion.cs（统一在那里定义）。
// W-A4 老路径返回的 Persisted/PushedToYk 映射到新字段 Persisted/SendStatus。

public class AlarmService : IAlarmService
{
    private readonly ILogger<AlarmService> _logger;
    private readonly YingkeGatewayClient _ykClient;
    private readonly IYingkeService _ykService;
    private readonly IAlarmRecordRepository _repo;

    public AlarmService(
        ILogger<AlarmService> logger,
        YingkeGatewayClient ykClient,
        IYingkeService ykService,
        IAlarmRecordRepository repo)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _ykClient = ykClient ?? throw new ArgumentNullException(nameof(ykClient));
        _ykService = ykService ?? throw new ArgumentNullException(nameof(ykService));
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
    }

    // ===================================================================
    // 老路径（W-A4）：AlarmRecordDto → InsertOrIgnore → ykClient.PushAlarmAsync
    // 行为不变，供 DetectController.PushAlarm 继续调用。
    // ===================================================================

    public async Task<AlarmHandleResult> HandleAlarmAsync(AlarmRecordDto alarm, CancellationToken ct = default)
    {
        if (alarm == null)
        {
            throw new ArgumentNullException(nameof(alarm));
        }
        if (string.IsNullOrWhiteSpace(alarm.Uuid))
        {
            throw new ArgumentException("uuid 必填", nameof(alarm));
        }

        _logger.LogInformation(
            "HandleAlarmAsync(W-A4) uuid={Uuid} lineNo={LineNo} faceNo={FaceNo} type={Type} level={Level}",
            alarm.Uuid, alarm.LineNo, alarm.FaceNo, alarm.Type, alarm.Level);

        // DTO → POJO 映射
        var input = new AlarmRecordInput(
            Uuid: alarm.Uuid!,
            Time: alarm.Time ?? string.Empty,
            Type: alarm.Type ?? 0,
            LineNo: alarm.LineNo ?? string.Empty,
            FaceNo: alarm.FaceNo ?? string.Empty,
            Level: alarm.Level ?? 0,
            Message: alarm.Message ?? string.Empty,
            Solve: alarm.Solve ?? 2,
            Reason: alarm.Reason,
            DefectName: alarm.DefectName);

        // 入库（按 uuid 幂等；命中重复返回 0）
        bool persisted;
        try
        {
            var written = _repo.InsertOrIgnore(input);
            persisted = written > 0;
            if (!persisted)
            {
                _logger.LogInformation("HandleAlarmAsync(W-A4) uuid={Uuid} 已存在（去重命中），跳过入库", alarm.Uuid);
            }
        }
        catch (Exception ex)
        {
            // 入库失败抛上去，让 Controller 转 500；不静默吞。
            _logger.LogError(ex, "HandleAlarmAsync(W-A4) 入库失败 uuid={Uuid}", alarm.Uuid);
            throw;
        }

        // 同步推英科：失败也不抛（报警业务是 fire-and-forget）
        bool pushed = false;
        try
        {
            var push = new AlarmPushDto
            {
                Line = alarm.LineNo,
                Face = alarm.FaceNo,
                AlarmTime = alarm.Time,
                AlarmDetails = alarm.Message,
                AlarmCount = 1,
            };
            // W-A6：PushAlarmAsync 现在接收 IReadOnlyList<AlarmPushDto>（批量推送）
            var status = await _ykClient.PushAlarmAsync(new[] { push }, ct).ConfigureAwait(false);
            pushed = status.HasValue && status.Value >= 200 && status.Value < 300;
        }
        catch (System.Net.Http.HttpRequestException ex)
        {
            _logger.LogWarning(ex, "推英科报警失败 uuid={Uuid}", alarm.Uuid);
        }

        return new AlarmHandleResult
        {
            AlarmId = alarm.Uuid ?? string.Empty,
            SendStatus = pushed ? "pushed" : "failed",
            YkCode = pushed ? 200 : null,
            ErrorMsg = pushed ? null : "W-A4 老路径：英科网关通道失败（无详细 error）",
            Persisted = persisted,
        };
    }

    // ===================================================================
    // 新路径（W-A7-M）：AlarmInputDto → UpsertByAlarmId → ykService.PushAlarmAsync
    // → UpdateSendStatus → 返回扩展 AlarmHandleResult（4 字段）。
    // ===================================================================

    public async Task<AlarmHandleResult> HandleAlarmAsync(AlarmInputDto input, CancellationToken ct = default)
    {
        if (input == null)
        {
            throw new ArgumentNullException(nameof(input));
        }
        if (string.IsNullOrWhiteSpace(input.LineNo))
        {
            throw new ArgumentException("lineNo 必填", nameof(input));
        }
        if (string.IsNullOrWhiteSpace(input.FaceNo))
        {
            throw new ArgumentException("faceNo 必填", nameof(input));
        }
        if (string.IsNullOrWhiteSpace(input.Time))
        {
            throw new ArgumentException("time 必填", nameof(input));
        }
        if (input.Type < 1 || input.Type > 3)
        {
            throw new ArgumentException("type 必须为 1/2/3", nameof(input));
        }
        if (input.Level < 1 || input.Level > 4)
        {
            throw new ArgumentException("level 必须为 1..4", nameof(input));
        }

        // 业务幂等键：入参有则透传，否则生成 Guid
        var alarmId = string.IsNullOrWhiteSpace(input.AlarmId)
            ? Guid.NewGuid().ToString("N")
            : input.AlarmId!.Trim();

        _logger.LogInformation(
            "HandleAlarmAsync(W-A7-M) alarmId={AlarmId} lineNo={LineNo} faceNo={FaceNo} type={Type} level={Level}",
            alarmId, input.LineNo, input.FaceNo, input.Type, input.Level);

        // 1) 入库（按 alarmId 幂等 → 命中 UPDATE / 未命中 INSERT；返回新增行数 0/1）
        var persisted = false;
        try
        {
            var recordInput = new AlarmRecordInput(
                Uuid: input.Uuid ?? string.Empty,
                Time: input.Time,
                Type: input.Type,
                LineNo: input.LineNo,
                FaceNo: input.FaceNo,
                Level: input.Level,
                Message: input.Message ?? string.Empty,
                Solve: input.Solve,
                Reason: input.Reason,
                DefectName: input.DefectName,
                AlarmId: alarmId,
                SendStatus: "pending",
                YkCode: 0,
                ErrorMsg: null);

            var written = _repo.UpsertByAlarmId(recordInput);
            persisted = written > 0;
            if (!persisted)
            {
                _logger.LogInformation(
                    "HandleAlarmAsync(W-A7-M) alarmId={AlarmId} 已存在（去重命中），仅 UPDATE 可变字段",
                    alarmId);
            }
        }
        catch (Exception ex)
        {
            // 入库失败抛上去，让 Controller 转 500；不静默吞。
            _logger.LogError(ex, "HandleAlarmAsync(W-A7-M) 入库失败 alarmId={AlarmId}", alarmId);
            throw;
        }

        // 2) 推英科网关（用 IYingkeService 统一走 ticket 缓存 + 日志）
        var push = new AlarmPushDto
        {
            WorkShop = input.WorkShop,
            Line = input.LineNo,
            Face = input.FaceNo,
            AlarmTime = input.Time,
            AlarmType = input.AlarmTypeDesc,
            AlarmLevel = input.AlarmLevelDesc,
            AlarmDetails = input.Message,
            AlarmResult = input.AlarmResult,
            AlarmCount = input.AlarmCount ?? 1,
        };

        int? ykCode = null;
        string? errorMsg = null;
        string sendStatus = "pending";

        try
        {
            ykCode = await _ykService.PushAlarmAsync(new[] { push }, ct).ConfigureAwait(false);
            if (ykCode == 200)
            {
                sendStatus = "pushed";
                _logger.LogInformation(
                    "HandleAlarmAsync(W-A7-M) 推英科成功 alarmId={AlarmId} ykCode={YkCode}",
                    alarmId, ykCode);
            }
            else
            {
                // 业务失败（400 等）/ 通道失败（null）
                sendStatus = "failed";
                errorMsg = ykCode.HasValue
                    ? $"英科网关业务失败 code={ykCode}"
                    : "英科网关通道失败（网络/HTTP/超时）";
                _logger.LogWarning(
                    "HandleAlarmAsync(W-A7-M) 推英科失败 alarmId={AlarmId} ykCode={YkCode} err={Error}",
                    alarmId, ykCode, errorMsg);
            }
        }
        catch (System.Net.Http.HttpRequestException ex)
        {
            sendStatus = "failed";
            errorMsg = ex.Message;
            _logger.LogWarning(ex, "HandleAlarmAsync(W-A7-M) 推英科网络异常 alarmId={AlarmId}", alarmId);
        }
        catch (Exception ex)
        {
            sendStatus = "failed";
            errorMsg = ex.Message;
            _logger.LogError(ex, "HandleAlarmAsync(W-A7-M) 推英科未捕获异常 alarmId={AlarmId}", alarmId);
        }

        // 3) 把推送结果回写 alarm_record.send_status / yk_code / error_msg
        // 用仓储契约的 int ykCode（null → 0），让 SQL 端不出错
        try
        {
            _repo.UpdateSendStatus(alarmId, sendStatus, ykCode ?? 0, errorMsg);
        }
        catch (Exception ex)
        {
            // 回写失败只警告，不影响业务结果返回（库里有完整业务字段，前端靠返回值判断）
            _logger.LogWarning(ex,
                "HandleAlarmAsync(W-A7-M) UpdateSendStatus 失败 alarmId={AlarmId}（不影响业务结果返回）",
                alarmId);
        }

        return new AlarmHandleResult
        {
            AlarmId = alarmId,
            SendStatus = sendStatus,
            YkCode = ykCode,
            ErrorMsg = errorMsg,
            Persisted = persisted,
        };
    }
}
