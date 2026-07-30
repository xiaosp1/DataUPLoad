// =============================================================================
// W-FRONT-02-D 路由 + 守卫（完整版）
//
// 关键设计：
//   - 9 个业务路由（含 /screen）全部挂在 MainLayout 下，通过 children 嵌套
//   - /login 和 /403 是 public 路由
//   - 路由 meta.permission 是字符串，守卫用 permission store has() 检查
//   - 无 satoken → /login；已登录但无权限 → /403
//   - 仍是 hash history + 只读 satoken，不引入 PSM 老 hack
// =============================================================================

import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import Login from '../views/Login.vue'
import MainLayout from '../layouts/MainLayout.vue'
import RealTime from '../views/RealTime.vue'
import Alarm from '../views/Alarm.vue'
import Defect from '../views/Defect.vue'
import Account from '../views/Account.vue'
import SystemConfig from '../views/SystemConfig.vue'
import Log from '../views/Log.vue'
import UserManage from '../views/UserManage.vue'
import Screen from '../views/Screen.vue'
import Forbidden from '../views/Forbidden.vue'
import { usePermissionStore } from '../stores/permission'
import { useUserStore } from '../stores/user'

declare module 'vue-router' {
  interface RouteMeta {
    /** true = 不需要登录（login、403） */
    public?: boolean
    /** 需要的权限码（角色名或细粒度码）；super_admin 永远放行 */
    permission?: string
    /** 顶栏 / 面包屑显示标题 */
    title?: string
    /** 是否展示在侧边栏 */
    hideInMenu?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  // 公开：登录
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true, title: 'Login' }
  },

  // 公开：403
  {
    path: '/403',
    name: 'Forbidden',
    component: Forbidden,
    meta: { public: true, title: 'Forbidden' }
  },

  // 主布局：8 个业务路由 + redirect
  {
    path: '/',
    component: MainLayout,
    redirect: '/realtime',
    children: [
      {
        path: 'realtime',
        name: 'RealTime',
        component: RealTime,
        meta: { permission: 'realtime', title: 'menu.realtime' }
      },
      {
        path: 'alarm',
        name: 'Alarm',
        component: Alarm,
        meta: { permission: 'alarm', title: 'menu.alarm' }
      },
      {
        path: 'defect',
        name: 'Defect',
        component: Defect,
        meta: { permission: 'defect', title: 'menu.defect' }
      },
      {
        path: 'account',
        name: 'Account',
        component: Account,
        meta: { permission: 'account', title: 'menu.account' }
      },
      {
        path: 'systemConfig',
        name: 'SystemConfig',
        component: SystemConfig,
        meta: { permission: 'systemConfig', title: 'menu.systemConfig' }
      },
      {
        path: 'log',
        name: 'Log',
        component: Log,
        meta: { permission: 'log', title: 'menu.log' }
      },
      {
        path: 'userManage',
        name: 'UserManage',
        component: UserManage,
        meta: { permission: 'userManage', title: 'menu.userManage' }
      },
      {
        path: 'screen',
        name: 'Screen',
        component: Screen,
        meta: { permission: 'screen', title: 'menu.screen', hideInMenu: true }
      }
    ]
  },

  // 兜底：未知路径 → /realtime（已登录）或 /login
  {
    path: '/:pathMatch(.*)*',
    redirect: '/realtime'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

/**
 * 守卫三步：
 *   1) public 路由放行；已登录访问 /login 跳 /realtime
 *   2) 非 public 无 satoken → /login
 *   3) 有 satoken 但无 meta.permission → /403
 */
router.beforeEach((to, _from, next) => {
  // W-FRONT-FLASH: 守卫改用 user store 判定登录态，不再直接读 cookie。
  // 旧版 getCookie('satoken') 在某些时序下偶发失败，导致已登录用户被踢到 /login；
  // 切到 /#/realtime 后 perm.has() 又因 store 未初始化返回 false，再被踢到 /403。
  // user store 由 Login.vue / fetchCurrent() 在 main.js 注册 pinia 之后写入，
  // isLoggedIn = Boolean(id && loaded) 与后端 /web/account/current 一致。
  let isLoggedIn = false
  let hasPermission = true
  try {
    const userStore = useUserStore()
    isLoggedIn = userStore.isLoggedIn
    const perm = usePermissionStore()
    const need = to.meta.permission
    if (need && perm) {
      hasPermission = perm.has(need)
    }
  } catch (e) {
    // store 还未注册等异常：保守放行（顶层 401 拦截会兜底）
    isLoggedIn = true
    hasPermission = true
  }

  if (to.meta.public) {
    if (isLoggedIn && to.name === 'Login') {
      return next({ name: 'RealTime' })
    }
    return next()
  }

  if (!isLoggedIn) {
    return next({ name: 'Login' })
  }

  if (!hasPermission) {
    return next({ name: 'Forbidden' })
  }

  return next()
})

export default router
