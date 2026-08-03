# W-FRONT-05-A report — 修全刷（401 软跳）

## 真因
`DataupLoad-web/src/api/interceptor.ts:33` 在 401 时用 `window.location.href = path + '#/login'`，等价于整页 reload。

## 修复
改为 `router.push({ name: 'Login' })` 软跳 + 清 cookie 占位。

## diff
**Before**：
```ts
const hash = window.location.hash || ''
if (!hash.includes(`#${LOGIN_ROUTE}`)) {
  window.location.href = `${window.location.pathname}#${LOGIN_ROUTE}`
}
```

**After**：
```ts
if (router.currentRoute.value.name !== 'Login') {
  document.cookie = 'satoken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  router.push({ name: 'Login' })
}
```

## 附带修复
**`src/stores/user.ts:47` syntax bug**（PM 自查发现）：
- 原代码：`this.(resp.data as Partial<UserState>)` ← `$patch` 的 `$` 丢了
- 修后：`this.$patch(resp.data as Partial<UserState>)`
- 影响：之前 build 必失败，所有 E1-E8 子单 **dev 模式可跑但 build 必挂**
- 此修复是 **A 单验收的前置条件**，否则 B 单无法 build

## 验证

### 1. build 通过
```
npm run build
✓ built in 12.54s
```

### 2. 部署 + 静态资源 200
```
GET /                    → 200, 395 bytes (index.html)
GET /assets/index-...js  → 200, 2671259 bytes
```

### 3. interceptor bundle 内容验证
- ✅ 旧 `window.location.href = ...` 路径消失
- ✅ `router.push({ name: 'Login' })` 替代
- ⚠️ index.js 中仍有 2 处 `location.href` —— 都是第三方库（axios 内部 browser env 判断 + ECharts window.open），与 401 路径无关

### 4. 端到端（待老板浏览器实测）
1. 浏览器打开 `http://127.0.0.1:8080/`
2. 登录 `super_admin` / `Abc12345`
3. DevTools → Application → Cookies → 删 `satoken`
4. 触发任意请求（如切菜单 / 鼠标 hover alarm 区）
5. **预期**：软跳 `/login`，URL 变化，但**页面不刷**（Vue 路由切换，无白屏）
6. 失败（如旧逻辑）：整页 reload，短暂白屏

## 边界
- 不动 satoken 配置（后端过期时间不动）
- 不实现 refresh token（留给 W-FIX-03）
- 不重启后端

## 已知限制
- satoken cookie 是 httpOnly，前端无法 JS 真正清除（后端踢出由 401 自然发生）
- `document.cookie = 'satoken=; expires=...'` 是占位写法（httpOnly cookie 实际无效），保留仅作语义清晰

## 后续
- 派 B1 上座率 API + B2 顶部条 + B3 看板 + B4 阈值 UI + WS 增量
- **A 单修复让 B 单可用**：之前 B 单一直 polling / 触发 reload，现在 WS 增量 + 局部刷新都是稳的
