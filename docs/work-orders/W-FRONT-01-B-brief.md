# W-FRONT-01-B — Login 页 + 路由守卫 + satoken 集成

- **父工单**: W-FRONT-01（依赖 W-FRONT-01-A 完成）
- **目标**: `localhost:5173/` 自动跳 `/Login`，填账号密码能登录成功跳 `/realTime`
- **前提**: 已读 `E:\DEMO\数据采集\docs\adr\0016-frontend-align-psm-spa-20260725.md`，理解对齐 PSM 的边界

## 任务清单

### B1. `src/api/http.js` — axios 实例
```js
import axios from 'axios'
const http = axios.create({
  baseURL: '/',          // vite proxy 会把 /web/* 转到 localhost:80
  withCredentials: true, // 关键：satoken cookie 必须带
  timeout: 10000,
})
export default http
```

### B2. `src/api/login.js` — 登录接口
- **密码前端 sha256**（PSM 一致）：前端 `CryptoJS.SHA256(password).toString()` 或 `js-sha256`
- 函数：`async function login({ username, password })` → `http.post('/web/auth/login', { username, password: sha256Hex(password) })`
- 成功（`res.data.code === 200`）：返回 `res.data.data`（含 userInfo），调用方把 username/role 写 sessionStorage（key：`user`、`role`）
- 失败：`throw new Error(res.data.message || 'login failed')`

### B3. `src/router/index.js` — 路由 + 守卫
- routes：
  - `{ path: '/', redirect: '/realTime' }`
  - `{ path: '/Login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true } }`
  - `{ path: '/realTime', name: 'RealTime', component: () => import('@/views/RealTime.vue') }`
- 全局 `beforeEach`：
  - 如果 `to.meta.public` → next()
  - 否则检查 `sessionStorage.getItem('user')`，无 → `next('/Login')`，有 → next()

### B4. `src/views/Login.vue` — Element Plus 登录页
- 居中卡片（`<el-card>`），宽 400px
- `<el-form :model="form" :rules="rules" ref="formRef">`
- 字段：`username`、`password`（type=password），规则：必填
- 底部：登录按钮 + 重置按钮
- `<el-alert v-if="errMsg" :title="errMsg" type="error" />`
- submit 处理：
  1. `await formRef.value.validate()`
  2. `loading = true`
  3. `const data = await login(form)`
  4. `sessionStorage.setItem('user', data.userInfo.username)`、`sessionStorage.setItem('role', data.userInfo.role)`
  5. `router.replace('/realTime')`
  6. catch → `errMsg = e.message` → `loading = false`

### B5. `src/views/RealTime.vue` — 占位业务页
- `<h1>RealTime - {{ sessionStorage.getItem('user') }}</h1>` + 一个"退出"按钮（清 sessionStorage + 跳 `/Login`）

## API 契约（必须按这个写）

后端 `/web/auth/login` 真实返回结构（**先 grep 后端验证，别瞎编**）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "xxx",         // sa-token 写到 cookie，body 里可能也有
    "userInfo": {
      "id": 1,
      "username": "super_admin",
      "role": "super_admin"
    }
  }
}
```

> ⚠️ **不要假设字段名**：工单开工前先 `POST /web/auth/login` 抓一次真实响应，字段名以响应为准

## 不交付

- ❌ 语言切换 UI（W-FRONT-01-C）
- ❌ 业务页（W-FRONT-02+）
- ❌ i18n key（W-FRONT-01-C 会替换 Login.vue 的硬编码字符串）

## 验收

1. `npm run dev` → `http://127.0.0.1:5173/` → **自动跳 `/Login`**
2. 填 `super_admin` + 密码 → 点登录 → 网络面板看到 `POST http://127.0.0.1:5173/web/auth/login`（被 proxy 转到 80 端口） → 200 → **跳 `/realTime`**
3. 刷新 `/realTime` → 不跳走（sessionStorage 还有 user）
4. 浏览器 DevTools Application → Cookies → 看到 `satoken` 值
5. 故意填错密码 → 看到错误 alert（**注意：当前后端密码错会 500，等 W-FRONT-01-B 验收时如果还没修，错误处理就 catch error 即可，但报告里要 flag 这个问题让 PM 排 W-FRONT-01-B-fix**）
6. 浏览器控制台：0 error，0 红字

## 报告

`docs/work-orders/W-FRONT-01-B-report.md`：
- Login.vue / http.js / login.js / router/index.js 完整代码贴出
- 登录成功截图（带 satoken cookie 的 DevTools 截图）
- 任何发现的问题

## 耗时上限

60 分钟
