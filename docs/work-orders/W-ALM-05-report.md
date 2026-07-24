# W-ALM-05 报告：handleAlarmNumGet WS 推送 + soundEnable 声音播放分支

- **Worker**: Java worker（深度 1/1 子 agent）
- **任务 ID**: W-ALM-05
- **前置依赖**: W-ALM-02（6 个 Service 方法 + `DealAlarmEvent` 链路已就绪）、W-ALM-03（Controller 端点就绪）、W-ALM-01（`selectAlarmCountByType` 已实现）
- **任务来源**: W-ALM-02 报告 §6 已知限制 #1（`handleAlarmNumGet` 未接 WS 推送 + `soundEnable` 未实现）
- **完成时间**: 2026-07-24

## 0. 范围澄清

任务简报里有几处与 PSM 反编译产物不完全一致，本工单按以下原则取舍：

| 简报描述 | 实际情况 | 处理 |
|---|---|---|
| "推 `PlaySoundWsMsgDTO` 内容：`{url: "sound/alarm.wav", count: N, interval: 5s}`" | PSM `PlaySoundWsMsgDTO` 仅 `uri` + `playCount` 两个字段，无 `interval` | 沿用 DPL `PlaySoundWsMsgDTO` 原字段（`uri` ↔ `url`、`playCount` ↔ `count`），`interval` 用 DPL 端常量表达（前端按 `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5` 轮播） |
| "间隔由 `AlarmConstants.SOUND_PLAY_COUNT_CFG_KEY` 控制" | `SOUND_PLAY_COUNT_CFG_KEY = "sound_play_count"` 是次数 key，不是间隔 key | 按任务字面要求新增 `SOUND_PLAY_INTERVAL_CFG_KEY = "sound_play_interval"`，但实际推送由 `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS` 兜底（DPL 无 system_config） |
| "`SOUND_PLAY_INTERVAL_CFG_KEY` = 'sound_play_interval' (PSM 同款)" | PSM 反编译产物无此 key（grep 全量无结果） | 仍按任务字面要求补齐；标注"任务声明 PSM 同款、实际 PSM 未出现，便于后续接 system_config 时不用改常量名" |
| "`dealClientAlarm` 升级判断 alarm 的 soundEnable" | PSM `dealClientAlarm` 本体不推 sound——它调 `deal(uuid)` → `sendAlarmMessage(alarm)`，由后者统一判 soundEnable | DPL 链路同款；本工单在 `sendAlarmMessage` 内真正激活 sound 分支，`dealClientAlarm` 自动覆盖 |
| "WS 推送逻辑 1:1 抄 PSM 反编译产物" | PSM `sendAlarmSoundWsMessage` 依赖 `ISystemConfigService` | 任务约束"不要修改其它模块"——无法引入该组件；DPL 改为 `AlarmConstants` 兜底常量，推送结构（`PlaySoundWsMsgDTO` + `WsMessage.type(ALARM_SOUND)` + `broadcastByUid(json, "web")`）仍 1:1 对齐 PSM |
| "调用 `WebSocketHandler.sendSoundBroadcast()`" | DPL `WebSocketHandler`（来自 `framework-starter-2.2.3-SNAPSHOT.jar`）仅有 `broadcastByUid(String, String)` / `broadcastByType(String, String)` / `broadcast(String)`，无 `sendSoundBroadcast` 方法 | 改用 `broadcastByUid(wsData.toJsonString(), "web")`——与 PSM `sendAlarmTextMessage` / `sendAlarmSoundWsMessage` 同款 API |
| "soundEnable 来源：join `defect_type` 表取 `sound_enable` 字段（PSM 用 LeftJoinQueryWrapper）" | PSM 反编译产物**无 `LeftJoinQueryWrapper` 字样**（grep 全量无结果）；PSM `sendAlarmMessage` 直接读 `defectType.getSoundEnable()`——`defectType` 是 `alarm.getDefectType()` 在 `add()`/`deal()` 里已反查好的内存对象 | DPL 链路同款：`add()` / `deal()` 在调 `sendAlarmMessage` 之前已经把 `defectTypeService.getByNameAndType(...)` 查到的 `DefectType` set 到 `alarm.setDefectType(...)`，`sendAlarmMessage` 直接读 `defectType.getSoundEnable()` |

> **核心结论**：推送链路与 PSM 反编译产物同构（结构、API、枚举、uid 都 1:1）；唯一差异是 DPL 用 `AlarmConstants` 兜底代替 `ISystemConfigService` 读 DB（任务约束所致）。这与 W-ALM-02 §6.1 已知限制一致，本工单把它从"完全跳过"升级到"用兜底常量推送"。

## 1. 改动的文件列表

| 状态 | 文件路径 | 行数（原 → 新） | 说明 |
|---|---|---|---|
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/constant/AlarmConstants.java` | 5 → **52** | 新增 4 个常量：`SOUND_PLAY_INTERVAL_CFG_KEY` / `SOUND_PLAY_DEFAULT_URI` / `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS` / `SOUND_PLAY_DEFAULT_COUNT` |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/AlarmRecordServiceImpl.java` | 521 → **576** | 新增私有 `sendAlarmSoundWsMessage(int)`；`handleAlarmNumGet` 末尾追加推送（total > 0 守卫）；`sendAlarmMessage` 的 soundEnable 分支由 log.debug 升级为真正推送 |

> **未修改任何 Controller**（任务要求）。
> **未修改任何其它模块**（任务要求；没有引入 `ISystemConfigService`、没有动 yingke / detect / line 包）。
> **未推 git**（任务要求）。

## 2. handleAlarmNumGet 升级要点

### 2.1 升级后逻辑

```java
@Override
public BaseResult handleAlarmNumGet() {
   // ... (W-ALM-02 的 selectAlarmCountByType 逻辑不变，省略) ...
   AlarmNumDTO alarmNum = AlarmNumDTO.builder()
      .totalNum(Integer.valueOf(total))
      .highNum(Integer.valueOf(specialAlarmNum))
      .build();
   // ====== W-ALM-05：handleAlarmNumGet 末尾推送声音到大屏 ======
   if (total > 0) {
      this.sendAlarmSoundWsMessage(total);
   }
   return BaseResult.build().ok().data(alarmNum);
}
```

### 2.2 关键决策

- **`total > 0` 守卫**：无未处理报警时不推声音，避免前端空播。
- **`count = total`**：把 `selectAlarmCountByType` 累加得到的 total 作为 `playCount` 推过去，让前端按报警总数轮播 N 次。
- **PSM 未在 handleAlarmNumGet 推声音**：PSM 反编译 `handleAlarmNumGet` 只 return `AlarmNumDTO`，无 WS 推送。任务简报要求"调 selectAlarmCountByType 后用 WebSocketHandler 推 PlaySoundWsMsgDTO"——本工单按任务要求补齐，**结构上 PSM 1:1 + 触发时机按任务要求**。
- **触发频率**：`AlarmRecordController` 第 110 行 `@GetMapping("/web/alarm/num")` → `handleAlarmNumGet()`。前端每次拉取都会触发一次；前端可加节流。

## 3. dealClientAlarm soundEnable 分支

### 3.1 链路分析

```
dealClientAlarm(lineNo, faceNo, reason)    // PSM 同款
    └─ this.deal(list.get(0).getUuid())    // 处理首条
        └─ deal(uuid):
            ├─ updateWrapper 把 solve 置 SOLVED
            └─ getOne(uuid) → defectTypeService.getByNameAndType(...)
                └─ alarm.setDefectType(defect)
                    └─ sendAlarmMessage(alarm)
                        └─ if (soundEnable == YES && solve == UNSOLVED)
                            └─ sendAlarmSoundWsMessage(1)   // ★ W-ALM-05 新激活
```

> PSM `dealClientAlarm` 本体没有"判断 soundEnable 推 sound"的分支——它走 `deal()` → `sendAlarmMessage()` → `sendAlarmSoundWsMessage()` 链路。
> 任务简报要求"`dealClientAlarm` 升级判断 alarm 的 soundEnable"——实际激活点在 `sendAlarmMessage`；一旦 `sendAlarmMessage` 真正推送，`dealClientAlarm` 链路自动覆盖。**未在 `dealClientAlarm` 本体添加新分支（与 PSM 1:1）**。

### 3.2 sendAlarmMessage soundEnable 分支升级

```java
// 之前（W-ALM-02）：
if (Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue())
   && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
   log.debug("defect alarm sound ws push skipped (system_config not wired).[alarm={}]", alarm);
}

// 现在（W-ALM-05）：
if (Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue())
   && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
   this.sendAlarmSoundWsMessage(AlarmConstants.SOUND_PLAY_DEFAULT_COUNT);
}
```

### 3.3 `defect_type.sound_enable` 取值路径

DPL 链路：

1. `add(AlarmDTO)`：`defectTypeService.listByAttribute(form.getType(), DefectType::getCategory)` → `sortDefectTypeByName.get(defectName)` → `alarm.setDefectType(...)`。
2. `deal(uuid)`：`defectTypeService.getByNameAndType(alarm.getDefectName(), alarm.getType())` → `alarm.setDefectType(...)`。
3. `sendAlarmMessage(alarm)`：直接读 `defectType.getSoundEnable()`（`DefectType` 模型 W-DET-01 已带 `soundEnable` 字段）。

> PSM 同款：PSM `sendAlarmMessage` 同样读 `defectType.getSoundEnable()`，`defectType` 是 `add()`/`deal()` 里已 set 好的内存对象——**不存在 SQL 层 LEFT JOIN**，任务简报里"PSM 用 LeftJoinQueryWrapper"与 PSM 反编译产物不符（已 grep 全量 PSM 反编译产物无 `LeftJoinQueryWrapper`）。

## 4. AlarmConstants 新增字段

| 字段 | 值 | 用途 |
|---|---|---|
| `SOUND_PLAY_INTERVAL_CFG_KEY` | `"sound_play_interval"` | 任务声明 PSM 同款；实际 PSM 反编译未出现，便于未来接 `ISystemConfigService` 时按 key 取值不用改常量名 |
| `SOUND_PLAY_DEFAULT_URI` | `"sound/alarm.wav"` | 默认声音 URI（DPL 静态资源相对路径，前端大屏拼 host 拿） |
| `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS` | `Integer.valueOf(5)` | 任务简报：interval 5s；DPL 端由前端按此值轮播 |
| `SOUND_PLAY_DEFAULT_COUNT` | `Integer.valueOf(1)` | 单条报警推送默认次数；PSM 从 `system_config.sound_play_count` 读，DPL 用兜底 |

```java
public class AlarmConstants {
    public static final String SOUND_PLAY_COUNT_CFG_KEY = "sound_play_count";     // 原有
    public static final String SOUND_PLAY_INTERVAL_CFG_KEY = "sound_play_interval"; // W-ALM-05 新增
    public static final String SOUND_PLAY_DEFAULT_URI = "sound/alarm.wav";          // W-ALM-05 新增
    public static final Integer SOUND_PLAY_DEFAULT_INTERVAL_SECONDS = 5;            // W-ALM-05 新增
    public static final Integer SOUND_PLAY_DEFAULT_COUNT = 1;                       // W-ALM-05 新增
}
```

## 5. sendAlarmSoundWsMessage 私有方法（新增）

```java
private void sendAlarmSoundWsMessage(int count) {
   try {
      PlaySoundWsMsgDTO soundMsg = new PlaySoundWsMsgDTO()
         .setUri(AlarmConstants.SOUND_PLAY_DEFAULT_URI)
         .setPlayCount(Integer.valueOf(count));
      WsMessage wsData = WsMessage.build()
         .type(WsTypeEnum.ALARM_SOUND.getValue())
         .data(soundMsg);
      this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");
   } catch (Exception ex) {
      log.warn("broadcast sound ws msg failed. cause: {}", ex.toString());
   }
}
```

### 5.1 与 PSM 推送结构 1:1 对齐

| 步骤 | PSM | DPL（W-ALM-05） | 1:1 |
|---|---|---|---|
| 1 | `WsMessage.build().type(WsTypeEnum.ALARM_SOUND.getValue())` | 同上 | ✅ |
| 2 | `new PlaySoundWsMsgDTO().setPlayCount(...).setUri(...)` | 同上 | ✅ |
| 3 | `wsData.data(msg)` | 同上 | ✅ |
| 4 | `webSocketHandler.broadcastByUid(json, "web")` | 同上 | ✅ |
| 5 | `try/catch` 防御式 log | 同上 | ✅ |

### 5.2 与 PSM 数据来源差异

PSM `sendAlarmSoundWsMessage(DefectTypePO)` 数据来源：

```java
// PSM 原版：
List<SystemConfigPO> configs = systemConfigService.listByConfigKey(
   [type.getSoundConfigKey(), "sound_play_count"]);  // e.g. ["defect_alarm_sound_uri", "sound_play_count"]
PlaySoundWsMsgDTO msg = new PlaySoundWsMsgDTO()
   .setPlayCount(Integer.parseInt(configs.get("sound_play_count").getConfigValue()))
   .setUri(configs.get(type.getSoundConfigKey()).getConfigValue());
```

DPL 兜底：

```java
// DPL 兜底：uri/interval/count 用 Constants 兜底（无 system_config）
.setUri(SOUND_PLAY_DEFAULT_URI)                       // "sound/alarm.wav"
.setPlayCount(Integer.valueOf(count))                 // 调用方传：handleAlarmNumGet=total / sendAlarmMessage=1
```

> PSM uri 按 `type.getSoundConfigKey()`（`defect_alarm_sound_uri` / `system_alarm_sound_uri` / `device_alarm_sound_uri`）三选一；DPL 兜底为统一 `sound/alarm.wav`。
> PSM count 按 `system_config.sound_play_count` 取；DPL 兜底按调用方语义（total / 1）。

## 6. 编译结果

### 6.1 任务指定的单文件命令

```powershell
cd E:\DEMO\数据采集
& X:\DataupLoad\jdk\bin\javac.exe -encoding UTF-8 -parameters `
   -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java `
   DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.java
```

输出：**(空)** → 退出码 `0`

零警告零错误（任务简报要求的命令直接通过）。

### 6.2 全 alarm 包批量回归

```powershell
$files = Get-ChildItem -Path DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm -Recurse -Filter *.java
& X:\DataupLoad\jdk\bin\javac.exe -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java $files
```

输出：
```
注: ...\AlarmTaskManager.java使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
exit=0
```

退出码 `0`，仅 1 条 `unchecked` 提示（位于 `AlarmTaskManager.java`，与本工单无关——W-F01-C 改 `@AllArgsConstructor` 时遗留）。

### 6.3 Controller 回归

```powershell
& X:\DataupLoad\jdk\bin\javac.exe -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java `
   AlarmRecordController.java DefectTypeController.java IgnoreAlarmController.java
```

输出：**(空)** → 退出码 `0`

三个 controller 全部编译通过，未引入回归。

### 6.4 字节码结构验证

```bash
javap -p com.hikrobotics.solution.module.alarm.service.impl.AlarmRecordServiceImpl | grep sendAlarmSound
# → private void sendAlarmSoundWsMessage(int);
```

`sendAlarmSoundWsMessage(int)` 私有方法已在字节码中存在（javap -p 显示 `private` 方法）。

```bash
javap -p -c com.hikrobotics.solution.module.alarm.service.impl.AlarmRecordServiceImpl | grep -E "sendAlarmSound|broadcastByUid"
# → 153: invokevirtual sendAlarmSoundWsMessage:(I)V       (sendAlarmMessage 内)
# →  98: invokevirtual sendAlarmSoundWsMessage:(I)V       (handleAlarmNumGet 内)
# → private void sendAlarmSoundWsMessage(int)             (方法定义)
# →   49: invokevirtual WebSocketHandler.broadcastByUid   (内部实现)
```

两个调用方 (`handleAlarmNumGet` + `sendAlarmMessage`) 都正确 invoke 新方法；方法内部调用 `WebSocketHandler.broadcastByUid(String, String)V`。

```bash
javap -p -cp X:\DataupLoad\target\classes com.hikrobotics.solution.module.alarm.constant.AlarmConstants
# → public static final String SOUND_PLAY_COUNT_CFG_KEY;
# → public static final String SOUND_PLAY_INTERVAL_CFG_KEY;        (W-ALM-05 新增)
# → public static final String SOUND_PLAY_DEFAULT_URI;             (W-ALM-05 新增)
# → public static final Integer SOUND_PLAY_DEFAULT_INTERVAL_SECONDS; (W-ALM-05 新增)
# → public static final Integer SOUND_PLAY_DEFAULT_COUNT;          (W-ALM-05 新增)
```

`AlarmConstants` 4 个新常量全部到位。

### 6.5 产物

```
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.class
  Length       : 23352 bytes
  LastWriteTime: 2026-07-24 19:22:43

X:\DataupLoad\target\classes\com\hikrobotics\solution\module\alarm\constant\AlarmConstants.class
  (重新编译)
```

## 7. 与 PSM 的最终对齐度（自评）

| 维度 | PSM 反编译 | DPL（W-ALM-05） | 与 PSM 1:1 |
|---|---|---|---|
| `sendAlarmMessage` 调用 soundEnable 分支 | ✅ | ✅ | ✅ 1:1 |
| `sendAlarmSoundWsMessage` 推送结构（`PlaySoundWsMsgDTO` + `WsMessage.type(ALARM_SOUND)` + `broadcastByUid(_, "web")`) | ✅ | ✅ | ✅ 1:1 |
| `sendAlarmSoundWsMessage` 数据来源 | `ISystemConfigService.listByConfigKey(...)` | `AlarmConstants` 兜底 | ⚠️ 数据源差异（任务约束无法引入 ISystemConfigService） |
| `handleAlarmNumGet` 推送声音 | ❌（PSM 不推） | ✅（按任务简报要求补齐） | ⚠️ PSM 没有该行为，本工单按任务要求超 PSM |
| `dealClientAlarm` → soundEnable 分支 | ✅（经 deal → sendAlarmMessage） | ✅（同款链路，sendAlarmMessage 激活） | ✅ 1:1 |
| `DealAlarmEvent` → `dealClientAlarmListener` → `dealClientAlarm` 链路 | ✅ | ✅ | ✅ 1:1（未修改） |
| 推送目标 uid | `"web"` | `"web"` | ✅ 1:1 |
| 异常处理（log warn + 吞掉） | ❌（PSM 不 catch） | ✅（DPL 加 try/catch 避免阻塞报警链路） | ⚠️ DPL 主动加固 |

## 8. 已知限制

1. **`ISystemConfigService` 未启用**：`SOUND_PLAY_DEFAULT_URI` / `SOUND_PLAY_DEFAULT_COUNT` 是兜底常量，无法像 PSM 那样从 `system_config` 表读运营配置的 uri / 播放次数；任务约束"不要修改其它模块"导致无法引入该组件。后续若要支持运行时配置，需要新建 `config` 模块（W-CONFIG-01 工单）。
2. **`SOUND_PLAY_INTERVAL_CFG_KEY` 未真正使用**：声明为常量但 DPL 没有读取它的链路；当前 interval 行为完全由前端按 `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5` 兜底。
3. **任务声明的"PSM 用 `LeftJoinQueryWrapper`"与 PSM 反编译产物不符**：grep 全量 PSM 反编译产物无 `LeftJoinQueryWrapper`；PSM `sendAlarmMessage` 直接读内存对象 `defectType.getSoundEnable()`（`defectType` 由 `add()`/`deal()` 提前反查）。本工单按 PSM 实际行为实现，没有在 SQL 层加 LEFT JOIN——因为不需要，且 `alarm_record.defect_name` 已存了 `defect_type.name`，`getByNameAndType` 已能反查。
4. **`handleAlarmNumGet` 在 PSM 不推声音**：本工单按任务简报"调 selectAlarmCountByType 后用 WebSocketHandler 推 PlaySoundWsMsgDTO"补齐，属于"超 PSM 1:1"的功能扩展。如果未来 PSM 升级确认不推声音，这里可以删掉（但当前任务明确要求保留）。
5. **`dealClientAlarm` 本体未改**：与 PSM 1:1（PSM `dealClientAlarm` 不直接判 soundEnable），soundEnable 判断在 `sendAlarmMessage` 内统一完成。
6. **声音 URI 是相对路径**：`SOUND_PLAY_DEFAULT_URI = "sound/alarm.wav"` 是相对路径，前端大屏需要拼 host 才能播放；如果前端静态资源不在根路径 `/sound/`，需要改这个常量（或者后续接 `ISystemConfigService`）。
7. **`sendAlarmMessage` 异常吞掉**：`catch (Exception)` + `log.warn` 模式与 `sendAlarmTextMessage` 一致；任何推送异常都不会阻塞报警链路（包括 yk 推送）。
8. **未推 git**：按任务要求，所有改动只在工作区，未 commit / push。

## 9. 文件改动行数总览

| 文件 | 状态 | 改动量 |
|---|---|---|
| `AlarmRecordServiceImpl.java` | 改 | +55 行（净增：576 - 521） |
| `AlarmConstants.java` | 改 | +47 行（净增：52 - 5） |
| **合计** | — | **+102 行** |

---

**签字**：Java worker（深度 1/1 子 agent）  
**任务状态**：✅ 已完成编译验证，WS 声音推送链路在 `handleAlarmNumGet` 和 `sendAlarmMessage` 两条路径全部激活；与 PSM 反编译产物在推送结构/API/uid 三个维度 1:1 对齐（数据来源因任务约束用 `AlarmConstants` 兜底代替 `ISystemConfigService`，见 §0 / §5.2 / §8.1）。
