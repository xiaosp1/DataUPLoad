using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 报警入库 + 推送英科网关 入口（W-A7-M）。
///
///   POST /api/alarm/push
///     - 入参 AlarmPushDto（兼容 W-A6 / 反编译 PSM `YKServiceImpl.pushAlarm2YK` 协议）
///     - 内部转 AlarmInputDto 调 AlarmService.HandleAlarmAsync
///     - 返回统一响应 { code, message, data: { alarmId, sendStatus, ykCode, errorMsg, persisted } }
///
/// 错误约定：
///   - body 为空 / 必填缺失 → 400
///   - 英科网关通道失败（HttpRequestException 等）→ 入库成功但 data.sendStatus="failed"
///     整体仍返 200（业务已落地，重试由前端/上游决定）
///   - 入库异常 → 500
///
/// ★ 与 W-A7-S 拆分：
///   - /api/alarm/push（本控制器）：英科推送格式入站 → Service 业务处理
///   - /api/alarm/save（W-A7-S WebhookController）：兼容老 PSM /api/alarm/save 格式
///     两者走同一个 AlarmService.HandleAlarmAsync(AlarmInputDto)。
/// </summary>
[ApiController]
[Route("api/alarm")]
public class AlarmController : ControllerBase
{
    private readonly IAlarmService _alarmService;
    private readonly IAlarmConversion _conversion;
    private readonly ILogger<AlarmController> _logger;

    public AlarmController(
        IAlarmService alarmService,
        IAlarmConversion conversion,
        ILogger<AlarmController> logger)
    {
        _alarmService = alarmService ?? throw new ArgumentNullException(nameof(alarmService));
        _conversion = conversion ?? throw new ArgumentNullException(nameof(conversion));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// 报警入库 + 推英科网关（POST /api/alarm/push）。
    /// </summary>
    [HttpPost("push")]
    public async Task<IActionResult> PushAlarm([FromBody] AlarmPushDto? dto, CancellationToken ct)
    {
        if (dto == null)
        {
            _logger.LogWarning("AlarmController.PushAlarm 收到空 body");
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        // 入参预校验（line/face/time 必填），避免把空数据送到 Service 抛异常转 500
        if (string.IsNullOrWhiteSpace(dto.Line))
        {
            return BadRequest(new { code = 400, message = "Line 必填" });
        }
        if (string.IsNullOrWhiteSpace(dto.Face))
        {
            return BadRequest(new { code = 400, message = "Face 必填" });
        }
        if (string.IsNullOrWhiteSpace(dto.AlarmTime))
        {
            return BadRequest(new { code = 400, message = "AlarmTime 必填" });
        }

        _logger.LogInformation(
            "AlarmController.PushAlarm line={Line} face={Face} time={Time} type={Type} level={Level}",
            dto.Line, dto.Face, dto.AlarmTime, dto.AlarmType, dto.AlarmLevel);

        AlarmInputDto input;
        try
        {
            input = _conversion.FromPushDto(dto);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "AlarmController.PushAlarm 参数转换失败");
            return BadRequest(new { code = 400, message = ex.Message });
        }

        try
        {
            var result = await _alarmService.HandleAlarmAsync(input, ct).ConfigureAwait(false);
            _logger.LogInformation(
                "AlarmController.PushAlarm 完成 alarmId={AlarmId} sendStatus={SendStatus} ykCode={YkCode} persisted={Persisted}",
                result.AlarmId, result.SendStatus, result.YkCode, result.Persisted);

            // 统一响应：code=0 业务成功（入库已落，sendStatus 表达推送状态）
            return Ok(new
            {
                code = 0,
                message = "ok",
                data = new
                {
                    alarmId = result.AlarmId,
                    sendStatus = result.SendStatus,
                    ykCode = result.YkCode,
                    errorMsg = result.ErrorMsg,
                    persisted = result.Persisted,
                },
            });
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "AlarmController.PushAlarm 参数错误 line={Line}", dto.Line);
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "AlarmController.PushAlarm 内部错误 line={Line}", dto.Line);
            return StatusCode(500, new { code = 500, message = ex.Message });
        }
    }
}
