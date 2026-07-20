using IntcoEdge.EdgeHost.Models;
using IntcoEdge.EdgeHost.Services;
using Microsoft.AspNetCore.Mvc;

namespace IntcoEdge.EdgeHost.Controllers;

/// <summary>
/// 英科网关推缺陷记录到 PSM 的入口 + B1 大屏查询接口。
///
/// 历史端点（W-A3，W-A5 完全不动）：
///   - POST /client/yk/defect-record
///   - POST /client/yk/defect-records
///
/// W-A5 新增端点：
///   - GET  /api/dict/defect-type
///   - GET  /api/dict/defect-group
///   - GET  /api/dict/face-group
///   - POST /api/defect/query
///   - GET  /api/line/statistic?lineNo=...
/// </summary>
[ApiController]
public class DefectController : ControllerBase
{
    private readonly ILogger<DefectController> _logger;
    private readonly IDictionaryService _dictService;
    private readonly IDefectQueryService _defectQueryService;

    public DefectController(
        ILogger<DefectController> logger,
        IDictionaryService dictService,
        IDefectQueryService defectQueryService)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _dictService = dictService ?? throw new ArgumentNullException(nameof(dictService));
        _defectQueryService = defectQueryService ?? throw new ArgumentNullException(nameof(defectQueryService));
    }

    // ===================================================================
    // 历史端点（W-A3 写入，W-A5 完全不动）
    // ===================================================================

    /// <summary>英科网关推送单条缺陷记录。</summary>
    [HttpPost("client/yk/defect-record")]
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

        // TODO(W-A4): 真正写入 SQLite defect_record 表。
        // INSERT INTO defect_record (line_no, face_no, glove_no, result, defect_type, img_list, time)
        //   VALUES (@lineNo, @faceNo, @gloveNo, @result, @defectType, @imgList, @time);

        return Ok(new { code = 0, message = "ok", data = new { id = data.Id } });
    }

    /// <summary>英科网关批量推送缺陷记录。</summary>
    [HttpPost("client/yk/defect-records")]
    public IActionResult PushDefects([FromBody] List<DefectRecordDto>? records, CancellationToken ct)
    {
        if (records == null || records.Count == 0)
        {
            return BadRequest(new { code = 400, message = "records 不能为空" });
        }

        _logger.LogInformation("DefectController.PushDefects 收到 {Count} 条", records.Count);

        // TODO(W-A4): 批量 INSERT，用事务。

        return Ok(new { code = 0, message = "ok", data = new { count = records.Count } });
    }

    // ===================================================================
    // W-A5 新增：字典查询
    // ===================================================================

    /// <summary>缺陷类型字典（来自 SQLite defect_type 表）。</summary>
    [HttpGet("api/dict/defect-type")]
    public IActionResult GetDefectTypeDict()
    {
        try
        {
            var data = _dictService.GetDefectTypes();
            return Ok(new { code = 0, data });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "GetDefectTypeDict 失败");
            return StatusCode(500, new { code = 500, message = "字典查询失败：" + ex.Message });
        }
    }

    /// <summary>缺陷分组字典（硬编码 4 组）。</summary>
    [HttpGet("api/dict/defect-group")]
    public IActionResult GetDefectGroupDict()
    {
        return Ok(new { code = 0, data = _dictService.GetDefectGroups() });
    }

    /// <summary>面别字典（硬编码 A/B 面）。</summary>
    [HttpGet("api/dict/face-group")]
    public IActionResult GetFaceGroupDict()
    {
        return Ok(new { code = 0, data = _dictService.GetFaceGroups() });
    }

    // ===================================================================
    // W-A5 新增：缺陷记录分页查询 + 统计
    // ===================================================================

    /// <summary>
    /// 缺陷记录分页查询 + 统计（POST /api/defect/query）。
    ///
    /// 错误约定：
    ///   - 参数错误（必填缺失、时间格式错） → 400
    ///   - 找不到数据 → 200, total=0, rows=[]（不算错）
    ///   - SQL 异常 → 500
    /// </summary>
    [HttpPost("api/defect/query")]
    public IActionResult QueryDefects([FromBody] DefectQueryRequest? req, CancellationToken ct)
    {
        if (req == null)
        {
            return BadRequest(new { code = 400, message = "body 不能为空" });
        }

        try
        {
            var data = _defectQueryService.Query(req);
            return Ok(new { code = 0, data });
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "QueryDefects 参数错误");
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "QueryDefects 失败");
            return StatusCode(500, new { code = 500, message = "查询失败：" + ex.Message });
        }
    }

    // ===================================================================
    // W-A5 新增：产线当日统计
    // ===================================================================

    /// <summary>
    /// 产线当日统计（GET /api/line/statistic?lineNo=A1）。
    ///
    /// 错误约定：
    ///   - lineNo 缺失 → 400
    ///   - 找不到产线（DB 无记录）→ 200, total=0, ng=0, ngRate=0
    ///   - SQL 异常 → 500
    /// </summary>
    [HttpGet("api/line/statistic")]
    public IActionResult GetLineStatistic([FromQuery] string? lineNo, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(lineNo))
        {
            return BadRequest(new { code = 400, message = "lineNo 必填" });
        }

        try
        {
            var data = _defectQueryService.GetLineDayStatistic(lineNo);
            return Ok(new { code = 0, data });
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "GetLineStatistic 参数错误");
            return BadRequest(new { code = 400, message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "GetLineStatistic 失败");
            return StatusCode(500, new { code = 500, message = "查询失败：" + ex.Message });
        }
    }
}
