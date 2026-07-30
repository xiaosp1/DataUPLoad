// =============================================================================
// W-FRONT-02-E7 用户管理 API
//
// 设计：
//   目前复用 /web/account 接口，按 role=operator 过滤（前端传 role=operator 参数）
//   查询条件：name（模糊匹配 username 或 realName）+ role
//   等后续 user module 落地后可替换为独立 user 接口
//
// 后端接口：
//   list            GET  /web/account/list   ?pageNum=1&pageSize=10&role=operator&name=
//   role/list       GET  /web/account/role/list
//   editProfile     POST /web/account/editProfile   (若不存在则降级 edit)
// =============================================================================

import http from './http'

// ---------------------------------------------------------------------------
// 类型定义
// ---------------------------------------------------------------------------

/** 操作员账号行（复用 AccountInfo 子集） */
export interface OperatorInfo {
  id: number
  username: string
  realName?: string | null
  contactInfo?: string | null
  role: string
  permission?: string[]
  createTime?: string
  updateTime?: string
}

/** 角色 */
export interface RoleInfo {
  id: number
  role: string
  permission?: string[]
}

/** 分页查询 */
export interface OperatorQuery {
  pageNum?: number
  pageSize?: number
  role?: string
  name?: string
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 后端响应包络 */
export interface ApiEnvelope<T = unknown> {
  success: boolean
  code: number
  msg?: string
  message?: string
  data: T
}

// ---------------------------------------------------------------------------
// 内部 helper
// ---------------------------------------------------------------------------
async function unwrap<T>(p: Promise<{ data: ApiEnvelope<T> }>): Promise<ApiEnvelope<T>> {
  const r = await p
  return r.data
}

// ---------------------------------------------------------------------------
// API 调用
// ---------------------------------------------------------------------------

/** 按 role=operator 过滤的分页列表 */
export const listOperator = (params: OperatorQuery) =>
  unwrap<PageResult<OperatorInfo>>(http.get('/web/account/list', { params }))

/** 角色列表（后续可用于筛选操作员角色） */
export const listRoles = () =>
  unwrap<RoleInfo[]>(http.get('/web/account/role/list'))

/**
 * 编辑操作员个人信息
 *
 * 若后端没有 /web/account/editProfile 接口，降级用 PUT /web/account
 * 这里优先尝试 editProfile，调用方 catch 后重试 edit
 */
export const editProfile = (data: { id: number; realName?: string; contactInfo?: string }) =>
  unwrap<void>(http.post('/web/account/editProfile', data))

/** 降级：通过 /web/account (PUT) 修改 */
export const editAccount = (data: { id: number; realName?: string; contactInfo?: string }) =>
  unwrap<void>(http.put('/web/account', data))

/**
 * 操作历史（复用 /web/log/list 按 username 过滤）
 *
 * 注意：当前后端 log/list 返回 500，前端需优雅降级显示"暂无操作记录"
 */
export const listLogByUser = (params: { username: string; pageNum?: number; pageSize?: number }) =>
  unwrap<PageResult<any>>(http.get('/web/log/list', { params }))
