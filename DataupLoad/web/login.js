/* 方案 X 最小登录脚本 — 不依赖任何外部库
   - SHA256 用 Web Crypto API（IE11 不支持，但 PSM 老前端也跑在现代浏览器上）
   - 用 fetch 调 /web/auth/login
   - 成功后把 satoken cookie 留着，2 秒跳 /index.html
*/
(function () {
  "use strict";

  function setLang(lang) {
    try { localStorage.setItem("lang", lang); } catch (e) {}
  }

  async function sha256Hex(text) {
    const buf = new TextEncoder().encode(text);
    const hash = await crypto.subtle.digest("SHA-256", buf);
    return Array.from(new Uint8Array(hash))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");
  }

  function setMsg(text, kind) {
    const el = document.getElementById("msg");
    el.textContent = text || "";
    el.className = "msg" + (kind ? " " + kind : "");
  }

  function setBusy(busy) {
    const btn = document.getElementById("submitBtn");
    btn.disabled = busy;
    btn.textContent = busy ? "登录中..." : "登录 / Login";
  }

  function getCookie(name) {
    const m = document.cookie.match(new RegExp("(?:^|;\\s*)" + name + "=([^;]+)"));
    return m ? decodeURIComponent(m[1]) : null;
  }

  async function doLogin(username, password) {
    const resp = await fetch("/web/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept-Language": (function () {
          try { return localStorage.getItem("lang") || "zh-CN"; } catch (e) { return "zh-CN"; }
        })(),
      },
      body: JSON.stringify({ username: username, password: await sha256Hex(password) }),
      credentials: "same-origin",
    });
    const txt = await resp.text();
    let json;
    try { json = JSON.parse(txt); } catch (e) { json = null; }
    if (!resp.ok) {
      throw new Error((json && json.message) || ("HTTP " + resp.status));
    }
    if (json && json.success === false) {
      throw new Error(json.message || "登录失败");
    }
    return { json: json, cookie: getCookie("satoken") };
  }

  async function verify() {
    const resp = await fetch("/web/account/current", {
      credentials: "same-origin",
    });
    if (!resp.ok) throw new Error("verify HTTP " + resp.status);
    const j = await resp.json();
    if (!j || !j.success) throw new Error((j && j.message) || "未登录");
    return j.data;
  }

  document.addEventListener("DOMContentLoaded", function () {
    // 已被 satoken 记住就直接跳主界面
    fetch("/web/account/current", { credentials: "same-origin" })
      .then(function (r) { return r.json(); })
      .then(function (j) {
        if (j && j.success && j.data) {
          window.location.replace("./legacy.html");
        }
      })
      .catch(function () {});

    document.getElementById("loginForm").addEventListener("submit", async function (ev) {
      ev.preventDefault();
      setMsg("", "");
      setBusy(true);
      try {
        const u = document.getElementById("username").value.trim();
        const p = document.getElementById("password").value;
        if (!u || !p) {
          setMsg("请输入用户名和密码", "error");
          setBusy(false);
          return;
        }
        const r = await doLogin(u, p);
        setMsg("登录成功，正在跳转...", "ok");
        // 二次校验 satoken 已被后端接受
        const me = await verify();
        console.log("current user:", me);
        setTimeout(function () { window.location.replace("./legacy.html"); }, 600);
      } catch (err) {
        setMsg("登录失败: " + (err && err.message ? err.message : err), "error");
        setBusy(false);
      }
    });
  });
})();
