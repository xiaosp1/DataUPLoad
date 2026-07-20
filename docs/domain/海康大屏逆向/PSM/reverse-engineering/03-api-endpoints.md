# 03. PSM HTTP API 端点清单

> PSM 共 **30+ 个 HTTP 端点**，分布在 9 个 Controller。
> **/client/** 是公开的（无需登录），**/web/** 需要登录（海康运维用）。
> **我们的 EdgeHost 只调 /client/***。

---

## ⭐ /client/* 公开接口（EdgeHost 用）

### /client/data/*  — 现场设备/EdgeHost **推数据**到 PSM

| Method | Path | Controller | 请求 DTO | 响应 | 必传字段 |
|---|---|---|---|---|---|
| **POST** | **`/client/data/detect`** | `DetectDataController` | `DetectDataUploadDTO` | `BaseResult` | `faceNo` + `lineNo` + `todayData` + `realTimeData` |
| POST | `/client/data/status` | `DetectDataController` | `List<StatusRecordPO>` | `BaseResult` | - |
| POST | `/client/data/alarm` | `AlarmRecordController` | `AlarmDTO` | `BaseResult` | `uuid` + `time` + `type` + `lineNo` + `faceNo` + `level` + `message` |
| POST | `/client/data/deal-alarm` | `AlarmRecordController` | `AlarmDealDTO` | `BaseResult` | `uuid` |

### /client/yk/*  — EdgeHost **拉数据**（英科对接接口）

| Method | Path | Controller | 请求 DTO | 响应 | 必传字段 |
|---|---|---|---|---|---|
| GET | `/client/yk/line-defect` | `YKController` | （无） | `BaseResult` | - |
| POST | `/client/yk/defect-record` | `YKController` | `SearchDefectRecordDTO` | `BaseResult` | `startTime` |

### /client/plan — EdgeHost 拉配方

| Method | Path | Controller | 请求 DTO | 响应 | 必传字段 |
|---|---|---|---|---|---|
| GET | `/client/plan` | `PlanController` | `ClientPlanQueryDTO` | `BaseResult` | - |

---

## 🔒 /web/* 管理接口（需要登录，海康运维用，**我们不调**）

### /web/line/* — 产线管理

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/line` | `LineController` | `PageQuery` | 产线列表 |
| POST | `/web/line` | `LineController` | `LineBodyDTO` | 新增产线 |
| PUT | `/web/line` | `LineController` | `LineUpdateDTO` | 修改产线 |
| DELETE | `/web/line` | `LineController` | `?id=...` | 删除产线 |
| PUT | `/web/line/order` | `LineController` | `List<ChgLineOrderDTO>` | 修改产线顺序 |
| GET | `/web/line/tree` | `LineController` | - | 查询产线树 |
| POST | `/web/line/plan/bind` | `LineController` | `LinePlanBindDTO` | 产线配方分发 |
| POST | `/web/line/plan/switch` | `LineController` | `LinePlanSwitchDTO` | 产线配方切换 |
| GET | `/web/line/panel` | `LineController` | `LinePanelQueryDTO` | 配方面板 |
| GET | `/web/line/status` | `LineController` | `LinePanelQueryDTO` | 配方状态 |
| GET | `/web/line/group` | `LineController` | - | 产线分组 |

### /web/line/state/* — 产线状态变更

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/line/state/statistic` | `StateChangeController` | `SearchStateStatisticForm` | 状态变更查询 |

### /web/line/stateStatisticPO — 空

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| - | `/line/stateStatisticPO` | `StateStatisticController` | （空实现） | （似乎没用） |

### /web/plan/* — 配方管理

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| POST | `/web/plan` | `PlanController` | `PlanDTO` | 新增配方 |
| DELETE | `/web/plan` | `PlanController` | `IdQuery` | 删除配方 |
| PUT | `/web/plan` | `PlanController` | `PlanDTO` | 修改配方 |
| GET | `/web/plan` | `PlanController` | `PlanQueryDTO` | 查询配方 |
| GET | `/web/plan-bind` | `PlanController` | `LinePlanBindQueryDTO` | 客户端绑定配方 |

### /web/detect/* — 缺陷查询（管理用）

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/detect/detail` | `DetectDataController` | `?faceId&startTime&endTime` | 缺陷详情 |
| GET | `/web/detect/statistic/export` | `DetectDataController` | `ExportDefectStatisticForm` | 导出 Excel |
| GET | `/web/detect/realtime` ⚠️ | `DetectDataController` | `?lineNo&faceNo` | 实时数据（**已 @Deprecated**） |

### /web/alarm/* — 报警管理

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/alarm/list` | `AlarmRecordController` | `AlarmQueryDTO` | 报警列表 |
| GET | `/web/alarm/num` | `AlarmRecordController` | - | 报警数量 |
| GET | `/web/alarm/list-info` | `AlarmRecordController` | `AlarmInfoQueryDTO` | 报警列表详情 |
| GET | `/web/alarm` | `AlarmRecordController` | `SearchAlarmDTO` | 按类型查询 |
| PUT | `/web/alarm/ignore` | `AlarmRecordController` | `IgnoreAlarmDTO` | 忽略报警 |

### /web/system-config — 系统配置

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/system-config` | `SystemConfigController` | - | 查询配置 |
| PUT | `/web/system-config` | `SystemConfigController` | `List<SystemConfigPO>` | 修改配置 |

### /web/defect-type/* — 缺陷类型管理（在 `DefectTypeController` 里）

| Method | Path | Controller | 请求 | 用途 |
|---|---|---|---|---|
| GET | `/web/defect-type` | `DefectTypeController` | （待查） | 查询缺陷类型 |
| POST | `/web/defect-type` | `DefectTypeController` | （待查） | 新增缺陷类型 |

---

## 📋 BaseResult 响应格式（推测）

Spring Boot 标准 Result 包装类，常见结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }   // 实际数据，类型依端点而异
}
```

**实际响应结构**需要等 PSM 启动 + curl 验证。

---

## 🔌 端点使用矩阵

| 我们 EdgeHost 要做什么 | 调用 PSM 的端点 |
|---|---|
| **拉缺陷字典** | `GET /client/yk/line-defect` |
| **拉缺陷记录** | `POST /client/yk/defect-record` |
| **拉产线状态** | （无对应端点，需要自己查 DB 或加 cache） |
| **推检测数据** | `POST /client/data/detect` |
| **推状态数据** | `POST /client/data/status` |
| **推报警** | `POST /client/data/alarm` |
| **处理报警** | `POST /client/data/deal-alarm` |
| **拉配方** | `GET /client/plan` |

---

## 🔍 待补全项

1. **`DefectTypeController` 的具体端点** — 没反编译完整，需要时再补
2. **BaseResult 字段名**（code/data/success/message 还是别的）— 需要实际调用验证
3. **`/client/yk/line-defect` 响应结构**（LineAndDefectDTO[] 还是嵌套对象）— 需要实际调用验证
4. **`/client/yk/defect-record` 响应结构**（缺陷记录列表的字段）— 需要实际调用验证

这些**待 PM 启动 PSM 后 curl 验证**，或者从 Spring Boot 框架推测。
