// =============================================================================
// W-FRONT-02-E4 账号管理 API（按真实后端 controller 路由写，非 brief 假设）
//
// ⚠️ 重要：实际后端接口与 W-FRONT-02-E4 brief 列出的 URL **不一致**。
// 本文件按 framework-starter AccountController 字节码 (starling 2.2.3-SNAPSHOT)
// 实际暴露的 endpoint 写：
//
//   list            GET    /web/account/list
//   getCurrent      GET    /web/account/current
//   add             POST   /web/account           body: AccountBodyDTO
//   mod             PUT    /web/account           body: AccountChgDTO
//   del             DELETE /web/account?id=X
//   changePwd       PUT    /web/account/pwd       body: AccountPwdDTO（sha256Hex）
//   changeInfo      POST   /web/account/info      body: AccountInfoDTO（sha256Hex）
//   resetPwd        POST   /web/account/pwd-reset body: { id }（仅重置，无新密码）
//   getMachineSN    GET    /web/account/serial-no
//   verifySerialNum POST   /web/account/verify-no
//   resetAdminPwd   POST   /web/account/pwd/admin body: AccountPwdDTO（sha256Hex）
//   listRoles       GET    /web/account/role/list
//
// 与 brief 差异（不能改 backend，记录下来）：
//   - brief 的 /add /edit /resetPwd/:id /status/:id /delete/:id 全部不存在
//   - 真实 controller 用 HTTP verb 区分 add(POST) / mod(PUT) / del(DELETE)
//   - add/mod body 用 roleId（FK to role 表），不是 role 字符串
//   - 账号无 status 字段（无启用/禁用），add 时也无 password 字段（后端默认生成 hash）
//   - resetPwd 只传 {id}，不能直接设新密码（调用者默认走 resetAdminPwd 流程）
//
// 密码 hash 约定（ADR-0014）：
//   - 所有 password 字段前端都先 sha256Hex
//   - 后端按 endpoint 决定：add/resetPwd 二次 bcrypt，changePwd/resetAdminPwd 单 bcrypt
//   - 前端不管具体流程，统一 sha256Hex 即可
//
// 返回结构：所有方法返回 `Promise<ApiEnvelope<T>>`，其中 envelope.data 是后端的实际 payload。
// 调用方拿 `envelope.data` 即可，**不要**再 .data 一次。
// =============================================================================

import http from './http'
import { sha256Hex } from '../utils/sha256'

// ---------------------------------------------------------------------------
// 后端响应包络
// ---------------------------------------------------------------------------
export interface ApiEnvelope<T = unknown> {
  success: boolean
  code: number
  msg?: string
  message?: string
  data: T
}

// ---------------------------------------------------------------------------
// 类型定义（与后端 DTO 对齐）
// ---------------------------------------------------------------------------

/** 当前用户 / 列表行通用字段（来自 AccountDTO，但 list 接口不回 password） */
export interface AccountInfo {
  id: number
  username: string
  realName?: string | null
  contactInfo?: string | null
  role: string
  permission?: string[]
  createTime?: string
  updateTime?: string
}

/** 当前用户（更窄的子集） */
export interface CurrentUser {
  id: number
  username: string
  role: string
  permission?: string[]
  createTime?: string
  updateTime?: string
}

/** 角色（来自 GET /web/account/role/list） */
export interface RoleInfo {
  id: number
  role: string
  permission?: string[]
  createTime?: string
  updateTime?: string
}

/** 新增账号 body（AccountBodyDTO） */
export interface AccountBodyDTO {
  username: string
  roleId: number
  realName?: string
  contactInfo?: string
}

/** 编辑账号 body（AccountChgDTO） */
export interface AccountChgDTO {
  id: number
  roleId: number
  realName?: string
  contactInfo?: string
}

/** 修改密码 body（AccountPwdDTO） */
export interface AccountPwdDTO {
  oldPassword: string  // sha256Hex
  password: string     // sha256Hex
  confirmPwd: string   // sha256Hex
}

/** 分页查询（AccountQuery） */
export interface AccountQuery {
  name?: string
  pageNum?: number
  pageSize?: number
  startTime?: string
  endTime?: string
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
  orders?: any[]
  optimizeCountSql?: boolean
  searchCount?: boolean
}

// ---------------------------------------------------------------------------
// 内部 helper：unwrap axios envelope
// ---------------------------------------------------------------------------
async function unwrap<T>(p: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const r = await p
  return r.data
}

// ---------------------------------------------------------------------------
// API 调用（统一返回 ApiEnvelope<T>）
// ---------------------------------------------------------------------------

/** 拉取当前登录用户 */
export const getCurrent = () =>
  unwrap<CurrentUser>(http.get('/web/account/current'))

/** 分页列表 */
export const listAccount = (params: AccountQuery) =>
  unwrap<PageResult<AccountInfo>>(http.get('/web/account/list', { params }))

/** 角色下拉（用于 add/edit 表单） */
export const listRoles = () =>
  unwrap<RoleInfo[]>(http.get('/web/account/role/list'))

/**
 * 新增账号
 *
 * 后端实际不会读 password 字段（body 不接受），新账号密码由后端默认生成（hash 形式，
 * 取决于 add 方法内部实现：按 ADR-0014 是 `bcrypt(sha256Hex(明文))`，但 add 不接收明文，
 * 因此走默认流程；具体能否登录受 ADR-0014 影响，与前端无关）。
 */
export const addAccount = (data: AccountBodyDTO) =>
  unwrap<void>(http.post('/web/account', data))

/** 编辑账号（不能改 username / password） */
export const editAccount = (data: AccountChgDTO) =>
  unwrap<void>(http.put('/web/account', data))

/** 删除账号 */
export const deleteAccount = (id: number) =>
  unwrap<void>(http.delete('/web/account', { params: { id } }))

/**
 * 修改当前用户密码
 *
 * 前端把三段都先 sha256Hex，DTO 整体走 PUT。
 * 后端校验：oldPassword 单 bcrypt 校验（与 DB hash 比对），password 长度必须 64 位 hex。
 */
export const changePwd = async (oldPlain: string, newPlain: string) => {
  const body: AccountPwdDTO = {
    oldPassword: await sha256Hex(oldPlain),
    password: await sha256Hex(newPlain),
    confirmPwd: await sha256Hex(newPlain)
  }
  return unwrap<void>(http.put('/web/account/pwd', body))
}

/** 重置指定用户密码（只传 id，后端按默认流程重置；不能指定新密码） */
export const resetPwd = (id: number) =>
  unwrap<void>(http.put('/web/account/pwd-reset', { id }))

/** 重置 super_admin 密码（前端传明文 → 内部 sha256Hex） */
export const resetAdminPwd = async (oldPlain: string, newPlain: string) => {
  const body: AccountPwdDTO = {
    oldPassword: await sha256Hex(oldPlain),
    password: await sha256Hex(newPlain),
    confirmPwd: await sha256Hex(newPlain)
  }
  return unwrap<void>(http.post('/web/account/pwd/admin', body))
}
