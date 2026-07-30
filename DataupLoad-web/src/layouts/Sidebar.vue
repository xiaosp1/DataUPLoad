<template>
  <div class="sidebar">
    <!-- 顶部品牌区 -->
    <div class="sidebar__brand">
      <div class="sidebar__logo">▣</div>
      <div class="sidebar__brand-text">
        <div class="sidebar__brand-title">{{ $t('app.name') }}</div>
        <div class="sidebar__brand-sub">v2</div>
      </div>
    </div>

    <!-- 滚动菜单容器 -->
    <nav class="sidebar__nav">
      <!-- 实时监控组 -->
      <div class="sidebar__group">
        <div class="sidebar__group-title">{{ $t('menu.groupMonitor') }}</div>
        <GlassMenuItem
          v-for="item in monitorItems"
          :key="item.name"
          :icon="item.icon"
          :active="isActive(item.name)"
          @click="go(item.name)"
        >
          {{ $t(item.label) }}
        </GlassMenuItem>
      </div>

      <!-- 系统管理组 -->
      <div class="sidebar__group">
        <div class="sidebar__group-title">{{ $t('menu.groupSystem') }}</div>
        <GlassMenuItem
          v-for="item in systemItems"
          :key="item.name"
          :icon="item.icon"
          :active="isActive(item.name)"
          @click="go(item.name)"
        >
          {{ $t(item.label) }}
        </GlassMenuItem>
      </div>
    </nav>

    <!-- 底部信息 -->
    <div class="sidebar__footer">
      <div class="sidebar__footer-dot" />
      <span>{{ $t('app.welcome') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-D 侧边栏
//   - 两组菜单：实时监控 / 系统管理
//   - 选中态：青色凸起药丸（GlassMenuItem 自带）
//   - 大屏模式 / 403 / 403 不进菜单
// =============================================================================
import { useRouter, useRoute } from 'vue-router'
import GlassMenuItem from '../components/GlassMenuItem.vue'

interface MenuItem {
  name: string
  label: string
  icon: string
}

const router = useRouter()
const route = useRoute()

const monitorItems: MenuItem[] = [
  { name: 'RealTime', label: 'menu.realtime', icon: '▣' },
  { name: 'Alarm',    label: 'menu.alarm',    icon: '⚠' },
  { name: 'Defect',   label: 'menu.defect',   icon: '✕' }
]

const systemItems: MenuItem[] = [
  { name: 'Account',      label: 'menu.account',      icon: '☷' },
  { name: 'SystemConfig', label: 'menu.systemConfig', icon: '⚙' },
  { name: 'Log',          label: 'menu.log',          icon: '⌘' },
  { name: 'UserManage',   label: 'menu.userManage',   icon: '◉' }
]

function isActive(name: string): boolean {
  return route.name === name
}

function go(name: string): void {
  if (route.name !== name) {
    router.push({ name })
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: var(--space-4);
  overflow: hidden;
}

// 品牌
.sidebar__brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid var(--glass-border);
}
.sidebar__logo {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-brand);
  color: var(--text-on-accent);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  box-shadow: 0 4px 16px rgba(92, 225, 255, 0.32);
}
.sidebar__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  min-width: 0;
}
.sidebar__brand-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar__brand-sub {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

// 菜单
.sidebar__nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding-right: 2px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.12) transparent;
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.12);
    border-radius: var(--radius-pill);
  }
}

.sidebar__group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.sidebar__group-title {
  padding: 0 var(--space-3);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: var(--font-weight-semibold);
}

// 底部
.sidebar__footer {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid var(--glass-border);
}
.sidebar__footer-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 8px rgba(95, 217, 127, 0.6);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

// 窄屏：隐藏文字
@media (max-width: 768px) {
  .sidebar__brand-text,
  .sidebar__group-title,
  .sidebar__footer span {
    display: none;
  }
  .sidebar__footer {
    justify-content: center;
  }
}
</style>
