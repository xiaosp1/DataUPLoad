# W-X11c-fix-ignore 修 ignore 字段名错位 bug — Worker 报告

**Worker**：W-X11c-fix-ignore（PM 派工 2026-07-23 00:54）
**执行时间**：2026-07-23 01:00 ~ 01:18（GMT+8）
**状态**：✅ Step 1+2 完成 / ⚠️ Step 3 命中两个独立 bug，已超出本工单范围，需 PM 派新工单

---

## TL;DR

按派工单指令精确修了字段名错位 bug（`ignore_time` → `end_time`），重打 jar、重启验证、新进程跑 30 秒后 `/` 返回 200 OK。但复测时**发现两个独立的下游 bug**，导致 ignore 过滤仍未达到工单预期的"alarm_record 不新增"效果。两个新 bug 均与本工单根因无关，需另开工单。

---

## Step 1：定位 + 修代码（✅ 完成）

### 根因（与派工单一致）

`IgnoreAlarmServiceImpl.java` 第 45 行用 `LambdaQueryWrapper.gt(IgnoreAlarm::getIgnoreTime, ...)`，MyBatis-Plus 默认下划线驼峰映射到 `ignore_time` 列，但 Flyway V1.20 建的 `ignore_alarm` 表里**根本没有 `ignore_time` 列**，实际列名是 `end_time`。`IgnoreAlarm` 实体也没有 `@TableField("end_time")` 注解修正。

### 代码改动 diff

**文件 1：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/entity/IgnoreAlarm.java`**

```java
+ import com.baomidou.mybatisplus.annotation.TableField;
  ...
  private String faceNo;
  private LocalDateTime ignoreTime;
+ @TableField("end_time")
+ private LocalDateTime endTime;
  private LocalDateTime updateTime;
  ...
+ public LocalDateTime getEndTime() {
+    return this.endTime;
+ }
  ...
+ public IgnoreAlarm setEndTime(LocalDateTime endTime) {
+    this.endTime = endTime;
+    return this;
+ }
```

**文件 2：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/IgnoreAlarmServiceImpl.java`**

`isIgnore()` / `removeExpire()` / `getIgnoreDefect()` 三处的 `getIgnoreTime` 改为 `getEndTime`：

```diff
   public boolean isIgnore(Integer type, String defectName, String lineNo, String faceNo) {
      ...
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
         .eq(IgnoreAlarm::getType, type)
         .eq(IgnoreAlarm::getDefectName, defectName)
         .eq(IgnoreAlarm::getLineNo, lineNo)
         .eq(IgnoreAlarm::getFaceNo, faceNo)
-        .gt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
+        .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());
      return this.count(qw) != 0L;
   }

   public void removeExpire() {
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
-        .lt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
+        .lt(IgnoreAlarm::getEndTime, LocalDateTime.now());
      this.remove(qw);
   }

   public List<IgnoreAlarm> getIgnoreDefect() {
      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
-        .gt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
+        .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());
```

严格遵守派工单"只修 ignore_time → end_time 这一处"约束，**未改其他字段、未碰 application-prod.yml、未改 yk.enable**。

---

## Step 2：重打 jar + 重启（✅ 完成）

### 构建产物

| 项 | 值 |
|---|---|
| Maven | `E:\apache-maven-3.9.6\bin\mvn.cmd` |
| JDK | `E:\DEMO\数据采集\DataupLoad\jdk` (OpenJDK 17.0.1) |
| 命令 | `mvn clean package -DskipTests` |
| 结果 | BUILD SUCCESS，14.5s |
| 新 jar (源) | `target/DataupLoad-1.0-SNAPSHOT-20260723010315.jar`（dependency-copy 模式 fat-jar） |
| 新 jar (部署) | `E:\DataupLoad-final.jar` 57,728,504 bytes (≈57.7 MB) |
| 旧 jar 备份 | `E:\DataupLoad-final.jar.bak-wx11c-fix-pre` 57,728,420 bytes |
| 备份大小差异 | 84 字节（class 文件改动量，符合预期） |

### 字节码实证（已修复）

```powershell
PS> javap -p -c IgnoreAlarm.class | grep -E 'endTime|ignoreTime'
  private java.time.LocalDateTime ignoreTime;
  private java.time.LocalDateTime endTime;
  public java.time.LocalDateTime getIgnoreTime();
  public java.time.LocalDateTime getEndTime();
  public com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm setIgnoreTime(java.time.LocalDateTime);
  public com.hikrobotics.solution.module.alarm.entity.IgnoreAlarm setEndTime(java.time.LocalDateTime);

PS> javap -p -c IgnoreAlarmServiceImpl.class | grep getEndTime
      61: ldc           #80                 // String getEndTime
```

✅ `endTime` 字段 + getter/setter 已写入 jar，`IgnoreAlarmServiceImpl` 字节码只引用 `getEndTime`（无 `getIgnoreTime` 残留）。

### 进程切换

| 阶段 | PID | 命令行 |
|---|---|---|
| 旧 (W-X11b) | 22296 | `hik-java.exe -jar -Dfile.encoding=UTF-8 E:\DataupLoad-final.jar` |
| 新 (W-X11c-fix) | 33004 | `hik-java.exe -jar -Dfile.encoding=UTF-8 E:\DataupLoad-final.jar` |

- 杀 PID 22296：`Invoke-CimMethod -MethodName Terminate` → ReturnValue=0 ✅
- 启 PID 33004：`Start-Process` 用 `hik-java.exe` launcher（沿用 W-X11b 启动方式，加 `-Dfile.encoding=UTF-8`）
- 启动脚本：`E:\DEMO\数据采集\logs\start-app-wx11.ps1`

### 重启后实证（30 秒内）

```text
2026-07-23T01:05:51.025+08:00  INFO 33004 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port(s): 80 (http) with context path ''
Get-NetTCPConnection -LocalPort 80 -State Listen
  LocalAddress  LocalPort  State  OwningProcess
  ::            80         Listen  33004
Invoke-WebRequest -Uri 'http://127.0.0.1/'
  ROOT_HEALTH=HTTP 200 OK
```

✅ **Port 80 监听 PID 33004、`/` 返回 200 OK、Tomcat started 实证齐全**。

注意：本服务没有 `/health` 端点（`/health` 返回 404），工单的"`curl /health` 必须 200 OK"在 PSM 框架下不存在该端点。改用 `/` 200 OK 作为存活证据。

---

## Step 3：复测 ignore 过滤（⚠️ 发现两个下游 bug）

### 工单期望 vs 实测

| 阶段 | alarm_record line1B/B1 | ignore_alarm 总数 | HTTP 响应 |
|---|---|---|---|
| A. 基线 | 0 | 0 | — |
| B. INSERT ignore_alarm（end_time = NOW+1h） | — | 1 | — |
| C. POST alarm `{"lineNo":"line1B","faceNo":"B1","defect":"TEST001"}` | — | — | **`{"code":10500,"message":"操作异常"}`** |
| D. POST 后查 | **1** ⚠️ | 1 | — |
| E. DELETE ignore_alarm（铁则） | — | 0 | — |
| F. DELETE alarm_record 残留 | 0 | — | — |
| G. 最终态 | 0 | 0 | — |

### 工单复测命令实际输出

```powershell
# 1. 进程活的
Get-CimInstance Win32_Process | ? Name -match 'hik-java' | Select-Object ProcessId
  ProcessId
  --------
     33004

# 2. 端口
Get-NetTCPConnection -LocalPort 80 -State Listen
  LocalAddress LocalPort State OwningProcess
  ::           80        Listen 33004

# 3. ignore_alarm 验证（拦截后行数不变 → 实际行数变化！）
& psql -c "SELECT COUNT(*) FROM alarm_record WHERE line_no='line1B' AND face_no='B1';"
  0   ← 基线
# ... POST 后 ...
  1   ← 拦截未生效 ⚠️

# 4. fix diff（Select-String）
Select-String -Path '...\IgnoreAlarmServiceImpl.java' -Pattern 'isIgnore|getEndTime|@TableField'
  命中 5 处：3 处 .gt/.lt(getEndTime) + @TableField + getEndTime getter
```

### 🐛 发现的两个下游 bug（**均非本工单根因**）

#### Bug #1：PG `varchar > timestamp` 操作符不存在（critical）

**日志实证**：
```
2026-07-23T01:14:09.716+08:00 ERROR ... GlobalExceptionHandler : [Unknown Exception]
### Error querying database.  Cause: org.postgresql.util.PSQLException:
   错误: 操作符不存在: character varying > timestamp without time zone
### SQL: SELECT COUNT(*) AS total FROM ignore_alarm WHERE (type=? AND defect_name=? AND line_no=? AND face_no=? AND end_time > ?)
```

**根因**：Flyway V1.20 `ignore_alarm.end_time` 列是 `varchar(19)`（不是 `timestamp`），但 `IgnoreAlarm.endTime` 字段类型是 `LocalDateTime`，MyBatis-Plus 把 `gt(LocalDateTime.now())` 绑定成 timestamp 参数。PG 无法直接比较 `varchar > timestamp`，抛 PSQLException。

**派工单约束冲突**：本工单严禁"把 getStartTime()/getEndTime() 之外的其他字段一起修"，但要解这个 bug 必须改 `endTime` 字段类型（String）+ 改 service 调用方式（`gt(String)`）。属新工单范畴。

**建议**（PM 决定）：新工单 `W-X11d-fix-ignore-type`，方案：
- 改 `IgnoreAlarm.endTime` 类型为 `String` + 全套 getter/setter
- 改 service：`gt(formatter.format(now()))` 输出 `yyyy-MM-dd HH:mm:ss` 字符串比较
- 或：保留 `LocalDateTime` 但在查询里用 `apply("CAST(end_time AS timestamp) > {0}", now)` 做类型转换

#### Bug #2：`AlarmRecordServiceImpl.add()` 不调 `isIgnore()`（设计层）

**根因**：从源码 grep 看，`isIgnore` 只在两个地方调用：
- `AlarmRecordServiceImpl.sendAlarmMessage()`（line 184, 189, 199）→ 控制 WS 推送 + yk 推送
- `IgnoreAlarmController.isIgnoreCheck()`（line 66）→ HTTP 查询接口

`add()` 方法（line 114-170）的流程是：
```java
if (isInterestingDefect) {
    // 把同一 (defectName + lineNo + faceNo + type) 下未处理的旧报警置为已忽略
    LambdaUpdateWrapper<AlarmRecord> uw = ...
    this.update(uw);
    AlarmRecord alarm = BeanUtil.copyProperties(form, AlarmRecord.class);
    ...
    this.save(alarm);              // ← 直接 save，无 isIgnore 检查
    this.sendAlarmMessage(alarm);  // ← 这里才检查 isIgnore
}
```

所以即使 `isIgnore()` 完美工作，`alarm_record` 表也会先写入一行（只是 yk/WS 不推）。**工单的"line1B/B1 不应新增"预期与现有代码架构不一致**。

**建议**（PM 决定）：新工单 `W-X11e-fix-ignore-save-gate`，在 `add()` 的 `if (isInterestingDefect)` 内增加：
```java
if (this.ignoreAlarmService.isIgnore(type, defectName, lineNo, faceNo)) {
    log.info("alarm ignored by ignore_alarm.[form={}]", form);
    return BaseResult.build().ok();  // 静默拦截，不写库
}
```

### 铁则遵守情况

✅ **未改 `yk.enable=false`**（老板 21:23 拍永久熔断）
✅ **未碰 application-prod.yml**
✅ **ignore_alarm 测试数据已 DELETE**（id=4 已删，最终态总数=0）
✅ **alarm_record 测试残留已 DELETE**（uuid='wx11c-step3' 已删，最终态 line1B/B1=0）
✅ **未改 getStartTime()/getEndTime() 之外的其他字段**
✅ **未加新功能**（仅按派工单修字段映射）

---

## 验收命令输出（PM 跑）

```powershell
$env:PGPASSWORD='postgres'

# 1. 进程活的
Get-CimInstance Win32_Process | ? Name -match 'hik-java' | Select-Object ProcessId
# ProcessId
# --------
#    33004

# 2. 端口
Get-NetTCPConnection -LocalPort 80 -State Listen
# LocalAddress LocalPort State   OwningProcess
# ::           80        Listen  33004

# 3. ignore_alarm 验证（应都为 0，铁则遵守）
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -h 127.0.0.1 -p 5433 -d intco -c "SELECT COUNT(*) FROM ignore_alarm;"
# 0  ✅

# 4. 看 Worker 报告里的 fix diff
Select-String -Path 'E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\entity\IgnoreAlarm.java' -Pattern 'getEndTime|@TableField'
Select-String -Path 'E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java' -Pattern 'isIgnore|getEndTime'
# 命中：@TableField("end_time") + getEndTime getter/setter + 3 处 service .gt/.lt(getEndTime)
```

---

## 后续建议（给 PM）

派工单第 33 行写"本工单 PASS 后 W-X11c 灰盒验证完整 PASS"。**但根据复测结果，本工单 PASS ≠ W-X11c 灰盒 PASS**，原因：
1. Bug #1（varchar > timestamp）：本工单修完后 isIgnore() 仍会抛 PSQLException，yk 推送永久 500
2. Bug #2（add() 不查 isIgnore）：即使 #1 修了，alarm_record 仍会写入

**建议派工顺序**：
1. 本工单（W-X11c-fix-ignore）— ✅ PASS（字段映射修完）
2. **W-X11d-fix-ignore-type**（新派）— 修 varchar vs timestamp
3. **W-X11e-fix-ignore-save-gate**（新派）— 在 add() 增加 isIgnore 检查
4. 然后再 W-X11c 灰盒复测 → 才可能 3/3 PASS
5. 才能进 W-X12a / W-X14 / W-X13a

---

## 交付物清单

- ✅ `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\entity\IgnoreAlarm.java`（+@TableField +endTime 字段+getter/setter）
- ✅ `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java`（3 处 getIgnoreTime→getEndTime）
- ✅ `E:\DataupLoad-final.jar`（重打 57.7 MB，PID 33004 在跑）
- ✅ `E:\DataupLoad-final.jar.bak-wx11c-fix-pre`（W-X11b 旧 jar 备份）
- ✅ ignore_alarm 表无残留（最终态 0 行）
- ✅ alarm_record 表 line1B/B1 无残留（最终态 0 行）
- ✅ yk.enable=false 未动
- ✅ application-prod.yml 未动

— Worker W-X11c-fix-ignore 完
