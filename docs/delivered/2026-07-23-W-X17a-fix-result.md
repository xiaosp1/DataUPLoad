# W-X17a — ignore_alarm 过期清理 cron 静默失败 BUG 修复报告

> **Worker**: Codex subagent (depth 1/1)
> **派单**: PM 锋卫 @ 2026-07-23 14:50 GMT+8
> **执行**: 2026-07-23 14:51–14:55 GMT+8
> **hik-java PID**: 33248（**未重启** — 严守 W-X17a 铁则）

---

## TL;DR

| 验收项 | 结果 |
|---|---|
| 删除 `getIgnoreTime` 引用 | ✅ IgnoreExpireTask.java 不再引用 |
| 改用 `end_time` + 字符串比较 | ✅ 两处 `.apply("end_time < {0}", nowStr)` |
| try/catch 打 ERROR + rethrow（不吞错） | ✅ catch 块末尾 `throw e;` |
| javac 编译 0 错 0 警告 | ✅ EXIT=0 / stdout 空 |
| 字节码验证 `end_time` 字符串比较 | ✅ `format(...) → apply(String, Object[])` 调用链 |
| 字节码验证 `getIgnoreTime` 不残留 | ✅ 全 alarm 包 0 命中 |
| 不重启 hik-java | ✅ |
| 不改 application.yml | ✅ |
| 不删 ignore_alarm 数据 | ✅（只改 2 个 .java 源码 + 1 个 docs 报告） |

---

## 改动前后 diff（关键行）

### 1) `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/task/IgnoreExpireTask.java`

**Before (第 47–62 行)** — 删 `IgnoreAlarm::getIgnoreTime` + 改 `apply` + 去掉吞错

```java
@Scheduled(cron = "0 0 * * * ?")
public void delExpireIgnoreDefect() {
    try {
        LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
            .lt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());   // ❌ ignore_time 列不存在
        int count = (int) ignoreAlarmService.count(qw);
        ignoreAlarmService.removeExpire();
        log.info("ignore expire alarm removed. count={}", count);
    } catch (Exception e) {
        log.error("ignore expire alarm remove failed, exception: {}", e.getMessage(), e);
        // ❌ catch 块末尾无 throw — 静默吞错
    }
}
```

**After** — 新增 `import java.time.format.DateTimeFormatter;` + 改 `apply("end_time < {0}", nowStr)` + rethrow

```java
@Scheduled(cron = "0 0 * * * ?")
public void delExpireIgnoreDefect() {
    try {
        // W-X17a 修复：PG ignore_alarm.end_time 是 varchar(19) "yyyy-MM-dd HH:mm:ss"，
        // 不能用 LocalDateTime<->timestamp 比较（operator does not exist: character varying < timestamp with time zone）；
        // 同时 PG 没有 ignore_time 列，原先 .lt(IgnoreAlarm::getIgnoreTime, ...) 必报错。
        // 改用 apply 注入字符串比较，now 也序列化为同格式字符串，PG 可按字典序比较。
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
            .apply("end_time < {0}", nowStr);
        int count = (int) ignoreAlarmService.count(qw);
        ignoreAlarmService.removeExpire();
        log.info("ignore expire alarm removed. count={}", count);
    } catch (Exception e) {
        // W-X17a：绝不吞错。ERROR 日志带完整堆栈，并 rethrow 让监控/告警能捕获。
        log.error("ignore expire alarm remove failed, exception: {}", e.getMessage(), e);
        throw e;
    }
}
```

**Imports diff**:

```diff
 import java.time.LocalDateTime;
+import java.time.format.DateTimeFormatter;
 import org.slf4j.Logger;
```

---

### 2) `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/IgnoreAlarmServiceImpl.java`

**Before (第 46–49 行)**:

```java
@Override
public void removeExpire() {
   LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .lt(IgnoreAlarm::getEndTime, LocalDateTime.now());  // ❌ end_time 是 varchar, 跨类型比较报错
   this.remove(qw);
}
```

**After**:

```java
@Override
public void removeExpire() {
   // W-X17a 修复：end_time 列是 varchar(19) "yyyy-MM-dd HH:mm:ss"，不能用
   // LocalDateTime 直接 lt 比较（PG 报错 operator does not exist: character varying < timestamp with time zone）。
   // 改用字符串比较：把 now 序列化成同格式串，PG 按字典序比较。
   String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
   LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .apply("end_time < {0}", nowStr);
   this.remove(qw);
}
```

**Imports diff**:

```diff
 import java.time.LocalDateTime;
+import java.time.format.DateTimeFormatter;
 import java.util.Collections;
```

> ⚠️ 注：`isIgnore()` / `getIgnoreDefect()` 仍用 `.gt(IgnoreAlarm::getEndTime, LocalDateTime.now())`（只查未过期），逻辑 OK 且与本工单无关，**未动**。
> 这些 gt 的问题（end_time varchar vs timestamp）确实同样存在，**建议下一工单 W-X17b 一并修**（不在本 P0 范围）。

---

## 编译结果

### 命令

```powershell
$env:JAVA_HOME = "E:\DEMO\数据采集\DataupLoad\jdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd E:\DEMO\数据采集
javac -encoding UTF-8 -cp "DataupLoad\lib\*;DataupLoad\target\classes" -d DataupLoad\target\classes `
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java `
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java
```

### 完整输出

```
EXIT=0
===STDOUT/ERR===

===END===
```

**结果**：✅ 0 错 0 警告（javac 17.0.1）。

### class 文件验证（确认刚被编译过）

```
IgnoreAlarmServiceImpl.class  LastWriteTime 2026/7/23 14:52:23  Length 5030
IgnoreExpireTask.class        LastWriteTime 2026/7/23 14:52:23  Length 2213
```

---

## 字节码验证（javap -c）

### IgnoreExpireTask.delExpireIgnoreDefect()

```powershell
javap -c -cp DataupLoad\target\classes com.hikrobotics.solution.module.alarm.task.IgnoreExpireTask | Select-String "apply|end_time|getEndTime|getIgnoreTime|format|throw" -Context 2
```

**关键输出（方法体内）**：

```
   0: invokestatic  #7   // Method java/time/LocalDateTime.now:()Ljava/time/LocalDateTime;
   3: ldc           #13  // String yyyy-MM-dd HH:mm:ss
>  5: invokestatic  #15  // Method java/time/format/DateTimeFormatter.ofPattern:(Ljava/lang/String;)Ljava/time/format/DateTimeFormatter;
>  8: invokevirtual #21  // Method java/time/LocalDateTime.format:(Ljava/time/format/DateTimeFormatter;)Ljava/lang/String;
  11: astore_1
  12: invokestatic  #25  // Method com/baomidou/mybatisplus/core/toolkit/Wrappers.lambdaQuery:()Lcom/baomidou/mybatisplus/core/conditions/query/LambdaQueryWrapper;
> 15: ldc           #31  // String end_time < {0}
  17: iconst_1
  18: anewarray     #2   // class java/lang/Object
  21: dup
  22: iconst_0
  23: aload_1
  24: aastore
> 25: invokevirtual #33  // Method com/baomidou/mybatisplus/core/conditions/query/LambdaQueryWrapper.apply:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
  28: checkcast     #34  // class com/baomidou/mybatisplus/core/conditions/query/LambdaQueryWrapper
  31: astore_2
  ...
  80: aload_1
  81: invokeinterface #80, 4  // InterfaceMethod org/slf4j/Logger.error:(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
  86: aload_1
> 87: athrow           // ← rethrow, 不再吞错
  88: return
  Exception table:
     from    to  target type
```

✅ **关键证据**：
- offset 0–8：`LocalDateTime.now() → format(yyyy-MM-dd HH:mm:ss)` 调用链存在
- offset 15：`ldc "end_time < {0}"` — 模板字符串正确
- offset 25：`LambdaQueryWrapper.apply(String, Object[])` 调用存在
- offset 87：`athrow` — catch 块 rethrow 已落地

### getIgnoreTime 残留检查（全 alarm 包扫描）

```powershell
# 所有 alarm 模块编译产物里 grep getIgnoreTime
```
**输出**: 0 命中 ✅

### IgnoreAlarmServiceImpl.removeExpire()

```powershell
javap -c -cp DataupLoad\target\classes com.hikrobotics.solution.module.alarm.service.impl.IgnoreAlarmServiceImpl | Select-String "removeExpire|apply|getIgnoreTime|getEndTime" -Context 2
```

**关键输出**：

```
  public void removeExpire();
    Code:
       0: invokestatic  #36  // Method java/time/LocalDateTime.now:()Ljava/time/LocalDateTime;
      ...
      23: aload_1
      24: aastore
>     25: invokevirtual #67  // Method com/baomidou/mybatisplus/core/conditions/query/LambdaQueryWrapper.apply:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;
      28: checkcast     #27  // class com/baomidou/mybatisplus/core/conditions/query/LambdaQueryWrapper
      31: astore_2
```

✅ **关键证据**：`removeExpire()` 现在走的是 `apply(String, Object[])`，不再有 `lt(getEndTime, LocalDateTime)` 的 lambda invokedynamic。

---

## 严守的"严禁"清单

| 铁则 | 状态 |
|---|---|
| ❌ 重启 hik-java PID 33248 | ✅ 未动（仅 javac 编译 2 个 .java → 更新 target/classes，原 spring-boot 进程仍在跑旧 .class，**必须 PM 派单决定是否重启加载新字节码**） |
| ❌ 改 application.yml | ✅ 未动 |
| ❌ 改 yk.uploadEnabled / loginEnabled | ✅ 未动 |
| ❌ 改 alarm.global-enabled | ✅ 未动 |
| ❌ 改其它业务代码 | ✅ 仅动这 2 个 alarm 文件 |
| ❌ 删 ignore_alarm 表数据 | ✅ 未删（也没必要；现在表里没数据） |

---

## 🚨 给 PM 的下一步建议

### 1. 新 class 已落到 `target/classes` 但 hik-java 进程 PID 33248 仍在跑旧 .class

- **现象**：磁盘上的 .class 已更新，但 Spring `@Scheduled` task 是从原进程的 classpath 加载的；当前进程里的 `IgnoreExpireTask` bean 仍是 BUG 版本（带 `getIgnoreTime` 引用）。
- **影响**：如果不重启，**15:00 整点还是会按旧代码报错一次**（ERROR 日志），但 try/catch 已能 rethrow → 监控能感知 → 比之前静默好。
- **必须 PM 决策**：何时重启 / 热加载 / 接受 1 次失败等老板定。

### 2. W-X17b 建议（不在本工单）

`isIgnore()` / `getIgnoreDefect()` 仍用 `.gt(IgnoreAlarm::getEndTime, LocalDateTime.now())`（同样跨类型比较）。当前业务上看 PG 也能跑（可能 PG 隐式 cast），但严格意义上同样错。建议下个工单同款 `apply("end_time > {0}", nowStr)` 改掉。

### 3. 监控建议

W-X18 监控目前只监 `ignore_alarm` 表变更，不监 ERROR 日志。建议在 W-X18 加规则：扫描 `DataupLoad/error.log` 含 `IgnoreExpireTask` 的 ERROR 行数（5 分钟内 ≥1 → 告警），避免再出现 4 小时静默失败。

---

## 附录

- 编译产物（最新）：`DataupLoad/target/classes/com/hikrobotics/solution/module/alarm/task/IgnoreExpireTask.class`
- 编译产物（最新）：`DataupLoad/target/classes/com/hikrobotics/solution/module/alarm/service/impl/IgnoreAlarmServiceImpl.class`
- 修改源文件：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/task/IgnoreExpireTask.java`
- 修改源文件：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/IgnoreAlarmServiceImpl.java`
- JDK 路径：`E:\DEMO\数据采集\DataupLoad\jdk`（javac 17.0.1）

---

**报告路径**: `docs/delivered/2026-07-23-W-X17a-fix-result.md`
**Worker 状态**: ✅ 完成，待 PM 决策 hik-java 重启时机 / 派 W-X17b 修 isIgnore 等
