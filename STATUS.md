# STATUS.md

<!-- 项目当前状态快照（PM 维护，对老板汇报用） -->

## 🟢 当前阶段：**新 EdgeHost v0.3 开发启动**（老板 15:54 拍板）— **Task A1 ✅ DONE 16:28**

| 项 | 值 |
|---|---|
| 项目代号 | **edge-v0.3**（沿用老 EdgeHost v0.2 版本号序列）|
| 项目目标 | 复刻 PSM（接收/解析/存库/Web UI/报警推送）+ Windows 桌面 UI |
| 技术栈 | **C# .NET 8**（老板 16:12 改，原 .NET 9）|
| 部署时切 | NativeAOT 单文件 exe + 装 .NET 9 runtime（沿用现场老 EdgeHost）|
| 数据库 | SQLite（沿用现场） |
| Web UI | Vue 3 + Element Plus |
| 桌面 UI | WPF |
| 部署目标 | 本机 `D:\IntcoEdge\edge-v0.3\`（待建）|
| 端口 | **5288**（REST API）|
| 当前 commit | v0.1.0（损坏字节，工作区已清）|
| 工作区干净度 | ✅ 清爽：docs/ 只有 17 个合法 md + PSM 反编译产物 + 损坏的 src/ 占位 |

## 📊 任务状态

**已拆解**：14 个 Task（详见 `docs/project/work-breakdown.md`）
**已派工**：A1 ✅ / A2 ✅ / B3 ✅
**已完成**：A1 项目骨架（net8.0，6 工程，5288 /health 200）+ B3 桌面 UI + A2 DB 迁移（intco.db）

## 🔑 老板 16:00 拍板的环境

| 项 | 值 |
|---|---|
| 本机外部 IP | `192.168.135.150` |
| 现场老 PSM | `https://192.168.135.15:443` |
| 英科网关 | `http://192.168.80.33:10031/api/dataportal/invoke` |
| 车间工控机群 | `192.168.135.70-89` |

## 🚧 现场 EdgeHost 状态

| 项 | 状态 | 备注 |
|---|---|---|
| 老 EdgeHost 进程 PID 31260 | ❌ **已掉线**（15:34 后）| 不影响工作 |
| 老 EdgeHost 文件目录 | ✅ `D:\IntcoEdge\edge-v0.2\` 完整 | 备份在 `.bak-edgehost/` |
| 现场老 PSM | ✅ 跑着 | `https://192.168.135.15:443` |
| 英科网关 | ✅ 通 | 25ms 延迟 |

## 🚦 Worker 派工状态（16:12 更新）

| Worker | Task | 状态 | 备注 |
|---|---|---|---|
| **w-a1-skeleton** | .NET 8 工程骨架 | 🟢 跑着 | 16:10 阻塞，16:12 改 net8.0 恢复 |
| **w-a2-db-schema** | SQLite + Flyway | 🟢 跑着 | net8.0，已对齐 |
| **w-b3-desktop** | WPF 桌面 UI | 🟢 跑着 | net8.0-windows，已对齐 |

**关键决策**：2026-07-20 16:12 老板拍板用 **.NET 8**（不用 .NET 9），理由：本机只装了 .NET 8 SDK；W-A2 已选 net8.0；强扭 net9.0 浪费 5-10 分钟装 SDK

## 📂 workspace 结构（v0.3 启动）

```
E:\DEMO\数据采集\
├── docs/                      ← ★ PM 维护的文档
│   ├── domain/
│   │   ├── 海康大屏逆向/
│   │   │   ├── 10-反编译产物/        (保留，海康老产物)
│   │   │   └── PSM/                 (反编译产物 + 9 份文档)
│   │   └── 英科医疗手套车间/         (4 份业务定义)
│   └── project/
│       ├── new-edgehost-scope.md    (项目总纲)
│       ├── work-breakdown.md        (14 Task 分解)
│       └── environment.md           (★ 现场环境)
├── memory/                    ← 持久化记忆（4 个合法 md）
├── src/                       ← ⚠️ 待创建（Worker 写代码）
├── scripts/                   ← ⚠️ 待创建（Worker 写脚本）
├── 顶层元数据/                ← AGENTS/SOUL/IDENTITY 等
└── .bak-edgehost/             ← 老 EdgeHost 备份
```

## 🎯 接下来要做的事（PM 待派工）

1. **派 Task E1（环境验证）** —— 半天
2. **派 Task A1（工程骨架）** —— 1.5 天
3. **派 Task A2（DB schema）** —— 1.5 天
4. **派 Task A3（DTO + HTTP 客户端）** —— 2 天
5. **派 Task B1（Web 大屏）** —— 3 天
6. **派 Task B3（桌面 UI 起步）** —— 5 天

**第一批 6 个 Worker 并行**（老板说"多 Worker 并行"）。

## ⚠️ 风险

| 风险 | 缓解 |
|---|---|
| Worker 派工失败 | 老板说"如果遇到派工出问题，你可以随时停下来，然后咱先解决派工的问题" |
| 老 PSM 在 `https://192.168.135.15:443` 上跑着，新 EdgeHost 必须用 5288 不冲突 | 已确认 |
| 现场老 EdgeHost 已掉线，但没有通知车间 | 不影响今天工作，但建议明天汇报 |
| v0.1.0 commit 损坏字节 | 不 git checkout，直接新工程 |

## 📋 相关文档

- `docs/project/work-breakdown.md` — 任务分解（★）
- `docs/project/new-edgehost-scope.md` — 项目总纲
- `docs/project/environment.md` — 现场环境
- `docs/domain/海康大屏逆向/PSM/reverse-engineering/` — 反编译文档（9 个 md）
- `docs/domain/英科医疗手套车间/` — 业务定义（4 个 md）
- `COMMITMENTS.md` — 老板拍板承诺记录
- `TODO.md` — 待办清单
