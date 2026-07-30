# W-FRONT-01-A — 前端工程脚手架

- **父工单**: W-FRONT-01
- **目标**: 在 `E:\DEMO\数据采集\DataupLoad-web\` 新建独立前端工程，`npm install` + `npm run dev` 跑通空壳
- **依赖**（lockfile 由你定，**不要**用 latest 漂移）：
  - `vue@^3.4.0`
  - `vue-router@^4.3.0`
  - `pinia@^2.1.7`
  - `element-plus@^2.7.0`
  - `@element-plus/icons-vue@^2.3.1`
  - `axios@^1.7.0`
  - `vue-i18n@^9.13.0`
  - 开发依赖：`vite@^5.3.0`、`@vitejs/plugin-vue@^5.0.0`、`sass@^1.77.0`
- **目录结构**（必须严格遵守）：
  ```
  E:\DEMO\数据采集\DataupLoad-web\
    package.json
    vite.config.js
    index.html              ← Vite 入口（开发用，不是部署用的那个）
    src/
      main.js               ← createApp(App).use(router).use(pinia).use(i18n).use(ElementPlus)
      App.vue               ← <router-view />
      router/
        index.js            ← 空 routes 数组占位
      api/
        http.js             ← 空 axios 实例占位
        login.js            ← 空 login 函数占位（避免 W-FRONT-01-B 还要找文件）
      views/
        Login.vue           ← 占位：<div>Login Placeholder</div>
        RealTime.vue        ← 占位
      i18n/
        index.js            ← 空 createI18n 占位
      store/
        index.js            ← 空 pinia 占位
      assets/
        .gitkeep
  ```
- **`vite.config.js` 关键字段**：
  - `base: '/'`（部署到根路径）
  - `server.port: 5173`、`server.host: '127.0.0.1'`、`server.proxy['/web'] = 'http://localhost:80'`（开发时跨域代理到后端）
  - **不要**在这里配置 `build.outDir = '../DataupLoad/src/main/resources/static'`（那是 W-FRONT-01-D 的事）
- **`main.js` 必装**：router + pinia + i18n + ElementPlus + ElementPlusIconsVue（后面几张子单都依赖这些挂载点，提前装好）
- **不交付**：
  - ❌ 任何业务逻辑
  - ❌ 登录表单 UI（W-FRONT-01-B）
  - ❌ i18n 字符串（W-FRONT-01-C）
  - ❌ 任何后端改动
  - ❌ build 配置（W-FRONT-01-D）
- **验收**（必须跑命令截图）：
  1. `cd E:\DEMO\数据采集\DataupLoad-web && npm install` 退出码 0
  2. `npm run dev` 启动 → 浏览器访问 `http://127.0.0.1:5173/` → 看到 `<div>Login Placeholder</div>` 或 App.vue 默认内容，无 console error
  3. `package-lock.json` 已生成（不要 `package-lock.json` 缺席）
  4. 不动 `DataupLoad/` 任何文件
- **报告**: `docs/work-orders/W-FRONT-01-A-report.md`，含 `npm install` 输出（最后 20 行）+ 浏览器截图 + 目录树
- **耗时上限**: 30 分钟（含 npm install）
