<template>
  <div class="defect-query-root">
    <!-- 过滤区 -->
    <div class="dashboard-card filter-card">
      <el-form :inline="true" :model="filter" @submit.prevent="onQuery">
        <el-form-item label="产线">
          <el-select v-model="filter.lineId" placeholder="全部产线" clearable style="width: 160px">
            <el-option v-for="l in store.lines" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷类型">
          <el-select v-model="filter.type" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="t in defectTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="filter.severity" placeholder="全部" clearable style="width: 120px">
            <el-option label="低" value="low" />
            <el-option label="中" value="medium" />
            <el-option label="高" value="high" />
            <el-option label="严重" value="critical" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onQuery">查询</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
          <el-button :icon="Download" @click="onExport" :disabled="!filtered.length">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计 -->
    <el-row :gutter="12" class="stat-row">
      <el-col :span="6">
        <div class="dashboard-card stat-card">
          <div class="stat-label">查询结果</div>
          <div class="stat-value num">{{ filtered.length }}<span class="unit">条</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card stat-card">
          <div class="stat-label">严重缺陷</div>
          <div class="stat-value num text-danger">{{ countBySeverity('critical') + countBySeverity('high') }}<span class="unit">条</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card stat-card">
          <div class="stat-label">平均置信度</div>
          <div class="stat-value num">{{ avgConfidence }}<span class="unit">%</span></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="dashboard-card stat-card">
          <div class="stat-label">未处理</div>
          <div class="stat-value num text-warning">{{ unresolvedCount }}<span class="unit">条</span></div>
        </div>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <div class="dashboard-card table-card">
      <el-table
        :data="pagedData"
        stripe
        border
        style="width: 100%"
        :default-sort="{ prop: 'occurredAt', order: 'descending' }"
      >
        <el-table-column prop="id" label="缺陷 ID" width="120" />
        <el-table-column prop="occurredAt" label="时间" width="170" sortable />
        <el-table-column prop="lineName" label="产线" width="120" />
        <el-table-column prop="cameraId" label="摄像头" width="120" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="severityTagType(row.severity)" size="small" effect="dark">
              {{ severityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="120" sortable>
          <template #default="{ row }">
            <span class="num">{{ (row.confidence * 100).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="resolved" label="状态" width="100">
          <template #default="{ row }">
            <span v-if="row.resolved" class="status-pill online">已处理</span>
            <span v-else class="status-pill warning">未处理</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="filtered.length"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores'
import { ElMessage } from 'element-plus'

const store = useAppStore()

const filter = ref({
  lineId: null,
  type: null,
  severity: null
})

const defectTypes = ['气泡', '杂质', '划痕', '变形', '色差', '尺寸偏差']

const page = ref(1)
const pageSize = ref(20)

const filtered = computed(() => {
  let list = store.defects
  if (filter.value.lineId) list = list.filter((d) => d.lineId === filter.value.lineId)
  if (filter.value.type) list = list.filter((d) => d.type === filter.value.type)
  if (filter.value.severity) list = list.filter((d) => d.severity === filter.value.severity)
  return list
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

const avgConfidence = computed(() => {
  if (!filtered.value.length) return 0
  const sum = filtered.value.reduce((s, d) => s + (d.confidence || 0), 0)
  return ((sum / filtered.value.length) * 100).toFixed(1)
})

const unresolvedCount = computed(() => filtered.value.filter((d) => !d.resolved).length)

function countBySeverity(sev) {
  return filtered.value.filter((d) => d.severity === sev).length
}

function severityTagType(s) {
  return { critical: 'danger', high: 'danger', medium: 'warning', low: 'info' }[s] || 'info'
}

function severityLabel(s) {
  return { critical: '严重', high: '高', medium: '中', low: '低' }[s] || s
}

async function onQuery() {
  await store.loadDefects({
    lineId: filter.value.lineId,
    type: filter.value.type,
    severity: filter.value.severity
  })
  page.value = 1
}

function onReset() {
  filter.value = { lineId: null, type: null, severity: null }
  onQuery()
}

function onExport() {
  // 导出 CSV（简单实现：前端拼字符串下载）
  const headers = ['缺陷ID', '时间', '产线', '摄像头', '类型', '严重程度', '置信度', '状态']
  const rows = filtered.value.map((d) => [
    d.id,
    d.occurredAt,
    d.lineName,
    d.cameraId,
    d.type,
    severityLabel(d.severity),
    (d.confidence * 100).toFixed(1) + '%',
    d.resolved ? '已处理' : '未处理'
  ])

  const csv = [headers, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `defects_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${filtered.value.length} 条缺陷记录`)
}

onMounted(async () => {
  if (store.lines.length === 0) await store.loadLines()
  if (store.defects.length === 0) await store.loadDefects()
})
</script>

<style scoped>
.defect-query-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card { flex-shrink: 0; }

.stat-row { flex-shrink: 0; }

.stat-card {
  padding: 12px 16px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.stat-value {
  font-family: 'DIN Alternate', 'Roboto Mono', monospace;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-value .unit {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 4px;
}

.text-success { color: var(--success); }
.text-warning { color: var(--warning); }
.text-danger { color: var(--danger); }

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-card :deep(.el-table) {
  flex: 1;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
