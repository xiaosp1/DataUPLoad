// =============================================================================
// W-FRONT-02-E3 缺陷处理 API
//
// 真实后端契约（与 brief 中的路径略有差异，按 DataupLoad 实际部署调整）：
//   - 列表：POST /web/detect/day-record/list-between?startTime=&endTime=
//     （brief 中的 /web/defectDayRecord/list 不存在；按时间区间全量返回，前端做分页/筛选）
//   - 详情：list-between 区间 + 前端按 id 过滤（list-by-attribute 不支持按 id）
//   - 处理：后端无 handle 接口；前端在本地维护 handled / remark 状态（乐观更新）
//   - 缺陷类型：GET /web/defect/line-type/list
//   - 7 日趋势：后端无 trend 接口；前端基于 list-between 聚合，按 time（yyyy-MM-dd）分组
//
// 所有响应统一 BaseResult 格式：{ success, code, message, data }
// axios 实例（http.js）baseURL = '/'，实际请求经 vite proxy 转发到 localhost:8080
// =============================================================================

import http from './http'

// ---------------------------------------------------------------------------
// 类型
// ---------------------------------------------------------------------------

/** 后端 DefectDayRecord 实体（按时间区间返回的字段） */
export interface DefectDayRecord {
  id: number
  /** 当日缺陷总数（聚合字段） */
  count: number
  /** 时间字符串 "yyyy-MM-dd HH:mm:ss" — day-record 是按 (time, lineNo, faceNo, type) 维度聚合 */
  time: string
  lineNo: string
  faceNo: string
  /** 缺陷类型名（中文字符串，对应 line_defect_type.name） */
  type: string
  updateTime?: string
  createTime?: string
  localTime?: string
  pos?: string
}

/** line_defect_type 字典项 */
export interface LineDefectType {
  id: number
  name: string
  showFlag: number
  lineNo: string
  faceNo: string
  updateTime?: string
  createTime?: string
  pos?: string
}

/** BaseResult 通用信封 */
export interface BaseResult<T = unknown> {
  success: boolean
  code: number
  message: string
  data: T
}

// ---------------------------------------------------------------------------
// API
// ---------------------------------------------------------------------------

/**
 * 缺陷日记录列表（区间查询 + 前端分页）
 *
 * @param params.startTime yyyy-MM-dd HH:mm:ss
 * @param params.endTime   yyyy-MM-dd HH:mm:ss
 * @param params.date      yyyy-MM-dd（保留 brief 中的兼容字段；本实现用 startTime/endTime）
 * @param params.lineId    保留 brief 兼容字段（本实现无 lineId，按 lineNo 字符串做前端筛选）
 * @param params.type      保留 brief 兼容字段
 * @param params.level     保留 brief 兼容字段（本实现无 level，前端按 type 启发式推断）
 */
export const listDefectDay = (params: {
  pageNum?: number
  pageSize?: number
  date?: string
  lineId?: number
  type?: string
  level?: string
}) => {
  // 默认按当天 0 点 ~ 23:59:59
  const date = params.date || new Date().toISOString().slice(0, 10)
  const startTime = `${date} 00:00:00`
  const endTime = `${date} 23:59:59`
  return http.post<BaseResult<DefectDayRecord[]>>(
    '/web/detect/day-record/list-between',
    null,
    { params: { startTime, endTime } }
  )
}

/**
 * 缺陷详情（按 id）
 *
 * 后端 list-by-attribute 不支持 attr=id，本实现走"最近 30 天区间 + 前端按 id 过滤"
 */
export const getDefectDetail = (id: number) => {
  const end = new Date()
  const start = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  const fmt = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  return http.post<BaseResult<DefectDayRecord[]>>(
    '/web/detect/day-record/list-between',
    null,
    { params: { startTime: fmt(start), endTime: fmt(end) } }
  )
}

/**
 * 标记缺陷已处理（写备注 / 标记完成）
 *
 * 后端无对应 endpoint；前端做乐观更新即可（直接 resolve 一个成功结果）。
 * 函数仍保留以便将来后端实现时只需改 URL。
 */
export const handleDefect = (_id: number, _remark: string): Promise<BaseResult<null>> => {
  return Promise.resolve({
    success: true,
    code: 0,
    message: 'ok',
    data: null
  })
}

/**
 * 线别缺陷类型字典（全量）
 */
export const listLineDefectType = () =>
  http.get<BaseResult<LineDefectType[]>>('/web/defect/line-type/list')

/**
 * 7 日趋势
 *
 * 后端无对应 endpoint；返回最近 7 天的区间查询数据，前端组件按日期聚合。
 */
export const getDefectTrend = (from: string, to: string) => {
  // from/to 是 yyyy-MM-dd，补成 00:00:00 / 23:59:59
  const startTime = `${from} 00:00:00`
  const endTime = `${to} 23:59:59`
  return http.post<BaseResult<DefectDayRecord[]>>(
    '/web/detect/day-record/list-between',
    null,
    { params: { startTime, endTime } }
  )
}
