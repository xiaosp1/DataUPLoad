# W-X15a — 修 isIgnore / getIgnoreDefect 同款 varchar vs timestamp BUG（🚨 P0）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 14:58
**优先级**：🔴 P0（防疯狂推送第 2 道关卡坏了——白名单查询直接报错）
**基于工单**：W-X15 测试 PASS 但发现 BUG（铁则 22 + 铁则 40 验收）

---

## 🚨 BUG 复现（W-X15 已实测）

### BUG #1：isIgnore() 跨类型比较（🔴 **P0 严重**）

```java
// DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java:24-32
public boolean isIgnore(Integer type, String defectName, String lineNo, String faceNo) {
   if (type == null || defectName == null || lineNo == null || faceNo == null) {
      return false;
   }
   LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .eq(IgnoreAlarm::getType, type)
      .eq(IgnoreAlarm::getDefectName, defectName)
      .eq(IgnoreAlarm::getLineNo, lineNo)
      .eq(IgnoreAlarm::getFaceNo, faceNo)
      .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());    // ❌ varchar > timestamp
   return this.count(qw) != 0L;
}
```

- **PG 报错**：`org.springframework.jdbc.BadSqlGrammarException: 操作符不存在: character varying > timestamp without time zone`
- **触发条件**：**每次报警 → sendAlarmMessage 调 isIgnore → 报错**
- **HTTP 后果**：`POST /client/data/alarm` 返回 "操作异常"，但报警**仍入 PG**（catch 在 controller 层）
- **业务后果**：
  - **白名单永远失效**（isIgnore 抛异常前 return false，但 query 报错 count=0 → 仍 false）
  - **yk 推送全开**（白名单拦不住）
  - **HTTP 调用方收到 500**

### BUG #2：getIgnoreDefect() 同款（🟡 P1）

```java
// IgnoreAlarmServiceImpl.java:55-61
public List<IgnoreAlarm> getIgnoreDefect() {
   LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());    // ❌ 同款
   List<IgnoreAlarm> list = this.list(qw);
   return list == null ? Collections.emptyList() : list;
}
```

- **影响**：`GET /web/alarm/ignore` 报错（但前端应无影响，前端未接）

### BUG #3：IgnoreAlarmDTO 空类（🟡 P1）

```java
// DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\dto\IgnoreAlarmDTO.java
public class IgnoreAlarmDTO {
}
```

- **后果**：`POST /web/alarm/ignore` 收到 DTO 是空的 → `handleAlarmIgnore(form)` 写不进任何字段
- **运维后果**：必须直接 SQL 写 ignore_alarm

### BUG #4：handleAlarmIgnore() 空跑占位符（🟡 P1）

```java
// IgnoreAlarmServiceImpl.java:19-22
public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
   // PSM 原版该方法体几乎为空，仅返回 OK；DataupLoad 沿用。
   return BaseResult.build().ok();
}
```

- **后果**：调用永远返回 OK 但啥都不做

### BUG #5（信息）：T8 noise→IGNORE 规则未移植

- **T8 测试 FAIL**：当前 noise 报警 solve=2（UNSOLVED），PSM 同款规则未移植
- **修法**：要 PSM 反编译 + 重新设计，超出 W-X15a 范围，**本工单不修**（开 W-X19）

---

## 📋 任务清单（必须全做）

### 1. 修 IgnoreAlarmServiceImpl.isIgnore()（🔴 P0）
- 删 `.gt(IgnoreAlarm::getEndTime, LocalDateTime.now())`
- 改用字符串比较（同 W-X17a 套路）：
  ```java
  String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .eq(IgnoreAlarm::getType, type)
      .eq(IgnoreAlarm::getDefectName, defectName)
      .eq(IgnoreAlarm::getLineNo, lineNo)
      .eq(IgnoreAlarm::getFaceNo, faceNo)
      .apply("end_time > {0}", nowStr);
  return this.count(qw) != 0L;
  ```

### 2. 修 IgnoreAlarmServiceImpl.getIgnoreDefect()（🟡 P1）
- 同步改字符串比较：
  ```java
  String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
      .apply("end_time > {0}", nowStr);
  List<IgnoreAlarm> list = this.list(qw);
  return list == null ? Collections.emptyList() : list;
  ```

### 3. 改 IgnoreAlarmDTO.java 加字段（🟡 P1）

参考 PSM 反编译 + 当前 entity（IgnoreAlarm 字段），加：
```java
public class IgnoreAlarmDTO {
    private Integer id;
    private Integer type;
    private String defectName;
    private String lineNo;
    private String faceNo;
    private Integer ignoreAll;
    private String faceId;
    private String startTime;     // varchar "yyyy-MM-dd HH:mm:ss"
    private String endTime;       // varchar
    // getter / setter 全套
}
```

### 4. 修 handleAlarmIgnore()（🟡 P1）

PSM 反编译源码按 insert 实现：
```java
public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
    if (form == null) return BaseResult.build().error("20101");
    if (form.getType() == null || form.getDefectName() == null) {
        return BaseResult.build().error("20102");
    }
    IgnoreAlarm entity = new IgnoreAlarm()
        .setType(form.getType())
        .setDefectName(form.getDefectName())
        .setLineNo(form.getLineNo())
        .setFaceNo(form.getFaceNo())
        .setIgnoreAll(form.getIgnoreAll())
        .setFaceId(form.getFaceId())
        .setEndTime(parseTime(form.getEndTime()));
    this.save(entity);
    return BaseResult.build().ok();
}
```

注意 `IgnoreAlarm` entity 没 `ignoreAll` / `faceId` 字段——需扩展 entity：
```java
// 在 IgnoreAlarm.java 加
@TableField("ignore_all")
private Integer ignoreAll;
@TableField("face_id")
private String faceId;
// getter/setter
```

### 5. 编译验证（0 错 0 警告）
```
cd E:\DEMO\数据采集
javac -encoding UTF-8 -cp "DataupLoad\lib\*;DataupLoad\target\classes" -d DataupLoad\target\classes ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\entity\IgnoreAlarm.java ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\dto\IgnoreAlarmDTO.java ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java
```

### 6. 字节码验证
```
javap -c -cp DataupLoad\target\classes com.hikrobotics.solution.module.alarm.service.impl.IgnoreAlarmServiceImpl | Select-String "apply|isIgnore|handleAlarmIgnore|getEndTime" -Context 2
```

### 7. 写报告
`docs/delivered/2026-07-23-W-X15a-fix-result.md`，必须包含：
- 改动前后 diff（5 文件）
- 编译结果（命令 + 输出）
- 字节码 `javap -c` 验证截图
- 单元测试：本地 psql 模拟 1 条 ignore_alarm，调 isIgnore(type, defect, line, face) 验证返 true
- **不重启 hik-java**（PM 决策）

---

## 🚫 严禁

- ❌ 重启 hik-java PID 33248（铁则 44）
- ❌ 改 application.yml
- ❌ 改 yk.uploadEnabled / loginEnabled
- ❌ 改 alarm.global-enabled（除非老板指令）
- ❌ 改 sendAlarmMessage（不在本工单范围）
- ❌ 改 PSM 端代码
- ❌ 删 ignore_alarm 表数据

---

## 🎯 PM 验收标准（铁则 40/41）

1. ✅ `isIgnore()` 不再引用 `getEndTime`（用 `.apply("end_time > {0}", nowStr)`）
2. ✅ `getIgnoreDefect()` 同步修复
3. ✅ `IgnoreAlarmDTO` 加 8 字段 + getter/setter
4. ✅ `handleAlarmIgnore()` 真的写库（不再空跑）
5. ✅ `IgnoreAlarm` entity 加 ignoreAll/faceId + getter/setter
6. ✅ javac 编译 0 错 0 警告
7. ✅ 字节码 `javap -c` 显示新逻辑（apply + save 调用）
8. ✅ 报告完整（含 diff + 编译 + 字节码 + 单元测试）

完成在群内回复。
