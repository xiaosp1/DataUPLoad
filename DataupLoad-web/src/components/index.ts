// =============================================================================
// W-FRONT-02-B 统一导出 5 个玻璃组件
// 提供两种导出：
//   1) named exports  → 直接 import { GlassCard } from '@/components'
//   2) default install → app.use(GlassComponents) 全局注册
// =============================================================================

import type { App, Plugin } from 'vue'

import GlassCard from './GlassCard.vue'
import GlassButton from './GlassButton.vue'
import GlassMenuItem from './GlassMenuItem.vue'
import GlassTable from './GlassTable.vue'
import GlassPage from './GlassPage.vue'
import GlassSkeletonTable from './GlassSkeletonTable.vue'

export { GlassCard, GlassButton, GlassMenuItem, GlassTable, GlassPage, GlassSkeletonTable }

export type GlassComponents = typeof GlassComponents

// 全局注册插件（Vue 3 Plugin 形态）
const GlassComponents: Plugin = {
  install(app: App) {
    app.component('GlassCard', GlassCard)
    app.component('GlassButton', GlassButton)
    app.component('GlassMenuItem', GlassMenuItem)
    app.component('GlassTable', GlassTable)
    app.component('GlassPage', GlassPage)
    app.component('GlassSkeletonTable', GlassSkeletonTable)
  }
}

export default GlassComponents
