import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局 ECharts 注册（按需引入可减小 bundle；这里为方便使用全量）
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  LineChart,
  BarChart,
  PieChart,
  GaugeChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
  MarkLineComponent,
  ToolboxComponent
} from 'echarts/components'

import App from './App.vue'
import router from './router'
import './assets/styles.css'

// ECharts 按需注册
use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GaugeChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent,
  MarkLineComponent,
  ToolboxComponent
])

const app = createApp(App)

// 注册 Element Plus 所有图标（够用；icon 数量在 200 以内）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// Element Plus 深色主题
document.documentElement.classList.add('dark')

app.mount('#app')
