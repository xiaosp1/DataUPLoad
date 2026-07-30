<template>
  <router-view />
</template>

<script setup lang="ts">
// W-FRONT-02-C: App.vue 只保留 router-view。
// 业务页（登录 / 实时数据 / 等 8 个路由）由对应 view 接管。

// W-PERF-B: 全局 WS 单例生命周期。
// App 挂载时启动 /ws?type=screen 连接；卸载时强制断开（force=true，忽略订阅计数）。
// 路由切换不会触发重连：store 内的 subscribe / unsubscribe 仅增减订阅者，
// controller 在 App 生命周期内全程存活。
import { onBeforeUnmount, onMounted } from 'vue'
import { connectScreenSingleton, disconnectScreenSingleton } from './stores/screen'
// W-RT-8: 报警徽章数据源（AlarmHint 依赖；与 screen 同款全局单例）
import { connectAlarmSingleton, disconnectAlarmSingleton } from './stores/alarm'

onMounted(() => {
  connectScreenSingleton()
  connectAlarmSingleton()
})

onBeforeUnmount(() => {
  // force=true：App 卸载时强制关闭 WS（不再考虑订阅计数）
  disconnectScreenSingleton(true)
  disconnectAlarmSingleton(true)
})
</script>

<style lang="scss">
#app {
  min-height: 100vh;
  width: 100%;
}
</style>
