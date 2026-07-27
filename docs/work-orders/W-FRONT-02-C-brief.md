# W-FRONT-02-C brief — Login.vue + 路由守卫 + satoken（去 PSM 守卫 hack）

- **任务**: 实现 Vue 3 登录页 + 路由守卫 + axios interceptor，**彻底干掉 PSM 老 SPA 的 cookie `token` hack**
- **依赖**: W-FRONT-02-B（玻璃组件库，已完成）
- **耗时上限**: 2 小时
- **目标**:
  1. **优化 PSM 守卫**（老板第 1 目标）
  2. 新前端从根上**正确读 satoken**，不再 hack

## 关键产出

### 1. `src/api/auth.ts`
```ts
import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/web'

export async function login(username: string, password: string) {
  // password: SHA-256 十六进制（跟老 gate-routing 一致）
  const pwdHex = await sha256Hex(password)
  const resp = await axios.post(`${API_BASE}/auth/login`,
    { username, password: pwdHex },
    { withCredentials: true }  // 浏览器自动带 satoken cookie
  )
  return resp.data
}

export async function getCurrentUser() {
  const resp = await axios.get(`${API_BASE}/account/current`, { withCredentials: true })
  return resp.data
}

async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
}
```

### 2. `src/api/interceptor.ts`
```ts
import axios from 'axios'

// 全局请求拦截器：自动带 satoken cookie（浏览器同源自动带，withCredentials 已设）
// 全局响应拦截器：401 自动跳 Login
axios.interceptors.response.use(
  resp => resp,
  err => {
    if (err.response?.status === 401) {
      // 清掉状态，跳登录
      window.location.href = '/#/login'
    }
    return Promise.reject(err)
  }
)
```

### 3. `src/router/index.ts`（**关键去 hack**）
```ts
import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import MainLayout from '@/layouts/MainLayout.vue'

function getCookie(name: string): string | null {
  const m = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'))
  return m ? decodeURIComponent(m[1]) : null
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { public: true } },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/realtime' },
        { path: 'realtime', name: 'RealTime', component: () => import('@/views/RealTime.vue') },
        // ... 其他 7 个 stub（D 子单完成）
      ]
    }
  ]
})

// 关键：守卫直接读 satoken，不读 token
router.beforeEach((to, from, next) => {
  const satoken = getCookie('satoken')  // ← 不读 token！
  if (to.meta.public) {
    if (satoken && to.name === 'Login') return next({ name: 'RealTime' })
    return next()
  }
  if (!satoken) return next({ name: 'Login' })
  next()
})

export default router
```

### 4. `src/views/Login.vue`
- 用 `GlassCard` + `GlassButton` 实现（**复用 B 子单组件**）
- **不要 SHA256 计算逻辑**（在 auth.ts 里）
- 调 `login()` API，**成功后等后端写 cookie**，然后 `router.push('/realtime')`
- **不要再 `document.cookie = "token=..."`**（旧 hack 已废）

### 5. `src/stores/user.ts`（Pinia）
```ts
import { defineStore } from 'pinia'
export const useUserStore = defineStore('user', {
  state: () => ({ id: 0, username: '', role: '', permission: [] }),
  actions: {
    async fetchCurrent() {
      const data = await (await import('@/api/auth')).getCurrentUser()
      this.$patch(data.data)
    }
  }
})
```

### 6. **关键去 hack 验证文件** `docs/work-orders/W-FRONT-02-C-report.md`
报告里必须包含 grep 结果：
- `grep -r 'document.cookie\s*=\s*"token=' src/` ← **0 结果**
- `grep -r 'syncTokenToLocalStorage' src/` ← **0 结果**
- `grep -r 'getCookie(.token.)' src/` ← **0 结果**（必须用 satoken）
- `grep -r 'withCredentials' src/api/` ← 至少 1 结果

## done criteria（12 项）

- [ ] `src/api/auth.ts` 含 sha256Hex + withCredentials
- [ ] `src/api/interceptor.ts` 含 401 跳登录
- [ ] `src/router/index.ts` beforeEach 读 `getCookie('satoken')`，**不读 token**
- [ ] `src/views/Login.vue` 用 GlassCard + GlassButton 实现
- [ ] `src/stores/user.ts` Pinia store
- [ ] `src/main.js` import interceptor
- [ ] **grep 0 命中** `document.cookie = "token`
- [ ] **grep 0 命中** `syncTokenToLocalStorage`
- [ ] **grep 0 命中** `getCookie('token')`
- [ ] **grep ≥1 命中** `withCredentials`
- [ ] 浏览器实测：`POST /web/auth/login super_admin/Abc12345` → satoken cookie 写入 → `/web/account/current` 返回 200 + user info
- [ ] verify-w-front-02-C.ps1 全 PASS

## 截图

`docs/work-orders/W-FRONT-02-C-sample.png` — 登录页 + 登录成功后跳 /realtime 的截图

## PM 验收

```powershell
pwsh -NoProfile -File scripts/verify-w-front-02-C.ps1
```

12/12 PASS。

## 禁止

- 不许复制老 PSM 任何 .vue / chunk
- 不许 `document.cookie = "token=..."` hack
- 不许把 sha256 计算放在 Login.vue
- 不许改 vite.config.js

