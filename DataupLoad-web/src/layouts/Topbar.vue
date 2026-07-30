<template>
  <div class="topbar">
    <!-- 左：面包屑 -->
    <div class="topbar__left">
      <el-breadcrumb separator="/" class="topbar__crumb">
        <el-breadcrumb-item :to="{ path: '/realtime' }">
          {{ $t('breadcrumb.home') }}
        </el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentCrumb">
          {{ currentCrumb }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右：操作区 -->
    <div class="topbar__right">
      <!-- 语言切换 -->
      <el-select
        v-model="locale"
        size="small"
        class="topbar__locale"
        @change="onLocaleChange"
      >
        <el-option
          v-for="loc in localeOptions"
          :key="loc.value"
          :label="loc.label"
          :value="loc.value"
        />
      </el-select>

      <!-- 全屏切换 -->
      <button
        class="topbar__icon-btn"
        :title="isFullscreen ? $t('topbar.exitFullscreen') : $t('topbar.fullscreen')"
        @click="toggleFullscreen"
      >
        <span>{{ isFullscreen ? '⤓' : '⤢' }}</span>
      </button>

      <!-- 用户菜单 -->
      <el-dropdown trigger="click" @command="onUserCommand">
        <div class="topbar__user">
          <div class="topbar__avatar">
            {{ avatarLetter }}
          </div>
          <div class="topbar__user-meta">
            <div class="topbar__user-name">{{ username }}</div>
            <div class="topbar__user-role">{{ roleLabel }}</div>
          </div>
          <span class="topbar__user-caret">▾</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :disabled="true">
              {{ $t('topbar.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              {{ $t('topbar.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// W-FRONT-02-D 顶栏
//   - 左：面包屑（首页 + 当前页）
//   - 右：语言切换 / 全屏切换 / 用户下拉（头像 + 用户名 + 角色 + 退出）
// =============================================================================
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t, locale: i18nLocale } = useI18n()

type Locale = 'zh-CN' | 'en-US' | 'id-ID'
const locale = ref<Locale>((i18nLocale.value as Locale) || 'zh-CN')

const localeOptions: { value: Locale; label: string }[] = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' },
  { value: 'id-ID', label: 'Bahasa Indonesia' }
]

const username = computed(() => userStore.username || 'Guest')
const avatarLetter = computed(() => {
  const n = username.value || 'G'
  return n.charAt(0).toUpperCase()
})
const roleLabel = computed(() => {
  const r = userStore.role
  if (!r) return ''
  // 角色可能是 'super_admin' / 'admin' / 'operator' 等
  const key = `role.${r}`
  return t(key)
})

const crumbMap: Record<string, string> = {
  RealTime: 'breadcrumb.realtime',
  Alarm: 'breadcrumb.alarm',
  Defect: 'breadcrumb.defect',
  Account: 'breadcrumb.account',
  SystemConfig: 'breadcrumb.systemConfig',
  Log: 'breadcrumb.log',
  UserManage: 'breadcrumb.userManage',
  Screen: 'breadcrumb.screen'
}

const currentCrumb = computed(() => {
  const key = crumbMap[String(route.name)] || ''
  return key ? t(key) : ''
})

function onLocaleChange(val: Locale) {
  i18nLocale.value = val
  try {
    localStorage.setItem('app.locale', val)
  } catch {
    /* ignore */
  }
}

// 监听 i18n 外部变化（同步 ref）
watch(i18nLocale, (v) => {
  if (v && v !== locale.value) {
    locale.value = v as Locale
  }
})

// 全屏
const isFullscreen = ref(false)

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.().catch(() => {
      /* 用户拒绝或浏览器不支持 */
    })
  } else {
    document.exitFullscreen?.().catch(() => {
      /* ignore */
    })
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
})
onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})

// 用户菜单
async function onUserCommand(cmd: string) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm(
        t('topbar.logoutConfirm'),
        t('topbar.logout'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning'
        }
      )
    } catch {
      return // 用户取消
    }
    userStore.reset()
    // satoken 由后端失效，前端只负责清本地态 + 跳登录
    router.push({ name: 'Login' })
  }
}
</script>

<style lang="scss" scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  width: 100%;
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  // sticky 模糊玻璃风（沿用全局 token）
  background: rgba(0, 0, 0, 0.10);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-radius: var(--radius-lg);
  padding: var(--space-2) var(--space-3);
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.topbar__crumb {
  font-size: var(--font-size-base);
  :deep(.el-breadcrumb__inner) {
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
  }
  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: var(--text-primary);
    font-weight: var(--font-weight-semibold);
  }
  :deep(.el-breadcrumb__separator) {
    color: var(--text-secondary);
    opacity: 0.6;
  }
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

// 语言切换
.topbar__locale {
  width: 130px;
  :deep(.el-select__wrapper) {
    background: rgba(255, 255, 255, 0.06);
    box-shadow: 0 0 0 1px var(--glass-border) inset;
    border-radius: var(--radius-md);
  }
  :deep(.el-select__placeholder),
  :deep(.el-select__selected-item.el-select__placeholder span) {
    color: var(--text-primary);
  }
}

// 图标按钮
.topbar__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--glass-border);
  color: var(--text-primary);
  font-size: var(--font-size-md);
  transition: all var(--transition-base);
  &:hover {
    background: rgba(92, 225, 255, 0.18);
    border-color: var(--accent-border);
    color: var(--accent);
  }
}

// 用户
.topbar__user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-1);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--glass-border);
  cursor: pointer;
  transition: all var(--transition-base);
  &:hover {
    background: rgba(92, 225, 255, 0.14);
    border-color: var(--accent-border);
  }
}

.topbar__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gradient-brand);
  color: var(--text-on-accent);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.topbar__user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  min-width: 0;
}

.topbar__user-name {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar__user-role {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.topbar__user-caret {
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}

@media (max-width: 768px) {
  .topbar__user-meta,
  .topbar__user-caret {
    display: none;
  }
}
</style>
