# W-DET-10b — Excel 导出 ≥25 行 0 字节 P0 修复

- **工单**: W-DET-10b（P0，代码修复 worker）
- **来源**: W-DET-10 压测发现（app.log 证据：`IllegalStateException A5:A6 overlap` + `Cannot call sendError()`）
- **严重度**: 🔴 P0 — 生产可用性回归，DB 真实数据 ≥25 行时 100% 失败，客户端 0 字节无错误
- **回归源**: W-DET-08 返工引入（端点从 10500 JSON 错误 → 静默 0 字节，**比 W-DET-07 时更差**）
- **前置依赖**: W-DET-08（ExcelUtils.export + DataMergeStrategy 1:1 抄 PSM）— 已完成
- **测试依赖**: W-DET-10 seed/cleanup 脚本（`scripts/seed_det.py` 副本在 `C:\perf-scripts\seed_det.py`）

---

## TL;DR

**真因**: `DataMergeStrategy.mergeSameValueCells` + `ExcelUtils.export` 在 **multi-table + 多行表头** 场景下，三层 PSM 假设崩塌：

1. **`mergeSameValueCells` 缺少 `this.start` 偏移**：`sheet.getRow(startRow)` / `sheet.getRow(i)` 应为 `sheet.getRow(startRow + this.start)` / `sheet.getRow(i + this.start)`。当前实现让 table2 读到 table1 的行 → 误合并 region → POI 检测 `A5:A6 overlap`。
2. **`relativeHeadRowIndex(id==1?0:2)` 公式假设 headerSize=1**：实际 `headerSize=2`（多行表头），table2 写到 row 2 时与 table1 的 data 行冲突。
3. **`start` 累加公式 `table.getRowNum() + 2` 同上假设 headerSize=1**。

baseline（1 sheet × 1 table）掩盖了所有问题，因为 `this.start=0` + 单 table 无 header 冲突。

**附加修复**: `ExcelUtils.export` 的 `catch (Exception ex)` 静默吞异常 — response 已 commit，客户端无错误信息。修复后即使再出 bug 也要让客户端看到 5xx JSON。

---

## 修复目标

1. **修 P0 真因**: DataMergeStrategy + ExcelUtils 让 ≥25 行 PERF 数据能正常导出 ≥1KB xlsx
2. **修错误处理**: ExcelUtils.export 异常时返回 JSON 错误，不静默吞
3. **回归验证**: W-DET-10 baseline / 1k / 10k / 100k 四个场景全部端到端成功
4. **不引入新回归**: W-DET-08 已通过的 P0/P1（数据非空 10500、表头错乱）不回归

---

## 任务清单（按依赖排序）

### T1. 读懂 W-DET-08 返工的代码差异
阅读以下文件，确认当前实现与 PSM 反编译产物的偏差：

- `DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\excel\DataMergeStrategy.java`（重点 `mergeSameValueCells` 行 142-180）
- `DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\util\ExcelUtils.java`（重点 `export` 方法行 100-150）
- `docs\work-orders\W-DET-08-report.md`（如有）— 看 W-DET-08 当时为什么返工
- `docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\detect\util\ExcelUtils.java`（PSM 原版）

**输出**: 在 W-DET-10b-report.md §0 列出当前实现 vs PSM 反编译产物的所有字段/方法差异（5-15 条表格）。

---

### T2. 修复 DataMergeStrategy.mergeSameValueCells

**目标**: 让 multi-table 场景下 table2 读到自己 sheet 区域的行。

**修复点**（`DataMergeStrategy.java:142-180`）：

```java
// 当前（错）
Row row = sheet.getRow(startRow);
...
(cRow = sheet.getRow(i)) != null

// 修复后
Row row = sheet.getRow(startRow + this.start);
...
(cRow = sheet.getRow(i + this.start)) != null
```

**注意**: `parseColumnIndexes` (line 192-210) 也用了 `sheet.getRow(this.headerSize - 1)` 不带偏移 — 但这个只在第一张 table 解析 columns（`this.indexes` 全局共享），后续 table 复用，所以**保持现状不动**。验证方法：跑 W-DET-10 baseline，columns 必须正确识别。

**修复后要求**: 4 场景（baseline / 1k / 10k / 100k）端到端返回有效 xlsx，无 `IllegalStateException A5:A6 overlap`。

---

### T3. 修复 ExcelUtils.export 多行表头偏移

**目标**: 让 multi-table + headerSize=2 场景下 sheet 行布局不冲突。

**选项（选 1）**：

- **3A（推荐，与 PSM 偏差最小）**: 把 `headerSize` 强制传 1 给 `DataMergeStrategy`（外部 caller `headerRowNum` 也用 1），EasyExcel `relativeHeadRowIndex` 公式 `id==1?0:2` 自然成立。**风险**: 多行表头的合并会少合并一行 header，但 PSM 反编译产物本身是单行表头设计，与 PSM 1:1 对齐符合 W-DET-05b spec。
- **3B**: 改 `relativeHeadRowIndex` 公式为 `id==1 ? 0 : (headerRowNum + 2)`，`start` 累加 `table.getRowNum() + headerRowNum + 2`。**风险**: 与 PSM 反编译偏差扩大，未来 PSM 升级冲突。
- **3C**: 在 `ExcelUtils.export` 里把多行表头合并成单行（header row1+row2 → header row1）。**风险**: 数据展示层有差异（多行表头可能展示分组语义）。

**选 3A**，理由：
1. 与 PSM 反编译产物 1:1 对齐（W-DET-05b ADR 已确定）
2. 改动最小（仅 caller 一行 `headerRowNum=1`）
3. 多行表头的合并样式缺失是已知小瑕疵，可在 W-DET-10c 后续工单优化

**实施位置**: ExcelUtils.java 行 121-128（`headerRowNum` 计算逻辑）— 改为 `headerRowNum = 1;` 硬编码，或在 caller `DefectRecordServiceImpl.handleStatisticDataExport` 调用 `new ExcelUtils.export(...)` 处判断。当前 DataMergeStrategy 构造器参数 `headerSize` 同步传 1。

---

### T4. 修复 ExcelUtils.export 错误处理

**目标**: 异常时客户端收到 5xx JSON 错误，不再 0 字节。

**修复**（`ExcelUtils.java:140-147`）：

```java
} catch (Exception ex) {
    log.error("export failed, error is " + ex.getMessage(), ex);
    // 新增：response 未 commit 时写 JSON 错误
    if (!response.isCommitted()) {
        try {
            response.reset();
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"export failed: " 
                + ex.getMessage().replace("\"", "\\\"") + "\"}");
        } catch (Exception ignored) {}
    } else {
        // response 已 commit（典型场景：EasyExcel 写入中异常）
        // 至少写 ERROR 日志 + 报警（如果 BaseResult 全局异常处理器未捕获）
        log.error("export failed AFTER response committed, client got truncated xlsx. ex=", ex);
    }
}
```

**注意**: 当前 `response.isCommitted()` 检查在 `catch` 块内，但实际异常发生在 `EasyExcel.writer.write()` 中间，**此时 header 已 commit**，所以走 else 分支。生产价值是日志 + 监控可发现这类问题。

**真正的客户端修复**: 让 Spring 全局异常处理器（`GlobalExceptionHandler`）接管 — 如果 ExcelUtils.export 抛 RuntimeException 出去，会被拦截返回标准 5xx JSON。但这要改 method signature。

**简化方案**: 改 `ExcelUtils.export` 让它在异常时 `throw new RuntimeException(ex)`（让外层 try/catch 处理），同时 caller `DefectRecordServiceImpl.handleStatisticDataExport` 用 try/catch 包，catch 后调用 `BaseResult.fail("export failed: " + ex.getMessage())` 返回 JSON。

**推荐**: T4 简化为 — 改 ExcelUtils.export 让异常抛出 + caller 用 try/catch + BaseResult.fail。**改动约 30 行**。

---

### T5. 端到端回归测试

**测试矩阵**（复用 W-DET-10 脚本 `C:\perf-scripts\run_det_scenario.ps1`）：

| Scenario | seed 行数 | 期望 Status | 期望 xlsx_bytes | 期望 elapsed_ms | 服务端日志 |
|---|---|---|---|---|---|
| baseline | 0 | 200 | 8000-9000 (同 W-DET-10 baseline) | <500 | 无 ERROR |
| 1k | 1000 | 200 | **>5000** ✅ | <2000 | 无 ERROR |
| 10k | 10000 | 200 | **>50000** ✅ | <10000 | 无 ERROR |
| 100k | 100000 | 200 | **>500000** ✅ | <60000 | 无 ERROR |

**额外测试**:
- **回归 #1**: W-DET-07 报告的 P0（数据非空 10500）— 已通过 W-DET-08 修复，跑 baseline 验证 xlsx 第 1 张 sheet header row 0+1 正常
- **回归 #2**: W-DET-07 报告的 P0（0 行表头错乱）— 跑 baseline（0 PERF seed），sheet 仍包含 7 day sheets

**自动化**:
- 写 `scripts/regress_det_export.ps1`：遍历 4 场景 → 收集 status / bytes / elapsed → 输出 PASS/FAIL 表格
- 不重启服务（与 W-DET-10 一致）

---

### T6. 数据清理 + 报告

- 测试结束清理 PERF seed（同 W-DET-10 流程）
- 服务进程 PID 9248 不重启
- 输出 `docs/work-orders/W-DET-10b-report.md` 包含：
  - §0 真因分析（带代码 diff）
  - §1 修改文件清单（before/after 行数）
  - §2 端到端测试结果表
  - §3 回归验证
  - §4 性能对比（修复前 0 字节 vs 修复后实际字节）
  - §5 ADR-0013 留痕（参考 W-DET-08 ADR 格式）

---

## 工时估算

| 任务 | 估计 |
|---|---|
| T1 读代码 + 对比 PSM | 15m |
| T2 修 mergeSameValueCells | 15m |
| T3 修 ExcelUtils headerSize | 15m |
| T4 修错误处理 | 20m |
| T5 端到端测试 | 30m |
| T6 报告 + ADR | 15m |
| **总计** | **~2h** |

---

## 完成定义

| 项 | 必须 |
|---|---|
| 4 场景全部 xlsx_bytes > 0 | ✅ |
| 服务端无 `IllegalStateException merged region` | ✅ |
| 服务端无 `Cannot call sendError` | ✅ |
| 客户端能下载 xlsx 并 Excel 可打开 | ✅ |
| 回归 #1 / #2 通过 | ✅ |
| 数据清理（PERF-* 全删） | ✅ |
| 服务 PID 9248 未变 | ✅ |
| ADR-0013 留痕 | ✅ |
| W-DET-10b-report.md 输出 | ✅ |
| 不 push git（PM 验收后统一 push） | ✅ |

---

## 不修项（PM 已决策，留给后续）

- 多行表头合并样式缺失（T3A 已知小瑕疵）→ W-DET-10c
- SQL 全表扫描无分页 → W-DET-09b（性能工单）
- EasyExcel 流式输出 → W-DET-09c
- 报警端 BaseResult.build() 信号失真 → W-ALM-08
- PowerShell CP936 编码 → 测试脚本层已用 `C:\perf-scripts\` 副本绕过

---

## 派工命令

```bash
codex exec -C "E:\DEMO\数据采集" --skip-git-repo-check -s workspace-write \
  "$(cat docs/work-orders/W-DET-10b-brief.md)"
```

**约束**:
- 不 push git
- 不修改 docs/work-orders/ 之外的文档
- 修改 Java 代码必须全量编译（`mvn compile` 或 javac + javac-classpath）
- 修改后必须重启服务并跑 T5 全测试矩阵
