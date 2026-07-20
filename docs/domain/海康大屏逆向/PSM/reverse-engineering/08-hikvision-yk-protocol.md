# 08. 海康 PSM ↔ 英科系统 对接协议（★ 权威）

> ⚠️ **本协议是 PM 反编译 PSM 看不到的部分**，来源是老板给的权威文档：
> **`E:\项目\数采\1-前期调研\海康视觉检验数据接口需求_20240830.docx`**
> （2024-08-30 出，27 KB，9 个表格，159 段落）

> 🚨 **本文档修正 PM 之前反编译得出的错误认知**：
> 1. ❌ 错：**"PSM 主动推报警给 MES"** → ✅ 对：**"PSM 当英科客户端，调英科统一网关 `192.168.80.33:10031`"**
> 2. ❌ 错：**"英科系统在 PSM 端有 yk 模块"** → ✅ 对：**"英科系统是 PSM 调用的外部服务，不是 PSM 自己实现"**
> 3. ❌ 错：**"报警是 PSM 单向推"** → ✅ 对：**"PSM 把报警数据封装成英科格式，调英科 `HandleVisualInspectionAlarm`"**

---

## 🌐 网络拓扑（修正版）

```
┌─────────────────┐      ┌─────────────────┐
│  视觉软件        │      │  英科系统         │
│  (现场 120 个)   │      │  192.168.80.33    │
│                 │      │  :10031           │
│  POST /client/* │      │  /api/dataportal/ │
└────────┬────────┘      │  invoke           │
         │               └────────▲──────────┘
         ▼                        │
┌─────────────────┐               │
│  PSM (海康大屏)  │ ──────────────┘
│  Spring Boot     │  调英科网关（带 Ticket）
│  本机 443        │  POST /api/dataportal/invoke
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MES (= 英科系统)│ ← ★ MES 不是 PSM 的下游，就是英科系统本身！
│                 │   现场车间通过英科系统看数据
└─────────────────┘
```

**修正前**的认知（PM 反编译时的推测）：

```
视觉软件 → PSM → MES   ❌ MES 是独立的
```

**修正后**（从 docx 看真相）：

```
视觉软件 → PSM ─┬→ MES 主动调 PSM 拉缺陷/字典（PSM 暴露 /client/* 接口）
                │
                └→ PSM 调英科网关（推报警 + 拉缺陷字典）
```

**★ 关键认知**：
- **MES 跟英科系统是同一个东西**（不是两个系统）
- **PSM 是"英科客户端"**——它不直接推 MES，而是按英科规定的格式调英科统一网关
- **车间里所有"对外"操作（报警推送、缺陷数据查询）都要经过英科这套 `ApiType/Method/Parameters/Context/Ticket` 协议**

---

## 📋 协议 1：海康 → 英科字典查询接口

**端点**：MES 调 PSM 的 `GET /client/yk/line-defect`
**用途**：查询产线下所有产线/缺陷类型/面别

### 请求

```
GET /client/yk/line-defect
（无请求体）
```

### 响应 — 成功

```json
{
  "code": 200,
  "results": {
    "lindGroup":   ["lin1A", "lin1B", "lin2A", "lin2B"],
    "defectGroup": ["底面破损", "侧面破损", "二次污染"],
    "faceGroup":   ["A面", "B面"]
  }
}
```

### 响应 — 失败

```json
{
  "code": 400,
  "results": null,
  "message": "异常原因"
}
```

### 响应字段表（★ 修正 PM 反编译时的猜测）

| 字段 | 类型 | 字段含义 |
|---|---|---|
| `code` | Int | 状态码（200=成功，400=失败） |
| `results` | Object | 数据（成功时存在） |
| `results.lindGroup` | List\<String\> | 产线组 |
| `results.defectGroup` | List\<String\> | 缺陷类型组 |
| `results.faceGroup` | List\<String\> | 面别组 |
| `message` | String | 异常原因 |

---

## 📋 协议 2：海康 → 英科缺陷数据查询接口

**端点**：MES 调 PSM 的 `POST /client/yk/defect-record`
**用途**：根据参数获取视觉中控平台中检测出次品的详细数据

### 请求

```json
{
  "time": "2024-08-13 14:00:00",
  "lindGroup":   ["lin1A", "lin1B", "lin2A", "lin2B"],   // 不填默认查所有
  "defectGroup": ["底面破损", "侧面破损", "二次污染"],     // 不填默认查所有
  "faceGroup":   ["A面", "B面"]                          // 不填默认查所有
}
```

### 请求字段表

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `time` | DateTime | ✅ | 日期时间（**YYYY-MM-dd HH:00:00** — ⚠️ 只到整点） |
| `lindGroup` | List\<String\> | ❌ | 产线（非必填/多选） |
| `defectGroup` | List\<String\> | ❌ | 缺陷类型（非必填/多选） |
| `faceGroup` | List\<String\> | ❌ | 面别（非必填/多选） |

### 响应 — 成功

```json
{
  "code": 0,
  "success": true,
  "data": {
    "defects": [
      {
        "time": "2024-08-13 14:01:54",
        "lind": "lin1A",
        "defect": "底面破损",
        "face": "A面",
        "detectionCount": 1
      }
    ],
    "removeCounts": [
      {
        "lind": "lin1A",
        "face": "A面",
        "removeCount": 1
      }
    ]
  }
}
```

### 响应 — 失败

```json
{
  "code": 400,
  "results": null,
  "message": "异常原因"
}
```

### 响应字段表

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | Int | 状态码 |
| `success` | Boolean | **★ 修正：响应里有 `success` 字段**，不是我之前猜的 `BaseResult` |
| `data` | Object | 数据部分 |
| `data.defects` | List\<Object\> | 缺陷统计列表 |
| `data.defects[].time` | DateTime | 统计时间 |
| `data.defects[].lind` | String | 产线 |
| `data.defects[].defect` | String | 缺陷类型 |
| `data.defects[].face` | String | 面别 |
| `data.defects[].detectionCount` | Int | 检测异常数量 |
| `data.removeCounts` | List\<Object\> | 剔除数量 |
| `data.removeCounts[].lind` | String | 产线 |
| `data.removeCounts[].face` | String | 面别 |
| `data.removeCounts[].removeCount` | Int | 剔除数量 |

### ⚠️ PM 之前反编译看不出来的关键事实

- **响应是混合型**：`{code, success, data: {defects, removeCounts}}` —— 既有 `success` 又有 `code`
- `time` 字段**必须整点**（HH:00:00），不能传 HH:01:54
- `detectionCount` = 检测异常数量（**不是缺陷记录条数**），`removeCount` = 剔除数量
- ⚠️ **响应字段命名是 `lind` 不是 `line`**（跟请求的 `lindGroup` 一样有 typo）

---

## 📋 协议 3：英科 → 海康（海康调用英科）— ★ 这才是真正的对接

**端点**：`POST http://192.168.80.33:10031/api/dataportal/invoke`（英科提供）
**测试地址**：`http://192.168.32.86:1025/api/dataportal/invoke`
**用途**：英科系统的统一网关，海康 PSM 调这个来做**登录**和**报警上传**

### ⚠️ 重要

- 这是**英科系统**（MES）的入口
- **海康 PSM 是英科的客户端**——PSM 调这个，**不是 MES 调 PSM**
- 所有调用必须带 `Context.Ticket`（从登录拿）

### 调用格式（★ PSM 调英科的所有 API 都按这个格式）

```json
{
  "ApiType": "AuthenticationController",   // 控制器名
  "Parameters": [                          // 参数列表（按顺序对应方法参数）
    {
      "Value": "HKSJSB"                    // 第一个参数的值
    },
    {
      "Value": "HKSJSB123"                 // 第二个参数的值
    }
  ],
  "Method": "Login",                       // 方法名
  "Context": {                             // 上下文（带 Ticket）
    "Ticket": "...",                       // 登录后拿到的票据
    "InvOrgId": 1                          // 库存组织 ID
  }
}
```

---

### 协议 3.1：登录验证接口

**PSM → 英科**：登录拿 Ticket

#### 请求

```json
{
  "ApiType": "AuthenticationController",
  "Parameters": [
    { "Value": "HKSJSB" },
    { "Value": "HKSJSB123" }
  ],
  "Method": "Login",
  "Context": {}
}
```

#### 响应

```json
{
  "Success": true,
  "Message": null,
  "Result": {
    "UserId": 50001,
    "EmployeeId": 60002,
    "UserCode": "HKSJSB",
    "UserName": "海康视觉设备[HKSJSB]",
    "InvOrg": 1
  },
  "Context": {
    "Ticket": "7bJQZV6mCvdNBAGqv37bzpK3d5S2s9dfx5XClK4fdYdPXrPssMuSV1bapcvDvw7F6malRs+pwfQfA1Te/EWmTw==",
    "InvOrgId": 1
  }
}
```

#### ⚠️ 关键事实

- **账号是固定的**：`HKSJSB / HKSJSB123`（**从 application-prod.yml 也能看到**）
- **登录返回的是 `Ticket`，不是 JWT、不是 Cookie**——是一种类似 base64 的特殊票据
- **Ticket 要带回 `Context.Ticket` 给后续 API 用**
- `Login-interval=50`（分钟）—— PSM 每 50 分钟重新登录一次拿新 Ticket
- 用户的 `InvOrg=1` —— **当前 PSM 部署在"库存组织 1"下**

---

### 协议 3.2：报警数据上传接口 ★

**PSM → 英科**：视觉检测客户端产生异常报警数据时调用一次接口

#### 要求

- ✅ 接口支持传送批量数据
- ❌ 海康侧不用考虑数据重复性的问题（英科侧去重）
- ✅ 异常处理完之后也需要调用此接口（**更新 AlarmResult**）

#### 请求

```json
{
  "ApiType": "VisualInspectionController",
  "Parameters": [
    {
      "Value": [
        {
          "WorkShop": "TEST",                    // ⚠️ 测试用"TEST"，现场改
          "Line": "Line A",                      // ⚠️ 产线编号
          "Face": "Front",                       // ⚠️ 面别
          "AlarmTime": "2024-08-30T14:30:00",    // ⚠️ ISO 8601 格式（注意 T 分隔符）
          "AlarmType": "Temperature Alert",      // 报警类型
          "AlarmLevel": "High",                  // 报警等级
          "AlarmDetails": "Temperature exceeded threshold",
          "AlarmResult": "已处理",               // 处理结果
          "AlarmCount": 1
        }
      ]
    }
  ],
  "Method": "HandleVisualInspectionAlarm",
  "Context": {
    "Ticket": "7bJQZV6mCvdNBAGqv37bzpK3d5S2s9dfx5XClK4fdYdPXrPssMuSV1bapcvDvw7F6malRs+pwfQfA1Te/EWmTw==",
    "InvOrgId": 1
  }
}
```

#### 响应 — 成功

```json
{
  "Success": true,
  "Message": null,
  "Result": {
    "code": 200,
    "message": null
  },
  "Context": {
    "Ticket": "tvWCOruuBALZB5wREyLp0qe29+Xo+XX5Si6VvYwambmAuaykrHO/D6Dd30Nqgi6r9n4ZONPDorwMs/L4AvyDiA==",
    "InvOrgId": 1
  }
}
```

#### 响应 — 失败

```json
{
  "Success": true,
  "Message": null,
  "Result": {
    "code": 400,
    "message": "处理报警时出错 Object reference not set to an instance of an object."
  },
  "Context": {
    "Ticket": "H4ioiq+mRqYrVdgLUKlpA4DIKvXv8IWYVeIfAF/JTyc4IicLA99k8Ery8HrNKxMBEpINAB/AYEQXasWdXgxXmA==",
    "InvOrgId": 1
  }
}
```

#### 字段表

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `WorkShop` | String | ✅ | **车间（固定值）** ⚠️ 从车间表查（见下） |
| `Line` | String | ✅ | 产线 |
| `Face` | String | ✅ | 面别 |
| `AlarmTime` | String | ✅ | **ISO 8601 格式**（`yyyy-MM-ddTHH:mm:ss`，注意 T 分隔符） |
| `AlarmType` | String | ✅ | 报警类型 |
| `AlarmLevel` | String | ✅ | 报警等级 |
| `AlarmDetails` | String | ✅ | 报警内容 |
| `AlarmResult` | String | ✅ | 处理结果 |
| `AlarmCount` | Number | ✅ | 报警次数 |

---

## 🏭 车间代码表（★ 老板要的）

来自 docx 表格 6/7/8：

### 河北英科（HB）

| workShop | 含义 |
|---|---|
| HBN1 | 河北英科先一车间（丁腈） |
| HBN2 | 河北英科先二车间（丁腈） |
| HBN3 | 河北英科先三车间（丁腈） |
| HBN4 | 河北英科先四车间（丁腈） |
| HBN5 | 河北英科先五车间（丁腈） |
| HBN6 | 河北英科先六车间（丁腈） |
| HBP1 | 河北英科 PVC 一车间 |
| HBP2 | 河北英科 PVC 二车间 |
| HBP3 | 河北英科 PVC 三车间 |
| HBP4 | 河北英科 PVC 四车间 |
| HBP5 | 河北英科 PVC 五车间 |
| HBP6 | 河北英科 PVC 六车间 |

### 江西英科（JX）

| workShop | 含义 |
|---|---|
| JXN1 | 江西英科先一车间 |
| JXN2 | 江西英科先二车间 |
| JXN3 | 江西英科先三车间 |
| JXN4 | 江西英科先四车间 |

### 青岛英科 / 美嘉（QZ）

| workShop | 含义 |
|---|---|
| **QZM1** | 青岛美嘉车间 |
| **QZN1** | 青岛英科先一车间 |
| **★ QZN2** | **青岛英科先二车间**（★ 当前 PSM 部署 `yk.workshop=QZN2`，10 条线体） |
| **QZN3** | 青岛英科先三车间 |
| QZP1 | 青岛英科 PVC 一车间 |
| QZP2 | 青岛英科 PVC 二车间 |
| QZP3 | 青岛英科 PVC 三车间 |

⚠️ **PM 推断**：老板说的"10 条线体的车间"就是 **QZN2（青岛英科先二车间）**。`application-prod.yml` 里 `yk.workshop=QZN2` 也佐证了。

---

## 📌 ★ PM 之前认知的错误修正清单

| 项 | PM 之前以为 | 真相（来自 docx） | 影响 |
|---|---|---|---|
| **MES 系统** | MES 是英科自研或第三方采购 | **MES = 英科系统**（统一网关 `192.168.80.33:10031`） | EdgeHost 对接的是英科网关 |
| **PSM 推报警** | PSM 直推 MES | **PSM 调英科统一网关**，按英科 `ApiType/Method/Parameters/Context/Ticket` 格式 | EdgeHost 如果要做报警推送，必须按英科格式调 |
| **yk 模块** | PSM 自己实现的英科对接 | **yk 模块只是 PSM 调用英科网关的客户端** | yk 模块是 PSM 代码，但不是英科系统的实现 |
| **登录机制** | 不知道 | **PSM 用 `HKSJSB/HKSJSB123` 登录英科**，每 50 分钟重登一次拿 Ticket | EdgeHost 自己也要登录拿 Ticket |
| **车间代码** | QZN2 是青岛英科二车间（PM 推测） | docx 确认 QZN2 = 青岛英科先二车间 | **确认无误** |
| **协议格式** | REST + JSON | **REST + JSON + ApiType 统一网关格式**（外层包 ApiType/Method/Parameters/Context） | 跟标准 REST 不同，要按英科格式调 |
| **响应字段** | 只有 `code/data` | **有 `success` + `code` + `data`** 三层 | PSM 响应确实有 `success` 字段 |

---

## 🔍 EdgeHost 对接影响

按这个协议，**新 EdgeHost 要做的对接**：

### EdgeHost 调英科系统（★ 新）

| 动作 | 协议 | 端点 |
|---|---|---|
| 登录拿 Ticket | 协议 3.1 | `POST /api/dataportal/invoke` (Method=Login) |
| 推报警 | 协议 3.2 | `POST /api/dataportal/invoke` (Method=HandleVisualInspectionAlarm) |

### EdgeHost 调 PSM（已有）

| 动作 | 协议 | 端点 |
|---|---|---|
| 拉字典 | 协议 1 | `GET /client/yk/line-defect` |
| 拉缺陷 | 协议 2 | `POST /client/yk/defect-record` |
| 推缺陷数据 | 视觉软件→PSM | `POST /client/data/detect` |
| 推状态数据 | 视觉软件→PSM | `POST /client/data/status` |

### EdgeHost 调英科的具体数据

如果 EdgeHost 决定**绕过 PSM 直连英科**，需要：
1. 用 `HKSJSB/HKSJSB123` 登录拿 Ticket（跟 PSM 同账号）
2. 用 Ticket 调 `HandleVisualInspectionAlarm` 推报警
3. ⚠️ **可能账号冲突**（PSM 和 EdgeHost 用同一个英科账号登录）

**PM 建议**：
- EdgeHost 不直连英科，让 PSM 推
- EdgeHost 只做"PSM ↔ MES"的中间层
- 但 **MES 直连 PSM 是允许的**（PSM /client/* 公开免登录）

---

## 📂 文档位置

- **权威来源**：`E:\项目\数采\1-前期调研\海康视觉检验数据接口需求_20240830.docx`
- **PM 解读文档**：`docs/domain/海康大屏逆向/PSM/reverse-engineering/08-hikvision-yk-protocol.md`（本文件）
- **PM 反编译 PSM 文档**：`docs/domain/海康大屏逆向/PSM/reverse-engineering/01~07-*.md`

---

## ⚠️ 待老板补充

1. **英科系统是不是只有这一套网关**，还是有别的入口？
2. **EdgeHost 想直连英科**，需不需要单独申请账号（避免跟 PSM 抢账号）？
3. **英科 `HandleVisualInspectionAlarm` 接口的限流**（每秒最多推多少条报警）？
4. **英科系统的测试环境**是什么 URL？（测试用 192.168.32.86:1025）
5. **Ticket 有效期多久**？50 分钟重登是 PSM 的策略还是英科强制？
6. **文档里没提到缺陷查询接口的英科端入口**——是不是英科只暴露这一个 `invoke` 网关？
