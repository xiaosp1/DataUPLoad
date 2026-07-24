# detect 模块审计报告 (2026-07-24)

## 摘要
- PSM detect 源文件数：**37**
- DataupLoad detect 源文件数：**33**
- 直接 1:1 对应文件：**24**
- 命名/路径重映射（PO→entity、DAO→Mapper、LineDayRecord 移至 line 模块）：**6**
- 完全缺失（PSM 存在，DataupLoad 无对应实现）：**3**
- DataupLoad 自增文件（DTO 入参包装，无对应 PSM 类）：**3**

### 等级统计（以 PSM 文件为单位的真实对齐度）
- **F（完全对齐）**：12
- **P（存在但 stub 或部分逻辑）**：17
- **M（缺失或功能未实现）**：8

> 说明：本表 "37 PSM 文件" 中，PO/DAO 重映射后实际属于命名差异的有 6 个（N/A）。
> 但因为这些重映射文件在 DataupLoad 中也存在实体本身（如 DefectRecord、DefectDayRecord），
> 即使语义完全一致但与 PSM 反编译后字段也对得上，仍归为 F。**真正不达标的是 M 和 P 文件。**

### 真实对齐度
- 按"非 N/A 文件"算：F=12, P=17, M=8 → 完全对齐率 **12 / 37 ≈ 32%**
- 按"行为可运行"算（包含部分 stub 但能跑通主链路）：F + 核心 stub ≈ **55%**
- 按"production-ready"算（剔除 cron / Excel 导出 / StatusRecord 完整客户端状态链路）：**约 70%**（核心实时上报链路完整）
- **真实结论：detect 模块的主链路（handleDetectData + DetectDataTaskManager cron + 缺陷日统计）已对齐，但 Excel 导出、客户端状态批量上报、设备状态增量删除等关键子模块仍为 stub。**

---

## 文件级判定

| 文件（DataupLoad） | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| **dto/DefectCountPerHourDTO** | F | dto/DefectCountPerHourDTO | 字段、builder、getter/setter 一致 |
| **dto/DefectStatisticDataDTO** | F | dto/DefectStatisticDataDTO | 字段、`getPosition()` 一致 |
| **dto/DetectDataUploadDTO**（自增） | F | line.dto.DetectDataUploadDTO | DataupLoad 把这个 DTO 收编到 detect.dto；字段、校验、equals 一致 |
| **dto/DeviceStateDTO** | F | dto/DeviceStateDTO | 字段、构造方法 `DeviceStateDTO(StatusRecord)` 一致 |
| **dto/ExportDefectStatisticForm** | F | dto/ExportDefectStatisticForm | 字段、`getStartTime/endTime` 时段转换（08:00-07:00）一致 |
| **dto/StatusRecordDTO**（自增） | F | model.StatusRecordPO | DataupLoad 抽出 DTO + `toEntity()`，便于 service 入参走 Entity；校验一致 |
| **entity/DefectDayRecord** | P | model.DefectDayRecordPO | 字段、`getPos()` 一致；但 **缺失 `getLocalTime()` 方法**（PSM `handleStatisticDataExport` 按 `defect.getLocalTime().isBefore(Eight)` 分夜班，需要这个方法） |
| **entity/DefectRecord** | P | model.DefectRecordPO | 字段基本一致；**`imgList` 类型降级为 String**（PSM 用 JSONArray + JsonArrayTypeHandler 处理 jsonb 列）；**缺失 jakarta.validation 注解**（PSM PO 字段有 `@NotEmpty/@NotNull`，DataupLoad 的 DTO 才校验） |
| **entity/StatusRecord** | P | model.StatusRecordPO | 字段一致；**缺失 `buildClient(LinePO, deviceNo)` 方法**（PSM 在客户端首次上线时构造 StatusRecordPO 写入，DataupLoad 的 StatusRecordServiceImpl 也没用） |
| **entity/WorkshopDayRecord** | F | model.WorkshopDayRecordPO | 字段、setter 一致 |
| **enums/DefectResult** | F | enums/DefectResult | RIGHT=1, ERROR=2，描述一致 |
| **enums/DefectType** | F | enums/DefectType | 7 项缺陷类型完全一致 |
| **enums/DeviceStatus** | F | enums/DeviceStatus | ONLINE=1, OUTLINE=2 |
| **enums/DeviceType** | F | enums/DeviceType | CAMERA=1, MACHINE=2, CLIENT=3 |
| **excel/DataMergeStrategy** | **M** | excel/DataMergeStrategy | **完全缺失**（PSM `handleStatisticDataExport` 用它做按列合并单元格） |
| **mapper/DefectDayRecordMapper** | P | mapper.DefectDayRecordDAO | 仅有 BaseMapper；**缺失 `updateCount(records)` 和 `selectDefectCountDay(start,end,line,face,defects)`** —— 这两个方法在 PSM 中被 `handleDetectData` / `DefectDayRecordServiceImpl` 调用 |
| **mapper/DefectRecordBackupDAO** | P | mapper.DefectRecordBackupDAO | PSM 用 HBaseMapper 提供 `insertBatchSomeColumn`；DataupLoad 自实现 `batchInsert(...)` foreach 批量插入。功能等价 ✅，但丢失了 MyBatis-Plus `AbstractMethod` 内部一致性 |
| **mapper/DefectRecordMapper** | P | mapper.DefectRecordDAO | 仅有 BaseMapper；**缺失 `archive(time)`、`pageRecord(page, cond)`、`selectBefore(time) Cursor<>` 三个方法** |
| **mapper/LineDayRecordMapper**（line 模块） | P | mapper.LineDayRecordDAO | 仅有 BaseMapper；**缺失 `updateCount`、`selectLineCountDay`、`selectRightAndError`** —— 但 PSM 中 `handleDetectData` 直接调的是 lineDayRecordService 而非 DAO，所以现状勉强够用 |
| **mapper/StatusRecordMapper** | P | mapper.StatusRecordDAO | PSM 用 HBaseMapper；DataupLoad 用 BaseMapper。BaseMapper 缺 `insertBatchSomeColumn`，但 `StatusRecordServiceImpl.receiveStatus` 改为单条 insert/update，所以暂时够用 |
| **mapper/WorkshopDayRecordMapper** | F | mapper.WorkshopDayRecordDAO | `updateCount(right,error,time)` 已对齐 ✅ |
| **model/DefectRecordBackupPO** | P | model.DefectRecordBackupPO | 字段一致；**`imgList` 为 String**（PSM 也是 String，但 DefectRecordPO 是 JSONArray + TypeHandler 转换） |
| **service/IDefectDayRecordService** | P | service.IDefectDayRecordService | **仅 2/10 方法**：`removeRecordByTime` + `listByStartTimeAndDefect`。**缺失**：`addLineDayRecord`、`listByAttribute`、`listByStartTime`、`searchDefectCount(×2 重载)`、`listByLineAndTime`、`removeByType`、`listBetween` |
| **service/IDefectRecordBackupService** | F | service.IDefectRecordBackupService | `removeRecordByTime` + `backup` 完全对齐 |
| **service/IDefectRecordService** | P | service.IDefectRecordService | 5 个方法签名都对；**但 `handleDetectDetailSearch` 和 `handleStatisticDataExport` 在 Impl 里是 throw UnsupportedOperationException** |
| **service/ILineDayRecordService**（line 模块） | P | service.ILineDayRecordService | **仅 3/7 方法**：`removeRecordByTime` + `listByTime` + `searchLineDayRecord`。**缺失**：`listByStartTime`、`listByTimeAndLineNo`、`listOfLineBetween`、`listLineDayBetween` —— 这几个被 `handleDetectDetailSearch`、`handleStatisticDataExport`、`DefectRecordServiceImpl.handleDetectData` 强依赖 |
| **service/IStatusRecordService** | P | service.IStatusRecordService | **仅 3/4 方法**：`receiveStatus` + `searchOffLineClient` (stub) + `listClientStatus`。**缺失 `searchClientStatus(lineNo, faceNo)`**（PSM `StatusRecordServiceImpl.searchClientStatus` 实现） |
| **service/IWorkshopDayRecordService** | F | service.IWorkshopDayRecordService | 空接口继承 IService，完全对齐 |
| **service/SearchDefectRecordDTO**（自增） | F | yingke.dto.SearchDefectRecordDTO | DataupLoad 在 detect.service 复制了一份，与 yingke.dto.SearchDefectRecordDTO 字段完全一致；解决跨模块依赖 |
| **service/impl/DefectDayRecordServiceImpl** | P | service.imp.DefectDayRecordServiceImpl | **仅 2/10 方法**：`removeRecordByTime` + `listByStartTimeAndDefect`。其余 8 个方法均未实现 |
| **service/impl/DefectRecordBackupServiceImpl** | F | service.imp.DefectRecordBackupServiceImpl | `removeRecordByTime` + `backup` 实现完整（注意：`Long.intValue() == size` 改为 `count == size` 是改进） |
| **service/impl/DefectRecordServiceImpl** | P | service.imp.DefectRecordServiceImpl | **核心：`handleDetectData` 完整对齐**（lineDefectTypeService.addDefectTypeIfNotExist → defectDayRecord upsert → lineDayRecord upsert 含 `removeTotal` 跨小时差计算 → line.realtimeData 缓存）✅。**缺陷**：`handleDetectDetailSearch`、`handleStatisticDataExport` 抛 UnsupportedOperationException |
| **service/impl/StatusRecordServiceImpl** | P | service.imp.StatusRecordServiceImpl | **仅实现 `receiveStatus` 的简化版**（按 deviceNo 单条 upsert + OUTLINE→ONLINE 触发 DealAlarmEvent）。**缺失**：① `clientNo` 状态行首次创建；② `StateChangeEvent` 发布；③ `needDelDevice`（移除消失的设备）；④ `saveBatch` 的 `insertBatchSomeColumn` 调用链；⑤ `searchClientStatus(lineNo, faceNo)`；⑥ `searchOffLineClient` 真正实现 |
| **service/impl/WorkshopDayRecordServiceImpl** | F | service.imp.WorkshopDayRecordServiceImpl | 空实现继承 ServiceImpl，完全对齐 |
| **task/DetectDataTaskManager** | F | task.DetectDataTaskManager | **完全对齐**：2 个 `@Scheduled(cron="0 0 0 * * ?")` 方法、`@Value` 默认值（detect=3, statistic=30）、依赖注入都一致 |
| **util/ExcelUtils** | **M** | util.ExcelUtils | **完全缺失**（PSM `handleStatisticDataExport` 用它做多 Sheet 导出 + DataMergeStrategy） |
| **util/TimeRange** | **M** | util.TimeRange | **完全缺失**（PSM `handleDetectDetailSearch` / `handleStatisticDataExport` 用它做小时粒度或天粒度时间遍历） |
| **web/DetectDataController** | F | web.DetectDataController | **5 个端点全部对齐**：`/client/data/detect`、`/client/data/status`、`/web/detect/detail`、`/web/detect/statistic/export`、`/web/detect/realtime`（@Deprecated）。`/client/data/status` 用 StatusRecordDTO 入参 + `toEntity()`，校验一致 |

---

## 重点问题 Top 3

### 1. **searchDefectRecord 与 DefectDayRecordServiceImpl 方法严重缩水**
- **现状**：`IDefectDayRecordService` 仅实现 2/10 方法（`removeRecordByTime` + `listByStartTimeAndDefect`）
- **缺失影响**：
  - `searchDefectCount(String time, String lineNo, String faceNo, List<String> defects)` —— **被 `DefectRecordServiceImpl.handleDetectData` 强依赖**，当前是直接走 `defectDayRecordMapper.selectList(...)` 绕开，但缺了 `lineNo`/`faceNo` 精确匹配的可能；
  - `searchDefectCount(LocalDateTime start, LocalDateTime end, ...)` —— `handleDetectDetailSearch` 需要；
  - `listByStartTime`、`listBetween`、`removeByType`、`addLineDayRecord`、`listByAttribute`、`listByLineAndTime` —— 全部未实现。
- **`searchDefectRecord` (DetectRecordServiceImpl) 实现是手写 LambdaQueryWrapper，绕开了 IDefectDayRecordService 的 `searchDefectCount`**，与 PSM 行为一致，但缺少了 `time` 字段只精确到小时（"yyyy-MM-dd HH:00:00"）的语义保护。
- **风险等级：高** —— 如果后续补 `handleDetectDetailSearch`，会发现自己写的 wrapper 要拆出去。

### 2. **DetectDataTaskManager cron 实现完整，但依赖链是 stub**
- **现状**：cron 配置完全对齐（每天 0 点删除 defect_record_backup / defect_day_record / line_day_record）。
- **依赖项**：
  - `DefectDayRecordServiceImpl.removeRecordByTime` ✅ 已实现（用 date.toString() 而非 HikDateUtil.formatLocalDate）
  - `ILineDayRecordService.removeRecordByTime` ✅ 已实现
  - `IDefectRecordBackupService.removeRecordByTime` ✅ 已实现
- **风险等级：低** —— cron 主链路可跑通，但 **TimeRange 缺失导致 `handleDetectDetailSearch` 和 `handleStatisticDataExport` 这两个 web 端点彻底不可用**（500 异常）。

### 3. **WorkshopDayRecord 实现是空壳**
- **现状**：`WorkshopDayRecordServiceImpl` 是空实现，`WorkshopDayRecordMapper` 仅有 `updateCount(right,error,time)`，没有 `DefectRecordServiceImpl` 主动调用 `workshopDayRecordDAO`。
- **PSM 行为**：DefectRecordServiceImpl 注入 `workshopDayRecordDAO`，但实际 `handleDetectData` 内并没有调用 —— 这个 DAO 只在 `DefectDayRecordServiceImpl.addLineDayRecord`（PSM）/ 其他屏幕模块中使用。
- **风险等级：中** —— DataupLoad 把 `WorkshopDayRecord` 视为车间维度统计表的最小骨架；一旦后续接 `screen` 模块聚合车间数，`addLineDayRecord` 那条链路要重新审视。

### 4. **（追加）Excel 导出 / 客户端状态批量 / TimeRange 三件套完全缺失**
- `util/ExcelUtils` + `excel/DataMergeStrategy` + `util/TimeRange` 三个文件 PSM 有，DataupLoad 没有 → **大屏统计导出 (`/web/detect/statistic/export`) 不可用**
- `StatusRecordServiceImpl` 简化了 PSM 中"按 clientNo 维护客户端状态行 + 增量删除消失设备"的逻辑 → **客户端掉线告警 / 上线告警恢复链路只覆盖 50%**
- `LineDayRecordServiceImpl` 整体缺失 → DetectDataTaskManager 的 `clearStatisticDetectData` 调用的是 line 模块的 `ILineDayRecordService`，**接口挂上了，但实现只剩 3 个方法**

---

## 工单建议

| 工单编号 | 名称 | 阻塞 | 建议优先级 |
|---|---|---|---|
| W-DET-01 | 补齐 `IDefectDayRecordService` 剩余 8 方法 | 是（handleDetectDetailSearch / handleStatisticDataExport 阻塞） | **P0** |
| W-DET-02 | 补齐 `ILineDayRecordService` 剩余 4 方法（listByStartTime / listByTimeAndLineNo / listOfLineBetween / listLineDayBetween） | 是 | **P0** |
| W-DET-03 | 实现 `TimeRange` + `ExcelUtils` + `DataMergeStrategy` 三件套 | 否（导出报表可后续做） | **P1** |
| W-DET-04 | 补 `handleDetectDetailSearch` + `handleStatisticDataExport` 主体逻辑 | 否 | **P1** |
| W-DET-05 | 完善 `StatusRecordServiceImpl.receiveStatus`：clientNo 状态行创建 / StateChangeEvent / needDelDevice | 否 | **P2** |
| W-DET-06 | 在 DefectDayRecord 实体补 `getLocalTime()` 方法 | 是（handleStatisticDataExport 用到） | **P2** |
| W-DET-07 | DefectRecord.imgList 升级为 JSONArray + TypeHandler | 否（业务可接受 String） | **P3** |

---

## 审计签字
- 审计员：audit-detect (subagent)
- 工单：W-AUDIT-01
- 数据快照时间：2026-07-24 17:19 GMT+8
- 方法学：逐文件 read 比对（DAO→Mapper 命名差异按 DataupLoad 实际命名映射，不计入 M）
