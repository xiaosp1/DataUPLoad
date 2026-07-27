<template>
  <section class="glass-page">
    <header v-if="title || subtitle || $slots.header" class="glass-page__header">
      <slot name="header">
        <div class="glass-page__title-row">
          <h2 class="glass-page__title">{{ title }}</h2>
          <slot name="actions" />
        </div>
        <p v-if="subtitle" class="glass-page__subtitle">{{ subtitle }}</p>
      </slot>
    </header>

    <div class="glass-page__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
interface Props {
  /** 主标题 */
  title?: string
  /** 副标题 / 描述 */
  subtitle?: string
}

withDefaults(defineProps<Props>(), {
  title: '',
  subtitle: ''
})
</script>

<style lang="scss" scoped>
.glass-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  width: 100%;
  min-height: 100%;
}

.glass-page__header {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: 20px 24px;
  background: rgba(0, 0, 0, 0.20);
  backdrop-filter: var(--glass-blur-soft);
  -webkit-backdrop-filter: var(--glass-blur-soft);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
}

.glass-page__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.glass-page__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.2px;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.glass-page__subtitle {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0;
}

.glass-page__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
