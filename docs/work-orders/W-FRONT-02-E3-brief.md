# W-FRONT-02-E3 brief — 缺陷处理

- **任务**: 实现 `/defect` 业务页：缺陷日记录 + 类型筛选 + 详情
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/Defect.vue`（替换 stub）

页面结构：
- **顶部统计卡**（4 个 GlassCard）：
  - 当日总缺陷数
  - 严重缺陷数
  - 已处理缺陷数
  - 漏检率（%）
- **筛选栏**（GlassCard）：
  - 日期范围 / 线别 / 缺陷类型 / 严重程度
  - 重置 + 查询
- **缺陷明细表格**（GlassTable）：
  - 列：记录 ID / 时间 / 线别 / 缺陷类型 / 严重度 / 图片缩略图 / 处理状态 / 处理人 / 操作（详情 / 处理）
  - 分页
- **详情弹窗**（el-dialog）：
  - 大图（缺陷样本图）
  - 元数据：检测时间 / 处理时间 / 检测员 / 处理人
  - 处理备注 textarea + 保存按钮
- **趋势图**（小 echarts）：
  - 近 7 天每日缺陷数柱状图

### 2. `DataupLoad-web/src/api/defect.ts`

```ts
import http from './http'

// 缺陷日记录
export const listDefectDay = (params: { pageNum: number; pageSize: number; date?: string; lineId?: number; type?: string; level?: string }) =>
  http.get('/web/defectDayRecord/list', { params })

// 缺陷详情
export const getDefectDetail = (id: number) => http.get(`/web/defectDayRecord/get/${id}`)

// 标记处理
export const handleDefect = (id: number, remark: string) =>
  http.post(`/web/defectDayRecord/handle/${id}`, { remark })

// 线别缺陷类型
export const listLineDefectType = () => http.get('/web/lineDefectType/list')

// 7 日趋势
export const getDefectTrend = (from: string, to: string) =>
  http.get('/web/defectDayRecord/trend', { params: { from, to } })
```

### 3. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `defect.title` | 缺陷处理 | Defect Handling | Penanganan Cacat |
| `defect.kpi.total` | 当日总缺陷 | Today's Total Defects | Total Cacat Hari Ini |
| `defect.kpi.severe` | 严重缺陷 | Severe Defects | Cacat Serius |
| `defect.kpi.handled` | 已处理 | Handled | Ditangani |
| `defect.kpi.missRate` | 漏检率 | Miss Rate | Tingkat Miss |
| `defect.filter.date` | 日期 | Date | Tanggal |
| `defect.filter.line` | 线别 | Line | Lanes |
| `defect.filter.type` | 缺陷类型 | Defect Type | Tipe Cacat |
| `defect.filter.level` | 严重度 | Severity | Tingkat |
| `defect.table.id` | 记录 ID | Record ID | ID Catatan |
| `defect.table.time` | 时间 | Time | Waktu |
| `defect.table.line` | 线别 | Line | Lanes |
| `defect.table.type` | 缺陷类型 | Defect Type | Tipe Cacat |
| `defect.table.level` | 严重度 | Severity | Tingkat |
| `defect.table.image` | 样本图 | Sample | Contoh |
| `defect.table.status` | 处理状态 | Status | Status |
| `defect.table.handler` | 处理人 | Handler | Penangan |
| `defect.detail.title` | 缺陷详情 | Defect Detail | Detail Cacat |
| `defect.detail.detectTime` | 检测时间 | Detect Time | Waktu Deteksi |
| `defect.detail.handleTime` | 处理时间 | Handle Time | Waktu Tangani |
| `defect.detail.remark` | 处理备注 | Remark | Catatan |
| `defect.detail.save` | 保存 | Save | Simpan |
| `defect.trend.title` | 近 7 日趋势 | 7-Day Trend | Tren 7 Hari |

### 4. `docs/work-orders/W-FRONT-02-E3-report.md`

- 截图 + 详情弹窗截图
- 7 日趋势图截图
- i18n 三语截图

## done criteria

- [ ] KPI 4 卡渲染
- [ ] 筛选联动查询
- [ ] 表格分页正常
- [ ] 严重度行有颜色标记（severe=红 / normal=黄）
- [ ] 详情弹窗显示大图 + 备注编辑
- [ ] 保存备注后状态更新
- [ ] 7 日趋势图渲染
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E3-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/defectDayRecord/list?pageNum=1&pageSize=10
curl http://localhost:80/web/lineDefectType/list
curl http://localhost:80/web/defectDayRecord/get/1
curl http://localhost:80/web/defectDayRecord/trend?from=2026-07-23&to=2026-07-30
```

## 禁止

- 不许引入新的图表库
- 不许改 vite.config.ts
- 不许跨子单（不许碰 alarm.ts）

