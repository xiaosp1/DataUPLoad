# W-FRONT-02-E4 brief — 账号管理

- **任务**: 实现 `/account` 业务页：账号 CRUD + 当前用户信息
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/Account.vue`（替换 stub）

页面结构：
- **顶部当前用户卡**（GlassCard）：
  - 用户名 / 角色 / 权限 tags / 最后登录时间
  - 修改密码按钮（弹窗）
- **账号列表表格**（GlassTable）：
  - 列：ID / 用户名 / 角色 / 权限 / 创建时间 / 状态（启用/禁用） / 操作（编辑 / 重置密码 / 启用-禁用 / 删除）
  - 分页 + 搜索
  - 新增账号按钮（弹窗表单）
- **新增/编辑弹窗**（el-dialog + el-form）：
  - 用户名 / 角色下拉（super_admin / admin / operator / viewer）
  - 权限多选
  - 密码（仅新增时必填）
- **修改密码弹窗**：
  - 旧密码（sha256 前端 hash） / 新密码 / 确认密码
  - 提交

### 2. `DataupLoad-web/src/api/account.ts`

```ts
import http from './http'

// 当前用户
export const getCurrent = () => http.get('/web/account/current')

// 列表
export const listAccount = (params: { pageNum: number; pageSize: number; username?: string }) =>
  http.get('/web/account/list', { params })

// 详情
export const getAccount = (id: number) => http.get(`/web/account/get/${id}`)

// 新增（重要：密码是 bcrypt(sha256Hex(明文))，前端只传 sha256Hex）
// 后端会再 bcrypt 一次（ADR-0014）
export const addAccount = (data: { username: string; password: string; role: string; permission: string[] }) =>
  http.post('/web/account/add', { ...data, password: sha256Hex(data.password) })

// 编辑
export const editAccount = (data: { id: number; role: string; permission: string[]; status: number }) =>
  http.post('/web/account/edit', data)

// 重置密码（同 add：传 sha256Hex）
export const resetPwd = (id: number, newPwd: string) =>
  http.post(`/web/account/resetPwd/${id}`, { password: sha256Hex(newPwd) })

// 启用/禁用
export const toggleStatus = (id: number, status: number) =>
  http.post(`/web/account/status/${id}`, { status })

// 删除
export const deleteAccount = (id: number) =>
  http.post(`/web/account/delete/${id}`)
```

### 3. `DataupLoad-web/src/utils/sha256.ts`（已有则跳过）

```ts
// 用 crypto-js（已在 dependency 的话）或浏览器原生
// 若没有 crypto-js，改用 Web Crypto API：
export async function sha256Hex(text: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
}
```

> ⚠️ **重要**: 后端 `add`/`resetPwd` 流程是 `bcrypt(sha256Hex(明文))`（双重哈希，ADR-0014），前端**只**传 sha256Hex 即可。
> 但 `resetAdminPwd`（super_admin 走这条）是单 bcrypt → 前端传**明文**。登录用 `bcryptCheck` 是单 bcrypt → 前端**只**传 sha256Hex。
> 见 docs/adr/0014-password-hash-double-bcrypt.md

### 4. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `account.title` | 账号管理 | Account Management | Manajemen Akun |
| `account.current.title` | 当前用户 | Current User | Pengguna Saat Ini |
| `account.current.username` | 用户名 | Username | Nama Pengguna |
| `account.current.role` | 角色 | Role | Peran |
| `account.current.permission` | 权限 | Permission | Hak Akses |
| `account.current.lastLogin` | 最后登录 | Last Login | Login Terakhir |
| `account.current.changePwd` | 修改密码 | Change Password | Ubah Sandi |
| `account.table.id` | ID | ID | ID |
| `account.table.username` | 用户名 | Username | Nama Pengguna |
| `account.table.role` | 角色 | Role | Peran |
| `account.table.permission` | 权限 | Permission | Hak Akses |
| `account.table.createdAt` | 创建时间 | Created | Dibuat |
| `account.table.status` | 状态 | Status | Status |
| `account.table.action` | 操作 | Action | Aksi |
| `account.status.active` | 启用 | Active | Aktif |
| `account.status.disabled` | 禁用 | Disabled | Nonaktif |
| `account.add.title` | 新增账号 | Add Account | Tambah Akun |
| `account.edit.title` | 编辑账号 | Edit Account | Edit Akun |
| `account.form.username` | 用户名 | Username | Nama Pengguna |
| `account.form.password` | 密码 | Password | Sandi |
| `account.form.role` | 角色 | Role | Peran |
| `account.form.permission` | 权限 | Permission | Hak Akses |
| `account.pwd.title` | 修改密码 | Change Password | Ubah Sandi |
| `account.pwd.old` | 旧密码 | Old Password | Sandi Lama |
| `account.pwd.new` | 新密码 | New Password | Sandi Baru |
| `account.pwd.confirm` | 确认密码 | Confirm Password | Konfirmasi Sandi |
| `account.action.add` | 新增 | Add | Tambah |
| `account.action.edit` | 编辑 | Edit | Edit |
| `account.action.resetPwd` | 重置密码 | Reset Password | Reset Sandi |
| `account.action.toggle` | 启用/禁用 | Toggle | Toggle |
| `account.action.delete` | 删除 | Delete | Hapus |

### 5. `docs/work-orders/W-FRONT-02-E4-report.md`

- 截图 + 当前用户卡截图
- 新增弹窗截图
- 修改密码弹窗截图
- 三语截图
- **特别注意**: 验证新增用户**能登录**（用新账号 POST /web/auth/login 看 200）

## done criteria

- [ ] 当前用户卡渲染
- [ ] 账号列表分页 + 搜索
- [ ] 新增弹窗表单校验
- [ ] 新增后用新账号能登录（**关键**）
- [ ] 编辑 / 重置密码 / 启用禁用 / 删除按钮都可点击
- [ ] 修改密码弹窗校验（旧密码错 → 提示 / 新密码两次不一致 → 提示）
- [ ] 状态 tag 颜色（绿=启用 / 灰=禁用）
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E4-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/account/current -b "satoken=xxx"
curl http://localhost:80/web/account/list?pageNum=1&pageSize=10 -b "satoken=xxx"
```

> ⚠️ super_admin 登录后才能拿到 satoken。先用 PM 给的 satoken 测（自测时浏览器登录拿 cookie）。

## 禁止

- 不许尝试暴力破解或绕过密码 hash（ADR-0014 已 fix）
- 不许明文传密码（必须 sha256Hex）
- 不许直接调 bcrypt（前端无 bcrypt 库）

