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
// - build 产物输出到 dist/，可直接被 EdgeHost 静态托管
export default defineConfig({
  base: '/',
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
    outDir: 'dist',
    emptyOutDir: true,
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
