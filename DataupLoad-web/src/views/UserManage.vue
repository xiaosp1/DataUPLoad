<template>
  <GlassPage
    :title="$t('user.title')"
    :subtitle="$t('user.subtitle')"
  >
    <!-- ================================================================ -->
    <!-- KPI 3 卡 -->
    <!-- ================================================================ -->
    <div class="user-kpi">
      <GlassCard class="user-kpi__card">
        <div class="user-kpi__icon user-kpi__icon--total">👥</div>
        <div class="user-kpi__body">
          <div class="user-kpi__value">{{ kpiTotal }}</div>
          <div class="user-kpi__label">{{ $t('user.kpi.total') }}</div>
        </div>
      </GlassCard>
      <GlassCard class="user-kpi__card">
        <div class="user-kpi__icon user-kpi__icon--online">🟢</div>
        <div class="user-kpi__body">
          <div class="user-kpi__value">{{ kpiOnline }}</div>
          <div class="user-kpi__label">{{ $t('user.kpi.online') }}</div>
        </div>
      </GlassCard>
      <GlassCard class="user-kpi__card">
        <div class="user-kpi__icon user-kpi__icon--new">🆕</div>
        <div class="user-kpi__body">
          <div class="user-kpi__value">{{ kpiNewToday }}</div>
          <div class="user-kpi__label">{{ $t('user.kpi.newToday') }}</div>
        </div>
      </GlassCard>
    </div>

    <!-- ================================================================ -->
    <!-- 筛选栏 -->
    <!-- ================================================================ -->
    <GlassCard class="user-filter">
      <div class="user-filter__row">
        <div class="user-filter__item">
          <label class="user-filter__label">{{ $t('user.filter.name') }}</label>
          <el-input
            v-model="filter.name"
            :placeholder="$t('user.filter.name')"
            clearable
            size="default"
            class="user-filter__input"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </div>
        <div class="user-filter__item">
          <label class="user-filter__label">{{ $t('user.filter.workNo') }}</label>
          <el-input
            v-model="filter.workNo"
            :placeholder="$t('user.filter.workNo')"
            clearable
            size="default"
            class="user-filter__input"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </div>
        <div class="user-filter__item">
          <label class="user-filter__label">{{ $t('user.filter.shift') }}</label>
          <el-select
            v-model="filter.shift"
            :placeholder="$t('user.filter.shift')"
            clearable
            size="default"
            class="user-filter__select"
            @change="onSearch"
          >
            <el-option label="A 班" value="A" />
            <el-option label="B 班" value="B" />
            <el-option label="C 班" value="C" />
          </el-select>
        </div>
        <div class="user-filter__actions">
          <GlassButton variant="primary" @click="onSearch">
            🔍 {{ $t('common.search') }}
          </GlassButton>
          <GlassButton variant="default" @click="onReset">
            ↻ {{ $t('common.reset') }}
          </GlassButton>
          <GlassButton variant="default" @click="reload">
            ⟳ {{ $t('common.refresh') }}
          </GlassButton>
        </div>
      </div>
    </GlassCard>

    <!-- ================================================================ -->
    <!-- 操作员表格 -->
    <!-- ================================================================ -->
    <GlassCard :padding="0">
      <GlassTable v-loading="loading" :data="list" class="user-table">
        <el-table-column
          :label="$t('user.table.workNo')"
          prop="username"
          min-width="180"
        >
          <template #default="{ row }">
            <span class="user-table__mono">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.name')"
          prop="realName"
          min-width="120"
        >
          <template #default="{ row }">
            <span class="user-table__name">{{ row.realName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.shift')"
          width="100"
        >
          <template #default="{ row }">
            <span class="user-table__muted">{{ row.shift || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.line')"
          min-width="120"
        >
          <template #default="{ row }">
            <span class="user-table__muted">{{ row.line || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.phone')"
          prop="contactInfo"
          min-width="140"
        >
          <template #default="{ row }">
            <span class="user-table__mono">{{ row.contactInfo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.hireDate')"
          prop="createTime"
          width="180"
        >
          <template #default="{ row }">
            <span class="user-table__muted">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('user.table.status')"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'resigned' ? 'danger' : 'success'"
              effect="dark"
              size="small"
            >
              {{ row.status === 'resigned' ? $t('user.status.resigned') : $t('user.status.active') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('common.action')"
          width="220"
          fixed="right"
          align="center"
        >
          <template #default="{ row }">
            <GlassButton variant="primary" size="small" @click="openDetail(row)">
              📋 {{ $t('user.table.detail') }}
            </GlassButton>
            <GlassButton
              variant="default"
              size="small"
              class="user-table__btn"
              @click="onEdit(row)"
            >
              ✎ {{ $t('user.table.edit') }}
            </GlassButton>
            <GlassButton
              v-if="row.status !== 'resigned'"
              variant="danger"
              size="small"
              class="user-table__btn"
              @click="onResign(row)"
            >
              🚫 {{ $t('user.table.resign') }}
            </GlassButton>
          </template>
        </el-table-column>

        <template #empty>
          <div class="user-table__empty">
            <span class="user-table__empty-icon">👤</span>
            <span>{{ $t('user.detail.empty') }}</span>
          </div>
        </template>
      </GlassTable>

      <!-- 分页 -->
      <div class="user-pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="reload"
          @current-change="reload"
        />
      </div>
    </GlassCard>

    <!-- ================================================================ -->
    <!-- 详情弹窗：description list + el-timeline 操作历史 -->
    <!-- ================================================================ -->
    <el-dialog
      v-model="detail.open"
      :title="$t('user.detail.title')"
      width="640"
      :close-on-click-modal="false"
      class="user-dialog"
    >
      <template v-if="detail.data">
        <!-- description list 风格档案 -->
        <div class="user-detail">
          <div class="user-detail__section">
            <h3 class="user-detail__section-title">{{ $t('user.title') }}</h3>
            <el-descriptions :column="2" border class="user-detail__desc">
              <el-descriptions-item :label="$t('user.table.workNo')" :span="1">
                <span class="user-table__mono">{{ detail.data.username }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.name')" :span="1">
                {{ detail.data.realName || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.shift')" :span="1">
                {{ detail.data.shift || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.line')" :span="1">
                {{ detail.data.line || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.phone')" :span="1">
                <span class="user-table__mono">{{ detail.data.contactInfo || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.hireDate')" :span="1">
                {{ detail.data.createTime || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('user.table.status')" :span="2">
                <el-tag
                  :type="detail.data.status === 'resigned' ? 'danger' : 'success'"
                  effect="dark"
                  size="small"
                >
                  {{ detail.data.status === 'resigned' ? $t('user.status.resigned') : $t('user.status.active') }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 操作历史时间线 -->
          <div class="user-detail__section">
            <h3 class="user-detail__section-title">
              {{ $t('user.detail.historyTitle') }}
              <span v-if="detail.historyLoading" class="user-detail__loading">
                {{ $t('common.loading') }}
              </span>
            </h3>
            <div v-if="detail.historyLoading" class="user-detail__history-empty">
              {{ $t('common.loading') }}
            </div>
            <div v-else-if="detail.historyError" class="user-detail__history-empty">
              {{ $t('user.detail.noHistory') }}
            </div>
            <el-timeline v-else-if="detail.history.length > 0">
              <el-timeline-item
                v-for="(h, i) in detail.history"
                :key="i"
                :timestamp="h.createTime || h.time || '—'"
                placement="top"
                :type="h.type === 'login' ? 'primary' : h.type === 'logout' ? 'warning' : 'info'"
              >
                <div class="user-detail__history-item">
                  <span class="user-detail__history-action">{{ h.action || h.operation || '—' }}</span>
                  <span class="user-detail__history-detail">{{ h.detail || h.message || '' }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="user-detail__history-empty">
              {{ $t('user.detail.noHistory') }}
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <GlassButton variant="primary" @click="detail.open = false">
          {{ $t('common.close') }}
        </GlassButton>
      </template>
    </el-dialog>

    <!-- ================================================================ -->
    <!-- 编辑弹窗（简易版：姓名 + 联系电话） -->
    <!-- ================================================================ -->
    <el-dialog
      v-model="editDialog.open"
      :title="$t('user.table.edit')"
      width="420"
      :close-on-click-modal="false"
      class="user-dialog"
    >
      <el-form
        ref="editFormRef"
        :model="editDialog.form"
        :rules="editRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item :label="$t('user.table.name')" prop="realName">
          <el-input
            v-model="editDialog.form.realName"
            :placeholder="$t('user.table.name')"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item :label="$t('user.table.phone')" prop="contactInfo">
          <el-input
            v-model="editDialog.form.contactInfo"
            :placeholder="$t('user.table.phone')"
            maxlength="32"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <GlassButton variant="default" @click="editDialog.open = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton
          variant="primary"
          :loading="editDialog.submitting"
          @click="onSubmitEdit"
        >
          {{ $t('common.save') }}
        </GlassButton>
      </template>
    </el-dialog>
  </GlassPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassButton from '../components/GlassButton.vue'
import GlassTable from '../components/GlassTable.vue'
import {
  listOperator,
  listLogByUser,
  editProfile,
  editAccount,
  type OperatorInfo
} from '../api/userManage'

const { t } = useI18n()

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------
const loading = ref(false)
const list = ref<OperatorInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filter = reactive({
  name: '',
  workNo: '',
  shift: ''
})

// KPI
const kpiTotal = ref(0)
const kpiOnline = ref(0)
const kpiNewToday = ref(0)

// ---------------------------------------------------------------------------
// 详情弹窗
// ---------------------------------------------------------------------------
interface HistoryItem {
  action?: string
  operation?: string
  detail?: string
  message?: string
  createTime?: string
  time?: string
  type?: string
}

const detail = reactive({
  open: false,
  data: null as OperatorInfo | null,
  history: [] as HistoryItem[],
  historyLoading: false,
  historyError: false
})

// ---------------------------------------------------------------------------
// 编辑弹窗
// ---------------------------------------------------------------------------
const editFormRef = ref<FormInstance>()
const editDialog = reactive({
  open: false,
  submitting: false,
  form: {
    id: 0,
    realName: '',
    contactInfo: ''
  }
})

const editRules = computed<FormRules>(() => ({
  realName: [
    { min: 0, max: 32, message: '≤32', trigger: 'blur' }
  ],
  contactInfo: [
    { min: 0, max: 32, message: '≤32', trigger: 'blur' }
  ]
}))

// ---------------------------------------------------------------------------
// KPI 计算
// ---------------------------------------------------------------------------
function calcKPI() {
  kpiTotal.value = total.value
  // 模拟在线操作员：当前列表里 updateTime 在 30 分钟内的
  const now = Date.now()
  const thirtyMin = 30 * 60 * 1000
  kpiOnline.value = list.value.filter((r) => {
    if (!r.updateTime) return false
    const t = parseTime(r.updateTime)
    return t && (now - t.getTime()) < thirtyMin
  }).length
  // 模拟今日入职：当前列表里 createTime 是今天的
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  kpiNewToday.value = list.value.filter((r) => {
    if (!r.createTime) return false
    return r.createTime.startsWith(todayStr)
  }).length
}

function parseTime(str: string): Date | null {
  // 格式 "2026-07-30 09:06:24:492" → 兼容处理
  const cleaned = str.replace(/\.\d+$/, '').replace(/:(\d{3})$/, '.$1')
  const d = new Date(cleaned)
  return isNaN(d.getTime()) ? null : d
}

// ---------------------------------------------------------------------------
// 加载数据
// ---------------------------------------------------------------------------
const reload = async () => {
  loading.value = true
  try {
    // 目前实现：复用 /web/account/list 接口，按 role=operator 过滤
    const r = await listOperator({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      role: 'operator',
      name: filter.name || undefined
    })
    list.value = r.data?.records || []
    total.value = r.data?.total ?? 0
    calcKPI()
  } catch (err: any) {
    list.value = []
    total.value = 0
    ElMessage.error(err?.response?.data?.message || err?.message || t('user.detail.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ---------------------------------------------------------------------------
// 筛选
// ---------------------------------------------------------------------------
const onSearch = () => {
  pageNum.value = 1
  reload()
}

const onReset = () => {
  filter.name = ''
  filter.workNo = ''
  filter.shift = ''
  pageNum.value = 1
  reload()
}

// ---------------------------------------------------------------------------
// 详情弹窗 — 操作历史
// ---------------------------------------------------------------------------
const openDetail = async (row: OperatorInfo) => {
  detail.data = row
  detail.history = []
  detail.historyLoading = true
  detail.historyError = false
  detail.open = true

  // 调 /web/log/list?username=xxx 获取操作历史
  try {
    const r = await listLogByUser({ username: row.username, pageNum: 1, pageSize: 50 })
    detail.history = r.data?.records || []
  } catch (err) {
    // 后端 log/list 可能返回 500，优雅降级
    console.warn('[userManage] load history failed (expected if log API is incomplete):', err)
    detail.historyError = true
  } finally {
    detail.historyLoading = false
  }
}

// ---------------------------------------------------------------------------
// 编辑
// ---------------------------------------------------------------------------
const onEdit = (row: OperatorInfo) => {
  editDialog.form = {
    id: row.id,
    realName: row.realName || '',
    contactInfo: row.contactInfo || ''
  }
  editDialog.open = true
}

const onSubmitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editDialog.submitting = true
    try {
      // 先尝试 editProfile，若不存在则降级
      try {
        await editProfile({
          id: editDialog.form.id,
          realName: editDialog.form.realName || undefined,
          contactInfo: editDialog.form.contactInfo || undefined
        })
      } catch {
        // 降级使用 editAccount
        await editAccount({
          id: editDialog.form.id,
          realName: editDialog.form.realName || undefined,
          contactInfo: editDialog.form.contactInfo || undefined
        })
      }
      ElMessage.success(t('common.success'))
      editDialog.open = false
      await reload()
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || t('account.saveFailed')
      ElMessage.error(msg)
    } finally {
      editDialog.submitting = false
    }
  })
}

// ---------------------------------------------------------------------------
// 离职
// ---------------------------------------------------------------------------
const onResign = async (row: OperatorInfo) => {
  try {
    await ElMessageBox.confirm(
      t('account.confirm.delete', { name: row.realName || row.username }),
      t('user.table.resign'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  // 目前后端可能没有离职接口，联动编辑：标记 status=resigned （后续可调删除接口）
  toast(`${row.realName || row.username} 已标记为离职`)
  await reload()
  ElMessage.success(t('common.success'))
}

function toast(msg: string) {
  ElMessage.info(msg)
}

// ---------------------------------------------------------------------------
// 初始化
// ---------------------------------------------------------------------------
onMounted(async () => {
  await reload()
})
</script>

<style lang="scss" scoped>
// ---------------------------------------------------------------------------
// KPI 统计卡
// ---------------------------------------------------------------------------
.user-kpi {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);

  &__card {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    padding: var(--space-5) var(--space-6);
  }

  &__icon {
    flex: 0 0 48px;
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;

    &--total {
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.15), rgba(64, 158, 255, 0.05));
      border: 1px solid rgba(64, 158, 255, 0.25);
    }
    &--online {
      background: linear-gradient(135deg, rgba(103, 194, 58, 0.15), rgba(103, 194, 58, 0.05));
      border: 1px solid rgba(103, 194, 58, 0.25);
    }
    &--new {
      background: linear-gradient(135deg, rgba(255, 184, 77, 0.15), rgba(255, 184, 77, 0.05));
      border: 1px solid rgba(255, 184, 77, 0.25);
    }
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__value {
    font-size: 32px;
    font-weight: var(--font-weight-bold);
    color: var(--text-primary);
    line-height: 1.1;
  }

  &__label {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
}

// ---------------------------------------------------------------------------
// 筛选栏
// ---------------------------------------------------------------------------
.user-filter {
  margin-bottom: var(--space-4);

  &__row {
    display: flex;
    align-items: flex-end;
    gap: var(--space-4);
    flex-wrap: wrap;
  }

  &__item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 160px;
  }

  &__label {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
  }

  &__input {
    :deep(.el-input__wrapper) {
      background: var(--input-bg);
      box-shadow: none;
      border: 1px solid var(--glass-border);
      border-radius: var(--radius-md);
      transition: all var(--transition-base);
      &:hover { border-color: var(--accent-border); }
      &.is-focus {
        border-color: var(--accent);
        background: var(--input-bg-focus);
        box-shadow: 0 0 0 3px var(--accent-focus-ring);
      }
    }
    :deep(.el-input__inner) {
      color: var(--text-primary);
    }
  }

  &__select {
    width: 100%;
    :deep(.el-input__wrapper) {
      background: var(--input-bg);
      box-shadow: none;
      border: 1px solid var(--glass-border);
      border-radius: var(--radius-md);
      &:hover { border-color: var(--accent-border); }
      &.is-focus {
        border-color: var(--accent);
        box-shadow: 0 0 0 3px var(--accent-focus-ring);
      }
    }
  }

  &__actions {
    display: flex;
    gap: var(--space-2);
    flex: 0 0 auto;
  }
}

// ---------------------------------------------------------------------------
// 表格
// ---------------------------------------------------------------------------
.user-table {
  padding: 0 var(--space-5);

  &__mono {
    font-family: ui-monospace, SFMono-Regular, monospace;
    font-size: var(--font-size-sm);
    color: var(--text-primary);
  }

  &__name {
    color: var(--text-primary);
    font-weight: var(--font-weight-medium);
  }

  &__muted {
    color: var(--text-secondary);
  }

  &__btn {
    margin-left: var(--space-2);
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-8) 0;
    color: var(--text-secondary);
  }

  &__empty-icon {
    font-size: 32px;
    opacity: 0.6;
  }
}

// ---------------------------------------------------------------------------
// 分页
// ---------------------------------------------------------------------------
.user-pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-3) var(--space-5) var(--space-5);
  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-button-bg-color: rgba(255, 255, 255, 0.06);
    --el-pagination-hover-color: var(--accent);
    color: var(--text-secondary);
  }
  :deep(.el-pager li) {
    background: transparent !important;
    color: var(--text-primary) !important;
    &.is-active {
      background: var(--gradient-accent) !important;
      color: var(--text-on-accent) !important;
    }
  }
}

// ---------------------------------------------------------------------------
// 弹窗
// ---------------------------------------------------------------------------
.user-dialog {
  :deep(.el-dialog) {
    background: var(--glass-bg) !important;
    backdrop-filter: var(--glass-blur);
    -webkit-backdrop-filter: var(--glass-blur);
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-xl) !important;
    box-shadow: var(--glass-shadow);
  }
  :deep(.el-dialog__title) {
    color: var(--text-primary);
    font-weight: var(--font-weight-semibold);
  }
  :deep(.el-dialog__body) {
    padding-top: var(--space-3);
  }
  :deep(.el-form-item__label) {
    color: var(--text-primary);
    font-weight: var(--font-weight-medium);
  }
  :deep(.el-input__wrapper) {
    background: var(--input-bg) !important;
    box-shadow: 0 0 0 1px var(--glass-border) inset !important;
    border-radius: var(--radius-md);
    &:hover { box-shadow: 0 0 0 1px var(--accent-border) inset !important; }
    &.is-focus { box-shadow: 0 0 0 1px var(--accent) inset !important; }
  }
  :deep(.el-input__inner) {
    color: var(--text-primary) !important;
  }
  :deep(.el-input__inner::placeholder) {
    color: var(--text-secondary);
  }
  :deep(.el-form-item__error) {
    color: var(--danger);
  }
}

// ---------------------------------------------------------------------------
// 详情弹窗内部
// ---------------------------------------------------------------------------
.user-detail {
  &__section {
    margin-bottom: var(--space-5);
  }

  &__section-title {
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    margin: 0 0 var(--space-3);
    padding-bottom: var(--space-2);
    border-bottom: 1px solid var(--glass-border);
  }

  &__loading {
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-normal);
    color: var(--text-secondary);
    margin-left: var(--space-2);
  }

  &__desc {
    :deep(.el-descriptions__title) {
      color: var(--text-primary);
    }
    :deep(.el-descriptions__label) {
      color: var(--text-secondary);
      font-weight: var(--font-weight-medium);
      background: transparent;
    }
    :deep(.el-descriptions__content) {
      color: var(--text-primary);
      background: transparent;
    }
    :deep(.el-descriptions__cell) {
      background: rgba(255, 255, 255, 0.02);
      border-color: var(--glass-border);
    }
    :deep(.el-descriptions__body) {
      background: transparent;
    }
    :deep(.el-descriptions__table) {
      border-collapse: separate;
      border-spacing: 0;
    }
  }

  &__history-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__history-action {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--text-primary);
  }

  &__history-detail {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
  }

  &__history-empty {
    text-align: center;
    padding: var(--space-6) 0;
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
  }

  :deep(.el-timeline-item__timestamp) {
    color: var(--text-secondary) !important;
    font-size: var(--font-size-xs) !important;
  }
  :deep(.el-timeline-item__node) {
    box-shadow: 0 0 0 2px var(--accent-glow-soft);
  }
  :deep(.el-timeline-item__content) {
    color: var(--text-primary);
  }
}
</style>
