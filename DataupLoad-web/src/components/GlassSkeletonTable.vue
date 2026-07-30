<!--
  W-PERF-D — 玻璃风骨架屏组件
  替代 Element Plus 的 v-loading，在表格初次加载时显示"已成型"的占位骨架
  让用户感觉页面"秒出"，而不是空白等待。

  风格与现有 GlassCard / GlassTable 完全对齐：
    - 玻璃面板 + backdrop-filter blur
    - 骨架色渐变高光扫过
    - 圆角 8px（与 el-table 一致）
    - 边框 1px 半透明白

  用法（5 个页面接入）：
    <GlassSkeletonTable v-if="loading" :columns="8" :rows="5" />
    <GlassTable v-else :data="rows" ...>...</GlassTable>
-->

<template>
  <div
    class="glass-skeleton-table"
    :style="containerStyle"
    :aria-busy="true"
    aria-label="loading"
  >
    <!-- 表头骨架（可选） -->
    <div v-if="hasHeader" class="glass-skeleton-table__header">
      <div
        v-for="col in columns"
        :key="`th-${col}`"
        class="glass-skeleton-table__cell glass-skeleton-table__cell--th"
      >
        <span class="glass-skeleton-table__bar glass-skeleton-table__bar--th" />
      </div>
    </div>

    <!-- 表体骨架 -->
    <div class="glass-skeleton-table__body">
      <div
        v-for="row in rows"
        :key="`tr-${row}`"
        class="glass-skeleton-table__row"
      >
        <div
          v-for="col in columns"
          :key="`td-${row}-${col}`"
          class="glass-skeleton-table__cell"
        >
          <span
            class="glass-skeleton-table__bar"
            :style="{ width: barWidthFor(col) }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 列数 */
  columns: number
  /** 骨架行数，默认 5 */
  rows?: number
  /** 是否显示表头骨架，默认 true */
  hasHeader?: boolean
  /** 容器高度，可选；不传则按 rows 自动撑开 */
  height?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  rows: 5,
  hasHeader: true,
  height: undefined
})

// 列宽微抖动：让骨架看起来"有内容变化"，而不是机械对齐
// 用一个简单的伪随机（基于列索引），避免 SSR/CSR 不一致就用固定模式
const widthPatterns = ['78%', '62%', '88%', '54%', '70%', '92%', '66%', '80%', '58%', '74%']

function barWidthFor(colIndex: number): string {
  // 第 1 列（序号）稍短；最后一列（操作）也稍短
  if (colIndex === 1) return '42%'
  if (colIndex === props.columns) return '60%'
  return widthPatterns[(colIndex - 1) % widthPatterns.length]
}

const containerStyle = computed(() => {
  if (props.height === undefined || props.height === null) return {}
  return {
    height: typeof props.height === 'number' ? `${props.height}px` : (props.height as string)
  }
})
</script>

<style lang="scss" scoped>
.glass-skeleton-table {
  position: relative;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur-light);
  -webkit-backdrop-filter: var(--glass-blur-light);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  box-shadow: var(--glass-shadow);
  overflow: hidden;
  padding: 0;

  // 顶部高光（与 GlassCard 一致）
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 30%);
    opacity: 0.6;
    z-index: 1;
  }

  &__header {
    display: flex;
    align-items: center;
    background: rgba(255, 255, 255, 0.06);
    border-bottom: 1px solid rgba(255, 255, 255, 0.10);
    padding: 14px 0;
  }

  &__row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);

    &:last-child {
      border-bottom: none;
    }
  }

  &__cell {
    flex: 1;
    min-width: 0;
    padding: 14px 16px;
    display: flex;
    align-items: center;

    &--th {
      justify-content: flex-start;
    }
  }

  &__bar {
    display: block;
    height: 12px;
    border-radius: 6px;
    background: linear-gradient(
      90deg,
      rgba(255, 255, 255, 0.06) 0%,
      rgba(255, 255, 255, 0.12) 50%,
      rgba(255, 255, 255, 0.06) 100%
    );
    background-size: 200% 100%;
    animation: skeleton-loading 1.4s ease infinite;

    &--th {
      height: 10px;
      width: 60% !important;
      opacity: 0.85;
    }
  }
}

// 关键帧：渐变高光从左到右扫过
@keyframes skeleton-loading {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}

// 减少动画偏好：尊重系统设置
@media (prefers-reduced-motion: reduce) {
  .glass-skeleton-table__bar {
    animation: none;
  }
}
</style>
