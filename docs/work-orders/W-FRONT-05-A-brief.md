# W-FRONT-05-A 修全刷（凶手：interceptor.ts 401 整页跳）

## 背景
老板报障：Web 端每隔一段时间整页刷新。PROBE 排查真因：`DataupLoad-web/src/api/interceptor.ts:33` 在 401 时用 `window.location.href = ...` 整页跳登录，等于 reload。

## 老板拍板
方案 1：改成 `router.push({ name: 'Login' })` 软跳，不刷页。

## 任务
1. **修改 `DataupLoad-web/src/api/interceptor.ts` line 33**：
   - 把 `window.location.href = ...` 改成 `router.push({ name: LOGIN_ROUTE_NAME })`
   - 加 `import router from '@/router'`
   - LOGIN_ROUTE 常量从字符串 `'login'` 改成 name `'Login'`（看 router/index.ts 实际命名）
2. **清状态**：401 跳登录前，调用 `useUserStore().logout()` 清 satoken + user info（避免半登录态脏数据）
3. **避开循环**：在登录页本身不发请求（已有 `hash.includes('#/login')` 判断，改成 `router.currentRoute.value.name === 'Login'`）
4. **保留**：响应拦截器其他逻辑不变

## 修改前后
**Before**：
```ts
if (status === 401) {
  const hash = window.location.hash || ''
  if (!hash.includes(`#${LOGIN_ROUTE}`)) {
    window.location.href = `${window.location.pathname}#${LOGIN_ROUTE}`
  }
}
```

**After**：
```ts
if (status === 401) {
  if (router.currentRoute.value.name !== 'Login') {
    try {
      const { useUserStore } = await import('@/stores/user')
      useUserStore().logout()
    } catch {
      /* store 不存在时静默 */
    }
    router.push({ name: 'Login' })
  }
}
```

## 验证步骤
1. `cd DataupLoad-web && npm run build`（无 TS 错误）
2. **dev 自测**：
   - `npm run dev`
   - 浏览器登录 → 进 RealTime
   - DevTools Application → Cookies → 删 `satoken`
   - 鼠标 hover alarm 区域 / 切菜单 / 触发任意请求
   - **预期**：软跳 /login，URL 变化但页面**不刷**（Vue 路由切换，无白屏）
3. **登录页不发请求**：直接在地址栏输 `/#/login` → 进登录页 → DevTools Network 应无 401 触发

## 输出物
- 修改后的 `DataupLoad-web/src/api/interceptor.ts`
- `docs/work-orders/W-FRONT-05-A-report.md`：含 diff + dev 实测截图 1 张（跳登录前后）

## 耗时上限
15 分钟

## 边界
- **不动 satoken 配置**（后端过期时间不动）
- **不实现 refresh token**（留给将来 P2）
- **不重启后端**
- **不动 router/index.ts 路由配置**
- **不引入新依赖**
