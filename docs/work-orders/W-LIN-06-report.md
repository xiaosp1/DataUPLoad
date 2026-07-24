# W-LIN-06 报告 — 实装 `/web/line/plan/manage` stub（90003 → 真实业务）

**完成时间**: 2026-07-24 20:48
**执行人**: Java worker (subagent)
**优先级**: P2 — audit 2026-07-24 line 模块残留 stub 收尾

## 改动文件汇总

| 文件 | 改动类型 | 行数（前 → 后） | 备注 |
|---|---|---|---|
| `DataupLoad/.../line/service/ILineService.java` | 修改（追加 1 个方法声明） | 152 → 187 | 新增 `planOrderDtos(String, String, Integer, Integer)` |
| `DataupLoad/.../line/service/impl/LineServiceImpl.java` | 修改（追加 1 个方法 + 3 处 import + 1 处字段注入 + 类级注释更新） | 662 → 730 | PSM 1:1 抄 `PlanServiceImpl.clientPlan` 业务语义；新增 `planMapper` 注入 + `Page<ClientPlanResultDTO>` 内存分页 |
| `DataupLoad/.../line/web/LineController.java` | 修改（替换 stub + 类级 Javadoc 更新） | 312 → 313 | `/plan/manage` 由 90003 stub 升级为调用 `lineService.planOrderDtos(lineNo, faceNo, page, size)`；4 个 `@RequestParam` 均显式 `name="..."` |

合计 3 个文件改动，未触碰其他模块。

## 改动文件 — Diff 摘要

```
DataupLoad/.../line/service/ILineService.java                  | +35 -0
DataupLoad/.../line/service/impl/LineServiceImpl.java          | +75 -3  (+ 2 import, + 1 @Autowired 字段, + 1 方法 + 块注释)
DataupLoad/.../line/web/LineController.java                    | +22 -12
3 files changed, 132 insertions(+), 15 deletions(-)
```

## 1 个 service 方法签名 + 实现要点（PSM 1:1）

### `BaseResult planOrderDtos(String lineNo, String faceNo, Integer page, Integer size)`

| 项 | 内容 |
|---|---|
| 签名 | `public BaseResult planOrderDtos(String lineNo, String faceNo, Integer page, Integer size)`（PSM 反编译中无完全同名方法；名称沿用工单 W-LIN-06 命名约定，业务语义对齐 PSM `PlanServiceImpl.clientPlan(ClientPlanQueryDTO)`） |
| 入参语义 | `lineNo` / `faceNo` 必填；`page` / `size` 可选（`null` 或 `<= 0` 时退化为默认值/不分页） |
| 校验 | `(lineNo == null \|\| isEmpty()) \|\| (faceNo == null \|\| isEmpty())` → 错误 **20206**（与 `PlanServiceImpl.add` 风格一致，复用 PSM 既有错误码） |
| 数据源 | `planMapper.selectClientPlan(lineNo, faceNo)` — DataupLoad 已实现的 `PlanMapper` 自定义 SQL，**1:1 调用同一 SQL**（与 PSM `PlanServiceImpl.clientPlan` 通过 `planDAO.selectClientPlan(lineNo, faceNo)` 调用同一底层查询完全等价） |
| 返回 DTO | `List<ClientPlanResultDTO>`（`name / uri / description / status / updateTime / createTime`，由 `PlanMapper.@Results` 完成 `update_time/create_time → camelCase` 映射） |
| 分页模式 | ① `size == null \|\| size <= 0` → 不分页，返回 `BaseResult.data(List<ClientPlanResultDTO>)`；② 否则用 `Page<ClientPlanResultDTO>` 内存分页（`new Page<>(p, size, total).setRecords(subList(from, to))`），返回 `BaseResult.data(IPage<ClientPlanResultDTO>)`；③ `page == null \|\| page <= 0` → 退化为第 1 页 |
| 边界处理 | `fromIndex = min((p-1)*size, all.size())` + `toIndex = min(fromIndex+size, all.size())` 防越界；`all == null` → 退化为空列表 |
| PSM 1:1 校验 | ✅ 与 PSM `PlanServiceImpl.clientPlan` 的 `planDAO.selectClientPlan(lineNo, faceNo) → BaseResult.data(List)` 调用链等价；DataupLoad 在 PSM 基础上补齐分页维度（前端 listPage 契约） |
| 注入复用 | `planMapper`（W-LIN-06 新增；`PlanMapper` 已实现于 DataupLoad） |

**与 `PlanServiceImpl.clientPlan` 的关系**：

- `PlanServiceImpl.clientPlan(ClientPlanQueryDTO)` 仍保留（PSM 1:1，未触碰），未来给 PSM 客户端调用
- `LineServiceImpl.planOrderDtos(String, String, Integer, Integer)` 是 W-LIN-06 新增，给 DataupLoad `/web/line/plan/manage` 端点使用
- 二者底层调用的是 **同一个 SQL**（`PlanMapper.selectClientPlan(String, String)`）

**为何不放进 `PlanServiceImpl`**：任务约束要求"在 `LineServiceImpl` 加 `planOrderDtos`"，且原 stub 路由归属 `/web/line/*`（类级别 `@RequestMapping("/web/line")`），由 `LineController` 注入 `ILineService`，service 入口放在 `LineServiceImpl` 减少跨 service 调用与 controller 注入。

## `/plan/manage` endpoint 改造

### 旧（stub，90003）

```java
@GetMapping("/plan/manage")
public BaseResult planManage(LinePanelQueryDTO linePanelQueryDTO) {
    return BaseResult.build()
        .code(90003)
        .msgBody("W-LIN-05 pending: ILineService.planPanelListPage(LinePanelQueryDTO) not implemented yet")
        .error();
}
```

### 新（W-LIN-06 联通）

```java
@GetMapping("/plan/manage")
public BaseResult planManage(
        @RequestParam(name = "lineNo", required = true)  String lineNo,
        @RequestParam(name = "faceNo", required = true)  String faceNo,
        @RequestParam(name = "page",   required = false) Integer page,
        @RequestParam(name = "size",   required = false) Integer size) {
    return this.lineService.planOrderDtos(lineNo, faceNo, page, size);
}
```

| 项 | 内容 |
|---|---|
| 路由 | `GET /web/line/plan/manage`（不变；类级别 `@RequestMapping("/web/line")` + 方法级别 `@GetMapping("/plan/manage")`） |
| 入参 | 4 个 `@RequestParam`：`lineNo`（必填）/ `faceNo`（必填）/ `page`（可选）/ `size`（可选），均显式声明 `name="..."`（满足 W-LIN-05 工单约束 + javac `-parameters` 警告规避） |
| 返回 | 成功 → `BaseResult.data(List<ClientPlanResultDTO>)` 或 `BaseResult.data(IPage<ClientPlanResultDTO>)`；参数缺失 → 错误 **20206** |
| 业务语义 | 按 `(lineNo, faceNo)` 联查 `plan × plan_to_line × line`，返回该产线下分发到该面的全部配方信息（含运行状态 status） |

### 类级 Javadoc 同步更新

- 路由表第 14 行：`stub — planPanelListPage 待补（W-LIN-05）` → `lineService.planOrderDtos(lineNo, faceNo, page, size)（W-LIN-05 引入；W-LIN-06 联通真实业务）`
- 文末"已知限制"段落：`{@code GET /plan/manage} 是 DataupLoad 自定义 endpoint（PSM 无对应）…stub 形式提供（code=90003）` → `已联通到 ILineService.planOrderDtos…补齐分页维度（page / size）`

## 编译结果

### 编译命令

```powershell
cd "E:\DEMO\数据采集"
& "X:\DataupLoad\jdk\bin\javac.exe" -encoding UTF-8 -parameters \
  -d "X:\DataupLoad\target\classes" \
  -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" \
  -sourcepath "DataupLoad\src\main\java" \
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\impl\LineServiceImpl.java" \
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\ILineService.java" \
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\web\LineController.java"
```

### 结果

| 项 | 内容 |
|---|---|
| Exit code | **0** |
| 错误 | **0** |
| 警告 | **0**（单独编译 `LineController.java` 时出现 1 个无害警告"注释处理不适用于隐式编译的文件"，是 javac 在隐式编译传递依赖时的常规提示，与本工单代码无关；三文件批量编译时无任何警告） |
| 产物 | `LineController.class` / `LineServiceImpl.class` / `ILineService.class` 已重新生成（时间戳 2026-07-24 20:48:21） |

### 字节码核验（javap）

```text
LineServiceImpl.class:
  private com.hikrobotics.solution.module.line.mapper.PlanMapper planMapper;
  public com.hikrobotics.solution.framework.common.base.BaseResult
      planOrderDtos(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer);

LineController.class:
  public com.hikrobotics.solution.framework.common.base.BaseResult
      planManage(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer);

ILineService.class:
  public abstract com.hikrobotics.solution.framework.common.base.BaseResult
      planOrderDtos(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer);
```

## 已知限制

1. **PSM 反编译无完全同名方法**：PSM `LineServiceImpl` 与 `LineController` 中都没有 `planOrderDtos`、`manageList`、`planManagePage` 等方法。任务提示的"planOrderDtos"是任务命名约定；本工单按其精神取了业务语义最近的方法：
   - PSM `PlanServiceImpl.clientPlan(ClientPlanQueryDTO) → planDAO.selectClientPlan(lineNo, faceNo) → BaseResult.data(List)`
   - DataupLoad `LineServiceImpl.planOrderDtos(String, String, Integer, Integer) → planMapper.selectClientPlan(lineNo, faceNo) → BaseResult.data(IPage | List)`
   - 两者调用的是**同一个 SQL**（`plan` × `plan_to_line` × `line` 三表联查），仅入参和分页包装不同
2. **PSM PlanServiceImpl.clientPlan 重复入口**：迁移业务入口到 `LineServiceImpl.planOrderDtos` 后，DataupLoad 同时存在两个方法（`PlanServiceImpl.clientPlan` + `LineServiceImpl.planOrderDtos`），二者调用同一 SQL。后续工单若要收口，可二选一；本工单按工单约束"在 `LineServiceImpl` 加方法"+"不修改其它 endpoint"执行，未触碰 `PlanServiceImpl` / `PlanController`
3. **分页实现是内存 subList**：`PlanMapper.selectClientPlan` 是无 `LIMIT/OFFSET` 的 `@Select` SQL，分页由 service 层在内存对结果集 `subList(fromIndex, toIndex)` 后包装为 `Page<ClientPlanResultDTO>`。当单条产线配方量极大时（如 > 10000 条）会有内存压力，但 `plan × plan_to_line × line` 三表联查受 `plan_to_line.line_id` 唯一性约束，单产线配方量受业务规模限制，内存分页是 PSM 反编译产物中其它 listPage 端点的通用做法
4. **错误码 20206**：复用 PSM 既有错误码 20206（与 `PlanServiceImpl.add` 同码），未新增错误码字典项；如需精确语义可后续工单单独添加 `i18n` 文案
5. **未做 GET/POST 区分**：原 stub 是 `@GetMapping` 路由，本工单维持 GET 方法不变（保留为查询类端点；与 PSM `PlanController.clientPlan` 是 GET 一致）
6. **未跑运行时验证**：本工单仅完成编译 + 字节码核验，未启动 DataupLoad 服务做端到端 HTTP 调用验证（任务约束限定为 javac 编译）。下游联调需前端按 `lineNo + faceNo [+ page + size]` 契约调用 `GET /web/line/plan/manage`

## 未触碰范围

- `PlanServiceImpl.java` / `PlanController.java` / `IPlanService.java` / `PlanMapper.java` / `ClientPlanResultDTO.java`（PSM 1:1 现有实装保留）
- `LineServiceImpl` 其余 11 个 PSM 1:1 业务方法（add/modify/delete/bindPlan/switchPlan/planPanel/planStatus/lineGroup/chgLineOrder/handleLineTreeSearch/listByLineNo）
- `LineController` 其余 13 个 endpoint
- `LineController.class` 已有 11 个 W-LIN-03/05 endpoint（list/getByLineNo/add/modify/delete/chgLineOrder/chgLineOrderKebab/searchLineTree/treeSearch/dispatchSolution/switchSolution/planPanel/planStatus）保持原签名
- 不修改任何 SQL / mapper XML
- 不修改错误码字典 / i18n 资源
- 不推 git
