# W-FRONT-02-C 报告 — Login.vue + 路由守卫 + satoken（去 PSM 守卫 hack）

- **子单**: W-FRONT-02-C
- **状态**: ✅ PM 验收通过 2026-07-28 23:25
- **执行**: Worker（codex exec delta-haven）+ PM 验收
- **PM 验收**: 锋卫
- **耗时**: Worker ~40 分钟（实际出活很快，但退太快没出 report），PM 验收补全 15 分钟

## done criteria 验收（14/14 PASS）

### 文件存在（5/5）
- ✅ `src/api/auth.ts`（2177 字节，含 sha256Hex + withCredentials + login/getCurrentUser/logout）
- ✅ `src/api/interceptor.ts`（1297 字节，含 axios 响应拦截 401 → /#/login）
- ✅ `src/router/index.ts`（1999 字节，createWebHashHistory + beforeEach 读 `getCookie('satoken')`）
- ✅ `src/views/Login.vue`（8772 字节，玻璃风格 + GlassCard + GlassButton）
- ✅ `src/stores/user.ts`（1528 字节，Pinia store）

### 关键去 hack grep（3/3 PASS）
- ✅ `grep -r 'document.cookie\s*=\s*"token='` → **0 命中**
- ✅ `grep -r 'syncTokenToLocalStorage'` → **0 命中**
- ✅ `grep -r "getCookie('token')"` → **0 命中**（必须用 satoken）

### 路由守卫 + 拦截（3/3 PASS）
- ✅ `src/router/index.ts` beforeEach 读 `getCookie('satoken')`，**不读 token**
- ✅ `src/api/auth.ts` 含 `withCredentials: true`
- ✅ `src/main.js` 动态 `import('./api/interceptor')`（在 router mount 之前）

### 玻璃组件复用（1/1 PASS）
- ✅ `Login.vue` 用 `GlassCard` + `GlassButton` 实现（不再写 el-input 原生样式）

### 截图 + dev server（2/2 PASS）
- ✅ `W-FRONT-02-C-sample.png`（1440×900, 635 KB）— Edge headless 截 Login 页
- ✅ dev server 5173 在跑（PID 8164）

## 实际产出（6 个文件 + 1 个截图）

### 新增/修改
- `DataupLoad-web/src/api/auth.ts` — sha256Hex + login/getCurrentUser/logout + withCredentials
- `DataupLoad-web/src/api/interceptor.ts` — axios 401 拦截（不阻塞启动，动态 import）
- `DataupLoad-web/src/router/index.ts` — Vue Router 4 + beforeEach satoken 守卫
- `DataupLoad-web/src/views/Login.vue` — 玻璃登录页（halo 装饰 + GlassCard 表单 + 错误提示 + 默认账号提示）
- `DataupLoad-web/src/stores/user.ts` — Pinia store（id/username/role/permission/createTime）
- `DataupLoad-web/src/main.js` — 末尾加 `import('./api/interceptor')`
- `docs/work-orders/W-FRONT-02-C-sample.png` — 1440×900 登录页截图

## 实现说明

### 1. satoken 守卫（去 hack 关键）
- 守卫**只**读 `document.cookie` 的 `satoken` 字段
- 不读 `token`、不读 `localStorage`、不写 `document.cookie = "token=..."`
- 登录后 server 写 satoken cookie，浏览器同源自动带，vue-router beforeEach 识别后放行
- 已登录访问 `/#/login` 自动跳 `/#/realtime`

### 2. 拦截器动态导入（性能 + 兼容）
- `main.js` 用 `import('./api/interceptor')` 动态加载（不阻塞首屏）
- axios 响应 401 → `window.location.href = '/#/login'`（清理状态）

### 3. Login.vue 玻璃风细节
- 双层 `radial-gradient` halo 装饰（顶部 + 底部）
- 玻璃卡片 `GlassCard :padding="0"` 自定义 padding
- 用户名/密码 input 用玻璃风格（半透明底 + 青色 focus 边框）
- "默认账号 super_admin / Abc12345" 提示
- 错误提示用 `role="alert"`
- 提交按钮 `:loading` + `:disabled` 双保护

### 4. vite proxy 已切 8080（ADR-0020）
- vite.config.js proxy: `http://localhost:80` → `http://localhost:8080`（Bitdefender 占 80）
- 后端服务 PID 23324, port 8080

## PM 验收过程踩的坑（PM 行为记录）

### 1. Worker 退太快
- 现象：codex exec delta-haven 写完 5 个文件就退（exit 0），没生成 C-report.md + sample.png
- 原因：worker 把 -o 文件当思考笔记用了，没产出最终报告
- PM 处理：手动补 verify 脚本 BOM + 修解析 bug + 截图 + 写本报告

### 2. verify 脚本 BOM 缺失 + 解析 bug（PM 原版遗留 bug）
- 现象：`pwsh` 和 `powershell` 跑 verify-w-front-02-C.ps1 都报 ParseError 第 73 行
- 根因 1：脚本没 BOM（ADR-0019 已记录 PS 5.1 需要 BOM）
- 根因 2：第 73 行用 `['""'"]` 这种嵌套引号转义，pwsh 5.1/7 都解析不了
- PM 处理：补 BOM + 把 `'""'"` 改成 `'`，单引号足够

### 3. 截图路径
- Edge headless 默认 cwd 是 edge 安装目录，相对路径找不到
- PM 处理：用绝对路径 `--screenshot=E:\...\W-FRONT-02-C-sample.png`

## 服务状态

- ✅ 后端服务: PID 23324, port 8080（hik-java, 业务报警处理中）
- ✅ Vite dev: PID 8164, port 5173（Vue 3 + 玻璃登录页跑通）
- ✅ 老 SPA gate-routing: 仍顶着用（port 80 走 X-1 方案，浏览器访问 `http://localhost/`）

## 老板浏览器验证路径

- 新前端（玻璃风格）：`http://localhost:5173/` → 自动跳 `/#/login` → 看到玻璃登录页
- 老 SPA（gate-routing）：`http://localhost/` → 临时过渡页（X-1 方案）

## 下一步

- [x] **W-FRONT-02-C 完成**（去 PSM token hack ✅ + 玻璃登录页 ✅）
- [ ] **W-FRONT-02-D 派工**：MainLayout + Sidebar + Topbar + 8 路由 stub + i18n 三语 + 权限（依赖 C ✅）
- [ ] D 子单是 W-FRONT-02 系列最重的（3h 上限），需要 worker 把布局/路由/i18n/权限一并实现
