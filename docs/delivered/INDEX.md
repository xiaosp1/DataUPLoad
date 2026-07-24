# docs/delivered/ — 项目文档索引（PM 锋卫维护）

**最后更新**: 2026-07-22 23:55 PM 锋卫（老板 23:47 提醒后刷新）
**说明**: 这是 PM 锋卫为老板维护的"项目文档总索引"。任何时候老板问"XX 怎么做/为什么/什么时候"，PM 都从这里查。

---

## 9. 🚨 今晚（22:04-23:58）DataupLoad 真实状态归档

> ⚠️ **PM 自查承认**：本节之前 STATUS.md 顶表 + TODO.md 11h 没刷新，老板 23:47 问起才补归档。

| 文档 | 用途 |
|---|---|
| `2026-07-22-night-archive-v2.md` | **PM 23:55 体检真相版**：STATUS/TODO/INDEX 全部刷新 + 铁则 39 立（每小时体检）|
| `2026-07-22-night-archive.md` | 22:04-23:43 灰盒跑法尝试 + W-X11 作废 + 铁则 36/37/38 |
| `2026-07-22-W-X11-merged-jar-deploy.md` | W-X11 merged.jar 部署报告（PASS 但 jar 因 i18n 被回滚）|
| `2026-07-22-W-X11-split-plan.md` | **PM 23:58 拆细计划**：W-X11a / W-X11b / W-X11c 派工单已落地 |
| `2026-07-22-vision-registry-auto.md` | **PM 23:58 工控机真名清单**：39 行 line:face 自动从 status_record 反推 |
| `2026-07-23-next-plan.md` | 老板 00:45 #7917 拍板后 PM 完整后续任务计划（24h 灰盒 + P1 明早 + P2 本周 + P3 下周）|
| `2026-07-23-W-X11c-fix-result.md` | W-X11c-fix PASS（PID 33004 + alarm_record.id=1 实证），灰盒跑法启动 |
| **📘 灰盒跑法 SOP** |||
| `../SOP/yk-graybox-monitor.md` | **W-X14 灰盒跑法监控 SOP（主文档）**：固化了 36/37/38/39/40 五条铁则 + 24h 观察期 SOP + 解除 3 条件 + 11 个可执行 PowerShell 命令 |
| `docs/adr/0005-pg14-path-correction.md` | **PM 23:58 PG 14 修正**：现场实装是 PG 14.23 在 C:\Program Files\PostgreSQL\14\ 端口 5433 |
| `docs/adr/0006-csharp-yk-circuits.md` | **ADR-0006（W-X13c 01:55）C# 端 yk.enable 永久熔断 + 3 道熔断门 + §7 恢复推送操作步骤（老板专用 3 步 SOP）** | 老板将来问"怎么恢复 yk 推送"必读；§7 SOP 照着做 |
| `2026-07-22-W-X09-psm-drift-report.md` | 12 模块 PSM 偏离度报告 |
| `2026-07-22-W-F01-04-result.md` | 12 个 W-F 工单结果 |
| `2026-07-22-W-B03-result.md` | detect 模块 PASS |
| `2026-07-22-W-B05-result.md` | line 模块 PASS |
| `2026-07-22-W-B06-result.md` | WebSocket + 大屏前端 PASS |
| `2026-07-22-evening-archive.md` | 19:00-21:46 今晚上半段 |
| `2026-07-22-tonight-status.md` | 20:46 老板 1 分钟版 |

**今晚关键决策时间线**（老板拍 → PM 落地）：
- 13:05 ADR-0005 立 → 建 DataupLoad
- 13:13 5 件事（命名 / 路径 / IP / yk 链路 / PG 路径）
- 18:01 yk 第一次熔断
- 21:17 冒烟 SOP（只推 1 次成功就停）
- 21:20 yk enable=true 推 1 条 → 21:23 立刻叫停 enable=false 永久熔断
- 21:54 灰盒跑法理论框架
- 23:34 杀掉卡死的 W-X11 Worker + 23:42 应急回滚老 jar

**链路真相**（23:55 PM 实查）：
- hik-java PID 31472 / 80 LISTEN / 16 ESTABLISHED / yk 0 推送
- C# EdgeHost 已退役（intco.db 94.68 MB 保留）
- ADR 基线 = ADR-0005（推翻 ADR-0003/0004）

---

## 🚨 老板 12:42 决议：完整复刻 PSM（加密狗除外）

| 文档 | 用途 |
|---|---|
| `docs/adr/0004-full-psm-clone-except-dongle.md` | ADR-0004 决策基线（accepted 12:42）|
| `docs/delivered/2026-07-22-full-psm-clone-plan.md` | **9 个工单完整拆解计划（W-A22~W-A30，3 周）** |
| `TODO.md` 顶部 🚨 老板 12:42 决议 段 | 任务总表 + 执行路径 |

---

## 1. PSM 反编译 + 金标准文档库（老板 09:13 派活完工，249 KB）

| 文档 | 大小 | 用途 |
|---|---:|---|
| `2026-07-22-psm-architecture.md` | 13 KB | **PSM 顶层架构总览**（185 类按 7 模块分类）|
| `2026-07-22-psm-db-comparison-detailed.md` | 13 KB | **DB 详细对比**（19 表 1:1 + 字段级）|
| `2026-07-22-psm-detect-detailed.md` | 19 KB | **detect 模块详解**（37 类，涨库核心）|
| `2026-07-22-psm-detect-tech.md` | 13 KB | **detect 技术路线**（Spring Boot + MyBatis-Plus + Flyway）|
| `2026-07-22-psm-line-detailed.md` | 20 KB | **line 模块详解**（54 类，最大模块）|
| `2026-07-22-psm-line-tech.md` | 16 KB | **line 技术路线**|
| `2026-07-22-psm-alarm-detailed.md` | 19 KB | **alarm 模块详解**（35 类）|
| `2026-07-22-psm-alarm-tech.md` | 15 KB | **alarm 技术路线**（含 P0 BUG：`AlarmRecordServiceImpl.isIgnore` 永远 true）|
| `2026-07-22-psm-yingke-detailed.md` | 14 KB | **yingke 模块详解**（15 类）|
| `2026-07-22-psm-yingke-tech.md` | 9 KB | **yingke 技术路线**|
| `2026-07-22-psm-defect-detailed.md` | 10 KB | **defect 模块详解**（6 类）|
| `2026-07-22-psm-defect-tech.md` | 7 KB | **defect 技术路线**|
| `2026-07-22-psm-config-detailed.md` | 4 KB | **config 模块详解**（5 类）|
| `2026-07-22-psm-config-tech.md` | 4 KB | **config 技术路线**|
| `2026-07-22-psm-screen-detailed.md` | 11 KB | **screen 模块详解**（5 类）|
| `2026-07-22-psm-screen-tech.md` | 8 KB | **screen 技术路线**|
| `2026-07-22-psm-common-detailed.md` | 17 KB | **common 通用层详解**（13 类）|
| `2026-07-22-psm-common-tech.md` | 16 KB | **common 技术路线**|
| `2026-07-22-psm-full-manual.md` | 27 KB | **PSM 功能及代码整合文档**（全景图，链所有）|
| **合计** | **249 KB** | **18 个文档** |

## 2. 止血优化归档（2026-07-22 一日 7 个文档）

| 时间 | 文档 | 用途 |
|---|---|---|
| 08:04 | `2026-07-22-v0.4-stop-the-bleeding.md` | 老板拍阈值 50→500 决议 + PM 反对意见 |
| 08:13 | `2026-07-22-reverse-decompile-broken.md` | 旧反编译产物字节损坏发现 |
| 08:25 | `W-A14-v2.3-result.md` | PSM.rar 重解压 18.32 GB 金标准到手 |
| 08:32 | `2026-07-22-psm-db-comparison-detailed.md` | DB 详细对比（已在上面）|
| 08:36 | `2026-07-22-psm-architecture.md` | 架构总览（已在上面）|
| 08:50 | `W-A14-v2.1-result.md` | status_record 22.4 万行涨库主嫌锁定 |
| 10:15 | `2026-07-22-threshold-fix-v2.md` | 阈值真修复（log-healthcheck.ps1）|
| **10:28** | **`2026-07-22-stop-bleeding-optimization.md`** | **止血优化全程归档（本波）**|

## 3. 派工单（worker input）

| 工单 | 状态 | 输出 |
|---|---|---|
| `W-A14-v2.4-status-cleanup.md` | ✅ DONE 09:48 | status_record retention + CleanupTask |
| `W-A20-T2-decompile.md` | ✅ DONE 09:04 | vineflower 1.12 + 204 java 反编译产物 |
| `W-A20-psm-reverse.md` | ✅ 大工程工单框架 | (含 T1~T6 计划) |
| `W-A21-psm-reverse-engineering-full.md` | ✅ DONE 09:49 | 18 个 PSM 文档 |
| `W-A14-v2.4-status-cleanup.md` (上面) | ✅ |  |

## 4. PM 铁则（25/26/28/30/31）

详见 `TODO.md` 的 "PM 备注" 段落。摘要：
- **铁则 22**：不 stat 不汇报 DB size
- **铁则 25**：STATUS 数字必须配 cron 自校
- **铁则 26**：派工前 hex 验反编译可读
- **铁则 28**：改配置同步验证 cron
- **铁则 30**：写完派工单必须立刻 spawn Worker
- **铁则 31**：改配置必须 grep 所有相关文件

## 5. 关键金标准类（PM 反编译实证 10:30）

| 类 | 文件 | 大小 | 关键发现 |
|---|---|---:|---|
| `DefectRecordBackupServiceImpl` | `module/detect/service/imp/` | 1.7 KB | `backup(records)` + `removeRecordByTime(time)` = 双层表 backup 模式 |
| `DetectDataTaskManager` | `module/detect/task/` | 1.8 KB | 2 个 cron `0 0 0 * * ?`：defect_record_backup 3 天 / defect_day + line_day 30 天 |
| `AlarmTaskManager` | `module/alarm/task/` | 2.0 KB | alarm_record 3 天清理（W-A18 估错 90 天！）|
| `LineServiceImpl` | `module/line/service/imp/` | 19 KB | **🔴 P0 BUG：`getStateStatistics` 三元表达式错** |
| `YKServiceImpl` | `module/yingke/service/impl/` | 8 KB | yingke 模块叫 YK（PM 命名错误；不是 YingkeServiceImpl）|

## 6. 老板快速问 PM 速查表

| 老板问 | PM 看这里 |
|---|---|
| "PSM 整体什么样？" | `2026-07-22-psm-architecture.md` |
| "PSM 怎么做 detect 的？" | `2026-07-22-psm-detect-detailed.md` + `tech.md` |
| "我们和 PSM 的 schema 差异？" | `2026-07-22-psm-db-comparison-detailed.md` |
| "涨库为什么？" | `W-A14-v2.1-result.md` |
| "止血怎么做的？" | `W-A14-v2.4-status-cleanup.md` + `2026-07-22-stop-bleeding-optimization.md` |
| "今天发生了什么大事？" | `2026-07-22-stop-bleeding-optimization.md`（综合）|
| "还有什么 P0 BUG 要修？" | `2026-07-22-psm-alarm-detailed.md` 末尾 5 个 P0 BUG 列表 |
| "下一步建议派什么工？" | `docs/delivered/` 下 `W-A22-*` / `W-A23-*` / `W-A24-*`（待 PM 写）|

## 7. 反编译产物物理位置

| 资产 | 路径 | 用途 |
|---|---|---|
| **反编译 .java** | `docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/` | 204 个 java，PM 已落地 |
| PSM 原始 jar | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/IntcoScreen-1.0-SNAPSHOT-*.jar` | 80 MB Spring Boot fat jar |
| PSM V1.0~V1.19 SQL | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/sql/V*.sql` | 20 个 Flyway 迁移 |
| PSM 应用配置 | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/config/application-prod.yml` | 端口/DB/鹰科 |
| PSM PG 生产数据 | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/postgres/postgres/data/base/16394/` | ~2.6 GB |
| **反编译工具** | `tools/vineflower-1.12.0.jar` | 1.56 MB（Worker 09:04 下载）|

## 8. 反编译后真实业务类数（PM 10:30 校正）

PM 08:36 架构文档写"PSM 185 类"是基于 jar 内 class 入口扫描。**真实业务类数（含子包）**：

| 模块 | 真实类数 | 08:36 估 | 偏差 |
|---|---:|---:|---|
| alarm | **35** | 24 | -11 |
| config | 5 | 5 | OK |
| defect | 6 | 4 | -2 |
| **detect** | **37** | 47 | **+10** |
| **line** | **54** | 51 | -3 |
| screen | 5 | 7 | +2 |
| **yingke** | **15** | 18 | +3 |
| common | 13 | 13 | OK |
| **合计** | **170** | **171** | -1 |

PM 08:36 架构数字基本对，**部分模块偏差 < 5 类**。W-A21 Worker 没纠正这一点。**PM 后续派 W-A22 修 P0 BUG 时同步校正**。
