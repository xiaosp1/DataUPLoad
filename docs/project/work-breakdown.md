# 新 EdgeHost 工作分解（★ 老板 15:54 拍板）

> **老板决策汇总**：
> - **决策 1**：外部端（Web UI）= 主 UI，桌面端 = 副 UI ✅
> - **决策 2**：技术栈 = **C# .NET 9** ✅
> - **决策 3**：**多 Worker 并行**（不局限于 2 个），先拆任务登记在案，按计划干 ✅
>
> **老板原话**："进行所有工作之前，要把工作内容登记在案，咱按计划来干"
> —— 这是强制要求。**任何 Worker 开始干活前，必须先在此文档登记 Task。**

---

## 🎯 项目目标（一句话）

**抄 PSM 反编译代码（C# 翻译）+ 加 Windows 桌面 UI（看门狗/PG 工具/测试工具）**，替代现场 PSM，完成：
1. 接收视觉软件推数据 → 解析 → 入库
2. MES 拉取缺陷数据 + 字典
3. 报警推送英科系统
4. Web UI 展示数据
5. Windows 桌面 UI 测试工具

---

## 📂 任务目录

```
E:\DEMO\数据采集\
├── src/                              ← 新 EdgeHost 工程在这里
│   ├── IntcoEdge.sln                 ← 解决方案
│   ├── IntcoEdge.EdgeHost/           ← 主进程（REST API + Web UI）
│   ├── IntcoEdge.WebUI/              ← Web UI（Vue 3，嵌进 EdgeHost）
│   ├── IntcoEdge.Desktop/            ← Windows 桌面 UI（WPF）
│   ├── IntcoEdge.Common/             ← 公共类库（DTO、HTTP 客户端、配置）
│   ├── IntcoEdge.Tests/              ← 单元测试
│   └── IntcoEdge.Db/                 ← SQLite + Flyway 迁移
├── docs/project/                     ← 项目文档（已经有了）
│   ├── new-edgehost-scope.md         ← 总纲
│   └── work-breakdown.md             ← ★ 本文档
└── scripts/                          ← 构建/部署脚本
```

---

## 📊 任务分解（按"功能"和"UI"两条并行线）

### 主线 A：功能实现（4 个 Worker 串行 / 部分并行）

#### Task A1：项目骨架 + HTTP 接收端

| 项 | 内容 |
|---|---|
| **依赖** | 无 |
| **输入** | `docs/domain/海康大屏逆向/PSM/reverse-engineering/03-api-endpoints.md` |
| **输出** | `.NET 8 ASP.NET Core 工程`，监听 5288，能响应 `GET /health`（老板 16:12 拍板） |
| **DoD** | `dotnet run` 起得来 + `curl http://127.0.0.1:5288/health` 返回 200 + sln 头 0D F0 09 00 |
| **Worker** | W-A1 |
| **估时** | 1.5 天 |

#### Task A2：数据库 schema + 迁移

| 项 | 内容 |
|---|---|
| **依赖** | 无（可与 A1 并行） |
| **输入** | `docs/domain/海康大屏逆向/PSM/reverse-engineering/06-sql-schema.md` |
| **输出** | SQLite + Flyway 迁移脚本 V1.0~V1.19，22 张表建好 |
| **DoD** | 22 张表存在 + Flyway 历史表能查 |
| **Worker** | W-A2 |
| **估时** | 1.5 天 |

#### Task A3：DTO 定义 + HTTP 客户端

| 项 | 内容 |
|---|---|
| **依赖** | A1（工程骨架） |
| **输入** | `docs/domain/海康大屏逆向/PSM/reverse-engineering/04-dto-field-mapping.md` + `08-hikvision-yk-protocol.md` |
| **输出** | C# DTO 类 + HttpClient 封装（PSM 客户端 + 英科客户端） |
| **DoD** | DTO 能序列化/反序列化 PSM 协议 1+2 + 英科协议 3.x |
| **Worker** | W-A3 |
| **估时** | 2 天 |

#### Task A4：视觉数据接收端（/client/data/*）

| 项 | 内容 |
|---|---|
| **依赖** | A1 + A2 + A3 |
| **输入** | PSM `DetectDataController` / `AlarmRecordController` 反编译 .java |
| **输出** | `POST /client/data/{detect,alarm,status}` 端点实现，写入 SQLite |
| **DoD** | curl 测试通过，数据正确入库 |
| **Worker** | W-A4 |
| **估时** | 3 天 |

#### Task A5：字典 + 缺陷查询（/client/yk/*）

| 项 | 内容 |
|---|---|
| **依赖** | A1 + A2 + A3 |
| **输入** | PSM `YKController` 反编译 .java + `08-hikvision-yk-protocol.md` 协议 1+2 |
| **输出** | `GET /client/yk/line-defect` + `POST /client/yk/defect-record` |
| **DoD** | curl 测试，响应格式严格按 PSM 协议 1+2 |
| **Worker** | W-A5 |
| **估时** | 2 天 |

#### Task A6：英科登录 + Ticket 管理

| 项 | 内容 |
|---|---|
| **依赖** | A3 |
| **输入** | PSM yk 模块反编译 .java + `08-hikvision-yk-protocol.md` 协议 3.1 |
| **输出** | 登录服务 + 50 分钟自动重登 + Ticket 缓存 |
| **DoD** | 单元测试通过，能登录拿到 Ticket |
| **Worker** | W-A6 |
| **估时** | 1.5 天 |

#### Task A7：报警推送（英科统一网关）

| 项 | 内容 |
|---|---|
| **依赖** | A4 + A6 |
| **输入** | `08-hikvision-yk-protocol.md` 协议 3.2 + PSM `AlarmRecordServiceImpl` |
| **输出** | `POST /api/dataportal/invoke` 调用 + 重试队列 + 失败告警 |
| **DoD** | 模拟报警能成功推英科 + 网络断了会重试 |
| **Worker** | W-A7 |
| **估时** | 2.5 天 |

### 主线 B：UI 实现（3 个 Worker 部分并行）

#### Task B1：Web UI 框架 + 产线状态大屏（★ 主 UI 之一）

| 项 | 内容 |
|---|---|
| **依赖** | A1（HTTP 服务起得来） |
| **输入** | PSM 内嵌 web 资源 + 反编译 HTML/JS |
| **输出** | Vue 3 + Element Plus，产线状态大屏页面 |
| **DoD** | 浏览器能打开，看到 10 条线的实时状态 |
| **Worker** | W-B1 |
| **估时** | 3 天 |

#### Task B2：Web UI 数据查询页（缺陷/报警/字典/配方/日志/配置）

| 项 | 内容 |
|---|---|
| **依赖** | A5（查询接口就绪） |
| **输入** | A5 接口 + PSM 反编译前端 |
| **输出** | 6 个查询/管理页面 |
| **DoD** | 每个页面都能查+展示数据 |
| **Worker** | W-B2（可拆 B2a/B2b 两人） |
| **估时** | 5 天 |

#### Task B3：Windows 桌面 UI（★ 副 UI）

| 项 | 内容 |
|---|---|
| **依赖** | 无 |
| **输入** | PSM 桌面端反编译（很小）+ 老板"测试工具"需求 |
| **输出** | WPF 应用：①看门狗 ②PG 客户端 ③**测试工具**（发数据看通不通）|
| **DoD** | 桌面 UI 能启动 + 3 个功能都能用 + 测试工具能发 POST 验通 |
| **Worker** | W-B3（可拆 B3a/B3b 两人） |
| **估时** | 5 天 |

### 主线 C：集成 + 部署

#### Task C1：集成测试

| 项 | 内容 |
|---|---|
| **依赖** | A1-A7 + B1-B3 |
| **输入** | 全部 Task 输出 |
| **输出** | 集成测试报告 + 修复关键 bug |
| **DoD** | 端到端冒烟测试通过（接收 → 入库 → MES 拉取 → 报警推送）|
| **Worker** | W-C1 |
| **估时** | 3 天 |

#### Task C2：看门狗服务化

| 项 | 内容 |
|---|---|
| **依赖** | C1 |
| **输入** | A1 主进程 |
| **输出** | Windows 服务（sc create）+ 自动重启 + 开机自启 |
| **DoD** | kill 主进程能自动拉起 + 重启电脑能自启 |
| **Worker** | W-C2 |
| **估时** | 1.5 天 |

#### Task C3：部署 + 文档

| 项 | 内容 |
|---|---|
| **依赖** | C1 + C2 |
| **输入** | 全部 Task 输出 |
| **输出** | `D:\IntcoEdge\edge-v0.3\` 完整部署 + 用户手册 + 运维手册 |
| **DoD** | 现场能部署 v0.3，老 v0.2 暂留作备份 |
| **Worker** | W-C3 |
| **估时** | 2 天 |

---

## 📅 时间线（10 周总规划）

```
W1:    [A1] [A2] [A3]      （工程骨架 + DB + DTO，可 3 Worker 并行）
W2:    [A4] [A5] [A6]      （接收 + 查询 + 英科登录，可 3 Worker 并行）
W3:    [A4 cont] [A7] [B1] （接收续 + 报警推送 + Web 大屏）
W4:    [A7 cont] [B2] [B3] （报警推送续 + Web 查询页 + 桌面 UI）
W5:    [B2 cont] [B3 cont] （查询页续 + 桌面 UI 续）
W6:    [B2 cont] [B3 cont] （同上）
W7:    [集成] [C1]          （端到端测试）
W8:    [C1 cont] [C2]       （集成续 + 服务化）
W9:    [C2 cont] [C3]       （服务化续 + 部署）
W10:   [C3 cont]            （部署 + 文档 + 上线观察）
```

**总共 14 个 Task，估时 33.5 工作日**，3-6 Worker 并行，**~10 周完成**。

---

## 🚦 任务派工状态

| Task | 标题 | Worker | 状态 | 启动条件 |
|---|---|---|---|---|
| A1 | 项目骨架 + HTTP 接收端 | **w-a1-skeleton** | ✅ DONE 16:28（6 项目 net8.0，5288 /health 200，sln 头 0D F0 09 00）| 老板批 |
| A2 | 数据库 schema + 迁移 | **w-a2-db-schema** | ✅ DONE 16:29（22 表 = 20 user + sqlite_sequence + flyway_schema_history，19 Flyway 脚本 success=1）| 老板批 |
| A3 | DTO 定义 + HTTP 客户端 | **w-a3-dto-httpclient** | ✅ DONE 17:02（16 DTO + 4 HTTP 客户端 + 3 控制器 + 27 单元测试 + 冒烟测试）| 16:47 派工 |
| A4 | 视觉数据接收端 | TBD | 🟡 待派工 | A3 完成 |
| A5 | 字典 + 缺陷查询 | TBD | 🟡 待派工 | A3 完成 |
| A6 | 英科登录 + Ticket 管理 | TBD | 🟡 待派工 | A3 完成 |
| A7 | 报警推送 | TBD | 🟡 待派工 | A4+A6 完成 |
| B1 | Web 大屏 | **w-b1-webui** | ✅ DONE 16:55（Vue 3 + Element Plus + ECharts，20 文件，dotnet build 0 错）| 16:47 派工 |
| B2 | Web 查询页（6 个）| TBD | 🟡 待派工 | A5 完成 |
| **B3** | **Windows 桌面 UI** | **w-b3-desktop** | **✅ DONE 16:21**（git 9d3ac31, 18 文件, dotnet build 0 错）| 16:00 启动 |
| C1 | 集成测试 | TBD | 🟡 待派工 | A+B 全完 |
| C2 | 看门狗服务化 | TBD | 🟡 待派工 | C1 完成 |
| C3 | 部署 + 文档 | TBD | 🟡 待派工 | C1+C2 完成 |

### 📊 进度（★ 17:05 更新）

| 状态 | Task 数 | 占比 |
|---|---|---|
| ✅ 已完成 | **5**（A1, A2, A3, B1, B3）| **38%** |
| 🟢 跑着 | **0** | **0%** |
| 🟡 待派工 | **8** | **62%** |

---

## 📐 Worker 派工原则（老板说"多 Worker 并行"）

1. **A 线 + B 线可并行**：A 在做后端接口，B 在做前端 UI，互不阻塞
2. **同一 Task 可拆**：比如 B2 可拆 B2a（缺陷查询页）+ B2b（报警查询页）
3. **依赖严格**：下表里"启动条件"未满足不能开工
4. **每个 Worker 独立目录**：`src/IntcoEdge.Xxx/{WorkerID}/`，避免合并冲突
5. **每 Task 完成后提交 git**，写一行 commit message 说明 Task ID
6. **PM 验收**：每个 Task 完成时，PM 用 DoD 验证 + hex 验证 + 单元测试

---

## ⚠️ 关键约束

| 项 | 内容 |
|---|---|
| **语言** | C# **.NET 8**（★ 老板 16:12 拍板，原计划 .NET 9 改为 .NET 8）|
| **理由** | 本机只装了 .NET 8 SDK（无 9 SDK）；.NET 8 是 LTS 到 2026-11-10；将来部署期再装 .NET 9 runtime 或重新编译 |
| **部署** | NativeAOT 单文件 exe（沿用现场老 EdgeHost，部署时切 .NET 9 runtime） |
| **数据库** | **SQLite**（沿用现场，跟老 EdgeHost 一致） |
| **迁移** | Flyway（沿用 PSM 模式） |
| **Web UI 框架** | Vue 3 + Element Plus |
| **桌面 UI 框架** | WPF（.NET 生态最成熟）|
| **HTTP 客户端** | System.Net.Http.HttpClient + IHttpClientFactory |
| **测试** | xUnit（沿用 .NET 生态）|
| **禁止改的** | `D:\IntcoEdge\edge-v0.2\`（现场老 EdgeHost，不动）|
| **禁止动的** | `E:\DEMO\数据采集\docs\domain\海康大屏逆向\PSM\`（反编译产物，参考用）|
| **可以删的** | 旧的损坏 md（已清完）|

## 📝 决策日志（★ 项目期间发生的调整）

| 时间 | 决策 | 原因 |
|---|---|---|
| 2026-07-20 15:54 | 技术栈选 C# .NET 9 | 老板原话"现场老 EdgeHost 用 .NET 9" |
| **2026-07-20 16:12** | **改 C# .NET 8** | **本机只装了 .NET 8 SDK；.NET 8 是 LTS；W-A2 已选 net8.0** |

---

## 📝 待老板批

1. **本任务分解**你看 OK 吗？要不要加/减 Task？
2. **第一批 Worker 派工**：A1 + A2 + A3 + B1 + B3 这 5 个无依赖或弱依赖 Task，**现在就派**？还是你还有别的要求？
3. **Worker 数量上限**：老板你说"多 Worker 并行"——**PM 默认同时最多 4 个 Worker**（4 个是 GPT/Claude 单次响应能接住的上限）。OK 不？

---

## 📂 相关文档

- `docs/project/new-edgehost-scope.md` — 项目总纲（老板 15:29 拍板）
- `docs/project/work-breakdown.md` — ★ 本文档
- `docs/domain/海康大屏逆向/PSM/reverse-engineering/` — 9 个反编译文档（Task 输入源）
- `docs/domain/英科医疗手套车间/` — 业务定义
- `E:\项目\数采\1-前期调研\海康视觉检验数据接口需求_20240830.docx` — ★ 协议权威源

---

## ⏳ 等老板回 3 件事

1. **任务分解** OK 吗？要不要改？
2. **第一批 Worker**：派哪几个？（PM 建议 A1+A2+A3+B1+B3）
3. **Worker 上限 4 个** OK 吗？

**现场老 EdgeHost 进程已掉线，但备份在**——不影响今天工作。

你说"开干"我就派 Worker。
