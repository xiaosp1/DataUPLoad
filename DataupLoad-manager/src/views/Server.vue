<template>
  <div class="server">
    <!-- 状态 + 操作 -->
    <div class="panel glass">
      <div class="panel__title">后端服务（hik-java · port 8080）</div>
      <div class="server-bar">
        <div class="status">
          <div class="dot" :class="status?.running ? 'dot--ok' : 'dot--err'"></div>
          <span class="status__text">{{ status?.running ? '运行中' : '已停止' }}</span>
          <span v-if="status?.pid" class="status__pid">PID {{ status.pid }}</span>
        </div>
        <div class="btns">
          <el-button type="success" :disabled="status?.running" :loading="busy" @click="doStart">
            <el-icon><VideoPlay /></el-icon>&nbsp;启动
          </el-button>
          <el-button type="danger" :disabled="!status?.running" :loading="busy" @click="doStop">
            <el-icon><VideoPause /></el-icon>&nbsp;停止
          </el-button>
          <el-button :loading="busy" @click="doRestart">
            <el-icon><RefreshRight /></el-icon>&nbsp;重启
          </el-button>
        </div>
      </div>
    </div>

    <!-- 运行日志 -->
    <div class="panel glass log-panel">
      <div class="panel__title">
        运行日志
        <el-button size="small" text @click="refreshLog"><el-icon><Refresh /></el-icon></el-button>
      </div>
      <pre class="log" ref="logRef">{{ logText }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { VideoPlay, VideoPause, RefreshRight, Refresh } from '@element-plus/icons-vue'

const status = ref(null)
const busy = ref(false)
const logText = ref('')
const logRef = ref(null)

async function refresh() {
  status.value = await window.manager.serverStatus()
}
async function refreshLog() {
  const r = await window.manager.serverLog(300)
  if (r.ok) {
    logText.value = r.lines.join('\n')
    nextTick(() => { if (logRef.value) logRef.value.scrollTop = logRef.value.scrollHeight })
  } else {
    logText.value = '（无日志：' + r.message + '）'
  }
}
async function doStart() {
  busy.value = true
  await window.manager.serverStart()
  await new Promise(r => setTimeout(r, 2500))
  busy.value = false
  await refresh()
  await refreshLog()
}
async function doStop() {
  busy.value = true
  await window.manager.serverStop()
  busy.value = false
  await refresh()
  await refreshLog()
}
async function doRestart() {
  busy.value = true
  await window.manager.serverRestart()
  await new Promise(r => setTimeout(r, 2500))
  busy.value = false
  await refresh()
  await refreshLog()
}

onMounted(() => { refresh(); refreshLog() })
</script>

<style scoped>
.server { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
.server-bar { display: flex; align-items: center; justify-content: space-between; }
.status { display: flex; align-items: center; gap: 8px; }
.status__text { font-size: 16px; font-weight: 600; }
.status__pid { font-size: 12px; color: var(--text-dim); }
.btns { display: flex; gap: 10px; }
.log-panel { flex: 1; display: flex; flex-direction: column; min-height: 320px; }
.log {
  flex: 1; margin: 0; padding: 14px;
  background: rgba(6, 12, 24, 0.72); border-radius: 10px;
  font-family: 'Cascadia Code', Consolas, monospace; font-size: 12px;
  color: #9fb8dd; line-height: 1.6;
  overflow: auto; white-space: pre-wrap; word-break: break-all;
  user-select: text;
}
</style>
