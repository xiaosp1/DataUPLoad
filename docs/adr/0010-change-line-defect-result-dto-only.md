# ADR-0010 — ChangeLineDefectResult 仅有 DTO，无 PO/DB 表

**状态**: 已澄清（逆向验证 PSM 反编译产物）
**日期**: 2026-07-24 20:08 (W-X29 W-DFT-01a 工单)
**决策人**: 锋卫 + W-DFT-01a worker

## 背景
W-X29 P2 冲刺派工时，根据审计报告 + 记忆推测设计了 W-DFT-01a：
> 新建 `defect/entity/ChangeLineDefectResult.java`（1:1 抄 PSM `defect/model/ChangeLineDefectResultPO.java`）

**工单中列出的字段**: id / lineId / beforeResult / afterResult / changeType / changeTime / operatorId / note（8 个字段）

## 逆向验证发现
W-DFT-01a worker 实际查看 PSM 反编译产物后发现 3 处偏差：

| 偏差 | 工单描述 | **PSM 实际产物** |
|------|---------|------------------|
| 1. 类名 | `defect/model/ChangeLineDefectResultPO.java` | `defect/dto/ChangeLineDefectResult.java`（无 PO 版本） |
| 2. 字段数 | 8 字段（id/lineId/beforeResult/...） | **2 字段**（`needDelDefects` + `needAddDefect`，均为 `Collection<String>`） |
| 3. DB 表 | `change_line_defect_result` 应存在 | **0 匹配**（所有 V0.x/V1.x 迁移脚本中均不存在） |

## 决策
**采用 worker 逆向验证结果**：新建 `ChangeLineDefectResult` 仅含 2 字段、放在 `defect/entity` 包、不加 `@TableName` / `@TableId` / `@JsonFormat` 持久化注解。

## 理由
1. **1:1 对齐 PSM** 是项目核心策略，不能虚构字段
2. **DTO vs PO 区别**：DTO 是返回前端的运行时结果集，PO 是数据库映射
3. **DB 表不存在**意味着工单描述的 `id/lineId/beforeResult/afterResult` 等字段在 PSM 中从未被持久化
4. **职责清晰**：类只承载"切换产线时的缺陷增删集合"，不承载变更审计

## 影响
- W-DFT-01b 的 LineDefectType CRUD 不依赖此 entity（独立）
- 未来如需持久化变更审计，需要：
  - 建 `change_line_defect_result` 表（带工单列出的 8 字段）
  - 新建对应 PO + Service + Controller
  - 与 DTO 分离（DPL 已有 DTO；新建 PO 即可）

## 历史工单
- W-X29 W-DFT-01a: 逆向验证 + 1:1 落地（本文件留痕）

## 教训
- **派工前应先确认产物存在性**：下次涉及新 entity 时，worker prompt 应先要求"grep PSM 反编译产物确认类存在 + 字段匹配"
- **工单描述与 PSM 实际不符时以 PSM 为准**：worker 必须有逆向验证能力，不允许为了"完成工单"虚构字段
- **W-AUDIT-01 审计深度不均**：defect 模块当时只给了 30% 对齐度估算，没有列具体偏差表
