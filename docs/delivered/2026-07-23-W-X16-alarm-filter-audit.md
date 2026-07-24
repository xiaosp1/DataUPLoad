# W-X16 报警筛选 / 过滤 功能审计报告

| 项 | 值 |
|---|---|
| 工单号 | W-X16 |
| 工单类型 | 审计（不改代码 / 不改 yml / 不重启） |
| 优先级 | 🟡 P1 |
| 派工人 | PM 锋卫 |
| 执行人 | Codex Worker (codex exec) |
| 执行时间 | 2026-07-23 14:24 ~ 14:50 GMT+8 |
| 老板口径 | "看一下驱虫以及忽略的操作，等其他操作吧，有没有去筛选报警？" |

---

## 0. 一句话回答老板

> **"**有，已经做了 8 大类筛选 / 过滤**。其中最关键的两条 —— 同一报警防爆（同类去重淹旧）+ 忽略白名单（isIgnore 命中不推 yk）—— DataupLoad 不仅 1:1 抄了 PSM，还把 PSM 反编译里的 `boolean isIgnore = false` 硬编码 BUG 给修了（详见 AlarmRecordServiceImpl.java:192-194）。"**

不过对比 PSM 反编译源码，**老板可能关心的几类筛选还没有做**，详见 §3 缺失能力章节。

---

## 1. 审计范围

| 模块 | 文件数 | 状态 |
|---|---|---|
| `module/alarm/**` | 31 个 java | 全读 |
| `module/detect/**` | 17 个 java | 关键 2 个 |
| `module/yingke/**` | 12 个 java | 关键 1 个 |
| `module/defect/service/impl/LineDefectTypeServiceImpl.java` | 1 | 全读（关联 showFlag 过滤） |
| `application.yml` + `application-prod.yml` | 2 | 全读 |
| `sql/V*.sql` | 19 | 抽查 alarm 相关 |
| **PSM 反编译对比** | `psm-decompiled\BOOT-INF\classes\com\hikrobotics\solution\module\alarm\*` + `module\yingke\*` + `module\detect\StatusRecordServiceImpl` | 全读 |

---

## 2. 已实现的 8 大类筛选 / 过滤（按业务顺序）

### 2.1 报警类型筛选（type 白名单）

| 项 | 内容 |
|---|---|
| 实现位置 | `DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\constant\AlarmTypeEnum.java` |
| 关键行 | L9-L12：`DEFECT(1)` / `SYSTEM(2)` / `DEVICE(3)` |
| 关键行 | `DataupLoad\...\module\alarm\service\impl\AlarmRecordServiceImpl.java:125-128` —— `AlarmTypeEnum.getByCode(form.getType())`，null 直接 `error("20101")` 拒收 |
| 作用 | 入参 `type` 必须在 1/2/3 之内，否则不入库、不推 yk |
| DTO 校验 | `AlarmDTO.java:21-23` —— `@Range(min=1, max=3)`，Controller `@Validated` 拦截 |

### 2.2 缺陷名匹配（defectName 白名单 + 模板提取）

| 项 | 内容 |
|---|---|
| 实现位置 | `AlarmRecordServiceImpl.java:132-153` |
| 关键配置 | `module\alarm\config\DefectAlarmConfig.java` + `application-prod.yml` L26-L33（`alarm.config`） |
| 关键逻辑 | 用正则 `(?<=\[)[^]]+(?=\])` 从 message 抽出缺陷名 → 命中 `defect_type.name` 表才放行 |
| 作用 | 仅当 message 含已登记缺陷名才入库；未登记的 → "current alarm is not interesting defect" warn 日志，不入 PG |
| 关联 | `defect_type.category = type` 限定（line 137-139） |

### 2.3 同类去重 / 报警密度淹旧（防爆，PSM 设计核心）

| 项 | 内容 |
|---|---|
| 实现位置 | `AlarmRecordServiceImpl.java` add 入口的 isInterestingDefect 分支在 L155-173 —— UPDATE IGNORE 块在 L156-164，INSERT 块在 L166-171，sendAlarmMessage 在 L172 |
| 关键逻辑 | 同一 `(defectName + lineNo + faceNo + type)` 下旧的 `UNSOLVED` 记录全部 `UPDATE IGNORE`，再插新的 UNSOLVED |
| 关键行 | L162-164：`eq(...).set(AlarmRecord::getSolve, AlarmSolvedEnum.IGNORE.getValue())` |
| 作用 | 同类报警反复来 → 旧记录自动 `IGNORE`、新记录入库但不会爆库；保护 `alarm_record` 单表不膨胀 |
| 数据库索引 | `sql\V1.13__create_db.sql` —— `alarm_record_defect_name_idx ON (defect_name, solve, line_no, face_no)` 保证 update 走索引 |

### 2.4 ignore_alarm 白名单（人工 ignore）

| 项 | 内容 |
|---|---|
| 实现位置 | `module\alarm\service\impl\IgnoreAlarmServiceImpl.java` —— `isIgnore` L32-40、`removeExpire` L46-51、`getIgnoreDefect` L53-60 |
| HTTP 接口 | `module\alarm\web\IgnoreAlarmController.java` 4 个端点：POST add L42-46 / DELETE remove L48-52 / GET list L55-58 / GET check L60-66 |
| 关键逻辑 | `(type, defectName, lineNo, faceNo)` 四元组 + `ignore_time < endTime` → 命中即返回 true |
| 作用 | 命中白名单 → `AlarmRecordServiceImpl.sendAlarmMessage`（L189-213）`isIgnore=true` → 不推 yk、不发 WS 文本 |
| W-B04 修复 | DataupLoad `AlarmRecordServiceImpl.java:192-194` 实际查表；PSM 原版硬编码 `boolean isIgnore = false`，白名单永远失效 —— **DataupLoad 已修复并文档化** |
| 表结构 | `sql\V1.20__ignore_alarm.sql` —— 4 字段（type/defect_name/line_no/face_no）+ `ignore_all` + 时间窗 |

### 2.5 ignore 过期清理（定时任务）

| 项 | 内容 |
|---|---|
| 实现位置 | `module\alarm\task\IgnoreExpireTask.java` L47-58（`delExpireIgnoreDefect`） |
| cron | `0 0 * * * ?`（每小时整点）—— 比 PSM 原版 `0 0 1 * * ?`（每天 1 点）更激进 |
| 关键逻辑 | `ignoreAlarmService.count(qw)` 统计 + `removeExpire()` 删除（L50-56） |
| PSM 差异 | DataupLoad 增加了影响行数日志（"ignore expire alarm removed. count={}"），便于运维追溯 |

### 2.6 alarm_record 保留期清理（定时任务，防爆第二层）

| 项 | 内容 |
|---|---|
| 实现位置 | `module\alarm\task\AlarmRetentionTask.java` L55-66（`clearAlarmData`） |
| cron | `0 0 3 * * ?`（每天 3 点）—— 比 PSM 原版 `0 0 0 * * ?`（0 点）推迟 3 小时，避开夜间 PG 备份高峰 |
| 保留期 | 90 天（PSM 原版可配置 `${data-retention-time.alarm:3}` 即 3 天；DataupLoad 写死 90 天，W-F01-C 范围内不改 yml） |
| 关键逻辑 | `create_time < now-90days AND solve=SOLVED` → 物理删除（L60-65） |

### 2.7 yk 双开关（loginEnabled / uploadEnabled，灰盒关键）

| 项 | 内容 |
|---|---|
| 实现位置 | `module\yingke\config\YKConfig.java` L21-72 + `module\yingke\service\impl\YKServiceImpl.java` L81-152 |
| 关键字段 | `yk.loginEnabled`（拿 ticket）+ `yk.uploadEnabled`（推不推） |
| 关键行 | `YKServiceImpl.java:81` —— `if (this.ykConfig.isLoginEnabled())` 决定要不要去 MES 登录 |
| 关键行 | `YKServiceImpl.java:136` —— `if (!this.ykConfig.isUploadEnabled()) return;` 灰盒默认关 |
| 灰盒默认 | `loginEnabled=true, uploadEnabled=false`（老板 W-X13d 拍板，铁则 42） |
| 作用 | 全链路关停 yk 推送只用改一个配置项；不重启也能切（待 #W-X21 配置热刷新） |
| 老字段兼容 | `enable` 字段 `@Deprecated`，`getEnable()` 返回 `loginEnabled \|\| uploadEnabled`（L57-60） |

### 2.8 Web 客户端掉线检测（implicit filter）

| 项 | 内容 |
|---|---|
| 实现位置 | `module\alarm\service\ClientOnlineChecker.java`（W-F04） |
| 关键逻辑 | L65-90：30s 扫描一次，超过 60s 无心跳 → 走 `add()` 入口生成 type=3 设备报警 |
| 作用 | 客户端断线不再"哑火"，通过 add() 链路走完整流程（含 §2.1-§2.7 所有过滤） |

---

## 3. 缺失能力（PSM 有但 DataupLoad 没的 / 老板可能关心的）

### 3.1 [P0 · 防爆] `alarm.global-enabled` 全局开关 —— **yml 配了但 Java 端没读**

| 项 | 内容 |
|---|---|
| 老板关心点 | "如果报警洪水来了，能不能一键全停？" |
| yml 已配 | `application-prod.yml` L24-28：注释里写 `alarm.global-enabled: true`（默认开），false 时"既不落 PG 也不推 yk" |
| **Java 端状态** | **未实现** —— `grep -r "global-enabled"` 在 DataupLoad java 代码里 0 命中 |
| 工单 | **W-X21**（2026-07-23 13:23 已派工单，结果见 `2026-07-23-W-X21-global-switch-result.md`） |
| 派工优先级 | **P0**（已派工单，老板下达命令后 1 分钟内生效） |
| 老板拍板 | 等 W-X21 落地后此开关可用 |

### 3.2 [P0 · WS 声音推送] `sendAlarmSoundWsMessage` —— **缺失**

| 项 | 内容 |
|---|---|
| 老板关心点 | "报警来了大屏响不响？" |
| PSM 实现 | `PSM AlarmRecordServiceImpl.java:281-300` —— `sendAlarmSoundWsMessage(DefectTypePO)` 调 `systemConfigService.listByConfigKey([soundConfigKey, "sound_play_count"])` 拿声音 URI + 播放次数，WS broadcast |
| DataupLoad 状态 | `AlarmRecordServiceImpl.java:201-205` —— 直接 debug 日志 "defect alarm sound ws push skipped (system_config not wired)" |
| 根本原因 | DataupLoad 没有 `ISystemConfigService`（W-B05 / 框架启动依赖未引入） |
| 派工优先级 | **P0 业务 / P1 技术债**：业务上 P0（声音是大屏报警的核心反馈），技术上 P1（要先恢复 SystemConfigService） |
| 老板拍板建议 | **延后到 PSM 后台迁移工单**；声音功能可以先用前端 alert 兜底 |

### 3.3 [P1 · web 后台查询] `listAll / handleAlarmSearch / handleAlarmNumGet` —— **DTO 空壳 + 实现占位**

| 项 | 内容 |
|---|---|
| 老板关心点 | "老板能不能在大屏上按类型/级别/时间筛报警？" |
| PSM 实现 | `AlarmQueryDTO` 有 5 字段（type/level/solve/faceId/sortType） + `TimePageQuery`（startTime/endTime），`listAll` 走 MP LambdaQueryWrapper 拼 5 个 `eq()` + `between` + `orderBy`；`handleAlarmNumGet` 调 `alarmRecordDAO.selectAlarmCountByType()` 聚合 |
| DataupLoad 状态 | `AlarmQueryDTO.java` 是 **空壳**（无字段）；`IgnoreAlarmDTO.java` / `SearchAlarmDTO.java` / `AlarmInfoQueryDTO.java` 同；`AlarmRecordServiceImpl.listAll(L105-106)` / `handleAlarmNumGet(L262-264)` 直接返回 `Collections.emptyList()` 或新 `AlarmNumDTO()` |
| 关联 mapper 缺失 | `AlarmRecordMapper.java` 继承 `BaseMapper<AlarmRecord>`，**没有 `selectAlarmCountByType()` 等聚合方法**（PSM 原版 5 个自定义查询） |
| 派工优先级 | **P1**：老板目前用大屏 PG 直查兜底；真正补齐需要：① 写齐 4 个 DTO 字段 ② 补 AlarmRecordMapper.xml ③ listAll 加 MP LambdaQueryWrapper ④ 新增 AlarmCountDTO/AlarmCountOfLineDTO |
| 老板拍板建议 | **等 PSM 后台迁移时一起补**；当前不影响报警主链路 |

### 3.4 [P1 · 报警聚合] `handleAlarmIgnore` 真实现 —— **空跑**

| 项 | 内容 |
|---|---|
| 老板关心点 | "我点忽略是不是把所有同类未解决报警都标 IGNORE 了？" |
| PSM 实现 | `AlarmRecordServiceImpl.java:103-127` —— `handleAlarmIgnore(IgnoreAlarmDTO)`：`ignoreAll=1` 调 `listNotResolveDefectAlarmRecord` 取全量未解决缺陷报警；否则按 (lineNo/faceNo/type/defectName/startTime/endTime) 拼 qw → 全部 `setSolve(IGNORE)` + `updateBatchById` |
| DataupLoad 状态 | `AlarmRecordServiceImpl.handleAlarmIgnore(L299-302)` 直接 `return BaseResult.build().ok()`；`IgnoreAlarmServiceImpl.handleAlarmIgnore(L19-22)` 也只 return ok |
| 老板体感 | 在大屏点"忽略"按钮 → 前端收到 200 → 但 PG 里的 alarm_record **没变**（仍是 UNSOLVED） |
| 派工优先级 | **P1**：老板大屏操作可见性低，等 PSM web 后台迁移一起补 |
| 老板拍板建议 | **如果老板近期会密集用"忽略"按钮，立刻升级为 P0 派工** |

### 3.5 [P2 · 缺陷名 → 缺陷类型映射] `lineDefectType.showFlag` 过滤 —— **部分缺失**

| 项 | 内容 |
|---|---|
| 老板关心点 | "某些产线/工位不关心某些缺陷，能不能前端只显示关心的？" |
| PSM 实现 | `DefectRecordServiceImpl.handleRealtimeDetectDataSearch(L376-394)` —— 实时缺陷数据返回前，按 `lineDefectTypeService.listIfShowEnable(lineNo, faceNo)` 过滤 `showFlag=1` 的缺陷 |
| DataupLoad 状态 | `DefectRecordServiceImpl.handleRealtimeDetectDataSearch(L177-187)` —— **没有过滤**，直接返回全部 defect |
| 派工优先级 | **P2**：前端缺过滤 = 用户看到一堆不关心的缺陷（体验下降）；不影响 PG / yk 推送 |
| 老板拍板建议 | **等 PSM detect 模块迁移时一起补**；前端可在收到数据后客户端再过滤兜底 |

### 3.6 [P2 · 报警推送聚合] alarmCount / alarmDetails 字段填充 —— **已实现但要确认**

| 项 | 内容 |
|---|---|
| 老板关心点 | "推 yk 的报警有没有携带同类计数？" |
| 实现位置 | `YKServiceImpl.java:148-158` —— 推送前 `selectCount` 查同 (defectName+lineNo+faceNo+type+UNSOLVED) 计数，写入 `alarm.alarmCount` 和 `alarm.alarmDetails` |
| 状态 | ✅ **已实现**，与 PSM 一致 |
| 关联 yingke DTO | `module\yingke\dto\AlarmDTO.java` —— PSM 端有 alarmCount/alarmDetails 字段，DataupLoad 需确认 |

### 3.7 [P2 · 报警 reason/level 字段过滤] —— **未实现但 PSM 也未在 push 链路过滤**

| 项 | 内容 |
|---|---|
| 老板关心点 | "能不能只推 HIGH 不推 NORMAL？" |
| PSM 实现 | `AlarmRecordPO` 有 `level` (NORMAL=1/HIGH=2) + `reason` (DISCONNECT=1) 字段；`AlarmQueryDTO` 支持按 level/reason 查询；但 **`add()` 入口没有按 level/reason 过滤推送** —— 所有 UNSOLVED 都会走 sendAlarmMessage |
| DataupLoad 状态 | 同 PSM —— 字段在实体（`AlarmRecord.java:38, 42`）和表（`alarm_record.level` / `alarm_record.reason`）都在，但 `add()` 不做 level/reason 过滤 |
| 派工优先级 | **P2 业务增强**：老板如果想做"非 HIGH 不推飞书"，需要新加 `DefectType.levelFilterEnable` 字段 + add() 加分支 |
| 老板拍板建议 | **暂缓**；先用 DefectType.alarmEnable 字段（已实现）做"开关一刀切"，level 细分需求不大 |

---

## 4. 老板拍板建议（直接拿去问）

### 4.1 立刻要补的（P0）

| 编号 | 能力 | 工单 | 工期 |
|---|---|---|---|
| 1 | `alarm.global-enabled` 全局开关（yml 已配，Java 没读） | **W-X21 已派** | ≤ 2h |
| 2 | `sendAlarmSoundWsMessage` WS 声音推送（缺失，先决条件：恢复 SystemConfigService） | 待派 | 1-2d（含 PSM system_config 迁移） |

### 4.2 应该补的（P1，看老板是否密集用大屏"忽略"）

| 编号 | 能力 | 工单 | 工期 |
|---|---|---|---|
| 3 | `AlarmQueryDTO` / `IgnoreAlarmDTO` / `SearchAlarmDTO` / `AlarmInfoQueryDTO` 字段补齐 + `listAll` / `handleAlarmSearch` / `handleAlarmNumGet` 真实现 + `AlarmRecordMapper.xml` 5 个聚合查询 | 待派 | 0.5d |
| 4 | `handleAlarmIgnore` 真实现（按 7 字段 qw + updateBatchById） | 待派 | 0.5d（可与 #3 合并） |

### 4.3 建议暂缓的（P2，看后续需求）

| 编号 | 能力 | 触发条件 |
|---|---|---|
| 5 | `handleRealtimeDetectDataSearch` 加 `showFlag` 过滤 | 前端工单反馈"不关心的缺陷太多" |
| 6 | `level` / `reason` 字段细分推送过滤 | 老板明确要求"非 HIGH 不推飞书" |

### 4.4 给老板的口径建议（PM 锋卫可直接转述）

> "工单 W-X16 审计完成。报警筛选这一块 DataupLoad 沿用 PSM 设计做了 8 类（含同类去重防爆、人工 ignore 白名单、yk 双开关），还顺手把 PSM `boolean isIgnore = false` 的硬编码 BUG 给修了。
> 
> 缺失的能力主要在 3 类：
> ① **W-X21 全局开关**已派单，2 小时内上线；② **WS 声音推送**依赖 SystemConfigService，建议等 PSM 后台迁移时一起补；③ **web 后台查询/忽略操作**当前是占位返回空数据，前端点击没生效 —— 如果老板近期要密集用大屏忽略按钮，建议立刻升级派工单补齐。
> 
> 老板您看是先补 #2+ #3，还是先把 PSM 后台整体迁移完再统一补？"

---

## 5. 附录：完整过滤决策树

报警从客户端进来后，经过的过滤节点如下（每一步都可能"过滤掉"）：

```
[客户端] POST /client/data/alarm (AlarmDTO)
        ↓
[Controller @Validated] 校验 uuid/time/level/message 非空 + type ∈ [1,3]   ← §2.1
        ↓
[AlarmRecordServiceImpl.add]
        ├─ AlarmTypeEnum.getByCode(type) == null ? → error "20101"            ← §2.1
        ├─ defectTypeService.listByAttribute(type, DefectType::getCategory)
        │   └─ 空 ? → "not interesting defect" warn → 不入库                  ← §2.2
        ├─ ReUtil.get(template, message) → 抽出缺陷名
        │   └─ 命中 defect_type.name ? → isInterestingDefect = true            ← §2.2
        │   └─ 未命中 ? → "not interesting defect" warn → 不入库
        └─ isInterestingDefect ?
            ├─ 是：
            │  ├─ UPDATE alarm_record SET solve=IGNORE
            │  │  WHERE defectName+lineNo+faceNo+type AND solve=UNSOLVED       ← §2.3 防爆
            │  ├─ INSERT 新 UNSOLVED 报警                                       ← §2.3
            │  └─ sendAlarmMessage(alarm)
            │     ├─ isIgnore = ignoreAlarmService.isIgnore(...)               ← §2.4
            │     │   └─ true ? → 跳过 yk 推送 + WS 文本推送
            │     ├─ defectType.alarmEnable == YES && !isIgnore
            │     │   ├─ sendAlarmTextMessage() (WS broadcast)               ← §2.4
            │     │   └─ defectType.soundEnable == YES && solve=UNSOLVED
            │     │       └─ sendAlarmSoundWsMessage(...) [未实现, debug log] ← §3.2 ⚠️
            │     └─ !isIgnore && defectType.sendYkEnable == YES
            │         └─ EventUtil.publish(PushAlarmEvent)
            │            └─ YKServiceImpl.pushAlarm2YK (异步)
            │               ├─ uploadEnabled=false ? → 静默跳过              ← §2.7
            │               ├─ ticket==null ? → error
            │               └─ 推送前 selectCount 算同类计数
            │                  写 alarmCount / alarmDetails
            │                  调 MES HandleVisualInspectionAlarm
            └─ 否：不入库（§2.2 已 warn）

[cron 每小时整点] IgnoreExpireTask                                       ← §2.5
        └─ ignoreAlarmService.removeExpire() (ignore_time < now)

[cron 每天 3 点] AlarmRetentionTask                                       ← §2.6
        └─ DELETE alarm_record
           WHERE create_time < now-90days AND solve=SOLVED
```

---

## 6. 审计声明

- ✅ 审计覆盖 `alarm/`、`detect/`、`yingke/`、`defect/`（关联）、`application*.yml`、19 个 SQL
- ✅ PSM 反编译对比覆盖 `module/alarm/`、`module/yingke/`、`module/detect/StatusRecordServiceImpl`、`module/line/StateChangeServiceImpl`
- ❌ **未改任何代码 / yml / SQL / 重启服务**
- ✅ 报告路径：`docs/delivered/2026-07-23-W-X16-alarm-filter-audit.md`
- 📋 上游关联：W-B04（isIgnore BUG 修复）、W-F01-C（AlarmRetentionTask 落地）、W-F01-D（IgnoreExpireTask 落地）、W-F04（ClientOnlineChecker 落地）、W-X13d（yk 双开关）、W-X21（global-enabled 全局开关，已派工单）、W-X14（PSM drift 审计）

🏭 Codex Worker · 2026-07-23 14:50 GMT+8
