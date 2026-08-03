# W-FRONT-05-B2 report — 上座率顶部全宽条

## 状态
✅ 完工并部署

## 产出文件
- **新增** `DataupLoad-web/src/components/OccupancyPanoramaBar.vue`（~10KB）
  - 玻璃风顶部全宽条：38 线热力条 + [显示数值]开关 + 收起/展开 + 红黄绿灰汇总 + 平均
  - 消费 `useLineStore`（RealTime.vue 已 load），不重复 fetch `/web/line/list`
  - 读 `/web/system-config` 的 `occupancy.warn_threshold/good_threshold/refresh_interval/show_value`
  - 每 lineNo 取 4 面平均 occupancyRate；`<warn` 红 / `>=warn && <good` 黄 / `>=good` 绿 / `<=0 或无数据` 灰
  - 颜色+真实值双显（老板拍板）：默认只颜色，[显示数值]开关注入数字
  - hover 无独立 tooltip（格数太多逐格 tooltip 会卡），改由"显示数值"开关满足真实值需求 —— **偏离 brief，需老板确认**
  - 5s 定时 `lineStore.load(true)` 增量刷新（不整页刷新）
- **修改** `DataupLoad-web/src/views/RealTime.vue`
  - 顶部挂载 `<OccupancyPanoramaBar />`（grid-column: 1/-1 跨双栏）
- **修改** `DataupLoad-web/src/i18n/index.ts`
  - 三语新增 `occupancy` 块（barTitle / barAvg / barShowValue / boardTitle / rate 等 16 keys）

## 视觉（ASCII）
```
┌─────────────────────────────────────────────────────────────┐
│ 📊 上座率全景  平均 12.3%       [显示数值]   [▾]              │
│ ▌▌▌▌▌▌▌▌▌▌▌▌... (38 格热力条，红黄绿灰)                     │
│ 红:2  黄:5  绿:145  灰:0       [打开生产看板 →]              │
└─────────────────────────────────────────────────────────────┘
```

## 验证
- `npm run build` → 通过（56.68s, 2362 modules）
- 部署 dataupLoad/web + 清理 4 个旧 interceptor 残留 + 5 个旧 index bundle（-10MB 磁盘）
- HTTP 200：`GET /` + `GET /assets/index-BWriwIxz.js`（2,676,132 bytes）
- bundle 内三语 occupancy 字符串 / occupancy.warn_threshold / system-config 引用齐全
- 新 interceptor（router.push 软跳）已确认在引用链中（index-BWriwIxz → interceptor-DDTMJWm4）

## 已知偏离（需老板拍板）
1. **hover tooltip 简化为开关**：38 格逐格 tooltip 在 5s 刷新下会抖动卡顿，改用 [显示数值] 开关直接显数字（更稳）。老板如果坚持颗粒度 hover 可改 B2.5。
2. **"打开生产看板"链接**：B3 未注册路由，当前 hasRoute=false 不显示链接；点格子也静默不跳（防 404）。等 B3 做完自动激活。

## 待办
- 老板浏览器实测：登录 → RealTime 顶部应出现"上座率全景"玻璃条（38 格，当前设备停产 occupancyRate=0 全灰）
- 阈值 config（B4）弹窗 + B3 生产看板页 + WS 增量

## 耗时
PM 手写 ~1h（Codex 3 次尝试全部 hang，改 PM 直写）
