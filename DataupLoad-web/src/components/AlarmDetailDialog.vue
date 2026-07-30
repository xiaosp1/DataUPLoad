<!--
  W-RT-9: 报警详情弹窗 (玻璃风 el-dialog)

  复用 Alarm.vue 的 alarm-dialog 玻璃风样式 + alarm-detail 行布局:
    - id / uuid (top)
    - 左边: lineNo + faceNo
    - 中间: time + duration (持续时长 1Hz tick)
    - 右边: type / level (彩色 chip)
    - 底部: defectName / message / image 占位
    - 操作: 忽略 (PUT /web/alarm/ignore) / 处理 (占位) / 关闭

  数据契约:
    props.alarm: AlarmRecord | null
    props.modelValue: boolean (v-model 兼容)
    emits: update:modelValue

  W-RT-9 调用入口:
    - LineDefectGrid emit('defect-click', {hour, val}) → RealTime.vue handleDefectClick
    - 调 getAlarm({lineNo, startTime, endTime, pageSize:1}) → 取第一条 → 打开弹窗
-->
<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="560px"
    class="alarm-dialog"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="onUpdateModelValue"
  >
    <div v-if="alarm" class="alarm-detail">
      <!-- 头部三列: id/uuid | line/face | type/level -->
      <div class="alarm-detail__head">
        <div class="alarm-detail__head-col">
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.id') }}</span>
            <span class="alarm-detail__value">{{ alarm.id }}</span>
          </div>
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.uuid') }}</span>
            <span class="alarm-detail__value alarm-detail__value--mono">{{ alarm.uuid || '—' }}</span>
          </div>
        </div>
        <div class="alarm-detail__head-col">
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.line') }}</span>
            <span class="alarm-detail__value">{{ alarm.lineNo || '—' }}</span>
          </div>
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.face') }}</span>
            <span class="alarm-detail__value">{{ alarm.faceNo || '—' }}</span>
          </div>
        </div>
        <div class="alarm-detail__head-col">
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.type') }}</span>
            <span class="alarm-detail__value">
              <span class="alarm-chip" :data-tone="typeTone(alarm.type)">{{ typeLabel(alarm.type) }}</span>
            </span>
          </div>
          <div class="alarm-detail__row">
            <span class="alarm-detail__label">{{ $t('alarm.detail.level') }}</span>
            <span class="alarm-detail__value">
              <span class="alarm-chip" :data-tone="levelTone(alarm.level)">{{ levelLabel(alarm.level) }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 时间 + 持续时长 -->
      <div class="alarm-detail__row">
        <span class="alarm-detail__label">{{ $t('alarm.detail.triggerTime') }}</span>
        <span class="alarm-detail__value">{{ alarm.time || '—' }}</span>
      </div>
      <div class="alarm-detail__row">
        <span class="alarm-detail__label">{{ $t('alarm.detail.duration') }}</span>
        <span class="alarm-detail__value">{{ formatDuration(durationMs) }}</span>
      </div>

      <!-- 缺陷 + 描述 -->
      <div class="alarm-detail__row">
        <span class="alarm-detail__label">{{ $t('alarm.detail.defect') }}</span>
        <span class="alarm-detail__value">{{ alarm.defectName || '—' }}</span>
      </div>
      <div class="alarm-detail__row">
        <span class="alarm-detail__label">{{ $t('alarm.detail.desc') }}</span>
        <span class="alarm-detail__value">{{ alarm.message || '—' }}</span>
      </div>

      <!-- 图像占位 (后端暂无 PSM 图像端点) -->
      <div class="alarm-detail__row alarm-detail__row--image">
        <span class="alarm-detail__label">{{ $t('alarm.detail.image') }}</span>
        <div class="alarm-detail__image">
          <span class="alarm-detail__image-placeholder">{{ $t('alarm.detail.noImage') }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <GlassButton variant="default" @click="close">
        {{ $t('alarm.detail.close') }}
      </GlassButton>
      <GlassButton
        v-if="alarm && alarm.solve === 2"
        variant="danger"
        :loading="ignoring"
        @click="onIgnore"
      >
        {{ $t('alarm.detail.ignore') }}
      </GlassButton>
      <GlassButton
        v-if="alarm && alarm.solve === 2"
        variant="primary"
        :disabled="true"
        @click="onHandle"
      >
        {{ $t('alarm.detail.handle') }}
      </GlassButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import GlassButton from './GlassButton.vue'
import { ignoreAlarm, type AlarmRecord } from '../api/alarm'

const props = defineProps<{
  /** v-model:visible */
  modelValue: boolean
  /** 报警详情; null = 空状态 */
  alarm: AlarmRecord | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()

const { t } = useI18n()

// ---------------------------------------------------------------------------
// 标题 (复用 alarm.detail.title; 父组件可按需覆盖, 这里固定用现有 i18n)
// ---------------------------------------------------------------------------
const dialogTitle = computed(() => t('realtime.detail.alarmDialogTitle'))

// ---------------------------------------------------------------------------
// 持续时长 (1Hz tick, 复用 Alarm.vue 逻辑)
// ---------------------------------------------------------------------------
const durationMs = ref(0)
let durationTimer: number | null = null

function parseAlarmTime(s: string | undefined): Date {
  if (!s) return new Date()
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/)
  if (m) {
    return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), Number(m[6]))
  }
  return new Date(s)
}

function tickDuration() {
  if (durationTimer) {
    window.clearInterval(durationTimer)
    durationTimer = null
  }
  if (!props.alarm) {
    durationMs.value = 0
    return
  }
  const start = parseAlarmTime(props.alarm.time)
  durationMs.value = Math.max(0, Date.now() - start.getTime())
  durationTimer = window.setInterval(() => {
    if (!props.alarm) return
    durationMs.value = Math.max(0, Date.now() - parseAlarmTime(props.alarm.time).getTime())
  }, 1000)
}

function stopDuration() {
  if (durationTimer) {
    window.clearInterval(durationTimer)
    durationTimer = null
  }
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

// 弹窗打开时启动 tick; 关闭时停
watch(
  () => [props.modelValue, props.alarm?.uuid],
  ([visible]) => {
    if (visible) {
      tickDuration()
    } else {
      stopDuration()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => stopDuration())

// ---------------------------------------------------------------------------
// type / level 彩色 chip
// ---------------------------------------------------------------------------
function typeLabel(type: number | undefined) {
  if (type === 1) return t('alarm.typeOption.defect')
  if (type === 2) return t('alarm.typeOption.system')
  if (type === 3) return t('alarm.typeOption.device')
  return '—'
}

function typeTone(type: number | undefined): 'red' | 'blue' | 'orange' {
  if (type === 1) return 'red'
  if (type === 2) return 'blue'
  if (type === 3) return 'orange'
  return 'blue'
}

function levelLabel(level: number | undefined) {
  if (level === 1) return t('alarm.levelOption.normal')
  if (level === 2) return t('alarm.levelOption.serious')
  return '—'
}

function levelTone(level: number | undefined): 'green' | 'red' {
  if (level === 2) return 'red'
  return 'green'
}

// ---------------------------------------------------------------------------
// 操作: 忽略 / 处理 / 关闭
// ---------------------------------------------------------------------------
const ignoring = ref(false)

async function onIgnore() {
  if (!props.alarm) return
  try {
    await ElMessageBox.confirm(t('alarm.list.ignoreConfirm'), t('alarm.detail.title'), {
      type: 'warning',
      confirmButtonText: t('common.confirm') || t('alarm.detail.ignore'),
      cancelButtonText: t('common.cancel'),
      customClass: 'alarm-confirm-box'
    })
  } catch {
    return
  }
  ignoring.value = true
  try {
    const now = new Date()
    const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`)
    const ignoreTime = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())} ${pad2(
      now.getHours()
    )}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())}`
    const resp = await ignoreAlarm({
      type: props.alarm.type ?? undefined,
      defectName: props.alarm.defectName ?? '',
      lineNo: props.alarm.lineNo ?? '',
      faceNo: props.alarm.faceNo ?? '',
      faceId: props.alarm.faceNo ?? '',
      startTime: props.alarm.time ?? '',
      endTime: props.alarm.time ?? '',
      ignoreTime
    })
    const ok = resp && (resp.success === true || (resp as any).code === 0)
    if (ok) {
      ElMessage.success(t('alarm.list.ignoreSuccess'))
      close()
    } else {
      const msg = (resp && (resp.msg || resp.message)) || t('alarm.list.ignoreFailed')
      ElMessage.error(String(msg))
    }
  } catch (err: any) {
    console.warn('[alarm-detail-dialog] ignore failed:', err)
    if (err?.response?.status !== 401) {
      ElMessage.error(t('alarm.list.ignoreFailed'))
    }
  } finally {
    ignoring.value = false
  }
}

function onHandle() {
  // W-RT-9 占位: 与 Alarm.vue onHandle 一致 (后端链路 E2 已就绪, 前端留待后续子单)
  ElMessage.info(t('alarm.list.handlePending') || 'Handle flow not yet wired')
}

// ---------------------------------------------------------------------------
// v-model helpers
// ---------------------------------------------------------------------------
function onUpdateModelValue(v: boolean) {
  emit('update:modelValue', v)
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style lang="scss" scoped>
.alarm-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  &__head {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--space-4);
    padding-bottom: var(--space-2);
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  &__head-col {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

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

// 彩色 chip (type / level)
.alarm-chip {
  display: inline-flex;
  align-items: center;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
  letter-spacing: 0.4px;
}
.alarm-chip[data-tone='red'] {
  color: var(--danger);
  background: rgba(255, 90, 95, 0.12);
  border-color: rgba(255, 90, 95, 0.35);
}
.alarm-chip[data-tone='green'] {
  color: var(--success);
  background: rgba(95, 217, 127, 0.12);
  border-color: rgba(95, 217, 127, 0.35);
}
.alarm-chip[data-tone='blue'] {
  color: var(--accent);
  background: rgba(92, 225, 255, 0.12);
  border-color: rgba(92, 225, 255, 0.35);
}
.alarm-chip[data-tone='orange'] {
  color: var(--warning);
  background: rgba(255, 183, 77, 0.12);
  border-color: rgba(255, 183, 77, 0.35);
}

// 弹窗头部 / 底部加玻璃风 (复用 Alarm.vue :deep 选择器风格)
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
:deep(.alarm-dialog .el-dialog__footer) {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: var(--space-3);
}
</style>
