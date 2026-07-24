# W-X17a — 修 ignore_alarm 过期清理 cron 静默失败 BUG（🚨 P0 紧急）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 14:50
**优先级**：🔴 P0（产线已坏 4 小时）
**基于工单**：W-X17 验证（PASS / 但发现 BUG）

---

## 🚨 BUG 复现（W-X17 已实测）

### BUG #1：IgnoreExpireTask 用了不存在的字段
- **代码位置**：`DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java:51`
- **错误代码**：
  ```java
  LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .lt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());   // ❌ ignore_time 不存在
  ```
- **PG 实际列**：`start_time(varchar 19)` / `end_time(varchar 19)` / `create_time(timestamp)` / `update_time(timestamp)`
- **PG 报错**：`column "ignore_time" does not exist`

### BUG #2：removeExpire() 字段类型不匹配
- **代码位置**：`DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java:46-49`
- **错误代码**：
  ```java
  public void removeExpire() {
     LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
        .lt(IgnoreAlarm::getEndTime, LocalDateTime.now());  // ❌ end_time 是 varchar
     this.remove(qw);
  }
  ```
- **PG 报错**：`operator does not exist: character varying < timestamp with time zone`

### BUG #3：try/catch 吞错，cron 静默失败
- **代码位置**：`IgnoreExpireTask.java:60-62`
- **后果**：
  - 11:00 / 12:00 / 13:00 / 14:00 共 4 次静默失败
  - DataupLoad.log 只输出一条 `ERROR ignore expire alarm remove failed`
  - **W-X18 监控也没接这个错误**（信号 6 只监表变更，不监 ERROR 日志）
  - ignore_alarm 表会无限增长

---

## 📋 任务清单

### 1. 修 IgnoreExpireTask.java
- **删掉 `getIgnoreTime` 引用**
- **改用 `end_time` + string 比较**（end_time 是 varchar(19) "yyyy-MM-dd HH:mm:ss"）
  ```java
  String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .apply("end_time < {0}", nowStr);
  ```
- **加 try/catch 不吞错**（日志必须 ERROR 或 rethrow）
- **cron 周期不变**（`0 0 * * * ?` 每小时）

### 2. 修 IgnoreAlarmServiceImpl.removeExpire()
- 同步改 string 比较（同上）
- 不再调用 `IgnoreAlarm::getEndTime`（避免类型推断报错）
- 验证 4 条测试数据：
  - old + expired end_time → 删
  - old + future end_time → 留
  - recent + expired end_time → 留（job 不该删当天刚配置的）

### 3. 编译验证（0 错 0 警告）
```
cd E:\DEMO\数据采集
javac -encoding UTF-8 -cp "DataupLoad\lib\*;DataupLoad\target\classes" -d DataupLoad\target\classes \
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java \
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java
```

### 4. 写报告
`docs/delivered/2026-07-23-W-X17a-fix-result.md`，必须包含：
- 改动前后 diff
- 编译结果（命令 + 输出）
- 字节码验证（`javap -c` 看到 invoke `format` + `apply`）
- **不重启 hik-java**（PM 决策）

---

## 🚫 严禁

- ❌ 重启 hik-java PID 33248（铁则 44）
- ❌ 改 application.yml
- ❌ 改 yk.uploadEnabled / loginEnabled
- ❌ 改 alarm.global-enabled（除非老板指令）
- ❌ 改其它业务代码
- ❌ 删 ignore_alarm 表数据（虽然旧数据已过期）

---

## 🎯 PM 验收标准（铁则 40/41）

1. ✅ `IgnoreExpireTask.java` 不再引用 `getIgnoreTime`
2. ✅ `removeExpire()` 用 string 比较通过
3. ✅ javac 编译 0 错 0 警告
4. ✅ 字节码 `javap -c` 显示字符串比较逻辑
5. ✅ 报告完整（含 diff + 编译结果 + 字节码证据）

完成在群内回复。
