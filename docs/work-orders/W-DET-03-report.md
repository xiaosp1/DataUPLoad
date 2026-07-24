# W-DET-03 报告 — 补全 detect 模块 day-record 4 个新 endpoint + line day-record 3 个 endpoint + DetectDataController name= 属性

- 工单：W-DET-03（P1，控制层补齐）
- Worker：Java W-DET-03
- 时间：2026-07-24
- 范围：仅 controller 层（detect + line 各 1 个新 controller + 1 个 controller 微调），**未改动** service / mapper / entity / DTO
- 前置依赖：W-DET-01（IDefectDayRecordService 8 个新方法）、W-DET-02（ILineDayRecordService 4 个新方法 + removeRecordByTime 边界修复）
- PSM 参照：
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/web/DetectDataController.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/service/IDefectDayRecordService.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/service/ILineDayRecordService.java`

---

## 1. 改动文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/web/DefectDayRecordController.java` | **新建** | 4 个 day-record endpoint：list-by-attribute / list-by-start / list-between / search-defect-count |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/web/LineDayRecordController.java` | **新建** | 3 个 day-record endpoint：list-by-start / list-of-line-between / list-line-day-between |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/web/DetectDataController.java` | 修改 | 给现有 `/web/detect/detail` + `/web/detect/realtime` 两个 endpoint 的 `@RequestParam` 显式加 `name=` 属性（W-DET-03 spec）；其他 endpoint 已是 PSM 对齐 |

**未改动** service / mapper / entity / DTO / 其它模块；未触碰 git。

---

## 2. endpoint 列表

### 2.1 `DefectDayRecordController`（detect 模块，根路径 `/web/detect/day-record`）

| HTTP | 路径 | 方法签名（controller） | 委派给 service |
|---|---|---|---|
| POST | `/web/detect/day-record/list-by-attribute` | `BaseResult listByAttribute(@RequestParam(name="attr") String attr, @RequestParam(name="value", required=false) String value)` | `IDefectDayRecordService.listByAttribute(value, getter)` |
| POST | `/web/detect/day-record/list-by-start` | `BaseResult listByStartTime(@RequestParam(name="startTime") String startTime)` | `IDefectDayRecordService.listByStartTime(startTime)` |
| POST | `/web/detect/day-record/list-between` | `BaseResult listBetween(@RequestParam(name="startTime") String startTime, @RequestParam(name="endTime") String endTime)` | `IDefectDayRecordService.listBetween(startTime, endTime)` |
| POST | `/web/detect/day-record/search-defect-count` | `BaseResult searchDefectCount(@RequestParam(name="time") String time, @RequestParam(name="lineNo") String lineNo, @RequestParam(name="faceNo") String faceNo, @RequestBody List<String> defects)` | `IDefectDayRecordService.searchDefectCount(time, lineNo, faceNo, defects)` |

**返回**：`BaseResult.build().data(...)` 包裹的 `List<DefectDayRecord>` 或 `List<DefectCountDTO>`。

#### 2.1.1 `list-by-attribute` 设计说明

service 接口签名是泛型：
```java
<T> List<DefectDayRecord> listByAttribute(T value, SFunction<DefectDayRecord, T> getter);
```

`SFunction` 无法从 HTTP 字符串还原。controller 层做白名单映射（switch on `attr`），覆盖 PSM 已有字段：`id` / `count` / `time` / `lineNo` / `faceNo` / `type`。其余 attr 返回空 list（视为非法输入）。

```java
private static SFunction<DefectDayRecord, ?> resolveGetter(String attr) {
   if (Objects.isNull(attr)) return null;
   switch (attr) {
      case "id":     return DefectDayRecord::getId;
      case "count":  return DefectDayRecord::getCount;
      case "time":   return DefectDayRecord::getTime;
      case "lineNo": return DefectDayRecord::getLineNo;
      case "faceNo": return DefectDayRecord::getFaceNo;
      case "type":   return DefectDayRecord::getType;
      default:       return null;
   }
}
```

调用 service 时用 rawtype cast（`@SuppressWarnings({"unchecked","rawtypes"})`），符合 MyBatis-Plus 泛型惯例。

#### 2.1.2 `search-defect-count` 入参

`time` / `lineNo` / `faceNo` 是 `@RequestParam` 字符串；`defects` 是 `@RequestBody List<String>`（JSON 数组），由调用方传 `["defect1","defect2"]` 形式。service 接口在 defects 空集合时直接返回空 list，controller 无需前置判断。

### 2.2 `LineDayRecordController`（line 模块，根路径 `/web/line/day-record`）

| HTTP | 路径 | 方法签名（controller） | 委派给 service |
|---|---|---|---|
| POST | `/web/line/day-record/list-by-start` | `BaseResult listByStartTime(@RequestParam(name="startTime") String startTime)` | `ILineDayRecordService.listByStartTime(startTime)` |
| POST | `/web/line/day-record/list-of-line-between` | `BaseResult listOfLineBetween(@RequestParam(name="start") @DateTimeFormat(iso=ISO.DATE_TIME) String start, @RequestParam(name="end") @DateTimeFormat(iso=ISO.DATE_TIME) String end, @RequestParam(name="lineNo") String lineNo, @RequestParam(name="faceNo") String faceNo)` | `ILineDayRecordService.listOfLineBetween(startDt, endDt, lineNo, faceNo)` |
| POST | `/web/line/day-record/list-line-day-between` | `BaseResult listLineDayBetween(@RequestParam(name="startTime") String startTime, @RequestParam(name="endTime") String endTime)` | `ILineDayRecordService.listLineDayBetween(startTime, endTime)` |

#### 2.2.1 `list-of-line-between` 时间参数解析

HTTP 入参 `start` / `end` 是 ISO-8601 字符串（`yyyy-MM-dd'T'HH:mm:ss`），通过私有 `parseLocalDateTime(String)` 转 `LocalDateTime`：

```java
private static LocalDateTime parseLocalDateTime(String raw) {
   if (raw == null || raw.isEmpty()) return null;
   String normalized = raw.contains("T") ? raw.replace('T', ' ') : raw;
   try {
      return HikDateUtil.transformTime(normalized);  // PSM 风格，默认 "yyyy-MM-dd HH:mm:ss"
   } catch (RuntimeException ex) {
      log.warn("parseLocalDateTime fail raw={}, fallback to ISO", raw, ex);
      return LocalDateTime.parse(raw);  // Spring ISO-8601 兜底
   }
}
```

`@DateTimeFormat(iso=ISO.DATE_TIME)` 同时声明在注解上，给 Spring 的内置转换器兜底；service 内部按天归并成 `00:00:00` / `23:59:59`（由 W-DET-02 impl 实现）。

### 2.3 `DetectDataController` 微调（detect 模块，根路径 `/`）

PSM DetectDataController 5 个 endpoint 在 B03 已全部对齐（audit 评级 F）。W-DET-03 任务要求"每个 `@RequestParam` 加 `name=\"...\"` 属性"，给两个 GET 端点补齐：

| 端点 | 改动 |
|---|---|
| `GET /web/detect/detail` | `@RequestParam Integer faceId` → `@RequestParam(name="faceId") Integer faceId`；`startTime` / `endTime` 同理 |
| `GET /web/detect/realtime` | `@RequestParam String lineNo` → `@RequestParam(name="lineNo") String lineNo`；`faceNo` 同理 |
| `GET /web/detect/statistic/export` | 不变（form-binding，不涉及 @RequestParam） |
| `POST /client/data/detect` | 不变（@RequestBody） |
| `POST /client/data/status` | 不变（@RequestBody） |

> **PSM 反编译产物核对**：PSM `DetectDataController.java` 仅含以上 5 个 endpoint，无 `detect/list` 等其它 endpoint；本次任务"PSM 还有 detail/list 等端点没补"经核对实际已 5/5 对齐。

---

## 3. 编译结果

### 3.1 单文件编译（任务指定命令）

```bash
cd E:\DEMO\数据采集
javac -encoding UTF-8 -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\web\*.java
```

实际编译 detect + line 两个 web 目录（含两个新 controller + 现有 DetectDataController）：

```
[exit=0]
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
```

✅ **成功**，仅有项目既有的 rawtype unchecked 警告（与本次改动无关）。

### 3.2 全模块编译

```
[exit=0]
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
```

✅ **成功**，整个 `DataupLoad/src/main/java` 全部 184 个 Java 文件通过编译（181 + 3 新/改）。

### 3.3 字节码验证

`DefectDayRecordController.class` javap 输出：
```
public class ...DefectDayRecordController {
  public ...BaseResult listByAttribute(String, String);
  public ...BaseResult listByStartTime(String);
  public ...BaseResult listBetween(String, String);
  public ...BaseResult searchDefectCount(String, String, String, List<String>);
  private static SFunction<DefectDayRecord, ?> resolveGetter(String);
}
```

`LineDayRecordController.class` javap 输出：
```
public class ...LineDayRecordController {
  public ...BaseResult listByStartTime(String);
  public ...BaseResult listOfLineBetween(String, String, String, String);
  public ...BaseResult listLineDayBetween(String, String);
  private static LocalDateTime parseLocalDateTime(String);
}
```

`@RequestParam(name=...)` 在 javap -v 输出中正确保留（`name="attr"`, `name="value"`, `name="startTime"`, `name="endTime"`, `name="time"`, `name="lineNo"`, `name="faceNo"`, `name="start"`, `name="end"`），不依赖 `-parameters` 编译参数。

---

## 4. 与前置工单的衔接

| 工单 | 落地状态 | W-DET-03 关联 |
|---|---|---|
| W-DET-01 | `IDefectDayRecordService` 接口 10/10 方法声明齐全，Impl 10/10 实现 | ✅ 4 个 endpoint 直接委派到 service 方法 |
| W-DET-02 | `ILineDayRecordService` 接口 7/7 方法声明齐全，Impl 7/7 实现 | ✅ 3 个 endpoint 直接委派到 service 方法 |

controller 层未触动 service 层（任务硬约束），所有委派都是"参数 → service → BaseResult.data(...)"薄封装。

---

## 5. 已知限制

1. **`list-by-attribute` 白名单字段有限**：当前 attr 字符串只映射到 DefectDayRecord 已知字段（`id` / `count` / `time` / `lineNo` / `faceNo` / `type`）。若 PSM 后续调用方传 `updateTime` / `createTime`，需扩展 `resolveGetter` switch。PSM 反编译未提供调用点，无法推断是否还有其他 attr。
2. **`search-defect-count` 只覆盖 4-参 String 重载**：service 接口有另一个 5-参 LocalDateTime 重载未暴露到 HTTP（task spec 未要求）；如后续需要按 LocalDateTime 范围聚合，可再加 endpoint `POST /web/detect/day-record/search-defect-count-between`。
3. **`list-of-line-between` 时间解析走 `HikDateUtil.transformTime`**：`HikDateUtil` 来自 `lib/framework-starter-2.2.3-SNAPSHOT.jar`，无源码；运行时可用。若该方法 pattern 与入参不匹配，会 fallback 到 Spring ISO 解析（已在代码内 try/catch）。
4. **`DetectDataController.handleDetectDetailSearch` 仍抛 `UnsupportedOperationException`**：service 实现未落地（W-B03 范围之外）；本次仅在 controller 层补 `name=` 属性，**不实现 service**。前端若调用 `/web/detect/detail` 仍会 500。
5. **`@RequestBody List<String> defects`**：Spring 默认按 JSON 数组反序列化整个 body。若调用方传 `{"defects": [...]}` 这种包装对象，会反序列化失败（`HttpMessageNotReadableException`）。调用方需严格传 `["defect1","defect2"]`。
6. **未触碰 git**：未做任何 git add / commit / push（工单要求"不要推 git"）。
7. **未写单元测试**：工单未要求；后续可补 `DefectDayRecordControllerTest` / `LineDayRecordControllerTest`（Mock service 验证 endpoint 入参映射 + BaseResult 包装）。
8. **DetectDataController 没新增 endpoint**：PSM 反编译仅 5 个 endpoint，DataupLoad 已 5/5 对齐；W-DET-03 spec "detail/list 等端点没补"经核对无遗漏，仅补 `name=` 属性。如 PSM 还有未反编译到的 controller（如某个 `DetectRecordController`），需在 audit 阶段补反编译。

---

## 6. 交付确认

- ✅ 新建 `DefectDayRecordController` 4 个 endpoint 1:1 对齐 service
- ✅ 新建 `LineDayRecordController` 3 个 endpoint 1:1 对齐 service
- ✅ `DetectDataController` 现有 5 endpoint + 4 `@RequestParam` 全部显式 `name=`
- ✅ 未触动 service / mapper / entity / DTO
- ✅ 单文件 + 全模块编译通过（exit=0）
- ✅ 字节码 `javap -v` 验证 `@RequestParam(name=)` 已保留
- ✅ 未推 git
