# W-FRONT-05-PROBE 全刷真因排查

## 背景
老板报障：Web 端界面**每隔一段时间会刷新整个页面**，不是想要的效果。希望只刷新数据区域，不刷整页。

## 目标
定位真因，**不修**，输出排查报告 + 修复建议。

## 必读
- `docs/adr/0009-alarm-record-service-extensions.md`（alarm 钩子位置）
- `E:\DEMO\数据采集\DataupLoad-web\src\router\index.ts`（路由）
- `E:\DEMO\数据采集\DataupLoad-web\src\api\*.ts`（axios 拦截器）

## 排查步骤
1. **grep 全刷凶手**（在 `DataupLoad-web/src/` 下搜）：
   - `location.reload`
   - `window.location.reload`
   - `router.go(0)`
   - `router.push` 同一路径
   - `window.location.href`
2. **检查 axios 拦截器**：
   - `src/api/request.ts` 或类似文件，看 401/403/500 是否直接 `location.reload()` / `router.go(0)`
3. **检查守卫**：
   - `src/router/beforeEach` 里有没有 reload 逻辑
4. **检查 WS 重连**：
   - `src/utils/ws.ts` 或 `src/utils/screenWs.ts` 重连时是否触发 reload
5. **检查定时器**：
   - 搜 `setInterval` + `reload` / `setTimeout` + `reload`
6. **浏览器实测（可选）**：
   - 如果 grep 没找到，开 DevTools → Network → Preserve log → 等 5min 看哪些请求触发整页加载

## 输出物
- `docs/work-orders/W-FRONT-05-PROBE-report.md`，含：
  - 凶手位置（文件 + 行号 + 代码片段）
  - 真因分析（1-2 句）
  - 修复建议（改法 + 风险 + 工作量估算）
  - 若 grep 都没找到：标注"未发现代码层凶手，可能是浏览器扩展/路由 hash 变化/服务端推送" 并列出排查方向

## 耗时上限
30 分钟

## 边界
- **不修代码**（这是 PROBE 工单）
- **不重启服务**
- 不动 PSM 老代码
