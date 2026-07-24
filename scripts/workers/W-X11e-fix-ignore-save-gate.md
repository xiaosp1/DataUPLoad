# W-X11e — 修 alarm_record 入库早返 ignore 检查缺失 bug

> **任务**：在 `AlarmRecordServiceImpl.add()` 加 isIgnore() 检查，避免入库
>
> **派工时间**：PM 夜班（待派，W-X11d PASS 后派）
> **预计耗时**：15 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X11d PASS（endTime 类型修好）
>
> ---
>
> ## 🔴 根因（Worker 体检确认）

`AlarmRecordServiceImpl.add()` 不调 `isIgnore()`，只在 `sendAlarmMessage()` 调 → alarm_record 总是先入库（yk/WS 推送会被拦截但 DB 已写）。**期望行为**：被 ignore 的告警完全不入 alarm_record。

## 修复方案

在 `AlarmRecordServiceImpl.add()` 的 `if (isInterestingDefect)` 块内加 isIgnore 早返。

## DoD（3 步）

### Step 1：定位代码（3 min）
- [ ] 读 `AlarmRecordServiceImpl.java` 找到 `add()` 方法
- [ ] 找 `if (isInterestingDefect)` 块
- [ ] 找现有 `isIgnore()` 调用位置（`sendAlarmMessage` 里）

### Step 2：加 isIgnore 检查（5 min）
- [ ] 在 `add()` 的 `if (isInterestingDefect)` 块内**最前面**加：
  ```java
  if (isIgnore(alarmRecord)) {
      log.info("alarm ignored by ignore_alarm: line={} face={} type={}", alarmRecord.getLineNo(), alarmRecord.getFaceNo(), alarmRecord.getDefectType());
      return null;  // 或 return existing record
  }
  ```
- [ ] 确保 import `IgnoreAlarmService` 已存在（没有就加）

### Step 3：重打 + 验证（7 min）
- [ ] `mvn clean package -DskipTests` 出新 jar
- [ ] 杀 PID + 启新 jar + /health 200
- [ ] INSERT ignore_alarm 测试行
- [ ] POST 1 条告警 → 检查 alarm_record **不应新增**
- [ ] DELETE 测试数据
- [ ] 报告：忽略前后 alarm_record 行数对比

## 验收命令（PM 跑）
```powershell
# 1. 链路活的
Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Select-Object ProcessId
# 2. alarm_record line1B/B1 = 0
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT COUNT(*) FROM alarm_record WHERE line_no='line1B' AND face_no='B1';"
# 3. ignore_alarm = 0（铁则）
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT COUNT(*) FROM ignore_alarm;"
```

## 严禁
- ❌ 不要改 `yk.enable=false`
- ❌ 不要留测试数据
- ❌ 不要碰 application-prod.yml
- ❌ 不要把 isIgnore 之外的其他业务规则一起"加"（只加这一处）

## 报告输出
`docs/delivered/2026-07-23-W-X11e-result.md`（≥ 1.5 KB，含 add() diff + 拦截前后 alarm_record 行数对比）
