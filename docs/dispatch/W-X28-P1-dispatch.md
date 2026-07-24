# W-X28 — P1 工单派工记录 (2026-07-24 19:14)

## 派工目标
W-X27 P0 冲刺完成（10/10）后，启动 P1 工单，把产线管理 + WS 推送 + 缺陷详情 + ADR 留痕全部补齐。
工单来源: `docs/audit/2026-07-24-AUDIT-REPORT.md` §三 P1 表

## 工单清单（7 项）

| 工单 | 内容 | 模块 | 工时 | 派工 |
|------|------|------|------|------|
| W-LIN-04 | LineMapper 2 SQL 投影 + LineDayRecordMapper.listByAttribute | line | 1h | codex |
| W-LIN-05 | LineServiceImpl 4 方法 + LineController plan/manage + tree-search stub | line | 3h | codex |
| W-ALM-05 | handleAlarmNumGet WS 推送 + soundEnable 声音播放 | alarm | 2h | codex |
| W-DET-04 | DefectDayRecordMapper updateCount/selectDefectCountDay + getLocalTime() | detect | 2h | codex |
| W-SCR-01 | screen putIfAbsent vs put 复测 + ADR | screen | 1h | codex |
| W-YK-01 | yingke 双开关 ADR 留痕 | yingke | 0.5h | 手动 |
| W-CLEAN-01 | LinePO.java 死代码删除 | line | 0.5h | 手动 |
| W-CLEAN-02 | AlarmRecordService 死代码检查 | alarm | 0.5h | 手动（依赖 ALM-02） |

## 派工时间表
- 19:14 启动 5 个并行 codex worker
- 19:14 同步手撸 3 个 ADR/清理任务
- 19:14-19:50 等 codex 完成
- 19:50-20:00 全量编译 + 重启 + 冒烟 + push

## 派工记录（待补充）

### W-LIN-04 — 19:14 sessions_spawn ✅
- 完成: 19:25 (11m13s)
- 改动: LineMapper 2 方法 + LineDayRecordMapper 1 方法
- 编译: 0 errors
- 报告: `docs/work-orders/W-LIN-04-report.md`
- ⚠️ 已知限制: listAll 返回 Line 丢 DTO 投影 (后续 W-LIN-05 补 LineDTO 路径)
- ⚠️ listByAttribute 是新增，PSM 分布在 5 个具体方法上

### W-LIN-05 — 19:14 sessions_spawn ✅
- 完成: 19:42 (28m)
- 改动: ILineService 108→150 + LineServiceImpl 537→662 + LineController 230→262
- 4 service 方法: lineGroup / chgLineOrder / handleLineTreeSearch / listByLineNo 重载
- 3 endpoint: /web/line/tree-search / /web/line/chg-line-order / /web/line/plan/manage (stub)
- 额外联通升级: PUT /web/line/order + GET /web/line/tree (W-LIN-03 stub)
- 编译: 183 文件 0 errors
- 报告: `docs/work-orders/W-LIN-05-report.md` (21KB)

### W-ALM-05 — 19:14 sessions_spawn ✅
- 完成: 19:28 (13m34s)
- 改动: AlarmRecordServiceImpl +55 行 + AlarmConstants +47 行
- 编译: 0 errors
- 报告: `docs/work-orders/W-ALM-05-report.md`
- 亮点: handleAlarmNumGet / sendAlarmMessage 双双接入 WS 声音推送 (broadcastByUid "web" 频道)
- ⚠️ 差异: PSM `sendAlarmSoundWsMessage` 从 ISystemConfigService 读配置，DPL 未启用该组件 → 走 AlarmConstants 兑底常量

### W-DET-04 — 19:14 sessions_spawn ✅
- 完成: 19:28 (14m15s)
- 改动: DefectDayRecordMapper.updateCount + DefectDayRecord.getLocalTime
- 编译: 183 文件 0 errors
- 报告: `docs/work-orders/W-DET-04-report.md`
- ⚠️ 已知限制: updateCount 用 ; 分隔需 JDBC allowMultiQueries=true
- ⚠️ getLocalTime 有 NPE 风险（PSM 也不防）
- ⚠️ service 层未包装（PSM 没有 updateDefectCount，PSM Impl 直接调 mapper）

### W-SCR-01 — 19:14 sessions_spawn ✅
- 完成: 19:30 (15m01s)
- 改动: ScreenServiceImpl 第 156 行 put → putIfAbsent
- 编译: 0 errors
- 报告: `docs/work-orders/W-SCR-01-report.md`
- ADR: `docs/adr/0006-screen-cache-strategy.md` (9484 bytes)
- ⚠️ 重大发现: 审计原报告 PSM/DPL 方向写反了 — 订正已在审计文件后追加、ADR 记录
- 复测: 6/6 断言通过，保留 putIfAbsent (贴 PSM)

## 手动任务（已完成 ✅）

### W-YK-01 — 19:18 手动完成
- ADR-0007-yingke-dual-switch.md
- 结论: W-X13d 已实现双开关，YKConfig.java Javadoc 已写完整，仅做正式 ADR 留痕

### W-CLEAN-01 — 19:18 手动完成
- ADR-0008-line-po-alias-kept.md
- 结论: LinePO.java 不是死代码，9 个引用点，保留作为 PSM 风格兼容层

### W-CLEAN-02 — 19:20 手动完成
- ADR-0009-alarm-service-extra-methods.md
- 结论: Impl 多 2 方法是 W-X30a/b 工单有意扩展，不是死代码

## codex worker 状态（跑着）
- W-LIN-04 / W-LIN-05 / W-ALM-05 / W-DET-04 / W-SCR-01 5 个并行
