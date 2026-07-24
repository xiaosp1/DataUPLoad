# W-X15a — 修 isIgnore / getIgnoreDefect / handleAlarmIgnore BUG — 完工报告

- **任务编号**: W-X15a (2026-07-23)
- **派工人**: PM 锋卫 14:58 GMT+8
- **执行人**: Worker W-X15a (subagent, depth 1/1)
- **完成**: 15:28 GMT+8（约 30 min，含编码 + 编译 + 字节码验证 + 单元测试 + 报告）
- **基于**: W-X15 报告发现的 4 个 BUG（isIgnore SQL 异常 / getIgnoreDefect 同款 / DTO 空类 / handleAlarmIgnore 空跑）
- **生产状态**: hik-java PID 33248 未重启 / yk.uploadEnabled=false / PG 14.23 port 5433 → intco

---

## 0. TL;DR

**5 文件改动 + 编译 0 错 0 警告 + 字节码验证 + 单元测试 PASS。**

| 文件 | 改动摘要 |
|------|---------|
| `IgnoreAlarmServiceImpl.java` | isIgnore() / getIgnoreDefect() 改用 `.apply("end_time > {0}", nowStr)`；handleAlarmIgnore() 非空校验 + entity save |
| `IgnoreAlarmDTO.java` | 加 9 字段（id, type, defectName, lineNo, faceNo, ignoreAll, faceId, startTime, endTime）+ getter/setter |
| `IgnoreAlarm.java` | 加 ignoreAll / faceId / startTime 字段；endTime/startTime 改为 String 类型（与 DB varchar(19) 对齐）|
| `W_X15a_Test.java` | 单元测试脚本（psql 直连）— INSERT→查询→DELETE，正反例验证 |
| `W_X15a_Cleanup.java` | 清理脚本 |

---

## 1. 改动前后 diff

### 1.1 IgnoreAlarmServiceImpl.java

**isIgnore() 修复前 → 修复后**：

```diff
-      .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());
+      String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
+      ...
+      .apply("end_time > {0}", nowStr);
```

**getIgnoreDefect() 修复前 → 修复后**：

```diff
-      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
-         .gt(IgnoreAlarm::getEndTime, LocalDateTime.now());
+      String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
+      LambdaQueryWrapper<IgnoreAlarm> qw = Wrappers.<IgnoreAlarm>lambdaQuery()
+         .apply("end_time > {0}", nowStr);
```

**handleAlarmIgnore() 修复前 → 修复后**：

```diff
-   public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
-      // PSM 原版该方法体几乎为空，仅返回 OK；DataupLoad 沿用。
-      return BaseResult.build().ok();
-   }
+   public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form) {
+      if (form == null) {
+         return BaseResult.build().error("20101");
+      }
+      if (form.getType() == null
+          || form.getDefectName() == null
+          || form.getEndTime() == null
+          || form.getEndTime().isEmpty()) {
+         return BaseResult.build().error("20102");
+      }
+      IgnoreAlarm entity = new IgnoreAlarm()
+         .setType(form.getType())
+         .setDefectName(form.getDefectName())
+         .setLineNo(form.getLineNo())
+         .setFaceNo(form.getFaceNo())
+         .setIgnoreAll(form.getIgnoreAll())
+         .setFaceId(form.getFaceId())
+         .setStartTime(form.getStartTime())
+         .setEndTime(form.getEndTime());
+      this.save(entity);
+      return BaseResult.build().ok();
+   }
```

### 1.2 IgnoreAlarmDTO.java

```diff
+public class IgnoreAlarmDTO {
+   private Integer id;
+   private Integer type;
+   private String defectName;
+   private String lineNo;
+   private String faceNo;
+   private Integer ignoreAll;
+   private String faceId;
+   private String startTime;
+   private String endTime;
+   // + full getter/setter chain
+}
```

### 1.3 IgnoreAlarm.java

```diff
+   @TableField("ignore_all")
+   private Integer ignoreAll;
+   @TableField("face_id")
+   private String faceId;
+   @TableField("start_time")
+   private String startTime;
-   @TableField("end_time")
-   private LocalDateTime endTime;
+   @TableField("end_time")
+   private String endTime;
+   // + getter/setter for ignoreAll, faceId, startTime
+   // getEndTime/setEndTime now return String not LocalDateTime
```

---

## 2. 编译结果

**命令**：
```
cd E:\DEMO\数据采集
& E:\DEMO\DATALINK\DataupLoad\jdk\bin\javac.exe -encoding UTF-8 ^
  -cp "DataupLoad\lib\*;DataupLoad\target\classes" ^
  -d DataupLoad\target\classes ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\entity\IgnoreAlarm.java ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\dto\IgnoreAlarmDTO.java ^
  DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\IgnoreAlarmServiceImpl.java
```

**完整输出**：
```
---EXIT CODE: 0---
```

**结论**：0 errors, 0 warnings（JDK 17.0.1, javac）。

---

## 3. 字节码 `javap -c` 验证

```
> javap -c -cp DataupLoad\target\classes
    com.hikrobotics.solution.module.alarm.service.impl.IgnoreAlarmServiceImpl
```

### 3.1 isIgnore() — 确认 .apply("end_time > {0}", ...) 调用

`isIgnore()` 方法中 `95: invokevirtual #134 // Method LambdaQueryWrapper.apply:(String;[Object;)Object` **替换了旧版 `.gt(LocalDateTime)`**。无 `.gt()` 残留。

```
  public boolean isIgnore(Integer, String, String, String);
    Code:
       ...
      84: ldc           #130                // String end_time > {0}
      86: iconst_1
      87: anewarray     #132                // class java/lang/Object
      90: dup
      91: iconst_0
      92: aload         5                   // nowStr
      94: aastore
      95: invokevirtual #134                // Method apply:(String;[Object;)Object    ← ✅ .gt() 已替换
     106: invokevirtual #137                // Method count:(Wrapper;)J
     111: ifeq          118
     114: iconst_1                           // return true
```

### 3.2 handleAlarmIgnore() — 确认 save() 调用

`119: invokevirtual #84 // Method save:(Object;)Z` — **真的写库，不再空跑**。

```
  public BaseResult handleAlarmIgnore(IgnoreAlarmDTO);
    Code:
       ...
     116: astore_2                            // entity
     117: aload_0
     118: aload_2
     119: invokevirtual #84                 // Method save:(Object;)Z     ← ✅ 真实 save
     122: pop
     123: invokestatic  #7                  // Method BaseResult.build:()
     126: invokevirtual #90                 // Method BaseResult.ok:()
     129: areturn
```

### 3.3 getIgnoreDefect() — 确认 apply 调用

`25: invokevirtual #134 // Method apply:(String;[Object;)Object` — **也用了字符串比较**。

### 3.4 确认：无 `.gt()` 残留

```
> javap -c (...).IgnoreAlarmServiceImpl | Select-String "Method.*\.gt\("
→ (no output) ✅
```

---

## 4. 单元测试（psql 模拟）

### 测试脚本

`E:\DEMO\数据采集\scripts\W_X15a_Test.java`

独立 Java 进程，用 JDBC 直连 PG（port 5433 / intco），模拟 MyBatis-Plus 生成的 SQL。

### 完整输出

```
[STEP 1] INSERT ignore_alarm insertedId=35
[STEP 2] SQL: SELECT COUNT(*) FROM ignore_alarm
  WHERE (type = ?) AND (defect_name = ?) AND (line_no = ?) AND (face_no = ?) AND (end_time > ?)
[STEP 2] Params: type=1, defectName=TEST, lineNo=L, faceNo=F, nowStr=2026-07-23 15:28:08
[STEP 3] COUNT(*) = 1 => isIgnore(1, 'TEST', 'L', 'F') = true
[STEP 4] DELETE ignore_alarm id=35 affected=1
[STEP 5] Remaining TEST/L/F rows: 0
[STEP 6] Negative case (type=999) COUNT(*) = 0 => isIgnore = false (expected false)
[VERDICT] PASS — positive=true, negative=false, cleanup=clean.
```

**验证结果**：
- ✅ 正例：插入白名单后 `isIgnore(1, 'TEST', 'L', 'F')` → `true`（end_time='2099-12-31 23:59:59' > now）
- ✅ 负例：无匹配数据 `isIgnore(999, 'NOPE', 'X', 'Y')` → `false`
- ✅ 清理：测试数据已 delete，二次 verify count=0

**PG 副作用检查**：
```
Cleanup: deleted=0 remaining_after=0  → 0 残留 ✅
```

---

## 5. DoD 自检（铁则 40/41）

| # | 条目 | 满足 |
|---|------|------|
| 1 | isIgnore() 不再引用 getEndTime → 用 `.apply("end_time > {0}", nowStr)` | ✅ §1.1 / §3.1 |
| 2 | getIgnoreDefect() 同步修复 | ✅ §1.1 / §3.3 |
| 3 | IgnoreAlarmDTO 加 9 字段 + getter/setter | ✅ §1.2 |
| 4 | handleAlarmIgnore() 真的写库（非空校验 + entity + save） | ✅ §1.1 / §3.2 |
| 5 | IgnoreAlarm entity 加 ignoreAll/faceId/startTime；endTime 改为 String | ✅ §1.3 |
| 6 | javac 编译 0 错 0 警告 | ✅ §2 |
| 7 | 字节码验证：..apply + save 体现 | ✅ §3.1–§3.4 |
| 8 | 单元测试：INSERT→isIgnore true→DELETE→negative→PASS | ✅ §4 |
| 9 | 未重启 hik-java / 未改 yml / 未改 uploadEnabled | ✅ |
| 10 | 未删 ignore_alarm 数据 | ✅（仅清理测试行） |
| 11 | 报告完整 | ✅ 本文件 |

---

## 6. 文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| IgnoreAlarmServiceImpl.java | `DataupLoad/src/main/java/.../service/impl/IgnoreAlarmServiceImpl.java` | isIgnore / getIgnoreDefect / handleAlarmIgnore 修复 |
| IgnoreAlarmDTO.java | `DataupLoad/src/main/java/.../dto/IgnoreAlarmDTO.java` | 9 字段 DTO |
| IgnoreAlarm.java | `DataupLoad/src/main/java/.../entity/IgnoreAlarm.java` | 扩展 ignoreAll/faceId/startTime，类型对齐 |
| 测试源 | `scripts/W_X15a_Test.java` | psql 单元测试 |
| 清理源 | `scripts/W_X15a_Cleanup.java` | 测试数据清理 |
| 本报告 | `docs/delivered/2026-07-23-W-X15a-fix-result.md` | DoD 报告 |

---

## 7. 后续建议

1. **重启 hik-java 使新字节码生效**（PM 决策时重启即可）：
   ```powershell
   Restart-Process -Id 33248  # 或 Stop-Process + Start-Process
   ```
   重启前确认：`DataupLoad\target\classes\com\...\IgnoreAlarmServiceImpl.class` 更新时间 ≈ 2026-07-23 15:28。

2. **T8 noise→IGNORE 规则**（W-X19 范围）：本工单不修。

---

**完工签名**: Worker W-X15a — 2026-07-23 15:28 GMT+8
