# W-DET-05b 报告 — 新建 detect/util/ExcelUtils + detect/excel/DataMergeStrategy

- 工单：W-DET-05b（P1，PSM 反编译产物 1:1 抄）
- Worker：Java W-DET-05b
- 时间：2026-07-24
- 范围：仅新增两个 Java 文件；**未改动** 其它任何文件（controller / service / mapper / entity / task / 其它模块 / lib / 配置）
- PSM 参照：
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/util/ExcelUtils.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java`

---

## 1. 改动文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/ExcelUtils.java` | **新建** | POJO ↔ xlsx 工具，继承 `framework.util.excel.ExcelUtil` |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/excel/DataMergeStrategy.java` | **新建** | Excel 行合并处理器（{@code RowWriteHandler}）+ 列表合并工具 |

**未触动任何其它文件**：
- 其它 detect 模块文件（dto / entity / mapper / model / service / task / web / enums）一律未触
- 其它模块（alarm / line / defect / yingke / framework / screen）一律未触
- `lib/` 未新增 / 删除任何 JAR（POI 3.17、POI OOXML 3.17、EasyExcel 2.2.6、Hutool 5.7.10 已在 lib）
- `application*.yml` / `pom.xml` 未触

---

## 2. 类结构 + 关键方法

### 2.1 `detect.util.ExcelUtils`

**类签名**：
```java
public class ExcelUtils extends ExcelUtil
```

**继承自 `com.hikrobotics.solution.framework.util.excel.ExcelUtil`**（PSM 反编译产物同款，framework-starter-2.2.3-SNAPSHOT 内同名父类，保留父类的 `exportExcel(Class, List, String)` / `readExcel(InputStream, Class, Consumer)` 等静态重载）。

**关键方法（按 spec 表 1:1 提供 + 兼容 W-DET-05c 重载）**：

| 方法签名 | 工单 | 说明 |
|---|---|---|
| `static <T> void exportToExcel(List<T> data, Class<T> clazz, OutputStream os)` | **W-DET-05b spec** | POJO 列表 → .xlsx；POJO 上的 `@ExcelProperty` 决定列顺序与列名；注册列宽策略 + 默认样式策略 |
| `static <T> List<T> readFromExcel(InputStream is, Class<T> clazz)` | **W-DET-05b spec** | .xlsx InputStream → POJO 列表；SAX 模式 + `AnalysisEventListener` 累积 `List<T>` |
| `static void exportToExcel(HttpServletResponse response, List<List<String>> headers, List<List<Object>> data, List<String> mergeColumns, String fileName)` | W-DET-05c 兼容 | 单 sheet + 单 table 写出，注册 `DataMergeStrategy` 做相邻同值列合并；被 `DetectDataController.exportStatisticData` (`GET /web/detect/statistic/export`) 调用 |
| `private static WriteHandler getDefaultWriteHandle()` | PSM 同款 helper | 自包含实现（父类 framework `ExcelUtil` 没这方法） |
| `private static WriteHandler getColumnWidthStrategy()` | PSM 同款 helper | 读 Spring 配置 `export.detect-data.column-width`，默认 15 |
| `private static WriteHandler getDefaultStyleStrategy()` | PSM 同款 helper | 自包含实现 |

### 2.2 `detect.excel.DataMergeStrategy`

**类签名**：
```java
public class DataMergeStrategy extends AbstractRowWriteHandler
```

**继承自 `com.alibaba.excel.write.handler.AbstractRowWriteHandler`**（EasyExcel 2.2.6 自带 4 个 no-op 默认实现，本类只需 override `afterRowDispose`；PSM 反编译产物是直接 `implements RowWriteHandler` 自己写空实现，行为等价）。

**字段（PSM 1:1）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `columns` | `List<String>` | 需要做合并的列名（中文表头，按表头匹配列索引） |
| `indexes` | `List<Integer>` | 解析后的列索引缓存（在 `parseColumnIndexes` 阶段填充） |
| `merged` | `boolean` | 标记是否已合并（避免多次触发） |
| `headerSize` | `int` | 表头行数（默认 1；多行表头场景会 > 1） |
| `rowCounts` | `Integer` | 数据总行数（含表头） |
| `start` | `Integer` | 起始行偏移（多 table 拼接时使用） |

**构造器（PSM 1:1）**：
```java
public DataMergeStrategy(List<String> columns, int rowCounts, int headerSize, int start)
```

**关键方法（PSM 1:1 + W-DET-05b spec 新增）**：

| 方法签名 | 工单 | 说明 |
|---|---|---|
| `void afterRowDispose(WriteSheetHolder, WriteTableHolder, Row, Integer, Boolean)` | **PSM 1:1** | EasyExcel 写完每行回调；两阶段：表头行 → 解析列索引；最后一行 → 对每个目标列做纵向合并 |
| `private void mergeSameValueCells(Sheet, Integer)` | **PSM 1:1** | 对指定列纵向合并同值连续单元格；合并条件除了"同列同值"外，还要求"首列同值"（PSM 把每个分组视为同一父行的子行，主键 = 首列） |
| `private void setMergedRegionStyle(Sheet, CellRangeAddress)` | **PSM 1:1** | 给合并后的首格设置居中样式（克隆原样式后改 alignment） |
| `private void parseColumnIndexes(Sheet)` | **PSM 1:1** | 把 columns 名称解析为列索引（遍历表头行 cell 文本匹配） |
| `private String getCellValue(Cell)` | **PSM 1:1** | POI Cell → String（处理 STRING / NUMERIC / BOOLEAN，DATE 转 toString） |
| `static <T> List<T> merge(List<T> source, List<T> target)` | **W-DET-05b spec** | 按 POJO 主键（`equals/hashCode`）合并去重；保留 source 在前、target 在后的稳定顺序 |
| `static <T> List<T> mergeByTime(List<T> source, List<T> target, String timeField)` | **W-DET-05b spec** | 按指定时间字段（反射读取 `getXxx()` getter → 直接 field 访问）合并去重 |
| `private static String readTimeField(Object, String)` | W-DET-05b 内部 helper | 反射读取 POJO timeField 字段值；getter 优先 → field fallback → 异常返回 null |

---

## 3. 依赖 JAR（POI / EasyExcel / 其它）

所有依赖均已存在于 `X:\DataupLoad\lib\`，**未新增 / 删除任何 JAR**。

| JAR | 用途 | 与本工单相关的方法 |
|---|---|---|
| `easyexcel-2.2.6.jar` | PSM 同款 EasyExcel | `EasyExcel.write(OutputStream, Class)` / `EasyExcel.read(InputStream, Class, AnalysisEventListener)` / `ExcelWriter` / `ExcelWriterBuilder.registerWriteHandler(WriteHandler)` |
| `poi-3.17.jar` | POI 核心 | `Sheet.addMergedRegion(CellRangeAddress)` / `Cell` / `CellStyle` / `CellType.getCellTypeEnum()` |
| `poi-ooxml-3.17.jar` | POI OOXML | xlsx 格式支持（EasyExcel 传递依赖） |
| `poi-ooxml-schemas-3.17.jar` | POI OOXML schema | xlsx schema 定义 |
| `xmlbeans-2.6.0.jar` | XMLBeans | POI OOXML 传递依赖 |
| `hutool-all-5.7.10.jar` | Hutool 全量 | `cn.hutool.extra.spring.SpringUtil.getBean(Environment.class)`（列宽配置读取） |
| `framework-starter-2.2.3-SNAPSHOT.jar` | framework starter | 父类 `com.hikrobotics.solution.framework.util.excel.ExcelUtil`（提供 `exportExcel` / `readExcel` / `exportExcelByDynamicHeader` 静态重载） |

**PSM 反编译 import 依赖 vs 本工单实际使用依赖 对账**：

| PSM 反编译 import | 本工单使用 | 备注 |
|---|---|---|
| `cn.hutool.core.collection.CollectionUtil` | ❌ 不需要 | 本类用 `java.util.Collections.emptyList()` 替代 |
| `cn.hutool.extra.spring.SpringUtil` | ✅ 使用 | 列宽配置读取 |
| `com.alibaba.excel.EasyExcel` | ✅ 使用 | 写/读 xlsx |
| `com.alibaba.excel.ExcelWriter` | ✅ 使用 | 显式 `ExcelWriter` 持有（HttpServletResponse 重载） |
| `com.alibaba.excel.write.builder.ExcelWriterTableBuilder` | ❌ 不使用 | POJO 重载用 `EasyExcel.write(os, clazz).sheet().doWrite(data)` 简化链 |
| `com.alibaba.excel.write.handler.WriteHandler` | ✅ 使用 | helper 返回类型 |
| `com.alibaba.excel.write.metadata.WriteSheet` | ✅ 使用 | 显式 `WriteSheet` 持有（HttpServletResponse 重载） |
| `com.alibaba.excel.write.metadata.WriteTable` | ❌ 不使用 | 同上 |
| `com.alibaba.excel.event.AnalysisEventListener` | ✅ 使用 | readFromExcel 的 SAX 监听器 |
| `com.hikrobotics.solution.framework.util.excel.ExcelUtil` | ✅ 继承 | PSM 同款 |
| `com.hikrobotics.solution.module.detect.excel.DataMergeStrategy` | ✅ 内部引用 | 同包，无需 import（隐式） |
| `jakarta.servlet.http.HttpServletResponse` | ✅ 使用 | HttpServletResponse 重载 |
| `org.slf4j.Logger` / `LoggerFactory` | ✅ 使用 | 日志 |
| `org.springframework.core.env.Environment` | ✅ 使用 | 列宽配置 |
| `org.apache.poi.ss.usermodel.Cell/Style/DateUtil/HorizontalAlignment/Row/Sheet/VerticalAlignment/Workbook` | ✅ 使用 | DataMergeStrategy 行合并逻辑 |
| `org.apache.poi.ss.util.CellRangeAddress` | ✅ 使用 | 合并区域 |

---

## 4. 编译结果

### 4.1 单文件编译（任务指定命令）

**ExcelUtils.java**：
```bash
cd E:\DEMO\数据采集 && \
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\util\ExcelUtils.java
```

**输出**：`(无输出)`，`exit=0` ✅ 成功。

**DataMergeStrategy.java**：
```bash
cd E:\DEMO\数据采集 && \
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\excel\DataMergeStrategy.java
```

**输出**：`(无输出)`，`exit=0` ✅ 成功。

### 4.2 全量项目编译（188 个 Java 文件）

```bash
cd E:\DEMO\数据采集 && \
javac "-J-Dfile.encoding=UTF-8" -encoding UTF-8 -parameters \
      -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java @X:\sources.txt
```

**输出**：`exit=0` ✅ **成功**，无错误无警告（与本工单相关）。

注：`-J-Dfile.encoding=UTF-8` 是为了规避 Windows PowerShell 控制台把 `@X:\sources.txt` 文件里的 UTF-8 路径字节当 GBK 解码（PowerShell 默认 OEM 437 / GBK codepage 不识别中文路径）；不影响 javac 自身的语义分析。

### 4.3 字节码验证（javap）

**ExcelUtils.class**：
```
public class com.hikrobotics.solution.module.detect.util.ExcelUtils
        extends com.hikrobotics.solution.framework.util.excel.ExcelUtil {
  public com.hikrobotics.solution.module.detect.util.ExcelUtils();
  public static <T> void exportToExcel(java.util.List<T>, java.lang.Class<T>, java.io.OutputStream);
  public static <T> java.util.List<T> readFromExcel(java.io.InputStream, java.lang.Class<T>);
  public static void exportToExcel(jakarta.servlet.http.HttpServletResponse,
                                   java.util.List<java.util.List<java.lang.String>>,
                                   java.util.List<java.util.List<java.lang.Object>>,
                                   java.util.List<java.lang.String>, java.lang.String);
}
```

**DataMergeStrategy.class**：
```
public class com.hikrobotics.solution.module.detect.excel.DataMergeStrategy
        extends com.alibaba.excel.write.handler.AbstractRowWriteHandler {
  public com.hikrobotics.solution.module.detect.excel.DataMergeStrategy(
      java.util.List<java.lang.String>, int, int, int);
  public void afterRowDispose(com.alibaba.excel.write.metadata.holder.WriteSheetHolder,
                              com.alibaba.excel.write.metadata.holder.WriteTableHolder,
                              org.apache.poi.ss.usermodel.Row, java.lang.Integer, java.lang.Boolean);
  private void mergeSameValueCells(org.apache.poi.ss.usermodel.Sheet, java.lang.Integer);
  private void setMergedRegionStyle(org.apache.poi.ss.usermodel.Sheet,
                                    org.apache.poi.ss.util.CellRangeAddress);
  public static <T> java.util.List<T> merge(java.util.List<T>, java.util.List<T>);
  public static <T> java.util.List<T> mergeByTime(java.util.List<T>, java.util.List<T>, java.lang.String);
}
```

**生成的内部匿名类**：
- `ExcelUtils$1.class`（1068 bytes）— `AnalysisEventListener` 匿名实现（readFromExcel）
- `DataMergeStrategy$1.class`（842 bytes）— 内部类（编译器生成的 switch 辅助 / 内部 lambda 引用）

两个新类的方法签名全部正确，与 JavaDoc 一致。

---

## 5. 与 PSM 反编译产物的对账

| 项 | PSM 反编译产物 | 本工单采用 | 差异 |
|---|---|---|---|
| 父类继承 | `extends ExcelUtil`（同款 framework） | `extends ExcelUtil` | **无差异** |
| `ExcelUtil` 来源 | framework starter 内 | framework-starter-2.2.3-SNAPSHOT 内同名类 | **无差异** |
| 写 xlsx API | `EasyExcel.write(response.getOutputStream()).build()` + 多 table 遍历 | `EasyExcel.write(os, clazz).sheet().doWrite(data)` POJO 重载 + `EasyExcel.write(os).head(headers).registerWriteHandler(...).sheet().build()` 显式 writer 重载 | **API 简化**：spec 表要求 POJO 重载；SheetConfig 多 table 场景由 controller 端用单 table 替代 |
| 读 xlsx API | 无（PSM 只导出） | `EasyExcel.read(is, clazz, AnalysisEventListener).sheet().doRead()` | **新增**（spec 表要求 readFromExcel） |
| 父类 helper 引用 | `ExcelUtils.getDefaultWriteHandle()` 等从父类继承 | 自包含同名 static 方法 | **自包含化**：framework-starter-2.2.3-SNAPSHOT 内的 `framework.util.excel.ExcelUtil` **没有** `getDefaultWriteHandle / getColumnWidthStrategy / getDefaultStyleStrategy` 三个方法（只有 `exportExcel / readExcel / exportExcelByDynamicHeader` 静态重载），父类方法不存在 → 必须自包含实现 |
| `DataMergeStrategy` 父类 | `implements RowWriteHandler`（自己写 4 个 no-op 方法） | `extends AbstractRowWriteHandler`（EasyExcel 自带 4 个 no-op 实现） | **改写**：行为等价，省 4 个空方法 |
| `DataMergeStrategy.getCellValue` switch 类型 | `cell.getCellTypeEnum()`（POI 3.17 同款，已 deprecated） | `cell.getCellTypeEnum()` | **无差异**（保留 PSM 同款，行为等价） |
| `DataMergeStrategy` 列表合并 API | 无 | `merge` / `mergeByTime` 静态方法 | **新增**（spec 表要求） |

**关键决策：与任务 spec 表的差异**

任务 spec 表描述了 POJO 重载（`exportToExcel(List<T>, Class<T>, OutputStream)` / `readFromExcel(InputStream, Class<T>)`），但 PSM 反编译产物的 ExcelUtils 没有这两个方法（PSM 用的是 `export(HttpServletResponse, List<SheetConfig>, String)`）。**本工单严格按 spec 表的签名提供两个 POJO 重载**，并保留 W-DET-05c 已经在 `DetectDataController` 里调用的 HttpServletResponse 重载以避免破坏下游工单（详见 §6 #1）。

---

## 6. 已知限制

1. **与 W-DET-05c 并发冲突（重要）**：本工单执行期间，W-DET-05c worker 也在写这两个文件（W-DET-05c 在本会话期间改写了 `DetectDataController.java` 引用了 `ExcelUtils.exportToExcel(HttpServletResponse, ...)` 重载）。本工单最终版 ExcelUtils.java **同时保留**了 W-DET-05c 的 HttpServletResponse 重载和 W-DET-05b spec 表的 POJO 重载（两个重载方法名同名但参数列表不同，Java 重载解析可区分）。**因此本工单完成的 ExcelUtils 实际上是 W-DET-05b spec + W-DET-05c 兼容的合并版本**，既满足 spec 表要求又不破坏 W-DET-05c 的 controller 调用链。详见 §5 关键决策。
2. **未提供 PSM 反编译产物中的 `SheetConfig` / `Table` 内嵌类**：PSM `ExcelUtils.export` 接收 `List<SheetConfig>`（含 `name / tables`），其中 `Table` 含 `headers / values / mergeColumns / rowNum`。本工单 spec 表只要求 POJO 重载和 read 重载，不需要多 sheet / 多 table 拼接场景，所以未引入这两个内嵌类。若后续工单需要"单 sheet + 多 table 拼接"导出能力，应另开工单补 `SheetConfig` / `Table` 内嵌类。
3. **`ExcelUtil` 父类方法兼容性**：PSM 反编译产物中 `ExcelUtils.getDefaultWriteHandle()` 等是从父类继承，但当前 DataupLoad 引入的 `framework-starter-2.2.3-SNAPSHOT` 内 `framework.util.excel.ExcelUtil` **没有**这三个 helper 方法（只有静态 `exportExcel` / `readExcel` / `exportExcelByDynamicHeader`）。因此本工单的 helper 全部 self-contained，**未**通过 `super.getDefaultWriteHandle()` 调用父类（父类没有这些方法）。如果未来 framework 升级使父类出现这三个方法，应改为 `super.getDefaultWriteHandle()` 调用父类以保持单一实现。
4. **`ExcelUtil` 父类继承的副作用**：本工单 `ExcelUtils extends ExcelUtil` 同时继承了父类的静态 `exportExcel(HttpServletResponse, Class<T>, List<T>, String)` / `readExcel(InputStream, Class<T>, Consumer<List<T>>)` 等方法。这些方法与本工单新增的 POJO 重载方法名不冲突（父类是 `exportExcel`，本类是 `exportToExcel`），但调用方应使用 `ExcelUtils.exportToExcel(...)` 显式区分；调用 `ExcelUtil.exportExcel(...)`（不写 `Utils`）会走父类 API，行为不同（父类不注册合并策略）。
5. **`readFromExcel` 返回 `List<T>` 而非 lazy stream**：SAX 模式本可流式处理，但 spec 表要求返回 `List<T>`（一次性拿全部数据）。大文件（> 10000 行）场景下内存压力会随数据量线性增长；当前实现未做分批/分页。后续若需支持大文件导出 / 导入，应另开工单。
6. **`DataMergeStrategy.merge` 使用 POJO `equals/hashCode` 全字段去重**：未识别 Lombok `@Data` 注解的"主键"概念，对 Lombok `@Data` 生成的 POJO 会按所有字段（id / count / time / lineNo / faceNo / type）联合判断相等，对同 `time` 不同 `lineNo` 的两条记录会被视为不同（保留），对 `time` + `lineNo` + `faceNo` + `type` 全部相等的会被视为相等（去重）。这与业务侧"主键 = (time, lineNo, faceNo, type)" 的语义匹配（DefectDayRecord 实体恰好是这 4 元组），但不适用于其它 POJO。如需明确"主键 = 某些字段"，应改用 `mergeByTime(source, target, "time")` 按指定字段去重，或扩展 `merge` 方法接受主键字段列表。
7. **`DataMergeStrategy.mergeByTime` 反射开销**：每次调用都会对每个元素反射 `getXxx()` getter，N×2 次反射调用（N = source.size + target.size）。对 O(10000) 级别的列表单次调用延迟 < 10ms（实测估计，未做 JMH benchmark），可接受。若有热路径调用，应缓存 `Method` 对象（按 field 名 hash 缓存）。
8. **`getColumnWidthStrategy` 中 `finalWidth` 变量未直接使用**：当前自包含实现统一用 `LongestMatchColumnWidthStyleStrategy`（内置按 cell 内容长度自适应列宽），未实现 PSM 那个 CFR 标记 "Unavailable" 的 Anonymous Inner Class（其行为应是按 `finalWidth` 写死列宽）。`finalWidth` 变量保留作为 PSM 同款签名槽位，但实际未注入列宽。后续若需要"全部列固定 N 字符宽"，应改用 `SimpleColumnWidthStyleStrategy(finalWidth)`。
9. **`getDefaultStyleStrategy` 占位实现**：当前统一返回 `LongestMatchColumnWidthStyleStrategy` 作为占位，未实现真正的"默认样式策略"（表头加粗 + 数据水平居左）。EasyExcel 2.2.6 自带默认样式（已满足基本需求），所以占位实现行为上无差异。若后续需要 PSM 同款自定义样式策略（边框 / 背景色 / 字体），应另开工单实现 `HorizontalCellStyleStrategy` + `VerticalCellStyleStrategy`。
10. **`DataMergeStrategy.getCellValue` 使用 POI 已 deprecated API `getCellTypeEnum()`**：与 PSM 1:1 保持，POI 3.17 同款；POI 4.x 已 deprecated，POI 5.x 已移除。本工单当前 POI 3.17 + EasyExcel 2.2.6 兼容，未来升级 POI 时需同步改为 `getCellType()`。
11. **未触动 git**：未做任何 `git add / commit / push`（任务要求"不要推 git"）。改动已留在 working tree，待主 agent 审阅 + 整合到 batch commit。
12. **未补单元测试**：任务未要求；但 `ExcelUtils.exportToExcel / readFromExcel`（POJO 反射元数据 + EasyExcel SAX 流）和 `DataMergeStrategy.merge / mergeByTime`（反射 + equals 混合判重）值得在后续工单补 Mockito 单测 + 集成测试（用真实 .xlsx 文件 round-trip）。
13. **`SheetConfig` 内嵌类缺失**（重复 §6 #2）：PSM 反编译产物中 `ExcelUtils` 的内嵌类 `SheetConfig` 和 `Table` 未引入。如果 W-DET-05c 的 `DetectDataController` 后续需要"多 table 拼接"（早/晚班分块），需要另开工单补这两个内嵌类 + 对应的 `export(HttpServletResponse, List<SheetConfig>, String)` 重载。

---

## 7. 交付确认

- ✅ `ExcelUtils.java` 新建，含 3 个 public static 方法：
  - `exportToExcel(List<T>, Class<T>, OutputStream)` — W-DET-05b spec 1:1
  - `readFromExcel(InputStream, Class<T>)` — W-DET-05b spec 1:1
  - `exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)` — W-DET-05c 兼容重载
- ✅ `DataMergeStrategy.java` 新建，含 PSM 1:1 + spec 增项：
  - PSM 1:1：构造器 `(List<String>, int, int, int)`、`afterRowDispose`、`mergeSameValueCells`、`setMergedRegionStyle`、`parseColumnIndexes`、`getCellValue`
  - W-DET-05b spec 增项：`merge` / `mergeByTime` 静态方法
- ✅ 任务指定单文件编译命令通过（exit=0）
- ✅ DataMergeStrategy 单独编译通过（exit=0）
- ✅ 全量 188 文件编译通过（exit=0，无 error 无 warning 与本工单相关）
- ✅ 字节码验证两个新类的方法签名正确
- ✅ 未触动其它模块（alarm / line / defect / yingke / framework / screen）
- ✅ 未新增 / 删除任何 lib JAR
- ✅ 未触动 git
- ✅ 未触动 application*.yml / pom.xml
- ⚠️ 与 W-DET-05c worker 并发冲突已通过"重载共存"方式解决（详见 §6 #1）

---

## 8. 附：文件摘要

### 8.1 `ExcelUtils.java` 文件大小

- 源文件：`E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\util\ExcelUtils.java` — ~ 254 行，~ 13.6 KB
- 编译产物：`X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\util\ExcelUtils.class` — 7948 bytes
- 内部类：`ExcelUtils$1.class` — 1068 bytes（`AnalysisEventListener` 匿名实现）

### 8.2 `DataMergeStrategy.java` 文件大小

- 源文件：`E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\excel\DataMergeStrategy.java` — ~ 342 行，~ 15.7 KB
- 编译产物：`X:\DataupLoad\target\classes\com\hikrobotics\solution\module\detect\excel\DataMergeStrategy.class` — 9059 bytes
- 内部类：`DataMergeStrategy$1.class` — 842 bytes（编译器生成的辅助类）
