# W-FRONT-02-E8 brief — 大屏模式

- **任务**: 实现 `/screen` 业务页：全屏大屏 + 多图表 + WS 实时数据
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/Screen.vue`（替换 stub）

页面结构（**全屏沉浸式，无侧边栏/顶栏**）：
- **顶部状态条**（半透明玻璃横条）：
  - 公司 logo + 平台名 + 当前时间（每秒刷新）
  - 在线线别数 / 今日总产量 / 今日缺陷 / 今日报警
  - 全屏切换按钮（FS API）
- **中间主图表区**（Grid 布局 4 个 echarts）：
  - 左上：实时折线（产量 + 缺陷叠加）
  - 右上：缺陷类型饼图
  - 左下：报警事件流（虚拟滚动列表）
  - 右下：线别状态矩阵（Grid 状态卡片）
- **底部滚动条**（跑马灯）：最新报警 / 缺陷事件
- **WS 数据连接**：用 `/ws?uid=screen&type=screen` 订阅大屏专用数据流

### 2. `DataupLoad-web/src/api/screen.ts`

```ts
import http from './http'

// 大屏聚合数据
export const getScreenData = () => http.get('/web/screen/data')

// 实时数据流（同 E1）
export const getRealtimeDetect = (lineId: number) => http.get('/web/detectData/realtime', { params: { lineId } })
```

### 3. `DataupLoad-web/src/utils/screenWs.ts`

```ts
export function connectScreenWs(uid: string, onMsg: (data: any) => void): WebSocket {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const ws = new WebSocket(`${proto}://${location.host}/ws?uid=${uid}&type=screen`)
  ws.onmessage = (ev) => {
    try { onMsg(JSON.parse(ev.data)) } catch { /* ignore */ }
  }
  return ws
}
```

### 4. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `screen.title` | 大屏模式 | Big Screen Mode | Mode Layar Besar |
| `screen.kpi.onlineLines` | 在线线别 | Online Lines | Lanes Aktif |
| `screen.kpi.todayOutput` | 今日产量 | Today's Output | Output Hari Ini |
| `screen.kpi.todayDefect` | 今日缺陷 | Today's Defect | Cacat Hari Ini |
| `screen.kpi.todayAlarm` | 今日报警 | Today's Alarms | Alarm Hari Ini |
| `screen.chart.trend` | 实时趋势 | Realtime Trend | Tren Realtime |
| `screen.chart.defectType` | 缺陷类型分布 | Defect Distribution | Distribusi Cacat |
| `screen.chart.alarmStream` | 实时报警流 | Alarm Stream | Stream Alarm |
| `screen.chart.lineMatrix` | 线别状态 | Line Matrix | Matriks Lanes |
| `screen.action.fullscreen` | 全屏 | Fullscreen | Layar Penuh |
| `screen.action.exitFullscreen` | 退出全屏 | Exit Fullscreen | Keluar |
| `screen.ticker.title` | 事件滚动 | Event Ticker | Ticker Peristiwa |

### 5. `docs/work-orders/W-FRONT-02-E8-report.md`

- 截图（全屏状态 + 4 图表 + 跑马灯）
- WS 推送实测（curl 触发后看图表更新）
- 三语截图
- 全屏切换实测

## done criteria

- [ ] 顶部状态条 + 时间实时刷新
- [ ] 4 个 echarts 图表正常渲染
- [ ] WS 连接成功（screen 类型）
- [ ] 跑马灯滚动（CSS animation）
- [ ] 全屏 API 切换（Fullscreen / Exit）
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E8-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/screen/data
# 若 404 → 降级为组合调用：
#   /web/line/list + /web/stateStatistic/day + /web/alarm/list + /web/defectDayRecord/list
```

## 特殊注意

- 大屏页面 **不需要侧边栏/顶栏**（hideInMenu=true 已配），但 MainLayout 仍会包一层 → 调整 MainLayout 让 screen 子路由隐藏 chrome
- echarts 大屏模式下要 `setOption` 而不是 `setOption(option, true)`，避免全量重绘
- WS 重连：心跳 30s 断线自动重连（用 setInterval）

## 禁止

- 不许引入新图表库
- 不许碰 MainLayout（用 router meta 控制）
- 不许改 vite.config.ts

