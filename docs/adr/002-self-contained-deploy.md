# 002. 部署策略：self-contained单文件 + D:\IntcoEdge强制根目录

日期：2026-07-06
状态：已接受

## 背景

现场工控机情况：
- 已安装.NET Framework 4.8，但**没有.NET 8 Runtime**
- C盘仅剩14.5GB，Windows更新、日志、临时解压、dump都会占C盘
- 海康已占用大量端口
- 运维能力弱，要求一键安装/卸载

## 选项

| 选项 | 优点 | 缺点 |
|---|---|---|
| 框架依赖发布+现场装Runtime | 体积小 | 现场装Runtime增加失败点；工控机可能无外网；版本管理复杂 |
| self-contained单文件 | 不依赖Runtime；xcopy部署；版本锁定 | 包体大（预计150-300MB） |
| 安装到Program Files | 符合Windows惯例 | 写入C盘，C盘紧张；权限问题 |
| 安装到D:\IntcoEdge\ | 不碰C盘业务数据；目录可强制ACL | 需假设D盘存在 |

## 决定

1. **发布方式**：`dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true`，目标机无需安装.NET 8
2. **安装根目录**：强制`D:\IntcoEdge\`，目录布局：
   - `D:\IntcoEdge\app\` 程序
   - `D:\IntcoEdge\config\` 配置
   - `D:\IntcoEdge\data\sqlite\` SQLite缓存
   - `D:\IntcoEdge\logs\` Serilog日志
   - `D:\IntcoEdge\diag\` 诊断包导出
   - `D:\IntcoEdge\backup\` 短期备份
   - `F:\IntcoEdgeArchive\` 长期归档（后续启用）
3. **端口规划**：5080(Web看板)、5188(API+Health+Diag)、6030(TDengine可选)、8100(AI诊断可选)，全部避让海康段
4. **服务形态**：EdgeHost注册为Windows Service；WPF作为托盘辅助
5. **启动前自检**：NTP对时→环境快照→目录自检→连通性探活→配置校验→启动→自报健康

## 后果

- 包体变大但消除Runtime依赖，首台工控机部署风险降低
- 所有业务数据不写C盘，保护系统盘
- install.ps1必须做D盘存在/可写/空间检查，不满足时fail-fast不装
- 后续23车间复制时，D盘假设需要每个车间验证
