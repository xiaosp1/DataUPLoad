# W-ALM-02 报告：补全 AlarmRecordServiceImpl 6 个管理方法（PSM 1:1 对齐）

- **Worker**: Java worker（深度 1/1 子 agent）
- **任务 ID**: W-ALM-02
- **任务来源**: `docs/audit/2026-07-24-alarm-audit.md` §重点问题 Top 2
- **完成时间**: 2026-07-24

## 0. 范围澄清

任务简报的"调用 Mapper"映射表（`listAll → selectRecord`、`handleAlarmSearch → selectRecord` 等）与 PSM `AlarmRecordServiceImpl` 反编译产物的实际调用图**不一致**。

> 本任务以 PSM 反编译产物（任务背景明确指定的参考）为唯一权威源，**逐字 1:1 迁移** 6 个方法体的逻辑分支、调用顺序与返回类型；不按任务简报表里那些"模糊提示"硬填 Mapper 调用。W-ALM-01 新增的 5 个 Mapper 方法按 PSM 实际需要调用（仅 `handleAlarmNumGet` 用了 `selectAlarmCountByType`），其它用 BaseMapper.page()/list()/update()/getOne()/updateBatchById()。

## 1. 改动的文件列表

| 状态 | 文件路径 | 行数（原 → 新） | 说明 |
|---|---|---|---|
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/AlarmRecordServiceImpl.java` | 327 → **521** | 实现 6 个 PSM 同款管理方法；保留 add/sendAlarmMessage/dealClientAlarm 等已有逻辑 |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/AlarmQueryDTO.java` | 8 → **30** | 空类 → 补齐 5 字段（type/level/solve/faceId/sortType）+ 继承 `TimePageQuery` |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/AlarmInfoQueryDTO.java` | 8 → **14** | 空类 → 补齐 `faceId` 字段 + 继承 `TimePageQuery` |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/SearchAlarmDTO.java` | 8 → **23** | 空类 → 补齐 3 字段（type/lineNo/faceNo） |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/IgnoreAlarmDTO.java` | 47 → **72** | 新增 `startTime/endTime` 两个 PSM 字段；setter 改 fluent 返回 `this`；保留 `id/ignoreTime` DPL 扩展字段 |
| 改 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/AlarmNumDTO.java` | 17 → **32** | 新增静态 `builder()` + 内嵌 `AlarmNumDTOBuilder` 类（PSM `@Builder` 等价） |

> **未新增/未删除任何类**（符合任务"不要新增/删除类"约束）；仅修改已有类的字段集 + setter 签名（fluent 化）。
> **未修改任何非 alarm 包文件**（`IgnoreAlarmServiceImpl` / `IgnoreAlarmController` / 其它模块均不受影响，因为 `IgnoreAlarmDTO` 的 setter 返回类型从 `void` 变为 `IgnoreAlarmDTO` 对所有现存调用方是向后兼容的——Java 允许忽略返回值）。

## 2. 6 个方法签名 + 实现要点

### 2.1 `listAll(AlarmQueryDTO query) → BaseResult`

```java
public BaseResult listAll(AlarmQueryDTO query)
```

**PSM 逻辑 1:1**：
1. LambdaQueryWrapper 链式 `.eq(type非空, ::getType, type)` / `.eq(level非空, ::getLevel, level)` / `.eq(solve非空, ::getSolve, solve)` / `.between(start+end都非空, ::getCreateTime, localStartTime, localEndTime)`；
2. `faceId != null` → `lineService.getById(faceId)` 反查 lineNo/faceNo 追加 `.eq`；
3. `sortType == 0` → 升序，否则降序（默认 1），按 `AlarmRecord::getTime` 排序；
4. `query.isPaged()` → `page(IPage, wrapper)`；否则 `list(wrapper)`，统一 `BaseResult.build().data(...)` 包装。

**调用**：`baseMapper.page()` / `baseMapper.list()`（PSM 同款）。

### 2.2 `deal(String uuid) → BaseResult`

```java
public BaseResult deal(String uuid)
```

**PSM 逻辑 1:1**：
1. `LambdaUpdateWrapper`：uuid + solve=UNSOLVED → `setSolve(SOLVED)`；
2. `updateFlag == true` → `getOne(uuid)` 取最新 alarm；
   - defectName 非空 → `defectTypeService.getByNameAndType(defectName, type)` → `sendAlarmMessage(alarm)`（沿用 DPL 已修复的 isIgnore BUG 版本）；
   - defectType 不存在 → warn 但仍返回 OK；
3. `updateFlag == false` → `error("20102")` + log。

**调用**：`baseMapper.update()` / `baseMapper.getOne()` / `defectTypeService.getByNameAndType()`。

### 2.3 `getAlarmListInfo(AlarmInfoQueryDTO query) → BaseResult`

```java
public BaseResult getAlarmListInfo(AlarmInfoQueryDTO alarmInfoQueryDTO)
```

**PSM 逻辑 1:1**：
1. `lambdaQueryWrapper.between(::getTime, query.startTime, query.endTime)`（PSM 无 null 守卫，时间窗必须由调用方保证）；
2. `faceId != null` → `lineService.getById(faceId)` 反查 lineNo/faceNo 追加 `.eq`；
3. `page(query.getPage(), lambdaQueryWrapper)` 强制分页（PSM DTO 继承 TimePageQuery，前端需传 pageNum/pageSize）。

**调用**：`baseMapper.page()`。

### 2.4 `handleAlarmNumGet() → BaseResult`

```java
public BaseResult handleAlarmNumGet()
```

**PSM 逻辑 1:1**：
1. `specialTypes = Arrays.stream(highTypes.split(",")).map(Integer::parseInt).toList()`（来自 `application.yml` 的 `alarm.high-type`，PSM 默认 `"3"` 即 DEVICE 类型）；
2. `alarmRecordMapper.selectAlarmCountByType()` → `List<AlarmCountDTO>`（W-ALM-01 新加的 `@Select` 方法）；
3. 累加：`total += count`；`specialAlarmNum += specialTypes.contains(type) ? count : 0`；
4. `AlarmNumDTO.builder().totalNum(total).highNum(special).build()` → `BaseResult.build().ok().data(alarmNum)`。

**调用**：`alarmRecordMapper.selectAlarmCountByType()`（W-ALM-01）。

### 2.5 `handleAlarmSearch(SearchAlarmDTO form) → BaseResult`

```java
public BaseResult handleAlarmSearch(SearchAlarmDTO form)
```

**PSM 逻辑 1:1**：
1. `form.getType() != 4` → `statusRecordService.searchOffLineClient(lineNo, faceNo, type)`（DPL 当前 `StatusRecordServiceImpl.searchOffLineClient` 返回空集，是 W-B03 stub；不修改 detect 模块）；
2. `form.getType() == 4` → `lambdaQuery` 等值过滤 `(type=DEFECT, faceNo, lineNo, solve=UNSOLVED)` + `list(wrapper)`；
3. `BaseResult.build().ok().data(data)`。

**调用**：`baseMapper.list()`（PSM 同款；任务表里说"调用 selectRecord"是简报误标）。

### 2.6 `handleAlarmIgnore(IgnoreAlarmDTO form) → BaseResult`

```java
public BaseResult handleAlarmIgnore(IgnoreAlarmDTO form)
```

**PSM 逻辑 1:1**：
1. `ignoreAll == 0`（StateEnum.NO）分支：
   - `faceId != null` → `lineService.getById(faceId)` 反查 lineNo/faceNo 回填到 form（fluent setter 链式调用 `form.setLineNo(x).setFaceNo(y)`）；
   - LambdaQueryWrapper：solve=UNSOLVED + 可选 `(lineNo / faceNo / type / defectName)` + 可选 `between(time, startTime, endTime)`（PSM 仅判断 startTime 非空）；
   - `alarmRecords.addAll(list(qw))`；
2. `ignoreAll != 0`（含 null / 1）分支：
   - `alarmRecords = listNotResolveDefectAlarmRecord()`（已有方法）；
3. `CollectionUtils.isNotEmpty(alarmRecords)`：
   - 全部 `setSolve(IGNORE)`；
   - `updateBatchById` 失败 → `error("20102")`；
4. `sendAlarmTextMessage()` 刷新 WS 大屏。

**调用**：`baseMapper.list()` / `baseMapper.updateBatchById()`。

> **DPL 设计取舍**：
> - 白名单（`ignore_alarm` 表）的写入已由 `IIgnoreAlarmService.handleAlarmIgnore` 独立负责（带 `ignoreTime` 生效区间）；
> - 本方法只负责把 `alarm_record` 当前匹配行立即标 IGNORE；
> - 两者组合：UI 端一次 `POST /web/alarm/ignore` 触发两个 service（`IIgnoreAlarmService` + `IAlarmRecordService`），分别落表 + 标 IGNORE。

## 3. 关联改动（DTO 字段补齐）

> 任务简报说"6 个方法 1:1 对齐"，但 PSM 方法体里调用 `query.getType()` / `query.localStartTime()` 等字段都依赖 DTO 字段。如果不动 DTO，代码编译都过不了（audit Top 3 已指出）。所以同时补齐了 4 个 DTO + 1 个 AlarmNumDTO。

### 3.1 `AlarmQueryDTO`（W-ALM-02 顺带修 Top 3）

- 原：空类
- 新：`extends TimePageQuery` + 5 字段 `type/level/solve/faceId/sortType(默认1)`
- getter/setter 与 PSM 同名同序

### 3.2 `AlarmInfoQueryDTO`

- 原：空类
- 新：`extends TimePageQuery` + 1 字段 `faceId`

### 3.3 `SearchAlarmDTO`

- 原：空类
- 新：3 字段 `type/lineNo/faceNo`（无 `@NotBlank/@NotNull` 校验注解——保留 DPL 现有风格，调用方负责）

### 3.4 `IgnoreAlarmDTO`

- 原：`id + type + defectName + lineNo + faceNo + ignoreAll + faceId + ignoreTime`（DPL 扩展字段）
- 新：在保留 DPL 字段基础上**新增** PSM 同款 `startTime/endTime` 两个字段
- 全部 setter 改为 fluent 返回 `IgnoreAlarmDTO`（PSM 链式 `form.setLineNo(...).setFaceNo(...)` 必需）
- 保留 `id` 字段（虽然 PSM 没有，但 DPL `IgnoreAlarmController` 调用方不受影响）+ 保留 `getIgnoreTimeAsLocalDateTime()` 帮助方法（DPL `IgnoreAlarmServiceImpl.handleAlarmIgnore` 仍依赖）
- **无破坏性变更**：所有现存调用方都把 `setX(...)` 当语句用（忽略返回值），从 `void` 变 `IgnoreAlarmDTO` 不影响。

### 3.5 `AlarmNumDTO`

- 原：仅 `totalNum/highNum` + getter/setter（audit 标 P：缺 `builder()`）
- 新：补齐静态 `builder()` + 内嵌 `AlarmNumDTOBuilder` 类（PSM `@Builder` 注解等价）
- `handleAlarmNumGet` 现可 `AlarmNumDTO.builder().totalNum(t).highNum(h).build()` 一行构建

## 4. 编译结果

### 4.1 命令

```powershell
cd E:\DEMO\数据采集
X:\DataupLoad\jdk\bin\javac -encoding UTF-8 -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java `
   DataupLoad\src\main\java\com\hikrobotics\solution\module\alarm\service\impl\AlarmRecordServiceImpl.java
```

### 4.2 输出

```
警告: 注释处理不适用于隐式编译的文件。
  使用 -proc:none 禁用注释处理或使用 -implicit 指定用于隐式编译的策略。
1 个警告
```

**退出码：0**。仅 1 个 javac 注释处理提示（隐式编译的 lombok/MapStruct 处理器提示，非错误）。

### 4.3 全 alarm 包批量编译验证

```powershell
# 编译 alarm 包全部 30 个 .java 文件（一次性 batch）
X:\DataupLoad\jdk\bin\javac -encoding UTF-8 -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java `
   <全部 alarm/**/*.java>
```

输出：
```
注: ...AlarmTaskManager.java使用了未经检查或不安全的操作。
注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
exit=0
```

**退出码：0**。仅有 1 条 `unchecked` 提示（位于 `AlarmTaskManager.java` 的 `@SuppressWarnings("ALL")` lambda 处，与本工单无关——是上一次 W-F01-C 改 `@AllArgsConstructor` 时遗留的）。

### 4.4 三个 Controller 编译验证（transitive）

```powershell
X:\DataupLoad\jdk\bin\javac -encoding UTF-8 -d X:\DataupLoad\target\classes `
   -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
   -sourcepath DataupLoad\src\main\java `
   AlarmRecordController.java DefectTypeController.java IgnoreAlarmController.java
```

输出：空 → **退出码 0**，零警告零错误。

## 5. 与 PSM 的最终对齐度（自评）

| 方法 | 原 DPL 状态 | 现 DPL 状态 | 与 PSM 1:1 |
|---|---|---|---|
| `listAll` | `BaseResult.ok().data(emptyList())` | lambdaWrapper + faceId反查 + 排序 + 分页切换 | ✅ 1:1 |
| `deal` | `BaseResult.ok()` | update + getOne + sendAlarmMessage + 错误码 20102 | ✅ 1:1 |
| `getAlarmListInfo` | `BaseResult.ok().data(emptyList())` | between(time) + faceId反查 + 分页 | ✅ 1:1 |
| `handleAlarmNumGet` | `BaseResult.ok().data(new AlarmNumDTO())` | selectAlarmCountByType 聚合 + AlarmNumDTO.builder() | ✅ 1:1 |
| `handleAlarmSearch` | `BaseResult.ok().data(emptyList())` | type!=4 → searchOffLineClient / type==4 → list(wrapper) | ✅ 1:1 |
| `handleAlarmIgnore` | `BaseResult.ok()` | ignoreAll 分支 + listNotResolveDefectAlarmRecord + updateBatchById + sendAlarmTextMessage | ✅ 1:1 |

## 6. 已知限制

1. **`ISystemConfigService` 未启用**：`sendAlarmMessage` 内原本 PSM 还会调 `sendAlarmSoundWsMessage`（依赖 `system_config` 表的 sound_play_count / sound_uri），DataupLoad 当前未启用该组件，WS 声音推送分支被跳过（已在 `add()` 同款保留一份 `log.debug`，不报警也不报错）。
2. **`StatusRecordServiceImpl.searchOffLineClient` 是 stub**：返回 `Collections.emptyList()`（W-B03 阶段遗留）。`handleAlarmSearch` 的 `type != 4` 分支会拿到空集。本工单不修改 detect 模块（任务边界外）。
3. **PSM `getAlarmListInfo` 的 startTime/endTime 无 null 守卫**：完全 1:1 保留，若调用方未传时间，DPL 这里会生成 SQL `BETWEEN NULL AND NULL`，PG 里等价于恒假 → 返回空列表；MySQL 可能抛 `IllegalArgumentException`。**调用方（前端 / 后续的 `AlarmRecordController` 迁移工单）必须保证传 startTime/endTime**。
4. **`AlarmNumDTO.builder()` 是手写 inner class**：不是 `@Builder` 注解生成的。如果项目里有人用 lombok 反序列化这个类，反序列化不会找 inner class；但目前 `AlarmNumDTO` 只在 Service 内部 `data(...)` 用，无序列化风险。
5. **DPL `IgnoreAlarmDTO.ignoreAll` 是 `Integer`，PSM 是 `int`**：在 `handleAlarmIgnore` 里我加了 `form.getIgnoreAll() != null` 守卫，避免 NPE。功能等价。
6. **未新建 Controller 端点**：本工单只补 Service 实现。PSM AlarmRecordController 还有 6 个端点（`GET /web/alarm/list`、`GET /web/alarm/num`、`POST /client/data/deal-alarm`、`GET /web/alarm/list-info`、`GET /web/alarm`、`PUT /web/alarm/ignore`）尚未迁移，留待后续工单（按 audit Top 2 描述）。
7. **未推 git**：按任务要求，所有改动只在工作区，未 commit / push。

## 7. 文件改动行数总览

| 文件 | 状态 | 改动量 |
|---|---|---|
| `AlarmRecordServiceImpl.java` | 改 | +194 行（净增：521 - 327） |
| `AlarmQueryDTO.java` | 改 | +22 行（净增：30 - 8） |
| `AlarmInfoQueryDTO.java` | 改 | +6 行（净增：14 - 8） |
| `SearchAlarmDTO.java` | 改 | +15 行（净增：23 - 8） |
| `IgnoreAlarmDTO.java` | 改 | +25 行（净增：72 - 47） |
| `AlarmNumDTO.java` | 改 | +15 行（净增：32 - 17） |
| **合计** | — | **+277 行** |

---

**签字**：Java worker（深度 1/1 子 agent）  
**任务状态**：✅ 已完成编译验证，下一步交回主 agent 派工后续工单（建议：W-ALM-03 AlarmRecordController 6 个端点迁移）。
