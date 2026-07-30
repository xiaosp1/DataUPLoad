<template>
  <router-view />
</template>

<script setup lang="ts">
// W-FRONT-02-C: App.vue 只保留 router-view。
// 业务页（登录 / 实时数据 / 等 8 个路由）由对应 view 接管。
</script>

<style lang="scss">
#app {
  min-height: 100vh;
  width: 100%;
}
</style>
