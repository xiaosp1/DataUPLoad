<template>
  <div class="dashboard-card line-overview">
    <div class="card-title">
      <span><span class="accent-bar"></span>产线概览（{{ lines.length }} 条）</span>
      <span class="text-muted">
        运行
        <span class="num text-success">{{ stats.running }}</span>
        / 告警
        <span class="num text-warning">{{ stats.warning }}</span>
        / 停机
        <span class="num text-danger">{{ stats.stopped }}</span>
      </span>
    </div>

    <el-scrollbar height="100%">
      <el-row :gutter="8">
        <el-col v-for="line in lines" :key="line.id" :span="12" class="line-col">
          <div class="line-card" :class="`status-${line.status}`" @click="$emit('select', line)">
            <div class="line-card-header">
              <span class="line-name">{{ line.name }}</span>
              <span class="status-pill" :class="line.status">{{ statusLabel(line.status) }}</span>
            </div>
            <div class="line-card-body">
              <div class="metric">
                <div class="metric-label">摄像头</div>
                <div class="metric-value num">{{ line.cameraCount }}</div>
              </div>
              <div class="metric">
                <div class="metric-label">24h 缺陷</div>
                <div class="metric-value num" :class="defectSeverityClass(line.defectCount24h)">
                  {{ line.defectCount24h }}
                </div>
              </div>
              <div class="metric">
                <div class="metric-label">OEE</div>
                <div class="metric-value num">{{ (line.oee * 100).toFixed(0) }}%</div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  lines: {
    type: Array,
    default: () => []
  }
})

defineEmits(['select'])

const stats = computed(() => {
  const r = { running: 0, warning: 0, stopped: 0 }
  for (const l of props.lines) {
    if (r[l.status] !== undefined) r[l.status]++
  }
  return r
})

function statusLabel(s) {
  return { running: '运行', warning: '告警', stopped: '停机' }[s] || s
}

function defectSeverityClass(count) {
  if (count >= 40) return 'text-danger'
  if (count >= 25) return 'text-warning'
  return 'text-success'
}
</script>

<style scoped>
.line-overview {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.line-col {
  margin-bottom: 8px;
}

.line-card {
  background: #0a1f33;
  border: 1px solid var(--border-soft);
  border-left: 3px solid var(--info);
  border-radius: 4px;
  padding: 8px 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.line-card:hover {
  border-color: var(--accent);
  background: var(--bg-card-hover);
}

.line-card.status-running {
  border-left-color: var(--success);
}
.line-card.status-warning {
  border-left-color: var(--warning);
}
.line-card.status-stopped {
  border-left-color: var(--danger);
}

.line-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.line-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.line-card-body {
  display: flex;
  justify-content: space-between;
  gap: 4px;
}

.metric {
  flex: 1;
  text-align: center;
}

.metric-label {
  font-size: 10px;
  color: var(--text-muted);
  margin-bottom: 2px;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.text-success { color: var(--success); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }
.text-muted { color: var(--text-muted); font-size: 11px; }
</style>
