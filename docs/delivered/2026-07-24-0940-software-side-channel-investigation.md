# W-X28: 推送"疯狂"真凶初探（PM 自查，未派工）

> **PM 自写**（不派 Worker）。直接查现场证据，15 分钟闭环。

## 老板问题原文

> 刚才程序还是在疯狂推送。 为啥呀？ 是因为有别的服务干扰？ 还是咱程序里的过滤机制没有起到作用？

## 现场状态（09:40 体检）

| 项 | 值 |
|---|---|
| hik-java PID | 27548 @ 2026/7/24 9:27:50 启动（**09:20→09:27 中间服务挂过**，09:27 重启） |
| 端口 80 LISTEN | 只有 hik-java 一家 |
| 16 台工控机 ESTABLISHED | 全连着我们这台 |
| yk.uploadEnabled | **false**（application-prod.yml 铁则 42 红线守住）|
| yk.loginEnabled | true（只拿 ticket，不推数据）|
| alarm.global-enabled | true（报警照常落 PG）|
| DataupLoad.log 最新时间戳 | 2026/7/24 09:20:07（**09:20→09:27 期间无日志 = 服务挂了 7 分钟**）|

## PM 初查 5 个疑点

### 疑点 1: 别服务干扰？ — ❌ 不是
- 80 端口 LISTEN 只有 hik-java（PID 27548）
- 16 台工控机 ESTABLISHED 全连我们
- **没有第二台服务在听**

### 疑点 2: 我们的程序在推？ — ⚠️ **半对**
- 09:20 前日志全部走"current alarm is not interesting defect"分支（应用层就过滤掉了）
- **这部分没推 yk，红线守住**
- **但是**：09:20 前有 **49 次 sendAlarmMessage 抛 BadSqlGrammarException**（`IgnoreAlarmServiceImpl.isIgnore:85` 调用 `SELECT COUNT(*) FROM ignore_alarm WHERE end_time > ?`）

### 疑点 3: SQL 错误是新坑？ — ⚠️ **是 W-X23c 没修干净**
- 错误：`操作符不存在: timestamp without time zone > character varying`
- 触发：`IgnoreAlarmServiceImpl.isIgnore(IgnoreAlarmServiceImpl.java:85)` → `IgnoreAlarmMapper.selectCount`
- SQL：`SELECT COUNT(*) FROM ignore_alarm WHERE (type=? AND defect_name=? AND line_no=? AND face_no=? AND end_time > ?)`
- W-X23c 当时只查了 `IgnoreAlarmService` 几个 grep 到的 SQL，**没扫到 isIgnore 第 85 行用的这个 selectCount**

### 疑点 4: 09:20→09:27 服务为啥挂了？ — ⚠️ **疑似 sendAlarmMessage 报太多次把服务拉爆**
- DataupLoad.log 在 09:20:07 之后没新日志
- 09:27:50 重启
- error.log 在 09:20 前后大量堆栈（49 次 sendAlarmMessage + 1 次 updateTicket）
- netty ReadTimeoutException 也有（PSM 推送链路 netty 通道空闲超时）

### 疑点 5: 真的推到 MES 了吗？ — ❌ **目前 0 推送（红线守住）**
- 49 次 sendAlarmMessage 全部在 isIgnore SQL 那里抛异常**就退出**了
- 根本没走到 yk.push 那一行
- yk.uploadEnabled=false 实际是**双保险**（应用层 + 配置层）

## PM 当前判断（说错就背锅）

**不是"疯狂推送"，是 sendAlarmMessage 在疯狂抛 SQL 异常（49 次/小时）**。
老板说的"疯狂推送"可能是：
- 飞书群里看到的推送消息？ → 看是不是 MES 那边直接发（不是我们走的 yk 链路）
- 还是 error.log 日志输出太密看着像"推送"？ → 49 次 BadSqlGrammarException 刷屏

## 真凶：W-X23c 修漏了一个 SQL

| 漏修位置 | 错误 | 当前状态 |
|---|---|---|
| `IgnoreAlarmServiceImpl.isIgnore:85` | `end_time > ?` 参数类型不匹配 | **仍报错 49 次** |
| 当时 W-X23c 修的（应该是另几条 SQL）| 已修 | 0 新错（90s 验证 OK）|

## PM 道歉

W-X23c 修复报告写了"90s 0 新错"，但实际：
1. 只跑了 `IgnoreAlarmService` 内 grep 到的几条 SQL 验证
2. **没扫 isIgnore 第 85 行用的 MyBatis Plus `IService.count()` 自动生成的 selectCount**
3. 当时 PSM 反编译 isIgnore 时只看字段名（ignoreTime），没看运行时 SQL
4. **铁则 45 立得不彻底**（runtime error 为准）—— PM 验收时没真的去看 sendAlarmMessage 全路径日志

## 下一步（PM 自提，等老板拍）

| 选项 | 描述 | 时间 |
|---|---|---|
| A | PM 自查 selectCount 的 mapper，把 `end_time > ?` 加 `::varchar` 强转（同 W-X23c 做法）| 5 分钟（手熟）|
| B | 派 W-X28 完整扫 IgnoreAlarmService / AlarmRecordService 全方法 SQL，列清单 + 全修复 + 全验证 | 1.5 小时 |

PM 建议：**先 A 止血，再 B 根治**。

## 老板的"疯狂推送"具体指啥？

PM 不敢瞎猜。请老板明示：
- 是飞书群在不停收到消息？（如果是，可能是 MES 192.168.80.33:10031 那边直接推的，不走我们）
- 还是看 alarm_record 表行数涨得快看着像在推？（99% 是 sendAlarmMessage 抛异常堆栈噪声）
- 还是别的情况？

## 归档

`docs/delivered/2026-07-24-0940-software-side-channel-investigation.md`

PM 准备 30 分钟内给老板完整答复。但需要老板先回 1 件事：**"疯狂推送"的具体现象是啥？**
