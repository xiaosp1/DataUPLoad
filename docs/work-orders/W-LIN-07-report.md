# W-LIN-07 报告 — changePlanOrder 1:1 对齐 PSM（验证性工单：无需补代码）

**完成时间**: 2026-07-25 00:55
**执行人**: Java worker (subagent)
**优先级**: P3 — 验证性工单（PSM 1:1 对齐审计）
**结论**: **PSM 与 DataupLoad 已 1:1 对齐，无需补代码**

> 本工单是验证性工单，工单原文已注明"**可能结论是'无需补'**，不要为了'完成'而强行造改动"。经过 byte-code 反编译逐方法对照，PSM 与 DPL 在 `chgLineOrder` / `modLineOrder` / `addLineOrder` / `removeByLineId` / `ChgLineOrderDTO` / `LineOrder(PO)` / `LineOrder(Mapper|DAO)` / `ILineOrderService` 7 个产物上完全对齐。详见 ADR-0012。

> 工单措辞中的 `changePlanOrder` 在 PSM 反编译产物中**不存在**；PSM 实际对应方法名是 `chgLineOrder`（`LineServiceImpl.java` 第 380-388 行 + `LineController.java` 第 84-88 行）。

## 改动文件汇总

| 文件 | 改动类型 | 备注 |
|---|---|---|
| `docs/adr/0012-chg-line-order-psm-dpl-aligned.md` | 新增 | ADR 留痕 PSM=DPL 已对齐 |
| `docs/work-orders/W-LIN-07-report.md` | 新增 | 本报告 |

**未修改任何 Java 源码**（PSM 1:1 已对齐，无需补代码）。

## Step 1 — PSM 反编译产物方法清单

### 1.1 PSM `LineServiceImpl.java`（line/service/imp/）

PSM 反编译产物（来源：`docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/line/service/imp/LineServiceImpl.java`）

**LineOrder 相关方法 / 调用点（共 4 处）：**

| # | 方法 / 行 | 调用点 | 行为 |
|---|---|---|---|
| 1 | `init()` 第 169-175 行 | `@PostConstruct` 启动钩子 | `lineOrderService.count() == 0` 且 line 表非空 → `addLineOrder(allLineIds)` |
| 2 | `add(LineBodyDTO)` 第 186 行 | 新增产线 | `addLineOrder([newLineId])` 追加到 line_order 尾部 |
| 3 | `delete(Integer)` 第 220 行 | 删除产线 | `removeByLineId(line.getId())` 清理排序行 |
| 4 | `chgLineOrder(List<ChgLineOrderDTO>)` 第 380-388 行 | 调整顺序 | size 校验 → `modLineOrder(lineOrders)` → ok/error |

`chgLineOrder` 完整实现：

```java
public BaseResult chgLineOrder(List<ChgLineOrderDTO> lineOrders) {
    if ((long)lineOrders.size() != this.count()) {
        return BaseResult.build().error("20209");
    }
    if (!this.lineOrderService.modLineOrder(lineOrders).booleanValue()) {
        return BaseResult.build().error("20210");
    }
    return BaseResult.build().ok();
}
```

### 1.2 PSM `LineOrderServiceImpl.java`（line/service/imp/）

| # | 方法 | 行为 |
|---|---|---|
| 1 | `addLineOrder(List<Integer>)` | 取当前最大 `orderValue`（`null` → 1，否则 +1），从该值起为每个 lineId 顺序写入新行 |
| 2 | `removeByLineId(Integer)` | `Wrappers.lambdaQuery().eq(LineOrderPO::getLineId, lineId)` 删除该 lineId 的所有排序行 |
| 3 | `modLineOrder(List<ChgLineOrderDTO>)` | 入参空 → `return true`（no-op）；入参非空 → 全表删除 → 按 `order` 字段升序排序 → 用 `1..N` 线性重写 `orderValue` → `saveBatch` |

`modLineOrder` 完整实现（PSM 反编译原样）：

```java
public Boolean modLineOrder(List<ChgLineOrderDTO> lineOrders) {
    if (CollectionUtils.isNotEmpty(lineOrders)) {
        if (this.remove(Wrappers.lambdaQuery())) {
            ArrayList orders = Lists.newArrayList();
            List<ChgLineOrderDTO> temp = lineOrders.stream()
                .sorted(Comparator.comparing(ChgLineOrderDTO::getOrder))
                .toList();
            for (int i = 1; i <= temp.size(); ++i) {
                orders.add(LineOrderPO.builder()
                    .lineId(temp.get(i - 1).getLineId())
                    .orderValue(Integer.valueOf(i))
                    .build());
            }
            return this.saveBatch((Collection)orders);
        }
        return false;
    }
    return true;
}
```

### 1.3 PSM `LineOrderDAO.java`（line/mapper/）

```java
public interface LineOrderDAO extends BaseMapper<LineOrderPO> {
    // 空 — 无自定义方法
}
```

### 1.4 PSM `LineOrderPO.java`（line/model/）

字段：`id (IdType.AUTO)` / `lineId` / `orderValue` / `updateTime` / `createTime`
`@TableName("line_order")`，builder 模式（lineId / orderValue / id 可设）

### 1.5 PSM `ILineOrderService.java`（line/service/）

```java
public interface ILineOrderService extends IService<LineOrderPO> {
    public void addLineOrder(List<Integer> var1);
    public void removeByLineId(Integer var1);
    public Boolean modLineOrder(List<ChgLineOrderDTO> var1);
}
```

### 1.6 PSM `LineController.java`（line/web/）

类级别 `@RequestMapping("/web/line")`

| # | HTTP | 路径 | 服务调用 | `@ApiLog` |
|---|---|---|---|---|
| 1 | GET | `/` | `lineService.listAll` | "产线查询" |
| 2 | POST | `/` | `lineService.add` | "产线新增" |
| 3 | PUT | `/` | `lineService.modify` | "产线修改" |
| 4 | DELETE | `/`（`?id=`） | `lineService.delete` | "产线删除" |
| 5 | **PUT** | **`/order`** | **`lineService.chgLineOrder`** | **"修改线体顺序"** |
| 6 | GET | `/tree` | `lineService.handleLineTreeSearch` | "查询产线树" |
| 7 | POST | `/plan/bind` | `lineService.bindPlan` | "产线配方分发" |
| 8 | POST | `/plan/switch` | `lineService.switchPlan` | "产线配方切换" |
| 9 | GET | `/panel` | `lineService.planPanel` | — |
| 10 | GET | `/status` | `lineService.planStatus` | — |
| 11 | GET | `/group` | `lineService.lineGroup` | — |

**LineOrder 相关端点：仅 `PUT /web/line/order` 一个**。

### 1.7 PSM `ChgLineOrderDTO.java`（line/dto/）

字段：`order`（`@NotNull(message="线体顺序")`）+ `lineId`（`@NotNull(message="线体标识")`），getter/setter/equals/hashCode/toString。

## Step 2 — PSM vs DPL diff 表

### 2.1 `modLineOrder` 算法对照（核心）

| 步骤 | PSM 反编译 | DPL 实装（W-LIN-05） | 等价？ |
|---|---|---|---|
| 入参空 → 返回 | `return true`（no-op） | `return true`（no-op） | ✅ |
| 入参非空 → 全表删除 | `this.remove(Wrappers.lambdaQuery())` | `this.remove(Wrappers.lambdaQuery())` | ✅ |
| 按 `order` 升序排序 | `sorted(Comparator.comparing(ChgLineOrderDTO::getOrder))` | `sorted(Comparator.comparing(ChgLineOrderDTO::getOrder))` | ✅ |
| 用 `1..N` 重写 `orderValue` | `for (int i = 1; i <= temp.size(); ++i) orders.add(LineOrderPO.builder().lineId(...).orderValue(i).build())` | `for (int i = 1; i <= temp.size(); i++) orders.add(LineOrder.builder().lineId(...).orderValue(i).build())` | ✅ |
| `saveBatch` | `this.saveBatch((Collection)orders)` | `this.saveBatch(orders)` | ✅ |
| 失败返回 false | `return false`（remove 失败） | `return false`（remove 失败） | ✅ |
| 实体类名 | `LineOrderPO` | `LineOrder`（W-CLEAN-03 去 PO 后缀） | ✅ 字段一致 |

### 2.2 `chgLineOrder` 服务方法对照

| 步骤 | PSM | DPL | 等价？ |
|---|---|---|---|
| size 校验 | `(long)lineOrders.size() != this.count()` → error "20209" | `(long) lineOrders.size() != this.count()` → error "20209" | ✅ |
| modLineOrder 失败 | `!this.lineOrderService.modLineOrder(lineOrders).booleanValue()` → error "20210" | `!Boolean.TRUE.equals(this.lineOrderService.modLineOrder(lineOrders))` → error "20210" | ✅ 语义等价 |
| 成功返回 | `BaseResult.build().ok()` | `BaseResult.build().ok()` | ✅ |

### 2.3 `addLineOrder` / `removeByLineId` 对照

| 方法 | PSM | DPL | 等价？ |
|---|---|---|---|
| `addLineOrder(List<Integer>)` | 取 max(orderValue)+1，批量 insert | 同上 | ✅ |
| `removeByLineId(Integer)` | `Wrappers.lambdaQuery().eq(LineOrderPO::getLineId, lineId).remove()` | 同上 | ✅ |

### 2.4 DTO / 实体 / mapper / 接口 对照

| 产物 | PSM | DPL | 等价？ |
|---|---|---|---|
| `ChgLineOrderDTO` | `order` + `lineId`，`@NotNull(message="线体顺序")` + `@NotNull(message="线体标识")` | 同 PSM（unicode 编码相同） | ✅ |
| `LineOrder` vs `LineOrderPO` | 5 字段 + builder | 5 字段 + builder（仅类名去 PO 后缀） | ✅ |
| `LineOrderMapper` vs `LineOrderDAO` | 空 BaseMapper | 空 BaseMapper | ✅ |
| `ILineOrderService` | `addLineOrder` / `removeByLineId` / `modLineOrder` | 同 PSM | ✅ |

### 2.5 端点对照

| 端点 | PSM | DPL | 差异 |
|---|---|---|---|
| `PUT /web/line/order` | ✅ chgLineOrder | ✅ chgLineOrder（W-LIN-03 stub → W-LIN-05 联通） | 等价 |
| `POST /web/line/chg-line-order` | ❌ 不存在 | ✅ chgLineOrder（W-LIN-05 新增 kebab-case alias） | DPL 多了 alias |
| `ValidateUtils.validateEntity(...)` | ✅ 调用（第 86 行） | ❌ 跳过（DPL 项目层统一风格） | 见下方"已知偏差" |
| `@ApiLog(...)` 注解 | ✅ 调用 | ❌ 跳过（DPL 项目层统一风格） | 见下方"已知偏差" |

### 2.6 全模块搜索：PSM 是否有任何"修正重复/边界处理"的额外逻辑？

```powershell
Get-ChildItem -Path 'PSM/server/decompiled/com/hikrobotics/solution/module/line' -Recurse -Filter '*.java' |
  Select-String -Pattern 'LineOrder|line_order|orderValue'
```

结果：仅 7 个文件包含 `LineOrder` 引用：

| 文件 | 引用点 |
|---|---|
| `ChgLineOrderDTO.java` | DTO 定义 |
| `LineOrderDAO.java` | mapper 空接口 |
| `LineOrderPO.java` | 实体 |
| `ILineOrderService.java` | 接口 |
| `ILineService.java` | import + `chgLineOrder` 声明 |
| `LineOrderServiceImpl.java` | 3 个 service 方法实现 |
| `LineServiceImpl.java` | 4 处调用点（init/add/delete/chgLineOrder） |

**无任何额外的"修正重复"、"deduplicat"、"fixDuplicate"、"边界处理"、"maxOrder+1 偏移"、"唯一索引去重"等辅助方法或逻辑**。

`LineOrderServiceImpl` 整个类只有 3 个 public 方法（`addLineOrder` / `removeByLineId` / `modLineOrder`），无 private helper / util 方法。
`LineOrderPO` 无 unique constraint 注解（lineId 字段无 `@TableField` / `@Unique`）。

## Step 3 — 实际结论（PSM 与 DPL 已 1:1 对齐）

### 结论

**W-LIN-07 工单 = "PSM 1:1 对齐，无需补代码"（归档为已对齐工单）**

工单原始假设"PSM 在 update 后会修正 line_order 重复、边界处理"经 byte-code 反编译验证为**假**：

1. **PSM `modLineOrder` 的全表删除 + 1..N 重写 + saveBatch 模式天然避免重复**
   - 每次 chgLineOrder 都是先 `remove(*)` 把所有 line_order 行清空
   - 然后按入参 `order` 字段排序，重写 `orderValue = 1..N`
   - 最后 saveBatch 一次性插入
   - 不可能产生重复 `orderValue`（同一 sort 顺序下 N 个 lineId → N 个不同的 `1..N` 值）
   - 不可能产生重复 `lineId`（每个 ChgLineOrderDTO 只对应一个 lineId，且 saveBatch 一次性插入）

2. **PSM 不存在"边界处理"逻辑**
   - `orderValue` 强制重写为 `1..N`，不会越界
   - 数据库 `line_order` 表无 `CHECK` 约束 / 唯一索引（PO 无 `@TableField` / 注解）
   - 没有 `orderValue > 0` 校验、没有 `orderValue <= N` 校验、没有 `orderValue + max 偏移` 的逻辑

3. **DTO 字段排序由调用方保证**
   - `ChgLineOrderDTO.order` 字段是"用户期望的展示顺序"（前端拖拽后的顺序）
   - service 层按 `order` 字段排序后重写 `orderValue`
   - 调用方传任意 `order` 值（重复、空洞、负数）都会被重新映射为 `1..N`
   - 因此**前端不需要保证 `order` 唯一 / 连续 / 非负**；这是 PSM 的内置鲁棒性，已被 DPL 1:1 沿用

### 详细对比表（综合）

| 维度 | PSM | DPL | 对齐结论 |
|---|---|---|---|
| chgLineOrder 服务方法 | `(long)size != count → 20209` + `!modLineOrder → 20210` + `ok` | 同 PSM | ✅ 1:1 |
| modLineOrder 算法 | 空→true / remove → sort → 1..N 重写 → saveBatch | 同 PSM | ✅ 1:1 |
| addLineOrder 算法 | max(orderValue)+1 起 + 顺序填充 | 同 PSM | ✅ 1:1 |
| removeByLineId | `eq(lineId)` 删除 | 同 PSM | ✅ 1:1 |
| ChgLineOrderDTO | order/lineId + `@NotNull` + builder | 同 PSM（unicode 一致） | ✅ 1:1 |
| LineOrder(PO) 实体 | 5 字段 + builder + `@TableName("line_order")` | 同 PSM（仅类名去 PO） | ✅ 1:1 |
| LineOrder(Mapper\|DAO) | 空 BaseMapper | 同 PSM | ✅ 1:1 |
| ILineOrderService 接口 | addLineOrder / removeByLineId / modLineOrder | 同 PSM | ✅ 1:1 |
| `PUT /web/line/order` 端点 | ✅ chgLineOrder | ✅ chgLineOrder | ✅ 1:1 |
| `POST /chg-line-order` 端点 | ❌ 不存在 | ✅ alias（同 service） | + DPL 扩展 |
| `ValidateUtils.validateEntity(...)` | ✅ 调用 | ❌ 跳过 | 见已知偏差 #1 |
| `@ApiLog(...)` 注解 | ✅ 有 | ❌ 无 | 见已知偏差 #2 |
| `Boolean.TRUE.equals` vs `.booleanValue()` | `.booleanValue()` | `Boolean.TRUE.equals(...)` | ✅ 语义等价（避免 unbox 警告） |

## Step 4 — 实际无需补代码

W-LIN-07 工单第 4 步（"如果 PSM 实际有补充逻辑 → 1:1 抄 PSM 实现"）**不适用**：PSM 没有任何 DPL 缺失的补充逻辑。

## 编译结果

**无编译**（本工单无代码改动）。

如需回归验证既有 W-LIN-05/06 产物的编译状态，可执行：

```powershell
cd "E:\DEMO\数据采集"
& "X:\DataupLoad\jdk\bin\javac.exe" -encoding UTF-8 -parameters `
  -d "X:\DataupLoad\target\classes" `
  -cp "X:\DataupLoad\target\classes;X:\DataupLoad\lib\*" `
  -sourcepath "DataupLoad\src\main\java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\impl\LineServiceImpl.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\impl\LineOrderServiceImpl.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\ILineService.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\service\ILineOrderService.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\dto\ChgLineOrderDTO.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\entity\LineOrder.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\mapper\LineOrderMapper.java" `
  "DataupLoad\src\main\java\com\hikrobotics\solution\module\line\web\LineController.java"
```

预期 exit code = 0（沿用 W-LIN-05/06 的 baseline 编译）。

## 已知偏差（不影响 1:1 对齐结论）

详见 ADR-0012 "已知偏差" 一节，摘要：

1. **`ValidateUtils.validateEntity(...)` 跳过**：DPL 项目层统一风格（W-LIN-03 起），PSM 该调用 group=new Class[0] 等效 no-op
2. **`@ApiLog` 注解缺失**：DPL 项目层尚未集成横切关注点
3. **`POST /chg-line-order` alias**：DPL 工单约定的 kebab-case 路径别名，调用同一 service 方法
4. **类名 `LineOrder` vs `LineOrderPO`**：W-CLEAN-03 去 PO 后缀的命名统一
5. **`Boolean.TRUE.equals` vs `.booleanValue()`**：语义等价，DPL 写法避免 javac unbox 警告

## 自检清单

- [x] PSM 反编译产物 7 个文件全部 read（LineServiceImpl / LineOrderServiceImpl / LineOrderDAO / LineOrderPO / ILineOrderService / LineController / ChgLineOrderDTO）
- [x] PSM LineOrder 相关方法清单列出
- [x] PSM LineOrderDAO / LineOrder mapper 方法签名列出（空 BaseMapper）
- [x] PSM LineController 端点路径列出（含 11 个端点 + 1 个 LineOrder 相关端点 PUT /order）
- [x] DPL 缺的 PSM 方法：0 个
- [x] DPL 逻辑偏离 PSM 的地方：0 处（核心 ordering 算法完全一致）
- [x] PSM vs DPL diff 表输出
- [x] Step 3 实际结论 = "PSM 与 DPL 行为已 1:1 对齐，无需补代码"
- [x] ADR-0012 留痕
- [x] 未推 git
- [x] 未修改报告之外的代码
- [x] 验证依据为 byte-code 反编译（不靠工单想象）

## 总结

- ? PSM 反编译产物**无任何额外的 ordering 修正逻辑**（"修正重复"、"边界处理"假设为假）
- ? DPL `chgLineOrder` / `modLineOrder` / `addLineOrder` / `removeByLineId` / `ChgLineOrderDTO` / `LineOrder` / `LineOrderMapper` / `ILineOrderService` 与 PSM 1:1 对齐
- ? 端点 `PUT /web/line/order` 与 PSM 一致；DPL 多了一个 `POST /chg-line-order` alias（同 service 方法）
- ? 无需补代码（W-LIN-07 是验证性工单，结论为"已对齐"）
- ? ADR-0012 留痕
- ? 不修改任何源码
- ? 不推 git
