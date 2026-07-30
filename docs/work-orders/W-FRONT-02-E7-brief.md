# W-FRONT-02-E7 brief — 用户管理

- **任务**: 实现 `/userManage` 业务页：与 E4 区分视角（E4=系统账号 / E7=车间操作员档案）
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 设计定位

- **E4（账号管理）**: 系统登录账号，关心 `username/password/role/permission`，影响能否登入
- **E7（用户管理）**: 车间操作员档案，关心 `姓名/工号/班组/负责线别/入职日期`，纯业务档案

> PSM 老 SPA 中"账号/用户"是双 tab 区分。本工单按此区分。若后端目前没拆 user 表，**先复用 account 接口 + role=operator 过滤**，等后续 user module 落地再迁移。worker 完工后在 report 标注"目前实现 = 复用 account 过滤"。

## 关键产出

### 1. `DataupLoad-web/src/views/UserManage.vue`（替换 stub）

页面结构：
- **顶部统计卡**（3 个 GlassCard）：
  - 总操作员数
  - 在线操作员数（30 分钟内有操作）
  - 今日新入职数
- **筛选栏**（GlassCard）：
  - 姓名 / 工号 / 班组 / 负责线别
- **操作员表格**（GlassTable）：
  - 列：工号 / 姓名 / 班组 / 负责线别 / 联系电话 / 入职日期 / 状态 / 操作（详情 / 编辑 / 离职）
  - 分页
- **详情弹窗**（el-dialog）：
  - 完整档案（用 description list 风格）
  - 操作历史时间线（用 el-timeline，调 log 接口按 username 过滤）

### 2. `DataupLoad-web/src/api/userManage.ts`

```ts
import http from './http'

// 目前复用 account，按 role 过滤
export const listUser = (params: { pageNum: number; pageSize: number; username?: string; role?: string }) =>
  http.get('/web/account/list', { params })

// 后续若有 user 表替换此
export const getUserDetail = (id: number) => http.get(`/web/account/get/${id}`)

export const editUser = (data: { id: number; realName: string; workNo: string; shift: string; phone: string }) =>
  http.post('/web/account/editProfile', data)  // 若后端无此接口 → 降级用 edit
```

### 3. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `user.title` | 用户管理 | User Management | Manajemen Pengguna |
| `user.kpi.total` | 总操作员 | Total Operators | Total Operator |
| `user.kpi.online` | 在线操作员 | Online Operators | Operator Online |
| `user.kpi.newToday` | 今日入职 | New Today | Baru Hari Ini |
| `user.filter.name` | 姓名 | Name | Nama |
| `user.filter.workNo` | 工号 | Work No | No. Kerja |
| `user.filter.shift` | 班组 | Shift | Shift |
| `user.filter.line` | 负责线别 | Assigned Line | Lanes |
| `user.table.workNo` | 工号 | Work No | No. Kerja |
| `user.table.name` | 姓名 | Name | Nama |
| `user.table.shift` | 班组 | Shift | Shift |
| `user.table.line` | 负责线别 | Line | Lanes |
| `user.table.phone` | 联系电话 | Phone | Telepon |
| `user.table.hireDate` | 入职日期 | Hire Date | Tgl Masuk |
| `user.table.status` | 状态 | Status | Status |
| `user.detail.title` | 档案详情 | Profile Detail | Detail Profil |
| `user.detail.history` | 操作历史 | History | Riwayat |

### 4. `docs/work-orders/W-FRONT-02-E7-report.md`

- 截图 + 详情弹窗截图 + 操作历史时间线截图
- 三语截图
- **明确标注**: 当前是复用 account 接口（role=operator 过滤）

## done criteria

- [ ] KPI 3 卡渲染
- [ ] 筛选 + 表格分页
- [ ] 详情弹窗含 description list + el-timeline 操作历史
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E7-report.md

## 后端 API 自测

```powershell
# 复用 account 接口
curl http://localhost:80/web/account/list?pageNum=1&pageSize=10
```

## 禁止

- 不许强依赖 user module（目前不存在，按"复用 account"实现）
- 不许碰路由
- 不许改后端

