# 02. PSM 模块架构

## 🏗️ 顶层结构（Spring Boot 3 应用）

```
IntcoScreen (Spring Boot 3.0.5)
├── Application.java                ← @SpringBootApplication 入口
├── common/                          ← 公共组件（跨模块复用）
│   ├── config/                      ← I18nConfig, ScheduleConfig
│   ├── constants/                   ← 公共常量 + 枚举
│   ├── handler/                     ← MyBatis JsonArrayTypeHandler
│   ├── task/                        ← GlobalTaskManager (定时任务调度)
│   └── utils/                       ← DongleUtils, EnumUtil, MathUtils
├── module/                          ← 业务模块（7 个）
│   ├── line/         (52 类)        ← 产线/配方
│   ├── detect/       (44 类)        ← 检测记录/状态
│   ├── alarm/        (36 类)        ← 报警
│   ├── yingke/       (14 类)        ← 英科对接（★ 我们用）
│   ├── screen/       (6 类)         ← 大屏展示
│   ├── defect/       (2 类)         ← 缺陷类型
│   └── config/       (2 类)         ← 系统配置
└── framework/                       ← 自研框架（反编译看不到，可能在 BOOT-INF/lib/*.jar）
```

## 📊 模块类分布

| 模块 | 类数 | 占比 | 关键职责 | 对外 Controller |
|---|---|---|---|---|
| **line** | 52 | 20.8% | 产线管理、配方管理、产线状态、产线统计 | `LineController`, `PlanController`, `StateChangeController`, `StateStatisticController` |
| **detect** | 44 | 17.6% | 检测数据上报、缺陷记录查询、状态记录、Excel 导出 | `DetectDataController` |
| **alarm** | 36 | 14.4% | 报警上报、报警处理、报警查询、报警忽略、缺陷报警配置 | `AlarmRecordController`, `DefectTypeController` |
| **yingke** | 14 | 5.6% | ★ 英科系统对接（拉缺陷字典 + 拉缺陷记录） | `YKController` |
| **screen** | 6 | 2.4% | 大屏数据查询（实时缺陷数、产线状态） | （无 controller，被动查询） |
| **defect** | 2 | 0.8% | 缺陷类型定义 | （被 detect 内部使用） |
| **config** | 2 | 0.8% | 系统配置读写 | `SystemConfigController` |
| **common** | 13 | 5.2% | 公共组件 | （无 controller） |

## 🗂️ 各模块子结构

### module/line/（52 类 — 最大）

```
line/
├── dto/         (24 类) ← 24 个 DTO（输入输出）
│   ├── LineBodyDTO, LineUpdateDTO        ← 产线增删改
│   ├── ChgLineOrderDTO                   ← 产线顺序
│   ├── LinePanelQueryDTO                 ← 产线面板查询
│   ├── LinePlanBindDTO                   ← 产线配方绑定
│   ├── LinePlanSwitchDTO                 ← 产线配方切换
│   ├── PlanDTO, PlanQueryDTO             ← 配方
│   ├── LinePlanBindQueryDTO              ← 配方绑定查询
│   ├── ClientPlanQueryDTO                ← 客户端配方查询
│   ├── DetectDataUploadDTO               ← ★ 检测数据上传（★ 关键 DTO）
│   ├── RealTimeDetectData                ← 实时检测数据
│   ├── TodayDetectDataDTO                ← 当日检测数据
│   ├── SearchStateStatisticForm          ← 状态统计查询
│   └── ...（12 个其他）
├── service/     (11 类)
│   ├── ILineService / LineServiceImpl    ← 产线 CRUD
│   ├── IPlanService / PlanServiceImpl    ← 配方 CRUD
│   ├── IStateChangeService               ← 产线状态变更
│   └── ...
├── mapper/      (6 类)   ← MyBatis Mapper 接口
├── model/       (6 类)   ← 持久化对象（LinePO, PlanPO, ...）
└── web/         (4 类)
    ├── LineController                    ← /web/line/*  (管理)
    ├── PlanController                    ← /web/plan/* + /client/plan (客户端)
    ├── StateChangeController             ← /web/line/state/statistic
    └── StateStatisticController          ← (空实现？)
```

### module/detect/（44 类）

```
detect/
├── service/     (12 类)
│   ├── IDefectRecordService / DefectRecordServiceImpl  ← 缺陷记录
│   ├── IStatusRecordService / StatusRecordServiceImpl  ← 状态记录
│   └── ...
├── util/        (9 类)   ← 检测专用工具（图片处理、特征提取等）
├── mapper/      (6 类)
├── model/       (6 类)   ← StatusRecordPO, DefectRecordPO, ...
├── dto/         (5 类)
├── enums/       (4 类)
├── excel/       (2 类)   ← 导出 Excel
└── web/         (1 类)
    └── DetectDataController             ← /client/data/detect + /web/detect/*
```

### module/alarm/（36 类）

```
alarm/
├── dto/         (12 类)
│   ├── AlarmDTO                         ← ★ 报警上传（关键 DTO）
│   ├── AlarmDealDTO                     ← 报警处理
│   ├── AlarmQueryDTO, AlarmInfoQueryDTO ← 报警查询
│   ├── SearchAlarmDTO, IgnoreAlarmDTO   ← 报警搜索/忽略
│   └── ...
├── service/     (6 类)
│   ├── IAlarmRecordService / AlarmRecordServiceImpl
│   ├── IDefectAlarmService              ← 缺陷触发报警
│   └── ...
├── mapper/      (3 类)
├── model/       (3 类)
├── constant/    (5 类)   ← AlarmLevelEnum, AlarmReasonEnum, AlarmTypeEnum, AlarmSolvedEnum, AlarmConstants
├── config/      (2 类)   ← DefectAlarmConfig（按缺陷类型配报警）
├── event/       (2 类)   ← 推送事件
└── web/         (2 类)
    ├── AlarmRecordController            ← /client/data/alarm + /web/alarm/*
    └── DefectTypeController             ← /web/defect-type/*
```

### module/yingke/（14 类 ★ 关键）

```
yingke/
├── dto/         (10 类)  ★
│   ├── AlarmDTO                         ← 英科版报警 DTO
│   ├── ContextDTO                       ← 上下文
│   ├── DetectDataDTO                    ← 英科版检测数据
│   ├── LineAndDefectDTO                 ← 产线+缺陷字典
│   ├── ListParamsDTO                    ← 列表查询参数
│   ├── LoginResultDTO                   ← 登录结果
│   ├── SearchDefectRecordDTO            ← ★★ 拉缺陷记录参数
│   ├── StringParamDTO                   ← 字符串参数
│   ├── YKRequestDTO                     ← ★ 英科 API 请求体（统一）
│   └── YKResponseDTO                    ← ★ 英科 API 响应体（统一）
├── service/     (2 类)
│   ├── IYKService / YKServiceImpl       ← ★★ 英科对接核心
├── event/       (1 类)
│   └── PushAlarmEvent                   ← 报警推送事件
├── config/      (1 类)
│   └── YKConfig                         ← 英科配置
└── web/         (1 类)
    └── YKController                     ← ★★ /client/yk/*
```

### module/screen/（6 类）

```
screen/
├── dto/         (3 类)
│   ├── ClientStatusDTO                  ← 客户端状态
│   ├── DefectNumberDTO                  ← 缺陷数量
│   └── ScreenDataDTO                    ← 大屏数据
└── service/     (2 类)
    ├── IScreenService / ScreenServiceImpl
```

### module/defect/（2 类）

```
defect/
└── service/     (2 类)
    └── IDefectTypeService / DefectTypeServiceImpl
```

### module/config/（2 类）

```
config/
├── service/     (1 类)
│   └── ISystemConfigService / SystemConfigServiceImpl
└── web/         (1 类)
    └── SystemConfigController           ← /web/system-config
```

## 🔗 模块间依赖关系（推测）

```
                    ┌──────────┐
                    │ common/  │  ← 所有模块依赖
                    └────┬─────┘
                         │
       ┌─────────────────┼─────────────────┐
       │                 │                 │
       ▼                 ▼                 ▼
   ┌────────┐        ┌────────┐        ┌────────┐
   │ line/  │◄──────►│detect/ │        │alarm/  │
   └────┬───┘        └────┬───┘        └────┬───┘
        │                 │                 │
        │                 │                 │
        ▼                 ▼                 ▼
   ┌─────────┐       ┌────────┐        ┌─────────┐
   │ config/ │       │defect/ │        │  event  │ (推送)
   └─────────┘       └────────┘        └─────────┘
                                            │
                                            ▼
                                       ┌─────────┐
                                       │ yingke/ │  ← 英科对接
                                       └─────────┘
                                            │
                                            ▼
                                       ┌─────────┐
                                       │ external│ (192.168.80.33:10031)
                                       └─────────┘

       ┌─────────┐
       │ screen/ │  ← 独立模块，提供大屏数据查询
       └─────────┘
```

## 📌 关键调用链

### 链路 1: 现场设备推缺陷到 PSM

```
设备 → POST /client/data/detect (DetectDataUploadDTO)
  → DetectDataController.uploadDetectData()
  → DefectRecordServiceImpl.handleDetectData()
  → DefectRecordServiceImpl.defectAlarmAnalyze()  ← 触发缺陷报警
  → AlarmRecordServiceImpl.addAlarm()  ← 写入报警
  → PushAlarmEvent  ← 发布 Spring 事件
  → (可能) YKServiceImpl.onPushAlarmEvent()  ← 推给英科系统
```

### 链路 2: EdgeHost 拉缺陷字典

```
EdgeHost → GET /client/yk/line-defect
  → YKController.searchLineAndDefect()
  → YKServiceImpl.handleLineAndDefectSearch()
  → (缓存或直接查 PSM DB)
  → 返回 LineAndDefectDTO[]
```

### 链路 3: EdgeHost 拉缺陷记录

```
EdgeHost → POST /client/yk/defect-record (SearchDefectRecordDTO)
  → YKController.searchDefectRecord()
  → YKServiceImpl.searchDefectRecord()
  → (直接查 PSM DB，调用英科系统作为补充？)
  → 返回缺陷记录列表
```

### 链路 4: EdgeHost 推报警到 PSM

```
EdgeHost → POST /client/data/alarm (AlarmDTO)
  → AlarmRecordController.addAlarmData()
  → AlarmRecordServiceImpl.add()
  → 写入 alarm_record 表
```

### 链路 5: PSM 拉英科系统数据

```
定时任务 (GlobalTaskManager + ScheduleConfig)
  → YKServiceImpl.fetchDefectFromYK()
  → HTTP POST http://192.168.80.33:10031/api/dataportal/invoke (YKRequestDTO)
  → 返回 YKResponseDTO
  → 解析后写入 PSM DB
```

---

## 🔍 反编译质量

- ✅ Controller 层：路径、参数、注解全保留
- ✅ DTO 层：字段名、类型、@NotBlank/@NotNull/@Range 校验全保留
- ⚠️ Service 实现：lambda + 部分 inner class 有 `Could not load the following classes` 警告，但核心方法签名可读
- ⚠️ 第三方依赖（spring/hikvision framework）：看不到源码，只能看到 class 名

**对 EdgeHost 对接开发：完全够用**。
