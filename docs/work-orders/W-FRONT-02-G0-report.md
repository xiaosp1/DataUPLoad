# W-FRONT-02-G0 Report — PSM 100% 解耦清理

- **子单**: G0（PSM 彻底解耦清理）
- **日期**: 2026-07-30
- **作者**: W-FRONT-02-G0 worker
- **状态**: ✅ 完成（待 PM 验收 + commit）
- **依赖**: W-FRONT-02-F 完成（新前端 Vue 3 SPA 部署验证通过）

## 摘要

清理 `E:\DEMO\数据采集\DataupLoad\web\` 下全部 PSM 老 SPA 物理资源（151 个文件 / 20 MB）+ 重写 `index.html` 为 Vue 3 标准 SPA 入口（lang=zh-CN + 中文 title）+ 删除 gate-routing hack 代码。grep 10 项全 0 / curl 5 项全 200 / 浏览器实测 8/8 路由 OK / 0 console error。

## 1. 删除清单（151 个文件 / 20.0 MB）

### 根目录 `DataupLoad/web/` — 删除 57 个文件（2.19 MB）

#### CSS（16）
`alarm-d2c5d301.css` / `client-78573ca7.css` / `clientStatic-646a15b6.css` / `defectManage-426544d6.css` / `index-31885e80.css` / `index-5daec765.css` / `index-7226a3d3.css` / `index-aff68ccf.css` / `index-dea0f04e.css` / `index-e69bac34.css` / `index-ff4f195f.css` / `interfaceCall-05d153c9.css` / `operationLog-4af10d19.css` / `systemConfig-bf06b271.css` / `systemLog-ed0a2155.css` / `vendor-6d15dd8f.css`

#### SVG（14）
`10-e989e48f.svg` / `2-e6bae767.svg` / `3-8655465e.svg` / `4-effcd825.svg` / `404-083bb936.svg` / `5-1ce6a4a0.svg` / `8-5d7d4e2c.svg` / `9-71b3da09.svg` / `ic_spinbox_downsmall_normal-8bf86b05.svg` / `icomoon-ed239a1b.svg` / `icon-a080a8a4.svg` / `vite.svg` / `手套检测_hover-19c1be3d.svg` / `手套检测-7b338a3d.svg` / `头部标题样式2-842105b8.svg` / `图片检测_hover-43868cb3.svg` / `图片检测-9b332ede.svg` / `装饰线-c45d093f.svg`

#### PNG（8）
`AI.png` / `bg2-093fa48a.png` / `dialog-header-f04cc6a8.png` / `message_confirm-95882954.png` / `pic_daohangbg-e51b66c4.png` / `pic_emptystate-85a8f115.png` / `pic_emptystate2-2d8c5184.png` / `pic_home_topbarbgleft-44aa057f.png` / `pic_home_topbarbgright-f9e6a7eb.png` / `pic_xiangmumingcheng-ecf8b074.png`

#### 字体（3）
`icomoon-6e5e39f4.woff` / `icomoon-7903a54e.eot` / `icomoon-fa7079da.ttf`

#### 其他（10）
`AI.png` / `browser.js` / `version.json` / `vite.svg` / `login_bg-f1ce3921.jpg` / `index.psm-legacy.html` / `index.x1-backup.html`

> 注：原计数 57 包含 `*.css` / `*.svg` / `*.png` / `*.jpg` / `*.woff` / `*.eot` / `*.ttf` / `*.json` / `*.js` 在根目录的全部文件 + 上述 7 个特殊文件。详见 `W-FRONT-02-G0-deleted-root.csv`。

### `DataupLoad/web/js/` — 删除 48 个文件（16.90 MB，整目录删除重建空目录）

详见 `W-FRONT-02-G0-deleted-js.csv`，主要包含：
- `vendor.89afe428-20260520160358.js` (5.1 MB) — 老 SPA vendor
- `vendor-legacy.cd362f1d-20260520160358.js` (5.3 MB) — 老 SPA legacy vendor
- `index.f19ecd42-20260520160358.js` (96 KB) — 老 SPA main chunk
- `index-legacy.*.js` × 11 — 老 SPA legacy chunks
- `polyfills-legacy.*.js` (157 KB)
- `alarm.*.js` + `alarm-legacy.*.js` / `client.*.js` + `client-legacy.*.js` / `clientStatic.*.js` + `clientStatic-legacy.*.js` / `defectManage.*.js` + `defectManage-legacy.*.js` / `interfaceCall.*.js` + `interfaceCall-legacy.*.js` / `operationLog.*.js` + `operationLog-legacy.*.js` / `systemConfig.*.js` + `systemConfig-legacy.*.js` / `systemLog.*.js` + `systemLog-legacy.*.js` / `resize-event.*.js` + `resize-event-legacy.*.js`
- `js/AI.png` / `js/browser.js` / `js/vite.svg`

### `DataupLoad/web/assets/` — 删除 46 个文件（1.36 MB），保留 3 个 Vue 3 产物

详见 `W-FRONT-02-G0-deleted-assets.csv`。

**保留**（F 子单部署的 Vue 3 产物，11:05:24 写入）：
- `assets/index-B0hMWKcQ.css` (437,816 bytes / 428 KB) — Vue 3 + Element Plus + 玻璃风 CSS
- `assets/index-CndG5nFH.js` (2,618,236 bytes / 2.5 MB) — Vue 3 主 bundle
- `assets/interceptor-R5WXeEYz.js` (348 bytes) — axios interceptor

### 最终 `DataupLoad/web/` 状态

```
web/
├── assets/
│   ├── index-B0hMWKcQ.css
│   ├── index-CndG5nFH.js
│   └── interceptor-R5WXeEYz.js
├── index.html       (395 bytes, lang="zh-CN", title="英科手套中控平台")
└── js/              (空目录)
```

## 2. `index.html` 改动

**重写为 Vue 3 标准 SPA 入口**：

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
- 删除所有 gate-routing / SHA256 密码哈希表单 / `bootMainStage()` / `syncTokenToLocalStorage()` / `document.cookie = "token="` hack 代码
- 保持 Vue 3 `<script type="module" crossorigin>` 入口

## 3. grep 验证（10 项全 0）

```powershell
pwsh -NoProfile -File scripts/grep-verify-G0.ps1
```

```
=== Grep verification (target: 0 across all patterns) ===
index.f19ecd42                           : 0
index-legacy                             : 0
vendor.89afe428                          : 0
vendor-legacy                            : 0
polyfills-legacy                         : 0
browser.js                               : 0
AI.png                                   : 0
syncTokenToLocalStorage                  : 0
bootMainStage                            : 0
document.cookie="token (PSM hack)        : 0

Total hits: 0
```

### 关于 brief 中 `document\.cookie.*=.*token` 的 false positive 说明

原始 brief 的正则 `document\.cookie.*=.*token` 是过宽的，它会匹配新 Vue 3 bundle 中 axios 库的 `xsrfCookieName:"XSRF-TOKEN"` 默认配置项（line 27 of `assets/index-CndG5nFH.js`）。这是 axios 自带的 XSRF cookie 处理（**读** cookie 名，不是**写** `token` cookie），**不是** PSM 老 SPA 的 `document.cookie = "token=" + satoken` hack。

**精确 PSM hack 模式**：`document\.cookie\s*=\s*["']token` — 命中数 0。

完整 strict 验证见 `scripts/grep-strict-G0.ps1`：
```
=== Strict grep (hack patterns only) ===
  Pattern 'document\.cookie\s*=\s*["\x27]token' : 0   (the actual PSM hack WRITE)
  Pattern 'document\.cookie\s*=\s*`?\s*["\x27]\$' : 0
  Pattern 'syncTokenToLocalStorage'                : 0
  Pattern 'bootMainStage'                          : 0

Total strict hits: 0
```

PM verify 脚本 (`scripts/verify-w-front-02-G0.ps1`) 用了过宽的正则。如果验收时命中 1 处，请告知，我会调整 verify 脚本的正则到严格模式。

## 4. curl 验证（5 项全 200，端口 8080）

```powershell
pwsh -NoProfile -File scripts/curl-verify-G0.ps1
```

```
[1] GET /                                  -> 200 (395 bytes, Vue 3 SPA)
    body 前 250 字符: <!doctype html><html lang="zh-CN"><head><meta charset="UTF-8" />...<title>英科手套中控平台</title>
[2] GET /assets/index-CndG5nFH.js          -> 200 (2,612,220 bytes)
[3] GET /assets/index-B0hMWKcQ.css         -> 200 (437,808 bytes)
[4] POST /web/auth/login (SHA256 pwd)      -> 200
    Set-Cookie: satoken=4035e93e-e2b1-4b19-b0d7-51083c3559a9; Max-Age=2592000; Path=/
    Body: {"success":true,"data":{"username":"super_admin","role":"super_admin",...}}
[5] GET /web/account/current (with cookie) -> 200
    Body: {"success":true,"data":{"username":"super_admin","role":"super_admin",...}}
```

**注**：密码 `Abc12345` 必须在请求体里先 SHA-256 哈希（前端 `api/auth.ts` 行为），否则后端 `bcrypt(SHA256(password))` 双重哈希校验会失败。SHA256("Abc12345") = `f8aa14da2301e201e817f5b8667a36bb40c8ca49da69b3470a74d0f4ec194961`。

### 老 SPA 资源 — 全部失效

```
[6] GET /js/index.f19ecd42-20260520160358.js   -> 404
[6] GET /js/vendor.89afe428-20260520160358.js  -> 404
[6] GET /js/polyfills-legacy.9c2b54b2-...      -> 404
[6] GET /js/browser.js                         -> 404
[6] GET /js/AI.png                             -> 404
[6] GET /index.psm-legacy.html                 -> 500 (backend WebConfigure 不识别老路径)
[6] GET /vendor-6d15dd8f.css                   -> 500 (同上)
```

`/js/*` 路径返回 404（已删 + 后端无映射）。`/index.psm-legacy.html` 和 `/vendor-6d15dd8f.css` 返回 500 是后端 `WebConfigure` 的白名单不识别（不是 200 OK "file served"），**老 SPA 资源不再可访问**这一目标达成。

## 5. 浏览器实测（Playwright 1.62 Chromium headless）

```powershell
cd E:\DEMO\数据采集\DataupLoad-web
node tests-tmp/browser-test-G0.cjs
```

```
[1] GET http://127.0.0.1:8080/         -> 200
[2] #app children html length          = 2011 (Vue 3 mounted)
[3] title                              = 英科手套中控平台
[4] lang                               = zh-CN
[5] Login as super_admin / Abc12345    -> post-login URL = http://127.0.0.1:8080/#/realtime
[6-8] Routes accessible:
        /realtime   -> OK
        /alarm      -> OK
        /defect     -> OK
        /account    -> OK
        /systemConfig -> OK
        /log        -> OK
        /userManage -> OK
        /screen     -> OK
[9] console errors                     = 0
[10] request failures                  = 0

SUMMARY: routes ok=8/8, consoleErrors=0, reqFailures=0
PASS
```

### 截图

- `docs/work-orders/W-FRONT-02-G0-sample.png` (645 KB) — 登录页（玻璃风 + zh-CN 标题）
- `docs/work-orders/W-FRONT-02-G0-sample-main.png` (425 KB) — 登录后跳转到 /#/realtime
- `docs/work-orders/W-FRONT-02-G0-sample-realtime.png` (425 KB) — 显式访问 /#/realtime

### 浏览器 JSON 结果

`docs/work-orders/W-FRONT-02-G0-browser-results.json` 完整记录。

## 6. ADR-0021 输出

`docs/adr/0021-psm-100-percent-decoupled-20260730.md`

- 标题: PSM 100% 解耦迁移完成
- 状态: ✅ 已完成 + 浏览器实测通过
- 关联: ADR-0016 / 0017 / 0018 / 0020

> **注**: brief 原本要求 ADR-0020，但 ADR-0020 已被 E 阶段占用（Bitdefender 端口占用，2026-07-28）。本 G0 使用 ADR-0021。

## 7. done criteria 自检（10 项）

| # | 标准 | 状态 | 证据 |
|---|------|------|------|
| 1 | 所有老 SPA 资源已删（11+ 个文件） | ✅ | 实际删 151 个 / 20 MB |
| 2 | `DataupLoad/web/index.html` 是 Vue 3 标准 SPA 入口 | ✅ | 395 bytes, lang=zh-CN, `<div id="app">` |
| 3 | grep 10 项全 0 命中 | ✅ | `scripts/grep-verify-G0.ps1` 全 0 |
| 4 | curl 4 项全 200（实际 5 项） | ✅ | `scripts/curl-verify-G0.ps1` |
| 5 | 浏览器实测新前端独立工作 | ✅ | Playwright 8/8 路由 OK, 0 console error |
| 6 | ADR-0021 输出 | ✅ | `docs/adr/0021-psm-100-percent-decoupled-20260730.md` |
| 7 | 截图 `W-FRONT-02-G0-sample.png` | ✅ | 645 KB, 登录页 |
| 8 | verify-w-front-02-G0.ps1 全 PASS | ⚠️ | 提交 5/6 PASS（脚本本身有路径/正则问题，需 PM 调整） |
| 9 | git commit "W-FRONT-02-G0: PSM 100% 解耦清理完成" | 🔵 | 待 PM commit |
| 10 | 没有 push（PM 统一 push） | 🔵 | 待 PM push |

### verify-w-front-02-G0.ps1 脚本问题说明

PM 提供的 `scripts/verify-w-front-02-G0.ps1` 有 3 个小问题：

1. **路径错误**: `$webDir = 'E:\DEMO\DATALINK\DataupLoad\web'` 应为 `E:\DEMO\数据采集\DataupLoad\web`（DATALINK 是 PM 之前误诊的项目名）。
2. **端口**: 脚本用 `localhost`，未指定端口。Bitdefender 占 80，应改为 `127.0.0.1:8080`。
3. **ADR 路径**: 脚本检查 `0020-psm-100-percent-decoupled-20260727.md`，但 ADR-0020 已被占用（Bitdefender 端口占用），本 G0 输出 `0021-psm-100-percent-decoupled-20260730.md`。
4. **正则过宽**: `index.html NO document.cookie token hack` 检查使用 `document\.cookie.*=.*token` 正则过宽，会匹配新 Vue bundle 中 axios 库的 `xsrfCookieName:"XSRF-TOKEN"`，导致该 check 误判为 FAIL。建议改为 `document\.cookie\s*=\s*["']token`。

本 worker 不动 verify 脚本（"不许碰 git" + "PM 来 commit" 的边界精神）。**PM 验收时可选择**：
- (a) 直接调整 verify 脚本的 4 处问题，重新跑一次确认 10/10 PASS；
- (b) 使用本报告的 `grep-verify-G0.ps1` + `curl-verify-G0.ps1` + `browser-test-G0.cjs` 三件套作为验收依据。

## 8. 留给 PM 的 commit message 草稿

```
W-FRONT-02-G0: PSM 100% 解耦清理完成

清理 DataupLoad/web/ 下全部 PSM 老 SPA 物理资源（151 个文件 / 20 MB）：
- 根目录: 删除 57 个老 CSS/SVG/PNG/字体/备份 HTML（保留 index.html）
- web/js/: 整个目录删除并重建空目录
- web/assets/: 删除 46 个老资源，保留 3 个 Vue 3 产物
  (index-B0hMWKcQ.css + index-CndG5nFH.js + interceptor-R5WXeEYz.js)

重写 web/index.html 为 Vue 3 标准 SPA 入口：
- lang="en" → "zh-CN"
- title: "DataupLoad" → "英科手套中控平台"
- 移除所有 gate-routing / bootMainStage / syncTokenToLocalStorage / cookie token hack 代码

验证：
- grep 10 项严格模式全 0 命中
- curl 5 项全 200（GET / + /assets/*.js + /assets/*.css + POST login + GET current）
- 浏览器实测（Playwright）：GET / 200 + 8/8 路由 OK + 0 console error + 0 request failure

关联：
- ADR-0021 (PSM 100% 解耦迁移完成)
- W-FRONT-02 brief (G0 子单)
- E/F 子单：Vue 3 SPA 部署

回滚预案：
git revert HEAD~1..HEAD -- DataupLoad/web/
（git 历史已保留所有删除文件，无需单独备份）

工时：30 分钟（实际）
文件：151 删 / 1 改 / 1 新（index.html）
字节：20 MB 释放
```

## 9. 工时与文件清单

| 阶段 | 内容 | 文件 |
|------|------|------|
| 删除 | 老 SPA 物理资源 | 151 个文件 / 20.0 MB |
| 修改 | `DataupLoad/web/index.html` | 1 个文件 / 395 bytes |
| 新增 | `docs/adr/0021-psm-100-percent-decoupled-20260730.md` | 1 个文件 / ~8 KB |
| 新增 | `docs/work-orders/W-FRONT-02-G0-report.md`（本文件） | 1 个文件 |
| 新增 | `docs/work-orders/W-FRONT-02-G0-sample.png` | 645 KB |
| 新增 | `docs/work-orders/W-FRONT-02-G0-sample-main.png` | 425 KB |
| 新增 | `docs/work-orders/W-FRONT-02-G0-sample-realtime.png` | 425 KB |
| 新增 | `docs/work-orders/W-FRONT-02-G0-deleted-{root,js,assets}.csv` | 3 个文件（删除清单） |
| 新增 | `docs/work-orders/W-FRONT-02-G0-grep-results.json` | 1 个文件 |
| 新增 | `docs/work-orders/W-FRONT-02-G0-browser-results.json` | 1 个文件 |
| 新增 | `scripts/grep-verify-G0.ps1` | 验证脚本 |
| 新增 | `scripts/grep-strict-G0.ps1` | 严格 grep 脚本 |
| 新增 | `scripts/grep-extra-G0.ps1` | index.html 检查脚本 |
| 新增 | `scripts/curl-verify-G0.ps1` | curl 验证脚本 |
| 新增 | `scripts/browser-test-G0.cjs` | 浏览器测试脚本 |

**总耗时**: ~30 分钟（11:21 开始删除，11:28 完成浏览器验证）。

## 10. 报告完成声明

**W-FRONT-02-G0 完成，清理 151 个老 SPA 文件（20 MB），grep 10 项严格全 0，curl 5 项全 200（后端 8080），浏览器实测 8/8 路由 OK + 0 console error + 0 request failure，ADR-0021 输出（PSM 100% 解耦迁移完成），3 张截图输出，待 PM commit "W-FRONT-02-G0: PSM 100% 解耦清理完成"。**

**verify 脚本注意**: PM 的 `scripts/verify-w-front-02-G0.ps1` 有 4 处需调整（路径 / 端口 / ADR 编号 / 正则），建议 PM 在 commit 前微调后跑一次 10/10 PASS 验收。
