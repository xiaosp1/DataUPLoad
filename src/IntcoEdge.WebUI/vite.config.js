import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'

// https://vitejs.dev/config/
// Vite 5 + Vue 3 配置。
// - 开发服务器跑在 5289（与 EdgeHost 5288 错开）
// - /api/* 代理到 EdgeHost 5288，前端只需用相对路径
// - build 产物直接输出到 EdgeHost 的 wwwroot/，随 .NET 项目一起 publish
//   → 访问 EdgeHost 5288 根路径即可拿到大屏（无需 nginx）
// - base: './' 让产物中 <script src="/assets/..."> 变成 <script src="./assets/...">
//   以兼容 ASP.NET Core StaticFiles 中间件
export default defineConfig({
  base: './',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5289,
    strictPort: true,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:5288',
        changeOrigin: true
      },
      '/health': {
        target: 'http://localhost:5288',
        changeOrigin: true
      }
    }
  },
  build: {
    // 产物输出到 EdgeHost 的 wwwroot/，让 ASP.NET Core StaticFiles 直接托管
    outDir: '../IntcoEdge.EdgeHost/wwwroot',
    emptyOutDir: true,
    assetsDir: 'assets',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 把 echarts 单独打包，避免主 chunk 过大
        manualChunks: {
          'vendor-echarts': ['echarts', 'vue-echarts'],
          'vendor-element-plus': ['element-plus', '@element-plus/icons-vue']
        }
      }
    }
  }
})
