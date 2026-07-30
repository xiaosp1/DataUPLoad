# W-FRONT-02-E6 brief — 操作日志

- **任务**: 实现 `/log` 业务页：操作日志列表 + 筛选 + 详情
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1h（比标准 1.5h 简单）
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/Log.vue`（替换 stub）

页面结构：
- **筛选栏**（GlassCard）：
  - 时间范围（近 24 小时 / 7 天 / 30 天 / 自定义）
  - 用户名（输入框）
  - 操作类型（登录 / 登出 / 新增 / 编辑 / 删除 / 报警处理）
  - 模块（account / alarm / defect / line / config / system）
  - 重置 + 查询
- **日志表格**（GlassTable）：
  - 列：时间 / 用户名 / 模块 / 操作 / 对象 / IP / 结果（成功/失败） / 操作（详情）
  - 分页
  - 成功 = 绿 tag / 失败 = 红 tag
- **详情抽屉**（el-drawer 右侧弹出）：
  - 完整字段：时间 / 用户 / 模块 / 操作 / 请求方法 / 请求路径 / 请求参数 / 响应结果 / 耗时

### 2. `DataupLoad-web/src/api/log.ts`

```ts
import http from './http'

export const listLog = (params: {
  pageNum: number
  pageSize: number
  username?: string
  module?: string
  operation?: string
  result?: string
  from?: string
  to?: string
}) => http.get('/web/log/list', { params })

export const getLogDetail = (id: number) => http.get(`/web/log/get/${id}`)
```

### 3. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `log.title` | 操作日志 | Operation Log | Log Operasi |
| `log.filter.timeRange` | 时间范围 | Time Range | Rentang Waktu |
| `log.filter.username` | 用户名 | Username | Nama Pengguna |
| `log.filter.module` | 模块 | Module | Modul |
| `log.filter.operation` | 操作 | Operation | Operasi |
| `log.filter.result` | 结果 | Result | Hasil |
| `log.table.time` | 时间 | Time | Waktu |
| `log.table.username` | 用户名 | Username | Nama Pengguna |
| `log.table.module` | 模块 | Module | Modul |
| `log.table.operation` | 操作 | Operation | Operasi |
| `log.table.target` | 对象 | Target | Target |
| `log.table.ip` | IP 地址 | IP Address | Alamat IP |
| `log.table.result` | 结果 | Result | Hasil |
| `log.detail.title` | 日志详情 | Log Detail | Detail Log |
| `log.detail.method` | 请求方法 | Method | Metode |
| `log.detail.path` | 请求路径 | Path | Path |
| `log.detail.params` | 请求参数 | Params | Params |
| `log.detail.response` | 响应结果 | Response | Respons |
| `log.detail.cost` | 耗时 | Cost | Durasi |
| `log.result.success` | 成功 | Success | Berhasil |
| `log.result.failed` | 失败 | Failed | Gagal |

### 4. `docs/work-orders/W-FRONT-02-E6-report.md`

- 截图 + 详情抽屉截图
- 三语截图

## done criteria

- [ ] 筛选联动查询
- [ ] 表格分页 + tag 颜色
- [ ] 详情抽屉显示完整字段
- [ ] JSON 格式化（请求参数 / 响应结果）
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E6-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/log/list?pageNum=1&pageSize=10
curl http://localhost:80/web/log/get/1
```

## 禁止

- 不许引入代码高亮库（JSON 格式化用 el-input type=textarea + 简单缩进即可）
- 不许碰路由
- 不许改后端

