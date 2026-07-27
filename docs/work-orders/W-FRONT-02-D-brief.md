# W-FRONT-02-D brief — 主布局 + 8 路由 stub + 权限 + i18n

- **任务**: 实现玻璃主框架（侧边栏 + 顶栏）+ 8 大业务页 stub + router 8 路由 + 三语 i18n + 权限控制
- **依赖**: W-FRONT-02-B（玻璃组件库，已完成）+ W-FRONT-02-C（路由表，已完成）
- **耗时上限**: 3 小时

## 关键产出

### 1. `src/layouts/MainLayout.vue`
- 整体布局：左 220px 侧边栏 + 右 主区（顶栏 + 内容）
- 背景：`background: var(--bg-base); background-image: var(--bg-gradient);`
- **整个布局浮在表面上**（不贴边，参考 style-sample-main.png）

### 2. `src/layouts/Sidebar.vue`
- 玻璃药丸菜单（参考截图）
- 菜单分两组：
  - **实时监控**：实时数据 / 报警管理 / 缺陷处理
  - **系统管理**：账号管理 / 系统配置 / 操作日志
- 用 `GlassMenuItem` 组件
- 选中态：青色玻璃凸起 + 外阴影 + 内顶高光

### 3. `src/layouts/Topbar.vue`
- 玻璃顶栏（sticky top + blur）
- 内容：左 breadcrumb / 右 语言切换器 + user chip
- 语言切换器：zh-CN / en-US / id-ID 三语
- user chip：avatar + 用户名 + 下拉（退出登录）

### 4. 8 个 stub 页面（用 GlassPage + 占位内容）
- `src/views/RealTime.vue` — 实时数据
- `src/views/Alarm.vue` — 报警管理
- `src/views/Defect.vue` — 缺陷处理
- `src/views/Account.vue` — 账号管理
- `src/views/SystemConfig.vue` — 系统配置
- `src/views/Log.vue` — 操作日志
- `src/views/UserManage.vue` — 用户管理（如有）
- `src/views/Screen.vue` — 大屏模式（如有）

每个 stub：`GlassPage title="..." subtitle="..."` + 一个 GlassCard 含 "🚧 业务对齐期 (E 子单实现)"

### 5. `src/router/index.ts`（修改，添加 8 路由）
- /realTime /alarm /defect /account /systemConfig /log 等
- meta.permissions 字段（如 `meta: { permission: 'user' }`）

### 6. `src/i18n/index.ts`（修改）
- 200+ i18n keys（菜单 / 通用按钮 / 报警 / 字段名）
- zh-CN / en-US / id-ID 三语

### 7. `src/stores/permission.ts`
```ts
import { defineStore } from 'pinia'
export const usePermissionStore = defineStore('permission', {
  state: () => ({ roles: [] as string[] }),
  actions: {
    has(p: string) {
      // 简化：admin 全通
      return this.roles.includes('super_admin') || this.roles.includes(p)
    }
  }
})
```

### 8. 路由守卫扩展（修改 C 子单的 router/index.ts）
- meta.permission 控制访问
- 无权限 → 跳 403 stub

## done criteria（15 项）

- [ ] `src/layouts/MainLayout.vue` 浮起玻璃风
- [ ] `src/layouts/Sidebar.vue` 药丸菜单 + 分组
- [ ] `src/layouts/Topbar.vue` sticky + 语言切换 + user chip
- [ ] 8 个 stub 页面都用 GlassPage + GlassCard
- [ ] `src/router/index.ts` 含 8 大路由
- [ ] `src/i18n/index.ts` 三语 + 200+ keys
- [ ] `src/stores/permission.ts` has() 函数
- [ ] 路由守卫含 permission 检查
- [ ] 浏览器实测：登录 → 8 路由可点
- [ ] 浏览器实测：菜单 active 态有青色凸起
- [ ] 浏览器实测：切换语言刷新对应文字
- [ ] 浏览器实测：刷新页面路由仍正确（hash history）
- [ ] 浏览器实测：未登录访问 /realtime → 跳 /login
- [ ] 截图 `docs/work-orders/W-FRONT-02-D-sample.png` 主界面
- [ ] verify-w-front-02-D.ps1 全 PASS

## 截图

`docs/work-orders/W-FRONT-02-D-sample.png` — 主框架 + 菜单 active + 语言切换

## PM 验收

```powershell
pwsh -NoProfile -File scripts/verify-w-front-02-D.ps1
```

15/15 PASS。

## 禁止

- 不许实现业务逻辑（E 子单）
- 不许复制老 PSM 资源
- 不许改 vite.config.js / main.js 的 glass 组件注册（B 子单已做）
- 不许实现 Login.vue（C 子单）

