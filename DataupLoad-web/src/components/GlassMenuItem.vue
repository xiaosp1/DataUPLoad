<template>
  <button
    type="button"
    class="glass-menu-item"
    :class="{ 'glass-menu-item--active': active }"
    @click="onClick"
  >
    <span v-if="icon" class="glass-menu-item__icon">{{ icon }}</span>
    <span class="glass-menu-item__label">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  /** 简单的文字/emoji 图标，例如 "▣"、"⚠"、菜单 icon 也接受 */
  icon?: string
  /** 当前激活态 */
  active?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  icon: '',
  active: false
})

const emit = defineEmits<{
  (e: 'click', evt: MouseEvent): void
}>()

function onClick(evt: MouseEvent) {
  emit('click', evt)
}
</script>

<style lang="scss" scoped>
.glass-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 14px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-family);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  text-align: left;

  &:hover {
    background: var(--glass-bg-hover);
    color: var(--text-primary);
    transform: translateX(2px);
  }

  &:focus-visible {
    outline: 2px solid rgba(92, 225, 255, 0.5);
    outline-offset: 2px;
  }
}

.glass-menu-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 13px;
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.glass-menu-item__label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

// ===== active 态：青色凸起 =====
.glass-menu-item--active {
  background: linear-gradient(135deg, rgba(92, 225, 255, 0.22), rgba(92, 225, 255, 0.08));
  color: var(--accent);
  box-shadow:
    0 4px 16px rgba(92, 225, 255, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  border-color: rgba(92, 225, 255, 0.28);

  &:hover {
    transform: none;
    background: linear-gradient(135deg, rgba(92, 225, 255, 0.28), rgba(92, 225, 255, 0.12));
    color: var(--accent);
  }

  .glass-menu-item__icon {
    background: rgba(92, 225, 255, 0.18);
  }
}
</style>
