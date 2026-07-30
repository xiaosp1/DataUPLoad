# W-FRONT-02-D 派工

## 任务

实现玻璃主框架 + 8 路由 stub + 三语 i18n + 权限控制（详见 `docs/work-orders/W-FRONT-02-D-brief.md`）。

## 必读

- **Brief**: `docs/work-orders/W-FRONT-02-D-brief.md`（15 项 done criteria）
- **总计划**: `docs/work-orders/W-FRONT-02-brief.md`（D 在阶段 2，依赖 B+C 已完成）
- **约束**: ADR-0016 + ADR-0018 + W-FRONT-02-C-report.md
- **PSM 风格基准**: `style-sample-login.png` + `style-sample-main.png`（老板确认）
- **依赖现状**:
  - B 玻璃组件就绪：`src/components/GlassCard.vue` / `GlassButton.vue` / `GlassMenuItem.vue` / `GlassTable.vue` / `GlassPage.vue`
  - C 路由守卫就绪：`src/router/index.ts`（createWebHashHistory + beforeEach 读 satoken）
  - C API/store 就绪：`src/api/auth.ts` / `src/api/interceptor.ts` / `src/stores/user.ts`
  - main.js 已注册 5 玻璃组件 + i18n + pinia + router + ElementPlus + 图标

## 关键产出（文件清单）

### 1. `src/layouts/MainLayout.vue`
左 220px 玻璃侧边栏 + 右侧顶栏 + 内容区，背景用 `var(--bg-base)` + `var(--bg-gradient)`，**整个布局浮在表面**（参考 style-sample-main.png）

### 2. `src/layouts/Sidebar.vue`
玻璃药丸菜单，分两组：
- 实时监控：实时数据 / 报警管理 / 缺陷处理
- 系统管理：账号管理 / 系统配置 / 操作日志
用 GlassMenuItem，选中态：青色玻璃凸起 + 外阴影 + 内顶高光

### 3. `src/layouts/Topbar.vue`
玻璃顶栏 sticky + blur，左 breadcrumb，右 语言切换器（zh-CN/en-US/id-ID）+ user chip（avatar + 用户名 + 退出下拉）

### 4. 8 个 stub 页面（用 GlassPage + GlassCard 占位）
- `src/views/RealTime.vue` — 实时数据
- `src/views/Alarm.vue` — 报警管理
- `src/views/Defect.vue` — 缺陷处理
- `src/views/Account.vue` — 账号管理
- `src/views/SystemConfig.vue` — 系统配置
- `src/views/Log.vue` — 操作日志
- `src/views/UserManage.vue` — 用户管理（如有）
- `src/views/Screen.vue` — 大屏模式（如有）

每个 stub：`<GlassPage title="..." subtitle="...">` + `<GlassCard>` 含"🚧 业务对齐期（E 子单实现）"

### 5. 修改 `src/router/index.ts`
补充 8 大路由 + meta.permission 字段（如 `meta: { permission: 'user' }`），子路由用 MainLayout 包裹：
```
/        → redirect /realtime
/login   → public（已有）
/realtime /alarm /defect /account /systemConfig /log 等 → MainLayout + stub
```

### 6. `src/i18n/index.ts`（**替换**现有 stub，扩展到 200+ keys 三语）
- zh-CN / en-US / id-ID
- 菜单项、通用按钮、面包屑、报警、字段名全覆盖
- **必须 import 中文文本为 ASCII-safe**（参考 ADR-0019）

### 7. `src/stores/permission.ts`
```ts
import { defineStore } from 'pinia'
export const usePermissionStore = defineStore('permission', {
  state: () => ({ roles: [] as string[] }),
  actions: {
    has(p: string) {
      return this.roles.includes('super_admin') || this.roles.includes(p)
    }
  }
})
```

### 8. 路由守卫扩展
修改 `src/router/index.ts` 的 beforeEach 增加 meta.permission 检查，无权限 → 跳 /403（建一个简单 stub）

## done criteria（15 项）

详见 brief，PM 验收时跑 `pwsh -NoProfile -File scripts/verify-w-front-02-D.ps1`，15/15 PASS。

## 关键约束

- **不许实现业务逻辑**（E 子单负责）
- **不许复制老 PSM 资源**
- **不许改 vite.config.js / main.js 的玻璃组件注册**（B 已做）
- **不许改 Login.vue**（C 已做）
- **必须用 `<script setup lang="ts">` TypeScript**
- 中文文本避开 UTF-8 BOM 问题（参考 ADR-0019）
- 保持 hash history 路由（`createWebHashHistory`），不要切 history 模式
- 路由守卫**仍只读 satoken**，不要回归 token hack

## 工作区

- `E:\DEMO\数据采集\DataupLoad-web\`（主目录内子目录，**不**用 worktree）
- codex 在主目录 `E:\DEMO\数据采集\` 跑，cd 进 DataupLoad-web 干活
- **不要 commit**（PM 验收后由 PM 统一 commit）

## 回执

完成后回 PM：
> "W-FRONT-02-D 完成，report 已写 docs/work-orders/W-FRONT-02-D-report.md"

并确保：
- `scripts/verify-w-front-02-D.ps1` 全 PASS（自己跑一次）
- `docs/work-orders/W-FRONT-02-D-sample.png` 截图就绪（主界面 + 菜单 active + 语言切换）
- 浏览器实测：登录 → 8 路由可点 + 菜单 active 态 + 切换语言刷新对应文字 + 刷新页面路由仍正确 + 未登录访问 /realtime → 跳 /login

## 禁止

- 不许跨子单
- 不许改 git 历史
- 不许引入 brief 之外的依赖
- 不许 commit / push
- 不许启动 / 重启服务（PM 负责）

## 超时

60 分钟无回执升级。
