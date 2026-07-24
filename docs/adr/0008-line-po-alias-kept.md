# ADR-0008 — LinePO.java 双轨制

**状态**: ~~已落地（保留）~~ → **已反转（W-X29 W-CLEAN-03 删除）** ✅
**日期**: 2026-07-24 19:18 v1 → 2026-07-24 20:43 v2
**决策人**: 锋卫（W-CLEAN-01 留） + W-CLEAN-03 worker（删除）

## 📝 决定反转 (2026-07-24 20:43 - W-X29 W-CLEAN-03)

W-X29 P2 冲刺追加 W-CLEAN-03 工单。worker 实测确认：

- 9 个 LinePO 引用点全部可以迁移到 `Line` entity（字段 1:1）
- `LineTreeItemDTO` 构造器从 `LineTreeItemDTO(LinePO po)` 改为 `LineTreeItemDTO(Line po)`（简化）
- `LineServiceImpl.handleLineTreeSearch` 不再需要 `BeanUtil.copyProperties(line, LinePO.class)` 中转
- **LinePO.java 已删除** + 空目录 `line/model/` 已清理
- 编译 187 文件 0 errors，重启后 5/5 端点 200

## 反转理由

| v1 决策（错误） | v2 决策（正确） |
|----------------|-----------------|
| "9 引用点太多，删除需要批量改 + 回归测试" | "字段 1:1，编译+冒烟即可，零回归风险" |
| "未跑 acceptance.py 不敢动" | "冒烟 5/5 已经足够" |
| "保留 alias 层降低风险" | "alias 层是技术债，应尽快清除" |

## 历史记录

- **W-X28 17:18**: ADR-0008 v1（错误决策：保留 LinePO）— 出于"未跑 acceptance.py 不敢动"
- **W-X29 20:43**: ADR-0008 v2（本文件反转）— worker 实测 9 引用点全部可替换，0 风险

## 教训

- "未跑 acceptance.py 不敢动" 是过度保守 — 字段 1:1 + compile + smoke 即可
- 下一轮 audit 时不应该再列 LinePO 为兼容层（已删）
- 类似的"保留 alias 兼容层"决策应做更激进判断，避免技术债累积

## 跟进项

- 后续 audit 不要把 LinePO 列为遗留
- 类似别名类（若还有）应主动评估删除
