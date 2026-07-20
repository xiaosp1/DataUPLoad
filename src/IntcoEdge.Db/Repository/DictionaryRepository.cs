using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db.Repository;

/// <summary>
/// 字典查询仓储：从 SQLite `defect_type` 表读数据。
///
/// 设计要点：
///   - 单一职责：只负责 "读"，写库由 W-A2 migration 或后续管理接口负责。
///   - 异常透传：仓储层不吞异常，让 Service 层决定如何转换（log + 返空 vs HTTP 500）。
///   - 连接管理：每次查询拿一个 SqliteConnection，用完即释放（不缓存，避免长连接锁）。
///
/// 线程模型：
///   - 仓储方法同步执行，调用方（Service 层）按需包 Task.Run 或 async 装饰。
///   - SQLite 读并发 OK，写串行（但字典表基本只读，无并发压力）。
/// </summary>
public interface IDictionaryRepository
{
    /// <summary>
    /// 读取所有缺陷类型（按 category, id 排序）。
    /// DB 文件不存在或表为空时返回空列表，不抛异常。
    /// </summary>
    IReadOnlyList<DefectTypeRow> GetAllDefectTypes();
}

/// <summary>
/// defect_type 表的行（仓储层内部结构，不直接暴露给 Controller）。
/// </summary>
public record class DefectTypeRow(
    long Id,
    string Name,
    int Category,
    int CountEnable,
    int CountThreshold,
    int RateEnable,
    int SendYkEnable,
    int AlarmEnable,
    int SoundEnable,
    int ShowImgEnable);

public class DictionaryRepository : IDictionaryRepository
{
    private readonly SqliteConnectionFactory _factory;

    public DictionaryRepository(SqliteConnectionFactory factory)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
    }

    public IReadOnlyList<DefectTypeRow> GetAllDefectTypes()
    {
        var result = new List<DefectTypeRow>();
        SqliteConnection conn;
        try
        {
            conn = _factory.OpenReadOnly();
        }
        catch (InvalidOperationException)
        {
            // DB 文件不存在（首次启动 / 还没跑 migration runner）：返回空集而非 500。
            return result;
        }

        using (conn)
        {
            using var cmd = conn.CreateCommand();
            cmd.CommandText = @"
SELECT id, name, category,
       count_enable, count_threshold, rate_enable,
       send_yk_enable, alarm_enable, sound_enable, show_img_enable
FROM defect_type
ORDER BY category ASC, id ASC";
            using var rd = cmd.ExecuteReader();
            while (rd.Read())
            {
                result.Add(new DefectTypeRow(
                    Id: rd.GetInt64(0),
                    Name: rd.GetString(1),
                    Category: rd.GetInt32(2),
                    CountEnable: rd.GetInt32(3),
                    CountThreshold: rd.GetInt32(4),
                    RateEnable: rd.GetInt32(5),
                    SendYkEnable: rd.GetInt32(6),
                    AlarmEnable: rd.GetInt32(7),
                    SoundEnable: rd.GetInt32(8),
                    ShowImgEnable: rd.GetInt32(9)
                ));
            }
        }
        return result;
    }
}
