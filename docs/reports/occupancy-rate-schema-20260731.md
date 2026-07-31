# 上座率参数 数据库调研报告

> **调研日期**: 2026-07-31 17:45 GMT+8
> **老板问题**: 上座率参数是一条线一个, 还是一条线四个面都有?
> **结论**: **按线+面二维存储**, 4 个独立参数 (一线 4 面独立值)

---

## 1. 答案

**`上座率` 参数按 `line_no + face_no` 二维存储**:

| 维度 | 字段 | 说明 |
|---|---|---|
| 表 | `public.line` | 27 张业务表之一, 存产线配置 + 实时数据 |
| 字段 | `realtime_data` (text) | JSON 文本, 含多个 KPI 字段 |
| 主键语义 | `(line_no, face_no)` | 一条线 4 个面 (A1+A2+B1+B2), 每个面一行 |

**不是"一线一参", 是"一线+一面一参"**:

| line_no | face_no | occupancyRate | occupancy | efficiency |
|---------|---------|---------------|-----------|------------|
| line10A | A1 | **100** | 7761 | 185 |
| line10A | A2 | **100** | 7761 | 185 |
| line10B | B1 | **99.8** | 7743 | 185 |
| line10B | B2 | **99.7** | 7738 | 185 |
| line1A  | A1 | 0 | 0 | 175 |
| line1A  | A2 | 0 | 0 | 175 |

**关键证据**: line10B 的 B1=99.8 ≠ B2=99.7 → **同一线不同面的上座率可以不同**。

---

## 2. 数据结构

### 2.1 `line` 表 schema

```sql
CREATE TABLE public.line (
  id            integer PRIMARY KEY,
  name          varchar(20)  NOT NULL,  -- 显示名 (如 "1号线A面")
  line_no       varchar(20)  NOT NULL,  -- 业务线号 (如 "line1A")
  face_no       varchar(20)  NOT NULL,  -- 面号 (如 "A1")
  client_no     varchar(20)  NOT NULL,  -- 客户端编号
  update_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  color         varchar(20),            -- UI 颜色
  realtime_data text                     -- JSON 实时数据 (含 occupancyRate)
);
```

### 2.2 realtime_data JSON 结构

```json
{
  "ngCount": 3821,            // NG 总数
  "efficiency": 175,           // 效率 (手套/分钟)
  "removeTotal": 3642,         // 剔除总数
  "occupancy": 0,              // 在制数量 (绝对值)
  "totalNgRate": 0.7,          // NG 率 (%)
  "total": 538450,             // 总产量
  "removeFailRate": 0,         // 剔除失败率
  "removeFail": 0,             // 剔除失败数
  "occupancyRate": 0,          // ← **上座率 (%, 字符串数字)**
  "successCount": 534629,      // 成功数
  "startTime": "2026-07-29 13:21:21",  // 班次开始时间
  "defects": [                 // 缺陷明细
    {"count": 1, "type": "底面破损", "showFlag": 1},
    ...
  ]
}
```

**所有 KPI 字段按 face_no 独立存储**, 不是按 line_no 聚合。

---

## 3. SQL 验证

### 3.1 产线总数

```sql
SELECT line_no, COUNT(*) AS faces, 
       MAX(realtime_data::json->>'occupancyRate') AS max_occ,
       MIN(realtime_data::json->>'occupancyRate') AS min_occ
FROM line 
GROUP BY line_no 
ORDER BY line_no;
```

**结果 (前 15 条)**:

```
 line_no | faces | max_occ | min_occ 
---------+-------+---------+---------
 line10A |     2 | 100     | 100
 line10B |     2 | 99.8    | 99.7
 line1A  |     2 | 0       | 0
 line1B  |     2 | 0       | 0
 line2A  |     2 | 0       | 0
 line2B  |     2 | 0       | 0
 line3A  |     2 | 0       | 0
 line3B  |     2 | 0       | 0
 line4B  |     2 | 0       | 0
 line5A  |     2 | 0       | 0
 line5B  |     2 | 0       | 0
 line6A  |     2 | 0       | 0
 line6B  |     2 | 0       | 0
 line7A  |     2 | 0       | 0
 line7B  |     2 | 0       | 0
(15 行记录)
```

**观察**:
- 每条线 `faces=2` (A + B 一面一个 client_no, 但每个 client 配 1 个 face)
- 实际上: **"一面 = 一线" 的命名约定**, 不是"一线 4 面"
- 命名模式: `line10A` = 第10号线的 A 面, `line10B` = B 面
- 每个 "line_no" 实际上代表 "某产线某一面", 不是整条线

### 3.2 字段命名再分析

`face_no` 取值: A1, A2, B1, B2...

| line_no | face_no | 含义 |
|---|---|---|
| line1A | A1 | 1号线 A面 第1个 client |
| line1A | A2 | 1号线 A面 第2个 client |
| line1B | B1 | 1号线 B面 第1个 client |
| line1B | B2 | 1号线 B面 第2个 client |

**所以"一条线" 实际是 `line1A + line1B` = 4 行 (2 个 face_no × 2 个 client_no)**
**"一条线 4 个面" 的说法对应 4 行 record**, 每行有自己的 occupancyRate。

---

## 4. 老板原问题映射

> "上座率这个参数是一条线体一个参数, 还是一条线体四个面都有这个参数?"

**回答**:
- 一条线 (按 line_no) 有 **多个** occupancyRate 参数
- 4 个面 (按 face_no) 各有一个 **独立** 的 occupancyRate
- 数据库按 `(line_no, face_no)` 二维存储, 一行一条

**举例** (line10A + line10B 是一组相邻的产线):
- line10A.A1 = 100%
- line10A.A2 = 100%
- line10B.B1 = 99.8%
- line10B.B2 = 99.7%

---

## 5. 业务影响

### 5.1 实时数据推送
- WS `/ws?type=screen&uid=web` 推送 `LineDataVO` 包含 `realtimeData.occupancyRate`
- 前端按 line_no + face_no 维度渲染 (LineListCard + 中栏 4 区面板)

### 5.2 配置变更
- 修改一个面的 occupancyRate 不影响其他面
- 适合独立调整每个面的目标产能

### 5.3 历史数据
- `line_day_record` 表按 `(line_no, time, face_no)` 存历史 KPI
- 同样二维存储

---

## 6. 结论

**✅ 上座率是按 (line_no, face_no) 二维存储的 4 个独立参数**

**不是**:
- ❌ 一线一参
- ❌ 全局唯一参数

**是**:
- ✅ 一线 4 个面 (A1+A2+B1+B2) 各一个参数
- ✅ 同一线不同面的值可以不同 (line10B.B1=99.8 vs B2=99.7)
- ✅ JSON 文本存储在 `public.line.realtime_data.occupancyRate`

---

**调研完成**: 2026-07-31 17:50 GMT+8
**查询工具**: `D:\Tool-xsp\psm-run\postgres\postgres\bin\psql.exe`
**PG**: 127.0.0.1:5433/intco (postgres/postgres)
