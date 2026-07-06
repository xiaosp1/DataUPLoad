# 海康工控机探查脚本使用说明

## 要带过去的文件
只需要一个：`probe-edge.ps1`

## 前置：你要知道的唯一信息
海康软件安装根目录，例如 `D:\PSM`。（到现场后从桌面图标->右键->属性->起始位置/打开文件位置 也可确认）

## 运行（任选一种）
在仓库根目录下执行：
```powershell
powershell -ExecutionPolicy Bypass -File scripts\probe-edge.ps1 -HikRoot "D:\PSM"
```
或：
1. **最省事**：在工控机上右键 `probe-edge.ps1` -> "使用 PowerShell 运行"。
   看到提示 `Please enter Hikvision install root` 时，把海康根目录粘进去回车。
2. **命令行方式**：
   ```powershell
   cd scripts
   Set-ExecutionPolicy -Scope Process Bypass
   .\probe-edge.ps1 -HikRoot "D:\PSM"
   ```

## 等多久
10-20 秒。看到 `[probe] DONE -> ...reports\probe\probe-report-<机器名>-<时间>.md` 就完成了。

## 产出
报告输出到仓库根目录下的 `reports/probe/`，文件名 `probe-report-<hostname>-<yyyyMMdd-HHmmss>.md`。**直接把这个 md 文件发回给我**，结论脚本已经自动算好：
- ✅ GREEN + A2 直读 PG：直接启动开发，不需要代理；
- ✅ GREEN + DUAL：A2 直读为主，同步准备 A1 代理做双校验；
- ⚠️  YELLOW + A1：PG 暂不通，先走 A1 代理，同时找海康要 PG 只读账号；
- ❌ RED：关键信息缺失，报告里会写明缺啥（一般是 MES 地址或 PG 密码），按提示补一下再跑一次。

> 说明：`reports/` 目录已在根目录 `.gitignore` 中忽略，探测报告不会入库，避免敏感信息泄漏。

## 脚本做了什么（全程只读）
1. 机器基础信息（OS/CPU/内存/磁盘/网卡 IP）
2. .NET 运行时、时区/NTP、RDP 状态
3. 海康相关进程（hik-*/java/postgres/看门狗类）
4. 所有 LISTENING 端口，标出 80/443/5432/8080/海康 java 进程
5. 在海康根目录下扫描配置文件，提取 MES URL、JDBC、数据库密码（自动脱敏）
6. 探测 PostgreSQL：进程是否在、5432 绑定地址、默认密码尝试（仅内置几个海康默认口令，不做爆破）、连通则列出库表
7. 对配置中提取到的 MES 端点做 TCP 连通性测试
8. 扫描海康近 24 小时日志，统计 error/exception/upload 关键字
9. CPU/内存 Top 进程快照
10. **最终自动判定 A1/A2/DUAL 推荐方案**

## 注意事项
- 全程只读：不停服务、不改配置、不删文件、不写海康目录。
- 建议以管理员身份运行 PowerShell（右键 PowerShell -> 以管理员身份运行），防火墙/部分服务信息更全；普通用户也能跑，只是个别项会显示 n/a。
- 密码在报告里自动脱敏（只保留前后 2 位），真实密码请通过保密渠道单独传递，不要在群里或 md 里出现。
- 脚本绝对不触碰硬件加密狗。