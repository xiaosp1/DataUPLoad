// =============================================================================
// W-FRONT-02-C Pinia user store
//
// 用户态完全由后端 /web/account/current 提供，satoken cookie 自动鉴权，
// 不再有任何前端 token / localStorage hack。
// =============================================================================

import { defineStore } from 'pinia'
import { getCurrentUser, CurrentUser } from '../api/auth'
import { usePermissionStore } from './permission'

interface UserState {
  id: number
  username: string
  role: string
  permission: string[]
  loaded: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    id: 0,
    username: '',
    role: '',
    permission: [],
    loaded: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.id && state.loaded)
  },

  actions: {
    /**
     * 拉一次 /web/account/current 同步当前用户。
     * 401 由 axios 全局拦截器统一跳登录，这里只关注业务成功分支。
     *
     * 同时把 role 和 permission 同步到 permission store，
     * 让路由守卫 (router/index.ts) 能正确判定 meta.permission，
     * 否则 super_admin 也会被踢到 /403（D-tier bug 修复点）。
     */
    async fetchCurrent(): Promise<void> {
      const resp = await getCurrentUser()
      // 后端 BaseResult 成功响应是 { success: true, code: 0, message: '...', data: ... }
      // 不是 HTTP 200；必须同时检查 success 和 code
      if (resp && resp.success === true && resp.code === 0 && resp.data) {
        this.$patch(resp.data as Partial<UserState>)
        this.loaded = true

        // 同步到 permission store（D-tier bug #2 修复）
        const perm = usePermissionStore()
        const role = (resp.data as CurrentUser).role
        const codes = (resp.data as CurrentUser).permission
        if (role) {
          perm.setRoles([role])
        }
        if (Array.isArray(codes) && codes.length > 0) {
          perm.setCodes(codes)
        }
      }
    },

    /**
     * 退出：清空本地态。satoken cookie 由后端 logout 接口负责失效。
     * 同时清空 permission store，避免下一位登录用户继承上一位的权限。
     */
    reset(): void {
      this.$patch({
        id: 0,
        username: '',
        role: '',
        permission: [],
        loaded: false
      } as Partial<UserState>)
      const perm = usePermissionStore()
      perm.reset()
    },

    /**
     * W-FRONT-05-A: 拦截器 / 主动登出的统一入口。
     * 语义比 reset() 更清晰：清空 user + permission 态。
     * satoken cookie 在拦截器场景下已被后端 401 标记失效；
     * 主动登出（Topbar.vue）仍走后端 logout 接口，这里只清前端态。
     */
    logout(): void {
      this.reset()
    }
  }
})

export type { CurrentUser }
