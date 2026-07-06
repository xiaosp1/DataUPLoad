# TODO — 工业数据采集与智能分析平台

> 最后更新：2026-07-06 22:25
> PM：🏭 锋卫

## 🔥 P0 阻塞
- [ ] （无）

## 🚧 Sprint 0：MVP Day1-Day5（MES上传+Mock先行，5天）

### Day1（当前）
- [ ] #1: 创建 .NET 8 解决方案骨架（IntcoEdge.sln + EdgeHost/EdgeCommon/EdgeMesUpload/EdgeStorage/EdgeTray 项目）
- [ ] #2: 接入 Serilog 结构化日志（JSON滚动30天、D:\IntcoEdge\logs\、敏感字段打码、动态级别切换）
- [ ] #3: 健康检查端点 /health /health/ready /health/live
- [ ] #4: MES上传 DTO + IMesUploader 接口定义 + 幂等键规则
- [ ] #5: WPF壳最小版（托盘、服务状态、最近日志、导出诊断包按钮）
- [ ] #6: 诊断包导出初版（环境快照+日志+配置打码）
- [ ] #7: 补 scripts/README.md（probe-edge.ps1 使用说明）

### Day2
- [ ] #8: MES上传核心：HttpClient工厂+超时+指数退避重试+熔断
- [ ] #9: SQLite本地队列 schema + 待上传/发送日志/死信表 + 幂等键唯一索引
- [ ] #10: 断网检测+补传+限速
- [ ] #11: 幂等去重（本地+MES返回重复识别）

### Day3
- [ ] #12: MES Mock服务（登录+接口+200/401/500/超时模拟+收到报文留存）
- [ ] #13: 上传规则配置（地址/认证/批量/类别开关，热更新）
- [ ] #14: 上传审计日志（traceId贯穿）
- [ ] #15: Mock对账页面/接口

### Day4
- [ ] #16: WPF诊断页面（环境/端口冲突/诊断包导出/日志级别切换）
- [ ] #17: WPF实时上传状态看板（队列/成功率/最近错误/手动补传）
- [ ] #18: WPF日志查看器（实时tail/级别过滤/模块过滤/搜索）

### Day5
- [ ] #19: self-contained win-x64 单文件发布脚本
- [ ] #20: install.ps1（强制D:\IntcoEdge\、ACL、端口/磁盘/网卡预检查、Windows Service注册）
- [ ] #21: uninstall.ps1（停服务/保留数据可选）
- [ ] #22: smoke.ps1 冒烟测试脚本
- [ ] #23: README + 现场部署文档 + FAQ

## 📋 Backlog（Sprint 1+）
- [ ] A2 海康 PG intco 只读采集器（拿到只读账号后启动）
- [ ] OPC-UA 采集（包装机欧姆龙NX102+汇川AC801，点表到位后）
- [ ] EIP 采集（点数机欧姆龙，点表到位后）
- [ ] AI 故障诊断（本地 Qwen2.5/DeepSeek，后置）
- [ ] 中心端 TDengine + MySQL + Vue3/ECharts 大屏（后置）

## 📥 等待外部输入
- [ ] 海康 PG intco 库只读账号+表结构（协调海康/IT）
- [ ] MES 真实地址、认证方式、接口规范（协调MES团队，Mock先行）
- [ ] 青州首站具体车间代码（QZN1/QZP1等，部署前确认）
- [ ] NTP 时间源地址（IT提供）
- [ ] 包装机/点数机 PLC 点表（电气/设备商提供）

## ✅ 最近完成
- [x] 五轮需求澄清 → docs/prd.md
- [x] 技术方案 v0.1 + §18 v0.2 现场适配 → docs/architecture.md
- [x] 工控机探查清单+probe-edge.ps1 v1.2 → docs/domain/ + scripts/
- [x] 青州现场探查完成，关键决策定案（A2/D:\IntcoEdge\self-contained/端口5080+5188）
- [x] 项目按 AGENT-OS v1.0 规范重构workspace结构
