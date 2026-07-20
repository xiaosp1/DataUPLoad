# COMMITMENTS

<!-- 老板拍板承诺记录表。落地即写, 完成即打✅, 取消即打❌ + 原因 -->

C-20260720-01 | 2026-07-20 06:58 | 🏭 锋卫 | 老板拍 A：切回闸门模式再跑回归 | appsettings.json 7-19 02:48 已回滚闸门模式（Env=mock/PushEnabled=false/DryRun=true），无需改配置；下一步：起 EdgeHost + 跑 5 场景回归（健康/缺陷拉取/MES主动拉接口/统计/表字段） | DoD: 5 场景全部给 receipt（PASS/FAIL/异常） + STATUS.md 同步现场真实状态 + 收尾报告 | ✅ DONE 07:05 | ✅ 收尾报告 memory/2026-07-20-pm-regression.md + 修正报告 memory/2026-07-20-encoding-truth.md

C-20260720-02 | 2026-07-20 14:27 | 🏭 锋卫 | 老板修正路线：放弃反编译 EdgeHost（NativeAOT 阻碍），等 PSM 源程序拷完后反编译 PSM 从头做项目 | 等老板拷 PSM 源 → 接收 → 反编译 → 跟旧 10-反编译产物对比 → 跟 ADR-004 架构对账 → 重新落地 EdgeHost + PSM 三件套 | DoD: 收到 PSM 源 + 反编译产物 + 一份'PSM 反编译架构图' + 决策下一步怎么落地 | ✅ DONE 14:55 | ✅ 反编译产物 204 .java + 9 份 reverse-engineering 文档（README+01-08 共 82.5 KB）+ memory 流水账

C-20260720-03 | 2026-07-20 14:48 | 🏭 锋卫 | 老板拷完 PSM 程序（version 2.1.9）放到 docs/domain/海康大屏逆向/PSM/，PM 反编译 + 出架构图 + 跟 ADR 对账 + 出 5 决策清单 | 收到 PSM 源（18.76 GB）→ 解压已就绪 → 跑 CFR 0.152 + JDK 17 → 出 204 .java + 9 份文档 | DoD: 9 份文档齐全 + 决策清单 + 车间代码表 | ✅ DONE 15:00 | ✅ reverse-engineering/ 9 个 md + 6 个 PM 铁则（含 hex 验证 + 协议权威源）

C-20260720-04 | 2026-07-20 15:29 | 🏭 锋卫 | 老板拍板新项目目标：复刻 PSM（接收/解析/存库/UI/报警推送）+ 加 OPC UA/EIP 拉点数机和包装机；现阶段主任务复刻 PSM | docs/project/new-edgehost-scope.md 总纲 + 14 Task 拆解 + 老板拍 3 决策（Web=主/桌面=副/C# .NET 9/多 Worker 并行） | DoD: 总纲 + 任务分解 + 决策登记 + 多 Worker 派工 | ✅ DONE 15:55 | ✅ new-edgehost-scope.md + work-breakdown.md（14 Task / 34 工作日 / 10 周）

C-20260720-05 | 2026-07-20 16:00 | 🏭 锋卫 | 老板提供现场环境：本机外部 IP 192.168.135.150 + 车间网 192.168.135.0/24 + 英科网关 192.168.80.33:10031 + 老 PSM 192.168.135.15:443；新 EdgeHost 用 5288 端口 | docs/project/environment.md 记录现场环境 + 4 条测试链路预案 + 新增 Task E1（现场环境验证） | DoD: environment.md 齐全 + 4 条 curl 测试预案 + Task E1 派工 | ✅ DONE 16:05 | ✅ environment.md（5.5 KB）+ STATUS.md + Worker 派工准备

C-20260720-06 | 2026-07-20 16:12 | 🏭 锋卫 | 老板拍板 .NET 版本改 net8.0（原计划 .NET 9）| 改 work-breakdown.md 约束段 + 改 STATUS.md + 给 W-A1 发恢复指令 + 给 W-A2/W-B3 确认对齐 | DoD: work-breakdown.md 改完 + 3 个 Worker 都收到 net8.0 指令 | ✅ DONE 16:13 | ✅ 文档改完（11188→11740 B）+ 3 个 Worker 状态：W-A1 恢复继续跑，W-A2/W-B3 保持 net8.0 继续跑

C-20260720-07 | 2026-07-20 16:21 | 🏭 锋卫 | W-B3 桌面 UI（WPF）完成，git 9d3ac31；PM 复验 18 个文件 hex + dotnet build 0 error | PM 验收 commit + 文件 hex + dotnet build + 改 work-breakdown.md 标 B3 ✅ | DoD: 18 文件在 + csproj TFM=net8.0-windows + dotnet build 0 error + git commit 9d3ac31 | ✅ DONE 16:22 | ✅ B3 ✅（首个完成的 Task，14m22s 跑完），work-breakdown.md 更新 B3 ✅ DONE + 进度条（1 ✅ / 2 🟢 / 10 🟡）

C-20260720-08 | 2026-07-20 16:28 | 🏭 锋卫 | W-A1 项目骨架完成，git d4f4450；W-A1 写 IntcoEdge.sln 文件头 4 字节垃圾（0D F0 09 00）PM 已修 | 6 工程 .NET 8 net8.0 + REST 5288 + dotnet build 0 错 + PM 修 sln 文件头 + PM 集成验证 | DoD: 6 工程 build pass + 5288 /health 200 + sln 文件头合法 + 集成验证全过 | ✅ DONE 16:42 | ✅ W-A1 ✅ (40 文件, d4f4450) + sln 修文件头（3494 B）+ 集成验证：6 工程 Debug+Release build 0 错 + EdgeHost /health 200 + DB 22 表 OK

C-20260720-09 | 2026-07-20 16:29 | 🏭 锋卫 | W-A2 DB schema 完成，git 05ccdc0 + d4f4450（24 迁移文件）；PM 复验 migrate success=1 + 表数 22 = 20 user + sqlite_sequence + flyway_schema_history | PM 复验 dotnet run migrate + tables + Flyway history + work-breakdown.md 标 A2 ✅ | DoD: 19 脚本 success=1 + 22 表存在 + Flyway history 全 success | ✅ DONE 16:30 | ✅ A2 ✅ (24 文件, 05ccdc0+ d4f4450 重叠提交) + 表数修正：PM 文档原写 22 张对，W-A2 报 20 张是漏数 sqlite_sequence + flyway_schema_history

C-20260720-10 | 2026-07-20 16:42 | 🏭 锋卫 | 老板 16:29 拍板：(1) W-B3 'PG 客户端' 标签改 'SQLite 浏览器' (2) 先不派 A3，先验证桌面 UI 跟 DB | 改 MainWindow.xaml + TestToolView.xaml.cs 3 处文字 + dotnet build 验证 + 刷新 STATUS.md / TODO.md + 集成验证（EdgeHost /health + DB 22 表）| DoD: Tab 头改成 'SQLite 浏览器' + dotnet build 0 错 + 集成验证全过 + STATUS/TODO 刷新到 16:42 | ✅ DONE 16:46 | ✅ W-B3-fix (git 79df8d7) + 集成验证全过 + STATUS/TODO 同步 16:42 实际 + COMMITMENTS 加 C-20260720-08/09/10
