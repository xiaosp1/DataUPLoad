import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './store'
import i18n from './i18n'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 1) Element Plus 基础样式
import 'element-plus/dist/index.css'
// 2) 全局 reset / body 深色渐变背景 / 玻璃工具类
import './styles/global.scss'
// 3) Element Plus 主题重写（必须放在 element-plus/dist/index.css 之后）
import './styles/element-overrides.scss'
// 4) 5 个玻璃组件
import GlassComponents, {
  GlassCard,
  GlassButton,
  GlassMenuItem,
  GlassTable,
  GlassPage
} from './components'

const app = createApp(App)

// 全局图标
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// 5 个玻璃组件：plugin 形式 + 兜底（保证无论 install 是否被调用都能用）
app.use(GlassComponents)
app.component('GlassCard', GlassCard)
app.component('GlassButton', GlassButton)
app.component('GlassMenuItem', GlassMenuItem)
app.component('GlassTable', GlassTable)
app.component('GlassPage', GlassPage)

app.use(router)
app.use(pinia)
app.use(i18n)
app.use(ElementPlus)

app.mount('#app')
