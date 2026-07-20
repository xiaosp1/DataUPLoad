using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

// =====================================================================
// 缺陷记录查询 DTO（W-A5 / 2）
// 请求体走 PSM 兼容字段命名风格（camelCase），响应体里嵌套 statistics。
// =====================================================================

/// <summary>
/// 缺陷记录查询请求体（POST /api/defect/query）。
///
/// 与 <see cref="SearchDefectRecordDto"/> 的差异：
///   - 那个是英科网关 PSM 兼容的"filter 集合"版本（lindGroup / defectGroup / faceGroup）。
///   - 这个是大屏 Web UI 用的"标量 + 分页"版本（lineNo / faceNo / defectType / page / pageSize）。
/// </summary>
public record class DefectQueryRequest
{
    /// <summary>起始时间，格式 `yyyy-MM-dd HH:mm:ss`（必填）。</summary>
    [JsonPropertyName("startTime")]
    public string? StartTime { get; init; }

    /// <summary>结束时间，格式 `yyyy-MM-dd HH:mm:ss`（必填）。</summary>
    [JsonPropertyName("endTime")]
    public string? EndTime { get; init; }

    /// <summary>产线编号（可选，不传=全部产线）。</summary>
    [JsonPropertyName("lineNo")]
    public string? LineNo { get; init; }

    /// <summary>面编号（可选）。</summary>
    [JsonPropertyName("faceNo")]
    public string? FaceNo { get; init; }

    /// <summary>缺陷类型（可选，与 defect_type.name 对齐）。</summary>
    [JsonPropertyName("defectType")]
    public string? DefectType { get; init; }

    /// <summary>页码，1-based，默认 1。</summary>
    [JsonPropertyName("page")]
    public int Page { get; init; } = 1;

    /// <summary>每页条数，默认 20，上限 200（防止一次拉太多压垮前端）。</summary>
    [JsonPropertyName("pageSize")]
    public int PageSize { get; init; } = 20;
}

/// <summary>
/// 缺陷记录行（响应体 rows 数组的元素）。
/// 字段命名对齐 SQLite `defect_record` 表的列名（snake_case → camelCase）。
/// </summary>
public record class DefectRecordRowDto
{
    [JsonPropertyName("id")]
    public long Id { get; init; }

    [JsonPropertyName("lineNo")]
    public string LineNo { get; init; } = string.Empty;

    [JsonPropertyName("faceNo")]
    public string FaceNo { get; init; } = string.Empty;

    [JsonPropertyName("gloveNo")]
    public string GloveNo { get; init; } = string.Empty;

    /// <summary>检测结果：1=良品 / 2=次品。</summary>
    [JsonPropertyName("result")]
    public int Result { get; init; }

    /// <summary>缺陷类型名称（与 defect_type.name 对齐）。</summary>
    [JsonPropertyName("defectType")]
    public string DefectType { get; init; } = string.Empty;

    /// <summary>缺陷图片路径列表（原始字符串，由前端按需解析）。</summary>
    [JsonPropertyName("imgList")]
    public string ImgList { get; init; } = string.Empty;

    /// <summary>检测时间，格式 `yyyy-MM-dd HH:mm:ss`。</summary>
    [JsonPropertyName("time")]
    public string Time { get; init; } = string.Empty;

    /// <summary>是否被剔除（DB defect_record.except_flag，0/1）。</summary>
    [JsonPropertyName("exceptFlag")]
    public int ExceptFlag { get; init; }
}

/// <summary>
/// 缺陷类型分布条目（statistics.defectTypeDistribution 数组元素）。
/// </summary>
public record class DefectTypeStatDto
{
    [JsonPropertyName("type")]
    public string Type { get; init; } = string.Empty;

    [JsonPropertyName("count")]
    public int Count { get; init; }
}

/// <summary>
/// 缺陷记录查询的统计信息（响应体 statistics 字段）。
/// </summary>
public record class DefectQueryStatistics
{
    /// <summary>时间范围内的总检测数（result=1 良品 + result=2 次品）。</summary>
    [JsonPropertyName("totalCount")]
    public int TotalCount { get; init; }

    /// <summary>次品数（result=2）。</summary>
    [JsonPropertyName("ngCount")]
    public int NgCount { get; init; }

    /// <summary>不良率（0~1，比如 0.0123 = 1.23%），前端再乘 100 显示。</summary>
    [JsonPropertyName("ngRate")]
    public double NgRate { get; init; }

    /// <summary>缺陷类型分布（按 defect_type.name 分组计数）。</summary>
    [JsonPropertyName("defectTypeDistribution")]
    public List<DefectTypeStatDto> DefectTypeDistribution { get; init; } = new();
}

/// <summary>
/// 缺陷记录查询响应体（POST /api/defect/query 返回的 data 字段）。
/// </summary>
public record class DefectQueryResponse
{
    /// <summary>总记录数（忽略分页，用于前端显示 "共 N 条"）。</summary>
    [JsonPropertyName("total")]
    public int Total { get; init; }

    /// <summary>当前页的数据行。</summary>
    [JsonPropertyName("rows")]
    public List<DefectRecordRowDto> Rows { get; init; } = new();

    /// <summary>统计信息（基于当前筛选条件下的全量聚合，不是只算当前页）。</summary>
    [JsonPropertyName("statistics")]
    public DefectQueryStatistics Statistics { get; init; } = new();
}
