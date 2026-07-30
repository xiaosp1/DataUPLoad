// =============================================================================
// W-FRONT-02-D 权限 store
// 角色 + 权限点两套机制：
//   - super_admin 永远放行
//   - 其他角色按路由 meta.permission 字符串匹配
// E 子单负责把后端返回的 roles 写进这里。
// =============================================================================

import { defineStore } from 'pinia'

interface PermissionState {
  roles: string[]
  /** 额外细粒度权限码（可选，对应路由 meta.permission） */
  codes: string[]
}

export const usePermissionStore = defineStore('permission', {
  state: (): PermissionState => ({
    roles: [],
    codes: []
  }),

  getters: {
    isSuperAdmin: (state) => state.roles.includes('super_admin')
  },

  actions: {
    /**
     * 判断当前用户是否拥有某个权限点
     * - super_admin 永远返回 true
     * - 否则查找 roles 数组（兼容 "user" / "log" 等角色名）
     *   或 codes 数组（细粒度权限码）
     */
    has(p: string): boolean {
      if (!p) return true
      if (this.roles.includes('super_admin')) return true
      if (this.roles.includes(p)) return true
      if (this.codes.includes(p)) return true
      return false
    },

    /**
     * 写入角色列表（由 user.fetchCurrent 或登录成功后调用）
     */
    setRoles(roles: string[]): void {
      this.roles = Array.isArray(roles) ? [...roles] : []
    },

    setCodes(codes: string[]): void {
      this.codes = Array.isArray(codes) ? [...codes] : []
    },

    reset(): void {
      this.roles = []
      this.codes = []
    }
  }
})
