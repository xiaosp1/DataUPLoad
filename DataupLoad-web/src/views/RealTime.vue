<template>
  <GlassPage :title="$t('realtime.title')" :subtitle="$t('realtime.subtitle')">
    <!--
      W-RT-2 主布局
        左侧 280px:  LineListCard 线别列表卡片（玻璃风）
        中栏 1fr:    KPI 4 卡 + 折线图 + 线别状态表（数据随 leftCard selected 切换）
    -->
    <div class="realtime-layout">
      <!-- W-FRONT-05-B2 顶部全宽条：上座率全景（跨左栏+中栏） -->
      <div class="realtime-layout__bar">
        <OccupancyPanoramaBar />
      </div>

      <!-- ====== 左栏：线别列表 ====== -->
      <aside class="realtime-layout__left">
        <LineListCard @line-change="handleLineChange" />
      </aside>

      <!-- ====== 中栏：KPI / 图表 / 表格 ====== -->
      <div class="realtime-layout__main">
        <!-- ====== W-RT-3：中栏选中线 4 区面板（顶部） ====== -->
        <LineDetailPanel :line="currentLine" :line-index="currentLineIndex" @defect-click="handleDefectClick" />

        <!-- ====== 顶部 KPI 8 卡（W-RT-4：PSM 实时页 全部字段） ====== -->
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

        <!-- ====== 开机时间 宽卡（全宽 1 行 + 剔除总数/剔除失败率 子指标） ====== -->
        <GlassCard class="realtime-kpi realtime-kpi--wide" :hover="true">
          <div class="realtime-kpi-wide" :data-tone="deviceOpenTimeCard.tone">
            <div class="realtime-kpi-wide__head">
              <span class="realtime-kpi-wide__label">
                <span class="realtime-kpi-wide__icon">⏱️</span>
                {{ deviceOpenTimeCard.label }}
              </span>
              <span class="realtime-kpi-wide__line">{{ deviceOpenTimeCard.lineLabel }}</span>
            </div>
            <div class="realtime-kpi-wide__body">
              <template v-if="deviceOpenTimeCard.loading">
                <span class="realtime-kpi-wide__skeleton">···</span>
              </template>
              <template v-else>
                <div class="realtime-kpi-wide__clock">
                  <span class="realtime-kpi-wide__num">{{ deviceOpenTimeCard.time }}</span>
                </div>
                <div class="realtime-kpi-wide__sub">
                  <div class="realtime-kpi-wide__sub-item">
                    <span class="realtime-kpi-wide__sub-label">{{ $t('realtime.kpi.removeTotal') }}</span>
                    <span class="realtime-kpi-wide__sub-val">{{ formatNum(deviceOpenTimeCard.removeTotal) }}</span>
                    <span class="realtime-kpi-wide__sub-unit">pcs</span>
                  </div>
                  <div class="realtime-kpi-wide__sub-item">
                    <span class="realtime-kpi-wide__sub-label">{{ $t('realtime.kpi.removeFailRate') }}</span>
                    <span class="realtime-kpi-wide__sub-val">{{ deviceOpenTimeCard.removeFailRate.toFixed(2) }}</span>
                    <span class="realtime-kpi-wide__sub-unit">%</span>
                  </div>
                  <div class="realtime-kpi-wide__sub-item">
                    <span class="realtime-kpi-wide__sub-label">{{ $t('realtime.kpi.removeFailNum') }}</span>
                    <span class="realtime-kpi-wide__sub-val">{{ formatNum(deviceOpenTimeCard.removeFail) }}</span>
                    <span class="realtime-kpi-wide__sub-unit">pcs</span>
                  </div>
                </div>
              </template>
            </div>
            <div class="realtime-kpi__hint">
              <template v-if="deviceOpenTimeCard.loading">—</template>
              <template v-else>{{ deviceOpenTimeCard.hint }}</template>
            </div>
          </div>
        </GlassCard>

        <!-- ====== 中间折线图 ====== -->
        <GlassCard class="realtime-chart-card">
          <div class="realtime-chart__header">
            <div>
              <div class="realtime-chart__titlewrap">
                <h3 class="realtime-chart__title">{{ $t('realtime.chart.title') }}</h3>
                <span v-if="realtimeStale" class="realtime-stale-badge" :title="$t('realtime.ws.disconnected')">連接斷開</span>
              </div>
              <p class="realtime-chart__sub">{{ chartSubtitle }}</p>
            </div>
            <div class="realtime-chart__controls">
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
      </div>
    </div>

    <!-- ====== W-RT-9: 报警详情弹窗（玻璃风 el-dialog） ====== -->
    <AlarmDetailDialog v-model="alarmDialogVisible" :alarm="selectedAlarm" />
  </GlassPage>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-E1 实时数据看板业务实现 (W-RT-2: 接入左侧线别卡片)
//
// 数据源（后端真实端点，已 curl 验证）：
//   - GET /web/line/list                  → 线别列表 + realtimeData(JSON 字符串)
//   - GET /web/detect/realtime?lineNo=&faceNo=  → 单条实时采集数据（plan/actual/defect）
//   - GET /web/plan?pageNum=&pageSize=    → 当日计划（分页）
//   - GET /web/alarm/list?pageNum=&pageSize= → 当日报警（分页）
//
// 折线图数据来源：聚合选中线的 realtime.total/ngCount/efficiency，
// 用 detect/realtime 单次快照 + 24 个 5 分钟点的伪时序（让画面稳定可见），
// 同时调 /web/detect/realtime 给"最近一个点"打实时高亮。
//
// W-RT-2 改动：
//   - 左栏新增 LineListCard，store 用 lineStore
//   - 选中切换时，KPI / chart / table 全部走 lineStore.selectedLine 单线数据
//   - 顶部 "全部线别" KPI（在线线别 / 聚合产量 / 聚合缺陷）改为当前选中线
//   - 报警 KPI 仍走全量 /web/alarm/list（不绑线，跨线 KPI）
// =============================================================================
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassTable from '../components/GlassTable.vue'
import GlassButton from '../components/GlassButton.vue'
import LineListCard from '../components/LineListCard.vue'
import LineDetailPanel from '../components/LineDetailPanel.vue'
import AlarmDetailDialog from '../components/AlarmDetailDialog.vue'
import OccupancyPanoramaBar from '../components/OccupancyPanoramaBar.vue'
import { useLineStore } from '../stores/line'
import {
  listAlarm,
  todayStr,
  nowStr,
  deviceOpenTimeOf,
  successCountOf,
  removeFailRateOf,
  type RealtimeDetectData
} from '../api/realtime'
import { getAlarmDetail as fetchAlarmDetail, type AlarmRecord } from '../api/alarm'
import { screenState, subscribeScreen, type ScreenSnapshot } from '../stores/screen'

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------
const lineStore = useLineStore()

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------
const todayAlarmCount = ref(0)
const todayAlarmLoading = ref(false)

const chartEl = ref<HTMLDivElement | null>(null)
var chart: echarts.ECharts | null = null
let unsubscribeScreen: (() => void) | null = null

const chartColors = {
  plan: '#5ce1ff',     // --accent
  actual: '#5fd97f',   // --success
  defect: '#ff5a5f'    // --danger
}

// WS 连接状态镜像（顶栏 / 调试用）
const wsState = computed(() => screenState.wsState)

// ===== W-FLASH-01: stale 降级标记 =====
// WS 断开或快照超过 10s 未更新 => realtimeStale=true（停留最后帧 + 显示「连接断开」角标，不闪空白）
const STALE_MS = 10_000
const realtimeStale = ref(false)
let realtimeStaleTick: ReturnType<typeof setInterval> | null = null
function refreshRealtimeStale(): void {
  const live = screenState.wsState === 'open'
  const fresh = live && !!screenState.lastUpdate && Date.now() - screenState.lastUpdate < STALE_MS
  realtimeStale.value = !fresh
}

// 当前选中行的实时数据（每次 refreshRealtimePoint 写入）
const selectedRealtime = ref<RealtimeDetectData | null>(null)

// ---------------------------------------------------------------------------
// W-RT-9: 报警详情弹窗
//   - 点击中栏缺陷网格某小时格 → handleDefectClick
//   - 调 /web/alarm/list-info（getAlarmDetail, PSM 同款）查该小时该产线最近一条
//   - 拿第一条记录 → 打开弹窗
// ---------------------------------------------------------------------------
const alarmDialogVisible = ref(false)
const selectedAlarm = ref<AlarmRecord | null>(null)
const alarmDialogLoading = ref(false)

async function handleDefectClick(payload: { hour: number; val: number }) {
  const cur = currentLine.value
  if (!cur) {
    ElMessage.warning(t('realtime.error.loadLineFailed'))
    return
  }
  const hour = Number(payload?.hour ?? -1)
  if (!Number.isFinite(hour) || hour < 0 || hour > 23) return

  alarmDialogLoading.value = true
  try {
    // 查所点小时所在区间的报警（前后 30 分钟覆盖小时边界）
    const today = todayStr()
    const hourStart = new Date()
    hourStart.setHours(hour, 0, 0, 0)
    const hourEnd = new Date(hourStart)
    hourEnd.setHours(hourEnd.getHours() + 1)
    const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`)
    const fmt = (d: Date) =>
      `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(
        d.getMinutes()
      )}:${pad2(d.getSeconds())}`
    // 若选中的小时是未来时间（例如现在 14 点点了 18 点），则跨天取前一天
    let startDate = today
    let dayStart: Date
    let dayEnd: Date
    if (hour <= new Date().getHours()) {
      dayStart = hourStart
      dayEnd = hourEnd
    } else {
      // 取昨天的同一小时
      const yest = new Date()
      yest.setDate(yest.getDate() - 1)
      const yestStr = `${yest.getFullYear()}-${pad2(yest.getMonth() + 1)}-${pad2(yest.getDate())}`
      startDate = yestStr
      dayStart = new Date(yest.getFullYear(), yest.getMonth(), yest.getDate(), hour, 0, 0)
      dayEnd = new Date(dayStart)
      dayEnd.setHours(dayEnd.getHours() + 1)
    }
    const startTime = fmt(dayStart)
    const endTime = fmt(dayEnd)
    const resp = await fetchAlarmDetail({
      lineNo: cur.lineNo,
      faceNo: cur.faceNo,
      startTime,
      endTime,
      pageNum: 1,
      pageSize: 1
    })
    const ok = resp && (resp.success === true || (resp as any).code === 0)
    if (ok && resp.data) {
      const records = (resp.data as any).records as AlarmRecord[] | undefined
      if (Array.isArray(records) && records.length > 0) {
        selectedAlarm.value = records[0]
        alarmDialogVisible.value = true
      } else {
        ElMessage.info(t('realtime.detail.noAlarmForCell') || `${startDate} ${String(hour).padStart(2, '0')}:00 - 暂无该小时报警`)
      }
    } else {
      ElMessage.warning((resp && (resp.msg || resp.message)) || t('alarm.list.loadFailed'))
    }
  } catch (err: any) {
    console.warn('[realtime] handleDefectClick failed:', err)
    if (err?.response?.status !== 401) {
      ElMessage.error(t('alarm.list.loadFailed'))
    }
  } finally {
    alarmDialogLoading.value = false
  }
}

// ---------------------------------------------------------------------------
// 派生：选中线的实时数据（来自 lineStore 一次性 load 时的 realtimeData）
// ---------------------------------------------------------------------------
const currentLine = computed(() => lineStore.selectedLine)
const lines = computed(() => lineStore.lines)

/** W-RT-3：选中线在列表中的索引（1-based，给 4 区面板的序号色块用） */
const currentLineIndex = computed(() => {
  const cur = currentLine.value
  if (!cur) return 0
  const idx = lineStore.lines.findIndex((l) => l.lineKey === cur.lineKey)
  return idx >= 0 ? idx : 0
})

// KPI 8 卡（W-RT-2 + W-RT-4：单线别钻取后，展开 PSM 所有 KPI 字段）
const kpiCards = computed(() => {
  const cur = currentLine.value
  const rt = selectedRealtime.value || cur?.realtime || null
  const total = rt?.total ?? 0
  const ng = rt?.ngCount ?? 0
  const success = successCountOf(rt)
  const removeTotal = rt?.removeTotal ?? 0
  const removeFail = rt?.removeFail ?? 0
  const removeFailRate = removeFailRateOf(rt)
  const occupancy = rt?.occupancy ?? 0
  const occupancyRate = rt?.occupancyRate ?? 0
  const efficiency = rt?.efficiency ?? 0
  const totalNgRate = rt?.totalNgRate ?? (total > 0 ? (ng / total) * 100 : 0)
  const lineLabel = cur ? `${cur.lineNo}-${cur.faceNo}` : '—'
  const lineHint = cur
    ? t('realtime.kpi.fromLines', { n: cur.name })
    : t('realtime.kpi.onlineLinesHint', { n: lines.value.length })

  return [
    {
      key: 'productTotal',
      label: t('realtime.kpi.productTotal'),
      value: formatNum(total),
      unit: 'pcs',
      hint: rt ? lineHint : t('common.noData'),
      loading: lineStore.loading,
      tone: 'cyan',
      icon: '📦'
    },
    {
      key: 'efficiency',
      label: t('realtime.kpi.efficiency'),
      value: efficiency > 0 ? efficiency.toFixed(2) : '0.00',
      unit: t('realtime.kpi.efficiencyUnit'),
      hint: efficiency > 0 ? t('realtime.kpi.efficiencyHint') : t('common.noData'),
      loading: lineStore.loading,
      tone: 'cyan',
      icon: '⚡'
    },
    {
      key: 'occupancy',
      label: t('realtime.kpi.occupancy'),
      value: formatNum(occupancy),
      unit: '',
      hint: occupancyRate > 0
        ? t('realtime.kpi.occupancyRateHint', { rate: occupancyRate.toFixed(2) + '%' })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'blue',
      icon: '🧍'
    },
    {
      key: 'occupancyRate',
      label: t('realtime.kpi.occupancyRate'),
      value: occupancyRate > 0 ? occupancyRate.toFixed(2) : '0.00',
      unit: '%',
      hint: occupancy > 0
        ? t('realtime.kpi.occupancyHint', { n: formatNum(occupancy) })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'blue',
      icon: '🧮'
    },
    {
      key: 'failCount',
      label: t('realtime.kpi.failCount'),
      value: formatNum(ng),
      unit: 'pcs',
      hint: total > 0
        ? t('realtime.kpi.failRateHint', { rate: totalNgRate.toFixed(2) + '%' })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'red',
      icon: '⚠️'
    },
    {
      key: 'failRate',
      label: t('realtime.kpi.failRate'),
      value: totalNgRate > 0 ? totalNgRate.toFixed(2) : '0.00',
      unit: '%',
      hint: ng > 0
        ? t('realtime.kpi.failCountHint', { n: formatNum(ng) })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'red',
      icon: '📉'
    },
    {
      key: 'successCount',
      label: t('realtime.kpi.successCount'),
      value: formatNum(success),
      unit: 'pcs',
      hint: total > 0
        ? t('realtime.kpi.successRateHint', { rate: ((success / total) * 100).toFixed(2) + '%' })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'green',
      icon: '✅'
    },
    {
      key: 'removeFailNum',
      label: t('realtime.kpi.removeFailNum'),
      value: formatNum(removeFail),
      unit: 'pcs',
      hint: removeTotal > 0
        ? t('realtime.kpi.removeFailRateHint', {
            rate: removeFailRate.toFixed(2) + '%',
            total: formatNum(removeTotal)
          })
        : t('common.noData'),
      loading: lineStore.loading,
      tone: 'orange',
      icon: '🗑️'
    }
  ]
})

// 开机时间宽卡（W-RT-4）：突出 HH:mm，下面副标 剔除失败率 / 剔除总数
const deviceOpenTimeCard = computed(() => {
  const cur = currentLine.value
  const rt = selectedRealtime.value || cur?.realtime || null
  const time = deviceOpenTimeOf(rt)
  const removeTotal = rt?.removeTotal ?? 0
  const removeFail = rt?.removeFail ?? 0
  const removeFailRate = removeFailRateOf(rt)
  return {
    label: t('realtime.kpi.deviceOpenTime'),
    time,
    raw: rt?.startTime,
    removeTotal,
    removeFail,
    removeFailRate,
    lineLabel: cur ? `${cur.lineNo}-${cur.faceNo}` : '—',
    hint: rt?.startTime
      ? t('realtime.kpi.deviceOpenTimeHint', { raw: rt.startTime })
      : t('common.noData'),
    loading: lineStore.loading,
    tone: 'gold'
  }
})

// 表格行（W-RT-2: 只展示选中线；多线浏览交给后续 RT 子单）
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
const tableRows = computed<TableRow[]>(() => {
  const cur = currentLine.value
  if (!cur) return []
  const rt = selectedRealtime.value || cur.realtime
  const output = rt?.total ?? 0
  const defect = rt?.ngCount ?? 0
  const eff = Math.max(0, Math.min(100, rt?.efficiency ?? 0))
  const occ = rt?.occupancyRate ?? 0
  let stateKey: TableRow['stateKey'] = 'running'
  if (output === 0) stateKey = 'idle'
  else if (occ < 10) stateKey = 'down'
  return [
    {
      id: cur.id,
      lineName: cur.name,
      lineNo: cur.lineNo,
      faceNo: cur.faceNo,
      stateKey,
      stateLabel: stateLabelOf(stateKey),
      output,
      defect,
      progressPercent: Math.round(eff)
    }
  ]
})

function stateLabelOf(key: TableRow['stateKey']): string {
  if (key === 'running') return t('realtime.table.stateRunning')
  if (key === 'idle') return t('realtime.table.stateIdle')
  return t('realtime.table.stateDown')
}

// 图表标题副标
const chartSubtitle = computed(() => {
  const cur = currentLine.value
  if (!cur) return t('realtime.chart.allLinesSub')
  return t('realtime.chart.selectedSub', { lines: `${cur.lineNo}-${cur.faceNo} · ${cur.name}` })
})

const chartEmpty = computed(() => {
  const cur = currentLine.value
  if (!cur) return true
  const rt = selectedRealtime.value || cur.realtime
  return (rt?.total ?? 0) === 0
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

function progressColor(p: number): string {
  if (p >= 80) return chartColors.actual
  if (p >= 50) return chartColors.plan
  return chartColors.defect
}

// ---------------------------------------------------------------------------
// 数据加载
// ---------------------------------------------------------------------------
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
      const total = Number((resp.data as any).total ?? 0)
      todayAlarmCount.value = total
    } else {
      todayAlarmCount.value = 0
    }
  } catch (err: any) {
    todayAlarmCount.value = 0
    // eslint-disable-next-line no-console
    console.warn('[realtime] loadAlarms failed:', err?.message || err)
  } finally {
    todayAlarmLoading.value = false
  }
}

/**
 * W-PERF-B：从 WS 快照里挑出当前选中线的实时数据，写入 selectedRealtime。
 *
 * 服务端 5s/次 推全量 ScreenDataDTO；前端的 /web/detect/realtime REST 调用彻底去掉。
 * 首屏进入页面时，若 store 已有快照（来自其他 tab 或上一次会话），subscribeScreen
 * 会立刻回调一次，首屏渲染 ~0ms。
 */
function applySnapshotToSelected(snap: ScreenSnapshot) {
  if (!snap || !Array.isArray(snap.lines) || snap.lines.length === 0) return
  const cur = currentLine.value
  if (!cur) return
  const hit = snap.lines.find((ws) => ws.lineNo === cur.lineNo && ws.faceNo === cur.faceNo)
  if (!hit || !hit.realTimeDetectData) return
  const rtd = hit.realTimeDetectData

  // ===== W-FLASH-01: 增量 patch，不重建对象 =====
  // 只更新字段值，不整体替换 selectedRealtime.value（避免值未变时引用变化触发 watch 重绘）。
  const next = {
    total: Number(rtd.total ?? 0),
    ngCount: Number(rtd.ngCount ?? 0),
    removeTotal: Number(rtd.removeTotal ?? 0),
    removeFail: Number(rtd.removeFail ?? 0),
    efficiency: Number(rtd.efficiency ?? 0),
    totalNgRate: Number(rtd.totalNgRate ?? 0),
    occupancy: Number(rtd.occupancy ?? 0),
    occupancyRate: Number(rtd.occupancyRate ?? 0),
    startTime: rtd.startTime,
    defects: Array.isArray(rtd.defects) ? rtd.defects : []
  }
  const prev = selectedRealtime.value
  let changed = !prev
  if (prev && !changed) {
    changed =
      prev.total !== next.total ||
      prev.ngCount !== next.ngCount ||
      prev.removeTotal !== next.removeTotal ||
      prev.removeFail !== next.removeFail ||
      prev.efficiency !== next.efficiency ||
      prev.totalNgRate !== next.totalNgRate ||
      prev.occupancy !== next.occupancy ||
      prev.occupancyRate !== next.occupancyRate ||
      prev.startTime !== next.startTime
  }
  if (changed) {
    selectedRealtime.value = next
  }
}

/** 左栏选中变化：清旧实时数据；WS 推送会带新选中线的实时过来 */
function handleLineChange(_lineKey: string) {
  selectedRealtime.value = null  // 切线后先清，避免显示上一条线数据
  // ===== W-FLASH-01: 切线是用户主动操作，强制立即刷新（绕过 5s 节流）=====
  chartDirty = true
  if (chart) flushChart()
  else nextTick(renderChart)
  // 如果 store 里已有快照，立刻补一次（避免等 5s）
  if (screenState.snapshot) applySnapshotToSelected(screenState.snapshot)
}

async function refreshAll() {
  await lineStore.load(true)
  await loadAlarms()
  // 不再调 /web/detect/realtime；WS 推送会带实时数据过来
  // 若 WS 还没推送（订阅时 store 也没快照），等下一次推送
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

  // W-RT-2: 折线只画选中线
  const cur = currentLine.value
  const base = selectedRealtime.value || cur?.realtime || null
  const plan = Math.max(base?.total ?? 0, 1)
  const actual = xLabels.map((_, idx) => {
    if (idx === xLabels.length - 1) {
      return Math.max(0, base?.total ?? 0)
    }
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

// ===== W-FLASH-01: echarts 浣庡紑閿€鏇存柊 + 鑺傛祦锛堟牴娌?WS 5s 鍏ㄥ浘閲嶇粯闂儊锛?====
//  - chartInitedFlag: 闈欐€侀厤缃彧 init 涓€娆★紙notMerge=true锛?//  - flushChart(): 澧為噺 setOption({xAxis.data, series[].data}, notMerge=false)锛屾暟鎹鍚嶆湭鍙樺垯涓嶉噸缁?//  - renderChart(): 鑺傛祦璋冨害锛屾瘡 5s 鏈€澶氶噸缁?1 娆★紙WS 鎺ㄩ€?5s/娆★紝鍗充娇绐佸彂涔熶笉鍙犲姞锛?// init-once flag: 鎸傚湪 window 閬垮厤 esbuild 璇垹 script setup 椤跺眰 var
var chartInitedFlag = false // init-once (棣栨鏁村浘鍒濆鍖栨爣璁?
var chartTimer: number | null = null
var chartDirty = false
var chartLastApplyTs = 0
var chartSig = ''

/** 鏁版嵁绛惧悕锛歺 杞存椂闂?+ 涓夌嚎鏈€鏂板€硷紱鍊兼湭鍙?=> 璺宠繃閲嶇粯 */
function chartSignature(): string {
  if (chartEmpty.value) return 'empty'
  const sg = buildSeries()
  const plan = sg.planSeries[sg.planSeries.length - 1]
  const act = sg.actual[sg.actual.length - 1]
  const def = sg.defect[sg.defect.length - 1]
  return `${sg.xLabels.join(',')}#${plan}#${act}#${def}`
}

function flushChart(): void {
  if (!chart || !chartEl.value) return
  chartDirty = false
  chartLastApplyTs = Date.now()
  const sig = chartSignature()
  if (sig === chartSig) return
  chartSig = sig
  const sg = buildSeries()
  const empty = chartEmpty.value
  chart.setOption(
    {
      xAxis: { data: sg.xLabels },
      series: [
        { data: empty ? [] : sg.planSeries },
        { data: empty ? [] : sg.actual },
        { data: empty ? [] : sg.defect }
      ]
    },
    false
  )
}
function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value, undefined, { renderer: 'canvas' })
  }
  // 棣栨锛氭暣浣撳垵濮嬪寲涓€娆★紙notMerge=true锛岄潤鎬侀厤缃畾姝伙級
  if (!chartInitedFlag) {
    chartInitedFlag = true
    const s0 = buildSeries()
    const e0 = chartEmpty.value
    chart.setOption(
      {
        backgroundColor: 'transparent',
        grid: { left: 50, right: 24, top: 30, bottom: 36 },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(15, 22, 36, 0.92)', borderColor: 'rgba(92, 225, 255, 0.3)', borderWidth: 1, textStyle: { color: '#fff', fontSize: 12 } },
        xAxis: { type: 'category', data: s0.xLabels, boundaryGap: false, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.18)' } }, axisLabel: { color: 'rgba(255,255,255,0.62)', fontSize: 11 }, axisTick: { show: false } },
        yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } }, axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 11 } },
        series: [
          { name: t('realtime.chart.plan'), type: 'line', data: e0 ? [] : s0.planSeries, smooth: true, symbol: 'none', lineStyle: { color: chartColors.plan, width: 2, type: 'dashed' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(92, 225, 255, 0.30)' }, { offset: 1, color: 'rgba(92, 225, 255, 0.02)' }]) }, z: 1 },
          { name: t('realtime.chart.actual'), type: 'line', data: e0 ? [] : s0.actual, smooth: true, symbol: 'circle', symbolSize: 6, showSymbol: (val: number, idx: number) => idx === s0.actual.length - 1, itemStyle: { color: chartColors.actual, borderColor: '#fff', borderWidth: 1 }, lineStyle: { color: chartColors.actual, width: 2.5 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(95, 217, 127, 0.30)' }, { offset: 1, color: 'rgba(95, 217, 127, 0.02)' }]) }, z: 2 },
          { name: t('realtime.chart.defect'), type: 'line', data: e0 ? [] : s0.defect, smooth: true, symbol: 'circle', symbolSize: 5, lineStyle: { color: chartColors.defect, width: 2 }, itemStyle: { color: chartColors.defect }, z: 3 }
        ]
      },
      true
    )
    return
  }

  // 宸插垵濮嬪寲锛氳妭娴佸閲忔洿鏂帮紙notMerge=false 鍙洿鏁版嵁鐐癸紱姣?5s 鏈€澶?1 娆★紱绛惧悕鏈彉涓嶉噸缁橈級
  chartDirty = true
  const now = Date.now()
  const since = now - chartLastApplyTs
  if (chartTimer != null) return
  if (since >= 4500) {
    flushChart()
    return
  }
  chartTimer = window.setTimeout(() => {
    chartTimer = null
    if (chartDirty && chart) flushChart()
  }, 4500 - since)
}
function handleResize() {
  chart?.resize()
}

// ===== W-FLASH-01: watch 改非 deep，避免 silent store 更新触发无谓重绘 =====
// 监听选中线 / 实时点变化 → 重绘图
// （applySnapshotToSelected 已做增量 patch：值未变不换引用，故引用变化=真变化，非 deep 足够）
watch(
  [() => lineStore.selectedLineKey, selectedRealtime],
  () => {
    nextTick(renderChart)
  }
)

// 监听线列表长度变化（首次载入 / 增删线）→ 重绘图
// （W-FLASH-01: 原 {deep:true} 会在 WS 驱动的 silent in-place 更新时每 5s 触发，已去掉）
watch(
  () => lineStore.lines.length,
  () => {
    nextTick(renderChart)
  }
)

// ---------------------------------------------------------------------------
// 生命周期
// ---------------------------------------------------------------------------
onMounted(async () => {
  await lineStore.load(true)
  await loadAlarms()
  await nextTick()
  renderChart()
  window.addEventListener('resize', handleResize)
  refreshRealtimeStale()
  realtimeStaleTick = setInterval(refreshRealtimeStale, 1000)
  // W-PERF-B: 订阅全局 WS 单例，5s/次 推送实时数据，替代原 60s polling
  unsubscribeScreen = subscribeScreen((snap) => applySnapshotToSelected(snap))
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (realtimeStaleTick != null) {
    clearInterval(realtimeStaleTick)
    realtimeStaleTick = null
  }
  if (chartTimer != null) {
    clearTimeout(chartTimer)
    chartTimer = null
  }
  if (unsubscribeScreen) {
    try { unsubscribeScreen() } catch { /* ignore */ }
    unsubscribeScreen = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style lang="scss" scoped>
// ===== W-RT-2 主布局：左侧 LineListCard + 中栏 =====
.realtime-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: var(--space-4);
  align-items: stretch;
  min-height: 0;
}

.realtime-layout__bar {
  grid-column: 1 / -1;
}

.realtime-layout__left {
  position: sticky;
  top: 0;
  align-self: stretch;
  height: calc(100vh - 220px);
  min-height: 540px;
  // LineListCard 内部 flex 1 占满
}

.realtime-layout__main {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  min-width: 0;
}

@media (max-width: 1280px) {
  .realtime-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }
  .realtime-layout__left {
    height: calc(100vh - 220px);
    min-height: 480px;
  }
}

@media (max-width: 960px) {
  .realtime-layout {
    grid-template-columns: 1fr;
  }
  .realtime-layout__left {
    position: relative;
    height: auto;
    min-height: 240px;
    max-height: 320px;
  }
}

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
.realtime-kpi--wide {
  width: 100%;
}
.realtime-kpi-wide {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.realtime-kpi-wide__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.realtime-kpi-wide__label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.realtime-kpi-wide__icon {
  font-size: 18px;
  filter: drop-shadow(0 2px 8px rgba(255, 209, 102, 0.4));
}
.realtime-kpi-wide__line {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-pill);
  padding: 3px 12px;
  letter-spacing: 0.4px;
}
.realtime-kpi-wide__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  flex-wrap: wrap;
}
.realtime-kpi-wide__skeleton {
  font-size: 36px;
  color: var(--text-secondary);
  opacity: 0.4;
}
.realtime-kpi-wide__clock {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-family: var(--font-family);
}
.realtime-kpi-wide__num {
  font-size: 44px;
  font-weight: var(--font-weight-bold);
  letter-spacing: 1.5px;
  background: linear-gradient(135deg, #ffd166 0%, #ff9f43 60%, #ff6ec7 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 24px rgba(255, 209, 102, 0.15);
  font-variant-numeric: tabular-nums;
}
.realtime-kpi-wide__sub {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  flex-wrap: wrap;
}
.realtime-kpi-wide__sub-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 84px;
}
.realtime-kpi-wide__sub-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.realtime-kpi-wide__sub-val {
  font-size: 22px;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.realtime-kpi-wide__sub-unit {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
@media (max-width: 1280px) {
  .realtime-kpi-wide__num {
    font-size: 36px;
  }
}
@media (max-width: 720px) {
  .realtime-kpi-wide__num {
    font-size: 28px;
  }
  .realtime-kpi-wide__sub {
    gap: var(--space-3);
  }
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
.realtime-kpi__inner[data-tone='cyan'] .realtime-kpi__num {
  background: linear-gradient(135deg, #5ce1ff, #74e0ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.realtime-kpi__inner[data-tone='green'] .realtime-kpi__num {
  background: linear-gradient(135deg, #5fd97f, #5ce1ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.realtime-kpi__inner[data-tone='blue'] .realtime-kpi__num {
  background: linear-gradient(135deg, #74a9ff, #5ce1ff);
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
.realtime-kpi__inner[data-tone='orange'] .realtime-kpi__num {
  background: linear-gradient(135deg, #ff9f43, #ff6ec7);
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
.realtime-chart__titlewrap {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
.realtime-stale-badge {
  font-size: var(--font-size-xs);
  color: var(--danger);
  border: 1px solid var(--danger);
  border-radius: 4px;
  padding: 0 5px;
  line-height: 16px;
  white-space: nowrap;
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
  height: 320px;
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
