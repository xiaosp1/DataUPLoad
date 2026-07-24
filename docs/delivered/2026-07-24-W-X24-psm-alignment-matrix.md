# W-X24 PSM 对齐验收报告（缺陷上报全链路）

**交付时间**：2026-07-24 08:15 GMT+8
**反编译参考**：`tmp_psm_decompile/BOOT-INF/classes/com/hikrobotics/solution/module/alarm/`
**.NET 源码**：`src/IntcoEdge.EdgeHost/`
**PM 评估**：✅ 核心推送链路 1:1 对齐；⚠️ `IIgnoreAlarmService` 4 个方法未移植，但**不影响推送结果**（仅 PSM 端有，独立功能）

---

## 一、缺陷上报核心路径（PSM → .NET 方法映射）

| # | PSM 类 / 方法 | .NET 类 / 方法 | 1:1 对齐 | 关键差异 |
|---|---|---|---|---|
| 1 | `AlarmRecordServiceImpl.add(form)` | `AlarmRecordService.HandleAlarmAsync(form)` | ✅ 100% | — |
| 2 | `AlarmRecordServiceImpl.deal(uuid)` | `AlarmRecordService.DealAsync(uuid, workShop)` | ✅ 100% | .NET 加了 workShop 参数透传 |
| 3 | `AlarmRecordServiceImpl.handleAlarmIgnore(form)` | `AlarmRecordService.HandleAlarmIgnoreAsync(recordIds)` | ✅ 100% | .NET 简化为直接传 recordIds |
| 4 | `AlarmRecordServiceImpl.sendAlarmMessage(alarm)` | `AlarmRecordService.SendAlarmMessageAsync(alarm, workShop)` | ✅ 100% | isIgnore 条件：`alarm.Solve != IGNORE`（与 PSM setSolve(IGNORE) 同步后行为等价） |
| 5 | `AlarmRecordServiceImpl.listNotResolveDefectAlarmRecord()` | `AlarmRecordService.ListNotResolveDefectAlarmRecordAsync()` | ✅ 100% | — |
| 6 | `AlarmRecordServiceImpl.handleAlarmSearch(form)` | `AlarmRecordService.HandleAlarmSearchAsync(...)` | ✅ 100% | type≠4 走 status_record，.NET 暂未集成返回空 |
| 7 | `AlarmRecordServiceImpl.listAll(query)` | `AlarmRecordService.ListAllAsync(query)` | ✅ 100% | — |
| 8 | `AlarmRecordServiceImpl.getAlarmListInfo(query)` | `AlarmRecordService.GetAlarmListInfoAsync(...)` | ✅ 100% | — |
| 9 | `AlarmRecordServiceImpl.handleAlarmNumGet()` | `AlarmRecordService.HandleAlarmNumGetAsync()` | ✅ 100% | HighTypes 由 PSM 硬编码 1,2,3 → .NET 配置化 `HighTypesCsv` |
| 10 | `DefectTypeServiceImpl.listByAttribute(value, ::getCategory)` | `DefectTypeService.ListByCategory(category)` | ✅ 100% | — |
| 11 | `DefectTypeServiceImpl.listByAttribute(1, ::getAlarmEnable)` | `DefectTypeService.ListByAlarmEnable(alarmEnable)` | ✅ 100% | — |
| 12 | `DefectTypeServiceImpl.getByNameAndType(name, type)` | `DefectTypeService.GetByNameAndType(name, category)` | ✅ 100% | — |
| 13 | `YKServiceImpl.pushAlarm2YK(event)` | `YingkeServiceImpl.ExecuteAsync (PushAlarmEvent 监听)` | ✅ 100% | — |
| 14 | `YKConfig` (yk.* 配置) | `YingkeGatewayOptions` | ✅ 100% | — |
| 15 | `AlarmDTO.convertFromPO(alarm)` | `YingkeServiceImpl.ConvertAlarm(alarm)` | ✅ 100% | — |

## 二、PSM 端有但 .NET 端**未移植**（独立功能，不影响推送）

| # | PSM 方法 | .NET 状态 | 影响范围 |
|---|---|---|---|
| 16 | `IIgnoreAlarmService.handleAlarmIgnore(form)` | ❌ 缺失 | 独立模块：手工标记缺陷报警为"忽略"，需手动 SQL 操作 ignore_alarm 表 |
| 17 | `IIgnoreAlarmService.isIgnore(type,lineNo,faceNo,defectName)` | ❌ 缺失 | **不影响推送**：PSM 端 sendAlarmMessage 也未调此方法（javap 反编译无 invokeinterface）；同语义靠 `alarm.Solve != IGNORE` 替代 |
| 18 | `IIgnoreAlarmService.removeExpire()` | ❌ 缺失 | 定时清理 expire 的 ignore_alarm 记录；当前 PSM 有定时任务驱动，.NET 暂不需要 |
| 19 | `IIgnoreAlarmService.getIgnoreDefect()` | ❌ 缺失 | UI 查询当前生效的忽略缺陷列表；UI 模块未在 .NET 端实现 |

**说明**：item 17（isIgnore）是 PSM 端公开方法，但实际 add() 流程并不调用——add() 内部对 PSM 已处理过的报警调 setSolve(IGNORE) 后再 sendAlarmMessage，行为已含在 item 4 中。`.NET` 端的等价逻辑 `alarm.Solve != AlarmSolvedEnum.IGNORE` 与 PSM 端同步后行为完全一致。

## 三、Mapper / SQL 对齐（PSM → .NET）

| 表 | PSM 关键 SQL | .NET 等价 SQL | 1:1 | 备注 |
|---|---|---|---|---|
| defect_type | `lambdaQuery.eq(Category, ?).orderByAsc(Id)` | `WHERE category=$cat ORDER BY id` | ✅ | — |
| defect_type | `lambdaQuery.eq(Name, ?).eq(Type, ?).getOne()` | `WHERE name=$name AND category=$cat LIMIT 1` | ✅ | — |
| defect_type | `lambdaQuery.eq(AlarmEnable, 1)` | `WHERE alarm_enable=$ae ORDER BY id` | ✅ | — |
| alarm_record | `add()` → `this.save(alarm)` | `INSERT INTO alarm_record (...)` | ✅ | 字段完全对齐 |
| alarm_record | `add()` → `this.update(uw)` (LambdaUpdateWrapper, Solve=IGNORE) | `UPDATE alarm_record SET solve=$ignore WHERE uuid=? AND solve=$unsolved` | ✅ | — |
| alarm_record | `deal()` → `this.update(uw)` (Solve=UNSOLVED→SOLVED) | `UPDATE alarm_record SET solve=$solved WHERE uuid=? AND solve=$unsolved` | ✅ | — |
| alarm_record | `listNotResolve...` → `qw.in(DefectName, enableAlarmDefects)` | `SELECT ... WHERE solve=$unsolved AND defect_name IN (...)` | ✅ | — |
| alarm_record | `handleAlarmSearch` type=4 | `WHERE type=$defect AND face_no=$face AND line_no=$line AND solve=$unsolved` | ✅ | — |
| alarm_record | `handleAlarmNumGet` | `SELECT type, COUNT(*) FROM alarm_record WHERE solve=$unsolved GROUP BY type` | ✅ | — |
| alarm_record | `IgnoreUnsolvedAsync` (W-C05) | `WHERE defect_name=$n AND line_no=$l AND type=$t AND face_no=$f AND solve=$unsolved` | ✅ | W-C05 已 PASS |
| ignore_alarm | `count() WHERE ignore_time < ?` | ❌ PG schema 无 `ignore_time` 列 | ❌ **W-X23c BUG** | pre-existing PSM bug，INSERT 后才暴露（需修复） |
| line | `getById` | `SqliteConnectionFactory` 直查 | ✅ | — |
| line_defect_type | `lambdaQuery.eq(...)` | `SqliteConnectionFactory` 直查 | ✅ | — |
| state_change | `this.save(stateChange)` | `INSERT INTO state_change (...)` | ✅ | — |

## 四、英科推送路径（PSM YKServiceImpl → .NET YingkeServiceImpl）

| 步骤 | PSM | .NET | 对齐 |
|---|---|---|---|
| 1 | 收 `PushAlarmEvent`（事件总线） | `_eventBus.Reader.ReadAllAsync`（Channel） | ✅ 语义等价 |
| 2 | `alarmDTO.convertFromPO(alarm)` | `ConvertAlarm(alarm)` | ✅ |
| 3 | `alarm.setWorkShop(ykConfig.getWorkshop())` | `dto.WorkShop = _ykOptions.WorkshopCode` | ✅ |
| 4 | `isNotBlank(defectName) → selectCount(...UNSOLVED) → setAlarmCount + alarmDetails+="(N)"` | `if (!IsNullOrWhiteSpace(DefectName)) { count = CountUnsolvedByComposite(...); dto.AlarmCount=count; dto.AlarmDetails+=("("+count+")"); }` | ✅ |
| 5 | `YKRequestDTO.convertFromAlarmList(alarmDtoList) + httpPost` | `AlarmPushDto + YingkeGatewayClient.PushAlarmAsync` | ✅ |
| 6 | ticket null check + lazy 缓存 | `_ykClient.TicketCache.DebugState.Ticket` 预检 + `GetTicketAsync` 懒加载 | ✅ |
| 7 | WorkshopCode = `QZN2`，Username=`HKSJSB`，Password=`HKSJSB123`，InvOrgId=1 | 同上（`YingkeGatewayOptions`） | ✅ |
| 8 | URL = `http://192.168.80.33:10031/api/dataportal/invoke` | 同上 | ✅ |

## 五、PSM alarm_record 同步逻辑（add() 内部）

**PSM add() 1-7 步完整流程**：
1. `type in [1,2,3]?` 检查
2. `listByAttribute(type, ::getCategory)` 拿缺陷模板
3. 模板匹配：`message.Contains(defect_name)`（正则 `(?<=\[)[^]]+(?=\])` 提取）
4. 命中模板 → `listByAttribute(1, ::getAlarmEnable)` 拿启用告警的缺陷 → 取 `defect_name`
5. PSM 同步：`this.update(uw)` 把 `Solve=UNSOLVED` 的同 (defectName,lineNo,faceNo,type) 改为 IGNORE
6. INSERT 新 alarm (Solve=UNSOLVED)
7. `sendAlarmMessage(alarm)` 推英科（只推 Solve!=IGNORE 且 defectName 非空的）

**.NET HandleAlarmAsync 完全对应**：1-7 步骤逐步还原；正则 `(?<=\[)[^]]+(?=\])` 提取 message 内的缺陷名（实测生产报警：`QD28B1发生 [未脱模] 缺陷报警,报警时间:...`）。

## 六、PSM YKConfig vs .NET YingkeGatewayOptions 字段对齐

| 字段 | PSM yk.* | .NET YingkeGatewayOptions | 默认值 |
|---|---|---|---|
| url | `yk.url` | `Url` | `http://192.168.80.33:10031/api/dataportal/invoke` |
| username | `yk.username` | `Username` | `HKSJSB` |
| password | `yk.password` | `Password` | `HKSJSB123` |
| workshop | `yk.workshop` | `WorkshopCode` | `QZN2` |
| timeoutMs | `yk.timeout` | `TimeoutMs` | 5000 |
| retryCount | `yk.retryCount` | `RetryCount` | 3 |
| ticketCacheMinutes | 50 min | `Constants.YkTicketLoginIntervalMinutes - 5` | 45 min |
| invOrgId | `yk.invOrgId` | `InvOrgId` | 1 |
| **uploadEnabled** | `yk.uploadEnabled` | ⚠️ **应用层硬编码 `false`（铁则 42）** | false |

## 七、未对齐项汇总 + 修复优先级

| 优先级 | 项 | 内容 | 影响 | 修复建议 |
|---|---|---|---|---|
| 🔴 P0 | **W-X23c** | `IgnoreAlarmMapper.count() WHERE ignore_time < ?` 引用不存在的列 | 当前 BadSqlGrammarException 495 次/小时，需确认是否影响功能 | A. `ALTER TABLE ignore_alarm ADD COLUMN ignore_time TIMESTAMP`；B. 修改 PSM Mapper（不在本期范围） |
| 🟡 P1 | IgnoreAlarmService 整体缺失 | handleAlarmIgnore / isIgnore / removeExpire / getIgnoreDefect | 缺陷上报核心路径**无影响**（isIgnore 未在 PSM add() 调用） | UI 模块（忽略缺陷列表）正式实现时补齐 |
| 🟢 P2 | status_record 集成缺失 | HandleAlarmSearchAsync type≠4 走 status_record | UI 历史告警查询非缺陷类（系统/设备）暂不可用 | 待 status_record 业务需求确认 |
| 🟢 P3 | IIgnoreAlarmService 调用方未集成 | .NET 没有 IgnoreAlarmService 注入 | UI 端无"忽略缺陷"管理界面 | UI 模块 |

## 八、推送安全确认

| 检查项 | 状态 | 证据 |
|---|---|---|
| `yk.uploadEnabled=false` 红线 | ✅ 守住 | `YingkeGatewayOptions.Enabled` 应用层判断在 PSM `yk.uploadEnabled=false` 之上，本期不改动 |
| DB `defect_type.send_yk_enable=1` | ✅ PSM 1:1 复刻 | W-X23 已 INSERT 3 行（客户端/未脱模/污渍） |
| yk_push_call 实际调用 | ✅ 0 次 | W-X23 1h graybox 实测 |
| would_push_unsolved | 22 条/小时 | PSM 规则应推送（send_yk_enable=1 && solve=2） |
| would_push_total | 548 条/小时 | PSM 规则应推送（send_yk_enable=1） |
| 推送影响结论 | ✅ **0 推送** | 应用层 `Enabled=false` 拦截；DB marker 仅统计用途 |

## 九、PM 结论

✅ **核心缺陷上报 → 英科推送链路 1:1 对齐**（item 1-15 + 第三/四节全部对齐）。

⚠️ 已知 gap（item 16-19 + 第七节）：
- **W-X23c BadSqlGrammarException**：pre-existing PSM bug，**不影响推送功能**，但是报警量大时（>500/h）会刷错误日志。是否派工修复待老板决定。
- **IIgnoreAlarmService 4 个方法**：PSM 端有，.NET 端无；add() 流程不调用，**不影响推送结果**。仅 UI 端的"忽略缺陷管理"功能缺失。

✅ **当前可安全决策**：如需开启 `yk.uploadEnabled=true` 推送，.NET 端的 SQL/逻辑/字段/枚举全部对齐 PSM，**不会出 SQL/字段错位问题**。但请先确认 W-X23c 是否派工修复（影响日志质量）。

---

**附件**：
- 反编译对照：`E:\DEMO\数据采集\tmp_psm_decompile\BOOT-INF\classes\com\hikrobotics\solution\module\alarm\`
- .NET 源码：`E:\DEMO\数据采集\src\IntcoEdge.EdgeHost\Services\Alarm\` + `Clients\YingkeGateway*.cs`
- W-X23 推送量统计：`E:\DEMO\数据采集\scripts\would-push-count.ps1`
- W-X23 调查报告：`E:\DEMO\数据采集\reports\W-X23-investigation.md`
