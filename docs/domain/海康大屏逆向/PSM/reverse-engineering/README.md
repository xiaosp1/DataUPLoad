# PSM 反编译工程文档

**生成时间**：2026-07-20
**被反编译对象**：`E:\DEMO\数据采集\docs\domain\海康大屏逆向\PSM\server\IntcoScreen-1.0-SNAPSHOT-20260605135937.jar`
**反编译产物**：`PSM/server/decompiled/`（204 个 .java / 786 KB）
**反编译工具链**：CFR 0.152 + PSM 自带 OpenJDK 17.0.1（hik-java.exe）
**反编译日志**：`D:\IntcoEdge\tmp\cfr.log`

> ⚠️ **本文档是产品级正式文档**（不是 memory 流水账）。
> 任何要查"PSM 怎么对接、字段是什么、端点是什么"的问题，**查这里**，不要查 `memory/`。

---

## 📑 文档目录

| 文件 | 用途 |
|---|---|
| `README.md`（本文件） | 文档索引 + PSM 总体概况 |
| `01-package-info.md` | 包元信息（about.json）+ 技术栈 + 运行配置 |
| `02-architecture.md` | PSM 模块架构 + 250 个类分布 + 模块依赖图 |
| `03-api-endpoints.md` | 30+ 个 HTTP 端点清单 + 请求/响应 DTO 路径 |
| `04-dto-field-mapping.md` | ★ 关键 DTO 字段详单（EdgeHost 对接直接用） |
| `05-edgehost-psm-mapping.md` | ★ 我们 EdgeHost 接口 ↔ PSM 端点一对一映射表 |
| `06-sql-schema.md` | 从 V1.0~V1.19 迁移脚本还原的 PSM 数据库 schema |
| `07-decisions-and-risks.md` | 5 个决策清单（待老板拍） + 已知风险 + 建议方案 |

---

## 🔑 一句话总结

**海康 PSM = Spring Boot 3 大屏中间件**。
- 它**接收**现场检测设备推过来的缺陷/状态/报警（POST /client/data/*）
- 它**自己**拉英科系统（192.168.80.33:10031）拿到缺陷记录（/client/yk/*）
- 它**展示**给操作员看（web 管理界面 + 大屏）
- 它**不直接**跟 MES 系统对接——**这是我们 EdgeHost 的活儿**

---

## 🏗️ PSM 包元信息（about.json）

| 字段 | 值 |
|---|---|
| 包名 | 英科中屏 |
| Server 版本 | **2.1.9** |
| Web 版本 | null（web 资源已内嵌到 jar 的 BOOT-INF/classes/web/） |
| Manager 版本 | V1.0.0 |
| 包编译时间 | 2026-02-09 10:29 |
| 包安装时间 | 2026-03-03 11:01 |
| 项目位置 | 空（部署现场空填） |

## ⚙️ 技术栈

| 维度 | 选型 |
|---|---|
| 框架 | Spring Boot **3.0.5** |
| JDK | **17.0.1**（OpenJDK，PSM 自带 `jdk/bin/hik-java.exe`） |
| ORM | MyBatis-Plus（Mapper 接口 + JsonArrayTypeHandler） |
| 数据库 | **PostgreSQL**（`jdbc:postgresql://127.0.0.1:5432/intco`） |
| 校验 | jakarta.validation + Hibernate Validator |
| 端口 | **80 (HTTP) / 443 (HTTPS, JKS 证书)** |
| TLS 证书 | `./keystone/hikrobot.jks`（密码 `hik12345`） |
| 加密狗 | type=41007, generation=5 |
| Flyway 迁移 | 启用（V1.0 ~ V1.19 共 19 个脚本） |

## 🌐 关键外部依赖

| 依赖 | 配置项 | 备注 |
|---|---|---|
| **英科系统** | `yk.url=http://192.168.80.33:10031/api/dataportal/invoke` | PSM 通过这个拉英科的缺陷数据 |
| | `yk.username=HKSJSB, password=HKSJSB123` | 英科系统登录凭证 |
| | `yk.workshop=QZN2` | 工厂编号 |
| | `yk.login-interval=50` | 重新登录间隔（分钟） |
| **数据库** | `jdbc:postgresql://127.0.0.1:5432/intco` | 本机 PostgreSQL，库名 `intco` |
| **加密狗** | type=41007 | 海康硬件加密狗 |

## 🔓 公开端点（无需登录）

`application-prod.yml` 里 `hik-security.auth.uri-permit`：
- `/data/**`（图片/文件访问）
- `/ws/**`（WebSocket）
- `/img/**`
- `/client/**`（★ 我们的 EdgeHost 全调这个）
- `/web/upload/file`

## 📂 反编译产物结构

```
PSM/
├── about.json                    ← 包元信息（hex 验证 ✅）
├── uninst.exe                    ← 卸载程序
├── server/                       ← ★ 反编译目标在这里
│   ├── IntcoScreen-1.0-SNAPSHOT-20260605135937.jar  ← 80.6 MB, 250 个 class
│   ├── decompiled/               ← ★ 204 个 .java 反编译产物
│   │   └── com/hikrobotics/solution/
│   │       ├── Application.class → Application.java
│   │       ├── common/           ← 4 config + 4 constant + 1 handler + 1 task + 3 util
│   │       └── module/
│   │           ├── line/         ← 52 类（最大）
│   │           ├── detect/       ← 44 类
│   │           ├── alarm/        ← 36 类
│   │           ├── yingke/       ← 14 类（★ 我们用）
│   │           ├── screen/       ← 6 类
│   │           ├── defect/       ← 2 类
│   │           └── config/       ← 2 类
│   ├── config/                   ← application.yml + application-{dev,local,prod}.yml
│   ├── sql/                      ← V1.0 ~ V1.19 Flyway 迁移脚本
│   ├── lib/                      ← 第三方 jar（jdk/jre 内部）
│   ├── jdk/                      ← PSM 自带 OpenJDK 17
│   ├── keystone/                 ← hikrobot.jks 证书
│   ├── signscript/               ← 签名脚本
│   ├── data/                     ← 上传文件 + 内嵌 web
│   ├── log/                      ← 运行日志
│   ├── web/                      ← 内嵌前端
│   ├── start.bat / start.sh      ← 启动脚本
│   ├── pom.xml                   ← Maven 配置
│   └── signature.md              ← 安装签名
├── backup/server/.../            ← 老版本备份（含同结构 sql + jar）
├── postgres/                     ← 自带 PostgreSQL（不要碰，已运行）
├── manager/                      ← 海康配置工具
├── driver/                       ← 加密狗驱动
└── reverse-engineering/          ← ★ 我们做的反编译文档（这个目录）
    ├── README.md                 ← 索引（本文件）
    ├── 01-package-info.md
    ├── 02-architecture.md
    ├── 03-api-endpoints.md
    ├── 04-dto-field-mapping.md
    ├── 05-edgehost-psm-mapping.md
    ├── 06-sql-schema.md
    └── 07-decisions-and-risks.md
```

## 🔧 反编译质量说明

- **204/250 = 81%** 类的源码被还原（CFR 跳过了 lambda + 部分 inner class）
- **Controller 层 100% 可读**（简单函数 + @RequestMapping 注解全保留）
- **DTO 层 100% 可读**（字段 + @NotBlank/@NotNull/@Range 校验全保留）
- **Service 接口 100% 可读**，ServiceImpl 部分 lambda 有 `Could not load the following classes` 警告
- **MyBatis Mapper 接口 100% 可读**（interface + @Param 注解保留）

**对 EdgeHost 开发够用**——我们只需要端点路径 + DTO 字段 + 校验规则 + SQL schema，这四样都拿到了。

---

## 📖 怎么用本文档

### 场景 1：写新 EdgeHost 接口（要往 PSM 推数据）

→ 查 [`03-api-endpoints.md`](03-api-endpoints.md) 找端点
→ 查 [`04-dto-field-mapping.md`](04-dto-field-mapping.md) 抄字段定义
→ 查 [`05-edgehost-psm-mapping.md`](05-edgehost-psm-mapping.md) 看我应该发什么

### 场景 2：理解 PSM 内部表结构

→ 查 [`06-sql-schema.md`](06-sql-schema.md) 看 V1.0~V1.19 拼出来的 schema
→ 对照 `PSM/server/sql/V*.sql` 看原始脚本

### 场景 3：要做决策（新 EdgeHost 技术栈、现场 exe 命运）

→ 查 [`07-decisions-and-risks.md`](07-decisions-and-risks.md) 看 5 个待拍决策

### 场景 4：理解 PSM 整体架构

→ 查 [`02-architecture.md`](02-architecture.md) 看模块依赖

---

## ⚠️ 重要约定

1. **本目录所有 .md 都是正式文档**，跟 `memory/` 严格分开
2. **memory/** 只写"什么时候做了什么、踩了什么坑、PM 经验"——不写"PSM 怎么对接"
3. **PM 新铁则**：本目录下任何文件**写完必须 hex 验证**（不依赖"Successfully wrote"返回）
4. **反编译产物**只读不写，原始 .java 在 `server/decompiled/`，改完放新目录不覆盖原产物
5. **本目录是 EdgeHost 工程的"协议契约"来源**——以后 EdgeHost DTO 必须跟 PSM DTO 字段 1:1 对齐
