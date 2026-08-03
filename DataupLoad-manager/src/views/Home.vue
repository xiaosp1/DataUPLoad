<template>
  <div class="home">
    <!-- 状态卡片 -->
    <div class="cards">
      <div class="card glass">
        <div class="card__label">后端服务</div>
        <div class="card__value" :class="overview?.server?.running ? 'ok' : 'err'">
          {{ overview?.server?.running ? '运行中' : '已停止' }}
        </div>
        <div class="card__sub" v-if="overview?.server?.pid">PID {{ overview.server.pid }}</div>
      </div>
      <div class="card glass">
        <div class="card__label">数据库 (PG)</div>
        <div class="card__value" :class="overview?.pgRunning ? 'ok' : 'err'">
          {{ overview?.pgRunning ? '运行中' : '未运行' }}
        </div>
        <div class="card__sub">127.0.0.1:5432</div>
      </div>
      <div class="card glass">
        <div class="card__label">产线连接</div>
        <div class="card__value">{{ overview?.connections ?? '—' }}</div>
        <div class="card__sub">端口 8080</div>
      </div>
      <div class="card glass">
        <div class="card__label">部署体检</div>
        <div class="card__value" :class="overview?.deployment?.ok ? 'ok' : 'warn'">
          {{ overview?.deployment?.ok ? '全部就绪' : '有缺失' }}
        </div>
        <div class="card__sub">点击"部署体检"查看明细</div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="panel glass">
      <div class="panel__title">快速操作</div>
      <div class="actions">
        <el-button type="primary" size="large" :loading="busy" @click="startAll">
          <el-icon><VideoPlay /></el-icon>&nbsp;一键启动
        </el-button>
        <el-button size="large" :loading="busy" @click="stopAll">
          <el-icon><VideoPause /></el-icon>&nbsp;一键停止
        </el-button>
        <el-button size="large" @click="$emit('refresh')">
          <el-icon><Refresh /></el-icon>&nbsp;刷新状态
        </el-button>
      </div>
    </div>

    <!-- 部署明细 -->
    <div class="panel glass" v-if="overview?.deployment">
      <div class="panel__title">部署组件检查</div>
      <div class="checks">
        <div v-for="(ok, k) in overview.deployment.checks" :key="k" class="check-item">
          <div class="dot" :class="ok ? 'dot--ok' : 'dot--err'"></div>
          <span>{{ checkLabels[k] || k }}</span>
          <span class="check-item__state" :class="ok ? 'ok' : 'err'">{{ ok ? '正常' : '缺失' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { VideoPlay, VideoPause, Refresh } from '@element-plus/icons-vue'

const props = defineProps({ overview: Object })
const emit = defineEmits(['refresh'])
const busy = ref(false)

const checkLabels = {
  serverDir: '服务端目录 (server/)',
  libJars: '依赖库 (lib/)',
  classes: '编译产物 (target/classes)',
  config: '配置目录 (config/)',
  web: '前端页面 (web/)',
  jdk: 'JDK (jdk/)',
  sql: '建库脚本 (sql/)'
}

async function startAll() {
  busy.value = true
  await window.manager.pgStart()
  await new Promise(r => setTimeout(r, 2000))
  await window.manager.serverStart()
  busy.value = false
  emit('refresh')
  ElMessage.success('启动指令已下发')
}

async function stopAll() {
  busy.value = true
  await window.manager.serverStop()
  await window.manager.pgStop()
  busy.value = false
  emit('refresh')
  ElMessage.success('停止指令已下发')
}
</script>

<style scoped>
.home { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.card { padding: 20px 22px; }
.card__label { font-size: 13px; color: var(--text-secondary); }
.card__value { font-size: 28px; font-weight: 800; margin: 10px 0 4px; letter-spacing: 1px; }
.card__value.ok { color: var(--ok); text-shadow: 0 0 16px var(--ok-glow); }
.card__value.err { color: var(--err); text-shadow: 0 0 16px var(--err-glow); }
.card__value.warn { color: var(--warn); }
.card__sub { font-size: 12px; color: var(--text-dim); }

.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; }
.actions { display: flex; gap: 12px; }
.checks { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 24px; }
.check-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-secondary); padding: 6px 0; }
.check-item__state { margin-left: auto; font-size: 12px; }
.ok { color: var(--ok); }
.err { color: var(--err); }
</style>
