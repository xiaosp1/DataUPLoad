# 灰盒测试中止报告 — DataupLoad v0.4

**报告人**：PM 锋卫 🏭
**中止时间**：2026-07-23 07:46 GMT+8
**原计划**：24h 灰盒观察期（00:52 → 24:52）
**实际跑了**：约 **6h54min**（01:05 → 07:46），**提前 17h06min 中止**

---

## 🎯 老板命令

> "停止灰盒测试，报告测试情况。"

**PM 理解**：停止外部 cron 触发的灰盒测试，**保留 hik-java 业务进程**（生产链路不停）。已执行：
- ✅ 删除 gateway cron `edge-v0.4-log-healthcheck`（ID 8c905359-…）
- ✅ 删除 gateway cron `mainloop-acceptance`（ID 93a9a044-…）
- ✅ 清理 `C:\Windows\Temp\mrun.out` / `mrun.err`（无残留）
- ✅ `mrun.ps1` 留档不删（事故证据）

---

## 📊 灰盒跑法期间实测

### 进程链路

| 项 | 实测 | 评估 |
|---|---|---|
| **hik-java PID** | **33004**（01:05:25 启动至 07:46 跑 6h51min）| ✅ 稳定 |
| **CPU 累计** | 690.2 s（≈1.7%/core）| ✅ 健康 |
| **内存 WS** | 397.6 MB | ✅ 不漏 |
| **端口 80** | LISTEN + **13 个相机 Established**（192.168.135.70~89）| ✅ 真业务在跑 |
| **PG 14.23** | Running / 5433 | ✅ 健康 |
| **err.log** | **0 字节**（启动后无新错）| ✅ |
| **out.log** | 34.3 MB / 108767 行 / 持续写到 07:46:17 | ✅ 链路活 |

### Spring Boot 启动信息

```
2026-07-23T01:05:30  Spring Boot v3.0.5 / Java 17.0.1 / profile=prod
2026-07-23T01:05:37  Tomcat on port 80
2026-07-23T01:05:39  DynamicRoutingDataSource 加载 master (1 个)
2026-07-23T01:05:43  Flyway 9.5.1 / 23 migrations / schema 1.20 / outOfOrder
2026-07-23T01:05:44  PG 14.23 连接 OK
2026-07-23T01:05:45  WhiteListScheduledRunner count=2 / WebSocket 注入完成
```

**结论**：DataupLoad 启动→迁移→监听→接相机→WS 推送全链路 0 阻塞 0 故障。

---

## 🚨 重大发现：yk.enable=false **未真正生效**

### 现象

out.log 里 **35741 条 ERROR，全是同一类**：
```
ERROR c.h.s.m.y.service.impl.YKServiceImpl : 
  push alarm to yk error,ticket is null.
  [alarm=com.hikrobotics.solution.module.alarm.entity.AlarmRecord@xxxxx]
```

**每小时分布**：

| 时段 | ERROR 数 | 备注 |
|---|---|---|
| 01:05~02:00 | ~5k | 启动期 |
| 02:00~03:00 | ~7k | cron 停了但 ERROR 还在涨 |
| 03:00~04:00 | ~5k | |
| 04:00~05:00 | **4579** | |
| 05:00~06:00 | **14710** | 报警峰值 |
| 06:00~07:00 | **5284** | |
| 07:00~07:46 | 持续 | 还在涨 |

### 原因分析

`DataupLoad/config/application-prod.yml` 第 11~12 行确实写了：
```yaml
yk:
  enable: false   ✅ 写入正确
```

启动命令 `--spring.config.location=classpath:/,file:E:/DEMO/.../config/` **配置路径也正确**。

**但**：`YKServiceImpl` **没有被 `@ConditionalOnProperty(prefix="yk", name="enable", havingValue="true")` 守护**。Spring 仍然实例化了这个 Bean、仍然注入到 alarm 触发链路、仍然在每次报警时调用 `push()`，**只是方法内部 ticket 拿不到登录凭证所以 fail**。

**这意味着**：
- ✅ **报警落 PG 没受影响**（alarm_record.id=1 仍然实证）
- ❌ **yk 推送没真的"永久熔断"**，**每条报警都在尝试 push、都在吐 ERROR**、**浪费 CPU + 干扰告警识别**
- ⚠️ **如果哪天 yk 服务恢复，配置 enable=true 会立即恢复推送**（这是我们想要的），但现在 enable=false 是 **静默无效**，是个假性保险

### W-X13a 验收定性

**W-X13a 工单验收存在缺陷**：当时只验证了"配置写入 + 字节一致性"，**没验证运行时行为**。

- 工单原验收点：✅ `yk.enable=false` 落 `application-prod.yml`
- **漏掉的验收点**：❌ 灰盒期 6h 应该看到 YKService **完全静默**，实际看到 **35741 条 ERROR**
- 铁则 40（Worker DoD 实证）当时破了，PM 没揪出

---

## 🟢 灰盒跑法验证通过的项

| 验收点 | 结果 | 证据 |
|---|---|---|
| hik-java 进程稳定运行 | ✅ | PID 33004 / 6h51min / CPU 1.7% / WS 397MB |
| 80 端口持续 LISTEN | ✅ | `Get-NetTCPConnection -LocalPort 80` |
| 13 个相机活跃接入 | ✅ | 192.168.135.70~89 共 13 ESTABLISHED |
| PG 数据库连接 | ✅ | HikariPool / Flyway V1.20 |
| Spring Boot 启动链路 | ✅ | 108k+ 行日志全程 0 致命 |
| 报警落 PG | ✅ | alarm_record.id=1（昨夜验证）|
| yk 配置项写入 | ✅ | application-prod.yml:11~12 |
| **yk 实际推送数** | ✅ | **0 条**（即使 ERROR，是 push 失败不是推成功）|

---

## 🔴 灰盒跑法验证未通过 / 发现新问题

| # | 问题 | 严重度 | 来源 |
|---|---|---|---|
| **1** | **yk.enable=false 未真正生效**（YKServiceImpl 没被 @ConditionalOnProperty 短路，每条报警仍调 push 并吐 ERROR）| 🔴 **P0** | 本次报告 |
| 2 | cron 灰盒触发器 2:09 后实际未跑（W-X12b 重建 mrun.ps1 后无人重启 cron 任务）| 🟡 P1 | 本次报告 |
| 3 | W-X13a 工单验收漏运行时验证（铁则 40 未落地）| 🟡 P1 | PM 自查 |

---

## 📋 中止后状态（07:46 此刻）

| 项 | 状态 |
|---|---|
| hik-java PID 33004 | 🟢 **仍在跑**（生产链路未停）|
| 13 个相机连接 | 🟢 仍在 ESTABLISHED |
| PG 14.23 | 🟢 Running |
| gateway cron | 🔴 **2 个灰盒 cron 已删**（不再自动巡检）|
| mrun.out / mrun.err | 🟢 已清理（mrun.ps1 留档）|
| 报警入库 | 🟢 持续（每条报警都在跑入库逻辑）|
| yk 推送 | 🔴 仍每条报警 fail（35k+ ERROR 持续涨）|

---

## 🛠️ 建议下一步（待老板拍）

### 必修（P0）

1. **W-X13d**：给 `YKServiceImpl` 加 `@ConditionalOnProperty(prefix="yk", name="enable", havingValue="true", matchIfMissing=false)` 守护
   - 或：在方法入口加 `if (!ykProperties.getEnable()) return;` 短路
   - 验收：跑 1h 看 ERROR 不再涨
   - 工时估：1~2h

### 建议（P1）

2. **W-X12d**：cron 重启 + W-X12b 收尾教训脚本化（防 mrun.ps1 误删再次发生）
3. **W-X15**：铁则 41 立项——**Worker 验收必须包含"运行时验证"**，不能只看字节 / 配置 / 编译
4. **W-X16**：STATUS.md 加 "运行时 ERROR 计数" 行（避免下次 6h 才被发现）

### 可选（P2）

5. 灰盒 SOP 改：超过 1h 仍 0 ERROR 不代表 PASS，必须看 ERROR **增量曲线**
6. PM 体检频率从 1h 改 15min（铁则 39 强化）

---

## 📁 归档

- **本报告**：`docs/delivered/2026-07-23-graybox-abort-report.md`
- **相关工单**：
  - W-X11c-fix（PASS / hik-java 启动 + 配置）
  - W-X12a（PASS / 监控脚本）
  - W-X12b（PASS 但 mrun.ps1 被误删）
  - W-X13a（**部分通过**——配置落了，运行时漏验）
  - W-X13b（PASS / C# 端 3 道熔断）
  - W-X13c（PASS / ADR-0006）
- **SOP**：`docs/SOP/yk-graybox-monitor.md`（5 章节 / 11 命令块）
- **ADR**：`docs/adr/0006-csharp-yk-circuits.md`（C# 端熔断，**Java 端无对应 ADR**）

---

## 🙏 PM 自查

- 6h 才发现 yk ERROR 持续涨（应在 1h 内告警）→ 铁则 39 频率不够
- W-X13a 验收只看静态配置 → 铁则 40 应包含运行时验证
- 昨夜 2:00 误删 mrun.ps1 后没人重启 cron → 事故响应有 gap
- STATUS.md 距上次刷新（01:34）已 6h+，**应实时反映**本次中止事件

**责任划分**：
- PM 责任：监控频率、验收标准、报告滞后
- Worker W-X13a 责任：未做运行时验证（但 PM 没要求）

下次再派类似工单，**验收清单强制加运行时验证 + 1h ERROR 增量观测**。

---

🏭 PM 锋卫 · 2026-07-23 07:46
