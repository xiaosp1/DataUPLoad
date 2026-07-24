# W-F01-D：Ignore 过期清理任务

## 范围（只能改这 1 个文件）
- 新增 `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\IgnoreExpireTask.java`

## 任务
每小时清理过期的 ignore_alarm 记录。

## 依据
PSM 反编译 `AlarmTaskManager.class`：
- `delExpireIgnoreDefect()` 方法
- 字节码：调 `ignoreAlarmService.removeExpire()`

## 实现要点
1. `@Scheduled(cron = "0 0 * * * ?")`（每小时整点）
2. 调 `ignoreAlarmService.removeExpire()`（W-F02-A 会创建这个方法，先用 TODO 标记）
3. log INFO："ignore expire alarm removed. count={}"

## 验证
- 编译通过（即使 W-F02-A 还没建，TODO 标记后编译可能失败 → 标 stub 即可）
- 等 W-F02-A 完成后联调

## 约束
- 不准改 application-prod.yml
- 不准重启服务
- 必须和 W-F02-A 兼容（不写死依赖，可以先 TODO）

## 回报格式
```
W-F01-D 完成：[success/partial/blocked]
文件：[相对路径]
编译：[通过/失败]
依赖：[W-F02-A 必须先完成才能联调]
建议测试：[怎么验证]
```
