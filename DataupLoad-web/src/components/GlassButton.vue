<template>
  <el-button
    v-bind="$attrs"
    :class="['glass-btn', `glass-btn--${variant}`]"
    :type="elType"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'primary' | 'default' | 'danger'
type ElType = 'primary' | 'default' | 'danger'

interface Props {
  /** 视觉变体：primary = 青色渐变，default = 玻璃面板，danger = 红色实心 */
  variant?: Variant
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default'
})

// Element Plus 原生 type 用作兜底（部分组件 hover 态继承）
const elType = computed<ElType>(() => props.variant)
</script>

<style lang="scss" scoped>
.glass-btn {
  position: relative;
  font-family: var(--font-family);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  padding: 10px 20px;
  height: auto;
  min-height: 40px;
  line-height: 1.2;
  transition: all var(--transition-base);
  border: 1px solid transparent;
  letter-spacing: 0.2px;
}

// ----- default：玻璃面板 -----
.glass-btn--default {
  background: var(--glass-bg);
  color: var(--text-primary);
  border-color: var(--glass-border);
  backdrop-filter: var(--glass-blur-light);

  &:hover {
    background: var(--glass-bg-hover);
    border-color: rgba(92, 225, 255, 0.4);
    color: var(--accent);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
    background: rgba(92, 225, 255, 0.12);
  }
}

// ----- primary：青色渐变（玻璃凸起） -----
.glass-btn--primary {
  background: var(--gradient-accent);
  color: var(--text-on-accent);
  border-color: rgba(92, 225, 255, 0.5);
  box-shadow: 0 4px 16px rgba(92, 225, 255, 0.30);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 22px rgba(92, 225, 255, 0.45);
    filter: brightness(1.05);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(92, 225, 255, 0.4);
    filter: brightness(0.95);
  }
}

// ----- danger：红色实心 -----
.glass-btn--danger {
  background: linear-gradient(135deg, var(--danger), #ff8a8d);
  color: #fff;
  border-color: rgba(255, 90, 95, 0.5);
  box-shadow: 0 4px 16px rgba(255, 90, 95, 0.30);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 22px rgba(255, 90, 95, 0.45);
    filter: brightness(1.05);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 10px rgba(255, 90, 95, 0.4);
    filter: brightness(0.95);
  }
}

// 禁用态统一
.glass-btn.is-disabled,
.glass-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  filter: none !important;
  box-shadow: none !important;
}
</style>
