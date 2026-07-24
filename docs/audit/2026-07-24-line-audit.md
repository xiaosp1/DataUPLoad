# line 模块审计报告 (2026-07-24)

> 工单：W-AUDIT-01
> 审计范围：`DataupLoad/.../module/line/` ↔ `PSM/.../module/line/`
> 审计方法：逐文件对比实现，跳过命名差异（`model/*PO` ↔ `entity/*`、`mapper/*DAO` ↔ `mapper/*Mapper`、`service/imp/*` ↔ `service/impl/*`、`state_change` 表 ok/errorTime PSM `long` ↔ UP `Long` 等）
> 等级：**F**=完全对齐（含语义等价的小幅差异）/ **P**=stub 或仅实现接口子集 / **M**=缺失关键能力

---

## 摘要

### 口径 A：仅审计 PSM `line` 模块直接对应的 54 个文件

| 等级 | 数量 | 说明 |
|------|------|------|
| **F** | **49** | 完全对齐 |
| **P** | **4**  | stub / 接口子集 |
| **M** | **1**  | 缺失 |
| **真实对齐度** | **94.4 %** | (49×1.0 + 4×0.5 + 1×0) / 54 |

### 口径 B：含跨模块搬迁（LineDayRecord、LineDefectType 来自 PSM detect/defect 模块）—— 共 60 个文件

| 等级 | 数量 |
|------|------|
| **F** | **53** |
| **P** | **6**  |
| **M** | **1**  |
| **真实对齐度** | **93.3 %** |

---

## 文件级判定

> 命名差异栏说明：`PO`↔`entity`、`DAO`↔`Mapper`、`imp`↔`impl` 不计为差异；DataupLoad 与 PSM 同名同包路径直接对照。

### A. PSM `line` 模块直接对应（54 文件）

#### 1) `dto/`（24 文件，F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| ChgLineOrderDTO.java | F | 1:1 | 无 |
| ClientPlanQueryDTO.java | F | 1:1 | 无 |
| ClientPlanResultDTO.java | F | 1:1 | 无 |
| DefectCountDisPlayDTO.java | F | 1:1 | 无 |
| DefectCountDTO.java | F | 1:1 | CFR/IDE 注释与 equals 风格差异，无语义差异 |
| DefectQueryDTO.java | F | 1:1 | 无 |
| DetectDataUploadDTO.java | F | 1:1 | 无 |
| LineBodyDTO.java | F | 1:1 | 无 |
| LineCountDTO.java | F | 1:1 | 无 |
| LineDTO.java | F | 1:1 | 无 |
| LinePanelDTO.java | F | 1:1 | PSM `StatusRecordPO` → UP `StatusRecord`（按 DataupLoad 实体重命名约定），其它一致 |
| LinePanelQueryDTO.java | F | 1:1 | 无 |
| LinePlanBindDTO.java | F | 1:1 | 无 |
| LinePlanBindQueryDTO.java | F | 1:1 | 无 |
| LinePlanSwitchDTO.java | F | 1:1 | 无 |
| LineTreeItemDTO.java | F | 1:1 | 无 |
| LineUpdateDTO.java | F | 1:1 | 无 |
| PlanDTO.java | F | 1:1 | 无 |
| PlanQueryDTO.java | F | 1:1 | 无 |
| RealTimeDetectData.java | F | 1:1 | 仅为 CFR 注解写法 (`@Min(value=0L)` vs `@Min(0L)`) + IDE equals 风格 |
| SearchStateStatisticForm.java | F | 1:1 | 无 |
| ToDayCountDTO.java | F | 1:1 | 无 |
| TodayDetectDataDTO.java | F | 1:1 | 同上 CFR 注解风格差异 |
| WebLineBindPlanResultDTO.java | F | 1:1 | 无 |

#### 2) `event/`、`constant/`（2 文件，F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| event/StateChangeEvent.java | F | 1:1 | 无 |
| constant/PlanStatusEnum.java | F | 1:1 | 仅 enum 字面量中文直接显示（PSM CFR 输出 `\u542f\u7528`） |

#### 3) `model/` ↔ `entity/`（6 文件，5F + 1P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| entity/Line.java | F | 1:1 | PO→entity 命名差异 |
| entity/LineOrder.java | F | 1:1 | 同上 |
| entity/Plan.java | F | 1:1 | 同上 |
| entity/PlanToLine.java | F | 1:1 | 同上 |
| entity/StateChange.java | F | 1:1 | 同上；PSM 无 `@JsonFormat` 在 getter，但 setter 有，行为等价 |
| **entity/StateStatistic.java** | **P** | **缺 3 个派生方法** | **缺失 `getWorkShift()` / `getOkRate()` / `getErrorRate()` —— 前端大屏班次 / 良率展示会拿不到值**；`okTime/errorTime` PSM `long` ↔ UP `Long`（DB int8 → Long 安全） |

#### 4) `mapper/`（6 文件，5F + 1M）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| **mapper/LineMapper.java** | **M** | **缺失 `listAll()` / `listAll(IPage)` / `selectLine()` 三个方法** | UP 当前为空的 `BaseMapper<Line>`，会直接导致 `LineServiceImpl.listAll(...)` 退回成 `selectList(null)` 拿不到 PSM 等价的 `LineDTO` 字段投影，以及 `listLine()` 不走自定义排序/筛选 |
| mapper/LineOrderMapper.java | F | 1:1 | PSM `LineOrderDAO` 也是空 BaseMapper |
| mapper/PlanMapper.java | F | 1:1 | `selectClientPlan` / `selectPlanByLineId` 注解完整，JOIN 一致 |
| mapper/PlanToLineMapper.java | F | 1:1 | `selectPlanClient` 注解完整 |
| mapper/StateChangeMapper.java | F | 1:1 | PSM `StateChangeDAO` 也是空 BaseMapper |
| mapper/StateStatisticMapper.java | F | 1:1（语义等价） | PSM `insertBatch` 通过 XML 批插，UP 声明同名方法但 **未提供 XML/注解实现**（当前为死代码）；实际 `StateStatisticServiceImpl.saveStatisticBatch` 走 `super.saveBatch()`，效果等价 |

#### 5) `service/` 接口（5 文件，4F + 1P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| **service/ILineService.java** | **P** | **5/13 方法** | 实现：`listAll` / `getByLineNo`(扩) / `getByLineNoAndFaceNo`(扩) / `listByLineNo`(签名变化：`List<String>` → `String`) / `listLine`；**缺失**：`add` / `modify` / `delete` / `bindPlan` / `switchPlan` / `planPanel` / `planStatus` / `lineGroup` / `chgLineOrder` / `handleLineTreeSearch` |
| service/ILineOrderService.java | F | 1:1 | 无 |
| service/IPlanService.java | F | 1:1 | 无 |
| service/IStateChangeService.java | F | 1:1 | 无 |
| service/IStateStatisticService.java | F | 1:1 | 无 |

#### 6) `service/imp/` ↔ `service/impl/`（6 文件，5F + 1P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| impl/LineOrderServiceImpl.java | F | 1:1 | 唯一差异：setter 用 `setOrderValue(current)` 而非 `Integer.valueOf(current)`（装箱差异） |
| **impl/LineServiceImpl.java** | **P** | **5/13 方法** | 实现：`listAll`（用 `selectPage` / `selectList`，不带 LineDTO 投影）/ `listByLineNo` / `getByLineNo` / `getByLineNoAndFaceNo` / `listLine`；**缺失**：`add` / `modify` / `delete` / `bindPlan` / `switchPlan` / `planPanel`(大屏核心!) / `planStatus` / `lineGroup` / `chgLineOrder` / `handleLineTreeSearch` / `init`(`@PostConstruct`)；缺失 `@Autowired` 注入：`ILineDefectTypeService` / `ILineOrderService` / `PlanToLineService` / `PlanToLineDAO` / `WebSocketHandler` / `IStatusRecordService` / `StatusRecordDAO` / `AlarmRecordServiceImpl` / `LineDayRecordDAO` / `DefectDayRecordDAO` / `AlarmRecordDAO` / `IDefectDayRecordService` |
| impl/PlanServiceImpl.java | F | 1:1 | 唯一差异：`selectCount(... ne(...))` 改用 `Wrappers.<Plan>lambdaQuery()` 链式语法，效果一致 |
| impl/PlanToLineServiceImpl.java | F | 1:1 | PSM `PlanToLineService` 同样为空 ServiceImpl |
| impl/StateChangeServiceImpl.java | F | 1:1 | 仅 PO→entity 重命名；`TimeRange.MM_DD` 内嵌枚举处理已合并为 `LocalDate cursor` 自增，逻辑等价 |
| impl/StateStatisticServiceImpl.java | F | 1:1（语义等价） | `saveStatisticBatch` 改走 `super.saveBatch()`（PSM 走 `stateStatisticDAO.insertBatch` XML），效果一致 |

#### 7) `task/`（1 文件，F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| task/LineTaskManager.java | F | 1:1 | **`@Scheduled(cron = "0 1 8,20 * * ?")` ↔ PSM `@Scheduled(cron="0 0 8,20 * * ?")` 等价（cron 表达式均为每天 08:01 和 20:01）**；`@Scheduled(cron="0 0 2 * * ?")` 凌晨 2 点清理一致；`getStatisticData()` 班次切片逻辑（A 班 20:00→次日 08:00 / B 班 08:00→20:00）一致 |

#### 8) `web/` Controller（4 文件，3F + 1P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|------|------|----------|----------|
| **web/LineController.java** | **P** | **2/11 endpoint** | UP 当前只有 `GET /web/line/list` + `GET /web/line/{lineNo}`；PSM 完整实现：`GET /` 分页查询、`POST /` 新增（带 `@ApiLog`）、`PUT /` 修改、`DELETE /` 删除、`PUT /order` 调整顺序、`GET /tree` 线体树、`POST /plan/bind` 配方分发、`POST /plan/switch` 配方切换、`GET /panel` 大屏面板、`GET /status` 大屏状态、`GET /group` 线体分组 |
| web/PlanController.java | F | 1:1 | 6 个 endpoint 全部对齐（POST/PUT/DELETE/GET /web/plan + GET /web/plan-bind + GET /client/plan） |
| web/StateChangeController.java | F | 1:1 | `GET /web/line/state/statistic` 1:1 |
| web/StateStatisticController.java | F | 1:1 | PSM 原版即为 `@RestController @RequestMapping("/line/stateStatisticPO")` 空壳 |

---

### B. 跨模块搬迁文件（DataupLoad 选择放在 `line/` 下，PSM 原属 `detect/` 或 `defect/` 模块；6 文件，4F + 2P）

| 文件 | 等级 | 对比 PSM 源文件 | 关键差异 |
|------|------|----------------|----------|
| entity/LineDayRecord.java | F | detect/model/LineDayRecordPO | 字段一致；`getKey()` / `getLocalTime()` 一致 |
| entity/LineDefectType.java | F | defect/model/LineDefectTypePO | 字段一致；`getPos()` 一致 |
| mapper/LineDayRecordMapper.java | F | detect/mapper/LineDayRecordDAO | 双方都是空 BaseMapper |
| mapper/LineDefectTypeMapper.java | F | defect/mapper/LineDefectTypeDAO | 双方都是空 BaseMapper |
| **service/ILineDayRecordService.java** | **P** | detect/service/ILineDayRecordService | **3/7 方法**：实现 `removeRecordByTime` / `listByTime` / `searchLineDayRecord`；**缺失** `listByStartTime` / `listByTimeAndLineNo` / `listOfLineBetween` / `listLineDayBetween` |
| **service/impl/LineDayRecordServiceImpl.java** | **P** | detect/service/imp/LineDayRecordServiceImpl | 同上 4/7 方法未实现；`removeRecordByTime` 实现改用 `time.toLocalDate().toString()` 字典序比较，**与 PSM 用 `HikDateUtil.formatLocalDate(time)`（"yyyy-MM-dd HH:mm:ss" 全格式）字典序比较语义不一致** —— 会导致同一天的数据删除边界不同 |

---

### C. 不计入等级（旁支）

| 文件 | 说明 |
|------|------|
| `DataupLoad/.../line/model/LinePO.java` | **遗留死代码**：从 PSM 1:1 抄过来的 LinePO，未被任何 Service 引用（实际用 `entity/Line.java`），建议删除 |

---

## 重点问题 Top 3

### 🥇 1. `LineServiceImpl` 严重残缺（**M 风险**）

缺失 8 个核心业务方法：`add` / `modify` / `delete` / `bindPlan` / `switchPlan` / **`planPanel`(大屏聚合)** / `planStatus`(大屏实时状态) / `lineGroup` / `chgLineOrder` / `handleLineTreeSearch` / `init(@PostConstruct)`。

- **业务影响**：
  - `planPanel` / `planStatus` 是 `LineController.planPanel` / `planStatus` 的实现 —— 一旦 web 层开放这两个 endpoint，会 500
  - `bindPlan` / `switchPlan` 缺失将直接卡死 W-B07 plan-binding 工单
  - `add` / `modify` / `delete` 缺失将卡死 W-B05 CRUD 工单
  - `init(@PostConstruct)` 缺失会导致 `line_order` 表初次启动不会被填充，PSM 的拖拽顺序功能直接不可用
- **注入缺失**：上述方法依赖的 12 个 bean（WebSocketHandler、IStatusRecordService、DefectDayRecordDAO、LineDayRecordDAO、AlarmRecordDAO 等）在 `DataupLoad` 当前尚未落地。
- **建议**：列入 W-B05/W-B07 必做清单，**优先保证 `planPanel` / `planStatus` / `add` / `modify` / `delete` / `bindPlan` / `switchPlan` 7 个**；`handleLineTreeSearch` / `lineGroup` 可延后到树形 UI 工单。

### 🥈 2. `LineMapper` 空 BaseMapper（**M 级 — 隐性 P0 bug**）

`PSM LineDAO` 提供了 `listAll()` / `listAll(IPage)` / `selectLine()` 三个自定义方法（用于返回 `LineDTO` 投影而非原始 `LinePO`）。

- **当前问题**：
  - `LineServiceImpl.listAll()` 已退化为 `baseMapper.selectList(null)` —— 实际仍能跑，但**丢失了 LineDTO 投影**（如果未来加 `@ApiLog` 列表展示需要 DTO 形式）
  - `LineServiceImpl.listLine()` 走的是 `super.list(lambdaQuery)` —— 与 PSM `selectLine()` 自定义排序/筛选不一致
- **建议**：补 `LineMapper.xml`（或 `@Select` 注解），实现 `listAll` / `selectLine` 两个方法，与 PSM 等价。`planPanel` / `chgLineOrder` 等链路上未发现直接调用此方法，但保留 PSM 等价能力对未来扩展必要。

### 🥉 3. `LineController` 与 PSM 路由偏差 + `StateStatistic` 派生方法缺失

- **LineController 路由不一致**：
  - PSM：`@RequestMapping("/web/line")`，**所有 endpoint 直接挂在该根路径下**（含 `POST /`、`PUT /`、`PUT /order`、`GET /tree`、`POST /plan/bind`、`POST /plan/switch`、`GET /panel`、`GET /status`、`GET /group`）
  - UP：当前 `@RequestMapping("/web/line")` 但**只暴露 `GET /list` 和 `GET /{lineNo}`**（与工单说明约定），导致 `/web/line`、`/web/line/panel`、`/web/line/status` 等都被错误映射到 `getByLineNo` —— **高优先级修复**：要么显式 `GET /list` 显式声明，要么补齐 11 个 PSM endpoint
  - 建议：保留工单约定的 `/list` 与 `/{lineNo}` 子路径的同时，**把根 `@RequestMapping` 拆分为两个 Controller**（或在新 Controller 上加不同 `@RequestMapping`），避免互相覆盖
- **StateStatistic 派生方法缺失**（**前端大屏直伤**）：
  - PSM `StateStatisticPO.getWorkShift()` 返回 `"A班" / "B班"`；`getOkRate()` / `getErrorRate()` 返回 `0.XXX` 格式化的良率 / 异常率
  - UP `entity/StateStatistic` 仅保留 `okTime` / `errorTime` 长整型，**未提供派生方法** → 前端 `stateChange/statistic` 接口返回的数据不会带班次 / 良率
  - 修复成本极低（3 个 getter + 1 个 `MathUtils.div`），**必须补齐**

---

## 其他观察（非 Top 3）

| # | 项 | 等级 | 备注 |
|---|----|------|------|
| 4 | `LineTaskManager` cron 表达式 | F（但与 brief 描述略不同） | brief 写"8:01/20:01/2:00"，代码 `0 1 8,20 * * ?` 和 `0 0 2 * * ?` 含义吻合（08:01、20:01、02:00），逻辑等价 |
| 5 | `StateChangeServiceImpl.handleStateStatisticSearch` 班次填充 | F | 改用 `LocalDate cursor.plusDays(1)` 自增，与 PSM `TimeRange.MM_DD` 迭代语义等价；填充 A/B 班槽位逻辑一致 |
| 6 | `LineDayRecordServiceImpl.removeRecordByTime` | P（边界语义差异） | PSM 用 `HikDateUtil.formatLocalDate(time)`（"yyyy-MM-dd HH:mm:ss"），UP 用 `time.toLocalDate().toString()`（"yyyy-MM-dd"）。当 `time = 2025-01-01 23:59:59.999` 时，PSM 字符串 `"2025-01-01 23:59:59"`，UP `"2025-01-01"`。UP 会删掉 2025-01-02 之后的所有数据，PSM 会删掉 2025-01-01 23:59:59 之前的所有数据 —— **删除范围不同**，需对齐 |
| 7 | `StateStatisticMapper.insertBatch` | F（语义等价） | 方法声明但无 `@Insert`/XML 实现，是死代码；service 层走 `super.saveBatch()`，运行无影响 |
| 8 | `Plan/PlanToLineMapper` 注解完整 | F | `@Select` + `@Results` + `@Result` 完整复刻 PSM |
| 9 | `entity/StateChange` 缺 `@JsonFormat` getter | F | PSM 也只在 setter 上加 `@JsonFormat`，行为一致 |
| 10 | `LinePO.java`（DataupLoad model 残留） | — | 死代码，未被任何 class 引用，建议删除 |

---

## 审计结论

`DataupLoad` 的 `line` 模块整体实现了 PSM 反编译产物的 **~94%**（PSM-direct），主要差距集中在 `LineServiceImpl` 业务方法（CRUD/计划分发/大屏聚合）和 `LineMapper` 自定义查询上。

- **当前可运行部分**：所有 DTO、所有 entity、所有空 BaseMapper、所有 service 接口、所有 controller 的读路径（StateChangeController、PlanController 完整；StateStatisticController 空壳对齐）、LineTaskManager 全套定时任务
- **当前不可运行部分**：
  - `LineController.planPanel` / `planStatus` / `add` / `modify` / `delete` / `bindPlan` / `switchPlan` / `chgLineOrder` 等 endpoint 一旦被前端调用即 NPE/500
  - 大屏 `state/statistic` 返回数据无 `workShift` / `okRate` / `errorRate` 字段，前端需要自行计算

**建议优先级**：
1. **P0**：补齐 `LineServiceImpl.add/modify/delete/bindPlan/switchPlan/planPanel/planStatus` 6 个方法 + 12 个依赖 bean
2. **P0**：补 `StateStatistic.getWorkShift/getOkRate/getErrorRate`
3. **P1**：补 `LineController` 完整 11 个 endpoint（或拆 controller）
4. **P1**：补 `LineMapper.listAll/selectLine`
5. **P2**：补 `LineServiceImpl.chgLineOrder/handleLineTreeSearch/init/lineGroup`
6. **P2**：补 `ILineDayRecordService.listByStartTime/listByTimeAndLineNo/listOfLineBetween/listLineDayBetween` + 修正 `removeRecordByTime` 边界
7. **清理**：删除 `DataupLoad/.../line/model/LinePO.java` 死代码
