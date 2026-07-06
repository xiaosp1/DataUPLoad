# STATUS — 工业数据采集与智能分析平台

> 最后更新：2026-07-06 22:25
> PM：🏭 锋卫
> 当前Sprint：Sprint 0（MVP 5天攻坚，MES上传+Mock先行）

## 当前状态：🟢 准备就绪，可启动 Day1

## 本周完成（准备阶段）
- 五轮需求澄清 + PRD v0.1 定稿（docs/prd.md）
- 技术方案 v0.1 + §18 v0.2 现场适配（docs/architecture.md，138KB）
- 青州现场工控机探查完成（probe-edge.ps1 v1.2）
- 关键技术决策定案：A2直读PG、D:\IntcoEdge\部署、self-contained单文件、端口5080/5188
- MVP范围锁定：MES上传模块+Mock服务+可观测底座+WPF诊断壳，5天交付
- ✨ workspace按 AGENT-OS v1.0 规范重构完成（目录/docs/adr模板/Issue驱动TODO）

## 正在做
- 准备启动 Day1：.NET 8 解决方案骨架 + 可观测底座 + MES接口定义

## 阻塞/风险
| 级别 | 项 | 应对 |
|---|---|---|
| 🟡 | MES真实地址/接口规范未拿到 | Mock先行，适配器预留 |
| 🟡 | 海康PG只读账号未拿到 | MVP不依赖A2采集，Sprint1再接入 |
| 🟢 | 单人开发5天范围紧 | 严格按v0.2重排Day1-5，PLC/AI/Vue全部后置 |
| 🟢 | 共机部署海康看门狗敏感 | 严格遵守不碰红线，CPU<20%/内存<1GB硬约束 |

## 下一步
1. 派 Worker #1：创建 .NET 8 sln 骨架和项目结构（Day1 第一个任务）
2. 搭好可观测最小集（Serilog + /health）
3. 推进 MES 接口定义
