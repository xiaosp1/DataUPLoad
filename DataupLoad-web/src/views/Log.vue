<template>
  <GlassPage :title="$t('log.title')" :subtitle="$t('log.subtitle')">
    <!-- 顶部筛选区 -->
    <GlassCard class="log-filter">
      <div class="log-filter__row">
        <!-- 操作者 -->
        <div class="log-filter__field">
          <label class="log-filter__label">{{ $t('log.filter.operator') }}</label>
          <el-input
            v-model.trim="query.operator"
            :placeholder="$t('log.filter.operator')"
            clearable
            class="log-filter__control"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>
        <!-- 操作描述 -->
        <div class="log-filter__field">
          <label class="log-filter__label">{{ $t('log.filter.operation') }}</label>
          <el-input
            v-model.trim="query.operation"
            :placeholder="$t('log.filter.operation')"
            clearable
            class="log-filter__control"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>
        <!-- 结果 -->
        <div class="log-filter__field">
          <label class="log-filter__label">{{ $t('log.filter.result') }}</label>
          <el-select
            v-model="query.result"
            :placeholder="$t('log.filter.allResult')"
            clearable
            class="log-filter__control"
            @change="reload"
          >
            <el-option :label="$t('log.filter.allResult')" :value="null" />
            <el-option :label="$t('log.filter.successOnly')" :value="1" />
            <el-option :label="$t('log.filter.failureOnly')" :value="0" />
          </el-select>
        </div>
        <!-- 模块 -->
        <div class="log-filter__field">
          <label class="log-filter__label">{{ $t('log.filter.module') }}</label>
          <el-input
            v-model.trim="query.module"
            :placeholder="$t('log.filter.module')"
            clearable
            class="log-filter__control"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>
        <!-- 操作按钮 -->
        <div class="log-filter__actions">
          <GlassButton variant="primary" :loading="loading" @click="reload">
            {{ $t('log.filter.query') }}
          </GlassButton>
          <GlassButton variant="default" @click="onReset">
            {{ $t('log.filter.reset') }}
          </GlassButton>
        </div>
      </div>

      <!-- 展开更多（IP + 时间范围） -->
      <div class="log-filter__row">
        <div class="log-filter__field">
          <label class="log-filter__label">{{ $t('log.filter.ip') }}</label>
          <el-input
            v-model.trim="query.ip"
            :placeholder="$t('log.filter.ip')"
            clearable
            class="log-filter__control"
            @keyup.enter="reload"
            @clear="reload"
          />
        </div>
        <div class="log-filter__field log-filter__field--time">
          <label class="log-filter__label">{{ $t('log.filter.timeRange') }}</label>
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            :start-placeholder="$t('log.filter.startTime')"
            :end-placeholder="$t('log.filter.endTime')"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="log-filter__control"
            @change="onTimeRangeChange"
          />
        </div>
        <div class="log-filter__actions">
          <span class="log-filter__hint">{{ $t('log.list.longTimeThreshold') }}</span>
        </div>
      </div>
    </GlassCard>

    <!-- 表格 -->
    <GlassCard :padding="0" class="log-table-card">
      <div class="log-toolbar">
        <div class="log-toolbar__count">
          <span>{{ $t('common.total') || 'Total' }}: </span>
          <strong>{{ total }}</strong>
        </div>
        <div class="log-toolbar__spacer" />
        <GlassButton variant="default" :loading="loading" @click="reload">
          {{ $t('common.refresh') }}
        </GlassButton>
      </div>

      <!-- W-PERF-D: 玻璃风骨架屏（首次加载） -->
      <GlassSkeletonTable v-if="loading && rows.length === 0" :columns="10" :rows="8" />
      <GlassTable v-else :data="rows" v-loading="loading" class="log-table">
        <el-table-column type="index" :label="$t('log.table.index')" width="56" align="center" />
        <el-table-column
          :label="$t('log.table.operator')"
          prop="operator"
          min-width="120"
        >
          <template #default="{ row }">
            <span class="log-table__operator">{{ row.operator || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('log.table.module')"
          prop="module"
          min-width="120"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.module"
              size="small"
              effect="plain"
              class="log-table__module-tag"
            >
              {{ row.module }}
            </el-tag>
            <span v-else class="log-table__muted">—</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('log.table.operation')"
          prop="operation"
          min-width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="log-table__op">{{ row.operation || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('log.table.uri')"
          prop="uri"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <code class="log-table__uri">{{ row.uri || '—' }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="$t('log.table.ip')" prop="ip" min-width="120">
          <template #default="{ row }">
            <span class="log-table__muted">{{ row.ip || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('log.table.cost')" prop="cost" width="100" sortable>
          <template #default="{ row }">
            <span v-if="row.cost == null" class="log-table__muted">—</span>
            <el-tag
              v-else-if="row.cost > 1000"
              type="danger"
              effect="dark"
              size="small"
              :title="$t('log.table.longTimeWarning')"
            >
              {{ row.cost }} {{ $t('log.table.unitMs') }}
            </el-tag>
            <span v-else class="log-table__cost">{{ row.cost }} {{ $t('log.table.unitMs') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('log.table.result')" prop="result" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.result === 1"
              type="success"
              effect="dark"
              size="small"
            >
              ✓ {{ $t('log.success') }}
            </el-tag>
            <el-tag
              v-else-if="row.result === 0"
              type="danger"
              effect="dark"
              size="small"
            >
              ✕ {{ $t('log.failure') }}
            </el-tag>
            <span v-else class="log-table__muted">—</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('log.table.createTime')"
          prop="createTime"
          width="180"
          sortable
        >
          <template #default="{ row }">
            <span class="log-table__muted">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('log.table.action')"
          width="100"
          fixed="right"
          align="center"
        >
          <template #default="{ row }">
            <GlassButton variant="default" size="small" @click="openDetail(row)">
              {{ $t('log.table.view') }}
            </GlassButton>
          </template>
        </el-table-column>

        <template #empty>
          <div class="log-table__empty">
            <span class="log-table__empty-icon">📋</span>
            <span>{{ $t('log.list.empty') }}</span>
          </div>
        </template>
      </GlassTable>

      <!-- 分页 -->
      <div class="log-pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </GlassCard>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="$t('log.detail.title')"
      direction="rtl"
      size="44%"
      :destroy-on-close="true"
      class="log-drawer"
    >
      <div v-if="current" class="log-drawer__body">
        <!-- 基础信息 -->
        <div class="log-drawer__section">
          <div class="log-drawer__section-title">
            <span class="log-drawer__bar" />
            <span>{{ $t('log.detail.basicInfo') }}</span>
          </div>
          <div class="log-drawer__grid">
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.operator') }}</span>
              <span class="log-drawer__value">{{ current.operator || '—' }}</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.module') }}</span>
              <el-tag v-if="current.module" size="small" effect="plain">
                {{ current.module }}
              </el-tag>
              <span v-else class="log-drawer__muted">—</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.operation') }}</span>
              <span class="log-drawer__value">{{ current.operation || '—' }}</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.requestAddress') }}</span>
              <span class="log-drawer__value">{{ current.ip || '—' }}</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.requestPath') }}</span>
              <code class="log-drawer__code">{{ current.uri || '—' }}</code>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.requestCost') }}</span>
              <el-tag
                v-if="(current.cost ?? 0) > 1000"
                type="danger"
                effect="dark"
                size="small"
              >
                {{ current.cost }} {{ $t('log.table.unitMs') }}
              </el-tag>
              <span v-else class="log-drawer__value">
                {{ current.cost ?? '—' }} {{ current.cost != null ? $t('log.table.unitMs') : '' }}
              </span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.table.result') }}</span>
              <el-tag
                v-if="current.result === 1"
                type="success"
                effect="dark"
                size="small"
              >
                ✓ {{ $t('log.success') }}
              </el-tag>
              <el-tag
                v-else-if="current.result === 0"
                type="danger"
                effect="dark"
                size="small"
              >
                ✕ {{ $t('log.failure') }}
              </el-tag>
              <span v-else class="log-drawer__muted">—</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.callTime') }}</span>
              <span class="log-drawer__muted">{{ current.createTime || '—' }}</span>
            </div>
            <div class="log-drawer__item">
              <span class="log-drawer__label">{{ $t('log.detail.completeTime') }}</span>
              <span class="log-drawer__muted">{{ current.updateTime || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 请求参数 -->
        <div class="log-drawer__section">
          <div class="log-drawer__section-title">
            <span class="log-drawer__bar" />
            <span>{{ $t('log.detail.requestParam') }}</span>
            <div class="log-drawer__spacer" />
            <el-button
              v-if="current.inputparam"
              size="small"
              link
              type="primary"
              @click="copyText(current.inputparam || '')"
            >
              {{ $t('log.detail.copy') }}
            </el-button>
          </div>
          <div class="log-drawer__payload">
            <div v-if="!current.inputparam" class="log-drawer__empty">
              {{ $t('log.detail.none') }}
            </div>
            <pre v-else-if="parsedReq" class="log-drawer__json">{{ parsedReq }}</pre>
            <el-input
              v-else
              type="textarea"
              :model-value="current.inputparam || ''"
              :autosize="{ minRows: 4, maxRows: 12 }"
              readonly
              class="log-drawer__textarea"
            />
            <div v-if="!parsedReq && current.inputparam && parseErrMsg" class="log-drawer__notice">
              {{ $t('log.detail.invalidJson') }}（{{ parseErrMsg }}）
            </div>
          </div>
        </div>

        <!-- 响应数据 -->
        <div class="log-drawer__section">
          <div class="log-drawer__section-title">
            <span class="log-drawer__bar" />
            <span>{{ $t('log.detail.responseData') }}</span>
            <div class="log-drawer__spacer" />
            <el-button
              v-if="current.outputparam"
              size="small"
              link
              type="primary"
              @click="copyText(current.outputparam || '')"
            >
              {{ $t('log.detail.copy') }}
            </el-button>
          </div>
          <div class="log-drawer__payload">
            <div v-if="!current.outputparam" class="log-drawer__empty">
              {{ $t('log.detail.none') }}
            </div>
            <pre v-else-if="parsedResp" class="log-drawer__json">{{ parsedResp }}</pre>
            <el-input
              v-else
              type="textarea"
              :model-value="current.outputparam || ''"
              :autosize="{ minRows: 4, maxRows: 16 }"
              readonly
              class="log-drawer__textarea"
            />
            <div v-if="!parsedResp && current.outputparam && parseErrMsg" class="log-drawer__notice">
              {{ $t('log.detail.invalidJson') }}（{{ parseErrMsg }}）
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </GlassPage>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { GlassPage, GlassCard, GlassButton, GlassTable, GlassSkeletonTable } from '../components'
import { listApiLog, type ApiLog, type ApiLogQuery } from '../api/log'

const { t } = useI18n()

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------

/** 列表查询条件（默认 result=null 表示全部） */
const query = reactive<{
  operator: string
  operation: string
  module: string
  ip: string
  result: number | null
  startTime: string
  endTime: string
}>({
  operator: '',
  operation: '',
  module: '',
  ip: '',
  result: null,
  startTime: '',
  endTime: ''
})

const timeRange = ref<[string, string] | null>(null)

const rows = ref<ApiLog[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

// 抽屉
const drawerVisible = ref(false)
const current = ref<ApiLog | null>(null)

// ---------------------------------------------------------------------------
// JSON 格式化
// ---------------------------------------------------------------------------

function tryPrettyJson(raw: string | null | undefined): { ok: boolean; text: string; err: string } {
  if (!raw) return { ok: false, text: '', err: '' }
  const trimmed = raw.trim()
  // 仅对看起来像 JSON / 数组的字符串做 pretty
  if (!(trimmed.startsWith('{') || trimmed.startsWith('['))) {
    return { ok: false, text: '', err: '' }
  }
  try {
    const obj = JSON.parse(trimmed)
    const pretty = JSON.stringify(obj, null, 2)
    return { ok: true, text: pretty, err: '' }
  } catch (e: unknown) {
    return { ok: false, text: '', err: (e as Error)?.message || 'parse error' }
  }
}

const reqParsed = computed(() => tryPrettyJson(current.value?.inputparam))
const respParsed = computed(() => tryPrettyJson(current.value?.outputparam))
const parsedReq = computed(() => (reqParsed.value.ok ? reqParsed.value.text : ''))
const parsedResp = computed(() => (respParsed.value.ok ? respParsed.value.text : ''))
const parseErrMsg = computed(() => {
  // 显示第一个非 JSON 的错误（若有）
  if (current.value?.inputparam && !reqParsed.value.ok && reqParsed.value.err) {
    return reqParsed.value.err
  }
  if (current.value?.outputparam && !respParsed.value.ok && respParsed.value.err) {
    return respParsed.value.err
  }
  return ''
})

// ---------------------------------------------------------------------------
// 行为
// ---------------------------------------------------------------------------

function buildParams(): ApiLogQuery {
  const params: ApiLogQuery = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    operator: query.operator || undefined,
    operation: query.operation || undefined,
    module: query.module || undefined,
    ip: query.ip || undefined,
    result: query.result === null ? undefined : query.result,
    startTime: query.startTime || undefined,
    endTime: query.endTime || undefined
  }
  return params
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    const env = await listApiLog(buildParams())
    if (env && env.success && env.data) {
      rows.value = env.data.records || []
      total.value = env.data.total || 0
    } else {
      rows.value = []
      total.value = 0
      const msg = env?.message || env?.msg || t('log.list.loadFailed')
      ElMessage.error(msg)
    }
  } catch (err: unknown) {
    // 401 由全局拦截器跳 /login；其他错误兜底
    rows.value = []
    total.value = 0
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status !== 401) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || (err as Error)?.message
        || t('log.list.networkError')
      ElMessage.error(msg)
    }
  } finally {
    loading.value = false
  }
}

// ---------------------------------------------------------------------------
// demo data fallback (only when query string contains `?demo=1`)
// 用于演示截图 / 离线预览，不影响生产路径
// ---------------------------------------------------------------------------
function buildDemoRows(): ApiLog[] {
  const now = Date.now()
  const f = (n: number) => new Date(now - n * 60_000).toISOString().replace('T', ' ').slice(0, 23)
  const json = (o: unknown) => JSON.stringify(o, null, 2)
  return [
    {
      id: 1009,
      operatorId: 1,
      operator: 'super_admin',
      ip: '127.0.0.1',
      module: 'account',
      operation: '重置账号密码',
      result: 1,
      uri: '/web/account/pwd-reset',
      cost: 48,
      createTime: f(2),
      updateTime: f(2),
      inputparam: json({ id: 21 }),
      outputparam: json({ success: true, code: 0 })
    },
    {
      id: 1008,
      operatorId: 21,
      operator: 'e4_demo',
      ip: '10.70.64.170',
      module: 'account',
      operation: '新增账号',
      result: 1,
      uri: '/web/account',
      cost: 123,
      createTime: f(8),
      updateTime: f(8),
      inputparam: json({ username: 'e4_demo_1785373584000', roleId: 3 }),
      outputparam: json({ success: true, code: 0 })
    },
    {
      id: 1007,
      operatorId: 1,
      operator: 'super_admin',
      ip: '127.0.0.1',
      module: 'auth',
      operation: '用户登录',
      result: 1,
      uri: '/web/auth/login',
      cost: 256,
      createTime: f(15),
      updateTime: f(15),
      inputparam: json({ username: 'super_admin' }),
      outputparam: json({ success: true, code: 0, data: { id: 1, username: 'super_admin' } })
    },
    {
      id: 1006,
      operatorId: 1,
      operator: 'super_admin',
      ip: '127.0.0.1',
      module: 'detect',
      operation: '查询实时数据',
      result: 1,
      uri: '/web/detect/realtime',
      cost: 35,
      createTime: f(20),
      updateTime: f(20),
      inputparam: null,
      outputparam: json({ total: 1842, ngCount: 7, efficiency: 98.6 })
    },
    {
      id: 1005,
      operatorId: 21,
      operator: 'e4_demo',
      ip: '10.70.64.170',
      module: 'alarm',
      operation: '忽略报警',
      result: 0,
      uri: '/web/alarm/ignore',
      cost: 1234,
      createTime: f(45),
      updateTime: f(45),
      inputparam: json({ faceId: 'F01-20260730-001', ignoreAll: true }),
      outputparam: json({ success: false, code: 10500, message: '操作异常' })
    },
    {
      id: 1004,
      operatorId: 1,
      operator: 'super_admin',
      ip: '127.0.0.1',
      module: 'line',
      operation: '更新线别配置',
      result: 1,
      uri: '/web/line',
      cost: 67,
      createTime: f(60),
      updateTime: f(60),
      inputparam: json({ id: 3, name: 'L03', faceNo: 1 }),
      outputparam: json({ success: true, code: 0 })
    },
    {
      id: 1003,
      operatorId: 1,
      operator: 'super_admin',
      ip: '192.168.1.45',
      module: 'defect',
      operation: '导出缺陷记录',
      result: 1,
      uri: '/web/defect/export',
      cost: 589,
      createTime: f(120),
      updateTime: f(120),
      inputparam: json({ startTime: '2026-07-29 00:00:00', endTime: '2026-07-30 00:00:00', format: 'xlsx' }),
      outputparam: null
    },
    {
      id: 1002,
      operatorId: 21,
      operator: 'e4_demo',
      ip: '10.70.64.170',
      module: 'alarm',
      operation: '查询报警列表',
      result: 1,
      uri: '/web/alarm/list',
      cost: 41,
      createTime: f(180),
      updateTime: f(180),
      inputparam: json({ pageNum: 1, pageSize: 20 }),
      outputparam: json({ records: [], total: 0 })
    },
    {
      id: 1001,
      operatorId: 1,
      operator: 'super_admin',
      ip: '127.0.0.1',
      module: 'systemConfig',
      operation: '更新系统配置',
      result: 1,
      uri: '/web/system-config',
      cost: 98,
      createTime: f(240),
      updateTime: f(240),
      inputparam: json([{ id: 1, configKey: 'sound_play_count', configValue: '3' }]),
      outputparam: json({ success: true, code: 0 })
    },
    {
      id: 1000,
      operatorId: null,
      operator: 'system',
      ip: '127.0.0.1',
      module: 'trace',
      operation: '启动扫描',
      result: 1,
      uri: '/internal/scan',
      cost: 1024,
      createTime: f(360),
      updateTime: f(360),
      inputparam: null,
      outputparam: json({ scanned: 42, ok: 42, failed: 0 })
    }
  ]
}

function maybeUseDemoData(): boolean {
  if (typeof window === 'undefined') return false
  try {
    // hash 路由下，?demo=1 在 hash 里（例如 #/log?demo=1）
    const hash = window.location.hash || ''
    const qIndex = hash.indexOf('?')
    const qs = qIndex >= 0 ? hash.slice(qIndex + 1) : ''
    const sp = new URLSearchParams(qs)
    if (sp.get('demo') === '1') return true
    // 同时兼容 location.search（如果未来改为 history 路由）
    const sp2 = new URLSearchParams(window.location.search || '')
    return sp2.get('demo') === '1'
  } catch {
    return false
  }
}

function onReset(): void {
  query.operator = ''
  query.operation = ''
  query.module = ''
  query.ip = ''
  query.result = null
  query.startTime = ''
  query.endTime = ''
  timeRange.value = null
  pageNum.value = 1
  reload()
}

function onTimeRangeChange(val: [string, string] | null): void {
  if (val && val.length === 2) {
    query.startTime = val[0] || ''
    query.endTime = val[1] || ''
  } else {
    query.startTime = ''
    query.endTime = ''
  }
  reload()
}

function openDetail(row: ApiLog): void {
  current.value = row
  drawerVisible.value = true
}

async function copyText(text: string): Promise<void> {
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      // 兜底：textarea + execCommand
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    ElMessage.success(t('log.detail.copied'))
  } catch {
    ElMessage.error(t('log.detail.copyFail'))
  }
}

onMounted(() => {
  if (maybeUseDemoData()) {
    rows.value = buildDemoRows()
    total.value = rows.value.length
    return
  }
  reload()
})
</script>

<style lang="scss" scoped>
.log-filter {
  margin-bottom: var(--space-4);

  &__row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3) var(--space-4);
    align-items: flex-end;
    margin-bottom: var(--space-3);
    &:last-child {
      margin-bottom: 0;
    }
  }
  &__field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 180px;
    flex: 0 1 220px;
    &--time {
      min-width: 360px;
      flex: 0 1 420px;
    }
  }
  &__label {
    font-size: var(--font-size-xs, 12px);
    color: var(--text-secondary, #888);
    font-weight: 500;
  }
  &__control {
    width: 100%;
  }
  &__actions {
    display: flex;
    gap: var(--space-2);
    align-items: center;
  }
  &__hint {
    font-size: var(--font-size-xs, 12px);
    color: var(--text-secondary, #888);
    margin-left: var(--space-2);
  }
}

.log-table-card {
  display: flex;
  flex-direction: column;
}

.log-toolbar {
  display: flex;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--glass-border, rgba(255, 255, 255, 0.12));

  &__count {
    font-size: var(--font-size-sm, 13px);
    color: var(--text-secondary, #888);
    strong {
      color: var(--text-primary, #f0f0f0);
      margin-left: 4px;
    }
  }
  &__spacer {
    flex: 1;
  }
}

.log-table {
  &__operator {
    font-weight: 500;
  }
  &__module-tag {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
  }
  &__op {
    color: var(--text-primary, #f0f0f0);
  }
  &__uri {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
    color: var(--text-secondary, #c0c0c0);
    background: rgba(255, 255, 255, 0.04);
    padding: 2px 6px;
    border-radius: 4px;
  }
  &__cost {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    color: var(--text-secondary, #c0c0c0);
  }
  &__muted {
    color: var(--text-secondary, #888);
    font-style: italic;
  }
  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--space-8) var(--space-4);
    color: var(--text-secondary, #888);
    gap: var(--space-2);
    &-icon {
      font-size: 36px;
    }
  }
}

.log-pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--glass-border, rgba(255, 255, 255, 0.12));
}

// 抽屉
.log-drawer {
  &__body {
    padding: 0 var(--space-4) var(--space-6);
  }
  &__section {
    margin-bottom: var(--space-5);
    &:last-child {
      margin-bottom: 0;
    }
    &-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: var(--font-size-md, 14px);
      font-weight: 600;
      color: var(--text-primary, #f0f0f0);
      margin-bottom: var(--space-3);
    }
  }
  &__bar {
    display: inline-block;
    width: 4px;
    height: 14px;
    border-radius: 2px;
    background: var(--accent, #5fbeff);
  }
  &__spacer {
    flex: 1;
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-3) var(--space-4);
  }
  &__item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }
  &__label {
    font-size: var(--font-size-xs, 12px);
    color: var(--text-secondary, #888);
  }
  &__value {
    color: var(--text-primary, #f0f0f0);
    word-break: break-all;
  }
  &__code {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
    color: var(--text-secondary, #c0c0c0);
    background: rgba(255, 255, 255, 0.04);
    padding: 4px 8px;
    border-radius: 4px;
    word-break: break-all;
  }
  &__muted {
    color: var(--text-secondary, #888);
  }
  &__payload {
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.12));
    border-radius: 6px;
    overflow: hidden;
  }
  &__json {
    margin: 0;
    padding: var(--space-3);
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
    line-height: 1.5;
    color: var(--text-primary, #f0f0f0);
    background: rgba(0, 0, 0, 0.2);
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
  &__textarea {
    :deep(.el-textarea__inner) {
      background: rgba(0, 0, 0, 0.2);
      color: var(--text-primary, #f0f0f0);
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
    }
  }
  &__empty {
    padding: var(--space-3);
    text-align: center;
    color: var(--text-secondary, #888);
    font-style: italic;
  }
  &__notice {
    padding: 6px var(--space-3);
    font-size: var(--font-size-xs, 12px);
    color: #b89853;
    border-top: 1px dashed var(--glass-border, rgba(255, 255, 255, 0.12));
  }
}

// element-plus el-drawer 主题适配
:deep(.el-drawer) {
  background: var(--glass-bg, rgba(20, 22, 32, 0.92));
  color: var(--text-primary, #f0f0f0);
}
:deep(.el-drawer__header) {
  color: var(--text-primary, #f0f0f0);
  font-weight: 600;
  margin-bottom: var(--space-3);
  padding: var(--space-4) var(--space-4) 0;
}
:deep(.el-drawer__body) {
  padding: 0;
}
:deep(.el-drawer__close-btn) {
  color: var(--text-secondary, #888);
}
</style>
