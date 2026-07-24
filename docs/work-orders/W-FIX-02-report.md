# W-FIX-02 Report — 清理 PSM 反编译产物残留（CFR header + assertj + @author）

**Worker**: Java developer (subagent W-FIX-02)
**Dispatch**: `docs/dispatch/W-X30-CLEANUP-dispatch.md` §W-FIX-02
**Run time**: 2026-07-24 21:28–21:55 (~27 min)
**Status**: ✅ 完成

---

## 1. 任务目标

W-X27/28/29 P0/P1/P2 冲刺期间，为快速对齐 PSM 反编译产物，1:1 抄了大量 PSM 文件。这些文件保留了 PSM 反编译器的痕迹，需要清理：

1. **CFR header 注释**：删除每个文件顶部的 `/* Decompiled with CFR 0.152. ... */` 反编译器注释块
2. **`@author` 残留**：删除 5 个 websocket 文件的 `@author DataupLoad W-B06` 行
3. **assertj 引用**：删除生产代码中的 `org.assertj.core.util.Lists` / `Sets` import + 调用

---

## 2. 改动文件清单

### 2.1 CFR header 清理（28 个文件，共删 235 行）

**`framework/` (2)**

| 文件 | -行 |
|------|----:|
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/common/base/BaseResult.java` | -11 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/common/query/IdQuery.java` | -3 |

**`module/alarm/` (2)**

| 文件 | -行 |
|------|----:|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/dto/DefectTypeDTO.java` | -10 |

**`module/line/` (18)**

| 文件 | -行 |
|------|----:|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/ChgLineOrderDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/ClientPlanQueryDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/ClientPlanResultDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/DefectQueryDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/DetectDataUploadDTO.java` | -10 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LineBodyDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LineCountDTO.java` | -6 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LineDTO.java` | -6 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LinePanelQueryDTO.java` | -8 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LinePlanBindDTO.java` | -8 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LinePlanBindQueryDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LinePlanSwitchDTO.java` | -8 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/LineUpdateDTO.java` | -8 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/PlanDTO.java` | -10 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/PlanQueryDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/ToDayCountDTO.java` | -6 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/dto/WebLineBindPlanResultDTO.java` | -7 |

**`module/screen/` (5)**

| 文件 | -行 |
|------|----:|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/dto/ClientStatusDTO.java` | -6 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/dto/DefectNumberDTO.java` | -7 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/screen/service/IScreenService.java` | -6 |

> ⚠️ 备注：`DefectCountDisPlayDTO.java`、`LineTreeItemDTO.java`、`SearchStateStatisticForm.java`、`ScreenDataDTO.java`、`ScreenServiceImpl.java` 同时出现在 §2.1 (CFR) 和 §2.3 (assertj)，故不重复列出。其 CFR 块删除行数：-7、-8、-9、-11、-31。

**策略**：所有 CFR 文件顶部第一行非空行就是 `/*`，没有类级 Javadoc 在 CFR 块之上，故可直接删除 `/* ... */` 块，并去除之后的空行让 `package` 成为第一行（与代码库其它文件风格一致）。

### 2.2 `@author` 清理（5 个文件，各 -1 行）

| 文件 | -行 |
|------|----:|
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/websocket/AlarmWebSocketHandler.java` | -1 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/websocket/DataupLoadWebSocketConfig.java` | -1 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/websocket/PathTypeHandshakeInterceptor.java` | -1 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/websocket/ScreenWebSocketHandler.java` | -1 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/framework/websocket/WebSocketDebugController.java` | -1 |

> 删除 `* @author DataupLoad W-B06` 行，保留 `* @since 2026-07-22` 与其余 Javadoc。
>
> **注意**：本批次的 `@author` 内容是项目内部工单 credit (`DataupLoad W-B06`)，不是 PSM 原始作者名（git 历史已确认文件是 2026-07-24 由本项目新创建）。但 brief 明确要求删除 `@author` 残留，故按指令执行。

### 2.3 assertj 替换（12 个文件，+44 / -45 行）

| 文件 | +/- | 替换内容 |
|------|:---:|---------|
| `module/alarm/config/DefectAlarmConfig.java` | +3 -3 | import + 2× `Lists.newArrayList()` → `new ArrayList<>()` |
| `module/alarm/service/impl/AlarmRecordServiceImpl.java` | +5 -5 | import + 4× `Lists.newArrayList()` |
| `module/config/service/impl/SystemConfigServiceImpl.java` | +2 -2 | import + 1× `Lists.newArrayList()` |
| `module/defect/service/impl/LineDefectTypeServiceImpl.java` | +3 -3 | import + 2× `Lists.newArrayList()` |
| `module/detect/service/impl/DefectDayRecordServiceImpl.java` | +4 -5 | import + 4× `Lists.newArrayList()` |
| `module/line/dto/DefectCountDisPlayDTO.java` | +3 -10 | import + 2× `Lists.newArrayList()` (同时含 CFR 清理) |
| `module/line/dto/LineTreeItemDTO.java` | +2 -10 | import + 1× `Lists.newArrayList()` (同时含 CFR 清理) |
| `module/line/dto/SearchStateStatisticForm.java` | +1 -11 | import + 1× `Sets.newHashSet()` → `new HashSet<>()` (同时含 CFR 清理) |
| `module/line/service/impl/LineOrderServiceImpl.java` | +2 -3 | import + 2× `Lists.newArrayList()` |
| `module/line/service/impl/LineServiceImpl.java` | +4 -5 | import + 4× `Lists.newArrayList()` / `Collections.singletonList()` + 1× Javadoc 更新 |
| `module/line/service/impl/StateChangeServiceImpl.java` | +1 -2 | import + 1× `Lists.newArrayList(Collection)` → `new ArrayList<>(Collection)` |
| `module/screen/dto/ScreenDataDTO.java` | +11 -23 | import + 4× `Lists.newArrayList()` (同时含 CFR 清理) |
| `module/screen/service/impl/ScreenServiceImpl.java` | +1 -33 | import + 1× `Sets.newHashSet()` → `new HashSet<>()` (同时含 CFR 清理) |
| `module/yingke/dto/ListParamsDTO.java` | +2 -2 | import + 1× `Lists.newArrayList()` |

**替换语义说明**：

- `Lists.newArrayList()`（零参） → `new ArrayList<>()` — 完全等价
- `Lists.newArrayList(lineData.getId())`（单参，单值） → `Collections.singletonList(lineData.getId())`
  - 见 `LineServiceImpl.java:248`：调用 `ILineOrderService.addLineOrder(List<Integer>)`，被调方只读不修改，singletonList 不可变但接口兼容
- `Lists.newArrayList(sortChangeByLine.get(line))`（Collection 参数） → `new ArrayList<>(sortChangeByLine.get(line))`
  - 见 `StateChangeServiceImpl.java:182`：被调方后续会 `.sort(...)`，必须可变 ArrayList
- `Sets.newHashSet()` → `new HashSet<>()` — 完全等价

每个文件都补回了对应的 `import java.util.ArrayList;` / `import java.util.HashSet;`（按 `java.util` 字典序插入原 `org.assertj` 位置之前），保持与代码库 import 顺序约定一致。

### 2.4 重叠文件（同时出现在多个分类）

| 文件 | CFR | @author | assertj |
|------|:---:|:-------:|:-------:|
| `module/line/dto/DefectCountDisPlayDTO.java` | ✅ |  | ✅ |
| `module/line/dto/LineTreeItemDTO.java` | ✅ |  | ✅ |
| `module/line/dto/SearchStateStatisticForm.java` | ✅ |  | ✅ |
| `module/screen/dto/ScreenDataDTO.java` | ✅ |  | ✅ |
| `module/screen/service/impl/ScreenServiceImpl.java` | ✅ |  | ✅ |

### 2.5 总计

- 改动文件：**42 个 .java**（去重后）
- 净行数变化：**+44 / -291**（删除明显大于新增，符合"清理"语义）

---

## 3. 编译结果

### 3.1 命令

```powershell
# brief 推荐的 sourcepath 形式（排除 LinePanelDTO.java）
cd E:\DEMO\数据采集
powershell -ExecutionPolicy Bypass -File tmp\fix02-compile2.ps1
```

实际命令（解决中文路径 GBK/UTF-8 问题后）：

```powershell
javac -encoding UTF-8 -parameters `
  -d 'X:\DataupLoad\target\classes' `
  -cp 'X:\DataupLoad\target\classes;X:\DataupLoad\lib\*' `
  -sourcepath 'DataupLoad\src\main\java' `
  @sources.txt
```

`sources.txt` 用系统默认编码（GBK，与 `dir /s /b` 输出一致）写入以保证 javac argfile 正确读取中文路径。

### 3.2 结果

```
File count: 186
Sources file: E:\DEMO\数据采集\tmp\sources-fix02.txt
Running javac with @argfile...
Exit code: 0
No errors.
```

并交叉验证了 `X:\compile.bat`（标准构建脚本）：

```
javac exit code: 0
```

**0 错误，0 新警告**（编译输出与改动前一致；`compile.err` 中的 GBK 显示乱码是 cmd 控制台 codepage 问题，已知遗留，与本工单无关 —— 见 W-FIX-01 报告 §"遗留噪音"）。

---

## 4. 保守清理原则执行

- ✅ 只删确定的反编译残留（CFR header / `@author` / assertj）
- ✅ 不重构其它代码、不改 PSM 1:1 业务逻辑
- ✅ `LineServiceImpl.java:248` 单参替换选 `Collections.singletonList` 而非 `Arrays.asList` 包装 — 因为 `Collections` 已导入，且被调方不修改列表
- ✅ `StateChangeServiceImpl.java:182` Collection 参数替换选 `new ArrayList<>(Collection)` — 保留被调方 `.sort(...)` 所需的可变性
- ✅ 保留所有原有 Javadoc（含 `W-X21`、`W-B05`、`W-DFT-01b` 等工单引用）

---

## 5. 已知限制

### 5.1 ChangeLineDefectResult.java 的 assertj 提及（保留）

```
DataupLoad/src/main/java/com/hikrobotics/solution/module/defect/entity/ChangeLineDefectResult.java:26-27:
 *   <li>集合字段使用 {@link ArrayList} 默认初始化，移除 PSM 中对 {@code org.assertj.core.util.Lists}
 *       的依赖（assertj 是测试依赖，不应出现在 entity 中）。</li>
```

这是 W-X30 之前已修改过的文件：Javadoc 中**描述了** assertj 清理决策（"为什么改用 ArrayList"），属于历史决策留痕。代码本身已无 assertj 引用。**未改动此文件**，符合"保守清理"原则。

### 5.2 LinePanelDTO.java 按 brief 要求排除编译

`brief` 中的 javac 命令显式排除 `LinePanelDTO.java`：

```powershell
Where-Object { $_.FullName -notlike '*LinePanelDTO.java' }
```

原因未在 brief 中说明，但保留该行为。LinePanelDTO.java 未被本工单修改（CFR/@author/assertj 扫描均未命中），但仍按 brief 排除参与本次编译验证。

### 5.3 `@author DataupLoad W-B06` vs PSM 原始作者

如 §2.2 所述，本批次的 `@author` 内容是项目内部工单 credit（指向 W-B06 websocket 工单），而非 PSM 原始作者信息。git 历史确认这 5 个文件是 2026-07-24 由本项目新创建（commit `c8e8947`），不存在 PSM 原始作者。

但 brief 明确要求"删除 `@author` 残留"，且这 5 个文件确实都含有 `@author` 行，故按指令执行删除。如果未来需要保留 "W-B06 实施者" 这种项目内部 credit 痕迹，建议通过 `git blame` 或 PR description 而非源码 `@author` 标签。

### 5.4 EOL 归一化

`tmp/fix02-clean-cfr.ps1` 使用 PowerShell `WriteAllLines`，在 Windows 平台上默认产生 CRLF 行尾。所有 42 个改动文件已用 `tmp/fix02-normalize-eol.ps1` 统一为 LF，符合 `.editorconfig` 的 `end_of_line = lf` 设置。

### 5.5 Git 暂未 push

按 brief 要求：**不要推 git**。改动仅在工作区，main agent 可在 W-X30 收尾时一并 push。

---

## 6. 工单状态

- [x] 扫描所有 .java 文件（CFR / @author / assertj）
- [x] 删除 28 个 CFR header 注释块（235 行）
- [x] 删除 5 个 websocket 文件的 `@author` 行
- [x] 替换 12 个文件中的 assertj import + 23 处 `Lists.newArrayList()` / 2 处 `Sets.newHashSet()`
- [x] 编译：0 error / 0 new warning
- [x] 报告写到 `docs/work-orders/W-FIX-02-report.md`
- [ ] git push（由 main agent 在 W-X30 收尾时执行）
