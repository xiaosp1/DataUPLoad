# W-FRONT-02-E1 brief — 实时数据看板

- **任务**: 实现 `/realtime` 业务页：多线别 KPI + 折线图（计划/实际/缺陷数）
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/RealTime.vue`（替换 stub）

页面结构（自上而下）：
- **顶部 KPI 行**（4 个 GlassCard 并排）：
  - 在线线别数（line 在线）
  - 当日总产量
  - 当日总缺陷数
  - 当日总报警数
- **中间折线图**（GlassCard + echarts）：
  - X 轴：时间（近 2 小时，每 5 分钟一个点）
  - 3 条线：plan（计划）/ actual（实际）/ defect（缺陷）
  - 线别选择器（顶部下拉，多选）
- **底部线别状态表**（GlassTable）：
  - 列：线别名 / 状态（运行/停机/故障） / 当前产量 / 今日缺陷 / 当前计划进度

### 2. `DataupLoad-web/src/api/realtime.ts`

```ts
import http from './http'

// 线别状态
export const listLine = () => http.get('/web/line/list')

// 实时数据（detectData）
export const getRealtimeDetect = (params: { lineId: number; from: string; to: string }) =>
  http.get('/web/detectData/realtime', { params })

// 当日统计
export const getDayStatistic = (date: string) =>
  http.get('/web/stateStatistic/day', { params: { date } })

// 当日计划进度
export const getDayPlan = (date: string) =>
  http.get('/web/plan/day', { params: { date } })

// 当前报警
export const getCurrentAlarm = () => http.get('/web/alarm/list', { params: { pageNum: 1, pageSize: 100 } })
```

### 3. `DataupLoad-web/src/i18n/locales/zh-CN.ts` 等三语（如需新增 key）

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `realtime.kpi.onlineLines` | 在线线别 | Online Lines | Lanes Aktif |
| `realtime.kpi.todayOutput` | 今日产量 | Today's Output | Output Hari Ini |
| `realtime.kpi.todayDefect` | 今日缺陷 | Today's Defect | Cacat Hari Ini |
| `realtime.kpi.todayAlarm` | 今日报警 | Today's Alarms | Alarm Hari Ini |
| `realtime.chart.title` | 实时趋势 | Realtime Trend | Tren Realtime |
| `realtime.chart.plan` | 计划 | Plan | Rencana |
| `realtime.chart.actual` | 实际 | Actual | Aktual |
| `realtime.chart.defect` | 缺陷 | Defect | Cacat |
| `realtime.table.line` | 线别 | Line | Lanes |
| `realtime.table.status` | 状态 | Status | Status |
| `realtime.table.output` | 产量 | Output | Output |
| `realtime.table.defect` | 缺陷 | Defect | Cacat |
| `realtime.table.progress` | 进度 | Progress | Progress |

### 4. `docs/work-orders/W-FRONT-02-E1-report.md`

- 截图 `W-FRONT-02-E1-sample.png`（新前端）
- 对比图 `W-FRONT-02-E1-psm.png`（PSM 老 SPA 同页）
- diff 结论（差异百分比）
- i18n 三语切换实测

## done criteria

- [ ] KPI 4 卡渲染数字 + 玻璃风
- [ ] 折线图正常显示（plan/actual/defect 三条）
- [ ] 线别下拉切换 → 折线图刷新
- [ ] 底部表格渲染线别列表
- [ ] 三语切换正常（中文/英文/印尼语）
- [ ] 数据为空 → "暂无数据"占位，不白屏
- [ ] 网络错 → el-message 错误提示
- [ ] 401 → 跳 /login（依赖 interceptor）
- [ ] 截图保存到 docs/work-orders/
- [ ] W-FRONT-02-E1-report.md 含 diff 结论

## 后端 API（worker 自测前先 curl 确认）

```powershell
# 已验证 OK（PSM 路径）
curl http://localhost:80/web/line/list
curl http://localhost:80/web/stateStatistic/day?date=2026-07-30
curl http://localhost:80/web/plan/day?date=2026-07-30
curl http://localhost:80/web/alarm/list?pageNum=1&pageSize=10

# 可能不存在（worker 自测时若 404 → 用 /web/detectData/realtime 或 /web/line/dayRecord 替代）
curl http://localhost:80/web/detectData/realtime?lineId=1
```

## 禁止

- 不许改 vite.config.ts / main.ts
- 不许改路由（meta 已配）
- 不许引入 echarts 之外的图表库
- 不许写 mock 数据（直接调真实 API）

