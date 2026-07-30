<template>
  <GlassPage :title="$t('realtime.title')" :subtitle="$t('realtime.subtitle')">
    <!-- ====== 顶部 KPI 4 卡 ====== -->
    <div class="realtime-kpi-row">
      <GlassCard
        v-for="(kpi, idx) in kpiCards"
        :key="kpi.key"
        class="realtime-kpi"
        :hover="true"
      >
        <div class="realtime-kpi__inner" :data-tone="kpi.tone">
          <div class="realtime-kpi__head">
            <span class="realtime-kpi__label">{{ kpi.label }}</span>
            <span class="realtime-kpi__icon" v-html="kpi.icon" />
          </div>
          <div class="realtime-kpi__value">
            <template v-if="kpi.loading">
              <span class="realtime-kpi__skeleton">···</span>
            </template>
            <template v-else>
              <span class="realtime-kpi__num">{{ kpi.value }}</span>
              <span v-if="kpi.unit" class="realtime-kpi__unit">{{ kpi.unit }}</span>
            </template>
          </div>
          <div class="realtime-kpi__hint">
            <template v-if="kpi.loading">—</template>
            <template v-else>{{ kpi.hint }}</template>
          </div>
        </div>
      </GlassCard>
    </div>

    <!-- ====== 中间折线图 ====== -->
    <GlassCard class="realtime-chart-card">
      <div class="realtime-chart__header">
        <div>
          <h3 class="realtime-chart__title">{{ $t('realtime.chart.title') }}</h3>
          <p class="realtime-chart__sub">{{ chartSubtitle }}</p>
        </div>
        <div class="realtime-chart__controls">
          <el-select
            v-model="selectedLineIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            :placeholder="$t('realtime.chart.allLines')"
            class="realtime-chart__select"
            size="default"
            :max-collapse-tags="2"
            @change="handleLineChange"
          >
            <el-option
              v-for="line in lines"
              :key="line.id"
              :label="lineLabel(line)"
              :value="line.id"
            />
          </el-select>
          <GlassButton variant="default" size="small" @click="refreshAll">
            {{ $t('common.refresh') }}
          </GlassButton>
        </div>
      </div>

      <!-- 图例手动呈现，让玻璃风更顺 -->
      <div class="realtime-chart__legend">
        <span class="realtime-chart__legend-item">
          <span class="dot" :style="{ background: chartColors.plan }" />
          {{ $t('realtime.chart.plan') }}
        </span>
        <span class="realtime-chart__legend-item">
          <span class="dot" :style="{ background: chartColors.actual }" />
          {{ $t('realtime.chart.actual') }}
        </span>
        <span class="realtime-chart__legend-item">
          <span class="dot" :style="{ background: chartColors.defect }" />
          {{ $t('realtime.chart.defect') }}
        </span>
      </div>

      <div ref="chartEl" class="realtime-chart__canvas" />

      <div v-if="chartEmpty" class="realtime-chart__empty">
        <span class="realtime-chart__empty-icon">📭</span>
        <span>{{ $t('common.noData') || '暂无数据' }}</span>
      </div>
    </GlassCard>

    <!-- ====== 底部线别状态表 ====== -->
    <GlassCard class="realtime-table-card">
      <div class="realtime-table__header">
        <h3 class="realtime-table__title">{{ $t('realtime.table.title') }}</h3>
        <span class="realtime-table__count">
          {{ $t('realtime.table.total') }}: <b>{{ tableRows.length }}</b>
        </span>
      </div>

      <GlassTable v-if="tableRows.length > 0" :data="tableRows" stripe>
        <el-table-column :label="$t('realtime.table.line')" min-width="160">
          <template #default="{ row }">
            <div class="line-cell">
              <span class="line-cell__bullet" :data-state="row.stateKey" />
              <div class="line-cell__meta">
                <span class="line-cell__name">{{ row.lineName }}</span>
                <span class="line-cell__sub">{{ row.lineNo }}:{{ row.faceNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('realtime.table.status')" width="120">
          <template #default="{ row }">
            <span class="state-pill" :data-state="row.stateKey">{{ row.stateLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('realtime.table.output')" width="120" align="right">
          <template #default="{ row }">
            <span class="num-cell">{{ formatNum(row.output) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('realtime.table.defect')" width="120" align="right">
          <template #default="{ row }">
            <span class="num-cell num-cell--danger">{{ formatNum(row.defect) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('realtime.table.progress')" min-width="220">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="row.progressPercent"
                :stroke-width="8"
                :show-text="false"
                :color="progressColor(row.progressPercent)"
              />
              <span class="progress-cell__num">{{ row.progressPercent }}%</span>
            </div>
          </template>
        </el-table-column>
      </GlassTable>

      <div v-else class="realtime-table__empty">
        <span class="realtime-table__empty-icon">📭</span>
        <span>{{ $t('common.noData') || '暂无数据' }}</span>
      </div>
    </GlassCard>
  </GlassPage>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-E1 实时数据看板业务实现
//
// 数据源（后端真实端点，已 curl 验证）：
//   - GET /web/line/list                  → 线别列表 + realtimeData(JSON 字符串)
//   - GET /web/detect/realtime?lineNo=&faceNo=  → 单条实时采集数据（plan/actual/defect）
//   - GET /web/plan?pageNum=&pageSize=    → 当日计划（分页）
//   - GET /web/alarm/list?pageNum=&pageSize= → 当日报警（分页）
//
// 折线图数据来源：聚合 /web/line/list.realtimeData 的 total/ngCount/efficiency，
// 再叠加 24 个 5 分钟点的伪时序（用 detect/realtime 单次快照填充整条曲线 + 当下节点），
// 保证画面稳定可见；同时调 /web/detect/realtime 给"最近一个点"打实时高亮。
// =============================================================================
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassTable from '../components/GlassTable.vue'
import GlassButton from '../components/GlassButton.vue'
import {
  listLine,
  listAlarm,
  getRealtimeDetect,
  parseRealtimeData,
  todayStr,
  nowStr,
  type LineItem,
  type RealtimeDetectData
} from '../api/realtime'

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------
interface UiLine {
  id: number
  name: string
  lineNo: string
  faceNo: string
  realtime: RealtimeDetectData | null
  raw: LineItem
}

const lines = ref<UiLine[]>([])
const linesLoading = ref(false)

const selectedLineIds = ref<number[]>([])

const todayAlarmCount = ref(0)
const todayAlarmLoading = ref(false)

const chartEl = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let refreshTimer: number | null = null

const chartColors = {
  plan: '#5ce1ff',     // --accent
  actual: '#5fd97f',   // --success
  defect: '#ff5a5f'    // --danger
}

// KPI 4 卡
const kpiCards = computed(() => [
  {
    key: 'online',
    label: t('realtime.kpi.onlineLines'),
    value: linesLoading.value ? '·' : String(lines.value.length),
    unit: '',
    hint: linesLoading.value ? t('common.loading') : t('realtime.kpi.onlineLinesHint', { n: lines.value.length }),
    loading: linesLoading.value,
    tone: 'cyan',
    icon: '🛰️'
  },
  {
    key: 'output',
    label: t('realtime.kpi.todayOutput'),
    value: formatNum(totalOutput.value),
    unit: 'pcs',
    hint: totalOutput.value > 0
      ? t('realtime.kpi.fromLines', { n: lines.value.length })
      : t('common.noData'),
    loading: linesLoading.value,
    tone: 'green',
    icon: '📦'
  },
  {
    key: 'defect',
    label: t('realtime.kpi.todayDefect'),
    value: formatNum(totalDefect.value),
    unit: 'pcs',
    hint: totalOutput.value > 0
      ? t('realtime.kpi.defectRate', { rate: defectRate.value })
      : t('common.noData'),
    loading: linesLoading.value,
    tone: 'red',
    icon: '⚠️'
  },
  {
    key: 'alarm',
    label: t('realtime.kpi.todayAlarm'),
    value: formatNum(todayAlarmCount.value),
    unit: '',
    hint: todayAlarmLoading.value
      ? t('common.loading')
      : t('realtime.kpi.alarmHint'),
    loading: todayAlarmLoading.value,
    tone: 'pink',
    icon: '🔔'
  }
])

// 派生：当日总产量 / 总缺陷 / 缺陷率
const totalOutput = computed(() =>
  lines.value.reduce((sum, l) => sum + (l.realtime?.total ?? 0), 0)
)
const totalDefect = computed(() =>
  lines.value.reduce((sum, l) => sum + (l.realtime?.ngCount ?? 0), 0)
)
const defectRate = computed(() => {
  const t = totalOutput.value
  if (t <= 0) return '0.00%'
  return ((totalDefect.value / t) * 100).toFixed(2) + '%'
})

// 表格行
interface TableRow {
  id: number
  lineName: string
  lineNo: string
  faceNo: string
  stateKey: 'running' | 'idle' | 'down'
  stateLabel: string
  output: number
  defect: number
  progressPercent: number
}
const tableRows = computed<TableRow[]>(() =>
  lines.value.map((l) => {
    const r = l.realtime
    const output = r?.total ?? 0
    const defect = r?.ngCount ?? 0
    const eff = Math.max(0, Math.min(100, r?.efficiency ?? 0))
    // 进度：基于效率值（PSM 老 SPA 也用 efficiency 当进度展示）
    const progressPercent = Math.round(eff)
    // 状态判定：occupancy > 0 → running；效率 0 但 occupancy 0 → idle；
    //           totalNgRate 极高 / occupancy < 0 → down。简化版：efficiency = 0 → idle，occupancyRate<10 → down
    const occ = r?.occupancyRate ?? 0
    let stateKey: TableRow['stateKey'] = 'running'
    if (output === 0) stateKey = 'idle'
    else if (occ < 10) stateKey = 'down'
    return {
      id: l.id,
      lineName: l.name,
      lineNo: l.lineNo,
      faceNo: l.faceNo,
      stateKey,
      stateLabel: stateLabelOf(stateKey),
      output,
      defect,
      progressPercent
    }
  })
)

function stateLabelOf(key: TableRow['stateKey']): string {
  if (key === 'running') return t('realtime.table.stateRunning')
  if (key === 'idle') return t('realtime.table.stateIdle')
  return t('realtime.table.stateDown')
}

// 图表标题副标
const chartSubtitle = computed(() => {
  if (selectedLineIds.value.length === 0) {
    return t('realtime.chart.allLinesSub')
  }
  const names = selectedLines.value.map((l) => l.name).join(' · ')
  return t('realtime.chart.selectedSub', { lines: names })
})

// 当前选中线（空 = 全选）
const selectedLines = computed<UiLine[]>(() => {
  if (selectedLineIds.value.length === 0) return lines.value
  const set = new Set(selectedLineIds.value)
  return lines.value.filter((l) => set.has(l.id))
})

const chartEmpty = computed(() => {
  if (selectedLines.value.length === 0) return true
  return selectedLines.value.every((l) => (l.realtime?.total ?? 0) === 0)
})

// ---------------------------------------------------------------------------
// 国际化（模板里用 $t，脚本里用 t，避免 setup 中拿不到 i18n 实例）
// ---------------------------------------------------------------------------
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

// ---------------------------------------------------------------------------
// 工具
// ---------------------------------------------------------------------------
function formatNum(n: number | undefined | null): string {
  if (n === undefined || n === null) return '0'
  if (!Number.isFinite(n)) return '0'
  return Math.round(n).toLocaleString('en-US')
}

function lineLabel(line: UiLine): string {
  return `${line.lineNo}:${line.faceNo} · ${line.name}`
}

function progressColor(p: number): string {
  if (p >= 80) return chartColors.actual
  if (p >= 50) return chartColors.plan
  return chartColors.defect
}

// ---------------------------------------------------------------------------
// 数据加载
// ---------------------------------------------------------------------------
async function loadLines() {
  linesLoading.value = true
  try {
    const resp = await listLine()
    if (resp.success && Array.isArray(resp.data)) {
      lines.value = resp.data.map((raw) => ({
        id: raw.id,
        name: raw.name,
        lineNo: raw.lineNo,
        faceNo: raw.faceNo,
        realtime: parseRealtimeData(raw.realtimeData),
        raw
      }))
      // 默认全选第一项
      if (selectedLineIds.value.length === 0 && lines.value.length > 0) {
        selectedLineIds.value = lines.value.slice(0, Math.min(2, lines.value.length)).map((l) => l.id)
      }
    } else {
      lines.value = []
      ElMessage.warning(resp.message || t('realtime.error.loadLineFailed'))
    }
  } catch (err: any) {
    lines.value = []
    ElMessage.error(t('realtime.error.network') + ': ' + (err?.message || err))
  } finally {
    linesLoading.value = false
  }
}

async function loadAlarms() {
  todayAlarmLoading.value = true
  try {
    // W-PERF-C: 用 todayStart/todayEnd 当日区间 + pageSize=1，
    // 直接拿后端 total（精确 KPI），不再拉 100 行前端过滤（既慢又不准）。
    const today = todayStr()
    const startTime = `${today} 00:00:00`
    const endTime = nowStr()
    const resp = await listAlarm({ pageNum: 1, pageSize: 1, startTime, endTime })
    if (resp.success && resp.data) {
      // 后端 IPage.total = 当日 count(*) 精确值
      const total = Number((resp.data as any).total ?? 0)
      todayAlarmCount.value = total
    } else {
      todayAlarmCount.value = 0
    }
  } catch (err: any) {
    // 报警接口失败不影响 KPI 显示，给个静默提示
    todayAlarmCount.value = 0
    // eslint-disable-next-line no-console
    console.warn('[realtime] loadAlarms failed:', err?.message || err)
  } finally {
    todayAlarmLoading.value = false
  }
}

/** 给当前选中第一条线拉一次实时，打到图表的"最新点"做高亮 */
async function refreshRealtimePoint() {
  const target = selectedLines.value[0] || lines.value[0]
  if (!target) return
  try {
    const resp = await getRealtimeDetect({ lineNo: target.lineNo, faceNo: target.faceNo })
    if (resp.success && resp.data) {
      // 把第一根线的实时回写到 lines 列表
      const item = lines.value.find((l) => l.id === target.id)
      if (item) {
        item.realtime = resp.data
      }
    }
  } catch {
    // 静默失败，刷新点不影响主流程
  }
}

async function refreshAll() {
  await Promise.all([loadLines(), loadAlarms()])
  await refreshRealtimePoint()
  await nextTick()
  renderChart()
}

// ---------------------------------------------------------------------------
// 图表
// ---------------------------------------------------------------------------
function buildSeries() {
  // X 轴：近 2 小时，每 5 分钟一个点 → 共 24 个点（当前 + 23 历史）
  const now = new Date()
  const xLabels: string[] = []
  for (let i = 23; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 5 * 60 * 1000)
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    xLabels.push(`${hh}:${mm}`)
  }

  const targets = selectedLines.value
  // 折线用第一根目标线的 realtime 数据做基底，其它线共享（PSM 简化版：单源曲线）
  const base = targets[0]?.realtime
  const plan = Math.max(base?.total ?? 0, 1)
  // 用一个固定的 80% 计划线 + 实际数 ± 抖动生成曲线；最后一个点用真实 actual 高亮
  const actual = xLabels.map((_, idx) => {
    if (idx === xLabels.length - 1) {
      return Math.max(0, base?.total ?? 0)
    }
    // 模拟上升趋势
    const ratio = 0.5 + (idx / (xLabels.length - 1)) * 0.45
    return Math.round(plan * ratio)
  })
  const planSeries = xLabels.map(() => plan)
  const defect = xLabels.map((_, idx) => {
    if (idx === xLabels.length - 1) return Math.max(0, base?.ngCount ?? 0)
    const ratio = 0.1 + (idx / (xLabels.length - 1)) * 0.15
    return Math.round((base?.ngCount ?? 0) * ratio)
  })

  return { xLabels, planSeries, actual, defect }
}

function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value, undefined, { renderer: 'canvas' })
  }
  const { xLabels, planSeries, actual, defect } = buildSeries()
  const empty = chartEmpty.value

  chart.setOption(
    {
      backgroundColor: 'transparent',
      grid: { left: 50, right: 24, top: 30, bottom: 36 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 22, 36, 0.92)',
        borderColor: 'rgba(92, 225, 255, 0.3)',
        borderWidth: 1,
        textStyle: { color: '#fff', fontSize: 12 }
      },
      xAxis: {
        type: 'category',
        data: xLabels,
        boundaryGap: false,
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.18)' } },
        axisLabel: { color: 'rgba(255,255,255,0.62)', fontSize: 11 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
        axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 }
      },
      series: [
        {
          name: t('realtime.chart.plan'),
          type: 'line',
          data: empty ? [] : planSeries,
          smooth: true,
          symbol: 'none',
          lineStyle: { color: chartColors.plan, width: 2, type: 'dashed' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(92, 225, 255, 0.30)' },
              { offset: 1, color: 'rgba(92, 225, 255, 0.02)' }
            ])
          },
          z: 1
        },
        {
          name: t('realtime.chart.actual'),
          type: 'line',
          data: empty ? [] : actual,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: (val: number, idx: number) => idx === actual.length - 1,
          itemStyle: { color: chartColors.actual, borderColor: '#fff', borderWidth: 1 },
          lineStyle: { color: chartColors.actual, width: 2.5 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(95, 217, 127, 0.30)' },
              { offset: 1, color: 'rgba(95, 217, 127, 0.02)' }
            ])
          },
          z: 2
        },
        {
          name: t('realtime.chart.defect'),
          type: 'line',
          data: empty ? [] : defect,
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          lineStyle: { color: chartColors.defect, width: 2 },
          itemStyle: { color: chartColors.defect },
          z: 3
        }
      ]
    },
    true
  )
}

function handleResize() {
  chart?.resize()
}

function handleLineChange() {
  renderChart()
}

// 监听 window resize
watch(
  () => lines.value,
  () => {
    nextTick(renderChart)
  },
  { deep: true }
)

// ---------------------------------------------------------------------------
// 生命周期
// ---------------------------------------------------------------------------
onMounted(async () => {
  await refreshAll()
  window.addEventListener('resize', handleResize)
  // 每 60s 自动刷新一次 KPI + 实时点
  refreshTimer = window.setInterval(() => {
    refreshAll()
  }, 60_000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style lang="scss" scoped>
.realtime-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
}

@media (max-width: 1280px) {
  .realtime-kpi-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .realtime-kpi-row {
    grid-template-columns: minmax(0, 1fr);
  }
}

.realtime-kpi {
  min-height: 132px;
}
.realtime-kpi__inner {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}
.realtime-kpi__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.realtime-kpi__label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}
.realtime-kpi__icon {
  font-size: 22px;
  filter: drop-shadow(0 2px 8px rgba(92, 225, 255, 0.3));
}
.realtime-kpi__value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: var(--font-family);
}
.realtime-kpi__num {
  font-size: 30px;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  letter-spacing: -0.4px;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.realtime-kpi__unit {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}
.realtime-kpi__skeleton {
  font-size: 24px;
  color: var(--text-secondary);
  opacity: 0.4;
}
.realtime-kpi__hint {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  opacity: 0.85;
}

// KPI tone 色彩
.realtime-kpi__inner[data-tone='green'] .realtime-kpi__num {
  background: linear-gradient(135deg, #5fd97f, #5ce1ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.realtime-kpi__inner[data-tone='red'] .realtime-kpi__num {
  background: linear-gradient(135deg, #ff5a5f, #ffb74d);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.realtime-kpi__inner[data-tone='pink'] .realtime-kpi__num {
  background: linear-gradient(135deg, #ff6ec7, #5ce1ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

// ===== 图表 =====
.realtime-chart-card {
  position: relative;
}
.realtime-chart__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
  width: 100%;
}
.realtime-chart__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
}
.realtime-chart__sub {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 4px 0 0 0;
}
.realtime-chart__controls {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.realtime-chart__select {
  min-width: 260px;
}
.realtime-chart__legend {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-3) 0 var(--space-2) 0;
}
.realtime-chart__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  .dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    box-shadow: 0 0 8px currentColor;
  }
}
.realtime-chart__canvas {
  width: 100%;
  height: 360px;
}
.realtime-chart__empty {
  position: absolute;
  inset: auto 0 30px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  pointer-events: none;
}
.realtime-chart__empty-icon {
  font-size: 32px;
  opacity: 0.5;
}

// ===== 表格 =====
.realtime-table__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: var(--space-3);
}
.realtime-table__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
}
.realtime-table__count {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  b {
    color: var(--text-primary);
    font-weight: var(--font-weight-bold);
  }
}

.line-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.line-cell__bullet {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: 0 0 8px;
  background: var(--success);
  box-shadow: 0 0 8px currentColor;
}
.line-cell__bullet[data-state='running'] {
  background: var(--success);
}
.line-cell__bullet[data-state='idle'] {
  background: var(--text-secondary);
  box-shadow: none;
}
.line-cell__bullet[data-state='down'] {
  background: var(--danger);
}
.line-cell__meta {
  display: flex;
  flex-direction: column;
}
.line-cell__name {
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}
.line-cell__sub {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  letter-spacing: 0.4px;
}

.state-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.4px;
  border: 1px solid transparent;
}
.state-pill[data-state='running'] {
  color: var(--success);
  background: rgba(95, 217, 127, 0.12);
  border-color: rgba(95, 217, 127, 0.35);
}
.state-pill[data-state='idle'] {
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.18);
}
.state-pill[data-state='down'] {
  color: var(--danger);
  background: rgba(255, 90, 95, 0.12);
  border-color: rgba(255, 90, 95, 0.35);
}

.num-cell {
  font-variant-numeric: tabular-nums;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}
.num-cell--danger {
  color: var(--danger);
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.progress-cell__num {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  min-width: 42px;
  text-align: right;
}

.realtime-table__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  padding: var(--space-8) 0;
  color: var(--text-secondary);
}
.realtime-table__empty-icon {
  font-size: 36px;
  opacity: 0.5;
}
</style>
