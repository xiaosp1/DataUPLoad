# W-FRONT-05-B3 report — 生产看板独立页

## 状态
✅ 完工并部署

## 产出文件
- **新增** `DataupLoad-web/src/views/ProductionBoard.vue`（~10.5KB）
  - 全部线体 × 面 = 152 格网格热力图（38 线，auto-fill 响应式 ~150px 卡）
  - 每线一卡：头部 lineNo + 平均值(带色)，内部 2×2 四面格子
  - 颜色：`<warn` 红 / `>=warn && <good` 黄 / `>=good` 绿 / `<=0` 灰
  - 颜色 + 真实值双显：show_value 配置默认 true（格子内显 `97.3`）+ hover tooltip（原生 title：线/面/上座率%）
  - 搜索过滤、刷新按钮、红黄绿灰汇总、点击卡跳 Realtime（带 line 参数）
  - 5s `lineStore.load(true)` 增量刷新
- **修改** `src/router/index.ts`
  - 新增路由 `/production-board` name `ProductionBoard`，permission `realtime`（与实时监控同权）
  - import ProductionBoard
- **修改** `src/layouts/Sidebar.vue`
  - monitorItems 加 `{name:'ProductionBoard', label:'menu.productionBoard', icon:'◫'}`（实时监控组第 2 项）
- **修改** `src/i18n/index.ts`
  - 三语 menu 加 `productionBoard`

## 验证
- `npm run build` → 通过（10.85s）
- 部署 + 清理全部旧 interceptor/index bundle
- HTTP 200：`GET /` + `GET /assets/index-BYESROxo.js`
- bundle 内：`push({name:"Login"})` ✓（A 单 interceptor 修复保留），`location.href` 仅第三方库 2 处（非 401）✓
- `production-board` 路由 + board title（生产看板）在 bundle ✓

## 实现备注
- 当前 occupancyRate 全 0（停产）：页面会全灰，属预期，等设备生产即有颜色
- ProductionBoard 复用 lineStore（RealTime 同源），不重复 fetch
- 点击线卡 → `router.push({name:'RealTime', query:{line}})`；RealTime 当前不消费该 query（B3.5 可选优化），点了只是回到实时页

## 待办
- B4 阈值配置弹窗 + WS 增量
- 老板浏览器实测：菜单多"生产看板"，进去看到 152 格网格

## 耗时
PM 手写 ~40min
