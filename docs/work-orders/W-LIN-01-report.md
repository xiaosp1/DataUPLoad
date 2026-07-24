# W-LIN-01 报告 — LineServiceImpl 8 个核心业务方法 + bean 注入

**完成时间**: 2026-07-24 18:25
**执行人**: Java worker (subagent)
**优先级**: P0 — audit 2026-07-24 Top 1

## 改动文件汇总

| 文件 | 行数 | 改动类型 | 备注 |
|---|---:|---|---|
| `DataupLoad/.../line/service/ILineService.java` | 108 | 修改 | 接口补齐 7 个新方法（add/modify/delete/bindPlan/switchPlan/planPanel/planStatus） |
| `DataupLoad/.../line/service/impl/LineServiceImpl.java` | 557 | 重写 | 8 个核心业务方法 + 12 个 bean 注入 |
| `DataupLoad/.../line/mapper/LineDayRecordMapper.java` | 69 | 修改 | 补齐 PSM `selectLineCountDay` / `selectRightAndError` 两个 SQL 注解 |
| `DataupLoad/.../detect/mapper/DefectDayRecordMapper.java` | 72 | 修改 | 补齐 PSM `selectDefectCountDay`（含 `<script>` foreach 内联） |
| `DataupLoad/.../detect/service/IStatusRecordService.java` | 51 | 修改 | 接口补齐 `searchClientStatus(lineNo, faceNo)` |
| `DataupLoad/.../detect/service/impl/StatusRecordServiceImpl.java` | 111 | 修改 | 实现 `searchClientStatus`（PSM 1:1 抄 StatusRecordServiceImpl） |

合计 6 个文件，1 个重写（LineServiceImpl），5 个修改。

## 8 个方法签名 + 实现要点

| # | 方法签名 | 实现要点 |
|---|---|---|
| 1 | `BaseResult add(LineBodyDTO)` | (lineNo, faceNo) 唯一校验 → `BeanUtil.copyProperties(dto, Line.class)` → 自动 `clientNo = lineNo + "-" + faceNo` → save → `lineOrderService.addLineOrder([id])` |
| 2 | `BaseResult modify(LineUpdateDTO)` | 按 id 查 line → 不存在返回 error → (lineNo, faceNo) 排除自身的唯一校验 → DTO 拷贝 → 自动设置 clientNo → `updateById` |
| 3 | `BaseResult delete(Integer)` | 按 id 查 line → 不存在 error 20204 → 查 status_record.type=CLIENT → ONLINE 则 error 20208 → `removeById` → 若有 clientStatusData，删 status_record 并触发 `alarmRecordService.dealClientAlarm(DISCONNECT)` → `lineOrderService.removeByLineId(id)`（@Transactional） |
| 4 | `BaseResult bindPlan(LinePlanBindDTO)` | 查 plan_to_line 当前列表 → 排序后相等 → no-op → 找当前 ENABLE 的 plan（currentPlanId）→ 若不在新 planIds 则 error 20205 → 清空旧 plan_to_line → 批量插入（新 id == currentPlanId 的 status=ENABLE）→ `CommonMethod.sendPlanChange` WebSocket 广播 |
| 5 | `BaseResult switchPlan(LinePlanSwitchDTO)` | 查当前 ENABLE plan → 查目标 plan → 目标不在 error 20207 → 目标 == 当前 → no-op → 旧 ENABLE → DISABLE → 目标 → ENABLE → `CommonMethod.sendPlanChange` 广播 |
| 6 | `BaseResult planPanel(LinePanelQueryDTO)` | 按 faceId 查 line → 不存在 error 20204 → `localStart = beginOfDay(form.localStartTime())` / `localEnd = endOfDay(form.localEndTime())` → 4 组聚合：(a) `lineCountDTOS`：`lineDayRecordDAO.selectLineCountDay` 按 (time) 累加 + `LocalDateTimeUtil` day 循环补 0 槽位；(b) `defectCountDTOS`：按 `lineDefectTypeService.listIfShowEnable` 限定 type，按 (type, time) 累加 + 补 0 槽位；(c) `alarmCountDTOS`：`alarmRecordDAO.selectAlarmCountDay` + 补 0 槽位；(d) `statusRecordPOS`：status_record 按 lineNo+faceNo 全设备状态；(e) `toDayCountDTO`：`lineDayRecordDAO.selectRightAndError` + `calPercentage` |
| 7 | `BaseResult planStatus(LinePanelQueryDTO)` | 按 faceId 查 line → 存在则返回该 line/face 全部 status_record；不存在 error 20204 |
| 8 | `void init()` | `@PostConstruct`：`lineOrderService.count() == 0` 且 `this.list()` 非空 → `lineOrderService.addLineOrder(lineIds)` |

## 12 个 bean 注入（1:1 抄 PSM）

| 字段 | 类型 | PSM 字段 | 备注 |
|---|---|---|---|
| `lineDefectTypeService` | `ILineDefectTypeService` | `lineDefectTypeService` | W-B03 已注入，复用 |
| `lineOrderService` | `ILineOrderService` | `lineOrderService` | W-B03 已注入，复用 |
| `planToLineService` | `PlanToLineServiceImpl` | `planToLineService` | PSM 用 `PlanToLineService`（@Service 类），DataupLoad 改成其实现类 |
| `planToLineDAO` | `PlanToLineMapper` | `planToLineDAO` | PSM DAO → DataupLoad Mapper |
| `webSocketHandler` | `WebSocketHandler` | `webSocketHandler` | framework-starter 注入 |
| `iStatusRecordService` | `IStatusRecordService` | `iStatusRecordService` | W-B03 已注入，新增 `searchClientStatus` |
| `statusRecordDAO` | `StatusRecordMapper` | `statusRecordDAO` | PSM DAO → DataupLoad Mapper |
| `alarmRecordService` | `AlarmRecordServiceImpl` | `alarmRecordService` | 包名 `module.alarm.service.impl`（PSM 用 `.imp`） |
| `lineDayRecordDAO` | `LineDayRecordMapper` | `lineDayRecordDAO` | PSM DAO → DataupLoad Mapper |
| `defectDayRecordDAO` | `DefectDayRecordMapper` | `defectDayRecordDAO` | PSM DAO → DataupLoad Mapper |
| `alarmRecordDAO` | `AlarmRecordMapper` | `alarmRecordDAO` | PSM DAO → DataupLoad Mapper |
| `defectDayRecordService` | `IDefectDayRecordService` | `defectDayRecordService` | 沿用 W-DET-01 接口 |

PSM 还有 `LineDAO lineDAO` 第 13 个注入 — DataupLoad 沿用 `ServiceImpl.baseMapper`（类型 `LineMapper`），等价语义，未冗余注入。

## 依赖文件改动详情

### LineDayRecordMapper（新增 2 方法）

```java
// selectLineCountDay — 按 line/face + 时间范围聚合每日产量/不良
@Select("SELECT (right_count + error_count) AS count, time, error_count " +
        "FROM line_day_record " +
        "WHERE time >= #{startTime} AND time <= #{endTime} " +
        "  AND line_no = #{lineNo} AND face_no = #{faceNo} " +
        "ORDER BY time")
List<LineCountDTO> selectLineCountDay(...)

// selectRightAndError — 当日正/次品聚合
@Select("SELECT COALESCE(SUM(right_count), 0) AS right_count, " +
        "       COALESCE(SUM(error_count), 0) AS error_count " +
        "FROM line_day_record " +
        "WHERE TIME >= TO_CHAR(NOW(), 'yyyy-MM-dd 00:00:00') " +
        "  AND TIME <= TO_CHAR(NOW(), 'yyyy-MM-dd 23:59:59') " +
        "  AND line_no = #{lineNo} AND face_no = #{faceNo} " +
        "GROUP BY line_no")
ToDayCountDTO selectRightAndError(...)
```

SQL 1:1 抄自 `psm-decompiled/.../LineDayRecordDAO.xml`。

### DefectDayRecordMapper（新增 1 方法）

```java
// selectDefectCountDay — 按时间范围 + line/face + 缺陷名集合查询
@Select("<script>...<if test='defects != null and defects.size() != 0'>" +
        "AND type IN <foreach ...>" +
        "</script>")
List<DefectCountDTO> selectDefectCountDay(...)
```

SQL 1:1 抄自 `psm-decompiled/.../DefectDayRecordDAO.xml`，`<if test>` 分支用 MyBatis 注解 `<script>` 内联。

### IStatusRecordService / StatusRecordServiceImpl（新增 searchClientStatus）

```java
// 接口
StatusRecord searchClientStatus(String lineNo, String faceNo);

// 实现（PSM 1:1）
@Override
public StatusRecord searchClientStatus(String lineNo, String faceNo) {
    LambdaQueryWrapper<StatusRecord> qw = Wrappers.<StatusRecord>lambdaQuery()
        .eq(StatusRecord::getLineNo, lineNo)
        .eq(StatusRecord::getFaceNo, faceNo)
        .eq(StatusRecord::getType, DeviceType.CLIENT.getValue());
    return this.baseMapper.selectOne(qw);
}
```

`delete` 依赖此接口做客户端在线校验；保持 W-B03 receiveStatus + W-X30b DealAlarmEvent 原有逻辑不变。

## 编译结果

```
$ cd E:\DEMO\数据采集 && cmd /c compile.bat
javac exit code: 0
```

✅ **成功** — 全量 `DataupLoad/src/main/java/**/*.java` 编译 0 errors，仅有 javac 隐式编译注解处理警告（不影响产物）。

针对性编译验证：
```
$ javac -encoding UTF-8 -d X:\DataupLoad\target\classes \
    -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
    -sourcepath DataupLoad/src/main/java \
    DataupLoad/src/main/java/com/hikrobotics/solution/module/line/service/impl/LineServiceImpl.java
→ exit 0
```

`javap` 验证 LineServiceImpl 编译产物含全部 8 个方法签名：
- `public BaseResult add(LineBodyDTO)`
- `public BaseResult modify(LineUpdateDTO)`
- `public BaseResult delete(Integer)`
- `public BaseResult bindPlan(LinePlanBindDTO)`
- `public BaseResult switchPlan(LinePlanSwitchDTO)`
- `public BaseResult planPanel(LinePanelQueryDTO)`
- `public BaseResult planStatus(LinePanelQueryDTO)`
- `public void init()`

## 已知限制 / 差异说明

1. **`TimeRange` 内联 day-step 循环（不新增类）**
   PSM `detect/util/TimeRange.java`（含 `TimeRange` 游标类 + `TimeRange$TimePattern` 内部枚举）在 DataupLoad 不存在（约束"不要新增/删除类"）。DataupLoad 框架 `framework.util.TimeRangeUtil` API 完全不一样（返回 `List<TimeRange>` record，没有游标）。三处循环（`planPanel` 的 line/alarm/defect 各一段）改用：
   ```java
   LocalDateTime cursor = localStart;
   while (cursor.isBefore(localEnd) || cursor.isEqual(localEnd)) {
       String time = HikDateUtil.formatLocalDate(cursor, HikDateUtil.simplePattern);
       ...
       cursor = cursor.plusDays(1);
   }
   ```
   等价语义（PSM `TimeRange.hasNext() = current.isBefore(end)`，PSM `end = endOfDay(...) = 23:59:59.999999999`，next() 返回 current 后 plusDays；展开后语义与上面循环一致）。

2. **`PlanToLineService` 接口 → Impl 类**
   PSM 反编译产物里 `PlanToLineService` 是 `@Service` 注解的 `ServiceImpl<PlanToLineDAO, PlanToLinePO>` 实体类（不是接口），DataupLoad 已重命名为 `PlanToLineServiceImpl`。注入类型改为 `PlanToLineServiceImpl`，但 `@Autowired` 按类型解析不受影响。

3. **包名 `.imp` → `.impl`**
   PSM 反编译包路径 `com.hikrobotics.solution.module.line.service.imp` 与 `module.alarm.service.imp`（用 `imp` 是反编译产物的源 jar 包名，与工程约定不一致）。DataupLoad 沿用工程标准 `.impl`（`module.line.service.impl` / `module.alarm.service.impl`）。

4. **`init()` 不进 ILineService 接口**
   PSM `ILineService` 也不含 `init()`；该方法仅在 impl 上声明 `@PostConstruct`，由 Spring 容器在依赖注入完成后回调，签名 `public void init()`。

5. **DataupLoad 改造未涉及实体类型一致性**
   所有 PSM 的 `LinePO` / `PlanToLinePO` / `StatusRecordPO` / `LineDefectTypePO` 在 DataupLoad 都有同名 / 同字段对应实体：`Line` / `PlanToLine` / `StatusRecord` / `LineDefectType`（包名 `line.entity`，W-B03 已统一）。本工单沿用 DataupLoad 实体，无新实体。

6. **`LinePanelDTO.statusRecordPOS` 字段名**
   沿用 PSM 风格 `List<StatusRecord>`（PSM 是 `List<StatusRecordPO>`，DataupLoad 实体改名后类型同步替换），字段名保持 `statusRecordPOS`（PSM 历史遗留命名，DataupLoad 工单保留）。

7. **`AlarmRecordServiceImpl` 注入 vs. `IAlarmRecordService` 接口**
   PSM 直接注入 `AlarmRecordServiceImpl`（实现在 impl 子包），而非接口。DataupLoad 也保留此写法 — 因为 `dealClientAlarm(lineNo, faceNo, alarmReason)` 是 W-X30b 补到 AlarmRecordServiceImpl 类的方法，接口 `IAlarmRecordService` 暂未声明。

8. **未实现的方法（明确出本工单范围）**
   PSM 接口还有 `lineGroup()` / `chgLineOrder(List<ChgLineOrderDTO>)` / `handleLineTreeSearch()` / `listByLineNo(List<String>)` 4 个方法，audit 列为 P2，本工单（Top 1 P0）**未实现**，待后续工单处理。
   备注：`handleLineTreeSearch` 涉及 `LineTreeItemDTO(LinePO)` 构造器，DataupLoad 实体为 `Line`，需给 `LineTreeItemDTO` 增加 `Line` 重载构造器或走 `BeanUtil.copyProperties`，是另一个独立工单的范畴。

## 自检清单

- [x] 8 个核心业务方法 1:1 对齐 PSM
- [x] 12 个 bean 注入 1:1 抄 PSM（LineDAO 冗余去掉）
- [x] DTO 全部复用 DataupLoad 已有类型（不新增类）
- [x] 编译 `javac` exit 0
- [x] `javap` 验证产物含全部目标签名
- [x] 修改的依赖文件（mapper / IStatusRecordService / StatusRecordServiceImpl）只追加，不改既有功能
