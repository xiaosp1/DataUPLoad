<template>
  <el-container class="layout-root">
    <!-- 顶部 Header -->
    <el-header class="layout-header" height="60px">
      <div class="header-left">
        <el-icon class="logo-icon"><Histogram /></el-icon>
        <span class="header-title">英科中控大屏</span>
        <span class="header-version">v0.3</span>
      </div>
      <div class="header-center">
        <span class="status-dot" :class="{ online: health.ok, offline: !health.ok }"></span>
        <span class="status-text">{{ health.text }}</span>
        <span class="header-time">{{ now }}</span>
      </div>
      <div class="header-right">
        <el-button text :icon="Refresh" @click="checkHealth">刷新</el-button>
      </div>
    </el-header>

    <el-container class="layout-body">
      <!-- 左侧菜单 -->
      <el-aside class="layout-aside" width="180px">
        <el-menu
          :default-active="$route.path"
          router
          background-color="#0f2942"
          text-color="#e0e6ed"
          active-text-color="#1976d2"
        >
          <el-menu-item index="/">
            <el-icon><DataLine /></el-icon>
            <span>主大屏</span>
          </el-menu-item>
          <el-menu-item index="/line">
            <el-icon><VideoCamera /></el-icon>
            <span>产线详情</span>
          </el-menu-item>
          <el-menu-item index="/defect">
            <el-icon><Document /></el-icon>
            <span>缺陷查询</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主区域 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Histogram, DataLine, VideoCamera, Document, Refresh } from '@element-plus/icons-vue'
import { checkApiHealth } from '@/api'

const now = ref('')
const health = ref({ ok: false, text: '未连接' })

let timer = null

function tickClock() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  now.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function checkHealth() {
  try {
    const ok = await checkApiHealth()
    health.value = { ok, text: ok ? 'EdgeHost 在线' : 'EdgeHost 离线（走 mock 数据）' }
  } catch {
    health.value = { ok: false, text: 'EdgeHost 离线（走 mock 数据）' }
  }
}

onMounted(() => {
  tickClock()
  timer = setInterval(tickClock, 1000)
  checkHealth()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.layout-root {
  height: 100vh;
  background: #0a1929;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #0f2942 0%, #102e4d 100%);
  border-bottom: 1px solid #1a3a5c;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #1976d2;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #e0e6ed;
  letter-spacing: 1px;
}

.header-version {
  font-size: 12px;
  color: #6b8caf;
  border: 1px solid #1a3a5c;
  border-radius: 3px;
  padding: 1px 6px;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: #b0bec5;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #6b8caf;
  box-shadow: 0 0 6px currentColor;
}

.status-dot.online {
  background: #67c23a;
  animation: pulse 2s infinite;
}

.status-dot.offline {
  background: #f56c6c;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-family: 'Microsoft YaHei', sans-serif;
}

.header-time {
  font-family: 'DIN Alternate', 'Roboto Mono', monospace;
  font-size: 16px;
  color: #e0e6ed;
  font-weight: 500;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
}

.layout-body {
  height: calc(100vh - 60px);
}

.layout-aside {
  background: #0f2942;
  border-right: 1px solid #1a3a5c;
}

.layout-aside :deep(.el-menu) {
  border-right: none;
}

.layout-main {
  padding: 16px;
  background: #0a1929;
  overflow: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
