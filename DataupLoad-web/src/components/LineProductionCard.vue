<!--
  W-RT-3 中栏 4 区面板 - ①生产信息卡（玻璃风）

  数据源：
    - props.line.realtime.{total, ngCount, successCount, efficiency, occupancy}
    - 良品 = total - ngCount（兜底）
    - 效率 = efficiency（个/分）
    - 当前产品图（占位：emoji + lineNo-faceNo）

  设计要点：
    - 大数字 + 小标签（数字采用 tabular-nums 对齐）
    - 当前产品图：左侧 emoji 方块，右侧 lineNo-faceNo + 产品代号
    - 玻璃面板 + 内顶高光（与 PSM 玻璃风一致）
-->
<template>
  <GlassCard class="lp-card" :hover="true">
    <div class="lp-card__inner">
      <!-- 头部：标题 + 当前产品图 -->
      <div class="lp-card__head">
        <h4 class="lp-card__title">
          <span class="lp-card__icon">📦</span>
          {{ $t('realtime.detail.production') }}
        </h4>
        <div class="lp-card__product">
          <span class="lp-card__product-thumb">🏭</span>
          <div class="lp-card__product-meta">
            <span class="lp-card__product-line">{{ lineName }}</span>
            <span class="lp-card__product-sub">{{ productSku }}</span>
          </div>
        </div>
      </div>

      <!-- 主体：4 个数字 -->
      <div class="lp-card__grid">
        <div class="lp-card__metric" data-tone="cyan">
          <span class="lp-card__metric-num">{{ formatNum(total) }}</span>
          <span class="lp-card__metric-label">{{ $t('realtime.detail.total') }}</span>
        </div>
        <div class="lp-card__metric" data-tone="green">
          <span class="lp-card__metric-num">{{ formatNum(good) }}</span>
          <span class="lp-card__metric-label">{{ $t('realtime.detail.good') }}</span>
        </div>
        <div class="lp-card__metric" data-tone="red">
          <span class="lp-card__metric-num">{{ formatNum(bad) }}</span>
          <span class="lp-card__metric-label">{{ $t('realtime.detail.bad') }}</span>
        </div>
        <div class="lp-card__metric" data-tone="orange">
          <span class="lp-card__metric-num">{{ efficiencyText }}</span>
          <span class="lp-card__metric-label">{{ $t('realtime.detail.efficiency') }}</span>
        </div>
      </div>
    </div>
  </GlassCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import GlassCard from './GlassCard.vue'
import type { LineListItem } from '../stores/line'
import { successCountOf } from '../api/realtime'

const props = defineProps<{
  line: LineListItem | null
}>()

const rt = computed(() => props.line?.realtime || null)
const total = computed(() => Number(rt.value?.total ?? 0))
const bad = computed(() => Number(rt.value?.ngCount ?? 0))
const good = computed(() => successCountOf(rt.value))
const efficiency = computed(() => Number(rt.value?.efficiency ?? 0))

const efficiencyText = computed(() =>
  efficiency.value > 0 ? efficiency.value.toFixed(2) : '0.00'
)

const lineName = computed(() => {
  if (!props.line) return '—'
  return `${props.line.lineNo}-${props.line.faceNo}`
})

const productSku = computed(() => {
  if (!props.line) return '—'
  return props.line.name || '—'
})

function formatNum(n: number): string {
  if (!Number.isFinite(n)) return '0'
  return Math.round(n).toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
.lp-card {
  width: 100%;
  height: 100%;
}
.lp-card__inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
}
.lp-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.lp-card__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  letter-spacing: 0.2px;
}
.lp-card__icon {
  font-size: 16px;
  filter: drop-shadow(0 2px 6px rgba(92, 225, 255, 0.3));
}
.lp-card__product {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-pill);
}
.lp-card__product-thumb {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(92, 225, 255, 0.25), rgba(95, 217, 127, 0.18));
  font-size: 13px;
}
.lp-card__product-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}
.lp-card__product-line {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  letter-spacing: 0.2px;
}
.lp-card__product-sub {
  font-size: 10px;
  color: var(--text-secondary);
  letter-spacing: 0.4px;
}

.lp-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  flex: 1;
}
.lp-card__metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.lp-card__metric-num {
  font-size: 22px;
  font-weight: var(--font-weight-bold);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}
.lp-card__metric-label {
  font-size: 10px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.lp-card__metric[data-tone='cyan'] .lp-card__metric-num {
  background: linear-gradient(135deg, #5ce1ff, #74e0ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.lp-card__metric[data-tone='green'] .lp-card__metric-num {
  background: linear-gradient(135deg, #5fd97f, #5ce1ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.lp-card__metric[data-tone='red'] .lp-card__metric-num {
  background: linear-gradient(135deg, #ff5a5f, #ffb74d);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.lp-card__metric[data-tone='orange'] .lp-card__metric-num {
  background: linear-gradient(135deg, #ff9f43, #ff6ec7);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
</style>
