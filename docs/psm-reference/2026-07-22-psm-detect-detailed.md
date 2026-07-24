# PSM detect 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ P0 已归档
**优先级**: 🔴 P0（涨库根因 + retention 金标准）

---

## 1. 业务定位

### 1.1 解决什么问题

detect 模块是 PSM 整个系统的**数据汇聚核心**：

- **接收检测数据**：客户端（边缘相机/PLC）通过 `POST /client/data/detect` 上传检测结果（含 NG 缺陷计数 + 实时剔除数）
- **维护 6 张表**：
  - `defect_record`（原始缺陷记录，已被 W-A9 删除，对应 EdgeHost 不需要）
  - `defect_record_backup`（**V1.6 retention 金标准**，`detectRetentionTime=3` 天）
  - `defect_day_record`（按小时分组的缺陷统计，`statisticDataRetentionTime=30` 天）
  - `line_day_record`（按小时的产线产出/良率/剔除统计）
  - `status_record`（设备在线/离线状态机）
  - `workshop_day_record`（车间级日统计，目前空实现）
- **提供统计查询**：每小时缺陷详情、按时间范围分页、Excel 导出（白班/夜班汇总）
- **驱动实时看板**：将实时数据写入 `line.realtime_data` JSON 字段，供 screen 模块聚合

### 1.2 与其他模块的依赖关系

```
detect ──→ defect (ILineDefectTypeService)        # 缺陷类型与产线绑定
detect ──→ line   (ILineService / LineDAO)         # 产线查询 + realtime_data 缓存
detect ──→ alarm  (AlarmRecordServiceImpl)         # 客户端掉线 → 触发 DealAlarmEvent
detect ──→ line   (StateChangeEvent)                # 设备状态变化事件
detect ──→ screen (DefectNumberDTO)                 # 被 screen 引用
detect ──→ yingke (SearchDefectRecordDTO)          # 鹰科查询接口
```

---

## 2. 类清单（37 个 java + 4 个 XML）

### 2.1 dto/ (4)
| 类 | 字节 | 责任 |
|---|---|---|
| `DefectCountPerHourDTO` | 中 | 每小时缺陷明细 + 剔除数 + Builder |
| `DefectStatisticDataDTO` | 小 | 统计数据结构 |
| `DeviceStateDTO` | 小 | 设备状态 DTO（含 lineNo/faceNo/deviceNo/status） |
| `ExportDefectStatisticForm` | 中 | Excel 导出表单（含白班/夜班 shift 计算） |

### 2.2 enums/ (4)
| 枚举 | 值 | 备注 |
|---|---|---|
| `DefectResult` | RIGHT(1) / ERROR(2) | 良品/次品 |
| `DefectType` | 7 类 | 底面破损/侧面破损/侧面破损Big/Small/侧面脏污/二次料/未脱模 |
| `DeviceStatus` | ONLINE(1) / OUTLINE(2) | 设备状态 |
| `DeviceType` | CAMERA(1) / MACHINE(2) / CLIENT(3) | 设备类型 |

### 2.3 excel/ (1)
| 类 | 责任 |
|---|---|
| `DataMergeStrategy` (+1 inner) | EasyExcel 单元格合并策略（线别/剔除数列合并） |

### 2.4 mapper/ (6)
| DAO | 关键方法 | XML |
|---|---|---|
| `DefectRecordDAO` | `archive(LocalDateTime)`、`pageRecord(Page, DefectQueryDTO)`、`selectBefore(LocalDateTime)` Cursor | `DefectRecordXml.xml` |
| `DefectRecordBackupDAO` | 继承 HBaseMapper | `DefectRecordBackupMapper.xml`（空 resultMap） |
| `DefectDayRecordDAO` | `updateCount(List)`、`selectDefectCountDay(...)` | `DefectDayRecordXml.xml` |
| `LineDayRecordDAO` | `updateCount(List)`、`selectLineCountDay(...)`、`selectRightAndError(...)` | `LineDayRecordXml.xml` |
| `StatusRecordDAO` | 默认 CRUD | `StatusRecordXml.xml`（空） |
| `WorkshopDayRecordDAO` | 默认 CRUD | `WorkshopDayRecordMapper.xml`（空） |

### 2.5 model/ (6)
| PO | 表 | 字段 |
|---|---|---|
| `DefectRecordPO` | defect_record | id/lineNo/faceNo/gloveNo/result/defectType/exceptFlag/imgList(JSONArray)/time/updateTime/createTime |
| `DefectRecordBackupPO` | defect_record_backup | 同上（imgList 改为 String，已删除） |
| `DefectDayRecordPO` | defect_day_record | id/count/time(lineNo/faceNo)/type |
| `LineDayRecordPO` | line_day_record | id/rightCount/errorCount/lineNo/faceNo/removeTotal/uploadRemoveTotal/time |
| `StatusRecordPO` | status_record | id/time/lineId/type/lineNo/faceNo/status/deviceNo/deviceName |
| `WorkshopDayRecordPO` | workshop_day_record | 默认 |

### 2.6 service/ (6) + service/imp/ (6)
| 接口 | 实现 | 责任 |
|---|---|---|
| `IDefectRecordService` | `DefectRecordServiceImpl` | **🚨 P0** 接收数据 + 统计 + Excel 导出 |
| `IDefectRecordBackupService` | `DefectRecordBackupServiceImpl` | **🚨 P0** backup + removeRecordByTime |
| `IDefectDayRecordService` | `DefectDayRecordServiceImpl` | **P1** 按小时分组的缺陷统计 |
| `ILineDayRecordService` | `LineDayRecordServiceImpl` | **P1** 按小时的产线良率统计 |
| `IStatusRecordService` | `StatusRecordServiceImpl` | 设备状态接收 + 心跳 |
| `IWorkshopDayRecordService` | `WorkshopDayRecordServiceImpl` | 空实现 |

### 2.7 task/ (1)
| 类 | cron | 责任 |
|---|---|---|
| `DetectDataTaskManager` | `0 0 0 * * ?` | **🚨 P0** `clearDetectData()` 清理 3 天前的 defect_record_backup；`clearStatisticDetectData()` 清理 30 天前的统计表 |

### 2.8 util/ (2)
| 类 | 责任 |
|---|---|
| `ExcelUtils` (+3 inner) | EasyExcel 导出（多 sheet + 单元格合并 + 列宽策略 + 边框样式） |
| `TimeRange` (+1 inner enum) | 时间范围迭代器（YYYY_MM_DD / YY_MM / MM_DD / HH） |

### 2.9 web/ (1)
| 类 | 端点 |
|---|---|
| `DetectDataController` | `POST /client/data/detect`、`POST /client/data/status`、`GET /web/detect/detail`、`GET /web/detect/statistic/export` |

---

## 3. 核心流程

### 3.1 数据接收流程（POST /client/data/detect）

```
客户端                          PSM DetectDataController
  │                                       │
  │ POST /client/data/detect              │
  │ DetectDataUploadDTO(form)             │
  │────────────────────────────────────→  │
  │                                       │ handleDetectData(form) [@Transactional]
  │                                       │
  │                                       │ 1. lineService.getByLineNoAndFaceNo() ──查 line 表
  │                                       │ 2. lineDefectTypeService.addDefectTypeIfNotExist() ──新增缺陷类型
  │                                       │ 3. defectDayRecordService.searchDefectCount() ──查本小时已存在记录
  │                                       │ 4. 累加 / 新建 defect_day_record ──saveOrUpdateBatch
  │                                       │ 5. lineDayRecordService.listByTimeAndLineNo() ──查本小时 line_day_record
  │                                       │ 6. 计算本小时 removeTotal（与上小时相减）
  │                                       │ 7. saveOrUpdate line_day_record
  │                                       │ 8. line.realtime_data = JSON(form.realTimeData) ──updateById
  │                                       │
  │ BaseResult.ok()                        │
  │←────────────────────────────────────│
```

### 3.2 Retention 流程（每日 0 点）

```
DetectDataTaskManager (cron: 0 0 0 * * ?)
  │
  ├─→ clearDetectData()
  │     defectRecordBackupService.removeRecordByTime(now - 3天)  ★ V1.6 金标准
  │     DELETE FROM defect_record_backup WHERE time <= #{time}
  │
  └─→ clearStatisticDetectData()
        ├─→ defectDayRecordService.removeRecordByTime(now - 30天)
        │     DELETE FROM defect_day_record WHERE time <= #{time}
        └─→ lineDayRecordService.removeRecordByTime(now - 30天)
              DELETE FROM line_day_record WHERE time <= #{time}
```

> ⚠️ **注意**：金标准只清理 defect_record_backup（3 天），**不主动执行 archive（INSERT INTO ... SELECT FROM）**！archive SQL 在 `DefectRecordXml.xml` 中存在但**未被 DetectDataTaskManager 调用** —— 是死代码还是被注释掉？这是个潜在疑问点。
>
> 实际 retention 走 `removeRecordByTime` 直接 DELETE backup 表，所以 backup 永远不会被填充（除非有未发现的代码路径）。需要 W-A 后续去 PSM 现场问。

### 3.3 设备状态接收流程（POST /client/data/status）

```
客户端 → DetectDataController.receiveStatus(records)
  │
  └─→ StatusRecordServiceImpl.receiveStatus(records) [@Transactional]
        │
        ├─→ 校验：所有 record 必须同 lineNo+faceNo
        ├─→ lineDAO.selectOne() ──查 line 表
        ├─→ 遍历 status_record (lineNo+faceNo) ──获取历史设备状态
        │     └─→ 客户端设备 (clientNo)：
        │           ├─ 不存在 → save(buildClient(...))
        │           └─ 存在且之前 OUTLINE → publish(DealAlarmEvent(reason=DISCONNECT))
        │                                  └─→ 触发断连报警
        ├─→ publish(StateChangeEvent) ──产线状态变更
        ├─→ 分类：新设备 (newAdd) + 已存在 (needUpdate)
        ├─→ saveBatch(newAdd) + updateBatchById(needUpdate)
        └─→ 清理：本轮未上报的非客户端设备 → removeBatchByIds
```

### 3.4 边界场景

| 场景 | 处理 |
|---|---|
| 客户端首推 status，line 表没记录 | 返回 error 20204（line 不存在）|
| 客户端之前 OUTLINE，现在重新 ONLINE | 触发 DealAlarmEvent(reason=DISCONNECT) |
| 同一 lineNo+faceNo 推不同设备状态混合 | 返回 error 20401（数据错乱）|
| `defect_day_record` 已存在本小时 → `setCount(form.count)` **覆盖**（非累加）|
| 上一小时不存在但本小时是第一条 | `removeTotal = form.realTimeData.removeTotal` |

---

## 4. 关键类逐个解析

### 4.1 🔴 P0: `DefectRecordServiceImpl` (162 行)

**核心方法 `handleDetectData(DetectDataUploadDTO form)`**:
```java
@Transactional(rollbackFor = Exception.class)
public BaseResult handleDetectData(DetectDataUploadDTO form) {
    LinePO line = lineService.getByLineNoAndFaceNo(form.lineNo, form.faceNo);
    if (line == null) return BaseResult.build().error("20204");
    
    List<DefectCountDTO> defects = form.todayData.defects;
    if (CollectionUtils.isNotEmpty(defects)) {
        lineDefectTypeService.addDefectTypeIfNotExist(line, defects);
        
        String statisticTime = HikDateUtil.formatLocalDate(form.todayData.statisticTime, "yyyy-MM-dd HH") + ":00:00";
        List<DefectDayRecordPO> needUpdateRecord = Lists.newArrayList();
        Map<String, DefectDayRecordPO> sortDefectRecordByType = Maps.newHashMap();
        List<String> defectTypes = defects.stream().map(DefectCountDTO::getType).toList();
        
        defectDayRecordService.searchDefectCount(statisticTime, form.lineNo, form.faceNo, defectTypes)
            .forEach(r -> sortDefectRecordByType.put(r.getType(), r));
        
        defects.forEach(dc -> {
            DefectDayRecordPO r = sortDefectRecordByType.getOrDefault(dc.getType(),
                new DefectDayRecordPO()
                    .setTime(statisticTime)
                    .setType(dc.getType())
                    .setCount(0)
                    .setLineNo(form.lineNo)
                    .setFaceNo(form.faceNo));
            r.setCount(dc.getCount());
            needUpdateRecord.add(r);
        });
        
        defectDayRecordService.saveOrUpdateBatch(needUpdateRecord);
        
        // ... 处理 line_day_record removeTotal 计算 ...
        line.setRealtimeData(JSONUtil.toJsonStr(form.realTimeData));
        lineService.updateById(line);
    }
    return BaseResult.build().ok();
}
```

**关键 SQL 字段**:
- `defect_day_record` 唯一键 = (time, line_no, face_no, type)
- `line_day_record` 唯一键 = (time, line_no, face_no)

**未使用的字段**: `service` (ExecutorService) — 声明了但**从未调用 submit()**

### 4.2 🔴 P0: `DefectRecordBackupServiceImpl` (37 行)

**两个核心方法**:
```java
@Override
public Integer removeRecordByTime(LocalDateTime time) {
    Wrapper<DefectRecordBackupPO> wrapper = Wrappers.lambdaQuery(DefectRecordBackupPO.class)
        .le(DefectRecordBackupPO::getTime, time);
    return defectRecordBackupDAO.delete(wrapper);
}

@Override
public boolean backup(List<DefectRecordPO> records) {
    if (CollectionUtils.isNotEmpty(records)) {
        List<DefectRecordBackupPO> backupRecords = records.stream()
            .map(DefectRecordBackupPO::new)  // 构造方法里 BeanUtil.copyProperties + id=null
            .toList();
        return defectRecordBackupDAO.insertBatchSomeColumn(backupRecords) == backupRecords.size();
    }
    return true;
}
```

**金标准配置**: `application.yml` 的 `data-retention-time.detect: 3`

### 4.3 🔴 P0: `DetectDataTaskManager` (47 行)

```java
@Scheduled(cron = "0 0 0 * * ?")
public void clearDetectData() {
    LocalDateTime time = LocalDateTime.now().minusDays(detectRetentionTime);  // 默认 3
    int count = defectRecordBackupService.removeRecordByTime(time);
    log.info("end delete defect record.[time={}][count={}]", time, count);
}

@Scheduled(cron = "0 0 0 * * ?")
public void clearStatisticDetectData() {
    LocalDateTime time = LocalDateTime.now().minusDays(statisticDataRetentionTime);  // 默认 30
    defectDayRecordService.removeRecordByTime(time);
    lineDayRecordService.removeRecordByTime(time);
}
```

### 4.4 🟡 P1: `DefectDayRecordServiceImpl` (165 行)

**核心方法**:
- `addLineDayRecord(lineNoList, defectNameList)` — REQUIRES_NEW 事务 + synchronized + 同日初始化（count=0 占位）
- `searchDefectCount(time, lineNo, faceNo, defects)` — 按小时查询
- `searchDefectCount(start, end, lineNo, faceNo, defects)` — 按时间段查询
- `listBetween(startTime, endTime)` — 用于 Excel 导出
- `removeRecordByTime(time)` — 时间清理
- `removeByType(types)` — 按缺陷类型清理

**关键 SQL** (`DefectDayRecordXml.xml`):
```sql
UPDATE defect_day_record SET count = count + #{record.count}
WHERE time = #{record.time} AND type = #{record.type}
  AND line_no = #{record.lineNo} AND face_no = #{record.faceNo}

SELECT count, type, time FROM defect_day_record
WHERE time >= #{startTime} AND time <= #{endTime}
  AND line_no = #{lineNo} AND face_no = #{faceNo}
  AND type IN (...) ORDER BY time
```

### 4.5 🟡 P1: `LineDayRecordServiceImpl` (98 行)

类似 DefectDayRecord，但 fields 多 `removeTotal/uploadRemoveTotal`。
**关键 SQL**: `selectRightAndError` 用 `TO_CHAR(NOW(),'yyyy-MM-dd 00:00:00')` 取今日合计（**PG/MySQL 兼容写法**）。

### 4.6 🟢 P2: `StatusRecordServiceImpl` (143 行)

核心 `receiveStatus`，已在 §3.3 详述。

### 4.7 ⚪ P3: `WorkshopDayRecordServiceImpl` — 空

---

## 5. 数据库交互

### 5.1 涉及表（6 张）

| 表 | 用途 | 字段 | retention |
|---|---|---|---|
| `defect_record` | 原始缺陷记录（含 imgList JSONArray）| 11 | ❌ EdgeHost 已删（W-A9）|
| `defect_record_backup` | 备份（imgList String）| 11 | **3 天**（金标准）|
| `defect_day_record` | 每小时每缺陷类型计数 | 8 | 30 天 |
| `line_day_record` | 每小时每线良率/剔除 | 10 | 30 天 |
| `status_record` | 设备在线/离线状态 | 11 | 无（无限增长）|
| `workshop_day_record` | 车间级日统计 | 默认 | 无 |

### 5.2 retention 配置

`application.yml`:
```yaml
data-retention-time:
  detect: 3      # defect_record_backup 保留天数
  statistic: 30  # defect_day_record + line_day_record 保留天数
```

### 5.3 status_record 涨库风险

> ⚠️ **status_record 没有 retention cron**！每条记录 lineId/type/lineNo/faceNo/status/deviceNo/deviceName，最坏情况持续累积。需要 W-A 后续评估是否需要 retention。

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `DefectDayRecordServiceImpl` | `DefectDayRecordService` | ✅ W-A9 |
| `DetectDataController` | `DetectController` | ✅ W-A9 |
| `DefectDayRecordPO` / `LineDayRecordPO` | DB 表 | ✅ W-A9 |

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `DefectRecordBackupServiceImpl` | ❌ 没做 | 🟡 P1（retention 金标准）|
| `DetectDataTaskManager` | ❌ 没做 | 🟡 P1（每日 0 点 cron）|
| `StatusRecordServiceImpl` | ❌ 没做 | 🟢 P2 |
| `LineDayRecordServiceImpl` | ⚠️ 部分（schema 已有）| 🟡 P1 |
| `WorkshopDayRecordServiceImpl` | ❌ 空实现 | ⚪ P3 |
| `ExcelUtils.handleStatisticDataExport` | ❌ 没做 | ⚪ P3 |

### 6.3 移植建议

- **defect_record_backup**: EdgeHost 已删 defect_record 表，**不需要移植 backup**。但 retention cron 思路（每日清理 N 天前数据）值得抄，用于 defect_day_record / line_day_record。
- **DetectDataTaskManager**: 改写为 .NET `IHostedService` + `Cronos` 库或 `Quartz.NET`
- **ExcelUtils**: EdgeHost 大概率用 ClosedXML 或 NPOI，但 PSM 用 EasyExcel + 多 sheet + 单元格合并，需要功能对齐

---

## 7. 风险 / 注意点

### 7.1 死代码风险

`DefectRecordXml.xml` 中的 `archive` SQL:
```sql
insert into defect_record_backup SELECT * FROM defect_record where time <= #{time}
```

PSM 反编译**没有调用方**！可能是 PSM V1.6 早期用过，retention 改成了"直接 DELETE backup"模式。需要 W-A 后续确认 PSM 现场逻辑。

### 7.2 时区问题

`DefectRecordPO` 用 `Asia/Shanghai` 时区，但 `status_record.time` 是 String（VARCHAR）—— 类型不一致，EdgeHost 移植时统一用 `TIMESTAMP WITH TIME ZONE`。

### 7.3 shift 计算

`ExportDefectStatisticForm.getEndTime()` 返回 `nextDay + " 07:00:00"`（**白班 08:00 → 夜班 07:00**），与 `DefectRecordServiceImpl.handleStatisticDataExport` 中的 `Eight=08:00 / TWENTY=20:00` **不一致**。需要 PSM 业务侧对齐：
- 导出 form：白班 08:00 → 次日 07:00（跨日）
- 内存计算：白班 08:00-19:59 + 夜班 20:00-次日 07:59

### 7.4 JSON 字段映射

`DefectRecordPO.imgList` 用 `JsonArrayTypeHandler`（自定义 MyBatis TypeHandler）映射 `JSONArray` ↔ MySQL `JSON` 列。EdgeHost 用 `System.Text.Json` + EF Core value converter。

### 7.5 单线程 executor 声明未使用

`DefectRecordServiceImpl` 声明 `ExecutorService service = Executors.newSingleThreadExecutor(...)` 但**从未 submit()**，可能是为了未来异步处理预留。

### 7.6 未声明变量 `tomorrow`

`handleStatisticDataExport` 中 `String tomorrow = null` 后续 `tomorrow = current.plusDays(1L).toLocalDate().toString()` —— 这里假设有跨日的 defect_day_record 数据，需要在 `line_day_record` 查询时支持跨日（当前实现只查 today + tomorrow 两天）。

---

## 8. 总结

detect 模块是 PSM 数据流的"心脏"，P0 关注点：
1. **`handleDetectData`**：核心写入路径（W-A9 已对齐）
2. **`DefectRecordBackupServiceImpl`**：retention 范式（V1.6 金标准，**3 天**）
3. **`DetectDataTaskManager`**：每日 0 点 cron（**W-A22+ 移植为 IHostedService**）

P1 关注点：
4. **`DefectDayRecordServiceImpl`**：按小时累加/查询（EdgeHost 已对齐）
5. **`LineDayRecordServiceImpl`**：按小时良率（EdgeHost 部分对齐）

风险点：archive SQL 是死代码、status_record 无 retention、shift 计算跨日边界、JSON 字段映射。
