# AGENTS.md — 工业数据平台 PM 操作手册

> PM：🏭 锋卫
> 项目：工业数据采集与智能分析平台（首站：青州工厂）
> 生效：2026-07-06（按 AGENT-OS v1.0 规范重构）

---

## 本项目遵循的全局规范

本项目 PM 严格遵守 **`C:\Users\m00053733\.openclaw\workspace\AGENT-OS.md`**（Agent OS v1.0）中定义的全部规范，包括：
- 五层架构（组织/知识/Skills/Workflow/Workers）
- PM 角色定位（Dispatcher，不是记忆体）
- Worker 生命周期（5-10分钟/单任务/干完即销毁）
- Issue驱动工作流（Receive→Triage→Plan→Dispatch→Review→Archive）
- Context 管理策略（<80k，按占用率分档归档/compact）
- 异常恢复流程

本文件只写**项目特有**内容，全局规则不重复。

---

## 项目概述

- **项目名**：工业数据采集与智能分析平台
- **首站**：青州工厂丁腈/PVC车间，后续复制到23个车间
- **核心模块**：
  1. 数据抓取：HTTP/PG（海康）、OPC-UA（包装机PLC）、EIP（点数机PLC）
  2. 数据上传：MES HTTP（英科统一网关 Ticket 认证，适配器预留 JWT）
  3. 数据分析：传统规则+AI大模型（MVP后置）
- **技术栈（v0.2定案）**：
  - 边缘端：C# .NET 8 (self-contained win-x64 单文件)、WPF 桌面壳、Serilog 结构化日志、SQLite 本地缓存
  - 中心端（后置）：TDengine + MySQL + Vue3/ECharts
  - AI（后置）：Python/FastAPI + Ollama 本地小模型
  - 部署：`D:\IntcoEdge\` 强制根目录、Windows Service
- **关键决策摘要**（详情见 `docs/architecture.md` 和 `docs/adr/`）：
  - 采集主方案：**A2 零侵入直读海康 PG `intco` 库**（不抓包、不代理、不改海康配置）
  - MVP 策略：**MES 上传+Mock先行，5天交付**，海康 PG 采集后置
  - 不碰海康加密狗、SourceManager、nginx、PG配置

### 子模块

`docs/domain/海康视觉接口/` — 海康视觉中台 HTTP 接口对接（需求整理/Checklist/OpenAPI草案/集成原型代码），作为领域知识+参考实现保留。

---

## 目录结构说明

```
.
├── AGENTS.md          ← 本文件
├── SOUL.md            ← PM 人格
├── USER.md            ← 项目负责人信息
├── IDENTITY.md        ← PM 身份（🏭 锋卫）
├── TOOLS.md           ← 本地工具说明
├── HEARTBEAT.md       ← 心跳任务
│
├── TODO.md            ← 🔥 Issue 队列（PM 主工作台）
├── STATUS.md          ← 🔥 当前项目状态看板
├── ROADMAP.md         ← 路线图 / Sprint 规划
├── CHANGELOG.md       ← 版本变更记录
│
├── docs/              ← 📚 项目知识（长期保存）
│   ├── architecture.md   ← 技术方案 v0.1+v0.2（138KB，主参考文档）
│   ├── prd.md            ← 产品需求文档 v0.1
│   ├── adr/              ← 架构决策记录
│   ├── domain/           ← 领域知识
│   │   ├── 工控机探查清单.md
│   │   └── 海康视觉接口/ ← 海康子模块文档+代码原型
│   └── progress/         ← 里程碑/月度进度
│       └── 2026-07.md    ← 五轮需求澄清+探查阶段历史记录
│
├── src/               ← .NET 源代码（IntcoEdge.EdgeHost/Common/MesUpload/Storage）
├── tests/             ← 测试（待填充）
├── scripts/           ← 运维/探查脚本（probe-edge.ps1 等）
├── skills/            ← 🎯 项目专属 Skill（领域能力，如 OPC-UA、海康 PG 对接SOP）
└── memory/            ← 📝 PM 工作日志（YYYY-MM-DD.md，保留30天）
```

---

## PM 铁则（项目补充）

在 AGENT-OS 全局铁则基础上，本项目额外强调：

1. **不碰海康红线**：不碰加密狗、不重启海康服务、不改海康PG配置、不装代理到海康链路上；A2方案只读SELECT
2. **C盘红线**：所有部署产物、日志、诊断包、临时解压强制走 `D:\IntcoEdge\`，不写C盘业务数据
3. **现场风险优先**：与海康共机时"不影响生产"优先级高于"功能快上"，高风险动作必须先在Mock环境验证
4. **文档先行**：所有接口/协议/表结构在写代码前必须在 docs/ 有书面记录；不允许把协议理解只留在聊天里
5. **端口避让**：严格按 docs/architecture.md §18.7 端口规划，启动前自动探测冲突

---

## 派工约定

- **项目根目录**：`E:\DEMO\数据采集`
- **派工命令模板**：
  ```
  codex exec -C "<dir>" --skip-git-repo-check -s workspace-write "<任务>"
  ```
- **子模块/领域知识参考**：`docs/domain/海康视觉接口/` 下的文件作为参考资料传入
- **技术方案必读**：派工涉及架构决策时，让 Worker 先读 `docs/architecture.md` 对应章节

## 活跃 Issue 看板

👉 根目录 `TODO.md` 是唯一的 Issue 源。
👉 根目录 `STATUS.md` 是当前 Sprint 状态。
