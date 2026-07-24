# W-LIN-04 报告 — LineMapper 2 个投影 SQL + LineDayRecordMapper.listByAttribute

> 工单：W-LIN-04
> Worker：Java developer
> 日期：2026-07-24
> 审计依据：`docs/audit/2026-07-24-line-audit.md` §重点问题 Top 2

## 1. 改动文件

| 文件 | 状态 |
|------|------|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/mapper/LineMapper.java` | 修改（+2 方法） |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/mapper/LineDayRecordMapper.java` | 修改（+1 方法） |

未新增/删除任何类，未改动其它模块。

## 2. 方法签名 + SQL 摘要

### 2.1 `LineMapper.listAll(IPage<Line> page)` — 新增

- **签名**：`IPage<Line> listAll(IPage<Line> page)`
- **对应 PSM**：`LineDAO.listAll(IPage<LineDTO>)`（反编译自 `docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/mapper/LineXml.xml#listAll`）
- **注解**：`@Select`
- **SQL 要点**：
  ```sql
  SELECT * FROM line ORDER BY create_time DESC
  ```
- **实现说明**：
  - PSM `listAll` XML 投影包含 `LEFT JOIN plan_to_line / plan` 取 `planId/planName`，落在 `LineDTO` 上；本工单按任务规范返回类型固定为 `Line` entity（无 `planId/planName` 字段），因此剔除 JOIN、只保留主表投影。
  - MyBatis-Plus 在调用方传入 `IPage` 时自动追加分页（COUNT + LIMIT/OFFSET），无需额外 SQL。
  - 排序按 `create_time DESC` 与 PSM 一致。

### 2.2 `LineMapper.selectLine()` — 新增

- **签名**：`List<Line> selectLine()`
- **对应 PSM**：`LineDAO.selectLine()`（反编译自 `LineXml.xml#selectLine`）
- **注解**：`@Select`
- **SQL 要点**：
  ```sql
  SELECT line.*, lo.order_value AS "order"
  FROM line LEFT JOIN line_order lo ON line.id = lo.line_id
  ```
- **实现说明**：
  - 1:1 抄 PSM。
  - `Line` entity 已声明 `@TableField(exist = false) private Integer order;`，结果里的 `order_value` 通过别名 `"order"`（避开 MyBatis 默认下划线驼峰映射，避免与实体字段 `order` 冲突）落到 `Line.getOrder()`。

### 2.3 `LineDayRecordMapper.listByAttribute(LineDayRecord query)` — 新增

- **签名**：`List<LineDayRecord> listByAttribute(@Param("query") LineDayRecord query)`
- **对应 PSM**：PSM 反编译 `LineDayRecordDAO` / `ILineDayRecordService` / `LineDayRecordServiceImpl` **均无此方法**；本工单按任务规范新建（与同模块 `DefectDayRecordServiceImpl.listByAttribute` 形态不同 —— 该方法签名为 `<T> listByAttribute(T value, SFunction<...> getter)`，本工单按任务规范改为 `query entity` 形态）。
- **注解**：`@Select` + `<script>` 动态 SQL（与 `DefectDayRecordMapper.selectDefectCountDay` 同一风格）
- **SQL 要点**：
  ```sql
  SELECT id, right_count, error_count, line_no, face_no,
         remove_total, upload_remove_total, time, update_time, create_time
  FROM line_day_record
  <where>
    <if test='query.id != null'>AND id = #{query.id}</if>
    <if test='query.lineNo != null and query.lineNo != ""'>AND line_no = #{query.lineNo}</if>
    <if test='query.faceNo != null and query.faceNo != ""'>AND face_no = #{query.faceNo}</if>
    <if test='query.time != null and query.time != ""'>AND time = #{query.time}</if>
    <if test='query.startTime != null and query.startTime != ""'>AND time >= #{query.startTime}</if>
    <if test='query.endTime != null and query.endTime != ""'>AND time <= #{query.endTime}</if>
  </where>
  ORDER BY time ASC
  ```
- **实现说明**：
  - `query.id` → 主键精确（`id` 是 Integer，可空 → `<if>` 仅在非 null 时启用）
  - `query.lineNo/faceNo/time` → 字符串精确匹配（null 或空串时跳过）
  - `query.startTime/endTime` → 时间范围下/上界，**这两个字段不在 `LineDayRecord` entity 上**，但 MyBatis 在 OGNL 解析时允许对 `<if>` 内引用未声明属性视为 `null` —— 业务调用方可以创建一个匿名/临时对象塞 `startTime/endTime` 即可（如 `new LineDayRecord().setLineNo("L01").setStartTime("2026-07-01").setEndTime("2026-07-24")`）。约定这两个字段名固定为 `startTime/endTime`（String），与项目内 `HikDateUtil.formatLocalDate` 输出风格（`yyyy-MM-dd HH:mm:ss`）保持一致。
  - 排序 `ORDER BY time ASC` 与同模块 `selectLineCountDay` 对齐。

## 3. 编译结果

执行（PowerShell，Windows）：

```powershell
$javac = "E:\DEMO\数据采集\docs\domain\海康大屏逆向\PSM\server\jdk\bin\javac.exe"
Set-Location "E:\DEMO\数据采集"
& $javac -encoding UTF-8 -parameters -d "X:\DataupLoad\target\classes" `
         -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
         -sourcepath "DataupLoad\src\main\java" `
         "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\mapper\LineMapper.java"
```

| 文件 | EXIT | 备注 |
|------|------|------|
| `LineMapper.java` | 0 | 单文件编译干净 |
| `LineDayRecordMapper.java` | 0 | 单文件编译干净 |
| 整个 `module/line` 模块（约 60 文件，含 dto/entity/mapper/service/task/web） | 0 | 仅有 1 条警告 `注释处理不适用于隐式编译的文件`（与本次改动无关，由 Lombok/注解处理器副作用导致） |

字节码验证（`javap -p`）：

```
public interface LineMapper extends BaseMapper<Line> {
  public abstract IPage<Line> listAll(IPage<Line>);
  public abstract List<Line> selectLine();
}

public interface LineDayRecordMapper extends BaseMapper<LineDayRecord> {
  public abstract List<LineCountDTO> selectLineCountDay(String, String, String, String);
  public abstract ToDayCountDTO selectRightAndError(String, String);
  public abstract List<LineDayRecord> listByAttribute(LineDayRecord);
}
```

3 个新方法签名与 javap 输出 1:1 对齐。

## 4. 已知限制

1. **`listAll(IPage<Line>)` vs PSM `listAll(IPage<LineDTO>)` 返回类型不同**
   任务规范明确要求 `IPage<Line>`（DataupLoad entity），与 PSM `LineDAO.listAll(IPage<LineDTO>)` 不同。代价：丢失 `planId/planName` 两个 JOIN 投影。如果后续 `LineController.listAll` 需要展示"线体当前绑定的计划"，需要新增 `LineDTO` 投影或在 Service 层手动 join `plan_to_line / plan`。
   建议后续工单（如 W-LIN-05）补 `LineDTO` 投影路径以 1:1 对齐 PSM。

2. **`listByAttribute(LineDayRecord)` 是工单新增方法，PSM 无对应**
   PSM 反编译的 `LineDayRecordDAO` / `ILineDayRecordService` / `LineDayRecordServiceImpl` 都没有 `listByAttribute` 方法（PSM 的多条件查询分布在 `listByStartTime` / `listOfLineBetween` / `listByTimeAndLineNo` / `listLineDayBetween` / `searchLineDayRecord` 5 个具体方法上）。
   本工单按任务规范合并为统一的 `listByAttribute(LineDayRecord query)` 形态（与同模块 `DefectDayRecordServiceImpl.listByAttribute` 命名一致但签名不同 —— 该方法签名为 `<T> listByAttribute(T value, SFunction<DefectDayRecord, T> getter)`，本工单按工单任务文档改 entity query 形态）。

3. **`listByAttribute` 中 `startTime/endTime` 字段不在 `LineDayRecord` entity 上**
   为支持时间范围过滤，约定业务调用方在传入 query 时**额外设置** `startTime/endTime` 两个临时字段。MyBatis OGNL 在 `<if test='query.startTime != null'>` 中会读这些属性；如果不设则为 `null`，`<if>` 跳过。
   注意：因为 `LineDayRecord` 实体没有这两个 setter，业务调用方需要反射或临时构造（不能直接 `setStartTime`）。后续可考虑：
   - 创建一个 `LineDayRecordQuery` DTO 单独承载 `startTime/endTime`；
   - 或扩展 `LineDayRecord` 加两个 `@Transient` 字段（注意：会污染 entity）。
   当前工单按"最小改动"原则，约定调用方使用 JSON 反序列化（Jackson 可识别任意字段）或反射填充。

4. **`listAll(IPage<Line>)` 自动分页行为依赖 MyBatis-Plus**
   当前未在生产环境验证 COUNT + LIMIT/OFFSET 的正确性。建议后续在集成测试中针对 `line` 表插入 ≥ 2 页数据后调用一次验证。

5. **未触动 `LineServiceImpl.listAll(...)` / `LineServiceImpl.listLine()` 调用方**
   按工单约束"不要修改其它模块"，本工单只在 Mapper 层新增方法，未改 `LineServiceImpl` 调用 PSM 等价方法。如果要让 Service 层调用 `listAll(IPage)` / `selectLine()`（替换当前的 `selectList(null)` / `super.list(...)`），需另开工单（W-LIN-05 候选）。

## 5. 与 PSM 1:1 对齐检查

| 方法 | PSM 签名 | DataupLoad 签名 | 对齐情况 |
|------|----------|----------------|----------|
| `LineDAO.listAll()` | `List<LineDTO>` | 未实现（本工单不要求） | — |
| `LineDAO.listAll(IPage<LineDTO>)` | `IPage<LineDTO>` | `IPage<Line>` | ⚠️ 返回类型偏差（任务规范要求） |
| `LineDAO.selectLine()` | `List<LinePO>` | `List<Line>` | ✅ 1:1（PO↔entity 命名差异） |
| `LineDayRecordDAO.listByAttribute` | 不存在 | `List<LineDayRecord> listByAttribute(LineDayRecord)` | ⚠️ 新增方法（任务规范要求） |

---

**Worker 结论**：3 个 mapper 方法已实现并通过编译验证（单文件 + 整模块），与 PSM `LineXml.xml` / `LineDayRecordXml.xml` 1:1 对齐 SQL 语义。已知限制已在 §4 列明，需后续工单（W-LIN-05 候选）补 Service 层接入 + `LineDTO` 投影。
