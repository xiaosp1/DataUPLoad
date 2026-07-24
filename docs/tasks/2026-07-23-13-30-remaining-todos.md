# 剩余可干事项清单 — 老板 13:30 "先干了" 决策

**整理人**：PM 锋卫 🏭
**整理时间**：2026-07-23 13:30

---

## 📊 现状快照

| 项 | 状态 |
|---|---|
| **生产链路** | ✅ hik-java PID 33248 alive（5h）/ 38 相机 ESTABLISHED / 业务正常 |
| **yk 双开关** | ✅ loginEnabled=true（ticket 拿到）/ uploadEnabled=false（静默）|
| **yk ERROR** | ✅ 0 增量（防疯狂推送第一道关生效）|
| **W-X15/W-X16/W-X17** | 🟡 已派 Worker，跑中（PM 等回报）|
| **W-X13d-v3 jar 重建** | 🟡 Worker 派出去（可能在跑，资产沉淀）|
| **E:\DataupLoad-final.jar** | 🔒 仍被 sandbox 锁（63.59MB，被 PM 之前弄小）|

---

## 🎯 老板终目的：**不要让报警信息疯狂推送**

防疯狂推送有 **3 道关卡**（W-X15 测的）：

1. **add() 同类去重**（AlarmRecordServiceImpl line 145-156）
2. **sendAlarmMessage isIgnore**（ignore_alarm 白名单）
3. **yk.uploadEnabled=false**（W-X13d 已落地）

W-X15 测的是 1+2+3 的完整链路。

---

## 📋 剩余能干的工单（按"不阻塞老板当前节奏"分组）

### A 组：W-X15~17 跑完后立刻派（依赖老板当前关注）

| # | 工单 | 内容 | 优先级 | 阻塞 |
|---|---|---|---|---|
| **A1** | **W-X18** 灰盒期 24h 监控扩展 | 把 W-X14 SOP 三信号扩展到 alarm_record 全字段 + ticket 续期监控 | 🔴 P0 | W-X15 完 |
| **A2** | **W-X19** 报警密度阈值告警 | 1min 内同类报警 > X 条 → 群内告警 + 自动临时 ignore | 🔴 P0 | A1 完 |
| **A3** | **W-X20** ignore_alarm 前端对接（缺）| PSM 有 web/alarm/ignore 页面，DataupLoad 后端有但前端可能没接 | 🟡 P1 | A1 完 |
| **A4** | **W-X21** defect_type 表 alarmEnable 全停的应急开关 | 加全局开关 `alarm.global-enabled`，紧急可一键关停 | 🟡 P1 | - |

### B 组：架构性（不影响当前）

| # | 工单 | 内容 | 优先级 | 阻塞 |
|---|---|---|---|---|
| **B1** | **W-B01 重启迁移**：把 cp 模式迁回 jar 模式 | 1) 等 jar 锁自动释放 2) 用 Maven 重建新 jar 3) 部署新 jar 4) 改回 jar 启动 | 🟡 P1 | - |
| **B2** | **W-B02** v0.5 报警 v2 接口（PSM WebSocket 4 路推送） | 当前只测 1 路 yk，PSM 是 4 路（WS 文本/WS 音效/yk MES/yk 音频） | 🟡 P1 | - |
| **B3** | **W-B03** retention cron 改 3 天（PSM 金标准，W-A18.5 升级） | 当前 DataupLoad 是 90 天（涨库风险）vs PSM 3 天 | 🟡 P1 | W-X17 测完 |
| **B4** | **W-B04** alarm_record retention V1.20 ignore_alarm 完整迁移测试 | 测 start_time/end_time 边界 + V1.20 schema 完整性 | 🟢 P2 | - |

### C 组：资产沉淀（PM 排好，老板随时可派）

| # | 工单 | 内容 | 优先级 | 阻塞 |
|---|---|---|---|---|
| **C1** | **W-C01** memory_search 索引修复 | 当前 memory_search 索引不一致（disabled=true），需要 `openclaw memory index --force` | 🟢 P2 | - |
| **C2** | **W-C02** git commit 大整理 | 当前项目根 96+ 脏文件（工单文档 / 测试 / log），需要按铁则 25 commit | 🟢 P2 | - |
| **C3** | **W-C03** ADR-0006 升级（Java 端 yk 双开关）| 现在只有 C# 端 ADR，Java 端 W-X13d 双开关也需要 ADR | 🟢 P2 | - |
| **C4** | **W-C04** 铁则 42/43/44/45 正式立项 | 4 条 PM 翻车立的新铁则，需要正式文档归档 | 🟢 P2 | - |
| **C5** | **W-C05** PSM AlarmRecordServiceImpl 1:1 对比报告 | PSM vs DataupLoad 报警处理代码 1:1 差异表 | 🟢 P2 | - |

### D 组：已经过期 / 需要老板重新评估

| # | 工单 | 状态 |
|---|---|---|
| **D1** | W-X11c 修 ignore varchar vs timestamp | 已 PASS（00:54）|
| **D2** | W-X11e 修 alarm_record 入库早返 | 待派（依赖 PG schema 重新评估）|
| **D3** | W-X12c Telegram 告警触发器 | 待派（老板当前焦点不在这）|
| **D4** | W-G01a/b/c PM 推送 SOP 脚本化 | 等灰盒解除 |
| **D5** | W-DB-MON PG 涨库监控 | P2，老板未拍 |

---

## 🎯 PM 建议立刻派的（A 组 4 项 + B1 jar 重建）

老板说"先干了"——PM 默认按 **A 组全派 + B1 jar 重建 + B3 retention 修** 这 6 项派：

| # | 工单 | 派单 |
|---|---|---|
| **W-X18** | 24h 监控扩展 | 立即 |
| **W-X19** | 报警密度阈值告警 | W-X15 完 |
| **W-X20** | ignore_alarm 前端对接 | A1 完 |
| **W-X21** | alarm 全局开关 | 立即 |
| **W-B01** | cp 模式迁回 jar | 立即 |
| **W-B03** | retention 3 天 | W-X17 完 |

---

## ⚠️ PM 关键承诺

- ✅ 不重启 hik-java PID 33248（铁则 44）
- ✅ 不改 yk.uploadEnabled（铁则 36，老板没单独指令）
- ✅ 不抢 PM 已派工单的 Worker（让 W-X15/16/17 跑完）
- ✅ 所有新工单都有 DoD 5 重验证清单（铁则 40）
- ✅ 任何"我看着没问题"汇报都打回（铁则 41）

---

🏭 PM 锋卫 · 2026-07-23 13:30
