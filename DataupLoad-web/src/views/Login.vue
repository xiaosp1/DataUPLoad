<template>
  <div class="login-shell">
    <!-- 顶部装饰：两层径向渐变模拟苹果系光晕 -->
    <div class="login-shell__halo login-shell__halo--1" />
    <div class="login-shell__halo login-shell__halo--2" />

    <GlassCard
      :padding="0"
      class="login-card"
    >
      <!-- 标题区 -->
      <div class="login-card__header">
        <div class="login-card__logo">▣</div>
        <h1 class="login-card__title">DataupLoad</h1>
        <p class="login-card__subtitle">实时数据采集 · 缺陷检测 · 报警管理</p>
      </div>

      <!-- 表单区 -->
      <form class="login-card__form" @submit.prevent="onSubmit">
        <div class="login-card__field">
          <label class="login-card__label" for="login-username">用户名</label>
          <div class="login-card__input-wrap">
            <span class="login-card__input-icon">☷</span>
            <input
              id="login-username"
              v-model.trim="username"
              class="login-card__input"
              type="text"
              autocomplete="username"
              placeholder="super_admin"
              :disabled="submitting"
              @keyup.enter="onSubmit"
            />
          </div>
        </div>

        <div class="login-card__field">
          <label class="login-card__label" for="login-password">密码</label>
          <div class="login-card__input-wrap">
            <span class="login-card__input-icon">⚿</span>
            <input
              id="login-password"
              v-model="password"
              class="login-card__input"
              :type="showPwd ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="Abc12345"
              :disabled="submitting"
              @keyup.enter="onSubmit"
            />
            <button
              type="button"
              class="login-card__toggle"
              :aria-label="showPwd ? '隐藏密码' : '显示密码'"
              @click="showPwd = !showPwd"
            >
              {{ showPwd ? '🙈' : '👁' }}
            </button>
          </div>
        </div>

        <div v-if="errorMsg" class="login-card__error" role="alert">
          ⚠ {{ errorMsg }}
        </div>

        <GlassButton
          variant="primary"
          class="login-card__submit"
          :loading="submitting"
          :disabled="submitting || !username || !password"
          @click="onSubmit"
        >
          {{ submitting ? '登录中…' : '登 录' }}
        </GlassButton>

        <p class="login-card__hint">
          默认账号 <code>super_admin</code> / <code>Abc12345</code>
        </p>
      </form>
    </GlassCard>

    <p class="login-shell__footer">
      © {{ year }} DataupLoad · v0.1.0 · 玻璃风登录
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { GlassCard, GlassButton } from '../components'
import { login } from '../api/auth'
import { useUserStore } from '../stores/user'
// W-ALARM-PUSH-FRONT：登录成功后重拉报警基线（App.vue onMounted 时的 baseline 可能因未登录 10401 失败）
import { loadAlarmBaseline } from '../stores/alarm'

const router = useRouter()
const userStore = useUserStore()

const username = ref<string>('')
const password = ref<string>('')
const showPwd = ref<boolean>(false)
const submitting = ref<boolean>(false)
const errorMsg = ref<string>('')

const year = computed(() => new Date().getFullYear())

async function onSubmit(): Promise<void> {
  if (submitting.value) return
  if (!username.value || !password.value) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  errorMsg.value = ''
  submitting.value = true
  try {
    // 调用登录 API（auth.ts 内部做 SHA-256 + withCredentials）
    const resp = await login(username.value, password.value)
    // 后端 BaseResult 成功响应是 { success: true, code: 0, message: '...', data: ... }
    // 不是 HTTP 200；必须同时检查 success 和 code
    if (resp && resp.success === true && resp.code === 0) {
      // 后端已通过 Set-Cookie 写入 satoken，浏览器自动带
      // 拉一次 /web/account/current 同步 role/permission 到 user store
      // fetchCurrent 内部会把 role 同步到 permission store（修 D-tier 第二个 bug）
      try {
        await userStore.fetchCurrent()
      } catch (e) {
        // 拉取失败不阻塞登录跳转（最坏情况路由守卫跳 /403，用户可重新登录）
        console.warn('[login] fetchCurrent failed', e)
      }
      // W-ALARM-PUSH-FRONT：登录成功后再拉一次报警基线（此时 satoken cookie 已带）
      // 修复：未登录访问时 connectAlarmSingleton 里 baseline 10401 失败 → 悬窗永远空
      void loadAlarmBaseline()
      // 直接跳到默认主页
      await router.push({ name: 'RealTime' })
    } else {
      errorMsg.value = resp?.message || resp?.msg || '登录失败，请检查账号密码'
    }
  } catch (err: any) {
    // 401 已被 axios 拦截器跳走；这里处理其他错误
    const status = err?.response?.status
    if (status === 401) {
      errorMsg.value = '账号或密码错误'
    } else {
      errorMsg.value = err?.response?.data?.msg || err?.message || '网络异常，请稍后再试'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: var(--space-5);
  overflow: hidden;
  isolation: isolate;
}

// 顶部装饰光晕
.login-shell__halo {
  position: absolute;
  pointer-events: none;
  z-index: -1;
  filter: blur(80px);
  opacity: 0.55;

  &--1 {
    top: -120px;
    left: -120px;
    width: 480px;
    height: 480px;
    background: radial-gradient(circle, rgba(92, 225, 255, 0.45) 0%, transparent 70%);
  }

  &--2 {
    bottom: -160px;
    right: -120px;
    width: 520px;
    height: 520px;
    background: radial-gradient(circle, rgba(255, 110, 199, 0.30) 0%, transparent 70%);
  }
}

// 主卡片
.login-card {
  width: 100%;
  max-width: 420px;
  padding: 0;

  :deep(.glass-card) {
    padding: 0;
  }
}

.login-card__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) var(--space-6) var(--space-5);
  text-align: center;
  border-bottom: 1px solid var(--glass-border);
}

.login-card__logo {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: var(--gradient-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--text-on-accent);
  box-shadow: 0 8px 24px rgba(92, 225, 255, 0.35);
  margin-bottom: var(--space-2);
}

.login-card__title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.login-card__subtitle {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

// 表单
.login-card__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-6);
}

.login-card__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.login-card__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

.login-card__input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--input-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    background var(--transition-base);

  &:focus-within {
    border-color: var(--accent);
    background: var(--input-bg-focus);
    box-shadow: 0 0 0 4px var(--accent-focus-ring);
  }
}

.login-card__input-icon {
  flex-shrink: 0;
  width: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text-secondary);
}

.login-card__input {
  flex: 1;
  height: 44px;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: var(--font-size-base);
  padding-right: var(--space-3);

  &::placeholder {
    color: rgba(255, 255, 255, 0.35);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.login-card__toggle {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  margin-right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 16px;
  transition: background var(--transition-fast);

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: var(--text-primary);
  }
}

.login-card__error {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: rgba(255, 90, 95, 0.12);
  border: 1px solid rgba(255, 90, 95, 0.35);
  color: var(--danger);
  font-size: var(--font-size-sm);
}

.login-card__submit {
  width: 100%;
  height: 46px;
  font-size: var(--font-size-md);
  margin-top: var(--space-2);
}

.login-card__hint {
  margin: 0;
  text-align: center;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);

  code {
    padding: 1px 6px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.08);
    color: var(--accent);
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  }
}

.login-shell__footer {
  margin-top: var(--space-6);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  opacity: 0.7;
}
</style>
