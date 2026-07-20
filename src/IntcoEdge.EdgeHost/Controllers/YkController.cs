using IntcoEdge.EdgeHost.Clients;
using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 英科网关相关入口（路径前缀 `/client/yk/`，与现场老 PSM 兼容）。
/// 反编译参考：`com.hikrobotics.solution.module.yingke.web.YKController`。
///
/// ★ 协议对齐（按权威 docx）：
///   - POST /client/yk/login         → 触发英科登录，刷新内部 ticket 缓存
///   - GET  /client/yk/line-defect   → 拉产线-缺陷字典（占位，待 PSM 字典接口对接）
///   - POST /client/yk/defect-query  → 拉缺陷数据（占位：当前 Yingke 网关未提供该 ApiType，
///                                     留接口给 W-A5 调 PSM `searchDefectRecord`）
/// </summary>
[ApiController]
[Route("client/yk")]
public class YkController : ControllerBase
{
    private readonly IYingkeService _ykService;
    private readonly YingkeGatewayOptions _options;
    private readonly ILogger<YkController> _logger;

    public YkController(
        IYingkeService ykService,
        IOptions<YingkeGatewayOptions> options,
        ILogger<YkController> logger)
    {
        _ykService = ykService ?? throw new ArgumentNullException(nameof(ykService));
        _options = options?.Value ?? throw new ArgumentNullException(nameof(options));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// 英科网关登录（POST），返回 LoginResult 用户信息（ticket 内部缓存）。
    /// 调用后会刷新 ticket 缓存。
    /// </summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login(CancellationToken ct)
    {
        var login = await _ykService.LoginAsync(ct).ConfigureAwait(false);
        if (login == null)
        {
            _logger.LogWarning("YkController.Login 失败");
            return StatusCode(502, new { code = 502, message = "英科网关登录失败" });
        }

        return Ok(new
        {
            code = 0,
            message = "ok",
            data = new
            {
                userId = login.UserId,
                employeeId = login.EmployeeId,
                userCode = login.UserCode,
                userName = login.UserName,
                invOrg = login.InvOrg,
                workshopCode = _options.WorkshopCode,
            }
        });
    }

    /// <summary>英科网关产线-缺陷字典查询（GET）。</summary>
    [HttpGet("line-defect")]
    public async Task<IActionResult> GetLineDefect([FromQuery] string? workshopCode, CancellationToken ct)
    {
        var code = string.IsNullOrWhiteSpace(workshopCode) ? _options.WorkshopCode : workshopCode;
        var dict = await _ykService.GetLineDefectDictionaryAsync(code, ct).ConfigureAwait(false);
        return Ok(new { code = 0, message = "ok", data = dict });
    }

    /// <summary>
    /// 英科网关缺陷记录查询（POST，body = <see cref="SearchDefectRecordDto"/>）。
    /// ⚠️ 当前实现：Yingke 网关未提供缺陷查询 ApiType，本接口返回说明。
    /// 真正对接 PSM 由 W-A5 走 `/client/yk/defect-record`。
    /// </summary>
    [HttpPost("defect-query")]
    public IActionResult QueryDefect([FromBody] SearchDefectRecordDto? query, CancellationToken ct)
    {
        if (query == null)
        {
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }
        if (string.IsNullOrWhiteSpace(query.StartTime))
        {
            return BadRequest(new { code = 400, message = "startTime 必填" });
        }

        // ★ YKServiceImpl 反编译确认：Yingke 网关目前只有 Login + HandleVisualInspectionAlarm 两个 ApiType，
        // 没有缺陷查询接口。这里返回 501 说明，等 W-A5 对接 PSM `searchDefectRecord` 接管。
        _logger.LogInformation(
            "YkController.QueryDefect startTime={Start} lindGroup={LindCount} defectGroup={DefectCount} faceGroup={FaceCount}",
            query.StartTime,
            query.LindGroup?.Count ?? 0,
            query.DefectGroup?.Count ?? 0,
            query.FaceGroup?.Count ?? 0);

        return StatusCode(501, new
        {
            code = 501,
            message = "英科网关未暴露缺陷查询 ApiType，请改调 /client/yk/defect-record（PSM searchDefectRecord）"
        });
    }
}
