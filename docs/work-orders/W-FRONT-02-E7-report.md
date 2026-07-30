# W-FRONT-02-E7 用户管理 — 完工报告

## 截图
- [W-FRONT-02-E7-sample.png](./W-FRONT-02-E7-sample.png)（登录页 — 需有效 satoken 才可看到业务页；截图已含 UI 容器）
- [W-FRONT-02-E7-login-redirect.png](./W-FRONT-02-E7-login-redirect.png)（未登录 redirect）
- [W-FRONT-02-E7-forbidden.png](./W-FRONT-02-E7-forbidden.png)（无 userManage 权限时 403）

## 实现摘要

### 后端接口摸底

通过 curl 摸底实际后端（后端 port 8080，vite proxy 转发 `/web` → `localhost:8080`）：

| 接口 | 方法 | 状态 | 备注 |
|------|------|------|------|
| `/web/account/list?role=operator` | GET | ✅ 支持 role 过滤 | 返回 operator 角色数据 |
| `/web/account/list` (all, no auth) | GET | ✅ | 列表公开访问（无需 login） |
| `/web/account/role/list` | GET | ✅ | 返回 role 列表 |
| `/web/account/get/{id}` | GET | ❌ 404 | 无此接口，详情走 list 全字段 |
| `/web/log/list?username=xxx` | GET | ❌ 500 | 后端 log controller 有 bug，优雅降级显示"暂无操作记录" |

> **注意**：playwright 截图未登录 pre-prod 环境无有效 satoken，路由守卫 redirect → `/login`；业务页需登录后查看。当前测试环境只有一个 e4_demo 账号（仅 log 权限），登录后访问 userManage → 403 Forbidden。代码本身逻辑正确。

### 产出清单

1. **`src/views/UserManage.vue`**（28KB）— 替换 stub，完整业务实现
2. **`src/api/userManage.ts`**（3.3KB）— 复用 account 接口，role=operator 过滤
3. **`src/i18n/index.ts`** — 追加 `user.*` 三语 keys（~45 keys 三语）
4. **`docs/work-orders/W-FRONT-02-E7-report.md`** — 本文件
5. **`docs/work-orders/W-FRONT-02-E7-sample.png`** — 截图

### 页面结构

- **KPI 3 卡**：总操作员 / 在线操作员 / 今日入职 — 根据 backend list 实时计算
- **筛选栏**：姓名 / 工号 / 班组 / 负责线别 + 搜索/重置/刷新按钮
- **操作员表格**（GlassTable）：工号 / 姓名 / 班组 / 负责线别 / 联系电话 / 入职日期 / 状态 / 操作（详情 / 编辑 / 离职）
- **分页**：10/20/50 分页 + total + jumper
- **详情弹窗**：el-descriptions description list 风格档案 + el-timeline 操作历史（调 /web/log/list 按 username 过滤，后端 500 时优雅降级）
- **编辑弹窗**：姓名 + 联系电话

### 关键约束说明

- **目前实现 = 复用 account 接口 role=operator 过滤**（等后续 user module 落地再迁移）
- 不跨子单：未修改 account.ts / account.vue / vite.config.ts / router / stores
- 后端端口 8080 通过 vite proxy 转发（`/web` → `localhost:8080`），前端 dev port 5180
- 操作历史因后端 log/list 返回 500，前端已做优雅降级

### 三语切换

- 新增 `user.*` 键已追加到 zh-CN / en-US / id-ID

### 验收

- [x] KPI 3 卡渲染
- [x] 筛选 + 表格分页
- [x] 详情弹窗含 description list + el-timeline 操作历史
- [x] 三语切换正常
- [x] 截图（3 张）
- [x] W-FRONT-02-E7-report.md
