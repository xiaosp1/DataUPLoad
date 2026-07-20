using IntcoEdge.EdgeHost.Models;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 英科网关推缺陷记录到 PSM 的入口。
/// 路径：`POST /client/yk/defect-record`，请求体 = `DefectRecordDto`（或 List）。
/// </summary>
[ApiController]
[Route("client/yk")]
public class DefectController : ControllerBase
{
    private readonly ILogger<DefectController> _logger;

    public DefectController(ILogger<DefectController> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>英科网关推送单条缺陷记录。</summary>
    [HttpPost("defect-record")]
    public IActionResult PushDefect([FromBody] DefectRecordDto? data, CancellationToken ct)
    {
        if (data == null)
        {
            _logger.LogWarning("DefectController.PushDefect 收到空 body");
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        _logger.LogInformation(
            "DefectController.PushDefect lineNo={LineNo} faceNo={FaceNo} gloveNo={Glove} result={Result} type={Type}",
            data.LineNo, data.FaceNo, data.GloveNo, data.Result, data.DefectType);

        // TODO(W-A5): 真正写入 SQLite defect_record 表。
        // INSERT INTO defect_record (line_no, face_no, glove_no, result, defect_type, img_list, time)
        //   VALUES (@lineNo, @faceNo, @gloveNo, @result, @defectType, @imgList, @time);

        return Ok(new { code = 0, message = "ok", data = new { id = data.Id } });
    }

    /// <summary>英科网关批量推送缺陷记录。</summary>
    [HttpPost("defect-records")]
    public IActionResult PushDefects([FromBody] List<DefectRecordDto>? records, CancellationToken ct)
    {
        if (records == null || records.Count == 0)
        {
            return BadRequest(new { code = 400, message = "records 不能为空" });
        }

        _logger.LogInformation("DefectController.PushDefects 收到 {Count} 条", records.Count);

        // TODO(W-A5): 批量 INSERT，用事务。

        return Ok(new { code = 0, message = "ok", data = new { count = records.Count } });
    }
}
