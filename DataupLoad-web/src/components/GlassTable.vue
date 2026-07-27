<template>
  <div class="glass-table-wrapper">
    <el-table
      v-bind="$attrs"
      class="glass-table"
      :data="data"
    >
      <slot />
    </el-table>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 表格数据（透传给 el-table） */
  data?: any[]
}

withDefaults(defineProps<Props>(), {
  data: () => []
})
</script>

<style lang="scss" scoped>
.glass-table-wrapper {
  position: relative;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--glass-shadow);
  padding: 4px;
  backdrop-filter: var(--glass-blur-light);
  overflow: hidden;
}

.glass-table {
  width: 100%;
  background: transparent !important;

  :deep(th.el-table__cell) {
    background: rgba(255, 255, 255, 0.06) !important;
    color: var(--text-secondary) !important;
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-semibold);
    letter-spacing: 0.4px;
    text-transform: uppercase;
    border-bottom: 1px solid rgba(255, 255, 255, 0.10) !important;
  }

  :deep(td.el-table__cell) {
    background: transparent !important;
    color: var(--text-primary) !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
    font-size: var(--font-size-base);
    padding: 14px 0 !important;
    transition: background var(--transition-fast);
  }

  :deep(tr) {
    background: transparent !important;
  }

  :deep(tr:hover > td.el-table__cell) {
    background: rgba(92, 225, 255, 0.08) !important;
  }

  // 空状态
  :deep(.el-table__empty-block) {
    background: transparent;
  }

  :deep(.el-table__empty-text) {
    color: var(--text-secondary);
  }

  // 去除默认下边框
  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }
}
</style>
