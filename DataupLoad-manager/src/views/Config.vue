<template>
  <div class="config">
    <!-- 快捷参数 -->
    <div class="panel glass">
      <div class="panel__title">核心参数</div>
      <el-form label-width="160px" label-position="left" class="form">
        <el-form-item label="服务端口">
          <el-input-number v-model="fields.serverPort" :min="1" :max="65535" />
          <span class="tip">后端 HTTP 端口（默认 8080）</span>
        </el-form-item>
        <el-form-item label="车间号 (workshop)">
          <el-input v-model="fields.workshop" placeholder="如 QZN3" style="width: 200px" />
          <span class="tip">海康通讯文档车间代码，如 QZN2=先二车间</span>
        </el-form-item>
        <el-form-item label="MES 地址 (yk.url)">
          <el-input v-model="fields.ykUrl" style="width: 380px" />
          <span class="tip">英科 MES 接口地址</span>
        </el-form-item>
        <el-form-item label="数据库主机">
          <el-input v-model="fields.dbHost" style="width: 200px" />
        </el-form-item>
        <el-form-item label="数据库端口">
          <el-input-number v-model="fields.dbPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="数据库名">
          <el-input v-model="fields.dbName" style="width: 200px" />
        </el-form-item>
        <el-form-item label="产线白名单 IP">
          <el-input v-model="fields.whiteIps" type="textarea" :rows="4" style="width: 480px"
            placeholder="每行一个 IP 或网段，如：&#10;192.168.135.70&#10;192.168.135.0/24" />
          <span class="tip">允许上报数据的产线工控机 IP（写入 white_ip 表）</span>
        </el-form-item>
      </el-form>
      <div class="actions">
        <el-button type="primary" :loading="saving" @click="saveCore">
          <el-icon><Check /></el-icon>&nbsp;保存并应用
        </el-button>
        <el-button @click="loadAll">
          <el-icon><Refresh /></el-icon>&nbsp;重新加载
        </el-button>
      </div>
    </div>

    <!-- 高级：原始 YAML -->
    <div class="panel glass">
      <div class="panel__title">
        高级：原始 YAML（application-prod.yml）
        <span class="tip">改完点"保存 YAML"，然后重启后端生效</span>
      </div>
      <el-input v-model="ymlText" type="textarea" :rows="18" class="yml" spellcheck="false" />
      <div class="actions">
        <el-button type="warning" :loading="savingYml" @click="saveYml">
          <el-icon><DocumentChecked /></el-icon>&nbsp;保存 YAML
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Check, Refresh, DocumentChecked } from '@element-plus/icons-vue'

const fields = ref({
  serverPort: 8080,
  workshop: '',
  ykUrl: '',
  dbHost: '127.0.0.1',
  dbPort: 5432,
  dbName: 'intco',
  whiteIps: ''
})
const ymlText = ref('')
const saving = ref(false)
const savingYml = ref(false)

// 从 yml 结构提取核心字段
function extract(obj) {
  const f = {
    serverPort: obj?.server?.port ?? 8080,
    workshop: obj?.hik?.workshop ?? obj?.workshop ?? '',
    ykUrl: obj?.yk?.url ?? obj?.yingke?.url ?? '',
    dbHost: obj?.spring?.datasource?.dynamic?.primary === 'intco'
      ? (obj?.spring?.datasource?.dynamic?.datasource?.intco?.url || '').match(/\/\/([^:]+):/)?.[1] || '127.0.0.1'
      : '127.0.0.1',
    dbPort: parseInt((obj?.spring?.datasource?.dynamic?.datasource?.intco?.url || '').match(/:(\d+)\//)?.[1]) || 5432,
    dbName: obj?.spring?.datasource?.dynamic?.datasource?.intco?.url?.split('/')?.pop() || 'intco',
    whiteIps: ''
  }
  return f
}

async function loadAll() {
  const cfg = await window.manager.configLoad()
  const prod = cfg?.prod || {}
  Object.assign(fields.value, extract(prod))
  // 白名单从配置读取（如果有 uri-permit / ip 列表）
  const wl = prod?.['hik-security']?.['white-list']
  if (wl) {
    fields.value.whiteIps = Array.isArray(wl.ip) ? wl.ip.join('\n') : (wl.ip || '')
  }
  const yml = await window.manager.configGetYml()
  ymlText.value = yml
}

async function saveCore() {
  saving.value = true
  const patch = { prod: {} }
  patch.prod['server.port'] = fields.value.serverPort
  if (fields.value.workshop) {
    patch.prod['hik.workshop'] = fields.value.workshop
    patch.prod['workshop'] = fields.value.workshop
  }
  if (fields.value.ykUrl) patch.prod['yk.url'] = fields.value.ykUrl
  // DB 连接（如果有 dynamic.datasource.intco）
  const cfg = await window.manager.configLoad()
  const url = cfg?.prod?.spring?.datasource?.dynamic?.datasource?.intco?.url
  if (url) {
    const newUrl = `jdbc:postgresql://${fields.value.dbHost}:${fields.value.dbPort}/${fields.value.dbName}`
    patch.prod['spring.datasource.dynamic.datasource.intco.url'] = newUrl
  }
  // 白名单 IP
  if (fields.value.whiteIps.trim()) {
    const ips = fields.value.whiteIps.split(/\r?\n/).map(s => s.trim()).filter(Boolean)
    patch.prod['hik-security.white-list.ip'] = ips
  }
  const r = await window.manager.configSave(patch)
  saving.value = false
  if (r.ok) {
    ElMessage.success('已保存：' + r.saved.join(', ') + '（需重启后端生效）')
  } else {
    ElMessage.error(r.message || '保存失败')
  }
}

async function saveYml() {
  savingYml.value = true
  const r = await window.manager.configSetYml(ymlText.value)
  savingYml.value = false
  r.ok ? ElMessage.success('YAML 已保存（需重启后端生效）') : ElMessage.error(r.message)
}

onMounted(loadAll)
</script>

<style scoped>
.config { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; display: flex; align-items: center; gap: 10px; }
.form :deep(.el-form-item) { margin-bottom: 14px; }
.tip { font-size: 12px; color: var(--text-dim); margin-left: 12px; }
.actions { display: flex; gap: 10px; margin-top: 4px; }
.yml { font-family: 'Cascadia Code', Consolas, monospace; font-size: 12px; }
</style>
