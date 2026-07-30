<template>
  <!--
    W-RT-8 报警徽章悬浮窗（玻璃风）

    功能：
      - 圆形玻璃徽章 + 角标红点 + 未处理报警数字
      - 可拖动（mousedown + pointermove，位置持久化到 localStorage）
      - hover 弹玻璃风小窗，列最近 5 条报警（lineNo/faceNo/time/level）
      - 点击条目 → 跳转 /alarm 路由

    数据：
      - subscribeAlarmHint（stores/alarm.ts）：基线 + WS 增量
      - 不打开新的 WS；走 alarmStore singleton（已在 App.vue 启动）
  -->
  <Teleport to="body">
    <div
      ref="rootEl"
      class="alarm-hint"
      :class="{ 'alarm-hint--dragging': isDragging }"
      :style="positionStyle"
      @mouseenter="onHoverEnter"
      @mouseleave="onHoverLeave"
    >
      <!-- 徽章本体（draggable 区域） -->
      <button
        ref="badgeEl"
        type="button"
        class="alarm-hint__badge"
        :title="$t('alarm.hint.clickHint')"
        @mousedown.stop="onDragStart"
        @click.stop="onBadgeClick"
      >
        <span class="alarm-hint__bell">🔔</span>
        <span v-if="pending > 0" class="alarm-hint__count">{{ pendingLabel }}</span>
        <span v-if="pending > 0" class="alarm-hint__pulse" aria-hidden="true" />
      </button>

      <!-- 拖动小提示（仅在首次 hover 时短暂显示） -->
      <div v-if="showDragHint" class="alarm-hint__drag-hint">
        <span class="alarm-hint__drag-icon">✥</span>
        {{ $t('alarm.hint.dragHint') }}
      </div>

      <!-- hover 弹窗（玻璃风小卡） -->
      <Transition name="alarm-pop">
        <div v-if="popoverVisible" class="alarm-hint__popover">
          <!-- 顶部 -->
          <div class="alarm-hint__popover-header">
            <div class="alarm-hint__popover-title">
              <span class="alarm-hint__popover-icon">⚠</span>
              <span>{{ $t('alarm.hint.title') }}</span>
              <span v-if="pending > 0" class="alarm-hint__popover-count">{{ pending }}</span>
            </div>
            <span class="alarm-hint__popover-sub">
              {{ $t('alarm.hint.recent') }}
            </span>
          </div>

          <!-- 列表 -->
          <div v-if="recent.length === 0" class="alarm-hint__empty">
            <span class="alarm-hint__empty-icon">✓</span>
            <span>{{ $t('alarm.hint.empty') }}</span>
          </div>
          <ul v-else class="alarm-hint__list">
            <li
              v-for="(item, idx) in recent"
              :key="String(item.id) + '-' + idx"
              class="alarm-hint__item"
              @click.stop="goAlarm(item)"
            >
              <span
                class="alarm-hint__level"
                :class="`alarm-hint__level--${item.level}`"
                :title="levelLabel(item.level)"
              >
                {{ levelShort(item.level) }}
              </span>
              <div class="alarm-hint__item-main">
                <div class="alarm-hint__item-line">
                  <span class="alarm-hint__line-no">{{ item.lineNo || '—' }}</span>
                  <span class="alarm-hint__dash">-</span>
                  <span class="alarm-hint__face-no">{{ item.faceNo || '—' }}</span>
                  <span class="alarm-hint__time">{{ item.time || '' }}</span>
                </div>
                <div class="alarm-hint__msg">{{ item.message || '' }}</div>
              </div>
              <span class="alarm-hint__chev">›</span>
            </li>
          </ul>

          <!-- 底部 -->
          <div class="alarm-hint__popover-footer">
            <button
              type="button"
              class="alarm-hint__view-all"
              @click.stop="goAlarmList"
            >
              {{ $t('alarm.hint.viewAll') }}
              <span class="alarm-hint__view-all-arrow">→</span>
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
// =============================================================================
// W-RT-8 报警徽章悬浮窗
//   - 位置：fixed（top: 88px, right: 24px 默认；可拖动后持久化）
//   - 拖动：pointerdown/move/up；不依赖 HTML5 draggable（避免浏览器默认行为）
//   - 持久化：localStorage key = 'alarmHint.position' = {top, right}
//   - i18n: alarm.hint.* 6 key × 3 locale = 18 翻译
// =============================================================================
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  alarmState,
  subscribeAlarmHint,
  type AlarmHintItem
} from '../stores/alarm'

// -----------------------------------------------------------------------------
// i18n
// -----------------------------------------------------------------------------
const { t } = useI18n()

// -----------------------------------------------------------------------------
// Router
// -----------------------------------------------------------------------------
const router = useRouter()

// -----------------------------------------------------------------------------
// 状态：来自 alarmStore singleton
// -----------------------------------------------------------------------------
const recent = computed<AlarmHintItem[]>(() => alarmState.recent)
const pending = computed<number>(() => alarmState.pending)
const pendingLabel = computed(() => {
  const n = alarmState.pending
  if (n <= 0) return ''
  if (n > 99) return '99+'
  return String(n)
})

// -----------------------------------------------------------------------------
// 拖动状态 + 持久化位置
// -----------------------------------------------------------------------------
interface HintPos {
  top: number
  right: number
}
const POS_KEY = 'alarmHint.position'

function readPersistedPos(): HintPos | null {
  try {
    const raw = localStorage.getItem(POS_KEY)
    if (!raw) return null
    const obj = JSON.parse(raw)
    if (
      obj &&
      typeof obj.top === 'number' &&
      typeof obj.right === 'number' &&
      obj.top >= 0 &&
      obj.right >= 0
    ) {
      return { top: obj.top, right: obj.right }
    }
  } catch {
    /* ignore */
  }
  return null
}

function writePersistedPos(p: HintPos) {
  try {
    localStorage.setItem(POS_KEY, JSON.stringify(p))
  } catch {
    /* ignore */
  }
}

const persisted = readPersistedPos()
const position = ref<HintPos>(
  persisted ?? { top: 88, right: 24 }
)
const positionStyle = computed(() => ({
  top: `${position.value.top}px`,
  right: `${position.value.right}px`
}))

// -----------------------------------------------------------------------------
// 拖动
// -----------------------------------------------------------------------------
const rootEl = ref<HTMLDivElement | null>(null)
const badgeEl = ref<HTMLButtonElement | null>(null)
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let posStartTop = 0
let posStartRight = 0
let dragMoved = false
const DRAG_THRESHOLD = 4 // px；超过则视为"真的拖"，避免误触 click

function onDragStart(ev: MouseEvent) {
  if (!ev || typeof ev.button !== 'number' || ev.button !== 0) return // 仅左键
  isDragging.value = true
  dragMoved = false
  dragStartX = ev.clientX
  dragStartY = ev.clientY
  posStartTop = position.value.top
  posStartRight = position.value.right
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd, { once: true })
}

function clampPos(top: number, right: number): HintPos {
  const maxTop = Math.max(24, window.innerHeight - 80)
  const maxRight = Math.max(8, window.innerWidth - 80)
  return {
    top: Math.min(Math.max(24, top), maxTop),
    right: Math.min(Math.max(8, right), maxRight)
  }
}

function onDragMove(ev: MouseEvent) {
  if (!isDragging.value) return
  const dx = ev.clientX - dragStartX
  const dy = ev.clientY - dragStartY
  if (!dragMoved && Math.abs(dx) + Math.abs(dy) > DRAG_THRESHOLD) {
    dragMoved = true
  }
  // 拖动方向：向右拖 → 减小 right；向下拖 → 增加 top
  position.value = clampPos(posStartTop + dy, posStartRight - dx)
}

function onDragEnd() {
  window.removeEventListener('mousemove', onDragMove)
  if (isDragging.value) {
    isDragging.value = false
    if (dragMoved) writePersistedPos(position.value)
  }
}

// -----------------------------------------------------------------------------
// hover popover
// -----------------------------------------------------------------------------
const popoverVisible = ref(false)
let hoverTimer: number | null = null
const HOVER_DELAY_MS = 120

function onHoverEnter() {
  if (hoverTimer != null) {
    window.clearTimeout(hoverTimer)
    hoverTimer = null
  }
  hoverTimer = window.setTimeout(() => {
    popoverVisible.value = true
    hoverTimer = null
  }, HOVER_DELAY_MS)
  // 首次进入时弹一下拖动提示，3 秒后自动消失
  if (!hasShownDragHint.value) {
    showDragHint.value = true
    hasShownDragHint.value = true
    window.setTimeout(() => {
      showDragHint.value = false
    }, 3000)
  }
}

function onHoverLeave() {
  if (hoverTimer != null) {
    window.clearTimeout(hoverTimer)
    hoverTimer = null
  }
  popoverVisible.value = false
}

const showDragHint = ref(false)
const hasShownDragHint = ref(false)

// -----------------------------------------------------------------------------
// 点击交互：徽章点 → 跳 /alarm；条目点 → 跳 /alarm（详情由 Alarm 页处理）
// -----------------------------------------------------------------------------
function onBadgeClick() {
  // 拖动后不触发 click
  if (dragMoved) {
    dragMoved = false
    return
  }
  goAlarmList()
}

function goAlarmList() {
  popoverVisible.value = false
  void router.push({ name: 'Alarm' })
}

function goAlarm(_item: AlarmHintItem) {
  popoverVisible.value = false
  void router.push({ name: 'Alarm' })
}

// -----------------------------------------------------------------------------
// 标签：等级 / 类型
// -----------------------------------------------------------------------------
function levelLabel(lvl: number): string {
  if (lvl === 1) return t('alarm.levelOption.normal')
  if (lvl === 2) return t('alarm.levelOption.serious')
  return '—'
}
function levelShort(lvl: number): string {
  if (lvl === 2) return '!'
  if (lvl === 1) return 'i'
  return '·'
}

// -----------------------------------------------------------------------------
// 生命周期：订阅 alarmStore（不重新连 WS）
// -----------------------------------------------------------------------------
let unsubscribe: (() => void) | null = null

onMounted(() => {
  unsubscribe = subscribeAlarmHint(() => {
    // 已通过 alarmState.reactive 自动响应；此处留作未来 hook（如声音/动画）
  })
})

onBeforeUnmount(() => {
  if (hoverTimer != null) {
    window.clearTimeout(hoverTimer)
    hoverTimer = null
  }
  window.removeEventListener('mousemove', onDragMove)
  if (unsubscribe) {
    unsubscribe()
    unsubscribe = null
  }
})
</script>

<style lang="scss" scoped>
.alarm-hint {
  position: fixed;
  z-index: var(--z-overlay, 1000);
  user-select: none;
  touch-action: none;
  // 拖动期间禁用子元素文本选择
  &--dragging {
    cursor: grabbing !important;
    * {
      cursor: grabbing !important;
      pointer-events: none;
    }
  }
}

// -----------------------------------------------------------------------------
// 徽章本体（玻璃风圆形 + 角标红点）
// -----------------------------------------------------------------------------
.alarm-hint__badge {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid var(--glass-border);
  background: rgba(20, 26, 46, 0.55);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  cursor: grab;
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base),
    background var(--transition-base);
  padding: 0;

  &:hover {
    transform: translateY(-1px);
    background: rgba(92, 225, 255, 0.12);
    border-color: var(--accent-border);
    box-shadow:
      0 6px 22px rgba(92, 225, 255, 0.25),
      0 0 0 1px rgba(92, 225, 255, 0.2) inset;
  }

  &:active {
    transform: translateY(0);
    cursor: grabbing;
  }

  &:focus-visible {
    outline: none;
    border-color: var(--accent);
    box-shadow:
      0 0 0 3px var(--accent-focus-ring),
      0 4px 16px rgba(0, 0, 0, 0.35);
  }
}

.alarm-hint__bell {
  font-size: 22px;
  line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.5));
}

.alarm-hint__count {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--danger), #ff8a8d);
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  line-height: 20px;
  text-align: center;
  box-shadow:
    0 0 0 2px rgba(20, 26, 46, 0.85),
    0 2px 6px rgba(255, 90, 95, 0.5);
  letter-spacing: 0.2px;
}

.alarm-hint__pulse {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--danger);
  opacity: 0.55;
  pointer-events: none;
  animation: alarm-pulse 1.6s ease-out infinite;
}

@keyframes alarm-pulse {
  0% {
    transform: scale(1);
    opacity: 0.55;
  }
  70% {
    transform: scale(2.2);
    opacity: 0;
  }
  100% {
    transform: scale(2.2);
    opacity: 0;
  }
}

// -----------------------------------------------------------------------------
// 拖动小提示（hover 时短暂显示）
// -----------------------------------------------------------------------------
.alarm-hint__drag-hint {
  position: absolute;
  top: 50%;
  left: calc(100% + 8px);
  transform: translateY(-50%);
  padding: 4px 10px;
  border-radius: var(--radius-pill);
  background: rgba(20, 26, 46, 0.85);
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  white-space: nowrap;
  backdrop-filter: var(--glass-blur-light);
  -webkit-backdrop-filter: var(--glass-blur-light);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  pointer-events: none;
  animation: drag-hint-in 0.3s ease forwards;
}

.alarm-hint__drag-icon {
  color: var(--accent);
  font-size: 14px;
}

@keyframes drag-hint-in {
  from {
    opacity: 0;
    transform: translate(-4px, -50%);
  }
  to {
    opacity: 1;
    transform: translate(0, -50%);
  }
}

// -----------------------------------------------------------------------------
// Hover 弹窗（玻璃风小卡）
// -----------------------------------------------------------------------------
.alarm-hint__popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 360px;
  max-width: calc(100vw - 32px);
  max-height: 480px;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
  background: rgba(20, 26, 46, 0.78);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  overflow: hidden;
  // 内顶高光
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 35%);
  }
}

.alarm-hint__popover-header {
  position: relative;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: rgba(255, 255, 255, 0.03);
}

.alarm-hint__popover-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.alarm-hint__popover-icon {
  color: var(--warning);
  font-size: 16px;
}

.alarm-hint__popover-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 20px;
  padding: 0 8px;
  border-radius: var(--radius-pill);
  background: rgba(255, 90, 95, 0.16);
  border: 1px solid rgba(255, 90, 95, 0.4);
  color: #ffb3b6;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.3px;
}

.alarm-hint__popover-sub {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

// -----------------------------------------------------------------------------
// 空态
// -----------------------------------------------------------------------------
.alarm-hint__empty {
  padding: var(--space-5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  background: rgba(95, 217, 127, 0.04);
}

.alarm-hint__empty-icon {
  color: var(--success);
  font-size: 18px;
}

// -----------------------------------------------------------------------------
// 列表项
// -----------------------------------------------------------------------------
.alarm-hint__list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: var(--radius-pill);
  }
}

.alarm-hint__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: background var(--transition-fast);
  &:hover {
    background: rgba(92, 225, 255, 0.10);
  }
  &:last-child {
    border-bottom: none;
  }
}

.alarm-hint__level {
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  flex-shrink: 0;

  &--1 {
    color: var(--accent);
    border-color: rgba(92, 225, 255, 0.4);
    background: rgba(92, 225, 255, 0.10);
  }
  &--2 {
    color: #ffb3b6;
    border-color: rgba(255, 90, 95, 0.5);
    background: rgba(255, 90, 95, 0.18);
    box-shadow: 0 0 0 2px rgba(255, 90, 95, 0.08);
  }
}

.alarm-hint__item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.alarm-hint__item-line {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}

.alarm-hint__line-no {
  color: var(--accent);
}
.alarm-hint__dash {
  color: var(--text-secondary);
  opacity: 0.7;
}
.alarm-hint__face-no {
  color: var(--text-primary);
}
.alarm-hint__time {
  margin-left: 6px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-normal);
  color: var(--text-secondary);
}

.alarm-hint__msg {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.alarm-hint__chev {
  color: var(--text-secondary);
  opacity: 0.5;
  font-size: 18px;
  line-height: 1;
}

// -----------------------------------------------------------------------------
// 底部
// -----------------------------------------------------------------------------
.alarm-hint__popover-footer {
  position: relative;
  padding: var(--space-2) var(--space-4);
  border-top: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  justify-content: flex-end;
}

.alarm-hint__view-all {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: var(--accent);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  &:hover {
    background: rgba(92, 225, 255, 0.10);
    transform: translateX(2px);
  }
}

.alarm-hint__view-all-arrow {
  font-size: 14px;
}

// -----------------------------------------------------------------------------
// 过渡（hover 弹窗淡入）
// -----------------------------------------------------------------------------
.alarm-pop-enter-active,
.alarm-pop-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}
.alarm-pop-enter-from,
.alarm-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
