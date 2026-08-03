# W-FLASH-01 报告 — 实时页视觉闪烁根治

> 状态：**DONE（8/2 21:1x 端到端验证通过）**
> 类型：前端 + 后端修复
> 根因：2 个（前端后台定时器与 WS 不同源 + sendScreen NPE 导致 WS screen 通道推不出数据）

---

## 一、问题

老板 8/1 反馈实实时页 `/realtime`「连续图形闪烁」。实测发现反复闪烁来自两个叠加原因。

## 二、根因（两个）

### 1. 前端：`OccupancyPanoramaBar` 用独立 `setInterval` 轮询 `lineStore.load()`
- 它每 5s 自己拉一次全量数据，与 WS推送节奏不同步 → 上座率条每次重绘时先清空再填充 → 肉眼闪烁。
- `RealTime.vue` 的 `watch` 用 `deep`，store 任一字段变都触发整图重绘。

### 2. 后端：`sendScreen` 每 5s 抛 NullPointerException
- `GlobalTaskManager.sendScreen:140` 调 `ScreenServiceImpl.sendScreenDataInfo()` → NPE。
- 定位：堆栈指向 `buildScreenData` 排序行 `Comparator.comparingInt(Line::getOrder)`。
- **根因**：`Line.order` 字段带 `@TableField(exist = false)`，MyBatis-Plus 从不从 DB 装载，恒为 `null`；`comparingInt` 拆箱 null 直接 NPE。
- 后果：WS screen 通道**从未成功推过数据**（probe 实测 12s 0 条；backup 日志 8/1 全天 12985 次调用 0 次成功）。
- 前端 1-5 改造即使做好也**无后端数据可驱动** → 白改。

## 三、修复内容

### 后端（1 文件）
`ScreenServiceImpl.java:84` 排序逻辑改为 null-safe：
```java
Comparator.comparingInt((Line l) -> l.getOrder() == null ? Integer.MAX_VALUE : l.getOrder())
          .thenComparing(Line::getColor, Comparator.nullsLast(Comparator.naturalOrder()))
```
`GlobalTaskManager.java` catch：沿用吞错（防 @Scheduled 中止），保留完整堆栈便于将来定位。

### 前端（方案 A：统一 WS 单一刷新源）
- `stores/screen.ts`：W-FLASH-01 新增全局 WS 单例快照（`screenState.snapshot.lines[]`，5s 服务端全量广播）。
- `OccupancyPanoramaBar.vue`：**删除**独立 `setInterval` 轮询 → 改为 `subscribeScreen` 消费 WS 快照；新增 stale 降级（WS 断/过期 10s → 停末帧 + 显示「連接斷開」轻标注，不闪空白）。
- `RealTime.vue`：
  - `applySnapshotToSelected` 浅比较增量 patch（值未变不换引用）。
  - `watch` 去 `deep`。
  - echarts `init-once` + 节流增量 `setOption(notMerge:false)`；数据签名未变跳重绘。
  - KPI 去 loading 闪烁。

## 四、验证（8/2 21:1x，Playwright headless）

| 项 | 结果 |
|----|------|
| 登录 → /#/realtime | ✅ |
| 上座率条渲染 | ✅ |
| KPI 渲染 | ✅ |
| 未跳 403 / login | ✅ |
| WS 数据新鲜（无「連接斷開」+ 有数值） | ✅ |
| console errors | **0** |
| page errors | **0** |
| WS screen 通道（node ws probe） | ✅ 收到真实数据 `line1A/A1 removeTotal=22` |

### 修复过程中的额外问题（一并解决）
1. **chartInitedFlag `is not defined`**：初版把 init-once flag 声明在注释行内/用 `ref`（被 esbuild 误删），改为模块顶层 `var chartInitedFlag=false` 并对引用处用 `!chartInitedFlag` / `=true` 后正常（minify 命名为 `$`）。
2. **interceptor 动态 import 404**：部署时只拷 `index-*`，漏了 `interceptor-*.js` chunk；改拷全部 assets。
3. **登录 10500**：早前 curl 在 PS 里转义出错（body 非法 JSON）误报；用 `node http` 直发验证 login 本就正常。
4. **连接池泄漏（Failed to obtain JDBC Connection）**：PID 30244 那次启动后 Hikari 连接池被耗尽（leak detection），所有 DB 操作失败 → WS screen 也推不出。**重启后清零**，3 分钟 0 leak / 0 fail。
5. **连接池泄漏根治（8/2 23:12 深夜，P0）**：重启后 **30~40 分钟内 50 连接又被长事务占满**，页面再次无数据。
   - **真因（DB 实测铁证）**：`DefectRecordServiceImpl.handleDetectData()`（`POST /client/data/detect` 入口，38 条产线每 5s 并发上传）的 `@Transactional(rollbackFor=Exception.class)` 把 `line_defect_type` / `defect_day_record` / `line_day_record` / `line` 四张表几十条 SQL 包进一个长事务。并发一高，事务互相锁等待（`transactionid` Lock）+ `idle in transaction`（SQL 做完但事务不提交）→ 连接被挂住不还 → 50 连接占满 → sendScreen/login 拿不到连接 → 页面无数据。
   - **已补 DB 索引**（加速查询，有效但不根治）：`uq_line_defect_type_line_face_name`（UNIQUE (line_no,face_no,name)）、`ix_defect_day_record_lf`、`ix_defect_day_record_tlf`、`ix_line_day_record_tlf`、`ix_status_record_lnf`、`ix_status_record_ut`。
   - **根治（改代码）**：去掉 `handleDetectData` 的方法级 `@Transactional`，各写操作改单条 SQL autocommit（连接用完即还，避免跨表持锁）。统计按小时、失败重传即可，无需跨表事务原子性。
   - **验证**：修复后 PG 实时指标 `long_txn=0 / waiting_lock=0 / pg_locks not granted=0`（修复前重启 30 分钟即出现 9 分钟长事务 + 25 active 全锁等待）；WS screen 60s probe 12 条全推、0 中断；sendScreen failed 0；端到端全 PASS。

## 五、残留风险

- **已在 8/2 深夜根治**：连接池泄漏（原 P1）已定位到 `handleDetectData` 长事务并拆事务解决，实测 `long_txn=0 / lock-wait=0 / 0 中断`。需跨夜观察确认长期稳定（已录 memory）。
- **P2 同秒 sendScreen 偶发瞬时失败**：拆分后每笔连接用完即还，但 38 线同时上传的瞬时峰值仍可能短暂触发 `connection-timeout`（当前 `10s`）——sendScreen 下轮自愈（WS 实测 0 中断），页面不无数据。如需进一步缓冲可上调 `maximum-pool-size`。
- P3：verify 脚本 WS 断言已改为「无『連接斷開』+ 有数值」（WS 正常无文案，仅断线显示标记）。

## 六、交付物

- 后端：`ScreenServiceImpl.java`（null-safe 排序）
- 前端：`stores/screen.ts`、`OccupancyPanoramaBar.vue`、`RealTime.vue`
- 验证脚本：`DataupLoad-web/verify-w-flash-01.mjs`
- 截图：`docs/work-orders/W-FLASH-01/w-flash-01-realtime.png`

**下一步**：老板浏览器实测（见 HEARTBEAT 步骤），实测通过后 commit + push。
