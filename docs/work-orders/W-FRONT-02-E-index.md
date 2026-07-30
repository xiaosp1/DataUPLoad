# W-FRONT-02-E 总索引 — 业务对齐期（8 子单并行）

- **派工时间**: 2026-07-30
- **触发**: 老板 07:59 拍板 A 路线（E1-E8 业务对齐期）
- **耗时上限**: 1.5h / 子单，max 8 张并行
- **PM**: 锋卫
- **派工时间**: 2026-07-30 08:06（老板拍板后）
- **多 worker 隔离**: 主目录同跑 + Vite dev port 错开（5174..5181），各自只改 1 个 view 文件 + 1 个 api 文件，PM merge 时按文件清单 apply
  - **为什么不用 worktree**: A/B/C/D 子单代码都在工作目录未 commit，worktree 拿不到 D 完成的 stub 文件。改用主目录并行的轻隔离方案。

## 8 张子单清单

| 子单 | 路由 | 业务来源（后端） | PSM 对照路径 |
|------|------|---------------|-------------|
| **E1** | /realtime | DetectData + Line + Plan + StateStatistic + StateChange | 折线 KPI 看板 |
| **E2** | /alarm | AlarmRecord + IgnoreAlarm + DefectType + WS /ws?uid=&type=alarm | 报警列表 + 实时推送 |
| **E3** | /defect | LineDefectType + DefectDayRecord | 缺陷表格 + 详情 |
| **E4** | /account | framework-starter account（POST /web/auth/login / GET /web/account/current /list/add/resetPwd） | 账号 CRUD + 当前用户 |
| **E5** | /systemConfig | SystemConfig + Line + LineDefectType | 系统配置 + 线别配置 |
| **E6** | /log | framework-starter logger（GET /web/log/list?pageNum=） | 操作日志 |
| **E7** | /userManage | framework-starter account（同一组接口） | 用户管理（区别 E4：账号/用户二义时按 PSM 双 tab 区分） |
| **E8** | /screen | Screen WS + Line + DefectDayRecord + StateStatistic | 大屏多图表 |

## 派工前 PM 已确认

- [x] D 子单完成，8 个 stub 已用 GlassPage + GlassCard 占位
- [x] 路由表已配齐 8 业务路由 + 403 + 守卫
- [x] 三语 i18n 643 keys（菜单/通用按钮/字段名都有）
- [x] B 子单 5 玻璃组件就位（GlassCard / GlassButton / GlassMenuItem / GlassTable / GlassPage）
- [x] 后端核心 controller 已存在（alarm/defect/line/config/detect）
- [x] framework-starter 提供 account/log/screen 接口

## 派工规则（每张子单 brief 顶部一致）

```
【任务】W-FRONT-02-E{N}
【必读】
  - docs/work-orders/W-FRONT-02-E{N}-brief.md
  - docs/work-orders/W-FRONT-02-brief.md（并行派工方案）
  - DataupLoad-web/src/views/{Page}.vue（stub，已 GlassPage 容器）
  - DataupLoad-web/src/router/index.ts（meta.permission 已配）
【必产出】
  1. docs/work-orders/W-FRONT-02-E{N}-report.md（截图 + diff 结论）
  2. DataupLoad-web/src/views/{Page}.vue（业务实现）
  3. DataupLoad-web/src/api/{page}.ts（API 调用）
  4. 如新增 i18n key，append 到 DataupLoad-web/src/i18n/locales/{zh,en,id}.ts
【PM 验收门控】
  - 60 分钟无回执升级（取进程快照）
  - 完成后回 PM: "W-FRONT-02-E{N} 完成, report 已写, 截图 X 张"
  - PM 对照截图 diff PSM 老 SPA，差异 > 5% 打回
【禁止】
  - 不许跨子单（不许动其它 E 子单文件）
  - 不许改 vite.config.ts / main.ts / package.json
  - 不许改 backend
  - 不许引入 brief 之外的依赖（echarts 已装，可直接 import）
  - 不许碰 PSM 老 SPA 资源
```

## verify 脚本（PM 用）

每个 E 子单 worker 必须自测：
```powershell
# 子单目录
cd E:\DEMO\数据采集-worktrees\w-front-02-E{N}\DataupLoad-web
# 跑 dev 自测
npm run dev -- --port 5174  # 8 子单错开端口 5174..5181
# 截图（playwright headless 或手动）
# PM 验收时对比 docs/domain/海康大屏逆向/PSM/server/web/{对应 js 截图}
```

## PM 验收 checklist（每张子单）

- [ ] src/views/{Page}.vue 不是 stub 状态（不再是"🚧 业务对齐期"占位）
- [ ] src/api/{page}.ts 真实调用后端
- [ ] 至少 1 张截图 docs/work-orders/W-FRONT-02-E{N}-sample.png
- [ ] 至少 1 张对比图 docs/work-orders/W-FRONT-02-E{N}-psm.png（PSM 老 SPA 同页）
- [ ] i18n 三语切换正常
- [ ] 报错兜底：网络错 / 401 / 500 都有提示
- [ ] 数据为空 / null / undefined 不白屏

## 已知约束（worker 必须知道）

- Vite dev server **已在主目录 PID 8164 port 5173 跑着**，新 worker **必须用 --port 5174..5181 错开**
- 后端 hik-java PID 35704 port 80 **稳定**，worker 自测都用 http://localhost:80
- satoken cookie 跨域处理：axios `withCredentials: true`，CORS 已配
- Element Plus 默认组件名 `el-button` `el-table` `el-form` `el-input` `el-dialog` `el-pagination` `el-message` 可直接用
- echarts 用 `import * as echarts from 'echarts'`（已装）
- i18n key 命名：`{page}.{module}.{action}` 如 `alarm.list.refresh`、`defect.detail.close`
- glass 风格继承：每个表格用 GlassTable，每张卡片用 GlassCard，按钮用 GlassButton

## 派工顺序（PM 操作）

1. 复制本索引到 worktree 控制台（一次性交代 8 张子单的共同规则）
2. 8 个 codex exec 并行下发（每个 worktree 一个）
3. 每个子单完成 → PM 在主目录 merge worktree → 写 report → 更新 HEARTBEAT
4. 8/8 完成 → 派 G0 + G

## 实际派工策略（2026-07-30 08:06 调整）

**放弃 git worktree，改用主目录并行 + 端口错开**：

- 每个 worker 用独立 codex exec session
- 每个 worker **只动**：
  - `DataupLoad-web/src/views/{Page}.vue`（自己的 stub）
  - `DataupLoad-web/src/api/{page}.ts`（自己的 API）
  - `DataupLoad-web/src/i18n/locales/{zh-CN,en-US,id-ID}.ts`（追加自己的 key，**不删不改别人 key**）
  - `docs/work-orders/W-FRONT-02-E{N}-report.md`（自己的报告）
- **共用资源**（不要碰）：
  - `vite.config.ts` / `package.json` / `main.ts` / `App.vue` / `router/index.ts` / `stores/*` / `layouts/*` / `components/Glass*.vue`
- **Vite dev port 分配**：
  | 子单 | Vite port | 后端 port |
  |------|-----------|----------|
  | E1 | 5174 | 80 |
  | E2 | 5175 | 80 |
  | E3 | 5176 | 80 |
  | E4 | 5177 | 80 |
  | E5 | 5178 | 80 |
  | E6 | 5179 | 80 |
  | E7 | 5180 | 80 |
  | E8 | 5181 | 80 |
- **PM merge 时**：8 个 worker 都完成 → PM 写一份总览 report → 整体 git add + commit（一次性 commit）

## 子单依赖（明确边界，不许越界）

| 子单 | 自己改 | 别人给的（不许改） |
|------|--------|------------------|
| E1 | RealTime.vue / api/realtime.ts | GlassPage GlassCard GlassButton |
| E2 | Alarm.vue / api/alarm.ts / utils/ws.ts | utils/ws.ts 由 E2 创建，E8 复用 |
| E3 | Defect.vue / api/defect.ts | 无 |
| E4 | Account.vue / api/account.ts / utils/sha256.ts | 无 |
| E5 | SystemConfig.vue / api/systemConfig.ts | 无 |
| E6 | Log.vue / api/log.ts | 无 |
| E7 | UserManage.vue / api/userManage.ts | 无 |
| E8 | Screen.vue / api/screen.ts / utils/screenWs.ts | 复用 E2 的 utils/ws.ts 思路 |


