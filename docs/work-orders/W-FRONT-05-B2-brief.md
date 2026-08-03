# W-FRONT-05-B2 顶部全宽条（上座率 Panorama Bar）

## 背景
老板需求：上座率单独放主页醒目位置，一眼看见全部线体。B1 已确认数据来源：
- `GET /web/line/list?pageNum=1&pageSize=200` → 38 条 line，每条 `realtimeData` JSON 字符串含 `occupancyRate`
- `GET /web/system-config` → 4 条上座率阈值配置（warn=80 / good=95 / refresh=5s / show_value=true）

## 位置
`src/views/RealTime.vue` **顶部全宽条**（在 MainLayout 顶部、Topbar 下方、主内容区上方）。

## 布局（ASCII）
```
┌─────────────────────────────────────────────────────────────────┐
│  上座率全景                                    [显示数值] [收起 ⚙] │  ← 标题行+开关
│  ██ ██ ██ ◼◼ ◼◼ ██ ██ ◼◼ ... (38 格热力条)                       │  ← 38 线热力条（可横向滚动）
│  line1A  line1B  line2A ...                                      │  ← 线号标签（可选，hover 显）
│  红:2  黄:5  绿:145        [打开生产看板]                           │  ← 汇总+入口
└─────────────────────────────────────────────────────────────────┘
```

## 功能需求
1. **热力条**：38 条线，每线 1 格（取该线 4 面平均 occupancyRate）
   - 颜色：`< warn_threshold` 红；`>= warn_threshold && < good_threshold` 黄；`>= good_threshold` 绿
   - 无数据（occupancyRate 缺失/0 且该线无生产）= 灰色
2. **颜色 + 真实值双显**（老板 11:05 拍板，B2-B3-NOTE.md）：
   - 默认：格子只显示颜色，hover tooltip 显示线号+面+真实值%+时间
   - `[显示数值]` 开关：打开后格子内直接显示数字（如 `97.3`）
3. **hover**：tooltip 显示 `lineNo / faceNo / occupancyRate% / updateTime`
4. **点击**：点击某格跳 `/production-board`（B3 页面）并定位到该线
5. **汇总**：右上角红/黄/绿计数 + 平均上座率
6. **阈值配置入口**：右上角齿轮 ⚙ → 弹 `OccupancyThresholdDialog`（B4 做，B2 先放占位按钮触发事件）
7. **可滚动**：38 格超宽时横向滚动，支持鼠标滚轮

## 数据加载
- 首次：`GET /web/line/list` + `GET /web/system-config` 并行拉取
- **不轮询不整页刷新**（W-FRONT-05-A 已修全刷）：B2 先做定时刷新（`setInterval` 5s 拉 line/list，只更新数据不复位组件）；B4 接 WS 增量后替换

## 视觉
- 玻璃风：半透明底 + backdrop-blur + 圆角 + 视觉对齐 MainLayout 现有风格
- 高度 ~72px（标题行 ~28px + 热力条 ~44px）
- 可收起（顶部一个 toggle，收起后只剩标题行）

## 必须引用
- `docs/work-orders/W-FRONT-05-B2-B3-NOTE.md`（渲染规则）
- `src/views/RealTime.vue`（现有结构，别破坏）
- `src/api/realtime.ts`（`LineItem` / `parseRealtimeData`）
- `src/api/screen.ts`（`ScreenLine` / occupancyRate 解析示例）

## 输出物
- 新组件 `src/components/OccupancyPanoramaBar.vue`
- 修改 `src/views/RealTime.vue`（挂载组件）
- i18n 三语 key（zh-CN/en-US/id-ID）：occupancy 相关 label
- build 通过 + 截图 1 张
- report：`docs/work-orders/W-FRONT-05-B2-report.md`

## 耗时上限
1.5 小时

## 边界
- **不做 B3 独立页**（范围外）
- **不做阈值配置弹窗**（B4 做，B2 只放占位事件）
- **不接 WS**（B4 做，B2 先定时刷新）
- **不动后端**
- **不 location.reload / router.go(0)**
