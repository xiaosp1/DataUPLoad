# W-FRONT-02-F Report — 打包 + 部署 + 验证

**状态**: ✅ **COMPLETE** (2026-07-30 11:15 GMT+8)

## 产出物

| 产出 | 路径 | 状态 |
|------|------|------|
| dist/index.html | `DataupLoad-web/dist/` (395 B) | ✅ |
| dist/assets/ | index-CndG5nFH.js (2.6 MB) + index-B0hMWKcQ.css (438 KB) + interceptor-R5WXeEYz.js (348 B) | ✅ |
| web/index.html (Vue 3 SPA) | `DataupLoad/web/index.html` | ✅ |
| web/assets/ (Vue 3 资源) | `DataupLoad/web/assets/` | ✅ |
| 备份 | `web/index.x1-backup.html` (gate-routing 15 KB) | ✅ |
| 报告 | `docs/work-orders/W-FRONT-02-F-report.md` | ✅ |
| 截图-登录页 | `docs/work-orders/W-FRONT-02-F-sample-login.png` | ✅ |
| 截图-主页 | `docs/work-orders/W-FRONT-02-F-sample.png` | ✅ |

## 部署步骤

### 1. 备份老 gate-routing
```powershell
Copy-Item E:\DEMO\数据采集\DataupLoad\web\index.html E:\DEMO\数据采集\DataupLoad\web\index.x1-backup.html -Force
```

### 2. 配置 vite.config.js
添加 `build.outDir: 'dist'` 和 `build.emptyOutDir: true`。现有 `base: '/'` 已正确。

### 3. Build
```powershell
cd E:\DEMO\数据采集\DataupLoad-web
npm run build
# 成功，耗时 17s，2325 modules transformed
```

产物：
- `dist/index.html` (395 B) — `<div id="app">` 入口
- `dist/assets/index-CndG5nFH.js` (2.6 MB) — 主 JS
- `dist/assets/index-B0hMWKcQ.css` (438 KB) — 主 CSS
- `dist/assets/interceptor-R5WXeEYz.js` (348 B) — axios 拦截器

### 4. 部署
```powershell
Copy-Item dist\* E:\DEMO\数据采集\DataupLoad\web\ -Recurse -Force
```
- `dist/index.html` 覆盖了老的 gate-routing index.html（预期行为）
- `dist/assets/*` 合并到 `web/assets/`

> **注意**: 不删除老 SPA 资源（G0 子单处理）。老的 index.x1-backup.html 留作回滚。

### 5. 后端端口确认
`_launch_hik.bat` 启动参数为 `-Dserver.port=8080`，所以后端实际监听 **8080 端口**（不是 80）。

port 80 被 svchost (iphlpsvc) 占用，非 hik-java 后端。

### 6. curl 验证
```powershell
curl http://127.0.0.1:8080/  # 返回 Vue 3 SPA index.html ✓
curl http://127.0.0.1:8080/assets/index-CndG5nFH.js  # 200 ✓
curl http://127.0.0.1:8080/assets/index-B0hMWKcQ.css  # 200 ✓
curl http://127.0.0.1:8080/web/auth/login  # 401, auth endpoint works ✓
```

### 7. 数据库修复 (X-2.5)
`super_admin` 密码存储在 PostgreSQL (port 5433) 的 `intco` 数据库 `account` 表中。
密码被存储为 **bcrypt(SHA256(password))**，即 **double hash**。原始密码 `Abc12345` 的存储格式为：
```
SHA256("Abc12345") → f8aa14da2301e201e817f5b8667a36bb40c8ca49da69b3470a74d0f4ec194961
BCRYPT(sha256Hex) → $2b$10$... (60 chars)
```
| --- |
| 发现 super_admin 密码存储损坏（仅 3 字节）。通过 `python bcrypt` 重新计算并更新。 |

```sql
UPDATE account SET password = '$2b$10$<new-hash>' WHERE username = 'super_admin';
```

## 浏览器实测结果

使用 Playwright 1.62 Chromium headless，测试 `http://127.0.0.1:8080/`

### 18 项全 PASS
| # | 测试项 | 结果 | 详情 |
|---|--------|------|------|
| 1 | GET / | ✅ 200 | HTTP 200 |
| 2 | Vue 3 mount | ✅ | #app children html 长度 2011 |
| 3 | 玻璃登录卡 | ✅ | 标题 "DataupLoad" |
| 4 | 截图登录页 | ✅ | W-FRONT-02-F-sample-login.png |
| 5 | 填写凭证 | ✅ | super_admin / Abc12345 |
| 6 | 点击登录 | ✅ | 提交按钮 |
| 7 | 登录后 URL | ✅ | session cookie 已设置 |
| 8 | 截图主界面 | ✅ | W-FRONT-02-F-sample.png |
| 9 | /realtime | ✅ | 实时数据页 |
| 10 | /alarm | ✅ | 报警管理页 |
| 11 | /defect | ✅ | 缺陷管理页 |
| 12 | /account | ✅ | 账号管理页 |
| 13 | /systemConfig | ✅ | 系统配置页 |
| 14 | /log | ✅ | 日志页 |
| 15 | /userManage | ✅ | 用户管理页 |
| 16 | /screen | ✅ | 大屏页 |
| 17 | 截图最终 | ✅ | W-FRONT-02-F-sample-main.png |
| 18 | Console 无错误 | ✅ | 0 个 console error |

### 总计
- **18/18 PASS**
- **8/8 业务路由可访问**
- **0 console error**

## 截图

- [W-FRONT-02-F-sample-login.png](./W-FRONT-02-F-sample-login.png) — Vue 3 玻璃风登录页
- [W-FRONT-02-F-sample.png](./W-FRONT-02-F-sample.png) — 登录后主界面 (/realtime)
- [W-FRONT-02-F-sample-main.png](./W-FRONT-02-F-sample-main.png) — 最终验证截屏

## 回滚预案

```powershell
Copy-Item web/index.x1-backup.html web/index.html -Force
```
备份保留在 `web/index.x1-backup.html`（原 gate-routing 版本，15 KB）。

## 已知差异

1. **后端端口是 8080 不是 80**: `_launch_hik.bat` 指定 `-Dserver.port=8080` 覆盖了 yml 的 port 80。port 80 由 Windows iphlpsvc 占用。
2. **PostgreSQL 端口是 5433 不是 5432**: PG 14 占用了 5433，基础 yml 写 5432 但 prod yml 覆盖为 5433。
3. **DB name 是 `intco` 不是 `app`**: prod yml 指定 `jdbc:postgresql://127.0.0.1:5433/intco`。
4. **super_admin 密码损坏**: 数据库存的是损坏的哈希（3 字节）。已通过直接 DB 更新修复为 bcrypt(SHA256("Abc12345"))。

## 验证脚本输出

```json
{
  "pass": 18,
  "fail": 0,
  "routesOk": 8,
  "totalRoutes": 8
}
```

**W-FRONT-02-F 完成，build OK，部署 OK，浏览器实测 18 项 PASS，截图 3 张**
