# ADR-0008 — LinePO.java 双轨制保留

**状态**: 已落地（代码已存在，ADR 留痕）
**日期**: 2026-07-24 19:18 (W-X28 W-CLEAN-01 工单澄清)
**决策人**: 锋卫

## 背景
W-AUDIT-01 审计报告 §四 清理工单提到 W-CLEAN-01: 删除 LinePO.java 死代码。
但实际扫描发现 LinePO.java 仍被多处引用：

- `module/detect/dto/StatusRecordDTO.java`
- `module/detect/service/impl/DefectRecordServiceImpl.java`
- `module/line/dto/LineTreeItemDTO.java`
- `module/line/entity/Line.java`（同包不同类型，可能注释引用）
- `module/line/model/LinePO.java`（自身）
- `module/line/service/ILineService.java`
- `module/line/service/impl/LineServiceImpl.java`
- `module/line/service/impl/StateChangeServiceImpl.java`
- `module/screen/service/impl/ScreenServiceImpl.java`

## 决策
**保留 LinePO.java**，作为 PSM 风格引用的兼容层。

## 理由
1. **LinePO.java 文件内 Javadoc 明确写了**："本工单优先使用 Line（entity 包）；此 PO 类保留以兼容现有 PSM 风格引用"
2. **9 个引用点**：包括 LineServiceImpl / ScreenServiceImpl 等核心代码，删除需要批量改代码 + 回归测试
3. **价值低成本**：PO 类 ~234 行，仅占编译产物极小空间
4. **风险高**：删除 LinePO 触发的回归测试覆盖率不足

## 当前架构
- **`module/line/entity/Line.java`**: 新代码主用（DataupLoad 项目约定 entity 包）
- **`module/line/model/LinePO.java`**: PSM 风格引用兼容层（旧代码 / 跨模块引用）
- 两个类的字段集一致（都映射到 `public.line` 表）

## 影响
- 编译产物多 1 个类（约 5KB）
- 不影响性能、不影响主链路
- 未来如有需求统一，可开 W-CLEAN-03 工单批量改 9 个引用点

## 历史工单
- W-B05: 引入 LinePO
- W-X28 W-CLEAN-01: 审计建议删除 → 经澄清保留（本文件留痕）

## 备注
本 ADR 把"清理工单"转为"留痕工单"，避免后续审计再次提出。
