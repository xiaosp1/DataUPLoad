# PSM line 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ P1 已归档
**优先级**: 🟡 P1（产线管理 + 方案分发）

---

## 1. 业务定位

### 1.1 解决什么问题

line 模块是 PSM 的"产线 + 方案"管理核心：

- **产线 CRUD**：`Line` 表全生命周期管理（增/改/删/排序/树形查询）
- **方案管理**：`Plan` + `PlanToLine`（产线-方案绑定关系）
- **方案分发/切换**：通过 WebSocket 通知客户端切换配方
- **设备状态变更追踪**：`StateChangeEvent` → 写入 `state_change`，`@Async @EventListener` 异步落库
- **设备状态统计**：`state_statistic` 累加（白班/夜班 ON/OFF 时长）
- **大屏面板聚合**：`planPanel()` 返回生产数据 + 缺陷 + 报警 + 设备状态综合视图

### 1.2 与其他模块的依赖关系

```
line ──→ defect (ILineDefectTypeService)        # 缺陷类型与产线绑定
line ──→ detect (StatusRecordDAO/LineDayRecordDAO/DefectDayRecordDAO) # 设备状态 + 良率数据
line ──→ alarm  (AlarmRecordDAO/AlarmReasonEnum) # 报警查询 + 断连报警
line ──→ detect (StateChangeEvent)                # 设备状态事件
line ──→ common (WebSocketHandler/CommonMethod)  # 方案变更广播
```

---

## 2. 类清单（54 个 java + 4 个 XML）

### 2.1 constant/ (1)
| 枚举 | 值 | 备注 |
|---|---|---|
| `PlanStatusEnum` | ENABLE(1) / DISABLE(2) | plan_to_line.status |

### 2.2 dto/ (24)
| 类别 | DTO |
|---|---|
| 产线管理 | `LineBodyDTO` / `LineUpdateDTO` / `LineDTO` / `LineTreeItemDTO` |
| 方案管理 | `PlanDTO` / `PlanQueryDTO` / `LinePlanBindDTO` / `LinePlanBindQueryDTO` / `LinePlanSwitchDTO` / `ClientPlanQueryDTO` / `ClientPlanResultDTO` / `WebLineBindPlanResultDTO` |
| 数据上传 | `DetectDataUploadDTO` / `TodayDetectDataDTO` / `RealTimeDetectData` |
| 统计 | `DefectCountDTO` / `DefectCountDisPlayDTO` / `LineCountDTO` / `LinePanelDTO` / `LinePanelQueryDTO` / `ToDayCountDTO` |
| 状态 | `ChgLineOrderDTO` / `SearchStateStatisticForm` |
| 查询 | `DefectQueryDTO` |

### 2.3 event/ (1)
| 事件 | 触发方 | 监听方 |
|---|---|---|
| `StateChangeEvent` | `StatusRecordServiceImpl.receiveStatus()` | `StateChangeServiceImpl.handleStateChange()` (@Async) |

### 2.4 mapper/ (6)
| DAO | 关键 XML 方法 |
|---|---|
| `LineDAO` | `listAll(Page)` / `listAll()` / `selectLine()` |
| `PlanDAO` | `selectClientPlan(lineNo, faceNo)` / `selectPlanByLineId(lineId)` |
| `PlanToLineDAO` | `batchInsert(List)` / `selectPlanClient(planId)` |
| `LineOrderDAO` | 默认 CRUD |
| `StateChangeDAO` | 默认 CRUD |
| `StateStatisticDAO` | `insertBatch(List)` (PG on conflict do nothing) |

### 2.5 model/ (6)
| PO | 表 | 关键字段 |
|---|---|---|
| `LinePO` (+Builder) | `line` | id/name/lineNo/faceNo/color/clientNo/realtimeData(订单、updateTime、createTime) |
| `PlanPO` | `plan` | id/name/uri/description |
| `PlanToLinePO` | `plan_to_line` | id/lineId/planId/status |
| `LineOrderPO` (+Builder) | `line_order` | id/lineId/orderValue |
| `StateChangePO` | `state_change` | id/lineId/type/changeTime |
| `StateStatisticPO` | `state_statistic` | id/lineId/statisticTime/okTime/errorTime（lineNo/faceNo/time 是 @TableField(exist=false) 临时字段）|

### 2.6 service/ (5) + service/imp/ (6)
| 接口 | 实现 | 责任 |
|---|---|---|
| `ILineService` | `LineServiceImpl` | **🟡 P1** 产线 CRUD + 排序 + 树形 + bindPlan + switchPlan + planPanel |
| `IPlanService` | `PlanServiceImpl` | **🟡 P1** 方案 CRUD + 查询 + 客户端绑定 |
| `ILineOrderService` | `LineOrderServiceImpl` | 产线排序 addLineOrder/modLineOrder |
| `IStateChangeService` | `StateChangeServiceImpl` | **🟡 P1** 状态变更事件 + 状态统计查询 |
| `IStateStatisticService` | `StateStatisticServiceImpl` | 状态统计 CRUD |
| (无接口) | `PlanToLineService` | 默认 ServiceImpl（继承）|

### 2.7 task/ (1)
| 类 | cron | 责任 |
|---|---|---|
| `LineTaskManager` | `0 1 8,20 * * ?` + `0 0 2 * * ?` | **🟡 P1** 每 8/20 点 01 分统计状态 + 每日 2 点清理 30 天前数据 |

### 2.8 web/ (4)
| Controller | 端点 |
|---|---|
| `LineController` | `/web/line` CRUD + `/web/line/order` + `/web/line/tree` + `/web/line/plan/bind` + `/web/line/plan/switch` + `/web/line/panel` + `/web/line/status` + `/web/line/group` |
| `PlanController` | `/web/plan` CRUD + `/web/plan-bind` + `/client/plan` |
| `StateChangeController` | `/web/line/state/statistic` |
| `StateStatisticController` | 空（请求路径 `/line/stateStatisticPO` 但无方法）|

---

## 3. 核心流程

### 3.1 产线初始化流程（应用启动）

```
Spring Boot 启动
  │
  └─→ LineServiceImpl.@PostConstruct init()
        │
        ├─→ lineOrderService.count() == 0 ?
        │     │
        │     └─ YES → 读取所有 line id
        │           └─→ lineOrderService.addLineOrder(lineIds)
        │                 └─→ 保存 LineOrderPO (lineId + orderValue 递增)
        │
        └─ DONE
```

### 3.2 产线添加流程

```
LineController.add(@RequestBody LineBodyDTO)
  │
  └─→ LineServiceImpl.add(lineDTO) [@Transactional]
        │
        ├─→ SELECT count(*) FROM line WHERE line_no=? AND face_no=?
        │     └─ > 0 → return error 20202 (重复)
        │
        ├─→ save(LinePO { lineNo, faceNo, name, color })
        │     └─→ clientNo = lineNo + "-" + faceNo
        │
        └─→ lineOrderService.addLineOrder([newLineId])
              └─→ 保存 LineOrderPO (lineId + max(orderValue)+1)
```

### 3.3 方案分发流程

```
LineController.dispatchSolution(@RequestBody LinePlanBindDTO)
  │
  └─→ LineServiceImpl.bindPlan(linePlanBindDTO)
        │
        ├─→ SELECT * FROM plan_to_line WHERE line_id=?
        │     ├─ 已有且 planIds 相等 → return ok (幂等)
        │     └─ 否则继续
        │
        ├─→ 查找当前运行方案 (status=1 ENABLE)
        │     └─ 如果当前运行方案不在新 planIds 中 → return error 20205
        │
        ├─→ DELETE FROM plan_to_line WHERE line_id=?
        ├─→ 保存新 plan_to_line 列表（保持当前运行方案 ENABLE）
        └─→ WebSocket 推送: CommonMethod.sendPlanChange(wsHandler, clientNo)
              └─→ 客户端收到方案变更通知
```

### 3.4 方案切换流程

```
LineController.switchSolution(@RequestBody LinePlanSwitchDTO)
  │
  └─→ LineServiceImpl.switchPlan(linePlanSwitchDTO)
        │
        ├─→ SELECT * FROM plan_to_line WHERE line_id=? AND status=1 (当前运行)
        ├─→ SELECT * FROM plan_to_line WHERE line_id=? AND plan_id=? (目标)
        │     └─ 目标不存在 → return error 20207
        │
        ├─→ 当前运行方案存在 + 与目标不同 → UPDATE status=2 (DISABLE)
        ├─→ UPDATE 目标方案 status=1 (ENABLE)
        └─→ WebSocket 推送: sendPlanChange(wsHandler, clientNo)
```

### 3.5 设备状态变更追踪（事件驱动）

```
DetectDataController.receiveStatus(records)
  │
  └─→ StatusRecordServiceImpl.receiveStatus() [@Transactional]
        │
        └─→ publish StateChangeEvent(time, status=ONLINE, lineNo, faceNo)
              │
              └─→ StateChangeServiceImpl.handleStateChange() [@Async @EventListener]
                    │
                    ├─→ lineService.getByLineNoAndFaceNo()
                    ├─→ SELECT * FROM state_change WHERE line_id=? AND change_time <= ? LIMIT 1
                    │     └─ null 或 type 不同 → save(new StateChangePO)
                    │
                    └─ 异步写入，避免阻塞 status_record 主流程
```

### 3.6 状态统计（每日 8/20 点 cron）

```
LineTaskManager @Scheduled(cron = "0 1 8,20 * * ?") getStatisticData()
  │
  ├─→ if now.hour == 8:
  │     start = 昨天 20:00
  │     end = 今天 08:00   (夜班)
  │
  └─→ else (now.hour == 20):
        start = 今天 08:00
        end = 今天 20:00   (白班)
  
  └─→ stateChangeService.getStateStatistics(lineIds, start, end)
        │
        ├─→ SELECT * FROM state_change WHERE line_id IN (...) AND change_time BETWEEN ? AND ?
        ├─→ 对每个产线：
        │     ├─ 排序 state_change by changeTime
        │     ├─ 首条前补 start 状态
        │     ├─ 末条后补 end 状态
        │     ├─ 相邻两条差值 → onlineTime / offlineTime (ChronoUnit.MILLIS)
        │     └─ 返回 List<StateStatisticPO>
        │
        └─→ stateStatisticService.saveStatisticBatch(statistics)
              └─→ INSERT INTO state_statistic(...) ON CONFLICT(line_id, statistic_time) DO NOTHING
                    (PostgreSQL upsert)
```

### 3.7 状态数据清理

```
LineTaskManager @Scheduled(cron = "0 0 2 * * ?") clearExpireStateData()
  │
  ├─→ expireTime = now - lineStateRetentionTime (默认 30 天)
  ├─→ DELETE FROM state_statistic WHERE statistic_time <= expireTime
  └─→ DELETE FROM state_change WHERE change_time <= expireTime
```

---

## 4. 关键类逐个解析

### 4.1 🟡 P1: `LineServiceImpl` (311 行) — 最大的 service 实现

**业务定位**：产线全生命周期管理 + 方案分发 + 大屏面板聚合

**核心方法签名**:
```java
public BaseResult listAll(PageQuery pageQuery)                    // 分页或全量查询
public void init()                                                // @PostConstruct 初始化 line_order
@Transactional BaseResult add(LineBodyDTO lineDTO)                // 新增产线
BaseResult modify(LineUpdateDTO lineUpdateDTO)                    // 修改产线
@Transactional BaseResult delete(Integer id)                      // 删除产线（带 clientStatus 检查）
BaseResult bindPlan(LinePlanBindDTO linePlanBindDTO)              // 方案分发（WebSocket 通知）
BaseResult switchPlan(LinePlanSwitchDTO linePlanSwitchDTO)        // 方案切换
BaseResult planPanel(LinePanelQueryDTO form)                      // 大屏面板数据聚合
BaseResult planStatus(LinePanelQueryDTO linePanelQueryDTO)        // 产线状态
BaseResult lineGroup()                                            // 产线分组
List<LinePO> listLine()                                           // 全量产线
BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders)         // 修改排序
LinePO getByLineNoAndFaceNo(String lineNo, String faceNo)         // 按 lineNo+faceNo 查询
BaseResult handleLineTreeSearch()                                 // 树形查询
List<LinePO> listByLineNo(List<String> lineNos)                   // 按 lineNo 批量查询
```

**planPanel 内部逻辑**（聚合 4 类数据）:
1. `lineDayRecordDAO.selectLineCountDay(start, end, lineNo, faceNo)` → `LinePanelDTO.lineCountDTOS`
2. `defectDayRecordDAO.selectDefectCountDay(start, end, lineNo, faceNo, defects)` → `defectCountDTOS` (按 type+time 分组)
3. `alarmRecordDAO.selectAlarmCountDay(start, end, lineNo, faceNo)` → `alarmCountDTOS`
4. `statusRecordDAO.selectList(lineNo, faceNo)` → `statusRecordPOS`
5. `lineDayRecordDAO.selectRightAndError(lineNo, faceNo)` → `toDayCountDTO` (今日合计)

**关键依赖**:
```java
@Autowired WebSocketHandler webSocketHandler;       // 方案变更广播
@Autowired AlarmRecordServiceImpl alarmRecordService;  // 删除产线时清理客户端报警
@Autowired PlanToLineService planToLineService;
```

### 4.2 🟡 P1: `PlanServiceImpl` (140 行)

**业务定位**：方案 CRUD + 客户端绑定查询

**核心方法**:
- `add(PlanDTO)` — 重名校验
- `del(IdQuery)` — 若 plan 已绑定产线 → error 20302
- `mod(PlanDTO)` — 修改后 WebSocket 通知所有相关产线
- `search(PlanQueryDTO)` — 分页或全量
- `getClientBindPlan(LinePlanBindQueryDTO)` — 按 lineId 查绑定方案
- `clientPlan(ClientPlanQueryDTO)` — 客户端查询本机方案

**关键 SQL** (`PlanXml.xml`):
```sql
-- 客户端方案
SELECT a.id, a.name, a.uri, a.description, b.status, a.update_time, a.create_time
FROM plan a, plan_to_line b, line c
WHERE a.id = b.plan_id AND b.line_id = c.id AND c.line_no = #{lineNo} AND c.face_no = #{faceNo}

-- 按产线查绑定
SELECT a.status, b.* FROM plan_to_line a LEFT JOIN plan b ON a.plan_id = b.id
WHERE a.line_id = #{lineId}

-- 方案相关的产线客户端
SELECT client_no FROM plan_to_line a, line b WHERE a.line_id = b.id AND a.plan_id = #{planId}
```

### 4.3 🟡 P1: `StateChangeServiceImpl` (212 行) — 事件驱动核心

**业务定位**：状态变更事件 + 班次级状态统计

**核心方法**:
```java
// @Async @EventListener(StateChangeEvent.class)
public void handleStateChange(StateChangeEvent event)
    └─→ 异步写入 state_change 表（仅当 type 不同时）

// 查询状态统计（含实时计算）
public BaseResult handleStateStatisticSearch(SearchStateStatisticForm form)
    └─→ listDailyStatisticDataBetween() + 实时补算当前班次

// 计算状态统计（核心算法）
public List<StateStatisticPO> getStateStatistics(lineIds, start, end)
    └─→ 状态变更序列差值法（详见 §3.6）

public void removeBefore(LocalDateTime time)
    └─→ DELETE FROM state_change WHERE change_time <= time
```

**⚠️ Bug 注意**: `getStateStatistics` 方法中：
```java
endOfCurrShift = endOfCurrShift.isAfter(LocalDateTime.now()) ? LocalDateTime.now() : startOfCurrShift;
//                                                                     ^^^^^^^^^^^^^^^^^^^
//                                                  BUG: 这里应该是 endOfCurrShift 而不是 startOfCurrShift！
```

### 4.4 🟡 P1: `LineTaskManager` (66 行)

**业务定位**：状态统计定时任务

**关键 cron**:
```java
@Scheduled(cron = "0 1 8,20 * * ?")  // 每日 8:01 / 20:01
public void getStatisticData()        // 写入 state_statistic

@Scheduled(cron = "0 0 2 * * ?")     // 每日 2:00
public void clearExpireStateData()    // 清理 30 天前数据
```

**配置**: `data-retention-time.line-state: 30`

### 4.5 ⚪ P3: `PlanToLineService` (10 行)

```java
@Service
public class PlanToLineService extends ServiceImpl<PlanToLineDAO, PlanToLinePO> {
    // 空实现，全部用 ServiceImpl 默认方法
}
```

### 4.6 ⚪ P3: `LineOrderServiceImpl` (66 行)

**核心方法**:
- `addLineOrder(lineIds)` — 取 max(orderValue) 然后从 +1 开始递增
- `removeByLineId(lineId)` — 删除产线时同步清理
- `modLineOrder(lineOrders)` — 全删全增（order_value 1..N）

### 4.7 ⚪ P3: `StateStatisticServiceImpl` (45 行)

简单 CRUD + `saveStatisticBatch(statistics)` + `listDailyStatisticDataBetween(lineIds, start, end)`。

---

## 5. 数据库交互

### 5.1 涉及表（6 张）

| 表 | 用途 | 字段 | retention |
|---|---|---|---|
| `line` | 产线基本信息 | id/name/lineNo/faceNo/color/clientNo/realtime_data | 无 |
| `plan` | 方案（配方）| id/name/uri/description | 无 |
| `plan_to_line` | 产线-方案绑定 | id/lineId/planId/status | 无 |
| `line_order` | 产线排序 | id/lineId/order_value | 无 |
| `state_change` | 设备状态变更 | id/lineId/type/changeTime | 30 天 |
| `state_statistic` | 班次级状态统计 | id/lineId/statisticTime/okTime/errorTime | 30 天 |

### 5.2 retention 配置

```yaml
data-retention-time:
  line-state: 30  # state_change + state_statistic 保留天数
```

### 5.3 关键 SQL

**`state_statistic` upsert**:
```sql
INSERT INTO state_statistic(line_id, ok_time, error_time, statistic_time)
VALUES (...)
ON CONFLICT(line_id, statistic_time) DO NOTHING
```
**这是 PostgreSQL 语法**，不能直接用于 MySQL。EdgeHost 移植时需要 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE`。

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `LinePO` 表 | line 表 | ✅ W-A12 |
| `LineRegistryService` | 同名 | ✅ W-A12（只做了注册表级别）|
| `lineOrder` 排序 | 集成在 LineRegistry | ✅ W-A12 |

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `LineServiceImpl`（CRUD + bindPlan + switchPlan）| ❌ 没全移植 | 🟡 P1 |
| `PlanServiceImpl`（方案 CRUD）| ❌ 没做 | 🟡 P1（plan 表已有）|
| `StateChangeServiceImpl` + `state_change` 表 | ❌ 没做 | 🟡 P1（V1.19 新表）|
| `StateStatisticServiceImpl` + `state_statistic` 表 | ❌ 没做 | 🟡 P1（V1.19 新表）|
| `LineTaskManager`（8/20 点 cron + 2 点清理）| ❌ 没做 | 🟡 P1 |
| WebSocket 方案变更广播 | ❌ 没做 | 🟢 P2 |

### 6.3 移植建议

**1. plan 模块直接抄**：
- 表结构：`plan` + `plan_to_line` EdgeHost 已有
- 业务逻辑：CRUD + bindPlan + switchPlan
- ⚠️ WebSocket 改 SignalR

**2. state_change 移植**：
- EdgeHost 需要 `state_change` 表（V1.19 新表）
- `@Async @EventListener` → .NET `BackgroundService` 消费 `Channel<StateChangeEvent>`
- `StateChangeServiceImpl.handleStateChange` 直接翻译

**3. state_statistic 算法**：
- 状态变更序列差值法是核心，1:1 翻译
- PostgreSQL `ON CONFLICT` → MySQL `INSERT IGNORE` 或 EF Core `OnConflict`

**4. LineTaskManager**：
- 改写为 .NET `IHostedService` + `Cronos` 库

---

## 7. 风险 / 注意点

### 7.1 state_change 涨库风险

`state_change` 表每条状态变更一条记录，**没有 retention cron 在白天清理**（只有 2 点的清理任务）。
**建议**: W-A22+ 在 detect.TaskManager 增加 1 小时 cron 清理过期 state_change（>24 小时）。

### 7.2 @Async 失效风险

`@Async @EventListener` 需要 `@EnableAsync` 注解（未在反编译中看到，可能在 framework 包）。EdgeHost 移植时确认启用。

### 7.3 state_change 重复记录

`handleStateChange` 中 `record == null || !record.getType().equals(event.getStatus().getValue())` —— 但只查 `changeTime <= event.time` 的最新一条，**不是同一个时间点**。如果短时间内两次状态变更（线上线下震荡），可能漏记。EdgeHost 移植时考虑加去重窗口。

### 7.4 PlanController 部分方法 GET/POST 不一致

`PlanController.del(IdQuery idQuery)` 是 `@DeleteMapping` 但参数是 IdQuery（含 id 字段），实际上要传 `idQuery.id`（不在 URL）。Spring MVC 会用 `@ModelAttribute` 解析，可能需要 query string `?id=1`。

### 7.5 line_order 初始化竞态

`@PostConstruct init()` 在 Spring 启动时检查 line_order 是否为空。如果启动后手动加了 line 但 line_order 没同步（DB 直接 insert line），会导致排序错乱。

### 7.6 planPanel 查询性能

`planPanel()` 在一次请求中查询 4 个 DAO（N+1 风险），单产线 24 小时数据返回 DTO 较大。需要分页或预聚合。

### 7.7 8/20 点 cron 漂移

`getStatisticData()` 用 `LocalTime.now().getHour() == 8`，但是 cron 是 `0 1 8,20 * * ?`（8:01 触发），所以实际是 8:01 触发时 hour=8，进入"昨天 20:00 - 今天 08:00"分支，正确。但若 cron 漂移到 8:00:59 触发，可能 hour 仍是 7，逻辑错误。EdgeHost 移植时直接用参数 `now.hour` 而非 cron 内部判断。

### 7.8 StateChangeServiceImpl BUG

`getStateStatistics` 中：
```java
endOfCurrShift = endOfCurrShift.isAfter(LocalDateTime.now()) ? LocalDateTime.now() : startOfCurrShift;
```
**预期应是**: `endOfCurrShift.isAfter(now) ? now : endOfCurrShift`（保留 endOfCurrShift 当不到 end 时）。当前写法导致 `now < endOfCurrShift` 时返回 `startOfCurrShift`（相当于丢弃数据）。

---

## 8. 总结

line 模块是 PSM 的"产线大脑"，P1 关注点：
1. **`LineServiceImpl.planPanel`**：大屏数据聚合（4 DAO 联合查询）
2. **`LineServiceImpl.bindPlan/switchPlan`**：方案分发 + WebSocket 广播
3. **`StateChangeServiceImpl`**：事件驱动 + 状态序列差值算法
4. **`LineTaskManager`**：8/20 点 + 2 点双 cron

关键移植风险：
- **state_change 涨库**（缺 retention）
- **PG `ON CONFLICT`** → MySQL 改写
- **@Async 失效** → 确认 framework 启用
- **state 序列差值算法** BUG 需修复
- **planPanel N+1** → 性能优化
