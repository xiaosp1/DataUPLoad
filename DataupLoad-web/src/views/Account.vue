<template>
  <GlassPage
    :title="$t('account.title')"
    :subtitle="$t('account.subtitle')"
  >
    <!-- 当前用户卡 -->
    <GlassCard class="account-current">
      <div class="account-current__row">
        <div class="account-current__avatar">
          {{ current?.username?.charAt(0)?.toUpperCase() || '?' }}
        </div>
        <div class="account-current__info">
          <div class="account-current__name">
            <span class="account-current__label">{{ $t('account.current.username') }}</span>
            <span class="account-current__value">{{ current?.username || '-' }}</span>
          </div>
          <div class="account-current__name">
            <span class="account-current__label">{{ $t('account.current.role') }}</span>
            <span class="account-current__value">{{ current ? $t(`role.${current.role}`) : '-' }}</span>
          </div>
          <div class="account-current__name">
            <span class="account-current__label">{{ $t('account.current.permission') }}</span>
            <span class="account-current__perms">
              <el-tag
                v-for="p in (current?.permission || [])"
                :key="p"
                type="info"
                size="small"
                effect="dark"
                class="account-current__tag"
              >
                {{ p }}
              </el-tag>
              <span v-if="!current?.permission?.length" class="account-current__empty">—</span>
            </span>
          </div>
          <div class="account-current__name">
            <span class="account-current__label">{{ $t('account.current.lastLogin') }}</span>
            <span class="account-current__value account-current__value--muted">
              {{ current?.updateTime || '—' }}
            </span>
          </div>
        </div>
        <div class="account-current__actions">
          <GlassButton variant="primary" @click="openPwdDialog">
            🔑 {{ $t('account.current.changePwd') }}
          </GlassButton>
        </div>
      </div>
    </GlassCard>

    <!-- 列表 + 工具栏 -->
    <GlassCard :padding="0">
      <!-- 工具栏 -->
      <div class="account-toolbar">
        <div class="account-toolbar__search">
          <el-input
            v-model="queryName"
            :placeholder="$t('common.search')"
            clearable
            class="account-toolbar__input"
            @keyup.enter="reload"
            @clear="reload"
          >
            <template #prefix>
              <span class="account-toolbar__icon">🔍</span>
            </template>
          </el-input>
        </div>
        <div class="account-toolbar__spacer" />
        <GlassButton variant="default" @click="reload">↻ {{ $t('common.refresh') }}</GlassButton>
        <GlassButton variant="primary" @click="openAddDialog">＋ {{ $t('account.action.add') }}</GlassButton>
      </div>

      <!-- W-PERF-D: 玻璃风骨架屏（首次加载） -->
      <GlassSkeletonTable v-if="loading && list.length === 0" :columns="8" :rows="6" />
      <!-- 表格 -->
      <GlassTable v-else :data="list" v-loading="loading" class="account-table">
        <el-table-column :label="$t('account.table.id')" prop="id" width="64" align="center" />
        <el-table-column :label="$t('account.table.username')" prop="username" min-width="120" />
        <el-table-column :label="$t('account.table.realName')" prop="realName" min-width="100">
          <template #default="{ row }">
            <span class="account-table__muted">{{ row.realName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('account.table.contactInfo')" prop="contactInfo" min-width="140">
          <template #default="{ row }">
            <span class="account-table__muted">{{ row.contactInfo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('account.table.role')" prop="role" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="dark" size="small">
              {{ $t(`role.${row.role}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('account.table.permission')" min-width="180">
          <template #default="{ row }">
            <span class="account-table__perms">
              <el-tag
                v-for="p in (row.permission || [])"
                :key="p"
                size="small"
                effect="plain"
                class="account-table__perm-tag"
              >
                {{ p }}
              </el-tag>
              <span v-if="!row.permission?.length" class="account-table__muted">—</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('account.table.createdAt')" prop="createTime" width="180">
          <template #default="{ row }">
            <span class="account-table__muted">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('account.table.action')" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <GlassButton variant="default" size="small" @click="openEditDialog(row)">
              ✎ {{ $t('account.action.edit') }}
            </GlassButton>
            <GlassButton
              variant="default"
              size="small"
              class="account-table__btn"
              :disabled="row.id === 1"
              @click="onReset(row)"
            >
              🔁 {{ $t('account.action.resetPwd') }}
            </GlassButton>
            <GlassButton
              variant="danger"
              size="small"
              class="account-table__btn"
              :disabled="row.id === 1"
              @click="onDelete(row)"
            >
              🗑 {{ $t('account.action.delete') }}
            </GlassButton>
          </template>
        </el-table-column>

        <template #empty>
          <div class="account-table__empty">
            <span class="account-table__empty-icon">🗂</span>
            <span>{{ $t('account.empty') }}</span>
          </div>
        </template>
      </GlassTable>

      <!-- 分页 -->
      <div class="account-pagination">
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="editDialog.open"
      :title="editDialog.mode === 'add' ? $t('account.add.title') : $t('account.edit.title')"
      width="480"
      :close-on-click-modal="false"
      class="account-dialog"
    >
      <el-form
        ref="editFormRef"
        :model="editDialog.form"
        :rules="editRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item :label="$t('account.form.username')" prop="username">
          <el-input
            v-model="editDialog.form.username"
            :disabled="editDialog.mode === 'edit'"
            :placeholder="$t('account.form.username')"
            maxlength="32"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="$t('account.form.realName')" prop="realName">
          <el-input
            v-model="editDialog.form.realName"
            :placeholder="$t('account.form.realName')"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item :label="$t('account.form.contactInfo')" prop="contactInfo">
          <el-input
            v-model="editDialog.form.contactInfo"
            :placeholder="$t('account.form.contactInfo')"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item :label="$t('account.form.role')" prop="roleId">
          <el-select
            v-model="editDialog.form.roleId"
            :placeholder="$t('account.form.rolePlaceholder')"
            class="account-dialog__select"
          >
            <el-option
              v-for="r in roles"
              :key="r.id"
              :label="$t(`role.${r.role}`) + ' (' + (r.permission?.join(',') || '-') + ')'"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <p v-if="editDialog.mode === 'add'" class="account-dialog__hint">
          {{ $t('account.defaultPwdNote') }}
        </p>
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

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="pwdDialog.open"
      :title="$t('account.pwd.title')"
      width="440"
      :close-on-click-modal="false"
      class="account-dialog"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdDialog.form"
        :rules="pwdRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item :label="$t('account.pwd.old')" prop="oldPassword">
          <el-input
            v-model="pwdDialog.form.oldPassword"
            type="password"
            show-password
            :placeholder="$t('account.pwd.oldPlaceholder')"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item :label="$t('account.pwd.new')" prop="password">
          <el-input
            v-model="pwdDialog.form.password"
            type="password"
            show-password
            :placeholder="$t('account.pwd.newPlaceholder')"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item :label="$t('account.pwd.confirm')" prop="confirmPassword">
          <el-input
            v-model="pwdDialog.form.confirmPassword"
            type="password"
            show-password
            :placeholder="$t('account.pwd.confirmPlaceholder')"
            autocomplete="new-password"
            @keyup.enter="onSubmitPwd"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <GlassButton variant="default" @click="pwdDialog.open = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton
          variant="primary"
          :loading="pwdDialog.submitting"
          @click="onSubmitPwd"
        >
          {{ $t('common.submit') }}
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
import GlassSkeletonTable from '../components/GlassSkeletonTable.vue'
import {
  getCurrent,
  listAccount,
  listRoles,
  addAccount,
  editAccount,
  deleteAccount,
  resetPwd,
  changePwd,
  type AccountInfo,
  type CurrentUser,
  type RoleInfo,
  type AccountBodyDTO,
  type AccountChgDTO
} from '../api/account'

const { t } = useI18n()

// ---------------------------------------------------------------------------
// 状态
// ---------------------------------------------------------------------------
const loading = ref(false)
const list = ref<AccountInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const queryName = ref('')

const current = ref<CurrentUser | null>(null)
const roles = ref<RoleInfo[]>([])

// ---------------------------------------------------------------------------
// 新增 / 编辑弹窗
// ---------------------------------------------------------------------------
const editFormRef = ref<FormInstance>()
const editDialog = reactive({
  open: false,
  mode: 'add' as 'add' | 'edit',
  submitting: false,
  form: {
    id: 0,
    username: '',
    roleId: 0,
    realName: '',
    contactInfo: ''
  } as AccountBodyDTO & { id: number }
})

const editRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('account.form.username'), trigger: 'blur' },
    { min: 2, max: 32, message: '2-32', trigger: 'blur' }
  ],
  roleId: [
    {
      required: true,
      message: t('account.form.rolePlaceholder'),
      trigger: 'change',
      validator: (_r, v, cb) => (v ? cb() : cb(new Error(t('account.form.rolePlaceholder'))))
    }
  ]
}))

const openAddDialog = () => {
  editDialog.mode = 'add'
  editDialog.form = {
    id: 0,
    username: '',
    roleId: roles.value[0]?.id ?? 0,
    realName: '',
    contactInfo: ''
  }
  editDialog.open = true
}

const openEditDialog = (row: AccountInfo) => {
  editDialog.mode = 'edit'
  editDialog.form = {
    id: row.id,
    username: row.username,
    roleId: roles.value.find((r) => r.role === row.role)?.id ?? 0,
    realName: row.realName ?? '',
    contactInfo: row.contactInfo ?? ''
  }
  editDialog.open = true
}

const onSubmitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editDialog.submitting = true
    try {
      if (editDialog.mode === 'add') {
        const body: AccountBodyDTO = {
          username: editDialog.form.username,
          roleId: editDialog.form.roleId,
          realName: editDialog.form.realName || undefined,
          contactInfo: editDialog.form.contactInfo || undefined
        }
        await addAccount(body)
        ElMessage.success(t('common.success'))
      } else {
        const body: AccountChgDTO = {
          id: editDialog.form.id,
          roleId: editDialog.form.roleId,
          realName: editDialog.form.realName || undefined,
          contactInfo: editDialog.form.contactInfo || undefined
        }
        await editAccount(body)
        ElMessage.success(t('common.success'))
      }
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
// 修改密码弹窗
// ---------------------------------------------------------------------------
const pwdFormRef = ref<FormInstance>()
const pwdDialog = reactive({
  open: false,
  submitting: false,
  form: {
    oldPassword: '',
    password: '',
    confirmPassword: ''
  }
})

const pwdRules = computed<FormRules>(() => ({
  oldPassword: [
    { required: true, message: t('account.pwd.old'), trigger: 'blur' },
    { min: 6, message: '≥6', trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('account.pwd.new'), trigger: 'blur' },
    { min: 6, message: '≥6', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('account.pwd.confirm'), trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        v === pwdDialog.form.password ? cb() : cb(new Error(t('account.pwd.mismatch'))),
      trigger: 'blur'
    }
  ]
}))

const openPwdDialog = () => {
  pwdDialog.form = { oldPassword: '', password: '', confirmPassword: '' }
  pwdDialog.open = true
}

const onSubmitPwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdDialog.submitting = true
    try {
      await changePwd(pwdDialog.form.oldPassword, pwdDialog.form.password)
      ElMessage.success(t('account.pwd.success'))
      pwdDialog.open = false
      // 刷新当前用户信息（updateTime 不会变，但保持一致）
      await loadCurrent()
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || t('account.saveFailed')
      // 旧密码错 → 10101（用户名或密码错误）的特殊情况提示更友好
      if (msg.includes('密码') || msg.includes('credential') || msg.includes('10101')) {
        ElMessage.error(t('account.pwd.wrongOld'))
      } else {
        ElMessage.error(msg)
      }
    } finally {
      pwdDialog.submitting = false
    }
  })
}

// ---------------------------------------------------------------------------
// 重置 / 删除
// ---------------------------------------------------------------------------
const onReset = async (row: AccountInfo) => {
  try {
    await ElMessageBox.confirm(
      t('account.confirm.reset', { name: row.username }),
      t('account.action.resetPwd'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  try {
    await resetPwd(row.id)
    ElMessage.success(t('common.success'))
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || t('account.saveFailed'))
  }
}

const onDelete = async (row: AccountInfo) => {
  try {
    await ElMessageBox.confirm(
      t('account.confirm.delete', { name: row.username }),
      t('account.action.delete'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }
  try {
    await deleteAccount(row.id)
    ElMessage.success(t('common.success'))
    await reload()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || t('account.deleteFailed'))
  }
}

// ---------------------------------------------------------------------------
// 加载
// ---------------------------------------------------------------------------
const loadCurrent = async () => {
  try {
    const r = await getCurrent()
    current.value = (r as any).data ?? null
  } catch (err) {
    console.warn('[account] load current failed', err)
  }
}

const loadRoles = async () => {
  try {
    const r = await listRoles()
    roles.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    console.warn('[account] load roles failed', err)
    roles.value = []
  }
}

const reload = async () => {
  loading.value = true
  try {
    const r = await listAccount({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: queryName.value || undefined
    })
    const data = (r as any).data
    list.value = data?.records || []
    total.value = data?.total ?? 0
  } catch (err: any) {
    list.value = []
    total.value = 0
    ElMessage.error(err?.response?.data?.message || err?.message || t('account.loadFailed'))
  } finally {
    loading.value = false
  }
}

const roleTagType = (role: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (role) {
    case 'super_admin':
      return 'danger'
    case 'admin':
      return 'warning'
    case 'operator':
      return 'primary'
    case 'viewer':
      return 'info'
    default:
      return 'info'
  }
}

onMounted(async () => {
  // W-PERF-D: 进入页面立即显示骨架屏，避免中间几秒空白
  loading.value = true
  await loadCurrent()
  await loadRoles()
  await reload()
})
</script>

<style lang="scss" scoped>
// ---------------------------------------------------------------------------
// 当前用户卡
// ---------------------------------------------------------------------------
.account-current {
  &__row {
    display: flex;
    align-items: center;
    gap: var(--space-6);
    flex-wrap: wrap;
  }
  &__avatar {
    flex: 0 0 72px;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: var(--gradient-accent);
    color: var(--text-on-accent);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    font-weight: var(--font-weight-bold);
    box-shadow: 0 4px 16px var(--accent-glow-strong);
  }
  &__info {
    flex: 1;
    min-width: 280px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: var(--space-3) var(--space-6);
  }
  &__name {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  &__label {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.4px;
  }
  &__value {
    font-size: var(--font-size-md);
    color: var(--text-primary);
    font-weight: var(--font-weight-semibold);
    &--muted {
      color: var(--text-secondary);
      font-weight: var(--font-weight-normal);
      font-family: ui-monospace, SFMono-Regular, monospace;
      font-size: var(--font-size-sm);
    }
  }
  &__perms {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  &__empty {
    color: var(--text-secondary);
  }
  &__tag {
    background: rgba(92, 225, 255, 0.18) !important;
    border-color: rgba(92, 225, 255, 0.4) !important;
    color: var(--accent) !important;
  }
  &__actions {
    flex: 0 0 auto;
    display: flex;
    gap: var(--space-3);
  }
}

// ---------------------------------------------------------------------------
// 工具栏
// ---------------------------------------------------------------------------
.account-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-wrap: wrap;
  &__search {
    flex: 0 1 320px;
    min-width: 220px;
  }
  &__input {
    :deep(.el-input__wrapper) {
      background: var(--input-bg);
      box-shadow: none;
      border: 1px solid var(--glass-border);
      border-radius: var(--radius-md);
      transition: all var(--transition-base);
      &:hover {
        border-color: var(--accent-border);
      }
      &.is-focus {
        border-color: var(--accent);
        background: var(--input-bg-focus);
        box-shadow: 0 0 0 3px var(--accent-focus-ring);
      }
    }
    :deep(.el-input__inner) {
      color: var(--text-primary);
    }
    :deep(.el-input__prefix) {
      color: var(--text-secondary);
    }
  }
  &__icon {
    font-size: 14px;
  }
  &__spacer {
    flex: 1;
  }
}

// ---------------------------------------------------------------------------
// 表格
// ---------------------------------------------------------------------------
.account-table {
  padding: 0 var(--space-5);
  &__perms {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  &__perm-tag {
    background: rgba(255, 255, 255, 0.05) !important;
    border-color: var(--glass-border) !important;
    color: var(--text-primary) !important;
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
.account-pagination {
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
// 弹窗内部
// ---------------------------------------------------------------------------
.account-dialog {
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
  :deep(.el-input__wrapper),
  :deep(.el-select .el-input__wrapper) {
    background: var(--input-bg) !important;
    box-shadow: 0 0 0 1px var(--glass-border) inset !important;
    border-radius: var(--radius-md);
    &:hover {
      box-shadow: 0 0 0 1px var(--accent-border) inset !important;
    }
    &.is-focus {
      box-shadow: 0 0 0 1px var(--accent) inset !important;
    }
  }
  :deep(.el-input__inner),
  :deep(.el-select__placeholder) {
    color: var(--text-primary) !important;
  }
  :deep(.el-input__inner::placeholder) {
    color: var(--text-secondary);
  }
  :deep(.el-form-item__error) {
    color: var(--danger);
  }
  &__select {
    width: 100%;
  }
  &__hint {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    margin: 0 0 var(--space-3);
    padding: var(--space-2) var(--space-3);
    background: rgba(255, 184, 77, 0.08);
    border-left: 2px solid var(--warning);
    border-radius: var(--radius-sm);
  }
}
</style>
