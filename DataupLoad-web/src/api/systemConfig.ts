// =============================================================================
// W-FRONT-02-E5 系统配置 API
//
// 与 brief 给的路径不完全一致，按 DataupLoad 后端 controller 实际暴露的
// 路由（详见 src/main/java/.../module/config/web/SystemConfigController.java
// 以及 module/line/web/LineController.java / module/defect/web/LineDefectTypeController.java）：
//
//   SystemConfig:  GET  /web/system-config        查全部
//                  PUT  /web/system-config        整批保存（请求体为 SystemConfigPO[]）
//   Line:          GET  /web/line/list            列表
//                  POST /web/line                 新增（LineBodyDTO：name/lineNo/faceNo/...）
//                  PUT  /web/line                 修改（LineUpdateDTO：id/...）
//                  DELETE /web/line?id={id}       删除
//   LineDefectType:GET  /web/defect/line-type/list
//                  POST /web/defect/line-type     新增（LineDefectType）
//                  PUT  /web/defect/line-type     修改
//                  DELETE /web/defect/line-type/{id}
//
// 鉴权：所有 list/add/edit/delete 接口都需要 satoken cookie；axios 必须带
// { withCredentials: true }（与 src/api/auth.ts 一致）。
// =============================================================================

import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/web'

// ---------------------------------------------------------------------------
// 通用 envelope
// ---------------------------------------------------------------------------
export interface ApiEnvelope<T = unknown> {
  code: number
  msg: string
  message?: string
  success: boolean
  data: T
}

// ---------------------------------------------------------------------------
// 系统配置（system_config 表）
// ---------------------------------------------------------------------------
export interface SystemConfigItem {
  id?: number
  configName?: string
  configKey: string
  configValue: string
  updateTime?: string
  createTime?: string
}

/** GET /web/system-config — 查全部配置项 */
export function listSystemConfig(): Promise<ApiEnvelope<SystemConfigItem[]>> {
  return axios
    .get<ApiEnvelope<SystemConfigItem[]>>(`${API_BASE}/system-config`, {
      withCredentials: true
    })
    .then((r) => r.data)
}

/** PUT /web/system-config — 整批保存（后端要求 @NotEmpty 数组） */
export function updateSystemConfig(
  list: SystemConfigItem[]
): Promise<ApiEnvelope<unknown>> {
  return axios
    .put<ApiEnvelope<unknown>>(`${API_BASE}/system-config`, list, {
      withCredentials: true,
      headers: { 'Content-Type': 'application/json' }
    })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 线别（line 表）
// ---------------------------------------------------------------------------
export interface LineItem {
  id: number
  name: string
  lineNo: string
  faceNo?: string
  color?: string
  clientNo?: string
  realtimeData?: string
  updateTime?: string
  createTime?: string
}

export interface LineCreateForm {
  name: string
  lineNo: string
  faceNo: string
  color?: string
}

/** GET /web/line/list — 全部线体 */
export function listLine(): Promise<ApiEnvelope<LineItem[]>> {
  return axios
    .get<ApiEnvelope<LineItem[]>>(`${API_BASE}/line/list`, {
      withCredentials: true
    })
    .then((r) => r.data)
}

/** POST /web/line — 新增产线 */
export function addLine(form: LineCreateForm): Promise<ApiEnvelope<unknown>> {
  return axios
    .post<ApiEnvelope<unknown>>(`${API_BASE}/line`, form, {
      withCredentials: true,
      headers: { 'Content-Type': 'application/json' }
    })
    .then((r) => r.data)
}

/** PUT /web/line — 修改产线（必须带 id） */
export function editLine(
  form: Partial<LineItem> & { id: number }
): Promise<ApiEnvelope<unknown>> {
  return axios
    .put<ApiEnvelope<unknown>>(`${API_BASE}/line`, form, {
      withCredentials: true,
      headers: { 'Content-Type': 'application/json' }
    })
    .then((r) => r.data)
}

/** DELETE /web/line?id={id} — 删除产线 */
export function deleteLine(id: number): Promise<ApiEnvelope<unknown>> {
  return axios
    .delete<ApiEnvelope<unknown>>(`${API_BASE}/line`, {
      params: { id },
      withCredentials: true
    })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 线别缺陷类型（line_defect_type 表）
// ---------------------------------------------------------------------------
export interface LineDefectTypeItem {
  id: number
  name: string
  showFlag: number // 1=启用 0=禁用
  lineNo: string
  faceNo: string
  updateTime?: string
  createTime?: string
}

/** GET /web/defect/line-type/list */
export function listLineDefectType(): Promise<ApiEnvelope<LineDefectTypeItem[]>> {
  return axios
    .get<ApiEnvelope<LineDefectTypeItem[]>>(
      `${API_BASE}/defect/line-type/list`,
      { withCredentials: true }
    )
    .then((r) => r.data)
}

/** POST /web/defect/line-type — 新增 */
export function addLineDefectType(
  form: Partial<LineDefectTypeItem>
): Promise<ApiEnvelope<unknown>> {
  return axios
    .post<ApiEnvelope<unknown>>(
      `${API_BASE}/defect/line-type`,
      form,
      {
        withCredentials: true,
        headers: { 'Content-Type': 'application/json' }
      }
    )
    .then((r) => r.data)
}

/** PUT /web/defect/line-type — 修改（按 id 主键） */
export function editLineDefectType(
  form: Partial<LineDefectTypeItem> & { id: number }
): Promise<ApiEnvelope<unknown>> {
  return axios
    .put<ApiEnvelope<unknown>>(
      `${API_BASE}/defect/line-type`,
      form,
      {
        withCredentials: true,
        headers: { 'Content-Type': 'application/json' }
      }
    )
    .then((r) => r.data)
}

/** DELETE /web/defect/line-type/{id} */
export function deleteLineDefectType(id: number): Promise<ApiEnvelope<unknown>> {
  return axios
    .delete<ApiEnvelope<unknown>>(
      `${API_BASE}/defect/line-type/${id}`,
      { withCredentials: true }
    )
    .then((r) => r.data)
}
