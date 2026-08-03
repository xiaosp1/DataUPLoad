<template>
  <div class="database">
    <!-- 状态 -->
    <div class="panel glass">
      <div class="panel__title">嵌入式 PostgreSQL</div>
      <div class="db-bar">
        <div class="status">
          <div class="dot" :class="status?.running ? 'dot--ok' : (status?.installed ? 'dot--warn' : 'dot--err')"></div>
          <span class="status__text">{{ status?.running ? '运行中' : (status?.installed ? '已安装·未运行' : '未安装') }}</span>
          <span v-if="status?.pid" class="status__pid">PID {{ status.pid }}</span>
        </div>
        <div class="btns">
          <el-button type="primary" :disabled="status?.installed" :loading="busy" @click="doInstall">
            <el-icon><Download /></el-icon>&nbsp;安装 PG
          </el-button>
          <el-button type="success" :disabled="!status?.installed || status?.running" :loading="busy" @click="doStart">
            <el-icon><VideoPlay /></el-icon>&nbsp;启动
          </el-button>
          <el-button type="danger" :disabled="!status?.running" :loading="busy" @click="doStop">
            <el-icon><VideoPause /></el-icon>&nbsp;停止
          </el-button>
        </div>
      </div>
      <div class="db-info">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="主机">127.0.0.1</el-descriptions-item>
          <el-descriptions-item label="端口">5432</el-descriptions-item>
          <el-descriptions-item label="超管密码">Abc12345</el-descriptions-item>
          <el-descriptions-item label="数据目录">{{ status?.dataDir || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <!-- 数据库列表 -->
    <div class="panel glass">
      <div class="panel__title">数据库</div>
      <el-table :data="dbs" size="small" style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="owner" label="属主" min-width="120" />
        <el-table-column prop="size" label="大小" min-width="100" />
        <el-table-column prop="remark" label="用途" min-width="240" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Download, VideoPlay, VideoPause } from '@element-plus/icons-vue'

const status = ref(null)
const busy = ref(false)
const dbs = ref([])

async function refresh() {
  status.value = await window.manager.pgStatus()
  // 预置数据库信息（P3 初始化后实际查询）
  dbs.value = [
    { name: 'intco', owner: 'postgres', size: '—', remark: '业务库（detect/alarm/line 等）' },
    { name: 'postgres', owner: 'postgres', size: '—', remark: '系统库' }
  ]
}
async function doInstall() {
  busy.value = true
  ElMessage.info('正在静默安装 PG（约 1-3 分钟）...')
  const r = await window.manager.pgInstall()
  busy.value = false
  if (r.ok) { ElMessage.success(r.message); } else { ElMessage.error(r.message) }
  await refresh()
}
async function doStart() {
  busy.value = true
  const r = await window.manager.pgStart()
  busy.value = false
  r.ok ? ElMessage.success(r.message) : ElMessage.error(r.message)
  await refresh()
}
async function doStop() {
  busy.value = true
  const r = await window.manager.pgStop()
  busy.value = false
  r.ok ? ElMessage.success(r.message) : ElMessage.error(r.message)
  await refresh()
}
onMounted(refresh)
</script>

<style scoped>
.database { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; }
.db-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.status { display: flex; align-items: center; gap: 8px; }
.status__text { font-size: 16px; font-weight: 600; }
.status__pid { font-size: 12px; color: var(--text-dim); }
.btns { display: flex; gap: 10px; }
.db-info { max-width: 640px; }
</style>
