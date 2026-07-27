# W-FRONT-02-G0 brief — PSM 彻底解耦清理

- **任务**: 清理所有 PSM 老 SPA 残留 + gate-routing hack 代码 + 老资源文件 + 老 SPA 编译产物
- **依赖**: W-FRONT-02-F 完成（新前端部署验证通过）
- **耗时上限**: 1 小时

## 关键产出（清理动作清单）

### 1. 删老 SPA 资源（`DataupLoad/web/`）
- [ ] `DataupLoad/web/index.psm-legacy.html`（回滚备份，git 历史有）
- [ ] `DataupLoad/web/js/index.f19ecd42-*.js`
- [ ] `DataupLoad/web/js/index-legacy.0208e821-*.js`
- [ ] `DataupLoad/web/js/polyfills-legacy.*.js`
- [ ] `DataupLoad/web/js/browser.js`（已 hack 进 /js/ 的老 PSM 资源）
- [ ] `DataupLoad/web/js/AI.png`
- [ ] `DataupLoad/web/js/vendor.89afe428-*.js`
- [ ] `DataupLoad/web/js/vendor-legacy.cd362f1d-*.js`
- [ ] `DataupLoad/web/assets/*.css`（所有老 SPA 的 css，包括 vendor-6d15dd8f.css / index-ff4f195f.css 等）
- [ ] `DataupLoad/web/login.html`（如还有）
- [ ] `DataupLoad/web/login.js`（如还有）
- [ ] `DataupLoad/web/test-marker-12345.html`（如还有）

### 2. 重写 `DataupLoad/web/index.html`
**新版** = Vue 3 标准 SPA 入口：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>英科手套中控平台</title>
  <link rel="stylesheet" href="/assets/index-XXXXX.css">
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/assets/index-XXXXX.js"></script>
</body>
</html>
```

**禁止**：
- ❌ SHA256 登录表单（gate-routing 那段）
- ❌ `bootMainStage()` 动态注入老 SPA script
- ❌ `syncTokenToLocalStorage()` 复制 satoken → token
- ❌ `document.cookie = "token="` 任何 hack 代码
- ❌ 任何老 PSM chunk 的引用

### 3. 验证清理彻底（grep 全 0）
- [ ] `grep -r 'index\.f19ecd42' DataupLoad/web/` ← 0
- [ ] `grep -r 'index-legacy' DataupLoad/web/` ← 0
- [ ] `grep -r 'vendor\.89afe428' DataupLoad/web/` ← 0
- [ ] `grep -r 'polyfills-legacy' DataupLoad/web/` ← 0
- [ ] `grep -r 'browser\.js' DataupLoad/web/` ← 0
- [ ] `grep -r 'AI\.png' DataupLoad/web/` ← 0
- [ ] `grep -r 'satoken.*token.*cookie' DataupLoad/web/` ← 0
- [ ] `grep -r 'document\.cookie.*=.*token' DataupLoad/web/` ← 0
- [ ] `grep -r 'syncTokenToLocalStorage' DataupLoad/web/` ← 0
- [ ] `grep -r 'bootMainStage' DataupLoad/web/` ← 0

### 4. 验证新前端独立工作（curl）
- [ ] `GET /` 返回新 Vue 3 SPA index.html（200 + 短小）
- [ ] `GET /assets/index-XXXXX.js` 返回新前端 JS（200）
- [ ] `POST /web/auth/login` super_admin/Abc12345 → satoken cookie 写入
- [ ] `GET /web/account/current` 带 satoken → 200
- [ ] 浏览器 `http://localhost/` 显示新玻璃登录页（不是老 gate-routing）

### 5. ADR 输出
- `docs/adr/0020-psm-100-percent-decoupled-20260727.md`
  - 描述清理范围
  - 列删除文件清单
  - 列 grep 验证 0 结果
  - 列最终架构（前端 Vue 3 SPA + 后端不变）

## done criteria（10 项）

- [ ] 所有老 SPA 资源已删（11+ 个文件）
- [ ] `DataupLoad/web/index.html` 是 Vue 3 标准 SPA 入口
- [ ] grep 10 项全 0 命中
- [ ] curl 4 项全 200
- [ ] 浏览器实测新前端独立工作
- [ ] ADR-0020 输出
- [ ] 截图 `docs/work-orders/W-FRONT-02-G0-sample.png` 新前端
- [ ] verify-w-front-02-G0.ps1 全 PASS
- [ ] git commit "W-FRONT-02-G0: PSM 100% 解耦清理完成"
- [ ] **没有 push**（PM 统一 push）

## PM 验收

```powershell
pwsh -NoProfile -File scripts/verify-w-front-02-G0.ps1
```

10/10 PASS。

## 禁止

- 不许碰 git 历史（git push --force 等）
- 不许碰后端 Java 代码
- 不许删 `backups/2026-07-27-cleanup/`（已 gitignore）

