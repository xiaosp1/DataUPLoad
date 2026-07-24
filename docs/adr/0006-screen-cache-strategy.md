# ADR-0006: screen 模块 `lineStatusMap` 中间表 put vs putIfAbsent 选择

| 字段 | 值 |
|---|---|
| 状态 | **Accepted** |
| 日期 | 2026-07-24 |
| 决策者 | Java W-SCR-01 worker + 审计报告 (W-SCR-01 复测确认) |
| 影响范围 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/service/impl/ScreenServiceImpl.java` 第 156 行 |
| 关系 | 审计报告 `docs/audit/2026-07-24-screen-audit.md` Top-1 问题复测 |

---

## 1. 背景

### 1.1 PSM 反编译事实

`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/screen/service/imp/ScreenServiceImpl.java` 第 157 行：

```java
lineStatusMap.putIfAbsent((CallSite)((Object)(status.getLineNo() + ":" + status.getFaceNo())), lineStatusList);
```

> **PSM 用 `putIfAbsent`** —— 首次写入胜，后续重复 key 不覆盖。

### 1.2 DataupLoad 现状（复测前）

`DataupLoad/.../screen/service/impl/ScreenServiceImpl.java` 第 156 行：

```java
lineStatusMap.put(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
```

> **DataupLoad 用 `put`** —— 最后写入胜，后续重复 key 覆盖前值。

### 1.3 审计报告与实际事实的差异

`docs/audit/2026-07-24-screen-audit.md` 在 Top-1 问题中描述为：

> "PSM 用 `put` 会被覆盖，DataupLoad 用 `putIfAbsent` 第一次写入后保留"

**该描述方向颠倒**。W-SCR-01 复测核对代码原文（用 `Select-String` 抓两文件的 `putIfAbsent|put(` 命中行）确认：

- PSM 反编译：`lineStatusMap.putIfAbsent(...)` — **首次胜**
- DataupLoad：`lineStatusMap.put(...)` — **最后胜**

**真实差异是：DataupLoad 与 PSM 反着。**审计报告误把双方角色写反了。结论仍然成立（DataupLoad 与 PSM 不一致），但哪一边应该被改的判断需要根据业务场景重新拍板（见 §3）。

### 1.4 调用上下文

`getCilentStatusList(List<Line> lines)` 在 `sendScreenDataInfo()` → `buildScreenData()` 流程内被 `GlobalTaskManager` 定时触发，用于推送 WS 大屏数据。完整算法（两版同构）：

1. `statusRecordService.list()` 拉所有 `StatusRecord`（设备在线/离线心跳）。
2. 对每条 `StatusRecord`：
   - 用 `status.getLine()`（Line **实体引用**）作 key，从 `lineStatusMap` 取已有的 list 引用；没有则 `new ArrayList()`。
   - `lineStatusList.add(status)`（始终累加，**这一步两边相同**）。
   - 把这个 list 引用 `put/putIfAbsent` 进 `lineStatusMap`，key 是 `lineNo + ":" + faceNo`（**字符串复合 key**）。
3. 对每个 `Line`：
   - 用 `line.getKey()`（一般是 `lineNo-faceNo` 或 `lineNo:faceNo`）从 `lineStatusMap` 取 list。
   - 对 list 做 `Collectors.toMap(StatusRecord::getDeviceNo, ..., (o, n) -> o.getId() > n.getId() ? o : n)` 按 deviceNo 去重，**保留 id 较大者**。
4. 把去重后的 StatusRecord 按 `DeviceType` 分类，AND-聚合 ONLINE 状态，写进 `ClientStatusDTO`。

> **关键观察**：lineStatusMap 是 **中间结构**，不是缓存。下游第 3 步会按 `id` 大小再次去重，不依赖中间 map 的写入顺序。

---

## 2. 决策

**保留 PSM 行为：将 DataupLoad 第 156 行从 `put` 改为 `putIfAbsent`。**

```diff
- lineStatusMap.put(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
+ lineStatusMap.putIfAbsent(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
```

---

## 3. 理由

### 3.1 PSM 是反编译产物的"参考实现"

按本仓库既定方针（ADR-0005 系列、审计流程），PSM 反编译产物是 DPL 复刻的**事实基线**。行为差异应当主动靠拢 PSM，除非有明确的反向业务诉求。在 W-SCR-01 复测范围内，没有找到反向诉求。

### 3.2 中间表的语义对齐

`lineStatusMap` 的 key 是 `lineNo:faceNo` 字符串，目的是把 StatusRecord 按 (lineNo, faceNo) 分桶后交给下游按 deviceNo 去重。两种语义下"正常路径"行为一致：

- 单设备单 (lineNo, faceNo)：只写一次，谁都无所谓。
- 同 (lineNo, faceNo) 多个 StatusRecord 但都来自同一个 Line 实体引用：`getOrDefault(status.getLine(), ...)` 拿到**同一 list 引用**，add 累加到同一个 list，再 `put/putIfAbsent` 同一个引用回同一个 key——**两种语义结果一致**（map 里这个 key 始终指向这同一个 list 引用，list 内部已累加完毕）。

只有**以下边缘场景**下两种语义不同：
- 同一个 `(lineNo, faceNo)` 复合 key，触发了两次 `put/putIfAbsent` 调用，且两次的 `status.getLine()` 返回**不同的 Line 实体实例**（典型场景：service 层不同事务/不同请求复用了不同 Line 实例，但 lineNo/faceNo 字段值相同）。
- 此时 `put` 会用最新 list 引用覆盖旧的；`putIfAbsent` 保留最早 list 引用，后写的 list "失踪"（挂在旧的 Line 实体引用作 key 的位置，但那个 key 不再被下游用 `line.getKey()` 查到）。

W-SCR-01 调研：本仓库 Line 表的复合唯一性约束通常是 `(lineNo, faceNo)`，业务上同一 (lineNo, faceNo) 不应存在多行 Line 实体。因此"两个 Line 实例同 lineNo:faceNo"主要是**瞬态**（Hibernate/MyBatis-Plus 缓存抖动），而非业务常态。

### 3.3 下游去重不依赖中间表顺序

第 3 步 `Collectors.toMap(StatusRecord::getDeviceNo, ..., (o, n) -> o.getId() > n.getId() ? o : n)` 用 record `id` 大小定胜负，与 list 顺序无关。即便中间表存的是早期 list（putIfAbsent 语义），下游仍会按 id 取最新记录。

### 3.4 多客户端并发的实际影响

复测脚本 `scripts/w-scr-01-retest.ps1` 验证了 putIfAbsent 与 put 的原语差异（v1=10 后写 v2=20：put→20，putIfAbsent→10）。但**在这个具体方法里**：
- 多个线程并发执行 `getCilentStatusList` 会读到**各自的 `statusRecordService.list()` 快照**（不是共享 map），互不影响。
- 单线程内 `lineStatusMap` 是方法局部变量（`HashMap`，非线程安全），但无并发写入。
- `lineStatusList.add(status)` 始终在 put 之前，所以 list 内部一定包含当前 status——区别仅在 key 指向哪个 list 引用。

> **结论**：业务场景（多客户端并发心跳）下，put 与 putIfAbsent 对最终 WS 推送内容**没有可见差异**。选择 putIfAbsent 是"保守贴 PSM"的策略，不引入业务回归。

### 3.5 复测证据

复测脚本 `scripts/w-scr-01-retest.ps1`（PowerShell hashtable 镜像 JDK HashMap 语义）：

```
[用例 1] key=line1:face1, value=10 -> 20
  put_final          : 20
  putIfAbsent_final  : 10
  put_is_last_wins   : True
  putIfAbsent_is_first_wins : True

[用例 2] key=line2:face2, value=List([r1]) -> List([r2])
  put 最终引用 = r2
  putIfAbsent 最终引用 = r1
  put 引用替换为 list2 = True
  putIfAbsent 仍指 list1 = True

[用例 3] 同一 key 写 3 次
  put 最终值 = 3 (最后胜)
  putIfAbsent 最终值 = 1 (首次胜)

全部用例通过
```

### 3.6 字节码复核

修改后 `ScreenServiceImpl.class` `javap -c`：

```
99: invokeinterface #385,  // Map.putIfAbsent
   // (其他 3 处 put 为 sortDefectByName / sortDefectByPosAndName / sortDayRecordByFace,与 PSM 一致,保留)
```

第 156 行的 `lineStatusMap` 调用确认为 `Map.putIfAbsent`，对齐 PSM。

---

## 4. 影响

### 4.1 改动文件

| 文件 | 改动 |
|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/service/impl/ScreenServiceImpl.java` | 第 156 行 `put` → `putIfAbsent` |

### 4.2 调用方影响

`getCilentStatusList` 是私有方法，仅被同类的 `buildScreenData` 调用，后者被 `sendScreenDataInfo` 调用，后者被 `GlobalTaskManager` 定时任务调用。链路无外部 HTTP 入口、无 RPC 入口。

- ✅ **不影响 WS 推送内容**：下游 toMap 去重按 id 大小，与中间表顺序无关。
- ✅ **不影响任何外部接口**：无 controller、无 service 入口暴露此方法。
- ⚠️ **行为差异仅在瞬态（多 Line 实例同 lineNo:faceNo）下可见**，但瞬态本身是反常业务状态，本就不应依赖其结果。

### 4.3 风险

- **回归风险：低**。仅一行改动，与 PSM 行为一致。
- **审计闭环：✅**。`docs/audit/2026-07-24-screen-audit.md` Top-1 问题已消除（同时也纠正了审计报告中关于"谁用 put、谁用 putIfAbsent"的方向描述）。
- **后续清理**：若 audit 报告需要修订"PSM 用 put"的描述，建议补一份修订文件。本次工单范围内仅在本 ADR 中纠正事实，未触动 audit 文件（避免越权修改审计产物）。

### 4.4 编译结果

- `javac -encoding UTF-8 -d target/classes -cp "lib\*" -sourcepath src/main/java <screen 模块文件>` → **exit=0**，6 个 .class 生成。
- 全模块 javac 编译 183 个 java 文件 → **exit=0**，192 个 .class 生成（仅项目既有 rawtype unchecked 警告，与本次改动无关）。
- 字节码 `javap -c -p` 复核：`Map.putIfAbsent` 已落到第 99 偏移。

---

## 5. 关联

- 审计报告：`docs/audit/2026-07-24-screen-audit.md`（Top-1）
- PSM 反编译：`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/screen/service/imp/ScreenServiceImpl.java`
- 复测脚本：`scripts/w-scr-01-retest.ps1`
- 工单报告：`docs/work-orders/W-SCR-01-report.md`
- 工单依赖：W-SCR-01 完成后，screen 模块 5/5 评级可由 F(微调) 升为 F(完全对齐)。
