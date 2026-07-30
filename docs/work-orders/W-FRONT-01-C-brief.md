# W-FRONT-01-C — i18n 三语复用 PSM

- **父工单**: W-FRONT-01（依赖 W-FRONT-01-B 完成）
- **目标**: Login 页支持 zh-CN / en-US / id-ID 三语切换，立即生效

## 任务清单

### C1. 创建 `src/i18n/index.js`
```js
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'
import idID from './id-ID'

const saved = localStorage.getItem('lang') || 'zh-CN'
export const i18n = createI18n({
  legacy: false,
  locale: saved,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'id-ID': idID,
  },
})
```

### C2. 三个语言文件

**字符串来源**：PSM 编译产物已经提取（参考 `E:\DEMO\数据采集\docs\domain\海康大屏逆向\PSM\server\web\js\index.f19ecd42-20260520160358.js` 第 2 段那个长字符串），直接 copy 关键 key：

```js
// src/i18n/zh-CN.js
export default {
  login: {
    title: '账号登录',
    platformName: '英科手套检测中控平台',
    username: '账号',
    password: '密码',
    login: '登录',
    reset: '重置',
    usernamePlaceholder: '请输入账号',
    passwordPlaceholder: '请输入密码',
    validation: {
      usernameRequired: '请输入账号',
      passwordRequired: '请输入密码',
    },
    messages: {
      loginFailed: '登录失败',
      loginError: '登录出错',
    },
  },
  header: {
    logout: '退出登录',
  },
}
```

```js
// src/i18n/en-US.js
export default {
  login: {
    title: 'Account Login',
    platformName: 'Volkswagen Yingke Version Operation Platform',
    username: 'Username',
    password: 'Password',
    login: 'Login',
    reset: 'Reset',
    usernamePlaceholder: 'Please enter username',
    passwordPlaceholder: 'Please enter password',
    validation: {
      usernameRequired: 'Please enter username',
      passwordRequired: 'Please enter password',
    },
    messages: {
      loginFailed: 'Login failed',
      loginError: 'Login error',
    },
  },
  header: {
    logout: 'Logout',
  },
}
```

```js
// src/i18n/id-ID.js
export default {
  login: {
    title: 'Login Akun',
    platformName: 'Platform Operasi Visi Volkswagen Yingke',
    username: 'Nama Pengguna',
    password: 'Kata Sandi',
    login: 'masuk',
    reset: 'reset',
    usernamePlaceholder: 'Silakan masukkan nama pengguna',
    passwordPlaceholder: 'Silakan masukkan kata sandi',
    validation: {
      usernameRequired: 'Silakan masukkan nama pengguna',
      passwordRequired: 'silahkan masukkan kata sandi',
    },
    messages: {
      loginFailed: 'Login gagal',
      loginError: 'Kesalahan login',
    },
  },
  header: {
    logout: 'Keluar',
  },
}
```

### C3. Login.vue 改造
- 顶部加 `<el-select v-model="lang" @change="changeLang">` 三项：`zh-CN` / `en-US` / `id-ID`
- 所有硬编码中文替换为 `$t('login.xxx')`
- 表单校验 message 同步走 `$t('login.validation.xxx')`
- 错误 alert 走 `$t('login.messages.loginFailed')`
- `changeLang(d)`：`localStorage.setItem('lang', d); i18n.global.locale.value = d`

### C4. RealTime.vue 改造
- "退出"按钮文案走 `$t('header.logout')`
- 顶部加 `$t('login.title')` 或 `'RealTime'` 兜底

## 不交付

- ❌ 持久化用户偏好到后端（localStorage 够用）
- ❌ 业务页的 i18n（那是 W-FRONT-02+）

## 验收

1. `npm run dev` → `/Login` → 默认中文
2. 切到 `en-US` → 标题变 `Account Login`、按钮 `Login`、占位符英文
3. 切到 `id-ID` → `Login Akun` / `masuk`
4. 校验提示也跟语言走（清空账号 blur → 显示对应语言）
5. 刷新页面 → localStorage 记住语言

## 报告

`docs/work-orders/W-FRONT-01-C-report.md`：
- 三个语言文件贴出
- 三语言切换的 3 张截图

## 耗时上限

40 分钟
