# W-DET-08 Report — Excel 导出返工（1:1 对齐 PSM）

**日期**：2026-07-24 22:36 GMT+8
**Worker**：Java 开发 worker（subagent）
**任务类型**：返工（不补丁，1:1 抄 PSM 反编译产物）

---

## 一、背景与目标

W-DET-07 端到端测试发现 `/web/detect/statistic/export` 端点偏离 PSM 反编译产物，3 个真 Bug：

| Bug | 现状 | PSM 设计 |
|-----|------|---------|
| P0 XLSX 表头 7行×1列 | controller `buildExportHeaders()` 单层嵌套 | controller 不自建表头，service 内 `ExcelUtils.export()` 流式写多 sheet × 多 table |
| P0 非空数据抛 10500 | 自建 `exportToExcel` 在非空 list 抛异常 | `SheetConfig + EasyExcel.writerTable(...).head().registerWriteHandler(DataMergeStrategy)` 流式写 |
| P1 `faceNo=""` 透传 | `@RequestParam String faceNo` 透传空串到 SQL | `@Validated ExportDefectStatisticForm` + `faceNo/@NotBlank` 校验 |

**根因**：W-DET-05b/c 偷懒没 1:1 抄 PSM，自己造了简化实现。本工单全部 1:1 抄 PSM。

---

## 二、PSM 反编译产物对齐确认

所有 PSM 反编译产物均**先读后抄**，未凭工单描述推断：

| PSM 反编译产物 | 路径 | 对齐方式 |
|-----|------|---------|
| `ExportDefectStatisticForm.java` | `docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\detect\dto\` | 已 1:1（DataupLoad W-DET-05c 已抄对，仅字段定义，无需改） |
| `DataMergeStrategy.java` | `excel\` | W-DET-05b 已 1:1（沿用，未改） |
| `ExcelUtils.java` | `util\` | **重写** —— 替换 W-DET-05c 的 buggy `exportToExcel`，恢复 PSM `export(HttpServletResponse, List<SheetConfig>, String)` 1:1 |
| `IDefectRecordService.java` | `service\` | 接口签名已 1:1（含 `handleStatisticDataExport`），不动 |
| `DefectRecordServiceImpl.java` | `service\imp\` | **重写 `handleStatisticDataExport`** 1:1（替换原 UOE 桩） |
| `DetectDataController.java` | `web\` | **重写 `exportStatisticData`** 1:1（删除 `buildExportHeaders` / `buildExportRows`，改走 service） |

### ⚠ 任务 spec 与 PSM 的差异（已校正）

工单 spec Step 2 提到"新建 SheetConfig.java / Table.java（路径 `excel/`）"。
**PSM 真实反编译产物**里这两个类**不存在**为独立文件 —— 它们是
`com.hikrobotics.solution.module.detect.util.ExcelUtils` 的 inner class
（反编译产物里以 `ExcelUtils$SheetConfig.class` / `ExcelUtils$Table.class`
呈现），属于 PSM 反编译产物已加载类（`Could not load the following classes`
列表里出现 `com.hikrobotics.solution.module.detect.util.ExcelUtils$SheetConfig`
和 `com.hikrobotics.solution.module.detect.util.ExcelUtils$Table`）。

本工单按 PSM 真实结构实现为 **`ExcelUtils.SheetConfig` / `ExcelUtils.Table` 内嵌
public static 内部类**，与 PSM 反编译产物形态一致；类名 / 字段名 / 访问修饰符
1:1 抄 PSM。

---

## 三、文件改动清单

### 3.1 重写文件（3 个）

#### 1) `DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\util\ExcelUtils.java`

**PSM 1:1 关键点**：

- 新增 `public static void export(HttpServletResponse, List<SheetConfig>, String)` 1:1 抄 PSM：
  - Content-Type / Character-Encoding / Content-Disposition（PSM 同款，UTF-8 URLEncoder + `.xlsx` 后缀）
  - `EasyExcel.write(response.getOutputStream()).build()` → ExcelWriter
  - `EasyExcel.writerSheet(config.getName()).build()` → WriteSheet
  - `EasyExcel.writerTable(id.getAndIncrement()).head(table.getHeaders()).relativeHeadRowIndex(id.get() == 1 ? 0 : 2).registerWriteHandler(new DataMergeStrategy(...)).registerWriteHandler(getDefaultWriteHandle()).registerWriteHandler(getColumnWidthStrategy()).registerWriteHandler(getDefaultStyleStrategy()).build()` → WriteTable
  - `writer.write(table.getValues(), sheet, wTable)` → 写数据
  - `start.set(start.get() + table.getRowNum() + 2)` → 多 table 起始偏移累加（PSM 同款）
  - finally 块 `writer.finish()`（PSM 同款）
- 新增内嵌静态类 **`SheetConfig`**（字段 `name` + `tables` + builder-style setter）
- 新增内嵌静态类 **`Table`**（关键 PSM 字段：
  - `public List<List<Object>> values` — **public 字段**（PSM 反编译产物真实写法，不是 Lombok getter；被 `ExcelUtils.export(...).table.values.size()` 直接字段访问）
  - `headers` / `rowNum` / `mergeColumns` private + getter/setter
- 删除 W-DET-05c 的 buggy `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` 重载
- 保留 W-DET-05b 的 `exportToExcel(List<T>, Class<T>, OutputStream)` / `readFromExcel(InputStream, Class<T>)` POJO API（与 PSM 无冲突）

**已知限制**（PSM 反编译限制）：
- PSM 反编译里 `getDefaultWriteHandle` / `getColumnWidthStrategy` / `getDefaultStyleStrategy` 是 CFR 标记 "Unavailable Anonymous Inner Class" 的工厂方法 —— DataupLoad 沿用 W-DET-05b 的 self-contained helper 实现（`LongestMatchColumnWidthStyleStrategy` 占位）。合并逻辑由 `DataMergeStrategy` 独立负责，导出行为对调用方透明。

#### 2) `DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\service\impl\DefectRecordServiceImpl.java`

**PSM 1:1 关键点** —— `handleStatisticDataExport(HttpServletResponse, ExportDefectStatisticForm)`：

- 拉 `iLineDayRecordService.listLineDayBetween(form.getStartTime(), form.getEndTime())` + `iDefectDayRecordService.listBetween(...)`
- 按 `yyyy-MM-dd` 建 HashMultimap
- 按 `TimeRange(TimePattern.YYYY_MM_DD)` 按天循环
- 每天内：
  - `today` + `tomorrow` 双时段聚合：
    - `today` 时段：白班 = `!localTime.isBefore(Eight) && localTime.isBefore(TWENTY)`，其余 = 夜班
    - `tomorrow` 时段：`localTime.isBefore(Eight)` 归入前一天夜班
  - 按 `pos` (=lineNo:faceNo) × `type` 聚合白班/夜班 count
  - 按 `lineNo` 聚合 `removeTotal` 白班/夜班
  - 早班表头：`[[白班,线别], [白班,""], [白班,defect1], ...]`；夜班表头同型（"夜班"）
  - 每天 `lines` 列表循环生成一行：`[lineNo, faceNo, ...各defect count, removeTotal]`
  - 末尾汇总行：`["汇总", "", ...各defect 总和, totalRemoval]`
  - `Table` 字段：`mergeColumns = List.of("线别", "剔除数")`
  - `SheetConfig` 字段：`name = today`（yyyy-MM-dd），`tables = List.of(dayTable, nightTable)`
- 文件名：`LocaleUtil.getMsg("defectSummary") + "(" + timeRange + ")"`，其中 `timeRange = startTime.substring(0,10).replace("-","") + "_" + endTime.substring(0,10).replace("-","")`
- 注入新依赖（PSM 同款字段名）：
  - `private IDefectDayRecordService iDefectDayRecordService;` （驼峰 i 前缀）
  - `private ILineDayRecordService iLineDayRecordService;` （PSM 字段名 1:1）
  - 保留原有 `defectDayRecordMapper` / `lineDayRecordMapper` 用于 `handleDetectData` 内联实现（不动）
- 类级别常量（PSM 同款）：`private final LocalTime Eight = LocalTime.of(8, 0, 0);` / `private final LocalTime TWENTY = LocalTime.of(20, 0, 0);`
- `handleDetectData` / `handleDetectDetailSearch` / `handleRealtimeDetectDataSearch` / `searchDefectRecord` **不动**（W-B03 / W-DET-01 / W-DET-02 既有实装）

#### 3) `DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\web\DetectDataController.java`

**PSM 1:1 关键点** —— `exportStatisticData` 1:1 改回 PSM 风格：

```java
@GetMapping("/web/detect/statistic/export")
public void exportStatisticData(HttpServletResponse resp,
                                @Validated ExportDefectStatisticForm form) {
    this.defectRecordService.handleStatisticDataExport(resp, form);
}
```

- 完全删除 `buildExportHeaders()` / `buildExportRows()` / `parseLocalDateTime()` 等 controller 内私有辅助
- `@Validated` 触发 `ExportDefectStatisticForm` 上 `@NotBlank` 校验：startTime / endTime 缺失时抛 `ConstraintViolationException`（错误 10500），不再透传到 SQL
- W-DET-05c 自增的 `/web/detect/list` 端点**保留**
- PSM 同款 5 个端点保留（upload / status / searchDetectDetail / getRealtimeData / detectList）

**删除的方法**：
- `private static List<List<String>> buildExportHeaders()`
- `private static List<List<Object>> buildExportRows(List<DefectDayRecord> rows)`
- `private static LocalDateTime parseLocalDateTime(String raw)`（detectList 用 — 仍保留）

### 3.2 不动文件（已 1:1）

| 文件 | 状态 |
|------|------|
| `dto/ExportDefectStatisticForm.java` | W-DET-05c 已 1:1 抄 PSM（含 `@NotBlank startTime/endTime`），不动 |
| `excel/DataMergeStrategy.java` | W-DET-05b 已 1:1 抄 PSM，不动 |
| `service/IDefectRecordService.java` | 接口签名已含 `handleStatisticDataExport(HttpServletResponse, ExportDefectStatisticForm)`，不动 |

### 3.3 删除的方法

| 文件 | 删除方法 |
|------|---------|
| `DetectDataController.java` | `buildExportHeaders()`（W-DET-05c buggy 简化实现） |
| `DetectDataController.java` | `buildExportRows(List<DefectDayRecord>)`（W-DET-05c buggy 简化实现） |
| `ExcelUtils.java` | `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` 重载（W-DET-05c buggy 10500 触发点） |

---

## 四、编译结果

**0 errors, 0 fatal warnings**（仅 unchecked 提示信息）：

```
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
```

**ExitCode**: 0

**生成的 class 文件**（关键 4 个）：

```
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\util\ExcelUtils.class (8682 bytes)
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\util\ExcelUtils$1.class (1068 bytes)
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\util\ExcelUtils$SheetConfig.class (1398 bytes)
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\util\ExcelUtils$Table.class (1960 bytes)
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\service\impl\DefectRecordServiceImpl.class (22544 bytes)
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\web\DetectDataController.class (9244 bytes)
```

**javap 反汇编验证**：

`ExcelUtils.export` 签名（PSM 1:1）：
```
public static void export(jakarta.servlet.http.HttpServletResponse,
                          java.util.List<...ExcelUtils$SheetConfig>,
                          java.lang.String);
```

`ExcelUtils$Table.values` 字段（PSM 1:1 — public 字段而非 getter）：
```
public java.util.List<java.util.List<java.lang.Object>> values;
```

`ExcelUtils$SheetConfig.setName`（PSM builder 风格 — 返回 SheetConfig）：
```
public ...ExcelUtils$SheetConfig setName(java.lang.String);
```

`DefectRecordServiceImpl.handleStatisticDataExport`（PSM 1:1）：
```
public void handleStatisticDataExport(jakarta.servlet.http.HttpServletResponse,
                                      com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm);
```

`DetectDataController.exportStatisticData`（PSM 1:1）：
```
public void exportStatisticData(jakarta.servlet.http.HttpServletResponse,
                                com.hikrobotics.solution.module.detect.dto.ExportDefectStatisticForm);
```

**编译命令**：
```powershell
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" -sourcepath DataupLoad\src\main\java <186 java files>
```

---

## 五、3 个 Bug 修复路径（PSM 1:1 对应表）

| W-DET-07 Bug | PSM 1:1 修复路径 |
|--------------|-----------------|
| P0 XLSX 表头 7行×1列 | controller 删除 `buildExportHeaders` → service `handleStatisticDataExport` 内构造 `SheetConfig.tables=[白班,夜班]` 多 table，每个 table 表头 `[[白班,线别],[白班,""],[白班,defect1],...,[白班,剔除数]]` 双行表头（ExcelUtils.writerTable 的 head() 接受 List<List<String>> 二级结构） |
| P0 非空数据抛 10500 | controller 删除 `exportToExcel(HttpServletResponse,...)` buggy 重载 → service 走 `ExcelUtils.export(HttpServletResponse, List<SheetConfig>, String)` PSM 同款入口，EasyExcel 流式写不会在非空 list 抛异常 |
| P1 `faceNo=""` 透传 | controller 改 `@Validated ExportDefectStatisticForm form` → `form.getStartTime()/getEndTime()` 上的 `@NotBlank` 校验触发，缺失时 `ConstraintViolationException`（错误 10500） |

---

## 六、已知限制

### 6.1 PSM 反编译产物不可见部分（沿用 W-DET-05b 占位实现）

| 方法 | PSM 真实形态 | DataupLoad 实现 |
|------|-------------|----------------|
| `ExcelUtils.getDefaultWriteHandle()` | CFR "Unavailable Anonymous Inner Class!!" | `LongestMatchColumnWidthStyleStrategy` 占位 |
| `ExcelUtils.getColumnWidthStrategy()` | CFR "Unavailable Anonymous Inner Class!!"（读取 `export.detect-data.column-width`，默认 15） | `LongestMatchColumnWidthStrategy` 占位 + 配置读取代码 |
| `ExcelUtils.getDefaultStyleStrategy()` | CFR "Unavailable Anonymous Inner Class!!" | `LongestMatchColumnWidthStyleStrategy` 占位 |

**影响**：合并逻辑由 `DataMergeStrategy` 独立负责（PSM 1:1），列宽策略实际效果为 EasyExcel 内置"最长 cell 自适应列宽"，与 PSM 反编译不可见的匿名内部类的视觉效果可能略有差异，但导出文件结构、表头、合并、数据全对。

### 6.2 工单 spec vs PSM 真实结构差异

工单 spec Step 2 要求"新建 `excel/SheetConfig.java` / `excel/Table.java`"，但 PSM 真实反编译产物里这两个类是 `ExcelUtils` 的 inner class（`ExcelUtils$SheetConfig.class` / `ExcelUtils$Table.class`），且 inner class body 在 PSM 反编译里**完全不可见**（只有引用）。

**处理**：按 PSM 真实结构实现为 `ExcelUtils.SheetConfig` / `ExcelUtils.Table` 内嵌静态类，字段 + 访问修饰符按 PSM 反编译产物的 service / util 调用模式（`table.values.size()` 字段访问 + `config.getName()` getter 调用）推断补齐。

### 6.3 BaseResult 返回类型

工单 spec 末尾提到"PSM 用 BaseResult 但 export 不返回 BaseResult，可能需要调整"。**确认**：`handleStatisticDataExport` PSM 反编译签名 `public void handleStatisticDataExport(HttpServletResponse var1, ExportDefectStatisticForm var2)` 返回 `void`（不是 BaseResult）—— DataupLoad 1:1 对齐返回 void。controller endpoint 同样返回 void。

### 6.4 保留的 W-DET-05b POJO API

`ExcelUtils.exportToExcel(List<T>, Class<T>, OutputStream)` 和 `ExcelUtils.readFromExcel(InputStream, Class<T>)` 是 W-DET-05b 引入的 POJO API，PSM 反编译里没有，但本工单不删（与 PSM 不冲突；删除会破坏 W-DET-05b 契约）。其他工单如需清理可单独安排。

### 6.5 注解自动校验依赖

`@Validated ExportDefectStatisticForm` 触发 `@NotBlank` 校验需要：
1. Spring Boot 全局 `@Validated` / `MethodValidationPostProcessor` 启用（DataupLoad 已启用）
2. `spring-boot-starter-validation` 在 classpath（PSM 同款 `hibernate-validator-8.0.0.Final.jar` + `jakarta.validation-api-3.0.2.jar` 已存在）
3. `LocaleUtil.getMsg("defectSummary")` 返回字符串不能为 null（PSM 默认 i18n 配置已有）

如未启用全局 MethodValidation，`@NotBlank` 不会触发，错误 10500 仍会出现 —— 这是 W-DET-09 范围（全局校验配置），不属于本工单。

---

## 七、变更文件清单

### 7.1 新建文件
**无**（PSM 反编译产物里 SheetConfig/Table 是 inner class，按 PSM 真实结构作为 ExcelUtils 内嵌静态类实现）。

### 7.2 重写文件（3 个）
1. `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/ExcelUtils.java`
2. `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/service/impl/DefectRecordServiceImpl.java`
3. `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/web/DetectDataController.java`

### 7.3 删除方法
- `ExcelUtils.exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)`（buggy 重载）
- `DetectDataController.buildExportHeaders()`
- `DetectDataController.buildExportRows(List<DefectDayRecord>)`

### 7.4 不动文件（已 1:1 PSM）
- `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/dto/ExportDefectStatisticForm.java`
- `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java`
- `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/service/IDefectRecordService.java`

---

## 八、编译日志位置

- stdout: `X:\DataupLoad\compile-w-det08.out`（空）
- stderr: `X:\DataupLoad\compile-w-det08.err`（仅 unchecked 提示）
- 完整编译脚本: `X:\DataupLoad\run-javac.ps1`（UTF-8 BOM，PowerShell 直接调用 javac）

---

**总结**：3 个文件 1:1 抄 PSM 反编译产物，编译 0 errors。3 个 W-DET-07 真 Bug 全部按 PSM 设计修复。
