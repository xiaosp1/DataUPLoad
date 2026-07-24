# W-X11d — 修 ignore_alarm.end_time 类型不匹配 bug

> **任务**：修 `IgnoreAlarm.endTime` 字段类型（LocalDateTime → String）+ service 用字符串比较
>
> **派工时间**：PM 夜班（待派，W-X11c-fix PASS 后已归档）
> **预计耗时**：20 min
> **执行人**：Worker（PM 严盯，每 5 min 查进度）
> **依赖**：W-X11c-fix-ignore PASS ✅（已切到 PID 33004，链路活的）
>
> ---
>
> ## 🔴 根因（Worker 体检确认）

Flyway V1.20 的 `ignore_alarm.end_time` 是 `varchar(19)`，但 entity 字段是 `LocalDateTime`，MyBatis-Plus 绑定 timestamp 参数 → PG 抛 `操作符不存在: character varying > timestamp without time zone` PSQLException。

## 修复方案

- `IgnoreAlarm.endTime` 改 `String` 类型
- `IgnoreAlarm.startTime` 同样改 `String`
- service 用 `gt(formatter.format(now()))` 字符串比较
- 改 controller DTO 字段类型（如果有）

## DoD（3 步）

### Step 1：改 entity（5 min）
- [ ] `IgnoreAlarm.java`：`endTime: LocalDateTime` → `endTime: String`，同样 `startTime`
- [ ] 加 import `java.time.LocalDateTime` → `java.time.format.DateTimeFormatter`
- [ ] 删 getter/setter 的 LocalDateTime 类型（保留字段名）

### Step 2：改 service（5 min）
- [ ] `IgnoreAlarmServiceImpl.isIgnore()` 第 50 行附近的 gt(...) 调用改字符串比较
- [ ] 用 `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())` 生成当前时间字符串
- [ ] 删 service 里 LocalDateTime import

### Step 3：重打 + 验证（10 min）
- [ ] `mvn clean package -DskipTests` 出新 jar → `E:\DataupLoad-final.jar`（备份 `.bak-w-x11d-pre`）
- [ ] 用铁则 38 杀 PID 33004 + 启新 jar
- [ ] 等 30s + `curl /` → 200 OK
- [ ] 复测 ignore：INSERT varchar 格式时间 → POST 告警 → 看 SQL 不抛异常
- [ ] DELETE 测试数据
- [ ] 报告：jar 大小 + /health 200 + SQL 无 PSQLException 实证

## 验收命令（PM 跑）
```powershell
# 1. 链路活的
Get-CimInstance Win32_Process | Where-Object Name -Match 'hik-java' | Select-Object ProcessId
Get-NetTCPConnection -LocalPort 80 -State Listen
# 2. error.log 无 PSQLException
Select-String -Path 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log' -Pattern 'PSQLException' | Select-Object -Last 5
# 3. ignore_alarm 表空（铁则遵守）
$env:PGPASSWORD='***'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT COUNT(*) FROM ignore_alarm;"
```

## 严禁
- ❌ 不要改 `yk.enable=false`
- ❌ 不要碰 application-prod.yml
- ❌ 不要留测试数据
- ❌ 不要把 endTime/startTime 之外的字段一起"修"（只修这 2 个）
- ❌ 不要试图加新功能（修 bug 就修 bug）

## 报告输出
`docs/delivered/2026-07-23-W-X11d-result.md`（≥ 1.5 KB，含 entity diff + service diff + 重启 200 OK 实证）
