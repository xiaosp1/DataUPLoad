// =============================================================================
// W-FRONT-02-C 登录 / 当前用户 API
//
// 关键去 hack：
//   1) 密码不在前端 SHA256 之外做任何处理（跟老 gate-routing 一致：明文密码
//      → SHA-256 hex → 后端 sa-token 比对）
//   2) 登录成功由后端通过 Set-Cookie 写入 satoken，浏览器同源请求会自动带上
//   3) 所有请求必须 withCredentials: true，否则 cookie 不会带上
// =============================================================================

import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/web'

export interface LoginRequest {
  username: string
  password: string
}

export interface ApiEnvelope<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface CurrentUser {
  id: number
  username: string
  role: string
  permission?: string[]
}

/**
 * 登录
 *
 * @param username 用户名（明文）
 * @param password 密码（明文），函数内部会做 SHA-256 hex
 * @returns 后端返回的业务体（success = code === 200）
 */
export async function login(username: string, password: string): Promise<ApiEnvelope> {
  const pwdHex = await sha256Hex(password)
  const resp = await axios.post<ApiEnvelope>(
    `${API_BASE}/auth/login`,
    { username, password: pwdHex } satisfies LoginRequest,
    { withCredentials: true }
  )
  return resp.data
}

/**
 * 拉取当前登录用户。后端靠 satoken cookie 鉴权。
 */
export async function getCurrentUser(): Promise<ApiEnvelope<CurrentUser>> {
  const resp = await axios.get<ApiEnvelope<CurrentUser>>(
    `${API_BASE}/account/current`,
    { withCredentials: true }
  )
  return resp.data
}

/**
 * SHA-256 hex 计算（用浏览器原生 crypto.subtle，不引依赖）
 *
 * 老 PSM gate-routing 用同样的算法（hex 字符串），保持向后兼容。
 */
export async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}
