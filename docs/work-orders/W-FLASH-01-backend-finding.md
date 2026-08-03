# W-FLASH-01 进展通报 + 后端根因发现（sendScreen NPE）

**日期**: 2026-08-02 14:20
**状态**: ⚠️ 前端已改完待验证，发现后端 WS 推送长期 NPE（疑似闪烁真正根源）

---

## 一、前端 1-5 改造已完成 ✅

`DataupLoad-web`（W-FLASH-01 brief）：
1. **OccupancyPanoramaBar.vue** — 删独立 `setInterval` 轮询，改 `subscribeScreen` 消费 WS 快照
2. **RealTime.vue** — `applySnapshotToSelected` 改浅比较，值变化才替换引用；watch 去 `deep`
3. **echarts** — `init-once` + `flushChart()` 节流增量 `setOption(notMerge:false)`，不再整图重建
4. **loading** — KPI 去闪烁，首条静默加载
5. **stale 降级** — WS 断线停最后一帧 + "连接断开"标注，10s 新鲜度心跳

✅ vite build PASS（EXIT=0），新 bundle `index-DL20edoN.js` 已部署到 `DataupLoad/web/`，后端 200 能提供（/ /index.html /assets/index-DL20edoN.js 均 200）。

---

## 二、重大发现：后端 WS 快照推送长期 NPE ⚠️（新旧问题查证）

**证据**：
- 8/1 backup 日志 `DataupLoad.0.log`：`GlobalTaskManager.sendScreen:140 sendScreen failed: java.lang.NullPointerException` **全天每 5s 一次**，全天 12985 次 sendScreen 调用 **0 次成功**
- 8/2 当前日志：同样每 5s 一次 NPE，**一直持续至今**

**含义**：
- `sendScreen` 每 5s `broadcastByType(json,"screen")` 但**构建 ScreenDataDTO 时抛 NPE → WS type=screen 通道推不出完整快照**
- 前端 OccupancyPanoramaBar / RealTime 靠 WS screen 快照 → **收不到可用数据**
- **这极可能就是老板看到的"上座率/实时页闪烁"的真正后端根源**（8/1 全天存在，老板 8/1 反馈闪烁）

**定位**：`ScreenServiceImpl.buildScreenData()/getCilentStatusList()`。疑点代码（getCilentStatusList）：
```java
lineStatusMap.getOrDefault(status.getLine(), new ArrayList<>());  // status.getLine() 可能异常
...
lineStatusMap.putIfAbsent(status.getLineNo()+":"+status.getFaceNo(), lineStatusList);  // key 不一致
```
但日志 `ex.toString()` 无堆栈，需加 `printStackTrace` 或临时打堆栈定位精确行。

---

## 三、登录 10500 也阻塞浏览器验证 ⚠️

- `POST /web/auth/login` (sha256 Abc12345) → `{"success":false,"code":10500,"message":"操作异常"}`
- 10500 是 W-FRONT-04-C 已修过的 mapper-locations 错误，**现在复发**
- DB `account` 表 super_admin id=1 hash=`$2b$10$zrWeYn3...`（E4 worker 改密码 / F 子单修复的历史遗留）

---

## 四、需要老板拍板

**W-FLASH-01 前端已做完**，但没有可用的 WS screen 数据 + 登录 10500 挡路，无法在浏览器实测"不闪烁"。

**建议优先修后端 sendScreen NPE**（可能直接消灭闪烁），需：
1. 定位 NPE 精确行（临时堆栈）
2. 修复 `ScreenServiceImpl`
3. 重新编译 + 重启后端 PID 28104（⚠️ 产线在依赖后端收报警，需挑时段）

**请老板确认**：
- A. 是否先派后端工单修 sendScreen NPE（核心）+ 10500 → 再回验证前端
- B. 还是前端 W-FLASH-01 先这么交付，后端另开单
