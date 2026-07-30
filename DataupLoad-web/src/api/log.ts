// =============================================================================
// W-FRONT-02-E6 操作日志 API（按真实后端 controller 路由写，非 brief 假设）
//
// 重要：brief 列出的 `/web/log/list`、`/web/log/get/{id}` 与实际后端
// controller 路径不一致。实际是基于 framework-starter 的
// ApiLogController（请求映射 `/web/api-log`，方法 `list`）。
//
// 反编译 framework-starter-2.2.3-SNAPSHOT.jar 得到的真实契约：
//
//   list  GET  /web/api-log/list
//     参数（ApiLogQuery 继承 PageQuery）：
//       pageNum, pageSize                  分页
//       operator                           操作者（模糊匹配 username）
//       operation                          操作类型 / 描述（模糊匹配）
//       module                             模块名（模糊匹配）
//       ip                                 IP（模糊匹配）
//       req                                请求体关键字（inputparam LIKE）
//       resp                               响应体关键字（outputparam LIKE）
//       result                             1=成功 / 0=失败（精确匹配）
//       startTime, endTime                 LocalDateTime 字符串（yyyy-MM-dd HH:mm:ss）
//     返回：BaseResult<Page<ApiLogPO>> → unwrap → { records: ApiLog[], total }
//
//   注：没有 `/get/{id}` 详情接口；详情用 list 行直接展开（包含 inputparam
//       / outputparam 完整字符串）。drawer 内对 JSON 自动 pretty-print。
//
// 与 brief 差异（不能改 backend，记录下来）：
//   - brief 的 `/web/log/list` 实际路径是 `/web/api-log/list`
//   - brief 的 `/web/log/get/{id}` 不存在；详情 = list row 的 inputparam/outputparam
//   - 系统日志另有一个 `/web/system-log/list`（SystemLogController）；与
//     本页"操作日志"无关，保留扩展位不实现
//
// 字段映射（ApiLogPO）：
//   id, operatorId, operator, ip, module, operation, result,
//   inputparam, outputparam, cost(ms), uri,
//   createTime(调用时间), updateTime(完成时间)
// =============================================================================

import http from './http'

// ---------------------------------------------------------------------------
// 类型定义
// ---------------------------------------------------------------------------

/** 后端 ApiLogPO 行（按 framework-starter bytecode） */
export interface ApiLog {
  id: number
  operatorId?: number | null
  operator: string
  ip?: string | null
  module?: string | null
  operation?: string | null
  /** 1=成功 / 0=失败 / null / 其它 视为未分类 */
  result?: number | null
  inputparam?: string | null
  outputparam?: string | null
  /** 毫秒 */
  cost?: number | null
  uri?: string | null
  createTime?: string | null
  updateTime?: string | null
}

/** ApiLogQuery 字段（PageQuery 子类） */
export interface ApiLogQuery {
  pageNum?: number
  pageSize?: number
  operator?: string
  operation?: string
  module?: string
  ip?: string
  /** yyyy-MM-dd HH:mm:ss；后端是 LocalDateTime 字符串 */
  startTime?: string
  endTime?: string
  /** 1=成功 / 0=失败 */
  result?: number | null
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

/**
 * 操作日志分页列表
 *
 * @example
 *   listApiLog({ pageNum: 1, pageSize: 20, operator: 'admin' })
 */
export const listApiLog = (params: ApiLogQuery) =>
  unwrap<PageResult<ApiLog>>(http.get('/web/api-log/list', { params }))

/**
 * 尝试拉取单条详情（如未来 controller 加了 `/get/{id}`）。
 *
 * 当前 controller 没有 get by id（仅 list）；保留此函数供未来扩展。
 * 调用方 catch 404 / NotFound 后回退到 list row 即可。
 */
export const getApiLog = (id: number) =>
  unwrap<ApiLog>(http.get(`/web/api-log/get/${id}`))
