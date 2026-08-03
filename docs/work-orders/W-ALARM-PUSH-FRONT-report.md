# W-ALARM-PUSH-FRONT — 报警悬窗无数据（前端 WS 解析 bug）

- **日期**：2026-08-03
- **状态**：✅ 修复完成 + 部署 + headless 验证通过
- **报障**：老板 8/3 18:50「大屏上的这个报警悬窗还是没有数据」

---

## 一、现象

老板登录平台后，报警悬窗（AlarmHint 悬浮球，右上角铃铛）**一直显示"无未处理报警"空状态**，无数字徽章；大屏"最新报警"列表一度也为空。

## 二、排查过程

### 初步判断（排除项）
- 后端 `/web/alarm/list` → **有数据**（total 125 万条，最新报警持续写入）✅
- 后端 WS 推送 → **正常广播**（连 uid=1&type=alarm 能收到 `{"type":"alarm","data":[...]}`）✅
- 大屏 REST snapshot → 登录后 `/web/alarm/list?pageSize=10`、`/web/line/list` 全部 200 ✅

### 真因（前端 2 个解析 bug + 1 个时序问题）

**① WS 消息字段错位（主因）**
后端 `WsMessage.build().type(ALARM).data(alarms)` 序列化后是：
```json
{"type":"alarm","data":[{...}, {...}]}
```
前端 `stores/alarm.ts` + `views/Alarm.vue` 的 onMessage 读的是 **`msg.payload`**：
```js
const payload = msg?.payload   // ← 不存在！后端字段是 data
if (!payload || typeof payload !== 'object') return  // ← 直接丢弃
```
→ **WS 推送的报警前端一条都收不到**。

**② 数组不解析（次因）**
后端 `sendAlarmTextMessage()` 推的是未处理报警**数组**（`data: [...]`），前端只认**单条对象** → 即使读到 data 也会 normalize 失败返回 null。

**③ baseline 时序问题（存量数据进不来）**
`App.vue onMounted` → `connectAlarmSingleton()` → 立即 `loadAlarmBaseline()`（GET /web/alarm/list?pageSize=5&solve=2）。**未登录访问时**该请求无 cookie → **10401 认证无效** → baseline 失败 → recent 空。**登录成功后没有任何地方重拉 baseline** → 存量未处理报警永远进不来。

## 三、修复（前端 3 个文件）

| 文件 | 改动 |
|------|------|
| `DataupLoad-web/src/stores/alarm.ts` | onMessage 改读 `msg.data ?? msg.payload` + **数组逐条遍历** normalize + pushRecent |
| `DataupLoad-web/src/views/Alarm.vue` | 同上（Alarm 页 WS 增量解析） |
| `DataupLoad-web/src/views/Login.vue` | 登录成功后 `void loadAlarmBaseline()`（此时 satoken cookie 已带，重拉基线成功） |

部署：vite build（13s）→ 新 bundle `index-C8u2luyA.js` 拷到 `DataupLoad/web/assets/` + 更新 index.html 引用。

## 四、验证（Playwright headless，全部通过）

```
报警悬窗 hover: 5 条真实报警（line8A/7A/6B/6A 剥除机未就位等）
铃铛徽章: 99+（未处理报警 256 条）
报警页: 76 行报警数据
大屏: alarmItems=10 + WS 已连接
```

## 五、残留

- 登录前 console 仍有 1 条无害的 `10401 认证无效` 警告（未登录时 baseline 尝试），登录后 Login.vue 重拉成功，不影响功能。后续可优化：未登录时不触发 baseline。

## 六、待老板验收

1. 刷新 `http://127.0.0.1:8080/`（**Ctrl+Shift+R 强刷**，避免旧 bundle 缓存）
2. 登录 `super_admin / Abc12345`
3. 右上角铃铛应显示 **99+** 数字徽章；hover 弹出 5 条未处理报警
4. 触发新报警 → 悬窗实时 +1 + 声音（sound_enable 时）
