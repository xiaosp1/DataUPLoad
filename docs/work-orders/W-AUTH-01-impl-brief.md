# W-AUTH-01-impl — 放开 PSM 账号体系（W-B03 excludeFilters 反转）

**优先级**: P1（接续 W-AUTH-01 排查，老板 09:27 拍板方案 A）

**目标**: 让 `super_admin / Abc12345` 能登录 `http://localhost:80/`（前端 `/Login` 路由）

---

## 当前状态（PM 已排查）

详见 `docs/work-orders/W-AUTH-01-brief.md`

**3 个 P1 阻塞点**：
1. `Application.java` `@ComponentScan excludeFilters` 屏蔽了 `account.*` / `appaccount.*` / `oauth2.*` / `auth.*` 4 个包
2. `application.yml` `hik-security.auth.uri-permit` 白名单缺 `/web/auth/**` 和 `/web/account/**`
3. DB `app_account` 表 super_admin 密码哈希 ≠ `bcrypt(sha256("Abc12345"))`，登录会被拒

---

## 实施任务清单（T1-T6）

### T1 — 放开 excludeFilters（核心）

文件：`DataupLoad/src/main/java/com/hikrobotics/solution/Application.java`

**操作**：从 `@ComponentScan.excludeFilters.pattern` 数组里**删除**以下 4 条 regex：

```java
// 删除 1
"com\\.hikrobotics\\.solution\\.framework\\.component\\.account\\..*",
// 删除 2
"com\\.hikrobotics\\.solution\\.framework\\.component\\.appaccount\\..*",
// 删除 3
"com\\.hikrobotics\\.solution\\.framework\\.component\\.oauth2\\..*",
// 删除 4
"com\\.hikrobotics\\.solution\\.framework\\.component\\.auth\\..*",
```

**风险预案**（如果启动报 UnsatisfiedDependency）：

| 报错类型 | 原因 | 修复 |
|---|---|---|
| `NoSuchBeanDefinition: EncryptionUtils` | dongle exclude 但被 oauth2 引用 | 临时把 dongle 也放开 |
| `NoSuchBeanDefinition: DongleUtils` | 同上 | 同上 |
| `NoSuchBeanDefinition: WebConfigure` | ssl exclude 但被 oauth2 WebSecurity 引用 | 临时把 ssl 也放开 |
| `RedisConnectionFailureException` | sa-token oauth2 默认用 redis | 加 `sa-token.oauth2-server-url` 配置或临时禁 sa-token oauth2 |
| `HikSecurityConfig not initialized` | hik-security 缺 OAuth2Properties bean | 已存在（`hik-security.oauth.enable: false`），无需改 |

**降级预案**：如果级联依赖太多（如 dongle 需要 native lib），保留 dongle/ssl exclude，只放开 account/appaccount/oauth2/auth。

---

### T2 — 放开 hik-security 白名单

文件：`DataupLoad/src/main/resources/config/application-prod.yml`

**操作**：在 `hik-security.auth.uri-permit` 列表里**新增**两条：

```yaml
hik-security:
  auth:
    uri-permit:
      - /data/**
      - /ws/**
      - /img/**
      - /client/**
      - /web/upload/file
      - /web/defect**
      # W-AUTH-01 新增
      - /web/auth/**
      - /web/account/**
```

---

### T3 — 重置 super_admin 密码为 `Abc12345`

**方式 A（推荐）**：Java 代码一次性跑

```java
// 写一个一次性脚本，注入 BCrypt 工具
String plain = "Abc12345";
String sha = DigestUtil.sha256Hex(plain);
String bcrypt = BCrypt.hashpw(sha, BCrypt.gensalt(10));
// UPDATE app_account SET password = '<bcrypt>' WHERE username = 'super_admin';
```

**方式 B（备选）**：直接用 PG CLI 生成 BCrypt 写到 DB（需要 PG 端有 pgcrypto 扩展）。

PM 自己跑（不在 codex 范围）：

```powershell
# 启动一个一次性 java 程序，或用现有 jar 入口注入
# 推荐：写一个 BatchUpdateRunner.main() 临时调用 AccountServiceImpl.resetPwd(1, "Abc12345")
```

**为什么 PM 自己跑**：
- 不是 codex 任务（代码改动小，单条 SQL）
- 涉及 BCrypt 密钥盐，要确保编码/字符集一致
- 失败回滚成本低（直接再 UPDATE 回去）

---

### T4 — 全量编译 + 启动

```powershell
# 编译（T1 改后）
& 'E:\DEMO\DATALINK\DataupLoad\jdk\bin\javac.exe' -cp (Get-Content 'E:\DEMO\DATALINK\DataupLoad\.classpath.txt' | Out-String) -d 'E:\DEMO\DATALINK\DataupLoad\target\classes' -parameters `
    (Get-ChildItem 'E:\DEMO\数据采集\DataupLoad\src\main\java' -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)

# 重启服务
cmd /c 'E:\hik\restart.bat'
```

**预期日志**（成功标志）：
- `Started Application in X.XXX seconds`
- 没有 `UnsatisfiedDependencyException` / `NoSuchBeanDefinitionException`
- `LoginController` 出现在 RequestMappingHandlerMapping 注册列表里

---

### T5 — 端到端验证

```powershell
# T5.1 login API
$body = '{"username":"super_admin","password":"***"}'
Invoke-WebRequest -Uri 'http://localhost:80/web/auth/login' -Method POST -ContentType 'application/json' -Body $body

# 期望：
# status=200
# body={"success":true,"code":0,"data":{"token":"xxx","needResetPwd":false},...}

# T5.2 logout API
$token = '从 T5.1 拿到'
Invoke-WebRequest -Uri 'http://localhost:80/web/auth/logout' -Method POST -Headers @{Authorization="Bearer $token"}

# T5.3 isLogin API
Invoke-WebRequest -Uri 'http://localhost:80/web/auth/isLogin' -Headers @{Authorization="Bearer $token"}

# T5.4 前端 SPA 登录
# 浏览器开 http://localhost:80/
# 应跳到 /Login 路由
# 输入 super_admin / Abc12345 → 跳回 /
# localStorage.token 已写入
```

**失败矩阵**：
- 401 "account not found" → DB 表名错了（应是 `app_account` 而非 `account`）
- 401 "password is error" → T3 重置失败，或 BCrypt 编码不一致
- 500 / Bean 异常 → 回到 T1 降级预案

---

### T6 — 回归 & 留痕

- [ ] 跑 `C:\perf-scripts\run_det_scenario.ps1` baseline 端到端（确认 detect 模块没被影响）
- [ ] 跑 alarm WS 推送测试（确认 AlarmRecordService 没被影响）
- [ ] 输出 `docs/work-orders/W-AUTH-01-report.md`（实施 + 验证记录）
- [ ] 输出 `docs/adr/0013-account-system-enabled.md`（ADR 留痕：W-B03 反转决策）
- [ ] git commit（PM 验收后统一 push）

---

## codex 任务范围

**codex 负责**：T1 + T2 + T4 + T5 + T6 文档
**PM 负责**：T3（密码重置） + 最终验收 + git push

---

## 注意事项

1. **不要 push git**：PM 验收后统一 push
2. **不要碰 yk 模块**：yk 的 loginEnabled/uploadEnabled 跟 W-AUTH-01 无关
3. **保持原 excludeFilters 注释风格**：在 Application.java javadoc 里加一行"W-AUTH-01 反转：放开 account 体系"
4. **hik-security 配置注意**：`oauth.enable: false` 必须保留，否则会尝试连 redis
5. **TestCredentials**：临时写一个 doc 字段 `test-creds: super_admin/Abc12345` 方便后续验证（不是代码，是文档）

---

## 启动时间预估

- codex 改代码: 30min
- 全量编译: 5min
- 重启 + 等待: 2min
- 验证: 10min
- 留痕: 10min
- **总: ~1h**
