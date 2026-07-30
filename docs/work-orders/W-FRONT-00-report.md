# W-FRONT-00 Report — 老前端 bundle 登录 502/Whitelabel 排查（PM 误诊修正 + 老板 Console 诊断）

> **状态**: 🟡 进行中 — 已出诊断脚本 + ADR-0017 占位，等老板跑 Console 后回填根因
> **派工时间**: 2026-07-27 11:09
> **派单人**: 锋卫（PM）
> **执行者**: Worker（codex exec 子进程）
> **验收**: 老板浏览器实测 + PM 复核

---

## 0. 重要更正（PM 自查结论）

PM 之前认为"前端 bundle 没有 `/web/auth/login` 调用"——**错的**。

PM 11:08 自查 bundle 实际内容（节选）：

| 文件 | 真实内容 |
|---|---|
| `index.f19ecd42-20260520160358.js` L76900 | `const S="/web/"; const Xa={login:${S}auth/login,...}` ✅ |
| `index.ad4e4d09-20260520160358.js` Login chunk `function k()` | `const e={username, password:N.sha256(u.password)}; R.post(F.login, e);` ✅ |
| `vendor.89afe428-20260520160358.js` | 5.3MB 第三方库（vue / element-plus / axios）✅ |

**结论**：前端代码 100% 正确，不需要注入 `/web/auth/login` 调用。问题在**调用时**——baseURL / CORS / cookie SameSite / Pinia 时序等任一个都可能。

详细 ADR 见 `docs/adr/0017-frontend-bundle-pm-misdiagnosis-20260727.md`。

---

## A. 浏览器 Console 诊断脚本（老板跑）⏵️ 待执行

> **复制下面整段 → 老板浏览器 F12 Console → 粘贴 → 回车 → 把 console 输出整段截图或粘贴回 PM**

```javascript
// ============ W-FRONT-00 诊断脚本 v1 (2026-07-27 11:09) ============
// 老板请：F12 → Console → 粘贴以下整段 → 回车 → 把输出全文回贴给 PM

// 1. 检查 $api 是否注入 + login 路径
console.log("[1] F.login =", window.__VUE_APP__?.config?.globalProperties?.$api?.login);

// 2. 检查 axios baseURL
console.log("[2] $http baseURL =", window.__VUE_APP__?.config?.globalProperties?.$http?.defaults?.baseURL);

// 3. 直接试一次登录，看完整错误（绕过 axios，绕过 $api）
(async () => {
  const enc = await crypto.subtle.digest("SHA-256", new TextEncoder().encode("Abc12345"));
  const sha = Array.from(new Uint8Array(enc)).map(b=>b.toString(16).padStart(2,"0")).join("");
  console.log("[3] sha256(Abc12345) =", sha);
  try {
    const r = await fetch("/web/auth/login", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      credentials: "include",
      body: JSON.stringify({username: "super_admin", password: sha})
    });
    console.log("[4] status:", r.status);
    console.log("[5] headers:", Object.fromEntries(r.headers.entries()));
    console.log("[6] body:", await r.text());
  } catch(e) {
    console.error("[7] fetch err:", e);
  }
})();
```

**老板回填格式建议**（方便 PM 解析）：

```
[1] F.login = ...
[2] $http baseURL = ...
[3] sha256(Abc12345) = ...
[4] status: ...
[5] headers: ...
[6] body: ...
[7] fetch err: ... (如有)
```

**预期对照表**（老板 console 跑完比对这张表）：

| 输出 | 含义 | 下一动作（PM 收到后） |
|---|---|---|
| `[1] = "/web/auth/login"` + `[2] = ""` 或 `/web` + `[4] = 200` + body 有 data | **前端链路 100% 正常** | 真实根因是老板浏览器本地状态问题（缓存/cookie/CORS），走 F 节 |
| `[1] = undefined` | **$api 没注入**（最可能：bundle 没加载或 globalProperties 注册失败） | 检查 bundle 加载顺序、main.js 是否调 `app.config.globalProperties.$api = et` |
| `[2] = "http://wrong-host"` 或类似 | **axios baseURL 配错** | 改 `index.f19ecd42` 里 axios 创建处 baseURL |
| `[4] = 0` + `[7] = TypeError: Failed to fetch` | **CORS 拒绝**（localhost → 127.0.0.1 算跨域） | 改 `application-prod.yml` 加 `hik.cors.*` 白名单 |
| `[4] = 405` | **老板直接 GET /web/auth/login 了**（浏览器输入 URL） | 老板改输入 `/`（SPA 入口），不是 `/web/auth/login` |
| `[4] = 401` + body `{"code":401,...}` | **密码错了**（super_admin 密码不是 Abc12345 了） | 走 W-AUTH-02 重置密码流程（GenHash("Abc12345") + DB UPDATE） |
| `[4] = 500` + body i18n 错误 | **业务跑通但 i18n bundle 找不到** | 已知问题（见 W-AUTH-02 第 6 节 P3 待办），走 LocaleUtil fallback patch |
| `[4] = 200` 但 `[5]` 没 Set-Cookie | **sa-token cookie 没落盘**（SameSite 问题） | sa-token 配置加 `setCookieSameSite=Lax` |

---

## B. 实际根因（占位 — 等老板 Console 输出后回填）⏸️ 待回填

> **本节等老板跑完 §A 脚本后由 Worker 回填。预设模板：**

**根因**：<待填>

**证据链**：
- `[1]` 输出 = <待填>
- `[2]` 输出 = <待填>
- `[4]` status = <待填>
- `[6]` body = <待填>

**与 §A 预期对照表中的哪一行匹配**：<待填>

---

## C. 修复方案（占位 — 等老板 Console 输出后回填）⏸️ 待回填

> **最小化补丁原则：不预先改代码。根因确定后再改。**

| 根因 | Patch 文件 | 改动 |
|---|---|---|
| baseURL 配错 | `index.f19ecd42-20260520160358.js`（axios 创建处） | 改 `baseURL: ""` 或 `baseURL: "/"` |
| CORS 拒绝 | `application-prod.yml` | 加 `hik.cors.allowed-origins: "*"` 或具体白名单 |
| cookie SameSite | sa-token config（`application-prod.yml`） | `setCookieSameSite=Lax` |
| 老板浏览器缓存 | 无（运维指令） | F12 → Network → Disable cache → Ctrl+Shift+R |
| $api 没注入 | `index.f19ecd42`（main.js 入口） | 检查 `app.config.globalProperties.$api = et` 是否执行 |
| 路由跳组件失败 | `index.ad4e4d09` (Login chunk) | 检查 `W.push("/")` 后组件挂载 |

详细 patch 待 §A 输出后给出。

---

## D. 验证清单（占位 — 等修复后回填）⏸️ 待回填

- [ ] 老板浏览器 hard refresh (Ctrl+Shift+R) + Disable cache
- [ ] `GET /` → 200 + `index.html`（SPA 入口）
- [ ] 输入 `/` → 自动跳 `/Login` 路由 → Element Plus 表单渲染
- [ ] 输入 `super_admin / Abc12345` → POST `/web/auth/login` → 200 → 跳 `/`（realTime）
- [ ] `GET /web/account/current` 带 cookie → 200，返回 super_admin 信息
- [ ] F12 Application → Cookies 看到 `satoken=xxx`（SameSite=Lax）
- [ ] F12 Network 看不到 CORS 报错（OPTIONS 预检 200）

---

## E. 关联文档

- **Brief**: `docs/work-orders/W-FRONT-00-brief.md`
- **ADR-0017**: `docs/adr/0017-frontend-bundle-pm-misdiagnosis-20260727.md`（PM 误诊修正）
- **ADR-0016**: `docs/adr/0016-frontend-align-psm-spa-20260725.md`（前端架构对齐 PSM）
- **W-AUTH-02**: `docs/work-orders/W-AUTH-02-report.md`（super_admin 密码重置）
- **W-FRONT-01 子单**: A/B/C/D/E（前端工程脚手架 → 长期解，本工单是临时止血）

---

## F. 时间戳

- **派单**: 2026-07-27 03:14:14
- **诊断脚本交付**: 2026-07-27 03:14:14
- **ADR-0017 占位交付**: 2026-07-27 03:14:14
- **真实根因回填**: <待填>
- **修复 patch 交付**: <待填>
- **老板浏览器验收**: <待填>
