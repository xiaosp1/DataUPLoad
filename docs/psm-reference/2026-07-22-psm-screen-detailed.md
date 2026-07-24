# PSM screen 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: ⚪ P3（大屏数据聚合，纯 DTO + Service，无 web）

---

## 1. 业务定位

### 1.1 解决什么问题

screen 模块是 PSM 的**大屏数据聚合器**：

- **大屏数据组装**：`ScreenDataDTO`（detectData + defectSum + removeSum + clientStatusList + alarms）
- **WebSocket 广播**：`WsTypeEnum.SCREEN` 类型推送到所有 web 客户端
- **定时触发**：`ScreenServiceImpl.sendScreenDataInfo()` 应该由某处 cron 定时调用（反编译未找到调用方，**可能是死代码或未编译**）

### 1.2 与其他模块的依赖关系

```
screen ──→ detect (IDefectDayRecordService / ILineDayRecordService / IStatusRecordService)
screen ──→ line   (ILineService)
screen ──→ defect (ILineDefectTypeService)
screen ──→ alarm  (AlarmRecordPO)  // 数据结构引用
screen ──→ common (WebSocketHandler / WsMessage / WsTypeEnum)
```

---

## 2. 类清单（5 个 java + 0 个 XML）

### 2.1 dto/ (3)
| 类 | 字段 |
|---|---|
| `ScreenDataDTO` (+1 inner DetectDataDTO) | detectData/defectSum/removeSum/clientStatusList/alarms |
| `DefectNumberDTO` | defectName/defectCount |
| `ClientStatusDTO` | lineNo/faceNo/lineId/order/cameraStatus/eliminatorStatus/clientStatus |

### 2.2 service/ (1) + service/imp/ (1)
| 接口 | 实现 | 责任 |
|---|---|---|
| `IScreenService` | `ScreenServiceImpl` (138 行) | **⚪ P3** 大屏数据组装 + WebSocket 推送 |

### 2.3 web/ (0)
**⚠️ 没有 Web Controller**！screen 模块纯靠 `sendScreenDataInfo()` 方法被外部触发。

---

## 3. 核心流程

### 3.1 大屏数据组装与推送

```
ScreenServiceImpl.sendScreenDataInfo()
  │
  ├─→ buildScreenData()
  │     │
  │     ├─→ lineDefectTypeService.listIfShowEnable(null, null)  // 所有产线所有 showFlag=YES 缺陷
  │     │     → sortDefectByPosAndName + needShowDefectNames
  │     │
  │     ├─→ currentHours = now (按小时取整)
  │     ├─→ defectDayRecordService.listByStartTimeAndDefect(needShowDefectNames, currentHours)
  │     │     → 按 type + pos 分组
  │     │
  │     ├─→ lineDayRecordService.listByTime(currentHours)
  │     │     → 本小时 line_day_record（用于 removeTotal）
  │     │     → result.removeSum += data.removeTotal
  │     │
  │     ├─→ lineService.listLine() → 按 order + color 排序
  │     │
  │     └─→ for each line:
  │           ├─→ DefectDataDTO (lineNo/faceNo/order/color/lineId)
  │           ├─→ for each needShowDefectNames:
  │           │     ├─→ 查本小时本产线本缺陷的总数
  │           │     ├─→ DefectNumberDTO → 加入 defectCounts
  │           │     └─→ if 第一次循环: 同时累加到 result.defectSum
  │           │           ⚠️ 这里有 BUG（见 §7.1）
  │           ├─→ if line 有 realtime_data → 反序列化
  │           └─→ 加入 result.detectData
  │
  └─→ webSocketHandler.broadcastByUid(WsMessage{type=SCREEN, data=screenDataDTO}.toJsonString(), "web")
```

### 3.2 客户端状态聚合

```
getCilentStatusList(lines)
  │
  ├─→ statusRecordService.list() → List<StatusRecordPO>
  │     → 按 line (lineNo:faceNo) 分组
  │
  └─→ for each line:
        ├─→ 取该产线所有 status_record，按 deviceNo 去重（保留最新）
        ├─→ 分别累加 camera / eliminator / client 的状态
        │     └─→ Boolean.TRUE/FALSE 累加（null 初始化，然后 `status & acc`）
        └─→ ClientStatusDTO (cameraStatus / eliminatorStatus / clientStatus)
```

**⚠️ 状态计算细节**:
```java
Boolean status = DeviceStatus.ONLINE.getValue().equals(statusRecordPO.getStatus()) ? TRUE : FALSE;
if (DeviceType.CAMERA.getValue().equals(statusRecordPO.getType())) {
    cameraStatus = cameraStatus == null ? status : status & cameraStatus;
    // 多个相机 AND：任一离线 → 离线
}
```

---

## 4. 关键类逐个解析

### 4.1 ⚪ P3: `ScreenServiceImpl` (138 行)

**核心方法**:
```java
public void sendScreenDataInfo()                       // 推送入口（被 cron 或外部调用）
private ScreenDataDTO buildScreenData()                // 聚合数据
private List<ClientStatusDTO> getCilentStatusList(...)  // 设备状态聚合
```

**注意**: `buildScreenData` 是 `private`，但 `sendScreenDataInfo` 是 `public`，所以外部只能调用 `sendScreenDataInfo`。

### 4.2 ScreenDataDTO 内嵌类

```java
public static class DetectDataDTO {
    private String lineNo;
    private String faceNo;
    private Integer order;
    private Integer lineId;
    private String color;
    private Integer removeTotal;
    private List<DefectNumberDTO> hourDefectCount;
    private RealTimeDetectData realTimeDetectData;
    // + getters/setters
}
```

---

## 5. 数据库交互

screen 模块**没有自己的表**，纯聚合查询：
- `line_defect_type` — 缺陷类型（来自 defect 模块）
- `defect_day_record` — 按小时统计（来自 detect 模块）
- `line_day_record` — 按小时统计（来自 detect 模块）
- `line` — 产线列表（来自 line 模块）
- `status_record` — 设备状态（来自 detect 模块）

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

无（screen 模块是纯大屏聚合，EdgeHost 没有大屏职责）

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `ScreenServiceImpl.sendScreenDataInfo` | ❌ 不需要 | ⚪ N/A |
| `ScreenDataDTO` 等 DTO | ❌ 不需要 | ⚪ N/A |

### 6.3 移植建议

**screen 模块不需要移植到 EdgeHost**：
1. EdgeHost 是产线侧网关，负责本地数据汇聚
2. screen 是 PSM 大屏展示层（Web 客户端）
3. 大屏由 PSM Web 前端直接连 PSM 拿数据（HTTP + WebSocket）

---

## 7. 风险 / 注意点

### 7.1 ⚠️ P2 BUG：defectSum 计算时机错误

```java
boolean isCalcTotalDefectCount = false;
// ...
for (String defectName : needShowDefectNames) {
    // ... 计算本产线本缺陷 ...
    if (!isCalcTotalDefectCount) {
        totalCountOfFace = sortDefectByPos.values().stream()
            .flatMap(counts -> counts.stream().map(DefectDayRecordPO::getCount))
            .reduce(0, Integer::sum);
        defectNumberDTO = new DefectNumberDTO();
        defectNumberDTO.setDefectName(defectName);
        defectNumberDTO.setDefectCount(totalCountOfFace);
        result.getDefectSum().add(defectNumberDTO);  // ⚠️ 这里用的局部变量 totalCountOfFace
    }
}
// 外层循环：
// for each line:
//     isCalcTotalDefectCount = true;  // 第二条产线后停止计算 defectSum
```

**问题**:
- `isCalcTotalDefectCount = true` 在 **每条产线结束后**设置
- 但 `result.getDefectSum().add()` 是在 **defectName 内层循环**中（第一次产线时）
- 实际行为：第一次产线（通常是 order=1）计算 defectSum，第二条产线开始跳过
- 由于 `totalCountOfFace = sortDefectByPos.values()...` 用的是**所有产线**的 sortDefectByPos（不是 line-specific），所以 defectSum 实际上是**全产线总和**——这可能是预期行为，但逻辑上**只算了第一次产线**（即 only first line）就跳过了，浪费了后续计算。

**⚠️ 实际是死代码**：因为 `isCalcTotalDefectCount = true` 在 `defectName` 内层循环结束后立即设置，**第二条产线开始时 defectName 循环就直接跳过 `result.defectSum.add()`**。但因为 `totalCountOfFace` 计算用的是全局数据 `sortDefectByPos`，所以第一条产线已经计算了正确总和。

**结论**: 代码能跑出正确结果，但**逻辑读起来很奇怪**。建议重构：
```java
// 在 buildScreenData 开始时单独计算 defectSum
result.setDefectSum(needShowDefectNames.stream()
    .map(name -> new DefectNumberDTO()
        .setDefectName(name)
        .setDefectCount(sortDefectByPosAndType.getOrDefault(name, Map.of())
            .values().stream()
            .flatMap(List::stream)
            .mapToInt(DefectDayRecordPO::getCount)
            .sum()))
    .toList());

// 然后在产线循环中只计算本产线 defectCounts
```

### 7.2 ⚠️ sendScreenDataInfo 调用方未找到

反编译中没有发现 `@Scheduled` 或 `@EventListener` 调用 `sendScreenDataInfo()`。可能：
1. 在 framework 包中有调用（反编译未覆盖）
2. 通过反射调用
3. **死代码**（如果有前端轮询其他接口）

### 7.3 listIfShowEnable(null, null)

```java
this.lineDefectTypeService.listIfShowEnable(null, null);
```

SQL 是：
```sql
SELECT * FROM line_defect_type
WHERE (lineNo IS NULL OR line_no = ?)  -- StringUtils.isNotBlank(lineNo) → true 时才加
  AND (faceNo IS NULL OR face_no = ?)
  AND show_flag = YES
```

两个 null 都传入时，**等价于无 lineNo/faceNo 过滤**，返回所有产线的 showFlag=YES 缺陷。这与 `listIfShowEnable` 的方法名暗示的"按产线查询"不符——但实际 SQL 行为正确。

### 7.4 list() 排序

`lineService.listLine()` 返回 `List<LinePO>`，按 `order` + `color` 排序：
```java
.sorted(Comparator.comparingInt(LinePO::getOrder).thenComparing(LinePO::getColor))
```

**⚠️ NPE 风险**: `LinePO.getOrder()` 字段是 `Integer`（不是 int），如果为 null 会抛 NullPointerException。

### 7.5 getOrDefault 空指针

```java
Map<String, LineDefectTypePO> sortDefectByName = sortDefectByPosAndName.getOrDefault(line.getPos(), Maps.newHashMap());
```

`line.getPos()` 返回 `lineNo + ":" + faceNo`，如果产线没保存过任何 line_defect_type，会返回空 map —— OK。

### 7.6 realtimeData 反序列化

```java
if (StringUtils.isNotBlank(line.getRealtimeData())) {
    detectDataOfLine.setRealTimeDetectData(JSONUtil.toBean(line.getRealtimeData(), RealTimeDetectData.class));
}
```

如果 `line.realtimeData` 是非法 JSON（异常数据），`JSONUtil.toBean` 抛异常 → 整个 `buildScreenData` 中断。需要 try-catch 或数据校验。

---

## 8. 总结

screen 模块是 PSM 大屏数据聚合，P3 关注点：
1. **`ScreenServiceImpl.buildScreenData`**：5 表聚合（line + line_defect_type + defect_day_record + line_day_record + status_record）
2. **`getCilentStatusList`**：设备状态 AND 聚合
3. **WebSocket 推送**：SCREEN 类型

关键风险：
- ⚠️ **defectSum 计算逻辑晦涩**（实际能跑但代码难懂）
- ⚠️ **sendScreenDataInfo 调用方未找到**（可能是死代码或 framework 调用）
- `listIfShowEnable(null, null)` 行为正确但与命名不符
- `LinePO.order` 字段可能 NPE
- `JSONUtil.toBean` 失败可能中断整个推送

**EdgeHost 移植优先级：⚪ N/A**（screen 不需要移植）
