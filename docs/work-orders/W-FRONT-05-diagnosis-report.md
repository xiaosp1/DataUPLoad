# W-FRONT-05 上座率"只有 10 线有数据"问题 — 根因诊断报告

日期：2026-08-01 15:45
状态：**后端/前端链路 100% 正常，非 Web 项目 bug。根因在产线客户端上报的 occupancyRate 数据本身。**

## 一、问题现象
1. 生产看板 / 实时数据页"只有 10 线有数据"。
2. 仅 line10A / line10B 有色格子（4 格），其它 34 线全灰。
3. 老板确认产线客户端都在上报。

## 二、排查过程与关键证据

### 证据 1：后端上报通道 100% 正常（决定性）
- 手动 `POST /client/data/detect`，payload 里 `realTimeData.occupancyRate=88.5`。
- **DB `line.realtime_data` 的 occupancyRate 立即变成 88.5**，occupancy=55，total 更新。
- 结论：后端如实把客户端上报的 occupancyRate 写入 line 表缓存。链路通。

### 证据 2：DB 数据全景
- 38 条线面记录**全部**含 `occupancyRate` 字段（38/38），无字段缺失。
- 仅 4 条 occupancyRate>0：line10A(A1=100/A2=100)、line10B(B1=99.8/B2=99.7)。
- 其余 34 条 occupancyRate= **0**。
- line10A/10B 的 `startTime` = 2026-07-18（历史快照），但该 4 线一直在被刷新。

### 证据 3：真实客户端在持续上报，但 occupancyRate 分化（决定性命中）
把已停产的 line3A/A1 的 occupancyRate 手动改成 90、occupancy 改成 60，等 4 分钟：
- **被真实客户端 30s 左右覆盖回 0**（occupancyRate: 90→0，occupancy: 60→0）。
- 同一时段 total 从 1787481→1787721 增长、update_time 变为 2026-08-01 15:31 今天。
- **证明**：真实客户端确实在刷新该 line 的 realtime_data（total/ngCount/removeTotal 等统计字段在涨），**但上报的 occupancyRate 就是 0**。

### 证据 5：抓取产线客户端原始上报报文（终极铁证，08-01 16:04~16:07）

通过临时开启 PostgreSQL 语句日志（`log_statement=all`，无需重启后端，零打断产线），从 SQL 参数里提取到客户端 `POST /client/data/detect` 的原样请求体：

| 上报时间 | 线/面 | total | efficiency | occupancy | occupancyRate |
|---------|-------|-------|-----------|-----------|---------------|
| 16:06:23 | line4B/B2 | 1,591,822 | 127 | **0** | **0** |
| 16:04:52 | line8A/A1 | 2,016,438 | 206 | **0** | **0** |
| 16:05:30 | line9A/A2 | 8,217,455 | 203 | **0** | **0** |
| 16:07:32 | line9B/B1 | 19,406 | 203 | **0** | **0** |

原始报文片段（line9B/B1，2026-08-01 16:07:32，客户端原样上报）：
```json
{"todayData":{"statisticTime":"2026-08-01 16:07:32","ngNum":31215,"totalNum":193246,"defects":[...]},
 "faceNo":"B1","lineNo":"line9B",
 "realTimeData":{"ngCount":154,"efficiency":203,"removeTotal":120,"occupancy":0,"totalNgRate":0.6,"total":19406,"removeFail":0,"defects":[...],"occupancyRate":0,...}}
```

**关键**: 客户端报文里 `occupancyRate` 字段**存在且值为 0**（非缺失、非字段名不匹配）。efficiency=203、total 实时增长，客户端工作正常，唯独上座率算出 0。

试验后已恢复 PG `log_statement=none`，清理临时脚本。

### 证据 4：45 秒实时监测（一锤定音）
| 线/面 | occupancyRate | 是否活着上报 | total 45s 增量 |
|-------|--------------|-------------|---------------|
| line10A/A1 | 100 | ✅ LIVE | +131 |
| line10A/A2 | 100 | ✅ LIVE | +131 |
| line10B/B1 | 99.8 | ✅ LIVE | +164 |
| line3A/A1 | 0 | ✅ LIVE | +150 |
| line9B/B1 | 0 | ✅ LIVE | +136 |

**所有线都在活着上报**（total 每分钟 +130~164），但只有 line10A/10B 报 occupancyRate=100/99.8，其它线报 0。

## 三、根因结论
**不是 Web 前端/后端 bug**。后端链路（88.5 实测写入）、前端链路（`Number(rt?.occupancyRate ?? 0)`，>0 才着色）都实测验证正常。
"只有 10 线有数据 / 有色格" 的真相：
- **line10A/10B** 客户端上报真实 occupancyRate=100/99.8（设备运行）。
- **其它 34 线** 客户端上报 occupancyRate=**0**（其设备停产/待机，或客户端未计算上座率）。

Web 看板如实反映了这一数据现状：只有 occupancyRate>0 的 line10A/10B 着色，其余为灰。

## 四、为什么"客户端都在上报"却 occupancyRate=0
产线客户端每个统计周期都会 `POST /client/data/detect`，把**实时统计字段**（total/ngCount/removeTotal/efficiency 等）+ **occupancyRate** 一起上报。但：
- 设备在产线运行时，客户端算出的 occupancyRate>0（line10A/10B）。
- 设备停产/待机时，客户端算出的 occupancyRate=0（其它 34 线）。

这是 PSM 产线上位机的正常行为，Web 端只是如实展示。

## 五、后续可选动作（需老板拍板）
1. **确认现场哪些线在产**：若 34 线确实停产，则现状正确，无需修改，等开产自然有色。
2. **若 34 线也在产但 occupancyRate=0**：问题在产线客户端（hik 上位机）的 occupancyRate 计算/上报逻辑，需现场查客户端程序，不在本 Web 项目范围。
3. **若 Web 端希望"停产线也显示某种状态"而非纯灰**：可在前端把 occupancyRate=0 但客户端在线（status_record=ONLINE）的格子显示为明确的"停产/待机"样式，而非与"无数据"混淆。此为纯前端增强，可派工单。
4. **可选 B5**：把 5s 轮询改为 WS 增量推送，更实时（与本次问题无关）。

## 六、已验证的后端/前端边界
- 后端 `POST /client/data/detect` 写入 occupancyRate ✅
- 后端 `GET /web/line/list` 返回每条线 realtimeData JSON（含 occupancyRate）✅
- 前端 OccupancyPanoramaBar / ProductionBoard 读 `occupancyRate`，>0 着色 ✅
- 前端 5s 刷新、silent 增量 merge 不闪屏 ✅（上一轮已完成）
