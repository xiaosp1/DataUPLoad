# W-FRONT-01 — 前端对齐 PSM SPA（延后执行）

- **状态**: 延后（**不阻塞登录修复**）
- **延后原因**: 老 PSM 前端 Login.vue 在浏览器无法触发登录请求（路由挂载 / 事件绑定问题），W-FRONT-01-A~E 子单 2026-07-25 14:30 派工后无 report 回执，新前端未接管
- **优先级**: P2（前端体验优化，不影响核心业务）
- **延后触发**: 老板指令 2026-07-27 12:41 — 先用方案 X（最小 login.html 替代），Y 改天再推
- **重启动条件**:
  - 方案 X 登录链路稳定后，再开新工单 W-FRONT-02 重启 A→E 流程
  - 每个子单必须 PM 验收过再下下一张，避免再卡

## 子单待重派

| 单号 | 任务 | 状态 |
|------|------|------|
| W-FRONT-01-A | Vite+Vue3+ElementPlus+Router+Pinia 脚手架 | 待重派（需要 PM 盯紧 npm install + dev 启动） |
| W-FRONT-01-B | Login.vue + 路由守卫 + satoken 集成 | 待派 |
| W-FRONT-01-C | i18n 三语 + 切换 UI | 待派 |
| W-FRONT-01-D | vite build → static/ + jar 重打包 + 重启 | 待派 |
| W-FRONT-01-E | 端到端 12 项验收 + verify 脚本 | 待派 |

## 已留痕 ADR
- ADR-0016: 前端对齐 PSM SPA（Vue 3 + Element Plus + Vite）
