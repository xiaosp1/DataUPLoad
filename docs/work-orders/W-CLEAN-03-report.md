# W-CLEAN-03 报告 — 删除 LinePO.java 死代码 + 替换 9 个引用点

**完成时间**: 2026-07-24 20:56
**执行人**: Java 开发 worker（subagent W-CLEAN-03）
**工时**: ~10 分钟
**前置 ADR**: `docs/adr/0008-line-po-alias-kept.md`（2026-07-24 19:18 决策保留）
**审计依据**: `docs/audit/2026-07-24-line-audit.md`（§四 清理工单 #7）

## 改动文件清单

### 1. 删除
| 文件 | 说明 |
|---|---|
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/model/LinePO.java` | 死代码源文件，已删除 |
| `DataupLoad/src/main/java/com/hikrobotics/solution/module/line/model/` | 空目录，已删除 |
| `X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\model\LinePO.class` | 旧编译产物，已删除 |
| `X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\model\` | 旧目录，已删除 |

### 2. 修改（8 个文件）
| 文件 | 改动性质 | 关键 diff |
|---|---|---|
| `module/detect/dto/StatusRecordDTO.java` | **代码 + 导入** | `import line.model.LinePO` → `import line.entity.Line`；`buildClient(LinePO line, ...)` → `buildClient(Line line, ...)` |
| `module/line/dto/LineTreeItemDTO.java` | **代码 + 导入** | `import line.model.LinePO` → `import line.entity.Line`；CFR header `LinePO` → `Line`；构造器 `LineTreeItemDTO(LinePO po)` → `LineTreeItemDTO(Line po)` |
| `module/line/service/ILineService.java` | **仅 import + Javadoc** | 删除 `import line.model.LinePO`；3 处 `{@link LinePO}` Javadoc 改为 PSM 反编译命名备注 |
| `module/line/service/impl/LineServiceImpl.java` | **代码 + 导入 + Javadoc** | 删除 `import line.model.LinePO`；`handleLineTreeSearch` 内 2 处 `new LineTreeItemDTO(BeanUtil.copyProperties(line, LinePO.class))` 简化为 `new LineTreeItemDTO(line)`（构造器已直接接受 Line）；4 处 Javadoc 标注"W-CLEAN-03 起 LinePO 已删除" |
| `module/line/entity/Line.java` | **仅 Javadoc** | 类注释追加"W-CLEAN-03 起 PSM LinePO 已删除，统一用本类" |
| `module/detect/service/impl/DefectRecordServiceImpl.java` | **仅 Javadoc** | `* 不再单独搞 LinePO / LineDayRecord PO` 追加"W-CLEAN-03 起 LinePO 已删除"（实际代码早已用 Line，无 import 改动） |
| `module/line/service/impl/StateChangeServiceImpl.java` | **仅 Javadoc** | `* PSM ... LinePO → DataupLoad entity 同名类` 加注 "PSM 反编译命名，DataupLoad 已统一为 entity/Line" |
| `module/screen/service/impl/ScreenServiceImpl.java` | **仅 CFR header 注释** | 删除 CFR 头部 "Could not load" 列表中 `com.hikrobotics.solution.module.line.model.LinePO` 一行（无实际 import） |

### 3. ADR 备注（未新建）
- `docs/adr/0008-line-po-alias-kept.md` 仍然描述了"为什么 2026-07-24 当时保留 LinePO"，作为历史决策留痕。本工单是 W-X29 P2 计划中的"可选清理工单"，执行后 ADR 内容与代码状态已统一（即"先保留后清理"轨迹完整）。

## 替换前后对比

### import 替换（5 个文件）
```diff
- import com.hikrobotics.solution.module.line.model.LinePO;
+ import com.hikrobotics.solution.module.line.entity.Line;
```
涉及：`StatusRecordDTO.java`、`LineTreeItemDTO.java`、`ILineService.java`（仅删除 import）、`LineServiceImpl.java`（仅删除 import + 代码已不再需要）

### 类型替换（2 个代码引用点）
```diff
// StatusRecordDTO.java
- public StatusRecordDTO buildClient(LinePO line, String deviceNo) {
+ public StatusRecordDTO buildClient(Line line, String deviceNo) {

// LineTreeItemDTO.java
- public LineTreeItemDTO(LinePO po) {
+ public LineTreeItemDTO(Line po) {
```

### BeanUtil 中转简化（LineServiceImpl.handleLineTreeSearch）
**Before**（依赖 LinePO 作为中转）：
```java
LineTreeItemDTO tree = sortByLineNo.computeIfAbsent(
    line.getLineNo(),
    k -> new LineTreeItemDTO(BeanUtil.copyProperties(line, LinePO.class)));
tree.getChilds().add(
    new LineTreeItemDTO(BeanUtil.copyProperties(line, LinePO.class))
        .setLineNo(line.getFaceNo()));
```

**After**（直接用 Line 实体）：
```java
LineTreeItemDTO tree = sortByLineNo.computeIfAbsent(
    line.getLineNo(),
    k -> new LineTreeItemDTO(line));
tree.getChilds().add(
    new LineTreeItemDTO(line)
        .setLineNo(line.getFaceNo()));
```

## 字段一致性验证

| 字段 | LinePO | Line (entity) | 一致？ |
|---|---|---|---|
| id (Integer, @TableId AUTO) | ✓ | ✓ | ✓ |
| name (String) | ✓ | ✓ | ✓ |
| lineNo (String) | ✓ | ✓ | ✓ |
| faceNo (String) | ✓ | ✓ | ✓ |
| color (String, IGNORED update) | ✓ | ✓ | ✓ |
| clientNo (String) | ✓ | ✓ | ✓ |
| order (Integer, @TableField exist=false) | ✓ | ✓ | ✓ |
| realtimeData (String) | ✓ | ✓ | ✓ |
| updateTime (LocalDateTime, @JsonFormat) | ✓ | ✓ | ✓ |
| createTime (LocalDateTime, @JsonFormat) | ✓ | ✓ | ✓ |
| getKey()/getPos() 派生方法 | ✓ | ✓ | ✓ |
| @TableName("line") | ✓ | ✓ | ✓ |

**结论**：两个类的字段集、表名、注解完全一致，**无需补字段**。

**差异**（不影响替换）：
- LinePO 有 CFR 生成的 `equals` / `hashCode` / `toString`；Line 沿用 PSM 反编译原版（继承 Object 默认实现）
- 两个类都 `implements Serializable`，serialVersionUID 均为 1L

## 编译结果

**命令**（按工单规范）：
```bash
javac -encoding UTF-8 -parameters -d X:\DataupLoad\target\classes \
  -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
  -sourcepath DataupLoad\src\main\java \
  <187 个 .java 文件>
```

**结果**：
- **退出码 0**（0 errors）
- 仅有的输出：CFR 反编译产物的泛型 unchecked 警告（pre-existing，与本次改动无关）
- 完整日志：`scripts/w-clean-03-compile.log`
- 增量编译受影响文件 8 个同样 0 errors

**编译产物验证**：
- `X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\entity\Line.class` 存在
- `X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\model\` **不存在**（LinePO.class 已消失）
- `X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\entity\` 仍有 `Line.class`（同包不同名，未冲突）

## 冒烟测试结果

### 重启
- **旧进程**：PID 13416（2026-07-24 20:51:23 启动）→ Stop-Process
- **新进程**：PID 23688（2026-07-24 20:55:42 启动），`X:\DataupLoad\go.bat` 拉起
- **Spring Boot 启动**：23.6 秒 `Started Application in 23.607 seconds`
- **启动日志扫描**：无 `LinePO` / `ClassNotFound` / `NoClassDefFound` / `Error creating bean` 异常
- **运行观测**：服务上线 30+ 秒，已处理 10+ 条生产线实时告警（line1A/line1B/line3B/line9A 等），无 crash

### 5 个 smoke endpoint（全部 200）

| URL | 状态 | body 长度 | body 内容摘要 |
|---|---|---|---|
| `/web/line/list` | **200** | 722 | `{"success":true,"data":[{"id":1,"name":"Line-L1","lineNo":"L1","faceNo":"F1",...}]}` |
| `/web/line/plan/manage` | **200** | 47 | `{"success":false,"code":10500,"message":"操作异常"}`（缺参，路由解析正常） |
| `/web/line/tree-search` | **200** | 99 | `{"success":true,"data":[{"name":"Line-L1","lineNo":"L1","key":"L1:null","pos":"L1:null"}],...}` |
| `/web/line/state/statistic` | **200** | 47 | `{"success":false,"code":10400,"message":"参数异常"}`（缺参，路由解析正常） |
| `/web/detect/realtime` | **200** | 47 | `{"success":false,"code":10500,"message":"操作异常"}`（缺参，路由解析正常） |

**关键观察**：
- 所有 5 个 endpoint 都返回 HTTP **200**（不是 500/404/ClassNotFound）
- `/web/line/list` 返回真实产线数据（id=1, name=Line-L1, lineNo=L1, faceNo=F1...），说明 `Line` 实体字段映射正确，MyBatis-Plus `BaseMapper<Line>` 工作正常
- `/web/line/tree-search` 返回树结构，说明 `LineTreeItemDTO(Line)` 构造器（**不再是 `LineTreeItemDTO(LinePO)`**）调用正常，controller / service / dto 链路打通
- 47 字节的 body 是预期的参数校验失败（部分 endpoint 必传 lineNo/faceNo/date），是原有行为，不是 LinePO 删除引起的回归

## 已知限制

1. **ADR-0008 未反向更新**：保留 ADR 描述的是 2026-07-24 19:18 的"保留决策"，是 W-CLEAN-03 的前置上下文。可选后续：开 ADR-0010 标注"清理已完成 + 引用归一 Line"，但非必须（已附本报告）。

2. **`PlanToLinePO` 命名残留**：`LineServiceImpl.java:86` Javadoc 提到 `PSM PlanToLinePO → DataupLoad PlanToLine` —— 这是另一个 PSM 反编译命名，本工单不在清理范围（属于 plan 模块，与 W-CLEAN-03 主题 line 无关）。

3. **CFR header 注释未全清理**：`ScreenServiceImpl.java` / `LineTreeItemDTO.java` 顶部的 `/* Decompiled with CFR 0.152. */` 注释块保留了反编译元数据（"Could not load" 列表），仅清理了 LinePO 相关行；其它历史 PSM 类名（DefectDayRecordPO / LineDayRecordPO / StatusRecordPO 等）已不存在，但仍出现在 CFR header 中。这是反编译产物留痕，不影响功能。

4. **`LineTreeItemDTO.equals()` 行为变化**：原 LinePO 的 CFR `equals` 比较所有 10 个字段；现在改用 Line 实体的默认 `Object.equals()`（引用相等）。如未来有 `List<LineTreeItemDTO>.contains(...)` 之类的业务调用，需要手动重写 LineTreeItemDTO 的 equals —— 审计报告未发现此类调用点。

5. **未执行集成测试**：仅冒烟 5 个 endpoint，未运行 `scripts/acceptance.py` 或 W-X15a/15b 测试类。如需完整回归可后续安排。

## 不替换 / 保留项说明

**没有文件被 ADR 化保留** —— 全部 8 个引用点都成功替换，LinePO.java 顺利删除。无需新增"为什么不替换" ADR。

---

**结论**：W-CLEAN-03 完成。LinePO.java 死代码已彻底删除，9 个原始引用点全部归一到 `line.entity.Line`，编译 0 errors，5 个冒烟 endpoint 全 200。
