<template>
  <div v-if="visible" class="otd-mask" @click.self="close">
    <div class="otd">
      <div class="otd__head">
        <span class="otd__title">{{ $t('occupancy.thresholdTitle') }}</span>
        <button class="otd__x" type="button" @click="close">✕</button>
      </div>

      <div class="otd__body">
        <!-- 实时预览 -->
        <div class="otd__preview">
          <span class="otd__preview-label">{{ $t('occupancy.thresholdPreview') }}</span>
          <div class="otd__samples">
            <div class="otd__sample" :class="toneOf(30)"><span>30</span></div>
            <div class="otd__sample" :class="toneOf(70)"><span>70</span></div>
            <div class="otd__sample" :class="toneOf(85)"><span>85</span></div>
            <div class="otd__sample" :class="toneOf(96)"><span>96</span></div>
            <div class="otd__sample" :class="toneOf(0)"><span>—</span></div>
          </div>
        </div>

        <!-- 黄阈值 -->
        <label class="otd__field">
          <span class="otd__field-label">{{ $t('occupancy.warnThreshold') }}: <b>{{ warn }}</b></span>
          <input
            type="range"
            :min="0"
            :max="100"
            :value="warn"
            @input="onWarn($event)"
          />
        </label>

        <!-- 绿阈值 -->
        <label class="otd__field">
          <span class="otd__field-label">{{ $t('occupancy.goodThreshold') }}: <b>{{ good }}</b></span>
          <input
            type="range"
            :min="0"
            :max="100"
            :value="good"
            @input="onGood($event)"
          />
        </label>

        <!-- 刷新间隔 -->
        <label class="otd__field">
          <span class="otd__field-label">{{ $t('occupancy.refreshInterval') }}: <b>{{ refresh }}s</b></span>
          <input
            type="range"
            :min="1"
            :max="60"
            :value="refresh"
            @input="onRefresh($event)"
          />
        </label>

        <!-- 显示数值默认 -->
        <label class="otd__field otd__field--inline">
          <span>{{ $t('occupancy.barShowValue') }}</span>
          <input
            type="checkbox"
            :checked="showValue"
            @change="onShowValue($event)"
          />
        </label>
      </div>

      <div class="otd__foot">
        <button class="otd__btn otd__btn--ghost" type="button" @click="close">
          {{ $t('common.cancel') }}
        </button>
        <button
          class="otd__btn otd__btn--primary"
          type="button"
          :disabled="saving"
          @click="save"
        >
          {{ saving ? $t('common.saving') : $t('common.save') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-05-B4 上座率阈值配置弹窗
// 读 /web/system-config → 改 4 个 occupancy.* → PUT 整表回写
// 实时预览：左阈值红/黄/绿边界随滑杆变化
// =============================================================================
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  listSystemConfig,
  updateSystemConfig,
  type SystemConfigItem
} from '../api/systemConfig'

const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const warn = ref(80)
const good = ref(95)
const refresh = ref(5)
const showValue = ref(true)
// 保存原始 config 列表（PUT 需要整表）
let fullConfig: SystemConfigItem[] = []

function toneOf(v: number): string {
  if (v <= 0) return 'otd__sample--gray'
  if (v < warn.value) return 'otd__sample--red'
  if (v < good.value) return 'otd__sample--yellow'
  return 'otd__sample--green'
}

async function open(): Promise<void> {
  visible.value = true
  saving.value = false
  try {
    const rsp = await listSystemConfig()
    if (Array.isArray(rsp?.data)) {
      fullConfig = rsp.data as SystemConfigItem[]
      for (const c of fullConfig) {
        const v = Number(c.configValue)
        if (c.configKey === 'occupancy.warn_threshold' && !Number.isNaN(v)) warn.value = v
        else if (c.configKey === 'occupancy.good_threshold' && !Number.isNaN(v)) good.value = v
        else if (c.configKey === 'occupancy.refresh_interval' && v > 0) refresh.value = v
        else if (c.configKey === 'occupancy.show_value') showValue.value = c.configValue.toLowerCase() === 'true'
      }
    }
  } catch (err) {
    ElMessage.error(String((err as Error)?.message || 'config load failed'))
  }
}

function close(): void {
  if (saving.value) return
  visible.value = false
}

function onWarn(e: Event): void {
  const v = Number((e.target as HTMLInputElement).value)
  // 保证 warn < good
  warn.value = Math.min(v, good.value - 1 >= 0 ? good.value - 1 : 0)
}
function onGood(e: Event): void {
  const v = Number((e.target as HTMLInputElement).value)
  // 保证 good > warn
  good.value = Math.max(v, warn.value + 1 <= 100 ? warn.value + 1 : 100)
}
function onRefresh(e: Event): void {
  refresh.value = Math.max(1, Number((e.target as HTMLInputElement).value))
}
function onShowValue(e: Event): void {
  showValue.value = (e.target as HTMLInputElement).checked
}

async function save(): Promise<void> {
  if (saving.value) return
  saving.value = true
  try {
    // 更新本地整表里的 4 个 occupancy.*
    const apply = (key: string, val: string) => {
      const c = fullConfig.find((x) => x.configKey === key)
      if (c) c.configValue = val
    }
    apply('occupancy.warn_threshold', String(warn.value))
    apply('occupancy.good_threshold', String(good.value))
    apply('occupancy.refresh_interval', String(refresh.value))
    apply('occupancy.show_value', showValue.value ? 'true' : 'false')
    // 若某 key 不在表里则 push（以防 DB 缺）
    const ensure = (key: string, name: string, val: string) => {
      if (!fullConfig.find((x) => x.configKey === key)) {
        fullConfig.push({ configKey: key, configName: name, configValue: val })
      }
    }
    ensure('occupancy.warn_threshold', 'Warn Threshold', String(warn.value))
    ensure('occupancy.good_threshold', 'Good Threshold', String(good.value))
    ensure('occupancy.refresh_interval', 'Refresh Interval', String(refresh.value))
    ensure('occupancy.show_value', 'Show Value', showValue.value ? 'true' : 'false')

    const rsp = await updateSystemConfig(fullConfig)
    if (rsp && rsp.success) {
      ElMessage.success(t('occupancy.thresholdSaved'))
      close()
    } else {
      ElMessage.error(rsp?.message || 'save failed')
    }
  } catch (err) {
    ElMessage.error(String((err as Error)?.message || 'save failed'))
  } finally {
    saving.value = false
  }
}

defineExpose({ open, close })
</script>

<style scoped lang="scss">
.otd-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.otd {
  width: 360px;
  max-width: 90vw;
  background: rgba(24, 28, 36, 0.95);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  padding: 20px;
}
.otd__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.otd__title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.otd__x {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
}
.otd__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.otd__preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.otd__preview-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.otd__samples {
  display: flex;
  gap: 8px;
}
.otd__sample {
  width: 46px;
  height: 30px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}
.otd__sample--red { background: linear-gradient(180deg, #ff5a5f, #b6373b); }
.otd__sample--yellow { background: linear-gradient(180deg, #ffd75e, #d3a82a); }
.otd__sample--green { background: linear-gradient(180deg, #5fd97f, #2e9e4f); }
.otd__sample--gray { background: linear-gradient(180deg, #4a4f58, #2c3038); }
.otd__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.otd__field-label {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}
.otd__field-label b {
  color: var(--accent);
}
.otd__field input[type='range'] {
  accent-color: var(--accent);
  width: 100%;
}
.otd__field--inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}
.otd__field--inline input[type='checkbox'] {
  accent-color: var(--accent);
}
.otd__foot {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: 18px;
}
.otd__btn {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  cursor: pointer;
  border: none;
}
.otd__btn--ghost {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--glass-border);
}
.otd__btn--primary {
  background: var(--gradient-brand);
  color: var(--text-on-accent);
}
.otd__btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
