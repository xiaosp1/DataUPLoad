# W-DET-05a 报告 — detect/util/TimeRange 游标类 + TimePattern 枚举

**完成时间**: 2026-07-24 20:15
**执行人**: Java worker (subagent)
**优先级**: P0 — W-LIN-01 已知限制 #1 的前置依赖
**参考**: `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/util/TimeRange.java`

## 改动文件汇总

| 文件 | 行数 | 改动类型 | 备注 |
|---|---:|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/TimeRange.java` | 96 | **新建** | PSM 1:1 抄，含 `TimeRange` 游标类 + `TimeRange.TimePattern` 内部枚举 |

合计 **1 个新建文件**，未修改任何其它文件（与任务约束一致）。

## 类结构

```java
package com.hikrobotics.solution.module.detect.util;

public class TimeRange {
    public enum TimePattern {
        YYYY_MM_DD("yyyy-MM-dd"),  // ordinal 0, plusDays
        MM_DD("MM-dd"),            // ordinal 1, plusDays
        YYYY_MM("yyyy-MM"),        // ordinal 2, plusMonths
        HH("yyyy-MM-dd HH");       // ordinal 3, plusHours
    }

    private final TimePattern pattern;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private LocalDateTime current;          // mutable 游标

    public TimeRange(LocalDateTime start, LocalDateTime end, TimePattern pattern);
    public String getPattern();             // 委托 pattern.getDesc()
    public boolean hasNext();               // current.isBefore(end)
    public LocalDateTime next();            // 返回 current 后按 pattern 步进
    public void init();                     // current = start（游标复用入口）
    public LocalDateTime getStart();
    public LocalDateTime getEnd();
}
```

## 关键方法

| 方法 | 实现 | 备注 |
|---|---|---|
| 构造函数 | `this.current = start` | PSM 字段顺序 `pattern → end → start → current` |
| `hasNext()` | `current.isBefore(end)` | 半开区间 [start, end)，end 本身不消费 |
| `next()` | `result = current;` 然后按 pattern 步进 current | 返回**当前值**后才推进，与典型 `Iterator.next()` 语义一致 |
| `init()` | `current = start` | PSM LineServiceImpl#planPanel 三段循环复用同一实例（line/alarm/defect）时 reset 用 |

## 关键设计决定

### 1. PSM 1:1 抄 — 优先级最高

任务约束明确写"1:1 抄 PSM 反编译产物"，所以字段修饰符（`final start/end/pattern`、非 `final current`）、方法签名、`switch` 行为（包括 case 4 无 `break` 的最后 fall-through 等价语义）全部对齐 PSM。

任务描述里的类结构示例（字段 `hasNext` + 方法 `setHasNext` + `next()` 返回 `TimeRange`）与 PSM 反编译产物**不一致**，但任务约束的"1:1 抄 PSM"是硬约束；以 PSM 为准。

### 2. TimePattern 枚举源顺序推导

PSM 反编译产物 `next()` 内的 switch：

```java
switch (1.$SwitchMap$...$TimePattern[this.pattern.ordinal()]) {
    case 1:                          // ordinal 0 → plusDays
    case 2:                          // ordinal 1 → plusDays (fall-through 同体)
        this.current = this.current.plusDays(1L);
        break;
    case 3:                          // ordinal 2 → plusMonths
        this.current = this.current.plusMonths(1L);
        break;
    case 4:                          // ordinal 3 → plusHours
        this.current = this.current.plusHours(1L);
}
```

javac 合成的 `$SwitchMap[ordinal] = ordinal + 1`，所以 4 个 case 必然对应 4 个 enum 常量。

PSM caller 使用证据（grep 全局）：
| 引用点 | 常量 | 实际步进语义 |
|---|---|---|
| `LineServiceImpl#planPanel` line 310 | `YYYY_MM_DD` | plusDays（开始/结束 = beginOfDay/endOfDay） |
| `DefectRecordServiceImpl#exportToExcel` line 254 | `YYYY_MM_DD` | plusDays |
| `StateChangeServiceImpl` line 135 | `MM_DD` | plusDays（`startTime.atStartOfDay()` ~ `endTime.atStartOfDay().plusDays(1)`） |
| `DefectRecordServiceImpl#statisticByHour` line 217 | `HH` | plusHours |

只有 `YYYY_MM` 无 caller（因为 plusMonths 维度没人调用），但 PSM 仍把它的 ordinal 放在 2（即第三个），所以必须存在。

**推断的 PSM 源顺序**（按 PSM switch-case 1:1 还原）：
```java
public enum TimePattern {
    YYYY_MM_DD,   // ordinal 0 → case 1 → plusDays
    MM_DD,        // ordinal 1 → case 2 → plusDays (fall-through)
    YYYY_MM,      // ordinal 2 → case 3 → plusMonths
    HH            // ordinal 3 → case 4 → plusHours
}
```

注：ordinal 0 / 1 的具体先后顺序仅影响 enum 常量名映射，对运行时行为完全等价（都是 plusDays）。本工单按 PSM 命名约定（先全名 `YYYY_MM_DD` 后简称 `MM_DD`）摆放。

### 3. `getDesc()` 取值

枚举的 `getDesc()` 返回值用于 `HikDateUtil.formatLocalDate(LocalDateTime, String)`，对照 framework `HikDateUtil.class` 静态字段：

| 枚举常量 | `getDesc()` | 依据 |
|---|---|---|
| `YYYY_MM_DD` | `"yyyy-MM-dd"` | LineServiceImpl planPanel line 312 显式 `HikDateUtil.formatLocalDate(range.next(), range.getPattern())`，结果作为 line_day_record.time 的 key；与 `HikDateUtil.simplePattern` 静态字段值一致 |
| `MM_DD` | `"MM-dd"` | StateChangeServiceImpl 用 `TimePattern.MM_DD` 迭代日维度，对应 `LocalDateTime.of(time.toLocalDate(), eight/twenty)` 构造 StateStatisticPO.statisticTime，故 desc 应是月日粒度 |
| `YYYY_MM` | `"yyyy-MM"` | 与 `HikDateUtil.YEAR_MONTH` 静态字段值一致 |
| `HH` | `"yyyy-MM-dd HH"` | DefectRecordServiceImpl 用 `TimePattern.HH` 迭代小时，但 formatter 走单参数 `formatLocalDate(current)` = `defaultPattern = "yyyy-MM-dd HH:mm:ss"`；命名上 hour 模式应输出到小时粒度，故 desc 选 `"yyyy-MM-dd HH"`（与任务描述里 `YYYY_MM_DD_HH` 命名语义对齐） |

## 编译结果

```
$ cd E:\DEMO\数据采集 && javac -encoding UTF-8 -parameters \
    -d X:\DataupLoad\target\classes \
    -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
    -sourcepath DataupLoad/src/main/java \
    DataupLoad/src/main/java/com/hikrobotics/solution\module\detect\util\TimeRange.java
→ exit 0
```

`javap` 验证产物含全部目标签名：

```
$ javap -p .../detect/util/TimeRange.class
public class com.hikrobotics.solution.module.detect.util.TimeRange {
  private final com.hikrobotics.solution.module.detect.util.TimeRange$TimePattern pattern;
  private final java.time.LocalDateTime start;
  private final java.time.LocalDateTime end;
  private java.time.LocalDateTime current;
  public com.hikrobotics.solution.module.detect.util.TimeRange(...);
  public java.lang.String getPattern();
  public boolean hasNext();
  public java.time.LocalDateTime next();
  public void init();
  public java.time.LocalDateTime getStart();
  public java.time.LocalDateTime getEnd();
}

$ javap -p .../detect/util/TimeRange\$TimePattern.class
public final class ...TimeRange$TimePattern extends java.lang.Enum<...TimeRange$TimePattern> {
  public static final ...TimeRange$TimePattern YYYY_MM_DD;
  public static final ...TimeRange$TimePattern MM_DD;
  public static final ...TimeRange$TimePattern YYYY_MM;
  public static final ...TimeRange$TimePattern HH;
  public static ...TimeRange$TimePattern[] values();
  public static ...TimeRange$TimePattern valueOf(java.lang.String);
  public java.lang.String getDesc();
}
```

✅ **成功** — `javac` exit 0，`javap` 验证 TimeRange + TimeRange$TimePattern 双产物均符合 PSM。

### 全量 `compile.bat` 验证

```
$ cd E:\DEMO\数据采集 && cmd /c compile.bat
javac exit code: 0
```

✅ 全量 `DataupLoad/src/main/java/**/*.java` 编译 0 errors，无 TimeRange 相关警告。
注：先期一次编译出现 4 个 `DataMergeStrategy.java` "找不到符号 STRING/NUMERIC/BOOLEAN/FORMULA" 错误，是 stale `.class` 缓存（target/classes 残留上一轮 `TimeRange` 编译前的 DataMergeStrategy 旧产物）触发的增量分析异常；删除 `target/classes/com/hikrobotics/solution/module/detect/util/TimeRange*.class` 后重新编译干净通过。**该问题与本工单无关**，是 framework 已有 POI 3.17 兼容代码 (`switch (cell.getCellType())` + `case STRING:`) 的预存问题（POI 3.17 的 `Cell.getCellType()` 返回 `int` 而非 `CellType` enum），待后续 W-DET-05c 修复。

## 运行时验证（smoke test）

为确认 `next()` / `hasNext()` / `init()` 的 PSM 语义正确性，临时写了一个 `TimeRangeSmokeTest.java` 在 `X:\tmp\` 下：

| 用例 | 期望 | 实际 |
|---|---|---|
| YYYY_MM_DD `[2026-01-15 00:00, 2026-01-18 00:00)` | 3 次 next: Jan 15/16/17 | ✅ Jan 15/16/17 |
| `init()` 后再次遍历 | hasNext=true, next=Jan 15 | ✅ |
| HH `[2026-01-15 10:00, 2026-01-15 13:00)` | 3 次 next: 10/11/12 点 | ✅ 10/11/12 点 |
| YYYY_MM `[2026-01-01, 2026-04-01)` | 3 次 next: Jan/Feb/Mar | ✅ Jan/Feb/Mar |
| MM_DD `[2026-01-15 00:00, 2026-01-18 00:00)` | 3 次 next: Jan 15/16/17（同 YYYY_MM_DD） | ✅ Jan 15/16/17 |
| 4 个 pattern 的 `getDesc()` | `yyyy-MM-dd` / `MM-dd` / `yyyy-MM` / `yyyy-MM-dd HH` | ✅ 全部一致 |

测试通过后已删除临时文件。

## 已知限制 / 差异说明

1. **任务描述的类结构示例与 PSM 不一致**
   任务描述给的类结构（字段 `hasNext`、方法 `setHasNext`、`next()` 返回 `TimeRange`）是"游标 builder"风格，而 PSM 是"mutable iterator"风格。本工单以 PSM 为准（任务约束"1:1 抄 PSM"），后续 W-DET-05b/c 替换 LineServiceImpl 循环时直接调 `range.next()` 拿 `LocalDateTime` 即可。

2. **任务描述的 enum 常量名与 PSM caller 用法不一致**
   任务描述列出 `YYYY_MM_DD_HH, YYYY_MM_DD, YYYY_MM`（3 个），但 PSM switch 有 4 个 case（必然对应 4 个常量），且 PSM caller 实际使用 `HH, MM_DD, YYYY_MM_DD`。本工单按 PSM 命名为 `YYYY_MM_DD, MM_DD, YYYY_MM, HH`，并将任务描述里的 `YYYY_MM_DD_HH` 解读为"HH 的 desc 字符串 `yyyy-MM-dd HH` 的可读名字"（参见上面 `getDesc()` 取值表的 HH 行说明）。

3. **`YYYY_MM_DD` 与 `MM_DD` 运行时等价**
   两个常量都映射到 `plusDays(1)`，仅 `getDesc()` 不同。W-DET-05b 替换 LineServiceImpl#planPanel 时只能用 `YYYY_MM_DD`（与 PSM planPanel 调用一致），不可改用 `MM_DD`，否则 `HikDateUtil.formatLocalDate` 输出格式会变成 `01-15` 而非 `2026-01-15`，破坏 line_day_record.time 的 Map key。

4. **未实现的方法（明确出本工单范围）**
   - `setHasNext(boolean)` / `setStart()` / `setEnd()` / `setPattern()` 等 PSM 也没有的 setter（本工单仅 1:1 抄 PSM 公开方法）
   - `equals()` / `hashCode()` / `toString()`（PSM 也没有，本工单不新增）
   - `LocalDateTime getCurrent()`（PSM 没有公开 current getter；外部只能通过 next() 拿到）

5. **不与 framework `TimeRangeUtil` 冲突**
   framework 的 `com.hikrobotics.solution.framework.util.TimeRangeUtil`（包名不同，类名也不同 `TimeRangeUtil` 而非 `TimeRange`）是 `static List<TimeRange> getHourRange/getDayRange/getMonthRange(...)` 风格，返回 `List<TimeRangeUtil$TimeRange>`（record 风格）。本工单新建的 `detect/util/TimeRange` 是 PSM 1:1 的 mutable iterator 风格，两个类可以共存，使用时各自 import 不同包即可。

6. **不修改 LineServiceImpl.planPanel**
   替换 day-step 内联循环为 `TimeRange` 游标属于 W-DET-05b 范畴，本工单仅落 PSM 类本身，不动调用方代码（与任务约束"不要修改其它文件"一致）。

## 自检清单

- [x] 新建文件路径与任务一致 `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/util/TimeRange.java`
- [x] 类结构 1:1 对齐 PSM 反编译产物（字段 final 性、方法签名、switch-case 行为）
- [x] TimePattern 枚举源顺序符合 PSM switch-case 推导（4 个常量、HH = ordinal 3 = plusHours）
- [x] 任务指定 javac 命令 exit 0
- [x] `javap` 验证产物含 `TimeRange` + `TimeRange$TimePattern` 全部目标签名
- [x] 全量 `compile.bat` exit 0（无 TimeRange 相关警告）
- [x] smoke test 5 个用例全部通过
- [x] 未修改任何其它文件（与任务约束一致）
- [x] 未推 git（与任务约束一致）
