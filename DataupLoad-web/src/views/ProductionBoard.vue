<template>
  <GlassPage :title="$t('occupancy.boardTitle')" :subtitle="$t('occupancy.boardSubtitle')">
    <!-- 工具栏 -->
    <div class="pb-toolbar">
      <div class="pb-toolbar__left">
        <span class="pb-toolbar__label">{{ $t('occupancy.barShowValue') }}</span>
        <input
          type="checkbox"
          :checked="showValue"
          @change="onToggleValue"
        />
        <span class="pb-toolbar__sep" />
        <span class="pb-toolbar__label">{{ $t('occupancy.line') }} {{ searchLine }}</span>
        <input
          v-model="searchLine"
          class="pb-toolbar__search"
          placeholder="line1A"
        />
      </div>
      <div class="pb-toolbar__right">
        <span class="pb-toolbar__legend">
          <span class="pb-toolbar__dot pb-toolbar__dot--red"></span>{{ summary.red }}
          <span class="pb-toolbar__dot pb-toolbar__dot--yellow"></span>{{ summary.yellow }}
          <span class="pb-toolbar__dot pb-toolbar__dot--green"></span>{{ summary.green }}
          <span class="pb-toolbar__dot pb-toolbar__dot--gray"></span>{{ summary.gray }}
        </span>
        <button class="pb-toolbar__refresh glass-btn" type="button" @click="refreshNow">
          {{ $t('topbar.refresh') }}
        </button>
      </div>
    </div>

    <!-- 152 格全展开网格 -->
    <div class="pb-grid">
      <div
        v-for="group in groupedCells"
        :key="group.lineNo"
        class="pb-group"
        @click="jumpToRealtime(group.lineNo)"
      >
        <div class="pb-group__header">
          <span class="pb-group__line">{{ group.lineNo }}</span>
          <span class="pb-group__avg" :class="`tone-${group.avgTone}`">
            {{ group.avg > 0 ? group.avg.toFixed(1) + '%' : '--' }}
          </span>
        </div>
        <div class="pb-group__faces">
          <div
            v-for="face in group.faces"
            :key="face.faceNo"
            class="pb-cell"
            :class="`pb-cell--${face.tone}`"
            :title="face.tooltip"
          >
            <span class="pb-cell__face">{{ face.faceNo }}</span>
            <span v-if="showValue && face.value > 0" class="pb-cell__val">
              {{ face.value.toFixed(1) }}
            </span>
            <span v-else class="pb-cell__val">{{ face.value > 0 ? '' : '—' }}</span>
          </div>
        </div>
      </div>
    </div>
  </GlassPage>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-05-B3 生产看板独立页
// 全部线体 × 4 面 = 152 格，上座率热力图（红黄绿灰）
// 颜色 + 真实值双显（show_value 配置默认 true）
// 5s 增量刷新（lineStore.load(true)）；搜索过滤；点击线跳 Realtime
// =============================================================================
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import GlassPage from '../components/GlassPage.vue'
import { useLineStore } from '../stores/line'
import { listSystemConfig, type SystemConfigItem } from '../api/systemConfig'

interface FaceCell {
  faceNo: string
  value: number
  tone: 'red' | 'yellow' | 'green' | 'gray'
  tooltip: string
}
interface LineGroup {
  lineNo: string
  avg: number
  avgTone: 'red' | 'yellow' | 'green' | 'gray'
  faces: FaceCell[]
}

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const lineStore = useLineStore()

const showValue = ref(true)
const warnThreshold = ref(80)
const goodThreshold = ref(95)
const refreshInterval = ref(5)
const searchLine = ref('')
let timer: ReturnType<typeof setInterval> | null = null
let unmounted = false

// 分组：按 lineNo 聚合 4 面
function buildGroups(): LineGroup[] {
  const map = new Map<string, LineGroup>()
  for (const line of lineStore.lines) {
    const val = Number(line.realtime?.occupancyRate ?? 0)
    let g = map.get(line.lineNo)
    if (!g) {
      g = { lineNo: line.lineNo, avg: 0, avgTone: 'gray', faces: [] }
      map.set(line.lineNo, g)
    }
    const tone: FaceCell['tone'] =
      val <= 0 ? 'gray'
      : val < warnThreshold.value ? 'red'
      : val < goodThreshold.value ? 'yellow'
      : 'green'
    g.faces.push({
      faceNo: line.faceNo,
      value: val,
      tone,
      tooltip: `${line.lineNo} / ${line.faceNo}  ${t('occupancy.rate')}: ${val.toFixed(1)}%`
    })
  }
  const groups = [...map.values()]
  // 按 lineStore 业务顺序排序
  groups.sort((a, b) => {
    const ia = lineStore.lines.findIndex((l) => l.lineNo === a.lineNo)
    const ib = lineStore.lines.findIndex((l) => l.lineNo === b.lineNo)
    return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib)
  })
  for (const g of groups) {
    const sum = g.faces.reduce((a, f) => a + f.value, 0)
    g.avg = g.faces.length ? sum / g.faces.length : 0
    g.avgTone =
      g.avg <= 0 ? 'gray'
      : g.avg < warnThreshold.value ? 'red'
      : g.avg < goodThreshold.value ? 'yellow'
      : 'green'
  }
  return groups
}

const allGroups = computed(() => buildGroups())

// 搜索过滤
const groupedCells = computed(() => {
  const q = searchLine.value.trim().toLowerCase()
  if (!q) return allGroups.value
  return allGroups.value.filter((g) => g.lineNo.toLowerCase().includes(q))
})

const summary = computed(() => {
  const s = { red: 0, yellow: 0, green: 0, gray: 0 }
  for (const g of allGroups.value) {
    for (const f of g.faces) {
      if (f.tone === 'red') s.red++
      else if (f.tone === 'yellow') s.yellow++
      else if (f.tone === 'green') s.green++
      else s.gray++
    }
  }
  return s
})

function onToggleValue(e: Event): void {
  showValue.value = (e.target as HTMLInputElement).checked
  // 本地持久化（sessionStorage，刷新保留）
  try {
    sessionStorage.setItem('occupancy.showValue', showValue.value ? 'true' : 'false')
  } catch { /* ignore */ }
}

function refreshNow(): void {
  lineStore.load(true, true) // 手动刷新 silent
}

function jumpToRealtime(lineNo: string): void {
  router.push({ name: 'RealTime', query: { line: lineNo } })
}

function startTimer(): void {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (!unmounted) lineStore.load(true, true) // silent 增量刷新，不闪
  }, refreshInterval.value * 1000)
}

onMounted(async () => {
  // 读 show_value session 偏好
  try {
    const s = sessionStorage.getItem('occupancy.showValue')
    if (s) showValue.value = s === 'true'
  } catch { /* ignore */ }
  // 读阈值 config
  try {
    const rsp = await listSystemConfig()
    const cfgs: SystemConfigItem[] = Array.isArray(rsp?.data) ? (rsp.data as SystemConfigItem[]) : []
    for (const c of cfgs) {
      const v = Number(c.configValue)
      if (c.configKey === 'occupancy.warn_threshold' && !Number.isNaN(v)) warnThreshold.value = v
      else if (c.configKey === 'occupancy.good_threshold' && !Number.isNaN(v)) goodThreshold.value = v
      else if (c.configKey === 'occupancy.refresh_interval' && v > 0) refreshInterval.value = v
      else if (c.configKey === 'occupancy.show_value') showValue.value = c.configValue.toLowerCase() === 'true'
    }
  } catch { /* 默认 */ }
  // query 带 lineNo 时预填搜索
  const q = route.query.line as string | undefined
  if (q) searchLine.value = q
  lineStore.load(true)
  startTimer()})

onBeforeUnmount(() => {
  unmounted = true
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.pb-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 10px 14px;
  background: rgba(0, 0, 0, 0.2);
  -webkit-backdrop-filter: var(--glass-blur-soft);
  backdrop-filter: var(--glass-blur-soft);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
}
.pb-toolbar__left,
.pb-toolbar__right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.pb-toolbar__label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.pb-toolbar__sep {
  width: 1px;
  height: 18px;
  background: var(--glass-border);
}
.pb-toolbar input[type='checkbox'] {
  accent-color: var(--accent);
  cursor: pointer;
}
.pb-toolbar__search {
  width: 120px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: var(--font-size-xs);
}
.pb-toolbar__legend {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.pb-toolbar__dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
  margin-right: 2px;
}
.pb-toolbar__dot--red { background: #ff5a5f; }
.pb-toolbar__dot--yellow { background: #ffd75e; }
.pb-toolbar__dot--green { background: #5fd97f; }
.pb-toolbar__dot--gray { background: #4a4f58; }
.pb-toolbar__refresh {
  background: var(--gradient-brand);
  color: var(--text-on-accent);
  border: none;
  border-radius: var(--radius-sm);
  padding: 4px 12px;
  font-size: var(--font-size-xs);
  cursor: pointer;
}

.pb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: var(--space-3);
}
.pb-group {
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 10px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.pb-group:hover {
  border-color: var(--accent);
  box-shadow: 0 0 12px rgba(92, 225, 255, 0.18);
}
.pb-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.pb-group__line {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.pb-group__avg {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}
.tone-red { color: #ff5a5f; }
.tone-yellow { color: #ffd75e; }
.tone-green { color: #5fd97f; }
.tone-gray { color: var(--text-secondary); }
.pb-group__faces {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.pb-cell {
  position: relative;
  border-radius: var(--radius-sm);
  padding: 8px 6px;
  text-align: center;
  color: #fff;
  font-weight: var(--font-weight-semibold);
  min-height: 46px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.pb-cell--red { background: linear-gradient(180deg, #ff5a5f, #b6373b); }
.pb-cell--yellow { background: linear-gradient(180deg, #ffd75e, #d3a82a); }
.pb-cell--green { background: linear-gradient(180deg, #5fd97f, #2e9e4f); }
.pb-cell--gray { background: linear-gradient(180deg, #4a4f58, #2c3038); }
.pb-cell__face {
  font-size: 10px;
  opacity: 0.9;
}
.pb-cell__val {
  font-size: 15px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}
</style>
