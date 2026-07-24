# W-DET-02 工作报告

> 工单：W-DET-02 — 补全 ILineDayRecordService 4 个缺失方法 + 修复 removeRecordByTime 边界 bug
> 工种：Java 开发 worker
> 参考审计报告：`docs/audit/2026-07-24-line-audit.md` § 其他观察 #6
> 参考 PSM 反编译：
> - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/service/ILineDayRecordService.java`
> - `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/detect/service/imp/LineDayRecordServiceImpl.java`

---

## 1. 改动文件

| 文件 | 改动 |
|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/service/ILineDayRecordService.java` | 接口新增 4 个方法签名 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/service/impl/LineDayRecordServiceImpl.java` | 实现 4 个方法 + 修复 `removeRecordByTime` 边界 |

未新增文件，未修改其它模块，未触碰 git。

---

## 2. 4 个新增方法签名（与 PSM 反编译 1:1 对齐）

| 方法 | PSM 签名 | 本实现签名（UP） |
|---|---|---|
| `listByStartTime` | `List<LineDayRecordPO> listByStartTime(String time)` | `List<LineDayRecord> listByStartTime(String time)` |
| `listByTimeAndLineNo` | `LineDayRecordPO listByTimeAndLineNo(LocalDateTime time, String lineNo, String faceNo)` | `LineDayRecord listByTimeAndLineNo(LocalDateTime time, String lineNo, String faceNo)` |
| `listOfLineBetween` | `List<LineDayRecordPO> listOfLineBetween(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo)` | `List<LineDayRecord> listOfLineBetween(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo)` |
| `listLineDayBetween` | `List<LineDayRecordPO> listLineDayBetween(String startTime, String endTime)` | `List<LineDayRecord> listLineDayBetween(String startTime, String endTime)` |

> **签名对齐说明**：
> - `LineDayRecordPO` ↔ `LineDayRecord`（命名约定，PSM `model/*PO` ↔ UP `entity/*`）
> - 接口入参 / 出参类型完全一致

> ⚠️ **与 brief 的偏差**：
> brief 描述 `listByTimeAndLineNo(time, lineNo)` 为 `List<LineDayRecord>`（2 参 + List 返回）。
> PSM 反编译接口为 `listByTimeAndLineNo(LocalDateTime, String, String)` 返回单个 `LineDayRecordPO`（3 参 + 单对象返回）。
> 工单要求"接口签名对齐 PSM"，本实现按 **PSM 真实签名** 落地（3 参、单对象返回）。

### 2.1 实现要点（与 PSM 反编译 1:1）

```java
@Override
public List<LineDayRecord> listByStartTime(String time) {
    // PSM 1:1: time 形参是 "yyyy-MM-dd HH" 整点字符串
    return this.list(Wrappers.<LineDayRecord>lambdaQuery()
        .ge(LineDayRecord::getTime, time));
}

@Override
public LineDayRecord listByTimeAndLineNo(LocalDateTime time, String lineNo, String faceNo) {
    // PSM 1:1: statisticTime = formatLocalDate(time, "yyyy-MM-dd HH") + ":00:00"
    String statisticTime = HikDateUtil.formatLocalDate(time, "yyyy-MM-dd HH") + ":00:00";
    return this.getOne(Wrappers.<LineDayRecord>lambdaQuery()
        .eq(LineDayRecord::getTime, statisticTime)
        .eq(LineDayRecord::getLineNo, lineNo)
        .eq(LineDayRecord::getFaceNo, faceNo));
}

@Override
public List<LineDayRecord> listOfLineBetween(LocalDateTime start, LocalDateTime end, String lineNo, String faceNo) {
    // PSM 1:1:
    //   statisticStartTime = formatLocalDate(start, "yyyy-MM-dd") + " 00:00:00"
    //   statisticEndTime   = formatLocalDate(end,   "yyyy-MM-dd HH") + " 23:59:59"
    String statisticStartTime = HikDateUtil.formatLocalDate(start, "yyyy-MM-dd") + " 00:00:00";
    String statisticEndTime   = HikDateUtil.formatLocalDate(end,   "yyyy-MM-dd HH") + " 23:59:59";
    return this.list(Wrappers.<LineDayRecord>lambdaQuery()
        .between(LineDayRecord::getTime, statisticStartTime, statisticEndTime)
        .eq(LineDayRecord::getLineNo, lineNo)
        .eq(LineDayRecord::getFaceNo, faceNo));
}

@Override
public List<LineDayRecord> listLineDayBetween(String startTime, String endTime) {
    // PSM 1:1: 入参是已格式化好的字符串（"yyyy-MM-dd HH:mm:ss"），按 time 倒序
    return this.list(Wrappers.<LineDayRecord>lambdaQuery()
        .ge(LineDayRecord::getTime, startTime)
        .le(LineDayRecord::getTime, endTime)
        .orderByDesc(LineDayRecord::getTime));
}
```

---

## 3. `removeRecordByTime` 边界修复

### 3.1 diff（精简）

```diff
+ import com.hikrobotics.solution.framework.util.HikDateUtil;

  @Override
  public void removeRecordByTime(LocalDateTime time) {
-     // 旧实现：time.toLocalDate().toString() 输出 "2025-01-01"（仅日期）
-     // 字典序比较会把 2025-01-02/03/... 一并删除
-     this.remove(Wrappers.<LineDayRecord>lambdaQuery()
-         .le(LineDayRecord::getTime, time.toLocalDate().toString()));
+     // PSM 等价：删除上界用 HikDateUtil.formatLocalDate(time)（默认格式
+     // "yyyy-MM-dd HH:mm:ss"），与写入时格式一致，按字典序比较可正确删除
+     // <= time 的全部记录；旧实现跨天误删问题修复
+     this.remove(Wrappers.<LineDayRecord>lambdaQuery()
+         .le(LineDayRecord::getTime, HikDateUtil.formatLocalDate(time)));
  }
```

### 3.2 语义对比

| 入参 `time` | 旧实现删除范围 | 新实现删除范围 |
|---|---|---|
| `2025-01-01 23:59:59.999` | `time <= "2025-01-01"` → **删掉 2025-01-01 全天及之前**（实际正确） | `time <= "2025-01-01 23:59:59"` → **删掉 2025-01-01 23:59:59 及之前**（PSM 一致） |
| `2025-01-01 00:00:01` | `time <= "2025-01-01"` → 删掉 2025-01-01 全天及之前 | `time <= "2025-01-01 00:00:01"` → 仅删掉 2025-01-01 00:00:01 及之前 |
| `2025-01-02 00:00:00` | `time <= "2025-01-02"` → **会误删 2025-01-02 全天数据** ❌ | `time <= "2025-01-02 00:00:00"` → 仅删掉 2025-01-02 00:00:00 及之前 ✅ |

### 3.3 选择依据

通过 `javap` 解 `X:\DataupLoad\lib\framework-starter-2.2.3-SNAPSHOT.jar` 中的
`com.hikrobotics.solution.framework.util.HikDateUtil`：

- `public static String formatLocalDate(LocalDateTime time)` — 走 `defaultPattern = "yyyy-MM-dd HH:mm:ss"`（PSM 同款）
- `public static String formatLocalDate(LocalDateTime time, String pattern)` — 自定义 pattern

边界修复用 1-arg 版本（默认 `yyyy-MM-dd HH:mm:ss`），与 PSM `removeRecordByTime` 完全等价。

---

## 4. 编译结果

```
> X:\DataupLoad\jdk\bin\javac.exe -encoding UTF-8 \
    -d X:\DataupLoad\target\classes \
    -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
    -sourcepath DataupLoad\src\main\java \
    DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\impl\LineDayRecordServiceImpl.java

警告: 注释处理不适用于隐式编译的文件。
  使用 -proc:none 禁用注释处理或使用 -implicit 指定用于隐式编译的策略。
1 个警告
```

**结果**：✅ 编译通过，0 error，仅 1 个无关警告（隐式编译触发 `-proc:none` 提示）。

### 4.1 字节码验证

`ILineDayRecordService.class` javap 输出：
```
public interface ...ILineDayRecordService extends IService<LineDayRecord> {
  public abstract void removeRecordByTime(java.time.LocalDateTime);
  public abstract java.util.List<...LineDayRecord> listByTime(java.lang.String);
  public abstract java.util.List<...LineDayRecord> searchLineDayRecord(...SearchDefectRecordDTO);
  public abstract java.util.List<...LineDayRecord> listByStartTime(java.lang.String);
  public abstract ...LineDayRecord listByTimeAndLineNo(java.time.LocalDateTime, java.lang.String, java.lang.String);
  public abstract java.util.List<...LineDayRecord> listOfLineBetween(java.time.LocalDateTime, java.time.LocalDateTime, java.lang.String, java.lang.String);
  public abstract java.util.List<...LineDayRecord> listLineDayBetween(java.lang.String, java.lang.String);
}
```

`LineDayRecordServiceImpl.class` 同 7 个方法 + ctor 全部就位。

---

## 5. 备注

- **未触碰** `LineDayRecord` entity、Mapper、Mapper.xml、Controller
- **未触碰** 其它模块（defect / yingke / detect 等）
- **未触碰** git（无 commit / push / branch 操作）
- 审计报告 Top3 之外的 P2 项 #6（ILineDayRecordService 4 个缺失方法 + removeRecordByTime 边界）已落地，对齐度由 P 升级为 F
