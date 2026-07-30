<template>
  <div class="screen-page">
    <!-- 装饰光晕 -->
    <div class="screen-page__halo screen-page__halo--1" />
    <div class="screen-page__halo screen-page__halo--2" />
    <div class="screen-page__halo screen-page__halo--3" />

    <!-- 顶部条 -->
    <header class="screen-header">
      <div class="screen-header__left">
        <div class="screen-header__brand">
          <span class="screen-header__logo">▣</span>
          <span class="screen-header__title">{{ $t('screen.title') }}</span>
        </div>
        <span class="screen-header__clock">{{ clockText }}</span>
      </div>

      <div class="screen-header__center">
        <span
          class="screen-header__ws"
          :class="`screen-header__ws--${wsState}`"
          :title="wsLabel"
        >
          <span class="screen-header__ws-dot" />
          <span class="screen-header__ws-text">{{ wsLabel }}</span>
        </span>
      </div>

      <div class="screen-header__right">
        <span class="screen-header__updated">{{ $t('screen.header.lastUpdate') }}: {{ updatedText }}</span>
        <button class="screen-header__btn" type="button" @click="reload" :title="$t('screen.header.refresh')">
          <span class="screen-header__btn-icon">⟳</span>
          <span class="screen-header__btn-text">{{ $t('screen.refresh') }}</span>
        </button>
        <button
          v-if="!isFullscreen"
          class="screen-header__btn screen-header__btn--accent"
          type="button"
          @click="enterFullscreen"
        >
          <span class="screen-header__btn-icon">⛶</span>
          <span class="screen-header__btn-text">{{ $t('screen.header.fullscreen') }}</span>
        </button>
        <button
          v-else
          class="screen-header__btn screen-header__btn--accent"
          type="button"
          @click="exitFullscreen"
        >
          <span class="screen-header__btn-icon">⤡</span>
          <span class="screen-header__btn-text">{{ $t('screen.header.exitFullscreen') }}</span>
        </button>
        <button class="screen-header__btn" type="button" @click="goBack" :title="$t('screen.header.back')">
          <span class="screen-header__btn-icon">←</span>
          <span class="screen-header__btn-text">{{ $t('screen.exit') }}</span>
        </button>
      </div>
    </header>

    <!-- 降级提示（任一 API 失败） -->
    <div v-if="snapshot && snapshot.degraded" class="screen-degraded">
      <span class="screen-degraded__icon">⚠</span>
      <span class="screen-degraded__text">
        {{ $t('screen.error.loadFailed') }} · {{ snapshot.degradedReasons.join(' · ') }}
      </span>
    </div>

    <!-- 4 张 KPI 卡 -->
    <section class="screen-kpi-row">
      <div
        v-for="k in kpiCards"
        :key="k.key"
        class="screen-kpi"
        :class="`screen-kpi--${k.tone}`"
      >
        <div class="screen-kpi__inner">
          <div class="screen-kpi__head">
            <span class="screen-kpi__label">{{ k.label }}</span>
            <span class="screen-kpi__icon">{{ k.icon }}</span>
          </div>
          <div class="screen-kpi__value">
            <span class="screen-kpi__num">{{ k.value }}</span>
            <span class="screen-kpi__unit">{{ k.unit }}</span>
          </div>
          <div class="screen-kpi__hint">{{ k.hint }}</div>
        </div>
      </div>
    </section>

    <!-- 4 个图表 Grid：左大(趋势)、右上(饼图)、右下(报警列表)、下整宽(Grid 线别) -->
    <section class="screen-grid">
      <!-- 折线趋势 -->
      <article class="screen-card screen-card--trend">
        <header class="screen-card__head">
          <div class="screen-card__title-block">
            <h3 class="screen-card__title">{{ $t('screen.chart.trend') }}</h3>
            <p class="screen-card__sub">{{ $t('screen.chart.trendSub') }}</p>
          </div>
          <div class="screen-card__legend">
            <span class="screen-card__legend-item screen-card__legend-item--plan">— {{ $t('realtime.chart.plan') }}</span>
            <span class="screen-card__legend-item screen-card__legend-item--actual">— {{ $t('realtime.chart.actual') }}</span>
            <span class="screen-card__legend-item screen-card__legend-item--defect">— {{ $t('realtime.chart.defect') }}</span>
          </div>
        </header>
        <div ref="trendEl" class="screen-card__chart" />
      </article>

      <!-- 饼图 -->
      <article class="screen-card screen-card--pie">
        <header class="screen-card__head">
          <div class="screen-card__title-block">
            <h3 class="screen-card__title">{{ $t('screen.chart.defectPie') }}</h3>
            <p class="screen-card__sub">{{ $t('screen.chart.pieSub') }}</p>
          </div>
        </header>
        <div ref="pieEl" class="screen-card__chart" />
      </article>

      <!-- 报警列表 -->
      <article class="screen-card screen-card--alarm">
        <header class="screen-card__head">
          <div class="screen-card__title-block">
            <h3 class="screen-card__title">{{ $t('screen.chart.alarmList') }}</h3>
            <p class="screen-card__sub">{{ $t('screen.chart.alarmSub') }}</p>
          </div>
          <span class="screen-card__count">{{ snapshot?.alarms.length ?? 0 }}</span>
        </header>
        <ul class="screen-alarm-list">
          <li v-if="!snapshot || snapshot.alarms.length === 0" class="screen-alarm-list__empty">
            {{ $t('screen.alarm.noData') }}
          </li>
          <li
            v-for="(a, idx) in snapshot?.alarms ?? []"
            :key="`${a.id || a.uuid}-${idx}`"
            class="screen-alarm-item"
            :class="{
              'screen-alarm-item--level-2': a.level === 2,
              'screen-alarm-item--new': a._new
            }"
          >
            <span class="screen-alarm-item__time">{{ a.time }}</span>
            <span class="screen-alarm-item__loc">{{ a.lineNo }}·{{ a.faceNo }}</span>
            <span
              class="screen-alarm-item__type"
              :class="`screen-alarm-item__type--${a.type ?? 0}`"
            >{{ typeLabel(a.type) }}</span>
            <span class="screen-alarm-item__msg">{{ a.message }}</span>
            <span
              class="screen-alarm-item__status"
              :class="`screen-alarm-item__status--${a.solve}`"
            >{{ statusLabel(a.solve) }}</span>
          </li>
        </ul>
      </article>

      <!-- 线别 Grid 状态卡 -->
      <article class="screen-card screen-card--grid">
        <header class="screen-card__head">
          <div class="screen-card__title-block">
            <h3 class="screen-card__title">{{ $t('screen.chart.lineGrid') }}</h3>
            <p class="screen-card__sub">{{ $t('screen.chart.gridSub') }}</p>
          </div>
          <span class="screen-card__count">{{ snapshot?.lines.length ?? 0 }}</span>
        </header>
        <div class="screen-line-grid">
          <div v-if="!snapshot || snapshot.lines.length === 0" class="screen-line-grid__empty">
            {{ $t('screen.error.empty') }}
          </div>
          <div
            v-for="ln in snapshot?.lines ?? []"
            :key="ln.id"
            class="screen-line-card"
            :class="`screen-line-card--${ln.state}`"
          >
            <div class="screen-line-card__head">
              <span class="screen-line-card__name">{{ ln.name }}</span>
              <span class="screen-line-card__state">
                <span class="screen-line-card__state-dot" />
                {{ stateLabel(ln.state) }}
              </span>
            </div>
            <div class="screen-line-card__loc">{{ ln.lineNo }} · {{ ln.faceNo }}</div>
            <div class="screen-line-card__metrics">
              <div class="screen-line-card__metric">
                <span class="screen-line-card__metric-label">{{ $t('screen.line.output') }}</span>
                <span class="screen-line-card__metric-value">{{ formatNum(ln.total) }}</span>
              </div>
              <div class="screen-line-card__metric">
                <span class="screen-line-card__metric-label">{{ $t('screen.line.defect') }}</span>
                <span class="screen-line-card__metric-value screen-line-card__metric-value--defect">
                  {{ formatNum(ln.ngCount) }}
                </span>
              </div>
            </div>
            <div class="screen-line-card__bar">
              <span
                class="screen-line-card__bar-fill"
                :style="{ width: progressPercent(ln) + '%' }"
              />
            </div>
          </div>
        </div>
      </article>
    </section>

    <!-- 跑马灯 -->
    <footer class="screen-ticker" :class="{ 'screen-ticker--empty': tickerLines.length === 0 }">
      <span class="screen-ticker__label">
        <span class="screen-ticker__label-icon">📡</span>
        {{ $t('screen.ticker.title') }}
      </span>
      <div class="screen-ticker__viewport">
        <div class="screen-ticker__track" :key="tickerFingerprint">
          <span
            v-for="(line, i) in tickerLines"
            :key="`t-${i}`"
            class="screen-ticker__item"
            :class="{ 'screen-ticker__item--serious': line.level === 2 }"
          >
            <span class="screen-ticker__time">[{{ line.time }}]</span>
            <span class="screen-ticker__loc">{{ line.lineNo }}·{{ line.faceNo }}</span>
            <span class="screen-ticker__msg">{{ line.message }}</span>
            <span class="screen-ticker__sep">●</span>
          </span>
          <span v-if="tickerLines.length === 0" class="screen-ticker__item screen-ticker__item--empty">
            {{ $t('screen.ticker.noData') }}
          </span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-E8 大屏模式业务页
//
// 4 echarts 图 + 1 列表 + 1 Grid 状态卡 + 跑马灯 + WS 实时
//
// 数据源（按真实后端契约）：
//   - /web/line/list                  线别 + realtimeData（聚合 KPI）
//   - /web/alarm/list                 最新报警（分页）
//   - /web/detect/day-record/list-between  当日缺陷日记录（饼图聚合）
//   - /web/screen/data                已知 500，端点存在但后端空实现 → 降级
//
// WS：/ws?uid=&type=screen（30s 心跳，断线自动重连 — 由 utils/screenWs.ts 提供）
//
// i18n：screen.* 全套三语 key 已就位
//
// 设计：
//   - 全屏沉浸式（无 sidebar / topbar chrome — 由 MainLayout 隐藏）
//   - 顶栏：左 brand + clock / 中 WS 状态 / 右 refresh / fullscreen / exit
//   - KPI 4 卡（玻璃态 / 渐变数字）
//   - 4 区 Grid：trend 左大 / pie 右上 / alarm 右下 / line grid 下整宽
//   - 底部 CSS 跑马灯（animation: ticker-roll）；无依赖
// =============================================================================

import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { useI18n } from 'vue-i18n'
import { fetchScreenSnapshot, type ScreenSnapshot, type ScreenLine } from '../api/screen'
import { connectScreenWs, type ScreenWsMessage } from '../utils/screenWs'
import type { WsController, WsState } from '../utils/ws'
import { useUserStore } from '../stores/user'

const { t: $t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------
const snapshot = ref<ScreenSnapshot | null>(null)
const wsState = ref<WsState>('idle')
const now = ref(Date.now())
let wsCtrl: WsController | null = null
let clockTimer: number | null = null
let dataTimer: number | null = null
let fetchSeq = 0

// ---------------------------------------------------------------------------
// 工具
// ---------------------------------------------------------------------------
function formatNum(n: number | undefined | null): string {
  if (n === undefined || n === null) return '0'
  if (!Number.isFinite(n)) return '0'
  return Math.round(n).toLocaleString('en-US')
}

function pad2(n: number) {
  return n < 10 ? `0${n}` : `${n}`
}

function fmtClock(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

function fmtUpdated(d: Date): string {
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

function progressPercent(ln: ScreenLine): number {
  // 用 efficiency（0-100）当进度
  return Math.max(0, Math.min(100, Math.round(ln.efficiency || 0)))
}

// ---------------------------------------------------------------------------
// 顶栏文字（计算）
// ---------------------------------------------------------------------------
const clockText = computed(() => fmtClock(new Date(now.value)))

const updatedText = computed(() => {
  if (!snapshot.value) return '--:--:--'
  return fmtUpdated(new Date(snapshot.value.fetchedAt))
})

const wsLabel = computed(() => {
  if (wsState.value === 'open') return $t('screen.header.connected') as string
  if (wsState.value === 'connecting') return $t('screen.header.connecting') as string
  return $t('screen.header.disconnected') as string
})

// ---------------------------------------------------------------------------
// KPI 卡
// ---------------------------------------------------------------------------
const kpiCards = computed(() => {
  const k = snapshot.value?.kpi
  return [
    {
      key: 'online',
      label: $t('screen.kpi.onlineLines'),
      value: formatNum(k?.onlineLines ?? 0),
      unit: '',
      hint: '',
      tone: 'cyan',
      icon: '🛰️'
    },
    {
      key: 'output',
      label: $t('screen.kpi.todayOutput'),
      value: formatNum(k?.todayOutput ?? 0),
      unit: $t('screen.kpi.unit'),
      hint: '',
      tone: 'green',
      icon: '📦'
    },
    {
      key: 'defect',
      label: $t('screen.kpi.todayDefect'),
      value: formatNum(k?.todayDefect ?? 0),
      unit: $t('screen.kpi.unit'),
      hint: '',
      tone: 'red',
      icon: '⚠️'
    },
    {
      key: 'alarm',
      label: $t('screen.kpi.todayAlarm'),
      value: formatNum(k?.todayAlarm ?? 0),
      unit: '',
      hint: '',
      tone: 'pink',
      icon: '🔔'
    }
  ]
})

// ---------------------------------------------------------------------------
// 类型 / 状态 标签
// ---------------------------------------------------------------------------
function typeLabel(type: number | undefined): string {
  if (type === 1) return $t('screen.alarm.typeDefect') as string
  if (type === 2) return $t('screen.alarm.typeSystem') as string
  if (type === 3) return $t('screen.alarm.typeDevice') as string
  return '—'
}
function levelLabel(level: number | undefined): string {
  if (level === 1) return $t('screen.alarm.levelNormal') as string
  if (level === 2) return $t('screen.alarm.levelSerious') as string
  return '—'
}
function statusLabel(s: number | undefined): string {
  if (s === 1) return $t('screen.alarm.statusHandled') as string
  if (s === 2) return $t('screen.alarm.statusPending') as string
  if (s === 3) return $t('screen.alarm.statusIgnored') as string
  return '—'
}
function stateLabel(state: ScreenLine['state']): string {
  if (state === 'running') return $t('screen.line.running') as string
  if (state === 'idle') return $t('screen.line.idle') as string
  return $t('screen.line.down') as string
}

// ---------------------------------------------------------------------------
// 跑马灯
// ---------------------------------------------------------------------------
const tickerLines = ref<Array<{ time: string; lineNo: string; faceNo: string; message: string; level?: number }>>([])
const tickerFingerprint = ref('init')

function pushTicker(line: { time: string; lineNo: string; faceNo: string; message: string; level?: number }) {
  tickerLines.value.unshift(line)
  // 最多保留 30 条，避免 DOM 过大
  if (tickerLines.value.length > 30) tickerLines.value.length = 30
  tickerFingerprint.value = String(Date.now())
}

function initTickerFromSnapshot() {
  tickerLines.value = []
  const alarms = snapshot.value?.alarms ?? []
  for (const a of alarms) {
    tickerLines.value.push({
      time: a.time,
      lineNo: a.lineNo,
      faceNo: a.faceNo,
      message: a.message,
      level: a.level
    })
  }
  tickerFingerprint.value = `snap-${snapshot.value?.fetchedAt ?? Date.now()}`
}

// ---------------------------------------------------------------------------
// echarts 实例
// ---------------------------------------------------------------------------
const trendEl = ref<HTMLDivElement | null>(null)
const pieEl = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const trendColors = {
  plan: '#5ce1ff',
  actual: '#5fd97f',
  defect: '#ff5a5f'
}

const pieColors = ['#5ce1ff', '#ff6ec7', '#ffb74d', '#5fd97f', '#ff5a5f', '#8a7bff']

function renderTrend() {
  if (!trendEl.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendEl.value, undefined, { renderer: 'canvas' })
  }
  const t = snapshot.value?.trend
  const empty = !t || t.actual.every((v) => v === 0)

  trendChart.setOption(
    {
      backgroundColor: 'transparent',
      grid: { left: 56, right: 28, top: 24, bottom: 36 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 22, 36, 0.92)',
        borderColor: 'rgba(92, 225, 255, 0.3)',
        borderWidth: 1,
        textStyle: { color: '#fff', fontSize: 12 }
      },
      xAxis: {
        type: 'category',
        data: t?.timeLabels ?? [],
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
          name: $t('realtime.chart.plan') as string,
          type: 'line',
          data: empty ? [] : t?.plan ?? [],
          smooth: true,
          symbol: 'none',
          lineStyle: { color: trendColors.plan, width: 2, type: 'dashed' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(92, 225, 255, 0.18)' },
              { offset: 1, color: 'rgba(92, 225, 255, 0.01)' }
            ])
          },
          z: 1
        },
        {
          name: $t('realtime.chart.actual') as string,
          type: 'line',
          data: empty ? [] : t?.actual ?? [],
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: (val: number, idx: number) => idx === (t?.actual.length ?? 0) - 1,
          itemStyle: { color: trendColors.actual, borderColor: '#fff', borderWidth: 1 },
          lineStyle: { color: trendColors.actual, width: 2.5 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(95, 217, 127, 0.22)' },
              { offset: 1, color: 'rgba(95, 217, 127, 0.01)' }
            ])
          },
          z: 2
        },
        {
          name: $t('realtime.chart.defect') as string,
          type: 'line',
          data: empty ? [] : t?.defect ?? [],
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          lineStyle: { color: trendColors.defect, width: 2 },
          itemStyle: { color: trendColors.defect },
          z: 3
        }
      ]
    },
    true
  )
}

function renderPie() {
  if (!pieEl.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieEl.value, undefined, { renderer: 'canvas' })
  }
  const data = snapshot.value?.defectPie ?? []
  const empty = data.every((d) => d.value === 0)

  pieChart.setOption(
    {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 22, 36, 0.92)',
        borderColor: 'rgba(92, 225, 255, 0.3)',
        borderWidth: 1,
        textStyle: { color: '#fff', fontSize: 12 },
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: 16,
        top: 'middle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: 'rgba(255,255,255,0.78)', fontSize: 12 }
      },
      color: pieColors,
      series: [
        {
          name: $t('screen.chart.defectPie') as string,
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['38%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: 'rgba(15, 22, 36, 0.6)',
            borderWidth: 2
          },
          label: {
            show: true,
            color: 'rgba(255,255,255,0.78)',
            fontSize: 11,
            formatter: '{b}\n{d}%'
          },
          labelLine: { lineStyle: { color: 'rgba(255,255,255,0.32)' } },
          data: empty ? data.map((d) => ({ ...d, value: 0 })) : data,
          emptyCircleStyle: { color: 'rgba(255,255,255,0.06)' }
        }
      ]
    },
    true
  )
}

function renderAllCharts() {
  renderTrend()
  renderPie()
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

// ---------------------------------------------------------------------------
// 数据加载
// ---------------------------------------------------------------------------
async function loadSnapshot() {
  const my = ++fetchSeq
  try {
    const data = await fetchScreenSnapshot()
    if (my !== fetchSeq) return
    snapshot.value = data
    initTickerFromSnapshot()
    nextTick(renderAllCharts)
  } catch (err: any) {
    if (my !== fetchSeq) return
    console.warn('[screen] snapshot failed', err)
    if (err?.response?.status !== 401) {
      ElMessage.warning($t('screen.error.loadFailed') as string)
    }
  }
}

function reload() {
  loadSnapshot()
}

// ---------------------------------------------------------------------------
// WebSocket（/ws?uid=&type=screen）
// ---------------------------------------------------------------------------
function pushAlarm(alarm: any) {
  if (!alarm || typeof alarm !== 'object') return
  const cur = snapshot.value
  if (!cur) return
  // 头部插入
  cur.alarms.unshift({ ...alarm, _new: true })
  // 总数 +1
  cur.kpi.todayAlarm += 1
  // 跑马灯推一条
  pushTicker({
    time: alarm.time ?? fmtClock(new Date()),
    lineNo: alarm.lineNo ?? '',
    faceNo: alarm.faceNo ?? '',
    message: alarm.message ?? '',
    level: alarm.level
  })
  // 5 秒后清除 _new 标记
  window.setTimeout(() => {
    if (!snapshot.value) return
    const idx = snapshot.value.alarms.findIndex(
      (a) => (alarm.uuid && a.uuid === alarm.uuid) || a.id === alarm.id
    )
    if (idx >= 0) delete (snapshot.value.alarms[idx] as any)._new
  }, 5000)
}

function connectWs() {
  if (wsCtrl) return
  const uid = userStore.id ? String(userStore.id) : 'web'
  wsCtrl = connectScreenWs(
    uid,
    (msg: ScreenWsMessage) => {
      const tp = String(msg?.type || '')
      const payload = msg?.payload
      if (!payload || typeof payload !== 'object') return
      // 路由：alarm → pushAlarm；defect → KPI 增 1；device/line → 仅记录
      if (tp === 'alarm' || tp === 'push-alarm' || tp === 'new-alarm') {
        const cand: any = (payload as any).data ?? payload
        if (cand && (cand.id || cand.uuid || cand.message || cand.time)) {
          pushAlarm(cand)
        }
      } else if (tp === 'defect') {
        // 缺陷实时推送：KPI 累计
        const cand: any = (payload as any).data ?? payload
        if (snapshot.value && cand && typeof cand.count === 'number') {
          snapshot.value.kpi.todayDefect += cand.count
        }
      } else if (tp === 'heartbeat') {
        // ignore（createWs 已有心跳）
      }
    },
    (s) => {
      wsState.value = s
    }
  )
  wsCtrl.open()
}

function disconnectWs() {
  if (wsCtrl) {
    try {
      wsCtrl.close()
    } catch {
      /* ignore */
    }
    wsCtrl = null
  }
}

// ---------------------------------------------------------------------------
// 全屏切换
// ---------------------------------------------------------------------------
const isFullscreen = ref(false)

function syncFullscreenState() {
  isFullscreen.value = !!document.fullscreenElement
}

function enterFullscreen() {
  const el = document.documentElement
  const req = el.requestFullscreen || (el as any).webkitRequestFullscreen || (el as any).mozRequestFullScreen || (el as any).msRequestFullscreen
  if (req) {
    req.call(el).catch(() => {
      /* user denied or unsupported */
    })
  }
}

function exitFullscreen() {
  if (document.fullscreenElement) {
    const exit = document.exitFullscreen || (document as any).webkitExitFullscreen || (document as any).mozCancelFullScreen || (document as any).msExitFullscreen
    if (exit) exit.call(document)
  }
}

function goBack() {
  // 退出全屏 + 返回 realtime
  exitFullscreen()
  router.push({ name: 'RealTime' })
}

// ---------------------------------------------------------------------------
// 生命周期
// ---------------------------------------------------------------------------
onMounted(async () => {
  // 1. 时钟
  clockTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)

  // 2. 全屏状态同步
  document.addEventListener('fullscreenchange', syncFullscreenState)

  // 3. 拉数据
  await loadSnapshot()

  // 4. 定时刷新（30s）
  dataTimer = window.setInterval(() => {
    loadSnapshot()
  }, 30_000)

  // 5. WS
  connectWs()

  // 6. resize
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
  if (dataTimer) {
    window.clearInterval(dataTimer)
    dataTimer = null
  }
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  window.removeEventListener('resize', handleResize)
  disconnectWs()
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  if (pieChart) {
    pieChart.dispose()
    pieChart = null
  }
})

// 响应数据变化重绘图表
watch(
  () => snapshot.value,
  () => {
    nextTick(renderAllCharts)
  },
  { deep: true }
)
</script>

<style lang="scss" scoped>
// =============================================================================
// W-FRONT-02-E8 大屏模式（全屏沉浸式 / 玻璃科技风）
//
// 布局：
//   - 顶部条（fixed top 0；高度 56）
//   - KPI 4 卡（高度 ~110）
//   - Grid 4 区：trend / pie / alarm / line-grid
//   - 底部跑马灯（fixed bottom 0；高度 36）
//
// 配色：沿用 D-tier tokens --accent / --accent-2 / --success / --warning / --danger
// =============================================================================
.screen-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  padding: 56px 16px 36px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background:
    radial-gradient(at 12% 8%, rgba(92, 225, 255, 0.12), transparent 55%),
    radial-gradient(at 88% 92%, rgba(255, 110, 199, 0.10), transparent 55%),
    linear-gradient(135deg, #0b1426 0%, #1d1d1f 50%, #2a1f3d 100%);
  color: var(--text-primary);
  font-family: var(--font-family);
  isolation: isolate;
}

// 装饰光晕
.screen-page__halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
  z-index: 0;
}
.screen-page__halo--1 {
  top: -10%;
  left: -8%;
  width: 520px;
  height: 520px;
  background: rgba(92, 225, 255, 0.18);
}
.screen-page__halo--2 {
  bottom: -15%;
  right: -10%;
  width: 560px;
  height: 560px;
  background: rgba(255, 110, 199, 0.16);
}
.screen-page__halo--3 {
  top: 40%;
  left: 50%;
  width: 360px;
  height: 360px;
  background: rgba(95, 217, 127, 0.08);
  transform: translateX(-50%);
}

// ---------------------------------------------------------------------------
// 顶部条
// ---------------------------------------------------------------------------
.screen-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
  background: linear-gradient(180deg, rgba(11, 20, 38, 0.78) 0%, rgba(11, 20, 38, 0.55) 100%);
  backdrop-filter: blur(28px) saturate(160%);
  -webkit-backdrop-filter: blur(28px) saturate(160%);
  border-bottom: 1px solid var(--glass-border);
  z-index: 50;

  &__left {
    display: flex;
    align-items: center;
    gap: 18px;
  }
  &__brand {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    background: var(--gradient-brand);
    color: #0b1426;
    font-size: 16px;
    font-weight: var(--font-weight-bold);
    box-shadow: 0 6px 18px rgba(92, 225, 255, 0.35);
  }
  &__title {
    font-size: 18px;
    font-weight: var(--font-weight-bold);
    letter-spacing: 0.6px;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  &__clock {
    font-family: ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
    font-size: 14px;
    color: var(--accent);
    letter-spacing: 1.2px;
    text-shadow: 0 0 12px rgba(92, 225, 255, 0.35);
  }

  &__center {
    display: flex;
    justify-content: center;
  }
  &__ws {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    border-radius: var(--radius-pill);
    font-size: 12px;
    font-weight: var(--font-weight-medium);
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid var(--glass-border);
    color: var(--text-secondary);

    &-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: currentColor;
    }
    &--open {
      color: var(--success);
      border-color: rgba(95, 217, 127, 0.4);
      background: rgba(95, 217, 127, 0.10);
      .screen-header__ws-dot {
        box-shadow: 0 0 8px rgba(95, 217, 127, 0.7);
        animation: ws-pulse 2s ease-in-out infinite;
      }
    }
    &--connecting {
      color: var(--accent);
      border-color: rgba(92, 225, 255, 0.4);
      background: rgba(92, 225, 255, 0.10);
      .screen-header__ws-dot {
        animation: ws-pulse 1s ease-in-out infinite;
      }
    }
    &--closed,
    &--closing {
      color: var(--danger);
      border-color: rgba(255, 90, 95, 0.4);
      background: rgba(255, 90, 95, 0.10);
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
    justify-content: flex-end;
  }
  &__updated {
    font-size: 12px;
    color: var(--text-secondary);
    letter-spacing: 0.4px;
  }
  &__btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: var(--radius-md);
    border: 1px solid var(--glass-border);
    background: rgba(255, 255, 255, 0.04);
    color: var(--text-primary);
    font-size: 12px;
    letter-spacing: 0.4px;
    cursor: pointer;
    transition: all var(--transition-base);

    &:hover {
      background: rgba(255, 255, 255, 0.10);
      border-color: rgba(92, 225, 255, 0.4);
    }
    &:active {
      transform: translateY(1px);
    }
    &--accent {
      background: var(--accent-soft);
      border-color: var(--accent-border);
      color: var(--accent);
    }
    &-icon {
      font-size: 14px;
      line-height: 1;
    }
  }
}

@keyframes ws-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.65; }
}

// ---------------------------------------------------------------------------
// 降级提示条
// ---------------------------------------------------------------------------
.screen-degraded {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  background: rgba(255, 183, 77, 0.12);
  border: 1px solid rgba(255, 183, 77, 0.4);
  color: var(--warning);
  font-size: 12px;
  letter-spacing: 0.4px;

  &__icon {
    font-size: 14px;
  }
}

// ---------------------------------------------------------------------------
// KPI 4 卡
// ---------------------------------------------------------------------------
.screen-kpi-row {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  flex: 0 0 auto;
}
.screen-kpi {
  position: relative;
  height: 110px;
  padding: 14px 18px;
  border-radius: var(--radius-lg);
  background: rgba(15, 22, 36, 0.55);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.06), transparent 60%);
    pointer-events: none;
  }
  &--cyan { box-shadow: 0 0 0 1px rgba(92, 225, 255, 0.18), 0 12px 32px rgba(92, 225, 255, 0.10); }
  &--green { box-shadow: 0 0 0 1px rgba(95, 217, 127, 0.20), 0 12px 32px rgba(95, 217, 127, 0.10); }
  &--red { box-shadow: 0 0 0 1px rgba(255, 90, 95, 0.20), 0 12px 32px rgba(255, 90, 95, 0.10); }
  &--pink { box-shadow: 0 0 0 1px rgba(255, 110, 199, 0.20), 0 12px 32px rgba(255, 110, 199, 0.10); }

  &__inner {
    display: flex;
    flex-direction: column;
    gap: 6px;
    height: 100%;
  }
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__label {
    font-size: 12px;
    color: var(--text-secondary);
    letter-spacing: 0.6px;
    text-transform: uppercase;
  }
  &__icon {
    font-size: 18px;
    filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.18));
  }
  &__value {
    display: flex;
    align-items: baseline;
    gap: 6px;
  }
  &__num {
    font-size: 32px;
    font-weight: var(--font-weight-bold);
    letter-spacing: -0.4px;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }
  &__unit {
    font-size: 12px;
    color: var(--text-secondary);
  }
  &__hint {
    margin-top: auto;
    font-size: 11px;
    color: var(--text-secondary);
    letter-spacing: 0.3px;
  }
}

@media (max-width: 1280px) {
  .screen-kpi-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

// ---------------------------------------------------------------------------
// 4 区 Grid
// ---------------------------------------------------------------------------
.screen-grid {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  grid-template-rows: minmax(0, 1.05fr) minmax(0, 1fr);
  grid-template-areas:
    'trend pie'
    'alarm grid';
  gap: 12px;
}

.screen-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 14px 18px;
  border-radius: var(--radius-lg);
  background: rgba(15, 22, 36, 0.55);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 35%);
    pointer-events: none;
  }

  &--trend { grid-area: trend; }
  &--pie { grid-area: pie; }
  &--alarm { grid-area: alarm; }
  &--grid { grid-area: grid; }

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 8px;
    flex: 0 0 auto;
  }
  &__title-block {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  &__title {
    margin: 0;
    font-size: 14px;
    font-weight: var(--font-weight-bold);
    color: var(--text-primary);
    letter-spacing: 0.4px;
  }
  &__sub {
    margin: 0;
    font-size: 11px;
    color: var(--text-secondary);
    letter-spacing: 0.3px;
  }
  &__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 28px;
    height: 22px;
    padding: 0 8px;
    border-radius: var(--radius-pill);
    background: var(--accent-soft);
    border: 1px solid var(--accent-border);
    color: var(--accent);
    font-size: 11px;
    font-weight: var(--font-weight-semibold);
  }
  &__legend {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 11px;
    color: var(--text-secondary);
  }
  &__legend-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    &::before {
      content: '';
      display: inline-block;
      width: 14px;
      height: 2px;
      border-radius: 1px;
      margin-right: 4px;
    }
    &--plan::before { background: var(--accent); }
    &--actual::before { background: var(--success); }
    &--defect::before { background: var(--danger); }
  }
  &__chart {
    position: relative;
    flex: 1;
    min-height: 0;
  }
}

// ---------------------------------------------------------------------------
// 报警列表
// ---------------------------------------------------------------------------
.screen-alarm-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 6px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  &::-webkit-scrollbar { width: 6px; height: 6px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: var(--radius-pill);
  }
  &::-webkit-scrollbar-track { background: transparent; }

  &__empty {
    padding: 24px 0;
    text-align: center;
    color: var(--text-secondary);
    font-size: 12px;
  }
}

.screen-alarm-item {
  display: grid;
  grid-template-columns: 90px 60px 56px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border-left: 2px solid var(--accent);
  font-size: 12px;
  transition: all var(--transition-base);

  &--level-2 {
    border-left-color: var(--danger);
    background: linear-gradient(90deg, rgba(255, 90, 95, 0.12), rgba(255, 90, 95, 0.04) 60%, transparent);
  }
  &--new {
    animation: row-flash 1.4s ease-out 1;
  }
  &__time {
    font-family: ui-monospace, Menlo, monospace;
    color: var(--text-secondary);
    font-size: 11px;
  }
  &__loc {
    font-family: ui-monospace, Menlo, monospace;
    color: var(--text-primary);
    font-size: 11px;
  }
  &__type {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 18px;
    padding: 0 8px;
    border-radius: var(--radius-pill);
    font-size: 11px;
    font-weight: var(--font-weight-medium);
    border: 1px solid transparent;
    &--1 { background: rgba(255, 110, 199, 0.18); color: #ff9ed7; border-color: rgba(255, 110, 199, 0.32); }
    &--2 { background: rgba(92, 225, 255, 0.18); color: var(--accent); border-color: rgba(92, 225, 255, 0.32); }
    &--3 { background: rgba(255, 183, 77, 0.18); color: var(--warning); border-color: rgba(255, 183, 77, 0.32); }
  }
  &__msg {
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__status {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 18px;
    padding: 0 8px;
    border-radius: var(--radius-pill);
    font-size: 11px;
    font-weight: var(--font-weight-medium);
    border: 1px solid transparent;
    &--1 { background: rgba(95, 217, 127, 0.16); color: var(--success); border-color: rgba(95, 217, 127, 0.32); }
    &--2 { background: rgba(255, 90, 95, 0.20); color: #ff8b8e; border-color: rgba(255, 90, 95, 0.45); }
    &--3 { background: rgba(255, 255, 255, 0.06); color: var(--text-secondary); border-color: rgba(255, 255, 255, 0.18); }
  }
}

@keyframes row-flash {
  0% { background: rgba(92, 225, 255, 0.30); }
  100% { background: rgba(255, 255, 255, 0.04); }
}

// ---------------------------------------------------------------------------
// 线别 Grid
// ---------------------------------------------------------------------------
.screen-line-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  grid-auto-rows: minmax(110px, 1fr);
  gap: 10px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.18) transparent;
  &::-webkit-scrollbar { width: 6px; height: 6px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: var(--radius-pill);
  }
  &::-webkit-scrollbar-track { background: transparent; }

  &__empty {
    grid-column: 1 / -1;
    padding: 28px 0;
    text-align: center;
    color: var(--text-secondary);
    font-size: 12px;
  }
}

.screen-line-card {
  position: relative;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: hidden;

  &--running { border-left: 2px solid var(--success); }
  &--idle { border-left: 2px solid rgba(255, 255, 255, 0.32); }
  &--down { border-left: 2px solid var(--danger); }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }
  &__name {
    font-size: 13px;
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  &__state {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 10px;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.4px;
  }
  &__state-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
  }
  &--running &__state { color: var(--success); }
  &--idle &__state { color: var(--text-secondary); }
  &--down &__state { color: var(--danger); }

  &__loc {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 10px;
    color: var(--text-secondary);
    letter-spacing: 0.4px;
  }

  &__metrics {
    display: flex;
    gap: 14px;
    margin-top: 2px;
  }
  &__metric {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  &__metric-label {
    font-size: 10px;
    color: var(--text-secondary);
    letter-spacing: 0.4px;
  }
  &__metric-value {
    font-size: 16px;
    font-weight: var(--font-weight-bold);
    color: var(--text-primary);
    &--defect { color: var(--danger); }
  }

  &__bar {
    position: relative;
    height: 4px;
    border-radius: var(--radius-pill);
    background: rgba(255, 255, 255, 0.08);
    overflow: hidden;
  }
  &__bar-fill {
    display: block;
    height: 100%;
    background: var(--gradient-brand);
    box-shadow: 0 0 8px rgba(92, 225, 255, 0.45);
    transition: width var(--transition-base);
  }
}

// ---------------------------------------------------------------------------
// 跑马灯
// ---------------------------------------------------------------------------
.screen-ticker {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  background: linear-gradient(0deg, rgba(11, 20, 38, 0.78) 0%, rgba(11, 20, 38, 0.55) 100%);
  backdrop-filter: blur(28px) saturate(160%);
  -webkit-backdrop-filter: blur(28px) saturate(160%);
  border-top: 1px solid var(--glass-border);
  z-index: 50;

  &__label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex: 0 0 auto;
    padding: 4px 12px;
    border-radius: var(--radius-pill);
    background: var(--accent-soft);
    border: 1px solid var(--accent-border);
    color: var(--accent);
    font-size: 12px;
    font-weight: var(--font-weight-semibold);
    letter-spacing: 0.5px;
  }
  &__label-icon {
    font-size: 12px;
  }
  &__viewport {
    position: relative;
    flex: 1;
    min-width: 0;
    height: 100%;
    overflow: hidden;
    display: flex;
    align-items: center;
  }
  &__track {
    display: inline-flex;
    align-items: center;
    gap: 24px;
    white-space: nowrap;
    padding-left: 100%;
    animation: ticker-roll 60s linear infinite;
    will-change: transform;

    .screen-ticker:hover & {
      animation-play-state: paused;
    }
  }
  &__item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--text-primary);
    font-size: 12px;

    &--serious {
      color: var(--danger);
    }
    &--empty {
      color: var(--text-secondary);
      font-style: italic;
    }
  }
  &__time {
    color: var(--accent);
    font-family: ui-monospace, Menlo, monospace;
    font-size: 11px;
  }
  &__loc {
    color: var(--text-secondary);
    font-family: ui-monospace, Menlo, monospace;
    font-size: 11px;
  }
  &__msg {
    color: inherit;
  }
  &__sep {
    color: var(--accent);
    opacity: 0.5;
    margin: 0 8px;
  }

  &--empty &__track {
    animation: none;
  }
}

@keyframes ticker-roll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

// ---------------------------------------------------------------------------
// 响应式
// ---------------------------------------------------------------------------
@media (max-width: 1280px) {
  .screen-grid {
    grid-template-columns: 1fr;
    grid-template-rows: 360px 280px 280px 280px;
    grid-template-areas:
      'trend'
      'pie'
      'alarm'
      'grid';
  }
  .screen-page {
    padding: 56px 10px 36px;
  }
}
</style>
