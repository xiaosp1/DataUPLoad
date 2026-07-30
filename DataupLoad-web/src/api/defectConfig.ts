// =============================================================================
// W-DEFECT-CFG 子单 C — 缺陷配置 API 模块
//
// 关键去 hack：
//   1) 全部走 /web 路径，前缀由 vite proxy 转发到后端 8080
//   2) withCredentials: true 带上 satoken cookie（与 alarm.ts 风格一致）
//   3) 端点 1:1 对齐后端 W-DEFECT-CFG 子单 A：
//        - GET    /web/defect      缺陷列表分页查询（SearchDefectDTO：pageNum/pageSize/name/category）
//        - POST   /web/defect      新增缺陷配置（DefectTypeDTO + AddGroup）
//        - PUT    /web/defect      编辑缺陷配置（DefectTypeDTO + UpdateGroup）
//        - DELETE /web/defect?id=  按 id 删除缺陷（IdQuery）
//   4) /web/defect-api 是 PSM 老 SPA 的兼容路径（同 controller 暴露），新 SPA 走 /web/defect
//   5) 401 跳 /login 由 axios 全局 interceptor 统一处理（interceptor.ts）
// =============================================================================

import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/web'

export interface ApiEnvelope<T = unknown> {
  code: number
  msg?: string
  message?: string
  data: T
  success?: boolean
}

// ---------------------------------------------------------------------------
// 缺陷类型实体（PSM DefectTypePO 1:1，仅展示字段）
// ---------------------------------------------------------------------------
export interface DefectType {
  id: number
  name: string
  /** 1=缺陷报警 2=系统报警 3=设备报警 */
  category: number
  /** 是否推送大屏（0=不推 1=推） */
  alarmEnable: number
  /** 是否推声音（0=不推 1=推；仅当 alarmEnable=1 时有效） */
  soundEnable: number
  /** 是否推英科（0=不推 1=推） */
  sendYkEnable: number
  /** 后端字段，新 SPA 仅展示，无业务交互 */
  countEnable?: boolean
  countThreshold?: number
  rateEnable?: boolean
  showImgEnable?: boolean
  createTime?: string
  updateTime?: string
}

// ---------------------------------------------------------------------------
// 列表查询入参（PSM SearchDefectDTO 1:1）
// ---------------------------------------------------------------------------
export interface ListDefectParams {
  pageNum: number
  pageSize: number
  /** 缺陷名（模糊匹配） */
  name?: string
  /** 类别（1/2/3 精确匹配） */
  category?: number | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ---------------------------------------------------------------------------
// 列表查询（GET /web/defect）
// ---------------------------------------------------------------------------
export function listDefect(params: ListDefectParams): Promise<ApiEnvelope<PageResult<DefectType>>> {
  return axios
    .get(`${API_BASE}/defect`, { params, withCredentials: true })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 新增（POST /web/defect）
// ---------------------------------------------------------------------------
export interface CreateDefectBody {
  name: string
  category: number
  alarmEnable: number
  soundEnable: number
  sendYkEnable: number
}

export function createDefect(body: CreateDefectBody): Promise<ApiEnvelope<unknown>> {
  return axios
    .post(`${API_BASE}/defect`, body, { withCredentials: true })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 编辑（PUT /web/defect）
// ---------------------------------------------------------------------------
export interface UpdateDefectBody {
  id: number
  name: string
  category: number
  alarmEnable: number
  soundEnable: number
  sendYkEnable: number
}

export function updateDefect(body: UpdateDefectBody): Promise<ApiEnvelope<unknown>> {
  return axios
    .put(`${API_BASE}/defect`, body, { withCredentials: true })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 删除（DELETE /web/defect?id=）
// ---------------------------------------------------------------------------
export function deleteDefect(id: number): Promise<ApiEnvelope<unknown>> {
  return axios
    .delete(`${API_BASE}/defect`, { params: { id }, withCredentials: true })
    .then((r) => r.data)
}
