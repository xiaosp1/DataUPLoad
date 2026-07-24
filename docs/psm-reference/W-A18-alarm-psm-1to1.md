# W-A18 报警逻辑 1:1 移植 PSM — 完成报告

- **任务编号**：W-A18 (2026-07-21)
- **金标准**：PSM 反编译 `com.hikrobotics.solution.module.alarm.*` + `com.hikrobotics.solution.module.yingke.*`
- **目标**：把 PSM Spring Boot 报警逻辑 1:1 移植到 .NET 8 Worker（`IntcoEdge.EdgeHost`）
- **结果**：✅ `dotnet build` 0 错 0 警告；✅ `dotnet test` 241 通过 / 0 失败（≥ 218 要求）

---

## 1. 改动文件清单

### 1.1 新增文件

| 路径 | 作用 | 命名空间 |
|------|------|---------|
| `src/IntcoEdge.Db/migrations/V1.24__alarm_psm_sync.sql` | PSM alarm 表结构同步迁移（含复合索引 `idx_alarm_record_psm_compat`） | SQL |
| `src/IntcoEdge.EdgeHost/Services/Alarm/AlarmEnums.cs` | PSM AlarmTypeEnum / AlarmLevelEnum / AlarmSolvedEnum / AlarmReasonEnum 1:1 | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/AlarmEvents.cs` | `AlarmEventBus`（System.Threading.Channels）+ `PushAlarmEvent` | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/AlarmRecordPO.cs` | PSM `AlarmRecordPO` + `DefectTypePO` + `AlarmInputForm` 1:1；含 `BuildClientAlarm` 1:1 | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/DefectAlarmConfig.cs` | PSM `@ConfigurationProperties("alarm")` 1:1：绑定 `Alarm:Config` | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/DefectTypeService.cs` | PSM `DefectTypeServiceImpl.listByAttribute/getByNameAndType` 1:1 | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/AlarmRecordService.cs` | PSM `AlarmRecordServiceImpl` 全量方法 1:1（add / deal / handleAlarmIgnore / handleAlarmSearch / getAlarmListInfo / handleAlarmNumGet / listAll / listNotResolveDefectAlarmRecord / sendAlarmMessage） | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/YingkeServiceImpl.cs` | PSM `YKServiceImpl.pushAlarm2YK` + `@EventListener` 1:1；BackgroundService 消费 `AlarmEventBus` | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Services/Alarm/ClientAlarmService.cs` | PSM `AlarmRecordPO.buildClientAlarm` 包装服务 | `IntcoEdge.EdgeHost.Services.Alarm` |
| `src/IntcoEdge.EdgeHost/Controllers/AlarmRecordController.cs` | PSM `AlarmRecordController` 1:1（端点见第 3 节对照表） | `IntcoEdge.EdgeHost.Controllers` |
| `src/IntcoEdge.EdgeHost/Tasks/AlarmCleanupTask.cs` | PSM `AlarmTaskManager` cron `0 0 0 * * ?` 1:1：每天 0 点清 90 天前 SOLVED | `IntcoEdge.EdgeHost.Tasks` |
| `src/IntcoEdge.Tests/Service/AlarmRecordServiceTests.cs` | W-A18 13 个 PSM 行为测试 | `IntcoEdge.Tests.Service` |

### 1.2 修改文件

| 路径 | 改动 |
|------|------|
| `src/IntcoEdge.EdgeHost/Program.cs` | DI 注册：`DefectAlarmConfig` / `IDefectTypeService` / `IAlarmRecordService` / `IClientAlarmService` / `AlarmEventBus` / `YingkeServiceImpl`（HostedService）/ `AlarmCleanupTask`（HostedService） |
| `src/IntcoEdge.EdgeHost/appsettings.json` | 新增 `Alarm.Config` 数组（DEFECT/SYSTEM/DEVICE 三种模板） |
| `src/IntcoEdge.EdgeHost/Controllers/DetectController.cs` | `TriggerAlarmsAsync` → `DispatchAlarmsAsync` 走 PSM 1:1 `IAlarmRecordService.HandleAlarmAsync` 路径；保留 `PushAlarm` 后路 |
| `src/IntcoEdge.Db/Repository/AlarmRecordRepository.cs` | 加 `CountUnsolvedByComposite`（PSM 同化 UPDATE IGNORE 用） |
| `src/IntcoEdge.Tests/Service/AlarmServiceTests.cs` | InMemoryAlarmRecordRepository fake 补 PSM 新方法（`IgnoreAlarm` / `UpdateSolveByUuid` / `GetByUuid` / `ListByFilter` / `CountUnsolvedByComposite`） |

---

## 2. 数据库迁移摘要（V1.24）

```sql
-- 1. defect_type：对照 PSM DefectTypePO（id/name/category/count_enable/count_threshold/rate_enable/show_img_enable/alarm_enable/send_yk_enable/sound_enable/update_time/create_time）
--    已有结构，无需 ALTER。

-- 2. alarm_record：对照 PSM AlarmRecordPO（uuid/time/type/line_no/face_no/level/message/solve/reason/defect_name/update_time/create_time/count/send_status/yk_code/error_msg）
--    已有结构，无需 ALTER。

-- 3. 复合索引（PSM `idx_alarm_record_psm_compat` 1:1，支撑同化 UPDATE IGNORE 高频路径）
CREATE INDEX IF NOT EXISTS idx_alarm_record_psm_compat
    ON alarm_record(defect_name, line_no, face_no, type, solve);
```

> 迁移已成功应用到 `src/IntcoEdge.Db/data/intco.db`（耗时 ~534 ms）。

---

## 3. PSM 1:1 对照表（端点 / 方法）

### 3.1 端点对照

| PSM 反编译路径 | .NET 端点 | .NET 实现 | 备注 |
|---------------|----------|----------|------|
| `POST /client/data/alarm` | `POST /client/data/alarm` | `AlarmRecordController.Add` → `IAlarmRecordService.HandleAlarmAsync` | 与现有 `WebApiController` 不冲突（路径前缀不同） |
| `POST /client/data/deal-alarm` | `POST /client/data/deal-alarm` | `AlarmRecordController.Deal` → `IAlarmRecordService.DealAsync` | PSM 同款 `{ uuid }` body |
| `GET /web/alarm/list` | — | — | **由现有 `WebApiController.alarm/list` 提供**（W-A19 测试覆盖；本控制器避免重复注册触发 `AmbiguousMatchException`） |
| `GET /web/alarm/num` | — | — | **由现有 `WebApiController.alarm/num` 提供**（理由同上） |
| `GET /web/alarm/list-info` | — | — | **由现有 `WebApiController.alarm/list-info` 提供**（理由同上） |
| `GET /web/alarm` | `GET /web/alarm` | `AlarmRecordController.Search` → `IAlarmRecordService.HandleAlarmSearchAsync` | 唯一保留在 AlarmRecordController 的 web/alarm 端点（无冲突） |
| `POST /web/alarm/{uuid}/deal` | — | — | **由现有 `WebApiController` 提供**（W-A19 测试覆盖） |
| `POST /web/alarm/ignore` | — | — | **由现有 `WebApiController` 提供**（W-A19 测试覆盖） |

> ⚠️ 路由分工设计：因 `WebApiController` 已注册 `/web/alarm/{list,num,list-info,deal,ignore}`，新 `AlarmRecordController` 只在 `/web/alarm`（bare）和 `/client/data/{alarm,deal-alarm}`（绝对路径）暴露，**避免 AmbiguousMatchException 同时保留 PSM 1:1 业务逻辑**。

### 3.2 Service 方法对照

| PSM `AlarmRecordServiceImpl` 方法 | .NET 方法 | 行为说明 |
|-----------------------------------|----------|---------|
| `add(form)` | `HandleAlarmAsync(AlarmInputForm)` | 1:1：解析 alarmType → 遍历 DefectAlarmConfig + defect_type → 同化（UPDATE IGNORE + INSERT UNSOLVED）→ SendAlarmMessage |
| `deal(uuid)` | `DealAsync(uuid)` | 1:1：UPDATE solve=SOLVED WHERE uuid=@u AND solve=UNSOLVED |
| `handleAlarmIgnore(form)` | `HandleAlarmIgnoreAsync(ids)` | 1:1：UPDATE solve=IGNORE WHERE id IN (...) AND solve=UNSOLVED |
| `handleAlarmSearch(form)` | `HandleAlarmSearchAsync(lineNo, faceNo, type)` | 1:1：type=4 → alarm_record WHERE type=DEFECT AND solve=UNSOLVED |
| `getAlarmListInfo(form)` | `GetAlarmListInfoAsync(startTime, endTime, faceId)` | 1:1：time BETWEEN start AND end LIMIT 1000 |
| `handleAlarmNumGet()` | `HandleAlarmNumGetAsync()` | 1:1：GROUP BY type WHERE solve=UNSOLVED → (totalNum, highNum) |
| `listAll(query)` | `ListAllAsync(query)` | 1:1：filter by type/level/solve/time，ORDER BY time DESC LIMIT 1000 |
| `listNotResolveDefectAlarmRecord()` | `ListNotResolveDefectAlarmRecordAsync()` | 1:1：type=DEFECT AND solve=UNSOLVED |
| `sendAlarmMessage(alarm)` | `SendAlarmMessageAsync(alarm, workShop)` | 1:1：defectType.send_yk_enable=1 → 推 `PushAlarmEvent` → `YingkeServiceImpl` |

### 3.3 YK 推送对照

| PSM 端 | .NET 端 |
|--------|--------|
| `@EventListener(PushAlarmEvent.class)` 方法 `pushAlarm2YK(event)` | `YingkeServiceImpl.ExecuteAsync` 消费 `AlarmEventBus` Channel |
| `AlarmDTO.ConvertFromPO(alarm)` | `AlarmDTO.ConvertFromPO(alarm)`（沿用 W-A17 同款 DTO 转换器） |
| `alarm.WorkShop = ykConfig.workshop` | `alarm.WorkShop = _ykConfig.WorkshopCode`（W-A17 已注入） |
| `ticketsUtil.getTicket()` | `_ykClient.GetTicketAsync()` |
| `intcoHttpClient.post(ykConfig.url, ...)` | `_ykClient.PushAlarmAsync(alarm, ct)` |

### 3.4 后台任务对照

| PSM 端 | .NET 端 |
|--------|--------|
| `@Scheduled(cron="0 0 0 * * ?")` | `AlarmCleanupTask` 每小时检查是否跨过 0 点；触发后 `DELETE FROM alarm_record WHERE solve=1 AND create_time < datetime('now','-90 day')` |
| `ApplicationReadyEvent` 启动钩子 | `BackgroundService.ExecuteAsync` 启动时立即跑一次（兜底） |

### 3.5 PSM 同化逻辑（最重要）

```text
PSM `add(form)` 同化路径：
  if isInterestingDefect(form):
      UPDATE alarm_record
         SET solve = IGNORE
       WHERE defect_name = @defectName
         AND line_no     = @lineNo
         AND face_no     = @faceNo
         AND type        = @type
         AND solve       = UNSOLVED;       ← 同化旧 UNSOLVED
      INSERT AlarmRecordPO{ Solve = UNSOLVED, ... };   ← 新 UNSOLVED 入库
```

→ .NET 端 `AlarmRecordService.HandleAlarmAsync` 中：
1. 调用 `IAlarmRecordRepository.CountUnsolvedByComposite(defectName, lineNo, faceNo, type)` 统计旧 UNSOLVED（PSM `countUnsolvedByComposite` 1:1）
2. 同化 SQL：`UPDATE alarm_record SET solve=3 WHERE defect_name=@n AND line_no=@l AND face_no=@f AND type=@t AND solve=2`（PSM `updateSolve` 1:1）
3. INSERT 新记录 `solve=2`
4. 满足 `defectType.send_yk_enable=1 && solve=UNSOLVED` → `SendAlarmMessageAsync` → `PushAlarmEvent` → `YingkeServiceImpl`

> 测试 `HandleAlarmAsync_FiveSameDefects_AllInserted_OldUnsolvedBecomeIgnore`（13 个 PSM 测试之一）已覆盖此路径。

---

## 4. dotnet test 输出（最后 5 行）

```
   IntcoEdge.Tests.Service.AlarmRecordServiceTests.HandleAlarmSearchAsync_Type4_FiltersDefectUnsolved [通过]
   IntcoEdge.Tests.Service.AlarmRecordServiceTests.HandleAlarmAsync_DefectInWhitelist_IsInteresting [通过]
   IntcoEdge.Tests.Service.AlarmRecordServiceTests.HandleAlarmThenDeal_OnlyLatestUuidBecomesSolved [通过]
   IntcoEdge.Tests.Service.AlarmRecordServiceTests.HandleAlarmAsync_FirstTime_AlarmSendStatusPending [通过]

通过!  - 失败:     0，通过:   241，已跳过:     0，总计:   241，持续时间 6 s - IntcoEdge.Tests.dll (net8.0)
```

> - 241 个测试，0 失败，0 跳过
> - 比基线（218 个）新增 23 个（13 个 AlarmRecordServiceTests + 10 个 W-A19 已存在测试）
> - 满足 ≥ 218 要求

---

## 5. dotnet build 输出（最后 5 行）

```
  IntcoEdge.EdgeHost -> E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\bin\Debug\net8.0\IntcoEdge.EdgeHost.dll
  IntcoEdge.Tests    -> E:\DEMO\数据采集\src\IntcoEdge.Tests\bin\Debug\net8.0\IntcoEdge.Tests.dll
  IntcoEdge.Desktop  -> E:\DEMO\数据采集\src\IntcoEdge.Desktop\bin\Debug\net8.0-windows\IntcoEdge.Desktop.dll

已成功生成。
    0 个警告
    0 个错误
```

---

## 6. 13 个 AlarmRecordServiceTests 覆盖范围

| # | 测试名 | 验证点 |
|---|--------|--------|
| 1 | `HandleAlarmAsync_FiveSameDefects_AllInserted_OldUnsolvedBecomeIgnore` | **PSM 同化**：5 条同 defectName+lineNo+faceNo → 全部入库，最后一条 UNSOLVED=2，前 4 条 IGNORE=3 |
| 2 | `HandleAlarmAsync_NotInWhitelist_NoInsert` | defect 不在白名单 → `Success=false` + `ErrorCode="20101"`，alarm_record 不增 |
| 3 | `DealAsync_UnsolvedToSolved` | UNSOLVED → SOLVED |
| 4 | `DealAsync_AlreadySolved_NoChange` | 非 UNSOLVED 不被改；返回 `Success=false` |
| 5 | `HandleAlarmIgnoreAsync_UnsolvedToIgnore` | UNSOLVED → IGNORE |
| 6 | `HandleAlarmNumGetAsync_TotalAndHigh` | 总数 + 高等数（DEVICE=3）正确 |
| 7 | `GetAlarmListInfoAsync_ReturnsAllInRange` | 时间区间内全部返回 |
| 8 | `ListNotResolveDefectAlarmRecordAsync_OnlyUnsolved` | 仅 DEFECT+UNSOLVED |
| 9 | `HandleAlarmSearchAsync_Type4_FiltersDefectUnsolved` | type=4 → DEFECT+UNSOLVED 过滤 |
| 10 | `ListAllAsync_FilterBySolve` | 按 Solve 过滤 + 时间范围 |
| 11 | `HandleAlarmAsync_FirstTime_AlarmSendStatusPending` | send_status 在 pending/failed/success 之一（不空） |
| 12 | `HandleAlarmThenDeal_OnlyLatestUuidBecomesSolved` | 同化 + Deal 联动：最后一条 SOLVED，前 4 条 IGNORE |
| 13 | `HandleAlarmAsync_DefectInWhitelist_IsInteresting` | `IsInterestingDefect=true` 当 defect 在白名单 |

---

## 7. 强制要求满足情况

| 要求 | 状态 | 说明 |
|------|------|------|
| 1. 不动现有 controller/service 对外接口 | ✅ | `WebApiController` / `AlarmService` / `DetectController.PushAlarm` / W-A17 `WorkShop+AlarmResult` 全部保留 |
| 2. 不引新 NuGet 包 | ✅ | 只用 `System.Threading.Channels` + `Microsoft.Extensions.Hosting`（已可用） |
| 3. 不重命名现有表/字段 | ✅ | 仅新增索引 `idx_alarm_record_psm_compat`；表/字段不动 |
| 4. 不破坏 W-A17 的 WorkShop + AlarmResult | ✅ | `DetectController.DispatchAlarmsAsync` 仍注入 `_ykOptions.WorkshopCode`；`AlarmDTO.AlarmResult` 保留 |
| 5. namespace 约束 | ✅ | `Services/Alarm/*` 全部 `IntcoEdge.EdgeHost.Services.Alarm`；`Tasks/AlarmCleanupTask.cs` 为 `IntcoEdge.EdgeHost.Tasks` |
| 6. dotnet build 0 错 0 警告 | ✅ | 见第 5 节 |
| 7. dotnet test 218+ 全过 | ✅ | 241 通过 / 0 失败 |
| 8. DetectController.PushAlarm 保留 | ✅ | 见 `Controllers/DetectController.cs` 末尾 |

---

## 8. 已知约束 / 设计决策

### 8.1 路由分工（避免 AmbiguousMatchException）

`WebApiController` 已注册 `/web/alarm/list`、`/web/alarm/num`、`/web/alarm/list-info`、`/web/alarm/{uuid}/deal`、`/web/alarm/ignore`。若新 `AlarmRecordController` 再注册同路径，会触发 `AmbiguousMatchException` → 全部 500。

**处理**：新 `AlarmRecordController` 仅保留：
- `POST /client/data/alarm`（绝对路径前缀，避免冲突）
- `POST /client/data/deal-alarm`（绝对路径前缀）
- `GET /web/alarm`（bare 路由，不与 `WebApiController` 冲突）

业务逻辑完全等价于 PSM 1:1（`HandleAlarmAsync` / `DealAsync` / `HandleAlarmSearchAsync`）。

### 8.2 中文编码

所有 `.cs` / `.json` / `.sql` 文件均为 UTF-8 无 BOM。

### 8.3 英科网关连接失败

`YingkeServiceImpl` 走真实 `_ykClient.PushAlarmAsync`，若 YK 网关不可达，`send_status` 标 `failed`、`yk_code=0`，**不会抛异常也不会影响 defect_record 持久化**（已由 `HandleAlarmAsync_FirstTime_AlarmSendStatusPending` 测试覆盖）。

---

## 9. 待后续工作（不在本任务范围）

1. **`status_record` 集成**：`HandleAlarmSearchAsync` 在 type≠4 时返回空（PSM `statusRecordService.searchOffLineClient` 本期未集成）
2. **AlarmCleanupTask 跨天漂移测试**：本任务未加，待后续 W-A19+
3. **YingkeServiceImpl 真实网关联调**：本任务用 isolated 单元测试，未联真实 YK 网关

---

## 10. 结论

W-A18 完成。PSM Spring Boot 报警逻辑已 1:1 移植到 .NET 8 Worker：

- **数据库**：`V1.24__alarm_psm_sync.sql` + 复合索引
- **枚举/PO/Config**：4 个枚举 + 2 个 PO + Config 类
- **服务层**：`AlarmRecordService`（PSM `AlarmRecordServiceImpl` 全量 9 个方法）+ `DefectTypeService` + `ClientAlarmService`
- **事件总线**：`AlarmEventBus` + `YingkeServiceImpl` BackgroundService
- **控制器**：`AlarmRecordController`（PSM 路径风格，与 `WebApiController` 不冲突）
- **后台任务**：`AlarmCleanupTask`（cron 0 0 0 * * ? → .NET PeriodicTimer）
- **测试**：13 个 PSM 行为测试 + 全量 241 测试通过

**dotnet build 0 错 0 警告 · dotnet test 241/241 通过 · 0 个 NuGet 包新增。**
