# 05. EdgeHost ↔ PSM 端点对接映射

> 本文是**新 EdgeHost 工程开发的最关键对接表**。
> 我们要做的 EdgeHost 4 类接口跟 PSM 的端点一对一映射。

---

## 📊 对接总览

| # | EdgeHost 接口 | PSM 对应端点 | 状态 | 备注 |
|---|---|---|---|---|
| 1 | `POST /api/edge/mes/query` (拉缺陷) | `POST /client/yk/defect-record` | ✅ 已对齐 | 字段 1:1 |
| 2 | `GET /api/edge/mes/config` (拉字典) | `GET /client/yk/line-defect` | ✅ 已对齐 | 字段 1:1 |
| 3 | `POST /api/edge/mes/alarm` (推报警) | `POST /client/data/alarm` | 🟡 字段对齐，level 待定 | 详见下方 |
| 4 | `POST /client/data/detect` (设备推检测) | `POST /client/data/detect` | 🟡 同名同路径，DTO 字段待对齐 | EdgeHost 是否做这个角色待定 |

---

## 1️⃣ EdgeHost `POST /api/edge/mes/query` → PSM `POST /client/yk/defect-record`

### 请求（EdgeHost 收到 MES 查询请求）

```json
{
  "method": "getDefectList",
  "params": {
    "startTime": "2026-07-20 00:00:00",
    "endTime":   "2026-07-20 23:59:59",
    "page": 1,
    "pageSize": 100,
    "defectGroup": ["底面破损"],
    "lineGroup": ["L01"]
  }
}
```

### 转换后（转发给 PSM）

```json
{
  "startTime":   "2026-07-20 00:00:00",
  "endTime":     "2026-07-20 23:59:59",
  "lindGroup":   ["L01"],          // ⚠️ 字段名从 lineGroup → lindGroup（typo）
  "defectGroup": ["底面破损"],
  "faceGroup":   []
}
```

### 响应（PSM → EdgeHost → MES）

PSM 返回 `BaseResult`，data 字段是缺陷记录列表（**结构待 PSM 启动验证**）。

EdgeHost 负责：
- 把 PSM 的响应包装成 MES 期望的格式
- 处理分页（PSM 是否支持分页待验证）

---

## 2️⃣ EdgeHost `GET /api/edge/mes/config` → PSM `GET /client/yk/line-defect`

### 转换（无需改字段，直接转发）

```
GET http://PSM/client/yk/line-defect
```

### 响应（PSM 返回）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "lineNo": "L01",
      "lineName": "一号产线",
      "defects": [
        {"defectCode": "001", "defectName": "底面破损"}
      ]
    }
  ]
}
```

**结构待 PSM 启动验证**。EdgeHost 直接转给 MES 即可。

---

## 3️⃣ EdgeHost `POST /api/edge/mes/alarm` → PSM `POST /client/data/alarm`

### 请求（MES 推报警给 EdgeHost）

现场 EdgeHost 用的是 SQLite，`alarm_record` 表字段名（2026-07-20 现场实测）：

| EdgeHost 字段 | 类型 | 来源 |
|---|---|---|
| `lineNo` | TEXT | 产线编号 |
| `faceNo` | TEXT | 面编号 |
| `type` | INT | 报警类型（具体值要查现场表） |
| `alarmLevel` | INT | 报警级别 |
| `time` | TEXT | 报警时间 |
| `message` | TEXT | 报警描述 |

### 转换后（转发给 PSM）

```json
{
  "uuid":    "<EdgeHost 生成 UUID>",     // EdgeHost 没有 uuid，要自己生成
  "time":    "2026-07-20 14:55:00",
  "type":    1,                            // ← EdgeHost 的 type → PSM 的 type（可能要映射）
  "lineNo":  "L01",
  "faceNo":  "A1",
  "level":   2,                            // ← EdgeHost 的 alarmLevel → PSM 的 level
  "message": "底面破损"
}
```

### ⚠️ 需要决策的字段映射

| EdgeHost 字段 | PSM 字段 | 处理方式 |
|---|---|---|
| `id` (auto) | `uuid` | **EdgeHost 生成 UUID 给 PSM 用**（PSM 用 uuid 去重） |
| `time` | `time` | 字段名一致，直接转 |
| `type` | `type` | ⚠️ EdgeHost 现场的具体值含义需要查表，可能要映射 |
| `alarmLevel` | `level` | ⚠️ 字段名不一致，需要重命名 + 验证枚举值是否兼容 |
| `message` | `message` | 字段名一致，直接转 |
| `lineNo` | `lineNo` | 字段名一致，直接转 |
| `faceNo` | `faceNo` | 字段名一致，直接转 |

---

## 4️⃣ 现场设备 → EdgeHost → PSM `POST /client/data/detect`

### 这是个**未决策**的角色分配

**现状**：现场设备直接 POST 到 PSM 的 `/client/data/detect` 端点，绕过 EdgeHost。

**两种方案**：

#### 方案 A：现场设备绕过 EdgeHost，直接 POST 到 PSM

```
现场设备 → POST http://PSM:443/client/data/detect
```

- 优点：EdgeHost 不用做"透明代理"，减少复杂度
- 缺点：EdgeHost 拿不到原始检测数据，无法做统计/告警分析

#### 方案 B：现场设备 → EdgeHost → PSM（EdgeHost 做代理）

```
现场设备 → POST http://EdgeHost:5188/...
EdgeHost → POST http://PSM:443/client/data/detect
```

- 优点：EdgeHost 拿到原始数据，能做统计 + 告警聚合
- 缺点：EdgeHost 出问题会断链

**PM 建议：方案 A**——保持现场设备的"直连 PSM"现状，EdgeHost 只做"PSM ↔ MES"对接。这样符合 ADR-004 的"第一层"设计（EdgeHost 不在数据流主路径上）。

**等老板拍板**。

---

## 🔍 响应结构验证清单

EdgeHost 开发完后，需要用以下 curl 验证 PSM 真实响应结构：

```bash
# 1. 拉缺陷字典
curl -k -X GET https://PSM/client/yk/line-defect

# 2. 拉缺陷记录
curl -k -X POST https://PSM/client/yk/defect-record \
  -H "Content-Type: application/json" \
  -d '{"startTime":"2026-07-20 00:00:00"}'

# 3. 推报警
curl -k -X POST https://PSM/client/data/alarm \
  -H "Content-Type: application/json" \
  -d '{"uuid":"test-001","time":"2026-07-20 14:55:00","type":1,"lineNo":"L01","faceNo":"A1","level":2,"message":"test"}'

# 4. 推检测数据
curl -k -X POST https://PSM/client/data/detect \
  -H "Content-Type: application/json" \
  -d '{"faceNo":"A1","lineNo":"L01","todayData":{},"realTimeData":{}}'
```

**⚠️ 这些 curl 不能在生产时间跑**——会写假数据进 PSM。
**需要老板安排测试窗口**。
