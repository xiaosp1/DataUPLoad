<!--
  W-RT-3 中栏 4 区面板 - ③设备三联状态（玻璃风）

  PSM 实时页 4 区面板 - 摄像机 / 编码器 / PLC 三联
  数据源：props.deviceStatus = { camera, encoder, plc } (online | offline)
    后端目前未提供 deviceStatus 字段，使用占位：online 状态随机生成（保持稳定）
    或读取 realtimeData.deviceStatus（如有）
-->
<template>
  <GlassCard class="lds-card" :hover="true">
    <div class="lds-card__inner">
      <div class="lds-card__head">
        <h4 class="lds-card__title">
          <span class="lds-card__icon">🔌</span>
          {{ $t('realtime.detail.device') }}
        </h4>
      </div>

      <div class="lds-card__grid">
        <div
          v-for="dev in devices"
          :key="dev.key"
          class="lds-card__item"
          :data-state="dev.online ? 'online' : 'offline'"
        >
          <div class="lds-card__item-head">
            <span class="lds-card__item-icon">{{ dev.icon }}</span>
            <span class="lds-card__item-name">{{ dev.name }}</span>
          </div>
          <div class="lds-card__item-foot">
            <span class="lds-card__item-dot" />
            <span class="lds-card__item-state">{{ dev.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </GlassCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  /** 可选：后端实时设备状态（如有） */
  deviceStatus?: { camera?: boolean; encoder?: boolean; plc?: boolean }
  /** 当前选中线的 realtime（用来推导 PLC 心跳/状态） */
  total?: number
  efficiency?: number
}>(), {
  deviceStatus: () => ({}),
  total: 0,
  efficiency: 0
})

const tOnline = 'realtime.detail.online'
const tOffline = 'realtime.detail.offline'

interface DevItem {
  key: string
  name: string
  icon: string
  online: boolean
  label: string
}

const devices = computed<DevItem[]>(() => {
  const ds = props.deviceStatus || {}
  // 摄像机/编码器：以后端字段为准；没有字段时用"在线"占位（玻璃风始终亮）
  // PLC：以"有产量(>0) + 有效率"判断在运行
  const plcOnline = props.total > 0 && props.efficiency > 0
  const cam = ds.camera !== false
  const enc = ds.encoder !== false
  return [
    {
      key: 'camera',
      name: useT('realtime.detail.camera'),
      icon: '📷',
      online: cam,
      label: useT(cam ? tOnline : tOffline)
    },
    {
      key: 'encoder',
      name: useT('realtime.detail.encoder'),
      icon: '🎞️',
      online: enc,
      label: useT(enc ? tOnline : tOffline)
    },
    {
      key: 'plc',
      name: useT('realtime.detail.plc'),
      icon: '⚙️',
      online: plcOnline,
      label: useT(plcOnline ? tOnline : tOffline)
    }
  ]
})

// 直接用 i18n global，避免 setup 内逐项引用 useI18n
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
function useT(key: string): string {
  try {
    return String(t(key))
  } catch {
    return key
  }
}
</script>

<style lang="scss" scoped>
.lds-card {
  width: 100%;
  height: 100%;
}
.lds-card__inner {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  height: 100%;
}
.lds-card__head {
  padding-bottom: var(--space-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.lds-card__title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  letter-spacing: 0.2px;
}
.lds-card__icon {
  font-size: 16px;
  filter: drop-shadow(0 2px 6px rgba(255, 183, 77, 0.3));
}

.lds-card__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
  flex: 1;
}
.lds-card__item {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: border-color var(--transition-base), background var(--transition-base);
}
.lds-card__item[data-state='online'] {
  background: linear-gradient(135deg, rgba(95, 217, 127, 0.10), rgba(92, 225, 255, 0.06));
  border-color: rgba(95, 217, 127, 0.30);
}
.lds-card__item[data-state='offline'] {
  background: rgba(255, 90, 95, 0.06);
  border-color: rgba(255, 90, 95, 0.30);
}

.lds-card__item-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lds-card__item-icon {
  font-size: 18px;
}
.lds-card__item-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  letter-spacing: 0.2px;
}

.lds-card__item-foot {
  display: flex;
  align-items: center;
  gap: 6px;
}
.lds-card__item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: 0 0 8px;
}
.lds-card__item[data-state='online'] .lds-card__item-dot {
  background: var(--success);
  box-shadow: 0 0 8px var(--success);
}
.lds-card__item[data-state='offline'] .lds-card__item-dot {
  background: var(--danger);
  box-shadow: 0 0 8px var(--danger);
}
.lds-card__item-state {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.3px;
}
.lds-card__item[data-state='online'] .lds-card__item-state {
  color: var(--success);
}
.lds-card__item[data-state='offline'] .lds-card__item-state {
  color: var(--danger);
}
</style>
