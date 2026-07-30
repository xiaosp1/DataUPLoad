# W-FRONT-02-D 子单交付报告

> **状态：✅ 全部完成 · 15/15 verify PASS · 5/5 浏览器实测 PASS**
> 实施时间：2026-07-29
> 子单：W-FRONT-02 总计划 v2 的 D 子单（玻璃风格 Vue 3 前端 · 主布局 + 8 路由 stub + 三语 i18n + 权限守卫）

---

## 1. 产出文件清单（11 个新增 + 2 个删除 · 全部 UTF-8 无 BOM）

### 1.1 新增 layouts（3 个）
| 文件 | 大小 | 说明 |
|---|---|---|
| `DataupLoad-web/src/layouts/MainLayout.vue` | 4239 B | 主布局 · 整布局浮在大玻璃面板上 · 左 220px 侧栏 + 顶栏 + 内容区 · 高度 `calc(100vh - var(--space-6)*2)` |
| `DataupLoad-web/src/layouts/Sidebar.vue` | 5328 B | 玻璃药丸菜单 · 两组（实时监控 / 系统管理）· 7 个 GlassMenuItem · 选中态青色凸起 + 路由 active |
| `DataupLoad-web/src/layouts/Topbar.vue` | 8523 B | sticky + blur 玻璃顶栏 · 左面包屑 · 右语言切换（zh-CN/en-US/id-ID el-select）+ 全屏切换 + 用户下拉（头像 + 用户名 + 角色 + 退出） |

### 1.2 新增 stub 页面（9 个 · 8 个业务路由 + 403）
| 文件 | 大小 | 路由 | 权限点 |
|---|---|---|---|
| `DataupLoad-web/src/views/RealTime.vue` | 1183 B | `/realtime` | `realtime` |
| `DataupLoad-web/src/views/Alarm.vue` | 1174 B | `/alarm` | `alarm` |
| `DataupLoad-web/src/views/Defect.vue` | 1177 B | `/defect` | `defect` |
| `DataupLoad-web/src/views/Account.vue` | 1180 B | `/account` | `account` |
| `DataupLoad-web/src/views/SystemConfig.vue` | 1193 B | `/systemConfig` | `systemConfig` |
| `DataupLoad-web/src/views/Log.vue` | 1168 B | `/log` | `log` |
| `DataupLoad-web/src/views/UserManage.vue` | 1187 B | `/userManage` | `userManage` |
| `DataupLoad-web/src/views/Screen.vue` | 1175 B | `/screen` (隐藏菜单) | `screen` |
| `DataupLoad-web/src/views/Forbidden.vue` | 2979 B | `/403` (public) | — |

### 1.3 修改文件（3 个）
| 文件 | 说明 |
|---|---|
| `DataupLoad-web/src/router/index.ts` | 8 个业务路由 + 403 + redirect + catch-all · `meta.permission` + `meta.public` + `meta.title` + `meta.hideInMenu` · 守卫扩展 satoken + permission 检查 |
| `DataupLoad-web/src/i18n/index.ts` | **替换**原 `index.js` · 643 个 key-like 行（每语 ~214 key）· zh-CN / en-US / id-ID · 菜单 / 面包屑 / 通用按钮 / 报警 / 字段名全覆盖 · 支持 localStorage 持久化 |
| `DataupLoad-web/src/stores/permission.ts` | Pinia store · `roles: string[]` + `codes: string[]` · `has(p)`：`super_admin` 永远 true，否则查 roles/codes · `setRoles` / `setCodes` / `reset` |

### 1.4 旧 stub 删除（2 个，避免 Vite 解析冲突）
| 文件 | 说明 |
|---|---|
| `DataupLoad-web/src/i18n/index.js` | 已被 `index.ts` 取代 |
| `DataupLoad-web/src/router/index.js` | 已被 `index.ts` 取代 |

### 1.5 已存在的关键文件（未修改 · 确认在位）
- `DataupLoad-web/src/components/{GlassCard,GlassButton,GlassMenuItem,GlassTable,GlassPage}.vue` （B 子单玻璃组件）
- `DataupLoad-web/src/components/index.ts` （B 子单注册表）
- `DataupLoad-web/src/api/{auth,interceptor}.ts` （C 子单 API）
- `DataupLoad-web/src/stores/user.ts` （C 子单 user store）
- `DataupLoad-web/src/styles/{tokens,element-overrides,global}.scss` （B 子单设计 tokens）
- `DataupLoad-web/src/views/Login.vue` （C 子单登录页 · 未修改）
- `DataupLoad-web/src/main.js`（已注册 5 玻璃组件 + i18n + pinia + router + ElementPlus · 未修改）
- `DataupLoad-web/src/App.vue`（保持 `<router-view />` · 未修改）

---

## 2. 关键设计亮点

### 2.1 整布局浮在表面
`MainLayout.vue` 把整个布局包在一个 `.glass-panel` 内部，左侧 220px 固定宽度 sidebar，右侧 sticky topbar + 滚动内容区。两道半透明背景光晕（青/粉）透过玻璃面板呈现，整体观感与 `style-sample-main.png` 一致。

### 2.2 玻璃药丸菜单选中态
`GlassMenuItem--active` 自动应用青色凸起样式（`--gradient-active-pill` + 外阴影 + 内顶高光），菜单项通过 `route.name` 同步 active 态，点击 `router.push({ name })` 跳转。

### 2.3 三语切换实时生效
- `el-select` 触发 `locale.value` 变更 → `i18n.locale.value = val` → 所有 `$t(...)` 立即重渲染
- 选择持久化到 `localStorage['app.locale']`，下次启动自动恢复
- 初始化 `locale` 从 localStorage 读取，缺省 `zh-CN`

### 2.4 路由守卫三级防护
```
public 路由 → 放行（已登录访问 /login → /realtime）
非 public 无 satoken → /login
非 public 有 satoken 但无 meta.permission → /403
兜底：未知路径 → /realtime
```

### 2.5 403 页面独立
不在 MainLayout 内（避免被踢出时仍试图进入布局），自带玻璃面板 + 渐变数字 403 + 返回首页按钮（已登录回 /realtime，未登录回 /login）。

---

## 3. Done Criteria 自检（15 项）

| # | 检查项 | 结果 | 证据 |
|---|---|---|---|
| 1 | MainLayout.vue 存在 | ✅ PASS | `E:\DEMO\数据采集\DataupLoad-web\src\layouts\MainLayout.vue` (4239 B) |
| 2 | Sidebar.vue 存在 | ✅ PASS | `E:\DEMO\数据采集\DataupLoad-web\src\layouts\Sidebar.vue` (5328 B) |
| 3 | Topbar.vue 存在 | ✅ PASS | `E:\DEMO\数据采集\DataupLoad-web\src\layouts\Topbar.vue` (8523 B) |
| 4 | 6 个 stub 页面存在 | ✅ PASS | RealTime/Alarm/Defect/Account/SystemConfig/Log 全部到位（实际 8 个业务路由 + 403） |
| 5 | router 8+ 路由 | ✅ PASS | regex 匹配到 12 个 `path:` 定义 |
| 6 | i18n zh-CN | ✅ PASS | `index.ts` 含 `'zh-CN': zhCN` |
| 7 | i18n en-US | ✅ PASS | `index.ts` 含 `'en-US': enUS` |
| 8 | i18n id-ID | ✅ PASS | `index.ts` 含 `'id-ID': idID` |
| 9 | permission store 存在 | ✅ PASS | `stores\permission.ts` (1636 B) |
| 10 | permission has() 函数 | ✅ PASS | `actions.has(p)` 已实现 |
| 11 | 路由守卫 permission 检查 | ✅ PASS | `beforeEach` 中 `perm.has(need)` |
| 12 | Sidebar 用 GlassMenuItem | ✅ PASS | `Sidebar.vue` 中 `import GlassMenuItem` |
| 13 | Topbar 三语切换 | ✅ PASS | `el-select` + `i18n` + `zh-CN/en-US/id-ID` 都在 |
| 14 | 截图已提交 | ✅ PASS | `docs\work-orders\W-FRONT-02-D-sample.png` (311867 B) |
| 15 | dev server 5173 监听中 | ✅ PASS | `Get-NetTCPConnection -LocalPort 5173` 返回 Listen / PID 8164 |

**verify 脚本执行结果**：`pwsh -NoProfile -File scripts/verify-w-front-02-D.ps1` → **PASS: 15 FAIL: 0 · ALL CHECKS PASSED -- W-FRONT-02-D approved**

---

## 4. 浏览器实测（5 项 · 全部 PASS）

测试方法：`puppeteer-core` + 系统 Chrome 1440x900 headless，脚本位于 `C:\tmp\capture.mjs`。

| # | 实测场景 | 期望 | 实测 | 结果 |
|---|---|---|---|---|
| 1 | 加载 `/#/login` | 登录页正常渲染（h1="DataupLoad"） | `loginH1 = "DataupLoad"` | ✅ |
| 2 | satoken + super_admin → /realtime | sidebar + topbar 出现，菜单可点 | `sidebar present: true / topbar present: true`，依次点击 Alarm/Defect/Account/SystemConfig/Log/UserManage 全部 hash 跳转成功 | ✅ |
| 3 | 切换语言（zh-CN → en-US → id-ID → zh-CN） | 页面文案实时切换 | en-US 标题 `Realtime Data`，id-ID 标题 `Data Realtime`，中文标题 `实时数据` | ✅ |
| 4 | 清空 satoken → 访问 /alarm | 守卫拦截 → /login | `hash after clearing cookie & going to /alarm: #/login` | ✅ |
| 5 | 设 role=operator（无 account 权限） → 访问 /account | 守卫拦截 → /403 | `hash for /#/account with operator role: #/403` | ✅ |

唯一控制台错误是 `favicon.ico 404`，无害（不计入 verify）。

### 截图证据
- `docs\work-orders\W-FRONT-02-D-sample.png`（311867 B · 1440x900 · 中文 zh-CN 实时数据页 · 完整布局）

---

## 5. 关键约束遵守情况

- ✅ 不实现业务逻辑（8 个 stub 全部 `🚧 业务对齐期（E 子单实现）` 占位）
- ✅ 不复制老 PSM 资源
- ✅ 不修改 vite.config.js / main.js / Login.vue / 玻璃组件 / styles/tokens.scss
- ✅ 全部使用 `<script setup lang="ts">` TypeScript
- ✅ 所有中文文本 UTF-8 无 BOM（直接 write，不经 PowerShell Out-File）
- ✅ 保持 `createWebHashHistory`，不切 history 模式
- ✅ 路由守卫仍只读 satoken cookie
- ✅ 不 commit / push / 重启服务
- ✅ 不启动 vite dev（一直在跑 PID 8164）

---

## 6. 已知边界 / 留给 E 子单

1. **角色加载时机**：当前 permission store 默认空 roles。E 子单应在登录成功 / `fetchCurrent()` 后调用 `permissionStore.setRoles(['super_admin'])` 等。
2. **reload 后权限态丢失**：刷新页面时 Pinia 内存态丢失，守卫会瞬时把用户踢到 /403，直到 E 子单接好 `fetchCurrent()` 引导流程。
3. **路由参数 / query**：本子单只接通了 `:path` 跳转，路由参数化（如 `/account/:id`）由 E 子单按需扩展。
4. **大屏模式**：菜单项已定义但 `hideInMenu: true`，由 E 子单接入实际大屏布局。

---

## 7. 文件总览（最终）

```
DataupLoad-web/src/
├── api/                        (C 子单 · 未改)
│   ├── auth.ts
│   └── interceptor.ts
├── components/                 (B 子单 · 未改)
│   ├── GlassButton.vue
│   ├── GlassCard.vue
│   ├── GlassMenuItem.vue
│   ├── GlassPage.vue
│   ├── GlassTable.vue
│   └── index.ts
├── layouts/                    ★ D 子单新增
│   ├── MainLayout.vue
│   ├── Sidebar.vue
│   └── Topbar.vue
├── router/
│   └── index.ts                ★ D 子单改
├── i18n/
│   └── index.ts                ★ D 子单改（替换原 index.js）
├── store/
│   └── index.js                (C 子单 pinia 实例 · 未改)
├── stores/
│   ├── permission.ts           ★ D 子单新增
│   └── user.ts                 (C 子单 · 未改)
├── styles/                     (B 子单 · 未改)
│   ├── element-overrides.scss
│   ├── global.scss
│   └── tokens.scss
├── views/
│   ├── Login.vue               (C 子单 · 未改)
│   ├── Account.vue             ★ D 子单
│   ├── Alarm.vue               ★ D 子单
│   ├── Defect.vue              ★ D 子单
│   ├── Forbidden.vue           ★ D 子单
│   ├── Log.vue                 ★ D 子单
│   ├── RealTime.vue            ★ D 子单（替换原 stub）
│   ├── Screen.vue              ★ D 子单
│   ├── SystemConfig.vue        ★ D 子单
│   └── UserManage.vue          ★ D 子单
├── App.vue                     (C 子单 · 未改)
└── main.js                     (C 子单 · 未改)
```

---

**结论：W-FRONT-02-D 子单按 brief 完成全部产出 · verify 15/15 PASS · 浏览器实测 5/5 PASS · 准备 PM 验收。**
