# W-FLASH-01 关键进展 + 需要老板授权重启后端

**时间**: 2026-08-02 14:35 ｜ **状态**: 前端 1-5 已完成，发现闪烁真正根因在后端 WS 推送（已实测证实）

---

## ✅ 前端 1-5 已全部改完 + build PASS + 已部署

`DataupLoad-web`（Vue3 SPA，老板要的 1-5）：
1. OccupancyPanoramaBar 删独立 5s 轮询 → 改 `subscribeScreen` 消费 WS
2. RealTime.vue `applySnapshotToSelected` 改浅比较增量 patch，watch 去 deep
3. echarts init-once + 节流增量 `setOption(notMerge:false)`
4. KPI 去 loading 闪烁
5. WS 断线 stale 降级（停末帧 + "连接断开"标注）

✅ vite build EXIT=0 → 新 bundle `index-DL20edoN.js` + CSS 已部署 `DataupLoad/web/`
✅ 后端静态资源 200（/ 、/index.html、/assets/index-DL20edoN.js）

---

## 🔴 重大根因：后端 `sendScreen` 每 5s NPE → WS screen 通道 0 数据（实测证实）

### 实测证据（不重启、只读探测）
写 WS 客户端连 `ws://127.0.0.1:8080/ws?uid=probe&type=screen`：
- 连接 **OPEN 成功**
- **12 秒内收到 0 条消息**（sendScreen 按 5s 应推 2 条+）

### 日志证据
- `GlobalTaskManager.sendScreen:140 sendScreen failed: java.lang.NullPointerException`
- **8/1 全天 12985 次 sendScreen 调用，0 次成功**（0/12985）
- 8/2 至今每 5s 1 次，持续
- 后端 28104 自 7/31 13:41 未重启，一直如此

### 含义
前端 WS screen 通道**收不到任何快照** → 实时页/上座率条无数据 → 前端只能靠残余 timer 或 loading，**这就是"闪烁"的直接后端根源**（时间线与老板 8/1 反馈完全吻合）。

---

## 🟡 登录 10500 复发（阻塞浏览器实测）
- `POST /web/auth/login`(sha256 Abc12345) → `{"success":false,"code":10500,"message":"操作异常"}`
- W-FRONT-04-C 修过（mapper-locations），现复发；需重启看新堆栈定位

---

## 💡 修复路径（需老板授权重启后端）

**修 `ScreenServiceImpl.sendScreen` NPE + 验证 10500** 都需要**重启后端 PID 28104**（7/31 老进程）。这会**瞬时中断产线报警接收（秒级）**。

建议流程（一次重启）：
1. `GlobalTaskManager.sendScreen` catch 临时打**完整堆栈**（定位 NPE 精确行）
2. 重启后端 → 从新日志拿 NPE 堆栈 + 10500 堆栈
3. 修 `ScreenServiceImpl` NPE + 修 10500 mapper → 重编译 → 再重启
4. 回测 WS screen 数据到达 + 登录成功 → 浏览器验证前端不闪烁

---

## 🤔 请老板拍板

- **A（推荐）**：授权我重启后端，按上面 4 步一次性修好 sendScreen NPE + 10500，再完成前端闪烁验证 → commit + push
- **B**：前端 W-FLASH-01 先按现状交付（代码已改好），后端 sendScreen/10500 另开后端工单，等老板另行拍板重启时机

我强烈建议 A —— sendScreen NPE 是闪烁根源，不修它前端 1-5 改造没有数据可驱动，等于白改。产线中断仅秒级，老板可挑时段。
