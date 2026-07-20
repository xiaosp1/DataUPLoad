import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash history，方便部署到任意静态托管（无需服务端 fallback 改写）
// 路由路径：/ → Dashboard；/line → LineDetail；/defect → DefectQuery
const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '主大屏' }
  },
  {
    path: '/line',
    name: 'LineDetail',
    component: () => import('@/views/LineDetail.vue'),
    meta: { title: '产线详情' }
  },
  {
    path: '/line/:id',
    name: 'LineDetailById',
    component: () => import('@/views/LineDetail.vue'),
    meta: { title: '产线详情' }
  },
  {
    path: '/defect',
    name: 'DefectQuery',
    component: () => import('@/views/DefectQuery.vue'),
    meta: { title: '缺陷查询' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach((to) => {
  if (to.meta && to.meta.title) {
    document.title = `${to.meta.title} · 英科中控大屏`
  }
})

export default router
