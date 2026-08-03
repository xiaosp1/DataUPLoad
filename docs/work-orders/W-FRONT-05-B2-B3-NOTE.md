# B2/B3 必读（PM 拍板：颜色 + 真实值双显）

## 老板 11:05 确认
> "上座率除了用黄绿红灯显示外，也一定显示器真实值哈"

## B2（顶部全宽条）渲染规则
- **默认**：格子**只显示颜色**（红/黄/绿玻璃块），hover 出 tooltip：线号 + 面 + 真实值% + 更新时间
- **可切换**：右上角加个开关 `显示数值`，打开后格子内**直接显示数字**（如 `97.3`）
- 顶部全宽条 38 格 × ~20px 一格，**空间小**，默认不开数字
- 鼠标 hover 任何格子 → 浮 tooltip 显数字 + 元数据
- 点击格子 → 跳 `/production-board` 并定位到该线

## B3（生产看板独立页面）渲染规则
- **页面默认显示数值**（格子够大，38×4=152 格，~80px 一格）
- 排序：高到低 / 低到高 / 按线号
- 趋势曲线：每线 4 面的最近 1h 趋势（接口待 B3 brief 细化）
- 右上角 `显示数值` 开关默认**开**

## B4（阈值配置 UI）
- 3 个滑杆（warnThreshold / goodThreshold / refreshInterval）
- 实时预览 5 条样例数据颜色变化
- 调完一键保存 → POST `/web/occupancy/thresholds`

## 配置接口（来自 B1）
```json
{
  "warnThreshold": 80,    // < 此值为红
  "goodThreshold": 95,    // >= 此值为绿；中间为黄
  "refreshInterval": 5,   // 5s
  "showValue": true       // 格子内是否显示数值
}
```

## 通用规则
- 颜色阈值：`< warnThreshold` 红；`>= warnThreshold && < goodThreshold` 黄；`>= goodThreshold` 绿
- 真实值 null/无数据：灰色（不在三色阈值内）
- 数字保留 1 位小数（99.8）
