<template>
  <div
    class="glass-card"
    :class="[
      { 'glass-card--hoverable': hover },
      { 'glass-card--solid': !glass }
    ]"
    :style="cardStyle"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 内边距像素（默认 20），传 0 也接受 */
  padding?: number
  /** 鼠标悬停时整体抬升 + 亮起边框 */
  hover?: boolean
  /** true（默认）= 玻璃面板；false = 纯黑半透明底（适合嵌套在玻璃面板里） */
  glass?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  padding: 20,
  hover: false,
  glass: true
})

const cardStyle = computed(() => ({
  padding: typeof props.padding === 'number' ? `${props.padding}px` : (props.padding as any)
}))
</script>

<style lang="scss" scoped>
.glass-card {
  position: relative;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  color: var(--text-primary);
  transition:
    transform var(--transition-base),
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    background var(--transition-base);
  overflow: hidden;
}

// 子元素上方有一道高光，体现苹果系玻璃"内顶高光"
.glass-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.10), transparent 35%);
  opacity: 0.7;
}

.glass-card--solid {
  background: rgba(0, 0, 0, 0.25);
}

.glass-card--hoverable {
  cursor: pointer;
}

.glass-card--hoverable:hover {
  transform: translateY(-2px);
  border-color: rgba(92, 225, 255, 0.4);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(92, 225, 255, 0.18);
  background: var(--glass-bg-hover);
}
</style>
