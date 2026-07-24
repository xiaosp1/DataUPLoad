# W-F01-B：白名单定时同步

## 范围（只能改这一个文件）
- 新增 `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\framework\security\WhiteListScheduledRunner.java`

## 任务
白名单 IP 每 5 分钟从数据库拉到内存。

## 依据
- PSM `WhiteListRunner.run`（17:04:58 跑成功 log："white ip list refresh over."）
- 现在只启动时跑一次，需要改成定时

## 实现要点
1. `@Scheduled(fixedRate = 5 * 60 * 1000)`，每 5 分钟
2. 逻辑：读 white_ip 表 → 写到内存 Set（用 `ConcurrentHashMap.newKeySet()`）
3. log INFO："white ip list refresh over, count={}"
4. 启动时也跑一次（@PostConstruct）

## 验证
- 编译通过
- 改 white_ip 表，5 分钟内生效
- log 每 5 分钟一条 "white ip list refresh over"

## 约束
- 不准改 application-prod.yml
- 不准重启服务
- 完成只回报，不动其他文件

## 回报格式
```
W-F01-B 完成：[success/partial/blocked]
文件：[相对路径]
编译：[通过/失败]
日志证据：[关键 log 行]
建议测试：[怎么验证]
```
