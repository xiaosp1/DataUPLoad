# W-DET-04 报告 — 补 `DefectDayRecordMapper.updateCount` + `DefectDayRecord.getLocalTime()`

- 工单：W-DET-04（P2，对应审计报告 Top 1 文件级判定 / entity/DefectDayRecord）
- Worker：Java W-DET-04
- 时间：2026-07-24
- 范围：仅 `detect.mapper.DefectDayRecordMapper` + `detect.entity.DefectDayRecord` 两个文件；**未改动** service / DTO / controller / task / 其它模块
- 审计参照：`docs/audit/2026-07-24-detect-audit.md` §文件级判定 `mapper/DefectDayRecordMapper`（P）+ `entity/DefectDayRecord`（P）
- PSM 参照：
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/mapper/DefectDayRecordDAO.java`
  - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/model/DefectDayRecordPO.java`
  - `docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/module/mapper/DefectDayRecordXml.xml`

---

## 1. 改动文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/mapper/DefectDayRecordMapper.java` | 修改 | 新增 `@Update updateCount(List<DefectDayRecord>)` 方法 + `@Update` import |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/detect/entity/DefectDayRecord.java` | 修改 | 新增 `LocalTime getLocalTime()` 方法 + `HikDateUtil` / `LocalTime` import |

**未改动 / 未新增其它文件**：
- `DefectDayRecordServiceImpl.java`（任务 spec 提到可选 `updateDefectCount(Integer, Integer)` 方法，标注为"可选"且当前模块无 caller；按"不要新增/删除类" + "1:1 对齐 PSM"原则未引入该方法）
- 其它模块（alarm / line / yingke / framework / screen）一律未触
- 未引入新依赖（`HikDateUtil` 已在 `framework-starter-2.2.3-SNAPSHOT.jar` 内）

---

## 2. 关键决策：与任务 spec 表的差异（重要）

任务 spec 表格描述了一个"简化版" `updateCount`，但本工单**严格 1:1 对齐 PSM 反编译产物**（任务硬性要求），因此实际签名 / SQL 与 spec 表有 3 处差异，列举如下：

| 项 | 任务 spec 表描述 | PSM 反编译产物（实际采用） | 选择依据 |
|---|---|---|---|
| `updateCount` 入参 | `@Param("id") Integer id, @Param("count") Integer count` | `@Param("records") List<DefectDayRecordPO>` | 任务硬性要求"1:1 对齐 PSM 反编译产物"，且 W-DET-01 报告已知限制也要求遵循 PSM XML 1:1 |
| `updateCount` 返回 | `int` | `boolean`（DAO）/ `int`（注解实现退化为受影响行数） | 注解 @Update 没法返回 boolean；用 int 表达受影响行数（0/正数均视作"成功"） |
| `updateCount` SQL | `UPDATE defect_day_record SET count=#{count} WHERE id=#{id}` | `UPDATE defect_day_record SET count = count + #{record.count} WHERE time=? AND type=? AND line_no=? AND face_no=?`（foreach separator `;`） | 同上，PSM XML 1:1 |
| `getLocalTime` 返回 | `String` | `LocalTime` | 任务硬性要求"1:1 抄 PSM 同款"；PSM 返回 `LocalTime`（`HikDateUtil.transformTime(String).toLocalTime()`），审计报告 §文件级判定 描述也是"PSM handleStatisticDataExport 按 defect.getLocalTime().isBefore(Eight)"，要 `.isBefore(...)` 必须 `LocalTime` |
| `getLocalTime` 实现 | `this.time.toLocalTime().toString()` | `HikDateUtil.transformTime((String)this.time).toLocalTime()` | spec 表写法把 `this.time` 当 `LocalDateTime` 处理，但实体 `time` 字段是 `String`（与 PSM PO 一致），`String.toLocalTime()` 不存在，编译失败。PSM 写法用 `HikDateUtil.transformTime(String)` 解析，符合字段类型 |

**结论**：以上差异是任务 spec 表与"1:1 对齐 PSM"硬性要求冲突时的取舍，**遵循 PSM**。详见 §6 已知限制 #1。

---

## 3. 方法签名 + SQL

### 3.1 `DefectDayRecordMapper.updateCount`（W-DET-04 新增）

**接口签名**：
```java
@Mapper
public interface DefectDayRecordMapper extends BaseMapper<DefectDayRecord> {
    // ...已有 selectDefectCountDay...

    @Update("<script>" +
            "<foreach collection='records' item='record' index='index' separator=';'>" +
            "  UPDATE defect_day_record SET count = count + #{record.count} " +
            "  WHERE time = #{record.time} " +
            "    AND type = #{record.type} " +
            "    AND line_no = #{record.lineNo} " +
            "    AND face_no = #{record.faceNo}" +
            "</foreach>" +
            "</script>")
    int updateCount(@Param("records") List<DefectDayRecord> records);
}
```

**SQL（与 PSM `DefectDayRecordXml.xml#updateCount` 1:1）**：
```xml
<update id="updateCount" parameterType="java.util.List">
    <foreach collection="records" item="record" index="index" separator=";">
        update defect_day_record set count = count + #{record.count}
        where time = #{record.time}
          and type = #{record.type}
          and line_no = #{record.lineNo}
          and face_no = #{record.faceNo}
    </foreach>
</update>
```

**关键点**：
- `count = count + #{record.count}`（累加语义，不是直接覆盖；与 spec 表"覆盖"语义不同）
- 4 元组匹配 `(time, type, line_no, face_no)`（不是 `id`）
- `<foreach separator=";">` —— 多条 UPDATE 用 `;` 分隔，**依赖 JDBC `allowMultiQueries=true`**（详见 §6 #4）

### 3.2 `DefectDayRecord.getLocalTime`（W-DET-04 新增）

**实现（与 PSM `DefectDayRecordPO.java#getLocalTime` 1:1）**：
```java
public LocalTime getLocalTime() {
    return HikDateUtil.transformTime((String) this.time).toLocalTime();
}
```

**关键点**：
- `time` 字段类型是 `String`（与 PSM PO 一致）
- `HikDateUtil.transformTime(String)` 来自 `framework-starter-2.2.3-SNAPSHOT.jar`：
  ```
  public static java.time.LocalDateTime transformTime(java.lang.String);
  public static java.time.LocalDateTime transformTime(java.lang.String, java.lang.String);
  ```
- 调用方（如 `DefectRecordServiceImpl.handleStatisticDataExport`，未来 W-DET-04 后续工单实现）用法：
  ```java
  final LocalTime Eight = LocalTime.of(8, 0);
  if (defect.getLocalTime().isBefore(Eight)) {
      // 夜班
  }
  ```

---

## 4. 编译结果

### 4.1 单文件编译（任务指定命令）

```bash
cd E:\DEMO\数据采集
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\entity\DefectDayRecord.java
```

**输出**：
```
（无输出）
exit=0
```
✅ **成功**，无警告。

### 4.2 mapper 单独编译

```bash
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java \
      DataupLoad\src\main\java\com\hikrobotics\solution\module\detect\mapper\DefectDayRecordMapper.java
```
✅ exit=0，无警告。

### 4.3 全量项目编译（183 个 Java 文件）

```bash
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
      -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
      -sourcepath DataupLoad\src\main\java @X:\sources.txt
```

**输出**：
```
注: 某些输入文件使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
exit=0
```
✅ **成功**。两条"unchecked"提示是项目既有的 raw type 警告，与本次改动无关。

### 4.4 字节码验证（javap）

**DefectDayRecordMapper** 字节码：
```
public abstract int updateCount(java.util.List<com.hikrobotics.solution.module.detect.entity.DefectDayRecord>);
public abstract java.util.List<...DefectCountDTO> selectDefectCountDay(
    java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>);
```

**DefectDayRecord** 字节码：
```
public java.lang.String getPos();
public java.time.LocalTime getLocalTime();
public java.lang.Integer getId();
...
```

两个新方法签名正确，与 JavaDoc 一致。

---

## 5. 与审计报告 Top 文件的对账

| 审计结论 | 本工单处理 |
|---|---|
| `DefectDayRecordMapper` 缺失 `updateCount` | ✅ 新增 `@Update updateCount(List<DefectDayRecord>)`，SQL 1:1 抄 PSM |
| `DefectDayRecord` 缺失 `getLocalTime()` | ✅ 新增 `LocalTime getLocalTime()`，1:1 抄 PSM（`HikDateUtil.transformTime(...).toLocalTime()`） |

**注**：审计报告说"`DefectDayRecordMapper` 仅有 BaseMapper"，但 W-LIN-01 已新增 `selectDefectCountDay`（同工单已 covered）。本次只补齐 `updateCount`，任务 spec 跳过 `selectDefectCountDay`（已被 W-LIN-01 加过）。

---

## 6. 已知限制

1. **任务 spec 表与 PSM 反编译产物冲突**：spec 表描述了简化版的 `updateCount(id, count)` 和 `this.time.toLocalTime().toString()`，但 PSM 反编译产物是另一套。**本工单严格 1:1 抄 PSM**（任务硬性要求），未遵循 spec 表的简化签名/SQL/返回类型；详见 §2 表。如果业务侧确实需要 spec 表的简化版（按 id 直接覆盖 count），应另开工单作为 DataupLoad 自增方法，**不**覆盖 PSM 1:1。
2. **未补 `updateDefectCount(Integer, Integer)` service 包装方法**：任务 spec 标注为"可选"，当前 `DefectDayRecordServiceImpl` 内无 caller，PSM 1:1 也不暴露该方法（PSM `DefectDayRecordServiceImpl` 直接调 DAO 不经 service 包装）；按"不要新增/删除类" + "1:1 对齐 PSM"原则不引入。后续若 `DefectRecordServiceImpl.handleDetectData` 走 service 路径，需在工单 W-DET-04 后续或独立工单添加。
3. **`getLocalTime()` NPE 风险**：PSM 同款未做 null 检查；`time == null` 时 `HikDateUtil.transformTime(null)` 行为依赖 PSM 实现。当前 DataupLoad 沿用 PSM 1:1 行为不另行保护。如调用方存在 `time` 为 null 的情形（比如新建未保存的 entity），需在调用方判空或在本方法加 `time == null ? null : HikDateUtil.transformTime(time).toLocalTime()` 保护（**这将偏离 PSM 1:1，需工单单独评审**）。
4. **`updateCount` 依赖 `allowMultiQueries=true`**：PSM XML 的 `<foreach separator=";">` 是 MySQL 多语句语法；当前 DataupLoad 默认 datasource（PostgreSQL，按 postgresql-42.5.4.jar + HikariCP-5.0.1）URL 未显式开启 `allowMultiQueries`，且 PG 默认禁止多语句。**实际部署需**：
   - 若用 MySQL：在 `spring.datasource.dynami...` URL 末尾追加 `&allowMultiQueries=true`
   - 若用 PostgreSQL：默认不支持 `;` 分隔的多语句；可改写 mapper 用 `<foreach>` 走单条 update + 多条拼接（语义不再 1:1），或批量重写为 update from values 形式
   - 单条 records 调用 `updateCount`（size==1）可正常工作；多条需要上述配置
5. **`updateCount` 返回 int 与 PSM boolean 不完全等价**：PSM DAO 返回 `boolean`（MyBatis 在没有抛异常时即视为成功，rows 数量多少不影响 boolean）；本工单注解实现返回 `int`（受影响行数）。调用方若按"是否成功"判断，应将 `result >= 0` 视为成功（实际 MyBatis 在没抛异常时 result 也不会是负数）。
6. **未触动 service 层 / cron / handleDetectData**：当前 `DefectRecordServiceImpl.handleDetectData` 直接走 `defectDayRecordMapper`（项目内联代码，绕过 service），所以本工单新增的 `updateCount` mapper 方法当前**无 caller**。后续工单若要让 `handleDetectData` 走 service 路径，应：
   - 在 `IDefectDayRecordService` + `DefectDayRecordServiceImpl` 加 `int updateDefectCount(List<DefectDayRecord> records)` 包装方法（impl 调 `baseMapper.updateCount(records)`）
   - 修改 `handleDetectData` 调用 service 方法
7. **未触动 git**：未做任何 `git add / commit / push`（任务要求"不要推 git"）。改动已留在 working tree，待主 agent 审阅 + 整合到 batch commit。
8. **未补单元测试**：任务未要求；但 `DefectDayRecordMapper.updateCount`（foreach 多语句行为 + separator `;` 在 PG/MySQL 差异）和 `DefectDayRecord.getLocalTime()`（`HikDateUtil.transformTime` 的多种输入串格式）值得在后续工单补 Mock/SQL 单测。
9. **`getLocalTime` 抛异常链**：若 `time` 不是 `HikDateUtil` 支持的格式（如 `"yyyy-MM-dd"` 无时间部分、或空串 `""`），`transformTime` 会抛 `DateTimeParseException`；调用方需保证 `time` 完整为 `"yyyy-MM-dd HH:mm:ss"` 或其它 `HikDateUtil` 支持的格式。

---

## 7. 交付确认

- ✅ `DefectDayRecordMapper` 新增 `updateCount(List<DefectDayRecord>)` 方法（@Update 注解，1:1 抄 PSM XML）
- ✅ `DefectDayRecord` 新增 `LocalTime getLocalTime()` 方法（1:1 抄 PSM PO）
- ✅ 任务指定单文件编译命令通过（exit=0）
- ✅ mapper 单独编译通过（exit=0）
- ✅ 全量 183 文件编译通过（exit=0）
- ✅ 字节码验证两个新方法签名正确
- ✅ 未新增/删除类
- ✅ 未触动其它模块（alarm / line / yingke / framework / screen）
- ✅ 未推 git
- ⚠️ 与任务 spec 表存在 3 处差异（已 §2 / §6 说明，遵循"1:1 对齐 PSM"硬性要求）
