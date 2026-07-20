using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Models;

namespace IntcoEdge.EdgeHost.Services;

/// <summary>
/// 缺陷计数 → 缺陷记录仓储入参转换（W-A7-Bug 修复）。
///
/// 背景：
///   - DetectDataDto.todayData.defects 和 realTimeData.defects 是 DefectCountDto 列表，
///     每个 DefectCountDto 表示"某种类型在某区间的缺陷数量"（PSM DefectCountDTO 反编译）。
///   - defect_record 表存的是"单条手套级"缺陷记录（PSM DefectRecordPO）。
///   - 两者粒度不同：Detec[DefectCountDto.type] 有 count 个缺陷，需要展开成 count 条 defect_record。
///   - 每条展开的 record 用 "{lineNo}-{faceNo}-{time}-{type}-{idx}" 当 gloveNo
///     （保 defect_record 不漏 NOT NULL 字段；同时没真实手套编号时用合成键标识）。
///
/// 幂等：gloveNo 用 deterministic 合成键 → 重复推送同 batch 同 count 时 INSERT 撞唯一冲突，
/// 但 SQLite 没有 gloveNo UNIQUE INDEX，目前是直接 INSERT 重复。W-A4 注释里说"不做幂等，
/// 保留原始事件流"，所以这里保留重复。
/// </summary>
public interface IDefectConversion
{
    /// <summary>把 DefectCountDto 列表展开成 defect_record 入参列表（按 count 展开）。</summary>
    List<DefectRecordInput> FromDetectData(
        string lineNo,
        string faceNo,
        string statisticTime,
        IEnumerable<DefectCountDto> counts);
}

public class DefectConversion : IDefectConversion
{
    private readonly ILogger<DefectConversion> _logger;

    public DefectConversion(ILogger<DefectConversion> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public List<DefectRecordInput> FromDetectData(
        string lineNo,
        string faceNo,
        string statisticTime,
        IEnumerable<DefectCountDto> counts)
    {
        var result = new List<DefectRecordInput>();
        if (counts == null) return result;

        foreach (var c in counts)
        {
            if (c == null || string.IsNullOrWhiteSpace(c.Type)) continue;
            var cnt = c.Count ?? 0;
            if (cnt <= 0) continue;

            var time = string.IsNullOrWhiteSpace(c.Time) ? statisticTime : c.Time!;

            for (var i = 0; i < cnt; i++)
            {
                result.Add(new DefectRecordInput(
                    LineNo: lineNo,
                    FaceNo: faceNo,
                    GloveNo: $"{lineNo}-{faceNo}-{time}-{c.Type}-{i}",
                    Result: 2,           // 缺陷 → 2=次品
                    DefectType: c.Type!,
                    ImgList: string.Empty,
                    Time: time));
            }
        }

        return result;
    }
}