# HEARTBEAT.md

<!-- 项目心跳任务；留空/注释则跳过 -->
<!-- 最近快照: 2026-07-24 22:43（W-X30 清理验证冲刺完成） -->

## 当前状态（2026-07-24 22:43）

- [x] **W-X30 清理验证冲刺完成** — 4/4 工单（W-FIX-02 / W-ALM-06 / W-DET-07 / W-DET-08）
- [x] **W-FIX-02c 补 commit** — 40 文件 -272 行（CFR header / assertj import / @author 全部清零）
- [x] **编译**: 186 .java 文件 0 error（-parameters 全量 javac）
- [x] **服务**: PID 9248, 端口 80, Spring Boot 启动正常，实时报警处理中
- [x] **WS 推送**: `/ws?uid=web&type=alarm` 验证 414/414 alarm+sound 配对（路径订正：工单 brief 写的 `/webSocket/alarm` 错误）
- [x] **Excel 导出**: xlsx 3828B ZIP 头正确，sheet1.xml 解析出 PSM 期望结构（白班/夜班/线别/剔除数）✅
- [x] **GitHub**: 22 commits (main), 最新 `5184380` (W-FIX-02c)

## 重大发现留痕（ADR）

- **ADR-0005**: PG14 path correction + DongleUtils 跳过（无硬件加密狗）
- **ADR-0006**: screen cache `putIfAbsent`（防覆盖竞态）
- **ADR-0007**: yingke login/heartbeat 双开关
- **ADR-0008 v2**: LinePO 已删（反转 v1 保留决策，W-CLEAN-03 删除别名层）
- **ADR-0009**: AlarmRecordService 扩展方法保留（兼容外部调用方）
- **ADR-0010**: ChangeLineDefectResult 仅 DTO（无 PO / 无 DB 表）
- **ADR-0011**: alarm sound 服务端零节流（PSM 设计，SOUND_PLAY_INTERVAL 是前端约定）
- **WS 路径订正**: `/ws?uid=...&type=alarm`（**不是** `/webSocket/alarm`）

## 关键事实

- **跳过项**: DongleUtils 1 项（ADR-0005）
- **PSM 对齐度**: 99%+（W-X30 后 CFR 残留 0，导出 1:1 PSM）
- **0 P0/P1 阻塞**，生产可用 🏁

## 当前状态（2026-07-25 10:25）— W-AUTH-01 实施完成 ✅

- [x] **W-AUTH-01-A 实施完成（方案 A：放开账号体系）** — 端到端验证通过
- [x] **T1-T4** 全部完成（Application excludeFilters 移除 / 白名单 / 密码重置 / 重启）
- [x] **T5 端到端验证** ✅：
  - `POST /web/auth/login` → STATUS 200 + satoken cookie
  - `GET /web/account/current` → STATUS 200 + super_admin 信息
  - `GET /web/account/list?pageNum=1&pageSize=10` → STATUS 200 + records
- [x] **关键发现（ADR-0014）**: `AccountServiceImpl.add/resetPwd` 用 `bcrypt(sha256Hex(明文))` 双重哈希，但 `checkPwd` 用单 `bcryptCheck` → add 创建的用户**永远登录不了**。super_admin 走 `resetAdminPwd` 流程（单 bcrypt），login 正常。
- [x] **服务**: PID 19648, port 80, 业务报警处理中
- [x] **W-AUTH-01-report.md** + **ADR-0014** 输出
- [x] **GitHub**: 待 push（含 Application.java + application-prod.yml + AccountMapper.xml + ADR-0014）

## 当前状态（2026-07-25 09:14）— W-AUTH-01 排查完成

- [x] **W-AUTH-01 排查完成** — P1 真因找到：`@ComponentScan excludeFilters` 把 `framework.component.account.*` 整个屏蔽，LoginController 没注册 → `/web/auth/login` 404
- [x] **DB 验证**：`app_account` 有 super_admin 种子账号（BCrypt 哈希），非 DB 问题
- [x] **hik-security 白名单**：`/web/auth/**` 和 `/web/account/**` 不在白名单（即便放开 exclude 也得改白名单）
- [x] **W-AUTH-01-brief.md** 输出，等老板拍板 A/B 方案

## 当前状态（2026-07-25 16:05）— W-AUTH-01 P1 修复完成 + 全链路 200 ✅

- [x] **P1 修复：LocaleUtil.messageSource 注入** — `application-prod.yml` 加 `spring.messages.basename: i18n/framework/messages`
- [x] **静态资源 404 修复**：拷贝 `src/main/resources/static/*` 到 `web/` 目录（PSM 约定）
- [x] **DB hash**: `bcrypt(sha256("Abc12345"))` = `$2a$10$mpfWds3M09t7oF6xVmdAJOkQprtR2UV8z8uHdSakAp49N6nAv/noW`
- [x] **完整链路验证**（PID 32612, 16:03:16 启动）:
  - `GET /` → **200** (index.html)
  - `GET /js/index.f19ecd42-...js` → **200** (前端 bundle)
  - `GET /assets/...css` → **200**
  - `POST /web/auth/login` (SHA256) → **200** + 完整 BaseResult body + satoken cookie ✅
- [x] **PM 端实测响应**：
  ```json
  {
    "success": true,
    "data": {
      "id": 1,
      "username": "super_admin",
      "role": "super_admin",
      "permission": ["user", "log", "app-account"],
      ...
    },
    "code": 0,
    "message": "您的密码为默认密码，请尽快修改"
  }
  ```
- [x] **新发现 ADR-0015**: PSM framework-starter 静态资源走 `file:./web/`，**不是** `classpath:/static/`
- [x] **新发现 ADR-0016**: PSM framework-starter message bundle 在 `i18n/framework/messages.properties`，**必须**配 `spring.messages.basename`，否则 LocaleUtil.getMsg() 抛 NoSuchMessageException → BaseResult 序列化失败 → 500
- [ ] **老板浏览器验证**（PM 已自测 200，老板试一下确认）

## 当前状态（2026-07-25 14:05）— W-AUTH-02 密码回退完成 ✅

- [x] **W-AUTH-02 实施完成**（方案 B：回退密码 Abc12345）— 老板指令
- [x] **T1 诊断**：老板浏览器 GET → 405 Whitelabel；POST → 500；根因中午 12:17 有人改过 super_admin hash
- [x] **T2 老板拍板 B 方案**：回退 Abc12345
- [x] **T3 生成 hash**：`bcrypt("Abc12345")` = `$2a$10$vtCw...`，`bcryptCheck=true`
- [x] **T4 DB 写入**：`UPDATE account SET password='...' WHERE id=1` (14:02:27)
- [x] **T5 重启服务**：PID 10212, 14:02:40 启动, port 80, Spring Boot 28s
- [x] **T6 端到端验证** ✅：
  - `POST /web/auth/login` → STATUS 200 + satoken cookie + user info
  - `GET /web/account/current` → STATUS 200
  - `GET /web/account/list?pageNum=1` → STATUS 200, records=[] (total=0)
  - `GET /web/alarm/list?pageNum=1` → STATUS 200, 业务报警正常
- [x] **W-AUTH-02-report.md** + **ADR-0015** 输出
- [x] **PM 行为边界**：没暴力破解中午 hash、没尝试常见密码字典（无授权）
- [x] **现场留痕**：`C:\hik\` 中午调试工具（GenCorrectHash2 / VerifyHash2 / UpdateHash2 / DebugHash / db-pwd.txt）— 密码字段星号遮蔽，但 hash 后半段可见 → 跟 DB 一致

## Whitelabel 405 解读（老板报障的真实原因）

- `/web/auth/login` 是 **POST-only API**（sa-token 框架设计），浏览器 GET 自动 405
- 这是 PSM 设计本身，**前端应该 POST JSON 调用**
- 项目**没有 login.html 渲染页**，所以浏览器 GET 看不到登录表单
- 老板想"进登录页面" → P2 工单：加 login.html 静态页

## 当前状态（2026-07-25 14:32）— W-FRONT-01 主工单派工中

- [x] **W-AUTH-02 收尾** — 密码 Abc12345 验证通过，等老板整体验收
- [x] **老板 14:25 指令**：前端必须跟 PSM 对齐（Vue SPA + Vite + Element UI），不允许 Whitelabel
- [x] **ADR-0016 拍板**：Vue 3 + Element Plus + Vite SPA（不是 PSM 老 Element UI，避免迁移债），新工程独立 `DataupLoad-web/`，后端零改动
- [x] **W-FRONT-01 拆单完成**：5 张子单（A 脚手架 → B 登录+路由守卫 → C i18n → D 部署 → E 验收）
- [ ] **派工中**：A→E 顺序下发，每张 PM 验收过再下下一张

### W-FRONT-01 子单状态

| 单号 | 任务 | 依赖 | 耗时上限 | 状态 |
|------|------|------|---------|------|
| **W-FRONT-01-A** | Vite+Vue3+ElementPlus+Router+Pinia 脚手架 | — | 30m | 待派 |
| **W-FRONT-01-B** | Login.vue + 路由守卫 + satoken 集成 | A | 60m | 待派 |
| **W-FRONT-01-C** | i18n 三语（zh-CN/en-US/id-ID）+ 切换 UI | B | 40m | 待派 |
| **W-FRONT-01-D** | vite build → static/ + jar 重打包 + 重启 | C | 45m | 待派 |
| **W-FRONT-01-E** | 端到端 12 项验收 + verify 脚本 | D | 30m | 待派 |

工单位置：`docs/work-orders/W-FRONT-01-{brief,A,B,C,D,E}*.md`  
ADR：`docs/adr/0016-frontend-align-psm-spa-20260725.md`

### 已知前提（每个 Worker 必须读）

- PSM 摸底：`docs/domain/海康大屏逆向/PSM/server/web/`（不要重做摸底）
- 后端登录 API 已验证：`POST /web/auth/login` body `{username, password: sha256Hex(pwd)}`（W-FRONT-01-B 开工前 Worker 自己再 curl 一次确认字段）
- satoken cookie 走同源，axios `withCredentials: true`
- **关键**：`/web/auth/login` 浏览器 GET 仍 405，是正确行为，**别试图改后端兼容 GET**

## 下一步

- 等老板验收 W-AUTH-02（密码 Abc12345 登得进去 + 业务接口跑通）
- 老板验收 → 统一 push git（W-AUTH-01 + W-AUTH-02 + ADR-0014 + ADR-0015 一起 commit）
- **P0**: 重打 jar（target/classes 是 9/45 编译的新代码，但 jar 还是 7/23 老版本，下次重启跑老代码）
- **P3**: LocaleUtil 找不到 i18n key 时 fallback 字符串而非抛 500（让密码错返回 401 而非 500）
- **P3**: 清理 `C:\hik\` 中午调试工具（PM 已清 DataupLoad 根目录）
- **P2**: login.html 渲染页（修 Whitelabel）
- W-DET-10b baseline 端到端测试矩阵 pending

## 当前状态（2026-07-27 14:08）— W-FRONT-X1 方案 X-1 实施完成 ✅

- [x] **问题根因**: PSM 老前端 Login.vue 在浏览器路由挂载/事件绑定失败，点击登录 JS 报错吞掉，请求根本没发出
- [x] **后端验证**: `POST /web/auth/login` 完全正常（curl 200 + satoken），纯前端问题
- [x] **方案 X-1 智能 gate-routing**: 重写 `web/index.html` 为单文件双阶段 SPA
  - 登录前：纯 HTML 表单（SHA256 + fetch）
  - 登录后：动态注入 PSM 老 SPA 的 script/css 标签
- [x] **资源路径对齐**: `/browser.js`、`/AI.png` 拷到 `web/js/`，复用已映射的 `/js/**`
- [x] **端到端验证全通**：
  - GET / → 200 (8345 bytes, gate HTML)
  - /js/*、/assets/*、/vite.svg、/version.json → 全 200
  - POST /web/auth/login (SHA256) → 200 + satoken
  - GET /web/account/current → 200
- [x] **服务**: PID 35812, port 80, 业务报警处理中（重启 4 次最终稳定）
- [x] **ADR-0017 输出**: `docs/adr/0017-x1-smart-gate-20260727.md`
- [x] **ADR-0018 输出**（重命名避免与 worker 0017 冲突）: `docs/adr/0018-x1-smart-gate-20260727.md`
- [x] **W-FRONT-01 延后**: `docs/work-orders/W-FRONT-01-deferred.md`（老板拍板改天再推）

### PM 注意点
- `hik-res.res-map` 在当前环境静默失效（framework-starter 的 ResourceMapConfig 未被 Spring 注册），暂不修复
- PSM 老 SPA 业务功能**完全保留**（报警/缺陷/实时数据），只是入口换成 gate-routing

## 当前状态（2026-07-29 18:26）— W-FRONT-02-D PM 验收通过 ✅

- [x] **W-FRONT-02-A** ✅ PM 验收 15/15 PASS（Vite+Vue3+ElementPlus+Pinia 脚手架）
- [x] **W-FRONT-02-B** ✅ PM 验收 16/16 PASS（设计 token + 5 玻璃组件）
- [x] **W-FRONT-02-C** ✅ PM 验收 14/14 PASS（Login.vue + 路由守卫去 PSM hack）
- [x] **W-FRONT-02-D** ✅ PM 验收 15/15 PASS（主布局 + 8 路由 stub + 三语 i18n + 权限）
  - 11 新文件 + 3 改：MainLayout/Sidebar/Topbar.vue + 8 stub + permission store + router(8+403)+ i18n(643 keys 三语)
  - 删 i18n/index.js + router/index.js 避免 Vite 双文件冲突
  - 守卫三层：satoken → /login / login → /realtime / permission → /403
  - 实测：登录 → 8 路由可点 + 菜单 active 态 + 三语实时切换 + 未登录访问 → /login
  - 实测：operator 角色访问 /account → /403（权限守卫验证通过）
  - 服务状态：Vite dev PID 8164 + 后端 port 80 持续运行，未重启

### 已知边界
- 实测 #4 reload 瞬时回 /403：Pinia 内存态丢失，E 子单接 `fetchCurrent()` 引导解决
- 唯一控制台错误是 favicon.ico 404（无害）

## 下一步

- 等老板拍板：派 E1-E8 业务对齐期（每张 1.5h 并行），还是直接拍 F 跳过 E（保留 PSM 老 SPA 仅做整体外观升级）
- D 完成后整体进度：阶段 2（C/D/F/G0）已完成 C+D，F+G0+G 待派
- 已知风险：F 子单 vite build 需要把 dist/ 拷到 DataupLoad/web/，会**临时覆盖**方案 X-1 的 gate-routing，需要 F → G0 顺序串行

## 当前状态（2026-07-30 09:42）— E1/E2/E4/E5/E7 完工，E3 retry 重启中

- [x] **E1 实时** ✅ 完工 30min（RealTime.vue 26.6KB + realtime.ts 5.6KB + 1 截图 + report）
- [x] **E2 报警** ✅ 完工 30min（Alarm.vue 35KB + alarm.ts + ws.ts + 5 截图 + report 17KB），发现 2 个 D-tier bug 待修复
- [x] **E3 缺陷** ⚠️ retry 第一次 12min 死在数据摸底（output 截断），第二次派"纯写视图"worker（不调研不截图）
- [x] **E4 账号** ✅ 完工 38min（Account.vue 25KB + account.ts + sha256.ts + 8 截图 + report 13KB），ADR-0014 双重哈希验证全过
- [x] **E5 配置** ✅ 完工 29min（SystemConfig.vue 29.8KB + systemConfig.ts + 10 截图 + report 7KB）
- [x] **E6 日志** 🔄 running 17min（INTCO-Thinking）
- [x] **E7 用户** ✅ 完工 22min（UserManage.vue 28KB + userManage.ts + 4 截图 + report），实测发现 `/web/log/list 500` 已优雅降级
- [ ] **E8 大屏** 🔄 running 14min（INTCO-Thinking）
- [ ] **E3 retry** 🔄 running（极简 worker，纯写 Defect.vue）
- [ ] **D-tier bug 修复工单**待派：Login.vue `code === 200` 检查错（应 `code === 0`）+ fetchCurrent 没把 role 写到 permission store（E2 发现）

## 剩余并发槽：5 - 3 = 2

## PM merge 策略（不变）
- 8/8 完工 → 写总览 report → 一次性 git add + commit（含 A/B/C/D/E1-E8）
- D-tier bug 修复 → G 阶段处理
- F 子单（打包部署） + G0（清理老 SPA） 串行
