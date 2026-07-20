using System.Globalization;
using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

// =====================================================================
// 缺陷记录查询服务（W-A5 / 2 + 3）
//   - 校验时间格式
//   - 解析 page/pageSize 上界
//   - 调仓储拼装 DTO
//   - 异常分类：参数错 → 抛 ArgumentException 让 Controller 返 400；
//               其它 → 抛原异常让 Controller 返 500。
// =====================================================================

public interface IDefectQueryService
{
    /// <summary>缺陷记录分页 + 统计。</summary>
    /// <exception cref="ArgumentException">参数错误（Controller 翻译为 400）。</exception>
    DefectQueryResponse Query(DefectQueryRequest req);

    /// <summary>产线当日统计。</summary>
    /// <exception cref="ArgumentException">参数错误。</exception>
    LineStatisticResponse GetLineDayStatistic(string lineNo);
}

public class DefectQueryService : IDefectQueryService
{
    /// <summary>默认每页条数（与 W-A5 任务单一致）。</summary>
    public const int DefaultPageSize = 20;

    /// <summary>每页条数上限（防止前端一次拉太多压垮 EdgeHost / SQLite）。</summary>
    public const int MaxPageSize = 200;

    /// <summary>PSM 时间字符串格式（与 PSM Java 端 HikDateUtil.transformTime 一致）。</summary>
    public const string TimeFormat = "yyyy-MM-dd HH:mm:ss";

    private readonly IDefectQueryRepository _repo;
    private readonly ILogger<DefectQueryService> _logger;

    public DefectQueryService(IDefectQueryRepository repo, ILogger<DefectQueryService> logger)
    {
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public DefectQueryResponse Query(DefectQueryRequest req)
    {
        if (req is null) throw new ArgumentNullException(nameof(req));

        // ---- 1. 必填参数校验 ----
        if (string.IsNullOrWhiteSpace(req.StartTime))
        {
            throw new ArgumentException("startTime 必填");
        }
        if (string.IsNullOrWhiteSpace(req.EndTime))
        {
            throw new ArgumentException("endTime 必填");
        }

        // ---- 2. 时间格式校验（PSM 风格 yyyy-MM-dd HH:mm:ss）----
        if (!TryParseTime(req.StartTime, out _))
        {
            throw new ArgumentException(
                $"startTime 格式错误：'{req.StartTime}'（期望 yyyy-MM-dd HH:mm:ss）");
        }
        if (!TryParseTime(req.EndTime, out _))
        {
            throw new ArgumentException(
                $"endTime 格式错误：'{req.EndTime}'（期望 yyyy-MM-dd HH:mm:ss）");
        }

        // ---- 3. 分页参数归一化 ----
        var page = req.Page < 1 ? 1 : req.Page;
        var pageSize = req.PageSize <= 0 ? DefaultPageSize
                     : req.PageSize > MaxPageSize ? MaxPageSize
                     : req.PageSize;

        var p = new DefectQueryParams(
            StartTime: req.StartTime,
            EndTime: req.EndTime,
            LineNo: NullIfEmpty(req.LineNo),
            FaceNo: NullIfEmpty(req.FaceNo),
            DefectType: NullIfEmpty(req.DefectType),
            Page: page,
            PageSize: pageSize);

        _logger.LogInformation(
            "DefectQueryService.Query startTime={Start} endTime={End} lineNo={Line} faceNo={Face} type={Type} page={Page}/{Size}",
            req.StartTime, req.EndTime, req.LineNo, req.FaceNo, req.DefectType, page, pageSize);

        // ---- 4. 调仓储 ----
        var result = _repo.Query(p);

        // ---- 5. 拼 DTO ----
        var rows = new List<DefectRecordRowDto>(result.Rows.Count);
        foreach (var r in result.Rows)
        {
            rows.Add(new DefectRecordRowDto
            {
                Id = r.Id,
                LineNo = r.LineNo,
                FaceNo = r.FaceNo,
                GloveNo = r.GloveNo,
                Result = r.Result,
                DefectType = r.DefectType,
                ImgList = r.ImgList,
                Time = r.Time,
                ExceptFlag = r.ExceptFlag,
            });
        }

        var dist = new List<DefectTypeStatDto>(result.Distribution.Count);
        foreach (var d in result.Distribution)
        {
            dist.Add(new DefectTypeStatDto { Type = d.Type, Count = d.Count });
        }

        var ngRate = result.TotalCount > 0
            ? Math.Round((double)result.NgCount / result.TotalCount, 4)
            : 0d;

        return new DefectQueryResponse
        {
            Total = result.Total,
            Rows = rows,
            Statistics = new DefectQueryStatistics
            {
                TotalCount = result.TotalCount,
                NgCount = result.NgCount,
                NgRate = ngRate,
                DefectTypeDistribution = dist,
            },
        };
    }

    public LineStatisticResponse GetLineDayStatistic(string lineNo)
    {
        if (string.IsNullOrWhiteSpace(lineNo))
        {
            throw new ArgumentException("lineNo 必填");
        }

        var today = DateTime.Now.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        _logger.LogInformation("DefectQueryService.GetLineDayStatistic lineNo={Line} today={Today}", lineNo, today);

        var stat = _repo.QueryLineDay(lineNo, today);

        var top5 = new List<DefectTopDto>(stat.Top5.Count);
        foreach (var t in stat.Top5)
        {
            top5.Add(new DefectTopDto { Type = t.Type, Count = t.Count });
        }

        var timeline = new List<LineTimelinePointDto>(stat.Timeline.Count);
        foreach (var b in stat.Timeline)
        {
            timeline.Add(new LineTimelinePointDto
            {
                Time = b.Hour,
                Total = b.Total,
                Ng = b.Ng,
            });
        }

        var ngRate = stat.Total > 0
            ? Math.Round((double)stat.Ng / stat.Total, 4)
            : 0d;

        return new LineStatisticResponse
        {
            LineNo = lineNo,
            Today = today,
            Total = stat.Total,
            Right = stat.Right,
            Ng = stat.Ng,
            NgRate = ngRate,
            DefectTypeTop5 = top5,
            Timeline = timeline,
        };
    }

    // ---------- 私有工具 ----------

    private static bool TryParseTime(string s, out DateTime _)
    {
        return DateTime.TryParseExact(
            s,
            TimeFormat,
            CultureInfo.InvariantCulture,
            DateTimeStyles.None,
            out _);
    }

    private static string? NullIfEmpty(string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s;
}
