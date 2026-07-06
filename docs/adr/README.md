# ADR 索引（Architecture Decision Records）

ADR 用于记录关键架构决策：背景（Context）、可选方案、最终决定（Decision）、以及带来的后果/影响（Consequences）。

- 编号规则：3 位数字递增（`001-xxx.md`、`002-xxx.md`、…），不回收编号。
- 状态：`已采纳` / `已废弃` / `待定`，在文首标注。
- 模板要点：标题、日期、状态、背景、选项、决定、后果。

## 索引

| 编号 | 标题 | 状态 | 日期 | 一句话摘要 |
| --- | --- | --- | --- | --- |
| 001 | [采集方案选择：A2零侵入直读海康PG为主链路](001-a2-pg-direct-read.md) | 已采纳 | 2026-07-06 | 海康链路零侵入优先，A2 直读 PG `intco` 库作为主采集方案，A1/DUAL 仅作为降级/双保险。 |
| 002 | [部署策略：self-contained单文件 + D:\IntcoEdge强制根目录](002-self-contained-deploy.md) | 已采纳 | 2026-07-06 | 现场不依赖预装 Runtime，部署强制落到 `D:\IntcoEdge\`，避免占用 C 盘和减少环境差异。 |
| 003 | [MVP策略：MES上传+Mock先行，海康采集后置](003-mvp-mes-mock-first.md) | 已采纳 | 2026-07-06 | Sprint 0 先交付 MES 上传链路 + Mock 闭环，5 天可交付，海康 PG/PLC/AI 后置到后续 Sprint。 |