// =============================================================================
// W-FRONT-02-E1 实时数据看板 API（替代 stub）
//
// 后端实际可用端点（已 curl 验证）：
//   - GET /web/line/list                  线别列表（含 realtimeData JSON 字符串）
//   - GET /web/detect/realtime?lineNo=&faceNo=  实时采集数据（plan/actual/defect）
//   - GET /web/plan?pageNum=&pageSize=    计划（分页）
//   - GET /web/alarm/list?pageNum=&pageSize= 报警（分页）
//   - GET /web/detect/day-record/list-between?startTime=&endTime=  缺陷日记录
//
// 注：brief 原写的 /web/stateStatistic/day 与 /web/plan/day 在当前后端
//     并未实现（StateStatisticController 是空壳，PlanController 只有 /web/plan），
//     改用真实可用的端点；KPI 数字直接从 /web/line/list.realtimeData 聚合。
//
// 鉴权：satoken cookie 由 axios.withCredentials 携带（与 auth.ts 一致）。
// =============================================================================

import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/web'

/** 后端统一响应包装 */
export interface ApiEnvelope<T = unknown> {
  code: number
  success: boolean
  message: string
  data: T
}

/** 缺陷条目（realtimeData.defects[] / DefectDayRecord 共用形态） */
export interface DetectDefect {
  type: string
  count: number
  showFlag?: number
}

/** /web/line/list 单条（realtimeData 为 JSON 字符串） */
export interface LineItem {
  id: number
  name: string
  lineNo: string
  faceNo: string
  clientNo?: string
  realtimeData: string | null
  updateTime?: string
  createTime?: string
  key?: string
  pos?: string
}

/** 解析 realtimeData 后的实时采集数据 */
export interface RealtimeDetectData {
  total: number
  ngCount: number
  removeTotal: number
  removeFail: number
  efficiency: number
  totalNgRate: number
  occupancy: number
  occupancyRate: number
  startTime?: string
  defects?: DetectDefect[]
  // ===== W-RT-4: PSM 多出来字段（后端兜底计算，写入 JSON） =====
  /** 良品数量 = total - ngCount */
  successCount?: number
  /** 剔除失败率 = removeFail / removeTotal * 100（百分比，0 兜底） */
  removeFailRate?: number
}

/** 从 startTime (HH:mm:ss) 派生 deviceOpenTime (HH:mm) */
export function deviceOpenTimeOf(d: RealtimeDetectData | null | undefined): string {
  if (!d?.startTime) return '--:--'
  // startTime 形如 "HH:mm:ss" 或 "HH:mm"，取前 5 字符
  const m = /^(\d{1,2}:\d{2})/.exec(String(d.startTime))
  return m ? m[1] : d.startTime
}

/** 良品数量兜底 */
export function successCountOf(d: RealtimeDetectData | null | undefined): number {
  if (!d) return 0
  if (d.successCount !== undefined && d.successCount !== null) return d.successCount
  const total = Number(d.total || 0)
  const ng = Number(d.ngCount || 0)
  return Math.max(0, total - ng)
}

/** 剔除失败率兜底（百分比） */
export function removeFailRateOf(d: RealtimeDetectData | null | undefined): number {
  if (!d) return 0
  if (d.removeFailRate !== undefined && d.removeFailRate !== null) return d.removeFailRate
  const rt = Number(d.removeTotal || 0)
  const rf = Number(d.removeFail || 0)
  if (rt <= 0) return 0
  return Math.round((rf / rt) * 10000) / 100
}

/** 报警条目 */
export interface AlarmItem {
  id: number
  uuid?: string
  time: string
  type?: number
  lineNo: string
  faceNo: string
  level?: number
  message: string
  solve?: number
  defectName?: string
}

/** 计划条目（空时也是 records=[]） */
export interface PlanItem {
  id?: number
  name?: string
  startTime?: string
  endTime?: string
  total?: number
  [key: string]: unknown
}

/** 缺陷日记录条目 */
export interface DefectDayRecord {
  id?: number
  time?: string
  lineNo?: string
  faceNo?: string
  type?: string
  count?: number
}

// ---------------------------------------------------------------------------
// 工具：构造带 withCredentials 的 axios 实例（与 auth.ts 一致）
// ---------------------------------------------------------------------------
function get<T>(url: string, params?: Record<string, unknown>): Promise<ApiEnvelope<T>> {
  return axios
    .get<ApiEnvelope<T>>(`${API_BASE}${url}`, {
      params,
      withCredentials: true
    })
    .then((resp) => resp.data)
}

function post<T>(url: string, body?: unknown, params?: Record<string, unknown>): Promise<ApiEnvelope<T>> {
  return axios
    .post<ApiEnvelope<T>>(`${API_BASE}${url}`, body, {
      params,
      withCredentials: true
    })
    .then((resp) => resp.data)
}

// ---------------------------------------------------------------------------
// 端点
// ---------------------------------------------------------------------------

/** 线别列表 */
export function listLine(): Promise<ApiEnvelope<LineItem[]>> {
  return get<LineItem[]>('/line/list')
}

/** 单条实时采集数据（按 lineNo + faceNo） */
export function getRealtimeDetect(params: {
  lineNo: string
  faceNo: string
}): Promise<ApiEnvelope<RealtimeDetectData>> {
  return get<RealtimeDetectData>('/detect/realtime', params)
}

/** 当日计划（分页；返回 Page<PlanItem>） */
export function listPlan(params: { pageNum?: number; pageSize?: number; name?: string } = {}) {
  return get<{ records: PlanItem[]; total: number }>('/plan', {
    pageNum: params.pageNum ?? 1,
    pageSize: params.pageSize ?? 20,
    name: params.name ?? ''
  })
}

/**
 * 当日报警（分页）
 *
 * W-PERF-C: KPI 只用 total 不需要 records，默认 pageSize=1 让后端做 count(*) 而不返回 rows。
 * 调用方如要 records，再显式传 pageSize。
 *
 * 注：realtime 页 KPI 只关心当日 total，传 pageSize=1 + startTime/endTime 即可，
 * 响应里的 `total` 就是当日 count(*) 的精确值；不再拉 100 行前端过滤。
 */
export function listAlarm(
  params: {
    pageNum?: number
    pageSize?: number
    lineNo?: string
    startTime?: string
    endTime?: string
  } = {}
) {
  return get<{ records: AlarmItem[]; total: number; size: number; current: number }>(
    '/alarm/list',
    {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 1,
      lineNo: params.lineNo,
      startTime: params.startTime,
      endTime: params.endTime
    }
  )
}

/** 当日缺陷日记录（按时间区间） */
export function listDefectBetween(params: { startTime: string; endTime: string }) {
  return post<DefectDayRecord[]>('/detect/day-record/list-between', undefined, params)
}

/** 解析 line.realtimeData JSON 字符串，失败时返回 null */
export function parseRealtimeData(raw: string | null | undefined): RealtimeDetectData | null {
  if (!raw) return null
  try {
    const obj = JSON.parse(raw) as RealtimeDetectData
    return obj
  } catch {
    return null
  }
}

/** 当前日期字符串 yyyy-MM-dd（本地时区） */
export function todayStr(d: Date = new Date()): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** "yyyy-MM-dd HH:mm:ss" 当前时间 */
export function nowStr(d: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${todayStr(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
