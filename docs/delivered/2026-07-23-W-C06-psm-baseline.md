# W-C06 — PSM 端 1 小时平均推送数（金标准对照）— 完工报告

- **任务编号**: W-C06 (2026-07-23)
- **派工人**: PM 锋卫 16:41 GMT+8（与 W-C05 同一批派单）
- **执行人**: Worker W-C06 (subagent, depth 1/1)
- **开工**: 2026-07-23 16:46
- **完工**: 2026-07-23 17:08 (~22 min)
- **铁则遵守**: ✅ 未改源码 / ✅ 未改 yml / ✅ 未重启任何进程 / ✅ 只读反编译产物 + log 备份
- **关联工单**: W-A21 (PSM 反编译全解析) / W-C05 (白名单种子已完工)
- **辅助产物**:
  - `docs\tasks\W-C06-psm-baseline-analyze.py`（PM 一键复现脚本，~8 KB）
  - `docs\tasks\W-C06-psm-baseline-result.json`（完整分析输出，~29 KB）

---

## 0. 任务总结（一句话给老板）

> **PSM 端 1 小时平均推送数 ≈ 6.24 条**（基于 26 天 / 2.61 GB 日志 / 272 个有推送活动的小时）
> 典型值（中位数）= **2 条/小时**；P90 = 15；P99 = 53；峰值 128 条/小时（事故态）
> 推送成功率 65.4%（1109 成功 / 1696 尝试）；其余 34.6% 失败均为 PSM ↔ 英科认证/网络问题，**不是业务过滤**。

---

## 1. PSM 端部署位置

### 1.1 本机扫描结论：**未运行**

| 检查项 | 结果 |
|--------|------|
| `Get-Service \| ? Name -match psm` | 无匹配 |
| `Get-NetTCPConnection -LocalPort 8080,8443,9001,9090` | 无匹配 |
| `schtasks /query /fo LIST` | 无 PSM 任务 |
| `Get-Process \| ? Name -match psm\|java\|tomcat` | 仅 `hik-java` PID 33248（**DataupLoad**，非 PSM）|
| 全盘扫 `*PSM*` / `*psm*.jar` / `*psm*.war`（C/D/E/F/）| **0 个生产 PSM 安装** |

> ⚠️ PM 工单预判正确：PSM 端**未部署在本机**。

### 1.2 现场 PSM 服务器（从反编译日志溯源）

PSM jar 启动路径在**生产服务器** `D:\hikrobotics\PSM\server\`（用户 `YK123456`），**不在本机**。
证据来自 `intco-screen\backup\2026-XX-XX\info.0.log` 启动行：

```
2026-07-17 04:53:06.681 - INFO intco-screen [main] [Application.logStarting:51]
  Starting Application v1.0-SNAPSHOT using Java 17.0.1 with PID 6684
  (D:\hikrobotics\PSM\server\IntcoScreen-1.0-SNAPSHOT-20260605135937.jar
   started by YK123456 in D:\Hikrobotics\PSM\server)
```

### 1.3 本机可用的 PSM 资产（**金标准来源**）

| 资产 | 路径 | 用途 |
|------|------|------|
| **PSM 生产日志备份** | `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\log\intco-screen\backup\2026-06-22 ~ 2026-07-17\` | **本工单金标准数据来源**（2.61 GB，26 天，93 个日志文件）|
| PSM jar | `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\IntcoScreen-1.0-SNAPSHOT-20260605135937.jar` | 反编译产物输入 |
| 反编译 class + java | `docs\domain\海康大屏逆向\psm-decompiled\`（204 个 java）+ `tmp_psm_decompile\` | 推送代码逻辑验证 |
| PSM 部署配置 | `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\config\application-prod.yml` | YK endpoint 配置（指向 `http://192.168.80.33:10031/api/dataportal/invoke`）|
| PSM SQL 迁移 | `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\backup\server\20260605221305\sql\V*.sql`（19 份）| ignore_alarm 白名单溯源 |

---

## 2. PSM 推送代码路径（反编译验证）

### 2.1 推送链路

```
HTTP POST /client/data/alarm      (intco-screen web/web 接收来自边缘客户端)
       │
       ▼
AlarmRecordServiceImpl.add()      # 过滤"interesting defect"（正则模板匹配）
       │  ├─→ if !isInterestingDefect → log.warn("current alarm is not interesting defect")
       │  │     → 7,158,264 条（26 天总计）  ← **这部分没推送**
       │  │
       │  └─→ if isInterestingDefect → save() → sendAlarmMessage(alarm)
       │                                  │
       │                                  ├─→ sendAlarmTextMessage()      (WS broadcast)
       │                                  └─→ publish(PushAlarmEvent)
       │                                          │
       │                                          ▼
       │                                  YKServiceImpl.pushAlarm2YK(@EventListener @Async)
       │                                          │
       │                                          ├─→ log.warn("success receive alarm event")
       │                                          ├─→ POST http://192.168.80.33:10031/...
       │                                          │     (body: apiType=VisualInspectionController, method=HandleVisualInspectionAlarm)
       │                                          ├─→ if !resp.success → log.error("push alarm info to yk failed")
       │                                          └─→ if ticket==null → log.error("push alarm to yk error, ticket is null")
```

> **关键观察**: `sendAlarmMessage()` 内的 `boolean isIgnore = false` 是**死代码**（从未被赋值），
> 这意味着 ignore_alarm 白名单**没被实际检查**。详情见 §3 + W-A21 §4.5。

### 2.2 推送触发点

| 触发场景 | 推送入口 | log 标记 |
|---------|---------|---------|
| 新报警（缺陷匹配）| `AlarmRecordServiceImpl.add() → sendAlarmMessage()` | `WARN ... pushAlarm2YK: success receive alarm event` |
| 已处理报警（手动 deal）| `AlarmRecordServiceImpl.deal() → sendAlarmMessage()` | 同上 |
| 客户端重连批量处理 | `dealClientAlarm() → deal()` | 同上 |

---

## 3. PSM 端默认 ignore_alarm 白名单（**与 W-C05 互验**）

### 3.1 审查结论：**PSM 没有 ignore_alarm 默认种子**

| 资产 | 与 ignore_alarm 默认值的关系 |
|------|---------------------------|
| `V1.0__create_db.sql` | CREATE TABLE alarm_record（无 ignore_alarm）|
| `V1.5__alarm_db.sql` | `ALTER TABLE alarm_record alter COLUMN message type varchar(1000)`（仅扩字段）|
| `V1.7__white_init.sql` | `INSERT INTO white_ip VALUES('*.*.*.*')` — **IP 白名单，不是 ignore_alarm** |
| **`V1.14__create_db.sql`** | `CREATE TABLE ignore_alarm(...)` + `INSERT INTO defect_type '客户端'` + `INSERT INTO system_config (3 行音频 URI)` — **ignore_alarm 表本身无 INSERT** |
| `V1.17__create_db.sql` | `CREATE UNIQUE INDEX ignore_alarm_type_idx`（仅加唯一索引，无 INSERT）|
| `IgnoreAlarmPO` / `IgnoreAlarmServiceImpl` / `IgnoreAlarmDAO` | isIgnore() 只 SELECT，handleAlarmIgnore() 是空实现占位符 |
| `2026-07-22-psm-alarm-detailed.md` §3.3 / §4.5 | 只描述字段和方法；**无默认 seed** |

**互验结论**：与 W-C05 §1.1 独立验证结果**完全一致** — PSM 端 `ignore_alarm` 表设计为空，由用户通过 `PUT /web/alarm/ignore` 动态填充。

### 3.2 派生规则（与 W-C05 §1.2 一致）

| # | 派生规则 | 触发条件 | 业务效果 |
|---|---------|---------|---------|
| ① | `defect_name='*', type=3, ignore_all=1` | 全 DEVICE 类（PSM 默认 `alarm.high-type=3`）| 全屏蔽 DEVICE |
| ② | `defect_name='客户端', type=3, ignore_all=1` | PSM 默认 defect seed（V1.14）| 屏蔽"客户端"DEVICE |
| ③ | `defect_name='PSM-DEFECT-MARKER', type=1` | type=1 (DEFECT) 占位 | 占位 |
| ④ | `defect_name='PSM-SYSTEM-MARKER', type=2` | type=2 (SYSTEM) 占位 | 占位 |

> 注：①②③④ 这 4 条**不是** PSM 文档里有的种子，是 W-C05 Worker 从 PSM 配置推导出的"语义等价"占位行；
> **PSM 真实运行时是 0 行**。

---

## 4. PSM 端 1h 平均推送数（金标准）

### 4.1 总览（26 天 / 2.61 GB 日志）

| 指标 | 值 |
|------|---|
| 扫描文件数 | 93（error/warn/info 各 31 天 × 3，约 2.61 GB）|
| 时间范围 | 2026-06-22 ~ 2026-07-17（含 2026-07-02/03/11 等部分停产日）|
| **总推送尝试 = 成功 + 失败** | **1,696** |
| └─ push_success（WARN）| 1,109（65.4%）|
| └─ push_err_ticket（ERROR "ticket is null"）| 485（28.6%）|
| └─ push_err_resp（ERROR "yk failed"）| 102（6.0%）|
| **有推送活动的小时数** | **272** |
| **平均推送 / 小时** | **6.24 条**（1,696 ÷ 272）|
| 中位数 P50 | **2** 条/小时 |
| P90 | 15 条/小时 |
| P99 | 53 条/小时 |
| 最大值 | 128 条/小时（事故态）|
| 最小值 | 1 条/小时（边界）|

### 4.2 按天分布

| 日期 | 推送尝试 | 平均/小时（当日有活动小时）|
|------|---------|----------------------------|
| 2026-06-22 | 87 | 5.80 |
| 2026-06-23 | 51 | 3.92 |
| 2026-06-24 | 44 | 4.40 |
| 2026-06-25 | 76 | 8.44 |
| **2026-06-26** | **105 + 64 err = 169** | **28.17** ⬆️ |
| **2026-06-27** | **181 + 0 err = 181** | **11.31** ⬆️ |
| 2026-06-28 | 44 | 3.14 |
| 2026-06-29 | 81 | 5.06 |
| 2026-06-30 | 40 | 4.44 |
| 2026-07-01 | 61 | 4.69 |
| 2026-07-02 | 26 | 3.71（部分停产）|
| **2026-07-03** | **82 + 76 err = 158** | **31.60** ⬆️（凌晨 ticket 失效）|
| 2026-07-04 | 96 | 6.40 |
| 2026-07-05 | 126 | 7.41 |
| 2026-07-06 | 9 + 52 err = 61 | 5.55（PSM ↔ YK 网络异常）|
| 2026-07-07 | 0 + 34 err | 3.09（全失败）|
| 2026-07-08 | 0 + 43 err | 3.07（全失败）|
| 2026-07-09 | 0 + 108 err | 6.35（全失败）|
| 2026-07-10 | 0 + 97 err | 4.62（全失败）|
| 2026-07-11 | 0 + 27 err | 2.08（全失败）|
| 2026-07-12 | 0 + 10 err | 2.00（全失败）|
| 2026-07-13 | 0 + 46 err | 11.50（全失败）|
| 2026-07-14 | 0 + 4 err | 2.00（全失败）|
| 2026-07-15 | 0 + 6 err | 2.00（全失败）|
| 2026-07-16 | 0 + 1 err | 1.00（全失败）|
| 2026-07-17 | 0 + 19 err | 3.80（全失败）|

### 4.3 关键观察

1. **健康期（2026-06-22 ~ 2026-07-01）**：每天 40-100 条成功推送，平均 4-9 条/小时。
2. **断网/认证异常期（2026-07-06 ~ 2026-07-17，连续 12 天）**：所有 push 失败（ticket=null），
   平均 2-12 条/小时（全为 ERROR）。PSM ↔ `192.168.80.33:10031` 英科 endpoint 不通或 ticket 续签失败。
3. **事故态峰值**：2026-06-27 单日 181 成功 + 6 月 26 日 105 成功 + 7 月 5 日 126 成功
   → 当日单小时峰值可达 128 条（短时缺陷爆发）。

### 4.4 业务推送量 vs 总报警量

| 类别 | 26 天总计 | 平均/天 |
|------|----------|--------|
| **推送尝试（business）** | 1,696 | 65 |
| **业务推送成功** | 1,109 | 43 |
| `alarm_not_interesting` 过滤掉 | **7,158,264** | **275,318** |
| **总报警量** | ~7,159,960 | ~275,383 |

> **关键比率**：PSM 业务推送 / 总报警 = **0.024%**（约 1:4220）。
> 这就是为什么 PSM 1h 平均推送数这么低 —— 99.976% 的报警在 `AlarmRecordServiceImpl.add()` 的
> `isInterestingDefect` 正则模板过滤阶段就被丢弃，**不会进入 sendAlarmMessage**。

### 4.5 反编译验证：`isInterestingDefect` 过滤逻辑

```java
// AlarmRecordServiceImpl.add() (反编译自 AlarmRecordServiceImpl.java)
for (DefectAlarmConfig.DefectTypeConfig config : this.alarmConfig.getConfig()) {
    if (config.getType().toUpperCase().equals(alarmType.name())) {
        message = ReUtil.get(config.getTemplate(), form.getMessage(), 0);  // 正则提取缺陷名
        for (String name : sortDefectTypeByName.keySet()) {
            if (message.contains(name)) {
                defectName = name;
                if (alarmType == AlarmTypeEnum.DEFECT) {
                    message = StrUtil.format("[{}] 缺陷报警", defectName);
                }
                isInterestingDefect = true;
                break;
            }
        }
    }
}
```

`alarm.config` (来自 `application-prod.yml`):
```yaml
alarm:
  config:
  - type: defect
    template: (?<=\[)[^]]+(?=\])    # 提取方括号内的缺陷名
  - type: system
    template: ^([^。]*)              # 提取第一个句号前
  - type: device
    template: ^([^。]*)
```

**业务侧结论**：
- 推送量低 ≠ 系统空闲，而是**正则模板严格过滤**（仅匹配 `[缺陷名]` 格式或句号前导文本）
- 监控推送数 P50=2, P90=15 是 DataupLoad 重构后的金标准对照线
- DataupLoad 重构后，1h 推送数应该落在 **2 ~ 15 条之间**（P50 ~ P90）为正常范围
- 超过 50 条/小时 → 进入 P99 警戒线（事故态）
- 超过 100 条/小时 → 接近 PSM 历史最大 128 条/小时（2026-06-27 测得）

---

## 5. 调研方法（PM 可复现）

### 5.1 一键复现脚本

脚本已落档到 `docs\tasks\W-C06-psm-baseline-analyze.py`（不需要 PSM 现场访问），纯本机跑：

```bash
Set-Location "E:\DEMO\数据采集"
python docs\tasks\W-C06-psm-baseline-analyze.py > docs\tasks\W-C06-psm-baseline-result.json 2>&1
# 等待 ~5 分钟（扫描 2.61 GB 日志）
Get-Content docs\tasks\W-C06-psm-baseline-result.json
```

脚本会扫描 `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\log\intco-screen\backup\YYYY-MM-DD\*.log`
所有 `error.N.log` / `warn.N.log` / `info.N.log` 文件，按小时分桶输出：

```json
{
  "totals": { "push_success": 1109, "push_err_ticket": 485, "push_err_resp": 102 },
  "push_total": 1696,
  "total_hours_with_push_activity": 272,
  "avg_push_per_active_hour": 6.24,
  "median_p50": 2,
  "p90": 15,
  "p99": 53,
  ...
}
```

### 5.2 关键 log 正则（自验证用）

| 信号 | 正则 | log level |
|------|------|-----------|
| 推送成功 | `\[YKServiceImpl\.pushAlarm2YK:\d+\] success receive alarm event` | WARN |
| 推送失败-ticket | `\[YKServiceImpl\.pushAlarm2YK:\d+\] push alarm to yk error,ticket is null` | ERROR |
| 推送失败-YK 拒绝 | `\[YKServiceImpl\.pushAlarm2YK:\d+\] push alarm info to yk failed` | ERROR |
| 过滤掉（非推送）| `\[AlarmRecordServiceImpl\.add:\d+\] current alarm is not interesting defect` | WARN |
| YK 认证成功 | `\[YKServiceImpl\.updateTicket:\d+\] success to get ticket from yk` | INFO |
| YK 认证失败 | `\[YKServiceImpl\.updateTicket:\d+\] get ticket from yk system failed` | ERROR |

> **注意**：PSM 日志含 ANSI 转义码（`[7m...[0m`），用 `re.sub(r'\x1b\[[0-9;]*[mK]|\[[0-9]+m', '', line)` 先剥离再正则匹配。

### 5.3 反编译产物手动验证

```bash
# PSM 推送代码入口
Get-Content "E:\DEMO\数据采集\docs\domain\海康大屏逆向\psm-decompiled\BOOT-INF\classes\com\hikrobotics\solution\module\yingke\service\impl\YKServiceImpl.java"

# ignore_alarm 默认种子验证（应该为空）
Get-Content "E:\DEMO\数据采集\docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\backup\server\20260605221305\sql\V1.14__create_db.sql"
# 应只看到 CREATE TABLE ignore_alarm + INSERT INTO defect_type('客户端') + INSERT INTO system_config(...)
# 不应有 INSERT INTO ignore_alarm
```

---

## 6. 给 PM 的关键结论

### 6.1 一句话金标准（老板要的）

> **PSM 端 1 小时平均推送数 = 6.24 条**（P50=2, P90=15, P99=53, max=128）
> 老板原话"我最终就要一个一小时推送几个" → 答案是 **健康态 5~10 条/小时，事故态可达 50~130 条/小时**。

### 6.2 DataupLoad 重构对照线（建议阈值）

| 状态 | 阈值（推送/小时）| 触发动作 |
|------|------------------|----------|
| 静默 | < 1 | 检查 hik-java 是否运行；查 YK endpoint 是否可达 |
| **正常** | **1 ~ 15（P50~P90）** | ✅ 绿区 |
| 繁忙 | 15 ~ 53（P90~P99）| 黄色：检查 defect_type 配置是否漏配 |
| 事故 | 53 ~ 128（P99~max）| 红色：检查生产线异常 |
| 极端 | > 128 | 紫色：PSM 历史未见；DataupLoad 需立即熔断 |

### 6.3 ignore_alarm 互验结论

**W-C05 + W-C06 双重独立验证**：PSM 端 `ignore_alarm` 表在迁移层（V1.14）和运行时均为**空表**，
所有 4 条 INSERT（id=38-41）都是 Worker 派生的"语义等价"行，**不是 PSM 真实种子**。

> 如果 PM/老板决定"PSM 都没有默认白名单，那 DataupLoad 也不应该有"，
> 撤销 W-C05 的 4 条 INSERT 即可（SQL 见 W-C05 §3.1）。
>
> 如果决定保留，建议**标记这 4 条为"派生"而非"PSM 真实种子"**，避免误导。

### 6.4 推送失败根因（建议后续工单）

26 天中 587 次推送失败全部是 `push alarm to yk error, ticket is null` 或 `push alarm info to yk failed`，
**不是业务过滤**。可能原因：

1. PSM ↔ `192.168.80.33:10031` 网络不通（防火墙/NAT）
2. YK ticket 续签失败（`updateTicket` 每 50 分钟一次，2026-07-06 后完全失败）
3. YK 服务端拒收（resp.success=false）

**建议工单**：W-C06a — 排查 PSM ↔ YK endpoint `192.168.80.33:10031` 断网根因（PM 可派工）

---

## 7. DoD 验收

| DoD | 状态 | 证据 |
|-----|------|------|
| PSM 端部署位置（找到/未找到）| ✅ | §1.1 本机扫描无；§1.2 现场 `D:\hikrobotics\PSM\server`（从 log 启动行溯源）|
| PSM 推送日志（金标准来源）| ✅ | §1.3 `docs\domain\海康大屏逆向\10-反编译产物-NEW\PSM\server\log\intco-screen\backup\2026-06-22~07-17\`（2.61 GB）|
| PSM 默认 ignore_alarm 白名单 | ✅ | §3 + 与 W-C05 互验一致（**PSM 无默认种子**）|
| PSM 端 1h 平均推送数（金标准）| ✅ | §4.1 **6.24 条/小时**（272 个有推送活动小时）|
| 调研方法（PM 可复现）| ✅ | §5 脚本 `tmp\W-C06-analyze.py` + 正则 + 反编译路径 |
| 未改源码 | ✅ | 0 个 `.cs` 文件被改 |
| 未改 yml | ✅ | 0 个 `.yml` 文件被改 |
| 未重启进程 | ✅ | 未调用任何 taskkill / mrun / Stop-Service |

---

## 8. 后续工单衔接

1. **W-C06a（建议）**：排查 PSM ↔ `192.168.80.33:10031` 英科 endpoint 12 天断网根因
   （2026-07-06 起所有 push 失败）
2. **W-C06b（建议）**：DataupLoad 重构后，对照 §6.2 阈值表做 7 天回归测试
3. **W-A21 衔接**：PSM 反编译全解析已落档，本工单 §2 / §3 引用其结论
4. **W-C05 衔接**：ignore_alarm 互验一致；建议 PM/老板拍板 §6.3 的撤销/保留决策

---

**完工签名**: Worker W-C06 — 2026-07-23 17:08 GMT+8
**回执**: PM 锋卫请回复"收到 W-C06" + 拍板 §6.3 + §6.4 工单安排
