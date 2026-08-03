# W-FLASH-02 工单 — 全站无差别界面闪烁根治

**完成时间**: 2026-08-03 15:44 GMT+8
**优先级**: P0（老板实测反馈"整个界面闪烁"，不分页面/操作）
**修复方式**: 全站 `backdrop-filter` 降级 + GPU 合成层隔离
**现场确认**: 老板现场机器亲自确认**不再闪烁** ✅

---

## 一、背景

- 老板 8/3 反馈"界面还是闪"——即使 W-FLASH-01（数据源不同步）已修复仍闪
- 关键描述：**"整个界面闪烁"**、**不分页面、不分操作、全屏模式也闪**
- 这排除了单个组件/页面 bug，指向**全局渲染层**问题

## 二、诊断方法（Headless 像素级 + 元素级）

用 Playwright headless 写序列诊断脚本（临时产物，不入库）：

1. **像素级**（`diag-flash-02.mjs`）：1600×900 视口连续采样 30 帧（~130ms/帧），逐像素对比
   - 突发发现：**全局平均亮度恒定 247.2**（排除整屏白/黑闪）
   - 但**相邻帧变化周期性跳升**：修复前每 5 帧出现 `0.09~0.10%` 且左下/左上象限集中（qTL 68~74%），WS 每 5s 推送一次就跳一次 → **周期性整屏小幅抖动**
2. **元素级**（`diag-flash-03.mjs`）：枚举所有带 transition/动画元素，确认运动源
   - 确认 `main-layout__panel` backdrop `blur(40px) saturate(1.8)`（整屏 1552×852 大玻璃层）
   - halo 光晕 `filter: blur(80px)`（480/520px 大圆，z-index:0 在玻璃层底下）
   - 大量组件 `transition: all`

## 三、根因

**全站玻璃层 `backdrop-filter` 严重过重 + 底部大 blur 光晕 → GPU 整屏重采样抖动**

- `.main-layout__panel`（整屏大玻璃面板）+ 每个 `glass-panel`/`glass-card` 都套 `blur(40px) saturate(180%)`
- backdrop-filter 机制：**每当 backdrop 层下方任何像素变化**（WS 每 5s 推数据、切菜单、hover），浏览器都要对整个面板重新做 40px 模糊采样 = **整屏重采样**
- 加上底下两个 `filter: blur(80px)` 的半透明大光晕作为采样源，GPU 负担进一步放大
- headless 主机 GPU 较弱 → 周期性整屏抖闪
- W-FLASH-01 只修了"数据源不同步"（前端 WS 单源 + 后端 NPE + 连接池），**没碰 backdrop-filter 这个全局渲染层** → 所以老板修完还在闪

## 四、改动（3 文件）

### 4.1 `DataupLoad-web/src/styles/tokens.scss`
全站唯一控制点——3 个玻璃模糊变量一次性降级（影响 20 个组件文件）：
```scss
// 修复前
--glass-blur: blur(40px) saturate(180%);
--glass-blur-soft: blur(30px) saturate(160%);
--glass-blur-light: blur(20px) saturate(160%);
// 修复后
--glass-blur: blur(16px) saturate(150%);
--glass-blur-soft: blur(12px) saturate(140%);
--glass-blur-light: blur(8px) saturate(140%);
```

### 4.2 `DataupLoad-web/src/layouts/MainLayout.vue`
- `.main-layout__panel`：加 `will-change: transform; transform: translateZ(0)` — 提为独立稳定 GPU 合成层，避免内容更新反复触发整屏 backdrop 回溯
- `.main-layout__content`：加 `contain: layout paint; will-change: transform; transform: translateZ(0)` — 内容区重绘隔离，不扩散到父玻璃面板
- `.main-layout__halo`：`filter: blur(80px)` → `blur(40px)` + `will-change` — 降底层采样源复杂度

### 4.3 `DataupLoad-web/index.html`（vite 模板）
- lang `en`→`zh-CN`、title `DataupLoad`→`英科手套中控平台` — 根治 build 后重置英文问题（W-FRONT-02-G0 已中文化但每次 build 被覆盖）

## 五、验证

### 5.1 像素 A/B（headless，修复前 vs 修复后）

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 首屏大面积变化 | **14.91%** | **0.01%** |
| 周期性整屏抖峰（WS 5s 触发） | 每 5 帧 0.09~0.10% | **无（最高 0.05%，多数 0.00~0.03%）** |
| 左上角 qTL 周期性跳变 | 68~74% | **全程 0%** |
| 残余变化 | 多象限分散 | 仅右下角 echarts 折线图（数据点更新，正常） |

### 5.2 现场确认（决定性）
老板 8/3 15:44 现场机器亲自确认：**确实不再闪烁** ✅

## 六、部署

```
cd DataupLoad-web && npm run build   # 17.64s, bundle index-BMj2uDIZ.js
清理 web/assets 旧 bundle → 拷贝 dist/* → 部署完成
后端 8080（PID 13724）未重启，SPA 静态覆盖即生效
curl: / /index.html /assets/index-BMj2uDIZ.js → 全 200
index.html: lang=zh-CN + 中文 title 已生效
```

## 七、归档文件

- 本次改动: `src/styles/tokens.scss`、`src/layouts/MainLayout.vue`、`index.html`
- 共享快照: `docs/work-orders/W-FLASH-01/`（上轮完整报告 + 截图）
- 验证脚本（入库）: `DataupLoad-web/verify-w-flash-01.mjs`
- 临时诊断脚本: 不入库（已加 .gitignore）

## 八、残留 / 后续

- P3: 磨砂玻璃效果较原版略减弱（blur 40→16）。如需更"玻璃感"可折中 blur(24px)，但会引入部分重采样负担——建议保持现状（现场已确认视觉可接受）
- P3: 单文件 bundle 2.6MB 超 1500KB 警告（`manualChunks` 代码分割）——改天处理
- 连接池根治（handleDetectData 拆事务 + 索引）为 8/2 深夜完成，需跨夜观察确认长期稳定

## 九、结论 🏁

W-FLASH-02 根治"整个界面闪烁"：真因是**全站 backdrop-filter 过重的 GPU 整屏重采样**，W-FLASH-01 修的"数据源"是另一条独立问题。两者叠加以往老板持续看到闪。本次改 3 文件 + 一次 build 部署即解决，现场确认通过。
