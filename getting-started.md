# Getting Started — IntcoEdge（工业数据采集与智能分析平台，青州首站）

一句话简介：面向集团车间的工业数据采集与上传底座，首站在青州工厂丁腈/PVC 车间落地，通过 MES HTTP 上报并预留海康 PG/OPC-UA/EIP 采集接入。

## 环境要求
- Windows x64（现场工控机/开发机）
- .NET 8 SDK（开发、构建用）；现场部署采用 self-contained 单文件，不需要预装 Runtime
- PowerShell 5.1 或 PowerShell 7+
- 部署根目录：`D:\IntcoEdge\`（C 盘不放业务数据/日志/诊断包）

## 编译运行
在仓库根目录执行：

```powershell
dotnet restore IntcoEdge.sln
dotnet build IntcoEdge.sln -c Release --nologo
dotnet run --project src/IntcoEdge.EdgeHost/IntcoEdge.EdgeHost.csproj -c Release --no-build
```

启动项目是 `IntcoEdge.EdgeHost`（Exe）；`IntcoEdge.Common`/`IntcoEdge.MesUpload`/`IntcoEdge.Storage` 为类库。

## 探查脚本
`scripts/probe-edge.ps1` 用于现场工控机只读探查：收集主机/进程/端口/配置/PG/MES 连通性线索，并自动给出 A1/A2/DUAL 采集建议。

```powershell
powershell -ExecutionPolicy Bypass -File scripts/probe-edge.ps1 -HikRoot "D:\PSM"
```

- 全程只读：不停服务、不改配置、不删文件、不碰加密狗
- 输出目录：`reports/probe/`（相对仓库根；脚本会自动向上探测仓库根），文件名 `probe-report-<hostname>-<yyyyMMdd-HHmmss>.md`
- 目录不存在会自动创建；`reports/` 已在 `.gitignore` 中忽略，不会入库

## 文档地图
- `AGENTS.md`：项目规则/PM 操作手册（本项目特有的铁则、派工约定、目录结构）
- `SOUL.md`：PM 人格/风格
- `IDENTITY.md`：PM 身份信息（锋卫 🏭）
- `USER.md`：项目负责人与偏好
- `TOOLS.md`：本地工具与环境备注
- `docs/architecture.md`：技术方案主文档（v0.1 + §18 v0.2 现场适配，138KB，必读）
- `docs/prd.md`：产品需求文档 v0.1
- `docs/adr/`：架构决策记录（ADR 001/002/003…），含索引 `docs/adr/README.md`
- `docs/domain/`：领域知识（工控机探查清单、海康视觉接口子模块文档/原型）
- `TODO.md`：Issue 队列（PM 主工作台）
- `STATUS.md`：当前 Sprint 状态看板
- `ROADMAP.md`：版本路线图 / Sprint 规划
- `CHANGELOG.md`：版本变更记录

## 注意事项
- 不碰海康红线：不碰加密狗、不重启海康服务、不改海康 PG 配置、不装代理到海康链路；A2 方案只读 `SELECT`。
- C 盘红线：所有部署产物、日志、诊断包、临时解压强制走 `D:\IntcoEdge\`。
- 端口避让：现场启动前自动探测端口冲突，EdgeHost 默认避开海康占用端口；关注 `5080`（EdgeHost）、`5188`（诊断/看板）以及 `5432/8080/8100` 等海康相关端口，详见 `docs/architecture.md` §18.7 端口规划。