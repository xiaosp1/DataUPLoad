using System.Text.Json.Serialization;

namespace IntcoEdge.EdgeHost.Models;

/// <summary>
/// 英科网关列表型参数包装器（用于把报警数组塞进 `Parameters[0].Value`）。
/// 反编译来源：`com.hikrobotics.solution.module.yingke.dto.ListParamsDTO`。
/// 关键事实：
///   - `Parameters` 数组里第 0 个元素的 `Value` 是真正的业务对象列表
///   - PSM 把 `ListParamsDTO` 当成单参数，整体塞进 `Parameters`，再嵌套一层 `Value: [...]`
///   - JSON：`[{ "Value": [ {Alarm1}, {Alarm2} ] }]`
/// </summary>
/// <typeparam name="T">业务对象类型（如 `AlarmPushDto`）。</typeparam>
public record class YkListParam<T>
{
    /// <summary>真正的业务对象列表（PSM `ListParamsDTO.Value`）。</summary>
    [JsonPropertyName("Value")]
    public List<T>? Value { get; init; }

    public static YkListParam<T> Wrap(List<T> items) => new() { Value = items };
}
