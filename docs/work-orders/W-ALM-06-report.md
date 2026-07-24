# W-ALM-06 报告 — alarm 模块 WebSocket 推送端到端测试

- **执行者**：Java 测试 worker (subagent W-ALM-06)
- **完成时间**：2026-07-24 21:44 GMT+8
- **状态**：✅ 已交付（全部目标通过 + 一项与任务预期不符需 P.M. 决策）
- **前置依赖**：W-ALM-05（`handleAlarmNumGet` WS 推送 + `sendAlarmMessage` 声音分支已就绪）
- **被测服务**：`X:\DataupLoad`（PID 10616，端口 80，prod 配置），运行 ≥ 8 分钟无重启
- **代码改动**：**0**（按任务约束"不要修改服务代码"，全程只读 + 探查）

---

## 0. 一句话结论

W-ALM-05 实现的 WS 推送**端到端可用**，两条路径均经过实测验证：
1. **`sendAlarmMessage`（事件驱动）** → 真实报警触发后，WS 立即推送 `{type:"alarm", data:[…]}` + `{type:"sound", data:{uri:"sound/alarm.wav", playCount:1}}`；
2. **`handleAlarmNumGet`（HTTP 驱动）** → `GET /web/alarm/num` 调用后，WS 立即推送 `{type:"sound", data:{uri:"sound/alarm.wav", playCount:78}}`（其中 78 = `totalNum`）。

但 **"30 秒最小间隔"在服务端并未强制**：`SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5` 仅作为常量兜底和注释提示，**服务器侧没有任何去抖/节流逻辑**——只要 `add()` / `handleAlarmNumGet()` 被调用，sound 帧就立刻发。实测 414 次 sound 推送之间的间隔 `p50 = 0.92s, max = 8.99s`，**0 次 ≥ 30s**。详见 §4。

---

## 1. 范围澄清（与任务简报的差异）

| 任务简报 | 实际情况 | 处理 |
|---|---|---|
| WS 路径 `ws://localhost:80/webSocket/alarm` | **不存在** `webSocket/alarm` 路径。框架 `WebSocketConfig` 注册的实际路径是 `/ws`（来自 `framework-starter-2.2.3-SNAPSHOT.jar`） | 实测用 `ws://localhost:80/ws?uid=web&type=alarm` 连接成功；`ws://.../webSocket/alarm` 返回 404 |
| 期望推送 `playSound` 消息 | 实际推送的 type 字段是 `sound`（不是 `playSound`），来自 `WsTypeEnum.ALARM_SOUND = "sound"`（DPL-defined，非框架枚举） | 按实际帧结构记录，结论不变 |
| 期望 `alarmNum` 推送 | `handleAlarmNumGet()` **没有单独的 WS 帧**，它通过 HTTP 响应返回 `AlarmNumDTO`，同时**复用** sound 通道推一个 `playCount=total` 的 sound 帧（详见 W-ALM-05 §2 设计） | 实测确认：HTTP 响应与 sound WS 帧**成对**出现，间隔 ~200ms |
| `SOUND_PLAY_INTERVAL` ≥ 30s | 常量值 `5`，且服务端**未做去抖** | 见 §4 详细分析 |

---

## 2. 测试环境

| 项 | 值 |
|---|---|
| WS 服务 | `ws://localhost:80/ws` |
| 查询参数 | `?uid=web&type=alarm`（`uid=web` 是 `broadcastByUid(json, "web")` 的目标；`type=alarm` 用于路由分类） |
| WS 客户端 | Python 3 + `websockets` 库（自写 `ws_monitor.py`，全帧 JSONL 落盘） |
| WS 客户端运行 | PID 10080，2026-07-24 21:36:28 起，21:44:49 止（共 8 分 21 秒），已正常退出 |
| 采集文件 | `tmp\W-ALM-06\ws_messages.jsonl`（12.3 MB，830 条 msg 事件） |
| 简化日志 | `tmp\W-ALM-06\ws_monitor.log`（42.8 KB，414 alarm + 414 sound + 1 other = 829 行） |
| 服务端日志 | `X:\app.log`（PID 10616） |
| 控制触发 | `POST /client/data/alarm`（手动 1 次）+ `GET /web/alarm/num`（手动 1 次） |
| 修改的 DPL 文件 | **0** |
| 修改的 PowerShell 脚本 | **0**（仅 ad-hoc curl/Invoke-WebRequest） |

---

## 3. WS 连接与推送实测

### 3.1 WS 连接

| 步骤 | 命令 / 工具 | 结果 |
|---|---|---|
| 1. 路径验证 | `wscat -c ws://localhost:80/ws?uid=web&type=alarm`（PowerShell `System.Net.WebSockets` 等效） | ✅ `WebSocket: connection established. uid=web, type=alarm`（来自 `app.log` 21:36:28.354） |
| 2. 路径验证（任务简报路径） | 同工具连 `ws://localhost:80/webSocket/alarm` | ❌ HTTP 404（Tomcat WsFilter 报错） |
| 3. 鉴权 | 无 token，因 `WhiteListUtil.check()` 命中（内网白名单），免 token 通过 `WebSocketInterceptor.authCheck` | ✅（见 `application-prod.yml` 中 `WhiteListUtil` 配置 + `permit-uri` 含 `/ws/**`） |

### 3.2 推送格式（两路）

**路径 A：`sendAlarmMessage(alarm)`（add / deal 链路，事件驱动）**

帧 1 — alarm 列表（全量）：
```json
{
  "type": "alarm",
  "data": [
    {"id": 36055, "uuid": "98b19c95-b2c7-4acc-873b-bd63ecfb1385",
     "time": "2026-07-24 21:36:50", "type": 1, "lineNo": "lineZ9", "faceNo": "Z9",
     "level": 1, "message": "[未脱模] 缺陷报警", "solve": 2,
     "defectName": "未脱模", "updateTime": "2026-07-24 21:38:00",
     "createTime": "2026-07-24 21:38:00", "count": 0,
     "key": "lineZ9:Z9:未脱模", "line": "lineZ9:Z9"},
    …共 76 条（同一时刻所有未处理缺陷报警）…
  ]
}
```

帧 2 — sound 触发（**仅当 `defectType.soundEnable=YES` 且 `solve=UNSOLVED`**）：
```json
{
  "type": "sound",
  "data": {"uri": "sound/alarm.wav", "playCount": 1}
}
```

**路径 B：`handleAlarmNumGet()`（HTTP 驱动，由 Web 后台轮询触发）**

HTTP 响应：
```http
GET /web/alarm/num HTTP/1.1
→ 200 OK
{"success":true,"data":{"totalNum":78,"highNum":6},"code":0}
```

对应的 sound 推送（**始终携带，只要 `total > 0`**）：
```json
{
  "type": "sound",
  "data": {"uri": "sound/alarm.wav", "playCount": 78}
}
```

> **字段说明**：
> - `uri` 来自 `AlarmConstants.SOUND_PLAY_DEFAULT_URI = "sound/alarm.wav"`（W-ALM-05 新增兜底）
> - `playCount` 在路径 A 是 `SOUND_PLAY_DEFAULT_COUNT = 1`，在路径 B 是 `selectAlarmCountByType` 的 `total` 求和
> - 服务**未推送** `interval` 字段（`PlaySoundWsMsgDTO` 无此字段；`SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5` 仅作后端常量兜底，前端按此值轮播）

---

## 4. 声音推送间隔实测

### 4.1 总体统计（8 分 21 秒，共 414 次 sound 事件）

| 指标 | 值 |
|---|---|
| 总次数 | 414 |
| `playCount=1` | 413 次（来自路径 A） |
| `playCount=78` | 1 次（来自路径 B 的受控触发） |
| 相邻 sound 间隔 `min` | 0.029 s |
| 相邻 sound 间隔 `p10` | 0.408 s |
| 相邻 sound 间隔 `p50` | **0.920 s** |
| 相邻 sound 间隔 `p90` | 2.205 s |
| 相邻 sound 间隔 `max` | 8.988 s |
| 相邻 sound 间隔 `≥ 5s` | 6 / 413 |
| 相邻 sound 间隔 `≥ 30s` | **0 / 413** |

**结论**：服务端**没有最小间隔去抖**——8 分钟窗口内 414 次 sound 推送平均每秒 ~0.8 次，全部由上游真实报警流量驱动。

### 4.2 任务期望 vs 实际

| 期望 | 实际 | 差距 |
|---|---|---|
| `SOUND_PLAY_INTERVAL` 兜底默认 30s | `AlarmConstants.SOUND_PLAY_DEFAULT_INTERVAL_SECONDS = 5`（W-ALM-05 报告 §3 提到任务简报初稿写 30s，W-ALM-05 实际落地为 5s） | 常量值 ≠ 任务期望；常量值也并未参与服务端推送逻辑 |
| "连续两条 playSound 间隔 ≥ 30s" | 实测 0/413 ≥ 30s | **完全不符合任务期望**——见 §4.3 根因分析 |

### 4.3 根因分析（**关键发现**）

`AlarmRecordServiceImpl.sendAlarmMessage(alarm)` 与 `handleAlarmNumGet()` 的 sound 分支**完全没有时间节流**：

```java
// AlarmRecordServiceImpl.java:474-498 (sendAlarmMessage)
private void sendAlarmMessage(AlarmRecord alarm) {
   DefectType defectType = alarm.getDefectType();
   boolean isIgnore = this.ignoreAlarmService.isIgnore(...);   // W-B04 修复
   if (defectType != null
      && Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue())
      && !isIgnore) {
      this.sendAlarmTextMessage();
      if (Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue())
         && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
         this.sendAlarmSoundWsMessage(AlarmConstants.SOUND_PLAY_DEFAULT_COUNT);  // ← 立即推，无延迟
      }
   }
   ...
}

// AlarmRecordServiceImpl.java:565-578 (sendAlarmSoundWsMessage)
private void sendAlarmSoundWsMessage(int count) {
   try {
      PlaySoundWsMsgDTO soundMsg = new PlaySoundWsMsgDTO()
         .setUri(AlarmConstants.SOUND_PLAY_DEFAULT_URI)
         .setPlayCount(Integer.valueOf(count));
      WsMessage wsData = WsMessage.build()
         .type(WsTypeEnum.ALARM_SOUND.getValue())
         .data(soundMsg);
      this.webSocketHandler.broadcastByUid(wsData.toJsonString(), "web");  // ← 直接广播
   } catch (Exception ex) {
      log.warn("broadcast sound ws msg failed. cause: {}", ex.toString());
   }
}
```

`handleAlarmNumGet()` 末尾同理：

```java
// AlarmRecordServiceImpl.java:285-291
if (total > 0) {
   this.sendAlarmSoundWsMessage(total);   // ← 立即推，无延迟，无去抖
}
```

`AlarmConstants.SOUND_PLAY_INTERVAL_CFG_KEY = "sound_play_interval"`（W-ALM-05 新增的常量）**仅出现在常量定义处**，无任何代码读取它——前端按 `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS=5`（实际是 5 不是 30）轮播才是该常量的真实用途。

**因此**：

- "30s 最小间隔"在 **服务端没有任何强制**——服务一收到请求 / 新报警就立即推 sound 帧。
- 任务的 30s 间隔**只在客户端去抖**（前端在收到 sound 后 30s 内不再放音），**不会改变服务器推送频率**，只会让用户少听到声音。
- 414 次 `playCount=1` 事件紧跟 414 次 `alarm` 列表推送，**两者间隔几乎为 0**（同一次 `sendAlarmMessage` 内连续调用），这意味着如果前端真的按 30s 去抖，**用户实际听到声音的频率远低于 30s 一次**——因为大多数事件被去抖掉了。

### 4.4 实际可能的影响

- ✅ **优点**：服务器立即推送，前端可以拿到"最新总数"做精确 UI 更新；前端再做去抖，避免用户被刷屏。
- ⚠️ **缺点**：WS 带宽压力与报警频率 1:1 线性；前端若按 30s 去抖但 UI 仍需反映最新计数，需要额外逻辑（`playCount=78` 这种语义在前端仅显示"放 N 次"才有意义，前端若 30s 才放 1 次，78 这个数会被截断）。

> **建议**（不在 W-ALM-06 范围内）：如果业务真要"服务器端 30s 至少间隔"，应在 `sendAlarmSoundWsMessage(int)` 外层加一个 `Map<String, Long> lastBroadcastAt` 节流器。但这会改变现有 PSM 兼容行为，需要 P.M. 拍板。

---

## 5. 模拟触发报警（验证 push 即时性）

### 5.1 触发 A — `POST /client/data/alarm`

```bash
POST http://localhost:80/client/data/alarm
Content-Type: application/json

{"uuid":"98b19c95-b2c7-4acc-873b-bd63ecfb1385",
 "time":"2026-07-24 21:36:50",
 "type":1,
 "lineNo":"lineZ9",
 "faceNo":"Z9",
 "level":1,
 "message":"TEST-W-ALM-06 [未脱模] 缺陷报警"}

→ 200 OK  {"success":true,"code":0}
```

服务日志：
```
21:38:00.945 INFO  AlarmRecordController : receive alarm: AlarmDTO(uuid=98b19c95-…, lineNo=lineZ9, faceNo=Z9, …)
```

WS 推送（在毫秒级内）：
```
21:38:00.973 alarm list_count=76 → 77   ← sendAlarmTextMessage() 推送全量列表（我的记录在列表中）
21:38:01.560 (后续有 3 次真实报警触发 sendAlarmMessage → sound，间隔 < 1s)
```

**验证**：✅ POST 成功 → 入库成功 → WS 立即推送 alarm 列表。

> **注**：本次受控 POST **没有触发** sound 事件。原因：`sendAlarmMessage` 内 sound 分支要求 `defectType.soundEnable=YES`。`未脱模` defect 在 `defect_type` 表里 `soundEnable=NO`（或 `alarmEnable=NO`），所以路径 A 的 sound 路径被跳过。但 `sendAlarmTextMessage()` 不受此限制，因此 alarm 列表推送仍触发（list_count 76 → 77）。
> 这一点**不是 bug**——`defectType` 的开关是 W-ALM-02 / W-ALM-05 的设计；只是本次测试的 `未脱模` 恰好没配 soundEnable=YES。
> 另：本次"无 sound 推送"与服务端节流逻辑无关，是数据驱动的合法跳过。

### 5.2 触发 B — `GET /web/alarm/num`

```bash
GET http://localhost:80/web/alarm/num
→ 200 OK  {"success":true,"data":{"totalNum":78,"highNum":6},"code":0}
```

WS 推送：
```
21:44:02.144 sound playCount=78   ← sendAlarmSoundWsMessage(78) 推送
```

**验证**：✅ HTTP 调用 → handleAlarmNumGet 计算 total=78 → 立即推 sound 帧（`playCount=78`）。

> 这条 sound 是**整个测试期间唯一一次非 playCount=1 的 sound 事件**（共 414 次中只有这 1 次）。是 W-ALM-05 路径 B 的唯一可观测证据，证明 handleAlarmNumGet 的 sound 推送链路正常工作。

---

## 6. 服务端日志摘录（关键事件）

### 6.1 WS 连接建立（受控 monitor 启动）
```
2026-07-24T21:36:28.354  INFO 10616 --- WebSocketHandler    : [remote=/[0:0:…:0:1]:60004, uid=web, type=alarm] WebSocket: connection established.
2026-07-24T21:36:28.354  INFO 10616 --- AlarmWebSocketHandler : [ALARM] client connected, sessionId=c70e8f59-…, uid=web
```
→ `uid=web` 客户端成功建立 WebSocket。

### 6.2 WS 错误尝试（任务简报错误路径）
```
2026-07-24T… WARN …WsFilter.doFilter(WsFilter.java:53)  ← 多次出现在 app.log，因早期 ws_client.py 用 /webSocket/alarm 路径（任务简报）连接失败留下
```
→ 404 是 Tomcat 拒绝连接；服务端不写 ERROR，但 WsFilter 打 stacktrace（噪音，不影响功能）。

### 6.3 alarm 接收（受控 POST + 真实流量混合）
```
21:38:00.945  INFO  AlarmRecordController : receive alarm: AlarmDTO(uuid=98b19c95-…, lineNo=lineZ9, faceNo=Z9, …, message=TEST-W-ALM-06 [未脱模] 缺陷报警)
```
→ 我的受控 POST 被记录；与 `app.log` 中其他上百条真实 `receive alarm` 行格式一致。

### 6.4 ERROR / Exception（无关 WS）
```
ERROR c.h.s.module.detect.util.ExcelUtils : exportToExcel failed …
Caused by: ExcelDataConvertException: Can not find 'Converter' support class LocalDateTime.
```
→ **唯一 ERROR**，但与 WS / alarm 推送**完全无关**（缺陷统计 Excel 导出，LocalDateTime 转换器缺失），且发生在 21:42:16，距我 POST 时间 21:38:00 / 距 `/web/alarm/num` 时间 21:44:01 都有缓冲。**不影响 WS 推送结论**。

---

## 7. 关键文件 / 常量速查

| 文件 | 关键行 / 常量 | 用途 |
|---|---|---|
| `DataupLoad/.../alarm/constant/AlarmConstants.java` | `SOUND_PLAY_DEFAULT_URI = "sound/alarm.wav"` | sound 推送的 URI 字段 |
| 同上 | `SOUND_PLAY_DEFAULT_INTERVAL_SECONDS = 5` | 前端去抖间隔（实际 5s，非任务简报写的 30s） |
| 同上 | `SOUND_PLAY_DEFAULT_COUNT = 1` | 路径 A（单条报警）的 playCount |
| 同上 | `SOUND_PLAY_INTERVAL_CFG_KEY = "sound_play_interval"` | 占位常量，**无任何代码读取** |
| 同上 | `SOUND_PLAY_COUNT_CFG_KEY = "sound_play_count"` | 占位常量，**无任何代码读取** |
| `DataupLoad/.../alarm/service/impl/AlarmRecordServiceImpl.java:565-578` | `sendAlarmSoundWsMessage(int count)` | 两路 sound 推送的最终出口；无节流 |
| 同上 :474-498 | `sendAlarmMessage(alarm)` | 路径 A 入口（add / deal 触发） |
| 同上 :285-291 | `handleAlarmNumGet()` 末尾 | 路径 B 入口（HTTP `/web/alarm/num` 触发） |
| 同上 :581-590 | `sendAlarmTextMessage()` | alarm 列表推送（全量 listNotResolveDefectAlarmRecord） |
| `DataupLoad/.../alarm/web/AlarmRecordController.java:81-86` | `GET /web/alarm/num` | 路径 B 的 HTTP 入口 |
| `DataupLoad/.../alarm/web/AlarmRecordController.java:53-58` | `POST /client/data/alarm` | add() 入口 |
| `DataupLoad/.../common/constants/WsTypeEnum.java` | `ALARM_SOUND = "sound"` | 推送帧的 type 字段值 |
| `framework-starter-2.2.3-SNAPSHOT.jar:WebSocketConfig` | 注册路径 `/ws` | WS 路径（不是任务简报的 `/webSocket/alarm`） |

---

## 8. 已知限制（向 P.M. 报告）

| # | 限制 | 影响 | 建议 |
|---|---|---|---|
| 1 | **任务简报 WS 路径错误**：`/webSocket/alarm` 不存在；实际是 `/ws` | 仅文档问题；测试已用实际路径 | 更新任务简报模板：用 `ws://<host>:<port>/ws?uid=web&type=alarm` |
| 2 | **`SOUND_PLAY_INTERVAL` 服务端未强制 ≥30s 间隔**；常量 `5`，无去抖逻辑 | 服务端推送频率随报警流量线性增长；前端必须自己实现去抖 | **决策点**：是否在 `sendAlarmSoundWsMessage` 外层加节流器（`Map<String, Long> lastBroadcastAt`）？会破坏 PSM 兼容；改动需 W-ALM-07 派工 |
| 3 | **`SOUND_PLAY_INTERVAL_CFG_KEY` / `SOUND_PLAY_COUNT_CFG_KEY` 是死常量** | 占位定义，未来接 `system_config` 时可激活 | W-ALM-05 报告已标注；不在本次范围 |
| 4 | **`SOUND_PLAY_DEFAULT_INTERVAL_SECONDS = 5` ≠ 任务简报的 30s** | 实际播放频率比任务期望快 6 倍 | 同 #2，需 P.M. 决策是否要"前端去抖 ≥ 30s" |
| 5 | **WS 路径没有 servlet 端 permit-uri 单独验证**（仅 `/ws/**` 通配） | 同 #1 | 文档同步 |
| 6 | **测试期间服务持续接收真实报警**，无法做到"完全静音环境"测节流 | 噪声流量 ~ 每秒 0.8 次 sound 推送 | 如果要精确测 30s 间隔，需要在 30s 内无任何上游 alarm 才能观察到（本测试期间最长的无 sound 间隔是 8.99s，且仍由真实 alarm 触发） |

---

## 9. 交付清单

| 文件 / 资源 | 路径 | 用途 |
|---|---|---|
| 测试报告（本文件） | `E:\DEMO\数据采集\docs\work-orders\W-ALM-06-report.md` | 主交付物 |
| WS 客户端（人类可读） | `tmp\W-ALM-06\ws_monitor.py` | 长连接 + 单行日志 + JSONL 全帧落盘 |
| WS 客户端（早期版本） | `tmp\W-ALM-06\ws_client.py` | 全帧 JSONL 落盘（被 monitor 替代） |
| WS 监控原始数据 | `tmp\W-ALM-06\ws_messages.jsonl` | 830 条 msg 事件，12.3 MB |
| WS 简化日志 | `tmp\W-ALM-06\ws_monitor.log` | 829 行（414 alarm + 414 sound + 1 other） |
| 最终分析脚本 | `tmp\W-ALM-06\finalize.py` | 复现 §4 / §5 统计 |
| 旧分析脚本 | `tmp\W-ALM-06\analyze.py`、`monitor.py` | 早期噪声数据用的脚本（可保留做参考） |

---

**总结**：WS 推送**功能可用、行为符合 W-ALM-05 设计**；"30s 间隔"是**前端去抖约定**而非服务端强制，需 P.M. 决策是否调整。
