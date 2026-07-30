<template>
  <!--
    W-RT-2 左栏线别列表卡片（玻璃风）

    功能：
      - 玻璃面板 + 单击切换选中
      - 列表项：序号彩色块 + lineNo-faceNo + 当小时总缺陷数 + 总剔除数
      - 选中态：边框高亮 (蓝绿) + 背景色加深 + 左侧高亮条
      - hover：背景色轻微变化 + 抬升 2px

    设计对齐 PSM 实时页左侧 `.defect-li-item` + `.li-active`（详见 W-REALTIME-PSM §1.2）；
    UI 严格走自家玻璃风 token，不复用 PSM 控件样式。
  -->
  <div class="line-list-card glass-card">
    <div class="line-list-card__header">
      <div class="line-list-card__title-row">
        <span class="line-list-card__title-icon">🛰️</span>
        <h3 class="line-list-card__title">{{ $t('realtime.lineList.title') }}</h3>
      </div>
      <span class="line-list-card__count">
        {{ $t('realtime.lineList.total') }}: <b>{{ lineStore.lines.length }}</b>
      </span>
    </div>

    <!-- 加载骨架：3 行占位 -->
    <div v-if="lineStore.loading && lineStore.lines.length === 0" class="line-list-card__skeleton">
      <div v-for="n in 3" :key="n" class="line-list-card__skeleton-item" />
    </div>

    <!-- 错误态 -->
    <div v-else-if="lineStore.lines.length === 0" class="line-list-card__empty">
      <span class="line-list-card__empty-icon">📭</span>
      <span>{{ $t('common.noData') || '暂无数据' }}</span>
    </div>

    <!-- 列表主体 -->
    <ul v-else class="line-list-card__list" role="listbox" :aria-label="$t('realtime.lineList.title')">
      <li
        v-for="(line, idx) in lineStore.lines"
        :key="line.lineKey"
        :class="['line-item', { 'line-item--active': line.lineKey === lineStore.selectedLineKey }]"
        role="option"
        :aria-selected="line.lineKey === lineStore.selectedLineKey"
        @click="handleClick(line)"
      >
        <!-- 选中态左侧高亮条 -->
        <span class="line-item__active-bar" />

        <!-- 序号彩色块（PSM 风格，按 line.color 取色；没设置时用 accent） -->
        <span
          class="line-item__index"
          :style="{ background: indexGradient(idx) }"
        >
          {{ idx + 1 }}
        </span>

        <!-- 文本主区 -->
        <div class="line-item__main">
          <div class="line-item__no-row">
            <span class="line-item__no">{{ line.lineNo }}</span>
            <span class="line-item__dash">-</span>
            <span class="line-item__face">{{ line.faceNo }}</span>
          </div>
          <div class="line-item__name" :title="line.name">{{ line.name }}</div>
        </div>

        <!-- 计数（缺陷 / 剔除） -->
        <div class="line-item__stats">
          <div class="line-item__stat line-item__stat--defect" :title="$t('realtime.lineList.defect')">
            <span class="line-item__stat-num">{{ formatNum(line.hourDefectCount) }}</span>
            <span class="line-item__stat-label">{{ $t('realtime.lineList.defect') }}</span>
          </div>
          <div class="line-item__stat line-item__stat--remove" :title="$t('realtime.lineList.remove')">
            <span class="line-item__stat-num">{{ formatNum(line.hourRemoveCount) }}</span>
            <span class="line-item__stat-label">{{ $t('realtime.lineList.remove') }}</span>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-RT-2 LineListCard
//  仿 PSM 实时页左侧 `.defect-li-item` + `.li-active` 选中态
//  数据走 line store（stores/line.ts），点击时 store.select(lineNo) + emit('line-change')
// =============================================================================
import { useLineStore, type LineListItem } from '../stores/line'

const lineStore = useLineStore()

const emit = defineEmits<{
  'line-change': [lineNo: string]
}>()

function handleClick(line: LineListItem): void {
  if (line.lineKey === lineStore.selectedLineKey) return
  lineStore.select(line.lineKey)
  emit('line-change', line.lineKey)
}

function formatNum(n: number): string {
  if (!Number.isFinite(n)) return '0'
  return Math.round(n).toLocaleString('en-US')
}

/**
 * 序号色块：按列表索引从固定色板取（PSM 风格）。
 * 色板选 glass 风和谐的冷暖色，避免 PSM 那种刺眼的纯红绿。
 */
const colorRamp = [
  'linear-gradient(135deg, #5ce1ff 0%, #5fd97f 100%)', // 蓝绿
  'linear-gradient(135deg, #ff6ec7 0%, #5ce1ff 100%)', // 粉蓝
  'linear-gradient(135deg, #5fd97f 0%, #ffb74d 100%)', // 绿橙
  'linear-gradient(135deg, #ffb74d 0%, #ff5a5f 100%)', // 橙红
  'linear-gradient(135deg, #8ee4ff 0%, #ff6ec7 100%)', // 浅蓝粉
  'linear-gradient(135deg, #5fd97f 0%, #5ce1ff 100%)', // 绿蓝
  'linear-gradient(135deg, #c8a8ff 0%, #5ce1ff 100%)'  // 紫蓝
]

function indexGradient(idx: number): string {
  return colorRamp[idx % colorRamp.length]
}
</script>

<style lang="scss" scoped>
// ---------------------------------------------------------------------------
// 玻璃面板基底：复用 design tokens（与 GlassCard 一致；此处不直接套 GlassCard
// 是因为需要内部滚动容器，且要自己管 padding 与内层结构）
// ---------------------------------------------------------------------------
.line-list-card {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: var(--space-4);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  overflow: hidden;
  // 内顶高光
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.10), transparent 35%);
    opacity: 0.7;
  }
}

// ---------------------------------------------------------------------------
// 头部
// ---------------------------------------------------------------------------
.line-list-card__header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.line-list-card__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.line-list-card__title-icon {
  font-size: 16px;
  filter: drop-shadow(0 2px 6px rgba(92, 225, 255, 0.3));
}
.line-list-card__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  letter-spacing: 0.2px;
}
.line-list-card__count {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  b {
    color: var(--text-primary);
    font-weight: var(--font-weight-bold);
  }
}

// ---------------------------------------------------------------------------
// 列表容器：内部滚动
// ---------------------------------------------------------------------------
.line-list-card__list {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
  overflow-x: hidden;
  // 玻璃风滚动条
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: var(--radius-pill);
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

// ---------------------------------------------------------------------------
// 单项
// ---------------------------------------------------------------------------
.line-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 10px 12px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid transparent;
  cursor: pointer;
  user-select: none;
  transition:
    transform var(--transition-fast),
    border-color var(--transition-base),
    background var(--transition-base),
    box-shadow var(--transition-base);

  &:last-child {
    margin-bottom: 0;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.07);
    border-color: rgba(255, 255, 255, 0.14);
    transform: translateY(-1px);
  }
}

// 选中态：PSM 蓝绿底（换成 glass 风高亮）
.line-item--active {
  background: linear-gradient(
    135deg,
    rgba(92, 225, 255, 0.16) 0%,
    rgba(95, 217, 127, 0.10) 100%
  );
  border-color: rgba(92, 225, 255, 0.55);
  box-shadow:
    0 0 0 1px rgba(92, 225, 255, 0.25),
    0 6px 18px rgba(92, 225, 255, 0.10);
  transform: translateY(-1px);
}

// 选中态左侧高亮条
.line-item__active-bar {
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
  transition: background var(--transition-base);
}
.line-item--active .line-item__active-bar {
  background: linear-gradient(180deg, #5ce1ff, #5fd97f);
  box-shadow: 0 0 8px rgba(92, 225, 255, 0.7);
}

// 序号彩色块
.line-item__index {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  color: #0b1426;
  letter-spacing: -0.4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

// 文本主区
.line-item__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.line-item__no-row {
  display: flex;
  align-items: center;
  gap: 2px;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.line-item__no {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}
.line-item__dash {
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}
.line-item__face {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--accent);
}
.line-item__name {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

// 计数（缺陷 / 剔除）
.line-item__stats {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-end;
  font-variant-numeric: tabular-nums;
}
.line-item__stat {
  display: flex;
  align-items: baseline;
  gap: 4px;
  line-height: 1.1;
}
.line-item__stat-num {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
}
.line-item__stat-label {
  font-size: 10px;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}
.line-item__stat--defect {
  .line-item__stat-num { color: var(--danger); }
  .line-item__stat-label { color: rgba(255, 90, 95, 0.7); }
}
.line-item__stat--remove {
  .line-item__stat-num { color: var(--warning); }
  .line-item__stat-label { color: rgba(255, 183, 77, 0.7); }
}

// ---------------------------------------------------------------------------
// 骨架屏
// ---------------------------------------------------------------------------
.line-list-card__skeleton {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex: 1;
}
.line-list-card__skeleton-item {
  height: 56px;
  border-radius: var(--radius-md);
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.03) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.03) 100%
  );
  background-size: 200% 100%;
  animation: line-list-skeleton 1.4s ease-in-out infinite;
}
@keyframes line-list-skeleton {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

// ---------------------------------------------------------------------------
// 空 / 错误态
// ---------------------------------------------------------------------------
.line-list-card__empty {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}
.line-list-card__empty-icon {
  font-size: 28px;
  opacity: 0.5;
}
</style>
