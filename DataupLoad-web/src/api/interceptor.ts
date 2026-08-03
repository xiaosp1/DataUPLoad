// =============================================================================
// W-FRONT-02-C axios 全局拦截器
// W-FRONT-05-A 改造：401 改用 router 软跳（不再整页 reload）
//
// 设计：
//   - 请求拦截：浏览器同源 + axios 默认 withCredentials 不在 axios 实例里
//     设全局（避免侵入其他调用方）。auth.ts 里每个 API 显式传
//     { withCredentials: true }。
//   - 响应拦截：401 → 软跳登录页（清 user store 状态）。其余错误原样 reject。
// =============================================================================

import axios from 'axios'
import router from '@/router'

// 全局请求拦截器：保留扩展位（header / trace 等），目前无副作用
axios.interceptors.request.use(
  (config) => {
    return config
  },
  (err) => Promise.reject(err)
)

// 全局响应拦截器：401 软跳登录页（W-FRONT-05-A 改造）
axios.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      // 已在登录页则不再重复跳（避免循环）
      if (router.currentRoute.value.name !== 'Login') {
        // 清 cookie（W-FRONT-05-A 简化：只清 cookie，store 留给 Login.vue mounted 处理）
        // satoken 是 httpOnly，前端只能写 expires；这里用占位 cookie 触发失效
        document.cookie = 'satoken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
        // 软跳：Vue Router 切换，URL 变化但页面不刷
        router.push({ name: 'Login' })
      }
    }
    return Promise.reject(err)
  }
)
