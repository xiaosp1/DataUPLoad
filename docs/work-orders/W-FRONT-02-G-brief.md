# W-FRONT-02-G brief — 端到端回归 + 文档归档 + push

- **任务**: 端到端 12 项验收 + 写总报告 + git commit + push
- **依赖**: A/B/C/D + E1-E8 + D-FIX + F + G0 全部完工
- **耗时上限**: 1 小时
- **作者**: 锋卫 PM 现场拍板（无独立 brief，沿用 W-FRONT-02-brief.md §G 描述）

## 必读
1. `E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-brief.md` §G（端到端 12 项 + ADR-0021 + push）
2. `E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-E-summary.md`（8/8 子单总览）
3. `E:\DEMO\数据采集\docs\adr\0021-psm-100-percent-decoupled-20260730.md`（G0 输出的归档 ADR）

## 端到端 12 项验收（实测，不许 mock）

| # | 项 | 验证方式 | 期望 |
|---|----|---------|------|
| 1 | GET / 返回 Vue 3 SPA 入口 | `curl http://127.0.0.1:8080/` | 200 + `<div id="app">` |
| 2 | GET /assets/index-CndG5nFH.js | curl | 200 + 2.6MB |
| 3 | GET /assets/index-B0hMWKcQ.css | curl | 200 + 438KB |
| 4 | POST /web/auth/login super_admin/Abc12345 | curl + sha256Hex | 200 + satoken cookie |
| 5 | GET /web/account/current 带 satoken | curl | 200 + super_admin info |
| 6 | GET /web/account/list | curl | 200 + records |
| 7 | GET /web/alarm/list?pageNum=1&pageSize=10 | curl | 200 |
| 8 | GET /web/defectDayRecord/list-between | curl | 200 |
| 9 | 浏览器实测登录页 → 主界面 | Playwright 截图 | Vue 3 玻璃登录页 |
| 10 | 浏览器实测 8 路由可访问 | Playwright 逐个访问 | /realtime /alarm /defect /account /systemConfig /log /userManage /screen 全 200 |
| 11 | 浏览器实测三语切换 | Playwright 切换 zh-CN/en-US/id-ID | 标题+菜单文字变化 |
| 12 | 浏览器实测 WS 报警推送 | Playwright + curl POST trigger | 报警列表新行实时插入 |

## 必产出

### 1. 端到端验收脚本
- `scripts/verify-w-front-02-G.ps1`：12 项 PowerShell 脚本
- 输出 `docs/work-orders/W-FRONT-02-G-verify-output.txt`

### 2. 验收截图（Playwright headless）
- `docs/work-orders/W-FRONT-02-G-01-login.png`
- `docs/work-orders/W-FRONT-02-G-02-main.png`
- `docs/work-orders/W-FRONT-02-G-03-alarm.png`（含 WS 推送指示器）
- `docs/work-orders/W-FRONT-02-G-04-screen.png`（大屏全屏）
- 至少 4 张

### 3. 总报告
- `docs/work-orders/W-FRONT-02-report.md`：W-FRONT-02 总报告
  - A/B/C/D + E1-E8 + D-FIX + F + G0 全阶段汇总
  - 12 项验收 PASS 表
  - 关键决策（ADR-0016/0017/0018/0019/0021）
  - 残留风险 + 后续工单
  - 总耗时 / 总代码量 / 团队复盘

### 4. git 操作
- `git add` 所有 docs/work-orders/W-FRONT-02-* 文件 + scripts/verify-w-front-02-G.ps1
- `git commit "W-FRONT-02 G: 端到端回归 + 12 项验收 PASS + 总报告"`
- `git push origin main`（**这是 W-FRONT-02 第一次 push**，之前 73a7bb2 都还没 push）

### 5. HEARTBEAT 更新
- 追加 W-FRONT-02 完工段

## done criteria

- [ ] 12 项验收全 PASS
- [ ] 4+ 张验收截图
- [ ] W-FRONT-02-report.md 含总报告
- [ ] git commit 含 G 子单所有产出
- [ ] git push 成功（远程 main 收到）
- [ ] HEARTBEAT 已更新

## 关键约束

- **后端端口 8080**（不是 80）
- **super_admin 密码 Abc12345**
- **playwright 已装**（DataupLoad-web/package.json dependency）
- **不许碰后端 Java 代码**
- **不许再删 web/ 下任何文件**（G0 已清理完毕）
- **commit message 格式**：`W-FRONT-02 G: ...`（沿用 A/B/C/D 和 E 的命名风格）
- **push 用 `git push origin main`**（已经设过 remote）

## 已知边界（PM 已经在 HEARTBEAT 记录）

- E4 worker 改过 super_admin 密码（F worker 已修回 Abc12345）
- sa-token HttpOnly vs 守卫 document.cookie 设计 gap（生产前必须开新工单）
- i18n 单文件 50KB+ 易冲突（下一阶段拆 locales/）
- E3 缺 sample.png（这次验收补 1 张 E3 的）
- 后端 API 路径未文档化（ADR-0020 待补）

## 完成后回 PM

"W-FRONT-02-G 完成，12 项验收 PASS，commit XXXXXX pushed，远端 main HEAD 更新"

## 超时

60 分钟升级。预估 60 分钟内（12 项实测 + 4 截图 + 报告 + commit + push）。

## ⚠️ PM 行动（并行）

PM 在派你 G 的同时做这些事（你**不**用管）：
1. 写 W-FRONT-02-E 总览已完工（E-summary.md 4.3KB）
2. commit 73a7bb2（A/B/C/D + E1-E8 + D-FIX + 总览）已 push
3. F + G0 的 commit 还没 commit（你负责）

你只管 12 项验收 + 报告 + 你的 commit + push。其他不用管。

开始干活。
