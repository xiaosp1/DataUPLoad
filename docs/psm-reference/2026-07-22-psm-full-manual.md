# PSM 反编译 + 解析全手册

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档（全文索引）
**优先级**: 🟡 P1（综合文档）

---

## 0. 文档导航

本手册由 17 个文档组成：

### 0.1 PM 已落档（前置）
- `2026-07-22-psm-architecture.md` — PSM 整体架构概览
- `2026-07-22-psm-db-comparison-detailed.md` — 数据库对比详细
- `W-A14-v2.1-result.md` / `W-A14-v2.3-result.md` / `W-A14-v2.4-status-cleanup.md`
- `W-A20-T2-decompile.md` / `W-A20-T2-decompile-result.md`
- `W-A20-psm-reverse.md`

### 0.2 W-A21 T2: 7 个模块详细解析（功能块）
- `2026-07-22-psm-detect-detailed.md` — 🔴 P0
- `2026-07-22-psm-line-detailed.md` — 🟡 P1
- `2026-07-22-psm-alarm-detailed.md` — 🟡 P1
- `2026-07-22-psm-yingke-detailed.md` — 🟡 P1
- `2026-07-22-psm-defect-detailed.md` — 🟢 P2
- `2026-07-22-psm-config-detailed.md` — ⚪ P3
- `2026-07-22-psm-screen-detailed.md` — ⚪ P3
- `2026-07-22-psm-common-detailed.md` — 🟡 P1

### 0.3 W-A21 T3: 8 个技术路线解析（架构）
- `2026-07-22-psm-detect-tech.md`
- `2026-07-22-psm-line-tech.md`
- `2026-07-22-psm-alarm-tech.md`
- `2026-07-22-psm-yingke-tech.md`
- `2026-07-22-psm-defect-tech.md`
- `2026-07-22-psm-config-tech.md`
- `2026-07-22-psm-screen-tech.md`
- `2026-07-22-psm-common-tech.md`

### 0.4 W-A21 T4: 本文档（整合）

---

## 1. PSM 系统全景

### 1.1 系统定位

**PSM**（Production Solution Manager）是海康机器人面向工业视觉检测场景的**生产管理平台**。核心职责：
- 接收产线侧检测数据（缺陷记录、良率统计、设备状态）
- 提供产线/方案/缺陷管理后台
- 实时大屏展示 + 报警推送
- 与上层鹰科（YK）系统集成
- 加密狗授权保护

### 1.2 模块划分（8 大模块）

| 模块 | 文件数 | 优先级 | 业务核心 |
|---|---|---|---|
| **detect** | 37 + 4 XML | 🔴 P0 | 数据接收、统计、retention |
| **line** | 54 + 4 XML | 🟡 P1 | 产线/方案/状态管理 |
| **alarm** | 35 + 1 XML | 🟡 P1 | 报警接收/去重/推送/retention |
| **yingke** | 15 + 0 XML | 🟡 P1 | 鹰科 HTTP 集成 |
| **defect** | 6 + 1 XML | 🟢 P2 | 产线-缺陷绑定 |
| **config** | 5 + 0 XML | ⚪ P3 | 系统配置 KV |
| **screen** | 5 + 0 XML | ⚪ P3 | 大屏数据聚合 |
| **common** | 11 + 0 XML | 🟡 P1 | 横切关注点 + GlobalTaskManager |

**总计**: 168 个 java + 17 个 XML = 185 个文件（实际反编译 169 个 java，差异在架构文档）

### 1.3 技术栈总览

| 层 | 技术 |
|---|---|
| Web | Spring Boot + Spring MVC + Spring Event |
| ORM | MyBatis-Plus 3.x + MyBatis XML Mapper |
| 数据库 | MySQL / PostgreSQL 兼容（部分 SQL 用 PG 语法如 `ON CONFLICT`、`row_number() OVER`、`TO_CHAR`）|
| HTTP | WebClient（HikWebClient，同步阻塞）|
| WebSocket | 框架组件 + WsMessage（4 种类型）|
| 工具 | Hutool + Guava + Apache Commons |
| Excel | EasyExcel + POI |
| 日志 | SLF4J + Logback |
| 调度 | Spring @Scheduled + ThreadPoolTaskScheduler |
| 加密狗 | JNA 调用第五代 SDK |
| JSON | Hutool JSONArray + Jackson + MyBatis TypeHandler |

### 1.4 数据流全景

```
┌──────────────────────────────────────────────────────────────────┐
│ 客户端（Edge/PLC）                                                 │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ EdgeHost / 大屏 / 客户端软件                                 │    │
│  │   - 上报检测数据、报警、设备状态                              │    │
│  │   - 接收 WebSocket 推送（报警文本/音效、方案变更、大屏数据）│    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
        ↓ HTTP                       ↑ WebSocket
┌──────────────────────────────────────────────────────────────────┐
│ PSM Server（Spring Boot）                                         │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │   detect       │  │   line         │  │   alarm        │     │
│  │ (数据接收+    │  │ (产线+方案)    │  │ (报警中枢)     │     │
│  │  统计+        │  │                │  │                │     │
│  │  retention)   │  │                │  │                │     │
│  └────────┬───────┘  └────────┬───────┘  └────────┬───────┘     │
│           │                   │                   │             │
│           └────────┬──────────┴───────────────────┘             │
│                    ↓                                              │
│           ┌────────────────────┐                                  │
│           │  common.GlobalTask │                                  │
│           │     Manager        │  (5 秒检测 + 5 秒推屏 + 60 秒验狗)│
│           └─────────┬──────────┘                                  │
│                     │                                             │
│           ┌─────────┴──────────┐                                  │
│           │                    │                                  │
│  ┌────────▼─────┐    ┌─────────▼───────┐                          │
│  │  screen       │    │   yingke        │                          │
│  │ (大屏数据)    │    │ (鹰科集成)     │                          │
│  └────────┬──────┘    └─────────┬───────┘                          │
│           │                     │                                  │
│           └─────────┬───────────┘                                  │
│                     ↓                                              │
│           ┌──────────────────────┐                                 │
│           │  defect (产线-缺陷)   │                                 │
│           │  config (KV 配置)    │                                 │
│           └──────────────────────┘                                 │
│                     ↓                                              │
│           ┌──────────────────────┐                                 │
│           │  MySQL / PostgreSQL  │                                 │
│           └──────────────────────┘                                 │
└──────────────────────────────────────────────────────────────────┘
                                  ↕ HTTP
                       ┌──────────────────────┐
                       │  鹰科（外部）         │
                       │  （视觉检测平台）    │
                       └──────────────────────┘
```

---

## 2. 数据库全景

### 2.1 数据表清单（19 张）

| 模块 | 表 | 字段数 | retention | 用途 |
|---|---|---|---|---|
| detect | `defect_record` | 11 | ❌ 已删（EdgeHost）| 原始缺陷记录（含 imgList JSONArray）|
| detect | `defect_record_backup` | 11 | **3 天** | 原始缺陷备份 |
| detect | `defect_day_record` | 8 | 30 天 | 按小时每缺陷类型计数 |
| detect | `line_day_record` | 10 | 30 天 | 按小时每线良率/剔除 |
| detect | `status_record` | 11 | **无 retention** ⚠️ | 设备在线/离线状态 |
| detect | `workshop_day_record` | 默认 | 无 | 车间级日统计（空实现）|
| line | `line` | 11 | 无 | 产线基本信息 |
| line | `plan` | 6 | 无 | 方案（配方）|
| line | `plan_to_line` | 5 | 无 | 产线-方案绑定 |
| line | `line_order` | 4 | 无 | 产线排序 |
| line | `state_change` | 6 | 30 天 | 设备状态变更 |
| line | `state_statistic` | 7 | 30 天 | 班次状态统计 |
| alarm | `alarm_record` | 13 | 3 天（仅 SOLVED）| 报警记录 |
| alarm | `defect_type` | 12 | 无 | 缺陷类型配置（含报警开关）|
| alarm | `ignore_alarm` | 7 | 每日 1 点清理 | V1.14 临时忽略记录 |
| defect | `line_defect_type` | 5 | 无 | 产线-缺陷绑定 |
| config | `system_config` | 6 | 无 | 系统配置 KV |

### 2.2 retention 配置

```yaml
data-retention-time:
  detect: 3        # defect_record_backup 保留天数（金标准 V1.6）
  alarm: 3         # SOLVED 报警保留天数
  statistic: 30    # defect_day_record + line_day_record 保留天数
  line-state: 30   # state_change + state_statistic 保留天数
```

### 2.3 涨库风险表

| 表 | 风险 | 建议 |
|---|---|---|
| `status_record` | **高**（无 retention，每条设备状态变更/心跳更新都增加）| V1.19+ 加 retention cron（每日清理 >24h）|
| `state_change` | 中（白天无 retention，2 点清理 >30 天）| V1.19+ 加小时级清理 |
| `alarm_record` | 低（UNSOLVED/IGNORE 不清理，但有上限）| 监控 UNSOLVED 数量 |
| `plan_to_line` | 极低（人工维护）| - |

### 2.4 PG 专有语法（移植注意点）

| SQL | PG | MySQL 替代 |
|---|---|---|
| `INSERT ... ON CONFLICT(line_id, statistic_time) DO NOTHING` | ✅ | `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE` |
| `SELECT row_number() OVER (PARTITION BY ...)` | ✅ | `ROW_NUMBER() OVER (PARTITION BY ...)` 也支持 |
| `TO_CHAR(TIME::DATE, 'yyyy-MM-dd')` | ✅ | `DATE_FORMAT(time, '%Y-%m-%d')` |

---

## 3. cron 任务清单（9 个）

| 模块 | 类 | cron | 责任 |
|---|---|---|---|
| common | `GlobalTaskManager.checkClientStatus` | initialDelay=60s, fixedDelay=5s | 客户端掉线检测 + 写报警 |
| common | `GlobalTaskManager.sendScreen` | initialDelay=10s, fixedDelay=5s | 大屏数据推送 |
| common | `GlobalTaskManager.checkDogOnlineStatus` | initialDelay=5s, fixedDelay=60s | 加密狗验证（失败退出）|
| detect | `DetectDataTaskManager.clearDetectData` | `0 0 0 * * ?` | 清理 defect_record_backup（3 天）|
| detect | `DetectDataTaskManager.clearStatisticDetectData` | `0 0 0 * * ?` | 清理 defect_day_record + line_day_record（30 天）|
| alarm | `AlarmTaskManager.clearAlarmData` | `0 0 0 * * ?` | 清理 SOLVED alarm_record（3 天）|
| alarm | `AlarmTaskManager.delExpireIgnoreDefect` | `0 0 1 * * ?` | 清理过期 ignore_alarm |
| line | `LineTaskManager.getStatisticData` | `0 1 8,20 * * ?` | 班次状态统计（8/20 点）|
| line | `LineTaskManager.clearExpireStateData` | `0 0 2 * * ?` | 清理 state_change + state_statistic（30 天）|

**调度线程池**: 5 线程（`ScheduleConfig`）

---

## 4. 事件驱动清单

| 事件 | 触发方 | 监听方 | 类型 |
|---|---|---|---|
| `StateChangeEvent` | `StatusRecordServiceImpl` | `StateChangeServiceImpl.handleStateChange` | @Async |
| `DealAlarmEvent` | `StatusRecordServiceImpl` | `AlarmRecordServiceImpl.dealClientAlarmListener` | @Async |
| `PushAlarmEvent` | `AlarmRecordServiceImpl` | `YKServiceImpl.pushAlarm2YK` | @Async |
| `WsActionEvent` | WebSocket 框架 | `WsConnectListener.sendAlarmMessage` | 同步 |

**关键依赖**: 需 framework 包启用 `@EnableAsync`（反编译未找到，需确认）

---

## 5. WebSocket 消息类型（4 种）

| 类型 | 触发方 | 接收方 | 数据 |
|---|---|---|---|
| `SCREEN` | `ScreenServiceImpl.sendScreenDataInfo` | 所有 web 客户端 | ScreenDataDTO |
| `ALARM` | `AlarmRecordServiceImpl.sendAlarmTextMessage` | 所有 web 客户端 | List<AlarmRecordPO> |
| `ALARM_SOUND` | `AlarmRecordServiceImpl.sendAlarmSoundWsMessage` | 所有 web 客户端 | PlaySoundWsMsgDTO（URI + 播放次数）|
| `PLAN_CHANGE` | `CommonMethod.sendPlanChange` | 特定 client（clientNo）| "changePlan" |

**推送方式**:
- `broadcastByUid(wsData, "web")` — 广播所有 web 客户端
- `broadcastByUid(wsData, clientNo)` — 单播特定 client（clientNo = lineNo-faceNo）

---

## 6. 业务规则汇总

### 6.1 时区与时间格式

| 字段 | 时区/格式 |
|---|---|
| `DefectRecordPO.time` | `Asia/Shanghai` |
| `StatusRecordPO.time` | String（yyyy-MM-dd HH:mm:ss）|
| `DefectDayRecordPO.time` | String（yyyy-MM-dd）|
| `LineDayRecordPO.time` | String（yyyy-MM-dd HH:mm:ss）|
| `StateChangePO.changeTime` | LocalDateTime（无时区标注）|

### 6.2 班次切换

| 班次 | 时间 | 切换 cron |
|---|---|---|
| 白班 | 08:00 - 19:59 | - |
| 夜班 | 20:00 - 次日 07:59 | - |
| 班次统计 | 8:01 / 20:01 | `0 1 8,20 * * ?` |

**⚠️ 不一致点**:
- `ExportDefectStatisticForm.getEndTime()` 返回 `nextDay + " 07:00:00"`（白班 08:00 → 夜班 07:00 跨日）
- 内存计算白班 08:00-19:59 + 夜班 20:00-次日 07:59
- `state_change` 8/20 点切换

### 6.3 缺陷类型（7 类）

| DefectType 枚举 | 中文 | value |
|---|---|---|
| BOTTOM_BREAK | 底面破损 | 1 |
| SIDE_BREAK | 侧面破损 | 2 |
| SIDE_BREAK_BIG | 侧面破损Big | 3 |
| SIDE_BREAK_SMALL | 侧面破损Small | 4 |
| SIDE_DIRTY | 侧面脏污 | 5 |
| SECOND_MATERIAL | 二次料 | 6 |
| NOT_DEMOULDED | 未脱模 | 7 |

### 6.4 缺陷大类（DefectTypeEnum）

| DefectTypeEnum | value |
|---|---|
| BREAKAGE | 1 |
| DIRTY | 2 |
| OTHER | 3 |

### 6.5 报警类型

| AlarmTypeEnum | code | soundConfigKey |
|---|---|---|
| DEFECT | 1 | defect_alarm_sound_uri |
| SYSTEM | 2 | system_alarm_sound_uri |
| DEVICE | 3 | device_alarm_sound_uri |

### 6.6 报警状态

| AlarmSolvedEnum | value |
|---|---|
| SOLVED | 1 |
| UNSOLVED | 2 |
| IGNORE | 3 |

### 6.7 设备状态

| DeviceStatus | value |
|---|---|
| ONLINE | 1 |
| OUTLINE | 2 |

### 6.8 设备类型

| DeviceType | value |
|---|---|
| CAMERA | 1 |
| MACHINE | 2 |
| CLIENT | 3 |

### 6.9 方案状态

| PlanStatusEnum | value |
|---|---|
| ENABLE | 1 |
| DISABLE | 2 |

### 6.10 报警级别

| AlarmLevelEnum | value |
|---|---|
| NORMAL | 1 |
| HIGH | 2 |

### 6.11 报警原因

| AlarmReasonEnum | value |
|---|---|
| DISCONNECT | 1 |

### 6.12 国际语言

| Locale | 语言 |
|---|---|
| zh-CN | 简体中文（默认）|
| en-US | 英语 |
| id-ID | 印度尼西亚语 |

---

## 7. 关键技术决策矩阵

| 决策点 | PSM 选择 | 替代方案 | EdgeHost 建议 |
|---|---|---|---|
| ORM | MyBatis-Plus + XML Mapper | Hibernate/JPA | 沿用 MyBatis-Plus |
| 数据库 | MySQL/PG 兼容 | 单一选型 | EdgeHost 已用 PG |
| 加密狗 | JNA + SDK | 软件授权 | 不需要 |
| HTTP 客户端 | HikWebClient（同步阻塞）| RestTemplate/HttpClient | HttpClient + async |
| WebSocket | 框架组件 + WsMessage | Spring WebSocket | SignalR |
| 事件驱动 | Spring ApplicationEvent + @Async | 消息队列（Kafka）| Channel<T> + BackgroundService |
| 报警去重 | 同类 UNSOLVED → IGNORE | 状态机 | 沿用 |
| 班次切换 | 8/20 点 cron | 排班系统 | 沿用 |
| Retention | @Scheduled cron | MySQL 事件 | IHostedService + Cronos |
| JSON 字段 | JsonArrayTypeHandler | JSONB | EF Core value converter |
| 加密狗失败 | System.exit(1) | 降级模式 | 不需要 |

---

## 8. 风险全景矩阵

### 8.1 🔴 P0 风险（必须修复）

| 模块 | 风险 | 影响 |
|---|---|---|
| detect | `archive` SQL 是死代码 | retention 语义不明确 |
| line | `getStateStatistics` BUG（`: startOfCurrShift` 应为 `: endOfCurrShift`）| 当前班次统计可能丢失 |
| common | 加密狗失败 = 进程退出 | 临时掉线导致反复重启 |
| common | `System.exit(1)` 绕过 Spring 清理 | 数据库连接泄漏 |
| defect | `addDefectTypeIfNotExist` 清理逻辑 BUG | 客户端部分上报时误删其他 defect |
| alarm | `isIgnore` 判断永远是 true | ignore_alarm 实际未生效 |
| alarm | `IgnoreAlarmServiceImpl.handleAlarmIgnore` 未实现 | 前端调用无效 |

### 8.2 🟡 P1 风险（建议修复）

| 模块 | 风险 |
|---|---|
| detect | `status_record` 无 retention cron |
| detect | shift 计算不一致（form vs 内存 vs state_change）|
| detect | ExecutorService 声明未使用 |
| line | `@PostConstruct init()` 启动竞态 |
| line | `state_change` 涨库（白天无 retention）|
| line | `@Async` 默认线程池（SimpleAsyncTaskExecutor）|
| alarm | 日志文本错误（"90 days" 应为 "3 days"）|
| alarm | `@Lazy` 注入打破循环依赖 |
| alarm | WebSocket 推送频率无节流 |
| common | 调度线程池只有 5 个 |
| common | disconnect 报警可能频繁 |
| common | i18n filter 解析失败可能 NPE |
| common | `@EnableAsync` 未确认 |

### 8.3 🟢 P2 风险（记录但不紧急）

| 模块 | 风险 |
|---|---|
| detect | JSON 字段映射潜在类型问题 |
| detect | Hard delete 无软删除 |
| line | `planPanel()` N+1 |
| line | PG `ON CONFLICT` 专有 |
| alarm | `AlarmRecordPO` UUID 用 currentTimeMillis |
| alarm | `dealClientAlarm` 并发风险 |
| alarm | `row_number() OVER` PG 专有 |
| yingke | Ticket 续期失败不重试 |
| yingke | ThreadPoolTaskScheduler 未关闭 |
| yingke | HikWebClient 同步阻塞 |
| yingke | 推送失败仅 log |
| defect | `listByLine` 私有方法未使用 |
| defect | `DefectTypeEnum` 与 `DefectType` 命名相似 |
| config | configValue 类型校验缺失 |
| screen | defectSum 计算逻辑晦涩 |
| screen | 5 秒推送 5 表查询 DB 压力大 |
| screen | `LinePO.order` Integer 可能 NPE |
| screen | `JSONUtil.toBean` 失败可能中断推送 |
| common | `@MappedJdbcTypes(ARRAY)` 与 `setString` 冲突 |
| common | `AlarmRecordPO.buildClientAlarm` UUID 冲突 |
| common | `DongleUtils` 反编译数组创建问题 |

---

## 9. EdgeHost 移植全景

### 9.1 已对齐（V1.6 ~ V1.18）

| W-A | 模块 | 内容 |
|---|---|---|
| W-A6 | config | system_config 表 + CRUD |
| W-A9 | detect | defect_record 已删 + JSON TypeHandler |
| W-A12 | line | line 表 + LineRegistryService（注册表级别）|
| W-A17 | defect | line_defect_type 表 + 服务 |
| W-A18 | alarm | alarm_record + AlarmEventBus + cron |

### 9.2 🟡 待移植（V1.19+）

| 模块 | 任务 | 工作量 | 依赖 |
|---|---|---|---|
| detect | `DetectDataTaskManager` retention cron | 0.5d | defect_day_record + line_day_record schema |
| detect | `StatusRecordServiceImpl` 心跳检测 | 1d | 设备心跳协议 |
| detect | `LineDayRecordServiceImpl` 补全 | 0.5d | schema 已有 |
| line | `LineServiceImpl` CRUD + bindPlan + switchPlan | 1.5d | plan + plan_to_line |
| line | `StateChangeServiceImpl` + state_change 表 | 1d | detect.StatusRecord |
| line | `LineTaskManager` 8/20/2 三 cron | 0.5d | 状态序列算法 |
| line | 修复 `getStateStatistics` BUG | 0.1d | - |
| defect | 修复 `addDefectTypeIfNotExist` BUG | 0.1d | - |
| common | `checkClientStatus` 5 秒检测 + 60 秒阈值 | 1d | status_record + 心跳接口 |

### 9.3 🟢 可选（V1.20+）

| 模块 | 任务 | 工作量 |
|---|---|---|
| alarm | `ignore_alarm` 表 + 服务 | 0.5d |
| alarm | `DefectAlarmConfig` 正则模板 | 0.3d |
| alarm | 修复日志文本错误 | 0.05d |
| line | WebSocket `sendPlanChange` → SignalR | 0.5d |
| line | PlanController RESTful 重构 | 0.3d |
| common | WebSocket 4 种消息类型补全 | 0.3d |

### 9.4 ⚪ 不移植

| 模块 | 理由 |
|---|---|
| screen | 大屏由 PSM Web 处理，产线侧无大屏 |
| yingke | 产线侧不直连鹰科 |
| 加密狗 | 产线侧不需要 |
| i18n | 产线侧不需要多语言 |
| DongleUtils | 产线侧不需要 |

---

## 10. 文档索引

### 10.1 功能块详细解析（T2）

#### 🔴 P0
- **detect** — `2026-07-22-psm-detect-detailed.md`（15.6 KB）
  - 37 java + 4 XML，6 张表，3 个核心 cron
  - 金标准：`data-retention-time.detect: 3`
  - 关键类：DefectRecordServiceImpl / DefectRecordBackupServiceImpl / DetectDataTaskManager

#### 🟡 P1
- **line** — `2026-07-22-psm-line-detailed.md`（16.4 KB）
  - 54 java + 4 XML，6 张表，2 个核心 cron
  - 关键类：LineServiceImpl / PlanServiceImpl / StateChangeServiceImpl / LineTaskManager

- **alarm** — `2026-07-22-psm-alarm-detailed.md`（14.7 KB）
  - 35 java + 1 XML，3 张表，2 个核心 cron
  - 关键类：AlarmRecordServiceImpl / AlarmTaskManager

- **yingke** — `2026-07-22-psm-yingke-detailed.md`（11.3 KB）
  - 15 java，0 张表（外部 HTTP 集成）
  - 关键类：YKServiceImpl（推送 + Ticket 续期）

- **common** — `2026-07-22-psm-common-detailed.md`（12.7 KB）
  - 11 java，0 张表（横切关注点）
  - 关键类：GlobalTaskManager（3 cron）

#### 🟢 P2
- **defect** — `2026-07-22-psm-defect-detailed.md`（7.7 KB）
  - 6 java + 1 XML，1 张表
  - 关键类：LineDefectTypeServiceImpl

#### ⚪ P3
- **config** — `2026-07-22-psm-config-detailed.md`（3.3 KB）
  - 5 java，1 张表
  - 关键类：SystemConfigServiceImpl

- **screen** — `2026-07-22-psm-screen-detailed.md`（8.3 KB）
  - 5 java，0 张表（聚合）
  - 关键类：ScreenServiceImpl

### 10.2 技术路线解析（T3）

| 模块 | 技术路线 | 大小 |
|---|---|---|
| detect | `2026-07-22-psm-detect-tech.md` | 9.8 KB |
| line | `2026-07-22-psm-line-tech.md` | 12.7 KB |
| alarm | `2026-07-22-psm-alarm-tech.md` | 11.9 KB |
| yingke | `2026-07-22-psm-yingke-tech.md` | 7.0 KB |
| defect | `2026-07-22-psm-defect-tech.md` | 5.8 KB |
| config | `2026-07-22-psm-config-tech.md` | 2.6 KB |
| screen | `2026-07-22-psm-screen-tech.md` | 6.4 KB |
| common | `2026-07-22-psm-common-tech.md` | 13.2 KB |

---

## 11. 关键名词表

| 缩写 | 全称 | 说明 |
|---|---|---|
| PSM | Production Solution Manager | 海康生产管理平台 |
| YK | 鹰科 | 外部视觉检测平台 |
| defect_record | 原始缺陷记录 | 已删（EdgeHost）|
| defect_record_backup | 原始缺陷备份 | 3 天 retention |
| defect_day_record | 按小时缺陷统计 | 30 天 retention |
| line_day_record | 按小时产线统计 | 30 天 retention |
| status_record | 设备状态 | 无 retention ⚠️ |
| workshop_day_record | 车间日统计 | 空实现 |
| realtime_data | 实时检测数据 | line.realtime_data JSON |
| except_flag | 排除标志 | defect 表字段 |
| gloveNo | 手套号 | 检测产品 ID |
| removeTotal | 剔除总数 | 累计值 |
| uploadRemoveTotal | 上传剔除总数 | 上一小时值 |
| Eight / TWENTY | 08:00 / 20:00 | 白班/夜班切换点 |
| lineNo + ":" + faceNo | 产线位置 | 唯一标识 |
| clientNo = lineNo + "-" + faceNo | 客户端编号 | line 模块生成 |
| state_change | 设备状态变更记录 | 30 天 retention |
| state_statistic | 班次状态统计 | 30 天 retention |
| plan | 方案（配方）| id/name/uri/description |
| plan_to_line | 产线-方案绑定 | id/lineId/planId/status |
| line_order | 产线排序 | id/lineId/orderValue |
| alarm_record | 报警记录 | id/uuid/time/type/... |
| defect_type | 缺陷类型配置 | id/name/category/... |
| ignore_alarm | V1.14 临时忽略 | id/defectName/type/... |
| system_config | KV 配置 | id/configName/configKey/configValue |
| line_defect_type | 产线-缺陷绑定 | id/name/showFlag/lineNo/faceNo |
| HikWebClient | 框架 HTTP 客户端 | 同步阻塞 |
| JsonArrayTypeHandler | MyBatis TypeHandler | JSONArray ↔ String |
| WsTypeEnum | WebSocket 消息类型 | SCREEN/ALARM/ALARM_SOUND/PLAN_CHANGE |
| StateEnum | 状态枚举 | YES(1)/NO(0) |

---

## 12. 后续工作计划

### 12.1 W-A22+ 必做（基于 PSM 解析结果）

1. **修复 PSM BUG**（已发现的 P0 BUG）
   - `LineServiceImpl.getStateStatistics` 三元表达式
   - `LineDefectTypeServiceImpl.addDefectTypeIfNotExist` 清理逻辑
   - `AlarmRecordServiceImpl.sendAlarmMessage` `isIgnore` 永远 true
   - `IgnoreAlarmServiceImpl.handleAlarmIgnore` 未实现

2. **移植 retention 范式**
   - `DetectDataTaskManager`（0.5d）
   - `LineTaskManager`（0.5d）
   - `AlarmTaskManager`（已对齐 V1.18）

3. **移植状态机**
   - `StatusRecordServiceImpl` + 心跳接口（1d）
   - `StateChangeServiceImpl` + state_change 表（1d）
   - `StateChangeServiceImpl` 算法（0.5d）

4. **移植产线管理**
   - `LineServiceImpl` CRUD + bindPlan + switchPlan（1.5d）

### 12.2 W-A23+ 可选

1. **ignore_alarm 完整复刻**（修复 BUG 后 0.5d）
2. **WebSocket → SignalR 完整迁移**（1d）
3. **alarm 正则模板**（0.3d）
4. **监控告警（UNSOLVED 超阈值）**（0.5d）

### 12.3 W-A24+ 优化

1. **detect.shift 计算统一**（跨日 / 跨班次）
2. **screen 重构 defectSum 计算**（可读性）
3. **alarm.record 软删除**（审计）
4. **status_record 加 retention cron**
5. **统一时间字段类型**（TIMESTAMP WITH TIME ZONE）

---

## 13. 总结

本次 W-A21 任务完整解析了 PSM 反编译产物：

**工作量**:
- 8 个模块 × 2 类文档（功能 + 技术）= 16 个详细文档
- 1 个整合文档（本文件）
- 总计 ~200 KB

**关键发现**:
- 🔴 **3 个 P0 BUG**：line 算法、defect 清理逻辑、alarm isIgnore
- 🟡 **9 个 cron** 统一管理 retention
- 🟢 **6 类数据库涨库风险**（status_record 是最大的）
- 📊 **19 张表**全景梳理

**移植优先级**:
- V1.19 必移植：detect retention cron + status_record 心跳 + line CRUD + state 算法
- V1.20+ 可选：ignore_alarm + WebSocket → SignalR
- 不移植：screen + yingke + 加密狗

**PM 校对建议**:
- 重点核对 P0 BUG（line / defect / alarm）
- 重点核对 retention 配置（detect: 3 / alarm: 3 / line-state: 30 / statistic: 30）
- 重点核对 PSM V1.6 vs V1.19 schema 差异
- 重点核对反编译死代码（archive SQL、isIgnore、handleAlarmIgnore）

---

**文档生成时间**: 2026-07-22  
**Worker**: W-A21 Subagent  
**状态**: ✅ 完成，待 PM 校对
