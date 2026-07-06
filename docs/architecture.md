# 工业数据采集与智能分析平台技术方案

> 版本：v0.1
> 日期：2026-07-06
> 面向对象：后端/采集/前端/桌面端/AI/运维开发人员
> 依据文档：PROJECT-MEMO.md 五轮需求澄清结论
> 文档定位：指导一期 MVP 及后续 2~3 个月迭代开发，不提供业务实现代码，只给出架构约定、接口签名、DDL、伪代码与部署方案。

## 1. 文档目标与范围

本方案覆盖青州工厂首站落地所需的全部技术设计，目标是在 5 天内打通“数据采集→本地缓存→中心存储→上送 MES→可视化展示→AI 故障诊断”主链路，并为后续 23 个车间复制预留扩展点。文档遵循以下边界：

- 一期必须完成：海康视觉采集、PLC（OPC-UA/EIP）采集框架、边缘缓存、MES 海康数据上传、实时大屏基础版、报警规则引擎基础版、AI 故障诊断 MVP、点位配置基础界面、审计日志、Windows Service 自启。
- 一期预留但不完成：细粒度权限、消息队列、远程运维、飞书/小程序端、自动报告推送、跨基地 BI 汇总、预测性维护、工艺优化自动闭环。
- 部署范围：先车间 Windows 工控机，中心侧部署在工厂机房 Windows/Linux 服务器均可，但采集端与桌面壳必须以 Windows 为第一目标。
- 数据安全：一期不做字段级加密和权限体系，但所有“写 PLC、改配置、导出数据、手工补传、AI 诊断确认”操作必须落审计日志，为后续权限和追溯打基础。

本方案默认单人开发、C#/.NET 为主栈，Python 负责 AI 推理服务，整体设计以“稳、易部署、易复制、易排障”为最高优先级。

## 2. 总体架构

平台采用“边缘采集层 + 中心平台层”两层架构。车间工控机运行边缘采集服务和本地 WPF 壳，负责接设备、缓存、补传、本地上送和 AI 兜底；机房中心服务器运行聚合后端、TDengine/MySQL/Redis、Web 前端和 AI 服务，负责跨车间汇聚、统一展示、报表和复杂分析。

### 2.1 文字版架构图

```text
                             ┌─────────────────────────────────────────────────────────────┐
                             │                      工厂中心层（机房服务器）                 │
                             │                                                             │
                             │  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
                             │  │ Vue3+ECharts │  │ .NET 8 WebAPI│  │ Python FastAPI AI │  │
                             │  │ 大屏/报表/配置│  │ 聚合/查询/治理 │  │ Ollama+Qwen/DS    │  │
                             │  └──────┬───────┘  └──────┬───────┘  └────────┬──────────┘  │
                             │         │                 │                   │             │
                             │         └─────────────────┼───────────────────┘             │
                             │                           │                                 │
                             │   ┌──────────────┐  ┌─────┴──────┐  ┌───────────────────┐   │
                             │   │ MySQL(业务)   │  │Redis(缓存) │  │ TDengine(时序)     │   │
                             │   └──────┬───────┘  └─────┬──────┘  └────────┬──────────┘   │
                             │          │                │                  │              │
                             └──────────┼────────────────┼──────────────────┼──────────────┘
                                        │ HTTP/REST      │                  │
                                        │ 至少一次+幂等   │                  │
                             ┌──────────┴────────────────┴──────────────────┴──────────────┐
                             │                    车间边缘层（Windows 工控机 x N）           │
                             │                                                             │
                             │  ┌──────────────────────────────────────────────────────┐   │
                             │  │           .NET 8 EdgeHost (Windows Service)           │   │
                             │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │   │
                             │  │  │采集引擎  │ │规则/报警 │ │上传服务  │ │本地聚合   │ │   │
                             │  │  │Hik/OPC/EIP│ │阈值/组合 │ │MES Upload│ │分钟/班次 │ │   │
                             │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │   │
                             │  │       │            │            │            │        │   │
                             │  │  ┌────┴────────────┴────────────┴────────────┴─────┐  │   │
                             │  │  │      SQLite 边缘缓存 + 本地队列 + 审计/点位配置    │  │   │
                             │  │  └────┬────────────┬────────────┬────────────┬─────┘  │   │
                             │  └───────┼────────────┼────────────┼────────────┼────────┘   │
                             │          │            │            │            │            │
                             │  ┌───────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴───────┐    │
                             │  │ WPF 桌面壳  │ │本地WebView│ │本地AI兜底 │ │NTP/守护/备份│    │
                             │  │ 大屏/配置   │ │Vue站点   │ │小模型/规则 │ │自动任务   │    │
                             │  └─────────────┘ └──────────┘ └──────────┘ └────────────┘    │
                             │          │            │            │                         │
                             └──────────┼────────────┼────────────┼─────────────────────────┘
                                        │            │            │
           ┌──────────────┐    ┌────────┴───┐   ┌────┴─────┐  ┌───┴────────────┐
           │海康视觉大屏   │    │包装机 PLC  │   │点数机PLC │  │ MES 统一网关   │
           │ HTTP API     │    │ OPC-UA     │   │ EIP      │  │ REST / JWT     │
           └──────────────┘    └────────────┘   └──────────┘  └────────────────┘
```

### 2.2 架构原则

1. 边缘自治：工控机断网时可独立采集、缓存、展示、报警，网络恢复后自动补传，不丢最近 5 分钟数据作为硬约束，缓存上限可配置。
2. 中心汇聚：跨车间查询、统一配置模板、AI 模型服务、历史归档、报表导出、BI 聚合均在中心完成。
3. 配置驱动：点位、阈值、上传策略、班次、报警规则、AI 提示词全部通过数据库/配置文件管理，禁止硬编码。
4. 协议适配：采集驱动、MES 上传通道、AI 模型均采用接口抽象 + 适配器模式，方便后续增加 Modbus、S7、MQTT、英科统一网关等实现。
5. 可观测优先：所有链路必须有 traceId、设备编号、采集时间戳、入库时间戳、发送状态、重试次数，便于现场排障。
6. 一期轻量：不上 MQ，不引入微服务，不做服务网格；用进程内 HostedService + SQLite 队列 + Redis 缓存解决异步与削峰问题。

## 3. 模块划分

系统按职责拆分为 10 个一级模块，模块之间通过明确的 C# 接口、REST API 或本地进程调用协作，避免直接跨层读写数据库。

| 模块 | 运行位置 | 主要职责 | 关键输出 |
|---|---|---|---|
| 采集模块 | 边缘 EdgeHost | 驱动海康/OPC-UA/EIP 采集，支持轮询+订阅，点位映射，数据质量标记 | 原始测点 DataPoint、设备事件、报警事件 |
| 存储模块 | 边缘+中心 | SQLite 边缘缓存，TDengine 时序写入，MySQL 业务存储，Redis 热点缓存 | 持久化记录、查询接口、归档结果 |
| 上传模块 | 边缘 EdgeHost | MES 上传、幂等去重、失败重试、离线缓存补传、人工导出 | MES 消息包、发送日志、补传任务 |
| 分析模块 | 边缘+中心 | 分钟聚合、OEE、SPC、帕累托、趋势、报表、班次统计 | 聚合指标、报警结果、报表文件 |
| AI 模块 | 边缘可选+中心主用 | 本地小模型兜底、中心 RAG 诊断、案例检索、规则融合、解释输出 | 诊断结论、置信度、建议、引用案例 |
| 前端模块 | 中心 Web + 边缘本地 | Vue3+ECharts 大屏、趋势、报表、配置、报警、审计页面 | 可视化界面、配置表单 |
| 桌面端模块 | 边缘工控机 | WPF 壳，承载开机自启、全屏大屏、本地诊断、配置入口、服务状态监控 | 桌面应用、全屏看板、快捷运维 |
| 配置模块 | 边缘+中心 | 点位、设备、产线、班次、阈值、规则、上传策略、模型参数管理 | 配置版本、下发记录、校验结果 |
| 审计模块 | 边缘+中心 | 写PLC、改配置、导出、补传、手工操作、AI 确认操作留痕 | 审计日志、操作回放依据 |
| 备份模块 | 边缘+中心 | SQLite/MySQL/TDengine 备份、配置备份、日志轮转、恢复脚本 | 备份文件、备份任务报告 |

### 3.1 模块依赖原则

- 采集模块只输出标准化 `DataPoint/DeviceEvent/AlarmEvent`，不直接写 MES、不直接做复杂分析。
- 存储模块是唯一数据库访问入口，其他模块通过 Repository/QueryService 访问数据。
- 上传模块依赖“待上传队列”和“发送结果表”，不直接读设备点位。
- AI 模块只读清洗后的特征数据、报警记录和案例库，不直接连 PLC。
- WPF 桌面壳不承载核心业务逻辑，只承载展示、配置入口和 EdgeHost 状态监控。

## 4. 关键技术选型理由

| 选型 | 结论 | 理由 |
|---|---|---|
| 后端/采集服务 | C# .NET 8 | 单人开发者主技术栈；Windows 工控机部署成熟；OPC-UA、EIP 有稳定库；HostedService/BackgroundService 适合长任务；性能足以支撑单车间 500 万条/天 |
| 前端 | Vue 3 + ECharts | 学习成本低、大屏生态成熟、组件化适合多级看板；ECharts 对趋势/帕累托/SPC/实时刷新支持完善 |
| 桌面壳 | .NET WPF | 与后端同栈，可直接引用 .NET 类库；适合 Windows 全屏工控屏；能嵌入 WebView2 承载 Vue 大屏 |
| AI 服务 | Python + FastAPI + Ollama + Qwen2.5/DeepSeek | 本地模型生态最成熟；Ollama 部署简单；FastAPI 便于和 .NET 通过 HTTP 交互；Qwen2.5/DeepSeek 中文故障诊断效果较好，7B/14B 可在单张消费级 GPU 上跑 |
| 时序数据库 | TDengine | 写入性能高、压缩率高、适合亿级/天场景；支持超级表和标签建模；对工业点位场景天然适配；Windows 可部署，单机可承接青州试点规模 |
| 关系数据库 | MySQL 8.0 | 成熟稳定，设备档案、配置、工单、报警、审计、案例库等关系数据建模方便；运维人员熟悉 |
| 边缘缓存 | SQLite | 零部署、单文件、事务可靠，适合工控机离线缓存补传；不引入额外中间件 |
| 中心缓存 | Redis | 大屏热点、会话、AI 检索缓存、实时指标缓存；一期不上 MQ 时也可做轻量队列/状态协调 |
| 通讯协议 | HTTP/REST 为主，OPC-UA/EIP 直连 | 调试简单；MES、海康、AI 都可复用 REST；PLC 通过标准工业协议直连，避免中间件风险 |
| 部署形态 | Windows Service + NSSM/内置 Service | 边缘端必须自启、自恢复；.NET 8 可直接发布为 Windows Service |

不选方案说明：

- 一期不选 MQ/Kafka：当前单人开发、MES 侧未准备 MQ、链路短，用 SQLite 本地队列 + 定时发送即可满足“至少一次”，避免部署复杂度。
- 一期不选 Java/Go/Node 后端：偏离主技术栈，影响 5 天 MVP 速度。
- 一期不选云端 LLM API：需求明确“数据不出厂”，必须本地部署。
- 一期不选 Electron 桌面壳：WPF 更贴合 Windows 工控场景，可直接管理本地服务状态和系统权限。

## 5. 目录结构建议

建议采用单仓多应用结构，边缘、中心、AI、前端、桌面壳统一管理，方便复制部署。

```text
IndustrialDataPlatform/
├─ docs/
│  ├─ PROJECT-MEMO.md
│  ├─ 11-技术方案.md
│  └─ api/
├─ src/
│  ├─ Common/
│  │  ├─ Idp.Common/                 # 通用模型、工具、时间、雪花ID、审计接口
│  │  ├─ Idp.Protocol/              # 协议无关的采集/上传/报警模型
│  │  └─ Idp.Infrastructure/        # 通用仓储、配置、日志、缓存抽象
│  ├─ Edge/
│  │  ├─ Idp.EdgeHost/              # Windows Service 边缘主机
│  │  ├─ Idp.Collectors/
│  │  │  ├─ Hikvision/
│  │  │  ├─ OpcUa/
│  │  │  └─ EtherNetIp/
│  │  ├─ Idp.EdgeStorage/           # SQLite 上下文、本地队列、补传状态
│  │  ├─ Idp.Uploaders/             # MES 上传器、适配英科网关/REST
│  │  └─ Idp.EdgeAi/                # 本地规则+小模型客户端
│  ├─ Center/
│  │  ├─ Idp.CenterApi/             # ASP.NET Core WebAPI
│  │  ├─ Idp.Tdengine/              # 时序仓储、超级表初始化、写入/查询
│  │  ├─ Idp.MySql/                 # EF Core 上下文、迁移
│  │  ├─ Idp.Redis/                 # 缓存、热点指标、分布式锁
│  │  ├─ Idp.Analytics/             # OEE/SPC/聚合/报表
│  │  └─ Idp.AlarmEngine/           # 规则引擎
│  ├─ Ai/
│  │  ├─ idp_ai/
│  │  │  ├─ main.py
│  │  │  ├─ routers/
│  │  │  ├─ services/
│  │  │  ├─ rag/
│  │  │  └─ prompts/
│  │  └─ models/                    # 模型下载目录、Modelfile 说明
│  ├─ Desktop/
│  │  └─ Idp.Desktop/               # WPF 壳，嵌入 WebView2
│  └─ Web/
│     └─ idp-web/                   # Vue3 前端
├─ deploy/
│  ├─ edge/
│  │  ├─ install.ps1
│  │  ├─ EdgeHost.windows-service.xml
│  │  └─ ntp-config.ps1
│  ├─ center/
│  │  ├─ docker-compose.yml（可选）
│  │  ├─ tdengine/
│  │  ├─ mysql/
│  │  └─ redis/
│  └─ backup/
├─ scripts/
│  ├─ init-tdengine.sql
│  ├─ init-mysql.sql
│  └─ dev-cert.ps1
└─ tests/
   ├─ Idp.Tests.Unit/
   ├─ Idp.Tests.Integration/
   └─ Idp.Tests.E2E/
```

目录约定说明：

- `Common/Idp.Protocol` 统一定义跨模块 DTO，避免边缘、中心、AI 各自定义一套报文。
- `Edge` 和 `Center` 共享 `Common`，但不直接共享数据库上下文。
- AI 服务独立目录，Python 环境通过 `requirements.txt` 或 `poetry` 管理，.NET 通过 HTTP 调用。
- `deploy/edge` 必须提供一键安装/卸载脚本，现场部署人员只需要改配置文件即可上线。

## 6. 数据库设计

数据库分三层：TDengine 承接高频时序测点与聚合结果，MySQL 承接设备档案、配置、报警、工单、审计、案例库等关系数据，SQLite 在边缘侧承接待上传队列、短期缓存、本地配置快照。Redis 只做缓存与实时状态，不做持久真相。

### 6.1 建模约定

- 所有表统一主键：`id BIGINT`，采用雪花算法或 TDengine 自增/组合键按场景使用。
- 所有业务记录必须带 `tenant_id`（集团/基地）、`factory_code`、`workshop_code`、`line_code`、`device_code`，便于多基地复制。
- 时间字段统一使用 UTC + 本地时区双列或单列为 UTC，展示层按 Asia/Shanghai 转换；边缘采集带 `collect_time_local` 与 `collect_time_utc` 双字段，避免 NTP 漂移导致错序。
- 数据质量字段 `quality` 取值：`good / uncertain / bad / timeout`。
- 删除采用逻辑删除 `is_deleted`，关键配置保留版本号 `version`。

### 6.2 TDengine 超级表设计

TDengine 以“测点值”作为最小事实单元，采用“一张原始超级表 + 多张聚合超级表”的模式。超级表标签用于设备、车间、点位等静态元数据，子表按“设备-点位”自动创建。

#### 6.2.1 原始测点超级表

```sql
-- 原始测点：轮询/订阅/事件统一写入
CREATE STABLE IF NOT EXISTS st_metric_raw (
    ts              TIMESTAMP,      -- 采集时间（TDengine 主键）
    value_double    DOUBLE,         -- 数值型测点
    value_string    NCHAR(256),     -- 字符串型测点（配方号、状态文本等）
    value_json      NCHAR(1024),    -- 扩展 JSON（海康缺陷明细等）
    quality         NCHAR(16),      -- good/uncertain/bad/timeout
    source_type     NCHAR(16),      -- poll/subscription/event
    edge_node       NCHAR(64),      -- 边缘工控机编号
    collect_latency INT,            -- 采集到入库延迟（毫秒）
    ext             NCHAR(512)      -- 预留扩展
) TAGS (
    tenant_id       NCHAR(32),
    factory_code    NCHAR(32),
    workshop_code   NCHAR(32),
    line_code       NCHAR(32),
    device_code     NCHAR(32),
    device_type     NCHAR(32),      -- packaging_machine / glove_counter / hik_screen
    point_code      NCHAR(64),
    point_name      NCHAR(128),
    data_type       NCHAR(16),      -- bool/int/double/string/json
    unit            NCHAR(16),
    protocol        NCHAR(16)       -- hik_http/opcua/eip
);
```

自动建子表命名规范：`ct_raw_<device_code>_<point_code>`，例如 `ct_raw_pk01_mold_temp`。

#### 6.2.2 报警事件超级表

```sql
CREATE STABLE IF NOT EXISTS st_alarm_event (
    ts              TIMESTAMP,      -- 报警开始时间
    end_ts          TIMESTAMP,      -- 报警结束时间，未恢复为 NULL
    alarm_code      NCHAR(64),
    alarm_level     NCHAR(16),      -- info/warn/error/critical
    alarm_message   NCHAR(256),
    trigger_value   DOUBLE,
    trigger_expr    NCHAR(256),
    acknowledged    BOOL,
    ack_user        NCHAR(64),
    ai_diag_status  NCHAR(16),      -- none/queued/running/success/failed
    edge_node       NCHAR(64)
) TAGS (
    tenant_id       NCHAR(32),
    factory_code    NCHAR(32),
    workshop_code   NCHAR(32),
    line_code       NCHAR(32),
    device_code     NCHAR(32),
    point_code      NCHAR(64),
    alarm_type      NCHAR(32)       -- threshold/rule/device/plc/upload/ai
);
```

#### 6.2.3 分钟/小时/天聚合超级表

```sql
CREATE STABLE IF NOT EXISTS st_metric_1m (
    ts              TIMESTAMP,
    avg_val         DOUBLE,
    min_val         DOUBLE,
    max_val         DOUBLE,
    first_val       DOUBLE,
    last_val        DOUBLE,
    sample_count    INT,
    bad_count       INT
) TAGS (
    tenant_id       NCHAR(32),
    factory_code    NCHAR(32),
    workshop_code   NCHAR(32),
    line_code       NCHAR(32),
    device_code     NCHAR(32),
    point_code      NCHAR(64),
    point_name      NCHAR(128),
    unit            NCHAR(16),
    shift_code      NCHAR(16)      -- day/night（可空，天级统计再补）
);
```

小时级 `st_metric_1h`、天级 `st_metric_1d` 结构类似，在边缘侧可先做 1 分钟聚合，中心侧做更高层聚合与跨设备汇总。

#### 6.2.4 TDengine 使用约定

- 写入使用参数化批量 INSERT，按设备或子表批量提交，单批 1000~5000 条。
- 查询一律通过服务端 API，不允许前端直接写 SQL。
- 保留策略：原始测点 1 年，1 分钟聚合 2 年，1 小时/天聚合 5 年；超出由 TDengine 数据保留策略自动清理。
- 海康结构化记录可写入 `st_metric_raw.value_json`，对高频字段（缺陷类型、等级、工位）拆独立点位更好，但 MVP 先入 JSON 以加快进度。

### 6.3 MySQL 核心表设计

MySQL 采用 InnoDB，字符集 `utf8mb4`，统一使用 `created_at/updated_at` 审计时间。以下为核心表 DDL 示例，字段以“可直接建表”为目标。

#### 6.3.1 工厂/车间/产线/设备

```sql
CREATE TABLE factory (
  id              BIGINT PRIMARY KEY,
  tenant_id       VARCHAR(32)  NOT NULL DEFAULT 'group',
  factory_code    VARCHAR(32)  NOT NULL UNIQUE,
  factory_name    VARCHAR(128) NOT NULL,
  region          VARCHAR(64),
  status          VARCHAR(16)  NOT NULL DEFAULT 'active',
  remark          VARCHAR(255),
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE workshop (
  id              BIGINT PRIMARY KEY,
  factory_code    VARCHAR(32)  NOT NULL,
  workshop_code   VARCHAR(32)  NOT NULL,
  workshop_name   VARCHAR(128) NOT NULL,
  edge_node_code  VARCHAR(64),
  status          VARCHAR(16)  NOT NULL DEFAULT 'active',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_factory_workshop (factory_code, workshop_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE production_line (
  id              BIGINT PRIMARY KEY,
  factory_code    VARCHAR(32) NOT NULL,
  workshop_code   VARCHAR(32) NOT NULL,
  line_code       VARCHAR(32) NOT NULL,
  line_name       VARCHAR(128) NOT NULL,
  line_type       VARCHAR(32),
  status          VARCHAR(16) NOT NULL DEFAULT 'active',
  created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT    NOT NULL DEFAULT 0,
  UNIQUE KEY uk_line (factory_code, workshop_code, line_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device (
  id                BIGINT PRIMARY KEY,
  device_code       VARCHAR(64)  NOT NULL UNIQUE,
  device_name       VARCHAR(128) NOT NULL,
  device_type       VARCHAR(32)  NOT NULL, -- packaging_machine/glove_counter/hik_screen
  factory_code      VARCHAR(32)  NOT NULL,
  workshop_code     VARCHAR(32)  NOT NULL,
  line_code         VARCHAR(32)  NOT NULL,
  protocol          VARCHAR(16)  NOT NULL, -- hik_http/opcua/eip
  endpoint          VARCHAR(255),
  port              INT,
  plc_model         VARCHAR(64),
  polling_interval  INT          NOT NULL DEFAULT 60,
  subscribe_enabled TINYINT      NOT NULL DEFAULT 1,
  enabled           TINYINT      NOT NULL DEFAULT 1,
  tags              VARCHAR(512),
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT      NOT NULL DEFAULT 0,
  KEY idx_device_loc (factory_code, workshop_code, line_code),
  KEY idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 6.3.2 点位与采集配置

```sql
CREATE TABLE point (
  id                BIGINT PRIMARY KEY,
  device_code       VARCHAR(64)  NOT NULL,
  point_code        VARCHAR(64)  NOT NULL,
  point_name        VARCHAR(128) NOT NULL,
  data_type         VARCHAR(16)  NOT NULL, -- bool/int/double/string/json
  address           VARCHAR(255) NOT NULL, -- OPC NodeId / EIP 标签名 / Hik 接口字段
  address_ext       VARCHAR(255),
  unit              VARCHAR(16),
  scale             DECIMAL(18,6) DEFAULT 1,
  offset            DECIMAL(18,6) DEFAULT 0,
  poll_enabled      TINYINT      NOT NULL DEFAULT 1,
  poll_interval     INT,
  subscribe_enabled TINYINT      NOT NULL DEFAULT 0,
  deadband          DECIMAL(18,6) DEFAULT 0,
  upload_enabled    TINYINT      NOT NULL DEFAULT 1,
  alarm_enabled     TINYINT      NOT NULL DEFAULT 0,
  sort_no           INT          NOT NULL DEFAULT 0,
  remark            VARCHAR(255),
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_device_point (device_code, point_code),
  KEY idx_point_device (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 6.3.3 班次与规则配置

```sql
CREATE TABLE shift_config (
  id              BIGINT PRIMARY KEY,
  factory_code    VARCHAR(32) NOT NULL,
  workshop_code   VARCHAR(32) NOT NULL,
  shift_code      VARCHAR(16) NOT NULL, -- day/night
  shift_name      VARCHAR(32) NOT NULL,
  start_time      TIME        NOT NULL,
  end_time        TIME        NOT NULL,
  cross_day       TINYINT     NOT NULL DEFAULT 0,
  enabled         TINYINT     NOT NULL DEFAULT 1,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_shift (factory_code, workshop_code, shift_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alarm_rule (
  id              BIGINT PRIMARY KEY,
  rule_code       VARCHAR(64)  NOT NULL UNIQUE,
  rule_name       VARCHAR(128) NOT NULL,
  scope_type      VARCHAR(16)  NOT NULL, -- global/workshop/line/device/point
  scope_value     VARCHAR(255),
  alarm_level     VARCHAR(16)  NOT NULL,
  rule_type       VARCHAR(16)  NOT NULL, -- threshold/composite/no_data/custom
  expression      TEXT         NOT NULL, -- 示例：m.temperature > 90 && m.speed < 100 lasting 30s
  message_template VARCHAR(255),
  ai_trigger      TINYINT      NOT NULL DEFAULT 1,
  enabled         TINYINT      NOT NULL DEFAULT 1,
  priority        INT          NOT NULL DEFAULT 100,
  version         INT          NOT NULL DEFAULT 1,
  created_by      VARCHAR(64),
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 6.3.4 工单/配方下发

```sql
CREATE TABLE work_order (
  id               BIGINT PRIMARY KEY,
  order_no         VARCHAR(64) NOT NULL UNIQUE,
  factory_code     VARCHAR(32) NOT NULL,
  workshop_code    VARCHAR(32) NOT NULL,
  line_code        VARCHAR(32) NOT NULL,
  device_code      VARCHAR(64) NOT NULL,
  product_code     VARCHAR(64),
  product_name     VARCHAR(128),
  recipe_code      VARCHAR(64),
  recipe_name      VARCHAR(128),
  planned_qty      DECIMAL(18,2),
  order_status     VARCHAR(16) NOT NULL DEFAULT 'created',
  source           VARCHAR(16) NOT NULL, -- mes/manual
  mes_message_id   VARCHAR(64),
  created_by       VARCHAR(64),
  created_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted       TINYINT    NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plc_write_record (
  id               BIGINT PRIMARY KEY,
  device_code      VARCHAR(64) NOT NULL,
  write_type       VARCHAR(32) NOT NULL, -- recipe/workorder/param/manual
  request_id       VARCHAR(64) NOT NULL UNIQUE,
  payload          JSON        NOT NULL,
  source           VARCHAR(16) NOT NULL, -- mes/manual
  operator_id      VARCHAR(64),
  operator_name    VARCHAR(64),
  hmi_confirmed    TINYINT     NOT NULL DEFAULT 0,
  write_status     VARCHAR(16) NOT NULL, -- pending/sent/failed/confirmed/timeout
  error_message    VARCHAR(512),
  retry_count      INT         NOT NULL DEFAULT 0,
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_plc_write_device (device_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

说明：一期写 PLC 失败不自动重试，避免误写；记录保留状态机，后续可增加人工确认和回滚。
#### 6.3.5 MES 上传与审计/AI

```sql
CREATE TABLE mes_upload_queue (
  id                BIGINT PRIMARY KEY,
  message_id        VARCHAR(64)  NOT NULL UNIQUE, -- 本地生成幂等ID
  message_type      VARCHAR(32)  NOT NULL,         -- defects/alarms/production/parameters/status/workorder_result
  business_key      VARCHAR(128),
  factory_code      VARCHAR(32)  NOT NULL,
  workshop_code     VARCHAR(32)  NOT NULL,
  device_code       VARCHAR(64)  NOT NULL,
  payload           JSON         NOT NULL,
  collect_time      DATETIME     NOT NULL,
  status            VARCHAR(16)  NOT NULL DEFAULT 'pending', -- pending/sending/success/failed/dead
  retry_count       INT          NOT NULL DEFAULT 0,
  next_retry_at     DATETIME,
  last_error        VARCHAR(512),
  mes_response      VARCHAR(1024),
  sent_at           DATETIME,
  acknowledged_at   DATETIME,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_queue_status (status, next_retry_at),
  KEY idx_queue_business (device_code, collect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_log (
  id                BIGINT PRIMARY KEY,
  trace_id          VARCHAR(64),
  module            VARCHAR(32)  NOT NULL, -- config/plc/upload/export/ai/auth
  action            VARCHAR(64)  NOT NULL, -- create/update/delete/export/write/retry/confirm
  target_type       VARCHAR(32),
  target_id         VARCHAR(64),
  operator_id       VARCHAR(64),
  operator_name     VARCHAR(64),
  operator_role     VARCHAR(32),
  request_from      VARCHAR(32),  -- web/desktop/api/mes/edge
  request_ip        VARCHAR(64),
  before_data       JSON,
  after_data        JSON,
  result            VARCHAR(16)  NOT NULL, -- success/fail/partial
  error_message     VARCHAR(512),
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_module_time (module, created_at),
  KEY idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_diagnosis_record (
  id                BIGINT PRIMARY KEY,
  diagnosis_id      VARCHAR(64) NOT NULL UNIQUE,
  alarm_id          VARCHAR(64),
  factory_code      VARCHAR(32) NOT NULL,
  workshop_code     VARCHAR(32) NOT NULL,
  device_code       VARCHAR(64) NOT NULL,
  alarm_code        VARCHAR(64),
  input_snapshot    JSON        NOT NULL, -- 报警时刻关键参数、最近趋势片段摘要
  recalled_cases    JSON,                 -- 召回的历史案例ID和相似度
  rule_hits         JSON,
  model_name        VARCHAR(64),
  model_output      TEXT,
  conclusion        VARCHAR(512),
  root_cause        VARCHAR(512),
  suggestions       TEXT,
  confidence        DECIMAL(5,2),
  diag_status       VARCHAR(16) NOT NULL, -- queued/running/success/failed/fallback
  feedback_score    INT,                  -- 1-5，后续用于案例沉淀
  feedback_text     VARCHAR(512),
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ai_device_time (device_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_case_library (
  id                BIGINT PRIMARY KEY,
  case_code         VARCHAR(64) NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  fault_code        VARCHAR(64),
  device_type       VARCHAR(32),
  device_model      VARCHAR(64),
  symptoms          TEXT        NOT NULL,
  root_cause        TEXT        NOT NULL,
  solution          TEXT        NOT NULL,
  tags              VARCHAR(255),
  source            VARCHAR(32), -- manual/alarm/ai_confirmed
  enabled           TINYINT     NOT NULL DEFAULT 1,
  hit_count         INT         NOT NULL DEFAULT 0,
  success_count     INT         NOT NULL DEFAULT 0,
  created_by        VARCHAR(64),
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT     NOT NULL DEFAULT 0,
  KEY idx_case_fault (fault_code, device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 6.4 SQLite 边缘缓存表

SQLite 采用简化结构，表名与字段尽量与 MySQL 中心表保持一致，但只保留边缘运行必需字段。

```sql
CREATE TABLE edge_metric_stage (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  point_code       TEXT NOT NULL,
  device_code      TEXT NOT NULL,
  ts_ms            INTEGER NOT NULL, -- 采集时间毫秒戳
  value_double     REAL,
  value_string     TEXT,
  value_json       TEXT,
  quality          TEXT,
  source_type      TEXT,
  synced           INTEGER NOT NULL DEFAULT 0, -- 0未入中心/已入本地TDengine可标记1
  retry_count      INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE edge_upload_queue (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  message_id       TEXT NOT NULL UNIQUE,
  message_type     TEXT NOT NULL,
  business_key     TEXT,
  device_code      TEXT NOT NULL,
  payload          TEXT NOT NULL,
  collect_time_ms  INTEGER NOT NULL,
  status           TEXT NOT NULL DEFAULT 'pending',
  retry_count      INTEGER NOT NULL DEFAULT 0,
  next_retry_ms    INTEGER,
  last_error       TEXT,
  created_at_ms    INTEGER NOT NULL
);

CREATE TABLE edge_config_snapshot (
  config_key       TEXT PRIMARY KEY,
  config_value     TEXT NOT NULL,
  version          INTEGER NOT NULL,
  updated_at_ms    INTEGER NOT NULL
);
```

边缘缓存写入策略：

- 海康和 PLC 原始数据先写 `edge_metric_stage`，本地轮询线程批量写入本地 TDengine/中心 TDengine；若中心不可达，仅写本地并标记 `synced=0`。
- 需上传 MES 的数据在采集/事件产生时直接入 `edge_upload_queue`，由上传服务异步消费。
- 配置由中心下发后保存快照，边缘重启时优先读快照，避免中心不可用时无法启动。

## 7. 接口设计

接口分三层：对 MES 的 REST 上传接口（我们作为客户端调用）、采集驱动抽象接口（C# 内部扩展点）、内部模块 API 约定（边缘、中心、AI、前端之间的 HTTP/本地调用）。

### 7.1 MES 上传 REST 接口清单

首期建议采用标准 RESTful + JSON + JWT/Ticket 认证，路径以 `/api/v1/messages/*` 为前缀；如 MES 最终指定英科统一网关 `/api/dataportal/invoke`，则用 `MesUploader` 适配器兼容，不改业务层。

#### 7.1.1 认证

- 默认方案：`POST /api/v1/auth/token` 获取 JWT，有效期可配置；上传接口 Header 携带 `Authorization: Bearer <token>`。
- 兼容方案：如使用英科网关，则 Header 携带 `X-Ticket`，Body 内增加 `apiType/method` 字段。
- 所有上传请求都带 `X-Message-Id`、`X-Factory-Code`、`X-Workshop-Code`、`X-Edge-Node` 便于对端排障。

#### 7.1.2 上传接口清单

| 接口 | 方法 | 用途 | 说明 |
|---|---|---|---|
| `/api/v1/messages/defects` | POST | 海康缺陷上传 | 支持单条/批量，批量上限 500 |
| `/api/v1/messages/alarms` | POST | 设备报警上传 | 报警开始/恢复都传 |
| `/api/v1/messages/production` | POST | 产量/计数上传 | 分钟产量、班次产量均可 |
| `/api/v1/messages/parameters` | POST | 工艺参数上传 | 轮询/变化上报参数 |
| `/api/v1/messages/status` | POST | 设备状态上传 | 运行/停机/故障/待机 |
| `/api/v1/messages/workorder-result` | POST | 工单/配方执行结果 | 下发后 PLC 实际执行结果回传 |
| `/api/v1/messages/batch` | POST | 批量混合上传 | 可选，减少 HTTP 次数 |
| `/api/v1/workorders` | PULL/PUSH | MES 工单/配方下发 | 可中心轮询或 MES 主动回调 |

#### 7.1.3 报文示例

```json
POST /api/v1/messages/defects
Headers:
  Authorization: Bearer <token>
  X-Message-Id: 8f5b9d...
  Content-Type: application/json

{
  "messageId": "8f5b9d7b7a2e4d0f9a6b1c3e4d5f6001",
  "factoryCode": "QZ",
  "workshopCode": "WS01",
  "lineCode": "L01",
  "deviceCode": "HIK01",
  "eventTime": "2026-07-06T08:23:45+08:00",
  "defects": [
    {
      "defectId": "HK20260706000123",
      "defectType": "broken_finger",
      "defectLevel": "major",
      "imageRef": "",
      "position": "left_station_3",
      "count": 1
    }
  ],
  "ext": {}
}
```

#### 7.1.4 MES 返回约定

- 成功：HTTP 200 + `{"code":0,"message":"ok","data":{"received":true}}`
- 幂等冲突：HTTP 200 + `{"code":1001,"message":"duplicate"}` 本地视为成功
- 可重试失败：HTTP 5xx/超时/网络错误，进入指数退避重试
- 不可重试失败：HTTP 400/业务校验失败，标记为 `dead` 并报警人工处理

#### 7.1.5 MES 下发工单接口约定

如 MES 采用回调方式：

```http
POST /api/v1/callback/workorders
Authorization: Bearer <server-token>
Content-Type: application/json
```

报文字段：`orderNo / factoryCode / workshopCode / lineCode / deviceCode / productCode / recipeCode / plannedQty / issueTime`。
边缘收到后写 `work_order` 与 `plc_write_record`，由 PLC/HMI 人工确认生效。

### 7.2 采集驱动接口抽象（C#）

所有协议驱动必须实现统一接口，EdgeHost 通过反射或配置加载驱动实例。

```csharp
public interface IDeviceDriver : IAsyncDisposable
{
    string Protocol { get; }
    Task StartAsync(DeviceConnectionOptions options, CancellationToken ct);
    Task StopAsync(CancellationToken ct);
    Task<DriverHealthInfo> HealthCheckAsync(CancellationToken ct);

    Task<IReadOnlyList<DataPoint>> PollBatchAsync(
        IReadOnlyList<PointConfig> points,
        CancellationToken ct);

    Task SubscribeAsync(
        IReadOnlyList<PointConfig> points,
        Func<DataPoint, CancellationToken, Task> onDataChanged,
        CancellationToken ct);

    Task<PlcWriteResult> WriteAsync(
        WriteRequest request,
        CancellationToken ct);
}
```

关键模型伪代码：

```csharp
public record DataPoint(
    string DeviceCode,
    string PointCode,
    DateTime CollectTimeUtc,
    object Value,
    string Quality,
    string SourceType,
    IDictionary<string, object>? Ext = null);

public record WriteRequest(
    string RequestId,
    string DeviceCode,
    string WriteType,
    IReadOnlyList<WriteItem> Items,
    string Source,
    string OperatorId);

public record WriteItem(
    string PointCode,
    string Address,
    object Value,
    string DataType);

public record PlcWriteResult(
    string RequestId,
    bool Success,
    string? ErrorCode,
    string? ErrorMessage,
    bool HmiConfirmed);
```

驱动实现要求：

- `PollBatchAsync` 必须整体超时控制，默认单设备 3~5 秒，超时返回 `quality=timeout`。
- `SubscribeAsync` 必须包含重连逻辑；断线后自动恢复订阅并打日志。
- `WriteAsync` 一次只发一批，失败不重试，记录失败原因并返回给业务层报警。
- Hikvision 驱动实现为“HTTP 客户端 + 分页拉取 + 去重游标”，OPC-UA 用官方/成熟开源客户端，EIP 用 EtherNet/IP 客户端库。

### 7.3 内部模块 API 约定

内部 API 统一前缀：

- 边缘本地 API：`http://127.0.0.1:5200/api/edge`，仅供 WPF 和本机运维脚本调用。
- 中心 API：`http://<center-host>:5000/api/v1`，前端和边缘都访问。
- AI API：`http://<ai-host>:8000/api/ai`，中心 API 和边缘兜底客户端访问。

#### 7.3.1 中心 API 主要分组

| 模块 | 路径前缀 | 典型接口 |
|---|---|---|
| 设备与配置 | `/api/v1/devices` | 设备 CRUD、点位 CRUD、规则 CRUD、班次配置 |
| 实时数据 | `/api/v1/realtime` | 设备最新值、看板汇总、大屏摘要 |
| 历史数据 | `/api/v1/history` | 趋势查询、原始记录导出、条件检索 |
| 报警 | `/api/v1/alarms` | 报警列表、确认、关闭、AI 诊断结果 |
| 分析 | `/api/v1/analytics` | OEE、产量、良率、帕累托、SPC、班次报表 |
| 上传 | `/api/v1/upload` | 上传状态、补传任务、失败队列 |
| AI | `/api/v1/ai` | 诊断触发、历史诊断、案例库维护、问答入口 |
| 审计 | `/api/v1/audit-logs` | 查询、导出 |

#### 7.3.2 实时与历史查询接口签名示例

```http
GET /api/v1/realtime/dashboard?factoryCode=QZ&workshopCode=WS01
Response 200:
{
  "snapshotTime": "2026-07-06T08:30:00+08:00",
  "deviceCount": 48,
  "runningCount": 40,
  "alarmCount": 2,
  "shiftOutput": 12500,
  "defectRate": 0.012,
  "devices": [ ... ]
}

GET /api/v1/history/trend?deviceCode=PK01&pointCode=temperature
    &start=2026-07-06T00:00:00+08:00&end=2026-07-06T12:00:00+08:00&interval=1m
Response 200:
{
  "points": [
    { "ts": "2026-07-06T08:00:00+08:00", "v": 86.2 },
    { "ts": "2026-07-06T08:01:00+08:00", "v": 86.5 }
  ]
}
```

#### 7.3.3 AI 接口签名

```http
POST /api/ai/diagnose
Request:
{
  "alarmId": "ALM202607060001",
  "factoryCode": "QZ",
  "workshopCode": "WS01",
  "deviceCode": "PK01",
  "alarmCode": "TEMP_HIGH",
  "alarmMessage": "模温过高",
  "alarmTime": "2026-07-06T08:20:00+08:00",
  "snapshot": { ... },
  "recentWindowMinutes": 30
}

Response 200:
{
  "diagnosisId": "DIAG202607060001",
  "conclusion": "疑似冷却水流量不足导致模温持续偏高",
  "rootCause": "冷却阀动作迟缓 / 水路堵塞 / 配方温度设置偏高",
  "suggestions": [
    "检查冷却水压力和阀门动作",
    "核对当前配方温度设定是否与工单一致",
    "若近期换料，观察原料温度影响"
  ],
  "confidence": 0.86,
  "cases": [
    { "caseCode": "CASE-PK-0037", "similarity": 0.91, "title": "冷却阀滤网堵塞导致模温高" }
  ],
  "ruleHits": ["rule_temp_high_30s", "rule_pressure_low_combo"],
  "model": "qwen2.5-7b-instruct"
}
```

## 8. 关键流程时序

### 8.1 采集→缓存→上送 MES

流程目标：确保“至少一次”送达，断网可补传，重复数据由本地幂等 ID 控制。

```text
PLC/海康
   | 1.轮询/订阅拉取原始数据
   v
EdgeHost Collector
   | 2.标准化为 DataPoint（赋 messageId/traceId/本地时间戳）
   | 3.质量检查 good/uncertain/bad/timeout
   v
EdgeStorage SQLite
   | 4.写 edge_metric_stage / edge_upload_queue（本地事务）
   v
Local Writer / Upload Scheduler
   | 5a.批量写 TDengine（本地/中心）
   | 5b.按 message_type 组装 MES JSON
   v
TDengine / MES HTTP API
   | 6a.TDengine 返回成功 -> 更新 synced=1
   | 6b.MES 返回成功/duplicate -> 标记 success
   | 6c.MES/网络失败 -> 重试，超限转 dead 并预警
   v
Audit/Log/Frontend
   | 7.大屏轮询/推送实时状态，上传监控页看队列深度
```

伪代码约定：

```csharp
// 上传调度主循环（伪代码）
while (!stoppingToken.IsCancellationRequested)
{
    var batch = await uploadQueue.PeekPendingAsync(batchSize: 200, ct);
    foreach (var msg in batch.OrderBy(x => x.CollectTime))
    {
        try
        {
            await mesClient.SendAsync(msg, ct);
            await uploadQueue.MarkSuccessAsync(msg.MessageId);
        }
        catch (RetryableException ex)
        {
            await uploadQueue.MarkRetryAsync(msg.MessageId, ex.Message, backoff);
        }
        catch (FatalException ex)
        {
            await uploadQueue.MarkDeadAsync(msg.MessageId, ex.Message);
            await alarmService.RaiseUploadDeadAsync(msg);
        }
    }
    await Task.Delay(intervalMs, stoppingToken);
}
```

关键约束：

- 上传顺序优先按采集时间，避免新数据先到而老数据丢失。
- 同一 `messageId` 重试请求保持相同报文，避免 MES 侧无法去重。
- 队列长度阈值（如超过 5000 条/离线超过 2 小时）触发本地预警；超过上限（可配）停止接收新的上传任务但保留采集存储，并提示人工导出。

### 8.2 PLC 配方/工单下发

流程目标：MES 或人工界面均可下发，HMI/PLC 侧人工确认后才生效；失败报警但不自动重试。

```text
MES回调 / WPF/前端人工录入
   | 1.提交工单/配方下发请求
   v
CenterApi/EdgeApi 校验
   | 2.写 work_order + plc_write_record(pending)
   | 3.记录审计日志
   v
EdgeHost PlcWriter
   | 4.调用 driver.WriteAsync(payload)
   v
PLC/HMI
   | 5.PLC 接收但不自动生效
   | 6.操作工在 HMI 点击确认
   v
EdgeHost 状态回读
   | 7.读取确认点位/回执
   | 8.更新 plc_write_record=confirmed/failed
   | 9.回传 workorder_result 给 MES（如成功）
   v
Audit/Alarm
   | 10.失败 -> 报警提示工艺/设备人员；不自动重试
```

状态机：`pending -> sent -> confirmed -> closed`，异常分支：`pending -> failed`、`sent -> timeout -> failed`。

### 8.3 报警→AI 诊断

流程目标：报警触发后优先规则引擎，再进入 AI RAG 诊断，最后给出融合结论；网络或模型不可用时规则兜底。

```text
Alarm Engine
   | 1.规则命中/设备自报故障 -> 创建 alarm_event(status=open)
   | 2.判定 ai_trigger=1 -> 写 ai_diagnosis_record(queued)
   v
AI Router
   | 3.截取报警前30分钟关键点位窗口
   | 4.召回历史相似案例（向量+规则标签）
   | 5.拼 Prompt：设备信息+报警信息+实时快照+历史案例+规则命中
   v
Center AI Service(FastAPI+Ollama)
   | 6.优先调用中心大模型；失败则尝试本地小模型
   | 7.结构化输出 JSON：结论/根因/建议/置信度/引用案例
   v
Result Fusion
   | 8.规则结果 + 模型结果融合（规则必显，模型作建议）
   | 9.置信度<0.6 标记为“仅供参考”
   v
Frontend/WPF
   | 10.报警详情展示结论、建议、相似案例、反馈按钮
   v
Feedback
   | 11.工程师反馈是否有用；高评价案例沉淀入案例库
```

AI 不可用时降级路径：只展示规则命中、点位异常列表、历史相同故障代码 TOP 案例列表，不阻塞报警闭环。

## 9. AI 故障诊断模块方案

一期 AI 聚焦故障诊断 MVP，采用“本地小模型/中心小模型 + RAG 历史案例库 + 规则兜底”的三段式架构，不做聊天助手，不开放开放式闲聊。

### 9.1 模块位置

- 中心主推理：部署在 GPU 服务器，默认模型 `Qwen2.5-7B-Instruct`/`14B-Instruct` 或 `DeepSeek-R1-Distill-7B`。
- 边缘兜底：工控机部署 Ollama CPU 小模型（如 `qwen2.5-3b`），仅在中心不可用时启用，输出简化版建议。
- 所有数据进厂内局域网，不调用公网 API。

### 9.2 输入特征

AI 诊断请求包含以下结构化输入：

- 报警信息：`alarmCode/alarmMessage/alarmLevel/startTime/deviceCode/deviceType/plcModel`
- 实时快照：报警时刻关键点位最新值（温度、压力、速度、计数、运行状态、配方号等）
- 时间窗口片段：报警前 5/15/30 分钟聚合值、斜率、是否波动异常
- 规则命中：阈值规则/组合规则命中情况
- 设备上下文：型号、班次、当前工单、产品、配方
- 历史维修摘要：近 7/30 天同类报警次数、MTTR/MTBF
- 相似案例：RAG 召回的 Top-K 案例

### 9.3 RAG 方案

- 案例库来源：手工录入、历史报警+维修结果、AI 诊断经人工确认后沉淀。
- 检索方式：
  1. 粗排：按 `device_type + fault_code + 关键词` 过滤近 1~3 年案例。
  2. 精排：对 `symptoms + root_cause + solution` 做向量检索，优先选 bge-m3 / qwen-embedding 等中文向量化模型，嵌入向量缓存到 Redis/MySQL（MVP 可先用 TF-IDF/BM25 快速落地，后续替换向量库）。
  3. 重排：按同设备型号、同车间、同班次、时间近、历史成功率加权。
- 案例输出最多给模型 3~5 条，避免上下文过长。

### 9.4 Prompt 设计原则

- 严格中文输出，禁止编造不确定事实；若信息不足必须明确说明“无法判断，建议现场检查”。
- 强制结构化 JSON 输出，避免大段废话；前端解析 `conclusion/root_cause/suggestions/confidence/cases/needManualCheck`。
- 安全提示必须加在 system prompt 中：
  - 不得给出断电、拆机、带压维修等危险操作作为首选建议，危险操作前必须提示“挂牌上锁/由维修电工执行”。
  - 对涉及人身安全的报警（如安全门、急停），优先建议立即停机并通知班组长，不做推测性分析。

### 9.5 规则与模型融合策略

- 规则引擎先跑，所有阈值/组合条件命中结果作为强约束告诉模型。
- 模型输出不得覆盖“已确认安全报警/停机类报警”的强制处置建议。
- 当置信度低于阈值（默认 0.6）时，前端展示“AI 参考建议”，并突出规则结果。
- 当出现模型异常（格式错误、超时、5xx）时，接口返回 fallback 结果，不影响报警本身。

### 9.6 伪代码

```python
# FastAPI 诊断服务伪代码
@app.post("/api/ai/diagnose")
async def diagnose(req: DiagnoseRequest):
    rule_hits = rule_service.evaluate(req.device_code, req.alarm_time)
    features = feature_builder.build(req, rule_hits)
    cases = case_retriever.search(
        device_type=req.device_type,
        fault_code=req.alarm_code,
        symptoms=build_symptom_text(features),
        topk=5,
    )
    prompt = prompt_builder.render(
        template="fault_diagnose_cn_v1",
        req=req,
        features=features,
        cases=cases,
        rules=rule_hits,
    )
    try:
        raw = await ollama_client.generate(model=settings.model_name, prompt=prompt)
        parsed = output_parser.parse_json(raw)
        parsed = safety_filter.apply(parsed, rule_hits)
        record_result(parsed, cases, rule_hits)
        return parsed
    except ModelError:
        return fallback_builder.from_rules_and_cases(rule_hits, cases)
```

### 9.7 评估与反馈

- 诊断结果必须支持“有用/无用/误判/采纳处置”四档反馈。
- 每周从高评价结果中沉淀案例，低质量结果加入负例集。
- MVP 不做自动学习闭环，只做人工确认后入库。

## 10. 部署方案

### 10.1 拓扑结构

- 车间边缘：每车间 1 台 Windows 工控机，部署 `Idp.EdgeHost` Windows Service、WPF 桌面壳、本地 SQLite、可选本地 Ollama CPU 模型、可选单节点 TDengine（若车间需完全离线自治）。
- 工厂中心：部署 `Idp.CenterApi`、MySQL 8、TDengine 集群或单机、Redis、Nginx、Vue3 静态站点、FastAPI AI 服务、Ollama GPU 推理。
- 网络要求：工控机到 PLC/海康必须局域网互通；工控机到中心服务器建议固定 IP/白名单；中心服务器到 MES 走工厂内网。

文字拓扑：

```text
MES <---> Center API/Nginx <---> EdgeHost(Win x N) <---> PLC/Hik
                         |
                         +---> TDengine/MySQL/Redis
                         +---> AI(Ollama GPU)
                         +---> Vue3 Web
WPF 桌面壳 <---> EdgeHost(本地 API)
```

### 10.2 Windows Service 自启

- `Idp.EdgeHost` 使用 .NET 8 `UseWindowsService()` 直接发布为 Windows Service；服务名建议 `IdpEdgeHost`。
- 启动类型设为“自动（延迟启动）”，故障恢复策略设为“第一次/第二次/后续均重启服务”，重启间隔 1 分钟。
- WPF 桌面壳加入“任务计划程序”：用户登录后启动，全屏打开本地看板页；若检测 EdgeHost 未运行则提示一键启动。
- 提供 `install.ps1`：创建服务、写 appsettings、初始化 SQLite、注册 NTP、打开防火墙端口。

### 10.3 NTP 时间同步方案

- 中心服务器作为车间二级 NTP 源，指向工厂/集团 NTP；若工厂无 NTP，则指向外网 NTP 并保留中心本地时间作为降级。
- 工控机配置主 NTP 为中心服务器，备 NTP 为内网网关或 PLC 时钟源。
- EdgeHost 启动时检测系统时间漂移，若与中心差超过 5 秒则写报警，超过 30 秒则在大屏提示。
- 所有采集数据同时记录：设备时间（如果协议可取）、边缘系统时间、边缘采集处理时间；入库以边缘 UTC 时间为主，设备时间作参考。

### 10.4 备份与恢复

- 一期目标：自动备份近 3 个月，初期先脚本化手动执行，但目录和命名规范先定下来。
- MySQL：每日 `mysqldump`，保留 30 天；中心配置表和审计日志单独备份。
- TDengine：使用官方备份/导出工具，至少每日增量、每周全量；原始数据可按库备份，聚合数据必要时可重算。
- SQLite：EdgeHost 每次启动前复制一份数据库到 `backup/edge/`，每小时滚动备份近 24 小时，避免边缘库损坏导致待上传数据丢失。
- 配置：`appsettings.json`、点位导出 JSON、设备模板统一进 Git 或中心配置目录，换工控机可一键复现。

### 10.5 配置与发布

- 环境区分：`appsettings.json / appsettings.QZ-WS01.json / appsettings.Center.json`。
- 敏感配置（MES Token、数据库密码）一期可先用文件+NTFS 权限控制，后续接入环境变量或密钥管理。
- 发布包统一输出 `publish/edge`、`publish/center`、`publish/ai`、`publish/web`，边缘包要控制在 200MB 内，便于 U 盘/远程传输。

## 11. 5 天 MVP 攻坚里程碑分解

MVP 目标：5 天内打通“海康采集→本地存储→MES 上传通道→基础大屏→AI 诊断骨架→部署自启”，优先青州现场第一台工控机、第一台海康、第一台包装机/点数机样本点位。开发顺序以“跑通链路优先，页面和优化后补”为原则。

### Day1：工程骨架与部署底座

- 创建解决方案和项目骨架：`Idp.Common`、`Idp.EdgeHost`、`Idp.CenterApi`、`idp-web`、`idp_ai`、`Idp.Desktop`。
- 初始化数据库脚本：TDengine 超级表、MySQL 核心表、SQLite 边缘表；准备 EF Core 上下文和 TDengine 写入封装。
- 配置中心基础接口：工厂/车间/产线/设备/点位 CRUD，先提供接口和最小管理页面或手工 SQL 初始化。
- EdgeHost Windows Service 模板：日志、配置加载、健康检查、本地 HTTP 健康端点 `/api/edge/health`。
- 完成 Nginx/CenterApi/MySQL/Redis/TDengine 本地开发环境启动脚本。
- 完成 Day1 验收：所有服务可启动，中心 API 可连通数据库，EdgeHost 可安装为服务并自启。

### Day2：采集驱动与本地存储

- 实现海康 HTTP 采集驱动：登录/鉴权封装、缺陷列表和报警列表拉取、分页游标、去重。
- 实现 OPC-UA 采样驱动骨架：连接、批量轮询、订阅回调、断线重连；先接 1 台包装机若干测试点。
- 实现 EIP 驱动骨架：连接、轮询、状态读取；先接 1 台点数机。
- 实现 DataPoint 标准化管线：点位映射、缩放/偏移、质量标记、UTC 时间归一。
- 实现本地 SQLite 入库和 TDengine 批量写入，验证单设备连续写入稳定。
- 完成 Day2 验收：海康和样本 PLC 能持续采集到数据，TDengine 能查到原始测点，断线重连后自动恢复。

### Day3：MES 上传与补传链路

- 实现 MES 上传客户端：JWT 获取、重试策略、批量 POST、duplicate 幂等识别。
- 实现 `mes_upload_queue` 入库和发送调度：海康缺陷/报警先接入，参数和状态预留。
- 实现失败重试、dead 队列、队列积压报警；提供本地“手动补传/导出 JSON”接口。
- 完成 MES 接口模拟服务（Postman Mock 或本地最小 API），因为现场暂无测试 IP。
- 审计日志先接入：发送成功/失败、手工补传、导出操作必须落审计。
- 完成 Day3 验收：模拟 MES 可收到海康缺陷数据；断开模拟服务 10 分钟再恢复，数据可自动补传且不重复。

### Day4：大屏基础版、报警规则、AI 骨架

- Vue3 搭建布局：工厂/车间切换、左侧设备树、中央产量/良率/故障、右侧实时报警滚动。
- 实现实时看板 API：设备最新值、设备状态统计、最近报警、今日产量、缺陷率，大屏刷新 30 秒内。
- 报警规则引擎基础版：支持单点位阈值和持续时长规则，触发写 `st_alarm_event` 和 MySQL 报警表。
- FastAPI AI 服务搭起来：`/api/ai/diagnose` 接口、Ollama 调用封装、结构化 JSON 输出、fallback。
- 先录入 10~20 条典型故障案例（模温高、气压低、送料异常等），BM25/关键词检索版 RAG 可用。
- 完成 Day4 验收：大屏可看实时数据；造一个温度超阈值报警，AI 能返回中文诊断建议和参考案例。

### Day5：桌面壳、部署脚本、全链路联调

- WPF 桌面壳：启动检测 EdgeHost、全屏 WebView2 打开大屏、显示服务状态/本地日志入口。
- 编写 `deploy/edge/install.ps1`：安装服务、配置 appsettings、注册计划任务启动 WPF、开放本地端口。
- 编写 NTP 配置脚本、SQLite 备份脚本、日志轮转策略。
- 全链路联调：海康采集→TDengine→MES 上传模拟→大屏刷新→报警触发→AI 返回→审计可查。
- 修复阻塞问题，补最小化 README/部署说明，输出首个现场安装包。
- 完成 Day5 验收：一台工控机重启后服务和大屏自动起来；从采集到 MES 到 AI 的主链路连续跑 2 小时无重大异常。

## 12. 后续迭代路线

在 5 天 MVP 跑通后，建议按“先稳定复制，再分析闭环，再智能优化”的节奏推进。

### 一期 MVP+（2~6 周）

- PLC 点位可视化配置界面：地址、类型、频率、阈值、是否上传、是否报警、是否写入。
- PLC 数据上传 MES：工艺参数、状态、产量、报警全量接入。
- 报表基础版：日/周/月产量、良率、故障 TOP、班次汇总、Excel 导出。
- OEE 基础计算与展示；SPC 控制图先做单指标 X/MR 图。
- AI 案例库管理界面、反馈闭环、更多设备型号案例沉淀。
- 多车间模板复制：点位模板、设备模板、规则模板一键复制到新车间。

### 二期（约 +2 个月）

- MES 工单/配方下发正式联调，HMI 确认状态可视化。
- 规则引擎增强：组合条件、防抖、班次维度、设备组维度、连锁报警抑制。
- 历史回放：趋势曲线拖拽回放，报警时刻前后窗口一键查看。
- 帕累托、MTTR/MTBF、相关性分析、工艺参数对比。
- 边缘中心分层缓存优化，历史查询接口性能优化（按时间分区/预聚合）。
- 备份自动化：每日任务、备份成功失败通知、恢复演练脚本。

### 三期（约 +2 个月）

- 预测性维护：基于历史趋势和故障库做关键部件预警（如温度漂移、压力波动、计数偏差）。
- 工艺优化建议：参数区间推荐、同型号设备对标、换型差异分析。
- 缺陷根因分析：海康缺陷类型与工艺参数、班次、原料批次关联分析。
- AI 自动生成日报/周报初稿，人工确认后导出。
- 可引入 MQ（Kafka/RabbitMQ）做中心侧异步解耦，支撑 23 车间汇聚规模。

### 四期（平台化与多基地）

- 多基地集团视图：跨工厂产能、良率、故障对比、复制最佳实践。
- 飞书/企业微信/小程序端：管理层轻量看板、报警推送、审批类操作。
- 远程运维能力：远程日志采集、配置下发、版本更新、心跳监控。
- 权限体系：操作工/班长/工艺/设备/IT/管理员分级，数据按车间隔离。
- 模型分层：中心大模型+边缘小模型协同，MaaS 化管理模型版本与提示词。

## 13. 开发约定与风险控制

### 13.1 开发约定

- 日志统一使用结构化日志（Serilog），字段至少包含 `traceId/deviceCode/factoryCode/messageId`。
- 所有异步任务必须传 `CancellationToken`，服务停止时优雅退出，避免写半截数据。
- 接口返回统一信封：`{ code, message, data, traceId }`，HTTP 状态码和业务码区分可重试/不可重试。
- 时间统一在后端处理，前端不做复杂时区转换。
- 数据库迁移：MySQL 用 EF Core Migration；TDengine 用版本化 SQL 脚本启动时检查执行。
- 代码提交粒度按“一个链路可用一次提交”，避免 5 天攻坚中堆积大量不可运行代码。

### 13.2 主要风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| MES 接口文档暂缺 | 上传无法真实联调 | 先做接口抽象+Mock 服务，适配层预留英科/REST 两套 |
| OPC-UA/EIP 现场点位不稳定 | 采集成功率低 | 驱动层加重连、超时、质量标记；先样本点位打通，再扩点 |
| 工控机性能有限 | 本地 AI 或高频写入卡顿 | 边缘 AI 默认关闭，主要推理放中心；批量写 TDengine，SQLite 只做队列 |
| 时间不同步 | 数据乱序、补传错位 | NTP+漂移告警+双时间戳 |
| 单人开发范围大 | 5 天后仍难稳定 | 严格按 MVP 范围，非核心页面和复杂分析后置 |
| AI 误判误导维修 | 现场风险 | 规则兜底、置信度展示、危险操作强提示、反馈闭环 |

## 14. 验收标准（一期 MVP 口径）

在青州首站单车间试点环境下，至少满足以下标准：

1. 海康视觉缺陷/报警可稳定采集，连续运行 24 小时无服务崩溃，数据无 5 分钟以上缺口。
2. 样本包装机/点数机 PLC 点位可按配置轮询和订阅，断线 5 分钟内自动恢复。
3. MES 模拟接口下，海康数据可定时批量上传，断网 30 分钟内可自动补传，重复发送被幂等识别。
4. 大屏实时刷新延迟 ≤30 秒，能看到设备状态、产量、报警滚动、良率摘要。
5. 阈值报警可触发并显示在报警页，AI 能返回中文原因+建议+相似案例，模型异常时规则信息仍可见。
6. 写 PLC、改配置、手工补传、导出操作均有审计日志可查。
7. EdgeHost 注册为 Windows Service，工控机重启后自动启动；WPF 壳自动进入大屏页。
8. 提供完整安装包和部署脚本，新工控机部署时间控制在 2 小时内（不含现场网络协调）。

---

文档维护说明：本版本为 v0.1，作为开发启动基线。随着现场联调推进，若 MES 接口规范、PLC 点位表、AI 模型版本、部署拓扑发生变化，应在本文件持续更新，并在开头版本号中递增。
---

## 15. 5 天 MVP 攻坚 Day1-Day5 详细任务分解（补充）

> 本节是对前文“11. 5 天 MVP 攻坚里程碑分解”的可执行版展开，用于个人开发排期和每日验收。每天按“目标 / 具体任务 / 产出 / 验收标准”四列执行，当天结束必须满足验收标准才能进入下一天，避免链路问题留到最后集中爆发。

### 15.1 Day1：工程骨架、数据库与部署底座

**目标**：搭建能启动、能部署、能健康检查的工程骨架，打通开发环境，不做业务功能。

**具体任务**：
1. 创建解决方案 IndustrialDataPlatform.sln，建立 Idp.Common、Idp.Protocol、Idp.Infrastructure、Idp.EdgeHost、Idp.CenterApi、Idp.Desktop、idp-web、idp_ai 八个基础项目。
2. 配置 .NET 8 类库/服务间引用关系：EdgeHost 和 CenterApi 引用 Common/Protocol/Infrastructure；WPF 仅引用 Common 和本地 HTTP 客户端。
3. 引入基础包：Serilog、Microsoft.Extensions.Hosting.WindowsServices、StackExchange.Redis、MySql.EntityFrameworkCore 或 Pomelo、TDengine 驱动、OPC-UA 客户端库（OPCFoundation.NetStandard.Opc.Ua）、EtherNet/IP 客户端库。
4. 编写数据库初始化脚本：TDengine 原始测点和报警超级表、MySQL 设备/点位/审计/上传任务/案例库表、SQLite 边缘缓存与上传队列表。
5. 搭建 CenterApi 最小服务：健康检查 /health、设备/点位最小 CRUD、统一返回信封 {code,message,data,traceId}。
6. 搭建 EdgeHost Windows Service 模板：BackgroundService 主循环、配置加载、本地健康端点 http://localhost:18080/api/edge/health、结构化日志、优雅停止。
7. 本地开发环境准备：MySQL 8、Redis、TDengine 通过 docker-compose 或本机安装启动；准备 Postman 集合和 MES Mock 最小 API 工程。
8. 建立 deploy/edge/install.ps1 脚本骨架：创建服务、复制配置、开放端口、注册计划任务。
9. 前端建立 Vue3 + Vite + ECharts + Pinia + Vue Router 基础工程，准备布局骨架（顶栏、侧边设备树、主看板区、右侧报警区）。
10. AI 服务建立 FastAPI 工程骨架：/api/ai/health、/api/ai/diagnose 占位接口、Ollama 客户端封装、配置文件。

**产出**：可编译解决方案；初始化 SQL 脚本；可启动的 CenterApi、EdgeHost、AI 服务、前端空壳；MES Mock 工程；基础部署脚本骨架。

**验收标准**：
1. 一键运行脚本或 Visual Studio 多启动配置能同时拉起 CenterApi、MySQL、Redis、TDengine、AI 服务、前端。
2. EdgeHost 可以安装/卸载为 Windows Service，重启工控机后服务自动启动。
3. 调用 /health 和 /api/edge/health 均返回 200 和服务版本号。
4. 数据库初始化脚本可重复执行不报错。
5. 前端首页能正常打开并显示“链路未接入”占位内容。
6. Git 仓库形成第一次可运行提交，所有项目均能编译通过。

### 15.2 Day2：采集驱动、标准化管线与本地存储

**目标**：跑通海康 HTTP 采集和样本 PLC 采集，把数据稳定写入 SQLite/TDengine，具备断线重连基础能力。

**具体任务**：
1. 实现 ICollectorDriver 接口：StartAsync/StopAsync/ReadAsync/OnDataArrived，定义统一数据模型 DataPoint、DeviceEvent、AlarmEvent。
2. 海康驱动：封装登录鉴权、分页拉取缺陷列表、报警列表、新报警事件检测、增量游标、失败重试、超时设置；解析海康字段为统一缺陷/报警事件对象。
3. OPC-UA 驱动：基于官方客户端实现连接、会话、订阅、批量轮询；先针对欧姆龙 NX102 和汇川 AC801 各接若干测试点，验证布尔/整型/浮点/字符串类型读取。
4. EIP 驱动：实现点数机连接、轮询、状态/节拍/故障码读取；先接 1 台样本点数机。
5. 点位映射：实现 PointConfig -> DataPoint 的转换，包括地址、类型、缩放、单位、采集周期、质量标记；对读失败、超时、类型不匹配的数据标记 quality=bad/uncertain。
6. 时间处理：统一写入 collect_time_local、collect_time_utc、ingest_time，处理设备时间与本机时间不一致时的兜底逻辑。
7. 本地 SQLite 写入：原始数据临时落地、去重键（设备+点位+采集时间+值哈希）检查、批量插入事务。
8. TDengine 写入：实现批量写 st_metric_raw 和 st_alarm_event 的封装，子表自动创建，批量失败重试，写入延迟统计。
9. 断线重连：海康 401/网络错误自动重新登录；OPC-UA Session 断开按退避重连；EIP 连接异常标记通讯状态。
10. 最小调试页或日志：按设备打印最近采集时间、采集条数、最近错误、TDengine 写入延迟，便于现场确认数据在流动。

**产出**：三类采集驱动骨架；统一数据模型；SQLite 和 TDengine 写入管线；断线重连逻辑；样本设备采集验证记录。

**验收标准**：
1. 海康接口可连续拉取 2 小时无崩溃，缺陷和报警数据能写入 TDengine。
2. 至少 1 台包装机和 1 台点数机样本点位可持续采集，温度、压力、速度、计数、状态、故障码字段能正确解析。
3. 人为断开 PLC/海康网络 5 分钟再恢复，驱动能自动重连，期间产生明确“通讯异常”事件。
4. TDengine 中可通过 SQL 查到样本测点数据，时间戳、设备标签、点位编码正确。
5. 所有采集记录都带有 	raceId、设备编码、采集时间、质量标记。
6. 连续采集 2 小时无内存持续上涨、无数据库锁死、无未处理异常导致服务退出。

### 15.3 Day3：MES 上传、补传队列、幂等与审计

**目标**：打通 MES 上传通道（先用 Mock），实现至少一次投递、幂等去重、离线缓存补传和关键操作审计。

**具体任务**：
1. 定义 IMesUploader 接口：SendDefectAsync/SendAlarmAsync/SendProductionAsync/...，通过配置切换 RESTful 或统一网关实现。
2. 实现 RESTful 上传器：JWT 获取与刷新、JSON 序列化、超时设置、重试退避、HTTP 状态码可重试/不可重试分类。
3. 设计 SQLite mes_upload_queue：字段包含 message_id,message_type,device_code,payload_json,status,retry_count,next_retry_time,last_error,created_at,sent_at,confirmed_at。
4. 实现上传调度器：按采集时间顺序扫描待发队列，批量拉取，单条发送成功后标记 success；可重试异常执行指数退避；不可重试异常直接 dead。
5. 幂等规则：本地生成全局唯一 messageId（雪花 ID 或 UUID），重复发送保持相同报文和 ID，MES 返回 duplicate 也视为成功。
6. 断网补传：停止 MES Mock 10~30 分钟，期间队列持续堆积，恢复后自动按时间顺序补发，不重复、不乱序。
7. 队列告警：队列深度超过阈值、死信数超过阈值、单条重试次数超过阈值时写入本地告警并在状态页展示。
8. 人工补传/导出接口：提供 API 支持按时间段和消息类型重新入队或导出 JSON 文件，导出操作写审计。
9. 审计日志：发送成功/失败、手工补传、导出、队列清空、配置变更都写入 udit_log。
10. 对账基础：记录每条消息的发送次数、最后发送状态、MES 返回码和返回摘要，支持按时间范围统计成功率。

**产出**：MES 上传客户端；上传队列表和调度器；幂等与补传逻辑；Mock MES 服务；审计日志写入；上传监控接口。

**验收标准**：
1. 海康缺陷和报警数据能自动进入队列并发送到 MES Mock，Mock 返回 200 时状态变为 success。
2. MES Mock 返回 duplicate 时本地标记成功，不产生重复业务数据。
3. 停止 Mock 服务 10 分钟再恢复，队列中的消息能自动补发，补发顺序按采集时间从早到晚。
4. 连续发送 1 万条以上数据，不出现消息丢失；数据库中 success 数 + dead 数 + pending 数与入队数一致。
5. 手工补传和 JSON 导出接口可正常调用，操作记录可在审计日志表中查到。
6. 上传监控接口能正确返回队列深度、成功率、死信数、最近错误。

### 15.4 Day4：基础大屏、规则引擎、AI 故障诊断骨架

**目标**：做出可用的基础实时看板，打通阈值报警和 AI 诊断接口闭环，让现场能“看见数据、看见报警、看见建议”。

**具体任务**：
1. 前端大屏布局：顶部显示工厂/车间/时间，中间为设备状态总览、今日产量、良率、OEE 占位卡片，右侧为实时报警滚动列表，底部为各设备状态块。
2. 实时接口：CenterApi 提供“最新设备状态、设备状态统计、今日产量/良率、最近 N 条报警、最近 N 条缺陷”聚合查询，优先查 Redis 缓存或分钟聚合表，大屏 30 秒轮询。
3. 设备状态映射：运行/停机/故障/通讯异常/待料等状态按颜色统一映射，状态未知时显示灰色并标记未知。
4. 报警规则引擎基础版：支持单点位阈值规则（大于/小于/等于/区间）+ 持续时长防抖，命中后写入 st_alarm_event 和 MySQL 报警表，规则变更热加载。
5. 报警列表 API：按时间倒序返回报警，支持状态过滤（活动/已恢复/已确认），支持查询报警前后窗口数据。
6. AI 诊断接口：FastAPI /api/ai/diagnose 接收设备、报警、规则命中、前后窗口样本、相似案例；调用 Ollama 生成结构化 JSON；超时或格式错误走 fallback。
7. 案例库 MVP：手工录入 10~20 条典型故障案例（模温高、气压低、送料异常、计数偏差、点数机节拍异常等），先用 SQL 关键词 + BM25 粗检索，不急于上向量库。
8. Prompt 初版：约束输出 JSON 字段（结论、可能原因、建议步骤、置信度、相似案例、需人工检查项），并加入安全提示：禁止将危险作业作为首选建议。
9. 前端报警详情弹窗：展示报警信息、规则命中、最近参数快照、AI 建议、相似案例、反馈按钮。
10. 反馈最小闭环：用户点击“有用/无用/误判”先入库，MVP 不做自动训练，但保证数据可沉淀。

**产出**：基础大屏页面；实时聚合接口；阈值报警规则引擎；AI 诊断接口；首批案例库；报警详情弹窗。

**验收标准**：
1. 大屏能在 30 秒内看到设备状态变化和报警滚动，无持续空白。
2. 人为构造温度超阈值或状态异常时，1 个周期内产生报警并在大屏右侧出现。
3. 报警详情页能看到报警基础信息、相关点位快照、AI 返回的中文结构化建议。
4. 关闭 Ollama 或模型报错时，AI 接口仍返回 fallback 结果（规则命中+历史案例提示），不抛 500 导致页面崩溃。
5. 手工录入的案例能被检索到并出现在相似案例区域。
6. 大屏在 1920×1080 分辨率下布局不重叠、不溢出。

### 15.5 Day5：桌面壳、部署脚本、全链路联调与首个安装包

**目标**：把边缘端变成可交付的工控机运行环境，完成从采集到 MES 到 AI 的全链路联调，输出第一个可现场安装的 MVP 包。

**具体任务**：
1. WPF 桌面壳：启动后检测 EdgeHost 服务状态，未运行则提示一键启动；嵌入 WebView2 默认打开本地/中心大屏页；顶部显示服务状态、最近错误、本机 IP、时间同步状态。
2. 桌面壳全屏模式：支持开机自动进入全屏，退出全屏需管理员或指定快捷键，避免车间现场误关。
3. 本地诊断入口：在桌面壳菜单中提供“查看日志、打开配置目录、健康检查、手动触发补传、导出诊断包”的快捷入口。
4. 部署脚本完善：install.ps1 完成服务创建、appsettings 生成、SQLite 初始化、防火墙规则、日志目录权限、WPF 启动计划任务注册；提供 uninstall.ps1。
5. NTP 配置脚本：配置中心 NTP 服务器地址，开启 Windows 时间服务，输出同步状态。
6. 备份脚本初版：EdgeHost 启动前和每小时备份 SQLite 近 24 小时；日志按天轮转，超过保留期自动压缩。
7. 配置文件模板：提供 ppsettings.Edge.template.json 和 ppsettings.Center.template.json，标注必须修改项（MES 地址、数据库连接、设备编码、海康账号等）。
8. 全链路联调：海康采集→本地入库→TDengine 持久化→MES Mock 上传→大屏刷新→阈值报警→AI 诊断建议→审计日志可查，逐环节打点确认。
9. 压测与稳定性：连续运行 2 小时，记录采集条数、上传成功率、CPU、内存、磁盘占用、队列深度、AI 平均响应时间。
10. 文档输出：最简 README、部署说明、现场常见问题 FAQ（服务起不来、PLC 连不上、MES 不通、时间漂移等）。
11. 打包：输出 publish/edge 边缘安装包、publish/center 中心部署包、publish/ai AI 服务包、publish/web 前端静态文件。

**产出**：WPF 桌面壳；边缘安装/卸载脚本；NTP/备份/日志脚本；首个 MVP 安装包；部署说明和 FAQ。

**验收标准**：
1. 工控机重启后，EdgeHost 服务自动启动，WPF 壳自动进入大屏全屏，不需要人工双击程序。
2. 海康采集→存储→上传→大屏→报警→AI→审计主链路连续运行 2 小时无致命错误。
3. 断网 10 分钟再恢复，上传队列自动补传成功，不重复、不丢数。
4. 本地诊断入口能快速打开日志和配置目录，导出的诊断包包含配置摘要、最近日志、队列状态。
5. 在一台全新 Windows 工控机上，按部署说明能在 2 小时内完成安装并看到实时数据（不含现场网络协调和设备侧准备时间）。
6. 所有关键操作（写配置、导出、手工补传、服务重启）在审计日志中有记录。
7. MVP 安装包目录清晰，部署人员无需打开 Visual Studio 即可完成部署。

## 16. 后续迭代路线图（补充）

> 本节在不改动前文阶段划分的前提下，对二期、三期、四期给出更可执行的落地顺序、交付物和验收口径，用于 MVP 跑通之后的滚动规划。

### 16.1 二期：分析能力完善 + PLC 数据上传 MES（约 2 个月）

**目标**：从“能采能传”升级到“能看、能算、能管”，让车间主任和工艺工程师真正用起来，同时完成 PLC 数据到 MES 的稳定上传。

**重点范围**：
1. PLC 数据上传 MES：封装产量/计数、工艺参数、设备状态、故障报警、工单/配方执行结果五类消息，支持批量+事件触发双模式。
2. 点位配置界面完整化：设备管理、点位管理、设备型号模板、点位批量导入导出、点位生效状态、配置版本对比与回滚。
3. 报警规则增强：组合条件（多点位 AND/OR）、持续时长、防抖、班次生效范围、报警等级、报警抑制（连锁故障时只报根因）。
4. OEE 模块：按可用率、性能率、合格率计算，支持计划停机扣除、班次/班组/设备/车间多维对比，输出三率分解和损失帕累托。
5. SPC 控制图：先实现 X-MR、Xbar-R 等基础控制图，控制限支持手工配置和历史数据自动计算，异常点按 Western Electric 规则扩展。
6. 趋势与回放：多参数叠加、缩放、配方切换标记、报警前后一键回看、自定义指标组。
7. 故障分析：MTTR/MTBF、TOP N 故障帕累托、故障明细钻取、维修记录关联。
8. 报表与导出：日/周/月产量良率报表、班次汇总、故障报表、缺陷报表，大数据量异步导出 Excel。
9. 数据运维：近 3 个月自动备份、备份结果通知、恢复脚本、历史数据归档任务。

**关键交付物**：完整配置界面、PLC 上传器、OEE/SPC/故障分析/报表模块、自动备份脚本、二期部署包。

**验收标准**：
1. PLC 工艺参数、状态、产量、故障可以稳定上传 MES Mock（如有真实环境则对接真实 MES），断网补传成功率 ≥99.5%。
2. 工艺工程师可在界面上新增/修改点位，不需要改代码或改配置文件即可生效。
3. 组合规则报警可正常触发，误报率较一期纯阈值规则明显下降。
4. 单车间日/周/月报表可在 10 秒内打开并导出，导出结果与明细数据可对账。
5. OEE 指标与人工统计偏差在可解释范围内，关键口径可配置。
6. 自动备份任务连续运行 2 周无失败，恢复脚本在测试环境验证可用。

### 16.2 三期：AI 进阶 + 多车间复制工具包（约 2 个月）

**目标**：把 AI 从“报警时给建议”扩展为更完整的智能分析能力，同时形成可复制到其他车间/基地的工具包，把单车间部署时间压到 1 人日以内。

**重点范围**：
1. 预测性维护：基于关键部件的历史状态、故障、维修记录和趋势斜率，输出风险等级、预计窗口期和建议动作；先聚焦高价值部件（加热、伺服、送料、计数传感器）。
2. 工艺优化建议：分析温度、压力、速度等参数区间与良率/缺陷率/节拍的关系，按设备型号和配方给出推荐参数区间，明确风险提示和验证建议。
3. 缺陷根因分析：海康缺陷类型与同时窗工艺参数、状态、配方、工单、原料批次关联，按规则+模型给出根因排序和证据链。
4. 智能问答：限定在本平台数据范围内的自然语言查数，如“今天 3 号机产量是多少”“本周夜班良率最低的设备”；问答不支持开放式闲聊。
5. RAG 升级：从关键词/BM25 升级到中文向量检索（bge-m3/qwen-embedding），案例库支持人工编辑、标签、质量评分和淘汰机制。
6. 复制工具包：设备型号模板、点位模板、报警规则模板、班次字典模板、MES 配置模板、新车间初始化向导、复制前校验脚本。
7. 跨车间对比：青州内部多车间对比看板，产能、良率、OEE、故障 TOP、缺陷类型可横向对比。
8. 性能优化：TDengine 分区/预聚合、查询缓存、异步导出、历史冷热分层，验证青州 7 车间规模下的性能。
9. 运维增强：版本号管理、配置差异对比、边缘心跳、远程日志抓取接口预留。

**关键交付物**：预测维护/工艺优化/根因分析/智能问答模块；向量检索版 RAG；复制模板库；多车间对比看板；性能压测报告。

**验收标准**：
1. 对典型故障（如模温漂移、气压波动、计数偏差）可提前输出预警，验证样本不少于 20 个历史案例。
2. 工艺优化建议在样本设备上能给出合理参数区间，且明确标注“需小批量验证、不自动下发”。
3. 缺陷根因分析能把海康缺陷和对应时间窗的工艺异常关联展示，输出可解释的证据链。
4. 限定范围内自然语言查数准确率 ≥85%，超出范围时明确回答“暂不支持”而非编造。
5. 使用复制工具包在新车间完成基础配置（不含现场网络协调）的时间 ≤1 人日。
6. 青州 7 车间模拟/真实数据规模下，大屏与常规查询性能满足既定指标（大屏 ≤30 秒刷新，常规查询 ≤5 秒）。

### 16.3 四期：多端协同 + 自动报告 + 全基地推广（平台化阶段）

**目标**：平台从青州试点走向集团化推广，接入多端使用场景，形成跨基地的运维、BI 和智能分析能力。

**重点范围**：
1. 多端扩展：飞书/企业微信机器人与小程序、管理层移动端轻量看板、Web 端访问能力增强，支持在办公室和会议场景查看。
2. 自动报告：按日/周/月自动生成车间主任版和厂长版报告草稿，包含产能、良率、OEE、故障 TOP、AI 观察项和待跟进事项，可编辑后导出或推送。
3. 跨基地 BI：建立集团/基地/车间多级数据模型，支持跨基地产能、良率、故障、复制成熟度对比，形成管理层驾驶舱。
4. 远程运维：远程查看边缘节点状态、日志、配置版本、队列水位、心跳；支持配置下发、版本更新包分发、灰度升级。
5. 权限体系：建立操作工、班组长、工艺工程师、设备工程师、IT 运维、厂长、集团管理员等角色，菜单、数据、操作权限按车间隔离。
6. 告警通道正式接入：飞书/企业微信/短信/声光报警按等级分发，支持告警升级、值班排班、闭环确认。
7. 多模型管理：中心大模型/边缘小模型分层部署，模型版本、提示词版本、案例库版本统一管理，支持 A/B 测试和效果对比。
8. MQ/流处理可选引入：当 23 车间全面接入时，评估在中心侧引入 Kafka/RabbitMQ 做异步解耦与削峰，边缘端仍保持轻量。
9. 集团推广：淮北、江西等基地按复制手册上线，形成上线验收模板、培训资料、运维手册和问题库。

**关键交付物**：移动端/飞书入口；自动报告服务；跨基地 BI；远程运维中心；权限体系；外部告警通道；集团推广上线包。

**验收标准**：
1. 车间主任每天可收到自动生成的日报草稿，厂长可查看跨车间/跨基地汇总。
2. 飞书/移动端可查看关键指标和报警推送，并能做确认操作。
3. IT 运维可在中心侧查看所有边缘节点在线状态、版本、队列深度、最近错误，支持远程拉取诊断包。
4. 新基地按复制手册完成首个车间上线的时间控制在 3~5 个工作日内（含现场网络协调）。
5. 权限上线后，不同角色只能看到本车间/本职级数据，关键操作（写 PLC、导出、改配置）受权限和审计双重约束。
6. 23 车间接入规模下，中心侧写入、查询、报表和 AI 服务满足性能指标，核心链路无单点瓶颈。

## 17. 关键风险与技术应对（补充）

本节从技术实现和落地执行角度列出一期及复制阶段最需要关注的风险，给出工程层面的应对策略，作为开发和联调过程中的检查基线。

| 序号 | 风险 | 可能表现 | 技术/管理应对 |
|---|---|---|---|
| 1 | MES 接口长期不确定 | 上传只能打 Mock，真实联调拖到最后 | 上传层做接口抽象；先冻结本地报文模型；推动 MES 输出最小字段清单；保留 REST/统一网关双适配器 |
| 2 | PLC 点位地址错误或变更频繁 | 数据读错、写 PLC 写错、趋势异常 | 点位配置化+版本审计；样本机先验证；写地址白名单；点位变更必须测试后再生效 |
| 3 | OPC-UA/EIP 协议兼容性问题 | 欧姆龙/汇川或第三方库行为不一致，订阅不稳定 | 驱动层做设备型号适配；保留轮询作为兜底；连接状态和质量标记可观测；先跑稳轮询再优化订阅 |
| 4 | 工控机性能不足 | CPU 高、内存涨、SQLite 锁、WebView 卡顿 | 边缘端不跑大模型；批量写库；限制本地历史范围；提供诊断页；发布前做最低配置校验 |
| 5 | 网络频繁中断 | 上传失败、大屏空白、队列堆积 | 边缘自治；SQLite 队列；指数退避补传；队列水位告警；人工导出兜底；网络开通清单提前确认 |
| 6 | 时钟不同步 | 数据乱序、补传重复、班次统计错 | NTP 三层同步；双时间戳；漂移告警；上线检查必验；排序以边缘 UTC 时间为主 |
| 7 | TDengine/MySQL 建模不合理 | 写入慢、查询慢、磁盘爆涨 | 原始测点用超级表+标签；预聚合；冷热分层；大查询异步；试点阶段持续压测并调优保留策略 |
| 8 | AI 误判/胡编 | 维修建议误导现场 | Prompt 强约束 JSON；安全规则；置信度展示；规则兜底；人工反馈闭环；上线初期工程师审核 |
| 9 | 5 天 MVP 范围蔓延 | 想一次做完所有页面和分析，链路反而不稳 | 每日验收卡关口；非主链路功能一律后置；只有海康→存储→MES→看板→AI 闭环跑通才进入优化 |
| 10 | 现场协同阻塞 | 等 IP、等账号、等设备商配合 | 提前输出依赖清单；指定车间接口人；联调窗口提前预约；Mock 保证开发不阻塞现场 |
| 11 | 备份和恢复未验证 | 数据库损坏后无法恢复 | 一期就做脚本化备份；上线前做恢复演练；SQLite 滚动备份；配置文件进版本管理 |
| 12 | 多车间命名混乱 | 模板无法复用、报表不可比 | 一期强制编码规范；模板化配置；上线前做配置评审；沉淀命名字典和复制检查单 |

**风险处理规则**：每天站会/个人收工前对照本节检查一次，凡出现红项（阻塞主链路）的风险，必须当天给出绕过方案或明确负责人，不能带着阻塞过夜。

## 18. 附：一期上线前检查清单

本清单用于青州试点首个车间上线前自查，每项必须明确“通过/不通过/待确认”。未通过项原则上不允许进入正式上线，确需带风险上线的必须由业务负责人、IT 负责人共同签字确认。

### 18.1 环境与基础设施
1. 工控机操作系统版本、CPU、内存、磁盘剩余空间满足最低配置要求。
2. 工控机到 PLC、海康视觉、中心服务器、MES 的网络链路已打通，端口经测试可访问。
3. 防火墙规则已放通 EdgeHost、CenterApi、MySQL/TDengine/Redis、AI 服务所需端口。
4. 中心服务器 MySQL、TDengine、Redis、AI 服务可正常启动并通过健康检查。
5. NTP 时间同步已配置，工控机与中心服务器时间偏差在允许范围内，漂移告警已验证。
6. 磁盘目录已规划：程序目录、数据目录、日志目录、备份目录、导出目录权限正确。

### 18.2 采集配置
7. 海康设备 IP、账号、轮询周期、报警事件订阅已配置并验证可拉取数据。
8. 包装机 PLC（OPC-UA）连接参数、安全策略、点位表已导入，样本点位读取正确。
9. 点数机 PLC（EIP）连接参数、点位表已导入，状态/节拍/故障码读取正确。
10. 点位单位、缩放系数、数据类型、报警阈值已与工艺/设备人员确认。
11. 断线重连、通讯异常标记、超时设置已通过人为断网测试。
12. 点位模板和设备命名符合编码规范，车间/产线/设备编码无重复无歧义。

### 18.3 存储与上传
13. TDengine 超级表和子表已初始化，原始测点和报警写入正常。
14. MySQL 业务表、审计表、案例库、配置表迁移已执行到最新版本。
15. SQLite 边缘库已初始化，队列表、本地缓存表写入正常。
16. MES 地址、认证方式、接口路径、批量参数配置完成，如使用 Mock 需明确切换计划。
17. 上传队列调度正常，成功、重试、死信、人工补传、导出功能已验证。
18. 断网 30 分钟恢复后能自动补传，不重复、不丢数；幂等键保持一致。
19. 上传成功率、队列深度、死信数监控可在界面或日志中查看。

### 18.4 可视化与报警
20. 工厂总览/车间看板/设备详情/个人工位端四类视图均可打开，数据来源已联通。
21. 大屏刷新延迟 ≤30 秒，设备状态颜色、产量、良率、报警滚动显示正确。
22. 趋势图能查询最近 1 天、7 天、30 天数据，时间范围切换无报错。
23. 阈值/组合报警规则已导入典型规则，造数测试可正常触发、恢复、确认。
24. 报警详情页能查看规则命中、参数快照、AI 建议和相似案例。
25. 历史查询和 Excel 导出可正常使用，大数据量导出走异步任务，导出操作记审计。

### 18.5 AI 与规则
26. AI 服务健康检查通过，模型已下载并能正常返回结构化 JSON。
27. 典型故障至少准备 10~20 条案例，关键词/BM25/向量检索可召回。
28. Prompt 中安全约束已生效，涉及急停、安全门等报警优先提示停机和人工确认。
29. AI 不可用时能返回 fallback 结果（规则命中+历史案例），不影响报警处理。
30. 用户反馈按钮（有用/无用/误判）可用，反馈结果能入库供后续优化。

### 18.6 写 PLC 与审计
31. 写 PLC 白名单地址已确认，未配置地址禁止下发。
32. MES 下发和本地人工录入两个入口均走审计和状态记录。
33. PLC/HMI 二次确认流程已在设备侧联调通过，写失败只报警不自动重试。
34. 写 PLC、改配置、导出数据、手工补传、启停采集、清空队列等操作均有审计日志。
35. 审计日志支持按操作人、时间、类型、对象筛选，且不可物理删除。

### 18.7 部署、自启动与备份
36. EdgeHost 已注册为 Windows Service，重启工控机后自动启动，故障恢复策略配置完成。
37. WPF 桌面壳已加入开机计划任务，默认全屏进入大屏，可查看服务状态和日志入口。
38. 部署脚本 install.ps1/uninstall.ps1 在全新工控机上演练通过。
39. NTP 配置脚本、日志轮转、SQLite 自动备份任务已启用。
40. 配置文件模板、设备点位导出文件、部署说明文档已归档，便于复制到新车间。

### 18.8 稳定性与验收演练
41. 全链路连续运行 24 小时无服务崩溃，数据无连续 5 分钟以上缺口。
42. 人为模拟 PLC 断线、海康不可达、MES 不可达、AI 不可达，系统均有告警并可自恢复。
43. CPU、内存、磁盘占用在可接受范围内，队列无持续积压。
44. 现场操作人员已接受基础培训：大屏查看、报警确认、工位端使用、故障反馈。
45. 运维人员已掌握服务启停、日志查看、配置修改、手工补传、备份恢复流程。
46. 已形成上线问题清单和责任人，所有阻塞项关闭或有明确带风险上线审批。

---

**补充说明**：若 MVP 阶段仅交付海康链路，则以上清单中 PLC、AI、写 PLC、完整报表项可按阶段裁剪为“待二期验收”，但环境、自启动、缓存补传、审计、部署脚本这些底座项必须在 MVP 阶段就通过。
---

## 18. v0.2现场适配修订（青州首站现场适配版）

> 版本：v0.2 现场适配补充章节
> 日期：2026-07-06
> 适用范围：青州试点首台边缘工控机现场部署与 5 天 MVP 落地
> 修订原则：本节为 Append 增补，**不修改、不删除、不替换** v0.1 前文任何既有内容；凡 v0.1 前文与现场实况冲突之处，统一标注“v0.1 假设已过时，以本节为准”，旧原文继续保留以便追溯。
> 现场依据：20-工控机探查清单.md、PROJECT-MEMO.md、海康视觉接口整理稿 `..\海康视觉接口\01-接口需求整理.md`，以及 2026-07-06 青州现场人工/脚本探查结果。

### 18.1 现场环境实况

本节记录青州首站目标工控机的真实硬件、系统、网络、时钟与远程能力，作为部署、容量规划与排障的硬约束。

| 项 | 现场实况 | 对我方设计的硬约束 |
|---|---|---|
| CPU | Intel Core i7-9700K，8 核 8 线程，非 12 代 | 不可假设新机 AVX2/大核小核调度；边缘端长期 CPU 占用必须压低 |
| 内存 | 32GB，空闲约 23.6GB | 内存总量充足，但海康自身已占大量内存，我方进程必须自我克制 |
| 系统盘 C 盘 | 仅剩 14.5GB，极紧张 | 严禁向 C 盘写入程序、日志、数据库、临时包、诊断包；安装/数据/日志全部强制到 D 盘 |
| 数据盘 D 盘 | 剩余约 91GB | 作为我方主安装与运行盘，规划程序、SQLite、TDengine、日志、诊断导出目录 |
| 存储盘 F 盘 | 剩余约 1.4TB | 预留为长期备份、历史归档、大诊断包与离线导出盘，MVP 阶段不强依赖 |
| 网卡 | 双网卡：有线 `192.168.135.150`（车间） + WiFi `192.168.145.165` | 服务监听、出站连接、网卡健康检查必须显式绑定有线车间网卡，禁止走 WiFi |
| .NET 运行时 | 仅安装 .NET Framework 4.8，**未安装 .NET 8 Runtime** | 边缘端必须采用 `win-x64` self-contained 单文件发布，不能依赖现场预装 Runtime |
| 时间同步 | NTP 未配置，系统时间存在漂移风险 | 首次启动必须执行 NTP 对时；启动日志必须记录对时结果与漂移 |
| 远程通道 | RDP、ToDesk、向日葵均可用 | 远程运维通道充足，优先使用 RDP；诊断包/日志路径要便于远程拉取 |
| 防火墙 | Public profile 启用，Domain/Private 关闭 | 我方新增监听端口必须在 Public 入站规则中精确放通，不能简单关闭防火墙 |
| 运行形态 | 我方软件与海康 PSM 共机部署 | 零侵入、低资源、不与海康抢端口/CPU/磁盘 IO 是第一优先级 |

**过时假设标注（v0.1）**：

1. v0.1 §1/§2/§4/§7/§15 中“工控机为 i7-12 代、可承担更多本地 AI/WebView 负载”的假设已过时，以本节 i7-9700K、资源克制原则为准。
2. v0.1 §4 中“边缘端依赖现场预装 .NET 8 Runtime / Windows Service 直接依赖框架依赖发布”的假设已过时，MVP 部署统一改为 self-contained 单文件。
3. v0.1 §8 与 MEMO 第六轮中“海康数据源优先旁路抓上传流量 / 代理拦截”的优先级假设，在现场确认 PG 可直连后降级为备选，主方案以 §18.3 为准。
4. v0.1 §15 Day1-Day5 中“Day1 直接打通海康抓取链路”的顺序已过时，按用户最新要求改为“MES 上传模块 Mock 先行，海康 PG 采集后置接入”，见 §18.6。

### 18.2 海康软件实测情报

本节记录海康视觉 PSM 在现场工控机上的实际安装、进程、端口、数据库、看门狗与异常状态，任何部署动作都必须先核对本节，不得触碰海康既有运行链路。

#### 18.2.1 安装与运行根目录

- 海康安装根目录：`D:\hikrobotics\PSM\`
- 海康自启服务：`HikPostgreSQL_96213`（服务名看似 PG9.6，实际为 PostgreSQL 14，需以实际二进制和版本查询为准，不可凭服务名误判）
- PostgreSQL 监听：`0.0.0.0:5432`
- 海康业务库：`intco`，现场已验证可连接、库内有业务数据
- 对我方结论：A2 直读 PG 具备客观基础，但必须使用只读账号/只读角色，严禁回写海康库。

#### 18.2.2 核心进程现场快照

| 进程/组件 | 现场表现 | 影响判断 |
|---|---|---|
| `SourceManager.exe` | CPU 异常高，现场观测值约 27616%（多核算子累计或采样异常，需持续确认，但可确定是 CPU 头号大户） | 该进程疑似海康主控/看门狗核心，我方不得注入、不得挂起、不得抢占 CPU 时间片 |
| `nginx.exe` | 监听 80，提供海康 Web 入口 | 80 端口已被占用，我方 Web 看板必须改高端口 |
| `hik-java.exe` 多实例 | 合计内存约 1.2GB，承担业务服务 | Java 进程多、堆内存大，说明本机内存虽多但并非空闲可用 |
| `redis-server.exe` | 监听 6379 | 我方不得复用或覆盖海康 Redis 实例 |
| `MySQL` | 监听 3306 | 我方中心/边缘 MySQL 若同机部署必须换端口，MVP 边缘不装 MySQL |
| `postgres.exe` 大量子进程 | 监听 5432，业务运行中 | 可只读连接 `intco` 库，不可重启、不可改 pg_hba、不可改参数 |

#### 18.2.3 海康已占用端口（严禁冲突）

现场确认海康已占端口如下，我方任何监听端口、出站临时端口策略、内部服务端口都必须避开：

- 常见基础端口：`80, 443, 3306, 5432, 6379`
- 海康业务端口：`7000, 7001, 7005, 7008, 8002, 9001, 9010, 9011, 9020`
- 海康扩展/服务端口：`10000, 10001, 10002, 18001, 19080, 29017, 29027, 30000`
- 海康端口段：`40000-40020, 41000-41010, 42000-42020, 45100, 50000-50020, 61000, 62000`

对我方要求：

1. 不允许在上述端口上启动任何监听服务。
2. 不允许通过 `netsh interface portproxy`、反向代理、容器映射等方式隐式占用上述端口。
3. 安装脚本启动前必须做端口冲突探测，若目标端口被占则直接 fail-fast，不自动改端口到未知值。

#### 18.2.4 看门狗与加密狗

- 看门狗逻辑内嵌于 `SourceManager.exe`，现场表现为约 5 秒巡检一次进程、PostgreSQL、加密狗状态。
- 硬件加密狗已接入，现场存在“加密狗间歇掉线”相关报错/现象。
- 红线：
  - 严禁停掉、调试、注入、降权、限速 `SourceManager.exe`；
  - 严禁拔插、模拟、绕过、重刷加密狗；
  - 严禁修改海康任何 bin、config、service、计划任务文件；
  - 若看门狗因我方 CPU/内存/IO 挤占而误判异常，视为我方严重故障。

#### 18.2.5 MES 上传地址现状

- 在海康安装目录配置文件中扫描 `mes/upload/gateway/dataportal/invoke/url/jdbc` 等关键字后，**未直接找到 MES 上传目标地址**。
- 可能位置：数据库配置表、加密配置文件、Java 运行期参数、外部配置中心。
- 结论：
  - MVP 阶段不等待 MES 真实地址落地，先在我方模块内定义标准上传接口与 Mock 服务；
  - A1 旁路监听先不做实装，仅预留抓包/镜像接口，避免误伤海康链路；
  - 待后续定位 MES 真实地址与认证方式后，再接入真实端点。

#### 18.2.6 海康自身异常现状

现场观测到海康自身存在以下异常，我方采集与诊断模块要区分“我方问题”与“海康原生问题”：

1. 日志/界面出现 `alarm deal failed code=20102`；
2. 加密狗间歇掉线告警；
3. `SourceManager.exe` CPU 占用异常高；
4. 多个 Java 常驻进程常驻内存较高。

这意味着：

- 我方健康检查不能把“海康系统有错误日志”直接等同于我方故障；
- 诊断包中必须附带采集时的海康进程/端口/错误快照，方便区分责任边界；
- 在海康自身异常期间，A2 只读 PG 要做失败重试与退避，避免疯狂重连放大现场压力。

**过时假设标注（v0.1）**：

1. v0.1 §2/§7/§15 中“海康数据源以 HTTP API 拉取为主”的假设在青州首站不成立，当前真实可证数据源是 PG `intco` 库。
2. v0.1 §10/§11 中“边缘端默认安装 TDengine/MySQL/Redis 全套”的共机部署假设在首台机上不成立，海康已占用 3306/6379/5432，MVP 边缘端默认只跑我方 EdgeHost + SQLite + 可选本地 TDengine。
3. v0.1 §15.5 中“安装脚本可在本机创建 80/443/3306/6379 等常用端口服务”的假设已过时，所有端口必须按 §18.4/§18.7 规划。
### 18.3 采集方案最终决策（A2 主方案）

结合现场“PG `intco` 库可直连、MES 上传地址未明、SourceManager 看门狗敏感、加密狗不可碰、海康端口占用密集”的实际情况，青州首站采集路线正式定案如下。

#### 18.3.1 方案分级

| 方案 | 定义 | 青州 MVP 决策 | 目的 |
|---|---|---|---|
| A2 主方案 | 零侵入直读海康 PG `intco` 库 | **MVP 首选并唯一实装主链路** | 获取结构化缺陷/报警/结果数据，对海康进程零打扰 |
| A1 备选 | 旁路监听海康到 MES 的上传流量 | **仅预留接口，不实装转发代理** | 后续审计、对账、补数校验 |
| A3 兜底 | 网卡抓包（WinPcap/NpcAP 镜像解析） | **仅应急兜底，不默认启用** | 当 PG 读权限失效且 A1 无法落地时临时取证 |
| DUAL | A1+A2 双轨 | **代码预留抽象，不默认开启** | 后续高可靠工位审计对账 |

#### 18.3.2 A2 主方案技术路线

A2 采用“只读账号 + 增量读取 + 可降级读取模式”的设计，不安装任何代理、不修改海康配置、不重定向海康流量。

1. 连接方式：
   - 连接目标：`127.0.0.1:5432` 或绑定有线网卡地址访问本机 PG；
   - 账号权限：使用专用只读账号，仅授予 `intco` 库业务表 `SELECT` 权限，不给 `INSERT/UPDATE/DELETE/TRUNCATE`，不给系统表修改权限；
   - 连接参数：设置合理 `CommandTimeout`、`Pooling=false` 或小连接池，避免长连接占满海康现有连接；
   - 应用名称：设置 `ApplicationName=intco_edge_collector_readonly`，便于 DBA/排障识别。

2. 增量读取策略（按优先级自动降级）：
   - 优先：PostgreSQL `Logical Replication` 或 WAL 逻辑订阅（只读消费变更流）；若现场 PG 配置不允许开 publication/slot，则降级；
   - 次优先：基于业务时间戳字段（如 `create_time/update_time/alarm_time/result_time`）轮询增量；
   - 兜底：基于自增 ID/主键 + 时间戳双游标轮询，必要时增加轻量 `LISTEN/NOTIFY` 触发提示，但不依赖触发器回写海康库。

3. 严禁行为：
   - 严禁在海康库中建表、建触发器、建函数、改 schema；
   - 严禁使用高权限账号（postgres / sa 类）直连；
   - 严禁执行 `VACUUM/ANALYZE/REINDEX/ALTER` 等维护语句；
   - 严禁因为读不到数据就重启 PG 或修改 `postgresql.conf/pg_hba.conf`。

4. 数据读取原则：
   - 只读取业务需要的结果、缺陷、报警、工单/过账相关表；
   - 默认不读大字段图片、BLOB、日志长文本，必要时按需开启并落 F 盘；
   - 每批拉取条数受控，查询走增量索引字段，避免全表扫描打挂 PG。

#### 18.3.3 A1/DUAL/A3 的定位

- A1 旁路监听：当前不做中间人代理，不做透明转发，不修改海康出口配置；后续如需审计对账，优先采用端口镜像/旁路抓流/本机出向流量镜像等不影响生产的方式。
- DUAL 双轨：仅在关键工位需要“零漏数”证明时再开，A2 负责主数据，A1 负责上传事件审计，本地通过幂等键合并。
- A3 网卡抓包：只在故障排查时由开发/运维手工启动，不作为常驻服务；抓包文件落 D/F 盘并定期清理，避免撑爆 C 盘。

**过时假设标注（v0.1）**：

1. v0.1 §2、§7、§15 中“海康以 HTTP API 直连为主采集方式”与现场不符，以本节 A2 主方案为准。
2. PROJECT-MEMO 第六轮中“默认优先旁路拦截 MES 上传流量作为主链路”的临时判断已过时，青州首站以 A2 PG 直读为主链路。
3. v0.1 §2.2 中“一期不上 MQ 就直接抓海康 HTTP 实时事件”的设计，在青州现场改为基于 PG 增量轮询/逻辑复制获取业务事件。

### 18.4 部署约束与端口规划

本节明确青州首台工控机的安装路径、发布形态、启动顺序、网卡绑定与端口规划，所有安装脚本、配置模板、WPF 诊断页必须遵守。

#### 18.4.1 安装路径与目录约束

- 我方软件强制安装根目录：`D:\IntcoEdge\`
- 目录规划：
  - `D:\IntcoEdge\app\`：程序发布目录（self-contained 单文件、配置模板、脚本）
  - `D:\IntcoEdge\config\`：运行配置、appsettings 实例、证书（如有）
  - `D:\IntcoEdge\data\sqlite\`：SQLite 本地缓存、队列表、诊断状态
  - `D:\IntcoEdge\data\tdengine\`：如需本地 TDengine 数据目录，MVP 可暂不启用
  - `D:\IntcoEdge\logs\`：Serilog 结构化日志目录
  - `D:\IntcoEdge\diag\`：一键诊断包导出目录
  - `D:\IntcoEdge\backup\`：SQLite/配置本地短期备份
  - `F:\IntcoEdgeArchive\`：长期归档、大日志、历史诊断包（后续启用）
- 强制红线：
  - 不向 `C:\Program Files\`、`C:\Users\...\AppData\Local\Temp\` 长期写入业务数据；
  - MSIX/单文件解压临时目录若由 .NET 自动落到 C 盘，需在脚本中监控并限制大小；
  - 日志、诊断包、临时解压、崩溃 dump 默认全部重定向到 D 盘。

#### 18.4.2 发布形态

- Runtime：不依赖现场安装 .NET 8 Runtime。
- 发布参数：
  - `dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true`
  - 开启 `IncludeNativeLibrariesForSelfExtract=true`
  - 适当启用压缩以减小包体，但要评估首次启动解压开销
- 服务形态：
  - EdgeHost 优先注册为 Windows Service；
  - 若现场服务注册受限，则由 WPF 守护进程拉起，但必须支持异常退出重启；
  - WPF 桌面壳为运维辅助入口，不承载核心业务状态。

#### 18.4.3 首启顺序

首次启动必须严格按以下顺序执行，任何一步失败都在日志中明确失败原因，并在 WPF 诊断页显示：

1. NTP 对时：调用 Windows 时间服务，尝试对时并记录偏差；对时失败不阻塞全部服务，但标记高风险状态。
2. 环境快照：CPU、内存、磁盘、网卡、IP、.NET 版本、防火墙状态、海康进程/端口快照写入启动日志。
3. 目录自检：确认 D 盘目录存在且可写，C 盘剩余空间低于阈值只告警不写 C 盘。
4. 连通性自检：按配置探测 MES Mock/真实端点、PG `intco`、TDengine（如启用）、MySQL（如启用）。
5. 配置校验：校验必填配置项、端口冲突、网卡绑定地址、输出目录。
6. 启动服务：日志、配置、采集/上传调度器、健康检查端点、WPF 本地通信接口。
7. 自报健康：服务启动后在 `/health` 返回 `starting/ready/degraded` 状态，WPF 壳读取并显示。

#### 18.4.4 端口规划

我方端口全部选用高端口，避开 §18.2.3 列出的海康端口段。

| 服务 | 规划端口 | 协议 | 绑定网卡 | 是否对外 | 说明 |
|---|---|---|---|---|---|
| Web 看板 | 5080 | HTTP | 有线网卡 192.168.135.x | 车间内网可访问 | 本地/车间查看实时状态，不占用 80/8080/8443 |
| API 服务 | 5188 | HTTP | 有线网卡 | 本地+WPF+内部调用 | 含上传/查询/运维接口 |
| 健康检查 | 5188（复用 API） | HTTP | 有线网卡/本地回环 | 本地优先 | `/health`、`/ready`、`/diag` |
| TDengine | 6030 | TCP | 有线网卡/本地 | 本地为主 | 若本地部署 TDengine，必须避开 6030/6041 默认段冲突并确认未占用 |
| AI 诊断 | 8100 | HTTP | 本地回环优先 | 仅本机 | Python AI 服务若边缘启用，仅监听 127.0.0.1 |

补充约束：

- WPF 桌面壳与 EdgeHost 本机通信优先走 127.0.0.1 回环，减少暴露面。
- 防火墙入站规则仅放通需要给车间访问的最小端口（MVP 主要是 5080/5188）。
- 6030/8100 如仅本机使用，不创建 Public 入站规则。

#### 18.4.5 资源上限

针对海康已重负载运行的现场，我方边缘端必须设置硬约束：

- CPU：常态占用控制在整机 `<20%`（8 核算约单进程长期平均不超过 1.5~2 个核）；
- 内存：EdgeHost 主进程 `<1GB`，WPF 壳额外 `<300MB`，Python AI 若启用上限另控；
- 磁盘：日志按天滚动并保留 30 天；SQLite 与诊断包设置水位阈值，超阈值自动清理旧文件或告警；
- 网络：对 PG/MES 的轮询频率默认保守起步，避免打爆连接与上行带宽。

**过时假设标注（v0.1）**：

1. v0.1 §4 中“Windows Service + NSSM/内置 Service 默认框架依赖部署”的假设已过时，改为 self-contained。
2. v0.1 §10“部署方案”中默认目录可放 Program Files 的习惯假设已过时，强制 `D:\IntcoEdge\`。
3. v0.1 §11“可观测”、§15.5 中未明确首启 NTP 对时顺序，本节补充为强制步骤。
### 18.5 可观测能力详细设计

现场环境复杂、远程通道多、责任边界难分，一期可观测能力不是“锦上添花”，而是 MVP 必须同步落地的底座。Day1 即交付日志、健康检查、环境快照、诊断包导出，不得拖到上线前补。

#### 18.5.1 Serilog 结构化 JSON 滚动日志

1. 日志框架：统一使用 Serilog，边缘端所有模块（采集、上传、配置、WPF 守护、健康检查）写同一套结构化日志。
2. 输出格式：
   - 文件输出为 JSON Line（每行一条 JSON）；
   - 控制台输出为简化文本，便于现场直接看；
   - WPF 日志查看器解析 JSON，支持级别、模块、traceId 过滤。
3. 文件策略：
   - 按天切分：`D:\IntcoEdge\logs\edge-YYYYMMDD.json`
   - 保留 30 天，超出自动删除；
   - 单文件大小上限可配（默认 500MB），超大小自动分卷；
   - 敏感信息（密码、Ticket、Token、数据库口令）必须打码。
4. 必备字段：
   - `@timestamp`：UTC 时间
   - `level`：Verbose/Debug/Information/Warning/Error/Fatal
   - `traceId`：跨模块调用链 ID
   - `module`：collector/uploader/config/health/diag/wpf 等
   - `machineName`、`userName`、`ip`
   - `serviceName`、`serviceVersion`
   - `exception`：异常堆栈
   - `eventCode`：自定义事件码（如 `MES_UPLOAD_FAIL`、`PG_CONNECT_FAIL`、`DISK_LOW`）
5. 日志级别动态切换：
   - 通过配置文件或 API/诊断页切换默认级别，**无需重启服务**；
   - 支持按模块临时切 Debug，例如只把 `HikCollector` 调成 Debug 用于现场排障；
   - 级别变更事件必须写入审计日志。

#### 18.5.2 `/health` 健康检查端点

健康检查复用 API 端口 5188，不单独占用新端口。

| 端点 | 用途 | 返回内容 |
|---|---|---|
| `GET /health` | 综合健康状态 | `healthy/degraded/unhealthy`，版本、运行时长、CPU/内存、磁盘、关键依赖状态 |
| `GET /health/ready` | 是否可接收流量 | 依赖服务是否完成初始化、队列是否可写 |
| `GET /health/live` | 进程存活 | 进程未挂即可返回 200 |
| `GET /diag/snapshot` | 诊断快照（本机） | 环境快照、最近错误、队列深度、上传统计 |

依赖检查项：

- 磁盘（D/C/F 剩余空间）
- 网卡绑定状态（是否仍在 192.168.135.x 有线网卡）
- 海康 PG `intco` 连接（若已启用 A2 采集）
- MES Mock/真实端点连通性
- SQLite 写入可用性
- TDengine（如启用）
- 时间同步状态（最近一次对时结果、偏移）
- 海康关键进程存在性（nginx/postgres/SourceManager）

#### 18.5.3 一键诊断包导出（WPF）

WPF 桌面端必须提供“一键导出诊断包”按钮，点击后自动生成 zip，供远程人员分析。

诊断包内容：

1. 环境快照：OS、CPU、内存、磁盘、网卡/IP、防火墙、时区、开机时长、.NET 版本；
2. 海康快照：关键进程、监听端口、HikPostgreSQL 服务状态、最近海康自身错误摘要；
3. 我方快照：版本号、安装路径、配置快照（敏感字段打码）、最近启动日志；
4. 日志切片：最近 N 天/最近 N MB 日志（默认最近 3 天）；
5. 上传状态：最近上传成功/失败条数、队列深度、最近错误码；
6. 连通性测试结果：PG/MES/TDengine/MySQL/DNS/NTP 测试结果；
7. 端口冲突检测：我方拟用端口与当前监听端口冲突表；
8. 可选：Windows 事件日志中与应用相关的最近错误项。

约束：

- zip 默认输出到 `D:\IntcoEdge\diag\`；
- D 盘空间不足时可改到 F 盘，**绝不写到 C 盘用户桌面或 Temp**；
- 导出动作必须记审计日志；
- 文件名带机器名、时间戳，便于多次对比。

#### 18.5.4 启动连通性探活与环境快照

首次启动必须完成“环境快照 + 探活”并写入日志，至少包含：

1. 路由与网卡：确认默认路由优先走有线网卡 `192.168.135.x`，而不是 WiFi；
2. DNS/NTP：NTP 对时结果、DNS 解析结果；
3. TCP 探活：PG:5432（本地海康）、MES 地址、TDengine 端口、中心 API（若配置）；
4. HTTP 探活：MES 登录/心跳接口、健康检查自探活；
5. 进程探活：检测 `SourceManager.exe/nginx/postgres` 是否存活，若不存在标记为“海康未启动/状态异常”；
6. 磁盘水位：C 盘剩余、D 盘剩余、日志目录所在盘剩余；
7. 端口占用：我方目标端口 5080/5188/6030/8100 是否空闲。

#### 18.5.5 WPF 诊断页

WPF 不承担复杂业务，但必须是现场排障的第一入口：

1. 服务状态：EdgeHost 是否运行、运行时长、版本号；
2. 时间状态：系统时间、上次 NTP 对时结果、漂移；
3. 网卡状态：当前使用网卡、IP、是否绑定有线；
4. 上传状态：最近成功率、队列深度、死信数、最近错误；
5. 日志查看器：实时 tail、级别过滤、模块过滤、关键词搜索；
6. 快捷操作：打开日志目录、打开配置目录、手动触发补传、导出诊断包、动态切日志级别；
7. 海康状态摘要：关键进程是否存在、PG 是否可连、海康明显错误提示。

**过时假设标注（v0.1）**：

1. v0.1 §11“可观测性”已有基础要求，但未把“诊断包导出、日志动态级别、首启环境快照、网卡绑定检测”提升为 MVP Day1 必交付项，本节优先级更高。
2. v0.1 §15.5 将“本地诊断入口”列为 Day5 内容，现调整为 Day1 先出最小可导出能力，Day5 再完善页面。

### 18.6 Day1-Day5 MVP 任务重排（MES上传先行）

根据现场最新要求：**MVP 先从 MES 上传模块开始做 Mock 测试，不依赖海康 PG**。这意味着 5 天计划不再把“海康采集链路跑通”作为第一天入口，而是先把可观测底座、上传能力、本地缓存、Mock 服务、WPF 壳做稳，海康 PG 采集在 MVP 后段按接口接入。

> 注：原 v0.1 §15 Day1-Day5 内容保留不删，但首站执行顺序以本节为准。

#### 18.6.1 Day1：.NET 8 解决方案骨架 + 可观测底座 + MES 接口定义

目标：不碰海康，先把“能启动、能打日志、能看健康、能导出诊断、能定义上传接口”的底座搭起来。

具体任务：

1. 新建 .NET 8 解决方案与项目骨架：`Idp.EdgeHost`、`Idp.Common`、`Idp.Protocol`、`Idp.Uploaders`、`Idp.Desktop`。
2. 配置系统：`appsettings.json` + 环境变量覆盖 + 强制安装根目录 `D:\IntcoEdge\`。
3. Serilog 接入：JSON 滚动日志、按天切、30 天保留、敏感字段打码、动态级别切换接口。
4. 启动宿主：Windows Service 能力、首启 NTP 对时、环境快照写入日志、目录自检。
5. 健康检查：`/health`、`/health/ready`、`/health/live`、基础依赖探测框架。
6. MES 上传接口定义：
   - `IMesUploader`
   - `MesUploadMessage`
   - `MesUploadResult`
   - 幂等键生成规则
   - 重试策略接口
7. WPF 壳最小版：显示服务状态、最近日志、导出诊断包按钮（内容可以简陋但必须可用）。
8. 诊断包导出初版：环境快照 + 最近日志 + 配置快照（打码）。

验收标准：

- self-contained 单文件在无 .NET 8 Runtime 的 Windows 机器可启动；
- 启动日志能看到 CPU/内存/磁盘/网卡/NTP/目录/端口快照；
- `/health` 可返回状态；
- 不连海康、不连 MES 也能正常启动并导出诊断包。

#### 18.6.2 Day2：MES 上传核心实现 + SQLite 本地缓存 + 断网补传 + 幂等去重

目标：把上传主链路做成“可单测、可离线、可补传、可去重”的稳定核心，先不依赖真实 MES。

具体任务：

1. MES 上传核心：
   - JWT/Ticket 双适配认证接口；
   - `HttpClient` 工厂、超时、连接复用；
   - 指数退避重试、熔断、可配置重试次数；
   - 批量上传与单条上传策略接口。
2. SQLite 本地队列：
   - 待上传队列表；
   - 发送日志表；
   - 死信表；
   - 幂等键唯一索引；
   - 队列水位统计。
3. 断网补传：
   - 网络恢复检测；
   - 按时间顺序补传；
   - 批量补传限速，避免占满 CPU/带宽；
   - 补传过程中不阻塞新数据入队。
4. 幂等去重：
   - 本地幂等键 = `mesApiType + method + businessKeyHash + occurTime`；
   - 对同一条业务事件多次入队不重复发送；
   - MES 返回明确重复时标记为去重成功而非失败。
5. 审计与统计：
   - 成功、失败、重试、死信、补传成功统计；
   - 最近错误码与错误信息写入日志和诊断包。

验收标准：

- 人为断网时消息进入队列不丢；
- 网络恢复后按顺序补传；
- 重复推送同一业务事件不会重复入库/重复发送；
- SQLite 异常、磁盘满、HTTP 500/超时等场景有明确错误日志。

#### 18.6.3 Day3：MES Mock 服务 + 上传审计 + 配置化上传规则

目标：本地即可模拟 MES 接收，所有上传链路可在开发机/工控机本机闭环验证。

具体任务：

1. MES Mock 服务：
   - 本地可跑，支持登录获取 Token/Ticket；
   - 支持接收报警/缺陷/产量/状态等消息；
   - 可配置返回成功、超时、401、500、业务错误码；
   - 记录所有收到的报文，支持页面/API 查询。
2. 上传规则配置：
   - 上传地址、认证方式、批量大小、重试次数、超时；
   - 按消息类别开关（defect/alarm/production/status/...）；
   - 配置热更新，不重启生效。
3. 上传日志审计：
   - 每次上传请求/响应都有 traceId；
   - 记录发送时间、响应码、耗时、重试次数；
   - 支持按 traceId 查一次上传全生命周期。
4. Mock 对账页面/接口：
   - 本地查看已收到消息数、最近错误、重复投递检测；
   - 支持导出 Mock 接收结果，供人工对账。

验收标准：

- 本机启动 Mock 后，EdgeHost 可向其发送登录与业务上报；
- Mock 返回不同错误码时，上传器重试/死信逻辑符合预期；
- 可通过配置切换 Mock 与未来真实 MES 地址，不改代码。

#### 18.6.4 Day4：WPF 桌面端壳 + 诊断页面 + 实时上传状态看板 + 日志查看器

目标：把现场运维入口做出来，保证远程和现场都能快速看状态、排问题。

具体任务：

1. WPF 主壳：托盘图标、开机启动配置、管理员权限检测、全屏/非全屏模式开关。
2. 诊断页面：
   - 环境信息、服务状态、时间同步状态、网卡状态；
   - 端口冲突检测；
   - 一键导出诊断包；
   - 日志级别动态切换。
3. 实时上传状态看板：
   - 待传/已传/失败/死信/补传中数量；
   - 最近 1 小时成功率趋势；
   - 最近错误列表；
   - 手动触发补传按钮。
4. 日志查看器：
   - 实时滚动；
   - 级别筛选、模块筛选、关键字搜索；
   - 打开日志所在目录。
5. 健康展示：轮询 `/health` 与 `/diag/snapshot`，服务挂时红色提示并给出重启/导出诊断包按钮。

验收标准：

- 在工控机分辨率下页面不溢出、不错位；
- 不需要命令行即可完成“看状态、看日志、导出包、切日志级别、触发补传”；
- EdgeHost 异常退出时 WPF 能明确提示并记录。

#### 18.6.5 Day5：self-contained 打包 + 部署脚本 + 冒烟测试 + 文档

目标：形成可现场安装、可卸载、可验证的首版包。

具体任务：

1. 发布脚本：
   - self-contained win-x64 单文件发布；
   - 输出到 `publish/edge`；
   - 包含 WPF、EdgeHost、配置模板、Mock 可选包。
2. 安装脚本 `install.ps1`：
   - 强制安装到 `D:\IntcoEdge\`；
   - 创建目录、复制文件、生成实例配置；
   - 注册 Windows Service/计划任务；
   - 创建最小防火墙规则；
   - 启动前端口冲突检测、磁盘空间检测、网卡检测。
3. 卸载脚本 `uninstall.ps1`：
   - 停止服务、删除服务、保留数据/日志（可选全删）；
   - 不触碰海康任何文件/服务/端口规则。
4. 冒烟测试脚本：
   - 启动 EdgeHost；
   - 访问 `/health`；
   - 向 Mock 发送测试消息；
   - 导出诊断包；
   - 检查日志中有无 fatal/端口冲突/写入 C 盘等红线问题。
5. 文档：
   - README；
   - 现场部署步骤；
   - 常见问题（端口冲突、C 盘空间不足、NTP 失败、MES 地址待配、海康 PG 权限未开）。

验收标准：

- 在一台未装 .NET 8 Runtime 的 Windows x64 机器上可安装启动；
- 安装路径为 `D:\IntcoEdge\`；
- 冒烟测试通过并生成诊断包；
- 不与海康现有端口冲突。

**过时假设标注（v0.1）**：

1. v0.1 §15 Day1-Day5 中“Day1 直接从海康抓取启动”的顺序已过时，青州首站按本节执行。
2. v0.1 §15.3/§15.4 中海康采集、大屏、AI 的前后顺序对 MVP 首站不适用，MVP 首站只交付 MES 上传链路 + 可观测底座 + WPF 诊断壳 + Mock 服务；海康 A2 采集在随后的迭代中接入。
### 18.7 端口占用与规避清单（海康 vs 我方对照表）

本节给出海康已占端口与我方规划端口的对照，所有脚本启动前必须自动检查，冲突即停止。

#### 18.7.1 海康已占端口清单（严禁占用）

| 端口/段 | 协议 | 现场归属 | 我方策略 |
|---|---|---|---|
| 80 | TCP | nginx/海康 Web | 禁止监听，禁止反代占用 |
| 443 | TCP | 海康 HTTPS/相关组件 | 禁止监听 |
| 3306 | TCP | MySQL（海康） | 禁止复用，边缘不装 MySQL |
| 5432 | TCP | PostgreSQL（海康业务库 intco） | 仅作为只读客户端连接，不在本机再起 PG |
| 6379 | TCP | Redis（海康） | 禁止复用，不启我方 Redis |
| 7000/7001/7005/7008/8002/9001/9010/9011/9020 | TCP | 海康业务服务 | 禁止监听 |
| 10000/10001/10002/18001/19080/29017/29027/30000 | TCP | 海康扩展服务/平台通信 | 禁止监听 |
| 40000-40020 | TCP | 海康端口段 | 范围内端口全部避让 |
| 41000-41010 | TCP | 海康端口段 | 范围内端口全部避让 |
| 42000-42020 | TCP | 海康端口段 | 范围内端口全部避让 |
| 45100 | TCP | 海康端口 | 禁止监听 |
| 50000-50020 | TCP | 海康端口段 | 范围内端口全部避让 |
| 61000/62000 | TCP | 海康端口 | 禁止监听 |

#### 18.7.2 我方规划端口清单

| 端口 | 协议 | 服务 | Bind 地址 | 防火墙 | 说明 |
|---|---|---|---|---|---|
| 5080 | TCP | Web 看板/状态页 | 有线网卡 IP | 按需放通入站 | 车间可访问，替代 80 |
| 5188 | TCP | API + Health + Diag | 有线网卡 IP + loopback | 按需放通入站 | 与 WPF/内部模块共用 |
| 6030 | TCP | TDengine（可选） | 127.0.0.1 优先 | 默认不放开 | MVP 边缘可不启 |
| 8100 | TCP | AI 诊断（可选） | 127.0.0.1 | 不放开 | 本机 .NET 调用 |

#### 18.7.3 规避规则

1. 启动探测：服务启动前先检查 5080/5188/6030/8100 是否 LISTENING，若被占直接退出并打印 PID/进程名。
2. 出向连接不绑固定源端口，避免与海康监听端口偶发冲突。
3. 若后续增加端口，必须使用 5000 段/6000 段/8000 段中**未列入海康清单**的高端口，并更新本节。
4. 禁止使用 8080/8443/9000/9090/33060/54321 等常见但易冲突端口，除非脚本明确探测通过。
5. 防火墙规则只创建我方实际监听端口的精确规则，不做网段全开放。

### 18.8 风险更新（v0.2 现场新增）

在 v0.1 §17 风险表基础上，结合青州现场新增以下至少 6 条高优先级风险，首周开发与上线前必须逐项检查。

| 序号 | 风险 | 现场表现/触发条件 | 影响 | 应对措施 |
|---|---|---|---|---|
| R18-1 | C 盘空间极紧张 | C 盘仅剩 14.5GB，Windows 更新、日志、临时解压、dump 都可能占满 | 系统盘红、服务异常、海康或我方程序崩溃 | 强制安装 D:\IntcoEdge；日志/诊断/临时解压全改 D 盘；启动检测 C/D 盘剩余；禁止写 C 盘业务数据；包体压缩并控制单文件解压缓存 |
| R18-2 | .NET 8 Runtime 未安装 | 现场只有 FX4.8，无 .NET 8 | 框架依赖发布无法启动 | 全部 win-x64 self-contained 单文件发布；安装脚本不尝试在线装 Runtime；冒烟测试在纯净 Windows 验证 |
| R18-3 | NTP 未配置 | 工控机时间可能漂移，海康/MES/我方时间不一致 | 数据乱序、补传误判、MES 拒绝旧时间数据、班次统计错误 | 首启强制对时；日志记录偏移；/health 暴露时间状态；部署脚本可配置 NTP 服务器；保留设备时间/边缘时间/入库时间三时间戳 |
| R18-4 | SourceManager CPU 抢占异常 | SourceManager CPU 读数极高，疑似看门狗+主控高负载 | 我方进程 CPU 预算非常有限，轮询/补传过频会雪上加霜 | 边缘端 CPU<20%、内存<1GB；PG 轮询低频起步、指数退避；批量发送限速；不在生产高峰跑重任务；持续监控整机 CPU |
| R18-5 | 双网卡路由错走 WiFi | 同时存在有线 192.168.135.150 和 WiFi 192.168.145.165，若服务监听 0.0.0.0 或路由不当会走错网卡 | MES/PG/车间网络不通，数据从错误网卡出站，远程误判 | 显式绑定有线网卡 IP；启动时校验网卡与 IP；检测默认路由优先级；/health 报告当前监听 IP；禁止 WiFi 作为业务出口 |
| R18-6 | 海康自身报错干扰排障 | 海康已有 alarm deal failed code=20102、加密狗间歇掉线、SourceManager 高 CPU | 现场误把海康原生故障归因于我方，或我方采集被海康异常中断影响 | 诊断包采集海康进程/错误快照；区分“我方故障/海康故障/网络故障”事件码；对 PG 连接失败做退避，不反复重连刷屏 |
| R18-7 | MES 地址未在配置中找到 | 配置文件未扫到 MES URL/认证方式 | 无法在 5 天内完成真实 MES 联调 | 先做可切换 Uploader 适配器与 Mock 服务；预留 JWT/Ticket 双认证；现场继续定位 DB/加密配置中的真实地址；Mock 对账作为 MVP 验收依据 |
| R18-8 | 端口冲突导致服务不可用 | 海康占用端口多且段大，若用常见端口易冲突 | 我方服务起不来或误占海康端口影响生产 | 启动端口探测；端口表写入文档和脚本；默认固定 5080/5188/6030/8100；冲突 fail-fast，不自动随机选端口 |
| R18-9 | 共机部署下看门狗误判 | SourceManager 5 秒巡检进程/PG/加密狗，若我方引发高负载、PG 连接暴涨、加密狗驱动异常，可能触发海康自保 | 海康服务重启、停线风险 | 严禁碰加密狗和海康服务；控制连接池；限制 CPU/内存；上线窗口先做压测；任何异常优先自我限流 |

**风险控制原则（v0.2 补充）**：与海康共机时，“不影响生产”优先级高于“功能快上”。凡是可能造成 PG 连接暴涨、CPU 飙升、磁盘 IO 打满、网卡路由变化的动作，必须先在 Mock/压测环境验证，再到现场非生产窗口执行。
### 18.9 海康数据结构与上传接口参考

说明：当前青州首站主方案是 A2 直读海康 PG `intco` 库，但现场尚未完成全量表结构梳理。因此本节先沉淀已有海康视觉接口文档中可确认的字段、英科侧上传接口骨架、错误码与字段映射原则，作为 PG 表字段识别、报文建模、Mock 服务和未来真实 MES 适配的参考基线。待现场 PG 表结构梳理后，再补充“PG 表 -> 平台 DTO -> MES 报文”的精确映射附录。

#### 18.9.1 海康侧可参考查询接口字段（来自接口整理稿）

根据 `..\海康视觉接口\01-接口需求整理.md`，海康侧提供/描述过如下接口能力：

1. 配置查询接口（GET）：
   - 返回产线组 `lineGroup`
   - 返回缺陷类型组 `defectGroup`
   - 返回面别组 `faceGroup`
2. 视觉检测数据查询接口（POST，按时间/产线/缺陷/面别查询）：
   - 时间字段：`time`
   - 筛选字段：`lineGroup`、`defectGroup`、`faceGroup`
   - 明细字段：
     - `data.defects[].time`
     - `data.defects[].line`
     - `data.defects[].defect`
     - `data.defects[].face`
     - `data.defects[].detectionCount`
   - 删除/剔除统计字段：
     - `data.removeCounts[].line`
     - `data.removeCounts[].face`
     - `data.removeCounts[].removeCount`

这些字段说明海康结果模型至少围绕“时间、产线、缺陷类型、面别、检测数量、剔除数量”展开。在 A2 直读 PG 时，应优先寻找具备这些语义的结果表/统计表，避免一上来就把相机原始日志表当主数据源。

已知待对齐问题（接口文档原始问题保留，PG 建模时需核验）：

- `lindGroup/linds` 与 `lineGroup/lines` 命名不一致；
- 响应外层 `code/success/data/results/message` 封装不一致；
- `removeCounts` 语义尚需确认是“次品剔除数量”还是“数据删除次数”；
- 查询参数 `time` 传整点，但明细时间精确到秒，需要确认小时窗口语义。

#### 18.9.2 英科 MES 统一网关接口参考（报警上传）

接口整理稿中，英科接口采用统一入口模式：

- 入口：`POST /api/dataportal/invoke`
- 路由方式：通过 `ApiType + Method` 区分接口
- 认证：登录获取 `Ticket`，后续请求在 `Context.Ticket` 中携带
- 组织字段：`Context.InvOrgId = 1`

登录接口示例：

- `ApiType = AuthenticationController`
- `Method = Login`
- `Parameters[0].Value = 账号`
- `Parameters[1].Value = 密码`

登录返回关键字段：

- `Result.UserCode`
- `Result.UserName`
- `Context.Ticket`
- `Context.InvOrgId`

报警上传接口示例：

- `ApiType = VisualInspectionController`
- `Method = HandleVisualInspectionAlarm`
- `Parameters[0].Value` 为报警对象数组

报警对象字段参考：

| 字段 | 类型 | 说明 |
|---|---|---|
| WorkShop | String | 车间代码，使用固定枚举，如青州 `QZN1/QZP1/...` |
| Line | String | 产线 |
| Face | String | 面别，如 A 面/B 面 |
| AlarmTime | String | 报警时间，接口示例为 ISO 格式 |
| AlarmType | String | 报警类型/缺陷类型 |
| AlarmLevel | String | 报警等级 |
| AlarmDetails | String | 报警详情 |
| AlarmResult | String | 处理结果，如“已处理”，未处理取值待确认 |
| AlarmCount | Number | 报警次数/重复次数 |

#### 18.9.3 青州车间代码参考（报警上传用）

根据接口整理稿，青州基地代码如下，MES 报文 `WorkShop` 字段不得自定义中文车间名，必须使用枚举代码：

| 代码 | 车间 |
|---|---|
| QZM1 | 青州口罩车间 |
| QZN1 | 青州丁腈一车间 |
| QZN2 | 青州丁腈二车间 |
| QZN3 | 青州丁腈三车间 |
| QZP1 | 青州PVC一车间 |
| QZP2 | 青州PVC二车间 |
| QZP3 | 青州PVC三车间 |

注意：当前首站现场具体是哪一个车间代码，部署前必须由业务/IT 最终确认；配置文件中必须显式配置，不能写死猜测值。

#### 18.9.4 错误码与返回判定参考

接口整理稿中明确指出：

- 英科统一网关外层 `Success` 可能恒为 `true`，不能仅凭外层 `Success=true` 判断业务成功；
- 实际业务结果应看 `Result.code`：
  - `200`：成功
  - `400`：失败
- 失败原因取自 `Result.message`。

我方上传模块必须按此判定：

1. HTTP 200 仅代表 HTTP 传输成功，不代表业务受理成功；
2. 必须解析业务 `Result.code`；
3. `code != 200` 时按可重试/不可重试错误分类：
   - 认证失败/Ticket 过期：刷新 Ticket 后重试；
   - 参数错误/字段缺失：进入死信并告警，不无限重试；
   - 5xx/超时/网络错误：进入重试队列；
4. 所有原始响应体（脱敏后）必须保留在上传审计日志中。

#### 18.9.5 平台内部报文模型（MES Upload DTO）

为避免未来从 Mock 切真实 MES 时改业务逻辑，内部统一使用如下 DTO 语义（字段名可在代码中微调，但语义必须保留）：

- `MessageId`：本地唯一消息 ID（雪花/ULID）
- `IdempotencyKey`：幂等键
- `Category`：defect/alarm/production/status/parameter/workorder_result
- `WorkShop`
- `Line`
- `Face`
- `OccurTime`/`AlarmTime`
- `AlarmType`/`DefectType`
- `AlarmLevel`
- `Details`
- `Result`
- `Count`
- `SourceSystem`：hikvision_psm
- `SourceRecordId`：PG 主键/事件 ID
- `CollectedAtUtc`
- `EdgeNode`
- `RawPayloadHash`

MES 适配器负责将该内部 DTO 转成：

- RESTful + JWT 模式；
- 或英科统一网关 `ApiType/Method/Parameters/Context` 模式。

这样无论现场最终是 JWT 还是 Ticket，采集、缓存、补传、审计逻辑都不需要重写。

#### 18.9.6 PG 直读时的字段识别原则

待现场开放只读账号后，A2 采集器按以下原则识别主表，不盲猜、不写 SQL 改库：

1. 先看表名：优先识别含 `alarm/defect/result/inspection/record/product/remove/reject` 等业务语义的表；
2. 再看时间字段：优先含 `create_time/update_time/alarm_time/inspect_time/result_time` 字段的表；
3. 再看字典关联：若存在产线、缺陷类型、面别字典表，优先建立外键/编码映射视图；
4. 先做只读 `SELECT` 样本拉取，不做 `SELECT *` 大查询；
5. 对疑似大字段（图片、blob、clob、长 json）默认不拉，确认需要后再单独开关；
6. 每张识别到的业务表，补充文档《PG表映射附录》（后续追加，不在本节展开）。

**过时假设标注（v0.1）**：

1. v0.1 §5/§12 中默认假设“海康缺陷/报警字段会通过 HTTP API 直接给出标准结果”，青州现场短期内不以此为前提，而是以接口整理稿为参考、以 PG 实际结构为准。
2. v0.1 §8/§12 中对 MES 返回成功的判定仅提 HTTP/超时，未明确“外层 Success 可能恒 true、业务码看 Result.code”的特殊规则，本节补充为强约束。

### 18.10 v0.2 章节总结与执行口径

1. v0.1 原有章节继续保留、继续作为长期架构设计参考；
2. 青州首站 MVP 执行以本章 v0.2 现场适配结论为准；
3. 首站成功后，若后续车间硬件、网络、海康版本不同，必须在复制前重新跑探查脚本并追加新版本适配章节，不得直接假设所有车间都与青州首台一致；
4. 本章新增的“安装路径 D:\IntcoEdge、self-contained、首启 NTP、有线网卡绑定、端口 5080/5188、可观测 Day1 落地、MES Mock 先行、A2 主方案、不碰加密狗/SourceManager/海康配置”为青州首站不可妥协项。

---

> 本节结束。v0.1 原文中后续若出现编号与本节相同的章节，均不视为冲突：本节为现场适配增补章节，原文不删不改；后续版本如需统一编号，应另行出 v0.3 结构重整版，不在本次 Append 操作中处理。