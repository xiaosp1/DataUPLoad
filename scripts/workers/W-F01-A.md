# W-F01-A：yk ticket 定时续期

## 范围（只能改这一个文件）
- 新增 `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\yingke\scheduled\YKScheduledTicketRenewer.java`

## 任务
实现 yk ticket 定时续期，每 49 分钟调用一次 `YKServiceImpl.updateTicket()`。

## 依据
PSM 反编译 `IntcoScreen-1.0-SNAPSHOT-20260605135937.jar` 中：
- `YKServiceImpl.threadPoolTaskScheduler.schedule(Runnable, Instant)`
- 路径：`E:\DEMO\数据采集\tmp_psm_decompile\BOOT-INF\classes\com\hikrobotics\solution\module\yingke\service\impl\YKServiceImpl.class`
- 字节码 136-140 行：`schedule(Runnable, Instant)` 用于周期续 ticket

## 实现要点
1. 用 `@Scheduled(fixedDelay = 49 * 60 * 1000)`（PSM 配置 `yk.login-interval: 50`，提前 1 分钟续）
2. 注入 `YKServiceImpl`，调用 `updateTicket()` 私有方法（注意：原方法是 private，要么反射要么改成 package-private）
3. 不要自己起 ThreadPoolTaskScheduler，PSM 是 service 内部 new 一个，DataupLoad 用 `@Scheduled` 更简单
4. log INFO 级别：`yk ticket renewed by scheduler.[ticket=...]`

## 验证
```powershell
cd E:\DEMO\数据采集\DataupLoad
.\jdk\bin\javac.exe -d target\classes -cp "lib\*;target\classes" src\main\java\com\hikrobotics\solution\module\yingke\scheduled\YKScheduledTicketRenewer.java
```
- 编译通过
- 重启 hik-java 后看 log 每 49 分钟有 "yk ticket renewed"

## 约束
- 不准改 application-prod.yml
- 不准碰 yk.enable 开关
- 不准重启服务
- 不准推测试报警
- 完成只回报，不动其他文件

## 回报格式
```
W-F01-A 完成：[success/partial/blocked]
文件：[相对路径]
编译：[通过/失败]
日志证据：[关键 log 行]
建议测试：[怎么验证]
```
