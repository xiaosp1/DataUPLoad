using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 英科网关相关查询 / 登录入口。
/// 路径前缀：`/client/yk/`。
/// </summary>
[ApiController]
[Route("client/yk")]
public class YkController : ControllerBase
{
    private readonly IYingkeService _ykService;
    private readonly ILogger<YkController> _logger;

    public YkController(IYingkeService ykService, ILogger<YkController> logger)
    {
        _ykService = ykService ?? throw new ArgumentNullException(nameof(ykService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>英科网关登录（POST），返回 ticket 凭证（PSM 端约定：`LoginResult.UserId` 即 ticket）。</summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromQuery] string workshopCode, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(workshopCode))
        {
            return BadRequest(new { code = 400, message = "workshopCode 不能为空" });
        }

        var login = await _ykService.LoginAsync(workshopCode, ct).ConfigureAwait(false);
        if (login == null)
        {
            _logger.LogWarning("YkController.Login 失败 workshopCode={Workshop}", workshopCode);
            return StatusCode(502, new { code = 502, message = "英科网关登录失败" });
        }

        return Ok(new
        {
            code = 0,
            message = "ok",
            data = new
            {
                ticket = login.UserId,
                userCode = login.UserCode,
                userName = login.UserName,
                invOrg = login.InvOrg,
            }
        });
    }

    /// <summary>英科网关产线-缺陷字典查询（GET）。</summary>
    [HttpGet("line-defect")]
    public async Task<IActionResult> GetLineDefect([FromQuery] string workshopCode, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(workshopCode))
        {
            return BadRequest(new { code = 400, message = "workshopCode 不能为空" });
        }

        var dict = await _ykService.GetLineDefectDictionaryAsync(workshopCode, ct).ConfigureAwait(false);
        return Ok(new { code = 0, message = "ok", data = dict });
    }

    /// <summary>英科网关缺陷记录查询（POST，body = `SearchDefectRecordDto`）。</summary>
    [HttpPost("defect-query")]
    public async Task<IActionResult> QueryDefect([FromBody] SearchDefectRecordDto? query, CancellationToken ct)
    {
        if (query == null)
        {
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }
        if (string.IsNullOrWhiteSpace(query.StartTime))
        {
            return BadRequest(new { code = 400, message = "startTime 必填" });
        }

        var resp = await _ykService.QueryDefectAsync(query, ct).ConfigureAwait(false);
        if (resp == null)
        {
            return StatusCode(502, new { code = 502, message = "英科网关无响应" });
        }
        if (resp.Success != true)
        {
            return StatusCode(502, new { code = 502, message = resp.Message ?? "英科网关返回失败" });
        }

        return Ok(new { code = 0, message = "ok", data = resp.Result });
    }
}
