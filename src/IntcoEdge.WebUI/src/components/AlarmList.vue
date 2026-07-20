<template>
  <div class="dashboard-card alarm-list">
    <div class="card-title">
      <span><span class="accent-bar"></span>最新报警</span>
      <span class="text-muted">
        未确认
        <span class="num text-danger">{{ unackCount }}</span>
        / 共 {{ data.length }} 条
      </span>
    </div>

    <el-scrollbar height="100%">
      <div
        v-for="alarm in data"
        :key="alarm.id"
        class="alarm-item"
        :class="`level-${alarm.level}`"
        @click="$emit('select', alarm)"
      >
        <div class="alarm-dot" :class="`bg-${alarm.level}`"></div>
        <div class="alarm-content">
          <div class="alarm-header">
            <span class="alarm-type">{{ alarm.type }}</span>
            <span class="alarm-time num">{{ alarm.occurredAt.slice(11) }}</span>
          </div>
          <div class="alarm-line">{{ alarm.lineName }} · {{ alarm.message }}</div>
        </div>
        <el-tag v-if="!alarm.acknowledged" size="small" type="danger" effect="dark">未确认</el-tag>
        <el-tag v-else size="small" type="info" effect="plain">已确认</el-tag>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  limit: {
    type: Number,
    default: 5
  }
})

defineEmits(['select'])

const unackCount = computed(() => props.data.filter((a) => !a.acknowledged).length)
</script>

<style scoped>
.alarm-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #0a1f33;
  border: 1px solid var(--border-soft);
  border-left: 3px solid var(--info);
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.alarm-item:hover {
  border-color: var(--accent);
  background: var(--bg-card-hover);
}

.alarm-item.level-critical {
  border-left-color: var(--danger);
  background: rgba(245, 108, 108, 0.05);
}
.alarm-item.level-warning {
  border-left-color: var(--warning);
}
.alarm-item.level-info {
  border-left-color: var(--info);
}

.alarm-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 6px currentColor;
}

.bg-critical { background: var(--danger); }
.bg-warning { background: var(--warning); }
.bg-info { background: var(--info); }

.alarm-content {
  flex: 1;
  min-width: 0;
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.alarm-type {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.alarm-time {
  font-size: 11px;
  color: var(--text-muted);
}

.alarm-line {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-muted { color: var(--text-muted); font-size: 11px; }
.text-danger { color: var(--danger); }
</style>
