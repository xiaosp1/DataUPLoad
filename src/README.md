# IntcoEdge 解决方案骨架（v0.3）

老板 16:12 拍板：**.NET 8 LTS**（不是原计划的 .NET 9）。6 个工程，目标监听端口 **5288**。

## 工程清单

| 工程 | 类型 | TFM | 说明 |
|---|---|---|---|
| `IntcoEdge.Common` | classlib | `net8.0` | 公共类库：DTO、HTTP 客户端、配置、常量 |
| `IntcoEdge.Db` | exe | `net8.0` | SQLite + Flyway 迁移脚本运行器（W-A2 拥有） |
| `IntcoEdge.WebUI` | classlib | `net8.0` | Vue 3 + Element Plus 静态资产占位（Task B1 填充） |
| `IntcoEdge.Desktop` | WinExe (WPF) | `net8.0-windows` | Windows 桌面 UI：看门狗 / PG / 测试工具 |
| `IntcoEdge.EdgeHost` | ASP.NET Core | `net8.0` | 主进程：REST API + 嵌入 Web UI + /health |
| `IntcoEdge.Tests` | xUnit | `net8.0` | 单元测试 + 集成测试 |

## 快速命令

```bash
# 还原 + 编译单个工程
dotnet build src\IntcoEdge.EdgeHost\IntcoEdge.EdgeHost.csproj

# 编译全部 6 个工程
for p in src\IntcoEdge.*\IntcoEdge.*.csproj; do dotnet build "$p"; done

# 跑 EdgeHost（监听 0.0.0.0:5288）
dotnet run --project src\IntcoEdge.EdgeHost\IntcoEdge.EdgeHost.csproj

# 验证
curl http://127.0.0.1:5288/health    # -> 200 "ok"

# 跑测试
dotnet test src\IntcoEdge.Tests\IntcoEdge.Tests.csproj
```

## DoD 约束（Task A1）

- 解决方案 sln 文件头 4 字节固定为 `0D F0 09 00`（PM 审计标记）
- `dotnet build`（逐工程）必须 0 error
- `curl http://127.0.0.1:5288/health` 必须返回 200 `ok`

## 目录布局

```
src/
├── IntcoEdge.Common/
├── IntcoEdge.Db/
├── IntcoEdge.Desktop/
├── IntcoEdge.EdgeHost/   ← /health 在此
├── IntcoEdge.Tests/
├── IntcoEdge.WebUI/
└── README.md             ← 本文件
```
