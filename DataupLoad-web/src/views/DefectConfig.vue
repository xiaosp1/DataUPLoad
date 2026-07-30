<!--
  W-DEFECT-CFG 子单 C — 缺陷配置子 tab（嵌入 Alarm 页面）

  UI 1:1 抄 PSM 老 SPA defectManage.js：
    - 搜索栏：name + category（下拉 1/2/3）
    - 列表：index / name / type / 推送大屏 / 声音报警 / 推送英科 / createTime / 操作（编辑/删除）
    - 新增/编辑弹窗：name / category / alarmEnable / soundEnable（仅 alarmEnable=1 时显示）/ sendYkEnable
    - 玻璃风（GlassPage + GlassCard + GlassButton + GlassTable）

  端点 1:1 对齐后端 /web/defect：
    GET    /web/defect?pageNum=&pageSize=&name=&category=
    POST   /web/defect
    PUT    /web/defect
    DELETE /web/defect?id=

  复用 alarm 子 tab 时直接 <DefectConfig /> 即可（不需要 el-tabs 嵌套，
  Alarm.vue 自己控制 el-tabs，子页面只关心业务）。
-->
<template>
  <div class="defect-config">
    <!-- 搜索栏 -->
    <GlassCard>
      <div class="defect-filter">
        <div class="defect-filter__row">
          <div class="defect-filter__field">
            <label class="defect-filter__label">{{ $t('defectConfig.search.name') }}</label>
            <el-input
              v-model="filter.name"
              clearable
              class="defect-filter__control"
              :placeholder="$t('defectConfig.search.namePlaceholder')"
              @keyup.enter="onQuery"
              @clear="onQuery"
            />
          </div>

          <div class="defect-filter__field">
            <label class="defect-filter__label">{{ $t('defectConfig.search.category') }}</label>
            <el-select
              v-model="filter.category"
              clearable
              class="defect-filter__control"
              :placeholder="$t('defectConfig.search.categoryPlaceholder')"
              @change="onQuery"
              @clear="onQuery"
            >
              <el-option :label="$t('defectConfig.category.defect')" :value="1" />
              <el-option :label="$t('defectConfig.category.system')" :value="2" />
              <el-option :label="$t('defectConfig.category.device')" :value="3" />
            </el-select>
          </div>

          <div class="defect-filter__field defect-filter__field--actions">
            <GlassButton variant="primary" @click="onQuery">
              {{ $t('common.search') }}
            </GlassButton>
            <GlassButton variant="default" @click="onReset">
              {{ $t('common.reset') }}
            </GlassButton>
          </div>
        </div>
      </div>
    </GlassCard>

    <!-- 操作按钮 + 表格 -->
    <div class="defect-toolbar">
      <GlassButton variant="primary" @click="openCreate">
        {{ $t('defectConfig.action.add') }}
      </GlassButton>
      <GlassButton variant="default" @click="reload">
        {{ $t('common.refresh') }}
      </GlassButton>
    </div>

    <GlassTable
      :data="rows"
      v-loading="loading"
      element-loading-background="rgba(0,0,0,0.35)"
      :default-sort="{ prop: 'createTime', order: 'descending' }"
    >
      <el-table-column type="index" :label="$t('defectConfig.table.index')" width="64" align="center" />
      <el-table-column :label="$t('defectConfig.table.name')" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column :label="$t('defectConfig.table.type')" width="120" align="center">
        <template #default="{ row }">
          <span class="type-pill" :class="`type-pill--${row.category}`">
            {{ categoryLabel(row.category) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('defectConfig.table.alarmEnable')" width="110" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="row.alarmEnable === 1 ? 'status-pill--yes' : 'status-pill--no'">
            {{ row.alarmEnable === 1 ? $t('common.yes') : $t('common.no') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('defectConfig.table.soundEnable')" width="110" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="row.soundEnable === 1 ? 'status-pill--yes' : 'status-pill--no'">
            {{ row.soundEnable === 1 ? $t('common.yes') : $t('common.no') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('defectConfig.table.sendYkEnable')" width="110" align="center">
        <template #default="{ row }">
          <span class="status-pill" :class="row.sendYkEnable === 1 ? 'status-pill--yes' : 'status-pill--no'">
            {{ row.sendYkEnable === 1 ? $t('common.yes') : $t('common.no') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('defectConfig.table.createTime')"
        prop="createTime"
        width="180"
        sortable
        show-overflow-tooltip
      />
      <el-table-column :label="$t('defectConfig.table.action')" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <GlassButton variant="default" size="small" @click="openEdit(row)">
            {{ $t('common.edit') }}
          </GlassButton>
          <GlassButton
            variant="danger"
            size="small"
            class="action-delete"
            :loading="row._deleting"
            @click="onDelete(row)"
          >
            {{ $t('common.delete') }}
          </GlassButton>
        </template>
      </el-table-column>
      <template #empty>
        <div class="defect-empty">
          <div class="defect-empty__icon">⌖</div>
          <div class="defect-empty__text">{{ $t('defectConfig.list.empty') }}</div>
        </div>
      </template>
    </GlassTable>

    <!-- 分页 -->
    <div class="defect-pagination">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      class="defect-dialog"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="defect-form"
      >
        <el-form-item :label="$t('defectConfig.form.name')" prop="name">
          <el-input
            v-model="form.name"
            maxlength="20"
            show-word-limit
            :placeholder="$t('defectConfig.form.namePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('defectConfig.form.category')" prop="category">
          <el-select v-model="form.category" :placeholder="$t('defectConfig.form.categoryPlaceholder')">
            <el-option :label="$t('defectConfig.category.defect')" :value="1" />
            <el-option :label="$t('defectConfig.category.system')" :value="2" />
            <el-option :label="$t('defectConfig.category.device')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('defectConfig.form.alarmEnable')" prop="alarmEnable">
          <el-switch
            v-model="form.alarmEnable"
            :active-value="1"
            :inactive-value="0"
            inline-prompt
            :active-text="$t('common.yes')"
            :inactive-text="$t('common.no')"
          />
        </el-form-item>
        <el-form-item
          v-if="form.alarmEnable === 1"
          :label="$t('defectConfig.form.soundEnable')"
          prop="soundEnable"
        >
          <el-switch
            v-model="form.soundEnable"
            :active-value="1"
            :inactive-value="0"
            inline-prompt
            :active-text="$t('common.yes')"
            :inactive-text="$t('common.no')"
          />
          <span class="defect-form__hint">{{ $t('defectConfig.form.soundEnableHint') }}</span>
        </el-form-item>
        <el-form-item :label="$t('defectConfig.form.sendYkEnable')" prop="sendYkEnable">
          <el-switch
            v-model="form.sendYkEnable"
            :active-value="1"
            :inactive-value="0"
            inline-prompt
            :active-text="$t('common.yes')"
            :inactive-text="$t('common.no')"
          />
          <span class="defect-form__hint">{{ $t('defectConfig.form.sendYkEnableHint') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <GlassButton variant="default" @click="dialogVisible = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton variant="primary" :loading="submitting" @click="onSubmit">
          {{ $t('common.confirm') }}
        </GlassButton>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-DEFECT-CFG 子单 C — 缺陷配置子 tab 业务实现
//   - 列表 + 搜索 + 分页（name 模糊 + category 精确）
//   - 新增 / 编辑 / 删除（POST / PUT / DELETE /web/defect）
//   - 玻璃风 UI：卡片 / 圆角 / 阴影，跟 E 阶段 Alarm.vue / Defect.vue 一致
//   - 不引新依赖；不跨子单改其他文件
// =============================================================================

import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import GlassCard from '../components/GlassCard.vue'
import GlassButton from '../components/GlassButton.vue'
import GlassTable from '../components/GlassTable.vue'
import {
  listDefect,
  createDefect,
  updateDefect,
  deleteDefect,
  type DefectType,
  type ListDefectParams
} from '../api/defectConfig'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const t$ = (k: string) => (t as any)(k) as string

// ---------------------------------------------------------------------------
// 筛选 + 分页
// ---------------------------------------------------------------------------
const filter = reactive({
  name: '' as string,
  category: null as number | null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// ---------------------------------------------------------------------------
// 表格 + 加载
// ---------------------------------------------------------------------------
const rows = ref<Array<DefectType & { _deleting?: boolean }>>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const params: ListDefectParams = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      name: filter.name || undefined,
      category: filter.category ?? undefined
    }
    const resp = await listDefect(params)
    if (resp && resp.success !== false && resp.data) {
      const pr = resp.data as any
      rows.value = (pr.records || []) as any
      pagination.total = Number(pr.total || 0)
    } else {
      rows.value = []
      pagination.total = 0
      const msg = (resp && (resp.msg || resp.message)) || t$('defectConfig.list.loadFailed')
      ElMessage.warning(String(msg))
    }
  } catch (err: any) {
    console.warn('[defectConfig] fetchList failed', err)
    rows.value = []
    pagination.total = 0
    if (err?.response?.status !== 401) {
      ElMessage.error(t$('defectConfig.list.loadFailed'))
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
  filter.name = ''
  filter.category = null
  pagination.pageNum = 1
  fetchList()
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

// ---------------------------------------------------------------------------
// 类型标签
// ---------------------------------------------------------------------------
function categoryLabel(c: number) {
  if (c === 1) return t$('defectConfig.category.defect')
  if (c === 2) return t$('defectConfig.category.system')
  if (c === 3) return t$('defectConfig.category.device')
  return '—'
}

// ---------------------------------------------------------------------------
// 新增 / 编辑 / 删除
// ---------------------------------------------------------------------------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  category: 1 as number,
  alarmEnable: 1 as number,
  soundEnable: 1 as number,
  sendYkEnable: 1 as number
})

const rules = {
  name: [
    { required: true, message: () => t$('defectConfig.form.nameRequired'), trigger: ['blur', 'change'] },
    { min: 1, max: 20, message: () => t$('defectConfig.form.nameLength'), trigger: ['blur', 'change'] }
  ],
  category: [
    { required: true, message: () => t$('defectConfig.form.categoryRequired'), trigger: 'change' }
  ],
  alarmEnable: [
    { required: true, message: () => t$('defectConfig.form.alarmEnableRequired'), trigger: 'change' }
  ],
  soundEnable: [
    { required: true, message: () => t$('defectConfig.form.soundEnableRequired'), trigger: 'change' }
  ],
  sendYkEnable: [
    { required: true, message: () => t$('defectConfig.form.sendYkEnableRequired'), trigger: 'change' }
  ]
}

const dialogTitle = () => (editingId.value == null
  ? t$('defectConfig.action.add')
  : t$('defectConfig.action.edit'))

function resetForm() {
  form.name = ''
  form.category = 1
  form.alarmEnable = 1
  form.soundEnable = 1
  form.sendYkEnable = 1
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: DefectType) {
  form.name = row.name
  form.category = row.category
  form.alarmEnable = row.alarmEnable
  form.soundEnable = row.soundEnable
  form.sendYkEnable = row.sendYkEnable
  editingId.value = row.id
  dialogVisible.value = true
}

function onDialogClosed() {
  if (formRef.value) {
    formRef.value.resetFields()
    formRef.value.clearValidate()
  }
  resetForm()
}

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    let resp
    if (editingId.value == null) {
      resp = await createDefect({
        name: form.name,
        category: form.category,
        alarmEnable: form.alarmEnable,
        soundEnable: form.soundEnable,
        sendYkEnable: form.sendYkEnable
      })
    } else {
      resp = await updateDefect({
        id: editingId.value,
        name: form.name,
        category: form.category,
        alarmEnable: form.alarmEnable,
        soundEnable: form.soundEnable,
        sendYkEnable: form.sendYkEnable
      })
    }
    const ok = resp && (resp.success === true || (resp as any).code === 0)
    if (ok) {
      ElMessage.success(editingId.value == null
        ? t$('defectConfig.apiMsg.addSuccess')
        : t$('defectConfig.apiMsg.editSuccess'))
      dialogVisible.value = false
      fetchList()
    } else {
      const msg = (resp && (resp.msg || resp.message)) || (editingId.value == null
        ? t$('defectConfig.apiMsg.addFailed')
        : t$('defectConfig.apiMsg.editFailed'))
      ElMessage.error(String(msg))
    }
  } catch (err: any) {
    console.warn('[defectConfig] submit failed', err)
    if (err?.response?.status !== 401) {
      ElMessage.error(editingId.value == null
        ? t$('defectConfig.apiMsg.addFailed')
        : t$('defectConfig.apiMsg.editFailed'))
    }
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: DefectType & { _deleting?: boolean }) {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      t$('defectConfig.confirm.delete'),
      t$('defectConfig.confirm.title'),
      {
        type: 'warning',
        confirmButtonText: t$('common.confirm'),
        cancelButtonText: t$('common.cancel'),
        customClass: 'defect-confirm-box'
      }
    )
  } catch {
    return
  }
  row._deleting = true
  try {
    const resp = await deleteDefect(row.id)
    const ok = resp && (resp.success === true || (resp as any).code === 0)
    if (ok) {
      ElMessage.success(t$('defectConfig.apiMsg.deleteSuccess'))
      fetchList()
    } else {
      const msg = (resp && (resp.msg || resp.message)) || t$('defectConfig.apiMsg.deleteFailed')
      ElMessage.error(String(msg))
    }
  } catch (err: any) {
    console.warn('[defectConfig] delete failed', err)
    if (err?.response?.status !== 401) {
      ElMessage.error(t$('defectConfig.apiMsg.deleteFailed'))
    }
  } finally {
    row._deleting = false
  }
}

// ---------------------------------------------------------------------------
// 暴露给父组件（Alarm.vue 子 tab）触发首次加载
// ---------------------------------------------------------------------------
defineExpose({
  reload,
  fetchList
})

// ---------------------------------------------------------------------------
// 首次加载
// ---------------------------------------------------------------------------
fetchList()
</script>

<style lang="scss" scoped>
// =============================================================================
// 工具栏（新增 / 刷新）
// =============================================================================
.defect-toolbar {
  display: flex;
  gap: var(--space-2);
  margin-top: calc(-1 * var(--space-2));
}

// =============================================================================
// 搜索栏
// =============================================================================
.defect-filter {
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
    min-width: 180px;

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
    width: 200px;
  }
}

:deep(.defect-filter__control .el-input__wrapper),
:deep(.defect-filter__control .el-select__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 0 1px var(--glass-border);
  border-radius: var(--radius-md);
}

:deep(.defect-filter__control .el-input__wrapper:hover),
:deep(.defect-filter__control .el-select__wrapper:hover) {
  box-shadow: inset 0 0 0 1px rgba(92, 225, 255, 0.4);
}

// =============================================================================
// pill 样式
// =============================================================================
.type-pill,
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

.status-pill {
  &--yes { background: rgba(95, 217, 127, 0.16); color: var(--success); border-color: rgba(95, 217, 127, 0.32); }
  &--no { background: rgba(255, 255, 255, 0.06); color: var(--text-secondary); border-color: rgba(255, 255, 255, 0.18); }
}

.action-delete {
  margin-left: var(--space-2);
}

// =============================================================================
// 分页
// =============================================================================
.defect-pagination {
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
.defect-empty {
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
// 表单（弹窗）
// =============================================================================
.defect-form {
  :deep(.el-form-item__label) {
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
    letter-spacing: 0.3px;
    text-transform: uppercase;
  }

  &__hint {
    margin-left: var(--space-2);
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
  }
}

// =============================================================================
// 弹窗玻璃风
// =============================================================================
:deep(.defect-dialog .el-dialog) {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  color: var(--text-primary);
}

:deep(.defect-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}

:deep(.defect-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.defect-dialog .el-dialog__body) {
  color: var(--text-primary);
}
</style>
