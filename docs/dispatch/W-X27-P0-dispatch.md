# W-X27 — P0 工单派工记录 (2026-07-24 17:42)

## 派工目标
基于 W-AUDIT-01 审计报告，启动 P0 工单，把管理后台 + 大屏聚合全部解锁。
工单来源: `docs/audit/2026-07-24-AUDIT-REPORT.md` §三 P0 表

## 工单清单（6 项）

| 工单 | 内容 | 模块 | 工时 | 派工模式 |
|------|------|------|------|----------|
| W-ALM-01 | 补 AlarmRecordMapper 5 聚合查询 + XML | alarm | 4h | codex exec 并行 |
| W-ALM-02 | 补 AlarmRecordServiceImpl 6 管理方法 | alarm | 4h | 等 W-ALM-01 完后串行 |
| W-DET-01 | 补 IDefectDayRecordService 8 缺失方法 | detect | 3h | codex exec 并行 |
| W-DET-02 | 补 ILineDayRecordService 4 方法 + 边界修复 | line | 2h | codex exec 并行 |
| W-LIN-01 | 补 LineServiceImpl 8 核心业务方法 + 12 bean | line | 6h | codex exec 并行（量最大） |
| W-LIN-02 | 补 StateStatistic 3 派生方法 | line | 1h | 我手动 5 分钟搞定 |

## 派工策略

**并行 4 个 codex exec**：
1. W-ALM-01 → 拆 SQL/Mapper/XML
2. W-DET-01 → 8 个 Service 方法
3. W-DET-02 → 4 个方法 + 边界修复
4. W-LIN-01 → LineServiceImpl 全套

**串行 1 个**：
- W-ALM-02 等 W-ALM-01 完成后启动

**手动 1 个**：
- W-LIN-02 我手撸（简单快速）

## 派工时间表

- 17:42 — 启动 4 个并行 worker
- 17:42 — 同步手撸 W-LIN-02（预计 17:47 完成）
- 17:42–18:30 — 等待 4 个 worker 完成
- 18:30–18:50 — 编译验证 + 合并 W-ALM-02
- 18:50–19:00 — 重启 hik-java + 冒烟测试
- 19:00–19:10 — 推送 GitHub + 更新 STATUS.md

## 第二批派工 (18:26) — 补 controller + 编译修复

W-X27 第一批 5/6 全部完成 + GitHub push 成功。发现 controller 缺口，启动第二批 4 个并行 worker:

| 工单 | 内容 | runId |
|---|---|---|
| W-LIN-03 | LineController 11 endpoint | `b8a1c158-adfc-44a4-91e8-0576c3d15332` |
| W-ALM-03 | AlarmRecordController 6 endpoint | `126d1cbd-e493-4556-a6ac-35f526257069` |
| W-DET-03 | DefectDayRecord/LineDayRecord Controller | `ad7978e3-103a-451d-9171-c0ce37c164b9` |
| W-FIX-01 | javac -parameters + searchOffLineClient | `b9cbe35c-2d03-430f-b1d7-a19c0535a2f2` |

启动时间: 18:26
预计完成: 18:50 (约 25 分钟)
后续: 全量编译 + 重启 + 冒烟 + push

## 派工命令模板

```powershell
codex exec -C "E:\DEMO\数据采集" --skip-git-repo-check -s workspace-write "<prompt>"
```

## 派工记录

### W-ALM-01 — 17:43 sessions_spawn
- childSessionKey: `agent:industry:subagent:82b586d3-e0f3-4d44-a3a4-c0305af05724`
- runId: `c747541b-b358-407c-9d70-c26a2a6542f1`
- model: openai/INTCO-Thinking
- status: 跑着

### W-DET-01 — 17:43 sessions_spawn
- childSessionKey: `agent:industry:subagent:5ef0ba38-3cc8-448a-847e-95c3182cf424`
- runId: `2c342ae9-b477-4c27-8bce-86c238a6d1e6`
- model: openai/INTCO-Thinking
- status: 跑着

### W-DET-02 — 17:43 sessions_spawn ✅
- childSessionKey: `agent:industry:subagent:dbf64038-f7b0-42cf-94ec-4e125279e6c1`
- runId: `1c147574-59f4-4942-a369-f61a8a6d9fde`
- model: openai/INTCO-Thinking
- **status: 完成 17:53** (9m25s, 2.1m tokens)
- 报告: `docs/work-orders/W-DET-02-report.md`
- 改动: ILineDayRecordService.java + LineDayRecordServiceImpl.java
- 编译: 0 errors
- ⚠️ 注意: `listByTimeAndLineNo` 按 PSM 真实签名落地（3 参返回单对象），偏离 brief

### W-LIN-01 — 17:43 sessions_spawn ✅
- childSessionKey: `agent:industry:subagent:2526630d-821c-4019-9df8-9115d610cce8`
- runId: `5750b03c-b296-4f9a-9d7e-45b2b1b4797d`
- **status: 完成 18:21** (38m, 6 个文件, 8 个方法 1:1 PSM)
- 报告: `docs/work-orders/W-LIN-01-report.md`
- 编译: 0 errors
- ⚠️ 已知差异: LineController 11 endpoint 未补 (下一批 W-LIN-03)
- ⚠️ 已知差异: PSM `detect/util/TimeRange` 不存在 → 内联 day-step 循环等价实现

### W-ALM-02 — 等待 W-ALM-01
- 触发条件：W-ALM-01 完成事件
- 状态：阻塞

### W-LIN-02 — 17:43 手动启动 ✅
- 状态: **完成 17:45**
- 报告: `docs/work-orders/W-LIN-02-report.md`
- 编译: 0 errors
- hik-java: PID 11824 在线

## 第二批完成情况

### W-ALM-03 — 18:22 sessions_spawn ✅
- 完成: 18:31 (8m58s)
- 改动: AlarmRecordController 2 → 8 endpoint
- 编译: 0 errors
- 报告: `docs/work-orders/W-ALM-03-report.md`

### W-FIX-01 — 18:22 sessions_spawn ✅
- 完成: 18:34 (12m)
- 改动: 编译脚本加 -parameters + StatusRecordServiceImpl.searchOffLineClient
- 编译: 0 errors
- 报告: `docs/work-orders/W-FIX-01-report.md`

### W-LIN-03 — 18:22 sessions_spawn ✅
- 完成: 18:38 (16m)
- 改动: LineController 2 → 11 endpoint (含 2 stub)
- 编译: 0 errors
- 报告: `docs/work-orders/W-LIN-03-report.md`

### W-DET-03 — 18:22 sessions_spawn
- 状态: 跑着 (18:42 仍在跑)
