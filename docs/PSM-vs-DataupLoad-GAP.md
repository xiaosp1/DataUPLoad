# PSM 全功能 vs DataupLoad 现状对比 + 开发计划

**生成时间**: 2026-07-24 13:30
**基线**: `docs/psm-reference/W-A21-psm-reverse-engineering-full.md` + `2026-07-22-psm-architecture.md`
**对比方法**: PSM 8 大模块 185 类 × 逐项检查 DataupLoad 源码

---

## 1. 总览

| PSM 模块 | 类数 | DataupLoad 对齐度 | 状态 |
|---|---|---|---|
| **alarm** 报警 | 24 | ████████░░ 90% | 🟡 缺 DefectTypeController + IgnoreAlarmController(partial) |
| **detect** 检测 | 37+4XML | ██████░░░░ 60% | 🔴 缺 Backup + TaskManager + 多表 |
| **line** 产线 | 51+4XML | ██░░░░░░░░ 20% | 🔴 只搬了 LineController shell |
| **yingke** 英科 | 15 | █████████░ 95% | 🟢 YKServiceImpl + PushAlarmEvent 已对齐 |
| **defect** 缺陷 | 4+1XML | █████████░ 90% | 🟢 LineDefectType 表+Service 已有 |
| **config** 配置 | 5 | ░░░░░░░░░░ 0% | 🔴 整个模块没搬 |
| **screen** 大屏 | 5 | ░░░░░░░░░░ 0% | 🔴 整个模块没搬 |
| **common** 公共 | 11 | ████░░░░░░ 40% | 🔴 缺 GlobalTaskManager(3 个 cron) + ScheduleConfig |
| **Web 前端** | 94 文件 | ██████████ 100% | ✅ W-A19 已复制 PSM Vue 3 SPA |

---

## 2. 逐模块详细对比

### 2.1 alarm 报警模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| AlarmRecordController | ✅ 有 | 1:1（W-X30 已修双推+去重） |
| DefectTypeController | ❌ 没搬 | 🔴 缺 CRUD 接口 |
| IgnoreAlarmController | ⚠️ 部分 | 🟡 Controller 有但没对照 PSM 验证 |
| DealAlarmEvent | ✅ 有 | W-X30b 已补 |
| PushAlarmEvent | ✅ 有 | 1:1 |
| AlarmTaskManager（cron 清理 90 天 SOLVED） | ❌ 没搬 | 🔴 缺定时清理 |
| DefectAlarmConfig | ✅ 有 | 1:1 |
| alarm_record / defect_type / ignore_alarm 表 | ✅ 有 | 1:1 schema |

### 2.2 detect 检测模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| DetectDataController | ✅ 有 | 1:1 |
| DefectRecordServiceImpl | ✅ 有 | 1:1 |
| DefectDayRecordServiceImpl | ✅ 有 | 1:1 |
| LineDayRecordServiceImpl | ✅ 有 | 1:1 |
| StatusRecordServiceImpl | ✅ 有 | W-X30b 已补 DealAlarmEvent |
| **DefectRecordBackupServiceImpl** | ❌ **没搬** | 🔴 PSM V1.6 retention 金标准 |
| **DetectDataTaskManager**（2 个 cron） | ❌ **没搬** | 🔴 清理 defect_record_backup + defect_day_record |
| defect_record_backup 表 | ❌ **没建** | 🔴 依赖 V1.6 SQL |
| workshop_day_record 表 | ❌ | ⚪ P3 空实现 |

### 2.3 line 产线模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| LineController | ⚠️ 只有 shell | 🟡 接口签名有，业务逻辑空 |
| LineServiceImpl | ⚠️ 部分 | 🟡 只做了注册表级 CRUD |
| **PlanController** | ❌ **没搬** | 🔴 方案管理 |
| **PlanServiceImpl** | ❌ **没搬** | 🔴 plan 表有，业务没做 |
| **PlanToLineService** | ❌ **没搬** | 🔴 plan_to_line 表绑定 |
| **StateChangeController** | ❌ **没搬** | 🔴 状态变更记录 |
| **StateChangeServiceImpl** | ❌ **没搬** | 🔴 state_change 表 + V1.19 |
| **StateStatisticController** | ❌ **没搬** | 🔴 班次统计 |
| **StateStatisticServiceImpl** | ❌ **没搬** | 🔴 state_statistic 表 + V1.19 |
| **LineTaskManager**（2 个 cron） | ❌ **没搬** | 🔴 班次统计 + 清理过期状态 |
| **LineOrderService** | ❌ | 🔴 line_order 产线排序 |
| line / plan / plan_to_line / line_order 表 | ⚠️ 部分建表 | 🟡 plan 等表有 DDL 但无业务 |

### 2.4 yingke 英科推送模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| YKServiceImpl | ✅ 有 | 1:1（含内存去重） |
| PushAlarmEvent | ✅ 有 | 1:1 |
| YKConfig | ✅ 有 | 1:1（loginEnabled + uploadEnabled） |
| YKController（/client/yk/defect-record 等） | ✅ 有 | 1:1 |
| HikWebClient | ✅ 有 | 1:1 |

### 2.5 defect 缺陷绑定模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| ILineDefectTypeService | ✅ 有 | 1:1 |
| LineDefectTypeMapper | ✅ 有 | 1:1 |
| line_defect_type 表 | ✅ 有 | 1:1 |

### 2.6 config 系统配置模块（🔴 整块缺失）

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| SystemConfigController | ❌ | 🔴 系统配置 CRUD |
| ISystemConfigService | ❌ | 🔴 配置存取 |
| SystemConfigPO | ❌ | 🔴 system_config 表 |
| system_config 表 | ❌ | 🔴 音频/播报次数配置 |

### 2.7 screen 大屏模块（🔴 整块缺失）

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| ScreenServiceImpl | ❌ | 🔴 大屏 5 表数据聚合 |
| IScreenService | ❌ | 🔴 聚合接口 |
| ClientStatusDTO / ScreenDataDTO 等 | ❌ | 🔴 DTO 缺失 |
| WebSocket SCREEN 推送 | ❌ | 🔴 大屏实时推送 |

### 2.8 common 公共模块

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| **GlobalTaskManager** | ❌ **没搬** | 🔴 3 个核心 cron |
| ├ checkClientStatus (5s) | ❌ | 🔴 客户端掉线检测 + 写报警 |
| ├ sendScreen (5s) | ❌ | 🔴 大屏数据推送 |
| └ checkDogOnlineStatus (60s) | ❌ | ⚪ skip（ADR-0005 去加密狗） |
| ScheduleConfig | ❌ | 🔴 调度线程池配置 |
| I18nConfig | ⚠️ | 🟡 PSM 有，DataupLoad 未确认 |
| CommonMethod / Variable | ⚠️ | 🟡 工具类未完全对照 |

### 2.9 WebSocket 推送

| PSM 功能 | DataupLoad | 状态 |
|---|---|---|
| SCREEN 大屏数据推送 | ❌ | 🔴 依赖 ScreenServiceImpl |
| ALARM 报警文本推送 | ✅ | 1:1（AlarmWebSocketHandler） |
| ALARM_SOUND 报警音效 | ⚠️ | 🟡 未验证 |
| PLAN_CHANGE 方案变更推送 | ❌ | 🔴 依赖 PlanServiceImpl |
| WsConnectListener | ❌ | 🔴 WebSocket 连接时推报警 |

---

## 3. 缺失 DB 表清单

| 表 | PSM 版本 | 用途 | 优先级 |
|---|---|---|---|
| `defect_record_backup` | V1.6 | defect_record 备份(3天) | 🔴 P0 |
| `plan` | V1.0 | 方案(配方) | 🔴 P0 |
| `plan_to_line` | V1.0 | 产线-方案绑定 | 🔴 P0 |
| `line_order` | V1.0 | 产线排序 | 🟡 P1 |
| `state_change` | V1.19 | 设备状态变更(30天) | 🔴 P0 |
| `state_statistic` | V1.19 | 班次状态统计(30天) | 🔴 P0 |
| `workshop_day_record` | V1.0 | 车间日统计 | ⚪ P3 |
| `system_config` | V1.0 | 系统配置 KV | 🟡 P1 |

---

## 4. 缺失 Cron 任务清单

| 任务 | cron | 职责 | 优先级 |
|---|---|---|---|
| checkClientStatus | 每 5s | 客户端掉线检测 + 写报警 | 🔴 P0 |
| sendScreen | 每 5s | 大屏数据广播 | 🟡 P1 |
| clearDetectData | 每天 0:00 | 清理 defect_record_backup(3天) | 🔴 P0 |
| clearStatisticDetectData | 每天 0:00 | 清理 defect_day_record + line_day_record(30天) | 🔴 P0 |
| clearAlarmData | 每天 0:00 | 清理 SOLVED alarm_record(3天) | 🟡 P1 |
| delExpireIgnoreDefect | 每天 1:00 | 清理过期 ignore_alarm | 🟡 P1 |
| getStatisticData | 8:01,20:01 | 班次统计 | 🔴 P0 |
| clearExpireStateData | 每天 2:00 | 清理 state_change + state_statistic(30天) | 🔴 P0 |

---

## 5. 🔴 必须补齐的清单（P0 关键路径）

按依赖关系排序：

| # | 工单 | 模块 | 工作量 | 依赖 | 说明 |
|---|---|---|---|---|---|
| **P0-1** | 建缺失 DB 表 | detect+line | 0.5h | - | defect_record_backup + plan + plan_to_line + line_order + state_change + state_statistic |
| **P0-2** | GlobalTaskManager | common | 2h | P0-1 | 客户端掉线检测(5s) + 大屏推送(5s) + ScheduleConfig |
| **P0-3** | DefectRecordBackupService | detect | 1h | P0-1 | V1.6 备份 + retention |
| **P0-4** | DetectDataTaskManager | detect | 1h | P0-3 | 2 个 cron（清理 backup + 清理 statistics） |
| **P0-5** | AlarmTaskManager | alarm | 1h | - | 2 个 cron（清理 SOLVED + 清理 ignore_alarm） |
| **P0-6** | StateChange + StateStatistic | line | 2h | P0-1 | V1.19 状态变更 + 班次统计 |
| **P0-7** | LineTaskManager | line | 1h | P0-6 | 2 个 cron（班次统计 + 清理） |

## 6. 🟡 应该补齐的清单（P1 功能完整）

| # | 工单 | 模块 | 工作量 | 依赖 |
|---|---|---|---|---|
| **P1-1** | DefectTypeController + 补框架类 | alarm | 2h | BaseResult+IdQuery+DefectTypeDTO |
| **P1-2** | Plan + PlanToLine + LineOrder | line | 2h | P0-1 |
| **P1-3** | SystemConfig 模块全栈 | config | 1.5h | -
| **P1-4** | ScreenServiceImpl | screen | 2h | P0-2 |
| **P1-5** | WsConnectListener + PLAN_CHANGE 推送 | alarm | 1h | P1-2 |

## 7. ⚪ 可选补齐（P2-P3 锦上添花）

| # | 工单 | 工作量 |
|---|---|---|
| **P2-1** | WorkshopDayRecordServiceImpl | 0.5h |
| **P2-2** | AlarmSound 音效推送验证 | 1h |
| **P2-3** | I18nConfig 国际化 | 1h |

---

## 8. 执行顺序建议

```
第一步（今天）: P0-1 建表 → 15 分钟，为后续所有工单铺路

第二步（今天）: P0-2 GlobalTaskManager → 核心基础设施
                P0-3+P0-4 Backup+清理 cron → retention 防爆库
                两项可并行

第三步:        P0-5 AlarmTaskManager + P0-6 StateChange/Statistic + P0-7 LineTaskManager
                三项可并行

第四步:        P1-1 DefectTypeController（Web 后台缺陷管理）→ 老板刚问的
                P1-2 Plan 方案管理
                P1-3 SystemConfig 系统配置
                三项可并行
                
第五步:        P1-4 Screen + P1-5 WsConnectListener → 大屏功能
```

**总计工作量**: P0 约 8h + P1 约 8.5h = **约 16.5 小时**
