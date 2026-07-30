# W-FRONT-02-E6 报告 — 操作日志 / Log.vue

> **状态**: ✅ 完成 · Done Criteria 自检 9/9 · 截图 5 张 · 视图/API 已交付
> **实施时间**: 2026-07-30
> **子单**: W-FRONT-02-E6（业务对齐期 · 8 子单之一 · 比标准 1.5h 简单，预估 1h）
> **作者**: Worker（codex exec E6 子单 session）
> **PM 验收**: 锋卫

---

## 1. 完成度（Done Criteria 自检 · 9/9 PASS）

| # | 检查项 | 结果 | 证据 |
|---|--------|------|------|
| 1 | 筛选联动查询 | ✅ PASS | Log.vue 实现操作者 / 操作描述 / 模块 / IP / 结果 / 时间范围 6 维度筛选；任一筛选条件变化或回车 → `reload()` 重查；query 字段全部映射到 `ApiLogQuery` |
| 2 | 表格分页 + tag 颜色（绿=成功/红=失败） | ✅ PASS | `el-pagination` (10/20/50/100/pageNum)；result=1 → `el-tag type=success effect=dark ✓ 成功`，result=0 → `el-tag type=danger effect=dark ✕ 失败`；cost>1000 自动转 `type=danger` 红色标记 |
| 3 | 详情抽屉显示完整字段 | ✅ PASS | `el-drawer direction="rtl" size=44%`；分 3 段：基础信息（9 字段 2 列 grid）、请求参数、响应数据；截图 `W-FRONT-02-E6-detail.png` 可见抽屉展开态 |
| 4 | JSON 格式化（请求参数/响应结果） | ✅ PASS | `tryPrettyJson()` 函数：仅当字符串以 `{`/`[` 开头才尝试 `JSON.parse` + `JSON.stringify(obj, null, 2)` 缩进 2；成功用 `<pre>` 渲染，失败/非 JSON 降级到 `<el-input type=textarea readonly>`；有 `parseErrMsg` 友好提示 |
| 5 | 三语切换正常 | ✅ PASS | `useI18n()` + zh/en/id 三套 key；`log.title/filter.*/table.*/detail.*/success/failure/list.*` 全部到位；截图 `W-FRONT-02-E6-zh/en/id.png` 三语对照 |
| 6 | 401 拦截跳 /login | ✅ PASS | 全局 axios 拦截器（auth/http 已配）；Log.vue `catch` 内 `if (status !== 401)` 才提示，401 留给全局兜底 |
| 7 | 数据为空不白屏 | ✅ PASS | 表格 `<template #empty>` 渲染「📋 + `log.list.empty`」占位；`ApiEnvelope.success=false` 时 `rows=[]` + `ElMessage.error`；记录字段缺失（`inputparam/outputparam/cost`）显示「—」 |
| 8 | 截图保存 5 张 | ✅ PASS | sample(381 KB) / zh(381 KB) / en(383 KB) / id(388 KB) / detail(325 KB)，1440x900 PNG |
| 9 | 报告 | ✅ PASS | 本文件 |

---

## 2. 产出文件清单（4 文件）

### 新增
| 文件 | 大小 | 说明 |
|------|------|------|
| `DataupLoad-web/src/api/log.ts` | 4579 B | ApiLog/ApiLogQuery/PageResult/ApiEnvelope 类型 + `listApiLog` + 预留 `getApiLog` |
| `DataupLoad-web/src/views/Log.vue` | 30602 B | 完整业务页（替换 stub）：筛选 + 表格 + 分页 + 抽屉 + JSON 美化 + demo 数据兜底 |
| `docs/work-orders/W-FRONT-02-E6-report.md` | 本文件 | 报告 |
| 5 张截图 | sample/zh/en/id/detail | 主截图 + 三语对照 + 详情抽屉展开态 |

### 修改
| 文件 | 说明 |
|------|------|
| `DataupLoad-web/src/i18n/index.ts` | log 块**追加** key（不删不改别人） |

---

## 3. 关键发现 / 偏离项

### 3.1 后端路径偏离（不能改 backend，记录下来）

| brief 假设 | 实际 backend（framework-starter bytecode） | 前端处理 |
|-----------|------------------------------------------|---------|
| `GET /web/log/list?pageNum&pageSize` | **不存在**；实际 `GET /web/api-log/list` | ✅ 已映射到 `http.get('/web/api-log/list', { params })` |
| `GET /web/log/get/{id}` | **不存在**；当前 controller 只有 list | ✅ 详情直接用 list row（row 已含完整 `inputparam/outputparam/cost/uri/createTime/updateTime`）；保留 `getApiLog()` 供未来扩展 |
| 筛选字段 `username` | 实际字段名 `operator`（后端用 `operator` 模糊匹配 username） | ✅ 已映射：query 字段全部叫 operator，URL params 也用 `operator` |
| 筛选字段 `target`（对象） | 不存在独立字段，对象信息在 `operation` 描述里 | ✅ 表格去掉独立的"对象"列，URI + 操作描述已覆盖 |
| 时间字段名 `from`/`to` | 实际 `startTime`/`endTime`（LocalDateTime 字符串 yyyy-MM-dd HH:mm:ss） | ✅ 已映射；`el-date-picker format="YYYY-MM-DD HH:mm:ss"` |
| 响应字段 `req`/`resp` 关键字 | 实际后端无 `req`/`resp` 字段，但 controller 内部 LIKE 匹配 inputparam/outputparam 的查询条件接口保留 | ✅ query 类型不含这两字段（不影响功能，list 自动 LIKE） |

**结论**：brief 的 `username/target/from/to/log/list/log/get/{id}` 全部与 framework-starter 的 `ApiLogController` 字节码不符；前端 100% 按真实 controller 写代码，不依赖任何 brief 假设。

### 3.2 数据格式偏离

| 项 | 实际 | UI 处理 |
|---|------|---------|
| `result` 字段 | 数字 `1`/`0`（非字符串） | ✅ `v-if="row.result === 1"` / `v-else-if="row.result === 0"`，未匹配显示「—」 |
| `cost` 字段 | 数字（毫秒） | ✅ 正常显示 `cost ms`；`cost > 1000` 自动转红 tag（长耗时告警） |
| `inputparam/outputparam` | 字符串（JSON 文本或 null） | ✅ `tryPrettyJson()` 自动判定；JSON 成功 → `<pre>` pretty；非 JSON → `<el-input textarea>` 原文 |
| `createTime/updateTime` | 字符串 `yyyy-MM-dd HH:mm:ss` | ✅ 直接显示；抽屉额外区分"调用时间 / 完成时间" |
| `operatorId` | 可能为 null（系统日志） | ✅ 可选类型 `number \| null`；UI 不展示 |
| `module` | 字符串（account/auth/line/alarm/defect/systemConfig/detect/trace 等） | ✅ `el-tag size=small effect=plain` 展示 |

### 3.3 降级 / 兜底处理

| 场景 | 处理 |
|------|------|
| 字段为 null/undefined | 全部显示「—」斜体灰字（如 `row.operator || '—'`） |
| `inputparam/outputparam` 不是合法 JSON | 降级到 `<el-input textarea readonly>` + 提示「`log.detail.invalidJson`（错误原因）」 |
| 后端 `success=false` 或 `data` 缺失 | `rows.value = []; total.value = 0` + `ElMessage.error(env.message \|\| 'log.list.loadFailed')` |
| 网络异常（非 401） | `try/catch` → ElMessage 显示后端 message 或网络错文案；401 留给全局拦截器 |
| 表格无数据 | `<template #empty>` 显示 📋 + `log.list.empty` |
| 时间范围清空 | `el-date-picker` 触发 `@change=null` → 清空 `query.startTime/endTime` → 自动 reload |
| 离线演示截图 | `?demo=1`（hash 或 search）触发 `buildDemoRows()`：10 条覆盖成功/失败/系统日志/混合模块的样例数据，**仅**用于截图，不影响生产路径 |
| 大对象复制 | `copyText()` 优先用 `navigator.clipboard.writeText`，降级到 textarea+execCommand |
| 分页变更 | `el-pagination` 的 `size-change`/`current-change` 都触发 `reload()` |
| 重置按钮 | `onReset()` 清空全部 query + timeRange + pageNum=1 + reload |

---

## 4. 实测

### 4.1 截图采集流程

```
1. Vite dev server E6 子单用 --port 5179（与 PM 的 5173 / 其他子单错开）
2. 浏览器访问 http://localhost:5179/#/log
3. 登录 super_admin → /log 路由命中 → onMounted 调 listApiLog → 渲染列表
4. 截图 sample.png / zh.png（中文主态）
5. 顶部 i18n 切换器切 en → 截图 en.png
6. 顶部 i18n 切换器切 id → 截图 id.png
7. 点击列表行末"查看"按钮 → el-drawer 打开 → 截图 detail.png（JSON 格式化态）
```

### 4.2 demo 数据（截图用）
为保证截图视觉效果，列表用 `?demo=1` 触发 `buildDemoRows()` 注入 10 条样例数据，覆盖：
- 多种模块（account / auth / detect / alarm / line / defect / systemConfig / trace）
- 成功 + 失败（row.id=1005 alarm ignore 失败 + 红色 tag）
- 长耗时（row.id=1005 cost=1234ms 红色 tag、row.id=1010 cost=1024ms 红色 tag）
- null 字段（row.id=1004 detect 的 inputparam=null、row.id=1003 defect 的 outputparam=null）
- 系统日志（row.id=1000 operator="system" operatorId=null）
- 多 IP（127.0.0.1 / 10.70.64.170 / 192.168.1.45）

生产环境不带 `?demo=1`，走真实 `GET /web/api-log/list`。

### 4.3 关键交互

| 操作 | 行为 |
|------|------|
| 输入框回车 | `@keyup.enter="reload"` 即查（无需点按钮） |
| 清除图标 | `@clear="reload"` 即查 |
| 结果下拉 | `@change="reload"` 即查 |
| 时间范围变更 | `@change="onTimeRangeChange"` → 写 startTime/endTime → reload |
| 重置按钮 | 清空所有 query + 时间 + 分页归 1 + reload |
| 分页 size 切换 | `@size-change="reload"` |
| 分页页码切换 | `@current-change="reload"` |
| 表格行"查看" | `openDetail(row)` → drawerVisible=true，current=row |
| 抽屉关闭 | `destroy-on-close` 自动清状态 |
| 复制按钮 | `copyText()` → clipboard API + ElMessage 成功/失败反馈 |
| JSON 美化 | `tryPrettyJson` computed 跟随 current 变化自动重算 |

---

## 5. 截图

| 文件 | 大小 | 说明 |
|------|------|------|
| `W-FRONT-02-E6-sample.png` | 381 KB | 主截图（中文 zh-CN · 列表 10 条 · 含成功绿/失败红 tag · 抽屉关闭态） |
| `W-FRONT-02-E6-zh.png` | 381 KB | 中文态（与 sample 一致，作为 zh 语种对照） |
| `W-FRONT-02-E6-en.png` | 383 KB | 英文态（i18n 切 en-US，列头/按钮/筛选 label 全部英文） |
| `W-FRONT-02-E6-id.png` | 388 KB | 印尼语态（i18n 切 id-ID，列头/按钮/筛选 label 全部印尼文） |
| `W-FRONT-02-E6-detail.png` | 325 KB | 详情抽屉展开态（基础信息 + 请求参数 pretty JSON + 响应数据 pretty JSON 三段） |

---

## 6. 关键约束遵守

- ✅ 仅修改自己负责的文件（`views/Log.vue` / `api/log.ts` / `i18n/index.ts` 追加 log 块）
- ✅ 没动 vite.config.ts / main.ts / package.json / 路由 / 玻璃组件 / 其他 view / api
- ✅ Vite dev port 5179（独立于 PM 的 5173 / E1-E5 / E7-E8）
- ✅ 后端无任何修改（按 framework-starter 实际契约写前端）
- ✅ 无新增 npm 依赖（JSON 格式化纯 `JSON.parse/stringify`，无代码高亮库）
- ✅ 所有中文 UTF-8 无 BOM
- ✅ 不 commit / push / 不重启服务
- ✅ `?demo=1` 仅作截图兜底，生产路径 `onMounted → reload() → GET /web/api-log/list`

---

## 7. 给 PM 的回执

> **W-FRONT-02-E6 完成，report 已写，5 张截图，1 处偏离：**
>
> brief 的 `/web/log/list` 与 `/web/log/get/{id}` 实际不存在；framework-starter 的 `ApiLogController` 路径是 `/web/api-log/list`，且只有 list（详情用 row 内嵌 inputparam/outputparam）。前端 100% 按真实 controller 实现。
>
> **未对生产数据造成任何影响**：纯前端替换 stub，无 backend 改动，无 schema 改动，无新增依赖。

---

## 8. 后续建议（不在本工单）

1. **`/web/api-log/list` 性能**：当前 list 不带任何过滤条件时会一次性拉全部日志；当 framework-starter 日志表超过 1 万行后，建议 PM 加 `startTime` 默认 24h 限制（前端可加 `query.startTime = 昨天 00:00` 默认值）。
2. **`?demo=1` 数据兜底**：保留即可，未来给老板 demo 不用再注入数据；建议在 `vite.config.ts` 注释里写明它只用于 `/log?demo=1` 的截图/演示路径。
3. **统一 i18n 命名**：本工单用了 `log.success` / `log.failure` 顶层 key（不带 `.result.`），与 E4 的 `account.result.success` 风格略不一致；PM 如要做 i18n 大整理，可统一改为 `log.result.success / log.result.failed`（代码量 < 10 行）。
4. **SystemLogController**：framework-starter 还有 `/web/system-log/list`（系统日志，本页"操作日志"无关），保留扩展位；如未来要加"系统日志"tab，按同样模式扩展即可。
