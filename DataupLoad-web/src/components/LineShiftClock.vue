<!--
  W-RT-3 中栏 4 区面板 - ④时间信息卡（玻璃风）

  数据：
    - 当前时间（每秒刷新）
    - 班次：根据当前小时估算（早班 8-16 / 晚班 16-24 / 夜班 0-8）
    - 运行时长：deviceOpenTime (HH:mm) 起，到当前时间的时长（小时分钟）
-->
<template>
  <GlassCard class="lsc-card" :hover="true">
    <div class="lsc-card__inner">
      <div class="lsc-card__head">
        <h4 class="lsc-card__title">
          <span class="lsc-card__icon">⏰</span>
          {{ $t('realtime.detail.time') }}
        </h4>
      </div>

      <div class="lsc-card__body">
        <!-- 当前时间 -->
        <div class="lsc-card__clock">
          <span class="lsc-card__clock-num">{{ clockText }}</span>
          <span class="lsc-card__clock-date">{{ dateText }}</span>
        </div>

        <!-- 班次 / 运行时长 -->
        <div class="lsc-card__row">
          <div class="lsc-card__row-item">
            <span class="lsc-card__row-label">{{ shiftLabel }}</span>
            <span class="lsc-card__row-val" data-tone="cyan">{{ shiftText }}</span>
          </div>
          <div class="lsc-card__row-item">
            <span class="lsc-card__row-label">{{ $t('realtime.detail.runtime') }}</span>
            <span class="lsc-card__row-val" data-tone="orange">{{ runtimeText }}</span>
          </div>
        </div>
      </div>
    </div>
  </GlassCard>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import GlassCard from './GlassCard.vue'
import { deviceOpenTimeOf } from '../api/realtime'

const props = withDefaults(defineProps<{
  /** startTime（HH:mm:ss / HH:mm） */
  startTime?: string | null
}>(), {
  startTime: ''
})

const now = ref(new Date())
let timer: number | null = null

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
})
onBeforeUnmount(() => {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
})

const clockText = computed(() => {
  const d = now.value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

const dateText = computed(() => {
  const d = now.value
  const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${weekdays[d.getDay()]}`
})

const shiftLabel = computed(() => {
  // 单 key，模板里直接 $t；这里返回占位（实际不在模板用）
  return 'Shift'
})

const shiftText = computed(() => {
  const h = now.value.getHours()
  if (h >= 8 && h < 16) return '早班 / Day'
  if (h >= 16 && h < 24) return '晚班 / Eve'
  return '夜班 / Night'
})

/**
 * 运行时长：从 props.startTime (HH:mm[:ss]) 算到当前时间。
 * 若没有 startTime 或时间在未来，返回 "--:--"
 */
const runtimeText = computed(() => {
  const start = deviceOpenTimeOf({ startTime: props.startTime } as any)
  if (!start || start === '--:--') return '--:--'
  const m = /^(\d{1,2}):(\d{2})(?::(\d{2}))?$/.exec(start)
  if (!m) return '--:--'
  const startH = Number(m[1])
  const startM = Number(m[2])
  const startS = m[3] ? Number(m[3]) : 0
  const startSec = startH * 3600 + startM * 60 + startS
  const d = now.value
  const nowSec = d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds()
  let diff = nowSec - startSec
  if (diff < 0 || diff > 86400) return '--:--'
  const hh = Math.floor(diff / 3600)
  const mm = Math.floor((diff % 3600) / 60)
  return `${String(hh).padStart(2, '0')}h ${String(mm).padStart(2, '0')}m`
})
</script>

<style lang="scss" scoped>
.lsc-card {
  width: 100%;
  height: 100%;
}
.lsc-card__inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
}
.lsc-card__head {
  padding-bottom: var(--space-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.lsc-card__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  letter-spacing: 0.2px;
}
.lsc-card__icon {
  font-size: 16px;
  filter: drop-shadow(0 2px 6px rgba(92, 225, 255, 0.3));
}

.lsc-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  flex: 1;
  justify-content: space-between;
}
.lsc-card__clock {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.lsc-card__clock-num {
  font-size: 36px;
  font-weight: var(--font-weight-bold);
  font-variant-numeric: tabular-nums;
  letter-spacing: 1.5px;
  background: linear-gradient(135deg, #5ce1ff 0%, #5fd97f 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
}
.lsc-card__clock-date {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
}

.lsc-card__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}
.lsc-card__row-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.lsc-card__row-label {
  font-size: 10px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: var(--text-secondary);
}
.lsc-card__row-val {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.lsc-card__row-val[data-tone='cyan'] {
  background: linear-gradient(135deg, #5ce1ff, #74e0ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.lsc-card__row-val[data-tone='orange'] {
  background: linear-gradient(135deg, #ff9f43, #ff6ec7);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
</style>
