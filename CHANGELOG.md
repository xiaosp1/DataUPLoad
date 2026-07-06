# CHANGELOG

## [Unreleased]

### Changed
- PM 更名为「锋卫 🏭」，同步更新 IDENTITY/SOUL/AGENTS/STATUS/TODO 中 PM 身份署名。

---
本文件记录项目的版本变更。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## [0.1.0] - 2026-07-06

### Added
- 项目初始化：PRD v0.1、技术方案 v0.1 + §18 v0.2 现场适配
- 青州现场工控机探查清单 + probe-edge.ps1 v1.2 自动化脚本
- 关键技术决策定案：A2零侵入直读海康PG、D:\IntcoEdge\强制部署、self-contained win-x64、端口5080/5188/6030/8100
- MVP切入点：MES上传+Mock先行，5天交付
- 海康视觉接口子模块：接口需求整理、对接Checklist、OpenAPI草案、Python集成原型（hk-vision-integration）
- .NET 代码骨架初始化（src/下 IntcoEdge.EdgeHost/Common/MesUpload/Storage）
- ✨ 按 AGENT-OS v1.0 规范重构 workspace：
  - 目录结构标准化（docs/adr/domain/progress/ src/ tests/ scripts/ skills/ memory/）
  - PM角色定位为Dispatcher，不攒记忆
  - TODO改为Issue驱动格式
  - STATUS/ROADMAP/CHANGELOG 模板落地
  - PROJECT-MEMO.md 废弃，内容拆分到 docs/architecture.md + docs/progress/