<template>
  <div class="occ-bar glass-panel">
    <!-- 标题行 -->
    <div class="occ-bar__head">
      <div class="occ-bar__title">
        <span class="occ-bar__title-icon">📊</span>
        <span>{{ $t('occupancy.barTitle') }}</span>
        <span v-if="staleMarker" class="occ-bar__stale" :title="$t('realtime.ws.disconnected')">連接斷開</span>
        <span class="occ-bar__avg" v-if="avgRate > 0">
          {{ $t('occupancy.barAvg') }} <b>{{ avgRate.toFixed(1) }}%</b>
        </span>
      </div>

      <div class="occ-bar__tools">
        <!-- 显示数值开关 -->
        <label class="occ-bar__switch">
          <span class="occ-bar__switch-label">{{ $t('occupancy.barShowValue') }}</span>
          <input
            type="checkbox"
            :checked="showValue"
            @change="toggleShowValue"
          />
        </label>

        <!-- 收起/展开 -->
        <button class="occ-bar__toggle" type="button" @click="collapsed = !collapsed" :title="collapsed ? $t('occupancy.barExpand') : $t('occupancy.barCollapse')">
          {{ collapsed ? '▸' : '▾' }}
        </button>

        <!-- 阈值配置 ⚙ -->
        <button class="occ-bar__toggle occ-bar__gear" type="button" :title="$t('occupancy.thresholdTitle')" @click="openThreshold">⚙</button>
      </div>
    </div>

    <!-- 热力条 -->
    <div v-show="!collapsed" class="occ-bar__body">
      <div class="occ-bar__strip" @wheel.prevent="onWheel">
        <button
          v-for="cell in cells"
          :key="cell.key"
          type="button"
          class="occ-bar__cell"
          :class="[`occ-bar__cell--${cell.tone}`]"
          :style="{ '--cell-w': cellWidth }"
          :title="cell.tooltip"
          @click="jumpToBoard(cell.lineNo)"
        >
          <!-- 显示数值开关打开时显示数字 -->
          <span v-if="showValue && cell.value > 0" class="occ-bar__cell-val">
            {{ cell.value.toFixed(0) }}
          </span>
        </button>
      </div>

      <!-- 汇总 + 入口 -->
      <div class="occ-bar__foot">
        <span class="occ-bar__legend">
          <span class="occ-bar__dot occ-bar__dot--red"></span>{{ summary.red }}
          <span class="occ-bar__dot occ-bar__dot--yellow"></span>{{ summary.yellow }}
          <span class="occ-bar__dot occ-bar__dot--green"></span>{{ summary.green }}
          <span class="occ-bar__dot occ-bar__dot--gray"></span>{{ summary.gray }}
        </span>
        <router-link
          v-if="canOpenBoard"
          to="/production-board"
          class="occ-bar__board-link"
        >
          {{ $t('occupancy.barOpenBoard') }} →
        </router-link>
      </div>
    </div>

    <!-- 阈值配置弹窗 -->
    <OccupancyThresholdDialog ref="thresholdDialog" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { listSystemConfig, type SystemConfigItem } from '../api/systemConfig'
import { screenState, subscribeScreen } from '../stores/screen'
import OccupancyThresholdDialog from './OccupancyThresholdDialog.vue'

// W-FRONT-05-B2 顶部全宽条（上座率 Panorama Bar）
// ===== W-FLASH-01 改：数据源从 lineStore 改为全局 WS 单例快照 =====
// 消费 screenState.snapshot.lines[]（App.vue 已 connectScreenSingleton，服务端每 5s 广播全量快照），
// 不再调用 lineStore.load() / 不再用 setInterval 轮询，与实时页共用同一条刷新源，根治双源闪烁。
//
// 渲染规则（W-FRONT-05-B2-B3-NOTE.md）：
//   - 颜色阈值：< warn 红；>= warn && < good 黄；>= good 绿（由 system-config 提供）
//   - 无数据 / occupancyRate<=0：灰色
//   - 颜色 + 真实值双显：默认只颜色，[显示数值] 开关打开后格子内显数字
//   - 每线取 4 面平均 occupancyRate（>=1 格）
//   - WS 断线/快照过期：停留最后帧 + 标题旁显示「连接断开」轻量标记（不闪空白）

interface Cell {
  key: string
  lineNo: string
  value: number
  tone: 'red' | 'yellow' | 'green' | 'gray'
  tooltip: string
}

const { t } = useI18n()

const collapsed = ref(false)
const showValue = ref(false)
const warnThreshold = ref(80)
const goodThreshold = ref(95)
const refreshInterval = ref(5)
const cellWidth = '20px'

// ===== W-FLASH-01: 统一 WS 单一刷新源 =====
// 不再用 setInterval 轮询 lineStore.load()，改由全局 WS 单例（App.vue 连接）驱动。
// 数据源：screenState.snapshot.lines[]（每 5s 服务端广播的全量快照）。
let unsubscribeScreen: (() => void) | null = null
/** 连接新鲜度：WS 断开或快照超过阈值秒未更新 => stale（停留最后帧 + 标注） */
const STALE_MS = 10_000
const staleMarker = ref(false)
let freshTick: ReturnType<typeof setInterval> | null = null

// 平均上座率（全部线）
const avgRate = computed(() => {
  const cells = buildCells()
  if (!cells.length) return 0
  const sum = cells.reduce((acc, c) => acc + c.value, 0)
  return sum / cells.length
})

// 汇总计数
const summary = computed(() => {
  const cells = buildCells()
  const s = { red: 0, yellow: 0, green: 0, gray: 0 }
  for (const c of cells) {
    if (c.tone === 'red') s.red++
    else if (c.tone === 'yellow') s.yellow++
    else if (c.tone === 'green') s.green++
    else s.gray++
  }
  return s
})

// 热力格：每 lineNo 一格（取 4 面平均）
// 热力格：每 lineNo 一格（取 4 面平均）
// ===== W-FLASH-01: 数据源从 lineStore 改为 WS 快照 screenState.snapshot.lines[] =====
function buildCells(): Cell[] {
  const snapLines = screenState.snapshot?.lines ?? []
  const map = new Map<string, { sum: number; n: number }>()
  for (const line of snapLines) {
    const rt = line.realTimeDetectData
    const val = Number(rt?.occupancyRate ?? 0)
    const key = line.lineNo
    const cur = map.get(key)
    if (cur) {
      cur.sum += val
      cur.n += 1
    } else {
      map.set(key, { sum: val, n: 1 })
    }
  }
  const cells: Cell[] = []
  // 按 WS 快照中的业务顺序排序（保持 lineOrder）
  const keys = Object.keys(map)
  // 保持 snapLines 原始顺序（服务端已按 order 排好），只保留首次出现的 lineNo
  const orderedKeys: string[] = []
  for (const line of snapLines) {
    if (map.has(line.lineNo) && !orderedKeys.includes(line.lineNo)) orderedKeys.push(line.lineNo)
  }
  for (const key of orderedKeys) {
    const e = map.get(key)!
    const avg = e.n ? e.sum / e.n : 0
    let tone: Cell['tone']
    if (avg <= 0) tone = 'gray'
    else if (avg < warnThreshold.value) tone = 'red'
    else if (avg < goodThreshold.value) tone = 'yellow'
    else tone = 'green'
    const tooltip = `${key}  ${t('occupancy.rate')}: ${avg.toFixed(1)}%`
    cells.push({ key, lineNo: key, value: avg, tone, tooltip })
  }
  return cells
}

const cells = computed(() => buildCells())

const router = useRouter()
const thresholdDialog = ref<InstanceType<typeof OccupancyThresholdDialog> | null>(null)

function openThreshold(): void {
  thresholdDialog.value?.open()
}
// 是否可打开生产看板（路由存在）
const canOpenBoard = computed(() => {
  try {
    return router.hasRoute('ProductionBoard')
  } catch {
    return false
  }
})

function jumpToBoard(lineNo: string): void {
  try {
    const exists = router.hasRoute('ProductionBoard')
    if (exists) router.push({ name: 'ProductionBoard', query: { lineNo } })
  } catch {
    /* 路由未注册时静默 */
  }
}

function toggleShowValue(e: Event): void {
  showValue.value = (e.target as HTMLInputElement).checked
}

function onWheel(e: WheelEvent): void {
  const strip = e.currentTarget as HTMLElement
  strip.scrollLeft += e.deltaY
}

// ===== W-FLASH-01: 新增 stale 新鲜度检测 =====
// 每 1s 检查 WS 连接状态 + 快照新鲜度，仅驱动一个轻量 stale 标记（不改数据不重绘）。
function refreshStale(): void {
  const live = screenState.wsState === 'open'
  const fresh = live && !!screenState.lastUpdate && Date.now() - screenState.lastUpdate < STALE_MS
  staleMarker.value = !fresh
}

// ===== W-FLASH-01: 生命周期改造 =====
// 1. 只从 /web/system-config 读一次阈值（静态配置，非刷新源）
// 2. 订阅全局 WS 单例快照（screenState.snapshot 响应式，cells 自动随其更新）
// 3. 去掉 setInterval 轮询 lineStore.load()，避免与 WS 推送不同步导致闪烁
onMounted(async () => {
  try {
    // 从 /web/system-config 读阈值（静态配置）
    const rsp = await listSystemConfig()
    const cfgs: SystemConfigItem[] = Array.isArray(rsp?.data) ? (rsp.data as SystemConfigItem[]) : []
    for (const c of cfgs) {
      const v = Number(c.configValue)
      if (c.configKey === 'occupancy.warn_threshold' && !Number.isNaN(v)) warnThreshold.value = v
      else if (c.configKey === 'occupancy.good_threshold' && !Number.isNaN(v)) goodThreshold.value = v
      else if (c.configKey === 'occupancy.refresh_interval' && v > 0) refreshInterval.value = v
      else if (c.configKey === 'occupancy.show_value') showValue.value = c.configValue.toLowerCase() === 'true'
    }
  } catch {
    /* 用默认 80/95/5 */
  }
  // 关：首次非 silent lineStore.load(true)；open：不再主动触发任何 store 拉取。
  // 只订阅 WS 快照（subscribeScreen 幂等，App.vue 已 connectScreenSingleton）。
  if (screenState.snapshot) refreshStale()
  unsubscribeScreen = subscribeScreen(() => {
    // 收到快照即标记新鲜；screenState.snapshot 响应式变化会让 cells 自动重算
    staleMarker.value = false
  })
  // 启动新鲜度心跳（1s）
  freshTick = setInterval(refreshStale, 1000)
})

onBeforeUnmount(() => {
  if (unsubscribeScreen) {
    try {
      unsubscribeScreen()
    } catch {
      /* ignore */
    }
    unsubscribeScreen = null
  }
  if (freshTick != null) {
    clearInterval(freshTick)
    freshTick = null
  }
})
</script>

<style scoped lang="scss">
.occ-bar.glass-panel {
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: var(--glass-blur-soft);
  -webkit-backdrop-filter: var(--glass-blur-soft);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  padding: 12px 16px;
  margin-bottom: var(--space-4);
}

.occ-bar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.occ-bar__title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.occ-bar__title-icon {
  font-size: 15px;
}

.occ-bar__avg {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.occ-bar__avg b {
  color: var(--accent);
  font-weight: var(--font-weight-bold);
}
.occ-bar__stale {
  font-size: var(--font-size-xs);
  color: var(--danger);
  border: 1px solid var(--danger);
  border-radius: 4px;
  padding: 0 5px;
  line-height: 16px;
  white-space: nowrap;
}

.occ-bar__tools {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.occ-bar__switch {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  cursor: pointer;
}
.occ-bar__switch input {
  accent-color: var(--accent);
  cursor: pointer;
}

.occ-bar__toggle {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 0 4px;
}

.occ-bar__body {
  margin-top: 10px;
}

/* 热力条（横向滚动） */
.occ-bar__strip {
  display: flex;
  gap: 3px;
  overflow-x: auto;
  scrollbar-width: thin;
  padding-bottom: 4px;
}
.occ-bar__strip::-webkit-scrollbar {
  height: 4px;
}
.occ-bar__strip::-webkit-scrollbar-thumb {
  background: var(--glass-border);
  border-radius: 2px;
}

.occ-bar__cell {
  width: var(--cell-w);
  height: 34px;
  min-width: var(--cell-w);
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: var(--font-weight-bold);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.occ-bar__cell:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.25);
}
.occ-bar__cell--red {
  background: linear-gradient(180deg, #ff5a5f, #b6373b);
}
.occ-bar__cell--yellow {
  background: linear-gradient(180deg, #ffd75e, #d3a82a);
}
.occ-bar__cell--green {
  background: linear-gradient(180deg, #5fd97f, #2e9e4f);
}
.occ-bar__cell--gray {
  background: linear-gradient(180deg, #4a4f58, #2c3038);
}

.occ-bar__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: 8px;
}
.occ-bar__legend {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.occ-bar__dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
  margin-right: 2px;
}
.occ-bar__dot--red {
  background: #ff5a5f;
}
.occ-bar__dot--yellow {
  background: #ffd75e;
}
.occ-bar__dot--green {
  background: #5fd97f;
}
.occ-bar__dot--gray {
  background: #4a4f58;
}

.occ-bar__board-link {
  font-size: var(--font-size-xs);
  color: var(--accent);
  text-decoration: none;
  font-weight: var(--font-weight-semibold);
}
.occ-bar__board-link:hover {
  text-decoration: underline;
}
</style>
