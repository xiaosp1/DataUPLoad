# PSM common 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: 🟡 P1（含 GlobalTaskManager 核心 cron）

---

## 1. 业务定位

### 1.1 解决什么问题

common 模块是 PSM 的**横切关注点**集合：

- **全局定时任务**（🚨 P0）：客户端掉线检测 + 大屏推送 + 加密狗验证
- **国际化（i18n）**：zh-CN / en-US / id-ID 自动识别
- **WebSocket 类型枚举**：定义 4 种消息类型（SCREEN/ALARM/ALARM_SOUND/PLAN_CHANGE）
- **状态枚举**：YES(1)/NO(0) 通用开关
- **公共方法**：sendPlanChange WebSocket 广播
- **MyBatis TypeHandler**：JSONArray ↔ String 转换
- **加密狗工具**：第五代/第四代加密狗验证（JNA + SDK）
- **调度线程池**：5 线程 scheduled pool

### 1.2 与其他模块的依赖关系

```
common ──→ detect (StatusRecordDAO / IStatusRecordService / DeviceStatus / DeviceType)
common ──→ alarm  (IAlarmRecordService / AlarmReasonEnum / AlarmSolvedEnum)
common ──→ line   (StateChangeEvent)
common ──→ screen (IScreenService)
common ──→ framework (WebSocketHandler / EventUtil / HikDateUtil / HikDongleUtil / FifthDongleSDK)
```

---

## 2. 类清单（11 个 java）

### 2.1 config/ (2)
| 类 | 责任 |
|---|---|
| `I18nConfig` (+1 inner LocaleFilter) | Servlet Filter 根据 `Accept-Language` 头设置 Locale（zh-CN/en-US/id-ID）|
| `ScheduleConfig` | 实现 `SchedulingConfigurer`，配置 scheduled 线程池（5 线程）|

### 2.2 constants/ (4)
| 枚举 | 值 | 备注 |
|---|---|---|
| `StateEnum` | YES(1) / NO(0) | 通用开关 |
| `CommonMethod` (class, not enum) | sendPlanChange(WebSocketHandler, clientNo) | 静态广播工具 |
| `WsTypeEnum` | SCREEN/ALARM/ALARM_SOUND/PLAN_CHANGE | WebSocket 消息类型 |
| ... | | |

### 2.3 handler/ (1)
| 类 | 责任 |
|---|---|
| `JsonArrayTypeHandler` | MyBatis TypeHandler：JSONArray ↔ String |

### 2.4 task/ (1)
| 类 | cron | 责任 |
|---|---|---|
| `GlobalTaskManager` (140 行) | 3 个 cron | **🚨 P0** 客户端状态检测 + 大屏推送 + 加密狗 |

### 2.5 utils/ (3)
| 类 | 责任 |
|---|---|
| `DongleUtils` | 第五代加密狗验证（JNA 调用 `FifthDongleSDK`）|
| `EnumUtil` | 按 value 查找 enum（包装 Apache Commons EnumUtils）|
| `MathUtils` | 百分比除法（BigDecimal, scale 控制）|

---

## 3. 核心流程

### 3.1 🚨 P0: 客户端状态检测（每 5 秒）

```
GlobalTaskManager @Scheduled(initialDelay=60s, fixedDelay=5s) checkClientStatus()
  │
  ├─→ StopWatch start
  │
  ├─→ SELECT * FROM status_record WHERE type=CLIENT AND status=ONLINE
  │
  ├─→ listByAttribute("客户端", DefectTypePO::getName)
  │     └─→ 查"客户端"缺陷类型（用于 alarm.defectName="客户端"）
  │
  ├─→ for each statusRecord (lineNo, faceNo, time):
  │     ├─→ timeDiff = now - statusRecord.time (毫秒)
  │     │
  │     ├─→ if timeDiff > 60000ms (60秒):
  │     │     ├─→ 查该产线所有 status_record → 设 status=OUTLINE
  │     │     ├─→ publish(StateChangeEvent{status=OUTLINE, lineNo, faceNo})
  │     │     │     └─→ StateChangeServiceImpl.handleStateChange (@Async) 写入 state_change
  │     │     │
  │     │     └─→ synchronized (this):
  │     │           ├─→ SELECT * FROM alarm_record WHERE lineNo=? AND faceNo=? AND solve=UNSOLVED AND reason=DISCONNECT LIMIT 1
  │     │           │
  │     │           ├─→ if alarm == null (之前没报警过):
  │     │           │     ├─→ new AlarmRecordPO.buildClientAlarm(lineNo, faceNo)
  │     │           │     │     (uuid=currentTimeMillis, type=CLIENT, defectName="客户端", level=HIGH, reason=DISCONNECT)
  │     │           │     ├─→ if deviceDefectType 存在 → setDefectType(...) + isMatch=true
  │     │           │     ├─→ if isMatch OR isSaveAllAlarm (默认 true):
  │     │           │     │     ├─→ save(alarm)
  │     │           │     │     └─→ sendAlarmMessage(alarm)  // 触发 WebSocket 推送 + 鹰科推送
  │     │           │     │
  │     │           │     └─→ else: 不保存（仅匹配设备类型才存）
  │     │           │
  │     │           └─→ else (之前已报警): 不再重复
  │
  ├─→ if offLineDevices 不空 → updateBatchById(offLineDevices)
  │
  └─→ log "end check client status.[cst={耗时ms}]"
```

**⚠️ 关键点**:
- 60 秒无心跳 → 判定离线
- `synchronized (this)` 防止并发创建多个 UNSOLVED DISCONNECT 报警
- 离线后发断连报警（DISCONNECT reason）
- `isSaveAllAlarm=true` 表示：即使没有"客户端" defect_type 配置，也保存报警（兜底）

### 3.2 🚨 P0: 大屏推送（每 5 秒）

```
GlobalTaskManager @Scheduled(initialDelay=10s, fixedDelay=5s) sendScreen()
  │
  └─→ screenService.sendScreenDataInfo()
        │
        └─→ buildScreenData() → WsMessage{type=SCREEN} → broadcastByUid(..., "web")
```

**调用方已确认**！screen 模块的 `sendScreenDataInfo` 是被 `GlobalTaskManager.sendScreen` 定时调用的，每 5 秒推送一次大屏数据。

### 3.3 🚨 P0: 加密狗验证（每 60 秒）

```
GlobalTaskManager @Scheduled(initialDelay=5s, fixedDelay=60s) checkDogOnlineStatus()
  │
  ├─→ if dongleType == null:
  │     ├─→ log error "dog value can't be null"
  │     └─→ System.exit(1)  ← ⚠️ 强制退出进程
  │
  ├─→ if dongleGeneration == 4:
  │     └─→ HikDongleUtil.validateFourthDongle(dongleType, null)
  │
  └─→ else:
        └─→ DongleUtils.validateFifthDongle(Integer.parseInt(dongleType))
              │
              ├─→ MV_LoginLicense_Dll (JNA 调用)
              ├─→ MV_GetLockInfoSet_Dll (获取许可列表)
              ├─→ 遍历许可 ID，匹配 dongleType
              └─→ return true/false

  └─→ if validate 结果 == false:
        ├─→ log error "dog offline"
        └─→ System.exit(1)  ← ⚠️ 强制退出进程
```

**⚠️ 加密狗掉线 = 进程退出**：PSM 用硬性退出保护版权。
**配置**:
```yaml
dongle:
  type: ${DONGLE_TYPE}        # 必需，无默认
  generation: 5               # 4 或 5，默认 5
```

### 3.4 国际化流程

```
HTTP Request (Accept-Language: id-ID,id;q=0.9)
  │
  └─→ I18nConfig.LocaleFilter.doFilterInternal
        │
        ├─→ resolveLocale(request):
        │     ├─→ 解析 Accept-Language → language code
        │     ├─→ if "id" → LOCALE_ID_ID (印度尼西亚)
        │     ├─→ if "en" → LOCALE_EN_US (美国)
        │     ├─→ if "zh" → LOCALE_ZH_CN (中国)
        │     └─→ else    → LOCALE_ZH_CN (默认)
        │
        ├─→ LocaleContextHolder.setLocale(locale, true)
        ├─→ filterChain.doFilter(request, response)
        └─→ LocaleContextHolder.resetLocaleContext()
```

**注册**: `FilterRegistrationBean` 拦截所有 `/*`，order = `Integer.MIN_VALUE`（最高优先级）

### 3.5 WebSocket 广播方案变更

```
LineServiceImpl.bindPlan() / switchPlan() / mod()
  │
  └─→ CommonMethod.sendPlanChange(webSocketHandler, clientNo)
        │
        └─→ WsMessage{type="planChange"}.data("changePlan")
              │
              └─→ webSocketHandler.broadcastByUid(wsData.toJsonString(), clientNo)
                    └─→ 推送给特定 client（按 clientNo = lineNo-faceNo）
```

---

## 4. 关键类逐个解析

### 4.1 🚨 P0: `GlobalTaskManager` (140 行)

**3 个 cron 任务**:

| 方法 | initialDelay | fixedDelay | 责任 |
|---|---|---|---|
| `checkClientStatus()` | 60s | 5s | 客户端掉线检测 + 断连报警 |
| `sendScreen()` | 10s | 5s | 大屏数据推送 |
| `checkDogOnlineStatus()` | 5s | 60s | 加密狗验证（失败退出进程）|

**关键配置**:
```java
@Value("${alarm.save-all:true}") Boolean isSaveAllAlarm;
@Value("${dongle.type}") String dongleType;
@Value("${dongle.generation:5}") Integer dongleGeneration;
```

**⚠️ 核心 cron 间隔**:
- 客户端状态：5 秒（实时性高）
- 大屏：5 秒（实时性高）
- 加密狗：60 秒（容错）

### 4.2 I18nConfig

`LocaleFilter extends OncePerRequestFilter`，注册到 FilterRegistrationBean。

**支持语言**: zh-CN / en-US / id-ID
**默认**: zh-CN

**⚠️ BUG**: `resolveLocale` 中只匹配 language code，不匹配 country。例如 `zh-TW` 也会被识别为 `zh-CN`（繁体中文 → 简体中文）。需要扩展 country 映射。

### 4.3 ScheduleConfig

```java
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
}
```

**调度池大小: 5**。如果 cron 任务执行时间 > 5 秒，可能出现任务阻塞或串行执行。

### 4.4 DongleUtils

**加密狗验证（第五代）**:
1. `MV_LoginLicense_Dll` — 登录许可
2. `MV_GetLockInfoSet_Dll` — 获取许可列表（如果返回 4 表示 buffer 不够，需要 reallocate）
3. 遍历许可 ID，匹配 `dongleType`
4. 返回 true/false

**⚠️ BUG**: 代码 `lockCount = 1` 后 `dogInfos[0].size()`，但 `new ByReference().toArray(lockCount)` 实际是 Java 数组，不是 JNA 数组。这可能是反编译 decompiler 的优化问题，原始代码应该用 `new ByReference[lockCount]`。

### 4.5 JsonArrayTypeHandler

```java
@Component
@MappedJdbcTypes(JdbcType.ARRAY)
@MappedTypes(JSONArray.class)
public class JsonArrayTypeHandler implements TypeHandler<JSONArray> {
    public void setParameter(PreparedStatement ps, int i, JSONArray parameter, JdbcType jdbcType) {
        ps.setString(i, parameter == null ? null : parameter.toString());
    }
    public JSONArray getResult(ResultSet rs, String columnName) {
        return JSONUtil.parseArray(rs.getString(columnName));
    }
    // ...
}
```

**⚠️ 问题**: `@MappedJdbcTypes(JdbcType.ARRAY)` 但实际写入用 `setString`（不是 ARRAY 类型的写入）。这意味着 MySQL 列应该是 `JSON` 或 `TEXT` 类型，不是真的 PostgreSQL ARRAY。EdgeHost 移植时用 EF Core value converter 或 Dapper TypeHandler。

### 4.6 CommonMethod.sendPlanChange

```java
public static void sendPlanChange(WebSocketHandler webSocketHandler, String clientNo) {
    WsMessage wsData = WsMessage.build().type(WsTypeEnum.PLAN_CHANGE.getValue()).data("changePlan");
    webSocketHandler.broadcastByUid(wsData.toJsonString(), clientNo);
}
```

**推送给单个 client**（按 clientNo = lineNo-faceNo），其他 client 不会收到。

### 4.7 WsTypeEnum

```java
public enum WsTypeEnum {
    SCREEN("screen"),
    ALARM("alarm"),
    ALARM_SOUND("sound"),
    PLAN_CHANGE("planChange");
}
```

**4 种 WebSocket 消息类型**，前端按 type 字段分发处理。

---

## 5. 与 EdgeHost 对照

### 5.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `JsonArrayTypeHandler` | EF Core value converter | ✅ W-A9 |
| `CommonMethod.sendPlanChange` | ❌ 没做 | 🟡 P1 |

### 5.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `GlobalTaskManager.checkClientStatus` (60s 离线检测) | ❌ 没做 | 🟡 P1（V1.19 待移植）|
| `GlobalTaskManager.sendScreen` (5s 大屏推送) | ❌ 不需要 | ⚪ N/A |
| `GlobalTaskManager.checkDogOnlineStatus` | ❌ 不需要（产线侧无加密狗）| ⚪ N/A |
| `I18nConfig.LocaleFilter` | ❌ 不需要 | ⚪ N/A |
| `ScheduleConfig`（5 线程 scheduled pool）| ⚠️ IHostedService 已用 | ✅ |
| `DongleUtils` | ❌ 不需要 | ⚪ N/A |

### 5.3 移植建议

**W-A22+ 复刻 `GlobalTaskManager.checkClientStatus`**：
- 这是 V1.19 关键：5 秒检测客户端心跳，60 秒超时 → 标记离线 + 写报警
- EdgeHost 改写为 .NET `IHostedService` + `Channel<ClientStatusEvent>`
- `isSaveAllAlarm` 配置 → appsettings.json
- `dongleType/dongleGeneration` 配置 → 产线侧不需要

**WebSocket 类型枚举**：
- W-A18 已部分实现 SignalR
- W-A22+ 补全 PLAN_CHANGE / SCREEN 类型

---

## 6. 风险 / 注意点

### 6.1 ⚠️ P0: 加密狗失败 = 进程退出

```java
if (result) {
    log.info("dog online");
} else {
    log.error("dog offline ");
    System.exit(1);  // 强制退出
}
```

**风险**: 加密狗临时掉线（接触不良）会导致 PSM 反复重启。需要 W-A 后续：
- 失败时增加重试（不要立即退出）
- 退出前发送通知（邮件/SMS）
- 考虑 PSM 是否有 fallback 机制

### 6.2 ⚠️ P0: 客户端状态检测并发

```java
public void checkClientStatus() {
    synchronized (this) {  // 只锁了报警创建部分
        ...
    }
}
```

`synchronized` 只锁了 `alarmRecordService.save(alarm)` 周围，**但 statusRecordService 的查询和更新没锁**。如果有多个实例运行（理论上不应该），可能导致 race condition。

### 6.3 ⚠️ 调度线程池只有 5 个

`newScheduledThreadPool(5)`：
- 如果 `checkClientStatus` 因为数据库慢查询耗时 > 5 秒，会阻塞后续 cron 触发
- 如果多个 cron 同时触发，会串行执行

### 6.4 ⚠️ disconnect 报警的重复检测

`if (alarm == null)` 时才创建新报警——但同一产线可能因为网络抖动**先掉线 → 重连 → 再掉线**，每次重连都会 reset state（`ONLINE`），60 秒后再掉线时会再报警。这是预期行为，但报警量可能很大。

### 6.5 ⚠️ `deviceDefectType.get(0)` 取第一条

```java
if (CollectionUtils.isNotEmpty(deviceDefectType)) {
    isMatch = true;
    alarm.setDefectType(deviceDefectType.get(0));
}
```

假设只有一条"客户端" defect_type，但如果有重名，会取第一条（顺序由 SQL 决定，可能不稳定）。

### 6.6 ⚠️ System.exit(1) 绕过 Spring 容器关闭

`System.exit(1)` 会强制终止 JVM，不走 Spring `@PreDestroy` 流程。可能导致：
- 数据库连接未关闭
- WebSocket 连接未清理
- 缓存未刷盘

### 6.7 ⚠️ AlarmRecordPO.buildClientAlarm 用 currentTimeMillis 作 uuid

```java
this.uuid = String.valueOf(System.currentTimeMillis());
```

如果同一毫秒有两次客户端断连（不同产线），uuid 会冲突。EdgeHost 移植用 `UUID.randomUUID()`。

### 6.8 ⚠️ Scheduled cron 顺序问题

`@Scheduled` 的 cron 触发顺序由 fixedDelay 决定：
- `checkClientStatus`: 5 秒
- `sendScreen`: 5 秒
- 两次可能几乎同时触发

EdgeHost 移植时考虑错峰。

### 6.9 ⚠️ i18n filter 没处理 Accept-Language 解析失败

```java
Locale requestLocale = Locale.forLanguageTag(acceptLanguage.trim().split(",")[0].trim());
```

如果 `Accept-Language` 是 `*` 或格式错误，`forLanguageTag` 返回 null locale，下一步 `requestLocale.getLanguage()` 会 NPE。

### 6.10 ⚠️ EnumUtil 包装 Apache Commons EnumUtils

```java
public static <T extends Enum<T>, R> T getEnumByValue(R value, Function<T, ? extends R> getter, Class<T> enumClazz) {
    for (T anEnum : EnumUtils.getEnumList(enumClazz)) {
        R tempValue = (R)getter.apply(anEnum);  // 强转 R，可能 ClassCastException
        if (tempValue.equals(value)) {
            return anEnum;
        }
    }
    return null;
}
```

**⚠️ 强转风险**: `(R)getter.apply(anEnum)` —— getter 的返回类型可能不是 R，而是 getter 的实际返回类型。如果调用方写错泛型，会抛 ClassCastException。

---

## 7. 总结

common 模块是 PSM 的横切关注点集合，P0 关注点：
1. **`GlobalTaskManager.checkClientStatus`**（5 秒检测 + 60 秒超时）：🚨 P0 离线检测核心
2. **`GlobalTaskManager.sendScreen`**（5 秒推送）：🚨 P0 大屏数据
3. **`GlobalTaskManager.checkDogOnlineStatus`**（60 秒）：🚨 P0 加密狗验证 + 失败退出
4. **`JsonArrayTypeHandler`**：MyBatis JSON 字段映射

P1 关注点：
5. **`CommonMethod.sendPlanChange`**：WebSocket 广播
6. **`WsTypeEnum`**：4 种消息类型

关键风险：
- ⚠️ **加密狗掉线 = 进程退出**
- 调度线程池只有 5 个
- 状态检测并发未完全锁保护
- disconnect 报警可能频繁（网络抖动）
- i18n filter 解析失败可能 NPE
- System.exit(1) 绕过 Spring 清理
