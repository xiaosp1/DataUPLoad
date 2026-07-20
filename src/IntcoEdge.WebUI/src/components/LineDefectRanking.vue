<template>
  <div class="dashboard-card line-defect-ranking">
    <div class="card-title">
      <span><span class="accent-bar"></span>产线缺陷排行（24h）</span>
      <span class="text-muted">点击柱条查看详情</span>
    </div>
    <v-chart class="chart" :option="option" autoresize @click="onClick" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const router = useRouter()

// 按缺陷数降序
const sorted = computed(() => [...props.data].sort((a, b) => a.value - b.value))

const option = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisTrigger: 'item',
    backgroundColor: 'rgba(15, 41, 66, 0.95)',
    borderColor: '#1a3a5c',
    textStyle: { color: '#e0e6ed' },
    formatter: (params) => {
      const p = params[0]
      return `${p.name}<br/><span style="color:#f56c6c;font-weight:600">${p.value}</span> 次缺陷`
    }
  },
  grid: { top: 16, left: 80, right: 40, bottom: 24 },
  xAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#1a3a5c' } },
    splitLine: { lineStyle: { color: '#1a3a5c', type: 'dashed' } },
    axisLabel: { color: '#6b8caf', fontSize: 10 }
  },
  yAxis: {
    type: 'category',
    data: sorted.value.map((d) => d.name),
    axisLine: { lineStyle: { color: '#1a3a5c' } },
    axisLabel: { color: '#b0bec5', fontSize: 11 }
  },
  series: [
    {
      type: 'bar',
      data: sorted.value.map((d, i) => ({
        value: d.value,
        lineId: d.id,
        itemStyle: {
          // 颜色渐变：前 3 名用告警色
          color: i >= sorted.value.length - 3 ? '#f56c6c' : (i >= sorted.value.length - 6 ? '#e6a23c' : '#1976d2')
        }
      })),
      barMaxWidth: 24,
      label: {
        show: true,
        position: 'right',
        color: '#e0e6ed',
        fontSize: 11,
        fontWeight: 600,
        formatter: '{c}'
      }
    }
  ]
}))

function onClick(params) {
  if (params?.data?.lineId) {
    router.push(`/line/${params.data.lineId}`)
  }
}
</script>

<style scoped>
.line-defect-ranking {
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
