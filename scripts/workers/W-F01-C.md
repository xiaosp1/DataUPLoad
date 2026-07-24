# W-F01-C：报警 90 天清理任务

## 范围（只能改这 2 个文件）
- 新增 `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\task\AlarmRetentionTask.java`
- 复用现有 `AlarmRecordDAO`（已有 delete 方法）

## 任务
每天 3 点删除 90 天前、solved 的报警（防 DB 涨库）。

## 依据
PSM 反编译 `AlarmTaskManager.class`：
- `clearAlarmData()` 方法
- 字节码："delete alarm data 90 days ago success，delete count {}"
- 条件：`createTime < now-90 days AND solve = SOLVED`

## 实现要点
1. `@Scheduled(cron = "0 0 3 * * ?")`（每天 3 点）
2. 调 `alarmRecordDAO.delete(Wrappers.<AlarmRecordPO>lambdaQuery().lt(AlarmRecordPO::getCreateTime, LocalDateTime.now().minusDays(90)).eq(AlarmRecordPO::getSolve, AlarmSolvedEnum.SOLVED.getValue()))`
3. log INFO："delete alarm data 90 days ago success，delete count {}"
4. try/catch 包住，失败 log ERROR（PSM 也是这样）

## 验证
- 编译通过
- 插一条 createTime=91 天前、solve=1 的数据，等 3 点看是否删除
- 或临时改 cron `0/30 * * * * ?`（每 30 秒）做验证

## 约束
- 不准改 application-prod.yml
- 不准重启服务
- 不准真插 91 天前的数据（污染 DB），写测试用完删掉

## 回报格式
```
W-F01-C 完成：[success/partial/blocked]
文件：[相对路径]
编译：[通过/失败]
日志证据：[关键 log 行]
建议测试：[怎么验证]
```
