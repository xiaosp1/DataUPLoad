# W-A21 — PSM 反编译 + 功能块技术路线全解析（老板 09:13 派）

**老板拍板**: 09:13 (4 件合并为 1 个 W-A21)
**Worker**: subagent（terminal/pty）
**开工**: 2026-07-22 09:14
**状态**: 🟡 派工中

---

## 0. 背景

老板 09:13 派 4 件活：
1. 总览 PSM 的架构
2. 详细解析 PSM 有哪些功能块（由大到小）
3. 各功能块的技术路线详细解析
4. 过程中产生的信息详细记录归档，最终产生 psm 的功能及代码文档

**已完成（PM 08:36 提前做了 1/4）**：
- `docs/delivered/2026-07-22-psm-architecture.md`（185 类 / 7 模块 / 顶层架构总览）

**本工单要完成 2/3/4**。

---

## 1. 输入

| 资产 | 路径 |
|---|---|
| 反编译产物 | `docs/domain/海康大屏逆向/psm-decompiled/` (204 java, 09:04 Worker 完工) |
| PSM jar | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/IntcoScreen-1.0-SNAPSHOT-20260605135937.jar` |
| 架构骨架 | `docs/delivered/2026-07-22-psm-architecture.md`（PM 已落档，可继续扩充） |
| DB 对比 | `docs/delivered/2026-07-22-psm-db-comparison-detailed.md`（19 表对照） |
| 金标准 mapping | `docs/domain/海康大屏逆向/PSM/reverse-engineering/05-edgehost-psm-mapping.md`（7/20 旧）|
| application 配置 | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/config/application-prod.yml` |
| EdgeHost 自家代码 | `src/IntcoEdge.EdgeHost/` + `src/IntcoEdge.Db/` |

## 2. 任务步骤

### T1: 读架构骨架 + 反编译产物清单（10 分钟）
- 读 `2026-07-22-psm-architecture.md` 全文
- 用 `tree /F psm-decompiled\BOOT-INF\classes\com\hikrobotics\solution` 列文件树
- 验证 7 个业务模块的 class 文件都在

### T2: 功能块详细解析（按 detect/line/alarm/yingke/defect/config/screen 由大到小，每块 1 个独立 markdown）

每个功能块文档结构：
```
# PSM {模块名} 功能块详细解析

## 1. 业务定位
- 解决什么问题
- 与其他模块的依赖关系

## 2. 类清单（按层）
- constant/dto/event/mapper/model/service/task/web 各列

## 3. 核心流程
- 主业务流程（时序图或步骤）
- 边界场景

## 4. 关键类逐个解析
- 类名 + 大小 + 责任
- 关键方法 + 入参/出参
- 关键 SQL（从 mapper xml 或 @Select 注解提）

## 5. 数据库交互
- 涉及表 + 字段
- retention 策略

## 6. 与 EdgeHost 对照
- 已对齐部分（W-A*）
- 缺口（W-A 后续）
- 移植优先级

## 7. 风险 / 注意点
```

**输出位置**：`docs/delivered/2026-07-22-psm-{module}-detailed.md`
- 2026-07-22-psm-detect-detailed.md （47 类）
- 2026-07-22-psm-line-detailed.md （51 类）
- 2026-07-22-psm-alarm-detailed.md （24 类）
- 2026-07-22-psm-yingke-detailed.md （18 类）
- 2026-07-22-psm-defect-detailed.md （4 类）
- 2026-07-22-psm-config-detailed.md （5 类）
- 2026-07-22-psm-screen-detailed.md （7 类）
- 2026-07-22-psm-common-detailed.md （13 类）

### T3: 技术路线详细解析（每个功能块 1 个独立 markdown）

每个技术路线文档结构：
```
# PSM {模块名} 技术路线解析

## 1. 架构模式
- Spring Boot + MyBatis + Flyway + HikariCP
- 多模块 / 单模块 / 包结构
- 配置方式（yml + @ConfigurationProperties + @Value）

## 2. 数据访问层
- MyBatis-Plus vs MyBatis vs JDBC
- mapper xml vs annotation
- 事务管理（@Transactional 范围）

## 3. 业务层
- Service 接口/实现分离
- 事件驱动（ApplicationEvent + @EventListener）
- 异步处理（@Async / TaskExecutor）

## 4. 任务调度
- @Scheduled cron 表达式
- ScheduleConfig 线程池配置
- 错峰策略

## 5. Web 层
- Spring MVC @RestController
- 全局异常处理（@ControllerAdvice + @ExceptionHandler）
- 参数校验（@Valid + JSR-303）

## 6. 与 EdgeHost 对照（技术路线差异）
- 我们用 ASP.NET Core vs PSM 用 Spring Boot
- 我们用 EF Core vs PSM 用 MyBatis
- 我们用 BackgroundService vs PSM 用 @Scheduled
- 差异怎么映射（W-A* 怎么 1:1）

## 7. 移植建议
- 直接抄：XX
- 改写：XX（语言/框架差异）
- 不抄：XX（PSM 独有，不适合 EdgeHost）
```

**输出位置**：`docs/delivered/2026-07-22-psm-{module}-tech.md`

### T4: 整合文档（最终 PSM 功能及代码文档）

**输出位置**：`docs/delivered/2026-07-22-psm-full-manual.md`

结构：
```
# PSM 功能及代码文档（完整版）

## Part 1: 架构总览（链 PM 已写的 2026-07-22-psm-architecture.md）
## Part 2: 功能块详解（链 7 个 detailed.md）
## Part 3: 技术路线详解（链 7 个 tech.md）
## Part 4: PSM vs EdgeHost 对照表（链 db-comparison + mapping）
## Part 5: 移植优先级 + 后续 W-A* 计划
## Part 6: 风险清单
```

### T5: PM 验收 + 索引化（Worker 不做）

Worker 完工后 PM 锋卫写索引到 `docs/delivered/INDEX.md`。

## 3. DoD

| DoD | 验收 |
|---|---|
| T1 读完所有反编译文件（204 java）| ✅ file count 检查 |
| T2 7 个功能块 detailed.md 全写完 | ✅ 文件存在 + 每文件 > 5 KB |
| T3 7 个功能块 tech.md 全写完 | ✅ 文件存在 + 每文件 > 3 KB |
| T4 整合文档 2026-07-22-psm-full-manual.md | ✅ 文件存在 + > 20 KB |
| 归档到 `docs/delivered/` 目录 | ✅ |
| **每解析完一个 P0 类立即归档**（老板要求）| ✅ 不批量攒 |
| 不修改 PM 已落档的 `2026-07-22-psm-architecture.md`（除非扩充）| ✅ |
| 不 git commit（PM 统一 commit）| ✅ |
| 不重启 EdgeHost（PM 已经手动启了 PID 14656）| ✅ |
| 不碰 `10-反编译产物-NEW/` | ✅ |

## 4. 禁止

- ❌ 不要 commit git
- ❌ 不要重启 EdgeHost（PID 14656 是 PM 09:06 手动起来的，承载现场业务）
- ❌ 不要修改 PM 落档的 8 个文档（除非 append）
- ❌ 不要改任何 .cs 代码（这是反编译 + 文档活，不是改 EdgeHost 代码）

## 5. 时间预算

| Task | 预估 |
|---|---|
| T1 读文件树 | 10 分钟 |
| T2 7 个 detailed.md（每块 ~15 分钟）| 2 小时 |
| T3 7 个 tech.md（每块 ~10 分钟）| 1 小时 |
| T4 整合文档 | 30 分钟 |
| **总** | **3.5 - 4 小时** |

老板预期"派工干活"，所以可以跑 4 小时。

## 6. 派工命令

```bash
cd "E:\DEMO\数据采集"
codex exec -C "$PWD" --skip-git-repo-check -s workspace-write "
W-A21 派工: PSM 反编译全解析（架构/功能块/技术路线/整合文档）
完整工单: docs/delivered/W-A21-psm-reverse-engineering-full.md
读完整个文档，按 T1-T4 执行。
每解析完一个 P0 类立即归档（老板要求）。
完工写 docs/delivered/W-A21-result.md
"
```

或 sessions_spawn 用 native subagent：
```
你是 PM 锋卫的 W-A21 反编译解析 Worker。
执行 W-A21 派工单: docs/delivered/W-A21-psm-reverse-engineering-full.md
T1-T4 顺序执行。老板强调：每解析完一个 P0 类立即归档。
完工后写 docs/delivered/W-A21-result.md。
不要 commit。不要重启 EdgeHost（PID 14656）。
不要改 PM 已落档的 8 个文档。
```

## 7. Worker 完工信号

`docs/delivered/W-A21-result.md` 写完 → PM 验收 → 写 `INDEX.md` → 通知老板。
