<template>
  <div class="line-detail-root">
    <!-- 产线选择 -->
    <div class="dashboard-card filter-bar">
      <el-select
        v-model="selectedLineId"
        placeholder="选择产线"
        style="width: 200px"
        @change="onLineChange"
      >
        <el-option
          v-for="line in store.lines"
          :key="line.id"
          :label="line.name"
          :value="line.id"
        >
          <span>{{ line.name }}</span>
          <span class="status-pill" :class="line.status" style="margin-left: 8px;">
            {{ statusLabel(line.status) }}
          </span>
        </el-option>
      </el-select>

      <div v-if="currentLine" class="line-meta">
        <el-tag :type="currentLine.status === 'running' ? 'success' : (currentLine.status === 'warning' ? 'warning' : 'danger')">
          {{ statusLabel(currentLine.status) }}
        </el-tag>
        <span class="meta-item">车间：{{ currentLine.workshop }}</span>
        <span class="meta-item">摄像头：<span class="num">{{ cameras.length }}</span> 个</span>
        <span class="meta-item">在线：<span class="num text-success">{{ onlineCount }}</span></span>
        <span class="meta-item">24h 缺陷：<span class="num text-warning">{{ currentLine.defectCount24h }}</span></span>
        <span class="meta-item">OEE：<span class="num">{{ (currentLine.oee * 100).toFixed(0) }}%</span></span>
      </div>
    </div>

    <!-- 摄像头网格 -->
    <div class="dashboard-card camera-section">
      <div class="card-title">
        <span><span class="accent-bar"></span>摄像头实时状态（{{ cameras.length }}）</span>
        <span class="text-muted">
          在线
          <span class="num text-success">{{ onlineCount }}</span>
          / 告警
          <span class="num text-warning">{{ warningCount }}</span>
          / 离线
          <span class="num text-danger">{{ offlineCount }}</span>
        </span>
      </div>

      <el-scrollbar height="calc(100% - 36px)">
        <el-row :gutter="10">
          <el-col v-for="cam in cameras" :key="cam.id" :span="6" class="cam-col">
            <div class="cam-card" :class="`status-${cam.status}`">
              <div class="cam-header">
                <span class="cam-name">{{ cam.name }}</span>
                <span class="status-pill" :class="cam.status">
                  {{ cam.status === 'online' ? '在线' : (cam.status === 'warning' ? '告警' : '离线') }}
                </span>
              </div>
              <div class="cam-body">
                <div class="cam-preview">
                  <el-icon class="preview-icon"><VideoCamera /></el-icon>
                  <span class="preview-text">实时画面（v0.5 接入）</span>
                </div>
                <div class="cam-meta">
                  <div class="meta-row">
                    <span class="meta-key">编号</span>
                    <span class="meta-val num">{{ cam.id }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="meta-key">IP</span>
                    <span class="meta-val num">{{ cam.ip }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="meta-key">FPS</span>
                    <span class="meta-val num">{{ cam.fps || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { VideoCamera } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores'

const route = useRoute()
const store = useAppStore()

const selectedLineId = ref(route.params.id || null)

const currentLine = computed(() => store.lines.find((l) => l.id === selectedLineId.value) || null)
const cameras = computed(() => store.cameras.filter((c) => c.lineId === selectedLineId.value))
const onlineCount = computed(() => cameras.value.filter((c) => c.status === 'online').length)
const warningCount = computed(() => cameras.value.filter((c) => c.status === 'warning').length)
const offlineCount = computed(() => cameras.value.filter((c) => c.status === 'offline').length)

function statusLabel(s) {
  return { running: '运行', warning: '告警', stopped: '停机' }[s] || s
}

async function onLineChange(id) {
  await store.loadCameras(id)
}

watch(() => route.params.id, async (newId) => {
  if (newId) {
    selectedLineId.value = newId
    await store.loadCameras(newId)
  }
})

onMounted(async () => {
  if (store.lines.length === 0) await store.loadLines()
  if (selectedLineId.value) await store.loadCameras(selectedLineId.value)
  else if (store.lines.length > 0) {
    selectedLineId.value = store.lines[0].id
    await store.loadCameras(selectedLineId.value)
  }
})
</script>

<style scoped>
.line-detail-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.line-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.camera-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.cam-col {
  margin-bottom: 10px;
}

.cam-card {
  background: #0a1f33;
  border: 1px solid var(--border-soft);
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.15s ease;
}

.cam-card:hover { border-color: var(--accent); }
.cam-card.status-online { border-left: 3px solid var(--success); }
.cam-card.status-warning { border-left: 3px solid var(--warning); }
.cam-card.status-offline { border-left: 3px solid var(--danger); opacity: 0.6; }

.cam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: #0a1f33;
  border-bottom: 1px solid var(--border-soft);
}

.cam-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.cam-body {
  padding: 10px;
}

.cam-preview {
  background: #06121f;
  border-radius: 3px;
  height: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  color: var(--text-muted);
}

.preview-icon {
  font-size: 28px;
  margin-bottom: 4px;
}

.preview-text {
  font-size: 10px;
}

.cam-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
}

.meta-key { color: var(--text-muted); }
.meta-val { color: var(--text-secondary); }

.text-muted { color: var(--text-muted); font-size: 11px; }
.text-success { color: var(--success); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }
</style>
