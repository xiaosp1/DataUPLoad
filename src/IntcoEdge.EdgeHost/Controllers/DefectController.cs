using IntcoEdge.Db.Repository;
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
    private readonly IDefectRecordRepository _defectRepo;  // PM 20:35: 修 W-A4 TODO

    public DefectController(
        ILogger<DefectController> logger,
        IDictionaryService dictService,
        IDefectQueryService defectQueryService,
        IDefectRecordRepository defectRepo)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _dictService = dictService ?? throw new ArgumentNullException(nameof(dictService));
        _defectQueryService = defectQueryService ?? throw new ArgumentNullException(nameof(defectQueryService));
        _defectRepo = defectRepo ?? throw new ArgumentNullException(nameof(defectRepo));
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

        // PM 20:35 bug fix: 真正写入 SQLite defect_record 表（之前 W-A4 留 TODO）
        var id = _defectRepo.Insert(new DefectRecordInput(
            LineNo: data.LineNo ?? string.Empty,
            FaceNo: data.FaceNo ?? string.Empty,
            GloveNo: data.GloveNo ?? $"yk-{data.Id ?? 0}",
            Result: data.Result ?? 2,
            DefectType: data.DefectType ?? string.Empty,
            ImgList: data.ImgList ?? string.Empty,
            Time: data.Time ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")));

        return Ok(new { code = 0, message = "ok", data = new { id } });
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

        // PM 20:35 bug fix: 真正批量写入 defect_record 表（之前 W-A4 留 TODO）
        var inputs = records.Select(r => new DefectRecordInput(
            LineNo: r.LineNo ?? string.Empty,
            FaceNo: r.FaceNo ?? string.Empty,
            GloveNo: r.GloveNo ?? $"yk-batch-{Guid.NewGuid():N}",
            Result: r.Result ?? 2,
            DefectType: r.DefectType ?? string.Empty,
            ImgList: r.ImgList ?? string.Empty,
            Time: r.Time ?? DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"))).ToList();

        var count = _defectRepo.InsertBatch(inputs);

        return Ok(new { code = 0, message = "ok", data = new { count } });
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
