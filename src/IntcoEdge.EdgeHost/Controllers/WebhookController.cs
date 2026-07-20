using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// Webhook 报警接入（W-A7-S）。
///
/// 路由前缀：`/api/webhook`
/// 端点：
///   - POST /api/webhook/alarm   接 PSM 老格式 / 车间三方报警推送，转内部 AlarmInputDto 后
///                               调 IAlarmService.HandleAlarmAsync(AlarmInputDto)（W-A7-M 负责）。
///   - GET  /api/webhook/health  返回 ok。用于车间其它系统测试连通。
///
/// 边界说明：
///   - 本 Controller 不写 SQLite、不直推英科网关。所有业务逻辑走 IAlarmService。
///   - AlarmWebhookDto → AlarmInputDto 的字段映射 + 校验在 AlarmWebhookDto 内做。
///   - 校验失败（400）和运行期错误（500）都按统一外壳返 JSON：{code, message, data?}。
/// </summary>
[ApiController]
[Route("api/webhook")]
public class WebhookController : ControllerBase
{
    private readonly IAlarmService _alarmService;
    private readonly ILogger<WebhookController> _logger;

    public WebhookController(IAlarmService alarmService, ILogger<WebhookController> logger)
    {
        _alarmService = alarmService ?? throw new ArgumentNullException(nameof(alarmService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// 接 PSM 老格式 / 车间三方报警 webhook。
    /// </summary>
    [HttpPost("alarm")]
    public async Task<IActionResult> PushAlarm(
        [FromBody] AlarmWebhookDto? payload,
        CancellationToken ct)
    {
        if (payload == null)
        {
            _logger.LogWarning("WebhookController.PushAlarm 收到空 body");
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        // 字段映射 + 校验
        AlarmInputDto input;
        try
        {
            input = payload.ToAlarmInputDto();
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(
                "WebhookController.PushAlarm 入参校验失败 lineNo={LineNo} faceNo={FaceNo}: {Msg}",
                payload.LineNo, payload.FaceNo, ex.Message);
            return BadRequest(new { code = 400, message = ex.Message });
        }

        // 调 W-A7-M 的服务（接口契约：HandleAlarmAsync(AlarmInputDto, CancellationToken)）
        try
        {
            var result = await _alarmService.HandleAlarmAsync(input, ct).ConfigureAwait(false);
            _logger.LogInformation(
                "WebhookController.PushAlarm 接收成功 alarmId={AlarmId} lineNo={LineNo} faceNo={FaceNo} sendStatus={SendStatus}",
                result.AlarmId, input.LineNo, input.FaceNo, result.SendStatus);
            return Ok(new { code = 0, message = "ok", data = result });
        }
        catch (ArgumentException ex)
        {
            // 服务层发现 lineNo/faceNo/time 缺失或 level/type 越界也按 400 返
            _logger.LogWarning(ex, "WebhookController.PushAlarm 服务层校验失败 lineNo={LineNo}", input.LineNo);
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "WebhookController.PushAlarm 内部错误 lineNo={LineNo}", input.LineNo);
            return StatusCode(500, new { code = 500, message = ex.Message });
        }
    }

    /// <summary>
    /// 健康检查端点。车间其它系统发 GET 看连通性。
    /// </summary>
    [HttpGet("health")]
    public IActionResult Health()
    {
        return Ok(new { code = 0, message = "ok" });
    }
}
