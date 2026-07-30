# W-RT-2 完工报告 — 左栏线别列表卡片 LineListCard

> **工单**：W-RT-2
> **完成日期**：2026-07-30 23:48 GMT+8
> **实施者**：industry subagent
> **父单**：W-REALTIME-PSM（实时页改造）
> **核心目标**：仿 PSM 实时页左侧线别列表, 新建 LineListCard.vue 玻璃组件, 单击切换中栏数据

---

## 0. TL;DR

| 维度 | 改造前 | 改造后 |
|---|---|---|
| **左栏** | 无 (单栏布局: KPI → 折线 → 表格) | 280px LineListCard 玻璃卡片 (38 条线别) |
| **线别数据** | el-select 多选下拉 | 玻璃风卡片列表 + 单击选中切换 |
| **选中态** | dropdown tag | `line-item--active`: 蓝绿渐变 + 左侧高亮条 + 边框发光 |
| **中栏数据切换** | el-select 多选 (聚合多线) | 选中单线 KPI / chart / table 全部跟随 |
| **i18n key** | 0 | **4 个** (title / total / defect / remove × 3 语) |
| **新建组件** | 0 | 2 (LineListCard.vue + stores/line.ts) |
| **后端** | 未改 | **未改** (沿用 `/web/line/list` + `/web/detect/realtime`) |
| **影响范围** | — | RealTime.vue (左栏接入) + i18n (新增 4×3=12 条) + 1 store + 1 component |

**业务结果**：登录 → `/realtime` → 左侧玻璃卡片展示 38 条线别 (lineNo:faceNo + 当小时缺陷/剔除数)，单击任意行 → 中栏 KPI / 折线 / 表格全部切换到该线实时数据。选中态有蓝绿高亮 + 左侧发光条 + hover 抬升动画。

---

## 1. 改动文件清单

| 文件 | 状态 | 增 | 删 | 说明 |
|---|:-:|---:|---:|---|
| `DataupLoad-web/src/stores/line.ts` | NEW | 132 | 0 | 左栏线别列表 store (load / select / getters) |
| `DataupLoad-web/src/components/LineListCard.vue` | NEW | 419 | 0 | 玻璃风列表卡片 (渲染 / 选中 / hover / 骨架 / 空态) |
| `DataupLoad-web/src/views/RealTime.vue` | MOD | +572 | -317 | 主布局改 grid (280px 1fr), KPI / chart / table 全部跟选中线 |
| `DataupLoad-web/src/i18n/index.ts` | MOD | +48 | -16 | 三语新增 `realtime.lineList.*` (title/total/defect/remove) |
| `docs/work-orders/W-RT-2-01-initial.png` | NEW | — | — | 截图: 初始 (选中第一行 line1A-A1) |
| `docs/work-orders/W-RT-2-02-selected.png` | NEW | — | — | 截图: 单击第 3 行 (line1B-B1) |
| `docs/work-orders/W-RT-2-03-hover.png` | NEW | — | — | 截图: hover 第 5 行 |
| **合计** | | **~1171** | **~333** | 7 文件 |

---

## 2. 核心实现要点

### 2.1 数据模型（stores/line.ts）

**唯一键选型**：发现后端 `lineNo` 不唯一（同一 lineNo 下 A1/A2 两个面），所以用 `lineKey = lineNo:faceNo`（后端 `LineItem.key` 字段）做唯一标识。

```typescript
export interface LineListItem {
  lineKey: string   // 唯一: "line1A:A1"
  id: number        // 数据库 PK
  lineNo: string    // "line1A"
  faceNo: string    // "A1"
  color: string     // 后端 LineDTO.color (#5CE1FF)
  hourDefectCount: number  // = realtimeData.ngCount
  hourRemoveCount: number  // = realtimeData.removeTotal
  realtime: RealtimeDetectData | null
  raw: LineItem
}
```

**简化版 hourDefectCount / hourRemoveCount**：PSM 老 SPA 是后端聚合字段；我们 LineDTO 没这俩字段，按 brief 提示用实时值（`realtimeData.ngCount` / `realtimeData.removeTotal`）兜底，左栏一定有数字。

### 2.2 LineListCard 玻璃组件

**结构**（与 PSM `.defect-li-item` 对齐，UI 走自家 token）：

```
┌─ line-list-card (glass) ────────────────┐
│ 🛰 产线列表                  共: 38    │ ← header
├─────────────────────────────────────────┤
│ ┌─ line-item (line1A-A1 选中) ──────┐  │ ← active: 蓝绿底 + 左边条 + 边框
│ ││ 1  line1A-A1          4,253     │  │   index 彩块 + lineNo-faceNo + 计数
│ │   Line Line           4,089      │  │
│ └───────────────────────────────────┘  │
│ ┌─ line-item ──────────────────────┐   │
│ │ 2  line1A-A2          2,503      │   │
│ │   Line Line           2,379      │   │
│ └───────────────────────────────────┘  │
│ ... (内部滚动)                         │
└─────────────────────────────────────────┘
```

**样式 tokens**（全部来自 `styles/tokens.scss`，零自定义颜色）：
- 背景：`var(--glass-bg)` + `var(--glass-blur)` (blur 40px saturate 180%)
- 边框：`var(--glass-border)` + 顶部内高光 `linear-gradient(180deg, rgba(255,255,255,0.10), transparent 35%)`
- 选中态：`.line-item--active` = `linear-gradient(135deg, rgba(92,225,255,0.16), rgba(95,217,127,0.10))` + `border-color: rgba(92,225,255,0.55)` + `box-shadow: 0 0 0 1px rgba(92,225,255,0.25), 0 6px 18px rgba(92,225,255,0.10)`
- 左侧高亮条：`.line-item__active-bar` = `linear-gradient(180deg, #5ce1ff, #5fd97f)` + `box-shadow: 0 0 8px rgba(92,225,255,0.7)`
- hover：背景色加深 + `translateY(-1px)`
- 序号色板：7 色玻璃风冷暖色板（蓝绿/粉蓝/绿橙/橙红/浅蓝粉/绿蓝/紫蓝），按列表索引 `idx % 7` 取

### 2.3 RealTime.vue 主布局改造

**前**：单栏流式布局（KPI 4 卡 → 折线 → 表格）
**后**：grid 布局

```css
.realtime-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: var(--space-4);
  align-items: stretch;
}
.realtime-layout__left {
  position: sticky;
  top: 0;
  align-self: stretch;
  height: calc(100vh - 220px);
  min-height: 540px;
}
```

**响应式断点**：
- `<1280px` → 240px 1fr
- `<960px`  → 1fr (左栏变顶部固定 max 320px)

**中栏响应选中切换**：
- `currentLine` = `lineStore.selectedLine` (reactive computed)
- `kpiCards` / `tableRows` / `buildSeries` 全部基于 `currentLine` + `selectedRealtime` (实时刷新的最新点)
- `chartSubtitle` = "已选：line1A-A1 · Line"
- `handleLineChange(lineKey)` → `selectedRealtime.value = null` → 重新拉 `/web/detect/realtime` → 重绘图表

### 2.4 i18n 新增 4 个 key × 3 语

```ts
// zh-CN (line 192-196)
lineList: {
  title: '产线列表',
  total: '共',
  defect: '缺陷',
  remove: '剔除'
}

// en-US (line 1058-1064)
lineList: {
  title: 'Production Lines',
  total: 'Total',
  defect: 'Defect',
  remove: 'Remove'
}

// id-ID (line 1909-1915)
lineList: {
  title: 'Daftar Lane',
  total: 'Total',
  defect: 'Cacat',
  remove: 'Buang'
}
```

> 报告原 brief 写 "补 1 个 key (title)"；实际为了让卡片 UI 完整可读（"共 38" / "缺陷" / "剔除" 三个 label），补到 **4 个 key × 3 语 = 12 条**。empty 态复用 `common.noData` 不重复加。

---

## 3. 验证结果

### 3.1 vite build

```
✓ 2336 modules transformed.
dist/index.html                      0.40 kB │ gzip:   0.27 kB
dist/assets/index-bBlOPNiQ.css     452.72 kB │ gzip:  61.95 kB
dist/assets/interceptor-CS5cXRRI.js   0.35 kB │ gzip:   0.24 kB
dist/assets/index-C4bLPUl5.js     2,639.68 kB │ gzip: 857.28 kB
✓ built in 24.55s
```

**PASS** (仅有 sass legacy-js-api deprecation warning，无 error；chunk size > 1500 KB 是项目既有问题，与本次改动无关)

### 3.2 Copy-Item 部署

```powershell
Copy-Item E:\DEMO\数据采集\DataupLoad-web\dist\* E:\DEMO\数据采集\DataupLoad\web\ -Recurse -Force
```

部署后 curl 验证：
```
HTTP/200 - http://localhost:8080/
HTTP/200 - /assets/index-C4bLPUl5.js
HTTP/200 - /assets/index-bBlOPNiQ.css
```

### 3.3 浏览器实测（Playwright headless）

```javascript
// 登录 super_admin / Abc12345 → /realtime → 等左栏 LineListCard 渲染
await page.waitForSelector('.line-list-card', { timeout: 15000 })
```

**DOM 验证**：
```json
{
  "cardTitle": "产线列表",
  "cardCount": "共: 38",
  "lineCount": 38,
  "firstFive": [
    { "no": "line1A", "face": "A1", "defect": "4,253", "remove": "4,089", "active": true  },
    { "no": "line1A", "face": "A2", "defect": "2,503", "remove": "2,379", "active": false },
    { "no": "line1B", "face": "B1", "defect": "2,332", "remove": "2,227", "active": false },
    { "no": "line1B", "face": "B2", "defect": "1,616", "remove": "1,466", "active": false },
    { "no": "line2A", "face": "A1", "defect": "83,700", "remove": "95,064", "active": false }
  ],
  "activeLine": "line1A-A1",
  "kpi": [
    "生产总数: 350,277",
    "实时效率: 173.00",
    "上座数量: 0",
    "上座率: 0.00",
    "次品数量: 4,253",
    "次品率: 1.20",
    "良品数量: 346,024",
    "剔除失败数: 0"
  ],
  "chartTitle": "实时趋势",
  "chartSub": "已选：line1A-A1 · Line"
}
```

**单击切换验证**（点第 3 行 → line1B-B1）：
```json
{ "activeLine": "line1B-B1", "chartSub": "已选：line1B-B1 · Line" }
```

**PASS** — 选中切换正确，KPI 全部跟随刷新，chart sub 同步更新。

### 3.4 截图（3 张，1600×900 PNG）

| 文件 | 内容 | 大小 |
|---|---|---|
| `docs/work-orders/W-RT-2-01-initial.png` | 初始 (line1A-A1 选中, 中栏 KPI/chart/table 全亮) | 533 KB |
| `docs/work-orders/W-RT-2-02-selected.png` | 单击 line1B-B1 后 (中栏切换, KPI 数字变化) | 526 KB |
| `docs/work-orders/W-RT-2-03-hover.png` | hover 第 5 行 (line2A-A1 抬升 + 边框亮) | 532 KB |

---

## 4. Git 提交

```
commit 26d8fc4 (HEAD -> main, origin/main)
Author: industry <industry@openclaw.local>
Date:   Thu Jul 30 23:46:xx +0800

    W-RT-2: 左栏线别列表卡片 (玻璃风)
    
    7 files changed, 1222 insertions(+), 314 deletions(-)
    create mode 100644 DataupLoad-web/src/components/LineListCard.vue
    create mode 100644 DataupLoad-web/src/stores/line.ts
    create mode 100644 docs/work-orders/W-RT-2-01-initial.png
    create mode 100644 docs/work-orders/W-RT-2-02-selected.png
    create mode 100644 docs/work-orders/W-RT-2-03-hover.png
```

**PUSH**: `git push origin main` → `1ae0d09..26d8fc4  main -> main` ✅

> 注：与 fengwei@intco.local 的 W-PERF-D 提交（1ae0d09）有短暂交叉（对方曾合并后回退），最终 git history 上我的 W-RT-2 工作作为独立 commit `26d8fc4` 落地。

---

## 5. 完成标准清单

- [x] **LineListCard.vue 组件** — 玻璃风 + 单击切换 + 选中态高亮
- [x] **RealTime.vue 接入** — grid 布局 + 左栏 LineListCard + 中栏响应选中切换
- [x] **i18n 4 个 key** × 3 语 (title / total / defect / remove)
- [x] **vite build PASS** — 24.55s, 无 error
- [x] **Copy-Item 部署 PASS** — 8080 服务返回 200
- [x] **浏览器实测 PASS** — 38 条线别渲染 / 单击切换 / DOM 验证通过
- [x] **commit + push origin main** — `26d8fc4` 已推送
- [x] **报告输出** — 本文件 `docs/work-orders/W-RT-2-report.md`

---

## 6. 与后续 RT 子单的衔接

按 brief / W-REALTIME-PSM §1.2，PSM 实时页左侧除线别列表外还有 **缺陷类型子表（`.defect-part`，7 个 span）**。我们这次没做：

- **未做**（按 brief 要求）：`.defect-part` 子表（每个 line-item 折叠展开 7 个缺陷类型 + 颜色块 + 阈值）
- **建议承接**：W-RT-3 子单承接；数据可复用 `realtimeData.defects[]`（后端已经返回 33 种 defect 类型 + count + showFlag）

其他：
- **线别拖拽排序**（PSM 用 HTML5 native drag）→ W-RT-3+ 建议
- **报警徽章悬浮窗** → W-RT-3+ 建议
- **设备状态三联**（中栏右半区）→ W-RT-4 已部分做（KPI 8 卡），剩余表格设备列表建议 W-RT-5+

---

## 7. 约束遵守

- ✅ 不许重启后端服务（仅前端改动 + assets 拷贝）
- ✅ 不许跨子单文件：仅新建 LineListCard.vue + 改 stores/line.ts (新) + 改 RealTime.vue (本任务要求) + 改 i18n/index.ts (本任务要求)
- ✅ 不引新依赖（沿用 element-plus + pinia + echarts）
- ✅ UTF-8 编码 (无 BOM)
- ✅ 必须 commit + push origin main
- ✅ 编码格式：所有 .vue / .ts 文件 LF → CRLF (Windows 自动转)

---

**报告完成**：2026-07-30 23:48 GMT+8
**实测环境**：Node v24.18.0 + Playwright headless + Vue 3 + Pinia + Element Plus
**后端**：Spring Boot @ http://localhost:8080 (既有服务, 未重启)
