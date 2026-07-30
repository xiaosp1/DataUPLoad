// =============================================================================
// W-FRONT-02-C axios 全局拦截器
//
// 设计：
//   - 请求拦截：浏览器同源 + axios 默认 withCredentials 不在 axios 实例里
//     设全局（避免侵入其他调用方）。auth.ts 里每个 API 显式传
//     { withCredentials: true }。
//   - 响应拦截：401 → 跳登录页（清状态）。其余错误原样 reject，由调用方处理。
// =============================================================================

import axios from 'axios'

const LOGIN_ROUTE = '/login'

// 全局请求拦截器：保留扩展位（header / trace 等），目前无副作用
axios.interceptors.request.use(
  (config) => {
    return config
  },
  (err) => Promise.reject(err)
)

// 全局响应拦截器：401 跳登录
axios.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      // 避免在登录页本身重复跳
      const hash = window.location.hash || ''
      if (!hash.includes(`#${LOGIN_ROUTE}`)) {
        // 强制刷到登录页，避免半登录态的脏数据残留
        window.location.href = `${window.location.pathname}#${LOGIN_ROUTE}`
      }
    }
    return Promise.reject(err)
  }
)
