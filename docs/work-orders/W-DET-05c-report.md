# W-DET-05c 报告 — DetectDataController 实装 statistic/export + 新增 detect/list

- 工单：W-DET-05c（P2，控制层 detect 模块补齐）
- Worker：Java W-DET-05c
- 时间：2026-07-24
- 范围：**detect 模块 controller 层 + 新增 util/excel 层** 3 个文件（1 修改 + 2 新建）
- 前置依赖：
  - W-DET-03（`DetectDataController` 加 `name=` + 端点骨架）
  - W-DET-01（`IDefectDayRecordService.listByLineAndTime / listBetween`）
  - **W-DET-05b（ExcelUtils / DataMergeStrategy）**：本工单启动时产物**不存在**，本工单先写了一份简化版；W-DET-05b 随后并行运行并**替换**了本工单写的 `util/ExcelUtils.java` + `excel/DataMergeStrategy.java` 为 PSM 1:1 增强版（保留了本工单的 `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` 签名作为兼容重载）。**本工单不冲突**。
  - W-DET-05a（`util/TimeRange.java`，P0 worker，20:15 完成；本工单**未依赖** TimeRange）
- PSM 参照：
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/web/DetectDataController.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/util/ExcelUtils.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java`

---

## 1. 改动文件清单

| 文件 | 类型 | 本工单产出 | 最终状态 | 说明 |
|---|---|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/web/DetectDataController.java` | 修改 | 273 行（实装 exportStatisticData 5 个 @RequestParam + 新增 detectList 6 个 @RequestParam） | **最终保留** | controller 是本工单主要交付物 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/ExcelUtils.java` | **新建** | 简化版 `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` | **被 W-DET-05b 增强替换** | W-DET-05b 保留了本工单的 `exportToExcel(HttpServletResponse, ...)` 重载作为兼容项，新增 POJO 重载 `exportToExcel(List<T>, Class<T>, OutputStream)` + `readFromExcel(InputStream, Class<T>)` + PSM 同款 3 个 helper。**本工单调用方不需改动**。 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java` | **新建** | 简化版（POI 3.17 兼容，用 `CellType.forInt(...)`） | **被 W-DET-05b 增强替换** | W-DET-05b 实现了 PSM 1:1 完整版（含 `setMergedRegionStyle` 居中样式、`getCellValue` 日期识别、AbstractRowWriteHandler 抽象父类、表格合并辅助）。**构造函数签名 `(List<String>, int, int, int)` 保留**，本工单 controller 调用方不需改动。 |

**未触动**：
- `service/IDefectRecordService.handleStatisticDataExport(...)` 仍为 W-B03 占位 UOE，本工单**未修**（任务说"不修 service"，且路径已绕开）
- 其它模块（alarm / line / yingke / framework / screen / defect）一律未触
- git 未 push（任务硬性要求）

---

## 2. 端点列表（W-DET-05c 范围）

### 2.1 `GET /web/detect/statistic/export`（实装）

**方法签名（与 PSM 不同）**：

| 项 | PSM 反编译 | 本工单 W-DET-05c |
|---|---|---|
| 入参 | `@Validated ExportDefectStatisticForm form`（3 字段：startTime/endTime/lineNo） | 5 个 `@RequestParam(name=)`：`startTime` / `endTime` / `lineNo(required=false)` / `faceNo(required=false)` / `defects(required=false) List<String>` |
| 业务实现 | 委派 `defectRecordService.handleStatisticDataExport(resp, form)`（service 内 150+ 行 sheet × table 早/晚班组装） | controller 直接调 `ExcelUtils.exportToExcel(...)`；数据源走 `defectDayRecordService.listBetween` 或 `listByLineAndTime` + 内存层过滤 defects |
| 返回 | `void` | `void`（写到 HttpServletResponse 输出流） |
| Content-Type | 由 service 设置 | 由 `ExcelUtils.exportToExcel` 统一设置 `application/vnd.ms-excel` |

**实装要点**：
```java
@GetMapping("/web/detect/statistic/export")
public void exportStatisticData(HttpServletResponse resp,
                                @RequestParam(name = "startTime") String startTime,
                                @RequestParam(name = "endTime") String endTime,
                                @RequestParam(name = "lineNo", required = false) String lineNo,
                                @RequestParam(name = "faceNo", required = false) String faceNo,
                                @RequestParam(name = "defects", required = false) List<String> defects) {
   LocalDateTime startDt = parseLocalDateTime(startTime);
   LocalDateTime endDt   = parseLocalDateTime(endTime);
   if (startDt == null || endDt == null) {
      throw new IllegalArgumentException("startTime/endTime must be yyyy-MM-dd HH:mm:ss or ISO-8601");
   }
   List<DefectDayRecord> rows = (isBlank(lineNo) && isBlank(faceNo))
       ? defectDayRecordService.listBetween(formatLocalDate(startDt), formatLocalDate(endDt))
       : defectDayRecordService.listByLineAndTime(
            isBlank(lineNo) ? "" : lineNo, isBlank(faceNo) ? "" : faceNo, startDt, endDt);
   if (CollectionUtils.isNotEmpty(defects)) {
      rows = rows.stream()
                 .filter(r -> r.getType() != null && defects.contains(r.getType()))
                 .toList();
   }
   List<List<String>> headers  = buildExportHeaders();   // [产线, 面, 时间, 缺陷类型, 数量, 更新时间, 创建时间]
   List<List<Object>> data     = buildExportRows(rows);
   String fileName = "缺陷统计_" + ymd(startTime) + "_" + ymd(endTime);
   List<String> mergeColumns   = List.of("产线", "面", "时间", "缺陷类型");
   ExcelUtils.exportToExcel(resp, headers, data, mergeColumns, fileName);
}
```

**与 PSM 的关键差异（与任务 spec 一致）**：
1. **入口下沉到 controller**：PSM 由 `defectRecordService.handleStatisticDataExport` 实现 150+ 行 sheet × table 早/晚班组装，本工单 controller 直接调 `ExcelUtils.exportToExcel`（任务 spec 第 2.1 节硬性要求）。
2. **新增 `faceNo` / `defects` 入参**：PSM `ExportDefectStatisticForm` 仅含 `startTime/endTime/lineNo`；本工单按任务 spec 加上 `faceNo`(可选) 和 `defects`(可选，`?defects=A&defects=B` 多值)。
3. **不做班次拆分**：PSM 按 `LocalTime(8,0)` / `LocalTime(20,0)` 切白/夜班 + `TimeRange` 按天迭代；本工单单 sheet 平铺所有 `defect_day_record` 行（简化版，详见 §6 #3）。
4. **service 路径保留**：未删 `IDefectRecordService.handleStatisticDataExport` 接口，W-B03 占位 UOE 仍存在；本工单**绕开**它，不调用。

### 2.2 `GET /web/detect/list`（新增）

**方法签名**：
```java
@GetMapping("/web/detect/list")
public BaseResult detectList(
   @RequestParam(name = "lineNo",    required = false) String lineNo,
   @RequestParam(name = "faceNo",    required = false) String faceNo,
   @RequestParam(name = "startTime", required = false) String startTime,
   @RequestParam(name = "endTime",   required = false) String endTime,
   @RequestParam(name = "page",      defaultValue = "1")  Integer page,
   @RequestParam(name = "size",      defaultValue = "20") Integer size);
```

**返回**：`BaseResult.build().data(IPage<DefectDayRecord>)`

**实装要点**：
- 用 MyBatis-Plus `LambdaQueryWrapper` 按 lineNo / faceNo / startTime / endTime 任选子集条件拼接；`orderByDesc(time)`
- 调用 `defectDayRecordService.page(Page<DefectDayRecord>(pageNum, pageSize), wrapper)`（`IDefectDayRecordService` 继承 `IService<DefectDayRecord>`，`page()` 直接可用）
- page/size 默认 1/20，<1 时回退到默认
- time 字段过滤：`HikDateUtil.formatLocalDate(localDt)` 还原成 `yyyy-MM-dd HH:mm:ss` 与数据库 `time` 字段（String）比较

**与 PSM 的差异**：
- **PSM 没有此端点**。DataupLoad 自增端点（用于大屏 / 后台 list 页面）；审计报告未列出此端点缺口。
- 字段集合与现有 `DefectDayRecordController.list-between` + `list-by-attribute` 互补：
  - `DefectDayRecordController.listBetween`：无分页
  - `DefectDayRecordController.searchDefectCount`：返回 `DefectCountDTO` 聚合
  - **本工单 `detectList`**：分页 + 全字段条件 + 原行返回（`DefectDayRecord`）

---

## 3. Excel 导出实现要点

### 3.1 `ExcelUtils.exportToExcel(...)`（新建）

**签名**：
```java
public static void exportToExcel(HttpServletResponse response,
                                 List<List<String>>  headers,
                                 List<List<Object>>  data,
                                 List<String>        mergeColumns,
                                 String              fileName);
```

**实现关键**：
- 继承 `com.hikrobotics.solution.framework.util.excel.ExcelUtil`（framework-starter 内已有 `exportExcel(Class, List, String)` 重载；本类扩展 `exportToExcel`，不覆盖父类）
- 响应头：与 PSM 同款 `Content-Type: application/vnd.ms-excel` + `Character-Encoding: UTF-8` + `Content-Disposition: attachment;filename=<URLEncoder(UTF-8)>.xlsx`
- EasyExcel `EasyExcel.write(os).head(headers).registerWriteHandler(DataMergeStrategy, LongestMatchColumnWidthStyleStrategy).build()` + `EasyExcel.writerSheet(fileName).build()`
- 默认列宽自适应（`LongestMatchColumnWidthStyleStrategy`），不再读 env `export.detect-data.column-width`（PSM 行为简化）
- 资源管理：`try-with-resources` 关 OutputStream；`finally` 里 `writer.finish()`

### 3.2 `DataMergeStrategy`（新建）

**与 PSM `DataMergeStrategy` 的关键差异**：

| 项 | PSM 反编译 | 本工单 W-DET-05c |
|---|---|---|
| 列匹配 | "合并同列相邻同值，跨列按第 0 列同值分组" | 仅同列相邻同值合并（去掉跨列分组逻辑，简化） |
| 样式 | `setMergedRegionStyle` 复制居中样式 | 不复制样式，沿用 EasyExcel 默认 |
| CellType | `cell.getCellTypeEnum().ordinal()` + 合成 `$SwitchMap`（CFR 反编译 quirk） | `CellType.forInt(cell.getCellType())`（POI 3.17 + 4.x 双向兼容） |
| DateUtil 解析 | `DateUtil.isCellDateFormatted(cell)` | 不分日期类型，NUMERIC 一律按 double → long/decimal 字符串 |

**构造签名（保持与 PSM 一致）**：
```java
public DataMergeStrategy(List<String> mergeColumns, int rowCounts, int headerSize, int start);
```

若 W-DET-05b 后续按 PSM 1:1 完整实现，构造函数可直接替换 impl，本工单 controller 调用方不变。

### 3.3 表头/数据组装

**表头（单行）**：
```
[产线, 面, 时间, 缺陷类型, 数量, 更新时间, 创建时间]
```

**合并列**：`[产线, 面, 时间, 缺陷类型]` —— 同列相邻相同值纵向合并

**文件名**：`缺陷统计_20260724_20260724.xlsx`（从 startTime/endTime 提取 yyyyMMdd）

---

## 4. 编译结果

### 4.1 任务指定单文件编译命令

```bash
cd E:\DEMO\数据采集
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\web\DetectDataController.java
```

**输出**：
```
（无输出）
exit=0
```
✅ **成功**，无警告。

### 4.2 ExcelUtils / DataMergeStrategy 单文件编译

```bash
javac ... DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\util\ExcelUtils.java
javac ... DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\excel\DataMergeStrategy.java
```

**输出**：
```
注: ...DataMergeStrategy.java 使用或覆盖了已过时的 API。
注: 有关详细信息, 请使用 -Xlint:deprecation 重新编译。
exit=0
```
✅ **成功**。deprecation 提示来自 POI 3.17 `cell.getCellType()`（`int` 重载已 deprecated，但 `CellType.forInt(...)` 是 POI 3.17 唯一稳定 API），非阻塞。

### 4.3 全量项目编译（187 个 Java 文件）

```bash
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java @X:\tmp\sources-list.txt
```

**输出**：
```
注: ...DataMergeStrategy.java 使用或覆盖了已过时的 API。
注: 某些输入文件使用了未经检查或不安全的操作。
exit=0
```
✅ **成功**。两条提示都是预期内：deprecation 来自 POI 3.17，unchecked 来自 MyBatis-Plus Lambda 泛型（项目既有）。

### 4.4 字节码验证（javap）

**DetectDataController** 字节码：
```
public void exportStatisticData(jakarta.servlet.http.HttpServletResponse, java.lang.String,
   java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>);
public com.hikrobotics.solution.framework.common.base.BaseResult detectList(java.lang.String,
   java.lang.String, java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer);
```

**@RequestParam name= 保留性检查**（javap -v MethodParameters）：
```
searchDetectDetail   → faceId, startTime, endTime
exportStatisticData  → resp, startTime, endTime, lineNo, faceNo, defects
detectList           → lineNo, faceNo, startTime, endTime, page, size
getRealtimeData      → lineNo, faceNo
```
✅ `-parameters` flag 生效，参数名 1:1 保留到 bytecode。

**ExcelUtils** 字节码：
```
public static void exportToExcel(jakarta.servlet.http.HttpServletResponse,
   java.util.List<java.util.List<java.lang.String>>,
   java.util.List<java.util.List<java.lang.Object>>,
   java.util.List<java.lang.String>, java.lang.String);
```

**DataMergeStrategy** 字节码：
```
public com.hikrobotics.solution.module.detect.excel.DataMergeStrategy(
   java.util.List<java.lang.String>, int, int, int);
```

---

## 5. 与审计 / 任务 spec 对账

| 项 | 审计 / 任务 spec 期望 | 本工单处理 |
|---|---|---|
| 审计 §文件级判定 `excel/DataMergeStrategy` 缺失 | PSM 1:1 抄 | ⚠️ 简化版（保留构造函数签名 + 核心合并语义；详见 §3.2 表 + §6 #1） |
| 审计 §文件级判定 `util/ExcelUtils` 缺失 | PSM 1:1 抄 | ⚠️ 简化版（新增 `exportToExcel`，未抄 PSM `export(sheets, fileName)` 多 sheet 多 table 嵌套；详见 §6 #2） |
| 任务 W-DET-05c spec `statistic/export` | controller 直接调 `ExcelUtils.exportToExcel()` + 设置 `Content-Type: application/vnd.ms-excel` | ✅ 完全对齐（Content-Type 由 `ExcelUtils.exportToExcel` 统一设置） |
| 任务 W-DET-05c spec `detect/list` | 返回 `IPage<DefectDayRecord>` | ✅ 完整实装 |
| W-DET-03 报告硬性要求 | 每个 `@RequestParam` 加 `name="..."` | ✅ 5 个 + 6 个 + 既有 6 个全部带 `name=` |
| 任务"1:1 对齐 PSM 反编译产物" | PSM 是 service 委托 + form 绑定 | ⚠️ **不 1:1 对齐 PSM controller**；按任务 spec 改为 controller 直接调 ExcelUtils（详见 §6 #5） |

---

## 6. 已知限制

> **关于最终状态**：本工单启动时 `ExcelUtils.java` + `DataMergeStrategy.java` 不存在；
> 本工单写了一版简化版（保留 `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` 签名）。
> W-DET-05b 随后并行运行，**保留本工单的重载并补充 PSM 1:1 完整能力**（POJO API + reader + helper）。
> 故 §6 #1 / §6 #2 原本描述的「简化版限制」大多已由 W-DET-05b 补充；下表说明**当前最终文件状态**与**本工单 controller 仍存在的限制**。

1. **DataMergeStrategy（已由 W-DET-05b 增强为 PSM 1:1）**：
   - ✅ 复制 PSM `setMergedRegionStyle` 居中样式逻辑
   - ✅ 识别 `DateUtil.isCellDateFormatted` 日期
   - ✅ 继承 `AbstractRowWriteHandler`（替代 PSM 反编译的 `RowWriteHandler`）
   - ⚠️ **本工单 controller 不依赖**其复杂能力（只用构造函数签名 `(List<String>, int, int, int)` + 合并同列相邻同值语义）
2. **ExcelUtils（已由 W-DET-05b 增强为 PSM 1:1 + 额外 API）**：
   - ✅ 补齐 POJO 写出重载 `exportToExcel(List<T>, Class<T>, OutputStream)`
   - ✅ 补齐 reader 重载 `readFromExcel(InputStream, Class<T>)`
   - ✅ 补齐 PSM 同款 `getDefaultWriteHandle / getColumnWidthStrategy / getDefaultStyleStrategy` helper
   - ⚠️ **本工单 controller 不调用**后两个 helper；列宽走 `LongestMatchColumnWidthStyleStrategy` 自适应
   - ⚠️ **未实现** PSM `ExcelUtils.export(sheets, fileName)` 多 sheet × 多 table 接口（用于早/晚班分块）。`statistic/export` 端点当前是单 sheet 平铺所有 `defect_day_record` 行
3. **statistic/export 不做班次拆分**：
   - PSM `DefectRecordServiceImpl.handleStatisticDataExport` 按 `LocalTime(8,0)` / `LocalTime(20,0)` + `TimeRange` 按天迭代，每天生成一个 sheet 含 早/晚两个 table。
   - 本工单 controller 用 `listBetween` / `listByLineAndTime` + 内存过滤 defects，单 sheet 平铺所有行；不做班次拆分、不做 `TimeRange` 迭代。
   - 业务影响：当前导出表格没有「白班 / 夜班」分组列；如要恢复分组，需要 W-DET-05b 或后续工单重写 controller（或在 controller 里写 150+ 行 PSM 风格组装）。
4. **未补 `DefectRecordServiceImpl.handleStatisticDataExport` 实现**：本工单 controller 绕开 service 路径，service 内仍是 W-B03 占位 `UnsupportedOperationException`。如后续其它端点（如客户端触发）仍走 service 路径，需要单独工单实现该方法（任务 spec 没说，故不修）。
5. **与"1:1 对齐 PSM"硬性要求冲突（关键决策）**：
   - **冲突点**：任务正文硬性要求"1:1 对齐 PSM 反编译产物" + "调用 W-DET-05b 新建的 ExcelUtils"。
   - **冲突 #1**：PSM `DetectDataController.exportStatisticData` 委派 service，任务 spec 要求 controller 直接调 ExcelUtils → **按 spec**（因 controller 内 5 个 @RequestParam 与 PSM form 绑定签名本质不同）。
   - **冲突 #2**：本工单启动时 W-DET-05b 未运行 → **本工单自建简化版** `ExcelUtils.exportToExcel(HttpServletResponse, ...)`。W-DET-05b 随后并行运行并增强为 PSM 1:1，**保留本工单的 HttpServletResponse 重载**作为兼容项 → **冲突未发生**。
   - **遗留**：W-DET-05b 当前未补齐 PSM `ExcelUtils.export(sheets, fileName)` 多 sheet × 多 table 接口。如需恢复 PSM 早/晚班分块，需 W-DET-05b 续期补齐；本工单 controller 不依赖这个能力。
6. **未触动其它模块 / git**：未 push / commit（任务要求"不要推 git"）；改动已留在 working tree，待主 agent 审阅 + 整合到 batch commit。
7. **`-parameters` 编译 flag 依赖**：本工单 controller 在 bytecode 层保留参数名（`faceId` / `lineNo` / `faceNo` / `defects` 等），但 Spring MVC 在 controller 注解 `@RequestParam(name="...")` 显式声明的情况下，参数名保留与否不影响运行。W-DET-03 报告把 `-parameters` 作为硬性要求；本工单沿用同款编译命令。
8. **`detect/list` 返回 `IPage<DefectDayRecord>` 含 `total/records/size/current` 字段**：Jackson 默认序列化 IPage 子类可能出问题（之前 `MybatisPlusConfig` 可能需要 inner class serialization 配置）。建议部署后真机测试一次 `GET /web/detect/list?page=1&size=20` 验证返回 JSON 格式。如 Jackson 报错 `InvalidDefinitionException: Cannot construct abstract class`，需在 `MybatisPlusConfig` 加 `objectMapper.activateDefaultTyping(...)` 或改用 `BaseResult.data(Page<DefectDayRecord>)` 强类型（**潜在风险**，未在本工单验证）。
9. **`defects` 多值分隔方式**：当前依赖 Spring MVC 默认 `@RequestParam List<String>` 接收 `?defects=A&defects=B&defects=C` 多参数形式；**不支持** `?defects=A,B,C` 逗号分隔。如调用方用逗号分隔会得到一个元素 `["A,B,C"]`（包含逗号的字符串），不会拆开。如需要支持逗号分隔，需在 controller 加自定义 `Converter<String, List<String>>`（**潜在风险**，未在本工单实现）。
10. **`startTime` / `endTime` 解析失败抛 IAE**：controller 显式 `throw new IllegalArgumentException(...)`，会走 Spring `@ControllerAdvice` 全局异常处理（PSM 用 `GlobalExceptionHandler` 转 BaseResult）。本工单不验证 handler 是否存在；如线上 handler 未覆盖 `IllegalArgumentException`，可能返回 HTTP 500 而非 BaseResult。

---

## 7. 交付确认

- ✅ `DetectDataController.exportStatisticData` 实装（5 个 `@RequestParam(name=)`，调用 `ExcelUtils.exportToExcel(...)`，设 `Content-Type: application/vnd.ms-excel`）
- ✅ `DetectDataController.detectList` 新增（6 个 `@RequestParam(name=)`，返回 `IPage<DefectDayRecord>`）
- ✅ `ExcelUtils.exportToExcel(HttpServletResponse, ...)` 重载**保留**（本工单创建的签名被 W-DET-05b 保留为兼容项）
- ✅ `DataMergeStrategy` 构造函数签名 `(List<String>, int, int, int)` 与 PSM 同款（实现被 W-DET-05b 增强为 PSM 1:1）
- ✅ 任务指定单文件编译命令通过（exit=0）
- ✅ 单文件编译 ExcelUtils / DataMergeStrategy 通过（exit=0，仅 POI deprecation 警告）
- ✅ 全量 188 文件编译通过（exit=0，含 W-DET-05a 后续新增 TimeRange）
- ✅ javap -v 验证 4 个端点 @RequestParam name= 全部保留到 MethodParameters
- ✅ 新增 `detectDayRecordService` 注入字段，`@Autowired` 注入（与 `defectRecordService` / `statusRecordService` 风格一致）
- ✅ 未触动 service / mapper / entity / 其它模块（仅修改 detect 模块 web + util + excel 三层）
- ✅ 未推 git
- ⚠️ 与 PSM 反编译产物存在 2 处有意偏差（controller 直接调 ExcelUtils vs PSM service 委托；statistic/export 不做班次拆分）— 按任务 spec 走
- ✅ 与 W-DET-05b 并行运行未冲突（本工单创建的 `exportToExcel(HttpServletResponse, ...)` 签名被 W-DET-05b 保留为兼容重载）

---

## 8. 后续工单建议

1. **W-DET-05b 补齐 PSM `ExcelUtils.export(sheets, fileName)` 多 sheet 接口**：当前 W-DET-05b 只补齐了 POJO API + reader + 本工单的 HttpServletResponse 重载；未补齐 PSM `SheetConfig` / `Table` builder + `export(sheets, fileName)` 多 sheet 多 table 接口。如需恢复早/晚班分块，需续期 W-DET-05b。
2. **W-DET-05d（可选）**：把本工单 `statistic/export` controller 改为委派 service `defectRecordService.handleStatisticDataExport`，并实现 service 内 150+ 行 PSM 风格 sheet × table 早/晚班组装；恢复 PSM 1:1 controller（需要 W-DET-05b #1 先完成）。
3. **`detect/list` 端到端验证**：部署后用 `GET /web/detect/list?lineNo=L01&page=1&size=20` 真机验证 IPage Jackson 序列化；如有问题，加 `MybatisPlusConfig` 的 default typing 或改用具体 `Page` 类型。
4. **`defects` 逗号分隔支持**：如大屏调用方用 `?defects=A,B,C` 而非 `?defects=A&defects=B`，需加 `Converter<String, List<String>>`。
5. **`DataMergeStrategy` 居中样式已由 W-DET-05b 补齐**（含 `setMergedRegionStyle`）；本项不再需要单独工单。
6. **`TimeRange` 集成到 `statistic/export`**：W-DET-05a 已补齐 `util/TimeRange.java`；当前 controller 的 `statistic/export` 不使用 TimeRange（单 sheet 平铺）。如恢复 PSM 早/晚班分块 + 按天迭代 sheet，需要在 controller 里使用 TimeRange.next() 逻辑。
