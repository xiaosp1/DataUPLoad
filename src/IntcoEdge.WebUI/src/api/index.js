/**
 * API 客户端封装
 *
 * baseURL = '/api'：
 *   - 开发模式（vite dev 5289）：vite.config.js 的 server.proxy 把 /api 转发到 EdgeHost 5288
 *   - 生产模式（EdgeHost serve dist/）：同源访问，无 CORS 问题
 *
 * 当前 W-A3 还没完工，所有 axios 调用会失败。视图层用 try/catch + fallback 到 mock 数据。
 * W-A3 完成后，把视图里 readMock() 改回 fetchXxx() 即可，无需改业务代码。
 */

import axios from 'axios'
import * as mock from './mock'

// Axios 实例
const http = axios.create({
  baseURL: '/api',
  timeout: 5000,
  headers: { 'Content-Type': 'application/json' }
})

// 简单响应拦截：失败时打印警告，不抛出（视图层会走 mock fallback）
http.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    // eslint-disable-next-line no-console
    console.warn('[api] request failed, fallback to mock:', err?.message || err)
    return Promise.reject(err)
  }
)

// 健康检查
export async function checkApiHealth() {
  try {
    await axios.get('/health', { timeout: 2000 })
    return true
  } catch {
    return false
  }
}

// ---------- 业务接口（带 mock fallback） ----------

export async function fetchLines() {
  try {
    return await http.get('/lines')
  } catch {
    return mock.lines
  }
}

export async function fetchCameras(lineId) {
  try {
    return await http.get('/cameras', { params: lineId ? { lineId } : {} })
  } catch {
    return lineId ? mock.cameras.filter((c) => c.lineId === lineId) : mock.cameras
  }
}

export async function fetchDefects(params = {}) {
  try {
    return await http.get('/defects', { params })
  } catch {
    let list = mock.defects
    if (params.lineId) list = list.filter((d) => d.lineId === params.lineId)
    if (params.type) list = list.filter((d) => d.type === params.type)
    return list
  }
}

export async function fetchDefectTrend(params = {}) {
  try {
    return await http.get('/defects/trend', { params })
  } catch {
    return mock.defectTrend
  }
}

export async function fetchDefectRanking() {
  try {
    return await http.get('/defects/ranking')
  } catch {
    return mock.defectRanking
  }
}

export async function fetchAlarms() {
  try {
    return await http.get('/alarms')
  } catch {
    return mock.alarms
  }
}

export default http
