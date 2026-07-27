# W-FRONT-02-B brief — 设计 token + 玻璃组件库

- **任务**: 在 `DataupLoad-web/src/` 创建苹果系玻璃风格设计 tokens + 5 个核心玻璃组件
- **依赖**: W-FRONT-02-A（脚手架，已完成）
- **耗时上限**: 2 小时
- **必读**:
  - W-FRONT-02-brief.md
  - ADR-0016（前端对齐 PSM SPA）
  - ADR-0018（方案 X-1 临时过渡）
  - 风格基准图：`E:\DEMO\数据采集\style-sample-login.png` + `style-sample-main.png`

## 设计 tokens（必须严格按这个）

### colors.scss
```scss
// 背景
--bg-base: #1d1d1f;
--bg-gradient: radial-gradient(at 20% 0%, #2c3e6f 0%, transparent 50%),
               radial-gradient(at 80% 100%, #1e3a5f 0%, transparent 50%),
               linear-gradient(135deg, #0b1426 0%, #1d1d1f 50%, #2a1f3d 100%);

// 玻璃
--glass-bg: rgba(255, 255, 255, 0.08);
--glass-bg-hover: rgba(255, 255, 255, 0.12);
--glass-border: rgba(255, 255, 255, 0.18);
--glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
--glass-blur: blur(40px) saturate(180%);

// 文字
--text-primary: rgba(255, 255, 255, 0.92);
--text-secondary: rgba(255, 255, 255, 0.62);

// 主色
--accent: #5ce1ff;
--accent-hover: #8ee4ff;
--accent-2: #ff6ec7;
--success: #5fd97f;
--warning: #ffb74d;
--danger: #ff5a5f;
```

### spacing.scss
```scss
--space-1: 4px; --space-2: 8px; --space-3: 12px; --space-4: 16px;
--space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px;
```

### radius.scss
```scss
--radius-sm: 8px; --radius-md: 12px; --radius-lg: 16px;
--radius-xl: 20px; --radius-pill: 999px;
```

### typography.scss
```scss
--font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display',
               'PingFang SC', 'Microsoft YaHei', sans-serif;
--font-size-xs: 11px; --font-size-sm: 12px; --font-size-base: 14px;
--font-size-md: 15px; --font-size-lg: 18px; --font-size-xl: 24px;
--font-size-2xl: 28px;
```

## 5 个核心组件 API

### GlassCard.vue
```vue
<GlassCard :padding="20" :hover="false" :glass="true">
  内容
</GlassCard>
```
- props: `padding` (number, default 20), `hover` (bool, default false), `glass` (bool, default true)
- 默认：`background: var(--glass-bg); backdrop-filter: var(--glass-blur); border: 1px solid var(--glass-border); border-radius: var(--radius-xl); box-shadow: var(--glass-shadow);`

### GlassButton.vue（封装 el-button）
```vue
<GlassButton variant="primary" @click="onSubmit">登录</GlassButton>
```
- variants: `primary`（青色渐变）/ `default`（玻璃）/ `danger`（红色）
- 必须 100% 兼容 el-button 的 props（v-bind="$attrs"）

### GlassMenuItem.vue（药丸菜单）
```vue
<GlassMenuItem icon="▣" :active="isActive" @click="navigate">实时数据</GlassMenuItem>
```
- 药丸形状（border-radius: 999px）
- active 态：青色渐变 + 外阴影 + 内顶高光 + 青色边框

### GlassTable.vue（封装 el-table）
- 默认玻璃表头 + 半透明行 hover
- 兼容 el-table 所有 props

### GlassPage.vue（页面容器）
- props: `title` (string), `subtitle` (string)
- 内容：玻璃 header + 玻璃 content 区

## 必产出

1. `src/styles/tokens.scss`（全部 tokens）
2. `src/styles/element-overrides.scss`（重写 Element Plus 主题）
3. `src/styles/global.scss`（全局样式 + body 背景）
4. `src/components/GlassCard.vue`
5. `src/components/GlassButton.vue`
6. `src/components/GlassMenuItem.vue`
7. `src/components/GlassTable.vue`
8. `src/components/GlassPage.vue`
9. `src/components/index.ts`（统一导出）
10. **`docs/work-orders/W-FRONT-02-B-report.md`**（done criteria 逐条勾选）
11. **新截图** `docs/work-orders/W-FRONT-02-B-sample.png`（用 App.vue 临时演示，截图给老板看）

## done criteria（13 项）

- [ ] tokens.scss 含 colors/spacing/radius/typography 全部变量
- [ ] element-overrides.scss 重写 Element Plus 主色/背景/边框/文字
- [ ] global.scss 注入到 main.js
- [ ] GlassCard 在 App.vue 里能正常显示
- [ ] GlassButton 3 个 variant 都能渲染
- [ ] GlassMenuItem active 态有青色凸起效果
- [ ] GlassTable 渲染 el-table 默认样式但玻璃化
- [ ] GlassPage title + subtitle 渲染正确
- [ ] components/index.ts 统一导出 5 个组件
- [ ] main.js 全局注册 5 个组件
- [ ] npm run dev 启动后访问 5173 看到玻璃效果
- [ ] 截图 sample.png 提交到 docs/work-orders/
- [ ] verify-w-front-02-B.ps1 全 PASS

## 编码规范

- 全部 `<script setup lang="ts">` TypeScript
- Props 用 `defineProps<T>()` + interface
- 命名 PascalCase 组件 / camelCase props
- 不引入 brief 之外的依赖
- 不用 Element Plus icons-vue（业务图标后续再说）

## 不在本子单范围

- 不实现 Login.vue（C 子单）
- 不实现业务页（D/E 子单）
- 不打包部署（F 子单）
- 不改 vite.config.js（A 子单已配）

## PM 验收脚本

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-w-front-02-B.ps1
```

预期 13/13 PASS。

## 完成后回 PM

> "W-FRONT-02-B 完成，report 路径 docs/work-orders/W-FRONT-02-B-report.md，sample 截图 docs/work-orders/W-FRONT-02-B-sample.png，dev server PID <PID>"

PM 验收通过后才并行派 C / D / F / G0。

