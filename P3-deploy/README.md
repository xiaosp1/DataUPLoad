# DataupLoad P3 车间部署包

> 2026-08-03 生成 · 车间号 QZP3 · 部署目录 D:\DataupLoad\

## 包结构

```
P3-deploy/
├── install.bat          # 一键安装（管理员运行）
├── Manager/             # 桌面端管理工具（便携版 exe，双击即用）
│   └── DataupLoad-Manager-0.1.0-portable.exe
├── server/              # 后端服务
│   ├── jdk/             # JDK（hik-java）
│   ├── lib/             # 依赖库（142 jar）
│   ├── target/classes/  # 编译产物
│   ├── config/          # application-prod.yml（P3 配置）
│   ├── web/             # 前端 Vue 3 SPA
│   └── sql/             # Flyway 建库脚本（19 个）
└── pg/                  # 嵌入式 PostgreSQL
    ├── postgresql.exe   # PG 静默安装器
    ├── HeidiSQL.exe     # 数据库管理工具
    └── script/          # 启停脚本
```

## 快速安装（3 步）

1. 整个 `P3-deploy` 文件夹拷到 P3 工控机 `D:\DataupLoad\`（与 install.bat 同级）
2. **右键管理员运行** `install.bat`
3. 等脚本完成（自动：装 PG → 建库 intco → 写白名单 → 启后端 → 配开机自启）

完成后访问：**http://127.0.0.1:8080** （账号 super_admin / Abc12345）

## 配置（P3 定制，已写入 application-prod.yml）

| 项 | 值 |
|----|-----|
| 车间号 workshop | QZP3 |
| 后端端口 | 8080 |
| MES 地址 | http://192.168.32.86:1025/api/dataportal/invoke |
| 报警推 MES | uploadEnabled: true |
| 数据库 | 127.0.0.1:5432/intco（嵌入式 PG，密码 Abc12345） |
| 连接池 | 200/40/30s（100 产线规模） |
| 白名单 | 127.0.0.1 + 192.168.137.180 + 192.168.135.50~65（16 产线 IP）+ *.*.*.* |

## 产线 IP 对照（白名单）

```
Line1A=192.168.135.50  Line1B=.51  Line2A=.52  Line2B=.53
Line3A=.54  Line3B=.55  Line4A=.56  Line4B=.57
Line5A=.58  Line5B=.59  Line6A=.60  Line6B=.61
Line7A=.62  Line7B=.63  Line8A=.64  Line8B=.65
```

## 桌面端使用

双击 `Manager\DataupLoad-Manager-0.1.0-portable.exe`：
- **总览**：服务状态 / 产线连接 / 一键启停
- **后端服务**：启停 / 重启 / 日志
- **数据库**：PG 安装 / 启停
- **参数配置**：改车间号 / MES 地址 / 白名单 IP（改完重启后端生效）
- **部署体检**：组件完整性
- **系统设置**：开机自启 / 看门狗

## 日常运维

- 开机自启已配置（安装时写入启动文件夹）
- 看门狗：PG 或后端异常退出会自动拉起
- 日志位置：`server\log\DataupLoad\error.log`
- 数据备份：用 HeidiSQL 连接 127.0.0.1:5432 导出 intco 库

## 注意事项

- install.bat 需**管理员权限**（装 PG 服务 + 写启动文件夹）
- 若 8080 被占用：改 `server\config\application-prod.yml` 的 `server.port`，或桌面端"参数配置"改
- 若 P3 产线 IP 有变化：桌面端"参数配置"→ 白名单 IP 更新 → 保存 → 重启后端
- MES 地址若换生产环境：改 `yk.url` 为 `http://192.168.80.33:10031/api/dataportal/invoke`
