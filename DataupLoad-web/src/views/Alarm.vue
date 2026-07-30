<template>
  <GlassPage :title="$t('alarm.title')" :subtitle="$t('alarm.subtitle')">
    <!-- 右上角 actions slot：实时连接指示器 + 刷新按钮 -->
    <template #actions>
      <div class="alarm-header-actions">
        <span class="ws-indicator" :class="`ws-indicator--${wsState}`" :title="wsStateLabel">
          <span class="ws-indicator__dot" />
          <span class="ws-indicator__text">{{ wsStateLabel }}</span>
        </span>
        <GlassButton variant="default" size="small" @click="reload">
          {{ $t('alarm.list.refresh') }}
        </GlassButton>
      </div>
    </template>

    <!-- W-DEFECT-CFG 子单 C：报警记录查询 + 缺陷配置 二级 tab -->
    <GlassCard :padding="0">
      <el-tabs v-model="activeTab" class="alarm-tabs">
        <!-- ============================================================ -->
        <!-- Tab 1: 报警记录查询（沿用原 Alarm.vue 全部业务逻辑） -->
        <!-- ============================================================ -->
        <el-tab-pane :label="$t('alarm.tab.records')" name="records">
          <div class="alarm-tab-pane">

    <!-- 顶部筛选栏 -->
    <GlassCard>
      <div class="alarm-filter">
        <div class="alarm-filter__row">
          <!-- 时间范围 -->
          <div class="alarm-filter__field">
            <label class="alarm-filter__label">{{ $t('alarm.filter.timeRange') }}</label>
            <el-select
              v-model="filter.timeRangeKey"
              class="alarm-filter__control"
              @change="onTimeRangeChange"
            >
              <el-option :label="$t('alarm.filter.range1h')" value="1h" />
              <el-option :label="$t('alarm.filter.range24h')" value="24h" />
              <el-option :label="$t('alarm.filter.range7d')" value="7d" />
              <el-option :label="$t('alarm.filter.rangeCustom')" value="custom" />
            </el-select>
          </div>

          <!-- 自定义时段（仅 range=custom 展开） -->
          <div v-if="filter.timeRangeKey === 'custom'" class="alarm-filter__field">
            <label class="alarm-filter__label">{{ $t('alarm.filter.customRange') }}</label>
            <el-date-picker
              v-model="filter.customRange"
              type="datetimerange"
              range-separator="→"
              class="alarm-filter__control alarm-filter__control--wide"
              value-format="YYYY-MM-DD HH:mm:ss"
              :start-placeholder="$t('alarm.filter.range1h')"
              :end-placeholder="$t('alarm.filter.range24h')"
            />
          </div>

          <!-- 线别（PSM 同款级联） -->
          <div class="alarm-filter__field">
            <label class="alarm-filter__label">{{ $t('alarm.filter.line') }}</label>
            <el-cascader
              v-model="filter.lineIds"
              :options="lineOptions"
              :props="cascaderProps"
              :placeholder="$t('alarm.filter.allLine')"
              clearable
              class="alarm-filter__control alarm-filter__control--wide"
            />
          </div>

          <!-- 报警类型 -->
          <div class="alarm-filter__field">
            <label class="alarm-filter__label">{{ $t('alarm.filter.type') }}</label>
            <el-select
              v-model="filter.type"
              :placeholder="$t('alarm.filter.allType')"
              clearable
              class="alarm-filter__control"
            >
              <el-option :label="$t('alarm.typeOption.defect')" :value="1" />
              <el-option :label="$t('alarm.typeOption.system')" :value="2" />
              <el-option :label="$t('alarm.typeOption.device')" :value="3" />
            </el-select>
          </div>

          <!-- 状态 -->
          <div class="alarm-filter__field">
            <label class="alarm-filter__label">{{ $t('alarm.filter.status') }}</label>
            <el-select
              v-model="filter.solve"
              :placeholder="$t('alarm.filter.allStatus')"
              clearable
              class="alarm-filter__control"
            >
              <el-option :label="$t('alarm.status.pending')" :value="2" />
              <el-option :label="$t('alarm.status.handled')" :value="1" />
              <el-option :label="$t('alarm.status.ignored')" :value="3" />
            </el-select>
          </div>

          <!-- 操作按钮 -->
          <div class="alarm-filter__field alarm-filter__field--actions">
            <GlassButton variant="default" @click="onReset">
              {{ $t('alarm.filter.reset') }}
            </GlassButton>
            <GlassButton variant="primary" @click="onQuery">
              {{ $t('alarm.filter.query') }}
            </GlassButton>
          </div>
        </div>
      </div>
    </GlassCard>

    <!-- 报警表格 -->
    <GlassTable
      :data="rows"
      v-loading="loading"
      element-loading-background="rgba(0,0,0,0.35)"
      :row-class-name="rowClassName"
      :default-sort="{ prop: 'time', order: 'descending' }"
      @sort-change="onSortChange"
    >
      <el-table-column type="index" :label="$t('alarm.table.index')" width="64" align="center" />
      <el-table-column
        prop="time"
        :label="$t('alarm.table.triggerTime')"
        width="180"
        sortable
        show-overflow-tooltip
      />
      <el-table-column prop="lineNo" :label="$t('alarm.table.line')" width="110" show-overflow-tooltip />
      <el-table-column prop="faceNo" :label="$t('alarm.table.face')" width="90" show-overflow-tooltip />
      <el-table-column :label="$t('alarm.table.type')" width="110" align="center">
        <template #default="{ row }">
          <span class="type-pill" :class="`type-pill--${row.type}`">
            {{ typeLabel(row.type) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('alarm.table.level')" width="100" align="center">
        <template #default="{ row }">
          <span class="level-pill" :class="`level-pill--${row.level}`">
            {{ levelLabel(row.level) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="message" :label="$t('alarm.table.desc')" min-width="240" show-overflow-tooltip />
      <el-table-column :label="$t('alarm.table.status')" width="120" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="`status-pill--${row.solve}`">
            {{ statusLabel(row.solve) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('alarm.table.action')" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <GlassButton variant="default" size="small" @click="openDetail(row)">
            {{ $t('alarm.table.detail') }}
          </GlassButton>
          <GlassButton
            v-if="row.solve === 2"
            variant="danger"
            size="small"
            class="action-ignore"
            :loading="row._ignoring"
            @click="onIgnore(row)"
          >
            {{ $t('alarm.table.ignore') }}
          </GlassButton>
        </template>
      </el-table-column>
      <template #empty>
        <div class="alarm-empty">
          <div class="alarm-empty__icon">⌖</div>
          <div class="alarm-empty__text">{{ $t('alarm.list.empty') }}</div>
        </div>
      </template>
    </GlassTable>

    <!-- 分页 -->
    <div class="alarm-pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="onPageSizeChange"
        @current-change="onPageChange"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="$t('alarm.detail.title')"
      width="560px"
      class="alarm-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="detail" class="alarm-detail">
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.id') }}</span>
          <span class="alarm-detail__value">{{ detail.id }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.uuid') }}</span>
          <span class="alarm-detail__value alarm-detail__value--mono">{{ detail.uuid }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.triggerTime') }}</span>
          <span class="alarm-detail__value">{{ detail.time }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.duration') }}</span>
          <span class="alarm-detail__value">{{ formatDuration(durationMs) }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.line') }}</span>
          <span class="alarm-detail__value">{{ detail.lineNo || '—' }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.face') }}</span>
          <span class="alarm-detail__value">{{ detail.faceNo || '—' }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.type') }}</span>
          <span class="alarm-detail__value">{{ typeLabel(detail.type) }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.level') }}</span>
          <span class="alarm-detail__value">{{ levelLabel(detail.level) }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.defect') }}</span>
          <span class="alarm-detail__value">{{ detail.defectName || '—' }}</span>
        </div>
        <div class="alarm-detail__row">
          <span class="alarm-detail__label">{{ $t('alarm.detail.desc') }}</span>
          <span class="alarm-detail__value">{{ detail.message || '—' }}</span>
        </div>
        <div class="alarm-detail__row alarm-detail__row--image">
          <span class="alarm-detail__label">{{ $t('alarm.detail.image') }}</span>
          <div class="alarm-detail__image">
            <span class="alarm-detail__image-placeholder">{{ $t('alarm.detail.noImage') }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <GlassButton variant="default" @click="detailVisible = false">
          {{ $t('alarm.detail.close') }}
        </GlassButton>
        <GlassButton
          v-if="detail && detail.solve === 2"
          variant="danger"
          :loading="detail._ignoring"
          @click="onIgnore(detail, true)"
        >
          {{ $t('alarm.detail.ignore') }}
        </GlassButton>
        <GlassButton
          v-if="detail && detail.solve === 2"
          variant="primary"
          :disabled="true"
          @click="onHandle"
        >
          {{ $t('alarm.detail.handle') }}
        </GlassButton>
      </template>
    </el-dialog>

          </div>
        </el-tab-pane>

        <!-- ============================================================ -->
        <!-- Tab 2: 缺陷配置（W-DEFECT-CFG 子单 C） -->
        <!-- ============================================================ -->
        <el-tab-pane :label="$t('alarm.tab.defectConfig')" name="defectConfig">
          <div class="alarm-tab-pane">
            <DefectConfig />
          </div>
        </el-tab-pane>
      </el-tabs>
    </GlassCard>
  </GlassPage>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-E2 报警管理业务实现
//   - 报警列表（filter + paginate）
//   - WebSocket 实时推送（PSM 路径 /ws?uid=&type=alarm，前端零节流）
//   - 详情弹窗
//   - 忽略（PUT /web/alarm/ignore + lineNo/faceNo/defectName/time 定位）
//   - 错误兜底：401（interceptor 跳 /login）/ 网络错（el-message）/ 数据空（empty 槽）
// =============================================================================

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassButton from '../components/GlassButton.vue'
import GlassTable from '../components/GlassTable.vue'
import DefectConfig from './DefectConfig.vue'
import {
  listAlarm,
  ignoreAlarm,
  listLineTree,
  type AlarmRecord,
  type LineTreeNode,
  type ListAlarmParams
} from '../api/alarm'
import { useUserStore } from '../stores/user'
import { usePermissionStore } from '../stores/permission'
import { createWs, type WsController, type WsState } from '../utils/ws'

// ---------------------------------------------------------------------------
// i18n 兼容：使用全局 $t（在 <template> 里直接用，script 里用 vue-i18n 的 useI18n）
// ---------------------------------------------------------------------------
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

// ---------------------------------------------------------------------------
// W-DEFECT-CFG 子单 C：子 tab 切换状态（报警记录 / 缺陷配置）
// ---------------------------------------------------------------------------
const activeTab = ref<'records' | 'defectConfig'>('records')

// ---------------------------------------------------------------------------
// 用户态（用于 ws uid）+ 权限 store 同步（补救 D-tier 未做的 user.fetchCurrent → perm.setRoles 链路）
// ---------------------------------------------------------------------------
const userStore = useUserStore()
const permissionStore = usePermissionStore()

async function syncPermissionFromCurrentUser() {
  // 拿到当前登录用户的 role（已由 user.fetchCurrent 在 main 链路上调过，
  // 但 D-tier 没把 role 同步到 permission store → router has('alarm') 永远 false）
  if (!userStore.role) {
    try {
      await userStore.fetchCurrent()
    } catch (e) {
      console.warn('[alarm] fetchCurrent failed', e)
    }
  }
  if (userStore.role && !permissionStore.roles.includes(userStore.role)) {
    permissionStore.setRoles([userStore.role])
  }
  // 同步 codes（后端返回的细粒度权限码 + 当前页需要的 meta.permission）
  if (Array.isArray((userStore as any).permission) && (userStore as any).permission.length) {
    const codes = (userStore as any).permission as string[]
    for (const c of codes) {
      if (!permissionStore.codes.includes(c)) permissionStore.codes.push(c)
    }
  }
  // 兜底：super_admin 把全部 meta.permission 加上
  if (permissionStore.isSuperAdmin) {
    const all = ['realtime', 'alarm', 'defect', 'account', 'systemConfig', 'log', 'userManage', 'screen']
    for (const c of all) {
      if (!permissionStore.codes.includes(c)) permissionStore.codes.push(c)
    }
  }
}

// ---------------------------------------------------------------------------
// 筛选 + 分页状态
// ---------------------------------------------------------------------------
type TimeRangeKey = '1h' | '24h' | '7d' | 'custom'

const filter = reactive({
  // W-PERF-C: 默认 1h（~500 行）而非 7d（~80000 行），缩短首屏等待
  timeRangeKey: '1h' as TimeRangeKey,
  customRange: ['', ''] as [string, string],
  lineIds: [] as number[], // cascader 选中的 id 路径
  type: null as number | null,
  solve: null as number | null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// ---------------------------------------------------------------------------
// 线别级联（PSM line-tree）
// ---------------------------------------------------------------------------
const lineOptions = ref<LineTreeNode[]>([])
const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'childs',
  checkStrictly: false,
  emitPath: false
}

async function loadLineTree() {
  try {
    const resp = await listLineTree()
    if (resp && resp.success !== false && Array.isArray(resp.data)) {
      lineOptions.value = resp.data
    }
  } catch (err) {
    console.warn('[alarm] loadLineTree failed', err)
    ElMessage.warning(t('alarm.lineTree.loadFailed'))
  }
}

// ---------------------------------------------------------------------------
// 时间范围 -> startTime/endTime（PSM 端 time 是字符串 yyyy-MM-dd HH:mm:ss）
// ---------------------------------------------------------------------------
function pad2(n: number) {
  return n < 10 ? `0${n}` : `${n}`
}

function fmtDateTime(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(
    d.getMinutes()
  )}:${pad2(d.getSeconds())}`
}

function computeTimeRange(): { startTime: string; endTime: string } {
  const now = new Date()
  const end = fmtDateTime(now)
  if (filter.timeRangeKey === 'custom') {
    return {
      startTime: filter.customRange?.[0] || '',
      endTime: filter.customRange?.[1] || ''
    }
  }
  const from = new Date(now)
  if (filter.timeRangeKey === '1h') from.setHours(from.getHours() - 1)
  else if (filter.timeRangeKey === '24h') from.setHours(from.getHours() - 24)
  else if (filter.timeRangeKey === '7d') from.setDate(from.getDate() - 7)
  return { startTime: fmtDateTime(from), endTime: end }
}

// ---------------------------------------------------------------------------
// 表格 + 加载
// ---------------------------------------------------------------------------
const rows = ref<Array<AlarmRecord & { _ignoring?: boolean; _new?: boolean }>>([])
const loading = ref(false)

function buildQueryParams(): ListAlarmParams {
  const { startTime, endTime } = computeTimeRange()
  // cascader value 是 faceId（leaf 的 id），与 PSM 同款
  const faceId = Array.isArray(filter.lineIds) && filter.lineIds.length
    ? Number(filter.lineIds[filter.lineIds.length - 1])
    : null
  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    type: filter.type,
    solve: filter.solve,
    faceId,
    startTime,
    endTime,
    sortType: 1
  }
}

async function fetchList() {
  loading.value = true
  try {
    const resp = await listAlarm(buildQueryParams())
    if (resp && resp.success !== false && resp.data) {
      const pr = resp.data as any
      rows.value = (pr.records || []) as any
      pagination.total = Number(pr.total || 0)
    } else {
      // 后端返回 code != 0：清空 + 提示
      rows.value = []
      pagination.total = 0
      const msg = (resp && (resp.msg || resp.message)) || t('alarm.list.loadFailed')
      ElMessage.warning(String(msg))
    }
  } catch (err: any) {
    // 网络错 / 401 由 interceptor 处理跳登录页；这里只兜底显示
    console.warn('[alarm] fetchList failed', err)
    rows.value = []
    pagination.total = 0
    if (err?.response?.status !== 401) {
      ElMessage.error(t('alarm.list.loadFailed'))
    }
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.pageNum = 1
  fetchList()
}

function onQuery() {
  pagination.pageNum = 1
  fetchList()
}

function onReset() {
  // W-PERF-C: 重置也保持 1h 默认（首屏快）
  filter.timeRangeKey = '1h'
  filter.customRange = ['', '']
  filter.lineIds = []
  filter.type = null
  filter.solve = null
  pagination.pageNum = 1
  fetchList()
}

function onTimeRangeChange() {
  if (filter.timeRangeKey !== 'custom') {
    filter.customRange = ['', '']
  }
}

function onPageSizeChange(sz: number) {
  pagination.pageSize = sz
  pagination.pageNum = 1
  fetchList()
}

function onPageChange(p: number) {
  pagination.pageNum = p
  fetchList()
}

function rowClassName({ row }: { row: AlarmRecord & { _new?: boolean } }) {
  const classes: string[] = []
  if (row.solve === 2) classes.push('alarm-row--pending')
  if (row._new) classes.push('alarm-row--new')
  return classes.join(' ')
}

function onSortChange(_sort: any) {
  // 暂只支持 time 升降序，本工单走 sortType=1 降序默认；保留接口以备扩展
  // 如需切换可在此把 sort.order 转成 0/1 加到 buildQueryParams()
}

// ---------------------------------------------------------------------------
// 类型 / 等级 / 状态 标签
// ---------------------------------------------------------------------------
function typeLabel(t: number) {
  if (t === 1) return t$('alarm.typeOption.defect')
  if (t === 2) return t$('alarm.typeOption.system')
  if (t === 3) return t$('alarm.typeOption.device')
  return '—'
}
function levelLabel(l: number) {
  if (l === 1) return t$('alarm.levelOption.normal')
  if (l === 2) return t$('alarm.levelOption.serious')
  return '—'
}
function statusLabel(s: number) {
  if (s === 1) return t$('alarm.status.handled')
  if (s === 2) return t$('alarm.status.pending')
  if (s === 3) return t$('alarm.status.ignored')
  return '—'
}

// 避免在 setup 顶部声明 $t 时机问题（在 script setup 里 $t 是全局属性）
function t$(key: string) {
  return (t as any)(key) as string
}

// ---------------------------------------------------------------------------
// 详情弹窗
// ---------------------------------------------------------------------------
const detailVisible = ref(false)
const detail = ref<(AlarmRecord & { _ignoring?: boolean }) | null>(null)
const durationMs = ref(0)
const durationTimer = ref<number | null>(null)

function openDetail(row: AlarmRecord) {
  detail.value = { ...row }
  detailVisible.value = true
  tickDuration()
}

function tickDuration() {
  if (durationTimer.value) {
    window.clearInterval(durationTimer.value)
    durationTimer.value = null
  }
  if (!detail.value) return
  // 用 alarm.time 作为开始时间
  const start = parseAlarmTime(detail.value.time)
  durationMs.value = Math.max(0, Date.now() - start.getTime())
  durationTimer.value = window.setInterval(() => {
    if (!detail.value) return
    durationMs.value = Math.max(0, Date.now() - parseAlarmTime(detail.value.time).getTime())
  }, 1000)
}

function parseAlarmTime(s: string): Date {
  // "yyyy-MM-dd HH:mm:ss"
  if (!s) return new Date()
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/)
  if (m) {
    return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), Number(m[6]))
  }
  return new Date(s)
}

function formatDuration(ms: number): string {
  if (ms < 1000) return '0s'
  const total = Math.floor(ms / 1000)
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const mins = Math.floor((total % 3600) / 60)
  const secs = total % 60
  const parts: string[] = []
  if (days > 0) parts.push(`${days}d`)
  if (hours > 0) parts.push(`${hours}h`)
  if (mins > 0) parts.push(`${mins}m`)
  if (secs > 0 || parts.length === 0) parts.push(`${secs}s`)
  return parts.join(' ')
}

// ---------------------------------------------------------------------------
// 忽略报警
// ---------------------------------------------------------------------------
async function onIgnore(row: AlarmRecord & { _ignoring?: boolean }, fromDialog = false) {
  if (!row) return
  try {
    await ElMessageBox.confirm(t$('alarm.list.ignoreConfirm'), t$('alarm.detail.title'), {
      type: 'warning',
      confirmButtonText: t$('common.confirm') || t$('alarm.table.ignore'),
      cancelButtonText: t$('common.cancel'),
      customClass: 'alarm-confirm-box'
    })
  } catch {
    return
  }
  row._ignoring = true
  if (fromDialog && detail.value) detail.value._ignoring = true
  try {
    const resp = await ignoreAlarm({
      type: row.type ?? undefined,
      defectName: row.defectName ?? '',
      lineNo: row.lineNo ?? '',
      faceNo: row.faceNo ?? '',
      faceId: row.faceNo ?? '',
      startTime: row.time ?? '',
      endTime: row.time ?? '',
      ignoreTime: fmtDateTime(new Date())
    })
    const ok = resp && (resp.success === true || (resp as any).code === 0)
    if (ok) {
      ElMessage.success(t$('alarm.list.ignoreSuccess'))
      // 本地乐观更新
      const idx = rows.value.findIndex((r) => r.uuid === row.uuid || r.id === row.id)
      if (idx >= 0) rows.value[idx].solve = 3
      if (detail.value && (detail.value.uuid === row.uuid || detail.value.id === row.id)) {
        detail.value.solve = 3
      }
      if (fromDialog) detailVisible.value = false
    } else {
      const msg = (resp && (resp.msg || resp.message)) || t$('alarm.list.ignoreFailed')
      ElMessage.error(String(msg))
    }
  } catch (err: any) {
    console.warn('[alarm] ignore failed', err)
    if (err?.response?.status !== 401) {
      ElMessage.error(t$('alarm.list.ignoreFailed'))
    }
  } finally {
    row._ignoring = false
    if (fromDialog && detail.value) detail.value._ignoring = false
  }
}

// 处理（PSM 端 POST /client/data/deal-alarm；本工单先展示按钮占位，后端链路已就绪）
function onHandle() {
  ElMessage.info('Handle flow not yet wired to /client/data/deal-alarm in E2')
}

// ---------------------------------------------------------------------------
// WebSocket 实时连接（/ws?uid=&type=alarm）
// ---------------------------------------------------------------------------
const wsState = ref<WsState>('idle')
let wsCtrl: WsController | null = null

const wsStateLabel = computed(() => {
  switch (wsState.value) {
    case 'open':
      return t$('alarm.ws.connected')
    case 'connecting':
      return t$('alarm.ws.connecting')
    case 'closed':
    case 'closing':
      return t$('alarm.ws.disconnected')
    default:
      return t$('alarm.ws.disconnected')
  }
})

function pushIncomingAlarm(alarm: AlarmRecord) {
  if (!alarm) return
  // 服务端零节流（ADR-0011）：直接头插
  const enriched: AlarmRecord & { _new?: boolean } = { ...alarm, _new: true }
  // 去重（按 uuid 或 id）
  const dupIdx = rows.value.findIndex((r) => (alarm.uuid && r.uuid === alarm.uuid) || r.id === alarm.id)
  if (dupIdx === 0) return
  if (dupIdx > 0) rows.value.splice(dupIdx, 1)
  rows.value.unshift(enriched)
  // 总数 +1
  pagination.total += 1
  // 5 秒后去掉 _new 高亮（避免重复动画累积）
  window.setTimeout(() => {
    const idx = rows.value.findIndex((r) => r.uuid === alarm.uuid || r.id === alarm.id)
    if (idx >= 0) delete rows.value[idx]._new
  }, 5000)
}

function connectWs() {
  if (wsCtrl) return
  // 后端 WS 握手不依赖 satoken（framework-starter WebSocketInterceptor 只从 query 读 uid/type），
  // 即使 userStore 没拿到当前用户（current 接口 code 检查在 D-tier 有 bug），仍然尝试连接，
  // 这样 alarm 页指示器能正常变绿（连接状态独立于 user.fetchCurrent）。
  // uid 优先用 userStore.id（真的同步上的话），否则用 'web'（PSM 约定大屏客户端 uid）。
  const uid = userStore.id ? String(userStore.id) : 'web'
  wsCtrl = createWs({
    uid,
    type: 'alarm',
    onMessage(msg) {
      // 兼容多种后端消息形态：
      //   { type: 'alarm', payload: <AlarmRecord | { data: AlarmRecord }> }
      //   { type: 'data', payload: any }
      const tp = String(msg?.type || '')
      const payload = msg?.payload
      if (!payload || typeof payload !== 'object') return
      let alarm: AlarmRecord | null = null
      if (tp === 'alarm') {
        // payload 可能是 record 直接，也可能是 { data: record }
        const cand: any = (payload as any).data ?? payload
        if (cand && (cand.id || cand.uuid || cand.message || cand.time)) {
          alarm = cand as AlarmRecord
        }
      } else if (tp === 'push-alarm' || tp === 'new-alarm') {
        alarm = (payload as any).data ?? payload
      }
      if (alarm) pushIncomingAlarm(alarm)
    },
    onState(s) {
      wsState.value = s
    }
  })
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
// 生命周期
// ---------------------------------------------------------------------------
onMounted(async () => {
  await syncPermissionFromCurrentUser()
  await Promise.all([loadLineTree(), fetchList()])
  connectWs()
})

onBeforeUnmount(() => {
  disconnectWs()
  if (durationTimer.value) {
    window.clearInterval(durationTimer.value)
    durationTimer.value = null
  }
})
</script>

<style lang="scss" scoped>
// =============================================================================
// 顶部 actions（实时连接指示器 + 刷新）
// =============================================================================
.alarm-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.ws-indicator {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.3px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--glass-border);
  color: var(--text-secondary);
  transition: all var(--transition-base);

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--text-secondary);
    box-shadow: 0 0 0 0 currentColor;
    animation: none;
  }

  &--open {
    color: var(--success);
    border-color: rgba(95, 217, 127, 0.4);
    background: rgba(95, 217, 127, 0.10);

    .ws-indicator__dot {
      background: var(--success);
      box-shadow: 0 0 8px rgba(95, 217, 127, 0.6);
      animation: ws-pulse 2s ease-in-out infinite;
    }
  }

  &--connecting {
    color: var(--accent);
    border-color: rgba(92, 225, 255, 0.4);
    background: rgba(92, 225, 255, 0.10);

    .ws-indicator__dot {
      background: var(--accent);
      animation: ws-pulse 1s ease-in-out infinite;
    }
  }

  &--closed,
  &--closing {
    color: var(--danger);
    border-color: rgba(255, 90, 95, 0.4);
    background: rgba(255, 90, 95, 0.10);

    .ws-indicator__dot {
      background: var(--danger);
    }
  }
}

@keyframes ws-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.35); opacity: 0.65; }
}

// =============================================================================
// 筛选栏
// =============================================================================
.alarm-filter {
  &__row {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--space-4);
  }

  &__field {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    min-width: 160px;

    &--actions {
      flex-direction: row;
      align-items: flex-end;
      gap: var(--space-2);
      margin-left: auto;
    }
  }

  &__label {
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    letter-spacing: 0.3px;
    text-transform: uppercase;
  }

  &__control {
    width: 160px;

    &--wide {
      width: 280px;
    }
  }
}

// Element Plus 内部样式微调（玻璃风）
:deep(.alarm-filter__control .el-input__wrapper),
:deep(.alarm-filter__control .el-select__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px var(--glass-border);
  border-radius: var(--radius-md);
}

:deep(.alarm-filter__control .el-input__wrapper:hover),
:deep(.alarm-filter__control .el-select__wrapper:hover) {
  box-shadow: inset 0 0 0 1px rgba(92, 225, 255, 0.4);
}

// =============================================================================
// 表格列样式（pill 标签）
// =============================================================================
.type-pill,
.level-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.2px;
  border: 1px solid transparent;
}

.type-pill {
  &--1 { background: rgba(255, 110, 199, 0.18); color: #ff9ed7; border-color: rgba(255, 110, 199, 0.32); }
  &--2 { background: rgba(92, 225, 255, 0.18); color: var(--accent); border-color: rgba(92, 225, 255, 0.32); }
  &--3 { background: rgba(255, 183, 77, 0.18); color: var(--warning); border-color: rgba(255, 183, 77, 0.32); }
}

.level-pill {
  &--1 { background: rgba(95, 217, 127, 0.16); color: var(--success); border-color: rgba(95, 217, 127, 0.32); }
  &--2 { background: rgba(255, 90, 95, 0.18); color: var(--danger); border-color: rgba(255, 90, 95, 0.4); }
}

.status-pill {
  &--1 { background: rgba(95, 217, 127, 0.16); color: var(--success); border-color: rgba(95, 217, 127, 0.32); }
  &--2 { background: rgba(255, 90, 95, 0.20); color: #ff8b8e; border-color: rgba(255, 90, 95, 0.45); }
  &--3 { background: rgba(255, 255, 255, 0.06); color: var(--text-secondary); border-color: rgba(255, 255, 255, 0.18); }
}

// 行高亮：未处理 = 红色玻璃态；新到达 = 青色闪烁一次
:deep(.el-table__row) td.el-table__cell {
  transition: background var(--transition-base);
}
:deep(.el-table__row.alarm-row--pending td.el-table__cell) {
  background: linear-gradient(90deg, rgba(255, 90, 95, 0.12), rgba(255, 90, 95, 0.04) 60%, transparent) !important;
  border-left: 2px solid rgba(255, 90, 95, 0.45);
}
:deep(.el-table__row.alarm-row--new td.el-table__cell) {
  animation: row-flash 1.2s ease-out 1;
}
@keyframes row-flash {
  0%   { background: rgba(92, 225, 255, 0.30) !important; }
  100% { background: transparent; }
}

.action-ignore {
  margin-left: var(--space-2);
}

// =============================================================================
// 分页
// =============================================================================
.alarm-pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-2) var(--space-3);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.05);
  --el-pagination-button-color: var(--text-secondary);
  --el-pagination-hover-color: var(--accent);
  --el-pagination-button-disabled-bg-color: transparent;
}

// =============================================================================
// 空态
// =============================================================================
.alarm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) 0;
  color: var(--text-secondary);

  &__icon {
    font-size: 48px;
    opacity: 0.55;
  }
  &__text {
    font-size: var(--font-size-base);
  }
}

// =============================================================================
// 详情弹窗（玻璃面板适配）
// =============================================================================
.alarm-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  &__row {
    display: grid;
    grid-template-columns: 120px 1fr;
    gap: var(--space-3);
    align-items: start;
    padding: var(--space-2) 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);

    &--image {
      grid-template-columns: 120px 1fr;
    }
  }

  &__label {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    letter-spacing: 0.3px;
    text-transform: uppercase;
    padding-top: 2px;
  }

  &__value {
    color: var(--text-primary);
    font-size: var(--font-size-base);
    word-break: break-all;

    &--mono {
      font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
      font-size: var(--font-size-sm);
    }
  }

  &__image {
    width: 100%;
    min-height: 120px;
    border-radius: var(--radius-md);
    background: rgba(255, 255, 255, 0.04);
    border: 1px dashed rgba(255, 255, 255, 0.16);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__image-placeholder {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
  }
}

// 弹窗头部 / 底部加玻璃风
:deep(.alarm-dialog .el-dialog) {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  color: var(--text-primary);
}
:deep(.alarm-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}
:deep(.alarm-dialog .el-dialog__body) {
  color: var(--text-primary);
}
:deep(.alarm-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

// =============================================================================
// W-DEFECT-CFG 子单 C：子 tab 玻璃风
// =============================================================================
.alarm-tabs {
  padding: 0 8px;

  :deep(.el-tabs__nav-wrap::after) {
    background: rgba(255, 255, 255, 0.08);
  }

  :deep(.el-tabs__item) {
    color: var(--text-secondary);
    font-weight: var(--font-weight-semibold);
    height: 52px;
    line-height: 52px;
    font-size: var(--font-size-base);
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--accent);
  }

  :deep(.el-tabs__active-bar) {
    background: var(--accent);
  }
}

.alarm-tab-pane {
  padding: 24px 24px 32px;
}
</style>
