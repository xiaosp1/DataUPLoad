# W-FRONT-02-A 脚手架 brief

- **任务**: 在 `E:\DEMO\数据采集\DataupLoad-web\` 创建 Vite + Vue 3 + Element Plus + Router + Pinia 脚手架
- **依赖**: 无
- **耗时上限**: 30 分钟
- **必读**: 
  - W-FRONT-02-brief.md
  - ADR-0016 (前端对齐 PSM SPA)
  - ADR-0018 (方案 X-1 临时过渡)

## done criteria（PM 验收逐条勾选）

- [ ] **① npm install 成功** — DataupLoad-web/package.json 含 vite + vue@3 + vue-router@4 + pinia + element-plus + axios
- [ ] **② npm run dev 起在 5173** — DataupLoad-web/.gitignore 含 node_modules/, dist/
- [ ] **③ GET / 返回 Vue 3 默认页** — 启动 dev 后 curl http://localhost:5173/ 返回包含 `<div id="app"></div>` 的 HTML
- [ ] **④ 控制台 0 error** — `verify-w-front-02-A.ps1` 用 curl 抓页面无 404 资源
- [ ] **⑤ Element Plus 加载成功** — 随便写一个 `<el-button>点我</el-button>` 在 App.vue 验证组件库通

## 必产出

1. `E:\DEMO\数据采集\DataupLoad-web\package.json` — 含 vite/vue/element-plus/router/pinia/axios
2. `E:\DEMO\数据采集\DataupLoad-web\vite.config.js` — 端口 5173 + 代理 /web → http://localhost
3. `E:\DEMO\数据采集\DataupLoad-web\src\main.js` — 注册 router + pinia + element-plus
4. `E:\DEMO\数据采集\DataupLoad-web\src\App.vue` — 至少一个 `<el-button>` 验证组件库
5. `E:\DEMO\数据采集\DataupLoad-web\src\router\index.js` — 空路由表（含一个 `/` 占位）
6. `E:\DEMO\数据采集\DataupLoad-web\index.html` — Vue 3 标准入口
7. **`E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-A-report.md`** — done criteria 逐条勾选 + 启动日志截图

## 必读的踩坑提示

### vite.config.js 代理
```js
export default {
  server: {
    port: 5173,
    proxy: {
      '/web': {
        target: 'http://localhost',
        changeOrigin: true,
        // secure: false  // 后端是 http
      },
      '/js': 'http://localhost',
      '/assets': 'http://localhost',
      '/version.json': 'http://localhost'
    }
  }
}
```
**不代理** `Browser.js`、`AI.png`（已放在 web/js/，vite dev 直接读 web/ 即可）

### Element Plus 自动按需引入（不要全量）
```js
// main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import id from 'element-plus/es/locale/lang/id'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })  // 默认中文
```

### Pinia 风格
```js
// stores/user.js
import { defineStore } from 'pinia'
export const useUserStore = defineStore('user', {
  state: () => ({ user: null, satoken: '' }),
  actions: { setUser(u) { this.user = u }, setToken(t) { this.satoken = t } }
})
```

### Router hash mode
```js
const router = createRouter({
  history: createWebHashHistory(),  // hash mode 不需要 nginx 重写
  routes: [{ path: '/', component: Home }]
})
```

## PM 验收脚本

```powershell
# scripts/verify-w-front-02-A.ps1
# 调用: powershell -ExecutionPolicy Bypass -File scripts/verify-w-front-02-A.ps1
```

执行后**预期全部 PASS**：
```
✅ CHECK 1: package.json 存在
✅ CHECK 2: 含 vite@^5
✅ CHECK 3: 含 vue@^3
✅ CHECK 4: 含 element-plus
✅ CHECK 5: 含 vue-router
✅ CHECK 6: 含 pinia
✅ CHECK 7: 含 axios
✅ CHECK 8: vite.config.js 存在
✅ CHECK 9: src/main.js 存在
✅ CHECK 10: src/App.vue 存在
✅ CHECK 11: npm install 已执行（node_modules/ 存在）
✅ CHECK 12: npm run dev 启动中（PID 占用 5173）
✅ CHECK 13: GET http://localhost:5173/ 200
✅ CHECK 14: 返回 HTML 含 <div id="app">
✅ CHECK 15: Element Plus CSS 引用 OK
```

## 不在本子单范围

- 不实现 Login.vue（属于 B 子单）
- 不实现业务路由（属于 C 子单）
- 不实现业务页面（属于 D 子单）
- 不打包部署（属于 E 子单）

## 完成后回 PM

请回复：
> "W-FRONT-02-A 完成。report 路径: docs/work-orders/W-FRONT-02-A-report.md。verify 脚本: scripts/verify-w-front-02-A.ps1。dev server PID: <PID>"

PM 验收通过后才下 W-FRONT-02-B。

