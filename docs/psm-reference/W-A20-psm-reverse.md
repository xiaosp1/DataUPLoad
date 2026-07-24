# W-A20 — PSM 反编译重做 + 按功能块解析

**老板拍板**: 08:32 (大工程第 1+2 件)
**Worker**: PM 锋卫（亲自抓，体力+知识并行）
**开工**: 2026-07-22 08:34
**状态**: 🟡 进行中（架构已归档，反编译待启动）

---

## 1. W-A20 子任务（已拍/待拍）

| # | 子任务 | 状态 | 输出 | 时间预算 |
|---|---|---|---|---|
| **T1 解析 PSM 架构 + 按功能块分类** | ✅ **DONE 08:36** | `docs/delivered/2026-07-22-psm-architecture.md` | 已完成 |
| **T2 反编译 PSM jar → psm-decompiled/** | 🟡 **等老板拍/PM 启动** | `docs/domain/海康大屏逆向/psm-decompiled/**.java` | 15-30 分钟 |
| **T3 解析 P0 四个类实现逻辑** | 🟡 等 T2 完成 | `docs/delivered/2026-07-22-psm-decompiled-P01-DefectRecordServiceImpl.md` 等 | 2-4 小时 |
| **T4 解析 P1 类** | 🟡 等 T3 | 后续归档 | 4-8 小时 |
| **T5 解析 P2/P3 类** | 🟡 等 T4 | 后续归档 | 4-8 小时 |
| **T6 全量汇编文档** | 🟡 等 T5 | `docs/delivered/2026-07-22-psm-reverse-engineering-full.md` | 1 小时 |

## 2. T2 反编译工具链决策（PM 视角）

### 2.1 JDK
- ✅ PSM 自带 `jdk/bin/hik-java.exe`（海康专版）
- ❌ 系统无 java.exe（`Get-Command java.exe` 失败）
- **PM 决策**：用 hik-java 跑反编译工具（如不兼容，回退到下载 OpenJDK 21）

### 2.2 反编译工具
- **首选 vineflower 4.5.1**（IntelliJ 同款，质量最好）
- 备选 cfr 0.152（兼容性好）
- 下载位置：`https://github.com/Vineflower/vineflower/releases`

### 2.3 反编译命令（PM 准备）
```powershell
cd E:\DEMO\数据采集\docs\domain\海康大屏逆向
mkdir psm-decompiled -ErrorAction SilentlyContinue
& "10-反编译产物-NEW\PSM\server\jdk\bin\hik-java.exe" -jar vineflower-4.5.1.jar "10-反编译产物-NEW\PSM\server\IntcoScreen-1.0-SNAPSHOT-20260605135937.jar" "psm-decompiled\"
```

### 2.4 输出目录约定
```
E:\DEMO\数据采集\docs\domain\海康大屏逆向\psm-decompiled\
├── com/hikrobotics/solution/
│   ├── Application.java
│   ├── common/...
│   ├── module/
│   │   ├── alarm/...
│   │   ├── config/...
│   │   ├── defect/...
│   │   ├── detect/...
│   │   ├── line/...
│   │   ├── screen/...
│   │   └── yingke/...
└── (META-INF / BOOT-INF classpath 由 vineflower 跳过)
```

## 3. T3 P0 四个类的解析计划（PM 边读边写）

| 类 | 业务问题 | 关键反编译看点 |
|---|---|---|
| **DefectRecordServiceImpl** | 涨库根因？每条 defect 都 insert 吗？有去重吗？ | add() 方法实现 / isNeedBackup() / 字段填充逻辑 |
| **DefectRecordBackupServiceImpl** | PSM 怎么 retention？cron 是怎么动的？ | backup() 方法 / 保留天数配置 / move 流程 |
| **DetectDataTaskManager** | cron 表达式？每天几点？保留几天？ | @Scheduled 注解 / 触发方法 / 调用 DefectRecordBackupServiceImpl 还是 DefectRecordServiceImpl |
| **AlarmTaskManager** | alarm_record 清理逻辑（W-A18 已参考 0 0 0 * * ? + 90 天）| @Scheduled / DELETE WHERE / 时间字段名 |

## 4. 风险点（PM 已识别）

| 风险 | 影响 | 应对 |
|---|---|---|
| hik-java.exe 与 OpenJDK 行为可能不同（海康魔改）| 反编译失败 | 备选：下载 OpenJDK 21 / 试 cfr |
| 144 个 lib jar 里有 framework-starter-2.2.3-SNAPSHOT.jar（业务核心）| 这是公司自研 jar，可能也有反编译产物需求 | 看情况一起反编译 |
| 反编译产物 800+ java 文件，PM 一个人看不完 | T3 拖慢 | 派 Worker 干 T4/T5 体力活 |
| 反编译质量差异（变量名 `var1/var2` 还是 `lineNo`）| 解析时间翻倍 | cfr/vineflower 都试，选好的 |
| 老板中途改主意（比如要先止血）| T3 暂停 | T1 已归档是安全网 |

## 5. 给老板的选项

| 选项 | 含义 | PM 工作量 | 老板看得到的产出 |
|---|---|---|---|
| **A** PM 立刻启动 T2 (反编译) + T3 P0 解析 | 不间断 4-6 小时 | 大 | T1 已有，T3 边读边归档 |
| **B** 先派 Worker 干 T2 (反编译 + 验收)，PM 干 T3 解析 | 并行 2-3 小时 | 中 | 反编译产物 + 解析文档 |
| **C** 暂停大工程，先止血（派 W-A14 v2.4 加 backup + retention）| 4 小时 | 中 | DB 涨库解决，大工程延后 |
| **D** 同时干（A + W-A14 v2.4 并行，Worker 干 T2，反编译产物出后 PM 同时干 T3）| 最快但 PM 同时看多个事 | 大 | 止血 + 大工程并行 |

## 6. 当前 PM 已落地的归档（防信息丢失）

| 时间 | 文档 | 用途 |
|---|---|---|
| 08:25 | `W-A14-v2.3-result.md` | PSM.rar 重解压结果 |
| 08:32 | `2026-07-22-psm-db-comparison-detailed.md` | DB 详细对比 |
| 08:36 | `2026-07-22-psm-architecture.md` | 架构解析（185 类按 7 模块分类）|
| 08:36 | `W-A20-psm-reverse.md`（本归档）| 大工程工单 + 风险 + 选项 |

## 7. 一句话总结

> **T1 架构已归档（185 类按 7 模块分完），T2 反编译工具链准备好（hik-java + vineflower 待下），T3 P0 四个类（DefectRecord/Backup/Task/AlarmTaskManager）的解析计划已写，老板拍板 A/B/C/D 哪个就开干。**
