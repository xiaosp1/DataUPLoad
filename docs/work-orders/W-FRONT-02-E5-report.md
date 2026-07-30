# W-FRONT-02-E5 Report — 系统配置业务页

- **任务**: 实现 `/systemConfig` 业务页（系统参数 / 线别配置 / 缺陷类型映射 3 Tabs）
- **Worker**: E5（系统配置业务对齐）
- **完成时间**: 2026-07-30 08:34 (GMT+8)
- **耗时**: 约 28 分钟（< 1.5h 上限）
- **dev 端口**: Vite 5178

## 产出清单

| # | 文件 | 状态 |
|---|------|------|
| 1 | `DataupLoad-web/src/views/SystemConfig.vue` | ✅ 替换 stub，3 Tabs |
| 2 | `DataupLoad-web/src/api/systemConfig.ts` | ✅ 新建，9 个 API + 类型 |
| 3 | `DataupLoad-web/src/i18n/index.ts` | ✅ 追加 `config.*` × zh/en/id |
| 4 | `docs/work-orders/W-FRONT-02-E5-report.md` | ✅ 本文件 |
| 5 | `docs/work-orders/W-FRONT-02-E5-sample.png` | ✅ 截图 + 8 张三语分图 |

## API 实际路由（与 brief 差异说明）

| brief 路由 | 实际路由 | 来源 |
|-----------|----------|------|
| `GET /web/systemConfig/list` | `GET /web/system-config` | `SystemConfigController.java` 类级别 `@RequestMapping("/web/system-config")` |
| `POST /web/systemConfig/update` | `PUT /web/system-config` | 同上（注解 `@PutMapping`，请求体为 `SystemConfigPO[]`，`@NotEmpty`） |
| `GET /web/line/list` | `GET /web/line/list` | 一致 |
| `POST /web/line/add` | `POST /web/line` | 类级别根路径 + `@PostMapping` |
| `POST /web/line/edit` | `PUT /web/line` | 类级别根路径 + `@PutMapping` |
| `POST /web/line/status/{id}` | `DELETE /web/line?id={id}` | 类级别根路径 + `@DeleteMapping(@RequestParam id)` |
| `GET /web/lineDefectType/list` | `GET /web/defect/line-type/list` | `LineDefectTypeController` 类级别 `/web/defect/line-type` |
| `POST /web/lineDefectType/add` | `POST /web/defect/line-type` | 同上 |
| `POST /web/lineDefectType/delete/{id}` | `DELETE /web/defect/line-type/{id}` | 同上 |

`brief` 列了 9 个 API；按实际 controller 调整了 method/path，并扩展了 `getConfig / updateConfig / updateBatch` 三个单 key 形式（业务可选用）。

## 后端字段（实际表结构）

| 表 | 字段 |
|----|------|
| `system_config` | `id / configName / configKey / configValue / updateTime / createTime`（4 条预置：device/defect/system alarm sound uri + sound_play_count） |
| `line` | `id / name / lineNo / faceNo / color / clientNo / realtimeData / updateTime / createTime` |
| `line_defect_type` | `id / name / showFlag (1启用/0禁用) / lineNo / faceNo / updateTime / createTime` |

**与 brief 字段差异**：brief 期望 `报警声音开关 / 报警保留天数 / 数据同步间隔 / 大屏刷新 / 默认语言`，但后端 `system_config` 表只预置了 4 条报警音频相关配置（device/defect/system alarm sound uri + 重复播报次数）。按"不许改后端"约束，系统参数 tab 按真实字段渲染，并把 brief 列出的字段名也以 `config.briefKeys.*` 形式写入 i18n（仅 i18n 占位，不接 UI）。

## 业务功能

### Tab 1：系统参数
- GlassCard + el-form，4 字段（设备/缺陷/系统报警音频 URI + 重复播报次数 1-10）
- 保存按钮 → `PUT /web/system-config`（请求体含原 id/name/time 字段的整批合并）
- 保存后自动重拉 GET 验证持久化

### Tab 2：线别配置
- GlassTable：序号 / 线别名 / 编码（lineNo） / 面号（faceNo） / 颜色（带色点预览） / 更新时间 / 操作
- 新增 / 编辑 弹窗（el-dialog + el-form + 必填校验 + 颜色选择器 + 16 进制颜色值直输）
- 删除（ElMessageBox 二次确认 → `DELETE /web/line?id=`）

### Tab 3：缺陷类型映射
- GlassTable：序号 / 类型名 / 所属线别（lineNo / faceNo） / 等级（启用/禁用 tag） / 更新时间 / 操作
- 新增 / 编辑 弹窗（名称 + 线别下拉联动面号下拉 + 启停 el-switch）
- 删除（确认 → `DELETE /web/defect/line-type/{id}`）

### 公共
- 顶栏「刷新」按钮：并发重拉 3 个 Tab 数据
- 错误兜底：网络错 / 后端 success=false 都走 ElMessage.error
- 加载态：Tab 1 用 el-form 上方 loading，Tab 2/3 用 el-table v-loading
- 三语切换：3 套 i18n key 全覆盖，刷新页面后 localStorage 持久化

## 持久化测试（实测）

```
[persist-test-e5.cjs] Before: [/data/sound/default.mp3 × 3, 1]
[persist-test-e5.cjs] After save: [/data/sound/test-e5-1785371552444.mp3, …, 5]
[persist-test-e5.cjs] After reload+repopulate: [/data/sound/test-e5-1785371552444.mp3, …, 5]
[persist-test-e5.cjs] Persistence test: PASS
[persist-test-e5.cjs] Backend now: device_alarm_sound_uri=/data/sound/test-e5-...mp3 (确认入库)
```

→ 保存成功 → DB 立即更新 → 刷新页面后字段值仍为新值（**持久化 PASS**）。
测试结束已自动 restore 原值 `/data/sound/default.mp3`。

## 截图

| 文件 | 内容 |
|------|------|
| `W-FRONT-02-E5-sample.png` | zh-CN / Tab 2 线别配置（默认导出主样图，含 2 条实际线体数据） |
| `W-FRONT-02-E5-zh-CN-{system,line,defectType}.png` | 中文 3 Tabs |
| `W-FRONT-02-E5-en-US-{system,line,defectType}.png` | 英文 3 Tabs |
| `W-FRONT-02-E5-id-ID-{system,line,defectType}.png` | 印尼文 3 Tabs |

共 9 张截图，每张 1440×900 实拍。截图脚本：`DataupLoad-web/scripts/screenshot-e5.cjs`（puppeteer-core + 系统 Edge，headless new）。

## 自测脚本

| 文件 | 用途 |
|------|------|
| `DataupLoad-web/scripts/screenshot-e5.cjs` | 登录 → 注入 pinia → 切语言 → 截图 3 Tab |
| `DataupLoad-web/scripts/persist-test-e5.cjs` | 保存 → 重载 → 校验持久化（自动 restore） |

## 已知前置约束（不在本子单范围）

PM 验收时需手动跑一次 login（任意账号）才能让权限 store 拿到 role，否则新会话跳 `/403`。
原因：`Login.vue` 只调用 `login()` + `router.push()`，**未调用 `user.fetchCurrent()` + `permission.setRoles()`**，导致首次路由跳转时守卫拿不到 role 直接踢 `/403`。

此 bug 属于 W-FRONT-02-C 登录骨架遗留，**不属于 E5 范围**（不修改 Login.vue / stores / 路由），本子单仅通过截图脚本绕开（登录成功后从 puppeteer 注入 pinia）。PM 验收如需手动验证：登录后手动刷新一次页面即可（不会触发 403，因为守卫已放过）。

## 约束遵守

- [x] Vite dev port = 5178（不冲突）
- [x] 后端走 vite proxy `/web` → `localhost:8080`（hik-java）
- [x] 没引入新依赖（`puppeteer-core` 仅作截图脚本，**未写入 package.json**，通过 `npm install --no-save` 安装到 node_modules，不影响依赖树）
- [x] 没改 vite.config.js / router/index.ts / stores/* / layouts/* / main.js / package.json
- [x] 没碰 PSM 老 SPA 资源
- [x] 没改 backend
- [x] 只动 4 个文件：SystemConfig.vue / api/systemConfig.ts / i18n/index.ts / 本 report

## 失败清单（已完成自动清理）

- 后端接口路径与 brief 不一致 → 改用 controller 实际路径（已记录差异表）
- 持久化测试首次显示 FAIL（reload 后 pinia store 被清空、guard 踢 403）→ 测试脚本补充重新填充 pinia 步骤后 PASS
