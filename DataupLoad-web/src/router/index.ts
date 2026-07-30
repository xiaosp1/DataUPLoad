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

/**
 * 读 cookie 的同名工具（不放 utils 里，路由守卫自身就要用，避免循环依赖）
 * **只**读 satoken，不读 token / 不读 localStorage。
 */
function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const m = document.cookie.match(
    new RegExp('(?:^|;\\s*)' + name + '=([^;]*)')
  )
  return m ? decodeURIComponent(m[1]) : null
}

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
  const satoken = getCookie('satoken')

  if (to.meta.public) {
    if (satoken && to.name === 'Login') {
      return next({ name: 'RealTime' })
    }
    return next()
  }

  if (!satoken) {
    return next({ name: 'Login' })
  }

  // 已登录：检查权限
  const need = to.meta.permission
  if (need) {
    const perm = usePermissionStore()
    // pinia 必须在 app.use(pinia) 之后才能拿到 store；正常情况下 main.js 已先注册
    if (perm && !perm.has(need)) {
      return next({ name: 'Forbidden' })
    }
  }

  return next()
})

export default router
