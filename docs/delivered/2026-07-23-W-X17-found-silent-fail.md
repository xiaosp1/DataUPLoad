# 🚨 W-X17 重大产线 BUG 发现 — ignore_alarm 过期清理 cron 静默失败 6 小时

**发现时间**：2026-07-23 14:50
**发现工单**：W-X17（断连+retention cron 灰盒测试）
**执行人**：W-X17 Worker
**派工人**：PM 锋卫 🏭

---

## 🚨 BUG 摘要

**DataupLoad ignore_alarm 过期清理定时任务，自 09:00 起每小时静默失败，6 次失败无任何人发现。**

| 项 | 内容 |
|---|---|
| **影响** | ignore_alarm 表的过期记录永远不会自动清理（业务可用，但表会无限增长）|
| **失效时间** | **09:00 / 10:00 / 11:00 / 12:00 / 13:00 / 14:00 共 6 次** |
| **检测延迟** | **6 小时**（没人发现）|
| **错误可见性** | 仅 `error.log` 一行 ERROR + stack trace，try/catch 吞掉 |
| **报警链路** | W-X18 监控**未接**这个错误（信号 6 只监 ignore_alarm 表变更，不监 ERROR 日志）|
| **严重性** | 🔴 **P0 紧急**（老板当前关注"防疯狂推送" — 防推送第 2 道关卡静默坏了）|

---

## 🔍 3 个连环 BUG

### BUG #1：`getIgnoreTime` 引用了不存在的字段

```java
// DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java:51
LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
    .lt(IgnoreAlarm::getIgnoreTime, LocalDateTime.now());
```

- **PG 实际列**：`start_time(varchar 19)` / `end_time(varchar 19)`（无 `ignore_time`）
- **PG 报错**：`column "ignore_time" does not exist`
- **源头**：`IgnoreAlarm.java:22` 有 `private LocalDateTime ignoreTime;` 但无 `@TableField` 注解 → MP 生成 SQL 时用**字段名 ignore_time**而非 end_time
- **历史**：PSM 反编译源码里也是 `ignoreTime`（推测 PSM 也有同样 BUG，或 DB schema 不一样）

### BUG #2：`removeExpire()` 跨类型比较

```java
// DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java:46-49
public void removeExpire() {
   LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .lt(IgnoreAlarm::getEndTime, LocalDateTime.now());
   this.remove(qw);
}
```

- **PG 报错**：`operator does not exist: character varying < timestamp with time zone`
- **根因**：`end_time` 是 `varchar(19)` 存 `"yyyy-MM-dd HH:mm:ss"`，MP 给参数是 `LocalDateTime` → timestamp

### BUG #3：try/catch 吞错

```java
// DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java:60-62
} catch (Exception e) {
    log.error("ignore expire alarm remove failed, exception: {}", e.getMessage(), e);
}
```

- 错误只在 `error.log` 打一行，不抛
- 没有任何监控告警接这个错误
- **6 次失败无任何人发现**

---

## 🛠️ PM 立刻行动（14:50）

已派 **W-X17a**（subagent 正在跑）：
- 修 IgnoreExpireTask.java → 改用 `end_time` + 字符串比较
- 修 IgnoreAlarmServiceImpl.removeExpire() → 同步改字符串比较
- 编译验证 0 错 0 警告
- 字节码 `javap -c` 验证
- **不允许重启 hik-java PID 33248**

### 部署时机

PM 决策：**W-X17a 改完后，PM 等老板拍重启时机**（铁则 44：停生产进程必须有回滚路径 + sandbox 验证）。

回滚路径：`hik-java` cp 模式启动新代码已经验证过，回滚到老 cp 启动即可。

---

## 🎓 PM 翻车教训（铁则 48 立）

**W-X18 监控漏设计**：

| 已实现的 7 个信号 | 没接的 |
|---|---|
| ✅ yk 调用日志 | ❌ cron 异常日志（ERROR 行 + scheduling 线程死）|
| ✅ PG alarm_record 增长 | ❌ ignore_alarm 表清理异常 |
| ✅ yk 配置 | ❌ retention cron 异常 |
| ✅ ticket 续期 | ❌ schedule 线程存活（scheduling-1 是否还活着）|
| ✅ alarm_record 入库对比 | ❌ log 输出 ERROR 行数 |
| ✅ ignore_alarm 表变更 | |
| ✅ hik-java CPU user | |

**铁则 48（新立）**：监控脚本必须接 **scheduling 线程存活 + error.log ERROR 计数** 两个信号，否则 cron 类 BUG 永远发现不了。

**PM 翻车承认**：

- ❌ PM 在 W-X18 工单设计时漏想了 cron 异常监控
- ❌ PM 没在 W-X17 派工前自查 IgnoreExpireTask 代码（应该是第一道工序）
- ❌ PM 6 次 ERROR log 在 error.log 1.7MB 里沉底，从未主动翻

---

## 📊 当前产线状态（14:51）

| 项 | 值 |
|---|---|
| hik-java PID 33248 | alive 6h17min |
| ignore_alarm 表 | 3 条（W-X15 测试残留 + 1 条 xx）|
| 业务影响 | **无**（手动清理仍可用，cron 修了就恢复）|
| yk push | 0 ERROR |
| yk ticket | 续期 1 次（50min 周期正常）|

---

## 🪝 等老板拍

1. **W-X17a 跑完后是否立刻重启 hik-java？** 还是等 W-X15 报告回来一起重启？
2. **铁则 48 监控补丁（W-X18-v2）要不要立刻派？** 把 cron 异常 + scheduling 线程存活 + error.log ERROR 计数 3 个信号加上
3. **要不要派 W-B03（retention 90 → 3 天）？** 90 天涨库风险 + W-X17 顺手修的话成本极低

🏭 PM 锋卫
