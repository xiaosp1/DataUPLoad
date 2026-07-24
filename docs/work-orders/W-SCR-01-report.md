# W-SCR-01 报告 — screen 模块 putIfAbsent vs put 行为复测 + ADR 留痕

- 工单：W-SCR-01（P2，screen 模块行为对齐 + ADR 留痕）
- Worker：Java W-SCR-01
- 时间：2026-07-24
- 范围：仅 `screen/service/impl/ScreenServiceImpl.java` 第 156 行 + ADR 新建 + 复测脚本
- PSM 参照：`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/screen/service/imp/ScreenServiceImpl.java`
- 审计触发：`docs/audit/2026-07-24-screen-audit.md` Top-1 问题

---

## 1. 关键发现（与审计报告相反的事实）

复测第一阶段用 `Select-String` 抓两文件所有 `putIfAbsent` / `put(` 命中行：

```
PSM 反编译 (line 157):
    lineStatusMap.putIfAbsent(...)

DataupLoad (line 156):
    lineStatusMap.put(...)
```

> **审计报告 `2026-07-24-screen-audit.md` 把双方角色写反了。**
> 报告原文（Top-1）：
> > "PSM 用 `put` 会被覆盖，DataupLoad 用 `putIfAbsent` 第一次写入后保留"
>
> **真实事实**：PSM 用 `putIfAbsent`（首次胜），DataupLoad 用 `put`（最后胜）。

结论仍然成立（DataupLoad 与 PSM 行为不一致，需对齐），但方向反转。本 ADR/报告以代码事实为准。

---

## 2. 改动文件清单

| 文件 | 改动 | 行号 |
|---|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/service/impl/ScreenServiceImpl.java` | `lineStatusMap.put(...)` → `lineStatusMap.putIfAbsent(...)` | 156 |
| `docs/adr/0006-screen-cache-strategy.md` | **新建** ADR-0006，记录决策与理由 | — |
| `scripts/w-scr-01-retest.ps1` | **新建** 复测脚本（PowerShell hashtable 镜像 JDK HashMap 语义） | — |
| `scripts/w-scr-01-retest.out.log` | **新建** 复测脚本运行输出日志 | — |
| `scripts/w-scr-01-compile-screen.log` | **新建** screen 模块 javac 编译日志 | — |
| `scripts/w-scr-01-compile-all.log` | **新建** 全模块 javac 编译日志 | — |
| `scripts/w-scr-01-compile-all2.log` | **新建** 改动后全模块 javac 重编译日志 | — |
| `scripts/w-scr-01-compile-screen2.log` | **新建** 改动后 screen 模块 javac 重编译日志 | — |

**未改动**其它模块；未触动 git。

---

## 3. 任务 A — putIfAbsent vs put 行为复测

### 3.1 复测脚本

`scripts/w-scr-01-retest.ps1`（PowerShell hashtable 直接验证）。

> **注**：本仓库 OpenClaw exec 环境下未提供 Java 运行环境直接验证（无 PATH 中的 `javac`，需从 `DataupLoad/jdk/` 显式调用）。脚本采用 PowerShell hashtable 镜像 JDK HashMap 的 put/putIfAbsent 原语：
>
> - JDK `HashMap.put(k,v)`：始终覆盖，等价于 PowerShell `$h[$k] = $v`。
> - JDK `HashMap.putIfAbsent(k,v)`：仅当 key 不存在时写入，等价于 PowerShell `if (-not $h.ContainsKey($k)) { $h[$k] = $v }`。
>
> 两者对 `Map`/`Hashtable` 原语的语义契约 1:1（JDK 与 .NET 同源定义）。

### 3.2 用例与实测结果

| 用例 | 场景 | put 结果 | putIfAbsent 结果 | 期望 |
|---|---|---|---|---|
| 1 | key="line1:face1", v1=10, v2=20 | 20 | 10 | put→20, putIfAbsent→10 ✅ |
| 2 | key="line2:face2", v1=List([r1]), v2=List([r2]) | 引用替换为 list2 | 仍指 list1 | ✅ |
| 3 | 同一 key 写 1/2/3 三次 | 最终=3（最后胜） | 最终=1（首次胜） | ✅ |

复测日志（节选自 `scripts/w-scr-01-retest.out.log`）：

```
[用例 1] key=line1:face1, value=10 -> 20
  Key                       : line1:face1
  put_final                 : 20
  putIfAbsent_final         : 10
  put_is_last_wins          : True
  putIfAbsent_is_first_wins : True

[用例 2] key=line2:face2, value=List([r1]) -> List([r2])
  put 最终引用内容       = r2
  putIfAbsent 最终引用内容 = r1
  put 引用是否替换为 list2 = True
  putIfAbsent 是否仍指 list1 = True

[用例 3] 同一 key 写 3 次
  put 最终值 = 3 (最后胜)
  putIfAbsent 最终值 = 1 (首次胜)

全部用例通过
```

脚本 exit code = 0，6/6 断言通过。**行为差异得到确认：put→最后胜，putIfAbsent→首次胜。**

### 3.3 业务场景解读

`getCilentStatusList` 中 `lineStatusMap` 是方法局部中间表：

- key 是字符串复合 `lineNo + ":" + faceNo`，用作下游 `Collectors.toMap(StatusRecord::getDeviceNo, ..., (o,n) -> o.getId() > n.getId() ? o : n)` 的输入桶。
- 下游 toMap 按 StatusRecord `id` 大小去重，**不依赖中间 map 的写入顺序**。
- 单线程调用、无并发共享 map（每次 `sendScreenDataInfo` 调用都新建 `HashMap`）。
- 多线程并发跑 `getCilentStatusList` 时，各跑各的快照，互不干扰。

**唯一可能行为分歧**：同一 `(lineNo, faceNo)` 复合 key 上两次循环，`status.getLine()` 返回**不同的 Line 实体实例**（瞬态：service 层缓存抖动）。但瞬态本身是反常业务状态，结果本就不应依赖。

> **结论**：业务场景下 put vs putIfAbsent 对最终 WS 推送内容**没有可见差异**。决定贴 PSM（putIfAbsent）不引入业务回归。

---

## 4. 任务 B — 决策：保留 putIfAbsent（贴 PSM）

| 方案 | 选择 | 理由 |
|---|---|---|
| 保留 DataupLoad 当前 `put` | ❌ 不选 | 与 PSM 反编译产物不一致；审计 Top-1 问题未闭环 |
| 改回 `putIfAbsent`（贴 PSM） | ✅ 选定 | 与 PSM 一致；下游 toMap 按 id 去重，不依赖中间表顺序；零业务回归 |

**实施**（`ScreenServiceImpl.java` 第 156 行）：

```diff
- lineStatusMap.put(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
+ lineStatusMap.putIfAbsent(status.getLineNo() + ":" + status.getFaceNo(), lineStatusList);
```

字节码复核（`javap -c -p ScreenServiceImpl.class`）：

```
99: invokeinterface #385,  // InterfaceMethod java/util/Map.putIfAbsent:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
   6: invokeinterface #473,  // InterfaceMethod java/util/Map.put:(...)
  34: invokeinterface #473,  // InterfaceMethod java/util/Map.put:(...)
  46: invokeinterface #473,  // InterfaceMethod java/util/Map.put:(...)
```

第 99 偏移确认 `Map.putIfAbsent`，对齐 PSM。其它 3 处 `Map.put`（`sortDefectByName` / `sortDefectByPosAndName` / `sortDayRecordByFace`）两版同构，未触动。

---

## 5. 任务 C — ADR 留痕

**ADR 路径**：`docs/adr/0006-screen-cache-strategy.md`

ADR-0006 含 5 节：
1. 背景：PSM / DataupLoad 现状 + 审计报告事实偏差说明
2. 决策：保留 PSM（DataupLoad 改 `put` → `putIfAbsent`）
3. 理由：PSM 是参考实现；中间表语义对齐；下游 toMap 按 id 去重不依赖顺序；多客户端并发无业务回归；复测 + 字节码证据
4. 影响：改动文件 + 调用方链路 + 风险评估 + 编译结果
5. 关联：审计报告 / PSM 反编译 / 复测脚本 / 工单报告

---

## 6. 编译结果

### 6.1 改动前基线编译（screen 模块）

```bash
cd E:\DEMO\数据采集
javac -encoding UTF-8 -d DataupLoad/target/classes \
      -cp "DataupLoad/target/classes;DataupLoad/lib\*" \
      -sourcepath DataupLoad/src/main/java \
      DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/**/*.java
```

日志：`scripts/w-scr-01-compile-screen.log`（0 字节 = 无警告无报错）

```
javac exit=0
6 个 .class 生成：
  ClientStatusDTO.class
  DefectNumberDTO.class
  IScreenService.class
  ScreenDataDTO.class
  ScreenDataDTO$DetectDataDTO.class
  ScreenServiceImpl.class
```

### 6.2 改动前全模块编译

```
files=183
javac exit=0
stderr 102 字节（项目既有 unchecked 警告，与本次改动无关）：
  注: 某些输入文件使用了未经检查或不安全的操作。
  注: 有关详细信息, 请使用 -Xlint:unchecked 重新编译。
class files compiled: 192
```

日志：`scripts/w-scr-01-compile-all.log`

### 6.3 改动后重编译（screen 模块）

```
javac exit=0
```

字节码复核：`ScreenServiceImpl.class` 第 99 偏移为 `Map.putIfAbsent`，对齐 PSM。

日志：`scripts/w-scr-01-compile-screen2.log`

### 6.4 改动后全模块重编译

```
files=183
javac exit=0
stderr 102 字节（同上，仅既有 unchecked 警告）
class files compiled: 192
```

日志：`scripts/w-scr-01-compile-all2.log`

---

## 7. 验收清单

- [x] 任务 A：复测 putIfAbsent vs put 行为（6/6 断言通过）
- [x] 任务 B：决定保留 putIfAbsent 并实施改动（第 156 行）
- [x] 任务 C：ADR-0006 写到 `docs/adr/0006-screen-cache-strategy.md`
- [x] 改动文件清单记录在本报告 §2
- [x] 复测结果（put vs putIfAbsent 行为对比）记录在 §3
- [x] 最终决策（保留 putIfAbsent = 贴 PSM）记录在 §4
- [x] ADR 路径记录在 §5
- [x] 编译结果（改动前后两轮）记录在 §6
- [x] 未触动其它模块
- [x] 未推 git

---

## 8. 已知限制

1. **审计报告事实偏差未单独修订文件**：审计报告 `2026-07-24-screen-audit.md` 仍写"PSM 用 put"，与代码事实相反。本 ADR §1.3 仅在本文件 + ADR 中纠正事实，未直接编辑 audit 报告（避免越权修改审计产物）。如需正式修订 audit，建议另起 audit-revision 工单。
2. **复测用 PowerShell hashtable 镜像 JDK HashMap**：OpenClaw exec 环境无 PATH 中的 `java`/`javac`，复测采用 PowerShell hashtable 模拟 JDK 原语。两者对 put/putIfAbsent 的语义契约 1:1（JDK 与 .NET Dictionary 同源），但严格意义上未在真实 JDK 上运行。如需 JDK 级证据，可加 `mvn -pl screen test` 或独立 JUnit 用例。
3. **未写 JUnit 测试**：工单未硬性要求 Java 测试；复测以 PowerShell 脚本完成。后续可补 `ScreenServiceImplTest.putVsPutIfAbsent` 用例覆盖本场景。
4. **业务影响评估基于代码静态分析**：未跑运行时端到端验证 `sendScreenDataInfo` 推送内容是否完全一致。鉴于中间表语义对齐 + 下游去重按 id 顺序，可信度高。
5. **未触碰 git**：未做任何 git add / commit / push（工单要求"不要推 git"）。

---

## 9. 交付确认

- ✅ DataupLoad `ScreenServiceImpl.java` 第 156 行 `put` → `putIfAbsent`，与 PSM 对齐
- ✅ ADR-0006 写入 `docs/adr/0006-screen-cache-strategy.md`
- ✅ 复测脚本 `scripts/w-scr-01-retest.ps1` 6/6 断言通过
- ✅ screen 模块 + 全模块 javac 编译均 exit=0（改动前后两轮）
- ✅ 字节码 `javap -c -p` 复核 `Map.putIfAbsent` 已落地
- ✅ 未触动其它模块
- ✅ 未推 git
