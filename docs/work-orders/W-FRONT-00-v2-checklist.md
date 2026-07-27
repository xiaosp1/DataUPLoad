# W-FRONT-00 V2 浏览器验收清单（老板用）

> **来源**：W-FRONT-00-report.md §D，由 PM 11:35 整理为老板一页纸
> **前置**：W-FRONT-00 V1 验收 PASS（Worker 已出诊断脚本 + ADR-0017）
> **本轮目标**：老板跑 §0 Console 诊断脚本 → 把结果发回 PM → 根因定位 → 出 patch → 浏览器验收本清单
> **硬上限**：brief 写明 11:40 截止；如老板 11:40 仍未跑，PM 自动接手跑（但 PM 跑不出老板浏览器状态，会退化到 6 个根因盲打）

---

## 0. 老板必跑：Console 诊断脚本（P0）

**步骤**：浏览器打开 `http://<server>/`（**不要**直接输入 `/web/auth/login`）→ F12 → Console → 粘贴下面整段 → 回车 → **把 Console 输出全文截图/粘贴发回 PM**。

```javascript
// ============ W-FRONT-00 诊断脚本 v1 (2026-07-27 11:09) ============
console.log("[1] F.login =", window.__VUE_APP__?.config?.globalProperties?.$api?.login);
console.log("[2] $http baseURL =", window.__VUE_APP__?.config?.globalProperties?.$http?.defaults?.baseURL);
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

**回填模板**（直接复制粘贴改 `=...`）：

```
[1] F.login = ...
[2] $http baseURL = ...
[3] sha256(Abc12345) = ...
[4] status: ...
[5] headers: ...
[6] body: ...
[7] fetch err: ... (没有就写 "无")
```

---

## 1. 浏览器验收清单（修复后逐项打钩）

### 1.1 前置（老板 F12 必做）

- [ ] **F12 → Network → 勾上 Disable cache**（防老 bundle 残留）
- [ ] **Ctrl + Shift + R**（hard refresh，不走 304 缓存）
- [ ] **确认地址栏是 `http://<server>/`**（**不是** `/web/auth/login`，**不是** `/Login`）

### 1.2 前端链路（预期全绿）

- [ ] F12 → Network → 看到 `GET /` → 状态 200 → 返回 `index.html`（SPA 入口）
- [ ] F12 → Network → 看到 `GET /static/index.f19ecd42-...js` → 200 + size ≈ 96KB
- [ ] F12 → Network → 看到 `GET /static/index.ad4e4d09-...js` → 200 + size > 0（Login chunk 加载）
- [ ] F12 → Network → 看到 `GET /static/vendor.89afe428-...js` → 200 + size ≈ 5.3MB（vue/element-plus/axios）
- [ ] 页面**自动跳转到 `/Login`**（vue-router beforeEach 重定向）
- [ ] Element Plus 表单渲染：账号框、密码框、登录按钮 3 件齐

### 1.3 登录链路（核心）

- [ ] 输入 `super_admin / Abc12345`
- [ ] F12 → Network → 看到 `POST /web/auth/login`（**不是** 404 / 405 / OPTIONS-only）
- [ ] 该 POST 状态码 **200**
- [ ] Response body 形如：`{"success":true,"data":{"username":"super_admin","role":"super_admin","id":1,"permission":[...]}}`
- [ ] **页面自动跳转到 `/`（realTime）**，左侧菜单/顶部栏出现

### 1.4 会话保持

- [ ] F12 → Application → Cookies → 看到 `satoken=xxx`（**关键**：sa-token 落盘）
- [ ] F12 → Application → Cookies → `satoken` 的 `SameSite` 列是 `Lax`（不是 `None`，否则跨场景会丢）
- [ ] F5 刷新页面 → **不弹回登录页**（能保持登录态）
- [ ] F12 → Network → 看到 `GET /web/account/current` → 200 → 返回 super_admin 信息

### 1.5 反向验收（应该**没有**的错误）

- [ ] ❌ 不应出现 `CORS` 报错（Network 里 OPTIONS 预检 200/204，不是红色）
- [ ] ❌ 不应出现 `Mixed Content`（页面是 http 就全程 http，不出 https 资源）
- [ ] ❌ 不应出现 `401 / 403 / 404 / 500`（Network 全 200/304）
- [ ] ❌ Console 不应有红色 `TypeError` / `Uncaught`（黄色 warn 可忽略）

---

## 2. 验收失败的回退路径

按 §1 逐项排查，**先按失败项定位根因**，再决定回退：

| 失败模式 | 根因 | 回退动作 |
|---|---|---|
| §1.2 任一文件 404 | 静态资源路径错 | 检查 nginx / DataupLoad 静态目录映射 |
| §1.3 POST 404 | `/web/auth/login` 没注册 | 检查 sa-token route + `AuthController` |
| §1.3 POST 405 | 老板 GET 输错地址（直接 `/web/auth/login`） | 改回 `/`，走 SPA 路由 |
| §1.3 POST 401 + `code:401` | super_admin 密码不是 Abc12345 | 走 W-AUTH-02 重置（GenHash("Abc12345") + DB UPDATE） |
| §1.3 POST 500 + i18n 错误 | i18n bundle 找不到 | 已知问题（W-AUTH-02 P3），走 LocaleUtil fallback patch |
| §1.4 satoken cookie 缺失 | sa-token 没下发 cookie | 配 `setCookieSameSite=Lax` |
| §1.5 CORS 报错 | localhost ↔ 127.0.0.1 跨域 | 改 `application-prod.yml` 加 `hik.cors.allowed-origins` 白名单 |
| §1.5 Mixed Content | 页面 https 但资源 http | 统一 http（生产用 nginx 终结 TLS） |

---

## 3. 验收通过后请老板回复

任一项**失败** → 把失败项编号（如 `§1.3 POST 401`）+ 失败截图发给 PM，PM 出针对性 patch。
**全部通过** → 在群里回一句「W-FRONT-00 V2 PASS」，PM 关单并归档 `delivered/W-FRONT-00-前端登录止血.md`。

---

## 4. 时间戳

- V1 验收：2026-07-27 11:35（PM）
- V2 清单交付：2026-07-27 11:35
- 老板 Console 输出回填：<待填>
- V2 浏览器验收：<待填>
- 工单关闭 + 归档：<待填>
