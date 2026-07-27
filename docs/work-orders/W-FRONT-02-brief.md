# W-FRONT-02 总计划 v2 — Vue 3 玻璃风格前端 + PSM 守卫优化 + 100% 解耦

- **状态**: 派工中（阶段 0：B 已就绪）
- **触发**: 2026-07-27 老板两个目标：
  1. **优化 PSM 守卫**（干掉 cookie `token` hack，新前端从根上不绕）
  2. **界面风格重构**（苹果系玻璃风格，UI 完全重做）
- **PM**: 锋卫
- **风格基准**: `E:\DEMO\数据采集\style-sample-login.png` + `style-sample-main.png`（已老板确认）
- **关联**:
  - ADR-0016（前端对齐 PSM SPA）
  - ADR-0018（方案 X-1 gate-routing 临时过渡）
  - ADR-0019（PowerShell UTF-8 BOM）
  - W-FRONT-01（已延后，已废）
  - W-FRONT-X1-report（gate-routing 实施记录）
  - W-FRONT-02-A-report（A 子单验收）

## 自查（5 维度 · 5/5 完整 · 无错漏）

| 维度 | 子单覆盖 | 验证 |
|------|---------|------|
| **PSM 守卫优化** | C 子单：新路由守卫直接读 satoken，无 hack | G0 grep + verify-w-front-02-C.ps1 |
| **界面风格重构** | B 子单：设计 token + 玻璃组件库 + Element Plus 重写 | 截图对照 style-sample-*.png |
| **业务对齐** | E1-E8：8 张子单，每张截图 diff PSM 老 SPA | PM 逐像素验收 |
| **解耦清理** | G0：删老 SPA 文件 + 删 gate-routing + 删 hack | grep 验证 0 hack + 0 老 chunk |
| **回归 + 归档** | G：端到端 12 项 + ADR-0021 + push | verify-w-front-02-G.ps1 |

## 并行派工方案（5 阶段）

| 阶段 | 并行子单 | 耗时 | 关键依赖 |
|------|---------|------|---------|
| **1** | B（设计 token + 玻璃组件库） | 2h | A ✅ |
| **2**（B 完成后） | C / D / F / G0（4 张并行） | max 3h | B |
| **3**（D 完成后） | E1 / E2 / E3 / E4 / E5 / E6 / E7 / E8（8 张并行） | max 1.5h | D |
| **4**（全部完成后） | G（回归 + 归档） | 1h | G0 + E1-E8 |

**总耗时**：2 + 3 + 1.5 + 1 = **7.5h**（vs 单线 21.5h，压缩 65%）

## 多 worker 隔离策略

每个 worker 用 **git worktree** 隔离工作区：
- `E:\DEMO\数据采集-worktrees\w-front-02-B\` ← B worker
- `E:\DEMO\数据采集-worktrees\w-front-02-C\` ← C worker
- `E:\DEMO\数据采集-worktrees\w-front-02-D\` ← D worker
- ...
- 主目录 `E:\DEMO\数据采集` ← PM 控制 git merge

每个 worker 在自己的 worktree 干活，**完成后 PM 在主目录 merge**。

## 子单流水线（B 起步）

### B - 设计 token + 玻璃组件库（2h）

**关键产出**：
- `src/styles/tokens.scss` — 设计 tokens（颜色/阴影/圆角/间距/字号）
- `src/styles/element-overrides.scss` — Element Plus 主题变量重写
- `src/components/GlassCard.vue` — 玻璃卡片组件
- `src/components/GlassButton.vue` — 玻璃按钮组件（封装 el-button）
- `src/components/GlassMenuItem.vue` — 玻璃药丸菜单项
- `src/components/GlassTable.vue` — 玻璃表格组件（封装 el-table）
- `src/components/GlassPage.vue` — 玻璃页面容器

**约束**：
- 必须能 `import { GlassCard } from '@/components/GlassCard.vue'` 在 App.vue 直接用
- 元素值必须与 style-sample-*.png 一致
- 不能引入新依赖（除 Element Plus icons + echarts）
- 全部用 `<script setup lang="ts">` TypeScript

**verify 脚本**：`scripts/verify-w-front-02-B.ps1`（13 项 check）

### C - Login.vue + 路由守卫 + satoken（2h）

**关键产出**：
- `src/views/Login.vue` — 玻璃风格登录页（基于 GlassCard + GlassButton）
- `src/router/index.ts` — Vue Router 4 配置，beforeEach 守卫读 **satoken**（不读 token）
- `src/api/auth.ts` — 登录 API 调用（axios + withCredentials）
- `src/api/interceptor.ts` — axios 请求拦截器（自动带 satoken cookie）
- `src/stores/user.ts` — Pinia store（user info + satoken state）

**关键去 hack 验证**：
- grep `document.cookie\s*=\s*['"]token=` ← 应当 0 结果
- grep `syncTokenToLocalStorage` ← 应当 0 结果
- grep `getCookie\(['"]token['"]\)` ← 应当 0 结果
- router beforeEach 必须读 `getCookie('satoken')`

**verify 脚本**：`scripts/verify-w-front-02-C.ps1`（12 项 check）

### D - 布局 + 8 路由 stub + 权限 + i18n（3h）

**关键产出**：
- `src/layouts/MainLayout.vue` — 玻璃主框架（侧边栏 + 顶栏 + 内容区）
- `src/layouts/Sidebar.vue` — 玻璃侧边栏（药丸菜单）
- `src/layouts/Topbar.vue` — 玻璃顶栏（breadcrumb + 语言切换 + user chip）
- `src/views/RealTime.vue` 等 8 个 stub 页 — 占位 + GlassPage + 标题
- `src/router/index.ts` — 8 路由表（/realTime /alarm /defect /account /systemConfig /log /userManage /lineConfig 等）
- `src/i18n/index.ts` — i18n 三语（zh-CN / en-US / id-ID）+ 200+ keys
- `src/stores/permission.ts` — Pinia store（角色权限）

**verify 脚本**：`scripts/verify-w-front-02-D.ps1`（15 项 check）

### E1-E8 - 业务对齐期（8 张并行，每张 1.5h）

| 子单 | 页面 | PSM 对照点 |
|------|------|-----------|
| E1 | 实时数据看板 | /realTime 折线图 + KPI |
| E2 | 报警管理 | /alarm 列表 + WS 实时推送 |
| E3 | 缺陷处理 | /defect 表格 + 详情 |
| E4 | 账号管理 | /account /userManage CRUD |
| E5 | 系统配置 | /systemConfig /lineConfig 表单 |
| E6 | 操作日志 | /log 表格 + 筛选 |
| E7 | 设备监控 | /line 状态卡片 |
| E8 | 大屏模式 | /Screen 全屏 + 多图表 |

**每个 worker 必须**：
1. 先用 Edge headless 截 PSM 老 SPA 对应页（基准图）
2. 实现新前端对应页
3. 截新前端图
4. 用 PSM 基准图 vs 新前端图做 diff，差异超 5% 必须修
5. 输出 `docs/work-orders/W-FRONT-02-EX-report.md` 含两张图 + diff 结论

### F - 打包 + 部署（1.5h）

**关键产出**：
- `vite build` 产物 `dist/`
- 拷 `dist/*` 到 `DataupLoad/web/`
- 重启后端服务
- 验证 GET / 返回 Vue 3 SPA
- 端到端登录 → 主界面 → 8 大业务页都能进

### G0 - PSM 彻底解耦清理（1h）

**关键动作清单**：
1. 删 `DataupLoad/web/index.psm-legacy.html`
2. 删 `DataupLoad/web/js/index.f19ecd42*.js` / `index-legacy.0208e821*.js` / `polyfills-legacy*.js` / `browser.js` / `AI.png` / `vendor.89afe428*.js` / `vendor-legacy.cd362f1d*.js`
3. 删 `DataupLoad/web/assets/*.css`（PSM 老 SPA CSS）
4. 删 `DataupLoad/web/login.html` `login.js`（早就删了，确认）
5. **删 `DataupLoad/web/index.html` 里的 gate-routing**：去掉 SHA256 登录表单、bootMainStage 逻辑、syncTokenToLocalStorage、动态注入老 SPA script
6. **新 `DataupLoad/web/index.html`** = Vue 3 标准入口：`<div id="app"></div>` + `<script type="module" src="/assets/index-xxxxxx.js"></script>`
7. grep `DataupLoad/web/` 验证 0 老 SPA 残留 + 0 hack 代码
8. ADR-0021 输出

### G - 回归测试 + 文档归档（1h）

**关键产出**：
- 端到端 12 项验收（登录 / 8 大业务页 / i18n 切换 / 权限 / 报警 WS / 导出 Excel）
- `W-FRONT-02-report.md` 总报告
- ADR-0021「PSM 100% 解耦迁移完成」
- HEARTBEAT 更新
- git push 到 main

## PM 派工规则（worker 必读）

每个子单 brief 含：
1. **任务描述**（明确单一职责）
2. **必读**（brief 路径 + ADR 编号）
3. **必产出**（文件清单 + done criteria）
4. **约束**（不许改的文件清单 + 编码规范）
5. **PM 验收门控**（verify 脚本 + 等多久）
6. **派工 prompt 模板**：
   ```
   【任务】W-FRONT-02-X
   【必读】
   - /brief: docs/work-orders/W-FRONT-02-{X}-brief.md
   - /约束:  ADR-0016 + ADR-0018 + W-FRONT-02-brief.md
   【必产出】
   1. docs/work-orders/W-FRONT-02-{X}-report.md
   2. [代码文件清单]
   【PM 验收门控】
   - 完成后回 PM: "W-FRONT-02-X 完成, report 已写"
   - PM 执行 scripts/verify-w-front-02-{X}.ps1 不通过则打回
   - 60 分钟无回执升级
   【禁止】
   - 不许跨子单
   - 不许改 git 历史
   - 不许引入 brief 之外的依赖
   ```

## 服务状态（19:26）

- 后端 hik-java: PID 35704, port 80, 报警接收中
- Vite dev: PID 13200, port 5173, A 子单脚手架就绪
- 老 SPA gate-routing: 仍在 web/index.html（**G0 子单清理**）
- 老板确认风格：style-sample-login.png + style-sample-main.png

## 开始时间

- 派 B：2026-07-27 19:26

