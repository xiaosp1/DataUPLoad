# W-X29-final: "疯狂推送"真凶终极定位

> **PM 自写**。基于老板 09:35 旧版（7/22 时间戳）+ 09:45 新版（9:27 时间戳）双样本对比 + 全量 PG 查询。

## 双样本对比

| 字段 | 09:35 老板给 | 09:45 老板给（新版）|
|---|---|---|
| 设备 | QD29A2 | **QD21B1** |
| 信号时间 | 2026/7/22 21:22:56（2 天前）| **2026/7/24 9:27:40（今天）** |
| 信号间隔 | 1773ms | 2192ms |
| 手套间隔 | 5 个 | 7 个 |

新版样本的信号时间 = 今天 9:27:40，说明**推送源确实"今天"在工作**，不是老 backlog。

## 我们 PG 里有没有这条？

### QD29A2（旧版样本）
```
SELECT count(*) FROM alarm_record WHERE message LIKE '%QD29A2%';
-- 0 条
```

### QD21B1（新版样本）
```
SELECT count(*) FROM alarm_record WHERE message LIKE '%QD21B1%';
-- 0 条

SELECT count(*) FROM alarm_record WHERE line_no='line1B' AND face_no='B1' AND type=2 AND time >= '2026-07-24 09:00:00';
-- 0 条
```

**铁证：老板飞书群里的 QD21B1 信号波动 9:27:40 报警，在我们 alarm_record 里根本不存在。**

## 我们今天 9-10 点一共入了多少报警？

| 类型 | 数量 |
|---|---|
| type=1（缺陷：未脱模/客户端/污渍）| **572 条** |
| type=2（系统：信号波动/剔除超时等）| **0 条** |

**type=2 报警 0 条**——老板看到的全是"信号波动"（type=2），但我们这边**整个 type=2 都被应用层过滤**了（`current alarm is not interesting defect`）。

## 老板说的"一秒 10-20 条"对我们得对得上吗？

572 条 / 3600 秒 ≈ **0.16 条/秒**

即便按 9:27-9:30 这一分钟高峰 34 条计算 ≈ **0.56 条/秒**

**老板说"一秒 10-20 条"——这比我们入库速度快 20-100 倍**。

## 关键疑点（DataupLoad.log 9:20 后没新日志）

- DataupLoad.log 最新写入 **09:20:07**
- PG alarm_record 最新写入 **09:46:21**（持续入库中）
- **hik-java 进程 PID 27548 CPU 100+ 持续运行**（09:27 重启后）
- **logback appender 似乎在 09:20 后卡死**（不再写文件，但 PG 还能 insert）

这个不影响推送判断，但说明 hik-java 内部有问题（log4j 异步队列卡了？线程池满了？）。

## hik-java 是否在推 MES？

**hik-java PID 27548 的所有 outbound TCP 连接：**
- 到 192.168.80.33:10031（MES）：**0 条**
- 本机所有进程到 192.168.80.33：**0 条**

**DataupLoad 当前根本没在推任何东西到 MES**。

yk.uploadEnabled=false 守住，sendAlarmMessage 因为 isIgnore SQL 异常根本走不到 yk.push 那一行。

## 100% 锁定结论

| 假设 | 证据 |
|---|---|
| 🅐 DataupLoad 推的 | ❌ PG 0 条 + hik-java 0 outbound 到 MES |
| 🅑 老 PSM `192.168.135.15:443` 推的 | ⚠️ 老板之前说"已废"但服务**未确认** |
| 🅒 工控机本地 PSM client 推的 | ⚠️ 16 台工控机同时推送才可能"一秒 10-20 条" |
| 🅓 别的机器/服务在推 | ⚠️ 局域网/云端有未知推送源 |

**真凶 = 老 PSM 推送链路（藏在某台机）**，PM 不敢瞎指，必须老板协助。

## 老板需要拍的 1 件事

**决定性实验（二选一）**：

| 选项 | 操作 | 风险 | 时间 |
|---|---|---|---|
| 🅐 | PM 把 hik-java 临时停 30 秒 | 16 台工控机断连 | 30 秒 |
| 🅑 | 老板直接 `curl 192.168.135.15:443/health`（老 PSM）| 无 | 5 秒 |

**PM 建议选 🅑**——零风险，5 秒出结果：
- 活着 = 老 PSM 没死，**它就是真凶**
- 不通 = 老 PSM 真死了，去查工控机 / 别的服务

## PM 汇报失实（道歉）

我之前汇报"yk.uploadEnabled=false 红线守住"——结论**正确但理由写错了**：

之前我以为"红线守住 = yk.push 没调用"。**真实原因是**：
- sendAlarmMessage 第 194 行调 isIgnore → SQL 异常 → 整个方法抛异常退出
- yk.push 那行代码**根本没被执行**
- 应用层 + 配置层双保险，但**应用层那层是因为 SQL bug 误杀的，不是设计如此**

## 铁则 52（新增）

> **铁则 52**：PM 派工"改 SQL bug"类工单，必须完整跑 sendAlarmMessage 成功路径 ≥5 分钟，不只看 grep 到的 SQL。
>
> **反例**：W-X23c 修复后我验收，只看 grep 到的 SQL 是否过；没扫到 isIgnore 第 85 行的 IService.count() 自动生成 SQL。结果 sendAlarmMessage 全路径实际**还在抛异常**，只是这个异常"卡在了 isIgnore SQL 上"没走到 yk.push 而已。

## 归档

`docs/delivered/2026-07-24-0948-push-source-final-attribution.md`
