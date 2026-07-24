# 2026-07-22 PSM 架构解析（按功能块分类）

**触发**: 老板 08:32 "大工程" 第一步 "先重新解析 PSM 的架构 再将其架构按功能块分类"
**金标准**: `10-反编译产物-NEW/PSM/server/IntcoScreen-1.0-SNAPSHOT-20260605135937.jar` (80.64 MB Spring Boot fat jar)
**PM 自检**: 08:34
**状态**: 🟡 进行中（架构骨架已落，功能块解析进行中）

---

## 1. PSM 顶层架构

```
PSM (IntcoScreen Spring Boot 应用)
├── Application                    # Spring Boot 启动入口 (com.hikrobotics.solution.Application)
├── common/                        # 通用基础设施
│   ├── config/                    # 配置类 (I18nConfig / ScheduleConfig)
│   ├── constants/                 # 全局常量 + 枚举 (CommonMethod/Variable/StateEnum/WsTypeEnum)
│   ├── handler/                   # TypeHandler (JsonArrayTypeHandler - MyBatis JSON 字段映射)
│   ├── task/                      # 全局任务调度 (GlobalTaskManager)
│   └── utils/                     # 工具 (DongleUtils 软件加密狗 / EnumUtil / MathUtils)
└── module/                        # 7 大业务模块
    ├── alarm                      # 报警
    ├── config                     # 系统配置
    ├── defect                     # 缺陷类型
    ├── detect                     # 检测 (含 defect_record/line_day_record/status_record)
    ├── line                       # 产线 + 方案
    ├── screen                     # 大屏数据聚合
    └── yingke                     # 鹰科推送
```

**统计**: 185 个 class，按业务模块分 8 大块（common + 7 module），每个 module 标准布局：
- `constant/` 业务枚举/常量
- `dto/` 数据传输对象（Web ↔ Service ↔ Mapper 之间）
- `event/` 内部事件 (ApplicationEvent)
- `mapper/` MyBatis DAO 接口 (XxxDAO)
- `model/` PO 持久化对象 (XxxPO，对应 DB 表)
- `service/` 接口 (IXxxService)
- `service/imp/` 或 `service/impl/` 实现 (XxxServiceImpl)
- `task/` 后台调度 (XxxTaskManager)
- `web/` Controller (XxxController)

---

## 2. 7 大业务模块详解

### 2.1 🔔 alarm (报警模块) — 24 个 class

**核心职责**: 报警接收、解析、推送 YK、定时清理

| 层 | 类 | 职责 |
|---|---|---|
| enum | AlarmTypeEnum / AlarmLevelEnum / AlarmReasonEnum / AlarmSolvedEnum | 报警 4 大枚举 |
| config | DefectAlarmConfig (含 DefectTypeConfig 内部类) | 报警模板配置 (regex 解析 message)|
| dto | AlarmDTO / AlarmQueryDTO / AlarmInfoQueryDTO / AlarmDealDTO / AlarmNumDTO (+Builder) / AlarmCountDTO / AlarmCountOfLineDTO / DefectTypeDTO / IgnoreAlarmDTO / PlaySoundWsMsgDTO / SearchAlarmDTO / SearchDefectDTO | 12+1=13 个 DTO |
| event | DealAlarmEvent / WsConnectListener | WebSocket 连接事件 + 报警处理事件 |
| mapper | AlarmRecordDAO / DefectTypeDAO / IgnoreAlarmDAO | 3 个 DAO |
| model | AlarmRecordPO / DefectTypePO / IgnoreAlarmPO | 3 个 PO 对应 alarm_record/defect_type/ignore_alarm |
| service | IAlarmRecordService / IDefectTypeService / IIgnoreAlarmService | 3 个接口 |
| service/imp | AlarmRecordServiceImpl / DefectTypeServiceImpl / IgnoreAlarmServiceImpl | 3 个实现 |
| task | AlarmTaskManager | **🚨 报警定时任务**（cron 0 0 0 * * ? 清理 90 天 SOLVED）|
| web | AlarmRecordController / DefectTypeController | 2 个 Controller |

**EdgeHost 对齐状态**: W-A18 已 1:1 移植 AlarmRecordServiceImpl + AlarmEventBus + YingkeServiceImpl
**缺口**: IIgnoreAlarmService (V1.14 ignore_alarm 表)、DefectAlarmConfig（已部分实现）

### 2.2 ⚙️ config (系统配置) — 5 个 class

**核心职责**: 系统配置 (音频/播报次数) 存取

| 层 | 类 | 职责 |
|---|---|---|
| mapper | SystemConfigDAO | DB 存取 |
| model | SystemConfigPO | 对应 system_config 表 |
| service | ISystemConfigService / SystemConfigServiceImpl | 业务接口 + 实现 |
| web | SystemConfigController | REST API |

**EdgeHost 对齐状态**: ❌ **没做**（用 appsettings.json 代替）
**优先级**: 🟢 P2-3（我们用 .NET IConfiguration 已经够用）

### 2.3 🏷️ defect (缺陷类型子模块) — 4 个 class

**核心职责**: 缺陷类型与产线绑定

| 层 | 类 | 职责 |
|---|---|---|
| constant | DefectTypeEnum | 缺陷大类枚举 |
| dto | ChangeLineDefectResult | 切换产线缺陷结果 DTO |
| mapper | LineDefectTypeDAO | DB 存取 |
| model | LineDefectTypePO | 对应 line_defect_type 表 |
| service | ILineDefectTypeService / LineDefectTypeServiceImpl | 业务逻辑 |

**EdgeHost 对齐状态**: ✅ 表已有 line_defect_type，业务在 detect 模块实现

### 2.4 🔍 detect (检测核心模块) — 47 个 class — **🚨 最大模块**

**核心职责**: 接收检测数据、写入 defect_record、统计 defect_day_record/line_day_record、维护 status_record、retention (defect_record_backup)、Excel 导出

| 层 | 类 | 职责 |
|---|---|---|
| dto | DefectCountPerHourDTO (+Builder) / DefectStatisticDataDTO / DeviceStateDTO / ExportDefectStatisticForm | 5 个 DTO（含每小时缺陷统计、设备状态、Excel 导出表单）|
| enums | DefectResult / DefectType / DeviceStatus / DeviceType | 4 个核心枚举 |
| excel | DataMergeStrategy (含 DataMergeStrategy$1 内部类) | EasyExcel 合并策略 |
| mapper | **DefectRecordDAO** / **DefectRecordBackupDAO** / **DefectDayRecordDAO** / LineDayRecordDAO / StatusRecordDAO / WorkshopDayRecordDAO | **6 个 DAO（关键）** |
| model | DefectRecordPO / **DefectRecordBackupPO** / DefectDayRecordPO / LineDayRecordPO / StatusRecordPO / WorkshopDayRecordPO | 6 个 PO 对应 6 张表 |
| service | IDefectRecordService / **IDefectRecordBackupService** / IDefectDayRecordService / ILineDayRecordService / IStatusRecordService / IWorkshopDayRecordService | **6 个接口（关键）** |
| service/imp | DefectRecordServiceImpl / **DefectRecordBackupServiceImpl** / DefectDayRecordServiceImpl / LineDayRecordServiceImpl / StatusRecordServiceImpl / WorkshopDayRecordServiceImpl | 6 个实现 |
| task | **DetectDataTaskManager** | **🚨 检测数据定时任务**（推测 defect_record → backup → truncate）|
| util | ExcelUtils (+3 内部类 + SheetConfig + Table) / TimeRange (+2 内部类 + TimePattern) | 9 个工具类 |
| web | DetectDataController | 接收 `/client/data/detect` 的 Controller |

**EdgeHost 对齐状态**:
- ✅ W-A9 已删 defect_record 表 + 对齐 DefectDayRecordServiceImpl
- ❌ **DefectRecordBackupServiceImpl 没移植**（V1.6 retention 金标准）
- ❌ **DetectDataTaskManager 没移植**（推测 cron）
- 🟡 DetectDataController 部分实现 (我们的 DetectController 接收 /client/data/detect)

### 2.5 🏭 line (产线 + 方案模块) — 51 个 class — **🚨 第二大模块**

**核心职责**: 产线 CRUD、方案管理、状态变更、状态统计

| 层 | 类 | 职责 |
|---|---|---|
| constant | PlanStatusEnum | 方案状态枚举 |
| dto | ChgLineOrderDTO / ClientPlanQueryDTO / ClientPlanResultDTO / DefectCountDisPlayDTO / DefectCountDTO / DefectQueryDTO / DetectDataUploadDTO / LineBodyDTO / LineCountDTO / LineDTO / LinePanelDTO / LinePanelQueryDTO / LinePlanBindDTO / LinePlanBindQueryDTO / LinePlanSwitchDTO / LineTreeItemDTO / LineUpdateDTO / PlanDTO / PlanQueryDTO / RealTimeDetectData / SearchStateStatisticForm / ToDayCountDTO / TodayDetectDataDTO / WebLineBindPlanResultDTO | **24 个 DTO** |
| event | StateChangeEvent | 产线状态变更事件 |
| mapper | LineDAO / LineOrderDAO / PlanDAO / PlanToLineDAO / StateChangeDAO / StateStatisticDAO | 6 个 DAO |
| model | LineOrderPO (+Builder) / **LinePO** / PlanPO / PlanToLinePO / StateChangePO / StateStatisticPO | 7 个 PO |
| service | ILineOrderService / **ILineService** / IPlanService / IStateChangeService / IStateStatisticService | 5 个接口 |
| service/imp | LineOrderServiceImpl / **LineServiceImpl** / PlanServiceImpl / PlanToLineService / StateChangeServiceImpl / StateStatisticServiceImpl | 6 个实现 |
| task | LineTaskManager | 产线定时任务（推测 state_statistic 累加）|
| web | LineController / PlanController / StateChangeController / StateStatisticController | 4 个 Controller |

**EdgeHost 对齐状态**:
- ✅ W-A12 已对齐 line 表 + LineRepository + LineRegistryService
- ❌ **ILineService / LineServiceImpl 没全移植**（W-A12 只做了注册表级别）
- ❌ **StateChangeServiceImpl / StateStatisticServiceImpl 没移植**（V1.19 新表）
- ❌ **PlanServiceImpl / LineOrderServiceImpl / PlanToLineService 没移植**（plan/line_order 表已有，业务没做）

### 2.6 🖥️ screen (大屏模块) — 7 个 class

**核心职责**: 主大屏数据聚合 (看板)

| 层 | 类 | 职责 |
|---|---|---|
| dto | ClientStatusDTO / DefectNumberDTO / ScreenDataDTO (+DetectDataDTO 内部类) | 4 个 DTO |
| service | IScreenService / ScreenServiceImpl | 聚合多个模块数据 |

**EdgeHost 对齐状态**: 🟡 W-A19 的 WebApiController 部分对应 (`/web/line/state/statistic`)

### 2.7 📡 yingke (鹰科推送模块) — 18 个 class

**核心职责**: 跟英科 YK 系统对接（报警推送 / 缺陷查询 / 工单登录）

| 层 | 类 | 职责 |
|---|---|---|
| config | YKConfig | 鹰科连接配置（workshop / username / password / url）|
| dto | AlarmDTO / ContextDTO / DetectDataDTO (+ DefectDataDTO / RemoveCountDTO) / LineAndDefectDTO / ListParamsDTO / LoginResultDTO / SearchDefectRecordDTO / StringParamDTO / YKRequestDTO / YKResponseDTO | **12 个 DTO** |
| event | **PushAlarmEvent** | **🚨 推送报警事件** |
| service | IYKService / **YKServiceImpl** | 鹰科业务接口 + 实现 |
| web | YKController | `/client/yk/...` 系列端点 |

**EdgeHost 对齐状态**: ✅ W-A17 + W-A18 已对齐 YK 推送

---

## 3. 关键金标准类清单（PM 给反编译 Worker 的优先级表）

| 优先级 | 类 | 业务影响 | 反编译量 |
|---|---|---|---|
| **P0 🔴** | `DefectRecordServiceImpl` | 涨库根因（defect_record 主写入）| 中 |
| **P0 🔴** | `DefectRecordBackupServiceImpl` | retention 逻辑（V1.6 金标准）| 小 |
| **P0 🔴** | `DetectDataTaskManager` | 推测 defect_record → backup cron | 小 |
| **P0 🔴** | `AlarmTaskManager` | alarm_record 90 天 cron（W-A18 已参考）| 小 |
| P1 🟡 | `LineServiceImpl` | line 表全业务（W-A12 只做了注册）| 大 |
| P1 🟡 | `DefectDayRecordServiceImpl` | defect_day_record 累加 | 中 |
| P1 🟡 | `LineDayRecordServiceImpl` | line_day_record 累加 | 中 |
| P2 🟢 | `DefectAlarmConfig` | 报警正则模板（W-A18 已参考）| 小 |
| P2 🟢 | `YKServiceImpl` | 鹰科推送（W-A17/W-A18 已参考）| 中 |
| P2 🟢 | `StatusRecordServiceImpl` | status_record 业务 | 中 |
| P2 🟢 | `PlanServiceImpl` / `PlanToLineService` | plan 业务 | 中 |
| P2 🟢 | `StateChangeServiceImpl` / `StateStatisticServiceImpl` | V1.19 新表业务 | 小 |
| P3 ⚪ | `IgnoreAlarmServiceImpl` | ignore_alarm 业务（V1.14）| 小 |
| P3 ⚪ | `ScreenServiceImpl` | 大屏聚合 | 大 |

---

## 4. 反编译策略（PM 给 Worker 的输入）

### 4.1 工具链

```
工具: vineflower (推荐) 或 cfr (备选)
下载: 
  - vineflower: https://github.com/Vineflower/vineflower/releases (vineflower-4.5.1.jar)
  - cfr: https://github.com/leibnitz27/cfr/releases (cfr-0.152.jar)
JDK: PSM 自带 jdk 在 10-反编译产物-NEW/PSM/server/jdk/ (java -version 检查)
```

### 4.2 反编译命令模板

```bash
# 1. vineflower（推荐，质量好）
java -jar vineflower-4.5.1.jar IntcoScreen-1.0-SNAPSHOT-20260605135937.jar ./psm-decompiled/

# 2. cfr（备选，兼容性好）
java -jar cfr-0.152.jar IntcoScreen-1.0-SNAPSHOT-20260605135937.jar --outputdir ./psm-decompiled/
```

### 4.3 输出目录规划

```
psm-decompiled/
├── common/
│   ├── config/         # I18nConfig / ScheduleConfig
│   ├── constants/      # CommonMethod/Variable/StateEnum/WsTypeEnum
│   ├── handler/        # JsonArrayTypeHandler
│   ├── task/           # GlobalTaskManager
│   └── utils/          # DongleUtils / EnumUtil / MathUtils
├── alarm/              # 24 个 .java
├── config/             # 5 个 .java
├── defect/             # 4 个 .java
├── detect/             # 47 个 .java
├── line/               # 51 个 .java
├── screen/             # 7 个 .java
└── yingke/             # 18 个 .java
```

---

## 5. 下一步

| 步骤 | 派工 | 时长 |
|---|---|---|
| 1️⃣ | **PM 已完成**：解析架构 + 按功能块分类（本归档）| 30 分钟 |
| 2️⃣ | **派 Worker**：下载 vineflower + 反编译 jar 到 psm-decompiled/ | 15 分钟 |
| 3️⃣ | **PM 主导**：逐个功能块解析实现逻辑（先 P0 四件）| 2-4 小时 |
| 4️⃣ | **每解析完一个 P0 类立即归档**：`docs/delivered/2026-07-22-psm-decompiled-P0N-xxx.md` | 每 30 分钟归档 |
| 5️⃣ | **最终归档**：`docs/delivered/2026-07-22-psm-reverse-engineering-full.md` 全量 | 1 小时 |

---

## 6. 一句话总结

> **PSM 是个标准的 Spring Boot 多模块应用，185 个 class 分布在 common + alarm/config/defect/detect/line/screen/yingke 7 个业务模块，每个模块按 constant/dto/event/mapper/model/service/task/web 标准分层；detect（47）+ line（51）+ alarm（24）+ yingke（18）是核心四大模块；P0 反编译目标是 DefectRecordServiceImpl + DefectRecordBackupServiceImpl + DetectDataTaskManager + AlarmTaskManager，对应涨库根因 + retention 金标准 + cron 调度。**
