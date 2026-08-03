<template>
  <div class="settings">
    <div class="panel glass">
      <div class="panel__title">系统设置</div>
      <el-form label-width="160px" label-position="left" class="form">
        <el-form-item label="开机自启">
          <el-switch v-model="settings.autoStart" @change="saveAutoStart" />
          <span class="tip">开机自动启动后端服务（写入启动文件夹）</span>
        </el-form-item>
        <el-form-item label="看门狗">
          <el-switch v-model="settings.watchdog" @change="saveWatchdog" />
          <span class="tip">监控 PG + 后端进程，异常退出自动拉起（依赖开机自启）</span>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel glass">
      <div class="panel__title">环境信息</div>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="部署包根目录">{{ rootDir }}</el-descriptions-item>
        <el-descriptions-item label="桌面端版本">v0.1.0</el-descriptions-item>
        <el-descriptions-item label="运行时">Electron + Vue 3 + Element Plus</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="panel glass">
      <div class="panel__title">关于</div>
      <div class="about-text">
        DataupLoad 部署管理桌面端，用于 P3 工控机的安装、配置与运维。
        界面风格与 DataupLoad Web 端（玻璃风）保持一致。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const settings = ref({ autoStart: false, watchdog: false })
const rootDir = ref('')

async function load() {
  const s = await window.manager.settingsGet()
  settings.value = { ...settings.value, ...s }
  const ov = await window.manager.svcOverview()
  rootDir.value = ov?.rootDir || ''
}
async function saveAutoStart(v) {
  const r = await window.manager.settingsSet({ autoStart: v })
  r.ok ? ElMessage.success(v ? '已开启开机自启' : '已关闭开机自启') : ElMessage.error(r.message)
}
async function saveWatchdog(v) {
  const r = await window.manager.settingsSet({ watchdog: v })
  r.ok ? ElMessage.success(v ? '看门狗已启用' : '看门狗已停用') : ElMessage.error(r.message)
}
onMounted(load)
</script>

<style scoped>
.settings { display: flex; flex-direction: column; gap: 16px; padding: 4px; }
.panel { padding: 18px 22px; }
.panel__title { font-size: 15px; font-weight: 700; margin-bottom: 14px; }
.tip { font-size: 12px; color: var(--text-dim); margin-left: 12px; }
.about-text { font-size: 13px; color: var(--text-secondary); line-height: 1.8; }
</style>
