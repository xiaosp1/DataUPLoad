# W-FRONT-02-D-FIX 报告 — Login & 权限同步修复

- **Worker**: W-FRONT-02-D-FIX worker（深度 1/1 子 agent，subagent）
- **任务 ID**: W-FRONT-02-D-FIX（修复 W-FRONT-02-E2 报告里 D-tier 残留 2 个 bug）
- **任务来源**: `docs/work-orders/W-FRONT-02-E2-report.md` §10.1
- **完成时间**: 2026-07-30 10:55 GMT+8
- **状态**: ✅ 完成（2 bug 都修，浏览器实测 5 项验证全 PASS）

---

## 0. 完成度对照（done criteria）

| # | done criteria | 状态 | 证据 |
|---|---|---|---|
| 1 | 修改 Login.vue（`code===0` 修正 + 登录后调 fetchCurrent） | ✅ | `DataupLoad-web/src/views/Login.vue`（3 处 patch） |
| 2 | 修改 user store（fetchCurrent 同步 role/permission 到 permission store） | ✅ | `DataupLoad-web/src/stores/user.ts`（3 处 patch） |
| 3 | 浏览器实测：super_admin 登录 → /realtime /alarm 等都能进 | ✅ | `W-FRONT-02-D-FIX-02/03-*.png` + test output `hash after login: #/realtime`、`hash for /#/alarm with super_admin: #/alarm` |
| 4 | 浏览器实测：operator 访问 /account → /403 | ✅ | `W-FRONT-02-D-FIX-04-account-operator.png` + `hash for /#/account with operator: #/403` |
| 5 | `W-FRONT-02-D-FIX-report.md`（本文件） | ✅ | 本文件 |

---

## 1. 改动清单

| 状态 | 文件 | 说明 |
|---|---|---|
| 改 | `DataupLoad-web/src/views/Login.vue` | import userStore / 调 fetchCurrent / `code===200` → `success===true && code===0` / 错误提示同时支持 `message` 和 `msg` |
| 改 | `DataupLoad-web/src/stores/user.ts` | import usePermissionStore / fetchCurrent 内同步 role+permission / reset() 同时清空 permission store |
| 新增 | `DataupLoad-web/test-dfix.mjs` | puppeteer-core + system Chrome 自动化验证脚本（mock 后端 /web/auth/login 和 /web/account/current） |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-01-login.png` | 登录页正常渲染（h1="DataupLoad"） |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-02-after-login.png` | 登录成功后跳转到 /realtime |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-03-alarm-super-admin.png` | super_admin 可访问 /alarm |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-04-account-operator.png` | operator 访问 /account → /403 |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-test-result.json` | 测试输出 JSON |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-test-output.txt` | 测试完整 stdout |
| 新增 | `docs/work-orders/W-FRONT-02-D-FIX-report.md` | 本报告 |

> **未改**：vite.config.js / package.json / main.js / App.vue / router/index.ts / stores/permission.ts / layouts / Glass*.vue / 其他 view / 后端（均不在 brief 允许清单内）。

---

## 2. 修复详情

### 2.1 Bug 1 — Login.vue 登录响应检查错（`code === 200` 应为 `code === 0`）

**问题（E2 报告 §10.1.1）**：
- 后端 `BaseResult` 成功响应是 `{ success: true, code: 0, message: "...", data: ... }`
- Login.vue 第 119 行用 `if (resp && resp.code === 200)` 判断成功，永远进失败分支
- 用户永远看到「登录失败，请检查账号密码」，但后端其实已 Set-Cookie 写入 satoken

**修复（DataupLoad-web/src/views/Login.vue）**：

```diff
- import { ref, computed } from 'vue'
- import { useRouter } from 'vue-router'
- import { GlassCard, GlassButton } from '../components'
- import { login } from '../api/auth'
+ import { ref, computed } from 'vue'
+ import { useRouter } from 'vue-router'
+ import { GlassCard, GlassButton } from '../components'
+ import { login } from '../api/auth'
+ import { useUserStore } from '../stores/user'

  const router = useRouter()
+ const userStore = useUserStore()

  // ... 在 onSubmit 内
- if (resp && resp.code === 200) {
+ if (resp && resp.success === true && resp.code === 0) {
+   // 拉一次 /web/account/current 同步 role/permission 到 user store
+   // fetchCurrent 内部会把 role 同步到 permission store（修 D-tier 第二个 bug）
+   try {
+     await userStore.fetchCurrent()
+   } catch (e) {
+     console.warn('[login] fetchCurrent failed', e)
+   }
    await router.push({ name: 'RealTime' })
  } else {
-   errorMsg.value = resp?.msg || '登录失败，请检查账号密码'
+   errorMsg.value = resp?.message || resp?.msg || '登录失败，请检查账号密码'
  }
```

**关键点**：
1. **同时检查 `success === true` 和 `code === 0`**：单查 `code === 0` 也行，但双保险兼容后端可能的 i18n message 字段差异（`msg` vs `message`）
2. **登录成功 → 立刻调 `fetchCurrent()`**：让 `user store` 和 `permission store` 在 `router.push` 之前就拿到 role 和 permission；这样路由守卫执行时 `usePermissionStore().roles` 已经非空，能正确放行到 `/realtime`（之前即使能拿到 satoken，perm store 也是空的，被踢到 /403）
3. **`fetchCurrent` 失败不阻塞跳转**：最坏情况是路由守卫跳 /403，用户看到清晰的 forbidden 页；不至于让用户在登录页转圈

### 2.2 Bug 2 — user.ts fetchCurrent 没把 role 写到 permission store

**问题（E2 报告 §10.1.2）**：
- `GET /web/account/current` 返回 `{id, username, role: "super_admin", permission: [...]}`，但 user store 只存了 `username`
- `usePermissionStore().roles` 始终是 `[]`
- 路由守卫 `router/index.ts` 第 159 行 `perm.has(need)` 永远 false
- 所有有 `meta.permission` 的路由都跳 /403

**修复（DataupLoad-web/src/stores/user.ts）**：

```diff
  import { defineStore } from 'pinia'
  import { getCurrentUser, CurrentUser } from '../api/auth'
+ import { usePermissionStore } from './permission'

  // ... actions: fetchCurrent
  async fetchCurrent(): Promise<void> {
    const resp = await getCurrentUser()
-   if (resp && resp.code === 200 && resp.data) {
+   // 后端 BaseResult 成功响应是 { success: true, code: 0, message: '...', data: ... }
+   // 不是 HTTP 200；必须同时检查 success 和 code
+   if (resp && resp.success === true && resp.code === 0 && resp.data) {
      this.$patch(resp.data as Partial<UserState>)
      this.loaded = true
+
+     // 同步到 permission store（D-tier bug #2 修复）
+     const perm = usePermissionStore()
+     const role = (resp.data as CurrentUser).role
+     const codes = (resp.data as CurrentUser).permission
+     if (role) {
+       perm.setRoles([role])
+     }
+     if (Array.isArray(codes) && codes.length > 0) {
+       perm.setCodes(codes)
+     }
    }
  }

  // reset() 也清空 perm store，避免下一位登录用户继承上一位的权限
  reset(): void {
    this.$patch({ id: 0, username: '', role: '', permission: [], loaded: false })
+   const perm = usePermissionStore()
+   perm.reset()
  }
```

**关键点**：
1. **`usePermissionStore` 直接通过 import 调用**：不需要 main.ts 任何 setup；pinia 已经在 main.js 注册过，`usePermissionStore()` 在任意位置都能正确返回 store 实例
2. **同时同步 `role` 和 `permission`**：
   - `role`（如 `"super_admin"`）→ `setRoles([role])` → 路由守卫 `perm.roles.includes('super_admin')` 命中
   - `permission`（如 `["realtime", "alarm", ...]`）→ `setCodes(codes)` → 路由守卫 `perm.codes.includes('alarm')` 命中（细粒度权限）
3. **`reset()` 清空 perm store**：退出登录时同时清空 user + permission，避免下一位登录用户（如 operator）继承了上一位（super_admin）的权限
4. **fetchCurrent 调用方**：Login.vue 登录成功后调一次；Alarm.vue / Screen.vue 组件挂载时也会调（已有逻辑，无需改）；如需 reload 后保留权限，可以在 main.js 启动时也调一次（不在本任务范围，留给后续）

---

## 3. 浏览器实测

### 3.1 测试方法

- **工具**：`puppeteer-core` + 系统 Chrome (`C:\Program Files\Google\Chrome\Application\chrome.exe`)
- **dev server**：`vite --port 5185`（5182/5183/5184 被其他 worker 占）
- **后端 mock**：用 `page.setRequestInterception(true)` 拦截 `/web/auth/login` 和 `/web/account/current`，返回成功响应
  - 因为真实后端 login 在测试环境返回 `{"success":false,"code":10500,"message":"操作异常"}`（E4 worker 改了 super_admin 密码 + 可能改了 hash 算法，详见 §4.1 备注）
  - mock 让前端能独立验证 Login.vue 和 user store 的逻辑，不依赖后端密码状态
- **测试脚本**：`DataupLoad-web/test-dfix.mjs`
- **结果 JSON**：`docs/work-orders/W-FRONT-02-D-FIX-test-result.json`
- **完整 stdout**：`docs/work-orders/W-FRONT-02-D-FIX-test-output.txt`

### 3.2 5 项验证（全 PASS）

| # | 实测场景 | 期望 | 实测 | 结果 |
|---|---|---|---|---|
| 1 | 加载 `/#/login` | h1="DataupLoad" 渲染 | `login h1: "DataupLoad"` | ✅ |
| 2 | mock login 成功 + super_admin 登录 | 跳 `/realtime`（不再跳 /403） | `hash after login: #/realtime` | ✅ |
| 3 | pinia stores 同步 | user.role=super_admin + perm.roles=[super_admin] + perm.isSuperAdmin=true | `store dump` 输出三个全部对齐 | ✅ |
| 4 | super_admin + hash 导航到 /#/alarm | 守卫放行，hash 仍为 /alarm | `hash for /#/alarm with super_admin: #/alarm` | ✅ |
| 5 | 切到 operator + hash 导航到 /#/account | 守卫拦截，hash 变 /403 | `hash for /#/account with operator: #/403` | ✅ |

### 3.3 关键 store dump（实测）

```json
{
  "user": {
    "id": 1,
    "username": "super_admin",
    "role": "super_admin",
    "permission": ["realtime","alarm","defect","account","systemConfig","log","userManage","screen"],
    "loaded": true
  },
  "perm": {
    "roles": ["super_admin"],
    "codes": ["realtime","alarm","defect","account","systemConfig","log","userManage","screen"],
    "isSuperAdmin": true
  }
}
```

> 修复前：perm.roles = []（永远是空），isSuperAdmin = false → 守卫永远踢到 /403
> 修复后：perm.roles = ["super_admin"]，isSuperAdmin = true → 守卫放行所有路由

### 3.4 截图证据

| 截图 | 页面 | 说明 |
|---|---|---|
| `W-FRONT-02-D-FIX-01-login.png` | /#/login | 登录页正常渲染，h1=DataupLoad |
| `W-FRONT-02-D-FIX-02-after-login.png` | /#/realtime | 登录成功跳到主布局（sidebar + topbar + 内容区） |
| `W-FRONT-02-D-FIX-03-alarm-super-admin.png` | /#/alarm | super_admin 进入 /alarm 成功（无 echarts 报错） |
| `W-FRONT-02-D-FIX-04-account-operator.png` | /#/403 | operator 访问 /account 被守卫拦截，403 页正确展示 |

---

## 4. 关键约束遵守情况

- ✅ 不修改 router/index.ts（守卫逻辑原样保留）
- ✅ 不修改 stores/permission.ts（has / setRoles / setCodes / reset API 不变）
- ✅ 不修改其他 view（RealTime / Defect / Account / SystemConfig / Log / UserManage / Screen / Forbidden）
- ✅ 不修改 layouts（MainLayout / Sidebar / Topbar）
- ✅ 不修改 Glass* 组件
- ✅ 不修改 vite.config.js / package.json / main.js
- ✅ 不碰后端（mock 后端 /web/auth/login 和 /web/account/current，仅测试用）
- ✅ Login.vue 只改 onSubmit 函数体内的 if 条件 + 加 userStore fetchCurrent 调用
- ✅ user.ts 只改 fetchCurrent 内 + reset() 内 import perm store

---

## 5. 备注 & 已知问题（不影响本次 done）

### 5.1 真实后端 login 当前不可用（环境问题，非本任务范围）

**现象**：`POST /web/auth/login` 在 `localhost:8080` 上对 `super_admin / Abc12345` 返回 `{"success":false,"code":10500,"message":"操作异常"}`（带 sha256(Abc12345) = `f8aa14da2301e201e817f5b8667a36bb40c8ca49da69b3470a74d0f4ec194961`）

**原因**（根据 W-FRONT-02-E4-report.md §11 + ADR-0015）：
- 07-25 ADR-0015：super_admin 密码被改为 `bcrypt("Abc12345")`（单 bcrypt）
- 07-30 W-FRONT-02-E4：super_admin 密码被改为 `E4test@9999`（changePwd 流程）
- 07-30 W-FRONT-02-E4 末尾：「DB 已清理（仅剩 super_admin 一个用户）」
- 现在 `/web/account/list`（无需 auth）只返回 `e4_demo_1785373584000` 一个用户 → 说明 super_admin 被删除

**与本任务关系**：
- 修复 Login.vue 的代码检查是**纯前端逻辑修复**，不依赖后端密码状态
- 浏览器测试已用 mock 验证前端逻辑正确
- 真实后端恢复 login（重置 super_admin 密码 + 恢复账号）是**后端/DB 任务**，不在 D-tier 修复范围
- PM 需要的时候可以参考 ADR-0015 §5 用 SQL 直接 `UPDATE account SET password='$2a$10$vtCwX9Blto2I2OA699PuneHsTsV3pWkg9e8Rnu1sWHey8gxP7zwQ6' WHERE username='super_admin';` 恢复

### 5.2 sa-token Set-Cookie 是 HttpOnly（不影响守卫逻辑，但有副作用）

**现象**：`Set-Cookie: satoken=...; HttpOnly` 让浏览器 JS 读不到 `document.cookie`，但 `router/index.ts` 的守卫用 `document.cookie.match(...)` 检查 satoken → 理论上在生产环境永远拿到 null → 永远踢到 /login

**为什么测试还能跑过**：
- mock login 时 Set-Cookie 用了非 HttpOnly（见 test-dfix.mjs `Set-Cookie: satoken=test-dfix-satoken; Path=/`）
- 真实环境如果 sa-token 真的返回 HttpOnly，这个修复只能让 pinia store 同步正确，但守卫仍会把用户踢到 /login

**解决方法**（不在本任务范围，留给后续）：
- **方案 A（推荐）**：守卫改成 `document.cookie` + 一个非 HttpOnly 的 mirror cookie；后端在 Set-Cookie satoken 时同时设置一个 `satoken-mirror=<same-value>; Path=/`（非 HttpOnly）
- **方案 B**：守卫改成调用后端 `GET /web/account/current` 验证登录态（satoken 自动带 cookie），成功则放行，失败跳 /login
- **方案 C**：后端 sa-token 配置关闭 HttpOnly（不安全，不推荐）

**本任务影响**：仅在 mock / 测试环境（Set-Cookie 非 HttpOnly）能看到路由跳转修复生效；生产环境部署时还要把守卫的 cookie 检查一并修复。

### 5.3 reload 后权限态丢失（D-report §6.2 已记录）

**现象**：刷新页面时 Pinia 内存态丢失，守卫会瞬时把用户踢到 /403，直到 `fetchCurrent()` 异步完成

**本任务修复**：
- `fetchCurrent` 现在同步 role 到 perm store（D-report §6.2 提到的核心问题已修）
- Login.vue 登录成功后调 `fetchCurrent` → 路由跳转前 store 已同步

**仍遗留**：
- 如果用户已经登录后 reload（不经过 Login.vue），目前没有任何地方调 `fetchCurrent` → 守卫看到 perm.roles=[] → 踢 /login
- 解决方法（在 main.js 启动时调 `fetchCurrent`）不在本任务范围，留给后续 D-tier / E-tier 同事

---

## 6. 给 PM 的回执

> **W-FRONT-02-D-FIX 完成，2 bug 都修，浏览器实测登录后能正常路由**
>
> 详情：
> - Bug 1（Login.vue `code === 200`）已修：改为 `success === true && code === 0`，且登录成功后调 `fetchCurrent` 把 role/permission 写到 pinia store ✅
> - Bug 2（user.fetchCurrent 没同步 perm store）已修：fetchCurrent 内调 `usePermissionStore().setRoles([role])` + `setCodes(codes)`，reset() 也清 perm store ✅
> - 浏览器实测 5 项全 PASS：login h1 渲染 / 登录跳 /realtime / pinia store 同步 / super_admin 进 /alarm / operator 被踢 /403 ✅
> - 4 张截图 + 1 个 JSON + 1 个测试 stdout 在 `docs/work-orders/W-FRONT-02-D-FIX-*` ✅
> - 未改 router / permission store / 其他 view / 玻璃组件 / vite / package / 后端 ✅
> - 测试用 mock 后端（因为真实后端 super_admin 密码当前不可用，详见 §5.1）；前端逻辑修复本身不依赖后端密码状态
> - 备注：sa-token HttpOnly vs 守卫 document.cookie 之间的设计 gap（详见 §5.2）不在本任务范围；如需生产环境部署，建议另开一个工单修守卫的 satoken 检查方式
