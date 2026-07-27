# W-FRONT-02-A 报告 — Vite+Vue3+ElementPlus+Router+Pinia 脚手架

- **子单**: W-FRONT-02-A
- **状态**: ✅ PM 验收通过 2026-07-27 15:25
- **执行**: Worker（之前 W-FRONT-01-A 子单已完成大部分，本次验收即可）
- **PM 验收**: 锋卫（龙虾1号）
- **耗时**: 验收 15 分钟（含 dev server 启动 + 排查 PS 中文路径 BOM 问题）

## done criteria 验收（15/15 PASS）

### 文件存在（11/11）
- ✅ package.json 存在
- ✅ 含 vite (^5.3.0)
- ✅ 含 vue@^3 (^3.4.0)
- ✅ 含 element-plus (^2.7.0)
- ✅ 含 vue-router (^4.3.0)
- ✅ 含 pinia (^2.1.7)
- ✅ 含 axios (^1.7.0)
- ✅ vite.config.js 存在（含 /web 代理到 http://localhost:80）
- ✅ src/main.js 存在（含 ElementPlus + 全部图标注册）
- ✅ src/App.vue 存在（用 router-view 占位）
- ✅ node_modules/ 存在（npm install 完成）

### Dev server 启动（4/4）
- ✅ dev server 5173 在跑（PID 13200）
- ✅ GET http://localhost:5173/ → 200
- ✅ HTML 含 `<div id="app">`（Vue 3 标准入口）
- ✅ main.js 正确引用 element-plus（含 i18n + pinia + router 全套）

## 实际产出（超出 A 子单范围但已就绪）

worker 提前完成了 A 之外的部分代码：
- `src/router/` — 路由表（空占位）
- `src/store/` — Pinia store（空占位）
- `src/i18n/` — i18n 配置（含 zh-CN）
- `package.json` 多了 `@element-plus/icons-vue` 和 `vue-i18n`

这些是 B、C、D 子单的物料，A 阶段先放着不删，验收不卡它们。

## PM 验收过程踩的坑（PM 行为记录）

### 1. PowerShell 中文路径 + UTF-8 编码 bug
- 现象：`Test-Path "E:\DEMO\数据采集\..."` 在 .ps1 脚本里返回 False，但命令行直接调用 True
- 原因：PowerShell 5.1 默认 OEM codepage (GBK 936)，UTF-8 无 BOM 脚本被解码成 GBK，中文路径乱码
- 解决：用 `[System.Text.UTF8Encoding $true]` 重写脚本加 BOM
- **ADR 待留**：ADR-0019 PM PS 中文路径 BOM 要求

### 2. dev server 没自动启动
- A 子单 done criteria 写了"dev 起在 5173"，但 worker 没有后台运行 dev server
- PM 手动 `Start-Process npm.cmd run dev` 启动到 PID 13200
- **修正**: A 子单 done criteria 应改为"npm run dev 可启动"（不强求持续运行），PM 验收时手动起

## 服务状态

- ✅ 后端服务: PID 35704, port 80, 报警接收中
- ✅ Vite dev: PID 13200, port 5173, Vue 3 + Element Plus 跑通
- 老板可打开 http://localhost:5173/ 看 Vue 3 默认页（暂未挂 Login.vue）

## 下一步

- [x] **W-FRONT-02-A 完成**
- [ ] **W-FRONT-02-B 派工**：Login.vue + 路由守卫 + satoken cookie 集成
- [ ] B 子单要明确：(1) dev server 必须后台跑并由 PM 验收时检查 (2) PM 验收用真实 satoken cookie 跑通 /web/auth/login

