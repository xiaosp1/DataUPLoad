<!--
  W-RT-3 中栏 4 区面板 - ②24 小时缺陷热力图（玻璃风）

  布局：4 行 × 6 列 = 24 格（每格 1 小时）
  数据源：props.hourly[24] = number[] (0-23 hour defect counts)
    - 后端暂无聚合时，用缺陷类型 defects[] + 当前小时 ngCount 派生稳定占位
  颜色：颜色越深 = 缺陷数越多（玻璃风：青/绿/黄/红渐变）
  鼠标悬停：当前小时数字浮起 + 颜色高亮
-->
<template>
  <GlassCard class="ldg-card" :hover="true">
    <div class="ldg-card__inner">
      <div class="ldg-card__head">
        <h4 class="ldg-card__title">
          <span class="ldg-card__icon">🔥</span>
          {{ $t('realtime.detail.defect') }}
        </h4>
        <div class="ldg-card__legend">
          <span class="ldg-card__legend-label">0</span>
          <span
            v-for="(c, idx) in legendColors"
            :key="idx"
            class="ldg-card__legend-cell"
            :style="{ background: c }"
          />
          <span class="ldg-card__legend-label">{{ maxVal }}</span>
        </div>
      </div>

      <div class="ldg-card__grid">
        <div
          v-for="(val, hour) in cells"
          :key="hour"
          class="ldg-card__cell"
          :style="{ background: cellColor(val) }"
          :title="`${String(hour).padStart(2, '0')}:00 — ${val} defects`"
        >
          <span class="ldg-card__cell-hour">{{ String(hour).padStart(2, '0') }}</span>
          <span class="ldg-card__cell-val">{{ val }}</span>
        </div>
      </div>
    </div>
  </GlassCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import GlassCard from './GlassCard.vue'

const props = withDefaults(defineProps<{
  /** 24 小时缺陷数组（0-23） */
  hourly?: number[]
}>(), {
  hourly: () => []
})

/** 24 格；缺位补 0 */
const cells = computed<number[]>(() => {
  const arr = Array.from({ length: 24 }, (_, h) => {
    const v = props.hourly?.[h]
    return Number.isFinite(Number(v)) ? Number(v) : 0
  })
  return arr
})

const maxVal = computed(() => {
  const m = Math.max(0, ...cells.value)
  return m
})

/** 渐变图例色（玻璃风冷→暖） */
const legendColors = [
  'rgba(92, 225, 255, 0.18)',
  'rgba(92, 225, 255, 0.45)',
  'rgba(95, 217, 127, 0.55)',
  'rgba(255, 183, 77, 0.7)',
  'rgba(255, 90, 95, 0.85)'
]

/**
 * 单格颜色：按数值占比，从 0 (透明) → 暖红
 * 使用字符串色（避免与 SCSS 变量耦合）；用 rgba 透明度叠加。
 */
function cellColor(v: number): string {
  if (maxVal.value === 0) return 'rgba(255, 255, 255, 0.04)'
  const ratio = Math.max(0, Math.min(1, v / maxVal.value))
  if (ratio === 0) return 'rgba(255, 255, 255, 0.04)'
  if (ratio < 0.25) return `rgba(92, 225, 255, ${0.15 + ratio * 1.2})`
  if (ratio < 0.5) return `rgba(95, 217, 127, ${0.20 + ratio * 0.8})`
  if (ratio < 0.75) return `rgba(255, 183, 77, ${0.30 + ratio * 0.6})`
  return `rgba(255, 90, 95, ${0.45 + ratio * 0.5})`
}
</script>

<style lang="scss" scoped>
.ldg-card {
  width: 100%;
  height: 100%;
}
.ldg-card__inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
}
.ldg-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.ldg-card__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  letter-spacing: 0.2px;
}
.ldg-card__icon {
  font-size: 16px;
  filter: drop-shadow(0 2px 6px rgba(255, 90, 95, 0.3));
}

.ldg-card__legend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.ldg-card__legend-label {
  font-size: 10px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.3px;
}
.ldg-card__legend-cell {
  display: inline-block;
  width: 12px;
  height: 10px;
  border-radius: 2px;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.ldg-card__grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-auto-rows: 1fr;
  gap: 6px;
  flex: 1;
  min-height: 0;
}

.ldg-card__cell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
  transition: transform var(--transition-fast), border-color var(--transition-base);
  cursor: default;
  font-variant-numeric: tabular-nums;
}
.ldg-card__cell:hover {
  transform: translateY(-1px);
  border-color: rgba(92, 225, 255, 0.5);
  z-index: 1;
}
.ldg-card__cell-hour {
  font-size: 9px;
  letter-spacing: 0.4px;
  color: rgba(255, 255, 255, 0.65);
  text-transform: uppercase;
}
.ldg-card__cell-val {
  font-size: 13px;
  font-weight: var(--font-weight-bold);
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}
</style>
