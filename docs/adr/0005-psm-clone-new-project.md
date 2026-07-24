# ADR-0005: 另起项目复刻 PSM（Java + Spring Boot + PG 全栈）

| 字段 | 值 |
|---|---|
| 状态 | **Accepted**（老板 2026-07-22 13:05 拍板 + 13:13 拍 5 件事细节）|
| 日期 | 2026-07-22 13:05 + 13:13 修订 |
| 决策者 | 老板（尘 醒）|
| 影响范围 | 另起新项目（不动现有 EdgeHost）|
| 上一版 | ADR-0004（被 13:05 指令推翻）|
| 关系 | **推翻 ADR-0004 边缘复刻路线**，转向完整 PSM 全栈复刻 |

---

## 老板 13:13 拍板的 5 件事

| # | 事项 | 决定 |
|---|------|------|
| 1 | 新项目命名 | **DataupLoad** |
| 2 | 新项目路径 | **E:\DEMO\数据采集\DataupLoad\** |
| 3 | IP / 端口 | **192.168.135.150:80**（与原 PSM 同地址端口）|
| 4 | 飞书推送 | **沿用 PSM 现有链路**：报警 → MES → 飞书（MES 已有转飞书的功能），**不新做飞书直推** |
| 5 | PG 12 安装位置 | **E:\PostgreSQL\12\** |

---

## 老板 13:13 拍板的飞书推送链路澄清

老板原话："上传飞出报警这个其实原先我们已经做过了，只不过是被麦斯转了一手，其实原先的功能是我们将报警信息推送到麦斯之后，麦斯自己去推送飞书的这块，之前我们干过了，而且psm这款软件里面也有这部分功能"

**PM 解读**：
- **不是新做飞书直推**：之前 EdgeHost 已经做过 → 把报警推 MES，MES 自己转飞书
- **PSM 也有这部分**：PSM 通过 yk（鹰科）模块推 MES，MES 转飞书
- **新项目沿用 PSM yk 模块**：报警写入 → yk 推送 → MES → 飞书
- **不新写飞书 webhook client**

PM 立即把原 W-B04 工单里的"飞书直推"改成"沿用 PSM yk 推送链路"。

---

## 决策（沿用 ADR-0005 原内容）

**另起新项目 `DataupLoad`，完整复刻 PSM 全栈**。

### 1. 新项目技术栈（与 PSM 完全一致）

| 维度 | PSM 用 | 新项目用 |
|---|---|---|
| 语言 | Java 17 | ✅ **Java 17** |
| 框架 | Spring Boot 3.0.5 | ✅ **Spring Boot 3.0.5** |
| ORM | MyBatis-Plus + framework-starter | ✅ **MyBatis-Plus + framework-starter**（从 PSM fat jar 提取手动 install） |
| 数据库 | PostgreSQL 12 + Flyway V1.0~V1.19 | ✅ **PostgreSQL 12 安装到 E:\PostgreSQL\12\** + Flyway V1.0~V1.19（20 个 SQL 完整沿用） |
| 缓存 | (无) | ⚪ **不加 Redis**（PSM 没有就不加） |
| 任务调度 | @Scheduled + SchedulingConfigurer 5 线程池 | ✅ **@Scheduled + SchedulingConfigurer 5 线程池** |
| 事件机制 | ApplicationEvent + @Async @EventListener | ✅ **ApplicationEvent + @Async @EventListener** |
| Web 容器 | Tomcat (SSL 443) | ✅ **Tomcat** |
| WebSocket | Spring WebSocket 4 类型 | ✅ **Spring WebSocket 4 类型** |
| i18n | zh-CN / en-US / id-ID | ✅ **zh-CN 单语**（车间不需要多语言）|
| 加密狗 | JNA + MV_LoginLicense | ⚪ **不移植**（沿用 ADR-0004）|
| **额外** | （PSM 通过 yk → MES → 飞书）| ✅ **沿用 PSM yk 推送链路**，不新做飞书直推 |

### 2. 新项目目录与部署

```
E:\DEMO\数据采集\DataupLoad\          # 新项目根
├── pom.xml                           # Spring Boot 3.0.5 parent
├── jdk/                              # 复制 PSM jdk/ （自带 hik-java）
├── keystone/                         # 复制 PSM keystone/ （SSL 自签证书）
├── config/
│   ├── application.yml               # 主配置
│   ├── application-prod.yml          # 生产：IP 192.168.135.150 port 80 + DB + yk 配置
│   └── application-local.yml         # 本地
├── sql/                              # 复制 PSM sql/ V1.0~V1.19 完整 20 个
├── src/main/java/com/hikrobotics/solution/
│   ├── Application.java              # PSM 同款入口
│   ├── common/                       # 通用层（13 类，沿用 PSM）
│   ├── module/
│   │   ├── alarm/                    # 报警模块（35 类，复刻）
│   │   ├── config/                   # 系统配置（5 类）
│   │   ├── defect/                   # 缺陷类型（6 类）
│   │   ├── detect/                   # 检测 + retention（37 类）
│   │   ├── line/                     # 产线 + 状态（54 类）
│   │   ├── screen/                   # 大屏（5 类）
│   │   └── yingke/                   # 鹰科（15 类，沿用推 MES → 飞书）
│   └── framework/                    # 框架层（WebSocket / i18n，跳过加密狗）
├── src/main/resources/
│   ├── static/                       # 复制 PSM web/ 前端 94 文件 / 7MB
│   └── logback-spring.xml            # PSM 同款日志格式
└── src/test/java/                    # JUnit 5 + Mockito（沿用 PSM 测试风格）

PG 12 安装位置：E:\PostgreSQL\12\
PG 数据库：intco（沿用 PSM）
PG 用户：postgres / postgres（沿用 PSM）
```

### 3. 端口冲突预案（请老板拍）

新项目用 `192.168.135.150:80`，与现场老 PSM 同地址端口。
- **A：PM 直接停老 PSM 服务**（建议，今晚干净跑）
- **B：新项目改用 8081 端口**（保留老 PSM）
- **C：老板自己停老 PSM**（PM 不动）

**PM 默认按 A 执行**，等老板拍前不动老 PSM。

### 4. 现有 EdgeHost 命运

- ✅ **保留运行**：PID 26980 继续在 192.168.135.150:80 跑
- ⚠️ **新项目上线前必须停**（让位 150:80）
- ✅ **保留代码**：IntcoEdge.sln + src/ 不删
- ✅ **保留数据**：src/IntcoEdge.Db/data/intco.db 不动
- ⚠️ **不再开发新功能**

### 5. 今晚 22:00 验收点（沿用原计划，去掉飞书直推）

| # | 验收项 | DoD |
|---|--------|-----|
| 1 | 视觉软件数据拉进来 | 16 台工控机 POST /client/data/detect → 200 |
| 2 | 数据能解析 | DetectDataUploadDTO 字段全解析，无 NPE |
| 3 | 数据能存库 | PG defect_day_record / line_day_record / status_record 表有真实数据流入 |
| 4 | MES 能拉数据 | GET /client/yk/defect-record 返回数据，MES 系统能消费 |
| 5 | 报警能推飞书 | 报警 → yk 推 MES → MES 转飞书（沿用 PSM 链路，不新做）|
| 6 | Web 大屏能看 | 前端 1:1 复制 PSM web/，能加载并显示实时数据 |
| 7 | PSM 90% 模块跑通 | detect / line / alarm / screen / yingke / config / common 共 7 模块中至少 6 个跑通 |

**不验收**（今晚不做）：
- V1.19 state_change / state_statistic（明日上午）
- ignore_alarm 白名单（明日上午）
- defect_record_backup 3 天 retention（明日下午）
- Spring WebSocket 4 类型（今晚只做 screen + alarm 2 个）
- checkClientStatus 心跳检测（明日上午）
- 现场老 DB 数据迁移（明日下午）

---

## 实施计划（修订）

### 13:30 立即派工（不等老板拍端口冲突）

| 工单 | 内容 | 工作量 | Worker 数 | 修订 |
|------|------|------|---------|------|
| **W-B01** | 新项目脚手架（pom + 入口 + yml + jdk 复制）| 0.5 h | 1 | 不变 |
| **W-B02** | PG 12 部署到 E:\PostgreSQL\12\ + V1.0~V1.19 SQL 跑通 | 1.5 h | 1 | **路径固定 E:\PostgreSQL\12\** |
| **W-B03** | detect 模块（拉数据 + 解析 + 存库）| 3 h | 1 | 不变 |
| **W-B04** | alarm 模块（接收 + 写库 + 触发 yk 推送）| 2 h | 1 | **去掉"飞书直推"，改成沿用 PSM yk 链路** |
| **W-B05** | line 模块（line_day_record + 产线注册）| 1.5 h | 1 | 不变 |
| **W-B06** | WebSocket 推送 + 大屏前端 1:1 复制 | 1.5 h | 1 | 不变 |
| **W-B07** | MES 接口 `/client/yk/defect-record` | 0.5 h | 1 | 与 W-B03 合并 |
| **W-B08** | 接 16 台工控机（协议适配 + 推送测试）| 1.5 h | 1 | 不变 |

**总工作量**：~12 h，**3 个 Worker 并行可在 4-5 h 内完成**。

---

## 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| 今晚 22:00 9 小时完成 PSM 90% 太紧 | 🔴 | 3 个 Worker 并行 |
| framework-starter 私有 maven 仓库拉不到 | 🔴 | 从 PSM fat jar 提取手动 install |
| 现场老 PSM `135.150:80` 还在跑，端口冲突 | 🔴 | 老板拍 A/B/C |
| 现场 16 台工控机 IP 不清楚 | 🟡 | 先跑 PSM 模拟器，IP 清单老板拍后建 line_registry |
| V1.19 SQL 现场 PG 版本不对 | 🟡 | 用 PG 12（与 PSM 一致）|
| Spring Boot 3.0.5 现场 JDK 不兼容 | 🔴 | 复制 PSM `jdk/` 目录（自带 hik-java）|

---

## 相关文档

- ADR-0004 边缘复刻路线（**被本 ADR 推翻**）→ `docs/adr/0004-full-psm-clone-except-dongle.md`
- 今晚完整计划 → `docs/delivered/2026-07-22-psm-clone-tonight-plan.md`（待修订）
- PSM 反编译产物 → `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/`
- PSM 反编译 java → `docs/domain/海康大屏逆向/psm-decompiled/BOOT-INF/classes/com/hikrobotics/solution/`
- PSM Flyway V*.sql → `10-反编译产物-NEW/PSM/server/sql/V*.sql`（20 个）

## 后续 ADR

- **ADR-0006 C# 端 yk.enable 永久熔断 + 3 道熔断门**（W-X13c 2026-07-23 01:55）
  - 文件：`docs/adr/0006-csharp-yk-circuits.md`
  - 关系：补充本 ADR 决策 4（PSM 链路沿用）——Java 端 yk 已被铁则 36 永久熔断；C# 端 `YingkeServiceImpl.cs` 加同样 3 道熔断（配置门 / 票据门 / 白名单门）使 yk 推送在 C# 端也 by design 不可绕过
  - 何时查：老板将来问"C# 端 yk 怎么关的 / 想恢复怎么办"必读；§7 恢复推送操作步骤（3 步：配白名单 → 开 enable → PM 派工重启）是老板专用 SOP
