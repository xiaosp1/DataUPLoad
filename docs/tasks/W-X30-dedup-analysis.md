# W-X30 工单 — DataupLoad 报警推送频率过高（180条/10min）根因分析

**派工人**：PM 锋卫
**分析时间**：2026-07-24 10:51
**分析人**：subagent（codex exec）
**优先级**：🔴 P0（老板口径：报警不能疯狂推送）
**结论一句话**：DataupLoad 报警链路相对 PSM 缺失 3 道去重关卡（`alarm.interval` 字段定义了但没用到、`DealAlarmEvent` 监听器未接入、Controller 重复触发 yk push），导致每条报警都会被推送 yk **两次** + 短时间内相同未处理报警无法合并 + 客户端重连收不到事件清理旧的 UNSOLVED。

---

## 1. DataupLoad 当前 `add()` / `sendAlarmMessage()` 逻辑（已读源码确认）

文件：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/AlarmRecordServiceImpl.java`

### `add(AlarmDTO form)` 关键路径
1. 判 `alarm.global-enabled`（W-X21 全局开关，true 继续）→ false 直接 return。
2. `AlarmTypeEnum.getByCode(form.getType())` 找报警类型（DEFECT=1 / SYSTEM=2 / DEVICE=3）。
3. 从 `defect_type` 表查 `(type, category)` 命中的缺陷字典 → `sortDefectTypeByName`。
4. 对每个 `alarmConfig.config[]`，若 `config.type == alarmType`，用 `template` 正则从 `form.message` 抽 defectName。
5. 命中 defectName → `isInterestingDefect=true`：
   - **去重旧报警**（line 145-156）：
     ```sql
     UPDATE alarm_record SET solve=IGNORE
     WHERE defect_name=? AND line_no=? AND type=? AND face_no=? AND solve=UNSOLVED
     ```
   - 插入新 UNSOLVED 记录
   - `sendAlarmMessage(alarm)` → 内部判断 defectType.alarmEnable / soundEnable / sendYkEnable → 命中 yk 分支 → `EventUtil.publish(PushAlarmEvent)`

### `sendAlarmMessage(AlarmRecord alarm)` 关键路径
- `isIgnore = ignoreAlarmService.isIgnore(type, defectName, lineNo, faceNo)`（**W-B04 修复点**：PSM 原版硬编码 `false`，DataupLoad 改为真实查询 ignore_alarm 白名单）。
- 若 `defectType.alarmEnable=YES && !isIgnore` → `sendAlarmTextMessage()`（WS 全量广播）+ 判 soundEnable 推 WS 声音（DataupLoad 当前未启用 system_config，跳过）。
- 若 `!isIgnore && defectType.sendYkEnable=YES` → `EventUtil.publish(PushAlarmEvent)`。
- **`alarmInterval` 字段**：声明了 `@Value("${alarm.interval:60}")` 但**全代码 grep 没有任何使用**，形同摆设。

---

## 2. PSM 反编译 `add()` / `sendAlarmMessage()` 逻辑（`psm-decompiled/.../alarm/service/imp/AlarmRecordServiceImpl.java`）

PSM `add()` 去重逻辑和 DataupLoad **完全一致**：同 (defectName+lineNo+faceNo+type+UNSOLVED) 老记录置 IGNORE → save 新 UNSOLVED → `sendAlarmMessage`。

**PSM 的 `sendAlarmMessage` 也有 `boolean isIgnore = false;` 硬编码 bug**（W-B04 已修复）—— 这点两边一致，不影响推送频率。

### PSM 多出来的去重/收尾链路

| 组件 | 文件 | 作用 |
|---|---|---|
| **`@EventListener(DealAlarmEvent.class) dealClientAlarmListener`** | `AlarmRecordServiceImpl.java` | 监听客户端重连事件，自动清理旧的 UNSOLVED |
| **`dealClientAlarm(lineNo, faceNo, reason)`** | `AlarmRecordServiceImpl.java` | 真正去重逻辑：按 (lineNo, faceNo, **reason**) 查所有 UNSOLVED，**保留第一条**，其余置 SOLVED，最后对第一条 `deal()` → 推解决消息 |
| **`StatusRecordServiceImpl.receiveStatus`** | `detect/service/imp/StatusRecordServiceImpl.java` line 56-59 | 客户端断线/上线时发 `DealAlarmEvent`：`EventUtil.publish(new DealAlarmEvent(this).setLineNo(...).setFaceNo(...).setReason(AlarmReasonEnum.DISCONNECT.getValue()))` |
| **`WsConnectListener`** | `alarm/event/WsConnectListener.java` | 客户端 WS 连上时调 `sendAlarmTextMessage()`（一次 WS 全量广播） |

### PSM `add()` 内部去重关键细节（PSM vs DataupLoad 差异）

PSM 的 `dealClientAlarm` 是**核心防爆机制**：客户端一旦重连，会自动：
1. 把同一产线/工位/REASON 的所有未处理老报警里，**第一条保留**，**其余置 SOLVED**；
2. 对保留的那一条调 `deal()` → 推 SOLVED 状态到 yk / WS；
3. PSM `add()` 内部的老 UNSOLVED 置 IGNORE 也照常工作。

结果：PSM 客户端重连瞬间，**自动清理未确认告警的"积压"**，老板看到的 PSM 推送频率自然低。

### PSM `alarm.interval` 字段
PSM 也声明了 `@Value("${alarm.interval:60}")` 但同样**没有任何代码使用**——和 DataupLoad 一样是占位符。也就是说 PSM 原版就没实现"60秒内同缺陷只推一次"的时间窗口去重 —— 这个去重是**通过 `dealClientAlarm` 在客户端重连时清理**实现的，而不是代码里查时间窗口。

---

## 3. DataupLoad 缺失的去重机制（3 道关卡）

### 🚨 关卡 1：Controller 双重触发 yk push（最严重）

文件：`DataupLoad/.../alarm/web/AlarmRecordController.java`

```java
@PostMapping("/client/data/alarm")
public BaseResult addAlarmData(@Validated @RequestBody AlarmDTO alarmDTO) {
   log.info("receive alarm: {}", alarmDTO);
   BaseResult result = this.alarmRecordService.add(alarmDTO);   // 内含 sendAlarmMessage → publish PushAlarmEvent → yk push #1
   try {
      AlarmRecord record = new AlarmRecord();
      record.setUuid(...).setTime(...).setType(...).setLineNo(...).setFaceNo(...).setLevel(...).setMessage(...);
      this.ykService.pushAlarm(record);   // ← 同步入口，发布 PushAlarmEvent → yk push #2（重复推送！）
   } catch (Exception ex) {
      log.warn("yk push trigger failed (best-effort). cause: {}", ex.toString());
   }
   return result;
}
```

**对比 PSM 同名方法**：PSM `AlarmRecordController.addAlarmData` **只调** `alarmRecordService.add(alarmDTO)` 然后 return，**没有额外的 `ykService.pushAlarm()` 调用**。PSM 的 yk 推送完全靠 `add()` 内部 `sendAlarmMessage` → `EventUtil.publish(PushAlarmEvent)` 这一个事件触发。

**影响**：DataupLoad 每条报警会被发布 `PushAlarmEvent` **两次**（`sendAlarmMessage` 一次 + Controller 同步 push 一次）。`@EventListener(PushAlarmEvent.class) pushAlarm2YK` 是 `@Async` 但只要 `uploadEnabled=true`，两次都会进 MES → 飞书告警翻倍。

**且**：controller 直接 push 时**绕过了 `sendAlarmMessage` 里的 `defectType.sendYkEnable` 检查**（连 sendYkEnable=NO 的缺陷类型也会被强推）。

### 🚨 关卡 2：`DealAlarmEvent` 监听器缺失 + StatusRecord 不发布事件

DataupLoad `StatusRecordServiceImpl.receiveStatus`（`detect/service/impl/StatusRecordServiceImpl.java`）只做了按 `(lineNo, faceNo, deviceNo)` upsert，**完全没有发布 `DealAlarmEvent`**。

DataupLoad `AlarmRecordServiceImpl.dealClientAlarm` 虽然存在，但**没有任何 `@EventListener(DealAlarmEvent.class)` 监听器**（注释里说"DataupLoad 当前未引入 DealAlarmEvent 事件源，因此方法暂未被调用"），是死代码。

**影响**：客户端断线重连后，旧的 UNSOLVED 报警**永远不会自动清理**。每一次 `add()` 的去重 WHERE 是 `(defectName+lineNo+faceNo+type+solve=UNSOLVED)`，只要 PG 里同组合还有 UNSOLVED 的旧记录，新的就会被 IGNORE；但**如果客户端离线期间**同一个产线/工位出现了不同 defectName 的告警，老的 UNSOLVED 不会被 `dealClientAlarmListener` 收尾，每个 defectName 都独立堆积 UNSOLVED → yk 推 N 条。

**注**：PSM `dealClientAlarm` 的去重键是 `(lineNo, faceNo, reason=1=客户端掉线)`，是专门给"客户端掉线"reason 的，**不是给缺陷告警的**。但客户端重连时它会把掉线相关的旧 UNSOLVED 一并处理，避免堆积。

### 关卡 3：`alarm.interval` 时间窗口去重未实现

两边的 `alarm.interval` 字段都是占位（无消费代码）。DataupLoad 没有比 PSM 少这个（**PSM 也没实现**），所以这不是差异。但如果生产中确实出现同一 defectName 在短时间内重复 push（因为客户端/相机心跳异常反复触发同一条 `add()`），可以考虑**新加**这个时间窗口去重，作为"客户端重连清理"之外的第二道保险（详见 §5 建议 C）。

---

## 4. 180条/10min 的可能成因（按概率排序）

| 概率 | 成因 | 证据 |
|---|---|---|
| **极高** | Controller 双重触发 yk push（关卡 1）| DataupLoad AlarmRecordController.java 第 32-40 行 vs PSM 同名方法 1:1 对比 |
| 高 | `DealAlarmEvent` 缺失导致断线重连不清理（关卡 2）| DataupLoad StatusRecordServiceImpl 全文无 publish(DealAlarmEvent) + AlarmRecordServiceImpl 无 @EventListener |
| 中 | `add()` 只对"interesting defect"做 IGNORE 旧 UNSOLVED 的去重；如果 PG 里某组合没有 UNSOLVED（旧记录被 IGNORE 后），下一次新告警会**重新建一条 UNSOLVED** + 推送（去重键只命中"当前还活着的未处理"）| 源码 line 145-156 解读 |
| 中 | 报警频率本身高（38 相机 × 每相机 N 件/h × 未脱模缺陷率），与 PSM 同等流量但 PSM 因为监听器收尾推送数被压低 | 业务侧猜测（需 log 验证） |
| 低 | `isIgnore` 查询慢导致事件积压，超出 MES 接收能力 → 重传 | 暂未发现代码瓶颈；PushAlarmEvent 是同步 publish，`@Async` 消费 |

### 数据量级参考

老板说 PSM 推送远低于 180条/10min（1800条/h）。如果**双重推送**是主因，那么实际"真实报警"大约 90条/10min = 900条/h —— 这个数量和 38 相机生产节奏（单产线 1 件/分钟 × N 条产线 × 缺陷率 5-10%）依然偏高，需要进一步按关卡 2 收尾。**不能简单乘以 2 就当修好**。

---

## 5. 修复建议（按优先级 + 改动量）

### A. 删除 Controller 重复 push（必做，5 分钟，1 行代码）

文件：`DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/web/AlarmRecordController.java`

```java
@PostMapping("/client/data/alarm")
public BaseResult addAlarmData(@Validated @RequestBody AlarmDTO alarmDTO) {
   log.info("receive alarm: {}", alarmDTO);
   // 修复 W-X30：删除 controller 层重复 yk push。
   // yk 推送由 AlarmRecordServiceImpl.add() → sendAlarmMessage() 内 EventUtil.publish(PushAlarmEvent) 触发。
   // 这里再调一次会导致同一条报警被推两次，且绕过 sendYkEnable 检查。
   return this.alarmRecordService.add(alarmDTO);
}
```

⚠️ **同时检查是否有别的代码路径依赖 controller 的 push 行为**：
- grep `ykService.pushAlarm(` 全工程，确认没有第二个调用点（目前看到的就是 controller 这一个）。
- grep `AlarmRecordServiceImpl.add(` 调用方，确认 `addAlarmData` 是唯一入口（web 后台的 `ignore` / `search` / `deal` 等都不应该再调 `pushAlarm`）。

### B. 接入 `DealAlarmEvent` 监听器（强烈建议，30 分钟，1 个新文件 + 几行改动）

**B1**：新建 `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/event/DealAlarmEvent.java`（1:1 抄 PSM）。
**B2**：`AlarmRecordServiceImpl` 增加 `@EventListener(DealAlarmEvent.class) dealClientAlarmListener` 方法（已有 dealClientAlarm 实现，但缺事件源）。

⚠️ **修复 `dealClientAlarm` 的 key 错误**：当前 DataupLoad 的实现是按 `(lineNo, faceNo, type)` 查，**PSM 是按 `(lineNo, faceNo, reason)` 查**。要改成 PSM 同款：

```java
public void dealClientAlarm(String lineNo, String faceNo, Integer reason) {
   LambdaQueryWrapper<AlarmRecord> qw = Wrappers.<AlarmRecord>lambdaQuery()
      .eq(AlarmRecord::getLineNo, lineNo)
      .eq(AlarmRecord::getFaceNo, faceNo)
      .eq(AlarmRecord::getReason, reason)   // ← 改 type→reason（PSM 同款）
      .eq(AlarmRecord::getSolve, AlarmSolvedEnum.UNSOLVED.getValue());
   ...
}
```

**B3**：在 `StatusRecordServiceImpl.receiveStatus` 检测到 `clientState.status == OUTLINE` 变更时（PSM line 56-59），发布事件：

```java
if (Objects.equals(clientState.getStatus(), DeviceStatus.OUTLINE.getValue())) {
   EventUtil.publish(new DealAlarmEvent(this)
      .setLineNo(lineNo)
      .setFaceNo(faceNo)
      .setReason(AlarmReasonEnum.DISCONNECT.getValue()));
}
```

⚠️ DataupLoad 当前 `StatusRecordServiceImpl.receiveStatus` 是简化版（不区分上线/下线、不读 `clientNo`），需要先升级到 PSM 同款才能挂上事件发布。**改动量中等**，建议作为独立工单（建议工单号 W-X30b）。

### C. 新增"短时间窗口同缺陷去重"（建议，可选）

DataupLoad 当前 `alarm.interval` 字段空挂。可以在 `sendAlarmMessage` / `add()` 里加一个轻量去重：

```java
// add() 里 save 新 UNSOLVED 之前，判短时间窗口是否已存在
LambdaQueryWrapper<AlarmRecord> recent = Wrappers.<AlarmRecord>lambdaQuery()
   .eq(AlarmRecord::getDefectName, defectName)
   .eq(AlarmRecord::getLineNo, form.getLineNo())
   .eq(AlarmRecord::getFaceNo, form.getFaceNo())
   .eq(AlarmRecord::getType, form.getType())
   .gt(AlarmRecord::getCreateTime, LocalDateTime.now().minusSeconds(alarmInterval))
   .last("LIMIT 1");
if (this.count(recent) > 0) {
   log.info("alarm suppressed by interval={}s.[defect={}][line={}][face={}]",
      alarmInterval, defectName, form.getLineNo(), form.getFaceNo());
   return BaseResult.build().ok();
}
```

⚠️ 这个改动需要评估副作用：
- 优点：彻底压制"相机/客户端 bug 导致 1 分钟内推 180 条"的场景。
- 缺点：`alarm.interval=60` 会让老板看不到"1 分钟内第 2 件同样缺陷"（PSM 原版也没实现这个，老板对 PSM 现状认可，说明这个去重必要性不高）。
- **建议优先级 P1，工单独立派**，等 A+B 落地 + 老板确认仍有疯狂推送时再做。

### D. 加监控告警（强烈建议，30 分钟）

无论 A/B/C 哪一道修复，都建议加监控，否则下次复发老板还是发现不了：
- PG 统计：`SELECT count(*) FROM alarm_record WHERE create_time > now() - interval '10 min' AND solve = 2`（UNSOLVED 入库速率）。
- log 统计：grep `publish PushAlarmEvent` 出现频次；> N 条/分钟自动告警。
- yk push ERROR 数：`grep "push alarm info to yk failed" DataupLoad.log`（目前 W-X13d 灰盒期 uploadEnabled=false 时不会触发）。

---

## 6. 修复后的预期效果

假设业务侧"真实报警"频率 = 90条/10min（A 修复后会回到 PSM 同等的单次推送）：
- 修复 A（去掉 controller 双推）：推送量减半到 90条/10min。
- 修复 B（接 DealAlarmEvent）：客户端重连会清理 UNSOLVED 堆积，避免告警风暴。具体降幅取决于产线重连频率，预期 90 → 30-60 条/10min。
- 修复 C（时间窗口去重）：对相机/客户端 bug 反复触发场景硬保护，预期降到 PSM 同款水平（<10条/10min）。

---

## 7. 给 PM 的下一步派工建议

1. **W-X30a**（P0，5 分钟）：删除 controller 重复 yk push（建议 A）。无新文件，1 处删除。
2. **W-X30b**（P0，30 分钟）：接入 DealAlarmEvent 监听器 + 修正 dealClientAlarm key（建议 B）。
3. **W-X30c**（P1，30 分钟）：加监控告警（建议 D）。
4. **W-X30d**（P2，60 分钟，可选）：实现 `alarm.interval` 时间窗口去重（建议 C）。
5. **回归**：重跑 W-X15 灰盒测试 8 项 + 老板实拍监控确认 10min 内推送量 < 30 条。

---

## 附录：源码对照表（已 grep 验证）

| 维度 | PSM 反编译 | DataupLoad 当前 | 差异 |
|---|---|---|---|
| `add()` 去重 key | `(defectName+lineNo+faceNo+type+solve=UNSOLVED)` → IGNORE | 同 PSM | ✅ 一致 |
| `sendAlarmMessage()` 调用 | `add()` 内调用 | 同 PSM | ✅ 一致 |
| `isIgnore` 查询 | 硬编码 `false`（BUG）| 真实查 ignore_alarm（W-B04 修复） | ✅ DataupLoad 更好 |
| `@EventListener(DealAlarmEvent.class)` | ✅ 有 | ❌ 无监听器 | ❌ DataupLoad 缺失 |
| `dealClientAlarm` key | `(lineNo, faceNo, reason)` | `(lineNo, faceNo, type)` | ❌ DataupLoad 改坏 |
| `StatusRecordServiceImpl` 发 DealAlarmEvent | ✅ 客户端上线时发 | ❌ 不发 | ❌ DataupLoad 缺失 |
| `alarm.interval` 字段使用 | ❌ 占位未用 | ❌ 占位未用 | ✅ 一致（都没实现） |
| Controller yk push 次数 | 1 次（仅 `add()`）| 2 次（`add()` + controller 直接 push） | ❌ DataupLoad 双推 |
| WS 全量广播触发 | 客户端连上 + sendAlarmMessage | 客户端连上未实现 + sendAlarmMessage | ⚠️ DataupLoad 缺 WsConnectListener |
| alarm.global-enabled 全局开关 | ❌ 无 | ✅ 有（W-X21） | ✅ DataupLoad 更好 |
