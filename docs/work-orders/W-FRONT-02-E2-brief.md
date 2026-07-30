# W-FRONT-02-E2 brief — 报警管理

- **任务**: 实现 `/alarm` 业务页：报警列表 + WebSocket 实时推送 + 详情弹窗
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/Alarm.vue`（替换 stub）

页面结构：
- **顶部筛选栏**（GlassCard）：
  - 时间范围（近 1 小时 / 24 小时 / 7 天 / 自定义）
  - 线别（多选下拉）
  - 报警类型（多选下拉）
  - 状态（未处理 / 已处理 / 已忽略）
  - 重置 + 查询按钮
- **报警表格**（GlassTable）：
  - 列：触发时间 / 线别 / 类型 / 等级 / 描述 / 状态 / 操作（详情/忽略）
  - 分页（pageNum/pageSize）
  - 行高亮：未处理 = 红色玻璃态
- **WS 实时连接指示器**：右上角小圆点（绿 = 已连接 / 红 = 断开）
- **详情弹窗**（el-dialog）：
  - 报警 ID / 触发时间 / 持续时长 / 图片（如有）/ 完整描述
  - 忽略 / 处理按钮

### 2. `DataupLoad-web/src/api/alarm.ts`

```ts
import http from './http'

// 报警列表（分页）
export const listAlarm = (params: { pageNum: number; pageSize: number; lineId?: number; type?: string; status?: string; from?: string; to?: string }) =>
  http.get('/web/alarm/list', { params })

// 报警详情
export const getAlarmDetail = (id: number) => http.get(`/web/alarm/get/${id}`)

// 忽略报警
export const ignoreAlarm = (id: number) => http.post(`/web/alarm/ignore/${id}`)

// 报警类型下拉
export const listAlarmType = () => http.get('/web/alarm/type/list')
```

### 3. `DataupLoad-web/src/utils/ws.ts`（共享 WS 工具，可被 E2/E8 共用）

```ts
export interface WsMessage {
  type: 'alarm' | 'sound' | 'data'
  payload: any
}

export function connectAlarmWs(uid: string, onMsg: (m: WsMessage) => void): WebSocket {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const ws = new WebSocket(`${proto}://${location.host}/ws?uid=${uid}&type=alarm`)
  ws.onmessage = (ev) => {
    try { onMsg(JSON.parse(ev.data)) } catch { /* ignore */ }
  }
  return ws
}
```

### 4. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `alarm.title` | 报警管理 | Alarm Management | Manajemen Alarm |
| `alarm.filter.timeRange` | 时间范围 | Time Range | Rentang Waktu |
| `alarm.filter.line` | 线别 | Line | Lanes |
| `alarm.filter.type` | 类型 | Type | Tipe |
| `alarm.filter.status` | 状态 | Status | Status |
| `alarm.filter.reset` | 重置 | Reset | Reset |
| `alarm.filter.query` | 查询 | Query | Kueri |
| `alarm.table.triggerTime` | 触发时间 | Trigger Time | Waktu Trigger |
| `alarm.table.line` | 线别 | Line | Lanes |
| `alarm.table.type` | 类型 | Type | Tipe |
| `alarm.table.level` | 等级 | Level | Level |
| `alarm.table.desc` | 描述 | Description | Deskripsi |
| `alarm.table.status` | 状态 | Status | Status |
| `alarm.table.action` | 操作 | Action | Aksi |
| `alarm.status.pending` | 未处理 | Pending | Tertunda |
| `alarm.status.handled` | 已处理 | Handled | Ditangani |
| `alarm.status.ignored` | 已忽略 | Ignored | Diabaikan |
| `alarm.detail.title` | 报警详情 | Alarm Detail | Detail Alarm |
| `alarm.detail.duration` | 持续时长 | Duration | Durasi |
| `alarm.detail.ignore` | 忽略 | Ignore | Abaikan |
| `alarm.detail.handle` | 处理 | Handle | Tangani |
| `alarm.ws.connected` | 实时连接已建立 | Realtime Connected | Realtime Terhubung |
| `alarm.ws.disconnected` | 实时连接已断开 | Realtime Disconnected | Realtime Terputus |

### 5. `docs/work-orders/W-FRONT-02-E2-report.md`

- 截图 + WS 指示器状态
- WS 推送实测（curl 触发 WS 推送后看新行是否插入）
- i18n 三语截图

## done criteria

- [ ] 顶部筛选可联动查询
- [ ] 报警表格分页正常
- [ ] 未处理行红色玻璃态
- [ ] WS 连接成功 + 指示器绿点
- [ ] WS 收到新报警 → 表格自动插入新行 + 声音（PSM 设计）
- [ ] 详情弹窗显示完整字段
- [ ] 忽略按钮可触发 + 列表状态更新
- [ ] 三语切换正常
- [ ] 401 → 跳 /login
- [ ] 截图保存
- [ ] W-FRONT-02-E2-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/alarm/list?pageNum=1&pageSize=10
curl http://localhost:80/web/alarm/type/list
curl http://localhost:80/web/alarm/get/1

# WS 推送测试（可选）
curl http://localhost:80/web/alarm/ignore/1 -X POST
```

## WS 测试方式

让 worker 在 Vite dev 自测时：
1. 浏览器开 2 个 tab：A = 新前端 /realtime，B = 老 SPA /alarm
2. 在 B 触发一个忽略动作，看 A 是否收到 alarm WS 推送
3. 或者用 PM 验证脚本 curl POST 触发后端事件

## 禁止

- 不许在前端节流报警（PSM 设计，服务端零节流）
- 不许写 mock 数据
- 不许引入 Socket.IO（用原生 WebSocket）
- 不许碰 /ws 路径（PSM 约定 `?uid=&type=alarm`）

