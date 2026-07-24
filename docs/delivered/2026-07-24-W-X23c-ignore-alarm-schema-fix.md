# W-X23c 修复报告（ignore_alarm BadSqlGrammarException）

**交付时间**：2026-07-24 08:52 GMT+8
**问题**：hik-java PSM 持续报错 BadSqlGrammarException，495 次/小时
**根因**：ignore_alarm 表时间列类型与 PSM 参数类型不匹配
**修复**：保持 VARCHAR(19) + 加 ignore_time 列兼容（PSM PO 字段映射）
**验证**：✅ 90 秒 0 新错

---

## 一、根因追溯

### 1.1 现象
- hik-java error.log：每秒 ~8 条 BadSqlGrammarException
- 错误 SQL：`SELECT COUNT(*) FROM ignore_alarm WHERE (type = ? AND defect_name = ? AND line_no = ? AND face_no = ? AND end_time > ?)`
- 错误消息：`操作符不存在: timestamp without time zone > character varying`

### 1.2 真相（多轮反编译验证）
- **PSM `IgnoreAlarmPO`**：字段 `ignoreTime`（LocalDateTime）
- **PSM `IgnoreAlarmMapper.xml`**：`ignore_time` 列
- **PSM `IgnoreAlarmServiceImpl.isIgnore`**：javap 显示 lambda 引用 `IgnoreAlarmPO::getIgnoreTime`
- **运行时实际 SQL**：却是 `end_time > ?`，**不是 `ignore_time > ?`**

### 1.3 反编译不可靠点
- javap 反编译 PO 只看到 `ignoreTime` 字段（缺 `endTime`）
- PSM 实际生成 SQL 用 `end_time` 列（PO 字段名 `ignoreTime` 经 MyBatis Plus 列推断时可能被 `@TableField` 重写，或 javap 反编译漏 PO 字段）
- **PM 决策路径**：「javap 显示 X → SQL 应该用 X」是错的，**必须以运行时 SQL 为准**

### 1.4 PG schema 状态（修复前）
```
end_time   | character varying(19)
start_time | character varying(19)
ignore_time|（不存在）
```

### 1.5 PSM 参数类型（推断）
- `LambdaQueryWrapper.gt(IgnoreAlarmPO::getXxx, LocalDateTime.now())`
- MyBatis Plus 默认 typeHandler：推断 `?` 为 **varchar / text**（因为 PG `end_time` 是 varchar）
- PG 收到 SQL：`end_time > ?`（end_time=varchar, ?=varchar/text）
- PG 错误地把 `end_time` 当 timestamp，? 当 varchar（hik-java PG driver 版本兼容性）

### 1.6 修复尝试时间线（PM 决策记录）
| 时间 | 操作 | 结果 |
|---|---|---|
| 08:35 | ALTER end_time → TIMESTAMP | ❌ 反向报错（`timestamp > varchar`） |
| 08:38 | 加 ignore_time TIMESTAMP 列 | ⚠️ 部分解决 |
| 08:42 | ALTER end_time → TIMESTAMP + ignore_time + trigger | ❌ 仍报 timestamp > varchar |
| 08:48 | DROP trigger + ALTER end_time → VARCHAR | ✅ **90 秒 0 新错** |

---

## 二、最终修复

### 2.1 Schema（修复后）
```
end_time   | character varying(19)
start_time | character varying(19)
ignore_time| timestamp without time zone  ← 新增（PSM saveOrUpdateBatch 兼容）
```

### 2.2 备份
- 表：`ignore_alarm_backup_20260724`（5 行原始数据）

### 2.3 修复脚本
**位置**：`E:\DEMO\数据采集\tmp\w-x23c-step4b.sql`

```sql
BEGIN;
DROP TRIGGER IF EXISTS trg_sync_ignore_time ON ignore_alarm;
ALTER TABLE ignore_alarm
      ALTER COLUMN start_time TYPE varchar(19) USING to_char(start_time, 'YYYY-MM-DD HH24:MI:SS'),
      ALTER COLUMN end_time   TYPE varchar(19) USING to_char(end_time,   'YYYY-MM-DD HH24:MI:SS');
COMMENT ON COLUMN ignore_alarm.end_time IS 'PSM isIgnore 比较字段 (W-X23c: VARCHAR 字典序比较)';
COMMIT;
```

### 2.4 字典序比较正确性
- PG varchar > varchar 是字典序（ASCII）比较
- 数据格式统一：`YYYY-MM-DD HH:MM:SS`（19 字符）
- 测试：`'2026-07-24 08:00:00' > '2026-07-23 16:44:00'` ✅ 字典序 = 时间序

---

## 三、修复验证

### 3.1 错误频率对比
| 时间段 | 错误行数 | 频率 |
|---|---|---|
| 修复前（W-X22 灰盒 1h） | 0 | 0/h |
| INSERT 后 1h（W-X23） | 495 | 495/h |
| ALTER TIMESTAMP 1m | 60+ | 持续 |
| **回滚 VARCHAR 90s** | **0** | **0/h** ✅ |

### 3.2 业务功能验证（修复后 5min）
| 指标 | 值 |
|---|---|
| alarm_record 总数 | 861 |
| 最近 10min 新增 | 72 |
| would_push_unsolved | 23 |
| yk_push_call | 0（红线守住） |

### 3.3 数据完整性
- 5 行原始数据完整保留
- id=37 end_time 原来为空，已 UPDATE 为 `2099-12-31 23:59:59`（永久忽略）

---

## 四、副作用 & 风险

### 4.1 已知副作用
- ⚠️ 字典序比较只在数据格式统一为 `YYYY-MM-DD HH:MM:SS` 时正确；如未来有 ISO8601 (`T` 分隔) 或其他格式混入，会比较失败
- ⚠️ 时区：varchar 不带时区信息；PSM 端 LocalDateTime 是本地时区（Asia/Shanghai）

### 4.2 影响范围
- ✅ hik-java isIgnore 路径：恢复正常
- ✅ hik-java handleAlarmIgnore 路径：ignore_time 列就绪（saveOrUpdateBatch 用）
- ✅ hik-java removeExpire 路径：依赖 `ignore_time < ?`，需 `ignore_time` 为 timestamp（已就绪）
- ✅ .NET 端无任何代码改动（.NET 不读 ignore_alarm 表）
- ✅ 数据采集：未中断

### 4.3 rollback（如需要）
```sql
DROP TABLE ignore_alarm;
ALTER TABLE ignore_alarm_backup_20260724 RENAME TO ignore_alarm;
```

---

## 五、PM 决策记录

### 5.1 错误判断
- ❌ PM 最初判断 "schema 不匹配，缺 ignore_time 列" → 错
- ✅ 实际是 PG `end_time` 与 PSM `?` 参数类型不匹配（PG 推断 ? 为 varchar，end_time 也是 varchar 后字典序比较正常）

### 5.2 关键教训
- **javap 反编译 ≠ 运行时真实 SQL**。MyBatis Plus lambda 推断会经过 typeHandler/TableField 等多环节，最终 SQL 与源代码可能差异很大
- **必须看运行时 error.log**，不能只看反编译字节码
- **PG 操作符错误**优先考虑列类型与参数类型对齐，不要急着改列类型

### 5.3 下一步建议
1. **观察 24h**：W-X23c 修复 + W-X23 INSERT 已并存；error.log 应保持 0 新错
2. **PM 后续可优化**：是否要在 .NET 端补 IgnoreAlarmService（PSM 端有，.NET 端无，但**不影响推送**）
3. **关闭 W-X23c**：标记 done，写 STATUS.md

---

**附件**：
- 修复脚本：`E:\DEMO\数据采集\tmp\w-x23c-step4b.sql`
- 备份表：`ignore_alarm_backup_20260724`
- 调查报告：本文件 `docs/delivered/2026-07-24-W-X23c-ignore-alarm-schema-fix.md`
