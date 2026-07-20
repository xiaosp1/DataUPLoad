# 04. 关键 DTO 字段映射

> 本文是 **EdgeHost 开发最关键的文档**。
> 每个要发给 PSM 的请求体 / 解析 PSM 响应，都从这里抄字段定义。
> 字段名 = Java 字段名（小驼峰），类型 = Java 类型 + JSON 类型对照。

---

## ⭐ POST /client/data/detect 请求体

**Java**：`com.hikrobotics.solution.module.line.dto.DetectDataUploadDTO`

```json
{
  "faceNo": "A1",           // @NotBlank String — 面编号
  "lineNo": "L01",          // @NotBlank String — 产线编号
  "todayData": { ... },     // @NotNull TodayDetectDataDTO
  "realTimeData": { ... }   // @NotNull RealTimeDetectData
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `faceNo` | String | ✅ @NotBlank | 面编号（产线下分多个面） |
| `lineNo` | String | ✅ @NotBlank | 产线编号 |
| `todayData` | Object | ✅ @NotNull | 当日统计数据（嵌套 DTO） |
| `realTimeData` | Object | ✅ @NotNull | 实时数据（嵌套 DTO） |

### 嵌套 DTO（待补全 — 在 `module/line/dto/RealTimeDetectData.java` 和 `TodayDetectDataDTO.java`）

⚠️ 反编译时这两个 DTO 单独成文件，需要单独读：

- `com/hikrobotics/solution/module/line/dto/RealTimeDetectData.java`
- `com/hikrobotics/solution/module/line/dto/TodayDetectDataDTO.java`

---

## ⭐ POST /client/data/alarm 请求体

**Java**：`com.hikrobotics.solution.module.alarm.dto.AlarmDTO`

```json
{
  "uuid":   "uuid-12345",       // @NotEmpty String — 唯一 ID（去重用）
  "time":   "2026-07-20 14:55:00",  // @NotEmpty String — 报警时间
  "type":   1,                  // @Range(1,3) Integer — 报警类型
  "lineNo": "L01",              // @NotEmpty String — 产线编号
  "faceNo": "A1",               // @NotEmpty String — 面编号
  "level":  2,                  // @NotNull Integer — 报警级别
  "message": "底面破损"          // @NotEmpty String — 报警内容
}
```

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `uuid` | String | ✅ | @NotEmpty | 报警唯一 ID（PSM 用它去重） |
| `time` | String | ✅ | @NotEmpty | 报警时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `type` | Integer | ⚠️ | @Range(min=1, max=3) | 1=defect（缺陷）/2=system（系统）/3=device（设备） |
| `lineNo` | String | ✅ | @NotEmpty | 产线编号 |
| `faceNo` | String | ✅ | @NotEmpty | 面编号 |
| `level` | Integer | ✅ | @NotNull | 报警级别（具体值看 AlarmLevelEnum） |
| `message` | String | ✅ | @NotEmpty | 报警描述 |

### `type` 枚举（来自 `AlarmTypeEnum`）

| 值 | 含义 | 备注 |
|---|---|---|
| 1 | defect（缺陷） | 由缺陷触发 |
| 2 | system（系统） | PSM 自身故障 |
| 3 | device（设备） | 现场设备故障 |

### `level` 枚举（来自 `AlarmLevelEnum`）

⚠️ 反编译没完整拿到，建议查 `module/alarm/constant/AlarmLevelEnum.java`。常见值：

| 值 | 含义 |
|---|---|
| 1 | 提示 |
| 2 | 警告 |
| 3 | 严重 |
| 4 | 紧急 |

---

## ⭐ POST /client/data/deal-alarm 请求体

**Java**：`com.hikrobotics.solution.module.alarm.dto.AlarmDealDTO`

```json
{
  "uuid": "uuid-12345"   // @NotEmpty String — 要处理的报警 uuid
}
```

⚠️ 反编译没完整拿到 DTO 字段（应该只有 uuid），需要时再确认。

---

## ⭐ POST /client/data/status 请求体

**Java**：`List<StatusRecordPO>`（包装类）

⚠️ `StatusRecordPO` 是 PO（持久化对象），可能字段较多，需要单独查 `module/detect/model/StatusRecordPO.java`。

---

## ⭐ POST /client/yk/defect-record 请求体

**Java**：`com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO`

```json
{
  "startTime":   "2026-07-20 00:00:00",   // @NotBlank String — 起始时间
  "endTime":     "2026-07-20 23:59:59",   // String — 结束时间（可选）
  "lindGroup":   ["L01", "L02"],          // List<String> — ⚠️ 注意是 lindGroup 不是 lineGroup（typo！）
  "defectGroup": ["底面破损", "划痕"],     // List<String> — 缺陷类型过滤
  "faceGroup":   ["A1", "A2"]             // List<String> — 面过滤
}
```

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `startTime` | String | ✅ | @NotBlank | 起始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | String | - | - | 结束时间，可选 |
| `lindGroup` | List\<String\> | - | - | ⚠️ **typo：海康写成了 lindGroup**，不是 lineGroup |
| `defectGroup` | List\<String\> | - | - | 缺陷类型过滤 |
| `faceGroup` | List\<String\> | - | - | 面过滤 |

### ⚠️ 重要细节

- **`lindGroup` 是 PSM 的拼写错误**（lind = line + d），我们 EdgeHost 调用时**必须用错拼的字段名**，否则 PSM 不识别
- **`startTime`/`endTime` 会被 `HikDateUtil.transformTime()` 转成 `LocalDateTime`**，格式必须严格

---

## ⭐ GET /client/yk/line-defect 响应

**响应 data**：`List<LineAndDefectDTO>`（推测，待 PSM 启动后验证）

**Java**：`com.hikrobotics.solution.module.yingke.dto.LineAndDefectDTO`

⚠️ 反编译没完整拿到，需要时查 `module/yingke/dto/LineAndDefectDTO.java`。常见结构（推测）：

```json
[
  {
    "lineNo": "L01",
    "lineName": "一号产线",
    "defects": [
      {"defectCode": "001", "defectName": "底面破损"},
      {"defectCode": "002", "defectName": "划痕"}
    ]
  }
]
```

---

## GET /client/plan 请求参数

**Java**：`com.hikrobotics.solution.module.line.dto.ClientPlanQueryDTO`

⚠️ 反编译没完整拿到 DTO，常见字段（推测）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `lineNo` | String | 产线编号 |

---

## 待补全 DTO 清单

以下 DTO 还没读详细字段，下次需要时优先补全：

- [ ] `RealTimeDetectData` (detect 的实时数据)
- [ ] `TodayDetectDataDTO` (detect 的当日统计)
- [ ] `StatusRecordPO` (detect 的状态持久化对象)
- [ ] `LineAndDefectDTO` (yingke 的字典响应)
- [ ] `YKRequestDTO` / `YKResponseDTO` (yingke 调英科系统的请求/响应格式)
- [ ] `AlarmLevelEnum` (报警级别枚举)
- [ ] `AlarmTypeEnum` (报警类型枚举)
- [ ] `ClientPlanQueryDTO` (客户端配方查询)
- [ ] `BaseResult` 的字段名（code/data/success/message）
