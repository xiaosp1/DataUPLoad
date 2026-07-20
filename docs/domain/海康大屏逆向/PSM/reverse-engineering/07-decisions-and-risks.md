# 07. 决策清单 + 风险 + 建议方案

> 本文档汇总**新 EdgeHost 项目需要老板拍板的 5 个决策**，并附 PM 推荐方案。

---

## 🎯 决策 1：从头写新 EdgeHost 还是改 PSM？

### 现状

- PSM 是海康闭源商业产品（V2.1.9），我们没源码也没授权改
- 现场老 EdgeHost 是 NativeAOT 编译的 .NET 9 exe（46 MB），反编译失败
- 业务需求：MES 系统需要跟现场检测数据互通

### 选项

| 选项 | 说明 | 优点 | 缺点 |
|---|---|---|---|
| **A. 从头写新 EdgeHost** | 新建 .NET 或 Java 项目，按 ADR-004 第一层架构实现 | 完全可控；可对接 PSM；可继续对接 MES | 工作量大 |
| B. 改 PSM | 反编译 PSM 然后改源码 | 工作量小 | 不可能（海康加密狗 + 闭源） |
| C. 绕过 PSM，直接对接现场设备 | 现场设备 → EdgeHost → MES | 简单 | 失去大屏可视化 |

### ✅ PM 推荐

**A. 从头写新 EdgeHost**。理由：
1. PSM 改不动（海康闭源 + 加密狗）
2. ADR-004/004a/004b 已经画好架构图，方向清晰
3. 现场老 EdgeHost 的 DTO/SQL 跟我们新写的可以兼容（SQLite vs PostgreSQL 双栈）

---

## 🎯 决策 2：EdgeHost ↔ PSM 通信协议

### 现状

PSM 全是 `@RestController` + `@RequestMapping`，标准 Spring MVC REST + JSON。

### 选项

| 选项 | 说明 |
|---|---|
| **A. REST + JSON（HTTP/HTTPS）** | PSM 原生支持，80/443 端口 |
| B. gRPC | PSM 不支持，要 PSM 也加 gRPC |
| C. WebSocket | PSM 有 `/ws/**`，但用作推送不是请求-响应 |

### ✅ PM 推荐

**A. REST + JSON**。理由：
1. 跟 PSM 原生兼容，零适配成本
2. HTTPS 自带证书（hikrobot.jks），不用我们管
3. JSON 调试方便（curl 直接看）

### ⚠️ 风险

- PSM 默认走 443 HTTPS，但用 jks 自签证书，curl 需要 `-k` 跳过证书校验
- 如果 MES 不支持 HTTPS，要 EdgeHost 做 HTTPS → HTTP 反向代理

---

## 🎯 决策 3：EdgeHost 借 PSM 什么？

### 选项

| 选项 | 借什么 | 工作量 | 价值 |
|---|---|---|---|
| **A. DTO 字段定义** | DetectDataUploadDTO / AlarmDTO / SearchDefectRecordDTO 等的字段名+类型+校验 | 1 天（抄） | ⭐⭐⭐ 必须借 |
| **B. SQL 表结构** | V1.0~V1.19 反推出的表结构 | 1 天（已抄） | ⭐⭐⭐ 必须借 |
| **C. Service 实现** | YKServiceImpl / AlarmRecordServiceImpl 等 | 1 周（重写+调试） | ⭐⭐ 风险高 |
| **D. Controller 路径** | `/client/yk/*` 等路径 | 0（直接调） | ⭐⭐⭐ 必须借 |
| **E. PSM 内部枚举** | AlarmLevelEnum / AlarmTypeEnum | 半天 | ⭐⭐ 推荐借 |

### ✅ PM 推荐

**借 A + B + D + E，**不借 C**。

理由：
- A/B/D 是 PSM 给我们的"契约"，借了能保证跟 PSM 完全兼容
- E 借了省得自己猜枚举值
- C 不借，因为 PSM 内部实现耦合了 PSM 自己的 schema + 加密狗 + Hikvision framework，搬不动

---

## 🎯 决策 4：EdgeHost 技术栈（.NET 9 vs Java 17）

### 选项

| 维度 | **A. .NET 9（沿用现场）** | **B. Java 17 Spring Boot（跟 PSM 同栈）** |
|---|---|---|
| 现场兼容性 | ✅ 跟老 EdgeHost 同技术栈 | ❌ 完全重写 |
| 单文件部署 | ✅ NativeAOT 单 exe | ⚠️ Spring Boot 单 jar（可执行） |
| 性能 | ✅ NativeAOT 启动快、无 JIT | ⚠️ JVM 启动慢 |
| 跟 PSM DTO 复用 | ❌ 不同语言，DTO 要重新定义 | ✅ 抄过来改改就能用 |
| 跟 PSM SQL schema 复用 | ❌ 不同 ORM（EF Core vs MyBatis） | ✅ 同 ORM 工具可借鉴 |
| 学习曲线 | ✅ 团队已会 | ⚠️ 团队需学 Spring Boot |
| 单文件 vs 多文件 | ✅ 单 exe | ⚠️ jar + 配置文件 |
| 包大小 | ✅ 46 MB 单文件 | ⚠️ Spring Boot jar ~50 MB |
| 启动时间 | ✅ < 1 秒 | ⚠️ 3-5 秒 |
| 内存占用 | ✅ 80-150 MB | ⚠️ 200-300 MB |
| 运维工具链 | ✅ Windows 友好（PowerShell + dotnet CLI） | ✅ 也友好（PowerShell + java -jar） |

### ✅ PM 推荐

**A. .NET 9（沿用现场）**。理由：
1. 现场已经在跑 .NET 9 NativeAOT，重新选 Java 风险高
2. 部署工具链（start-mock.ps1、.bak-edgehost/）都是按 .NET 写的
3. DTO 字段名虽然要重新写，但都是机械工作
4. .NET 9 + System.Text.Json 处理 JSON 比 Java 简单
5. 老板的项目"工业数据平台"未来可能要做桌面客户端（IntcoEdge.Desktop），.NET 优势更大

### 备选

如果老板认为"跟 PSM 同栈更省事"，可以选 **B. Java 17**，但要承担：
- 重新学 Spring Boot 体系
- 跟现场老 EdgeHost 不兼容（新写一套部署）
- 失去 NativeAOT 单 exe 优势

---

## 🎯 决策 5：现场老 EdgeHost 的命运

### 现状

- 老 EdgeHost（PID 31260 + 5188）正在跑**闸门模式**（PushEnabled=false, DryRun=true）
- 老 exe 46 MB NativeAOT，反编译失败
- 老 exe 是 2026-07-18 18:54 编译版
- `D:\IntcoEdge\edge-v0.2\` 是完整运行目录

### 选项

| 选项 | 说明 | 优点 | 缺点 |
|---|---|---|---|
| **A1. 保留并存** | 老 v0.2 继续跑，新 v0.3 部署到 `D:\IntcoEdge\edge-v0.3\`，不同端口（如 5189） | 不影响生产；可灰度切换 | 端口资源占用 |
| A2. 完全替换 | 新 v0.3 覆盖老 v0.2，停机时间 5-10 分钟 | 简单 | 需要业务停机窗口 |
| A3. 保留过渡 | 老 v0.2 跑 1 周观察期，期间新 v0.3 并存；稳定后切换 | 最稳 | 最长切换周期 |

### ✅ PM 推荐

**A1. 保留并存**（如果是周一到周五生产时间）

**A2. 完全替换**（如果是周末或业务低谷）

理由：
- 工业项目 MES 对接，影响业务，停机成本高
- A1 不影响生产，新老并存 1-2 周，确认新 v0.3 稳定后再切
- 端口冲突好解决（v0.3 用 5189 或 5288）

### ⚠️ 风险

- 老 EdgeHost PID 31260 不能 kill——**它是 7-19 02:48 闸门模式稳定版**，是 MES 数据来源
- 新 v0.3 部署不能动老 exe 的 appsettings.json
- 新 v0.3 不能跟老 v0.2 用同一个 SQLite 数据库（写并发会冲突）

---

## ⚠️ 已知风险清单

### 风险 1：PSM 端点响应结构未知

**问题**：BaseResult 的字段名（code/data/success/message）从反编译看不出来
**影响**：EdgeHost 写代码时可能用错字段名（比如用 `success` 但 PSM 用 `code=200`）
**缓解**：启动 PSM 后用 curl 验证响应结构
**优先级**：🟡 中（开发前必须验证）

### 风险 2：lindGroup typo

**问题**：PSM `SearchDefectRecordDTO` 字段名拼错（lindGroup 不是 lineGroup）
**影响**：EdgeHost 用 lineGroup 调 PSM 会被忽略
**缓解**：在 EdgeHost 写死字段映射 `lineGroup → lindGroup`
**优先级**：🟢 低（一次写定）

### 风险 3：PSM 报警 level 枚举值

**问题**：V1.0 SQL 说 `level 1=一般 2=严重`，但 AlarmLevelEnum 反编译没拿到完整定义
**影响**：EdgeHost 推报警时 level 值可能跟 PSM 不兼容
**缓解**：启动 PSM 后查 `select distinct level from alarm_record;` 看实际值
**优先级**：🟡 中（验证后写死映射）

### 风险 4：PSM SQL 注释乱码

**问题**：PSM 自带 PG 编码可能是 GBK，SQL 注释乱码
**影响**：**不影响**，字段名/类型/默认值都是合法
**缓解**：无需处理
**优先级**：🟢 低

### 风险 5：现场老 EdgeHost 是 NativeAOT

**问题**：老 exe 反不到源码
**影响**：修老 EdgeHost 必须重编译
**缓解**：决策 5 推荐保留并存，新 v0.3 替换
**优先级**：🟡 中（影响老 EdgeHost 维护）

### 风险 6：yk 系统依赖外部英科服务

**问题**：PSM 的 yingke 模块只是个**网关**，真正的"英科系统"在 `192.168.80.33:10031`
**影响**：如果英科系统宕机，PSM 拉不到英科的缺陷数据
**缓解**：EdgeHost 可以**绕过 PSM 直接调英科**，但需要英科系统开放 API
**优先级**：🟡 中（取决于英科系统稳定性）

### 风险 7：决策 4 选 .NET 9 后 DTO 重新定义工作量大

**问题**：.NET 不能直接用 Java DTO，要重新写 C# class + JSON 序列化注解
**影响**：1-2 天额外工作
**缓解**：照抄 PSM DTO 字段名（用 C# 大小写约定）+ 用 System.Text.Json 自动 snake_case 映射
**优先级**：🟢 低（一次性）

---

## 🚀 PM 推荐的实施路径

按决策 1/4/5 默认走 A/A/A1，规划如下：

### Week 1：基础

- 老板拍板决策 1/4/5
- PM 启动 PSM + curl 验证全部 /client/* 端点响应结构
- 新建 EdgeHost 工程（.NET 9 ASP.NET Core WebAPI）
- 写 DTO 类（4 个：DetectDataUpload、Alarm、SearchDefectRecord、BaseResult）
- 写 HttpClient 封装 PSM REST 调用
- 写本地 SQLite 持久化（沿用现场 schema）

### Week 2：核心接口

- 实现 `POST /api/edge/mes/query`（拉缺陷）
- 实现 `GET /api/edge/mes/config`（拉字典）
- 实现 `POST /api/edge/mes/alarm`（推报警）
- 集成测试：用 curl 验证本地 EdgeHost → PSM 调用

### Week 3：集成 + 部署

- 集成测试：模拟 MES 调用 → EdgeHost → PSM 完整链路
- 部署到 `D:\IntcoEdge\edge-v0.3\`（端口 5189）
- 老 v0.2 继续跑 1 周观察
- 写部署文档 + 监控 + 告警

### Week 4：切换

- 老 v0.2 切到 v0.3（保留 24h 回滚窗口）
- 老 v0.2 进程保留但不启动，作为应急备份

---

## 📋 老板需要拍板的事

老板请回复 **1-5 号决策**，PM 就开干。

> 回复示例：`1-A 2-A 3-A 4-A 5-A1`（五个 A 系列）
> 或：`1A 2A 3A 4A 5A1`（简化版）
