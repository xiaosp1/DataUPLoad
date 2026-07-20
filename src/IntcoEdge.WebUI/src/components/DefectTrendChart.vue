<template>
  <div class="dashboard-card defect-trend-chart">
    <div class="card-title">
      <span><span class="accent-bar"></span>缺陷趋势（24h）</span>
      <span class="text-muted">按区域聚合</span>
    </div>
    <v-chart class="chart" :option="option" autoresize />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps({
  data: {
    type: Object,
    default: () => ({ hours: [], series: [] })
  }
})

const option = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15, 41, 66, 0.95)',
    borderColor: '#1a3a5c',
    textStyle: { color: '#e0e6ed' }
  },
  legend: {
    data: props.data.series.map((s) => s.name),
    textStyle: { color: '#b0bec5' },
    top: 0,
    right: 0
  },
  grid: { top: 40, left: 36, right: 16, bottom: 24 },
  xAxis: {
    type: 'category',
    data: props.data.hours,
    axisLine: { lineStyle: { color: '#1a3a5c' } },
    axisLabel: { color: '#6b8caf', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#1a3a5c' } },
    splitLine: { lineStyle: { color: '#1a3a5c', type: 'dashed' } },
    axisLabel: { color: '#6b8caf', fontSize: 10 }
  },
  series: props.data.series.map((s) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    data: s.data,
    symbol: 'circle',
    symbolSize: 5,
    lineStyle: { width: 2 }
  })),
  color: ['#1976d2', '#67c23a', '#e6a23c']
}))
</script>

<style scoped>
.defect-trend-chart {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart {
  flex: 1;
  min-height: 0;
}

.text-muted {
  color: var(--text-muted);
  font-size: 11px;
}
</style>
