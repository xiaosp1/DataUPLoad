<template>
  <GlassPage
    :title="$t('defect.title')"
    :subtitle="$t('defect.subtitle')"
  >
    <!-- KPI 区 -->
    <section class="kpi-grid">
      <GlassCard class="kpi-card kpi-card--total">
        <div class="kpi-card__label">{{ $t('defect.kpi.total') }}</div>
        <div class="kpi-card__value">{{ kpi.total }}</div>
        <div class="kpi-card__unit">pcs</div>
      </GlassCard>
      <GlassCard class="kpi-card kpi-card--severe">
        <div class="kpi-card__label">{{ $t('defect.kpi.severe') }}</div>
        <div class="kpi-card__value">{{ kpi.severe }}</div>
        <div class="kpi-card__unit">pcs</div>
      </GlassCard>
      <GlassCard class="kpi-card kpi-card--handled">
        <div class="kpi-card__label">{{ $t('defect.kpi.handled') }}</div>
        <div class="kpi-card__value">{{ kpi.handled }}</div>
        <div class="kpi-card__unit">pcs</div>
      </GlassCard>
      <GlassCard class="kpi-card kpi-card--miss">
        <div class="kpi-card__label">{{ $t('defect.kpi.missRate') }}</div>
        <div class="kpi-card__value">{{ kpi.missRate }}</div>
        <div class="kpi-card__unit">%</div>
      </GlassCard>
    </section>

    <!-- 筛选栏 -->
    <GlassCard class="filter-card">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item :label="$t('defect.filter.date')">
          <el-date-picker
            v-model="filter.dateRange"
            type="daterange"
            range-separator="~"
            value-format="YYYY-MM-DD"
            start-placeholder="Start"
            end-placeholder="End"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item :label="$t('defect.filter.line')">
          <el-select
            v-model="filter.lineNo"
            :placeholder="$t('defect.filter.line')"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="ln in lineOptions"
              :key="ln"
              :label="ln"
              :value="ln"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('defect.filter.type')">
          <el-select
            v-model="filter.type"
            :placeholder="$t('defect.filter.type')"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="t in typeOptions"
              :key="t"
              :label="t"
              :value="t"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('defect.filter.level')">
          <el-select
            v-model="filter.level"
            :placeholder="$t('defect.filter.level')"
            clearable
            style="width: 140px"
          >
            <el-option label="严重" value="severe" />
            <el-option label="一般" value="normal" />
            <el-option label="轻微" value="minor" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <GlassButton variant="primary" @click="onQuery">{{ $t('common.query') }}</GlassButton>
          <GlassButton variant="ghost" style="margin-left: 8px" @click="onReset">{{ $t('common.reset') }}</GlassButton>
        </el-form-item>
      </el-form>
    </GlassCard>

    <!-- 7 日趋势 -->
    <GlassCard class="trend-card">
      <template #header>
        <span>{{ $t('defect.trend.title') }}</span>
      </template>
      <div ref="trendChartRef" class="trend-chart" />
    </GlassCard>

    <!-- 明细表 -->
    <GlassCard class="table-card">
      <template #header>
        <span>{{ $t('defect.table.title') }}</span>
      </template>
      <el-table
        v-loading="loading"
        :data="pagedRows"
        :row-class-name="rowClassName"
        stripe
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          :label="$t('defect.table.id')"
          width="90"
          align="center"
        />
        <el-table-column
          prop="time"
          :label="$t('defect.table.time')"
          width="170"
        />
        <el-table-column
          prop="lineNo"
          :label="$t('defect.table.line')"
          width="90"
          align="center"
        />
        <el-table-column
          prop="type"
          :label="$t('defect.table.type')"
          min-width="160"
        />
        <el-table-column
          :label="$t('defect.table.level')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="severityTagType(row)" disable-transitions size="small">
              {{ severityLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('defect.table.image')"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <div class="thumb" @click="openDetail(row)">
              <span class="thumb__placeholder">🖼️</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('defect.table.status')"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <el-tag v-if="isHandled(row.id)" type="success" size="small">
              {{ $t('defect.status.handled') }}
            </el-tag>
            <el-tag v-else type="warning" size="small">
              {{ $t('defect.status.pending') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('defect.table.handler')"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <span>{{ isHandled(row.id) ? getRemark(row.id).handler : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('defect.table.action')"
          width="180"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <GlassButton size="small" variant="ghost" @click="openDetail(row)">
              {{ $t('defect.action.detail') }}
            </GlassButton>
            <GlassButton
              size="small"
              variant="primary"
              style="margin-left: 6px"
              :disabled="isHandled(row.id)"
              @click="openDetail(row, true)"
            >
              {{ $t('defect.action.handle') }}
            </GlassButton>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </GlassCard>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="$t('defect.detail.title')"
      width="640px"
      destroy-on-close
      @close="onDetailClose"
    >
      <div v-if="detailRow" class="detail">
        <div class="detail__image">
          <div class="detail__image-placeholder">
            <span>🖼️</span>
            <small>{{ detailRow.type }}</small>
          </div>
        </div>
        <el-descriptions :column="2" border size="small" class="detail__meta">
          <el-descriptions-item :label="$t('defect.table.id')">
            {{ detailRow.id }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.table.line')">
            {{ detailRow.lineNo }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.table.type')" :span="2">
            {{ detailRow.type }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.table.level')">
            <el-tag :type="severityTagType(detailRow)" size="small">
              {{ severityLabel(detailRow) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.table.status')">
            <el-tag v-if="isHandled(detailRow.id)" type="success" size="small">
              {{ $t('defect.status.handled') }}
            </el-tag>
            <el-tag v-else type="warning" size="small">
              {{ $t('defect.status.pending') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.detail.detectTime')">
            {{ detailRow.time }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.detail.handleTime')">
            {{ handleTimeOf(detailRow.id) || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('defect.table.handler')" :span="2">
            {{ isHandled(detailRow.id) ? getRemark(detailRow.id).handler : '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form class="detail__form">
          <el-form-item :label="$t('defect.detail.remark')">
            <el-input
              v-model="detailRemark"
              type="textarea"
              :rows="4"
              :placeholder="$t('defect.detail.remarkPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('defect.detail.handler')">
            <el-input
              v-model="detailHandler"
              :placeholder="$t('defect.detail.handlerPlaceholder')"
              style="max-width: 240px"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <GlassButton variant="ghost" @click="detailVisible = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton variant="primary" @click="onSaveDetail">
          {{ $t('defect.detail.save') }}
        </GlassButton>
      </template>
    </el-dialog>
  </GlassPage>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassButton from '../components/GlassButton.vue'

import {
  listDefectDay,
  getDefectDetail,
  handleDefect,
  listLineDefectType,
  getDefectTrend,
  type DefectDayRecord,
  type LineDefectType,
} from '@/api/defect'

const { t: _t } = useI18n()

// -----------------------------------------------------------------------------
// 状态
// -----------------------------------------------------------------------------

const loading = ref(false)
const rows = ref<DefectDayRecord[]>([])
const typeDict = ref<LineDefectType[]>([])

// 本地维护的处理状态（后端无 handle 接口）
interface LocalHandle {
  handled: boolean
  remark: string
  handler: string
  handleTime: string
}
const localMap = reactive(new Map<number, LocalHandle>())

const filter = reactive({
  dateRange: [] as string[],
  lineNo: '' as string,
  type: '' as string,
  level: '' as string,
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
})

// 详情弹窗
const detailVisible = ref(false)
const detailRow = ref<DefectDayRecord | null>(null)
const detailRemark = ref('')
const detailHandler = ref('')

// 趋势图
const trendChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
const trendFrom = ref('')
const trendTo = ref('')
const trendBuckets = ref<{ date: string; count: number }[]>([])

// -----------------------------------------------------------------------------
// 工具
// -----------------------------------------------------------------------------

function todayYmd(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function dateOnly(s: string): string {
  return (s || '').slice(0, 10)
}

function daysAgo(n: number): string {
  const d = new Date(Date.now() - n * 24 * 60 * 60 * 1000)
  const pad = (v: number) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function severityOf(row: DefectDayRecord): 'severe' | 'normal' | 'minor' {
  const t = row.type || ''
  if (t.startsWith('严重') || t.includes('严重')) return 'severe'
  if (t.startsWith('轻微') || t.includes('轻微')) return 'minor'
  return 'normal'
}

function severityLabel(row: DefectDayRecord): string {
  const s = severityOf(row)
  if (s === 'severe') return '严重'
  if (s === 'minor') return '轻微'
  return '一般'
}

function severityTagType(row: DefectDayRecord): 'danger' | 'warning' | 'info' {
  const s = severityOf(row)
  if (s === 'severe') return 'danger'
  if (s === 'minor') return 'info'
  return 'warning'
}

function rowClassName({ row }: { row: DefectDayRecord }): string {
  const s = severityOf(row)
  if (s === 'severe') return 'defect-row--severe'
  if (s === 'minor') return 'defect-row--minor'
  return 'defect-row--normal'
}

function isHandled(id: number): boolean {
  const e = localMap.get(id)
  return !!(e && e.handled)
}

function getRemark(id: number): LocalHandle {
  return (
    localMap.get(id) || { handled: false, remark: '', handler: '', handleTime: '' }
  )
}

function handleTimeOf(id: number): string {
  const e = localMap.get(id)
  return e?.handleTime || ''
}

// -----------------------------------------------------------------------------
// 计算属性
// -----------------------------------------------------------------------------

const lineOptions = computed(() => {
  const set = new Set<string>()
  for (const r of rows.value) if (r.lineNo) set.add(r.lineNo)
  return Array.from(set).sort()
})

const typeOptions = computed(() => {
  const set = new Set<string>()
  for (const r of rows.value) if (r.type) set.add(r.type)
  for (const d of typeDict.value) if (d.name) set.add(d.name)
  return Array.from(set).sort()
})

const filteredRows = computed(() => {
  return rows.value.filter((r) => {
    if (filter.lineNo && r.lineNo !== filter.lineNo) return false
    if (filter.type && r.type !== filter.type) return false
    if (filter.level) {
      const s = severityOf(r)
      if (filter.level === 'severe' && s !== 'severe') return false
      if (filter.level === 'normal' && s !== 'normal') return false
      if (filter.level === 'minor' && s !== 'minor') return false
    }
    return true
  })
})

const pagedRows = computed(() => {
  const start = (pagination.pageNum - 1) * pagination.pageSize
  return filteredRows.value.slice(start, start + pagination.pageSize)
})

const kpi = computed(() => {
  const list = filteredRows.value
  const total = list.reduce((s, r) => s + (r.count || 0), 0)
  const severe = list
    .filter((r) => severityOf(r) === 'severe')
    .reduce((s, r) => s + (r.count || 0), 0)
  let handledCount = 0
  for (const r of list) if (isHandled(r.id)) handledCount += r.count || 0
  const missRate =
    total > 0
      ? Number(((severe / total) * 100).toFixed(2))
      : 0
  return { total, severe, handled: handledCount, missRate }
})

// -----------------------------------------------------------------------------
// 数据加载
// -----------------------------------------------------------------------------

async function loadList() {
  loading.value = true
  try {
    const date =
      filter.dateRange && filter.dateRange.length === 2
        ? filter.dateRange[0]
        : todayYmd()
    const resp = await listDefectDay({ date, pageNum: 1, pageSize: 9999 })
    if (resp?.data?.success && Array.isArray(resp.data.data)) {
      rows.value = resp.data.data
    } else {
      rows.value = []
    }
  } catch (e) {
    console.error('[Defect] loadList error:', e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function loadTypeDict() {
  try {
    const resp = await listLineDefectType()
    if (resp?.data?.success && Array.isArray(resp.data.data)) {
      typeDict.value = resp.data.data
    }
  } catch (e) {
    console.warn('[Defect] loadTypeDict error:', e)
  }
}

async function loadTrend() {
  const to = todayYmd()
  const from = daysAgo(6)
  trendFrom.value = from
  trendTo.value = to
  try {
    const resp = await getDefectTrend(from, to)
    const list: DefectDayRecord[] =
      resp?.data?.success && Array.isArray(resp.data.data) ? resp.data.data : []

    const map = new Map<string, number>()
    for (let i = 0; i < 7; i++) {
      const d = daysAgo(6 - i)
      map.set(d, 0)
    }
    for (const r of list) {
      const d = dateOnly(r.time)
      if (map.has(d)) {
        map.set(d, (map.get(d) || 0) + (r.count || 0))
      }
    }
    trendBuckets.value = Array.from(map.entries()).map(([date, count]) => ({
      date,
      count,
    }))
  } catch (e) {
    console.warn('[Defect] loadTrend error:', e)
    trendBuckets.value = []
  }
  await nextTick()
  renderTrend()
}

function renderTrend() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const xs = trendBuckets.value.map((b) => b.date.slice(5))
  const ys = trendBuckets.value.map((b) => b.count)
  trendChart.setOption(
    {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: xs,
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.3)' } },
        axisLabel: { color: 'rgba(255,255,255,0.7)' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
        axisLabel: { color: 'rgba(255,255,255,0.7)' },
      },
      series: [
        {
          type: 'bar',
          data: ys,
          barWidth: '50%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#5b9bff' },
              { offset: 1, color: 'rgba(91, 155, 255, 0.2)' },
            ]),
          },
        },
      ],
    },
    true
  )
}

// -----------------------------------------------------------------------------
// 详情 / 操作
// -----------------------------------------------------------------------------

function openDetail(row: DefectDayRecord, forceEdit = false) {
  detailRow.value = row
  const e = getRemark(row.id)
  detailRemark.value = e.remark || ''
  detailHandler.value = e.handler || ''
  detailVisible.value = true
  if (forceEdit && isHandled(row.id)) {
    // 已处理也允许查看
  }
  // 同步拉一次详情（按 id 过滤区间结果），保证数据新鲜
  getDefectDetail(row.id).catch(() => undefined)
}

function onSaveDetail() {
  if (!detailRow.value) return
  const id = detailRow.value.id
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const handleTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(
    now.getDate()
  )} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  const handler = detailHandler.value.trim() || 'operator'
  localMap.set(id, {
    handled: true,
    remark: detailRemark.value.trim(),
    handler,
    handleTime,
  })
  // 乐观调用后端（虽然无实现，但不阻塞）
  handleDefect(id, detailRemark.value).catch(() => undefined)
  ElMessage.success('已保存')
  detailVisible.value = false
}

function onDetailClose() {
  detailRow.value = null
  detailRemark.value = ''
  detailHandler.value = ''
}

// -----------------------------------------------------------------------------
// 筛选
// -----------------------------------------------------------------------------

function onQuery() {
  pagination.pageNum = 1
  loadList()
}

function onReset() {
  filter.dateRange = []
  filter.lineNo = ''
  filter.type = ''
  filter.level = ''
  pagination.pageNum = 1
  loadList()
}

// -----------------------------------------------------------------------------
// 生命周期
// -----------------------------------------------------------------------------

function handleResize() {
  if (trendChart) trendChart.resize()
}

onMounted(() => {
  loadList()
  loadTypeDict()
  loadTrend()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style lang="scss" scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 16px);
  margin-bottom: var(--space-4, 16px);

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  padding: var(--space-4, 16px);
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  overflow: hidden;

  &__label {
    font-size: var(--font-size-sm, 13px);
    color: var(--text-secondary);
    opacity: 0.85;
  }

  &__value {
    font-size: 32px;
    font-weight: 600;
    color: var(--text-primary);
    line-height: 1.1;
    font-variant-numeric: tabular-nums;
  }

  &__unit {
    font-size: var(--font-size-xs, 12px);
    color: var(--text-secondary);
    opacity: 0.7;
  }

  &--total {
    background: linear-gradient(135deg, rgba(91, 155, 255, 0.18), rgba(91, 155, 255, 0.04));
  }
  &--severe {
    background: linear-gradient(135deg, rgba(255, 86, 86, 0.18), rgba(255, 86, 86, 0.04));
  }
  &--handled {
    background: linear-gradient(135deg, rgba(72, 199, 142, 0.18), rgba(72, 199, 142, 0.04));
  }
  &--miss {
    background: linear-gradient(135deg, rgba(255, 184, 77, 0.18), rgba(255, 184, 77, 0.04));
  }
}

.filter-card {
  margin-bottom: var(--space-4, 16px);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2, 8px);
}

.trend-card {
  margin-bottom: var(--space-4, 16px);
}

.trend-chart {
  width: 100%;
  height: 260px;
}

.table-card {
  margin-bottom: var(--space-6, 24px);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4, 16px);
}

.thumb {
  width: 56px;
  height: 40px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: transform 0.15s ease;

  &:hover {
    transform: scale(1.05);
    border-color: rgba(91, 155, 255, 0.6);
  }

  &__placeholder {
    font-size: 20px;
    opacity: 0.7;
  }
}

.detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 16px);

  &__image {
    display: flex;
    justify-content: center;
  }

  &__image-placeholder {
    width: 100%;
    max-width: 360px;
    height: 200px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px dashed rgba(255, 255, 255, 0.2);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--text-secondary);

    span {
      font-size: 56px;
    }
  }

  &__meta {
    width: 100%;
  }

  &__form {
    width: 100%;
  }
}

:deep(.defect-row--severe) td {
  background: rgba(255, 86, 86, 0.06) !important;
}

:deep(.defect-row--normal) td {
  background: rgba(255, 184, 77, 0.05) !important;
}

:deep(.defect-row--minor) td {
  background: rgba(91, 155, 255, 0.04) !important;
}
</style>
