# ADR-0023: 全站 backdrop-filter 降级策略（修整屏界面闪烁）

**日期**: 2026-08-03
**状态**: 已实施
**关联工单**: W-FLASH-02

## 背景

老板反馈"整个界面闪烁"，不分页面、不分操作、全屏模式也闪。像素级 + 元素级诊断确认：**全站玻璃层 `backdrop-filter` 过重导致的 GPU 整屏重采样抖动**。

## 决策

| 项 | 决策 |
|---|---|
| 玻璃模糊主值 | `blur(40px) saturate(180%)` → `blur(16px) saturate(150%)` |
| 玻璃模糊 soft | `blur(30px) saturate(160%)` → `blur(12px) saturate(140%)` |
| 玻璃模糊 light | `blur(20px) saturate(160%)` → `blur(8px) saturate(140%)` |
| 主面板/内容区 | 加 `will-change: transform` + `transform: translateZ(0)` 提为稳定 GPU 合成层 |
| 内容区隔离 | 加 `contain: layout paint` 防重绘扩散到父玻璃面板 |
| 背景 halo | `blur(80px)` → `blur(40px)`，降底层采样源复杂度 |

## 理由

- backdrop-filter 机制：**每当 backdrop 层下方像素变化，浏览器对整面板重新做模糊采样**。实时页 WS 每 5s 推数据、切菜单、hover 都是触发源 → 整屏重采样 → 视觉闪。
- tokens.scss 是全站唯一控制点（3 个 CSS 变量被 20 个组件文件引用），改一处全体生效，避免逐文件修改。
- 磨砂玻璃视觉效果通过降低半径基本保留，但 GPU 每帧采样成本大幅下降。
- 关键修复源集中在 MainLayout 大玻璃面板（1552×852 整屏），通过合成层隔离让内容区重绘不扩散。

## 结果

- 像素 A/B：首屏 14.91%→0.01%；周期性整屏抖峰 0.09~0.10% → 无；左上角抖动 68~74% → 0。
- 老板现场确认不再闪烁。

## 权衡/后续

- 玻璃磨砂感较原版略减弱（blur 40→16）。如需更"玻璃感"可折中 blur(24px)，但会引入部分重采样负担——建议保持现状（现场已确认视觉可接受）。
- 单文件 bundle 2.6MB 超 1500KB 警告，后续可用 `manualChunks` 代码分割。
