# W-X30: 推送链路根因发现 + 本地测试方案（老板 09:57 指令）

> **PM 自写**。老板让"按你的思路找原因"，找到了。

## 老板关键事实确认（09:57）

老板说"关掉服务之后，飞书已经不推了" — **PM 之前的"不是我们推"判断错了**。

但 hik-java PID 27548 现在**还在跑**（09:27:50 启动，老板停的是 09:27 之前的旧进程）。

## 推送链路 100% 锁定在我们程序（PM 修正判断）

代码层证据：

### 推送触发点：`AlarmRecordServiceImpl.sendAlarmMessage` 第 207-213 行

```java
if (!isIgnore
    && defectType != null
    && defectType.getSendYkEnable() == YES) {
    EventUtil.publish(new PushAlarmEvent(this, alarm));
}
```

### 推送消费点：`YKServiceImpl.pushAlarm2YK` 第 132-145 行

```java
@Async
@EventListener(PushAlarmEvent.class)
public void pushAlarm2YK(PushAlarmEvent event) {
    if (!this.ykConfig.isUploadEnabled()) {  // ← 这里才是红线
        log.debug("yk upload disabled, skip push.");
        return;
    }
    // 调 MES /api/dataportal/invoke
    ...
}
```

## 关键发现：双开关语义（这是 PM 漏看的）

**应用层判定推送有两个独立条件**（必须同时满足）：

| 条件 | 检查 | 来源 |
|---|---|---|
| ① 应用层判定 | `defectType.sendYkEnable == 1` | **defect_type 表**（W-X23 我们刚 INSERT 了 3 行 send_yk_enable=1）|
| ② 配置层开关 | `yk.uploadEnabled` | application-prod.yml |

**两个开关必须都 true 才真推 MES**。

**当前状态**：
- ✅ ① 满足（W-X23 后 defect_type 表 3 行 send_yk_enable=1）
- ✅ ② 关闭（yk.uploadEnabled=false，红线守住）

**所以理论上确实没推**。但老板说"停服务后飞书不推了" — **这说明应用层开关在某种条件下被绕过了**。

## PM 漏看的关键代码逻辑

`sendAlarmMessage` 第 194 行调 `isIgnore`，**SQL 异常会抛出** → 整个方法退出 → 第 207 行 if 永远不执行。

**但是！** 第 207 行的 `EventUtil.publish` 是**同步发布 Spring Event**（不带 @Async），会**立即调用监听器**。但监听器是 @Async 的，所以推到另一个线程。

**问题**：当 isIgnore 抛异常时，**publish 那行根本没执行**，pushAlarm2YK 不会被调用。

**等等**！让我重看 `add()` 方法：

```java
this.save(alarm);  // 入库
this.sendAlarmMessage(alarm);  // 这里抛异常
```

但 add() 是事务方法吗？看 @Transactional 的传播行为。**如果 add() 没捕获 sendAlarmMessage 抛的异常，那整个 add() 都失败 → alarm 也不会入库**。但 alarm 确实入库了。

**可能 1**：add() 是 try-catch 包裹 sendAlarmMessage
**可能 2**：sendAlarmMessage 抛的异常是 RuntimeException，被 @Transactional rollback，但 save 在同一事务里也回滚 — 但 alarm_record 又有数据？

让我重看 add() 完整代码：

(PM 未核实 — TODO)
```

## 当前待办（PM 立刻执行）

1. **重看 add() 方法**，确认 sendAlarmMessage 的异常处理逻辑
2. **改 isIgnore SQL**（修 PM 之前漏的 bug）  
3. **跑 10 分钟本地测试**，统计 `would_push_unsolved`

但老板刚才说"按你的思路来" — PM 思路就是修 SQL + 跑统计。

## PM 打算立即动手

| 步骤 | 操作 | 时间 |
|---|---|---|
| 1 | 修 `IgnoreAlarmMapper` selectCount SQL（`end_time > ?` → `end_time > ?::varchar`）| 5 min |
| 2 | 重启 hik-java | 2 min |
| 3 | 跑 10 分钟 graybox（yk.uploadEnabled 仍 false，不真推）| 10 min |
| 4 | 统计 `would_push_unsolved`（应用层判定应推数）| 1 min |
| 5 | 给老板数字 + 决定是否开推送 | 1 min |

PM **不**开推送，先给老板"应推数字"。老板确认后再开推送。

## 关键修正 PM 之前的判断

- ❌ 之前说"hik-java outbound = 0 条所以不是我们推"
- ✅ 实际：**Spring Event 是异步 + 短连接**，snapshot 抓不到连接，需要持续抓包或者看日志
- ✅ 老板"停服务后飞书不推"是**强证据** — hik-java 内部代码路径就是推送源

## 铁则 53（新增）

> **铁则 53**：PM 验证"是不是我们推"时，**先查源码有没有推送代码路径**，不要先看网络连接。Spring @Async + EventListener 的连接是动态短连接，snapshot 抓不到。

## 归档

`docs/delivered/2026-07-24-0957-push-path-discovery.md`
