# alarm 模块审计报告 (2026-07-24)

## 摘要
- F（功能完全对齐）: **24**
- P（文件存在但实现 stub / 部分逻辑）: **6**
- M（文件缺失 / 功能缺失）: **5**
- 真实对齐度: **~65%**

> 评级口径：F = 字段/方法/逻辑与 PSM 反编译产物逐项一致；P = 类/方法签名存在但实现是 stub 或缺关键分支；M = 文件/接口签名缺失或方法体为空。
> 总数 35 = PSM alarm 包 .java 文件数（不计 DPL 自增 4 个新文件）。

## 文件级判定

### constant 包（5/5 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `AlarmConstants.java` | ✅ F | AlarmConstants.java | 完全一致（仅常量字段） |
| `AlarmLevelEnum.java` | ✅ F | AlarmLevelEnum.java | 完全一致；NORMAL(1) / HIGH(2) |
| `AlarmReasonEnum.java` | ✅ F | AlarmReasonEnum.java | 完全一致；仅 DISCONNECT(1) |
| `AlarmSolvedEnum.java` | ✅ F | AlarmSolvedEnum.java | 完全一致；SOLVED(1)/UNSOLVED(2)/IGNORE(3) |
| `AlarmTypeEnum.java` | ✅ F | AlarmTypeEnum.java | 完全一致；DEFECT/SYSTEM/DEVICE + getByCode/getByConfigKey |

### config 包（1/1 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `DefectAlarmConfig.java` | ✅ F | DefectAlarmConfig.java | 主结构 1:1；**DPL 增 `globalEnabled` 字段（W-X21 紧急关停）**，不属于对齐降级 |

### dto 包（6 F / 3 P / 3 M）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `AlarmCountDTO.java` | ✅ F | AlarmCountDTO.java | 完全一致（count/countTime/level/type） |
| `AlarmCountOfLineDTO.java` | ✅ F | AlarmCountOfLineDTO.java | 完全一致；含 `getKey()` |
| `AlarmDealDTO.java` | ✅ F | AlarmDealDTO.java | 完全一致；`@NotEmpty uuid` |
| `AlarmDTO.java` | ✅ F | AlarmDTO.java | 完全一致；7 字段含 `@Range/@NotNull/@NotEmpty` 校验 |
| `DefectTypeDTO.java` | ✅ F | DefectTypeDTO.java | 完全一致；6 字段 + AddGroup/UpdateGroup |
| `PlaySoundWsMsgDTO.java` | ✅ F | PlaySoundWsMsgDTO.java | 完全一致（uri/playCount） |
| `AlarmNumDTO.java` | 🟡 P | AlarmNumDTO.java | **缺 builder()**；改成默认 0/0 + setX 返回自身；不影响调用方但与 PSM 工厂方法不兼容 |
| `IgnoreAlarmDTO.java` | 🟡 P | IgnoreAlarmDTO.java | **字段集不一致**：DPL 改为 `id + ignoreTime`，PSM 为 `startTime + endTime`；新增 `getIgnoreTimeAsLocalDateTime()`；形参映射需要重新对齐 |
| `AlarmInfoQueryDTO.java` | ❌ M | AlarmInfoQueryDTO.java | **DPL 是空类**；PSM 有 `Integer faceId` 字段并 `extends TimePageQuery` |
| `AlarmQueryDTO.java` | ❌ M | AlarmQueryDTO.java | **DPL 是空类**；PSM 有 type/level/solve/faceId/sortType 5 字段 + TimePageQuery |
| `SearchAlarmDTO.java` | ❌ M | SearchAlarmDTO.java | **DPL 是空类**；PSM 有 type/lineNo/faceNo + 校验注解 |
| `SearchDefectDTO.java` | ❌ M | SearchDefectDTO.java | **DPL 是空类**；PSM 有 name/category + `extends PageQuery` |

### event 包（2/2 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `DealAlarmEvent.java` | ✅ F | DealAlarmEvent.java | 完全一致；lineNo/faceNo/reason + ApplicationEvent |
| `WsConnectListener.java` | ✅ F | WsConnectListener.java | 完全一致；监听 `WsActionEvent` 并触发 `sendAlarmTextMessage()` |

### mapper 包（1 F / 1 P / 1 M）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `DefectTypeMapper.java` | ✅ F | DefectTypeDAO.java | 完全一致（仅 BaseMapper） |
| `IgnoreAlarmMapper.java` | 🟡 P | IgnoreAlarmDAO.java | **缺 `saveOrUpdateBatch(List<IgnoreAlarmPO>)`** 方法及对应 XML 实现 |
| `AlarmRecordMapper.java` | ❌ M | AlarmRecordDAO.java | **缺 5 个查询方法 + 对应 XML 资源**：`selectAlarmCountDay`、`countAlarmCount`、`selectAlarmCountByType`、`selectRecord`、`selectAlarmCount`；DataupLoad `src/main/resources` 下完全无 mapper XML |

> Mapper XML 缺失是严重问题：PSM 反编译产物 `BOOT-INF/classes/com/hikrobotics/solution/module/mapper/AlarmRecordXml.xml` 内含 `row_number() over (partition by ...)`、`group by level/type`、`to_char(time::date)` 等聚合 SQL，DataupLoad 一行都没有。

### entity/model 包（3/3 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `entity/AlarmRecord.java` | ✅ F | model/AlarmRecordPO.java | 完全一致；14 字段 + `buildClientAlarm()` + `getLine()/getKey()`；仅重命名 PO→实体 |
| `model/DefectType.java` | ✅ F | model/DefectTypePO.java | 完全一致；12 字段 |
| `entity/IgnoreAlarm.java` | ✅ F | model/IgnoreAlarmPO.java | 字段 1:1；DPL 额外加 `@TableField` 注解与 `setIgnoreTimeByString()`（不影响 PSM 行为） |

### service 接口包（3/3 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `IAlarmRecordService.java` | ✅ F | IAlarmRecordService.java | 10 方法签名 1:1，仅泛型 PO→实体 |
| `IDefectTypeService.java` | ✅ F | IDefectTypeService.java | 6 方法签名 1:1 |
| `IIgnoreAlarmService.java` | ✅ F | IIgnoreAlarmService.java | 4 方法签名 1:1 |

### service.impl 包（1 F / 2 P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `IgnoreAlarmServiceImpl.java` | ✅ F | IgnoreAlarmServiceImpl.java | 4/4 方法 1:1；`isIgnore / removeExpire / getIgnoreDefect` 与 PSM 等价；**DPL 修复了 PSM `handleAlarmIgnore` 的空 if 块 BUG**（PSM 原版有 `if (CollectionUtils.isNotEmpty(...))` 内空体，DPL 改成正确 `save(IgnoreAlarm)`） |
| `AlarmRecordServiceImpl.java` | 🟡 P | AlarmRecordServiceImpl.java | **核心 4 方法对齐**：`add()` 完整 1:1（含 ReUtil 模板匹配 + 旧 UNSOLVED→IGNORE + save + sendAlarmMessage；**DPL 修复 PSM sendAlarmMessage 中硬编码 `boolean isIgnore = false` BUG（W-B04）**，改调 `ignoreAlarmService.isIgnore()`）；`sendAlarmMessage` 主体对齐；`sendAlarmTextMessage`、`listNotResolveDefectAlarmRecord` 一致；`dealClientAlarm + dealClientAlarmListener` 一致（W-X30b）。**6 方法是 stub**：`listAll / deal / getAlarmListInfo / handleAlarmNumGet / handleAlarmSearch / handleAlarmIgnore` 均返回 `BaseResult.build().ok()` 空壳。`sendAlarmSoundWsMessage` 因 ISystemConfigService 未启用被跳过 |
| `DefectTypeServiceImpl.java` | 🟡 P | DefectTypeServiceImpl.java | 仅 2/6 方法有真实实现：`getByNameAndType`、`listByAttribute`（后者缺 orderByAsc）。**CRUD 4 方法全 stub**：`handleDefectTypeAdd / handleDefectTypeDel / listDefect / editDefect` |

### task 包（1/1 F）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `AlarmTaskManager.java` | ✅ F | AlarmTaskManager.java | 完全一致；`clearAlarmData` + `delExpireIgnoreDefect` 两个 `@Scheduled` cron 与逻辑均相同 |

### web 包（1 F / 1 P）

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| `DefectTypeController.java` | ✅ F | DefectTypeController.java | 完全一致；4 端点 `/web/defect` GET/POST/PUT/DELETE 全对齐（含 `@ApiLog`） |
| `AlarmRecordController.java` | 🟡 P | AlarmRecordController.java | **仅 1/7 端点对齐**：`POST /client/data/alarm` 完整 1:1（**DPL 修复 W-X30 重复推 yk BUG**，删除多余的 `ykService.pushAlarm`）。其余 6 端点缺失：`GET /web/alarm/list`、`GET /web/alarm/num`、`POST /client/data/deal-alarm`、`GET /web/alarm/list-info`、`GET /web/alarm`、`PUT /web/alarm/ignore` |

### DPL 自增文件（不影响对齐度统计，仅记录）

| 文件 | 说明 |
|---|---|
| `service/ClientOnlineChecker.java` | W-F04 新增；60s 心跳超时后通过 `add()` 入口生成 type=3 离线报警 |
| `task/AlarmRetentionTask.java` | W-F01-C 新增；与 AlarmTaskManager.clearAlarmData 重复实现，cron 改成凌晨 3 点、硬编码 90 天 |
| `task/IgnoreExpireTask.java` | W-F01-D 新增；与 AlarmTaskManager.delExpireIgnoreDefect 重复实现，cron 改成每小时，附带 W-X17a PG varchar 比较修复 |
| `web/IgnoreAlarmController.java` | W-F02-B 新增；把 PSM `AlarmRecordController.ignoreAlarm` 拆成独立 Controller（POST/DELETE/GET/GET-check 4 端点） |

## 重点问题 Top 3

### 1. 🔴 AlarmRecordMapper 五个聚合查询方法及对应 XML 资源全部缺失
PSM `AlarmRecordDAO` 定义了 `selectAlarmCountDay / countAlarmCount / selectAlarmCountByType / selectRecord / selectAlarmCount` 5 个查询方法，配套的 `AlarmRecordXml.xml` 内含 `row_number() over (partition by line_no, face_no, defect_name order by id desc)`、`group by level/type`、`to_char(time::date,'yyyy-MM-dd')` 等关键聚合 SQL。DataupLoad `AlarmRecordMapper` 仅 `extends BaseMapper<AlarmRecord>`，`src/main/resources` 下不存在任何 mapper XML。
**直接影响**：`handleAlarmNumGet()` 无法返回真实计数（只能 stub 为 0/0）；`AlarmTaskManager.clearAlarmData` 等场景需要的 delete-by-wrapper 也只能走 BaseMapper。
**修复路径**：从 PSM `BOOT-INF/classes/com/hikrobotics/solution/module/mapper/AlarmRecordXml.xml` 拷贝并改包名；为 Mapper 接口补 5 个 `@Select` 或对应方法签名。

### 2. 🟡 AlarmRecordServiceImpl 的 6 个查询 / 管理方法全是空壳
`listAll / deal / getAlarmListInfo / handleAlarmNumGet / handleAlarmSearch / handleAlarmIgnore` 均返回 `BaseResult.build().ok()`，导致：
- Web 后台列表页（`/web/alarm/list`、`/web/alarm/list-info`）拿不到数据；
- "处理报警"按钮（`POST /client/data/deal-alarm` → `deal()`）失效；
- 大屏告警计数（`/web/alarm/num` → `handleAlarmNumGet()`）始终 0；
- "忽略报警"链路断（PSM 是先 updateBatchById alarm_record 再写 ignore_alarm，DPL 改由 `IIgnoreAlarmService.handleAlarmIgnore` 单独落 ignore_alarm，但 `alarm_record` 的 IGNORE 标记无人处理）。
**核心入口（add + sendAlarmMessage + sendAlarmTextMessage + listNotResolveDefectAlarmRecord + dealClientAlarm）已对齐**，报警能正常进 PG + 推 WS + 推 yk；但管理后台的查询 / 标记功能整体缺失。
**修复路径**：先补 AlarmRecordMapper 的 5 个查询（见问题 1），再把 PSM 6 个方法体逐个迁回。

### 3. 🟡 4 个 DTO 是空类，3 个 DTO 字段不一致 → service.impl 端无 stub 报错但调用方一调就炸
`AlarmInfoQueryDTO / AlarmQueryDTO / SearchAlarmDTO / SearchDefectDTO` 在 DPL 是 `public class XXXDTO {}`，连字段都没有，但 `IAlarmRecordService.listAll / getAlarmListInfo / handleAlarmSearch`、`IDefectTypeService.listDefect` 的形参都引用这些类型。一旦 `AlarmRecordServiceImpl` 真去实现这些方法，会直接撞上编译期能过的 stub，但运行时 `query.getType()` 一调用就 NPE。
`IgnoreAlarmDTO` 字段集被改写（`id + ignoreTime` vs PSM 的 `startTime + endTime`），需要确认 `IgnoreAlarmServiceImpl.handleAlarmIgnore` 的 DTO 字段映射是否覆盖了所有 PSM 端用法（PSM 用 `between(time, startTime, endTime)` 做时间窗，DPL 改用 `ignoreTime` 单点，到期语义被改写）。
**修复路径**：从 PSM 反编译产物里复制 5 个 DTO 的字段 + getter/setter；`IgnoreAlarmDTO` 字段命名回归 PSM（或在 `IgnoreAlarmServiceImpl` 内做兼容映射）。

## 备注
- 整个 alarm 模块的"报警入口链路"（client → /client/data/alarm → AlarmRecordServiceImpl.add → save → sendAlarmMessage → WS + yk + ignore 白名单判定）已 **完全对齐 PSM 且修了 2 个 BUG**（W-B04 sendAlarmMessage 的 `isIgnore` 硬编码 false、W-X30 Controller 重复推 yk）。
- "管理后台"链路（list/deal/handleAlarmIgnore/listDefect/editDefect/DXF CRUD 等）整体 **未实现**。
- 5 个空 DTO 与 AlarmRecordMapper 缺方法是后续补全管理后台的瓶颈。
