<template>
  <div class="forbidden">
    <!-- 背景装饰 -->
    <div class="forbidden__halo forbidden__halo--1" />
    <div class="forbidden__halo forbidden__halo--2" />

    <div class="forbidden__panel glass-panel">
      <div class="forbidden__code">403</div>
      <h1 class="forbidden__title">{{ $t('forbidden.title') }}</h1>
      <p class="forbidden__subtitle">{{ $t('forbidden.subtitle') }}</p>
      <p class="forbidden__desc">{{ $t('forbidden.description') }}</p>

      <div class="forbidden__actions">
        <GlassButton variant="primary" @click="goHome">
          {{ $t('forbidden.backHome') }}
        </GlassButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-D 403 页面
//   - 公开路由（meta.public = true）
//   - 不在 MainLayout 内（被踢出时大概率还没权限进入布局）
// =============================================================================
import { useRouter } from 'vue-router'
import GlassButton from '../components/GlassButton.vue'

const router = useRouter()

function goHome() {
  // 已登录用户回主页，未登录用户去登录页
  if (typeof document !== 'undefined' && document.cookie.includes('satoken')) {
    router.push({ name: 'RealTime' })
  } else {
    router.push({ name: 'Login' })
  }
}
</script>

<style lang="scss" scoped>
.forbidden {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  overflow: hidden;
}

.forbidden__halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
}
.forbidden__halo--1 {
  top: -10%;
  left: -10%;
  width: 480px;
  height: 480px;
  background: rgba(255, 90, 95, 0.18);
}
.forbidden__halo--2 {
  bottom: -15%;
  right: -10%;
  width: 520px;
  height: 520px;
  background: rgba(255, 110, 199, 0.16);
}

.forbidden__panel {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-10) var(--space-8);
  max-width: 520px;
  width: 100%;
  gap: var(--space-3);
}

.forbidden__code {
  font-size: 96px;
  font-weight: var(--font-weight-bold);
  line-height: 1;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: var(--space-3);
}

.forbidden__title {
  font-size: var(--font-size-2xl);
  color: var(--text-primary);
  margin: 0;
  font-weight: var(--font-weight-bold);
}

.forbidden__subtitle {
  font-size: var(--font-size-md);
  color: var(--accent);
  margin: 0;
}

.forbidden__desc {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0 0 var(--space-5) 0;
  max-width: 380px;
}

.forbidden__actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
}
</style>
