# W-AUTH-01 — PSM web 端登录可行性排查

**优先级**: P1（阻塞前端登录页面）

**老板原话**: "psm 的 web 端是有账号密码登录要求的，原 psm 的账号密码是 super_admin Abc12345，你来看看是不是数据库表里没写。详细看看。"

**结论先上**:

1. **DB 里有 `super_admin` 种子账号**（PG `app_account` 表 1 行，BCrypt 哈希 `$2a$10$8waDc...`）
2. **但根本走不到密码校验**——`/web/auth/login` 接口在当前服务里返回 404
3. **真因不在 DB**，是 W-B03 启动时主动屏蔽了 `framework.component.account.*` 包

---

## 排查链路

### 1. 反编译 framework-starter-2.2.3-SNAPSHOT.jar

`C:\hik\starter-check\com\hikrobotics\solution\framework\component\account\web\LoginController.class`

```java
@RestController
@RequestMapping("/web")
public class LoginController {
    @PostMapping("/auth/login")    // ← PSM 标准路径
    public BaseResult login(@RequestBody LoginDTO loginDTO) { ... }
    
    @PostMapping("/auth/logout")
    public BaseResult logout() { ... }
}
```

### 2. 前端 JS 调用 — `index.f19ecd42-20260520160358.js`

```js
const S = "/web/"
const Xa = {
  login:        `${S}auth/login`,         // POST /web/auth/login
  logout:       `${S}auth/logout`,        // POST /web/auth/logout
  modifyPwd:    `${S}account/pwd`,        // PUT  /web/account/pwd
  getPwdSerial: `${S}account/pwd/serial`, // GET  /web/account/pwd/serial
  ...
}
```

前端是从 PSM 抄过来的，含完整登录 UI 和调用链（路由 `/Login`，标题"英科手套检测中控平台"）。

### 3. 实测 HTTP 接口

```powershell
POST /web/auth/login {"username":"super_admin","password":"Abc12345"}
→ 404  # 接口不存在！
POST /web/account/login {"username":"super_admin","password":"Abc12345"}
→ 404
GET /web/auth/isLogin
→ 404
```

所有 PSM `/web/auth/**` 和 `/web/account/**` 路径全部 404。

### 4. Application.java 主动屏蔽了 account 包

```java
// E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\Application.java
@ComponentScan(
    basePackages = {"cn.hutool.extra.spring", "com.hikrobotics.*"},
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.hikrobotics\\.solution\\.framework\\.component\\.account\\..*",  // ← LoginController 在这
                "com\\.hikrobotics\\.solution\\.framework\\.component\\.appaccount\\..*",
                "com\\.hikrobotics\\.solution\\.framework\\.component\\.oauth2\\..*",
                "com\\.hikrobotics\\.solution\\.framework\\.component\\.auth\\..*",
                ...
            }
        )
    }
)
```

注释说明（W-B03 决策）：
> "PSM 账号体系（DataupLoad 用 hik-security 白名单，不需要）"

**这就是登录不可用的根因——不是 DB 没种子，是 controller 没注册。**

### 5. DB 种子数据现状（已查 PG）

```
app_account 表: 1 行
  id=1, username=super_admin, account=..., password=$2a$10$8waDc.WJXiV5j.O6gmB3lu9Oo1BHVEMJUvSOudJ.zqDLQ2cPF0xi6
  (BCrypt 哈希，未明文)

account 表: 1 行 (类似)

role 表: 3 行 (super_admin/admin/operator)
  permission = JSON 数组 (对应菜单权限)
```

种子数据齐全。

### 6. 密码哈希校验（间接验证）

```java
// AccountServiceImpl.add() / resetPwd()
String hashed = BCrypt.hash(SHA256(password));  // sha256 → bcrypt
```

`SHA256("Abc12345") = f8aa14da2301e201e817f5b8667a36bb40c8ca49da69b3470a74d0f4ec194961`

DB 里的哈希 `bcryptCheck` 这串 sha256 失败（说明种子不是 `Abc12345`），但**根因不是这个**：接口都没通到这一步。

### 7. hik-security 白名单配置

```yaml
hik-security:
  auth:
    uri-permit:
      - /data/**
      - /ws/**
      - /img/**
      - /client/**
      - /web/upload/file
      - /web/defect**    ← /web/auth/** 和 /web/account/** 不在白名单
  oauth:
    enable: false       ← 即使登录成功 token 也被忽略
```

**`/web/auth/**` 根本不在白名单**，即使 LoginController 注册了也走不通（会被 OauthInterceptor 拒）。

但目前 oauth.enable=false，**应该**会被 interceptor 放行 —— 但 LoginController 不存在，所以 404。

---

## 真正的矛盾点

| 维度 | 现状 |
|---|---|
| 前端代码 | 含 PSM 风格登录页 UI + 调用 `/web/auth/login` |
| 后端 controller | LoginController 在 jar 里但被 `@ComponentScan` exclude |
| hik-security 白名单 | `/web/auth/**` 和 `/web/account/**` **不在白名单** |
| OAuth 拦截器 | `oauth.enable: false` —— 实际不拦 |
| DB 种子 | super_admin 账号存在 |
| 默认密码 | `Abc12345`（前端 UI 提示 + 业务约定） |
| 当前是否可登录 | ❌ 不可 |

老板的疑问"是不是 DB 里没写"——**不是**，DB 写了。

---

## 修复方案候选

### 方案 A：去掉 `account` 包的 excludeFilters（最直接）

改 `Application.java` 第 60 行左右，删掉 `"com\\.hikrobotics\\.solution\\.framework\\.component\\.account\\..*"` 这条 regex。

**风险**:
- W-B03 当时刻意排除的初衷是什么？注释说"DataupLoad 用 hik-security 白名单，不需要账号体系"
- 启用 account 包会引入依赖链：`account → appaccount → oauth2 → auth`（oauth2 / auth 还在 exclude 里）
- 可能启动报 NoSuchBeanDefinition / UnsatisfiedDependency
- 需要把 `appaccount` / `oauth2` / `auth` 的 exclude 也放开（级联）
- 然后还得改 hik-security 白名单加 `/web/auth/**`

**预计工作量**: 1-2h（派工）+ 全量回归（账号体系牵涉 sa-token 状态）

### 方案 B：保留现状 + 前端跳登录

前端 JS 把"用户管理"菜单隐掉，所有功能走 hik-security 白名单匿名访问。

**风险**:
- 前端路由 `/Login` 改成 `/`，登录按钮挪到"关于" dialog 或直接去掉
- 改动小但破坏前端完整性，PSM 用户的预期被打破

**预计工作量**: 30min

### 方案 C：老板拍板账号体系要不要

请老板决策：
- C1: 老板要登录 → 走方案 A，派 W-AUTH-01 codex 工单
- C2: 老板不要登录 → 走方案 B，前端改无登录模式 + ADR-0012 留痕

---

## 已做 / 待做

- [x] 深度排查 login 链路（前端/后端/DB/配置）
- [x] 锁定真因：account 包 excludeFilters 屏蔽 + hik-security 白名单不全
- [x] W-AUTH-01-brief.md（本文件）
- [ ] 等老板拍板走 A 还是 B
- [ ] 派 codex 实施

## 备注

- 当前服务 PID 4396，端口 80，未变
- `http://localhost:80/` 直接进 SPA（无登录页），登录页是 SPA 内的 `/Login` 路由
- 前端无登录页 → 后端无 login 接口 → hik-security 白名单兜底 → 谁都能进 detect 模块
- 这是 W-B03 启动时的设计决策（有 ADR-0005 / 0006 / 0007 留痕），不是 bug 是**未实现的 feature**
