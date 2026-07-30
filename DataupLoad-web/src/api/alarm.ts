// =============================================================================
// W-FRONT-02-E2 报警管理 API
//
// 关键去 hack：
//   1) 全部走 /web 路径，前缀由 vite proxy 转发到后端 8080
//   2) withCredentials: true 带上 satoken cookie（interceptor.ts 不在 axios
//      实例里设全局，避免侵入其他调用方；alarm.ts 里每个 API 显式声明）
//   3) 端点 1:1 对齐 PSM AlarmRecordController 反编译产物（W-ALM-03）：
//        - GET  /web/alarm/list      报警列表（AlarmQueryDTO：pageNum/pageSize/type/level/solve/faceId/startTime/endTime/sortType）
//        - GET  /web/alarm/list-info 报警详情 + 关联设备（AlarmInfoQueryDTO）
//        - PUT  /web/alarm/ignore    忽略报警（IgnoreAlarmDTO JSON body）
//        - GET  /web/line/tree       线别 + 工位级联（PSM 同款，alarm 页线别下拉数据源）
//
// 4) 不存在 /web/alarm/type/list 与 /web/alarm/get/{id} 端点（brief 初稿错误，
//    实际由 list（按 type 过滤）+ list-info（详情）两个端点覆盖）；E2 按真实
//    端点实现，不写 mock、不调不存在的接口。
//
// 5) 401 跳 /login 由 axios 全局 interceptor 统一处理（interceptor.ts）。
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
// 报警列表查询入参（PSM AlarmQueryDTO 1:1）
// ---------------------------------------------------------------------------
export interface ListAlarmParams {
  pageNum: number
  pageSize: number
  /** 报警类型：1=缺陷 2=系统 3=设备（PSM AlarmTypeEnum） */
  type?: number | null
  /** 等级：1=普通 2=严重 */
  level?: number | null
  /** 处理状态：1=已处理 2=未处理 3=已忽略 */
  solve?: number | null
  /** PSM 端 line.id（工位 ID），与 line-tree 联级选择器的 child id 一致 */
  faceId?: number | null
  /** 报警发生时间窗（yyyy-MM-dd HH:mm:ss，与 AlarmRecord.time 字符串格式一致） */
  startTime?: string
  endTime?: string
  /** 0=升序 1=降序（按 time） */
  sortType?: number
}

export interface AlarmRecord {
  id: number
  uuid: string
  time: string
  type: number
  lineNo: string
  faceNo: string
  level: number
  message: string
  solve: number
  defectName?: string
  key?: string
  line?: string
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 报警列表（PSM getAlarmList 1:1）
 *
 * @param params AlarmQueryDTO 字段（见 ListAlarmParams）
 * @returns 后端返回的 PageResult<AlarmRecord>
 */
export function listAlarm(params: ListAlarmParams): Promise<ApiEnvelope<PageResult<AlarmRecord>>> {
  return axios
    .get(`${API_BASE}/alarm/list`, { params, withCredentials: true })
    .then((r) => r.data)
}

/**
 * 报警详情 + 关联设备列表（PSM getAlarmListInfo 1:1）。
 *
 * 与 list 的区别：list-info 返回的是 AlarmRecord + 关联设备；用于详情弹窗。
 *
 * W-RT-9: 扩展入参, 额外支持 lineNo / faceNo / type / defectName
 *   - lineNo + faceNo 是 PSM 工位 ID 在前端的查询名称 (与 faceId 互斥)
 *   - 后端 list-info 实际是透传到 list (PSM 同款接口), 这几个参数会被接口忽略或作为额外过滤
 *   - 本期主要靠 startTime/endTime + pageSize:1 拿最近一条; 其它参数作为可选过滤
 */
export function getAlarmDetail(params: {
  faceId?: number | null
  lineNo?: string
  faceNo?: string
  type?: number | null
  defectName?: string
  pageNum?: number
  pageSize?: number
  startTime?: string
  endTime?: string
}): Promise<ApiEnvelope<PageResult<AlarmRecord>>> {
  return axios
    .get(`${API_BASE}/alarm/list-info`, { params, withCredentials: true })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 忽略报警（PSM ignoreAlarm 1:1）
// ---------------------------------------------------------------------------
export interface IgnoreAlarmBody {
  /** 不常用，保留兼容 */
  id?: number
  /** 报警类型过滤（可选） */
  type?: number
  /** defectName 过滤（PSM 同款） */
  defectName?: string
  /** 线别过滤（PSM 同款） */
  lineNo?: string
  /** 工位过滤（PSM 同款） */
  faceNo?: string
  /** 0=按条件忽略 1=全部忽略未处理 */
  ignoreAll?: number
  /** 工位 ID（PSM 通过 lineService.getById 反查 lineNo/faceNo） */
  faceId?: string
  /** 报警时间窗下界 */
  startTime?: string
  /** 报警时间窗上界 */
  endTime?: string
  /** ignore_alarm.ignore_time（yyyy-MM-dd HH:mm:ss） */
  ignoreTime?: string
}

/**
 * 忽略报警（PUT /web/alarm/ignore，PSM 同款）
 *
 * PSM 端是 @PutMapping + @RequestBody，不在 URL 上挂 id。
 * 单点忽略：传 lineNo + faceNo + defectName + startTime/endTime 缩小范围；
 * 批量忽略：传 ignoreAll=1 忽略全部未处理。
 */
export function ignoreAlarm(body: IgnoreAlarmBody): Promise<ApiEnvelope<unknown>> {
  return axios
    .put(`${API_BASE}/alarm/ignore`, body, { withCredentials: true })
    .then((r) => r.data)
}

// ---------------------------------------------------------------------------
// 线别级联（PSM lineTree 1:1，给 alarm 页线别下拉做数据源）
// ---------------------------------------------------------------------------
export interface LineTreeNode {
  id: number
  name: string
  lineNo: string
  childs: LineTreeNode[]
}

/**
 * 线别 + 工位级联（PSM lineTree）
 *
 * 返回结构：[{ id, name, lineNo, childs: [{ id, name, lineNo, childs: [] }] }]
 * 注意：PSM line 是 nested，但 leaf 工位上的 lineNo 实际是 faceNo 字符串（已与 PSM 1:1 对齐）。
 */
export function listLineTree(): Promise<ApiEnvelope<LineTreeNode[]>> {
  return axios
    .get(`${API_BASE}/line/tree`, { withCredentials: true })
    .then((r) => r.data)
}
