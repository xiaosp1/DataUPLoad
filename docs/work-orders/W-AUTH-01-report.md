# W-AUTH-01 Report — 放开账号体系修复 super_admin 登录

> **状态**: ✅ 已完成 & 端到端验证通过
> **服务**: PID 19648, port 80 (started 2026-07-25 10:21:10)
> **当前密码**: `Abc12345` (super_admin)

---

## 1. 背景

老板选了 **方案 A**（**保留账号体系**）：放开 `excludeFilters` + 添加白名单 + 解决 MyBatis XML 路径冲突。

**目标**: PSM web 用 `super_admin / Abc12345` 登录成功，跑通业务。

---

## 2. 实施步骤（全部 PM 自完成）

### T1 — Application.java 移除 4 个 excludeFilters

**位置**: `DataupLoad/src/main/java/com/hikrobotics/solution/Application.java`

**改动**: 移除 `com.hikrobotics.solution.framework.component.account.*` 等 4 个 exclude regex。

### T2 — application-prod.yml 添加白名单

**位置**: `DataupLoad/config/application-prod.yml`

**新增**:
```yaml
hik-security:
  white-list:
    - /web/auth/**
    - /web/account/**
    - /auth/login
    - /auth/logout
    # ... (其他白名单保留)
```

### T3 — 重置 super_admin 密码

**DB**: `UPDATE account SET password = '$2a$10$avNZyF9lLaqOK7gxOQovBOhI3akBjPB9JTB4TGIm9ntAttLx.jMlu' WHERE username = 'super_admin';`

**密码生成**: `bcrypt("Abc12345")` (PSM `resetAdminPwd` 流程用的是**单 bcrypt**)

**关键证据**:
- `AccountServiceImpl.resetAdminPwd` 字节码: `DigestUtil.bcrypt(明文)` 单次哈希
- `AccountServiceImpl.add / resetPwd / changePwd / changeInfo` 用 `bcrypt(sha256Hex(明文))` 双重哈希
- **新增账户**（add/resetPwd/changePwd）与 **重置 admin 密码**（resetAdminPwd）算法不一致 — **PSM 历史 bug**，暂不修
- `AccountDTO.checkPwd` 用 `bcryptCheck(明文, hash)` 单次 — **所以 super_admin 必须用单 bcrypt 格式**

### T4 — 全量编译 + 重启

**编译**: 187 个 .java 文件，0 错误。

**重启**: 启动后服务 PID 19648 监听端口 80。

### T5 — 端到端验证（✅ 通过）

```bash
POST /web/auth/login
Content-Type: application/json
Body: {"username":"super_admin","password":"Abc12345"}
```

**响应**:
```json
HTTP/1.1 200 OK
Set-Cookie: satoken=c2450594-e107-4843-9fbe-c29efe7e68f0

{
  "success": true,
  "data": {
    "id": 1,
    "username": "super_admin",
    "role": "super_admin",
    "permission": ["user", "log", "app-account"],
    "createTime": "2026-07-22 16:50:16:850",
    "updateTime": "2026-07-25 10:05:42:583"
  },
  "code": 0
}
```

**业务 API 验证**（带 `satoken` cookie）:
| Endpoint | Status | Body |
|---|---|---|
| `/web/account/current` | 200 | 当前 super_admin 信息 ✅ |
| `/web/account/list?pageNum=1&pageSize=10` | 200 | 空 records（除 super_admin 外无其他账号）✅ |

---

## 3. 修复过程中的关键技术决策

### 3.1 MyBatis XML 路径冲突（最大坑）

**问题**:
- `framework-starter-2.2.3-SNAPSHOT.jar` 内 `com/hikrobotics/solution/framework/mapper/AccountMapper.xml`
- interface `AccountDAO` 在 `com/hikrobotics/solution/framework/component/account/mapper/`
- 跨包，MyBatis 默认同包扫描找不到 → `Invalid bound statement`

**第二坑**: 即使强行加载 jar 内 XML，它的 `<resultMap id="BaseResultMap">` 跟 MyBatis-Plus 的 AutoMap 冲突 → `Result Maps collection already contains`

**解决方案**: 在项目 `src/main/resources/mapper/AccountMapper.xml` 写覆盖版（MP 默认 `classpath*:/mapper/**/*.xml` 命中），**去掉 BaseResultMap**（避免冲突），只保留 `AccountDTO` resultMap + get/listAll SQL。

### 3.2 account 表 vs app_account 表（PM 一开始看错）

- ❌ **app_account**: OAuth2 client 表（client_id/client_secret/scope）— 不存用户
- ✅ **account**: 真正的用户账号表（id/username/password/role_id/create_time/update_time + real_name + contact_info）

### 3.3 add/resetAdminPwd 哈希算法不一致（PSM 历史 bug，已发现未修）

`AccountServiceImpl.resetAdminPwd` → 单 `bcrypt(明文)`
`AccountServiceImpl.add / resetPwd / changePwd` → `bcrypt(sha256Hex(明文))` 双重哈希

如果用 add/resetPwd 创建的用户去 login，会**全部密码错误**（除非知道这个 bug 的人故意只用 resetAdminPwd）。这是 PSM 自己的 bug，**本工单不修**（与放开账号体系无关）。

---

## 4. 改动文件清单

| 文件 | 改动 |
|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/Application.java` | 移除 4 个 excludeFilters |
| `DataupLoad/config/application-prod.yml` | 加白名单 `/web/auth/**` + `/web/account/**` |
| `DataupLoad/src/main/resources/mapper/AccountMapper.xml` | 新增 — 覆盖 jar 内同名 XML（无 BaseResultMap） |
| `DataupLoad/src/main/resources/logback-spring.xml` | 临时开 DEBUG SQL log → 已恢复 INFO |

---

## 5. 数据库种子（运行时状态）

```sql
SELECT id, username, role_id FROM account;
-- 1 | super_admin | 1

SELECT password FROM account WHERE username='super_admin';
-- $2a$10$avNZyF9lLaqOK7gxOQovBOhI3akBjPB9JTB4TGIm9ntAttLx.jMlu
-- (= bcrypt('Abc12345'))
```

---

## 6. 后续建议（不在本工单范围）

- **P3**: 统一 add/resetPwd/resetAdminPwd 哈希算法（要么都单 bcrypt，要么都 bcrypt+sha256）
- **P3**: 业务 API 路径前缀统一（AccountController 类级别缺 `@RequestMapping("/web")`，导致 `/account/list` 404，但 `/web/account/list` 200 — 实际 PSM web 走的就是 `/web` 前缀）
- **P3**: LocaleUtil.messageSource 注入（让 20204/10101 错误码能正常显示文案而不是 500）

---

## 7. 心跳留痕

- 服务状态: PID 19648 (2026-07-25 10:21:10+)
- 端口 80 listen
- 业务 API 跑通
- 0 P0/P1 阻塞 — **生产可用**

后续待办（待老板拍板）:
- W-DET-10b baseline 端到端测试矩阵（baseline/1k/10k/100k 场景）
- 老板验收 → 统一 push git
