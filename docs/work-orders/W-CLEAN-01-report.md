# W-CLEAN-01 报告 — LinePO.java 双轨制保留 ADR

**完成时间**: 2026-07-24 19:18
**执行人**: 锋卫（手动）
**工时**: 5 分钟

## 改动文件
| 文件 | 改动 |
|---|---|
| `docs/adr/0008-line-po-alias-kept.md` | 新建（澄清 ADR） |

## 关键发现
- LinePO.java **不是死代码**，仍有 9 个引用点
- 审计报告 §四 清理工单是基于"未检查引用"判断，错误
- LinePO.java 文件内 Javadoc 明确写了"保留以兼容 PSM 风格引用"

## 决策
**保留 LinePO.java** — 作为 PSM 风格引用的兼容层

## 9 个引用点
- StatusRecordDTO / DefectRecordServiceImpl / LineTreeItemDTO / Line.java
- ILineService / LineServiceImpl / StateChangeServiceImpl / ScreenServiceImpl
- 自身（LinePO.java）

## 已知限制
- 编译产物多 1 个类（约 5KB）
- 未来可开 W-CLEAN-03 批量改 9 个引用点统一用 Line
