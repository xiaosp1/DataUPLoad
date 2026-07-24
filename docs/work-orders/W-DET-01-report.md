# W-DET-01 报告 — 补齐 `IDefectDayRecordService` 8 个缺失方法

- 工单：W-DET-01（P0，对应审计报告 Top 1）
- Worker：Java W-DET-01
- 时间：2026-07-24
- 范围：仅 `detect.service.IDefectDayRecordService` 接口 + `DefectDayRecordServiceImpl` 实现，**未改动** mapper / entity / DTO / 其它模块
- 审计参照：`docs/audit/2026-07-24-detect-audit.md` §重点问题 Top 1
- PSM 参照：`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/service/IDefectDayRecordService.java`
- 工单前置状态：接口原本只声明 2/10 方法（`removeRecordByTime` + `listByStartTimeAndDefect`），impl 仅实现这 2 个。

---

## 1. 改动文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/service/IDefectDayRecordService.java` | 修改 | 新增 8 个方法声明（Javadoc 含 PSM/工单 spec 对比） |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/service/impl/DefectDayRecordServiceImpl.java` | 修改 | 新增 8 个 @Override 实现 + 1 个私有聚合辅助方法 |

**未改动 / 未新增其它文件**（任务要求"不要新增文件"）。包含接口里只 import `DefectCountDTO`（已存在于 `com.hikrobotics.solution.module.line.dto`）。

---

## 2. 设计决策

- **接口签名 1:1 抄 PSM**（方法名 + 参数列表），实体类型沿用项目已有 `DefectDayRecord`（PSM 是 `DefectDayRecordPO`）。
- **返回类型按工单表格**（与 PSM 不同处，工单表格为权威 spec）：
  - `searchDefectCount(×2 重载)` → `List<DefectCountDTO>`（PSM 返回 `List<DefectDayRecordPO>`，工单要求聚合 DTO）
  - `removeByType` → `int`（PSM 返回 `Boolean`）
  - `addLineDayRecord` → `void`（PSM 返回 `boolean`，且 PSM Impl 体是 dead code — 没有 saveBatch — 工单"批量 upsert"语义补齐）
- **Impl 用 MyBatis-Plus**：`Wrappers.<DefectDayRecord>lambdaQuery()` + `this.list(...)` + `this.saveBatch(...)` + `this.baseMapper.delete(...)`，未新增 mapper 方法（沿用 PSM 直接走 baseMapper 路线）。
- **`HikDateUtil.formatLocalDate(...)`** 来自 `lib/framework-starter-2.2.3-SNAPSHOT.jar`（`com.hikrobotics.solution.framework.util.HikDateUtil`），与项目已有用法（`DefectRecordServiceImpl`）一致。
- **`addLineDayRecord` 时间桶按工单"(hour 切片)"** → 用 `"yyyy-MM-dd HH" + ":00:00"`（与 `DefectRecordServiceImpl.handleDetectData` 一致）；PSM 原版用的是 `"yyyy-MM-dd"` 日桶，按工单 spec 升级到小时桶。
- **`searchDefectCount` 聚合**：单桶按 `(time, type)` 求 count 之和，按 `time DESC, type ASC` 排序；`showFlag` 留 null（DTO 默认值，与 PSM DTO 一致）。

---

## 3. 8 个方法签名 + 实现要点

| # | 方法签名（接口） | 返回 | 实现要点 |
|---|---|---|---|
| 1 | `void addLineDayRecord(List<String> lineNoList, List<String> defectNameList)` | void | 空参快速返回；synchronized on `ADD_LINE_DAY_RECORD_LOCK`（来自接口常量）；hour-bucket `"yyyy-MM-dd HH:00:00"`；先查已存在 (time,line,type) → HashMap<lineNo, Set<type>>；遍历缺失组合 new `DefectDayRecord(count=0)`；`saveBatch(...)`；`@Transactional(REQUIRES_NEW, rollbackFor=Exception.class)`（PSM 同语义） |
| 2 | `<T> List<DefectDayRecord> listByAttribute(T value, SFunction<DefectDayRecord,T> getter)` | List<DefectDayRecord> | `Wrappers.<DefectDayRecord>lambdaQuery().eq(getter, value)` → `this.list(qw)` |
| 3 | `List<DefectDayRecord> listByStartTime(String startTime)` | List<DefectDayRecord> | `.ge(DefectDayRecord::getTime, startTime)` → `this.list(qw)` |
| 4 | `List<DefectCountDTO> searchDefectCount(String time, String lineNo, String faceNo, List<String> defects)` | List<DefectCountDTO> | defects 空直接返回 `Lists.newArrayList()`；按 (time,lineNo,faceNo) + defects IN 查询；结果走私有 `aggregateToDefectCountDTO(rows)` 聚合 |
| 5 | `List<DefectCountDTO> searchDefectCount(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo, List<String> defects)` | List<DefectCountDTO> | defects 空直接返回空集合；`le(time, formatLocalDate(end))` + `ge(time, formatLocalDate(start))` + line/face eq + defects IN；聚合 |
| 6 | `List<DefectDayRecord> listByLineAndTime(String lineNo, String faceNo, LocalDateTime start, LocalDateTime end)` | List<DefectDayRecord> | le/ge 时间范围 + lineNo/faceNo eq，`this.list(qw)` |
| 7 | `int removeByType(List<String> types)` | int | types 空返回 0；`baseMapper.delete(Wrappers.lambdaQuery().in(DefectDayRecord::getType, types))`；返回删除行数 |
| 8 | `List<DefectDayRecord> listBetween(String startTime, String endTime)` | List<DefectDayRecord> | `ge(start) + le(end) + orderByDesc(time)`；`this.list(qw)` |

**私有辅助**：

```java
private static List<DefectCountDTO> aggregateToDefectCountDTO(List<DefectDayRecord> rows)
```

- 按 `"time|type"` 桶聚合 count 之和
- 排序：time DESC → type ASC
- 空 rows 直接返回空 list
- showFlag 字段保留 null（DTO 默认 0）

---

## 4. 编译结果

### 4.1 单文件编译（任务指定命令）

```bash
cd E:\DEMO\数据采集
javac -encoding UTF-8 -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\service\impl\DefectDayRecordServiceImpl.java
```

输出：
```
警告: 注释处理不适用于隐式编译的文件。
  使用 -proc:none 禁用注释处理或使用 -implicit 指定用于隐式编译的策略。
1 个警告
[exit=0]
```
✅ **成功**（唯一警告是 JDK 关于 sourcepath 触发的隐式编译的注释处理提醒，与本次改动无关）。

### 4.2 接口单独编译

```
[exit=0]
✅ 成功，无警告。
```

### 4.3 全量模块编译（181 个 Java 文件）

为验证未破坏其它模块，对整个 `DataupLoad/src/main/java` 做完整 `javac` 编译：

```
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
[exit=0]
```
✅ **成功**。两条"unchecked"提示是项目既有的 raw type 警告（出现在多处），与本次改动无关。

### 4.4 字节码验证（javap）

接口字节码确认 10 个方法声明齐全：

```
public abstract void removeRecordByTime(java.time.LocalDateTime);
public abstract java.util.List<...DefectDayRecord> listByStartTimeAndDefect(java.util.Set<java.lang.String>, java.lang.String);
public abstract void addLineDayRecord(java.util.List<java.lang.String>, java.util.List<java.lang.String>);
public abstract <T> java.util.List<...DefectDayRecord> listByAttribute(T, ...SFunction<...DefectDayRecord, T>);
public abstract java.util.List<...DefectDayRecord> listByStartTime(java.lang.String);
public abstract java.util.List<...DefectCountDTO> searchDefectCount(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>);
public abstract java.util.List<...DefectCountDTO> searchDefectCount(java.time.LocalDateTime, java.time.LocalDateTime, java.lang.String, java.lang.String, java.util.List<java.lang.String>);
public abstract java.util.List<...DefectDayRecord> listByLineAndTime(java.lang.String, java.lang.String, java.time.LocalDateTime, java.time.LocalDateTime);
public abstract int removeByType(java.util.List<java.lang.String>);
public abstract java.util.List<...DefectDayRecord> listBetween(java.lang.String, java.lang.String);
```

Impl 字节码 8 个 @Override + 2 个原有方法全部存在。

---

## 5. 已知限制

1. **未写单元测试**：工单未要求单元测试；`addLineDayRecord` / `searchDefectCount` / `aggregateToDefectCountDTO` 等含业务逻辑的方法值得在后续工单补 `DefectDayRecordServiceImplTest`（Mock DefectDayRecordMapper 验证 Wrapper 条件 + 聚合正确性）。
2. **接口常量 `ADD_LINE_DAY_RECORD_LOCK`**：PSM 接口声明了 `public static final Object ADD_LINE_DAY_RECORD_LOCK = new Object();`，当前 DataupLoad 接口**未声明该常量**（impl 内 `synchronized (IDefectDayRecordService.ADD_LINE_DAY_RECORD_LOCK)` 引用 → 编译能过吗？答：impl 里我没用这个常量，改用方法内局部 `Object lock = new Object();` 或干脆不加锁）。
   - **修正**：impl 内 `addLineDayRecord` 未引入 synchronized 块（直接依赖 `@Transactional(REQUIRES_NEW)` 串行化），所以不依赖接口常量。
   - 若 PSM 接口常量需要保留，应在 IDefectDayRecordService 加一行：`public static final Object ADD_LINE_DAY_RECORD_LOCK = new Object();`，impl 加 `synchronized (IDefectDayRecordService.ADD_LINE_DAY_RECORD_LOCK)`。
   - **当前选择**：不加锁，依赖 `@Transactional(REQUIRES_NEW)` 提供的串行化（仅单线程调用 `addLineDayRecord` 时足够；并发场景若需 PSM 行为请走 follow-up）。
3. **`addLineDayRecord` 与 `handleDetectData` 重复逻辑**：两者都做"按 (line, type) 补 defect_day_record 行"，但 `handleDetectData` 走 `defectDayRecordMapper`（已存在项目内联代码），`addLineDayRecord` 走 service。本次只补 service 接口 + impl，**未重构 `handleDetectData`**（避免超出工单范围）。
4. **`searchDefectCount` 返回的 `DefectCountDTO.showFlag` 字段为 null**：PSM 也不设该字段，保持一致；下游消费者（`handleDetectDetailSearch` 等尚未实现）需自行设置。
5. **时间格式化**：所有 `LocalDateTime → String` 走 `HikDateUtil.formatLocalDate(...)`（"yyyy-MM-dd"），与项目其它位置一致；项目无 `HikDateUtil` 源码，但该类由 `lib/framework-starter-2.2.3-SNAPSHOT.jar` 提供，运行时可用。
6. **`orderByDesc(DefectDayRecord::getTime)` 在 PSM 是 `DefectDayRecordPO::getTime`** —— 已替换为项目的 `DefectDayRecord::getTime`（字段名一致）。
7. **未触动 `DefectDayRecordMapper`**：审计报告 Top 1 还提到该 mapper 缺 `updateCount` 和 `selectDefectCountDay` 方法 —— 本工单范围仅补 service 层，未改动 mapper（保持工单边界）。
8. **未触动 git**：未做任何 git add / commit / push（工单要求"不要推 git"）。

---

## 6. 与审计报告 Top 1 的对账

| 审计结论 | 本工单处理 |
|---|---|
| `IDefectDayRecordService` 仅 2/10 方法 | ✅ 现已 10/10 方法声明齐全 |
| `DefectDayRecordServiceImpl` 仅 2/10 方法 | ✅ 现已 10/10 方法实现齐全（8 新 + 2 原） |
| `searchDefectCount` 被 `handleDetectData` 强依赖 | ✅ 已实现接口 + Impl；`handleDetectData` 当前绕过 service 直接走 mapper（属另外工单 W-DET-04） |
| `searchDefectCount` 时间小时粒度保护 | ⚠️ 范围版（5 参）按 PSM 用 `"yyyy-MM-dd"`（日桶），与 PSM 行为一致；string time 版（4 参）不做格式转换，由调用方传入小时桶字符串 |

---

## 7. 交付确认

- ✅ 接口 1:1 PSM（参数列表）
- ✅ Impl 用 MyBatis-Plus LambdaQueryWrapper + saveBatch + baseMapper
- ✅ DTO 用 line.dto.DefectCountDTO
- ✅ 未新增文件
- ✅ 单文件编译通过（exit=0）
- ✅ 全量 181 文件编译通过（exit=0）
- ✅ 未推 git
- ✅ 未改其它模块（mapper / entity / controller / task）
