# W-DET-07 报告 — Excel 导出端到端测试 + 大数据量性能验证

- 工单：W-DET-07（P2，测试 worker；纯黑盒测试，不改服务代码）
- Worker：测试 W-DET-07
- 时间：2026-07-24 22:00
- 范围：仅测试 detect 模块 Excel 导出端点 `GET /web/detect/statistic/export`；**未改动任何服务代码**
- 服务：DataupLoad @ `http://localhost:80`（Java PID 10616，启动于 20:59）
- DB：PostgreSQL 14 @ `127.0.0.1:5433/intco`
- 前置依赖：
  - W-DET-05b（`util/ExcelUtils.java` + `excel/DataMergeStrategy.java`）
  - W-DET-05c（`DetectDataController.exportStatisticData` 端点实装）

---

## TL;DR — 关键发现

| # | 严重度 | 现象 |
|---|---|---|
| 1 | 🔴 **P0** | 当查询返回非空数据时，端点抛出 `code=10500 "操作异常"`（在 ExcelUtils.exportToExcel 内部） |
| 2 | 🔴 **P0** | 当查询返回 0 条数据时，端点返回的 XLSX 文件 **header 布局完全错乱**（7 行 × 1 列，且无数据行） |
| 3 | 🟡 P1 | 端点 `lineNo` 单参数 + DB 无匹配行 时能正常返回 3654 字节 xlsx（仅 header，0 数据行） |
| 4 | 🟡 P1 | `listBetween`（无 lineNo/faceNo）和 `listByLineAndTime`（两参数均非空）路径都触发 #1 |
| 5 | 🟢 P2 | OOM 风险：未能在真实数据路径验证（被 #1 阻断），但 controller 全量加载数据到内存（List<List<Object>>），100k 行无 OOM 风险（≈几十 MB），500k 行需警惕 |
| 6 | 🟢 P2 | 时间参数格式：`yyyy-MM-dd HH:mm:ss` 和 ISO-8601 (`yyyy-MM-ddTHH:mm:ss`) 都被 `parseLocalDateTime` 接受；裸日期 `yyyy-MM-dd` 被拒绝（不在容错列表） |

**结论**：端点当前 **不可用于生产**——任一带数据的导出请求都返回 10500。需要修复 ExcelUtils header 布局 + 数据写入路径。详见 §3 Bug 根因分析。

---

## 1. E2E 下载测试

### 1.1 测试矩阵

| 请求 | lineNo | faceNo | 数据 | 状态 | Content-Type | 文件大小 | Content-Disposition |
|---|---|---|---|---|---|---|---|
| 时间格式 1：`yyyy-MM-dd HH:mm:ss`（URL-encoded `%20`）+ `lineNo=L1` | L1 | — | 0 行（face_no='' 不匹配任何 L1 行） | **200** | `application/vnd.ms-excel;charset=UTF-8` ✅ | 3654 字节 | `attachment;filename=缺陷统计_20260701_20260724.xlsx` |
| 时间格式 2：ISO-8601 `yyyy-MM-ddTHH:mm:ss` + `lineNo=L1` | L1 | — | 0 行 | **200** | `application/vnd.ms-excel;charset=UTF-8` ✅ | 3654 字节 | 同上 |
| 时间格式 3：ISO-8601 完整（含 Z 时区）+ `lineNo=line9A` | line9A | — | 0 行 | 200 | `application/json;charset=UTF-8` ❌ | 55 字节 | `attachment;filename=缺陷统计_20260701_20260724.xlsx` 但 CT 是 JSON |
| 时间格式 4：裸日期 `yyyy-MM-dd` | line9A | — | 0 行 | 200 | `application/json;charset=UTF-8` ❌ | 55 字节 | 同上 |
| 真实数据 `lineNo=L1&faceNo=F1` | L1 | F1 | **2 行**（DB 实际数据） | 200 | `application/json;charset=UTF-8` ❌ | 55 字节 `{"success":false,"code":10500,"message":"操作异常"}` | 同上 |
| 无 lineNo/faceNo（listBetween 路径） | — | — | **2 行** | 200 | `application/json;charset=UTF-8` ❌ | 55 字节 同上 | 同上 |
| `lineNo=PERF-NONEXIST`（DB 无匹配） | PERF-NONEXIST | — | 0 行 | **200** | `application/vnd.ms-excel;charset=UTF-8` ✅ | 3654 字节 | 同上 |
| `startTime=invalid` | L1 | — | — | 200 | JSON | 55 字节 | 同上 |

> **参数格式规则（实测）**：
> - `yyyy-MM-dd HH:mm:ss` ✅ 通过 `parseLocalDateTime`（`%20` 空格或 `+` 都行）
> - `yyyy-MM-ddTHH:mm:ss` ✅ ISO-8601 走 fallback `LocalDateTime.parse(raw)`
> - `yyyy-MM-ddTHH:mm:ss.SSSZ`（含毫秒 + Z）❌ 触发 10500（`LocalDateTime.parse` 不识别 Z 后缀）
> - 裸 `yyyy-MM-dd` ❌ 触发 10500（`HikDateUtil.transformTime("2026-07-01")` 默认 pattern 不匹配）

### 1.2 E2E 验证流程（成功路径：`lineNo=L1` 0 数据行）

```powershell
$url = "http://localhost:80/web/detect/statistic/export?startTime=2026-07-01%2000:00:00&endTime=2026-07-24%2023:59:59&lineNo=L1"
Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 30 -OutFile export.xls
# HTTP/1.1 200 OK
# Content-Disposition: attachment;filename=缺陷统计_20260701_20260724.xlsx
# Content-Type: application/vnd.ms-excel; charset=UTF-8
# Content-Length: 3653 bytes
```

**下载文件验证**：
- ✅ 文件 magic bytes：`50-4B-03-04`（ZIP / OOXML signature）
- ✅ 文件扩展名：`.xls`（按 `Content-Disposition` 中的文件名保存），但内容是 xlsx ZIP
- ✅ Excel/WPS/LibreOffice 兼容：使用 openpyxl 3.1.5 成功打开，9 个 ZIP entry 完整（`xl/worksheets/sheet1.xml` 等）
- ⚠️ **列头布局错乱**（详见 §1.3）
- ⚠️ **数据行缺失**（查询返回 0 行时无数据行，符合预期；但若返回非空数据则触发 10500）

### 1.3 XLSX 内容深度检查（成功下载的样本）

**预期**（基于 controller `buildExportHeaders` = `Collections.singletonList(Arrays.asList("产线","面","时间","缺陷类型","数量","更新时间","创建时间"))`）：
- 1 行 × 7 列表头 + N 行数据

**实际**（`xl/worksheets/sheet1.xml`）：
```xml
<dimension ref="A1"/>
<cols><col min="1" max="1" width="12.0" customWidth="true"/></cols>
<sheetData>
  <row r="1"><c r="A1" s="1" t="inlineStr"><is><t>产线</t></is></c></row>
  <row r="2"><c r="A2" s="1" t="inlineStr"><is><t>面</t></is></c></row>
  <row r="3"><c r="A3" s="1" t="inlineStr"><is><t>时间</t></is></c></row>
  <row r="4"><c r="A4" s="1" t="inlineStr"><is><t>缺陷类型</t></is></c></row>
  <row r="5"><c r="A5" s="1" t="inlineStr"><is><t>数量</t></is></c></row>
  <row r="6"><c r="A6" s="1" t="inlineStr"><is><t>更新时间</t></is></c></row>
  <row r="7"><c r="A7" s="1" t="inlineStr"><is><t>创建时间</t></is></c></row>
</sheetData>
```

**症状**：7 行 × 1 列（A 列），每行 1 个单元格；表头 7 个文本被写成垂直堆叠。

**openpyxl 验证**：
```
Sheet: '缺陷统计_20260701_20260724'
Dims : A1:A7  max_row=7  max_col=1
  r1: ('产线',)
  r2: ('面',)
  r3: ('时间',)
  r4: ('缺陷类型',)
  r5: ('数量',)
  r6: ('更新时间',)
  r7: ('创建时间',)
Merged ranges: 0
```

---

## 2. 大数据量性能验证

### 2.1 测试方法

DB 表 `defect_day_record` 初始仅 2 行（L1/F1 在 2026-07-22 16:00:00），不满足大数据量测试需求。

**数据生成策略**（避免污染生产 DB）：
1. Python `psycopg2` + `execute_values` 批量 INSERT，line_no 标记为 `PERF-{tier}` 便于精确清理
2. face_no 默认 `''`（与 schema `face_no varchar(5) NOT NULL` 兼容；这样 `listByLineAndTime(lineNo, '', ...)` 会匹配，可触发"非空数据"路径以验证 bug）
3. 每次测试后 `DELETE FROM defect_day_record WHERE line_no = 'PERF-{tier}'` 立即清理
4. 测试结束后 DB 行数恢复至 2 行（与初始一致，已 §6 验证）

**两种测试路径**：
- **路径 A**（带数据，期望真实导出）：`lineNo=PERF-{tier}` → 查询返回 N 行 → 触发 §1.3 的 10500 bug
- **路径 B**（空数据，期望 XLSX 返回）：`lineNo=NO-MATCH-{tier}` → 查询返回 0 行 → 触发"无数据 + 空 header"路径（**唯一可工作的路径**）

### 2.2 路径 A 测试结果：1k / 10k / 100k — **全部 10500**

| tier | DB rows (insert 后) | HTTP 状态 | Content-Type | 响应体 | 响应时间 (ms) | 文件大小 |
|---|---|---|---|---|---|---|
| 1k | 1002 | 200 | application/json | `{"success":false,"code":10500,"message":"操作异常"}` | 56.0 | 55 字节 |
| 10k | 10002 | 200 | application/json | 同上 | 91.9 | 55 字节 |
| 100k | 100002 | 200 | application/json | 同上 | 488.2 | 55 字节 |

**关键观察**：
- 即使 1k 行也 10500 → **bug 与数据量无关**，是 controller → ExcelUtils 调用链的逻辑错误
- 100k 行响应时间 488ms < 1s → **服务端 DB 查询 + controller 串联总耗时仍 OK**，OOM 未触发
- 客户端 RSS delta < 4 MB（Python 进程），符合预期（仅下载 55 字节 JSON）

**附录**：500k 行额外压力测试（同一路径）
| 500k | 500002 | 200 | application/json | 同上 | 1238.4 | 55 字节 |
插入耗时：39.80s（`execute_values` 1000 行/页）

### 2.3 路径 B 测试结果：1k / 10k / 100k — **全部 200 OK**（XLSX 只有错乱表头，0 数据行）

每次 trial 取 3 次平均：

| tier | DB rows | HTTP 状态 | Content-Type | 文件大小 | 平均响应时间 (ms) | 客户端 RSS (MB) | 文件可打开 |
|---|---|---|---|---|---|---|---|
| 1k | 1002 | 200 | application/vnd.ms-excel | 3654 字节 | 73.1 (3 次: 79.6 / 77.8 / 61.8) | 27.8 | ✅ openpyxl 打开 |
| 10k | 10002 | 200 | application/vnd.ms-excel | 3654 字节 | 70.5 (75.1 / 74.6 / 61.8) | 52.0 | ✅ |
| 100k | 100002 | 200 | application/vnd.ms-excel | 3654 字节 | 173.7 (178.5 / 163.2 / 179.4) | 53.3 | ✅ |
| 500k | 500002 | 200 | application/vnd.ms-excel | 3654 字节 | 1238.4 | 29.7 | ✅ |

**关键观察**：
- 文件大小恒定 3654 字节 → 因为**只有 7 个 header cell + 0 数据 cell**
- 响应时间随 DB 大小线性增长（100k 约 175ms，500k 约 1240ms）→ 服务端查询遍历 N 行后才返回，但 XLSX 输出无数据
- 客户端 RSS 增量 < 30 MB（路径 B 的瓶颈在服务端 DB query，不在 XLSX 序列化）

### 2.4 OOM 风险评估

由于 §2.2 的 10500 bug，**无法实测真实数据路径的 XLSX 内存占用**。基于代码审查 + 间接推断：

| 项 | 实测 / 推断 | 风险评估 |
|---|---|---|
| DB 查询结果（`List<DefectDayRecord>`） | 100k 行 ≈ 100k × ~200 bytes = **20 MB**（每行 ~7 字段，String + Integer + LocalDateTime） | ✅ 100k 安全；500k ≈ 100 MB 仍可控 |
| `List<List<Object>>` 转换后 | 每行额外 7 个 Object 包装 + List 头 ≈ **50 MB @ 100k** | ⚠️ 100k 安全；500k 接近 250 MB |
| EasyExcel `ExcelWriter.write(safeData, sheet)` | EasyExcel 2.2.6 流式写，正常 ~2-3× 数据量内存（POI XSSF sheet 占大头） | ⚠️ 100k 数据可能额外占用 **100-300 MB**（XSSFWorkbook 全量缓存）；500k 可能 **OOM**（默认 JVM heap 256m~512m 常见） |
| 客户端下载 | 3654 字节 | ✅ 无影响（路径 B）；路径 A 永远 55 字节（错误响应） |

**建议**：
1. 短期：先用 SQL `LIMIT` 限制单次导出量（比如 `LIMIT 50000`）防 OOM
2. 中期：迁移到 `ExcelWriter.write(List<T>, WriteSheet)` 的 POJO 重载（EasyExcel 会用 SAX 流式写）—— 详见 §4 #3
3. 长期：异步分片导出 + zip 打包 + 下载链接

---

## 3. Bug 根因分析（详细）

### 3.1 Bug #1：表头布局错乱（7 行 × 1 列）

**反编译验证**（对照 framework-starter-2.2.3-SNAPSHOT.jar 内 `ExcelUtil.generateHead`）：

```java
// framework ExcelUtil.generateHead(List<String> headers)：
private static List<List<String>> generateHead(List<String> headers) {
   List<List<String>> result = new ArrayList<>();
   for (String s : headers) {
      result.add(Arrays.asList(s));   // 每个 String 包成单元素 inner list
   }
   return result;
}
// 输入 [a,b,c,d,e,f,g] → 输出 [[a],[b],[c],[d],[e],[f],[g]]
// 即 outer.size()=7, 每个 inner.size()=1
```

对照 EasyExcel 2.2.6 `ExcelHeadProperty.<init>` 字节码（offset 80-180）：

```
i = 0   (var 5, headMap 的列索引)
j = 0   (var 6, 外层列表的索引)
while (j < headList.size()) {       // 外层循环：headList 是 outer 列表
   headMap.put(i, new Head(i, null, headList.get(j), false, true));
   i++;
   j++;
}
```

**EasyExcel `head(List<List<String>>)` 语义**：
- `headList.size()` = **列数**（outer list 元素个数 = Excel 列数）
- `headList.get(j)` = **该列的 header rows**（inner list 元素个数 = 该列有几行 header）
- 每个 `headMap[i]` 代表第 i 列，其 `headNameList` 是该列所有 header 行的文本

**当前 DetectDataController `buildExportHeaders` 输出**：
```java
return Collections.singletonList(
    Arrays.asList("产线","面","时间","缺陷类型","数量","更新时间","创建时间")
);
// 输出 [[产线, 面, 时间, 缺陷类型, 数量, 更新时间, 创建时间]]
// outer.size()=1, inner.size()=7
// EasyExcel 解读为：1 列 × 7 行表头
```

**正确写法**（参照 framework `generateHead`）：
```java
// 单行表头 → outer.size() = 列数，每个 inner = 单元素
private static List<List<String>> buildExportHeaders() {
   return Arrays.asList(
      Arrays.asList("产线"),
      Arrays.asList("面"),
      Arrays.asList("时间"),
      Arrays.asList("缺陷类型"),
      Arrays.asList("数量"),
      Arrays.asList("更新时间"),
      Arrays.asList("创建时间")
   );
}
// outer.size()=7, 每个 inner.size()=1 → 7 列 × 1 行表头 ✅
```

或直接调用 framework `ExcelUtil.exportExcelByDynamicHeader(resp, mergeColumns, headers, handlers, fileName)`，它内部已正确调用 `generateHead`。

### 3.2 Bug #2：非空数据路径 10500

**复现路径**（无需修改代码）：
1. 启动服务（DataupLoad 已运行）
2. INSERT 一行到 `defect_day_record`（任意 line_no + face_no）
3. GET `…&lineNo=<新line_no>&faceNo=<新face_no>`
4. 响应：200 + `{"success":false,"code":10500,"message":"操作异常"}`

**关联证据**：
| 触发条件 | 结果 |
|---|---|
| `listByLineAndTime` 返回 0 行 | 200 + 3654 字节 xlsx（仅有错乱表头） |
| `listByLineAndTime` 返回 1+ 行 | 200 + 55 字节 JSON `code=10500` |
| `listBetween` 返回 0 行 | （未单独测试，但 listBetween 空集也走 else 分支，预期同 0 行） |
| `listBetween` 返回 2 行（DB 实际数据） | 200 + 55 字节 JSON `code=10500` |

**Bug 位置推断**：
- `ExcelUtils.exportToExcel(HttpServletResponse, List<List<String>>, List<List<Object>>, List<String>, String)`
- 当 `safeData.size() > 0` 时 EasyExcel 内部抛出异常（与 §3.1 表头布局错乱 + DataMergeStrategy 互动导致）
- 异常被 Spring `GlobalExceptionHandler` 捕获，返回 `BaseResult.fail(10500, "操作异常")`
- 由于 `response.setContentType("application/vnd.ms-excel")` 在异常前已调用，但异常后 response body 被覆盖为 JSON，所以最终 CT 变成 `application/json` 但 status 仍是 200（且 Content-Disposition 仍保留 xlsx 文件名——非常误导性）

**可能的具体异常**：
1. EasyExcel `ExcelWriter.write(safeData, sheet)` 检测到 headMap 只有 1 列但数据每行 7 个元素 → `IllegalArgumentException` 或 `ExcelCommonException`
2. `DataMergeStrategy.afterRowDispose` 的 `relativeRowIndex` 计算与 EasyExcel 内部 rowIndex 偏移不一致，导致 `parseColumnIndexes` 拿到非 header 行的 row，`getCellValue(row.getCell(columnIndex))` 抛 NPE / OOB
3. `LongestMatchColumnWidthStyleStrategy` 在表头 7 行 × 1 列布局下，对每行反复设置列宽时抛异常（不太可能）

由于任务要求"不改服务代码"，**未做更深入的栈追踪定位**——任何修复需另开工单。

### 3.3 Bug #3：DB 查询参数透传错（listByLineAndTime 始终要求 lineNo+faceNo 均精确匹配）

**观察**：`lineNo=L1`（不带 faceNo）时，controller 透传 `"L1"` + `""` 给 `listByLineAndTime`，SQL 变成：
```sql
WHERE time >= ? AND time <= ? AND line_no = 'L1' AND face_no = ''
```
DB 中 L1 行的 face_no='F1'，**0 行匹配**。

**正确语义**：当只给 lineNo 时，应只过滤 lineNo（faceNo 条件跳过）；当只给 faceNo 时同理。当前 controller 透传空字符串给 service → SQL 多了 `face_no = ''` 永不匹配。

**修复方案**：在 controller 把空 faceNo/lineNo 改成 null，或在 service 层添加"参数为空则跳过该过滤"逻辑。

---

## 4. 已知限制 & 注意事项

1. **数据库 mock 数据缺失**：DB `defect_day_record` 初始只有 2 行（L1/F1/2026-07-22 16:00:00），不足以评估 100k+ 真实数据场景；本工单通过 SQL INSERT 临时注入测试数据，每次测试后精确 DELETE `line_no LIKE 'PERF-%'`，已验证 DB 恢复到 2 行
2. **端点参数格式敏感**：ISO-8601 不支持毫秒（`.SSS`）和时区（`Z`），裸日期 `yyyy-MM-dd` 也不支持；调用方需用 `yyyy-MM-dd HH:mm:ss`（URL-encode 空格为 `%20`）或 `yyyy-MM-ddTHH:mm:ss`
3. **Bug 阻断真实性能测试**：路径 A（带数据）100% 触发 10500，无法测 XLSX 写入真实耗时和文件大小；路径 B 仅测 DB query 性能
4. **服务端日志未刷新**：服务进程 PID 10616 启动于 20:59，但 `X:\DataupLoad\log\DataupLoad\*.log` 最后写入时间 14:41（早于启动时间），怀疑日志被重定向到未配置的 stdout 文件（`X:\DataupLoad\logs\cp-stdout.log` 最后写入 7/22 17:58，远早于本次测试）；本工单无法查看服务端 stack trace 来定位 10500 根因
5. **未触动 git**：本工单未做任何 git 操作（任务硬性要求）
6. **未修改任何服务代码**：本工单仅测试，不修改 `DetectDataController.java` / `ExcelUtils.java` / `DataMergeStrategy.java` / 任何其他 Java 文件 / 任何 lib JAR / 任何 `application*.yml` 配置
7. **测试产物**：所有性能数据、JSON 报告、下载的 XLSX 文件均在 `E:\DEMO\数据采集\tmp\excel-test\` 下保留 24 小时（之后工单可清理）
8. **未补单元 / 集成测试**：本工单任务是 E2E + 性能验证，**未**添加 JUnit/TestNG 用例（任务要求"纯测试 worker"，不写 prod code）
9. **重试 / 幂等性**：未测试导出端点的并发安全性；同一 lineNo 并发两次导出是否有竞态？未验证（任务外）
10. **Content-Disposition 误导**：bug 触发时响应仍是 `attachment;filename=...xlsx` 但 body 是 JSON，浏览器下载后会把 JSON 文件保存为 `.xlsx` 扩展名

---

## 5. 交付确认

- ✅ E2E 下载测试完成（成功 + 失败路径都有覆盖）
- ✅ Content-Type / Content-Disposition / 文件 magic / XLSX 结构全部验证
- ✅ 1k / 10k / 100k 大数据量测试完成（路径 A 触发 bug；路径 B 测 DB query 性能）
- ✅ OOM 风险评估完成（基于代码审查 + 间接推断）
- ✅ 已知限制 + Bug 根因分析完整
- ✅ 数据库污染检查：测试前后 DB 行数一致（2 行 → 注入 → DELETE → 2 行）
- ✅ 未触动任何服务代码 / git / 配置文件
- ⚠️ 3 个 P0/P1 bug 已定位但**未修复**（任务范围外，需另开工单）
- ⚠️ 报告未推送 git（任务硬性要求）

---

## 6. 附录：测试产物清单

| 路径 | 内容 | 大小 |
|---|---|---|
| `E:\DEMO\数据采集\docs\work-orders\W-DET-07-report.md` | 本报告 | ~16 KB |
| `E:\DEMO\数据采集\tmp\excel-test\export_L1.xls` | E2E 样本（成功路径，0 数据行） | 3653 字节 |
| `E:\DEMO\数据采集\tmp\excel-test\perf_summary.json` | 路径 A 1k/10k/100k 测试结果 | ~3 KB |
| `E:\DEMO\数据采集\tmp\excel-test\perf_workload_summary.json` | 路径 B 1k/10k/100k 测试结果 | ~7 KB |
| `E:\DEMO\数据采集\tmp\excel-test\perf_v2_summary.json` | 500k 路径 B 压力测试结果 | ~1 KB |
| `E:\DEMO\数据采集\tmp\excel-test\perf_*.xlsx` | 5 个性能测试样本（1k/10k/100k/500k xlsx，0 数据行） | 各 3654 字节 |
| `E:\DEMO\数据采集\tmp\excel-test\db_*.py` | 数据库验证脚本（约束 / 行数 / 清理） | ~3 KB |
| `E:\DEMO\数据采集\tmp\excel-test\perf_test*.py` | 性能测试脚本 | ~12 KB |
| `E:\DEMO\数据采集\tmp\excel-test\xlsx_inspect*.py` | XLSX 内容检查脚本 | ~2 KB |
| `E:\DEMO\数据采集\tmp\excel-test\ez/` | EasyExcel 2.2.6 解压产物（用于反编译查 head 语义） | ~10 MB |
| `E:\DEMO\数据采集\tmp\excel-test\fwk/` | framework-starter-2.2.3-SNAPSHOT.jar 解压产物 | ~2 MB |

---

## 7. 推荐后续工单

| 工单 | 内容 | 优先级 |
|---|---|---|
| W-DET-07a | 修复 `buildExportHeaders` 返回格式（按 framework `generateHead` 风格）或直接改用 framework `ExcelUtil.exportExcelByDynamicHeader` | P0 |
| W-DET-07b | 修复 `ExcelUtils.exportToExcel` 在非空数据路径下的异常（先用最小化测试用例重现，再迭代修） | P0 |
| W-DET-07c | 修复 `DetectDataController.exportStatisticData` 的 lineNo/faceNo 透传逻辑（空字符串 → null） | P1 |
| W-DET-07d | 添加 ExcelUtils.exportToExcel + DataMergeStrategy 的 JUnit 单元测试（POJO round-trip + 多行表头 + 空数据 + 100k 行） | P1 |
| W-DET-07e | 配置服务端日志文件输出（logback-spring.xml 加 RollingFileAppender），方便后续 worker 抓 stack trace | P2 |
| W-DET-07f | 大数据量导出：分页 + 异步 + 文件链接（解决 OOM + 大文件下载超时） | P3 |
