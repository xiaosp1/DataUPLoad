# W-C05 — 从 PSM 抄白名单到 DataupLoad（🟡 P1 / 抄表）

**派工人**：PM 锋卫 🏭
**派工时间**：2026-07-23 16:45
**优先级**：🟡 P1（老板 16:41 指令："白名单你得去抄 PSM 的"）
**基于**：W-X15b 修完 isIgnore + W-A21 PSM 反编译全报告 + W-A18 PSM 1:1 工单

---

## 🎯 任务目标

**把 PSM 端的 ignore_alarm 白名单数据（终态）抄到 DataupLoad 的 ignore_alarm 表**，作为 W-X22 1h 灰盒实测的真实基线。

---

## 📋 任务清单

### 1. 找 PSM 端 ignore_alarm 数据来源（不依赖 PSM 运行）

**PSM 端运行地址未知**（没部署在本机），但**反编译文档已有**：

- `E:\DEMO\数据采集\docs\delivered\2026-07-22-psm-alarm-detailed.md`（18588 bytes）
- `E:\DEMO\数据采集\docs\delivered\W-A18-alarm-psm-1to1.md`（15498 bytes）
- `E:\DEMO\数据采集\docs\delivered\W-A20-psm-reverse.md`（5189 bytes）
- `E:\DEMO\数据采集\docs\delivered\W-A21-psm-reverse-engineering-full.md`（7111 bytes）

读这些文件，**提取 PSM 默认 ignore_alarm 白名单规则**——通常是：
- 全 (line, face, all) 屏蔽某个 defectName
- 全 (line, all) 屏蔽
- 全 (face, all) 屏蔽
- 单条带过期时间的临时白名单

### 2. 形成 PSM 默认白名单 SQL（INSERT 脚本）

写一个 `docs/tasks/W-C05-psm-whitelist-seed.sql`，包含：
- 全屏蔽：defectName=`<典型噪声>`，type=1/2/3，line_no='*' / face_no='*' / ignore_all=1
- 部分屏蔽：device 类 type=3 全屏蔽
- end_time 用 '2099-12-31 23:59:59'（永不过期）

> **不要拍脑袋造数据**——必须从 4 份 PSM 文档里**实际抠出真实规则**。如果文档里没有 PSM 默认白名单，**Worker 必须报告 PM**，不能瞎造。

### 3. 把白名单写入 DataupLoad ignore_alarm 表

**目标 PG**：`127.0.0.1:5433/intco`（当前生产库）

**步骤**：
1. INSERT 之前必须先备份现有数据：
   ```sql
   CREATE TABLE ignore_alarm_backup_20260723 AS SELECT * FROM ignore_alarm;
   ```
2. INSERT PSM 白名单
3. 验证总数：
   ```sql
   SELECT COUNT(*) FROM ignore_alarm;
   ```
4. **保留 W-X15b 留下的 W-X15-restore id=37**——必须 in-place update 或先保留

### 4. 不要碰代码

- ❌ 不改 IgnoreAlarmServiceImpl
- ❌ 不改 AlarmRecordServiceImpl
- ❌ 不改 yml
- ❌ 不重启 hik-java

### 5. 写报告 `docs/delivered/2026-07-23-W-C05-psm-whitelist-seed-result.md`

包含：
- PSM 默认白名单规则来源（具体引用了哪份文档的哪段）
- 写入条数 + 备份条数
- ignore_alarm 当前 SELECT 全表
- 验证脚本（可重复执行）

---

## 🚫 严禁

- ❌ 重启 hik-java
- ❌ 拍脑袋造白名单（必须从 PSM 文档里抠真实规则）
- ❌ 删除 W-X15-restore 痕迹
- ❌ 改任何业务代码

---

## 🎯 PM 验收标准

1. ✅ 4 份 PSM 文档实际被引用（报告里贴出引用段）
2. ✅ 备份表 `ignore_alarm_backup_20260723` 存在
3. ✅ INSERT 至少 3 条 PSM 默认白名单
4. ✅ W-X15-restore id=37 仍在
5. ✅ report 引用了 PSM 文档原文 + 列出所有 INSERT 条目
6. ✅ ignore_alarm 总数 ≥ 4

完成在群内回复。
