# PSM alarm 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: 🟡 P1（核心业务已 W-A18 对齐，V1.14 ignore_alarm 缺）

---

## 1. 业务定位

### 1.1 解决什么问题

alarm 模块是 PSM 的**报警中枢**：

- **报警接收**：客户端通过 `POST /client/data/alarm` 上报缺陷/系统/设备报警
- **报警去重 + 升级**：同类报警 UNSOLVED 时自动 IGNORE 旧记录（避免冗余）
- **报警推送**：
  - WebSocket 推送报警文本（`WsTypeEnum.ALARM`）
  - WebSocket 推送报警音效（`WsTypeEnum.ALARM_SOUND` + 播放次数/URI）
  - 鹰科推送（`PushAlarmEvent` → `YKServiceImpl`）
- **报警查询/分页/忽略**：Web API + ignore_alarm 表
- **设备状态断连报警**：监听 `DealAlarmEvent` 自动处理
- **定时清理**：每日 0 点清理 3 天前 SOLVED 报警

### 1.2 与其他模块的依赖关系

```
alarm ──→ detect (IStatusRecordService)              # 设备离线状态查询
alarm ──→ detect (StateChangeEvent/DeviceStatus)     # 触发断连报警
alarm ──→ line   (ILineService)                      # 按 faceId 解析 lineNo/faceNo
alarm ──→ config (ISystemConfigService)              # 读取音效 URI + 播放次数
alarm ──→ yingke (PushAlarmEvent)                    # 鹰科推送
alarm ──→ common (WebSocketHandler/WsTypeEnum)       # 实时推送
alarm ──→ framework (EventUtil)                      # 事件发布
```

---

## 2. 类清单（35 个 java + 1 个 XML）

### 2.1 config/ (1)
| 类 | 责任 |
|---|---|
| `DefectAlarmConfig` (+1 inner DefectTypeConfig) | `@ConfigurationProperties("alarm")` 加载报警模板配置（type + regex template + names）|

### 2.2 constant/ (5)
| 枚举 | 值 | 备注 |
|---|---|---|
| `AlarmTypeEnum` | DEFECT(1) / SYSTEM(2) / DEVICE(3) | 含 description + soundConfigKey |
| `AlarmLevelEnum` | NORMAL(1) / HIGH(2) | 普通/高级 |
| `AlarmReasonEnum` | DISCONNECT(1) | 当前仅 1 种（客户端掉线）|
| `AlarmSolvedEnum` | SOLVED(1) / UNSOLVED(2) / IGNORE(3) | 处理状态 |
| `AlarmConstants` | 通用常量 | 字符串字面量 |

### 2.3 dto/ (12)
| 类别 | DTO |
|---|---|
| 输入 | `AlarmDTO` / `AlarmDealDTO` / `IgnoreAlarmDTO` |
| 查询 | `AlarmQueryDTO` / `AlarmInfoQueryDTO` / `SearchAlarmDTO` / `SearchDefectDTO` |
| 输出 | `AlarmCountDTO` / `AlarmCountOfLineDTO` / `AlarmNumDTO` (+Builder) |
| 配置 | `DefectTypeDTO` |
| WebSocket | `PlaySoundWsMsgDTO` |

### 2.4 event/ (2)
| 事件 | 触发方 | 监听方 |
|---|---|---|
| `DealAlarmEvent` | `StatusRecordServiceImpl`（客户端重连）| `AlarmRecordServiceImpl.dealClientAlarmListener` (@Async) |
| `WsConnectListener` | 框架 `WsActionEvent` | `AlarmRecordServiceImpl.sendAlarmTextMessage` |

### 2.5 mapper/ (3)
| DAO | 关键 XML |
|---|---|
| `AlarmRecordDAO` | `selectAlarmCountDay` / `selectAlarmCountByType` / `selectRecord` / `selectAlarmCount` / `countAlarmCount` |
| `DefectTypeDAO` | 默认 + `DefectTypeMapper.xml` |
| `IgnoreAlarmDAO` | 默认 + `IgnoreAlarmMapper.xml` |

### 2.6 model/ (3)
| PO | 表 | 字段 |
|---|---|---|
| `AlarmRecordPO` | `alarm_record` | id/uuid/time/type/lineNo/faceNo/level/message/solve/reason/defectName + buildClientAlarm() |
| `DefectTypePO` | `defect_type` | id/name/category/countEnable/countThreshold/rateEnable/showImgEnable/alarmEnable/sendYkEnable/soundEnable |
| `IgnoreAlarmPO` | `ignore_alarm` | id/defectName/type/lineNo/faceNo/ignoreTime |

### 2.7 service/ (3) + service/imp/ (3)
| 接口 | 实现 | 责任 |
|---|---|---|
| `IAlarmRecordService` | `AlarmRecordServiceImpl` | **🚨 P0** 核心报警业务 |
| `IDefectTypeService` | `DefectTypeServiceImpl` | 缺陷类型 CRUD + 报警启用配置 |
| `IIgnoreAlarmService` | `IgnoreAlarmServiceImpl` | V1.14 忽略报警（含过期清理）|

### 2.8 task/ (1)
| 类 | cron | 责任 |
|---|---|---|
| `AlarmTaskManager` | `0 0 0 * * ?` + `0 0 1 * * ?` | **🚨 P0** 清理 3 天前 SOLVED 报警 + 每日 1 点清理过期 ignore |

### 2.9 web/ (2)
| Controller | 端点 |
|---|---|
| `AlarmRecordController` | `/web/alarm/list` + `/web/alarm/num` + `/client/data/alarm` + `/client/data/deal-alarm` + `/web/alarm/list-info` + `/web/alarm` + `/web/alarm/ignore` |
| `DefectTypeController` | 缺陷类型 CRUD |

---

## 3. 核心流程

### 3.1 报警接收流程（POST /client/data/alarm）

```
客户端 → AlarmRecordController.addAlarmData(AlarmDTO)
  │
  └─→ AlarmRecordServiceImpl.add(AlarmDTO)
        │
        ├─→ AlarmTypeEnum.getByCode(form.type) → 不支持 → error 20101
        │
        ├─→ defectTypeService.listByAttribute(form.type, DefectTypePO::getCategory)
        │     └─→ 按 category 过滤出该类型的所有缺陷配置
        │
        ├─→ 遍历 alarmConfig.config，匹配 alarmType:
        │     ├─→ ReUtil.get(template, form.message, 0) → 提取缺陷名
        │     └─→ 遍历 sortDefectTypeByName 找到包含的缺陷名
        │
        ├─→ 命中"感兴趣的缺陷"：
        │     ├─→ UPDATE alarm_record SET solve=IGNORE WHERE defectName=? AND lineNo=? AND type=? AND faceNo=? AND solve=UNSOLVED
        │     │     └─ 历史未处理同类报警 → 自动忽略
        │     ├─→ save(new AlarmRecordPO{solve=UNSOLVED, message="[缺陷名] 缺陷报警", defectName, defectType})
        │     └─→ sendAlarmMessage(alarm)
        │           │
        │           ├─→ 满足 alarmEnable：
        │           │     ├─→ sendAlarmTextMessage()  WebSocket 推送报警列表
        │           │     └─→ 满足 soundEnable + UNSOLVED → sendAlarmSoundWsMessage()
        │           │           └─→ systemConfigService.listByConfigKey([type.soundConfigKey, "sound_play_count"])
        │           │                 └─→ WebSocket 推送 ALARM_SOUND (含 URI + 播放次数)
        │           │
        │           └─→ 满足 sendYkEnable → publish(PushAlarmEvent)
        │                 └─→ YKServiceImpl 异步推送给英科
        │
        └─→ 未命中感兴趣的缺陷 → log.warn（不写入）
```

### 3.2 报警处理流程（POST /client/data/deal-alarm）

```
客户端 → AlarmRecordController.dealAlaram(AlarmDealDTO{uuid})
  │
  └─→ AlarmRecordServiceImpl.deal(uuid)
        │
        ├─→ UPDATE alarm_record SET solve=SOLVED WHERE uuid=? AND solve=UNSOLVED
        │
        ├─→ 成功 → 重新查询 alarm → 关联 defect_type
        └─→ sendAlarmMessage(alarm)  // 重新推送（已处理后）
```

### 3.3 报警忽略流程（PUT /web/alarm/ignore）

```
Web → AlarmRecordController.ignoreAlarm(IgnoreAlarmDTO)
  │
  └─→ AlarmRecordServiceImpl.handleAlarmIgnore(form)
        │
        ├─→ ignoreAll == NO:
        │     ├─→ faceId != null → lineService.getById → 设置 lineNo/faceNo
        │     ├─→ 按 lineNo/faceNo/type/defectName + 时间范围查询 UNSOLVED 报警
        │     └─→ saveBatch(setSolve=IGNORE)
        │
        ├─→ ignoreAll == YES:
        │     └─→ listNotResolveDefectAlarmRecord()  // 所有启用 alarm 的缺陷类型的 UNSOLVED 报警
        │           └─→ saveBatch(setSolve=IGNORE)
        │
        └─→ sendAlarmTextMessage()  // 推送更新后的列表
```

### 3.4 断连报警处理（事件驱动）

```
StatusRecordServiceImpl.receiveStatus() 检测到客户端重连
  │
  └─→ publish(DealAlarmEvent{lineNo, faceNo, reason=DISCONNECT})
        │
        └─→ AlarmRecordServiceImpl.dealClientAlarmListener(@Async @EventListener)
              │
              └─→ dealClientAlarm(lineNo, faceNo, DISCONNECT)
                    │
                    ├─→ 查询该 lineNo+faceNo+reason=DISCONNECT+solve=UNSOLVED 的所有报警
                    ├─→ 保留第 1 条（最新的），其余设为 SOLVED
                    └─→ deal(uuid) 处理最新的 1 条
```

### 3.5 报警定时清理（每日 0 点）

```
AlarmTaskManager @Scheduled(cron = "0 0 0 * * ?") clearAlarmData()
  │
  ├─→ expireTime = now - alarmRetentionTime (默认 3 天)
  ├─→ DELETE FROM alarm_record
  │     WHERE create_time < expireTime AND solve = SOLVED
  └─→ log "delete alarm data 90 days ago success"  ← 注意日志写的是 90 天但配置是 3 天（日志错误）

@Scheduled(cron = "0 0 1 * * ?") delExpireIgnoreDefect()
  │
  └─→ ignoreAlarmService.removeExpire()
        └─→ DELETE FROM ignore_alarm WHERE ignore_time < now
```

### 3.6 WebSocket 连接触发推送

```
Web 客户端连接 → 框架触发 WsActionEvent(action="connected")
  │
  └─→ WsConnectListener.sendAlarmMessage(@EventListener)
        │
        └─→ alarmRecordService.sendAlarmTextMessage()
              │
              ├─→ listNotResolveDefectAlarmRecord()  // 所有 UNSOLVED 缺陷报警
              └─→ webSocketHandler.broadcastByUid(wsData.toJsonString(), "web")
```

---

## 4. 关键类逐个解析

### 4.1 🚨 P0: `AlarmRecordServiceImpl` (286 行) — 核心

**核心方法清单**:
```java
public BaseResult listAll(AlarmQueryDTO query)              // 分页/全量列表
public void sendAlarmMessage(AlarmRecordPO alarm)          // 推送入口
@Transactional BaseResult handleAlarmIgnore(IgnoreAlarmDTO form)
@Transactional BaseResult add(AlarmDTO form)               // 核心：接收 + 去重 + 推送
@Transactional BaseResult deal(String uuid)                // 标记 SOLVED
@Transactional BaseResult getAlarmListInfo(AlarmInfoQueryDTO)
public BaseResult handleAlarmNumGet()                      // 大屏数字
public void dealClientAlarm(lineNo, faceNo, reason)        // 处理断连报警
@Async @EventListener public void dealClientAlarmListener(DealAlarmEvent event)
public BaseResult handleAlarmSearch(SearchAlarmDTO form)   // type=4 时返回 AlarmRecordPO；其他返回离线设备
public List<AlarmRecordPO> listNotResolveDefectAlarmRecord()  // 给 WS 推送用
private void sendAlarmSoundWsMessage(DefectTypePO)         // 播放音效
public void sendAlarmTextMessage()                         // 推送报警列表
```

**关键字段**:
- `@Value("${alarm.interval:60}") Integer alarmInterval;` — 配置但**代码中未使用**（可能是预留）
- `@Value("${alarm.high-type:3}") String highTypes;` — 大屏"高级报警"类型列表（逗号分隔），默认 `"3"` (DEVICE)

**关键 SQL 模板** (`AlarmRecordXml.xml`):
```sql
-- 按天聚合报警数
SELECT TO_CHAR(TIME::DATE,'yyyy-MM-dd') AS count_time, COUNT(1) as count
FROM alarm_record
WHERE time >= #{startTime} AND time <= #{endTime}
  AND line_no = #{lineNo} AND face_no = #{faceNo}
GROUP BY count_time ORDER BY count_time

-- 按类型聚合未处理报警数（用于大屏数字）
SELECT type, COUNT(*) FROM alarm_record WHERE solve=2 GROUP BY type

-- 每产线+缺陷的报警数（用 row_number 取最新）
SELECT tmp.* FROM (
    SELECT row_number() OVER (PARTITION BY ar.line_no, ar.face_no, ar.defect_name ORDER BY id DESC) group_id, *
    FROM alarm_record ar
    WHERE ar.defect_name IN (...) AND ar.solve=2
) tmp WHERE tmp.group_id=1
```

### 4.2 🚨 P0: `AlarmTaskManager` (46 行)

```java
@Value("${data-retention-time.alarm:3}")  // 默认 3 天
private Integer alarmRetentionTime;

@Scheduled(cron = "0 0 0 * * ?")  // 每日 0 点
public void clearAlarmData() {
    LocalDateTime time = now.minusDays(alarmRetentionTime);
    DELETE FROM alarm_record
    WHERE create_time < #{time} AND solve = SOLVED
}

@Scheduled(cron = "0 0 1 * * ?")  // 每日 1 点
public void delExpireIgnoreDefect() {
    ignoreAlarmService.removeExpire();
}
```

**金标准配置**: `data-retention-time.alarm: 3`（但日志写 90 天，文本不一致 ⚠️）

### 4.3 🟢 P2: `DefectAlarmConfig` (含内部类 DefectTypeConfig)

```yaml
# application.yml
alarm:
  config:
    - type: DEFECT
      template: '正则表达式提取缺陷名'
      names: [缺陷名列表]
    - type: SYSTEM
      template: ...
    - type: DEVICE
      template: ...
```

**模板语法**: hutool `ReUtil.get(template, message, 0)` —— 第一个捕获组作为缺陷名

### 4.4 ⚪ P3: `DefectTypeServiceImpl` (113 行)

CRUD + `listByAttribute(value, getter)` + `getByNameAndType(name, category)`

**业务规则**:
- 新增时强制 `countEnable=false / countThreshold=0 / rateEnable=false / showImgEnable=false`
- 编辑时校验 `soundEnable=YES 必须 alarmEnable=YES`，否则 error 20503

### 4.5 ⚪ P3: `IgnoreAlarmServiceImpl` (85 行)

**核心方法**:
- `isIgnore(type, defectName, lineNo, faceNo)` — 查询 `ignore_time > now` 的忽略记录
- `removeExpire()` — `DELETE FROM ignore_alarm WHERE ignore_time < now`
- `getIgnoreDefect()` — 同上但返回 list
- `handleAlarmIgnore(IgnoreAlarmDTO)` — ⚠️ **未实现**，只是 `return BaseResult.build().ok()`（占位符）

---

## 5. 数据库交互

### 5.1 涉及表（3 张）

| 表 | 用途 | 字段 | retention |
|---|---|---|---|
| `alarm_record` | 报警记录 | 13 | 3 天（仅 SOLVED）|
| `defect_type` | 缺陷类型配置（含报警开关）| 12 | 无 |
| `ignore_alarm` | V1.14 临时忽略记录 | 7 | 每日 1 点清理过期 |

### 5.2 retention 配置

```yaml
data-retention-time:
  alarm: 3  # SOLVED 报警保留天数
```

### 5.3 关键索引（推断）

- `alarm_record(line_no, face_no, type, solve, create_time)` — 多字段查询
- `alarm_record(uuid)` UNIQUE — deal 操作依赖
- `defect_type(category, name)` UNIQUE — `getByNameAndType` 依赖
- `ignore_alarm(type, defect_name, line_no, face_no, ignore_time)` — 复合查询

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `AlarmRecordServiceImpl.add()` + `deal()` | `AlarmEventBus` + 推送逻辑 | ✅ W-A18 |
| `AlarmTaskManager.clearAlarmData` (3 天 cron) | 移植为 `IHostedService` | ✅ W-A18 |
| `alarm_record` 表 | ✅ EdgeHost DB 已有 | ✅ W-A18 |
| `defect_type` 表 + `DefectTypeServiceImpl` | 部分 | ✅ W-A18 |
| `DealAlarmEvent` → `dealClientAlarmListener` | ✅ EdgeHost 已对齐 | ✅ W-A18 |

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `IgnoreAlarmServiceImpl` + `ignore_alarm` 表 | ❌ 没做（V1.14 新表）| 🟢 P2 |
| `handleAlarmIgnore`（按条件忽略批量报警）| ❌ 没做 | 🟢 P2 |
| `handleAlarmSearch`（type=4 查询报警）| 🟡 部分 | 🟢 P2 |
| `sendAlarmSoundWsMessage`（音效播放）| ❌ 没做（可能不需要）| ⚪ P3 |
| `AlarmReasonEnum.DISCONNECT` 扩展 | ❌ 只用 1 种 | ⚪ P3 |

### 6.3 移植建议

**1. 直接抄**：
- alarm_record 表结构 + AlarmRecordServiceImpl
- AlarmTaskManager → 改写为 IHostedService

**2. 改写**：
- WebSocket → SignalR
- `@Async @EventListener` → BackgroundService + Channel

**3. 不抄**：
- 音效播放（依赖具体音频文件，PSM 是浏览器端播放）
- 日志错误（"90 days" 文本与配置 3 天不符）

---

## 7. 风险 / 注意点

### 7.1 ⚠️ 日志与配置不一致

`AlarmTaskManager.clearAlarmData` 注释 `"delete alarm data 90 days ago success"` 但实际 retention 是 **3 天**。代码文本错误，**不影响行为但误导**。

### 7.2 ⚠️ `alarm.interval` 配置但未使用

`@Value("${alarm.interval:60}")` 字段定义但代码中**无引用**。可能是预留功能（报警发送频率限制）或死代码。

### 7.3 isIgnore 判断未应用

`AlarmRecordServiceImpl.sendAlarmMessage` 中有 `boolean isIgnore = false;` 局部变量，但**全程未赋值**（除初始化）。`if (Objects.equals(...) && !isIgnore)` 永远是 true。**疑似死代码或未完成的 ignore_alarm 集成**。

### 7.4 IgnoreAlarmServiceImpl.handleAlarmIgnore 未实现

方法体只有 `return BaseResult.build().ok()` —— 是占位符，**前端调用会无效**。W-A 后续可能要补全。

### 7.5 alarm_record UUID 唯一性风险

`AlarmRecordPO.buildClientAlarm` 用 `String.valueOf(System.currentTimeMillis())` 作为 uuid。如果同一毫秒多次触发（理论上极罕见）会导致 uuid 冲突。EdgeHost 移植建议用 `UUID.randomUUID().toString()`。

### 7.6 报警去重逻辑

`add()` 中 `UPDATE ... SET solve=IGNORE WHERE ... AND solve=UNSOLVED` —— 同类报警未处理时旧记录直接被忽略。**这意味着报警可能被快速淹没**，需要监控报警密度。

### 7.7 @Lazy 注入必要性

`AlarmRecordServiceImpl` 用 `@Lazy` 注入 `IStatusRecordService` 和 `ILineService`，推测是为了**打破循环依赖**。EdgeHost 移植时注意避免循环依赖（构造函数注入 vs 属性注入）。

### 7.8 WebSocket 推送 UID

`webSocketHandler.broadcastByUid(wsData, "web")` —— "web" 是固定 UID 前缀，所有 web 客户端都会收到。EdgeHost SignalR 用 Groups（按用户/角色）。

### 7.9 报警文本推送频率

`sendAlarmTextMessage()` 每次报警接收都触发，可能导致大屏刷新风暴。生产环境应该加节流（debounce）。

### 7.10 dealClientAlarm 索引越界风险

```java
for (int i = 1; i < alarms.size(); i++) {
    alarms.get(i).setSolve(SOLVED);  // 保留 alarms[0]，其余 SOLVED
}
if (CollectionUtils.isEmpty(preUpdateAlarm) || this.updateBatchById(preUpdateAlarm)) {
    this.deal(alarms.get(0).getUuid());  // 处理第 1 条
}
```

如果 `alarms.size() == 1`，`preUpdateAlarm` 为空，进入 `deal()` —— OK。
如果 `alarms.size() == 0`，跳过整个逻辑（外层 `if (alarms.size() != 0)`）—— OK。
**潜在风险**: 若 alarms[0] 已被其他线程 deal，会 deal 失败但 updateBatchById 已成功，可能造成数据不一致。

---

## 8. 总结

alarm 模块是 PSM 报警中枢，P0 关注点：
1. **`AlarmRecordServiceImpl.add()`**：核心写入 + 去重 + 推送（已 W-A18 对齐）
2. **`AlarmTaskManager`**：每日 0/1 点双 cron（已对齐）

P1 关注点：
3. **`DealAlarmEvent` 异步处理断连报警**（已对齐）
4. **`IgnoreAlarmServiceImpl`**：V1.14 新表，**未移植**

关键风险：
- 日志文本与配置不一致（90 vs 3）
- `alarm.interval` 未使用
- `isIgnore` 判断永远是 true
- `IgnoreAlarmServiceImpl.handleAlarmIgnore` 未实现
- 报警去重逻辑可能淹没报警
- WebSocket 推送频率无节流
