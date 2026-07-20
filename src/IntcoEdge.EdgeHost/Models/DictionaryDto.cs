using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

// =====================================================================
// 字典查询响应 DTO（W-A5 / 1）
// 三个端点共用一套结构：id + code + name + 扩展字段。
// 前端 (B1 大屏) 拿来当下拉选项 / 分类标签。
// =====================================================================

/// <summary>
/// 缺陷类型字典条目（来源：SQLite `defect_type` 表）。
/// </summary>
public record class DefectTypeDictDto
{
    /// <summary>主键 ID（DB defect_type.id）。</summary>
    [JsonPropertyName("id")]
    public long Id { get; init; }

    /// <summary>类型名称（如 "破洞"、"黑点"、"客户端" 等）。</summary>
    [JsonPropertyName("name")]
    public string Name { get; init; } = string.Empty;

    /// <summary>
    /// 类别编号（DB defect_type.category）：
    ///   1 = 破损
    ///   2 = 脏污
    ///   3 = 其他（默认值）
    /// 前端可用作一级分组标签。
    /// </summary>
    [JsonPropertyName("category")]
    public int Category { get; init; }

    /// <summary>
    /// 类别中文名（前端友好）。如果 DB 里没映射，就返回 "其他"。
    /// </summary>
    [JsonPropertyName("categoryName")]
    public string CategoryName { get; init; } = "其他";

    /// <summary>是否启用计数（DB defect_type.count_enable，0/1）。</summary>
    [JsonPropertyName("countEnable")]
    public int CountEnable { get; init; }

    /// <summary>是否上报英科（DB defect_type.send_yk_enable，0/1）。</summary>
    [JsonPropertyName("sendYkEnable")]
    public int SendYkEnable { get; init; }

    /// <summary>是否触发声光报警（DB defect_type.alarm_enable，0/1）。</summary>
    [JsonPropertyName("alarmEnable")]
    public int AlarmEnable { get; init; }

    /// <summary>是否在大屏显示图片（DB defect_type.show_img_enable，0/1）。</summary>
    [JsonPropertyName("showImgEnable")]
    public int ShowImgEnable { get; init; }
}

/// <summary>
/// 缺陷分组条目（按业务归类，比如 "外形"、"黑点"、"污渍"、"破洞"）。
/// 目前 DB 没表，先用代码常量；后续如果要做后台可配置，再迁到表里。
/// </summary>
public record class DefectGroupDictDto
{
    /// <summary>分组编码（唯一，前端 value）。</summary>
    [JsonPropertyName("code")]
    public string Code { get; init; } = string.Empty;

    /// <summary>分组名称（前端 label）。</summary>
    [JsonPropertyName("name")]
    public string Name { get; init; } = string.Empty;
}

/// <summary>
/// 面别字典条目（产线下的 "面" 集合，比如 A 面 / B 面 / C 面）。
/// 同样先用代码常量，等接入 line 表后可以扩展。
/// </summary>
public record class FaceGroupDictDto
{
    /// <summary>面编号（前端 value，比如 "1" / "2" / "A" / "B"）。</summary>
    [JsonPropertyName("code")]
    public string Code { get; init; } = string.Empty;

    /// <summary>面名称（前端 label，比如 "A面" / "B面"）。</summary>
    [JsonPropertyName("name")]
    public string Name { get; init; } = string.Empty;
}
