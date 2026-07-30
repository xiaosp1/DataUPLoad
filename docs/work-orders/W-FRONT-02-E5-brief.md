# W-FRONT-02-E5 brief — 系统配置

- **任务**: 实现 `/systemConfig` 业务页：系统参数 + 线别配置 + 缺陷类型映射
- **依赖**: W-FRONT-02-D（已完成，stub 在位）
- **耗时上限**: 1.5h
- **通用规则**: `docs/work-orders/W-FRONT-02-E-index.md`

## 关键产出

### 1. `DataupLoad-web/src/views/SystemConfig.vue`（替换 stub）

页面结构（Tabs 三页签）：
- **Tab 1: 系统参数**
  - GlassCard + el-form
  - 字段：报警声音开关 / 报警保留天数 / 数据同步间隔（秒）/ 大屏刷新间隔（秒）/ 默认语言 / 其他 key-value
  - 保存按钮 → 调 updateBatch
- **Tab 2: 线别配置**
  - GlassTable：线别名 / 编码 / 状态 / 描述 / 操作（编辑 / 启用禁用）
  - 新增 / 编辑弹窗
- **Tab 3: 缺陷类型映射**
  - GlassTable：缺陷类型名 / 等级 / 是否启用 / 操作
  - 新增 / 编辑 / 删除
  - 与线别的映射关系（多对多）

### 2. `DataupLoad-web/src/api/systemConfig.ts`

```ts
import http from './http'

// 系统配置（批量）
export const listConfig = () => http.get('/web/systemConfig/list')
export const getConfig = (key: string) => http.get(`/web/systemConfig/get/${key}`)
export const updateConfig = (key: string, value: string) =>
  http.post('/web/systemConfig/update', { key, value })
export const updateBatch = (data: Record<string, string>) =>
  http.post('/web/systemConfig/updateBatch', data)

// 线别
export const listLine = () => http.get('/web/line/list')
export const addLine = (data: { name: string; code: string; desc: string }) =>
  http.post('/web/line/add', data)
export const editLine = (data: { id: number; name: string; code: string; desc: string; status: number }) =>
  http.post('/web/line/edit', data)
export const toggleLine = (id: number, status: number) =>
  http.post(`/web/line/status/${id}`, { status })

// 线别缺陷类型
export const listLineDefectType = () => http.get('/web/lineDefectType/list')
export const addLineDefectType = (data: { lineId: number; typeId: number; level: number }) =>
  http.post('/web/lineDefectType/add', data)
export const deleteLineDefectType = (id: number) =>
  http.post(`/web/lineDefectType/delete/${id}`)
```

### 3. i18n 新增 key

| key | zh-CN | en-US | id-ID |
|-----|-------|-------|-------|
| `config.title` | 系统配置 | System Config | Konfigurasi Sistem |
| `config.tab.system` | 系统参数 | System Params | Parameter Sistem |
| `config.tab.line` | 线别配置 | Line Config | Konfigurasi Lanes |
| `config.tab.defectType` | 缺陷类型 | Defect Types | Tipe Cacat |
| `config.form.alarmSound` | 报警声音 | Alarm Sound | Suara Alarm |
| `config.form.alarmRetainDays` | 报警保留天数 | Alarm Retain Days | Hari Retensi Alarm |
| `config.form.syncInterval` | 同步间隔（秒） | Sync Interval (s) | Interval Sync (s) |
| `config.form.screenRefresh` | 大屏刷新（秒） | Screen Refresh (s) | Refresh Layar (s) |
| `config.form.defaultLang` | 默认语言 | Default Lang | Bahasa Default |
| `config.form.save` | 保存 | Save | Simpan |
| `config.line.name` | 线别名 | Line Name | Nama Lanes |
| `config.line.code` | 编码 | Code | Kode |
| `config.line.desc` | 描述 | Description | Deskripsi |
| `config.defectType.name` | 类型名 | Type Name | Nama Tipe |
| `config.defectType.level` | 等级 | Level | Level |
| `config.defectType.enabled` | 启用 | Enabled | Diaktifkan |
| `config.action.add` | 新增 | Add | Tambah |
| `config.action.edit` | 编辑 | Edit | Edit |
| `config.action.delete` | 删除 | Delete | Hapus |

### 4. `docs/work-orders/W-FRONT-02-E5-report.md`

- 截图三 tab
- 保存系统参数后 → 刷新页面看持久化
- 三语截图

## done criteria

- [ ] Tabs 三页签切换正常
- [ ] 系统参数表单可编辑 + 批量保存
- [ ] 保存后刷新页面持久化
- [ ] 线别表格 + 新增 / 编辑 / 启用禁用
- [ ] 缺陷类型映射可增删
- [ ] 表单校验（必填、长度）
- [ ] 三语切换正常
- [ ] 截图保存
- [ ] W-FRONT-02-E5-report.md

## 后端 API 自测

```powershell
curl http://localhost:80/web/systemConfig/list
curl http://localhost:80/web/line/list
curl http://localhost:80/web/lineDefectType/list
```

## 禁止

- 不许引入新依赖
- 不许碰路由
- 不许改后端

