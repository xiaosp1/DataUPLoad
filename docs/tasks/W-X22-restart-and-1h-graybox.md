# W-X22 — 重启 hik-java 加载 W-X17a/W-X15a/W-X15b 三轮 BUG fix + 1h 灰盒实测（🟡 P1）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 16:45
**优先级**：🟡 P1（老板 16:41 指令："D+B 一块跑... 依旧 enable=false，不要真的推到 mes... 我最终就要一个一小时推送几个"）

---

## 🎯 任务目标

**重启 hik-java 加载 3 轮 BUG fix → 跑 1h 灰盒 → 给老板一个"1 小时推送几次"的真实数字。**

老板指令硬约束：
- ✅ **yk.uploadEnabled 维持 false**（不要真的推到 MES）
- ✅ **alarm.global-enabled 维持 true**（让报警正常落 PG 走全链路）
- ❌ 不能改 yml
- ❌ 不能改代码

---

## 📋 任务清单

### 1. 等 W-C05 完成（前置依赖）

W-C05 在抄 PSM 白名单到 DataupLoad 的 ignore_alarm 表。

- 读 `docs/delivered/2026-07-23-W-C05-psm-whitelist-seed-result.md`
- 验证 ignore_alarm 至少有 3 条 PSM 默认白名单

### 2. 重启 hik-java 加载新 class

当前状态：
- hik-java PID 33248 / cp 模式 / 启动 08:34:44 / alive 8h+
- target\classes 已含 W-X17a (14:52) + W-X15a (15:27) + W-X15b (15:32) 三轮 fix

**步骤**：

```powershell
# 停 PID 33248（铁则 44：要有回滚路径，先确认 cp 启动脚本）
# 项目标准启动：scripts\start-app.bat（cp 模式，lib\*;target\classes）
# 回滚路径 = 同一个脚本可以启动新进程加载 target\classes

taskkill /F /PID 33248

# 等 5s 让端口释放
Start-Sleep -Seconds 5

# 用项目标准 cp 模式启动（scripts\start-app.bat 或 hik-java.exe -cp "DataupLoad\lib\*;DataupLoad\target\classes"）
cd E:\DEMO\数据采集
# 启动命令示例（参考 scripts\start-app.bat）：
# hik-java.exe -cp "DataupLoad\lib\*;DataupLoad\target\classes" > logs\dataupload.out.log 2>&1

# 等 30s 让 ESTABLISHED 重连
Start-Sleep -Seconds 30
```

**启动后验证 4 项**：
1. hik-java 进程存活（Get-Process hik-java）
2. 38 相机 ESTABLISHED（Get-NetTCPConnection -State Established | Measure-Object）
3. yk ticket 拿到（DataupLoad.log `success to get ticket from yk`）
4. alarm.global-enabled=true（yml 确认）

### 3. 跑 1h 灰盒（不真推 yk）

**快照点**：0min / 15min / 30min / 45min / 60min（共 5 次）

**每次快照收集**：

| 指标 | 来源 |
|---|---|
| receive alarm 计数（最近 5min delta）| `DataupLoad.log` `receive alarm` 计数 |
| not interesting defect 计数（最近 5min delta）| `DataupLoad.log` `not interesting defect` 计数 |
| isIgnore 命中数（最近 5min delta）| `DataupLoad.log` `isIgnore` 关键字 |
| yk push 实际调用数（最近 5min delta）| `DataupLoad.log` `pushAlarm2YK` / `yk.*push` 关键字 |
| BadSqlGrammarException 数（最近 5min delta）| `error.log` 关键字 |
| alarm_record 入库 delta | `SELECT count(*) FROM alarm_record` |
| ignore_alarm 命中数 | DataupLoad.log 中 `isIgnore.*true` 或忽略命中 |

### 4. 写报告 `docs/delivered/2026-07-23-W-X22-restart-1h-graybox-result.md`

必须包含：
- 重启时间戳（start / stop / new PID / ESTABLISHED 数量恢复时长）
- 1h 内 5 次快照表
- **老板要的最终数字：1h 平均推送 yk 次数 = X 次**
- 如果 X > 0，分析哪些走了全链路没被过滤（哪种 defectName / line / face）
- 如果 X = 0（最理想），说明 3 道关卡（模板 + 白名单 + uploadEnabled）全生效

### 5. 严守红线

- ❌ 不能改 yk.uploadEnabled / loginEnabled / alarm.global-enabled
- ❌ 不能改任何业务代码
- ❌ 不能动 ignore_alarm 数据
- ❌ 不能动 PSM 端
- ✅ 可以重启 hik-java（PM 已授权）
- ✅ 可以跑 1h 灰盒

### 6. 回滚预案

如果 hik-java 重启后 30s 内：
- 38 相机 ESTABLISHED < 30 → 立即查 netstat 看哪里卡
- yk ticket 拿不到 → 立即停进程，回滚到原 cp 模式启动命令

**禁止**：长时间拖延排查——超过 5min 没起来就回滚，PM 再派 W-X22b 排查。

---

## 🚫 严禁

- ❌ 改 yml / 改代码 / 删数据 / 改 PSM
- ❌ 把 yk.uploadEnabled 改成 true（**老板硬约束**）
- ❌ 把 alarm.global-enabled 改成 false（**老板没说改，且 PM 不能擅自关**）

---

## 🎯 PM 验收标准

1. ✅ W-C05 白名单已抄完
2. ✅ hik-java 重启成功，新 PID alive
3. ✅ 38 相机 ESTABLISHED 在 60s 内恢复
4. ✅ yk ticket 拿到
5. ✅ 1h 跑完 5 次快照
6. ✅ 老板要的"1h 推送数"具体数字 + 完整证据链
7. ✅ 报告含 `docs/delivered/2026-07-23-W-X22-restart-1h-graybox-result.md`

完成后在群内回复 + 报告路径 + 老板要的数字。
