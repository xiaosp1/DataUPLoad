using Microsoft.Data.Sqlite;

namespace IntcoEdge.Db.Repository;

// =====================================================================
// 缺陷记录查询仓储（W-A5 / 2 + 3）
//
// 设计要点：
//   1. SQL 拼接时严格用参数化（lineNo / faceNo / defectType / time），
//      杜绝 SQL 注入（哪怕是内网环境也别留口子）。
//   2. 分页走 LIMIT + OFFSET；OFFSET 在大表上性能差，但当前阶段
//      单产线单日量在 1w 量级以下，可接受；后续上 10w+ 再换 keyset。
//   3. 统计聚合在 SQL 里 GROUP BY，不要 C# 里再循环累加。
//   4. 时间参数直接拼字符串（已经是 SQL 格式），但用 ? 参数化占位符，
//      避免引号嵌套陷阱。
// =====================================================================

/// <summary>
/// 分页 + 筛选查询请求参数（仓储层 POJO，不含 DTO 序列化属性）。
/// </summary>
public record class DefectQueryParams(
    string StartTime,
    string EndTime,
    string? LineNo,
    string? FaceNo,
    string? DefectType,
    int Page,
    int PageSize);

/// <summary>
/// 分页 + 统计的查询结果。
/// </summary>
/// <param name="Total">时间范围内（忽略分页）的总行数。</param>
/// <param name="TotalCount">时间范围内的总检测数（= Total 因为 defect_record 一行一次检测）。</param>
/// <param name="NgCount">时间范围内的次品数（result=2）。</param>
/// <param name="Distribution">按 defect_type.name 分组的次品计数（只统计次品）。</param>
/// <param name="Rows">当前页的行数据。</param>
public record class DefectQueryResult(
    int Total,
    int TotalCount,
    int NgCount,
    IReadOnlyList<DefectTypeCount> Distribution,
    IReadOnlyList<DefectQueryRow> Rows);

public record class DefectTypeCount(string Type, int Count);

/// <summary>
/// defect_record 表的一行（仓储层内部 POJO）。
/// 命名用 DefectQueryRow 避免和 W-A4 的 DefectRecordRow 冲突。
/// </summary>
public record class DefectQueryRow(
    long Id,
    string LineNo,
    string FaceNo,
    string GloveNo,
    int Result,
    string DefectType,
    string ImgList,
    string Time,
    int ExceptFlag);

/// <summary>
/// 产线当日统计的查询结果（W-A5 / 3）。
/// </summary>
/// <param name="Total">当日累计检测总数。</param>
/// <param name="Right">当日良品数。</param>
/// <param name="Ng">当日次品数。</param>
/// <param name="Top5">当日缺陷类型 Top5（按 count DESC 截前 5）。</param>
/// <param name="Timeline">24 小时时间轴（按时钟小时聚合 total/ng）。</param>
public record class LineDayStatistic(
    int Total,
    int Right,
    int Ng,
    IReadOnlyList<DefectTypeCount> Top5,
    IReadOnlyList<HourBucket> Timeline);

public record class HourBucket(string Hour, int Total, int Ng);

public interface IDefectQueryRepository
{
    /// <summary>分页 + 统计一次搞定（两条 SQL：分页行 + 全量统计）。</summary>
    DefectQueryResult Query(DefectQueryParams p);

    /// <summary>产线当日统计（line_day_record + defect_record 双表聚合）。</summary>
    /// <param name="lineNo">产线编号。</param>
    /// <param name="dayPrefix">当日日期前缀，格式 `yyyy-MM-dd`（用于 LIKE 'yyyy-MM-dd%'）。</param>
    LineDayStatistic QueryLineDay(string lineNo, string dayPrefix);
}

public class DefectQueryRepository : IDefectQueryRepository
{
    private readonly SqliteConnectionFactory _factory;

    public DefectQueryRepository(SqliteConnectionFactory factory)
    {
        _factory = factory ?? throw new ArgumentNullException(nameof(factory));
    }

    public DefectQueryResult Query(DefectQueryParams p)
    {
        if (p is null) throw new ArgumentNullException(nameof(p));
        if (string.IsNullOrWhiteSpace(p.StartTime) || string.IsNullOrWhiteSpace(p.EndTime))
        {
            throw new ArgumentException("startTime / endTime 必填");
        }
        if (p.Page < 1) throw new ArgumentException("page 必须 >= 1");
        if (p.PageSize < 1) throw new ArgumentException("pageSize 必须 >= 1");

        // DB 不存在 → 返回空集（与 DictionaryRepository 保持一致）。
        // 首次启动 / migration runner 还没跑的场景不能抛 500。
        SqliteConnection conn;
        try
        {
            conn = _factory.OpenReadOnly();
        }
        catch (InvalidOperationException)
        {
            return new DefectQueryResult(Total: 0, TotalCount: 0, NgCount: 0,
                Distribution: Array.Empty<DefectTypeCount>(), Rows: Array.Empty<DefectQueryRow>());
        }

        using (conn)
        {
            return QueryCore(conn, p);
        }
    }

    private static DefectQueryResult QueryCore(SqliteConnection conn, DefectQueryParams p)
    {
        var (where, parameters) = BuildWhere(p);

        int total;
        int totalCount;
        int ngCount;
        List<DefectTypeCount> distribution;
        List<DefectQueryRow> rows;

        // ---- 1. 总数 + 总检测 + 次品数（一条聚合 SQL 解决）----
        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = $@"
SELECT COUNT(*) AS total,
       SUM(CASE WHEN result IN (1,2) THEN 1 ELSE 0 END) AS total_count,
       SUM(CASE WHEN result = 2 THEN 1 ELSE 0 END) AS ng_count
FROM defect_record
{where}";
            foreach (var (k, v) in parameters) cmd.Parameters.AddWithValue(k, v);
            using var rd = cmd.ExecuteReader();
            rd.Read();
            total = SafeInt(rd.GetValue(0));
            totalCount = SafeInt(rd.GetValue(1));
            ngCount = SafeInt(rd.GetValue(2));
        }

        // ---- 2. 缺陷类型分布（按 name GROUP BY，只算次品）----
        distribution = new List<DefectTypeCount>();
        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = $@"
SELECT defect_type, COUNT(*) AS cnt
FROM defect_record
{where} AND result = 2
GROUP BY defect_type
ORDER BY cnt DESC, defect_type ASC";
            foreach (var (k, v) in parameters) cmd.Parameters.AddWithValue(k, v);
            using var rd = cmd.ExecuteReader();
            while (rd.Read())
            {
                distribution.Add(new DefectTypeCount(
                    Type: rd.GetString(0),
                    Count: rd.GetInt32(1)
                ));
            }
        }

        // ---- 3. 当前页行数据（按 time DESC, id DESC）----
        rows = new List<DefectQueryRow>();
        var offset = (p.Page - 1) * p.PageSize;
        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = $@"
SELECT id, line_no, face_no, glove_no, result, defect_type, img_list, ""time"", except_flag
FROM defect_record
{where}
ORDER BY ""time"" DESC, id DESC
LIMIT $limit OFFSET $offset";
            foreach (var (k, v) in parameters) cmd.Parameters.AddWithValue(k, v);
            cmd.Parameters.AddWithValue("$limit", p.PageSize);
            cmd.Parameters.AddWithValue("$offset", offset);
            using var rd = cmd.ExecuteReader();
            while (rd.Read())
            {
                rows.Add(new DefectQueryRow(
                    Id: rd.GetInt64(0),
                    LineNo: rd.GetString(1),
                    FaceNo: rd.GetString(2),
                    GloveNo: rd.GetString(3),
                    Result: rd.GetInt32(4),
                    DefectType: rd.GetString(5),
                    ImgList: rd.GetString(6),
                    Time: rd.GetString(7),
                    ExceptFlag: rd.GetInt32(8)
                ));
            }
        }

        return new DefectQueryResult(total, totalCount, ngCount, distribution, rows);
    }

    public LineDayStatistic QueryLineDay(string lineNo, string dayPrefix)
    {
        if (string.IsNullOrWhiteSpace(lineNo))
        {
            throw new ArgumentException("lineNo 必填", nameof(lineNo));
        }
        if (string.IsNullOrWhiteSpace(dayPrefix))
        {
            throw new ArgumentException("dayPrefix 必填", nameof(dayPrefix));
        }

        SqliteConnection conn;
        try
        {
            conn = _factory.OpenReadOnly();
        }
        catch (InvalidOperationException)
        {
            return new LineDayStatistic(Total: 0, Right: 0, Ng: 0,
                Top5: Array.Empty<DefectTypeCount>(), Timeline: Array.Empty<HourBucket>());
        }

        using (conn)
        {
            return QueryLineDayCore(conn, lineNo, dayPrefix);
        }
    }

    private static LineDayStatistic QueryLineDayCore(SqliteConnection conn, string lineNo, string dayPrefix)
    {
        int total = 0, right = 0, ng = 0;
        List<DefectTypeCount> top5;
        List<HourBucket> timeline;

        // ---- 1. line_day_record 聚合：right_count + error_count ----
        // line_day_record 已经按 (line_no, time) 维度存好了 right/error，
        // 直接 SUM 即可；遇到 line_no 不存在 → 0 行，返回空集（total=0）。
        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = @"
SELECT COALESCE(SUM(right_count), 0),
       COALESCE(SUM(error_count), 0)
FROM line_day_record
WHERE line_no = $lineNo
  AND ""time"" LIKE $dayPattern";
            cmd.Parameters.AddWithValue("$lineNo", lineNo);
            cmd.Parameters.AddWithValue("$dayPattern", dayPrefix + "%");
            using var rd = cmd.ExecuteReader();
            if (rd.Read())
            {
                right = rd.GetInt32(0);
                ng = rd.GetInt32(1);
            }
        }
        total = right + ng;

        // ---- 2. defect_record 聚合 Top5 + 24h 时间轴 ----
        // 用 LEFT JOIN 兜底：line_day_record 没数据时，defect_record 还能出 Top5 / 时间轴。
        top5 = new List<DefectTypeCount>();
        using (var cmd = conn.CreateCommand())
        {
            cmd.CommandText = @"
SELECT defect_type, COUNT(*) AS cnt
FROM defect_record
WHERE line_no = $lineNo
  AND ""time"" LIKE $dayPattern
  AND result = 2
GROUP BY defect_type
ORDER BY cnt DESC, defect_type ASC
LIMIT 5";
            cmd.Parameters.AddWithValue("$lineNo", lineNo);
            cmd.Parameters.AddWithValue("$dayPattern", dayPrefix + "%");
            using var rd = cmd.ExecuteReader();
            while (rd.Read())
            {
                top5.Add(new DefectTypeCount(rd.GetString(0), rd.GetInt32(1)));
            }
        }

        timeline = new List<HourBucket>();
        using (var cmd = conn.CreateCommand())
        {
            // SQLite 用 strftime 把 'yyyy-MM-dd HH:mm:ss' 切成 'yyyy-MM-dd HH' 小时桶。
            cmd.CommandText = @"
SELECT strftime('%Y-%m-%d %H:00:00', ""time"") AS hour,
       COUNT(*) AS total,
       SUM(CASE WHEN result = 2 THEN 1 ELSE 0 END) AS ng
FROM defect_record
WHERE line_no = $lineNo
  AND ""time"" LIKE $dayPattern
GROUP BY hour
ORDER BY hour ASC";
            cmd.Parameters.AddWithValue("$lineNo", lineNo);
            cmd.Parameters.AddWithValue("$dayPattern", dayPrefix + "%");
            using var rd = cmd.ExecuteReader();
            while (rd.Read())
            {
                // hour 可能为 null（空表），过滤掉。
                if (rd.IsDBNull(0)) continue;
                timeline.Add(new HourBucket(
                    Hour: rd.GetString(0),
                    Total: rd.GetInt32(1),
                    Ng: SafeInt(rd.GetValue(2))
                ));
            }
        }

        return new LineDayStatistic(total, right, ng, top5, timeline);
    }

    // ---------- 私有工具 ----------

    /// <summary>
    /// 根据参数拼 WHERE 子句（不含 AND 前缀）+ 参数列表。
    /// 同时给 COUNT 聚合 + 分页查询复用，确保筛选条件一致。
    /// </summary>
    private static (string Where, List<(string Key, object Value)> Parameters) BuildWhere(DefectQueryParams p)
    {
        var clauses = new List<string> { @"""time"" >= $startTime", @"""time"" <= $endTime" };
        var parameters = new List<(string Key, object Value)>
        {
            ("$startTime", p.StartTime),
            ("$endTime", p.EndTime),
        };

        if (!string.IsNullOrWhiteSpace(p.LineNo))
        {
            clauses.Add("line_no = $lineNo");
            parameters.Add(("$lineNo", p.LineNo));
        }
        if (!string.IsNullOrWhiteSpace(p.FaceNo))
        {
            clauses.Add("face_no = $faceNo");
            parameters.Add(("$faceNo", p.FaceNo));
        }
        if (!string.IsNullOrWhiteSpace(p.DefectType))
        {
            clauses.Add("defect_type = $defectType");
            parameters.Add(("$defectType", p.DefectType));
        }

        return ("WHERE " + string.Join(" AND ", clauses), parameters);
    }

    /// <summary>SQLite COUNT/SUM 在空表时返回 NULL（DBNull），安全转 0。</summary>
    private static int SafeInt(object? v) => v == null || v == DBNull.Value ? 0 : Convert.ToInt32(v);
}
