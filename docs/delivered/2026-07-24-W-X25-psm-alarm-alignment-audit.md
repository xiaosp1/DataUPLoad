# W-X25 PSM alarm 模块对齐审计（DataupLoad vs PSM 反编译原版）

| 项 | 值 |
|---|---|
| 工单号 | W-X25 |
| 工单类型 | 审计（仅报告 / 不改代码 / 不改 yml / 不重启） |
| 优先级 | 🟡 P1 |
| 派工人 | PM 锋卫 |
| 执行人 | Codex Worker (PSM 对齐审计) |
| 执行时间 | 2026-07-24 |
| 反编译参考 | `docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/alarm/` |
| DataupLoad 源码 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/` |
| 上一份相关审计 | W-X16（筛选/过滤能力） / W-X23c（ignore_alarm schema 修复） / W-X24（推送链对齐） |

---

## 0. 一句话回答

> **核心报警入口链 (`/client/data/alarm` → `add()` → `sendAlarmMessage()` → `PushAlarmEvent` → yk) 与 PSM 反编译产物 1:1 对齐，并修掉两处 PSM BUG（`isIgnore` 硬编码 false / `startTime-endTime` DTO 语义不一致）。**
>
> **但 web 后台管理端（`/web/alarm/list` / `num` / `list-info` / `search` / `ignore` / `deal-alarm`）整套仍是占位返回 `BaseResult.ok()`；前台/运维只能看到数据，看不到 CRUD——这是 DataupLoad 当前最大的对齐缺口。**

---

## 1. 文件清单对齐

### 1.1 PSM 反编译 alarm 模块（34 个 .java）

```
config/DefectAlarmConfig.java
constant/AlarmConstants.java
constant/AlarmLevelEnum.java
constant/AlarmReasonEnum.java
constant/AlarmSolvedEnum.java
constant/AlarmTypeEnum.java
dto/AlarmCountDTO.java                dto/AlarmInfoQueryDTO.java      dto/AlarmQueryDTO.java           dto/PlaySoundWsMsgDTO.java
dto/AlarmCountOfLineDTO.java         dto/AlarmNumDTO.java            dto/DefectTypeDTO.java           dto/SearchAlarmDTO.java
dto/AlarmDealDTO.java                dto/AlarmDTO.java               dto/IgnoreAlarmDTO.java          dto/SearchDefectDTO.java
event/DealAlarmEvent.java            event/WsConnectListener.java
mapper/AlarmRecordDAO.java           mapper/DefectTypeDAO.java       mapper/IgnoreAlarmDAO.java
model/AlarmRecordPO.java             model/DefectTypePO.java         model/IgnoreAlarmPO.java
service/IAlarmRecordService.java     service/IDefectTypeService.java service/IIgnoreAlarmService.java
service/imp/AlarmRecordServiceImpl.java  service/imp/DefectTypeServiceImpl.java  service/imp/IgnoreAlarmServiceImpl.java
task/AlarmTaskManager.java
web/AlarmRecordController.java       web/DefectTypeController.java
```

### 1.2 DataupLoad alarm 模块（42 个 .java）

DST 多了 `entity/`（替代 PSM `model/` 后缀 PO），多 `task/AlarmRetentionTask` / `task/IgnoreExpireTask`（W-F01-C/D 拆分），多 `web/IgnoreAlarmController`（W-F02-B），多 `service/ClientOnlineChecker`（与 yk 推送相关，本审计不深究）。

| 子包 | PSM 数 | DST 数 | 差异 |
|---|---|---|---|
| `config` | 1 | 1 | ✓ 1:1 |
| `constant` | 5 | 5 | ✓ 1:1（hash 全部一致，仅 AlarmConstants 缩进 3→4） |
| `dto` | 12 | 12 | ⚠️ **4 个 DTO 为空类**（见 §3.2） |
| `event` | 2 | 2 | ✓ 1:1 |
| `mapper` | 3 | 3 | ⚠️ **DST mapper 全部裸 BaseMapper**，缺 PSM 自定义方法 |
| `model` / `entity` | 3 | 3 | ✓ 字段全对齐，仅后缀 PO→实体 |
| `service` | 3 | 4 | DST 多 `ClientOnlineChecker`（yk 推送辅助） |
| `service/impl` (imp) | 3 | 3 | ⚠️ **DST AlarmRecordServiceImpl 仅 4/13 方法实装**（见 §4） |
| `task` | 1 | 3 | ⚠️ **DST 新增 2 个 Task 与原 PSM 同款 AlarmTaskManager 重复调度**（见 §6） |
| `web` | 2 | 3 | DST 多 `IgnoreAlarmController`；⚠️ **AlarmRecordController 仅 1/7 端点实装**（见 §5） |

**结论**：文件覆盖率 100%，但实装率约 50%。详细缺口见后续章节。

---

## 2. 常量 / 枚举（constant/）

| 文件 | PSM | DST | 对齐 | 备注 |
|---|---|---|---|---|
| `AlarmConstants.java` | 5 行 | 5 行 | ✓ | 仅缩进 3→4 空格 |
| `AlarmLevelEnum.java` | 22 行 | 22 行 | ✓ | hash 完全一致 |
| `AlarmReasonEnum.java` | 21 行 | 21 行 | ✓ | hash 完全一致 |
| `AlarmSolvedEnum.java` | 23 行 | 23 行 | ✓ | hash 完全一致（`UNSOLVED=2` / `SOLVED=1` / `IGNORE=3` — `W-B04` 修复依赖此对齐） |
| `AlarmTypeEnum.java` | 51 行 | 51 行 | ✓ | hash 完全一致（`DEFECT=1` / `SYSTEM=2` / `DEVICE=3`） |

**结论**：常量层 100% 对齐。

---

## 3. DTO（dto/）

### 3.1 字段完全对齐（7 个）

| 文件 | 字段数 PSM | 字段数 DST | 备注 |
|---|---|---|---|
| `AlarmDTO.java` | 7 | 7 | ✓ hash 完全一致（含 `@Range(min=1,max=3)` 校验） |
| `AlarmCountDTO.java` | 4 | 4 | ✓ |
| `AlarmCountOfLineDTO.java` | 4 | 4 | ✓ |
| `AlarmDealDTO.java` | 1 | 1 | ✓ |
| `AlarmNumDTO.java` | 2 | 2 | ✓（DST 给两字段加了 `= 0` 默认值） |
| `DefectTypeDTO.java` | 6 | 6 | ✓ |
| `PlaySoundWsMsgDTO.java` | 2 | 2 | ✓ |

### 3.2 DST 为空类（4 个严重缺口）

| 文件 | PSM 字段 | DST 字段 | 缺口影响 |
|---|---|---|---|
| `AlarmInfoQueryDTO.java` | `faceId` + extends `TimePageQuery` + equals/hashCode | **空** | `/web/alarm/list-info` 不能传 `faceId` / `pageNo` / `pageSize`，返回空列表 |
| `AlarmQueryDTO.java` | `type / level / solve / faceId / sortType + @NotBlank sortType=1` + extends `TimePageQuery` | **空** | `/web/alarm/list` 入参丢失 5 个字段 + 分页 |
| `SearchAlarmDTO.java` | `type / lineNo / faceNo` | **空** | `/web/alarm` 搜索请求无字段 |
| `SearchDefectDTO.java` | `category / name` | **空** | `/web/defect` 列表搜索无字段 |

### 3.3 字段不一致（1 个语义缺口）

| 文件 | PSM 字段 | DST 字段 | 差异 |
|---|---|---|---|
| `IgnoreAlarmDTO.java` | `type / defectName / lineNo / faceNo / int ignoreAll / faceId / startTime / endTime` | `id / type / defectName / lineNo / faceNo / Integer ignoreAll / faceId / ignoreTime` | ① PSM `startTime/endTime` 用于 `handleAlarmIgnore` 中 `.between(AlarmRecord::getTime, form.getStartTime(), form.getEndTime())` **按 alarm_record 时间窗批量忽略**；DST 单 `ignoreTime` 是单点截止，**丢失时间窗批量过滤能力**。② PSM `ignoreAll` 为 `int` 原语，DST 改为 `Integer`（小问题）。③ DST 多了 `id`（DELETE 接口需要）。 |

**结论**：DTO 层 7/12 = 58% 实装；其中 `AlarmDTO`（客户端上报入口）100% 对齐，但 4 个 web 后台查询 DTO **完全空白**，1 个时间窗语义被简化。

---

## 4. Mapper（mapper/）

### 4.1 自定义方法对比

| Mapper | PSM 自定义方法 | DST | 缺口 |
|---|---|---|---|
| `AlarmRecordDAO` → `AlarmRecordMapper` | `selectAlarmCountDay` / `countAlarmCount` / `selectAlarmCountByType` / `selectRecord` / `selectAlarmCount` | **无**（裸 BaseMapper） | 5 个聚合查询全部缺失 → `handleAlarmNumGet` 等聚合接口无法实装 |
| `DefectTypeDAO` → `DefectTypeMapper` | 无 | 无 | ✓ 对齐（PSM 也只有 BaseMapper） |
| `IgnoreAlarmDAO` → `IgnoreAlarmMapper` | `saveOrUpdateBatch`（`ON CONFLICT (type,line_no,face_no,defect_name) DO NOTHING`） | **无**（裸 BaseMapper） | DST `handleAlarmIgnore` 走 `this.save(entity)` — **重复提交同一 (type+line+face+defect) 会触发 UNIQUE 冲突**；PSM 用 `DO NOTHING` 静默忽略 |

### 4.2 自定义 XML 资源

| 资源 | PSM | DST | 备注 |
|---|---|---|---|
| `AlarmRecordDAO.xml` | 5 个 `<select>` | 无 | 同上 |
| `IgnoreAlarmDAO.xml` | `<insert saveOrUpdateBatch>` ON CONFLICT | 无 | 同上 |
| `DefectTypeDAO.xml` | 无 | 无 | ✓ |

**结论**：Mapper 层 PSM 6 个自定义 SQL → DST 0 个；当前不阻塞报警入口（入口只走 `BaseMapper.insert/update`），但阻塞 `handleAlarmNumGet` 的 4 个聚合统计。

---

## 5. Service / ServiceImpl（service/）

### 5.1 `IAlarmRecordService`（11 个方法签名对齐）

| # | PSM 方法 | DST 实装 | 差异 |
|---|---|---|---|
| 1 | `listAll(AlarmQueryDTO)` | ❌ placeholder `BaseResult.ok().data(Collections.emptyList())` | 未实装 |
| 2 | `add(AlarmDTO)` | ✅ 1:1 + W-X21 全局开关 + W-B04 isIgnore 修复 + W-X30b dealClientAlarm | **最完整**，含 W-X21 / W-B04 / W-X30b 三处增强 |
| 3 | `deal(String uuid)` | ❌ placeholder | 未实装（依赖 ILineService 与 sendAlarmMessage 链） |
| 4 | `getAlarmListInfo(AlarmInfoQueryDTO)` | ❌ placeholder `data(emptyList())` | 未实装 |
| 5 | `handleAlarmNumGet()` | ❌ 返回 `new AlarmNumDTO()` (0/0) | 未实装（缺 mapper XML 聚合） |
| 6 | `handleAlarmSearch(SearchAlarmDTO)` | ❌ placeholder | 未实装（依赖 `IStatusRecordService.searchOffLineClient`） |
| 7 | `sendAlarmTextMessage()` | ✅ 1:1 + 异常吞错 → warn 日志 | 一致 |
| 8 | `listNotResolveDefectAlarmRecord()` | ✅ 1:1 | 一致 |
| 9 | `handleAlarmIgnore(IgnoreAlarmDTO)` | ❌ placeholder | 未实装（IIgnoreAlarmService 拆出去后此处返回 OK） |
| 10 | `sendAlarmMessage(AlarmRecord)` | ✅ 1:1 + **W-B04 isIgnore 修复**（硬编码 false → 查表） | 一致 + 修复 |
| 11 | (隐式) `dealClientAlarm / dealClientAlarmListener` | ✅ 1:1 + **W-X30b 修复**（按 reason 而非 type 去重） | 一致 + 修复 |

### 5.2 `IDefectTypeService`（6 个方法）

| # | PSM 方法 | DST 实装 | 差异 |
|---|---|---|---|
| 1 | `handleDefectTypeAdd(DefectTypeDTO)` | ❌ placeholder | 未实装 |
| 2 | `handleDefectTypeDel(Integer)` | ❌ placeholder | 未实装 |
| 3 | `listDefect(SearchDefectDTO)` | ❌ placeholder | 未实装 |
| 4 | `editDefect(DefectTypeDTO)` | ❌ placeholder | 未实装 |
| 5 | `getByNameAndType(name, type)` | ✅ 1:1 + null guard + `false` 不抛异常 | 一致 + 加固 |
| 6 | `listByAttribute(value, SFunction)` | ⚠️ 1:1 但**丢失 `orderByAsc(DefectType::getName)`** | 小差异 |

### 5.3 `IIgnoreAlarmService`（4 个方法）

| # | PSM 方法 | DST 实装 | 差异 |
|---|---|---|---|
| 1 | `handleAlarmIgnore(IgnoreAlarmDTO)` | ⚠️ 1:1 但**实现思路不同**：PSM body 为空（BUG），DST 实际 save + 设 ignore_time | DST 实装**优于** PSM |
| 2 | `isIgnore(type, defectName, lineNo, faceNo)` | ✅ 1:1 | 一致 |
| 3 | `removeExpire()` | ✅ 1:1 | 一致 |
| 4 | `getIgnoreDefect()` | ✅ 1:1 | 一致 |

**结论**：Service 层 4/21 实装 = 19%。**已实装的都是报警入口核心链**（`add` / `sendAlarmMessage` / `sendAlarmTextMessage` / `listNotResolveDefectAlarmRecord` / `dealClientAlarm` 系列 + `isIgnore` / `removeExpire` / `getIgnoreDefect` / `getByNameAndType` / `listByAttribute`）。`listAll` / `deal` / `getAlarmListInfo` / `handleAlarmNumGet` / `handleAlarmSearch` / `handleAlarmIgnore` / 4 个 DefectType CRUD 全部占位。

---

## 6. Web / Controller（web/）

### 6.1 `AlarmRecordController`

| # | PSM 端点 | 方法 | DST | 备注 |
|---|---|---|---|---|
| 1 | `GET /web/alarm/list` | listAll | ❌ 缺端点 | 返回 OK 但路径未注册 |
| 2 | `GET /web/alarm/num` | handleAlarmNumGet | ❌ 缺端点 | 同上 |
| 3 | `POST /client/data/alarm` | add | ✅ **1:1 + @Validated** | 报警入口核心 |
| 4 | `POST /client/data/deal-alarm` | deal | ❌ 缺端点 | 同上 |
| 5 | `GET /web/alarm/list-info` | getAlarmListInfo | ❌ 缺端点 | 同上 |
| 6 | `GET /web/alarm` | handleAlarmSearch | ❌ 缺端点 | 同上 |
| 7 | `PUT /web/alarm/ignore` | handleAlarmIgnore | ❌ 缺端点（移到 IgnoreAlarmController） | DST 拆分成独立 controller，见 §6.3 |

**当前 DST AlarmRecordController 仅暴露 1 个端点（`POST /client/data/alarm`），运维/管理后台完全不可用**。

### 6.2 `DefectTypeController`

| # | PSM 端点 | DST | 备注 |
|---|---|---|---|
| 1 | `POST /web/defect` | ✅ 端点存在 + 调用 handleDefectTypeAdd（占位） | 路径对齐 |
| 2 | `DELETE /web/defect` | ✅ 端点存在 + 占位 | |
| 3 | `GET /web/defect` | ✅ 端点存在 + 占位 | |
| 4 | `PUT /web/defect` | ✅ 端点存在 + 占位 | |

**DST 4 个端点都注册了但全部返回 OK 空数据**——前端能调到，但拿到空响应。

### 6.3 `IgnoreAlarmController`（DST 独有，PSM 无此 controller）

| # | DST 端点 | 方法 | 备注 |
|---|---|---|---|
| 1 | `POST /web/alarm/ignore/` | `IIgnoreAlarmService.handleAlarmIgnore` | W-F02-B 拆出 |
| 2 | `DELETE /web/alarm/ignore/{id}` | `removeById(id)` | 真实删除（DST 增强） |
| 3 | `GET /web/alarm/ignore/` | `getIgnoreDefect()` | 列出生效中的白名单 |
| 4 | `GET /web/alarm/ignore/check` | `isIgnore(type, lineNo, faceNo, defectName)` | 外部查询接口 |

**DST 拆出独立 controller 比 PSM 更模块化**——PSM 把 ignore 揉在 `AlarmRecordController.PUT /web/alarm/ignore` 一个端点里。

**结论**：Controller 层 5/11 端点路径注册（DST 实际生效的包括 `POST /client/data/alarm` + 4 个 DefectType + 4 个 IgnoreAlarm）。PSM AlarmRecordController 的 6 个 web 管理端点（`list` / `num` / `deal-alarm` / `list-info` / `search`）**路径都未在 DST 注册**，前端调用会 404。

---

## 7. Task / Scheduler（task/）

| 文件 | PSM | DST | 差异 |
|---|---|---|---|
| `AlarmTaskManager.clearAlarmData` | ✅ cron `0 0 0 * * ?` (午夜) | ✅ **保留原 cron + 实现** | 一致 |
| `AlarmTaskManager.delExpireIgnoreDefect` | ✅ cron `0 0 1 * * ?` (凌晨 1 点) | ✅ **保留原 cron + 实现** | 一致 |
| `AlarmRetentionTask` (DST 新增, W-F01-C) | — | ⚠️ cron `0 0 3 * * ?` (凌晨 3 点) | **与 AlarmTaskManager.clearAlarmData 功能完全重复** |
| `IgnoreExpireTask` (DST 新增, W-F01-D) | — | ⚠️ cron `0 0 * * * ?` (每小时) | **与 AlarmTaskManager.delExpireIgnoreDefect 功能完全重复** |

**🔴 重复调度风险**：
- `AlarmTaskManager.clearAlarmData`（午夜）+ `AlarmRetentionTask.clearAlarmData`（凌晨 3 点）= **同一个 SQL 跑两次/天**
- `AlarmTaskManager.delExpireIgnoreDefect`（凌晨 1 点）+ `IgnoreExpireTask.delExpireIgnoreDefect`（每小时）= **删除 ignore_alarm 跑 24+1 次/天**

DST 的 `AlarmRetentionTask` 还修了一个 PSM BUG：PSM 用 `${data-retention-time.alarm:3}` 默认只保留 **3 天**（注释却写"90 days ago"是误导），DST 硬编码 90L。

**建议**：
- P1：把 `AlarmTaskManager` 标记为 `@Deprecated` 或删掉，全部走 `AlarmRetentionTask` / `IgnoreExpireTask`（统一调度入口）
- P2：`IgnoreExpireTask` 注释说"按 W-X17a 改用 `end_time < {now}` 字符串比较" — 当前 schema 中 **end_time 是 varchar(19)**，PG 字典序比较依赖字符串格式统一为 `yyyy-MM-dd HH:mm:ss`，脆弱点（详见 W-X23c）

---

## 8. SQL Drift（resources/sql/）

### 8.1 ignore_alarm 表结构对比

| 列 | PSM V1.14 | DST V1.14 | DST V1.20 | DST 当前（含 W-X23c） |
|---|---|---|---|---|
| `id` | serial PK | serial PK | serial PK | serial PK |
| `defect_name` | varchar(128) NOT NULL | varchar(128) NOT NULL | varchar(50) | varchar(50) |
| `type` | int NOT NULL | int NOT NULL | int | int |
| `line_no` | varchar(20) NOT NULL | varchar(20) NOT NULL | varchar(20) | varchar(20) |
| `face_no` | varchar(20) NOT NULL | varchar(20) NOT NULL | varchar(20) | varchar(20) |
| `ignore_time` | **timestamp NOT NULL** | **timestamp NOT NULL** | **不存在**（V1.20 DROP） | **timestamp**（W-X23c ALTER 加回） |
| `start_time` | — | — | **varchar(19)** | varchar(19) |
| `end_time` | — | — | **varchar(19)** | varchar(19) |
| `ignore_all` | — | — | **int default 2** | int default 2 |
| `face_id` | — | — | **varchar(20)** | varchar(20) |

### 8.2 唯一索引

| 来源 | 语句 | 当前状态 |
|---|---|---|
| PSM V1.17 | `CREATE UNIQUE INDEX ignore_alarm_type_idx ON public.ignore_alarm ("type",line_no,face_no,defect_name);` | ✅ DST V1.17 也包含此索引 |
| DST V1.20 | （drop + recreate 表） | ⚠️ **DROP TABLE 会级联删索引**；DST V1.20 重 create 表时**未重建索引** |
| W-X23c | ALTER 加 ignore_time 列 | 未重建索引 |

**🔴 严重问题**：V1.20 的 `drop table if exists public.ignore_alarm; create table ...` 没有重建 unique 索引。当前实际 DB 状态是 `ignore_alarm` 表**没有 unique 索引**。后果：
- DataupLoad 的 `IgnoreAlarmServiceImpl.handleAlarmIgnore` 走 `this.save(entity)`（plain INSERT），**理论上可以无限插入相同 (type,line,face,defect) 记录**，没有 ON CONFLICT 拦截。
- PSM 的 `IgnoreAlarmDAO.saveOrUpdateBatch` 走 `ON CONFLICT DO NOTHING`（依赖索引），DST 没有等价的 XML mapper。

**修复建议**：补一个 V1.21__ignore_alarm_unique_idx.sql：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS ignore_alarm_type_idx
  ON public.ignore_alarm ("type", line_no, face_no, defect_name);
```

---

## 9. PSM BUG 修复汇总（DataupLoad 优于 PSM 处）

| # | PSM BUG | DST 修复位置 | 工单 |
|---|---|---|---|
| 1 | `AlarmRecordServiceImpl.sendAlarmMessage` 硬编码 `boolean isIgnore = false` 白名单永远失效 | `AlarmRecordServiceImpl.sendAlarmMessage` 调 `ignoreAlarmService.isIgnore()` | W-B04 |
| 2 | `AlarmRecordServiceImpl.handleAlarmIgnore` body 为空（只建 List 不 save） | `IgnoreAlarmServiceImpl.handleAlarmIgnore` 实际 save entity | W-F02-B |
| 3 | `AlarmTaskManager.clearAlarmData` 默认保留天数错（`${data-retention-time.alarm:3}` 与"90 days"注释矛盾） | `AlarmRetentionTask` 硬编码 `RETENTION_DAYS = 90L` | W-F01-C |
| 4 | `AlarmRecordServiceImpl.dealClientAlarm` 去重 key 用了 `(lineNo, faceNo, type)`（PSM 反编译注：实测可能不同，W-X30b 已确认 DST 用 `(lineNo, faceNo, reason)`） | 按 reason 去重 | W-X30b |
| 5 | （无）PSM 没有 `alarm.global-enabled` 紧急开关 | `DefectAlarmConfig.globalEnabled` + `AlarmRecordServiceImpl.add()` 入口判 | W-X21 |
| 6 | （无）PSM 没有 IgnoreAlarmController 独立端点 | 拆出独立 controller（POST/DELETE/GET list/GET check） | W-F02-B |

---

## 10. 缺口汇总 + 修复优先级

### 10.1 🔴 P0（建议立即修）

| # | 缺口 | 风险 | 建议 |
|---|---|---|---|
| 1 | **ignore_alarm 唯一索引缺失**（V1.20 DROP TABLE 后未重建） | 同一忽略组合可重复插入，导致 `isIgnore` count() 返回多条；UI 列表重复 | 补 V1.21__ignore_alarm_unique_idx.sql |

### 10.2 🟡 P1（PM 决策后再修）

| # | 缺口 | 阻塞 | 建议 |
|---|---|---|---|
| 2 | `AlarmRecordController` 6 个 web 管理端点全缺（list / num / deal-alarm / list-info / search） | 大屏 / 管理后台无法查询历史 / 处理告警 / 看统计 | 派单 0.5d，按 PSM 反编译实现 |
| 3 | 4 个 DTO 空类（AlarmQueryDTO / AlarmInfoQueryDTO / SearchAlarmDTO / SearchDefectDTO） | 上述端点的入参 binding | 同步派工时一起补 |
| 4 | 5 个 AlarmRecordMapper.xml 聚合查询缺失 | `handleAlarmNumGet` 等无法实装 | 与 #2 合并 |
| 5 | `IgnoreAlarmServiceImpl.handleAlarmIgnore` 重复提交会冲突（无 ON CONFLICT） | UI 重复点击添加忽略会 500 | 写 SaveOrUpdate XML mapper 复用 PSM 1:1 |
| 6 | `IgnoreAlarmDTO` 丢失 `startTime/endTime` 时间窗过滤 | 无法按 alarm_record 时间窗批量忽略 | 补 2 字段或在 service 内部改用 single ignoreTime |

### 10.3 🟢 P2（看后续需求）

| # | 缺口 | 备注 |
|---|---|---|
| 7 | `AlarmTaskManager` 与新 `AlarmRetentionTask` / `IgnoreExpireTask` 重复调度 | 合并到新类即可 |
| 8 | `DefectTypeServiceImpl` 4 个 CRUD 占位 | 待 PSM web 后台整体迁移 |
| 9 | `DefectTypeServiceImpl.listByAttribute` 缺 `orderByAsc(DefectType::getName)` | 小差异，不影响功能 |
| 10 | `sendAlarmSoundWsMessage` 未实装（依赖 system_config） | 暂时用 log.debug 占位，等 PSM 系统配置迁移时一起补 |
| 11 | `getAlarmListInfo` / `handleAlarmSearch` 未实装 | 依赖 IStatusRecordService.searchOffLineClient，等 status_record 业务需求确认 |

---

## 11. 与历史审计的差异

| 工单 | 范围 | 与本次的差异 |
|---|---|---|
| W-X16 | 筛选 / 过滤能力 | 已审计 add/sendAlarmMessage 防爆 + isIgnore + WS 推送；本次复核结论一致 |
| W-X23c | ignore_alarm schema 修复 | 本次发现 **唯一索引未随 V1.20 DROP TABLE 重建**，是 W-X23c 之外的第二个 P0 |
| W-X24 | PSM 推送链路对齐 | 仅审计 add→yk 主链路；本次补充 web 后台对齐 |

---

## 12. 审计声明

- ✅ PSM 反编译产物读取 34 个 .java（`docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/alarm/`）
- ✅ DataupLoad 源码读取 42 个 .java + 21 个 SQL 迁移
- ✅ 字段级 / 方法级 / 端点级 / SQL 级四层对比
- ❌ **未改任何代码 / yml / SQL / 重启服务**
- ✅ 报告路径：`docs/delivered/2026-07-24-W-X25-psm-alarm-alignment-audit.md`
- 📋 P0 #1（unique index）建议立即派单，**单文件 < 5 行 SQL，30 分钟可上线**

🏭 Codex Worker · 2026-07-24
