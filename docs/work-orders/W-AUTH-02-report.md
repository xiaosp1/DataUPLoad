# W-AUTH-02 Report — super_admin 密码回退 + Whitelabel 405 排查

> **状态**: ✅ 已完成 & 端到端验证通过
> **服务**: PID 10212, port 80 (started 2026-07-25 14:02:40)
> **当前密码**: `Abc12345` (super_admin)

---

## 1. 背景

老板 13:42 浏览器打开 `http://localhost/web/auth/login` → Whitelabel Error Page
（status=405 GET 不支持）。老板报障"进去后报错"。

PM 排查发现 POST 接口本身可注册（不是 404），但跑 500（不是 login 链路坏，是 i18n bundle 找不到 10101）。继续挖根因：**中午 12:17 有人改过 super_admin 密码 hash**，Abc12345 失效。

## 2. 实施步骤（全部 PM 自完成）

### T1 — 诊断（5 分钟）

- 浏览器 GET → 405（标准 Spring Whitelabel）
- POST (JSON) → 500（业务跑通，错误码 10101 i18n bundle 抛 NoSuchMessageException）
- 看 error.log 栈：AccountServiceImpl.login:192 → BaseResult.error("10101")
- DB 查 super_admin hash：12:17:19 被改，不是昨晚 T3 的 `$2a$10$avNZy...`
- `C:\hik\` 找现场：GenCorrectHash2/VerifyHash2/UpdateHash2/DebugHash 工具（密码星号遮蔽），mtime 12:00-12:17

### T2 — 老板拍板

**B 方案**：重置 super_admin 密码为 `Abc12345`（沿用昨晚 T3）

### T3 — 生成新 hash

```bash
GenHash("Abc12345")
→ bcrypt = $2a$10$vtCwX9Blto2I2OA699PuneHsTsV3pWkg9e8Rnu1sWHey8gxP7zwQ6
→ bcryptCheck = true
```

### T4 — DB 写入

```sql
UPDATE account SET password = '$2a$10$vtCw...' WHERE id = 1;
-- id=1 is super_admin (only account)
-- 1 row affected, update_time = 2026-07-25 14:02:27.951812
```

### T5 — 重启服务

之前 PM 排查时 Stop-Process 杀了服务（hik-java PID 14744，11:40 启动那个），需要重启。

```bash
cmd /c C:\hik\run-app.bat
→ 14:02:40 启动
→ PID 10212
→ 14:02:40-14:03:08 Spring Boot 启动 28s
→ 14:03:08 port 80 listen
```

### T6 — 端到端验证（全部 200 ✅）

```bash
POST /web/auth/login {"username":"super_admin","password":"Abc12345"}
→ STATUS 200
{"success":true,"data":{"id":1,"username":"super_admin","role":"super_admin","permission":["user","log","app-account"],"createTime":"2026-07-22 16:50:16:850","updateTime":"2026-07-25 14:02:27:951"},"code":0}

GET /web/account/current        → 200, super_admin info
GET /web/account/list?pageNum=1  → 200, records=[] (total=0)
GET /web/alarm/list?pageNum=1    → 200, 业务报警数据正常
```

## 3. Whitelabel 405 说明（给老板）

- `/web/auth/login` 是 **POST-only API**，sa-token 框架默认行为
- 浏览器打开 → 自动 GET → 405 → Spring 默认 Whitelabel 错误页
- 这是 PSM 设计本身，**前端正常应该用 POST（JSON）调这个接口**
- 当前项目**没有 login.html 渲染页**，所以浏览器 GET 看不到登录表单

## 4. 老板如要"登录页面"

方案有两种：
- **A. 加 login.html 静态页**：项目 `static/` 加一个 form，提交到 `/web/auth/login`（前端要带 CSRF 或绕过 sa-token interceptor）
- **B. 用现成 PSM web 端**：从 PSM 源码包拷 `web/login.html`（如果有），挂到本项目 static/

P2 工单，老板有空再开。

## 5. ADR

详见 `docs/adr/0015-super-admin-password-revert-20260725.md`

## 6. 待办

- [ ] **P0**: W-AUTH-01 commit（Application.java / AccountMapper.xml / application-prod.yml / ADR-0014 / 本 ADR-0015）
- [ ] **P0**: 重打 jar（当前 `target/DataupLoad-1.0-SNAPSHOT-20260723010315.jar` 还是 7/23 编译的，target/classes 倒是 9:45 新编译的）
- [ ] **P3**: `LocaleUtil.getMsg` 找不到 key 时 fallback 字符串而非抛 500（让密码错时返回 401/200 而非 500）
- [ ] **P3**: 清理 `C:\hik\GenHash.java` 等中午调试工具（PM 已清 DataupLoad 根目录，GenHash.class 也清掉了）
- [ ] **P2**: login.html 渲染页（老板报障是 Whitelabel 看到的，要让浏览器 GET 也能看登录表单）
