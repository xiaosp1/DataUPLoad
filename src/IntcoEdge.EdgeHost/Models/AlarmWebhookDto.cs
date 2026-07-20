using System.Text.Json;
using System.Text.Json.Serialization;
using IntcoEdge.EdgeHost.Services;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// Webhook 报警入站 DTO（W-A7-S）。
///
/// 设计目标：
///   - 兼容老 PSM / 第三方车间的 webhook payload 格式（字段命名接近车间语言，
///     不是 PSM 内部的 uuid/type/level/message 命名）。
///   - 通过 <see cref="ToAlarmInputDto"/> 转成 W-A7-M 设计的内部 <see cref="AlarmInputDto"/>
///     （Controller ↔ Service 之间的归一化载体），再调
///     <c>IAlarmService.HandleAlarmAsync(AlarmInputDto)</c>。
///
/// 字段对照（入站 webhook → 内部 AlarmInputDto）：
///   - AlarmId    (可选)  → AlarmId       若为空则由 Service 生成 Guid
///   - LineNo     (必填)  → LineNo
///   - FaceNo     (必填)  → FaceNo
///   - DeviceNo   (可选)  → 拼到 Message 前缀 [device=xxx]
///   - GloveNo    (可选)  → 拼到 Message 前缀 [glove=xxx]
///   - DefectType (必填)  → DefectName
///   - Severity   (必填)  → Level         1..4 数字；非数字/越界 → 400
///   - Time       (必填)  → Time          yyyy-MM-dd HH:mm:ss
///   - ImgList    (可选)  → 拼到 Message 末尾 [imgs=N]
///
/// 注：原 webhook 字段名都保留 PascalCase，方便车间/三方对接方按字面照抄。
/// </summary>
public record class AlarmWebhookDto
{
    /// <summary>报警 ID（可选）。若为空则服务端生成 GUID 作为幂等键。</summary>
    [JsonPropertyName("AlarmId")]
    public string? AlarmId { get; init; }

    /// <summary>产线编号（必填）。</summary>
    [JsonPropertyName("LineNo")]
    public string? LineNo { get; init; }

    /// <summary>面编号（必填）。</summary>
    [JsonPropertyName("FaceNo")]
    public string? FaceNo { get; init; }

    /// <summary>设备编号（可选）。</summary>
    [JsonPropertyName("DeviceNo")]
    public string? DeviceNo { get; init; }

    /// <summary>手套编号（可选）。</summary>
    [JsonPropertyName("GloveNo")]
    public string? GloveNo { get; init; }

    /// <summary>缺陷类型（必填）。</summary>
    [JsonPropertyName("DefectType")]
    public string? DefectType { get; init; }

    /// <summary>
    /// 报警级别：1=提示 / 2=警告 / 3=严重 / 4=紧急（必填，整数字符串或数字）。
    /// 用 <see cref="JsonElement"/> 接收，兼容 `"3"`（字符串）和 `3`（数字）两种入参形式。
    /// </summary>
    [JsonPropertyName("Severity")]
    public JsonElement? Severity { get; init; }

    /// <summary>报警时间，格式 `yyyy-MM-dd HH:mm:ss`（必填）。</summary>
    [JsonPropertyName("Time")]
    public string? Time { get; init; }

    /// <summary>图片列表（可选，JSON 字符串或数组字符串）。</summary>
    [JsonPropertyName("ImgList")]
    public string? ImgList { get; init; }

    /// <summary>
    /// 校验必填字段 + 字段格式。任何不通过都抛 <see cref="ArgumentException"/>，
    /// 由 WebhookController 转 400 BadRequest。
    /// </summary>
    public void Validate()
    {
        if (string.IsNullOrWhiteSpace(LineNo))
            throw new ArgumentException("LineNo 必填", nameof(LineNo));
        if (string.IsNullOrWhiteSpace(FaceNo))
            throw new ArgumentException("FaceNo 必填", nameof(FaceNo));
        if (string.IsNullOrWhiteSpace(DefectType))
            throw new ArgumentException("DefectType 必填", nameof(DefectType));
        if (Severity is null || Severity.Value.ValueKind == JsonValueKind.Null || Severity.Value.ValueKind == JsonValueKind.Undefined)
            throw new ArgumentException("Severity 必填", nameof(Severity));
        if (string.IsNullOrWhiteSpace(Time))
            throw new ArgumentException("Time 必填", nameof(Time));

        // 时间格式：yyyy-MM-dd HH:mm:ss（Repository / 英科推送都按这个走）
        if (!System.DateTime.TryParseExact(
                Time,
                "yyyy-MM-dd HH:mm:ss",
                System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None,
                out _))
        {
            throw new ArgumentException(
                "Time 格式错误，必须为 yyyy-MM-dd HH:mm:ss",
                nameof(Time));
        }

        // Severity 必须在 1..4 范围（接受 int 或 string 两种形态）
        if (!TryParseLevel(Severity, out _))
        {
            throw new ArgumentException(
                "Severity 必须是 1..4 的整数（1=提示/2=警告/3=严重/4=紧急）",
                nameof(Severity));
        }
    }

    /// <summary>
    /// 转换成 W-A7-M 设计的内部 <see cref="AlarmInputDto"/>，
    /// 喂给 <c>IAlarmService.HandleAlarmAsync(AlarmInputDto, CancellationToken)</c>。
    ///
    /// 转换规则：
    ///   - AlarmId    ← AlarmId ?? null（让 Service 生成 Guid）
    ///   - Uuid       ← AlarmId ?? null（透传 PSM 上游 UUID；缺失则交给 Service 默认）
    ///   - LineNo     ← LineNo
    ///   - FaceNo     ← FaceNo
    ///   - Type       ← 1（defect；webhook 场景默认是缺陷触发）
    ///   - Level      ← Severity (1..4)
    ///   - Time       ← Time
    ///   - DefectName ← DefectType
    ///   - Reason     ← null（webhook 没这字段）
    ///   - Solve      ← 2（未解决）
    ///   - Message    ← 由 DefectType + 可选 DeviceNo/GloveNo/ImgList 拼成的可读文本
    ///   - WorkShop / AlarmTypeDesc / AlarmLevelDesc / AlarmResult / AlarmCount
    ///                ← null（webhook 没这字段；Service 层会用默认值）
    /// </summary>
    public AlarmInputDto ToAlarmInputDto()
    {
        Validate();

        var lvl = TryParseLevel(Severity, out var parsed) ? parsed : 1;

        // 拼 Message：让下游日志/英科推送能看到完整上下文
        var parts = new List<string> { DefectType! };
        if (!string.IsNullOrWhiteSpace(DeviceNo))
            parts.Add($"[device={DeviceNo}]");
        if (!string.IsNullOrWhiteSpace(GloveNo))
            parts.Add($"[glove={GloveNo}]");
        if (!string.IsNullOrWhiteSpace(ImgList))
        {
            // 粗略统计图片数：逗号 + 字段数（不解析 JSON，避免 webhook 慢）
            var imgCount = CountImgList(ImgList!);
            parts.Add($"[imgs={imgCount}]");
        }
        var message = string.Join(' ', parts);

        return new AlarmInputDto
        {
            AlarmId = string.IsNullOrWhiteSpace(AlarmId) ? null : AlarmId!.Trim(),
            Uuid = string.IsNullOrWhiteSpace(AlarmId) ? null : AlarmId!.Trim(),
            Time = Time!,
            Type = 1,           // webhook 场景默认：缺陷触发
            LineNo = LineNo!,
            FaceNo = FaceNo!,
            Level = lvl,
            Message = message,
            Solve = 2,          // 未解决
            Reason = null,
            DefectName = DefectType,
            WorkShop = null,
            AlarmTypeDesc = null,
            AlarmLevelDesc = null,
            AlarmResult = null,
            AlarmCount = null,
        };
    }

    private static bool TryParseLevel(JsonElement? raw, out int level)
    {
        level = 0;
        if (raw is null || raw.Value.ValueKind == JsonValueKind.Null || raw.Value.ValueKind == JsonValueKind.Undefined)
            return false;
        var v = raw.Value;
        int n;
        switch (v.ValueKind)
        {
            case JsonValueKind.Number:
                if (!v.TryGetInt32(out n)) return false;
                break;
            case JsonValueKind.String:
                var s = v.GetString();
                if (string.IsNullOrWhiteSpace(s)) return false;
                if (!int.TryParse(s, out n)) return false;
                break;
            default:
                return false;
        }
        if (n < 1 || n > 4) return false;
        level = n;
        return true;
    }

    /// <summary>
    /// 粗略统计图片列表条数。容忍：
    ///   - JSON 数组：`["a.jpg","b.jpg"]` → 2
    ///   - 逗号分隔：`a.jpg,b.jpg`        → 2
    ///   - 空字符串 / null               → 0
    /// </summary>
    private static int CountImgList(string imgList)
    {
        if (string.IsNullOrWhiteSpace(imgList)) return 0;
        var s = imgList.Trim();
        // 去掉最外层 [ ]
        if (s.StartsWith('[') && s.EndsWith(']') && s.Length >= 2)
            s = s.Substring(1, s.Length - 2);
        if (string.IsNullOrWhiteSpace(s)) return 0;
        // 按逗号切，滤空
        return s.Split(',', StringSplitOptions.RemoveEmptyEntries).Length;
    }
}
