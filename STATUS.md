# STATUS.md

<!-- 项目当前状态快照（PM 维护，对老板汇报用） -->

## 🟢 当前阶段：**新 EdgeHost v0.3 开发中**（★ 16:42 刷新）

| 项 | 值 |
|---|---|
| 项目代号 | **edge-v0.3**（沿用老 EdgeHost v0.2 版本号序列）|
| 项目目标 | 复刻 PSM（接收/解析/存库/Web UI/报警推送）+ Windows 桌面 UI |
| 技术栈 | **C# .NET 8**（老板 16:12 改，原 .NET 9）|
| 部署时切 | NativeAOT 单文件 exe + 装 .NET 9 runtime（沿用现场老 EdgeHost）|
| 数据库 | SQLite（沿用现场） |
| Web UI | Vue 3 + Element Plus（待 W-B1 开工）|
| 桌面 UI | WPF net8.0-windows（已交付骨架 W-B3）|
| 部署目标 | 本机 `D:\IntcoEdge\edge-v0.3\`（待建）|
| 端口 | **5288**（REST API）|
| 当前 commit | 05ccdc0（A2 SQLite + Flyway）|

## 📊 任务状态（★ 16:42 实际）

**已拆解**：14 个 Task（详见 `docs/project/work-breakdown.md`）

| 状态 | Task | 备注 |
|---|---|---|
| ✅ DONE 16:28 | **A1 项目骨架** | git `d4f4450`，6 工程 net8.0，5288 /health 200，sln 头 0D F0 09 00 PM 已修 |
| ✅ DONE 16:29 | **A2 DB schema** | git `d4f4450`（24 个迁移文件）+ `05ccdc0`（.gitignore 修补），22 表（20 user + sqlite_sequence + flyway_schema_history），19 Flyway 脚本 success=1 |
| ✅ DONE 16:21 | **B3 桌面 UI** | git `9d3ac31`，18 文件，dotnet build 0 错，3 Tab（看门狗/SQLite 浏览器/测试工具）|
| 🟢 跑着 16:47 | **A3 DTO + HTTP 客户端** | session `b38a287a-...`，预计 2 天 |
| 🟢 跑着 16:47 | **B1 Web 大屏** | session `d23ac74b-...`，预计 3 天 |
| 🟡 待派工 | A4-A7, B2, C1-C3 | **8 个** 等下一步 |
| 🟢 **集成验证** | EdgeHost /health 200 | DB 22 表 OK，6 工程 Debug+Release 都 0 错 |

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
| 老 EdgeHost 进程 PID 31260 | ❌ **已掉线**（15:34 后）| 不影响工作，等 v0.3 替代 |
| 老 EdgeHost 文件目录 | ✅ `D:\IntcoEdge\edge-v0.2\` 完整 | 备份在 `.bak-edgehost/` |
| 现场老 PSM | ✅ 跑着 | `https://192.168.135.15:443` |
| 英科网关 | ✅ 通 | 25ms 延迟 |

## 🚦 Worker 派工状态（★ 16:42 实际：3 全完成，0 跑着，10 待派工）

| Worker | Task | 状态 | 备注 |
|---|---|---|---|
| **w-a1-skeleton** | .NET 8 工程骨架 | ✅ **DONE 16:28** | git d4f4450，6 工程 |
| **w-a2-db-schema** | SQLite + Flyway | ✅ **DONE 16:29** | git 05ccdc0，22 表 |
| **w-b3-desktop** | WPF 桌面 UI | ✅ **DONE 16:21** | git 9d3ac31，18 文件 |
| **w-a3-dto-httpclient** | DTO + HTTP 客户端 | 🟢 **跑着 16:47** | session b38a287a-471c-45fd-8ee7-964e1a86313b |
| **w-b1-webui** | Vue 3 Web 大屏 | 🟢 **跑着 16:47** | session d23ac74b-e255-4a9a-b28c-f22c8bbaee4c |

## 📂 workspace 结构（★ 16:42 已建）

```
E:\DEMO\数据采集\
├── docs/                      ← PM 维护的文档
│   ├── domain/
│   │   ├── 海康大屏逆向/             (反编译产物 + 9 份文档)
│   │   └── 英科医疗手套车间/         (4 份业务定义)
│   └── project/
│       ├── new-edgehost-scope.md    (项目总纲)
│       ├── work-breakdown.md        (★ 14 Task 分解，最新)
│       └── environment.md           (现场环境)
├── memory/                    ← 持久化记忆
├── src/                       ← ✅ 已建：IntcoEdge.sln + 6 工程
│   ├── IntcoEdge.sln                (3494 B，PM 修过文件头)
│   ├── IntcoEdge.Common/            (Constants.cs + csproj)
│   ├── IntcoEdge.Db/                (SQLite + Flyway + 19 migrations + SmokeTest)
│   ├── IntcoEdge.WebUI/             (空骨架)
│   ├── IntcoEdge.Desktop/           (WPF 桌面 UI)
│   ├── IntcoEdge.EdgeHost/          (REST 5288 /health 200)
│   ├── IntcoEdge.MesUpload/         (TODO A7)
│   ├── IntcoEdge.Storage/           (TODO A4)
│   └── IntcoEdge.Tests/             (ConstantsTests + HealthEndpointTests)
├── scripts/                   ← 编码验证工具（10 个 .py/.ps1）
├── IntcoEdge.sln              ← 根目录解决方案（3494 B，PM 修过文件头）
├── 顶层元数据/                ← AGENTS/SOUL/IDENTITY 等
└── .bak-edgehost/             ← 老 EdgeHost 备份
```

## 🎯 接下来要做的事（PM 16:42 当前状态）

**已停**：等老板拍下一步（老板 16:29 决策：先不派，验证桌面 UI 跟 DB）

**已验证（16:42 完成）**：
- ✅ dotnet build IntcoEdge.sln Debug = 0 warning 0 error
- ✅ dotnet build IntcoEdge.sln Release = 0 warning 0 error
- ✅ EdgeHost 启动 5288 /health = 200 OK
- ✅ DB 连通：intco.db 128 KB，22 表

**老板 16:29 拍的两件事**：
1. **W-B3 "PG 客户端" 标签 → 改成 "SQLite 浏览器"** —— 待派 Worker 改
2. **先不派 A3** —— 等桌面 UI + DB 集成验证完毕（✅ 已验证完毕）

**PM 下一步候选（等老板拍）**：
- **派 W-B3-fix（标签改 SQLite）** —— 5 分钟
- **派 W-A3（DTO + HTTP 客户端）** —— 2 天
- **派 W-B1（Web 大屏）** —— 3 天（依赖 A1 完成 ✅）
- **派 W-E1（现场环境验证 curl 4 条链路）** —— 半天

## ⚠️ 风险（★ 16:42 更新）

| 风险 | 缓解 |
|---|---|
| W-A1 和 W-A2 同时写 IntcoEdge.Db 导致 git 冲突 | 已通过 commit 解决（d4f4450 包含 W-A1+W-A2 重叠部分）|
| W-B3 Tab 标签 "PG 客户端" 与实际 SQLite 不一致 | 老板 16:29 拍要改，待派 Worker |
| 老 EdgeHost 已掉线 | 不影响今天工作，等 v0.3 替代 |
| v0.1.0 commit 损坏字节 | 不 git checkout，直接新工程 |
| W-A1 sln 文件头 4 字节垃圾（0D F0 09 00）| ✅ PM 已修（3494 B）|

## 📋 相关文档

- `docs/project/work-breakdown.md` — 任务分解（★）
- `docs/project/new-edgehost-scope.md` — 项目总纲
- `docs/project/environment.md` — 现场环境
- `docs/domain/海康大屏逆向/PSM/reverse-engineering/` — 反编译文档（9 个 md）
- `docs/domain/英科医疗手套车间/` — 业务定义（4 个 md）
- `COMMITMENTS.md` — 老板拍板承诺记录
- `TODO.md` — 待办清单
