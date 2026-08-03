<template>
  <!-- W-FRONT-02-E8：大屏模式路由（/screen）→ 隐藏 chrome，纯 router-view 全屏 -->
  <!-- W-FRONT-FLASH: 去掉 <transition> 包裹，避免守卫误踢 403 时的 fade 动画叠加 framenavigated 形成视觉闪烁 -->
  <router-view v-if="isScreen" v-slot="{ Component }">
    <component :is="Component" />
  </router-view>

  <div v-else class="main-layout">
    <!-- 背景装饰：与登录页风格保持一致，但更克制 -->
    <div class="main-layout__halo main-layout__halo--1" />
    <div class="main-layout__halo main-layout__halo--2" />

    <!-- 整布局浮在表面的大玻璃面板 -->
    <div class="main-layout__panel glass-panel">
      <!-- 左侧菜单 -->
      <aside class="main-layout__sidebar">
        <Sidebar />
      </aside>

      <!-- 右侧：顶栏 + 内容区 -->
      <div class="main-layout__main">
        <header class="main-layout__topbar">
          <Topbar />
        </header>

        <main class="main-layout__content">
          <!-- W-FRONT-FLASH: 去掉 <transition> 包裹，避免 fade 动画叠加 framenavigated 形成视觉闪烁 -->
          <router-view v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </main>
      </div>
    </div>

    <!-- W-RT-8: 报警徽章悬浮窗（玻璃风，可拖动；Teleport-to-body 不受 chrome 限制） -->
    <AlarmHint v-if="!isScreen" />
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-D 主布局
//   - 整布局浮在一个大玻璃面板上（参考 style-sample-main.png）
//   - 高度 calc(100vh - var(--space-6)*2) 留边距
//   - 左侧 Sidebar + 右侧 Topbar + router-view 内容区
//
// W-FRONT-02-E8 增量：/screen 路由（全屏沉浸式大屏）需隐藏 chrome
//   - 判断当前路由 path 是否以 /screen 开头：是 → 只渲染 router-view
//   - 不修改任何子组件；只是顶层分支判断
// =============================================================================
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'
// W-RT-8: 报警徽章悬浮窗（玻璃风，可拖动 + 角标）
import AlarmHint from '../components/AlarmHint.vue'

const route = useRoute()
const isScreen = computed(() => route.path === '/screen' || route.path.startsWith('/screen/'))
</script>

<style lang="scss" scoped>
.main-layout {
  position: relative;
  min-height: 100vh;
  width: 100%;
  padding: var(--space-6);
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  overflow: hidden;
}

// 背景装饰光晕（与登录页同款，半透明叠加）
.main-layout__halo {
  position: absolute;
  border-radius: 50%;
  // ===== W-FLASH-02: 整个画面闪 — 底层 halo 超大 blur(80px) 是 backdrop 采样负担 =====
  // backdrop-filter 每帧要重采样底部内容，blur(80px) 大圆 + 半透明叠加成本极高。
  // 降至 blur(40px) + 提为稳定合成层（will-change），视觉几乎不变但 GPU 负担大降。
  filter: blur(40px);
  will-change: transform;
  pointer-events: none;
  z-index: 0;
}
.main-layout__halo--1 {
  top: -10%;
  left: -10%;
  width: 480px;
  height: 480px;
  background: rgba(92, 225, 255, 0.18);
}
.main-layout__halo--2 {
  bottom: -15%;
  right: -10%;
  width: 520px;
  height: 520px;
  background: rgba(255, 110, 199, 0.16);
}

// 整布局外壳 = 大玻璃面板
.main-layout__panel {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  height: calc(100vh - var(--space-6) * 2);
  min-height: 600px;
  overflow: hidden;
  // ===== W-FLASH-02: 整个画面闪 — 提升为独立 GPU 合成层 =====
  // backdrop-filter 会把整个面板作为一个合成层；加 will-change + translateZ(0) 稳定合成层，
  // 避免内容区数据更新(WS 5s 推送)时反复触发整屏 backdrop 重采样。
  will-change: transform;
  transform: translateZ(0);
  // 让"内顶高光"效果继承 global.scss 里的 .glass-panel::before（这里再补一道）
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 35%);
    z-index: 0;
  }
}

// 侧栏
.main-layout__sidebar {
  position: relative;
  z-index: 1;
  flex: 0 0 220px;
  width: 220px;
  display: flex;
  flex-direction: column;
  padding: var(--space-5) var(--space-4);
  border-right: 1px solid var(--glass-border);
}

// 右侧主区
.main-layout__main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

// 顶栏
.main-layout__topbar {
  flex: 0 0 auto;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--glass-border);
}

// 内容区
.main-layout__content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-5);
  // ===== W-FLASH-02: 整个画面闪 — 内容区独立合成层 + 隔离 =====
  // contain: layout paint 让内容区自身的重绘不扩散到父玻璃面板（不触发整屏 backdrop 回溯）
  contain: layout paint;
  will-change: transform;
  transform: translateZ(0);
  // 自定义滚动条
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: var(--radius-pill);
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

// W-FRONT-FLASH: .fade-page-* class 已随 <transition> 一起删除

// 响应式：窄屏隐藏侧栏装饰
@media (max-width: 768px) {
  .main-layout {
    padding: var(--space-3);
  }
  .main-layout__panel {
    height: calc(100vh - var(--space-3) * 2);
  }
  .main-layout__sidebar {
    flex: 0 0 64px;
    width: 64px;
    padding: var(--space-3) var(--space-2);
  }
}
</style>
