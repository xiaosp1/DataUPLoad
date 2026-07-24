# W-YK-01 报告 — yingke 双开关 ADR 留痕

**完成时间**: 2026-07-24 19:18
**执行人**: 锋卫（手动）
**工时**: 5 分钟

## 改动文件
| 文件 | 改动 |
|---|---|
| `docs/adr/0007-yingke-dual-switch.md` | 新建（ADR 留痕） |

## 关键发现
- YKConfig.java 在 W-X13d 已实现双开关（loginEnabled + uploadEnabled）
- 代码 Javadoc 已写完整解释
- 旧字段 `enabled` 通过 getter 兼容（`loginEnabled || uploadEnabled`）

## ADR 内容
1. 背景：PSM 单开关 vs DPL 双开关
2. 决策：双开关
3. 业务场景表（4 种组合）
4. 兼容性说明
5. 灰盒默认配置

## 已知限制
- 无
