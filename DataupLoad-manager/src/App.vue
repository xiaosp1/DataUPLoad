<template>
  <div class="app-shell">
    <!-- 背景光晕 -->
    <div class="halo halo--1"></div>
    <div class="halo halo--2"></div>

    <!-- 侧边栏 -->
    <aside class="sidebar glass">
      <div class="logo">
        <div class="logo__icon">🏭</div>
        <div class="logo__text">
          <div class="logo__title">DataupLoad</div>
          <div class="logo__sub">部署管理</div>
        </div>
      </div>
      <nav class="nav">
        <div
          v-for="item in menus"
          :key="item.key"
          class="nav__item"
          :class="{ 'nav__item--active': active === item.key }"
          @click="active = item.key"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </nav>
      <div class="sidebar__footer">
        <div class="dot" :class="overview?.server?.running ? 'dot--ok' : 'dot--err'"></div>
        <span>后端 {{ overview?.server?.running ? '运行中' : '已停止' }}</span>
      </div>
    </aside>

    <!-- 主区 -->
    <main class="main">
      <header class="topbar">
        <div class="topbar__title">{{ currentMenu?.label }}</div>
        <div class="topbar__right">
          <el-tag size="small" :type="overview?.pgRunning ? 'success' : 'danger'" effect="dark">
            PG {{ overview?.pgRunning ? '运行中' : '未运行' }}
          </el-tag>
          <el-tag size="small" type="info" effect="dark" v-if="overview">
            产线连接 {{ overview.connections }}
          </el-tag>
        </div>
      </header>

      <section class="content">
        <Home v-if="active === 'home'" :overview="overview" @refresh="loadOverview" />
        <Server v-else-if="active === 'server'" />
        <Database v-else-if="active === 'database'" />
        <Config v-else-if="active === 'config'" />
        <Deploy v-else-if="active === 'deploy'" />
        <Settings v-else-if="active === 'settings'" />
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Monitor, Cpu, Coin, Tools, Setting } from '@element-plus/icons-vue'
import Home from './views/Home.vue'
import Server from './views/Server.vue'
import Database from './views/Database.vue'
import Config from './views/Config.vue'
import Deploy from './views/Deploy.vue'
import Settings from './views/Settings.vue'

const menus = [
  { key: 'home', label: '总览', icon: Monitor },
  { key: 'server', label: '后端服务', icon: Cpu },
  { key: 'database', label: '数据库', icon: Coin },
  { key: 'config', label: '参数配置', icon: Tools },
  { key: 'deploy', label: '部署体检', icon: Setting },
  { key: 'settings', label: '系统设置', icon: Setting }
]

const active = ref('home')
const overview = ref(null)
const currentMenu = computed(() => menus.find(m => m.key === active.value))

async function loadOverview() {
  overview.value = await window.manager.svcOverview()
}

let timer = null
onMounted(() => {
  loadOverview()
  timer = setInterval(loadOverview, 10000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.app-shell {
  position: relative;
  height: 100%;
  display: flex;
  overflow: hidden;
}
.halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
}
.halo--1 { width: 480px; height: 480px; background: rgba(59, 130, 246, 0.16); top: -160px; right: -120px; }
.halo--2 { width: 420px; height: 420px; background: rgba(34, 197, 94, 0.10); bottom: -160px; left: -100px; }

.sidebar {
  position: relative;
  z-index: 1;
  width: 216px;
  margin: 14px;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.logo { display: flex; align-items: center; gap: 12px; padding: 4px 8px 20px; border-bottom: 1px solid var(--glass-border); }
.logo__icon { font-size: 28px; }
.logo__title { font-size: 16px; font-weight: 700; letter-spacing: 1px; }
.logo__sub { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.nav { flex: 1; padding-top: 16px; display: flex; flex-direction: column; gap: 6px; }
.nav__item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-radius: 10px;
  color: var(--text-secondary); cursor: pointer;
  font-size: 14px; transition: all 0.2s;
}
.nav__item:hover { background: rgba(59, 130, 246, 0.08); color: var(--text-primary); }
.nav__item--active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.22), rgba(59, 130, 246, 0.08));
  color: var(--brand-light);
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.25);
}
.sidebar__footer {
  display: flex; align-items: center; padding: 12px 10px;
  font-size: 12px; color: var(--text-secondary);
  border-top: 1px solid var(--glass-border);
}

.main { position: relative; z-index: 1; flex: 1; display: flex; flex-direction: column; margin: 14px 14px 14px 0; min-width: 0; }
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 6px 14px;
}
.topbar__title { font-size: 18px; font-weight: 700; letter-spacing: 0.5px; }
.topbar__right { display: flex; gap: 8px; }
.content { flex: 1; overflow-y: auto; min-height: 0; }
</style>
