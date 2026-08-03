<template>
  <div class="deploy">
    <div class="panel glass">
      <div class="panel__title">
        部署体检
        <el-button size="small" text @click="check"><el-icon><Refresh /></el-icon></el-button>
      </div>
      <div class="tip-text" v-if="result">
        部署包根目录：<code>{{ result.rootDir }}</code>
      </div>
      <el-table :data="rows" size="small" style="width: 100%">
        <el-table-column prop="name" label="组件" min-width="220" />
        <el-table-column prop="okText" label="状态" width="100">
          <template #default="{ row }">
            <div class="dot" :class="row.ok ? 'dot--ok' : 'dot--err'"></div>
            <span :class="row.ok ? 'ok' : 'err'">{{ row.ok ? '正常' : '缺失' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="说明" min-width="280" />
      </el-table>
    </div>

    <div class="panel glass">
      <div class="panel__title">部署操作</div>
      <div class="actions">
        <el-button type="primary" :loading="busy" @click="initDb">
          <el-icon><MagicStick /></el-icon>&nbsp;初始化数据库（建库+种子数据）
        </el-button>
        <el-button type="success" :loading="busy" @click="startAll">
          <el-icon><VideoPlay /></el-icon>&nbsp;启动全部服务
        </el-button>
      </div>
      <div class="tip-text">
        <b>首次部署流程</b>：① 装 PG → ② 建库（intco + Flyway 脚本） → ③ 改配置（车间号/IP/白名单） → ④ 启动后端 → ⑤ 开启开机自启
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, MagicStick, VideoPlay } from '@element-plus/icons-vue'

const result = ref(null)
const busy = ref(false)

const rows = computed(() => {
  if (!result?.value?.checks) return []
  const labels = {
    serverDir: '服务端目录 server/',
    libJars: '依赖库 lib/',
    classes: '编译产物 target/classes',
    config: '配置目录 config/',
    web: '前端页面 web/',
    jdk: 'JDK',
    sql: '建库脚本 sql/'
  }
  return Object.entries(result.value.checks).map(([k, ok]) => ({
    name: labels[k] || k,
    ok,
    okText: ok ? '正常' : '缺失',
    detail: ok ? '已就绪' : '请检查部署包完整性'
  }))
})

async function check() {
  result.value = await window.manager.deployCheck()
}
async function initDb() {
  busy.value = true
  ElMessage.info('数据库初始化为手动步骤：请先用 PG 工具执行 sql/ 目录脚本（或确认 Flyway 自动执行）')
  busy.value = false
}
async function startAll() {
  busy.value = true
  await window.manager.pgStart()
  await new Promise(r => setTimeout(r, 2000))
  await window.manager.serverStart()
  busy.value = false
  ElMessage.success('服务启动指令已下发')
}
onMounted(check)
</script>

<style scoped>
.deploy { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
.tip-text { font-size: 13px; color: var(--text-secondary); margin: 10px 0; line-height: 1.8; }
.tip-text code { background: rgba(59,130,246,0.15); padding: 1px 6px; border-radius: 4px; font-size: 12px; }
.actions { display: flex; gap: 10px; }
.ok { color: var(--ok); }
.err { color: var(--err); }
</style>
