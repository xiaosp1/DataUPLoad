<!--
  W-FRONT-02-E5 系统配置业务页
  - 3 个 Tab：系统参数（system_config 表）/ 线别配置（line 表）/ 缺陷类型映射（line_defect_type 表）
  - 后端真实路由：
      GET/PUT /web/system-config
      GET/POST/PUT/DELETE /web/line (list)
      GET/POST/PUT/DELETE /web/defect/line-type (list)
  - brief 给的字段（报警保留天数/同步间隔/大屏刷新/默认语言 等）未对齐后端真实字段，按
    后端 SystemConfigPO 字段渲染（4 条 sound 相关配置）。
-->
<template>
  <GlassPage
    :title="$t('config.title')"
    :subtitle="$t('config.subtitle')"
  >
    <template #actions>
      <GlassButton variant="default" @click="refreshAll">
        {{ $t('config.action.refresh') }}
      </GlassButton>
    </template>

    <GlassCard :padding="0">
      <el-tabs
        v-model="activeTab"
        class="config-tabs"
        :class="`config-tabs--${theme}`"
      >
        <!-- ============================================================ -->
        <!-- Tab 1: 系统参数 -->
        <!-- ============================================================ -->
        <el-tab-pane :label="$t('config.tab.system')" name="system">
          <div class="config-tab-pane">
            <div v-if="configLoading" class="config-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>{{ $t('app.loading') }}</span>
            </div>

            <el-form
              v-else
              ref="systemFormRef"
              :model="systemForm"
              :rules="systemRules"
              label-position="left"
              label-width="220px"
              class="config-form"
            >
              <el-form-item
                :label="$t('config.form.alarmSoundDevice')"
                prop="device_alarm_sound_uri"
              >
                <el-input
                  v-model="systemForm.device_alarm_sound_uri"
                  :placeholder="$t('config.form.uploadTip')"
                  clearable
                />
              </el-form-item>

              <el-form-item
                :label="$t('config.form.alarmSoundDefect')"
                prop="defect_alarm_sound_uri"
              >
                <el-input
                  v-model="systemForm.defect_alarm_sound_uri"
                  :placeholder="$t('config.form.uploadTip')"
                  clearable
                />
              </el-form-item>

              <el-form-item
                :label="$t('config.form.alarmSoundSystem')"
                prop="system_alarm_sound_uri"
              >
                <el-input
                  v-model="systemForm.system_alarm_sound_uri"
                  :placeholder="$t('config.form.uploadTip')"
                  clearable
                />
              </el-form-item>

              <el-form-item
                :label="$t('config.form.soundPlayCount')"
                prop="sound_play_count"
              >
                <el-input-number
                  v-model="systemForm.sound_play_count"
                  :min="1"
                  :max="10"
                  :step="1"
                  controls-position="right"
                />
                <span class="config-form__hint">
                  {{ $t('config.form.playCountTip') }}
                </span>
              </el-form-item>

              <el-form-item>
                <GlassButton
                  variant="primary"
                  :loading="configSaving"
                  @click="saveSystemConfig"
                >
                  {{ configSaving ? $t('config.form.saving') : $t('config.form.save') }}
                </GlassButton>
                <GlassButton
                  variant="default"
                  style="margin-left: 12px"
                  @click="reloadSystemConfig"
                >
                  {{ $t('common.cancel') }}
                </GlassButton>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============================================================ -->
        <!-- Tab 2: 线别配置 -->
        <!-- ============================================================ -->
        <el-tab-pane :label="$t('config.tab.line')" name="line">
          <div class="config-tab-pane">
            <div class="config-toolbar">
              <div class="config-toolbar__title">
                {{ $t('config.line.title') }}
                <el-tag size="small" type="info" effect="plain">
                  {{ lineList.length }}
                </el-tag>
              </div>
              <GlassButton variant="primary" @click="openLineDialog()">
                <el-icon style="margin-right: 4px"><Plus /></el-icon>
                {{ $t('config.line.add') }}
              </GlassButton>
            </div>

            <GlassTable
              v-loading="lineLoading"
              :data="lineList"
              :empty-text="$t('config.line.empty')"
              stripe
              style="width: 100%"
            >
              <el-table-column type="index" :index="indexMethod" width="60" />
              <el-table-column
                :label="$t('config.line.name')"
                prop="name"
                min-width="160"
              />
              <el-table-column
                :label="$t('config.line.code')"
                prop="lineNo"
                width="120"
              />
              <el-table-column
                :label="$t('config.line.faceNo')"
                prop="faceNo"
                width="100"
              />
              <el-table-column
                :label="$t('config.line.color')"
                prop="color"
                width="100"
              >
                <template #default="{ row }">
                  <span
                    class="color-dot"
                    :style="{ background: row.color || '#888' }"
                  />
                  <span style="margin-left: 6px">{{ row.color || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('common.updateTime')"
                prop="updateTime"
                width="180"
              />
              <el-table-column
                :label="$t('common.action')"
                width="180"
                fixed="right"
              >
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    @click="openLineDialog(row)"
                  >
                    {{ $t('config.action.edit') }}
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    @click="confirmDeleteLine(row)"
                  >
                    {{ $t('config.action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </GlassTable>
          </div>
        </el-tab-pane>

        <!-- ============================================================ -->
        <!-- Tab 3: 缺陷类型映射 -->
        <!-- ============================================================ -->
        <el-tab-pane :label="$t('config.tab.defectType')" name="defectType">
          <div class="config-tab-pane">
            <div class="config-toolbar">
              <div class="config-toolbar__title">
                {{ $t('config.defectType.title') }}
                <el-tag size="small" type="info" effect="plain">
                  {{ defectTypeList.length }}
                </el-tag>
              </div>
              <GlassButton variant="primary" @click="openDefectDialog()">
                <el-icon style="margin-right: 4px"><Plus /></el-icon>
                {{ $t('config.defectType.add') }}
              </GlassButton>
            </div>

            <GlassTable
              v-loading="defectLoading"
              :data="defectTypeList"
              :empty-text="$t('config.defectType.empty')"
              stripe
              style="width: 100%"
            >
              <el-table-column type="index" :index="indexMethod" width="60" />
              <el-table-column
                :label="$t('config.defectType.name')"
                prop="name"
                min-width="160"
              />
              <el-table-column
                :label="$t('config.defectType.line')"
                width="180"
              >
                <template #default="{ row }">
                  {{ row.lineNo }} / {{ row.faceNo }}
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('config.defectType.level')"
                width="120"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="row.showFlag === 1 ? 'success' : 'info'"
                    effect="plain"
                    size="small"
                  >
                    {{ row.showFlag === 1
                      ? $t('config.defectType.enabled')
                      : $t('config.defectType.disabled') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('common.updateTime')"
                prop="updateTime"
                width="180"
              />
              <el-table-column
                :label="$t('common.action')"
                width="180"
                fixed="right"
              >
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    @click="openDefectDialog(row)"
                  >
                    {{ $t('config.action.edit') }}
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    @click="confirmDeleteDefect(row)"
                  >
                    {{ $t('config.action.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </GlassTable>
          </div>
        </el-tab-pane>
      </el-tabs>
    </GlassCard>

    <!-- ============================================================== -->
    <!-- 线别 新增 / 编辑 弹窗 -->
    <!-- ============================================================== -->
    <el-dialog
      v-model="lineDialogVisible"
      :title="lineDialogMode === 'add'
        ? $t('config.line.addTitle')
        : $t('config.line.editTitle')"
      width="520"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="lineFormRef"
        :model="lineForm"
        :rules="lineRules"
        label-width="120px"
      >
        <el-form-item :label="$t('config.line.name')" prop="name">
          <el-input v-model="lineForm.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('config.line.code')" prop="lineNo">
          <el-input v-model="lineForm.lineNo" maxlength="20" />
        </el-form-item>
        <el-form-item :label="$t('config.line.faceNo')" prop="faceNo">
          <el-input v-model="lineForm.faceNo" maxlength="20" />
        </el-form-item>
        <el-form-item :label="$t('config.line.color')" prop="color">
          <el-color-picker v-model="lineForm.color" />
          <el-input
            v-model="lineForm.color"
            maxlength="16"
            style="width: 160px; margin-left: 12px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <GlassButton variant="default" @click="lineDialogVisible = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton
          variant="primary"
          :loading="lineSubmitting"
          style="margin-left: 8px"
          @click="submitLineForm"
        >
          {{ $t('common.save') }}
        </GlassButton>
      </template>
    </el-dialog>

    <!-- ============================================================== -->
    <!-- 缺陷类型 新增 / 编辑 弹窗 -->
    <!-- ============================================================== -->
    <el-dialog
      v-model="defectDialogVisible"
      :title="defectDialogMode === 'add'
        ? $t('config.defectType.addTitle')
        : $t('config.defectType.editTitle')"
      width="540"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="defectFormRef"
        :model="defectForm"
        :rules="defectRules"
        label-width="140px"
      >
        <el-form-item :label="$t('config.defectType.name')" prop="name">
          <el-input v-model="defectForm.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('config.defectType.line')" prop="lineNo">
          <el-select
            v-model="defectForm.lineNo"
            placeholder="LineNo"
            filterable
            style="width: 45%"
            @change="onDefectLineChange"
          >
            <el-option
              v-for="ln in uniqueLineNos"
              :key="ln"
              :label="ln"
              :value="ln"
            />
          </el-select>
          <el-select
            v-model="defectForm.faceNo"
            placeholder="FaceNo"
            filterable
            style="width: 45%; margin-left: 4%"
          >
            <el-option
              v-for="fn in faceNosForLine(defectForm.lineNo)"
              :key="fn"
              :label="fn"
              :value="fn"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('config.defectType.level')" prop="showFlag">
          <el-switch
            v-model="defectForm.showFlagBool"
            :active-text="$t('config.defectType.enabled')"
            :inactive-text="$t('config.defectType.disabled')"
            inline-prompt
            @change="onDefectFlagChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <GlassButton variant="default" @click="defectDialogVisible = false">
          {{ $t('common.cancel') }}
        </GlassButton>
        <GlassButton
          variant="primary"
          :loading="defectSubmitting"
          style="margin-left: 8px"
          @click="submitDefectForm"
        >
          {{ $t('common.save') }}
        </GlassButton>
      </template>
    </el-dialog>
  </GlassPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Loading, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

import GlassPage from '../components/GlassPage.vue'
import GlassCard from '../components/GlassCard.vue'
import GlassTable from '../components/GlassTable.vue'
import GlassButton from '../components/GlassButton.vue'

import {
  listSystemConfig,
  updateSystemConfig,
  listLine,
  addLine,
  editLine,
  deleteLine,
  listLineDefectType,
  addLineDefectType,
  editLineDefectType,
  deleteLineDefectType,
  type SystemConfigItem,
  type LineItem,
  type LineDefectTypeItem
} from '../api/systemConfig'

const { t } = useI18n()

// ---------------------------------------------------------------------------
// 主题适配（继承全局 dark/light，CSS 变量已配）
// ---------------------------------------------------------------------------
const theme = computed(() =>
  document.documentElement.classList.contains('theme-light') ? 'light' : 'dark'
)

// ---------------------------------------------------------------------------
// Tab 切换
// ---------------------------------------------------------------------------
const activeTab = ref<'system' | 'line' | 'defectType'>('system')

// ---------------------------------------------------------------------------
// Tab 1: 系统参数
// ---------------------------------------------------------------------------
interface SystemForm {
  device_alarm_sound_uri: string
  defect_alarm_sound_uri: string
  system_alarm_sound_uri: string
  sound_play_count: number
}

const systemForm = reactive<SystemForm>({
  device_alarm_sound_uri: '',
  defect_alarm_sound_uri: '',
  system_alarm_sound_uri: '',
  sound_play_count: 1
})

const configLoading = ref(false)
const configSaving = ref(false)
const configList = ref<SystemConfigItem[]>([])
const systemFormRef = ref<FormInstance>()

const systemRules = {
  device_alarm_sound_uri: [
    { required: true, message: t('config.form.alarmSoundDevice'), trigger: 'blur' }
  ],
  defect_alarm_sound_uri: [
    { required: true, message: t('config.form.alarmSoundDefect'), trigger: 'blur' }
  ],
  system_alarm_sound_uri: [
    { required: true, message: t('config.form.alarmSoundSystem'), trigger: 'blur' }
  ],
  sound_play_count: [
    { required: true, message: t('config.form.soundPlayCount'), trigger: 'blur' }
  ]
}

async function loadSystemConfig() {
  configLoading.value = true
  try {
    const resp = await listSystemConfig()
    if (resp.success) {
      configList.value = Array.isArray(resp.data) ? resp.data : []
      configList.value.forEach((c) => {
        switch (c.configKey) {
          case 'device_alarm_sound_uri':
            systemForm.device_alarm_sound_uri = c.configValue
            break
          case 'defect_alarm_sound_uri':
            systemForm.defect_alarm_sound_uri = c.configValue
            break
          case 'system_alarm_sound_uri':
            systemForm.system_alarm_sound_uri = c.configValue
            break
          case 'sound_play_count':
            systemForm.sound_play_count = Number(c.configValue) || 1
            break
        }
      })
      if (configList.value.length === 0) {
        ElMessage.info(t('config.form.loadEmpty'))
      } else {
        ElMessage.success(
          t('config.form.loadOk', { n: configList.value.length })
        )
      }
    } else {
      ElMessage.error(resp.msg || resp.message || t('config.form.saveFail'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('config.form.saveFail'))
  } finally {
    configLoading.value = false
  }
}

async function saveSystemConfig() {
  if (!systemFormRef.value) return
  const valid = await systemFormRef.value.validate().catch(() => false)
  if (!valid) return

  configSaving.value = true
  try {
    // 把当前表单值合回原 list（保留 id / configName / 时间戳），对缺失 key 补全
    const formMap: Record<string, string> = {
      device_alarm_sound_uri: systemForm.device_alarm_sound_uri,
      defect_alarm_sound_uri: systemForm.defect_alarm_sound_uri,
      system_alarm_sound_uri: systemForm.system_alarm_sound_uri,
      sound_play_count: String(systemForm.sound_play_count)
    }
    const merged = configList.value.map((c) => ({
      ...c,
      configValue: formMap[c.configKey] ?? c.configValue
    }))
    // 后端 @NotEmpty 要求至少 1 条
    const resp = await updateSystemConfig(
      merged.length > 0 ? merged : (Object.entries(formMap).map(([k, v]) => ({
        configKey: k,
        configValue: v
      })) as SystemConfigItem[])
    )
    if (resp.success) {
      ElMessage.success(t('config.form.saveOk'))
      // 立刻重拉，验证持久化
      await loadSystemConfig()
    } else {
      ElMessage.error(resp.msg || resp.message || t('config.form.saveFail'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('config.form.saveFail'))
  } finally {
    configSaving.value = false
  }
}

function reloadSystemConfig() {
  loadSystemConfig()
}

// ---------------------------------------------------------------------------
// Tab 2: 线别
// ---------------------------------------------------------------------------
const lineList = ref<LineItem[]>([])
const lineLoading = ref(false)

async function loadLine() {
  lineLoading.value = true
  try {
    const resp = await listLine()
    if (resp.success) {
      lineList.value = Array.isArray(resp.data) ? resp.data : []
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  } finally {
    lineLoading.value = false
  }
}

const lineDialogVisible = ref(false)
const lineDialogMode = ref<'add' | 'edit'>('add')
const lineFormRef = ref<FormInstance>()
const lineSubmitting = ref(false)
const lineForm = reactive<{
  id?: number
  name: string
  lineNo: string
  faceNo: string
  color: string
}>({
  id: undefined,
  name: '',
  lineNo: '',
  faceNo: '',
  color: '#5CE1FF'
})

const lineRules = {
  name: [{ required: true, message: t('config.line.name'), trigger: 'blur' }],
  lineNo: [{ required: true, message: t('config.line.code'), trigger: 'blur' }],
  faceNo: [{ required: true, message: t('config.line.faceNo'), trigger: 'blur' }]
}

function openLineDialog(row?: LineItem) {
  if (row) {
    lineDialogMode.value = 'edit'
    Object.assign(lineForm, {
      id: row.id,
      name: row.name || '',
      lineNo: row.lineNo || '',
      faceNo: row.faceNo || '',
      color: row.color || '#5CE1FF'
    })
  } else {
    lineDialogMode.value = 'add'
    Object.assign(lineForm, {
      id: undefined,
      name: '',
      lineNo: '',
      faceNo: '',
      color: '#5CE1FF'
    })
  }
  lineDialogVisible.value = true
}

async function submitLineForm() {
  if (!lineFormRef.value) return
  const valid = await lineFormRef.value.validate().catch(() => false)
  if (!valid) return

  lineSubmitting.value = true
  try {
    let resp
    if (lineDialogMode.value === 'add') {
      resp = await addLine({
        name: lineForm.name,
        lineNo: lineForm.lineNo,
        faceNo: lineForm.faceNo,
        color: lineForm.color
      })
    } else {
      resp = await editLine({
        id: lineForm.id!,
        name: lineForm.name,
        lineNo: lineForm.lineNo,
        faceNo: lineForm.faceNo,
        color: lineForm.color
      })
    }
    if (resp.success) {
      ElMessage.success(
        lineDialogMode.value === 'add'
          ? t('config.line.createdOk')
          : t('config.line.updatedOk')
      )
      lineDialogVisible.value = false
      await loadLine()
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  } finally {
    lineSubmitting.value = false
  }
}

async function confirmDeleteLine(row: LineItem) {
  try {
    await ElMessageBox.confirm(
      t('config.line.confirmDelete', { name: row.name }),
      t('common.confirm'),
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
    const resp = await deleteLine(row.id)
    if (resp.success) {
      ElMessage.success(t('config.line.deletedOk'))
      await loadLine()
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  }
}

// ---------------------------------------------------------------------------
// Tab 3: 缺陷类型映射
// ---------------------------------------------------------------------------
const defectTypeList = ref<LineDefectTypeItem[]>([])
const defectLoading = ref(false)

async function loadDefectTypes() {
  defectLoading.value = true
  try {
    const resp = await listLineDefectType()
    if (resp.success) {
      defectTypeList.value = Array.isArray(resp.data) ? resp.data : []
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  } finally {
    defectLoading.value = false
  }
}

const defectDialogVisible = ref(false)
const defectDialogMode = ref<'add' | 'edit'>('add')
const defectFormRef = ref<FormInstance>()
const defectSubmitting = ref(false)
const defectForm = reactive<{
  id?: number
  name: string
  showFlag: number
  showFlagBool: boolean
  lineNo: string
  faceNo: string
}>({
  id: undefined,
  name: '',
  showFlag: 1,
  showFlagBool: true,
  lineNo: '',
  faceNo: ''
})

const defectRules = {
  name: [{ required: true, message: t('config.defectType.name'), trigger: 'blur' }],
  lineNo: [{ required: true, message: t('config.defectType.line'), trigger: 'change' }],
  faceNo: [{ required: true, message: t('config.defectType.line'), trigger: 'change' }]
}

const uniqueLineNos = computed(() =>
  Array.from(new Set(lineList.value.map((l) => l.lineNo).filter(Boolean)))
)

function faceNosForLine(lineNo: string) {
  if (!lineNo) return []
  return Array.from(
    new Set(
      lineList.value
        .filter((l) => l.lineNo === lineNo)
        .map((l) => l.faceNo)
        .filter(Boolean)
    )
  )
}

function onDefectLineChange(val: string) {
  // 切换 lineNo 时，如果当前 faceNo 不在新列表里，清空
  const faces = faceNosForLine(val)
  if (defectForm.faceNo && !faces.includes(defectForm.faceNo)) {
    defectForm.faceNo = ''
  }
}

function onDefectFlagChange(val: boolean | string | number) {
  defectForm.showFlag = val ? 1 : 0
}

function openDefectDialog(row?: LineDefectTypeItem) {
  if (row) {
    defectDialogMode.value = 'edit'
    Object.assign(defectForm, {
      id: row.id,
      name: row.name || '',
      showFlag: row.showFlag ?? 1,
      showFlagBool: row.showFlag === 1,
      lineNo: row.lineNo || '',
      faceNo: row.faceNo || ''
    })
  } else {
    defectDialogMode.value = 'add'
    const firstLine = lineList.value[0]
    Object.assign(defectForm, {
      id: undefined,
      name: '',
      showFlag: 1,
      showFlagBool: true,
      lineNo: firstLine?.lineNo || '',
      faceNo: firstLine?.faceNo || ''
    })
  }
  defectDialogVisible.value = true
}

async function submitDefectForm() {
  if (!defectFormRef.value) return
  const valid = await defectFormRef.value.validate().catch(() => false)
  if (!valid) return

  defectSubmitting.value = true
  try {
    const payload = {
      name: defectForm.name,
      showFlag: defectForm.showFlag,
      lineNo: defectForm.lineNo,
      faceNo: defectForm.faceNo
    }
    let resp
    if (defectDialogMode.value === 'add') {
      resp = await addLineDefectType(payload)
    } else {
      resp = await editLineDefectType({ id: defectForm.id!, ...payload })
    }
    if (resp.success) {
      ElMessage.success(
        defectDialogMode.value === 'add'
          ? t('config.defectType.createdOk')
          : t('config.defectType.updatedOk')
      )
      defectDialogVisible.value = false
      await loadDefectTypes()
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  } finally {
    defectSubmitting.value = false
  }
}

async function confirmDeleteDefect(row: LineDefectTypeItem) {
  try {
    await ElMessageBox.confirm(
      t('config.defectType.confirmDelete', { name: row.name }),
      t('common.confirm'),
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
    const resp = await deleteLineDefectType(row.id)
    if (resp.success) {
      ElMessage.success(t('config.defectType.deletedOk'))
      await loadDefectTypes()
    } else {
      ElMessage.error(resp.msg || resp.message || t('common.failed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || t('common.failed'))
  }
}

// ---------------------------------------------------------------------------
// 通用
// ---------------------------------------------------------------------------
function indexMethod(index: number) {
  return index + 1
}

async function refreshAll() {
  await Promise.all([loadSystemConfig(), loadLine(), loadDefectTypes()])
}

// 当前 tab 切换时懒加载对应数据
function onTabChange() {
  if (activeTab.value === 'line' && lineList.value.length === 0) {
    loadLine()
  } else if (activeTab.value === 'defectType' && defectTypeList.value.length === 0) {
    loadDefectTypes()
  }
}

onMounted(() => {
  loadSystemConfig()
  // 先把线别拉出来，缺陷类型弹窗的 select 才会有选项
  loadLine()
})
</script>

<style lang="scss" scoped>
.config-tabs {
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

.config-tab-pane {
  padding: 24px 24px 32px;
  min-height: 320px;
}

.config-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--text-secondary);
}

.config-form {
  max-width: 720px;

  &__hint {
    margin-left: 12px;
    color: var(--text-secondary);
    font-size: var(--font-size-sm);
  }
}

.config-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  &__title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
  }
}

.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  vertical-align: middle;
}

// 弹窗内 el-form-item 间距
:deep(.el-dialog .el-form-item) {
  margin-bottom: 18px;
}
</style>
