using IntcoEdge.Db.Repository;
using IntcoEdge.EdgeHost.Models;
using Microsoft.Extensions.Logging;

namespace IntcoEdge.EdgeHost.Services;

// =====================================================================
// 字典查询服务（W-A5 / 1）
//   - 缺陷类型字典：来自 SQLite `defect_type` 表
//   - 缺陷分组 / 面别：暂用代码常量（DB 暂无表）
//   - Controller 只调本服务，不直接接触 Repository
// =====================================================================

public interface IDictionaryService
{
    /// <summary>缺陷类型字典（含 category 中文名映射）。</summary>
    IReadOnlyList<DefectTypeDictDto> GetDefectTypes();

    /// <summary>缺陷分组字典（硬编码 4 组）。</summary>
    IReadOnlyList<DefectGroupDictDto> GetDefectGroups();

    /// <summary>面别字典（硬编码 A/B 两面）。</summary>
    IReadOnlyList<FaceGroupDictDto> GetFaceGroups();
}

/// <summary>
/// 缺陷分组（硬编码）。后续若做后台可配置，需要迁到 defect_group 表，
/// 本接口的 Controller 调用方不受影响。
/// </summary>
public static class DefectGroups
{
    public const string Shape = "外形";
    public const string Spot = "黑点";
    public const string Stain = "污渍";
    public const string Hole = "破洞";
}

/// <summary>
/// 面别（硬编码）。后续若接入 line_defect_type / line 表后改成查表，
/// 但 IDictionaryService 接口保持稳定。
/// </summary>
public static class FaceGroups
{
    public const string A = "A";
    public const string B = "B";
}

public class DictionaryService : IDictionaryService
{
    private readonly IDictionaryRepository _repo;
    private readonly ILogger<DictionaryService> _logger;

    public DictionaryService(IDictionaryRepository repo, ILogger<DictionaryService> logger)
    {
        _repo = repo ?? throw new ArgumentNullException(nameof(repo));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public IReadOnlyList<DefectTypeDictDto> GetDefectTypes()
    {
        try
        {
            var rows = _repo.GetAllDefectTypes();
            var list = new List<DefectTypeDictDto>(rows.Count);
            foreach (var r in rows)
            {
                list.Add(new DefectTypeDictDto
                {
                    Id = r.Id,
                    Name = r.Name,
                    Category = r.Category,
                    CategoryName = MapCategoryName(r.Category),
                    CountEnable = r.CountEnable,
                    SendYkEnable = r.SendYkEnable,
                    AlarmEnable = r.AlarmEnable,
                    ShowImgEnable = r.ShowImgEnable,
                });
            }
            return list;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "DictionaryService.GetDefectTypes 失败");
            throw;
        }
    }

    public IReadOnlyList<DefectGroupDictDto> GetDefectGroups()
    {
        return new List<DefectGroupDictDto>
        {
            new() { Code = "shape", Name = DefectGroups.Shape },
            new() { Code = "spot",  Name = DefectGroups.Spot  },
            new() { Code = "stain", Name = DefectGroups.Stain },
            new() { Code = "hole",  Name = DefectGroups.Hole  },
        };
    }

    public IReadOnlyList<FaceGroupDictDto> GetFaceGroups()
    {
        return new List<FaceGroupDictDto>
        {
            new() { Code = FaceGroups.A, Name = "A面" },
            new() { Code = FaceGroups.B, Name = "B面" },
        };
    }

    /// <summary>
    /// defect_type.category 编号 → 中文名。
    /// 对应 PSM Java 端 DefectCategoryEnum：1=破损 2=脏污 3=其他（默认值）。
    /// </summary>
    private static string MapCategoryName(int category) => category switch
    {
        1 => "破损",
        2 => "脏污",
        _ => "其他",
    };
}
