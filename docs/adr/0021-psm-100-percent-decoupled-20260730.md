# ADR-0021: PSM 100% 解耦迁移完成

- **状态**: ✅ 已完成 + 浏览器实测通过
- **日期**: 2026-07-30
- **触发**: W-FRONT-02-G0（PSM 100% 解耦清理）
- **关联**: ADR-0016（前端架构对齐 PSM） / ADR-0017（PM 误诊纠正） / ADR-0018（X-1 smart-gate） / ADR-0020（Bitdefender 占 80 端口）

## 背景

W-FRONT-02 工单序列先后经历 6 个子单（A→B→C→D→E→F），从一开始的「直接渲染 Whitelabel」迭代到「Vue 3 SPA + 后端 Spring Boot」。但 PSM 老 SPA 的物理资源（编译产物、CSS、图片、字体）和 gate-routing hack 代码一直残留在 `DataupLoad/web/`，与新 Vue 3 SPA 并存。本 ADR 锁定清理动作和最终架构。

## 清理范围

### 删除的文件（共 151 个，约 20 MB）

#### 根目录 `DataupLoad/web/` — 删除 57 个
- **CSS（16）**: `alarm-*.css` / `client-*.css` / `clientStatic-*.css` / `defectManage-*.css` / `index-31885e80.css` / `index-5daec765.css` / `index-7226a3d3.css` / `index-aff68ccf.css` / `index-dea0f04e.css` / `index-e69bac34.css` / `index-ff4f195f.css` / `interfaceCall-*.css` / `operationLog-*.css` / `systemConfig-*.css` / `systemLog-*.css` / `vendor-6d15dd8f.css`
- **SVG（14）**: `10-*.svg` / `2-*.svg` / `3-*.svg` / `4-*.svg` / `404-*.svg` / `5-*.svg` / `8-*.svg` / `9-*.svg` / `手套检测*.svg` / `头部标题样式2-*.svg` / `图片检测*.svg` / `装饰线-*.svg` / `ic_spinbox_*.svg` / `icomoon-*.svg` / `icon-*.svg` / `vite.svg`
- **PNG（8）**: `bg2-*.png` / `dialog-header-*.png` / `message_confirm-*.png` / `pic_*.png` / `AI.png`
- **字体（3）**: `icomoon-*.woff` / `icomoon-*.eot` / `icomoon-*.ttf`
- **其他（5）**: `AI.png` / `browser.js` / `version.json` / `vite.svg` / `login_bg-f1ce3921.jpg`
- **备份 HTML（2）**: `index.psm-legacy.html`（PSM 回滚备份，git 历史有）/ `index.x1-backup.html`（X-1 gate-routing 备份，新前端验证后已不需要）
- **杂项**: `version.json` / `*.js` / `*.css` / `*.svg` / `*.png` 根目录残留

#### `DataupLoad/web/js/` — 删除 48 个 / 重建空目录
- 全部 `js/*.js` 老 SPA chunks：`alarm-legacy.*.js` / `alarm.*.js` / `browser.js` / `client-legacy.*.js` / `client.*.js` / `clientStatic-legacy.*.js` / `clientStatic.*.js` / `defectManage-legacy.*.js` / `defectManage.*.js` / `index-legacy.*.js`（11 个）/ `index.*.js`（10 个） / `interfaceCall-legacy.*.js` / `interfaceCall.*.js` / `operationLog-legacy.*.js` / `operationLog.*.js` / `polyfills-legacy.*.js` / `resize-event-legacy.*.js` / `resize-event.*.js` / `systemConfig-legacy.*.js` / `systemConfig.*.js` / `systemLog-legacy.*.js` / `systemLog.*.js` / `vendor-legacy.cd362f1d-*.js`（5.3 MB）/ `vendor.89afe428-*.js`（5.1 MB）
- `js/AI.png` / `js/browser.js` / `js/vite.svg`
- 删除整个 `js/` 目录并重建为空目录（保持相对路径兼容）

#### `DataupLoad/web/assets/` — 删除 46 个 / 保留 3 个
- 删除：所有老 SPA CSS / SVG / PNG / JPG / WOFF / EOT / TTF
- **保留（Vue 3 产物，F 子单部署）**:
  - `assets/index-B0hMWKcQ.css` (438 KB)
  - `assets/index-CndG5nFH.js` (2.6 MB)
  - `assets/interceptor-R5WXeEYz.js` (348 B)

### 重写 `DataupLoad/web/index.html`

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>英科手套中控平台</title>
    <script type="module" crossorigin src="/assets/index-CndG5nFH.js"></script>
    <link rel="stylesheet" crossorigin href="/assets/index-B0hMWKcQ.css">
  </head>
  <body>
    <div id="app"></div>
  </body>
</html>
```

**关键改动**：
- `lang="en"` → `lang="zh-CN"`
- `<title>DataupLoad</title>` → `<title>英科手套中控平台</title>`
- 移除所有 gate-routing / SHA256 / bootMainStage / syncTokenToLocalStorage / `document.cookie = "token="` hack 代码
- 保留 Vue 3 标准 SPA 入口（`<script type="module">` + `<link rel="stylesheet">` + `<div id="app">`）

## 验证证据

### 1. grep 验证（10 项全 0 命中）

| 模式 | 含义 | 命中数 |
|------|------|--------|
| `index\.f19ecd42` | 老 SPA main chunk | 0 |
| `index-legacy` | 老 SPA legacy chunks | 0 |
| `vendor\.89afe428` | 老 SPA vendor chunk | 0 |
| `vendor-legacy` | 老 SPA vendor legacy | 0 |
| `polyfills-legacy` | 老 SPA polyfills | 0 |
| `browser\.js` | 老 PSM polyfill 库 | 0 |
| `AI\.png` | 老 PSM AI 图标 | 0 |
| `syncTokenToLocalStorage` | gate-routing 同步函数 | 0 |
| `bootMainStage` | gate-routing 启动函数 | 0 |
| `document\.cookie\s*=\s*["']token` | cookie token hack（精确模式） | 0 |

> **关于 brief 中 `document\.cookie.*=.*token` 的 false positive**：该正则过宽，会匹配新 Vue 3 bundle 中 axios 库的 `xsrfCookieName:"XSRF-TOKEN"` 默认配置项。这是 axios 库自带的 XSRF cookie 处理（**读** cookie 名，不是**写** `token` cookie），不是 PSM 老 SPA 的 `document.cookie = "token=" + satoken` hack。本 ADR 用严格模式 `document\.cookie\s*=\s*["']token` 替代，精确匹配「cookie write to token=」这种 PSM hack 行为。验证脚本 `scripts/grep-verify-G0.ps1` 已使用严格模式。

### 2. curl 验证（5 项全 200，后端端口 8080）

```
[1] GET /                                  -> 200 (395 bytes, Vue 3 SPA)
[2] GET /assets/index-CndG5nFH.js          -> 200 (2,612,220 bytes)
[3] GET /assets/index-B0hMWKcQ.css         -> 200 (437,808 bytes)
[4] POST /web/auth/login (SHA256 pwd)      -> 200 + Set-Cookie satoken=4035e93e-...
[5] GET /web/account/current (with cookie) -> 200
```

老 SPA 路径全部失效：
- `GET /js/index.f19ecd42-*.js` → 404
- `GET /js/vendor.89afe428-*.js` → 404
- `GET /js/polyfills-legacy.*.js` → 404
- `GET /js/browser.js` → 404
- `GET /js/AI.png` → 404
- `GET /index.psm-legacy.html` → 500（backend WebConfigure 不识别老路径，已删除本地文件）
- `GET /vendor-6d15dd8f.css` → 500（同上）

### 3. 浏览器实测（Playwright 1.62 Chromium headless, 1440×900）

```
[1] GET http://127.0.0.1:8080/         -> 200
[2] title="英科手套中控平台"
[3] lang="zh-CN"
[4] #app children html length=2011 (Vue 3 mounted)
[5] login as super_admin / Abc12345    -> 200 + satoken
[6] post-login URL                     -> http://127.0.0.1:8080/#/realtime
[7-14] 8 业务路由 (/realtime, /alarm, /defect, /account, /systemConfig, /log, /userManage, /screen) -> 全部 OK
[15] console errors                    -> 0
[16] request failures                  -> 0
```

截图：
- `docs/work-orders/W-FRONT-02-G0-sample.png` (645 KB) — 登录页（玻璃风 + zh-CN 标题）
- `docs/work-orders/W-FRONT-02-G0-sample-main.png` (425 KB) — 登录后 /#/realtime
- `docs/work-orders/W-FRONT-02-G0-sample-realtime.png` (425 KB) — 显式访问 /#/realtime

## 最终架构

```
[Browser]
   |
   | GET / (HTTP 200, 395 bytes, Vue 3 SPA entry)
   | GET /assets/index-CndG5nFH.js (2.6 MB, Vue 3 bundle)
   | GET /assets/index-B0hMWKcQ.css (438 KB, Element Plus + 玻璃风 CSS)
   | GET /assets/interceptor-R5WXeEYz.js (348 B, axios interceptor)
   | POST /web/auth/login (JSON, password 已是 SHA256-hex)
   | GET /web/account/current (with satoken cookie)
   | GET /web/realtime /web/alarm /web/defect /web/account ... 
   v
[Backend Spring Boot on port 8080]
   - sa-token auth (satoken cookie)
   - bcrypt(SHA256(password)) double-hash
   - PostgreSQL on port 5433, db=intco
   - WebConfigure static resource mapping: /index.html + /assets/**
   - /web/auth/login, /web/account/current, /web/realtime, etc.
```

**架构特征**：
- **彻底解耦**：Vue 3 SPA 完全独立，不再依赖任何 PSM 老 SPA chunks / 老 JS / 老 CSS
- **标准 4 层**: 浏览器 → Vue 3 SPA (router guard via document.cookie satoken READ) → axios (XSRF-TOKEN cookie) → Spring Boot REST API → PostgreSQL
- **零 hack 代码**：没有 bootMainStage / syncTokenToLocalStorage / cookie token hack
- **零老 SPA 资源**：所有 PSM 老 SPA 物理文件已删除（151 个文件 / 20 MB）

## 与 ADR 序列的关联

| ADR | 标题 | 关系 |
|-----|------|------|
| 0016 | 前端架构对齐 PSM | 本 ADR 是 0016 实施后的清理收尾 |
| 0017 | 前端 bundle PM 误诊纠正 | 解释了 E 阶段 PM 误诊原因 |
| 0018 | X-1 smart-gate | 本 ADR 撤销了 X-1 的所有 hack 代码 |
| 0019 | pwsh UTF-8 BOM | 工具链规范 |
| 0020 | Bitdefender 占 80 端口 | 本 ADR 使用 8080 端口（受 0020 影响） |

## 时间线

- **2026-07-22**: E 阶段开始 PSM 风格前端对齐（ADR-0016）
- **2026-07-25**: F 阶段 Vue 3 SPA 部署 + 验证 18/18 PASS
- **2026-07-27**: D 阶段 X-1 smart-gate 临时方案启用（ADR-0018）
- **2026-07-28**: Bitdefender 切端口 8080（ADR-0020）
- **2026-07-30 11:21**: G0 阶段完成清理（151 文件 / 20 MB） + 重写 index.html + grep 全 0 + curl 全 200 + 浏览器 8/8 路由 OK + 0 console error

## 后续

- PM 验收 `verify-w-front-02-G0.ps1` 脚本（10/10 PASS 预期）
- PM 提交 commit "W-FRONT-02-G0: PSM 100% 解耦清理完成"
- **不 push**（PM 统一 push）
- 不动 `backups/2026-07-27-cleanup/`（gitignore）
- 不动后端 Java 代码
- 不动 git 历史

## 备注

- `js/index.f19ecd42-20260520160358.js.pre-w-front-00.bak` 和 `js/vendor.89afe428-20260520160358.js.pre-w-front-00.bak` 这 2 个 `.pre-w-front-00.bak` 备份文件原本计划保留（git 历史备份），但实际已被 `js/` 整个目录删除时一起删除。如需回滚老 SPA chunks，可从 git 历史 `W-FRONT-00` 之前的 commit 取回。
- `web/index.x1-backup.html` 是 X-1 阶段的 gate-routing 备份，新前端已通过浏览器验证，按 brief 要求删除。
