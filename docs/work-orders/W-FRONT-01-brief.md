# W-FRONT-01 主工单 Brief — 前端架构对齐 PSM

- **开工**: 2026-07-25 14:30
- **PM**: 锋卫 🏭
- **目标**: 把本项目前端从"无前端、纯 API"模式，重构为 PSM 同款的 Vue 3 + Vite SPA，浏览器零 Whitelabel
- **触发**: 老板 14:25 指令"必须跟 PSM 对齐"
- **ADR**: `docs/adr/0016-frontend-align-psm-spa-20260725.md`
- **后端**: 不动（API 路径 / satoken / 静态资源映射即可）

---

## PSM 摸底（已完成）

| 项 | 值 |
|----|---|
| PSM web 根 | `E:\DEMO\数据采集\docs\domain\海康大屏逆向\PSM\server\web\` |
| 入口 | `index.html`（Vite SPA 壳，2638B） |
| 路由 | Vue Router 4（编译产物在 `js/index.f19ecd42-xxx.js`，96KB） |
| 登录 API | `POST /web/auth/login`，body `{username, password: sha256(pwd)}` |
| 兜底逻辑 | 路由守卫检查 sessionStorage，无 user → `router.replace('/Login')` |
| 主路由 | `/Login`、`/realTime`、`/dataview`、`/client`、`/alarm`、`/systemConfig`、`/userManage`、`/logs/*` |
| i18n | zh-CN / en-US / id-ID 三语（字符串已在编译产物里提取） |
| 编译产物总量 | vendor.js 5.2MB + index.js 96KB + css 320KB+ ≈ 12MB |

---

## 子工单（按顺序）

### W-FRONT-01-A — 前端工程脚手架
- 路径：`E:\DEMO\数据采集\DataupLoad-web\`（新建）
- 内容：`package.json` + `vite.config.js` + `index.html`（开发版） + `src/main.js` + `src/App.vue` + 空 `src/router/index.js` + 空 `src/api/http.js`
- 依赖：`vue@^3.4`、`vue-router@^4`、`pinia@^2`、`element-plus@^2.7`、`axios@^1.7`、`vite@^5`、`@vitejs/plugin-vue@^5`
- 验证：`npm install` 成功 + `npm run dev` 启动 + 浏览器 `localhost:5173` 看到空 App 组件
- 不动后端

### W-FRONT-01-B — Login 页 + 路由守卫 + satoken 集成
- `src/views/Login.vue`：Element Plus `<el-form>` + 账号/密码框 + 登录按钮 + 语言切换
- `src/api/http.js`：axios 实例，`withCredentials: true`（带 satoken cookie），baseURL = `/`
- `src/router/index.js`：注册 `/Login` + `/realTime`（占位），全局 `beforeEach` 守卫检查 sessionStorage.user，无则 `next('/Login')`
- `src/api/login.js`：`login({username, password})` → `http.post('/web/auth/login', {username, password: sha256Hex(password)})`，成功后 `sessionStorage.setItem('user', ...)`
- 验证：开发模式 `localhost:5173` 自动跳 `/Login`，填 `super_admin / Abc12345` → 200 → 跳 `/realTime` 占位页

### W-FRONT-01-C — i18n 三语复用 PSM
- `src/i18n/index.js`：Vue I18n 9 + `createI18n` + 三个 locale
- `src/i18n/zh-CN.js`、`en-US.js`、`id-ID.js`：直接复用 PSM 提取的字符串（`<Login>` 段、`<header>` 段、共用段）
- `Login.vue` 顶部放 `<el-select v-model="lang">` 切换语言，立即生效
- 验证：切换语言 → 表单 label 实时变化（账号/Username/Nama Pengguna）

### W-FRONT-01-D — 静态资源部署 + GET `/` 兜底
- `vite.config.js` 加 `build.outDir = '../DataupLoad/src/main/resources/static'`
- `package.json` 加 `build` 脚本：`vite build`（同时复制 `dist/index.html` 覆盖 `static/index.html`）
- 验证：`npm run build` 后 `static/` 有 `index.html` + `assets/` + `js/`
- 后端：Spring Boot 默认会把 `static/` 暴露到根路径，`GET /` 应自动返回 `index.html`（无需改 Java 代码）
- 边缘情况：如果 Spring Boot 默认 Welcome Page 处理不生效，W-FRONT-01-D brief 里给出 1 行 `WebMvcConfigurer.addViewControllers` 兜底方案

### W-FRONT-01-E — 端到端验收
- 重启 jar（必须 java -jar 跑新打的，不是 target/classes），保证读最新 static
- 验收清单：
  - `GET http://localhost/` → 200 + HTML（不是 Whitelabel、不是 404）
  - 浏览器访问 `http://localhost/` → 自动跳 `/Login`（路由守卫）
  - 填 `super_admin / Abc12345` → POST 200 → 跳业务页
  - 刷新页面不会丢登录态（sessionStorage 还在）
  - `GET /web/auth/login` 浏览器手动访问 → 405（这是正确行为，**前端从不直接 GET**，跟 PSM 行为一致）
- 报告：`W-FRONT-01-report.md`，含前后端最终目录树 + 启动截图

---

## 验收标准（硬指标）

| 项 | 标准 |
|----|------|
| `GET /` 浏览器 | 200 + HTML（首屏 SPA） |
| 浏览器 Whitelabel | **0**（所有路径都有正确响应） |
| `POST /web/auth/login` super_admin/Abc12345 | 200 + satoken cookie |
| 前端工程独立 npm run dev | 5173 端口可调 |
| `npm run build` | 产物进 `DataupLoad/src/main/resources/static/`，单 jar 部署 |
| i18n | zh-CN / en-US / id-ID 三语切换正常 |
| 后端 Java 代码 | **零改动**（除非 W-FRONT-01-D 兜底需要 1 行配置） |

## 风险

- 工作量大，**老板耐心**决定分阶段深度。默认只交付到 W-FRONT-01-E 验收登录闭环，业务页（realTime/dataView 等）留 W-FRONT-02+ 子单
- Node.js 工具链：本机有，老板生产机可能没有 → vite build 在发布机做一次，jar 内已含 static/
- Element Plus vs Element UI：默认 Plus，brief 注明，等老板拍板

## 不交付（明确范围）

- ❌ PSM 全部 16 个业务页（realTime / dataview / client / alarm / systemConfig / userManage / logs 等）— 留 W-FRONT-02 起的子单
- ❌ 大屏可视化（ECharts 图表）— W-FRONT-03
- ❌ 多皮肤 / 主题色 — W-FRONT-04
- ❌ WebSocket 实时报警推送前端 — W-FRONT-05
- ❌ 英科手套对接 — 与本工单无关

## 派工计划

PM 自己拆 brief + 派工，不写代码。子单 A→E 顺序下发，每单完成后 PM 验收再下下一单。
