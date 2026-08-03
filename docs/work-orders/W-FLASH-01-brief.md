# W-FLASH-01 工单 — 实时页闪烁根治（对齐 PSM 无感刷新）

**派工**: 2026-08-02
**优先级**: P0（老板实测"界面又在闪烁"）
**目标**: 消除实时页 `/realtime` 的视觉闪烁，复刻 PSM 老 SPA「数据刷新完全无体感」行为
**方向**: 方案 A（统一 WS 单一刷新源 + 增量 patch 渲染），1-5 全做，一张单

---

## 1. 背景 / 根因（PM 已锁定）

上次闪烁（W-FRONT-FLASH，7-30）已在源码修复（路由守卫改 user store + 去 transition）。**本次闪烁是全新的**，由 W-FRONT-05-B2 上座率全景条引入，只在实时页 `/realtime` 出现。当前部署 bundle `index-DcwZU6hD.js`（8/1 14:51）即含该功能。

**根因 = 三条独立刷新/重绘叠加，相位错开，肉眼可见闪烁：**

| # | 刷新源 | 说明 |
|---|---|---|
| A | **WS 全局单例**（App.vue 连接，服务端每 5s `broadcastByType(json,"screen")` 全量快照） | RealTime.vue `applySnapshotToSelected` 每次 `selectedRealtime.value = {全新对象}` → `watch(..., deep:true)` → **每 5s 整张 echarts `setOption` 全量重绘** + KPI 数字跳动 |
| B | **OccupancyPanoramaBar 独立 5s 定时器** | 组件 `onMounted` 起 `setInterval` 每 5s `lineStore.load(true,true)`，与 WS 推送**不同步**（各自独立起时）→ 顶部上座率条 + 左栏线列表跟着跳 |
| C | **OccupancyPanoramaBar 首次非 silent load** | `onMounted` 第一句 `lineStore.load(true)`（非 silent）置 `lineStore.loading=true→false` → KPI 卡 `loading` 瞬变 `···`→真值，一次闪 |

**结论：非 bug，是架构叠架。PSM 只有 WS 一条源 + 增量 patch，故无体感。**

---

## 2. PSM 对齐结论（Worker 必须理解）

- PSM 后端推频 = 我们 = **每 5s `broadcastByType(json,"screen")` 全量快照**（已从逆向文档确认，推频无差异）
- PSM 前端"无体感"的实现真相 = **只消费 WS 这一条源，且增量 patch 字段、不重建整棵 DOM / 不整图重绘**
- 我们当前的闪烁恰恰是"双源 + 全量对象替换 + echarts 整图 setOption + loading 瞬变"三重叠加
- **本工单 = 复刻 PSM 消费策略，不新造架构**

---

## 3. 实施步骤（1-5 全做）

### 步骤 1 — 砍掉独立刷新源（核心）
`DataupLoad-web/src/components/OccupancyPanoramaBar.vue`：
- **删除** 内部 `setInterval` 定时器 `startTimer()/stopTimer()` 及相关代码
- 改为消费 WS 快照：引入 `subscribeScreen`（`src/stores/screen.ts`），`onMounted` 订阅、`onBeforeUnmount` 退订
- 上座率数据源从 `lineStore.lines[].realtime.occupancyRate` 改为 WS 快照（`snap.lines[]` 里的 `realTimeDetectData.occupancyRate`），按 `lineNo` 聚合 4 面取平均——**保持原有的 buildCells 逻辑与颜色阈值规则不变，只换数据源**
- 全页面只剩 WS 一条刷新源

### 步骤 2 — 增量 patch，不重建对象
`DataupLoad-web/src/views/RealTime.vue` `applySnapshotToSelected`：
- 不要每次 `selectedRealtime.value = {全新对象}`
- 改为**浅比较/merge**：值没变化的不触发渲染；对象引用尽量保持稳定（对子字段赋新值而非整体换对象）
- 目标：WS 快照未变 → 不触发 `watch` / 不重绘

### 步骤 3 — echarts 低开销更新 + 节流
`DataupLoad-web/src/views/RealTime.vue`：
- WS 回调改为只更新 series 数据点：`chart.setOption({series:[{data}]}, {notMerge:false})`，**不重建 xAxis/yAxis/grid/backgroundColor 等静态配置**（静态配置初始化时定一次）
- 加分号节流：同一秒内多个 WS 帧合并，**每 5s 最多重绘 1 次**；数据值无变化时不重绘
- `renderChart()` 重型逻辑（init 全 option）只在首次/线切换时执行

### 步骤 4 — remove loading 瞬变
- `OccupancyPanoramaBar.vue` 首次加载改为 **silent**（`lineStore.load(true, true)` 或直接用已有 WS 快照首帧），不置 `loading`
- 确认 `kpiCards.loading` 在 silent WS 刷新下恒定，不出现 `···`→真值
- `RealTime.vue` 其余 `load(true)`（onMounted）保持非 silent 仅首屏，不影响

### 步骤 5 — stale 降级（WS 断线）
- WS 断线时（`screenState.wsState !== 'open'` 或快照超过阈值秒数未更新）：
  - 顶部上座率条 + KPI + 折线 **停留最后一帧数据**，不闪成空白
  - 可视化标注"连接断开/数据可能滞后"（如小圆点/字样，样式轻量）
  - 复用 `W-PERF-B` 已有的 `isScreenLive()`/心跳逻辑（`screenState.lastUpdate` 10s 阈值），不重复造

---

## 4. 验收标准（端到端）

| # | 验收 | 判定 |
|---|---|---|
| 1 | 实时页观察 30s | 无肉眼可见闪烁/跳动（KPI 数字、顶部条、折线均稳定，仅数值变化处自然更新） |
| 2 | Network/WS | 只有一条 `/ws?type=screen` 连接；**OccupancyPanoramaBar 不再发起额外 `/web/line/list` 轮询** |
| 3 | WS 断线模拟 | 断开后页面停留最后帧 + 显示"连接断开"状，不闪空白 |
| 4 | 8 路由切换 + 登录 | 不回归上次 W-FRONT-FLASH / W-FRONT-04-C（无 403 踢出、reload 路由保留） |
| 5 | vite build + 部署 | PASS，`Copy-Item` 到 `DataupLoad/web/`，`curl /` 返回新 bundle hash |

---

## 5. 约束（Worker 必须遵守）

- **只改前端**（`DataupLoad-web/`），后端零改动
- **不碰** `router/index.ts`、`MainLayout.vue`（上次闪烁修复，避免回归）
- screen store 是模块作用域 reactive（非 Pinia），订阅/退订模式参照 `RealTime.vue` 现有 `subscribeScreen` 用法
- 上座率颜色阈值（红<warn / 黄<good / 绿≥good / 灰=无数据）与 `listSystemConfig` 读取逻辑**保持不变**，只换数据源
- 编码 UTF-8 无 BOM，中文注释
- 完成后：vite build PASS → `robocopy`/`Copy-Item dist\* -> DataupLoad\web\` 部署（后端不重启，静态文件覆盖即可）→ 浏览器实测 30s 观察无闪烁 → 截图存档 → commit + push origin main

---

## 6. 交付物

1. 改动 diff（OccupancyPanoramaBar.vue / RealTime.vue，若涉及 screen store 或 api 一并列出）
2. vite build + 部署成功证据（新 bundle hash）
3. 浏览器实测截图（实时页 30s、WS 断线降级态）
4. 报告 `docs/work-orders/W-FLASH-01-report.md`（根因复核 + 改动清单 + 验收 5 项 PASS/FAIL + 风险）
5. commit + push origin main

**Commit message**: `W-FLASH-01: 实时页统一 WS 单一刷新源 + 增量渲染 (根治闪烁)`
