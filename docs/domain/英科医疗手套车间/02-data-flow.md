# 02. 现场数据流

> 本文档定义现场数据从产生到归档的完整链路。
> 数据流向：视觉相机 → 视觉检测软件 → PSM → MES

---

## 🌊 完整数据流（4 步）

```
  ┌─────────────────────┐
  │ 1. 视觉检测软件      │  ← 装在工控机上，每个相机对应一个检测节点
  │    (现场 120 个相机) │     每排手模 3 个相机 → 检测 3 个区段
  └──────────┬──────────┘
             │
             │  HTTP POST  (相机检测到缺陷 / 设备异常)
             ▼
  ┌─────────────────────┐
  │ 2. PSM (Spring Boot) │  ← 海康大屏中间件
  │    解析 → 校验 → 存库 │     /client/data/detect + /client/data/alarm
  └──────────┬──────────┘
             │
             │  拉（MES 主动） + 推（PSM 主动）
             ▼
  ┌─────────────────────┐
  │ 3. MES 系统          │  ← 英科自己的 MES（具体待老板补充）
  │    生产管理系统      │
  └─────────────────────┘
```

---

## 📊 详细数据流（按数据类型）

### 数据流 A：缺陷数据（被动接收 + 主动暴露）

```
[视觉软件] 
   检测到手套有缺陷
   ↓
   HTTP POST /client/data/detect
   Body: DetectDataUploadDTO {
     lineNo: "L01",
     faceNo: "A",
     todayData: {...},
     realTimeData: {...}
   }
   ↓
[PSM]
   解析 JSON → 校验字段（@NotBlank faceNo/lineNo）
   ↓
   写入 defect_record 表
   ↓
   (可能触发 defect_alarm — 缺陷率超阈值时报警)
   ↓
[数据库] defect_record 表存满
   ↓
[MES 主动调] GET /client/yk/defect-record
            Body: SearchDefectRecordDTO {
              startTime, endTime,
              lindGroup: ["L01", "L02"],   // ⚠️ typo
              defectGroup: ["底面破损"]
            }
   ↓
[PSM 返回] 缺陷记录列表
   ↓
[MES] 写入自己的 DB / 做统计分析 / 触发业务逻辑
```

### 数据流 B：报警数据（被动接收 + 主动推送）★ 跟缺陷数据流不同

```
[视觉软件]
   检测到设备异常 / 产线停线
   ↓
   HTTP POST /client/data/alarm
   Body: AlarmDTO {
     uuid: "uuid-001",
     time: "2026-07-20 14:55:00",
     type: 3,           // 1=缺陷 2=系统 3=设备
     lineNo: "L01",
     faceNo: "A",
     level: 2,          // 1=一般 2=严重
     message: "相机掉线"
   }
   ↓
[PSM]
   解析 → 校验（@NotEmpty uuid/time/lineNo/faceNo/message, @Range type 1-3, @NotNull level）
   ↓
   写入 alarm_record 表
   ↓
   触发 PushAlarmEvent（Spring 事件）
   ↓
   推送逻辑（具体怎么推 MES 待老板确认）:
   ├─ HTTP POST 到 MES 的某个端点？
   ├─ WebSocket 推送到 MES？
   ├─ 或者只是写到 DB 等 MES 来拉？
   ↓
[MES] 接收报警 → 通知操作员 / 触发业务流程
```

⚠️ **关键决策点**：老板 15:09 说"报警数据是 PSM 主动推送给 MES 的"，但**没说是哪种协议**。PM 需要老板补充 PSM 推 MES 报警的协议 + 端点。

### 数据流 C：状态数据（设备心跳）

```
[视觉软件 / 现场设备]
   定时上报心跳
   ↓
   HTTP POST /client/data/status
   Body: List<StatusRecordPO>
   ↓
[PSM] 写入 status_record 表
   (目前不推 MES — MES 想知道设备在离线状态得自己来拉，或者告警由 alarm 通道单独处理)
```

### 数据流 D：字典数据（被动暴露）

```
[MES 启动 / 定时刷新]
   HTTP GET /client/yk/line-defect
   ↓
[PSM] 查询 line + defect_type 表
   ↓
   返回 LineAndDefectDTO 列表（产线+缺陷字典）
   ↓
[MES] 缓存到 MES 本地字典
```

### 数据流 E：配方/方案数据

```
[MES / 工程师工具]
   HTTP GET /client/plan?lineNo=L01
   ↓
[PSM] 查询 plan + plan_to_line 表
   ↓
   返回该产线当前启用的配方
```

---

## 🔍 EdgeHost 在数据流中的位置

按 ADR-004 设计（**待老板拍板的方案**）：

### 方案 1：EdgeHost 做 PSM ↔ MES 的"翻译官"

```
[视觉软件] → PSM → (数据没动，PSM 直接对接 MES)
                         ↑↑↑
                         EdgeHost 在这里加一层（拦截 + 字段映射 + 统计）
```

### 方案 2：EdgeHost 替换 PSM 的部分功能（不做）

PSM 改不动，这条路堵死。

### 方案 3：EdgeHost 做"影子层"（镜像 PSM 数据 + 自做统计）

```
[视觉软件] → PSM ──┬→ MES（直连）
                    │
                    └→ EdgeHost（影子副本，做统计 / 缓存 / 备查）
```

**PM 倾向方案 3**——EdgeHost 拉 PSM 数据做本地副本，方便：
- 做实时统计（PSM 不暴露 stats 接口）
- 做 MES 高频查询的缓存（降低 PSM 压力）
- 做故障应急（PSM 挂了 EdgeHost 还有数据可查）

### 方案 4：EdgeHost 做报警推送中转

```
[PSM] → EdgeHost → MES
```

把 PSM 主动推 MES 报警这条链路改成"PSM 推 EdgeHost，EdgeHost 推 MES"，EdgeHost 做报警聚合 + 过滤 + 重试。

**等老板拍**。

---

## 📋 数据流对应的"端点 vs 数据表"对照

| 数据流 | 视觉软件调用 PSM 端点 | PSM 写表 | MES 调用 PSM 端点 |
|---|---|---|---|
| 缺陷数据 | POST /client/data/detect | defect_record | POST /client/yk/defect-record |
| 报警数据 | POST /client/data/alarm | alarm_record | （PSM 主动推 MES，协议待定） |
| 状态数据 | POST /client/data/status | status_record | （无） |
| 字典数据 | （无） | （无） | GET /client/yk/line-defect |
| 配方数据 | （无） | plan / plan_to_line | GET /client/plan |
| 产线数据 | （无） | line | GET /web/line/* （要登录，MES 可能用不到） |

---

## ⚠️ 数据流待确认

1. **PSM 主动推 MES 报警**：协议（HTTP POST / WebSocket / MQTT）？MES 接收端点？推频率？
2. **MES 拉缺陷数据频率**：实时 / 5 分钟 / 1 小时 / 按班次？
3. **视觉软件到 PSM 的网络**：局域网还是有跨网段？PSM 在哪台机器（IP）？
4. **PSM 到 MES 的网络**：同一车间局域网，还是跨工厂？
5. **MES 调 PSM 时是否需要鉴权**：`/client/*` 在 PSM 配置里是公开免登录，**意味着任何能访问 PSM:443 的人都能调用**——这安全吗？
6. **报警数据被 PSM 推给英科系统**：yk 模块是否也会推报警？还是只推缺陷？
