# W-FRONT-01-D — 静态资源部署 + GET `/` 兜底

- **父工单**: W-FRONT-01（依赖 W-FRONT-01-C 完成）
- **目标**: `npm run build` → 产物进 `DataupLoad/src/main/resources/static/` → 重启 jar 后 `GET /` 返回 SPA 入口

## 任务清单

### D1. `vite.config.js` 加 build 配置

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    proxy: {
      '/web': {
        target: 'http://127.0.0.1:80',
        changeOrigin: false,
      },
    },
  },
  build: {
    outDir: '../DataupLoad/src/main/resources/static',
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        // 让 chunk 文件名带 hash（PSM 同款）
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
  },
})
```

### D2. `package.json` 加 build 脚本

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

### D3. 后端欢迎页兜底（**只有 Spring Boot 默认不生效时才需要**）

测试步骤：
1. `npm run build`
2. 检查 `E:\DEMO\数据采集\DataupLoad\src\main\resources\static\index.html` 是否存在
3. **不重启 jar**，先静态测：
   - Spring Boot 默认会把 `static/index.html` 当 Welcome Page
   - 如果 `GET /` 仍 404，**不要改 Java**，加一个 `WebMvcConfigurer` Bean：
     ```java
     // src/main/java/com/hikrobotics/solution/WebConfig.java
     package com.hikrobotics.solution;
     import org.springframework.context.annotation.Configuration;
     import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
     import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
     @Configuration
     public class WebConfig implements WebMvcConfigurer {
         @Override
         public void addViewControllers(ViewControllerRegistry registry) {
             registry.addViewController("/").setViewName("forward:/index.html");
         }
     }
     ```
   - 加完必须 `javac` 编译 + 重启 jar 验证

### D4. jar 重打包（**P0**）
- 当前 `target/DataupLoad-1.0-SNAPSHOT-20260723010315.jar` 是 7/23 老版本
- `npm run build` 完成后，**Maven 重新打包**：
  ```powershell
  cd E:\DEMO\数据采集\DataupLoad
  mvn clean package -DskipTests
  ```
  或者如果环境没有 mvn，用 `start.bat` 老板指定的 java 路径：
  ```powershell
  cd E:\DEMO\数据采集\DataupLoad
  mvn clean package -DskipTests
  ```
- 新 jar 默认名 `target/DataupLoad-1.0-SNAPSHOT.jar`

### D5. 重启服务
- 停当前服务（PID 10212）
- 启新 jar（用 `C:\hik\run-app.bat` 但注意它的 classpath 走 `target/classes`，不是 jar，需要老板确认是要 jar 启动还是 classpath 启动）
- **报告里写清楚现状**

## 不交付

- ❌ 任何新 API
- ❌ 静态资源 CDN（单 jar 内嵌）
- ❌ Gzip / 缓存策略（基础即可，**老板同意后**再做）

## 验收

1. `npm run build` 退出码 0
2. `E:\DEMO\数据采集\DataupLoad\src\main\resources\static\` 下有：
   - `index.html`（SPA 入口）
   - `js/` 目录（带 hash 的 chunks）
   - `assets/` 目录（CSS + 静态资源）
3. `mvn clean package -DskipTests` 成功，新 jar 生成
4. 重启后 `curl -i http://localhost/` → 200 + `Content-Type: text/html` + body 含 `<div id="app">`
5. `curl -i http://localhost/assets/xxx.css` → 200 + CSS
6. `curl -i http://localhost/web/auth/login` → **405**（这是对的，前端从不用 GET 它）

## 报告

`docs/work-orders/W-FRONT-01-D-report.md`：
- `npm run build` 输出（最后 30 行）
- `static/` 目录树
- mvn package 输出（最后 20 行）
- 6 条 curl 验证响应
- 新 jar 路径 + 启动 PID

## 风险

- **`mvn` 工具链**如果本机没装，要走老办法（直接用 `target/classes` classpath 启动，但那样 W-FRONT-01-A/B/C/D 编译产物生效依赖 `target/classes` 是新代码 + 静态资源被复制——这个需要排查 classpath 启动能不能识别 `src/main/resources/static/`）
- **老板的 `run-app.bat`** 用 `@.classpath.txt`（classpath 模式），静态资源应该在 classpath 里能找到 `static/index.html`——和 jar 模式效果应该一致，但要在报告里实测

## 耗时上限

45 分钟
