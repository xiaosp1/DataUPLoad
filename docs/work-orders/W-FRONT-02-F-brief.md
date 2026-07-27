# W-FRONT-02-F brief — 打包 + 部署 + 老 SPA 退出

- **任务**: `vite build` 产物 → 部署到后端 → 验证 SPA 入口
- **依赖**: W-FRONT-02-D 完成（主框架就绪）
- **耗时上限**: 1.5 小时

## 关键产出

### 1. `vite.config.js` 修改（**允许**这次改）
- `base: '/'`
- `build.outDir: 'dist'`
- `build.emptyOutDir: true`

### 2. `dist/` 目录
- `dist/index.html` — Vue 3 SPA 入口
- `dist/assets/*.js` / `*.css`

### 3. 部署步骤
1. `npm run build` → 生成 `DataupLoad-web/dist/`
2. 拷贝：`Copy-Item dist\* E:\DEMO\DATALINK\DataupLoad\web\ -Recurse -Force`
   （注意：实际是 `DataupLoad/web/`，不是 `DataupLoad-web/web/`，路径以 PM 现场为准）
3. **不动老的 `web/index.html` gate-routing**（F 子单不做清理，留给 G0）
4. **临时改 `web/index.html`**：把入口从 gate-routing 改成 Vue 3 SPA（即只这一行替换，其他不动）
5. 重启后端服务（如需要，验证服务是否需要刷静态资源缓存）

### 4. 验证清单
- [ ] `GET /` 返回新 `index.html`（用 curl）
- [ ] `<title>` 是新前端的（不是 PSM 老 SPA 的）
- [ ] 浏览器实测 `http://localhost/` 看到 Vue 3 玻璃登录页
- [ ] 登录 super_admin/Abc12345 → 进入主界面
- [ ] 8 大业务页 stub 都能进

### 5. 回滚预案
- `web/index.psm-legacy.html` 备份存在
- `web/index.html` 改动记录在 `docs/work-orders/W-FRONT-02-F-report.md`

## done criteria（10 项）

- [ ] `npm run build` 成功
- [ ] `dist/index.html` 存在且 `<div id="app">` 在
- [ ] `dist/assets/*.js` 至少 1 个 chunk
- [ ] `dist/` 拷到 `web/` 完成
- [ ] `GET /` 返回新前端 index.html（curl 200 + size 变化）
- [ ] 浏览器 `http://localhost/` 显示玻璃登录页
- [ ] 登录 super_admin/Abc12345 成功
- [ ] 主界面 8 路由可点
- [ ] verify-w-front-02-F.ps1 全 PASS
- [ ] 截图 `docs/work-orders/W-FRONT-02-F-sample.png`

## PM 验收

```powershell
pwsh -NoProfile -File scripts/verify-w-front-02-F.ps1
```

10/10 PASS。

## 禁止

- 不许删除任何老 SPA 资源（G0 子单）
- 不许改 vite.config.js 的 dev server 配置（prod build 才动）

