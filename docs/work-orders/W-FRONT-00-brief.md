# W-FRONT-00-brief — 老前端 bundle 登录排查（第三次派工，PM 已自助定位）

> **优先级**: P0（老板浏览器现在登不进去，业务停摆）
> **派单时间**: 2026-07-27 11:09（PM 自助定位完成，第三次派工让 Worker 修）
> **派单人**: 锋卫（PM）
> **执行者**: Worker（codex exec 子进程）
> **验收**: 锋卫（PM）+ 老板浏览器实测

---

## ⚠️ 重大更正：PM 之前的诊断**全错**

PM 之前认为"前端 bundle 没有 `/web/auth/login` 调用"——**错的**。

实际情况（PM 11:08 自查确认）：

| 文件 | 真实内容 |
|---|---|
| `index.f19ecd42-20260520160358.js` | **定义 `const S="/web/"; const Xa={login:`${S}auth/login`,...}`** |
| `index.ad4e4d09-20260520160358.js` | **登录 chunk，含 `function k(){ const e={username, password:N.sha256(pwd)}; R.post(F.login, e); }`** |
| `vendor.89afe428-20260520160358.js` | 5.3MB 第三方库（vue / element-plus / axios），**不包含业务 API 路径** |

**所以"前端没调登录接口"的根因诊断是错的。PM 之前的 brief 让 worker 干"注入 /web/auth/login 调用"——根本不需要注入，调用已经在了。**

## 1. PM 11:08 完整定位

### 1.1 API 路径定义（`index.f19ecd42` L76900-77975）

```js
const S = "/web/"
const Qa = {
  getRoleList: `${S}account/role/list`,
  resetPwd: `${S}account/pwd-reset`,
  getUser: `${S}account/list`,
  userEdit: `${S}account`,
  getCurrentUserInfo: `${S}account/current`,
  ...
}
const Xa = {
  login: `${S}auth/login`,           // ← /web/auth/login ✓
  logout: `${S}auth/logout`,
  modifyPwd: `${S}account/pwd`,
  getPwdSerial: `${S}account/serial-no`,
  serialCheck: `${S}account/verify-no`,
  restAdminPwd: `${S}account/pwd/admin`
}
const et = { ...Qa, ...Xa }            // ← 合并后导出，$api 注入 Vue globalProperties
```

### 1.2 登录调用（`index.ad4e4d09` Login chunk `function k`）

```js
function k(){
  if(c.value) return !1;
  c.value = !0;
  I.value.validate(async o => {
    if(!o) { c.value=!1; return }
    try {
      m.value = !0;
      const e = { username: u.username, password: N.sha256(u.password) };  // ← SHA256 ✓
      const s = await R.post(F.login, e);                                   // ← POST /web/auth/login ✓
      if(s.success) {
        if(s.message && U.changeOpenPwd) U.changeOpenPwd(!0);
        U.changeUser({
          username: s.data.username,
          role: s.data.role,
          id: s.data.id,
          permission: s.data.permission
        });
        W.push("/");                                                          // ← 跳 / 路由（realTime）✓
      } else {
        g({ message: s.message || a("login.messages.loginFailed"), type:"error" });
      }
      c.value = !1;
    } catch(e) {
      console.log(e);
      g({ message: a("login.messages.loginFailed"), type:"error" });
      m.value = !1;
      c.value = !1;
    } finally {
      m.value = !1;
      c.value = !1;
    }
  })
}
```

**前端代码 100% 正确**：
- ✅ SHA256 密码（`N.sha256(u.password)`）
- ✅ POST `/web/auth/login`
- ✅ 成功后 push 业务路由
- ✅ 失败 toast

## 2. 真实根因（PM 推测，需要 Worker 验）

前端代码没问题，**问题在调用时**：

| 推测 | 验证方法 |
|---|---|
| **a) 老板浏览器没 hard refresh**，加载的还是更老的 bundle（早期版本可能没调接口） | 老板 F12 → Network → Disable cache + Ctrl+Shift+R |
| **b) `$api` 没注入**，`F` 是 undefined，`F.login` 抛 TypeError → catch 里打 console.log 不弹窗 | 老板 F12 → Console 看有没有 TypeError |
| **c) `R`（$http axios）baseURL 配错**，POST 到错误域名 | 老板 F12 → Network 看 POST 请求的 URL |
| **d) CORS 拒绝**（localhost → 127.0.0.1 算跨域） | 老板 F12 → Network 看 OPTIONS 预检 |
| **e) sa-token cookie 没落盘**（SameSite 问题） | 老板 F12 → Application → Cookies |
| **f) `U.changeUser` / `W.push` 在 Pinia store 未就绪时调用**，push 路由但组件渲染失败 | F12 Console + Vue devtools |

## 3. Worker 任务（改 brief，不要瞎注入！）

### T1 — 写一份诊断脚本给老板跑（10 min）

让老板在浏览器 Console 跑以下脚本，结果发回来：

```javascript
// 1. 检查 $api 是否注入
console.log('F.login =', window.__VUE_APP__?.config?.globalProperties?.$api?.login);
// 2. 检查 axios baseURL
console.log('$http baseURL =', window.__VUE_APP__?.config?.globalProperties?.$http?.defaults?.baseURL);
// 3. 直接试一次登录，看完整错误
(async () => {
  const enc = await crypto.subtle.digest("SHA-256", new TextEncoder().encode("Abc12345"));
  const sha = Array.from(new Uint8Array(enc)).map(b=>b.toString(16).padStart(2,"0")).join("");
  try {
    const r = await fetch("/web/auth/login", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      credentials: "include",
      body: JSON.stringify({username: "super_admin", password: sha})
    });
    console.log("status:", r.status);
    console.log("body:", await r.text());
  } catch(e) {
    console.error("fetch err:", e);
  }
})();
```

### T2 — 写 W-FRONT-00-report.md，分类说明（10 min）

报告分四节：
- **A. 实际根因**（基于老板 Console 输出）
- **B. 修复方案**（按根因分类给代码 patch）
- **C. 验证清单**
- **D. ADR-0017 引用**

### T3 — 按根因出 patch（10 min）

根据 T1 老板反馈的根因，做最小化补丁。**不要预先改代码！**

可能的 patch 方向（仅参考，按 T1 结果定）：

| 根因 | Patch |
|---|---|
| baseURL 配错 | 改 `index.f19ecd42` 里 `$http` 创建处的 baseURL |
| CORS 拒绝 | 改 `application-prod.yml` 加 `hik.cors.*` 白名单 |
| cookie SameSite | 改 sa-token 配置加 `setCookieSameSite=Lax` |
| 前端逻辑 bug | 改 `index.ad4e4d09` 的 `function k()` |

### T4 — 出 ADR-0017 + 文档（5 min）

`docs/adr/0017-frontend-bundle-actual-issue.md`：
- PM 之前误诊的事实
- 真实根因（以 T1 结果为准）
- 为什么"前端 bundle 没调登录接口"是错的诊断
- 修复方案 + 未来 SPA 重写路径

git commit: `W-FRONT-00: 登录真因定位与修复（不再注入）`

## 4. 交付物

1. **诊断脚本**（T1，老板跑）
2. **W-FRONT-00-report.md**（含分类说明）
3. **ADR-0017**（修正 PM 误诊）
4. **按根因出的 patch**（T3）
5. **git commit**
6. **HEARTBEAT.md 更新**

## 5. PM 行为边界（再次声明）

- ❌ PM **不**写代码 patch（让 Worker 干）
- ❌ PM **不**替老板跑浏览器 Console（老板跑）
- ✅ PM 跑 V1 grep + 老板反馈后给 V2 验证清单

## 6. 时间限制

11:10 起 30 分钟内交付，**最迟 11:40**。超时就 PM 自己干。
