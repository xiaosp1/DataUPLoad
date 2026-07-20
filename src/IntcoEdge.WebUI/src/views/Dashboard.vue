<template>
  <div class="dashboard-root">
    <!-- 顶部 3 个 KPI 卡 -->
    <el-row :gutter="12" class="kpi-row">
      <el-col :span="6">
        <div class="dashboard-card kpi-card">
          <div class="kpi-label">运行产线</div>
          <div class="big-num num">{{ store.runningLineCount }}<span class="unit">/ {{ store.lines.length }} 条</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card kpi-card">
          <div class="kpi-label">在线摄像头</div>
          <div class="big-num num text-success">{{ store.onlineCameraCount }}<span class="unit">/ {{ store.cameras.length }} 个</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card kpi-card">
          <div class="kpi-label">24h 缺陷总数</div>
          <div class="big-num num text-warning">{{ store.totalDefects24h }}<span class="unit">次</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card kpi-card">
          <div class="kpi-label">未确认报警</div>
          <div class="big-num num text-danger">{{ store.unacknowledgedAlarmCount }}<span class="unit">条</span></div>
        </div>
      </el-col>
    </el-row>

    <!-- 主大屏布局：仿 PSM 大屏 -->
    <div class="dashboard-grid">
      <!-- 上排：产线概览 / 缺陷趋势 / 报警列表 -->
      <div class="grid-item grid-overview">
        <LineOverview :lines="store.lines" @select="onLineSelect" />
      </div>
      <div class="grid-item grid-trend">
        <DefectTrendChart :data="trendData" />
      </div>
      <div class="grid-item grid-alarm">
        <AlarmList :data="store.alarms" :limit="5" />
      </div>

      <!-- 下排：产线缺陷排行 -->
      <div class="grid-item grid-ranking">
        <LineDefectRanking :data="rankingData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'
import { fetchDefectTrend, fetchDefectRanking } from '@/api'
import LineOverview from '@/components/LineOverview.vue'
import DefectTrendChart from '@/components/DefectTrendChart.vue'
import LineDefectRanking from '@/components/LineDefectRanking.vue'
import AlarmList from '@/components/AlarmList.vue'

const router = useRouter()
const store = useAppStore()

const trendData = ref({ hours: [], series: [] })
const rankingData = ref([])
let pollTimer = null

function onLineSelect(line) {
  router.push(`/line/${line.id}`)
}

async function loadCharts() {
  trendData.value = await fetchDefectTrend()
  rankingData.value = await fetchDefectRanking()
}

onMounted(async () => {
  await store.refreshAll()
  await loadCharts()
  // 模拟实时刷新：30 秒一次（v0.4 改为 WebSocket 推送）
  pollTimer = setInterval(async () => {
    await store.refreshAll()
    await loadCharts()
  }, 30_000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.dashboard-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* KPI 区 */
.kpi-row {
  flex-shrink: 0;
}

.kpi-card {
  text-align: left;
  padding: 14px 18px;
}

.kpi-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.big-num {
  font-family: 'DIN Alternate', 'Roboto Mono', monospace;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 1px;
  line-height: 1.2;
}

.big-num .unit {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 4px;
  font-weight: 400;
}

.text-success { color: var(--success); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }

/* 大屏 Grid 布局（仿 PSM） */
.dashboard-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 2fr 2fr 1fr;
  grid-template-rows: 200px 1fr;
  grid-template-areas:
    "overview trend alarm"
    "ranking ranking ranking";
  gap: 12px;
  min-height: 0;
}

.grid-item {
  min-height: 0;
  min-width: 0;
}

.grid-overview { grid-area: overview; }
.grid-trend    { grid-area: trend; }
.grid-alarm    { grid-area: alarm; }
.grid-ranking  { grid-area: ranking; }

/* 大屏内部：组件 div 撑满 */
.grid-item > * {
  height: 100%;
}
</style>
