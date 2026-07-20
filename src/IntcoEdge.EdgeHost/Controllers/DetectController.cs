using IntcoEdge.Common;
using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 视觉软件推送检测数据的入口。
/// 路径：`POST /client/data/detect`，请求体 = `DetectDataDto`。
/// </summary>
[ApiController]
[Route("client/data")]
public class DetectController : ControllerBase
{
    private readonly ILineRecordService _lineService;
    private readonly ILogger<DetectController> _logger;

    public DetectController(ILineRecordService lineService, ILogger<DetectController> logger)
    {
        _lineService = lineService ?? throw new ArgumentNullException(nameof(lineService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>接收视觉软件推送的检测数据。</summary>
    [HttpPost("detect")]
    public async Task<IActionResult> PushDetect([FromBody] DetectDataDto? data, CancellationToken ct)
    {
        if (data == null)
        {
            _logger.LogWarning("DetectController.PushDetect 收到空 body");
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        try
        {
            var rows = await _lineService.HandleDetectDataAsync(data, ct).ConfigureAwait(false);
            _logger.LogInformation("DetectController.PushDetect 写入行数={Rows} lineNo={LineNo} faceNo={FaceNo}",
                rows, data.LineNo, data.FaceNo);
            return Ok(new { code = 0, message = "ok", data = new { rows } });
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "DetectController.PushDetect 参数校验失败");
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "DetectController.PushDetect 内部错误 lineNo={LineNo}", data.LineNo);
            return StatusCode(500, new { code = 500, message = ex.Message });
        }
    }

    /// <summary>接收视觉软件推送的报警数据（PSM 兼容路径）。</summary>
    [HttpPost("alarm")]
    public async Task<IActionResult> PushAlarm([FromBody] AlarmRecordDto? alarm, [FromServices] IAlarmService alarmService, CancellationToken ct)
    {
        if (alarm == null)
        {
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        try
        {
            var result = await alarmService.HandleAlarmAsync(alarm, ct).ConfigureAwait(false);
            return Ok(new { code = 0, message = "ok", data = result });
        }
        catch (ArgumentException ex)
        {
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "DetectController.PushAlarm 内部错误 uuid={Uuid}", alarm.Uuid);
            return StatusCode(500, new { code = 500, message = ex.Message });
        }
    }
}
