# W-FRONT-04-C 子单 brief — 修 reload 路由保留（#11）

> **工单号**: W-FRONT-04-C（父单 W-FRONT-04 P0 bug 修复）
> **标题**: 修 reload 后被踢到 /#/login（cookie 有效但守卫看到 loaded=false）
> **来源**: W-FRONT-03 端到端验收报告 §4.2（FAIL #11）
> **老板指令**: 8:29 GMT+8「先补 #11 再重启」
> **耗时上限**: 25 分钟
> **提交范围**: 前端 2 文件 + 1 文件（可选）

---

## 1. 真因（一句话）

`router/index.ts` 的 `beforeEach` 守卫用 `userStore.isLoggedIn = Boolean(id && loaded)` 判定登录态；F5 reload 后 `loaded=false`（没人调 `fetchCurrent()`）→ 守卫把 `/#/realtime` 重定向到 `/#/login`，cookie 仍有效但 UI 体感掉登录。

## 2. 修复方案（双保险）

### 2.1 必修：路由守卫首跳等待（核心修复）

在 `router/index.ts` `beforeEach` 守卫里加一个"首次 boot 等待"：

```ts
// 守卫开头加：
if (!userStore.loaded) {
  // 异步恢复登录态；axios 拦截器处理 401 → /login
  await userStore.fetchCurrent()
}
```

放在 `try { ... }` 块内包住（避免 pinia 没就绪）。fetchCurrent 失败（401 / 网络错）由 axios 拦截器统一跳 /login，不需要守卫管。

### 2.2 必修：App.vue onMounted 同步 await fetchCurrent

`src/App.vue` 的 `onMounted` 里把 `connectScreenSingleton` + `connectAlarmSingleton` 包进一个 async handler，先 `await userStore.fetchCurrent()` 再 connect WS。WS 不会因为 fetchCurrent 失败就崩（拦截器已处理），用 try/catch 兜底。

### 2.3 选做：去掉兜底逻辑

守卫里 `try { ... } catch (e) { isLoggedIn = true; hasPermission = true }` 是 W-FRONT-FLASH 引入的"保守放行"，但现在守卫已经 await fetchCurrent，理论上不会再失败。这块**不要动**，留给后续工单清理。

## 3. 约束（Worker 必须遵守）

- ❌ **不许改后端** — 后端 PID 6000 仍跑老 jar，restart 等 W-FRONT-04-C 完工后老板拍板
- ❌ **不许新建路由 / store** — 只改 2 个现有文件
- ❌ **不许动 Login.vue / Forbidden.vue / 守卫三步的核心判断逻辑**
- ✅ 必须保持 sa-token HttpOnly + 守卫设计 gap 已知边界（W-FIX-03 待办）
- ✅ 必须保持 hash history，不许换 createWebHistory
- ✅ 必须保证无 console error / warn
- ✅ 必须保证首屏不阻塞超过 1 秒（fetchCurrent 通常 < 200ms）

## 4. 验收 5 项（PM 用 Playwright 复跑）

| # | 验收项 | 期望 | 验证方法 |
|---|--------|------|----------|
| 1 | 登录 → `/#/realtime` 正常 | ✅ 现有 | Playwright step 1 |
| 2 | **新**: 在 `/#/realtime` 按 F5（reload）| 仍在 `/#/realtime`，**不**跳 `/#/login` | Playwright step 2 |
| 3 | **新**: 在 `/#/alarm` 按 F5 | 仍在 `/#/alarm` | Playwright step 3 |
| 4 | **新**: cookie 失效（手动删 satoken）后 reload | 跳 `/#/login` | Playwright step 4 |
| 5 | 首屏不阻塞 | load 完成 < 1s | Playwright performance API |

## 5. 实施步骤

```
T1 (5min):  改 router/index.ts beforeEach，加 !loaded → await fetchCurrent
T2 (5min):  改 App.vue onMounted，加 await fetchCurrent 兜底
T3 (10min): npm run build（dist 重新打包）
T4 (3min):  Copy-Item dist\* DataupLoad\web\ -Recurse -Force
T5 (5min):  写 verify-w-front-04-c.mjs + 跑验收 5 项
T6 (2min):  git commit + push
```

## 6. commit message

`W-FRONT-04-C: 修 reload 路由保留 #11 — 守卫首跳 await fetchCurrent`

## 7. 不许做的事

- ❌ 不许动 user.ts fetchCurrent 内部（D-FIX 已对齐 role 同步）
- ❌ 不许引入 localStorage / sessionStorage 缓存 user info
- ❌ 不许换 cookie 读取方案（W-FIX-03 是后续工单）
- ❌ 不许重启后端
- ❌ 不许碰 PSM 老 SPA

## 8. 已知边界（不修，留痕）

- 首屏守卫 await fetchCurrent 期间，路由短暂 hold（< 200ms，用户无感）
- 极端网络抖动下 fetchCurrent 超时 → axios 拦截器兜底跳 /login（正确行为）

## 9. 完成后输出

- `docs/work-orders/W-FRONT-04-C-report.md`（实施报告，~5KB）
- `scripts/verify-w-front-04-c.mjs`（验收脚本）
- `docs/work-orders/W-FRONT-04-C-{01..05}-*.png`（5 张验收截图）
- git commit 1 个（router + App.vue + dist/）

## 10. PM 验收后再做的事

- C 完工 → 派 W-FRONT-04-A（拖拽持久化）+ W-FRONT-04-B（WS UID）
- A/B 完工 → 一起重启后端吃新 jar + 全链路回归
