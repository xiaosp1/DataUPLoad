# W-F02-B：IgnoreAlarm HTTP 接口

## 范围（只能改这 1 个文件）
- 新增 `E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\web\IgnoreAlarmController.java`

## 任务
提供 IgnoreAlarm CRUD 的 HTTP 接口。

## 依据
PSM 反编译 `AlarmRecordServiceImpl.handleAlarmIgnore` 字节码：
- HTTP 入口（需要查 PSM `IgnoreAlarmController` class，本任务简化）
- POST 创建 / DELETE 删除 / GET 列表

## 实现要点
1. `@RestController @RequestMapping("/web/alarm/ignore")`
2. 4 个接口：
   - `POST /` 添加忽略（调 `ignoreAlarmService.handleAlarmIgnore(dto)`）
   - `DELETE /{id}` 删除忽略
   - `GET /` 列表（`ignoreAlarmService.getIgnoreDefect()`）
   - `GET /check` 检查是否被忽略（参数：type, lineNo, faceNo, defectName）
3. 返回 `BaseResult`

## 验证
- 编译通过
- 接口路径都注册到 Spring

## 约束
- 不准写 SQL
- 不准改其他 Controller
- 不准重启服务

## 回报格式
```
W-F02-B 完成：[success/partial/blocked]
文件：[相对路径]
编译：[通过/失败]
接口清单：[4 个 URL + Method]
依赖：[W-F02-A 必须先完成]
建议测试：[怎么验证]
```
