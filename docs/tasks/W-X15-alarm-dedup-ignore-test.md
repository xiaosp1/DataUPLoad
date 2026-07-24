# W-X15 工单 — 报警去重 / 忽略 灰盒测试（防疯狂推送）

**派工人**：Worker（codex exec）
**派单时间**：2026-07-23 13:25
**优先级**：🔴 P0（老板 13:21 拍板：终目的 = **不要让报警信息疯狂推送**）
**当前生产状态**：hik-java PID 33248 / cp 模式跑 / uploadEnabled=false / 38 相机 ESTABLISHED / 业务正常

---

## 老板原始指令（13:21）

> "现在的报警需要测试去重 忽略等工作，我终的目的是不要让报警信息疯狂推送。你现在可以派工去做测试了，看一下驱虫以及忽略的操作，等其他操作吧，有没有去筛选报警？同样enable=FALSE的时候去测试。**能多派工就不要少派啊**。"

---

## 防御链（要测的 3 道关卡）

| 关卡 | 代码位置 | 作用 |
|---|---|---|
| **第 1 关：add() 同类去重** | `AlarmRecordServiceImpl.add()` line 145-156 | 同 (defectName+lineNo+faceNo+type) 且 solve=UNSOLVED 的旧报警自动 IGNORE，然后 save 新 UNSOLVED |
| **第 2 关：sendAlarmMessage isIgnore** | `AlarmRecordServiceImpl.sendAlarmMessage()` 调 `IgnoreAlarmService.isIgnore()` | 命中 ignore_alarm 白名单 → 不推 yk + 不发 WS |
| **第 3 关：yk.uploadEnabled=false** | `YKServiceImpl.pushAlarm2YK()` | uploadEnabled=false → 静默跳过（不推 MES）|

---

## 灰盒测试项（8 项，全在 enable=FALSE 状态跑）

### T1：add() 同类去重（核心）

| 项 | 内容 |
|---|---|
| **方法** | curl POST /client/data/alarm 3 条同类报警（同 defectName+lineNo+faceNo+type） |
| **期望** | PG alarm_record：1 条 UNSOLVED + 2 条 IGNORE（同类 UNSOLVED 旧记录被淹掉） |
| **断言 SQL** | `SELECT solve, count(*) FROM alarm_record WHERE defect_name=? AND line_no=? AND face_no=? AND type=? GROUP BY solve;` 应得 1 行 UNSOLVED + 2 行 IGNORE |

### T2：不同 defectName 各自独立

| 项 | 内容 |
|---|---|
| **方法** | curl POST 5 条不同 defectName 报警 |
| **期望** | 5 条 UNSOLVED 各自独立（同 lineNo+faceNo+type 但 defectName 不同）|
| **断言 SQL** | `SELECT count(*) FROM alarm_record WHERE solve=2;` 应得 5（前面 1 条 + 5 条）|

### T3：不同 lineNo 不互相淹

| 项 | 内容 |
|---|---|
| **方法** | curl POST 同 defectName 不同 lineNo 报警 |
| **期望** | 各 lineNo 独立 UNSOLVED |

### T4：ignore_alarm 白名单命中后不推 yk

| 项 | 内容 |
|---|---|
| **方法** | 1. curl POST /web/alarm/ignore 加白名单（type + lineNo + faceNo + defectName）<br>2. curl POST /client/data/alarm 命中白名单的报警 |
| **期望** | log 不出现 `publish PushAlarmEvent` 或 YKServiceImpl 相关 ERROR；DataupLoad.log 出现 `sendAlarmMessage` 但被 isIgnore 短路 |

### T5：ignore_alarm 白名单命中后也不发 WS

| 项 | 内容 |
|---|---|
| **方法** | 同 T4，看 log 是否有 `sendAlarmTextMessage` 调用 |
| **期望** | isIgnore=true 时 `sendAlarmTextMessage()` 不被调用 |

### T6：白名单删除后恢复推送（yk 双开关仍 uploadEnabled=false）

| 项 | 内容 |
|---|---|
| **方法** | 1. DELETE /web/alarm/ignore/{id} 删除白名单<br>2. curl POST 命中解除的报警 |
| **期望** | sendAlarmMessage 跑，但 YKServiceImpl.uploadEnabled=false → 静默跳过，无 yk ERROR |

### T7：报警密度压力（防爆验证）

| 项 | 内容 |
|---|---|
| **方法** | curl POST 100 条同类报警，1min 内发完 |
| **期望** | UNSOLVED 总数 ≤ 5（防爆设计，PSM 同款）|

### T8：noise 报警（"事件误触发"等）不落 UNSOLVED

| 项 | 内容 |
|---|---|
| **方法** | curl POST message 包含 "事件误触发" 但 defectName 在白名单 |
| **期望** | 进 alarm_record 但 solve=IGNORE（PSM 同款"事件误触发，移除当次触发"）|

---

## 测试命令模板

### POST 报警（curl）

```powershell
$url = "http://127.0.0.1:80/client/data/alarm"
$body = @{
  uuid = "test-{0}"  # 随机 UUID
  time = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
  type = 1            # DEFECT
  lineNo = "line1B"
  faceNo = "B1"
  level = 1
  message = "手套破损检测 缺陷名=破洞"  # 含 defectName 触发 isInterestingDefect
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json;charset=UTF-8"
```

### 加 ignore 白名单

```powershell
$igUrl = "http://127.0.0.1:80/web/alarm/ignore"
$igBody = @{
  type = 1
  lineNo = "line1B"
  faceNo = "B1"
  defectName = "破洞"
  startTime = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
  endTime = (Get-Date).AddDays(7).ToString("yyyy-MM-dd HH:mm:ss")
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri $igUrl -Method Post -Body $igBody -ContentType "application/json;charset=UTF-8"
```

### 查 PG 验证

```powershell
$env:PGPASSWORD = "postgres"
$psql = "C:\Program Files\PostgreSQL\14\bin\psql.exe"
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT solve, count(*) FROM alarm_record WHERE defect_name='破洞' GROUP BY solve;"
```

---

## Worker 任务清单

### 必做（5 项）

| # | 任务 | 时间 |
|---|---|---|
| 1 | 写 PowerShell 测试脚本 `scripts/test-alarm-dedup.ps1`（8 项测试）| 30 min |
| 2 | 跑 T1-T3（add() 去重 3 项）| 10 min |
| 3 | 跑 T4-T6（ignore_alarm 3 项）| 15 min |
| 4 | 跑 T7-T8（压力 + noise 2 项）| 10 min |
| 5 | 写验收报告 `docs/delivered/2026-07-23-W-X15-result.md` | 20 min |

### 可选（2 项，老板同意再加）

| # | 任务 | 时间 |
|---|---|---|
| 6 | 测断连报警（DealAlarmEvent）| 30 min |
| 7 | 测 retention cron（手动触发）| 15 min |

### 必须遵守的 DoD（铁则 40）

每项测试必须给：
- 命令（含完整 curl / SQL）
- 输出片段（PG 行数 + log 行）
- 断言（PASS/FAIL + 原因）
- 不允许"大致过了"

### 不允许

- ❌ 改任何源码
- ❌ 改 application-prod.yml 的 yk 配置
- ❌ 改 uploadEnabled（保持 false）
- ❌ 重启 hik-java
- ❌ 推真实报警到 MES（yk 永久关，灰盒期不变）

---

## 上报模板

```
=== W-X15 灰盒测试结果 ===
T1 add() 去重：✅ PASS / ❌ FAIL（原因）
T2 不同 defectName：✅ / ❌
T3 不同 lineNo：✅ / ❌
T4 ignore 命中不推 yk：✅ / ❌
T5 ignore 命中不发 WS：✅ / ❌
T6 删除白名单恢复推送：✅ / ❌
T7 100 条压力：✅ / ❌
T8 noise 报警：✅ / ❌

PG alarm_record 总数：N
yk push ERROR 数：0（期望）
ignore_alarm 白名单数：M

报告路径：docs/delivered/2026-07-23-W-X15-result.md
```

---

🏭 PM 锋卫 · 2026-07-23 13:25
