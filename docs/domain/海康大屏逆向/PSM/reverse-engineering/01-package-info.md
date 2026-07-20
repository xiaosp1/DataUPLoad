# 01. PSM 包信息 + 运行配置

> 本文是 PSM 反编译工程的元信息汇总。

## 📦 包元信息（来自 `PSM/about.json`）

```json
{
  "PackageTime": "2026-02-09 10:29:19",
  "CompileTime": "2026-01-19 09:37:31",
  "InstallTime": "2026-03-03 11:01:24",
  "ProjectInfo": {
    "ProjectName": "英科中控大屏",
    "No": null,
    "Location": ""
  },
  "VersionInfo": {
    "ServerVersion": "2.1.9",
    "WebVersion": null,
    "ManagerVersion": "V1.0.0"
  }
}
```

| 字段 | 值 | 说明 |
|---|---|---|
| PackageTime | 2026-02-09 10:29:19 | 海康出厂打包时间 |
| CompileTime | 2026-01-19 09:37:31 | 海康编译时间（比打包早 21 天） |
| InstallTime | 2026-03-03 11:01:24 | 现场安装时间 |
| ProjectName | 英科中控大屏 | 这是给**英科客户**做的大屏项目 |
| ServerVersion | **2.1.9** | 后端版本 |

## 🛠️ 技术栈（来自 `server/config/application*.yml` + jar MANIFEST）

| 维度 | 选型 | 证据 |
|---|---|---|
| 框架 | Spring Boot **3.0.5** | jar MANIFEST: `Spring-Boot-Version: 3.0.5` |
| JDK | OpenJDK **17.0.1** | `PSM/server/jdk/bin/hik-java.exe` (定制版包装) |
| ORM | MyBatis-Plus | Mapper 接口 (`module/*/mapper/*.class`) + JsonArrayTypeHandler |
| 数据库 | **PostgreSQL** | `spring.datasource.dynamic.datasource.master.url=jdbc:postgresql://127.0.0.1:5432/intco` |
| 校验 | jakarta.validation + Hibernate Validator | DTO 上的 `@NotBlank`/`@NotNull`/`@Range` |
| JSON | Jackson (Spring Boot 默认) | ResponseBody 默认 |
| WebSocket | spring-boot-starter-websocket | `/ws/**` 在 uri-permit 列表里 |
| 加密狗 | type=41007, generation=5 | `DongleUtils.class` + `dongle.type` 配置 |
| Flyway | 启用，out-of-order=true | `spring.flyway.enable=true`，19 个 V*.sql |

## ⚙️ 运行端口（来自 `application-prod.yml`）

| 端口 | 协议 | 用途 |
|---|---|---|
| **80** | HTTP | 主 HTTP 服务 |
| **443** | HTTPS | TLS 服务（用 `keystore=hikrobot.jks` + 密码 `hik12345`） |
| **5432** | TCP | 内置 PostgreSQL |
| （10031） | HTTP | **外部英科系统**（不在 PSM 上，PSM 反向调用） |

## 🔐 关键配置（来自 `application-prod.yml`）

```yaml
server:
  port: 443
  ssl:
    key-store: ./keystone/hikrobot.jks
    key-password: hik12345
    key-store-password: hik12345
  tomcat:
    threads:
      max: 1000

yk:
  enable: true                  # ★ 英科对接开关
  workshop: QZN2                 # ★ 工厂编号
  username: HKSJSB               # ★ 英科系统账号
  password: HKSJSB123            # ★ 英科系统密码
  login-interval: 50             # 重新登录间隔（分钟）
  url: http://192.168.80.33:10031/api/dataportal/invoke  # ★ 英科系统地址

spring:
  datasource:
    dynamic:
      datasource:
        master:
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://127.0.0.1:5432/intco
  flyway:
    enable: true
    out-of-order: true           # ★ 允许跳号执行迁移

hik-security:
  auth:
    uri-permit:
    - /data/**
    - /ws/**
    - /img/**
    - /client/**                 # ★ 公开 client 接口（我们 EdgeHost 用这个）
    - /web/upload/file

hik-log:
  trace:
    uri-ignore:
    - /ws/**
    - /client/data/defect        # 减少日志噪音
    - /client/data/status
    - /client/data/alarm

dongle:
  type: 41007
  generation: 5

mybatis-plus:
  configuration:
    settings:
      useCursorFetch: true        # ★ 大结果集用游标
```

## 🔓 公开端点（无需登录）

根据 `hik-security.auth.uri-permit`：

| 路径前缀 | 用途 | 我们要不要调 |
|---|---|---|
| `/data/**` | 静态文件（图片/上传文件） | ❌ |
| `/ws/**` | WebSocket 实时推送 | ❌（可选，未来用） |
| `/img/**` | 图片 | ❌ |
| **`/client/**`** | **客户端接口（设备推数据 + 英科对接）** | **✅ 我们的全部对接都在这里** |
| `/web/upload/file` | 文件上传 | ❌ |

所有 `/web/**` 接口（除 upload/file）都需要登录——是海康给运维人员用的管理后台，**我们不用**。

## 🗄️ 数据库

| 项 | 值 |
|---|---|
| 类型 | PostgreSQL |
| 地址 | `jdbc:postgresql://127.0.0.1:5432` |
| 库名 | `intco` |
| 用户名 | （在 application-prod.yml 里没显示，可能用 hikvision 默认） |
| 密码 | （同上） |
| 迁移 | Flyway 管理，19 个 V1.0~V1.19 脚本 |
| 主备 | dynamic-datasource 配置（master + 可能 slave，本次只看 master） |

⚠️ **注意**：**EdgeHost 现场用的是 SQLite (D:\IntcoEdge\data\intco.db)**，跟 PSM 的 PostgreSQL **不是同一个库**。PSM 部署在工厂机房的另一台机器上，我们 EdgeHost 不直接读写 PSM 的库，只通过 REST 接口。

## 🔗 外部依赖清单

| 依赖 | 地址 | 凭证 | 用途 |
|---|---|---|---|
| **英科系统** | `http://192.168.80.33:10031/api/dataportal/invoke` | HKSJSB / HKSJSB123 | PSM 拉英科缺陷数据 |
| **PostgreSQL** | `127.0.0.1:5432/intco` | （未明） | PSM 本地数据库 |
| **加密狗** | 本机 USB | type=41007 | PSM 启动授权 |
| **海康 Dongle License Server** | （未明） | dongle.generation=5 | 加密狗验证 |

## 🚀 启动 PSM（参考 `start.bat`）

```bat
cd PSM\server
hik-java.exe -jar IntcoScreen-1.0-SNAPSHOT-20260605135937.jar ^
  --spring.profiles.active=prod ^
  --server.port=443 ^
  --hik-security.auth.secret=<secret>
```

**实际启动参数**取决于现场的 `start.bat`（可能包含数据库密码、加密狗参数等）。
**⚠️ 不要随便重启 PSM**——会影响现场生产。

---

## 📂 原始文件位置

- `PSM/about.json` — 包元信息
- `PSM/server/config/application.yml` — 主配置
- `PSM/server/config/application-prod.yml` — 生产配置
- `PSM/server/jdk/bin/hik-java.exe` — PSM 自带 JDK
- `PSM/server/keystone/hikrobot.jks` — TLS 证书
- `PSM/server/start.bat` — 启动脚本
- `PSM/server/pom.xml` — Maven 配置（依赖列表）
