# W-LIN-05 报告 — LineServiceImpl 4 个 PSM 1:1 方法 + LineController 3 个 endpoint

**完成时间**: 2026-07-24 19:14
**执行人**: Java worker (subagent)
**优先级**: P2 — audit 2026-07-24 line 模块残缺方法收尾

## 改动文件汇总

| 文件 | 改动类型 | 行数（前 → 后） | 备注 |
|---|---|---|---|
| `DataupLoad/.../line/service/ILineService.java` | 修改（追加 4 个方法声明） | 108 → 150 | 新增 `lineGroup()` / `chgLineOrder(List)` / `handleLineTreeSearch()` / `listByLineNo(List<String>)` |
| `DataupLoad/.../line/service/impl/LineServiceImpl.java` | 修改（追加 4 个方法实现） | 537 → 662 | PSM 1:1 抄 4 个方法 + 新增 import (`ChgLineOrderDTO` / `LineTreeItemDTO` / `LinePO`) |
| `DataupLoad/.../line/web/LineController.java` | 修改（追加 3 个 endpoint + 联通 2 个原有 stub） | 230 → 262 | 新增 `GET /tree-search` / `POST /chg-line-order` / `GET /plan/manage`；W-LIN-03 的 `PUT /order` / `GET /tree` 从 stub 升级为联通 |

合计 3 个文件改动（接口 + service impl + controller），未触碰其他模块。

## 改动文件 — Diff 摘要

```
DataupLoad/.../line/service/ILineService.java        | +42 -0
DataupLoad/.../line/service/impl/LineServiceImpl.java| +125 -0
DataupLoad/.../line/web/LineController.java          | +85 -52
3 files changed, 252 insertions(+), 52 deletions(-)
```

## 4 个 service 方法签名 + 实现要点（PSM 1:1）

### 1. `BaseResult lineGroup()`

| 项 | 内容 |
|---|---|
| 签名 | `public BaseResult lineGroup()`（PSM 1:1） |
| 实现 | `this.baseMapper.selectList(new QueryWrapper<Line>().select("distinct NAME,line_no"))` |
| 返回 | `BaseResult.build().data(List<Line>)`（仅含 `name` + `lineNo` 字段的去重 line 列表） |
| 业务语义 | 产线分组查询（去重 name+line_no 维度），用于前端下拉/筛选 |
| 注入复用 | 无（仅用 baseMapper） |
| PSM 1:1 校验 | ✅ 与 PSM `lineDAO.selectList(new QueryWrapper().select(new String[]{"distinct NAME,line_no"}))` 等价；DataupLoad 用 `Line` 实体替代 PSM `LinePO`，select 字符串大小写兼容 PG / MyBatis Plus 3.5.x |

### 2. `BaseResult chgLineOrder(List<ChgLineOrderDTO>)`

| 项 | 内容 |
|---|---|
| 签名 | `public BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders)`（PSM 1:1） |
| 实现 | 三段校验：① `(long) lineOrders.size() != this.count()` → 错误 **20209**；② `lineOrderService.modLineOrder(...)` 返回 `false` → 错误 **20210**；③ 否则 `BaseResult.build().ok()` |
| 返回 | `BaseResult`（`ok` / `error(20209)` / `error(20210)`） |
| 业务语义 | 整批调整线体显示顺序（先按 order 排序，逐个生成 LineOrder 行覆盖原表） |
| 注入复用 | `lineOrderService`（W-LIN-01 已注入；`modLineOrder` 已在 W-B03 实现） |
| PSM 1:1 校验 | ✅ 与 PSM `chgLineOrder(List)` 逻辑完全一致（含 `(long)lineOrders.size() != this.count()` 强转） |
| 备注 | `(long) lineOrders.size()` 是 PSM 反编译特有的强转（避免 `int != long` 自动提升告警），DataupLoad 沿用 |

### 3. `BaseResult handleLineTreeSearch()`

| 项 | 内容 |
|---|---|
| 签名 | `public BaseResult handleLineTreeSearch()`（PSM 1:1） |
| 实现 | 按 lineNo 分组构建 `LineTreeItemDTO` 父子树：① 首次出现某 lineNo 时创建父节点（`id/name/lineNo` from PO）；② 后续同 lineNo 的 line 创建子节点（`lineNo` 替换为 faceNo）；③ 最终按父节点 id 升序排序 |
| 返回 | `BaseResult.build().data(List<LineTreeItemDTO>)` |
| 业务语义 | 产线树（前端树形 UI；父=线号、子=面号） |
| 注入复用 | 无（仅用 `this.list()`） |
| 关键改造 | `Line` 实体（DataupLoad）→ `LinePO`（PSM）转换：`BeanUtil.copyProperties(line, LinePO.class)` 后传给 `LineTreeItemDTO(LinePO)` 构造器。`LinePO` 仍存在于 `line.model.LinePO`（audit 标记为死代码，但 W-LIN-05 复用其作为构造器入参载体）。`Line` 与 `LinePO` 字段一致（id/name/lineNo/faceNo），copyProperties 安全 |
| PSM 1:1 校验 | ✅ 业务语义等价（按 lineNo 分组、faceNo 作子节点 lineNo） |

### 4. `List<Line> listByLineNo(List<String>)` — PSM 1:1 重载

| 项 | 内容 |
|---|---|
| 签名 | `public List<Line> listByLineNo(List<String> lineNos)`（PSM 1:1 重载） |
| 实现 | `CollectionUtil.isNotEmpty(lineNos)` → `this.list(Wrappers.<Line>lambdaQuery().in(Line::getLineNo, lineNos))`；否则 `Lists.newArrayList()` |
| 返回 | `List<Line>`（批量按 lineNo in 查询） |
| 业务语义 | 按 lineNo 集合批量查线体（无对应 controller endpoint，service 层内部调用） |
| 注入复用 | 无（仅用 `this.list()`） |
| 关键决策 | 与已有 `listByLineNo(String)`（W-B03 单参版本）共存为方法重载，Java 按参数类型分派无歧义；接口 `ILineService` 同时声明两个 `listByLineNo` 重载 |
| PSM 1:1 校验 | ✅ 与 PSM `listByLineNo(List<String>)` 完全等价（含空集合返回空列表的边界） |

## 3 个 endpoint 列表 + 联通情况

`LineController` 类级别 `@RequestMapping("/web/line")` 不变。W-LIN-05 新增 3 个 endpoint + 联通 2 个原有 stub：

| # | HTTP Method | Path（相对 `/web/line`） | 完整 URL | 调用的 Service 方法 | 来源 | 状态 |
|---:|---|---|---|---|---|---|
| 1 | `GET`    | `/list`        | `GET    /web/line/list`          | `lineService.listAll(pageQuery)` | W-B05 | ✅ 既有 |
| 2 | `GET`    | `/{lineNo}`    | `GET    /web/line/{lineNo}`      | `lineService.getByLineNo(lineNo)` | W-B05 扩展 | ✅ 既有 |
| 3 | `POST`   | `/`            | `POST   /web/line/`              | `lineService.add(lineDTO)` | W-LIN-03 | ✅ 既有 |
| 4 | `PUT`    | `/`            | `PUT    /web/line/`              | `lineService.modify(lineUpdateDTO)` | W-LIN-03 | ✅ 既有 |
| 5 | `DELETE` | `/`            | `DELETE /web/line/`              | `lineService.delete(id)` | W-LIN-03 | ✅ 既有 |
| 6 | `PUT`    | `/order`       | `PUT    /web/line/order`         | `lineService.chgLineOrder(lineOrders)` | PSM `PUT /order` | ✅ **W-LIN-05 联通**（W-LIN-03 stub → real） |
| 7 | `POST`   | `/chg-line-order` | `POST /web/line/chg-line-order` | `lineService.chgLineOrder(lineOrders)` | W-LIN-05 新增（kebab-case 别名） | ✅ **W-LIN-05 新增** |
| 8 | `GET`    | `/tree`        | `GET    /web/line/tree`          | `lineService.handleLineTreeSearch()` | PSM `GET /tree` | ✅ **W-LIN-05 联通**（W-LIN-03 stub → real） |
| 9 | `GET`    | `/tree-search` | `GET    /web/line/tree-search`   | `lineService.lineGroup()` | W-LIN-05 新增（PSM 等价 `GET /group`） | ✅ **W-LIN-05 新增** |
| 10| `POST`   | `/plan/bind`   | `POST   /web/line/plan/bind`     | `lineService.bindPlan(dto)` | W-LIN-03 | ✅ 既有 |
| 11| `POST`   | `/plan/switch` | `POST   /web/line/plan/switch`   | `lineService.switchPlan(dto)` | W-LIN-03 | ✅ 既有 |
| 12| `GET`    | `/panel`       | `GET    /web/line/panel`         | `lineService.planPanel(query)` | W-LIN-03 | ✅ 既有 |
| 13| `GET`    | `/status`      | `GET    /web/line/status`        | `lineService.planStatus(query)` | W-LIN-03 | ✅ 既有 |
| 14| `GET`    | `/plan/manage` | `GET    /web/line/plan/manage`   | （stub — `lineService.planPanelListPage` 待补） | W-LIN-05 新增（PSM 无对应） | ⚠️ **W-LIN-05 新增 stub** |

**endpoint 数**：14 个（11 个 W-LIN-03 既有 + 3 个 W-LIN-05 新增，其中 2 个是 stub 联通升级 + 1 个真新 stub）。

### 关键 endpoint 说明

#### `GET /web/line/tree-search`（W-LIN-05 新增；PSM 等价 `GET /group`）

- **路径选择**：PSM `LineController.lineGroup` 在原代码中用 `@GetMapping({"/group"})`。工单约定新路径 `/tree-search`，与既有 `/tree` 路径风格一致（动词性更强）。
- **调用**：`return this.lineService.lineGroup();`
- **返回**：`BaseResult.data(List<Line>)`，仅含 `name` + `lineNo` 字段的去重 line 列表（PSM QueryWrapper.select 投影）。
- **与 `/tree` 的区别**：
  - `/tree` → `handleLineTreeSearch()`：按 lineNo 分组的父子树结构（`List<LineTreeItemDTO>`），子节点 lineNo = faceNo
  - `/tree-search` → `lineGroup()`：纯 distinct name+lineNo 列表（`List<Line>`）
  - 两者并存，各自对应 PSM 不同业务语义

#### `POST /web/line/chg-line-order`（W-LIN-05 新增；PSM 等价 `PUT /order`）

- **路径选择**：工单约定新路径 `/chg-line-order`（kebab-case 风格），与既有 `POST /plan/bind` 等风格一致。HTTP method 改为 `POST`（工单约定），与 PSM 原 `PUT /order` 不一致，但 service 层方法不变，路由各异。
- **共存**：`PUT /order`（PSM 原路由）保留并联通（不再是 stub）；`POST /chg-line-order`（新路由）并行联通。两者调用同一个 `chgLineOrder(List)` service 方法。
- **调用**：`return this.lineService.chgLineOrder(lineOrders);`
- **返回**：`BaseResult.ok()` / `BaseResult.error("20209")` / `BaseResult.error("20210")`

#### `GET /web/line/plan/manage`（W-LIN-05 新增；PSM 无对应，stub）

- **路由**：DataupLoad 自定义，PSM 反编译中无对应路由。
- **工单说明**：调用 "planPanel 的 listPage 版本"，但 DataupLoad 当前 `ILineService` 接口未声明分页版本的 planPanel 方法；本工单约束不新增 service 类/方法，故以 stub 形式路由。
- **当前行为**：返回 `BaseResult.code(90003).msgBody("W-LIN-05 pending: ILineService.planPanelListPage(LinePanelQueryDTO) not implemented yet").error()`，与 W-LIN-03 stub 风格一致。
- **后续工单**：需在 `ILineService` 增加 `planPanelListPage(LinePanelQueryDTO) → BaseResult` 并实现分页逻辑。

### `@RequestParam` 命名规范

按工单要求"每个 `@RequestParam` 加 `name="..."` 属性"避免 javac `-parameters` 警告：
- `LineController.getByLineNo`：`@PathVariable(name = "lineNo") String lineNo`（W-LIN-03 既有）
- `LineController.delete`：`@RequestParam(name = "id") Integer id`（W-LIN-03 既有）

W-LIN-05 新增的 3 个 endpoint 全部用 `@RequestBody`（chg-line-order）或直接绑定 DTO（plan/manage），无需 `@RequestParam`，符合规范。`-parameters` 编译复查：0 警告。

## 编译结果

### 1. 针对性编译（service 层）

```powershell
PS E:\DEMO\数据采集> & "E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe" -encoding UTF-8 -parameters -d "X:\DataupLoad\target\classes" -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" -sourcepath "DataupLoad\src\main\java" "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\ILineService.java" "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\impl\LineServiceImpl.java"
EXIT=0
```

### 2. 针对性编译（controller）

```powershell
PS E:\DEMO\数据采集> & "E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe" -encoding UTF-8 -parameters -d "X:\DataupLoad\target\classes" -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" -sourcepath "DataupLoad\src\main\java" "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\web\LineController.java"
EXIT=0
```

### 3. 全量编译（183 个 Java 文件）

```powershell
PS E:\DEMO\数据采集> & "E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe" -encoding UTF-8 -parameters -d "X:\DataupLoad\target\classes" -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" -sourcepath "DataupLoad\src\main\java" <all 183 *.java files>
EXIT=0
```

仅 1 条警告：标准 `未经检查或不安全的操作` 注记（generic-erased 代码固有，非新增），与 W-LIN-01 基线一致。

### 4. javap 反射验证

#### ILineService 接口（4 个新方法签名）

```
public abstract BaseResult lineGroup();
public abstract BaseResult chgLineOrder(List<ChgLineOrderDTO>);
public abstract BaseResult handleLineTreeSearch();
public abstract List<Line> listByLineNo(List<String>);
```

#### LineServiceImpl 实现（4 个新方法签名）

```
public BaseResult lineGroup();
public BaseResult chgLineOrder(List<ChgLineOrderDTO>);
public BaseResult handleLineTreeSearch();
public List<Line> listByLineNo(List<String>);   // 重载，与既有 listByLineNo(String) 并存
```

#### LineController（14 个 endpoint 方法）

```
public BaseResult listAll(PageQuery);
public BaseResult getByLineNo(String);
public BaseResult add(LineBodyDTO);
public BaseResult modify(LineUpdateDTO);
public BaseResult delete(Integer);
public BaseResult chgLineOrder(List<ChgLineOrderDTO>);         // PUT /order（联通）
public BaseResult chgLineOrderKebab(List<ChgLineOrderDTO>);    // POST /chg-line-order（新增）
public BaseResult searchLineTree();                            // GET /tree（联通）
public BaseResult treeSearch();                                // GET /tree-search（新增）
public BaseResult dispatchSolution(LinePlanBindDTO);
public BaseResult switchSolution(LinePlanSwitchDTO);
public BaseResult planPanel(LinePanelQueryDTO);
public BaseResult planStatus(LinePanelQueryDTO);
public BaseResult planManage(LinePanelQueryDTO);               // GET /plan/manage（stub）
```

### 5. -Xlint:all 复查

```powershell
PS E:\DEMO\数据采集> & javac -encoding UTF-8 -parameters -Xlint:all ... <W-LIN-05 三个文件>
EXIT=0（11 个警告）
```

警告详情：
- 1 条：Spring annotation processor 缺失（"没有处理程序要使用以下任何注释..."）— 与本工单无关
- 10 条：mybatis-plus-extension-3.5.3.jar 中 kotlin Metadata 警告 — 来自第三方 jar，与本工单无关

**0 条 `-parameters` 警告** — 确认 `@RequestParam(name="...")` 规范执行到位。

### 6. 编译产物

```
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\service\ILineService.class       2,581 bytes
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\service\impl\LineServiceImpl.class 27,382 bytes
X:\DataupLoad\target\classes\com\hikrobotics\solution\module\line\web\LineController.class         4,806 bytes
```

## 已知限制

### 限制 1 — `GET /plan/manage` 是 stub（PSM 无对应 service 方法）

工单要求 `GET /web/line/plan/manage` 调用 "planPanel 的 listPage 版本"，但：
- DataupLoad 当前 `ILineService` 接口未声明分页版本的 planPanel 方法
- 本工单（W-LIN-05）约束不新增 service 类/方法
- PSM 反编译中无对应 endpoint

**当前实现**：路由已注册（`@GetMapping("/plan/manage")`），方法体返回 `BaseResult.code(90003).error()` + 文案 `"W-LIN-05 pending: ILineService.planPanelListPage(LinePanelQueryDTO) not implemented yet"`。

**后续工单**：
1. 在 `ILineService` 增加 `BaseResult planPanelListPage(LinePanelQueryDTO)` 方法声明
2. 在 `LineServiceImpl` 实现该方法（基于 `LinePanelQueryDTO` 的分页参数 + 5 个数据集合的分页聚合；可参考 `planPanel` 但改成 `IPage<LinePanelDTO>`）
3. 把 `LineController.planManage` 方法体改为 `return this.lineService.planPanelListPage(linePanelQueryDTO);`

### 限制 2 — `Line` 实体与 `LineTreeItemDTO(LinePO)` 构造器的桥接

PSM `LineTreeItemDTO` 构造器签名是 `LineTreeItemDTO(LinePO po)`，DataupLoad `LineServiceImpl` 操作的是 `Line` 实体（不是 `LinePO`）。本工单的桥接方案：
```java
new LineTreeItemDTO(BeanUtil.copyProperties(line, LinePO.class))
```

- **影响**：每次 `handleLineTreeSearch` 调用会做 N 次 copyProperties（N = line 数）。N 通常很小（10~100 量级），无性能瓶颈。
- **替代方案**：给 `LineTreeItemDTO` 增加 `LineTreeItemDTO(Line line)` 重载构造器 — 但工单约束"不要新增/删除类"（即使是 DTO），且修改既有 DTO 构造器也可能影响其他调用方（虽然目前 `LineTreeItemDTO` 仅在 PSM 反编译里被 handleLineTreeSearch 使用），故不采用。
- **后续清理**：可考虑把 `LineTreeItemDTO` 构造器参数改成 `Line` 实体（DataupLoad 主用实体），把 `LinePO` 残留死代码（audit 已标记）一并清理。

### 限制 3 — `LinePO` 死代码暂未删除

`DataupLoad/.../line/model/LinePO.java`（audit 标记为"残留死代码"）在本工单中被 `handleLineTreeSearch` 间接复用（作为 `LineTreeItemDTO` 构造器入参的载体类型）。删除 `LinePO` 需要同时：
- 修改 `LineTreeItemDTO(LinePO)` → `LineTreeItemDTO(Line)`（修改 DTO，可能有影响）
- 或给 `LineTreeItemDTO` 增加 Line 重载（新增 DTO 构造器）
- 或彻底改写 `handleLineTreeSearch` 不用 LineTreeItemDTO

这些动作超出 W-LIN-05 范围，留待后续 `LineMapper / LinePO` 清理工单一并处理。

### 限制 4 — `chgLineOrder` / `lineGroup` 的入参空指针保护

PSM `chgLineOrder(List)` 直接 `lineOrders.size()`，如果调用方传 `null` 会 NPE。DataupLoad 沿用 PSM 语义，不做空指针保护。**调用方必须传非空 List**（前端/调用层校验或传空列表 `[]`）。

类似地，`lineGroup()` 直接 `selectList(QueryWrapper)`，QueryWrapper 本身非空，但 select 字符串如果大小写错误（"name" vs "NAME"）可能在 PG 上行为不同（PG 默认小写敏感）。DataupLoad 沿用 PSM 大写 `NAME` 字符串。

### 限制 5 — `listByLineNo(String)` 与 `listByLineNo(List<String>)` 重载并存

W-B03 已声明 `listByLineNo(String lineNo)`，W-LIN-05 新增 `listByLineNo(List<String> lineNos)`。两者通过参数类型区分，Java 按类型分派无歧义。但 IDE 提示或代码 review 时可能产生困惑。**调用方必须显式指定参数类型**（单值或 List），不能依赖隐式转换。

接口 `ILineService` 同时声明两个重载：
```java
List<Line> listByLineNo(String lineNo);          // W-B03
List<Line> listByLineNo(List<String> lineNos);   // W-LIN-05
```

实现类同时实现两个，方法体各自独立。

### 限制 6 — 工单描述中的 `lineGroup()` 签名与 PSM 不一致

工单描述中 `lineGroup()` 标注返回 `Map<String, List<Line>>`，但 PSM 实际返回 `BaseResult`（data 为 `List<LinePO>` 投影）。本工单以 PSM 1:1 为准 — 实现返回 `BaseResult.data(List<Line>)`。如果调用方期望 `Map`，需要在 controller 层做转换（不在本工单范围）。

类似地，工单描述中 `handleLineTreeSearch(String)` 标注返回 `Map`，但 PSM 实际返回 `BaseResult`（无参）。本工单以 PSM 1:1 为准 — 实现为 `handleLineTreeSearch()` 无参 + 返回 `BaseResult.data(List<LineTreeItemDTO>)`。

### 限制 7 — 工单描述的"调用 planPanel 的 listPage 版本"无对应 service 方法

工单描述中 `GET /plan/manage` 应调用 "planPanel 的 listPage 版本"，但 PSM 和 DataupLoad 都没有该方法。如限制 1 所述，本工单以 stub 实现。后续工单需补齐 `planPanelListPage` service 方法。

### 限制 8 — `handleLineTreeSearch` 的子节点 `lineNo` 是 `faceNo`（PSM 业务怪癖）

PSM 业务逻辑（1:1 沿用）：父节点的 `lineNo = po.getLineNo()`，子节点的 `lineNo = po.getFaceNo()`。这是 PSM 反编译中的"业务怪癖"（子树区分父/子时把子节点 lineNo 替换为 faceNo），本工单保持原样。如果前端解析逻辑依赖 `lineNo` 是真正的 lineNo 而非 faceNo，需要在 `LineTreeItemDTO` 或前端层做额外处理。

### 限制 9 — 未触碰其他模块

按工单约束"不要修改其它模块"，本工单仅改动 `module.line` 包下的 3 个文件。未触碰：
- `module.alarm`（`AlarmRecordServiceImpl.dealClientAlarm`）
- `module.detect`（`IStatusRecordService.searchClientStatus`）
- `module.defect`（`ILineDefectTypeService.listIfShowEnable`）
- DTO 层（`LineTreeItemDTO` / `ChgLineOrderDTO` 等）

## 自检清单

- [x] 4 个 service 方法 1:1 对齐 PSM 反编译产物
- [x] 3 个 controller endpoint 全部新增（`tree-search` / `chg-line-order` / `plan/manage`）
- [x] 2 个 W-LIN-03 stub 联通升级（`PUT /order` / `GET /tree`）
- [x] 复用 W-LIN-01 已注入的 12 个 bean，未新增 bean 注入
- [x] DTO 全部复用 DataupLoad 已有类型（`ChgLineOrderDTO` / `LinePanelQueryDTO` / `LineTreeItemDTO`）
- [x] `@RequestParam` / `@PathVariable` 全部带 `name="..."` 属性（避免 javac `-parameters` 警告）
- [x] `javac` 编译退出码 0
- [x] `javap` 验证产物含全部目标签名（service + controller）
- [x] `-Xlint:all` 复查：0 条 `-parameters` 警告（11 条均为 3rd party / annotation processor 噪音）
- [x] 全量项目编译（183 个 Java 文件）通过，与 W-LIN-01 基线一致
- [x] 仅修改 `module.line` 包下的 3 个文件（接口 + impl + controller）
- [x] 未推 git

## 总结

- ✅ 4 个 service 方法 + 3 个 controller endpoint 全部按 PSM 反编译产物 1:1 对齐（或 stub 兜底）
- ✅ W-LIN-03 的 2 个 stub（`PUT /order` / `GET /tree`）已联通，service 层方法可直接调用
- ✅ `Line` 实体与 `LinePO` 死代码通过 `BeanUtil.copyProperties` 桥接，安全无侵入
- ✅ `javac` 编译退出码 0，无 `-parameters` 警告
- ⚠️ `GET /plan/manage` 是 stub（PSM 无对应；`planPanelListPage` service 方法待补）
- ⚠️ `LinePO` 死代码暂未删除（被 `handleLineTreeSearch` 间接复用）
- ❌ 不修改其他模块（符合工单约束）
- ❌ 不推 git（符合工单约束）
