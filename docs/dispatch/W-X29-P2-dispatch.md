# W-X29 — P2 工单派工记录 (2026-07-24 20:02)

## 派工目标
W-X28 P1 冲刺完成（8/8）后，启动 P2 工单冲刺，把 Excel 导出、defect CRUD、plan/manage 实装等增强功能补齐。
工单来源: `docs/audit/2026-07-24-AUDIT-REPORT.md` §三 P2 表 + W-X27/28 报告已知遗留

## 工单清单（7 项）

| 工单 | 内容 | 模块 | 工时 | 派工 |
|------|------|------|------|------|
| W-DET-05a | TimeRange 游标 + TimePattern 枚举 | detect | 1.5h | codex |
| W-DET-05b | ExcelUtils + DataMergeStrategy | detect | 2h | codex |
| W-DET-05c | DetectDataController statistic/export + detect/list 实装 | detect | 1.5h | codex |
| W-DFT-01a | ChangeLineDefectResult entity 迁移 | defect | 1h | codex |
| W-DFT-01b | LineDefectTypeServiceImpl 完整 CRUD + 新建 Controller | defect | 2h | codex |
| W-LIN-06 | plan/manage stub 实装 | line | 2h | 串行下一批 |
| W-CLEAN-03 | 删除 LinePO 9 引用 | line | 2h | 串行下一批 |
| W-DET-06 | JDBC allowMultiQueries 配置 | detect | 0.5h | 手动 |

## 派工时间表
- 20:02 启动 5 个并行 codex worker (DET-05a/b/c + DFT-01a/b)
- 20:02 同步手撸 W-DET-06 (allowMultiQueries 配置)
- 20:02-21:00 等 codex 完成
- 21:00-21:30 派第二批 W-LIN-06 / W-CLEAN-03
- 21:30-22:00 全量编译 + 重启 + 冒烟 + push

## 派工记录

### W-DET-05a — 20:02 sessions_spawn ✅
- 完成: 20:20 (18m15s)
- 改动: 新建 detect/util/TimeRange.java (96 行)
- 编译: 0 errors
- 报告: `docs/work-orders/W-DET-05a-report.md`
- 重大发现: W-DET-05b DataMergeStrategy 预存问题 (POI Cell.getCellType 返回 int 而非 enum) → 待 W-DET-05b worker 修复

### W-DET-05b — 20:02 sessions_spawn ✅
- 完成: 20:35 (33m)
- 改动: 新建 detect/util/ExcelUtils.java (254 行) + detect/excel/DataMergeStrategy.java (342 行)
- 依赖: easyexcel-2.2.6.jar + poi-3.17.jar (已有)
- 编译: 0 errors
- 报告: `docs/work-orders/W-DET-05b-report.md` (22.7KB)
- ⚠️ Race condition: 与 W-DET-05c worker 同时写同一文件，worker 主动提供 HttpServletResponse overload 兼容

### W-DET-05c — 20:02 sessions_spawn ✅
- 完成: 20:35 (33m)
- 改动: DetectDataController 173→273 行 + 实装 exportStatisticData + 新增 detectList
- 2 端点: /web/detect/statistic/export (Excel导出) + /web/detect/list (分页查询)
- 编译: 188 文件 0 errors
- 报告: `docs/work-orders/W-DET-05c-report.md` (23KB)
- ⚠️ 与 W-DET-05b race condition: 两个 worker 同时写 ExcelUtils / DataMergeStrategy。最终 W-DET-05b 提供 PSM 1:1 实现，W-DET-05c 保留 controller 调用不变。

### W-DFT-01a — 20:02 sessions_spawn
- 待补充

### W-DFT-01b — 20:02 sessions_spawn ✅
- 完成: 20:15 (13m)
- 改动: ILineDefectTypeService +97 / LineDefectTypeServiceImpl +74 / LineDefectTypeMapper +38 / 新建 LineDefectTypeController 222 行
- 5 endpoint: POST/PUT/DELETE/GET list/by-line
- 编译: 0 errors
- 报告: `docs/work-orders/W-DFT-01b-report.md`
- ⚠️ 偏差: brief 说 listByLineId 但 entity 只有 lineNo 字段 → 改为 listByLineNo 贴 PSM
- ⚠️ 偏差: PSM defect/web 包不存在 → Controller 借 alarm/web/DefectTypeController 样式新建

## 手动任务

### W-DET-06 — 20:02 手动启动 ✅
- 完成: 20:03 (1m)
- 改动: application-prod.yml 加 ?allowMultiQueries=true
- 报告: `docs/work-orders/W-DET-06-report.md`

### W-DFT-01a — 20:02 sessions_spawn ✅
- 完成: 20:08 (6m01s)
- 改动: 新建 defect/entity/ChangeLineDefectResult.java (2 字段)
- ⚠️ 重大逆向验证: PSM 没有 ChangeLineDefectResultPO / 无 DB 表
- worker 诚实未虚构，按 PSM DTO 实际产物落地 (2 个 Collection 字段)
- 决策: 以 PSM 反编译产物为准，不以工单表格为准
- 报告: `docs/work-orders/W-DFT-01a-report.md`
- ADR: ADR-0010 已生成

## 第一批 codex 完成 (5/5)

| 工单 | 完成时间 | 工时 |
|---|---|---|
| W-DET-05a | 20:20 | 18m |
| W-DET-05b | 20:35 | 33m (含 race condition 修复) |
| W-DET-05c | 20:35 | 33m (与 05b race condition) |
| W-DFT-01a | 20:08 | 6m |
| W-DFT-01b | 20:15 | 13m |

提交: c882101 ✅ push 成功

## 第二批 codex 启动 (20:38)

### W-LIN-06 — 20:38 sessions_spawn ✅
- 完成: 20:43 (5m13s)
- 改动: ILineService 152→187 + LineServiceImpl 662→730 + LineController 312→313
- 新增: planOrderDtos(String, String, Integer, Integer) 服务方法
- 改造: /web/line/plan/manage 从 stub 90003 改实装
- 逆验发现: PSM 实际无 planOrderDtos/manageList/planManagePage → 用 PSM 最近语义 PlanServiceImpl.clientPlan → planMapper.selectClientPlan
- 编译: 0 errors
- 报告: `docs/work-orders/W-LIN-06-report.md`
- 提交: c4924c7 ✅ push 成功

## 第三批 codex 启动 (20:46)

### W-CLEAN-03 — 20:46 sessions_spawn ✅
- 完成: 20:43 (5m36s)
- 改动: 8 文件 + 删 LinePO.java + 删空 line/model/ 目录
- 关键简化: handleLineTreeSearch 不再需 BeanUtil.copyProperties 中转
- 编译: 187 文件 0 errors
- 重启: PID 23688 (worker 自己重启服务)
- 冒烟: 5/5 端点 200
- 报告: `docs/work-orders/W-CLEAN-03-report.md` (7564 bytes)
- ⚠️ CFR 遗留: LineTreeItemDTO 仍含 assertj import (lib 已有该 jar，不阻塞)
- ADR-0008 已反转 (main agent 负责)
