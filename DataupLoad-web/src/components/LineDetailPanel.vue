<!--
  W-RT-3 中栏 4 区面板主容器（玻璃风）

  4 区布局 (2x2 grid):
    ┌────────────────┬────────────────┐
    │ ① 生产信息     │ ② 缺陷网格     │
    │ (Production)   │ (Defect 24h)   │
    ├────────────────┼────────────────┤
    │ ③ 设备三联     │ ④ 时间/班次    │
    │ (Device)       │ (Shift/Clock)  │
    └────────────────┴────────────────┘

  数据来源：props.line = lineStore.selectedLine（LineListItem）
  - 当前产品 + 总数/良品/次品/效率  → LineProductionCard
  - 24 小时缺陷热力图  → LineDefectGrid（hourly 数组；后端暂无聚合时用占位）
  - 摄像机/编码器/PLC  → LineDeviceStatus（占位：PLC = (total>0 && efficiency>0)）
  - 当前时间 + 班次 + 运行时长  → LineShiftClock
-->
<template>
  <div class="line-detail-panel">
    <!-- 空状态：提示选择产线 -->
    <GlassCard v-if="!line" class="line-detail-panel__empty" :hover="false">
      <div class="line-detail-panel__empty-inner">
        <span class="line-detail-panel__empty-icon">👈</span>
        <div class="line-detail-panel__empty-text">
          <h4 class="line-detail-panel__empty-title">请在左侧选择产线</h4>
          <p class="line-detail-panel__empty-sub">Select a production line from the left</p>
        </div>
      </div>
    </GlassCard>

    <!-- 4 区面板 -->
    <div v-else class="line-detail-panel__grid">
      <!-- 头部条：线别信息 -->
      <GlassCard class="line-detail-panel__header" :hover="false">
        <div class="line-detail-panel__header-inner">
          <div class="line-detail-panel__header-left">
            <span
              class="line-detail-panel__index"
              :style="{ background: indexBg }"
            >{{ lineIndex }}</span>
            <div class="line-detail-panel__meta">
              <span class="line-detail-panel__line-no">{{ line.lineNo }}-{{ line.faceNo }}</span>
              <span class="line-detail-panel__line-name">{{ line.name }}</span>
            </div>
          </div>
          <div class="line-detail-panel__header-right">
            <span class="line-detail-panel__pill" data-tone="cyan">
              {{ line.hourDefectCount }} defects / h
            </span>
            <span class="line-detail-panel__pill" data-tone="orange">
              {{ line.hourRemoveCount }} removed / h
            </span>
          </div>
        </div>
      </GlassCard>

      <!-- ① 生产信息 -->
      <LineProductionCard :line="line" />

      <!-- ② 缺陷热力图 -->
      <LineDefectGrid :hourly="hourlyData" @defect-click="(p) => emit('defect-click', p)" />

      <!-- ③ 设备状态 -->
      <LineDeviceStatus
        :total="totalNum"
        :efficiency="effNum"
      />

      <!-- ④ 时间/班次 -->
      <LineShiftClock :start-time="line.realtime?.startTime || ''" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import GlassCard from './GlassCard.vue'
import LineProductionCard from './LineProductionCard.vue'
import LineDefectGrid from './LineDefectGrid.vue'
import LineDeviceStatus from './LineDeviceStatus.vue'
import LineShiftClock from './LineShiftClock.vue'
import type { LineListItem } from '../stores/line'

const props = defineProps<{
  line: LineListItem | null
  /** 选中线在列表中的索引（用来渲染序号色块） */
  lineIndex?: number
}>()

/**
 * W-RT-9: 把 LineDefectGrid 的 defect-click 中继出去, 给 RealTime.vue 调弹窗
 */
const emit = defineEmits<{
  (e: 'defect-click', payload: { hour: number; val: number }): void
}>()

const totalNum = computed(() => Number(props.line?.realtime?.total ?? 0))
const effNum = computed(() => Number(props.line?.realtime?.efficiency ?? 0))

/**
 * 24 小时缺陷数据（0-23）：
 * 后端暂不提供该聚合；用 realtime.defects[] + ngCount 生成稳定的占位数据
 * （保持选中线时数据稳定，不抖动）。
 */
const hourlyData = computed<number[]>(() => {
  if (!props.line) {
    return Array.from({ length: 24 }, () => 0)
  }
  const rt = props.line.realtime
  const ng = Number(rt?.ngCount ?? 0)
  // 用 lineKey 作为种子，让同一线每次显示同一组数
  const seed = Array.from(props.line.lineKey).reduce((s, c) => s + c.charCodeAt(0), 0)
  const arr: number[] = Array.from({ length: 24 }, (_, h) => {
    // 简单的伪随机（确定性）：当前小时给最大值，其他小时散开
    const offset = (h * 37 + seed) % 17
    const ratio = h === new Date().getHours() ? 1 : 0.05 + (offset / 17) * 0.6
    return Math.max(0, Math.round(ng * ratio * 0.4))
  })
  return arr
})

const colorRamp = [
  'linear-gradient(135deg, #5ce1ff 0%, #5fd97f 100%)',
  'linear-gradient(135deg, #ff6ec7 0%, #5ce1ff 100%)',
  'linear-gradient(135deg, #5fd97f 0%, #ffb74d 100%)',
  'linear-gradient(135deg, #ffb74d 0%, #ff5a5f 100%)',
  'linear-gradient(135deg, #8ee4ff 0%, #ff6ec7 100%)',
  'linear-gradient(135deg, #c8a8ff 0%, #5ce1ff 100%)'
]
const indexBg = computed(() => colorRamp[(props.lineIndex ?? 0) % colorRamp.length])
const lineIndex = computed(() => (props.lineIndex ?? 0) + 1)
</script>

<style lang="scss" scoped>
.line-detail-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

// 空状态
.line-detail-panel__empty {
  width: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.line-detail-panel__empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-6);
}
.line-detail-panel__empty-icon {
  font-size: 48px;
  opacity: 0.5;
}
.line-detail-panel__empty-title {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.4px;
}
.line-detail-panel__empty-sub {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  letter-spacing: 0.4px;
}

// 4 区面板（2x2 grid）
.line-detail-panel__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto auto;
  grid-template-areas:
    'header header'
    'production defect'
    'device clock';
  gap: var(--space-3);
}
.line-detail-panel__header { grid-area: header; }
.line-detail-panel__grid > :nth-child(2) { grid-area: production; }   /* LineProductionCard */
.line-detail-panel__grid > :nth-child(3) { grid-area: defect; }       /* LineDefectGrid */
.line-detail-panel__grid > :nth-child(4) { grid-area: device; }       /* LineDeviceStatus */
.line-detail-panel__grid > :nth-child(5) { grid-area: clock; }        /* LineShiftClock */

@media (max-width: 960px) {
  .line-detail-panel__grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      'header'
      'production'
      'defect'
      'device'
      'clock';
  }
}

.line-detail-panel__header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
}
.line-detail-panel__header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.line-detail-panel__index {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  color: #0b1426;
  letter-spacing: -0.4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.4);
}
.line-detail-panel__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.line-detail-panel__line-no {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  letter-spacing: 0.4px;
  font-variant-numeric: tabular-nums;
}
.line-detail-panel__line-name {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  letter-spacing: 0.4px;
}

.line-detail-panel__header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
.line-detail-panel__pill {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.4px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
  font-variant-numeric: tabular-nums;
}
.line-detail-panel__pill[data-tone='cyan'] {
  color: var(--accent);
  background: rgba(92, 225, 255, 0.10);
  border-color: rgba(92, 225, 255, 0.35);
}
.line-detail-panel__pill[data-tone='orange'] {
  color: var(--warning);
  background: rgba(255, 183, 77, 0.10);
  border-color: rgba(255, 183, 77, 0.35);
}
</style>
