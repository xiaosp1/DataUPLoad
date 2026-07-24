# W-F02-A：IgnoreAlarm 全套模型

## 范围（5 个新文件）
1. `IgnoreAlarmPO.java`（实体）
2. `IgnoreAlarmDAO.java`（MyBatis-Plus Mapper）
3. `IgnoreAlarmDTO.java`（传输对象）
4. `IIgnoreAlarmService.java`（接口）
5. `IgnoreAlarmServiceImpl.java`（实现）

放在：`E:\DEMO\数据采集\DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\`

## 任务
同步 PSM `IgnoreAlarm*` 全套 5 个 class。

## 依据
PSM 反编译路径：`E:\DEMO\数据采集\tmp_psm_decompile\BOOT-INF\classes\com\hikrobotics\solution\module\alarm\`

字段（从 PSM 字节码 handleAlarmIgnore 方法反推）：
```sql
ignore_alarm (
    id serial primary key,
    ignore_all int,           -- 1-全部忽略 2-否
    face_id varchar,          -- 线体 ID
    line_no varchar(20),
    face_no varchar(20),
    type int,                 -- 1-缺陷 2-系统 3-设备
    defect_name varchar(50),
    start_time varchar(19),
    end_time varchar(19),
    create_time timestamp,
    update_time timestamp
)
```

## 实现要点
1. PO 用 `@TableName("ignore_alarm")` + `@TableId(type = IdType.AUTO)`
2. DAO 继承 `BaseMapper<IgnoreAlarmPO>`
3. ServiceImpl 继承 `ServiceImpl<IgnoreAlarmDAO, IgnoreAlarmPO>` 实现 `IIgnoreAlarmService`
4. 必须实现方法：
   - `BaseResult handleAlarmIgnore(IgnoreAlarmDTO dto)`（按 faceId 查 line → 写表）
   - `boolean isIgnore(Integer type, String lineNo, String faceNo, String defectName)`
   - `void removeExpire()`（按 end_time < now 删除）
   - `List<IgnoreAlarmPO> getIgnoreDefect()`

## 验证
```powershell
.\jdk\bin\javac.exe -d target\classes -cp "lib\*;target\classes" src\main\java\com\hikrobotics\solution\module\alarm\model\IgnoreAlarmPO.java src\main\java\com\hikrobotics\solution\module\alarm\mapper\IgnoreAlarmDAO.java src\main\java\com\hikrobotics\solution\module\alarm\dto\IgnoreAlarmDTO.java src\main\java\com\hikrobotics\solution\module\alarm\service\IIgnoreAlarmService.java src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java
```
- 5 个文件全部编译通过
- Service 接口方法签名和 PSM 一致

## 约束
- 不准建 SQL（建表交给 W-F02-C）
- 不准写 HTTP 接口（交给 W-F02-B）
- 不准改 application-prod.yml
- 不准重启服务

## 回报格式
```
W-F02-A 完成：[success/partial/blocked]
文件：[5 个相对路径]
编译：[通过/失败]
Service 方法签名：[贴出主要方法]
建议测试：[怎么验证]
```
