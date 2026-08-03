# W-ALARM-PUSH — 报警推送根治（WS uid 广播错位）

- **日期**：2026-08-03
- **状态**：✅ 修复完成，待老板浏览器验收
- **报障**：老板 8/3「报警推送功能并未实现」（登录后前端收不到实时报警）

---

## 一、现象

老板在平台上登录后，触发/产生报警时前端**收不到实时报警推送**（无弹窗、无声音、无徽章）。

之前 `W-ALM-05 / W-DEFECT-CFG` 已把报警**落库**打通（alarm_record 有数据），但推送链路「后端 WS → 前端」一直没通。

## 二、根因（100% 实证）

### 前端连接方式
前端所有报警 WS 连接（`stores/alarm.ts` + `Alarm.vue`）统一用：

```
uid = userStore.id ? String(userStore.id) : 'web'   // 登录后 uid = 用户id（super_admin → 1）
type = 'alarm'
```

即**登录态前端 uid=1，type=alarm**。

### 后端推送方式（错位点）
`AlarmRecordServiceImpl` 的推送方法：

```java
sendAlarmTextMessage()  →  webSocketHandler.broadcastByUid(json, "web")   // 推给 uid="web"
sendAlarmSoundWsMessage() → webSocketHandler.broadcastByUid(json, "web")  // 推给 uid="web"
```

`broadcastByUid(msg, "web")` 内部只匹配 `session.attr("uid") == "web"` 的会话。

### 后果
- 后端把报警推给 `uid="web"`
- 登录态前端连接是 `uid=1`
- **uid=1 ≠ uid="web" → 永远匹配不到 → 登录用户收不到任何报警推送**
- 只有未登录（uid 回退到 'web'）才可能收到

### WS 实测实证（修复前）
同时连两个 WS：`uid=1&type=alarm` 和 `uid=web&type=alarm`

```
[uid=web] RECV: {"type":"alarm",...}  ← 收到
[uid=web] RECV: {"type":"sound",...}  ← 收到
[uid=1]   got: 0                        ← 一条都没有
```

## 三、修复

把 `AlarmRecordServiceImpl` 两处 `broadcastByUid(…,"web")` 改为 `broadcastByType(…,"alarm")`：

```java
// 改前
this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");
// 改后：按 type 广播，所有 type=alarm 连接（无论 uid）都能收到
this.webSocketHandler.broadcastByType(wsData.toJsonString(), "alarm");
```

- `sendAlarmTextMessage()` → 报警列表推送
- `sendAlarmSoundWsMessage()` → 报警声音推送

按 **type** 广播后，登录态用户（uid=用户id）和其他端都能收到，与 `AlarmWebSocketHandler.push()`（已用 `broadcastByType(json,"alarm")`）保持一致。

## 四、验证

### 修复后 WS 实测（uid=1 登录态）
```
[uid=1] RECV: {"type":"alarm","data":[{...5条未处理报警...}]}
[uid=1] RECV: {"type":"alarm",...}
[uid=1] RECV: {"type":"sound","data":{"uri":"sound/alarm.wav","playCount":1}}
POST /client/data/alarm -> {"success":true,"code":0}
=== uid=1 got: 3 msgs ✅
```

### 产线未受影响
重启后最近 2 分钟 `/client/data/detect`：**ok 412 条 / 10500 0 条** ✅

## 五、改动清单

| 文件 | 改动 |
|------|------|
| `DataupLoad/src/main/java/.../AlarmRecordServiceImpl.java` | 2 处 `broadcastByUid("web")` → `broadcastByType("alarm")` |

只有 1 个 Java 文件，2 处改动。

## 六、待老板验收

1. 打开 `http://127.0.0.1:8080/`，登录 `super_admin / Abc12345`
2. 保持登录态（uid=用户id），停在任意页
3. 触发/等待一条报警产生
4. 应看到：**实时弹窗/侧滑报警 + 报警徽章 +（若 sound_enable）报警声音**
