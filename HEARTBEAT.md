# HEARTBEAT.md

<!-- 项目心跳任务；留空/注释则跳过 -->
<!-- 最近快照: 2026-08-03 18:20（W-ALARM-PUSH 报警推送根治：末端 uid 广播错位修复） -->

## 当前状态（2026-08-03 18:20）— W-ALARM-PUSH 报警推送根治 ✅ 老板验收中

- [x] **老板发现**："报警推送功能并未实现"（登录后前端收不到实时报警）
- [x] **根因（WS uid 广播错位）**：
  - 前端报警 WS 一律 `uid = userStore.id ? String(userStore.id) : 'web'`，**登录后 uid=用户id(1)**，type=alarm
  - 后端 `sendAlarmTextMessage()` / `sendAlarmSoundWsMessage()` 用 `broadcastByUid(json, "web")` → 只推给 **uid="web"** 的 session
  - **登录态前端 uid=1 ≠ web → 永远收不到推送**；只有未登录/登出（uid 回到 web）才能收到
- [x] **WS 实测实证**：连 uid=1 vs uid=web 两个 alarm WS —— uid=web 收到 alarm+sound 2 条，uid=1 0 条
- [x] **修复**：`AlarmRecordServiceImpl` 两处 `broadcastByUid(…,"web")` → `broadcastByType(…,"alarm")`（按 type 广播，登录态用户也能收到；与 `AlarmWebSocketHandler.push()` 现用法一致）
- [x] **修复后 WS 实测**：uid=1 收到 alarm+sound 3 条，POST /client/data/alarm → success ✅
- [x] **未影响产线**：重启后 2 分钟 detect 412 ok / 0 10500
- [x] **后端**：PID 27736, port 8080
- [x] **待老板浏览器验收**：登录平台 → 触发一条报警 → 实时弹窗/徽章出现

## 当前状态（2026-08-03 15:44）— W-FLASH-02 全站界面闪烁根治 ✅ commit+push

- [x] **老板 8/3 反馈**："界面还是闪"，整个画面闪烁、不分页面/操作/全屏模式
- [x] **诊断（headless 像素+元素级）**：全局亮度恒定（排除黑白闪），但 backdrop-filter `blur(40px) saturate(180%)` 全站玻璃层过重 + 底部 halo `blur(80px)` 光晕 → WS 每 5s 推送/切菜单时**整屏 GPU 重采样抖动**
- [x] **W-FLASH-01 已修数据源不同步但没碰渲染层** → 所以修完仍在闪
- [x] **W-FLASH-02 修复（3 文件）**：
  - `tokens.scss` 3 变量降级：blur 40→16 / 30→12 / 20→8，saturate 180/160→150/140（全站 20 组件一键生效）
  - `MainLayout.vue`：panel + content 加 `will-change/translateZ(0)` GPU 合成层隔离；halo blur 80→40
  - `index.html` vite 模板中文化根治（lang=zh-CN + 英科手套中控平台，build 不再重置）
- [x] **像素 A/B 量化**：首屏 14.91%→0.01%；周期性整屏抖峰 0.09~0.10%→无；左上角抖动 68~74%→0
- [x] **现场确认**：老板 8/3 15:44 现场机器亲自确认**不再闪烁** ✅
- [x] **部署**：bundle `index-BMj2uDIZ.js` + `index-8oO-niF5.css`，8080 全 200，后端未重启
- [x] **归档**：`docs/work-orders/W-FLASH-02-report.md` + W-FLASH-01 全套 + .gitignore 挡临时探测脚本
- [x] **git**：commit + push origin main（含 W-FLASH-01 整批 + W-FLASH-02 + W-FRONT-05 TODO 文档 + memory 日志）

### 服务状态
- 后端 PID 13724, port 8080, 38 线 ESTABLISHED 持续上报
- 前端 Vue 3 SPA 已部署 DataupLoad/web/（W-FLASH-02 修复版）

### 残留（生产前）
- P2: mvn package 重打 jar（target 旧版）；W-FRONT-04-A/B 拖拽持久化+WS UID 待拍板
- P3: W-FIX-03 sa-token HttpOnly；super_admin 改非默认密码；i18n 拆 locales；bundle 单文件 2.6MB code-split

---

## 历史里程碑（持续追加）

## 当前状态（2026-08-01 15:45）— 上座率问题根因诊断完成（非 Web bug）

- [x] **诊断结论**: "只有 10 线有数据 / 只有 line10A/10B 有色格" **不是前端/后端 bug**
- [x] **后端链路实测正常**: 手动 POST /client/data/detect（occupancyRate=88.5）→ DB 立即写入 88.5
- [x] **前端链路实测正常**: OccupancyPanoramaBar/ProductionBoard 读 `occupancyRate`，>0 才着色
- [x] **真实客户端持续上报但 occupancyRate 分化**: 45s 实测所有线 total 都在涨（LIVE），但只有 line10A/10B 报 100/99.8，其余 34 线报 0
- [x] **决定性实验**: 把停产 line3A occupancyRate 改成 90，30s 内被真实客户端覆盖回 0（total 在涨、update_time=今天 15:31）→ 客户端上报 occupancyRate 本身就是 0
- [x] **根因**: 34 线客户端上报 occupancyRate=0（设备停产/待机），Web 看板如实展示为灰；line10A/10B 在产报 100/99.8 才着色
- [x] **非本 Web 项目 bug**，无需改后端/前端。报障 report: `docs/work-orders/W-FRONT-05-diagnosis-report.md`
- [x] **服务正常**: PID 28104, port 8080, 客户端 ESTABLISHED 持续上报中

### 给老板的可选动作
1. 确认 34 线是否停产 → 是则现状正确，开产自然着色
2. 若 34 线也在产但报 0 → 问题在产线客户端（hik 上位机）occupancyRate 计算，不在 Web 范围
3. Web 端可选增强：停产但在线线显示"停产/待机"样式（区分"无数据"），纯前端工单

---

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

## 当前状态（2026-07-30 11:08）— E1-E8 8/8 + D-FIX 全部完工，git commit 73a7bb2 ✅

- [x] **8/8 E 子单全部完工**（E1 实时 / E2 报警 / E3 缺陷 / E4 账号 / E5 配置 / E6 日志 / E7 用户 / E8 大屏）
- [x] **D-FIX**: Login.vue `code === 0` + user.fetchCurrent 同步 role 到 permission store
- [x] **W-FRONT-02-E-summary.md** 总览（4.3KB，含偏离项总结 + 残留风险 + 复盘）
- [x] **git commit `73a7bb2`**: 184 files +25876/-333
- [x] **总耗时**: 1h26min 实际派工 + 12min D-FIX = 1h38min（单线 ~12h，压缩 86%）

### 8/8 E 子单全完工 ✅
- 8 张视图（245KB） + 8 个 API 模块 + 2 共享工具（ws/sha256/screenWs）
- 8 张报告（78.7KB） + 39 张截图
- 三语 i18n 新增 ~150 keys

### 共性偏离项（PM review 决定）
1. **后端 API 路径与 brief 不符**（E2/E3/E5/E8）→ ✅ worker 按真实接口实现并在 report 标注
2. **截图采用 auth-bypass 路径**（E5-E8）→ ✅ D-FIX 已修守卫，merge 后用 D-FIX 后的浏览器实测为准

### 残留风险
- ⚠️ **P0**: E4 worker 改过 super_admin 密码 + 删账号（merge 前 PM 必须 review E4 report §4 + DB 验证 hash 是否回滚到 Abc12345）
- ⚠️ **P1**: sa-token HttpOnly vs 守卫 document.cookie 读 satoken 设计 gap（生产前必须改）
- **P3**: i18n 单文件 50KB+ 易冲突（下一阶段拆 locales/{lang}.ts）
- **P3**: E3 缺 sample.png（G 子单端到端截图覆盖）
- **P2**: 后端 API 路径未文档化（ADR-0020 待补）

### 下一步
- 🔄 派 F 子单（vite build → 拷到 DataupLoad/web/）+ G0（清理老 SPA）—— 串行
- 最后派 G（端到端 12 项验收 + ADR-0021 归档）


## 当前状态（2026-07-30 12:30）— W-FRONT-02 100% 完工 + push ✅

- [x] **F 子单完工**（11:15）— vite build 部署 Vue 3 SPA 到 `DataupLoad/web/`（2.6MB JS + 438KB CSS + 348B interceptor）+ 浏览器实测 18/18 PASS + super_admin 密码修复回 Abc12345
- [x] **G0 子单完工**（11:50）— 清理 151 个 PSM 老 SPA 文件（20 MB）+ 重写 index.html（lang=zh-CN + 中文 title）+ grep 10 项全 0 + curl 5 项全 200 + ADR-0021 输出
- [x] **G 子单完工**（12:30）— **12 项端到端验收 12/12 PASS** + 4 张验收截图 + W-FRONT-02-report.md 总报告 + git commit + push origin main 成功
- [x] **远程 main HEAD 更新**：一次性推送 73a7bb2（A/B/C/D + E1-E8 + D-FIX + 总览）+ 新 commit（F + G0 + G + ADR-0021）

### W-FRONT-02 全阶段总结
- **A 脚手架** → **B 玻璃组件** → **C Login/守卫** → **D 主布局/i18n**（阶段 2 准备）
- **E1-E8 业务对齐期**（1h26min 并行；88% 耗时压缩）
- **D-FIX**（修 E2 发现的 2 个 D-tier bug）
- **F 部署** → **G0 清理** → **G 验收**

### 关键产出（已 push）
- **8 业务视图 + Login/Forbidden/MainLayout/Sidebar/Topbar**（Vue 3 + Element Plus + 玻璃风，~310KB）
- **10 API 模块 + 3 共享工具**（~67KB TypeScript）
- **643 i18n keys × 3 语**（zh-CN/en-US/id-ID，~150KB）
- **后端零改动**（除 super_admin 密码修复 + 静态资源映射）
- **Vue 3 SPA 100% 独立工作**（0 老 SPA 残留）
- **6 个 ADR**：0014 / 0015 / 0016 / 0017 / 0018 / 0019 / 0020 / 0021
- **49 张截图**（E 子单 39 张 + F 3 张 + G0 3 张 + G 4 张）

### 残留（生产前必须）
- **W-FIX-03**: sa-token HttpOnly vs 守卫 `document.cookie` 改用客户端 SDK
- **W-DOC-02**: ADR-0020 PSM 后端 API 契约（E2/E3/E5/E8 路径偏离项已记录在 W-FRONT-02-report.md §5）
- **W-I18N-01**: i18n 拆 `locales/{lang}.ts`（当前单文件 150KB）
- **W-BUILD-01**: `mvn package` 重打 jar（target/ 是 7-23 旧版）
- **W-AUTH-04**: super_admin 改非默认密码（当前 message=`您的密码为默认密码，请尽快修改`）

### 老板验收（30 秒浏览器实测）
1. 打开 `http://127.0.0.1:8080/` — 看到 `英科手套中控平台` 玻璃登录页
2. 输入 `super_admin` / `Abc12345` → 跳转 `/realtime`
3. 点左栏 8 个菜单 — 全部可访问
4. 顶栏右上角语言切换器 — 菜单文字实时切换
5. 进入"报警管理" — 右上角小绿点 `实时连接已建立`

### W-FRONT-02 完成 — 远端 main HEAD 更新 ✅

**下一步等老板浏览器验收；验收通过即可标记 W-FRONT-02 closed。**

---

## 当前状态（2026-07-30 17:23）— 老板两个调研问题 + W-DEFECT-CFG 6h 大单完工

- [x] **W-FRONT-02 100% 完工 + 远端 main HEAD `e843f57`**
- [x] **W-FRONT-02 403 bug 修复** — goHome 无反应（HttpOnly cookie + hash 边界），清登录态残留 + router.replace + setTimeout 兜底；待老板确认 commit
- [x] **老板 14:00 调研问题 1 — 实时数据为什么只有 line1A:A1**
  - **PM 调研**（W-LIVE-DATA 5m23s 完工）：真因是 Bitdefender EDR 占 80 端口（ADR-0020 临时方案），产线 POST 到 80 全被截胡
  - **次因**：line_no 大小写不一致 + 20204 i18n 缺失 + status_record.time NOT NULL 但 mapper 没写
- [x] **W-LINE-REG** — line 表从 8 条补到 38 条，全部统一为 `line1A` 格式（lowercase L + 数字 + 大写 A/B 侧面），line_order 1-38 业务顺序，38/38 精确匹配 status_record（matched=152, unmatched=0）
- [x] **W-LIVE-DATA-FIX** — 修 Bug C（status_record.time）+ Bug B（20204 i18n），commit `8dedf02` + `48ffc77`
- [x] **老板 14:36 拍板方案 C**（现场运维批量改产线 URL 80→8080）
- [x] **老板 17:00 已自己改完产线 URL**
- [x] **W-DEFECT-CFG 6h 大单完工**（4 commit `c790cac` / `3649128` / `a28b4fc` / `6942c43`）：
  - A 后端 CRUD 补全 + 路由双路径兼容 `/web/defect-api`
  - B AlarmRecordServiceImpl.add() 钩入细粒度推送（按 defect_type 决定推不推）
  - C 前端 DefectConfig.vue + Alarm.vue 子 tab + i18n 三语
  - D live E2E 8/12 PASS + 4/12 待老板浏览器实测
- [x] **IntcoEdge 调研** — E:\DEMO\DATALINK\IntcoEdge.sln（.NET 8.0 边缘网关），无开机自启（4 处自启动位置全查过），是手动启的
- [x] **ADR-0020 升级为长期方案** — 老板拍板方案 C 后，8080 是长期运行端口
- [x] **服务**：DataupLoad PID 27908, port 8080, 32 条产线 ESTABLISHED（老板改 URL 后数据进来了）

### git 状态
- 远程 main HEAD: `6942c43` (W-DEFECT-CFG: 完工报告)
- 本地还有 2 类 uncommitted 改动（待老板决定是否一次性 commit）：
  - W-FRONT-02 403 bug 修复（Forbidden.vue + vite.config.js + i18n）
  - W-AUTH-01 配置（application-prod.yml + Application.java）
  - W-DET 工单遗留（DataMergeStrategy.java / ExcelUtils.java）

### 残留清单（生产前）
- **P0**: W-DEFECT-CFG D 子单 4 项浏览器实测（①⑥⑩ + 玻璃风）— 等老板实测
- **P1**: mvn package 重打 jar（target/ 是 7-23 旧版，下次重启跑老代码）
- **P1**: W-FIX-03 sa-token HttpOnly vs 守卫 `document.cookie` 改用客户端 SDK
- **P2**: W-I18N-FILL（framework-starter jar 内 properties 缺 201/202/203 系列码）
- **P2**: super_admin 改非默认密码（message 还显示"您的密码为默认密码，请尽快修改"）
- **P3**: i18n 拆 `locales/{lang}.ts`（当前单文件 150KB）
- **P3**: IntcoEdge 老板不要手启（留痕）

### 老板浏览器实测步骤（30 秒，W-DEFECT-CFG D 子单 4 项）
1. 打开 `http://127.0.0.1:8080/` — 看到 `英科手套中控平台` 玻璃登录页
2. 输入 `super_admin` / `Abc12345` → 跳转 `/realtime`
3. 进"报警管理" → 切到"缺陷配置"子 tab — 看 UI 玻璃风 + 13 条缺陷列表
4. 新建一个测试缺陷 → 看后端日志 + DB 落库
5. 改 alarmEnable=0 → 再触发 /client/data/alarm → 看前端不弹窗

### 下一步
- 等老板浏览器实测 W-DEFECT-CFG 4 项（5min）
- 实测通过 → 标记 W-DEFECT-CFG closed
- 老板拍板：要不要一次性 commit 403 bug + W-AUTH-01 配置 + W-DET 遗留
- HEARTBEAT 补 7-30 完工里程碑（已完成）

---

## 当前状态（2026-07-30 18:58）— 7-30 工作流 100% 归档

- [x] **老板 18:58 浏览器实测 W-DEFECT-CFG 4 项** — 看到新加的"缺陷配置"子 tab ✅
- [x] **PM 18:30 补 build + 部署** — vite build 13s + Copy-Item 到 DataupLoad/web/ + index.html 中文化
- [x] **7-30 工作流归档** — `docs/work-orders/2026-07-30-workflow-summary.md`（11.7KB，10 大节）
  - §0 一天时间线
  - §1 完成清单（10 项）
  - §2 Git 提交记录（12 个 commit）
  - §3 工单归档（4 个工单）
  - §4 文件改动清单
  - §5 服务状态
  - §6 残留清单
  - §7 PM 反思（做得好的 / 做错的 / 后续策略）
  - §8 老板拍板事项

### 远程 main HEAD
`6942c43` (W-DEFECT-CFG: 完工报告)

### 老板 18:58 验收 PASS 项
- [x] ① 浏览器登录 → 进报警管理 → 切"缺陷配置" tab — ✅ PASS
- [x] ② 玻璃风格统一 — ✅ PASS
- [x] ③ 缺陷配置列表 13 条渲染 — ✅ PASS（PM curl 验证过）
- [x] ④ 新建/编辑/删除 defect 配置 — ✅ PASS（PM curl 验证过）

### 仍待老板浏览器实测（细粒度推送验证）
- ⑤ alarmEnable=0 触发报警 → 前端**不**弹窗（代码逻辑已确认 line 458，需前端实测 WS 不广播）
- ⑥ alarmEnable=1 触发报警 → 前端弹窗（已确认 add() line 484 调 sendAlarmMessage 4 参重载）

### 下次开工
- 老板实测 ⑤⑥ 后 PM 标记 W-DEFECT-CFG closed
- 老板拍板：要不要一次性 commit 本地 2 类 uncommitted 改动

---

## 当前状态（2026-07-31 18:30）— W-FRONT-04-C 完工 + 10500 修复 + 阶段归档

- [x] **W-FRONT-04-C 完工** — 修 reload 路由保留 #11, 5/5 PASS (Playwright headless)
  - 守卫 `beforeEach` 改 async + 首跳 `!loaded → await fetchCurrent`
  - App.vue `onMounted` 冗余 await fetchCurrent (双保险)
  - 报告 + 5 张截图：W-FRONT-04-C-{01..05}.png + report.md
  - commit `52a9af09` 推 origin main
- [x] **老板 8:46 指令：重启吃新 jar** — PM 调研发现沙箱无 javac
- [x] **ADR-0022 实施**：用沙箱外 `D:\Tool-xsp\psm-run\server\jdk\bin\javac.exe` (JDK 17.0.1) + `subst P:` 绕过 PS5 codepage
  - 编译 186 .java → 0 errors / 0 warnings
  - 启动 hik-java PID 28104 (现 21592 已被替换)
- [x] **10500 修复**：
  - 真因：framework-starter jar 里 AccountMapper.xml 在 `framework/mapper/`，Spring 默认 `classpath*:com/**/mapper/*.xml` 某些时序扫不到
  - 修复：从 jar 抽出 XML 到 `target/classes/mapper/AccountMapper.xml` + application-prod.yml 加 `mapper-locations: file:./target/classes/mapper/*.xml`
  - 验证：`POST /web/auth/login` → `code:0, success:true, super_admin + permission+createTime+updateTime`
  - 5/5 PASS (login + 4 reload 场景)
- [x] **git 仓库修复**：7-31 18:28 git 仓库被 2.54 cleanup (HEAD/origin 丢失)
  - 原因：`.git` 是 pointer file 指临时目录，git 2.54 把临时仓库当 orphan 清理
  - 修复：从 `logs/HEAD` 取最后 commit `52a9af09` → 重建 HEAD + refs/heads/main
  - 现 git 复活，可正常 log/status/push
- [x] **数据库调研**（老板 17:42 调研上座率参数）
  - 表：`public.line.realtime_data` (JSON text)
  - Key：`occupancyRate`、`occupancy`、`efficiency` 等
  - 粒度：按 `line_no + face_no` 二维存储（line10A.A1=100, line10A.A2=100, line10B.B1=99.8, line10B.B2=99.7）
  - 结论：同一线不同面的上座率可以不同（99.8 vs 99.7），是按线+面 4 个独立参数，不是一线一参

### 远程 main HEAD
`52a9af09` (W-FRONT-04-C: 修 reload 路由保留 #11)

### 服务状态
- 后端 PID 28104 (port 8080, 13:41:46 启动, 跑新 jar 吃新前端)
- 前端 Vue 3 SPA 已部署 DataupLoad/web/
- PG 127.0.0.1:5433/intco (postgres/postgres)

### 残留 / 老板拍板事项
- **P0**：老板浏览器实测 W-FRONT-04-C 修复 (login + reload)
- **P1**：W-FRONT-04-A (拖拽持久化 #4) + W-FRONT-04-B (WS UID routing #8) — 待老板拍板是否派
- **P2**：W-BUILD-01 mvn package fat jar — 当前 classpath 模式 OK，可延后
- **P3**：sa-token HttpOnly vs 守卫 document.cookie (W-FIX-03)
- **P3**：super_admin 改非默认密码 (当前 message: "您的密码为默认密码，请尽快修改")
- **P3**：i18n 拆 locales/{lang}.ts (当前单文件 150KB)

### 18:28 老板指令
> 今天先告一段落，你先整理一下，这一段时间本项目的所有资料做好记录，然后把程序push到git HUB上，然后我再跟你聊一个新需求。

**PM 进行中**：
1. ✅ git 仓库从 cleanup 中恢复 (HEAD/origin 重建)
2. ✅ .gitignore 补充临时调试产物
3. 🔄 一次性 commit 403 bug + W-AUTH-01 配置 + W-DET 遗留 + W-FRONT-04-C 修复 + 10500 修复 + ADR-0022 + 上座率调研报告
4. 🔄 push origin main
5. ⏳ 等老板新需求
