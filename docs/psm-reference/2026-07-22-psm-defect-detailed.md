# PSM defect 模块功能块详细解析

**解析日期**: 2026-07-22
**Worker**: W-A21 Subagent
**状态**: ✅ 已归档
**优先级**: 🟢 P2（小模块，独立 PO + Service）

---

## 1. 业务定位

### 1.1 解决什么问题

defect 模块是 PSM 的**产线-缺陷类型绑定**管理：

- **产线-缺陷绑定**：`line_defect_type` 表（每条记录：lineNo + faceNo + 缺陷名 + showFlag）
- **缺陷类型字典**：定义三大类别 `DefectTypeEnum.BREAKAGE(1)/DIRTY(2)/OTHER(3)`
- **动态同步**：客户端上报数据时，自动同步 `line_defect_type`（`addDefectTypeIfNotExist`）
- **查询接口**：`listIfShowEnable(lineNo, faceNo)` → 仅返回 `showFlag=YES` 的缺陷

### 1.2 与其他模块的依赖关系

```
defect ──→ detect (DefectRecordServiceImpl.addDefectTypeIfNotExist)  # 数据上传时同步
defect ──→ line   (DefectCountDTO / LinePO)                         # 产线 + 缺陷 DTO
defect ──→ yingke (LineDefectTypeService listIfShowEnable)          # 字典查询
```

---

## 2. 类清单（6 个 java + 1 个 XML）

### 2.1 constant/ (1)
| 枚举 | 值 | 备注 |
|---|---|---|
| `DefectTypeEnum` | BREAKAGE(1) / DIRTY(2) / OTHER(3) | 缺陷大类 |

### 2.2 dto/ (1)
| 类 | 字段 |
|---|---|
| `ChangeLineDefectResult` | needDelDefects + needAddDefect |

### 2.3 mapper/ (1)
| DAO | 关键 XML |
|---|---|
| `LineDefectTypeDAO` | 默认 + `LineDefectTypeMapper.xml` |

### 2.4 model/ (1)
| PO | 表 | 字段 |
|---|---|---|
| `LineDefectTypePO` | `line_defect_type` | id/name/showFlag/lineNo/faceNo |

### 2.5 service/ (1) + service/imp/ (1)
| 接口 | 实现 | 责任 |
|---|---|---|
| `ILineDefectTypeService` | `LineDefectTypeServiceImpl` (76 行) | **🟢 P2** 核心 |

---

## 3. 核心流程

### 3.1 客户端上报数据时同步 line_defect_type

```
DetectDataController /client/data/detect
  │
  └─→ DefectRecordServiceImpl.handleDetectData(form)
        │
        └─→ if (defects != empty):
              └─→ lineDefectTypeService.addDefectTypeIfNotExist(line, defects)
                    │
                    ├─→ SELECT * FROM line_defect_type WHERE line_no=? AND face_no=?
                    │     → 已有缺陷 → Map<name, LineDefectTypePO>
                    │
                    ├─→ 遍历 form.defects：
                    │     ├─→ uploadDefectNames.add(defect.type)
                    │     └─→ value = existDefectsOfLine.getOrDefault(defect.type, new LineDefectTypePO())
                    │           .setShowFlag(defect.showFlag).setLineNo(...).setFaceNo(...).setName(...)
                    │           existDefectsOfLine.put(value.name, value)
                    │
                    ├─→ saveOrUpdateBatch(existDefectsOfLine.values())  // 已有更新 + 新增
                    │
                    └─→ 遍历 existDefectsOfLine：
                          if (!uploadDefectNames.contains(name)) {
                              needDelDefectId.add(defect.id);
                          }
                          // 删除客户端本次未上报的 defect
                          // ⚠️ 注意：这是基于"完整列表"假设的清理逻辑
                          │
                          if (!needDelDefectId.isEmpty()) {
                              removeBatchByIds(needDelDefectId)
                          }
```

**⚠️ BUG 风险**: 如果客户端本次只上传了部分 defect，PSM 会**误删其他已存在的 defect**。需要确认实际业务中客户端是否总是上传完整 defect 列表。

### 3.2 按产线查询启用的缺陷类型

```
LineDefectTypeServiceImpl.listIfShowEnable(lineNo, faceNo)
  │
  └─→ SELECT * FROM line_defect_type
        WHERE line_no=? AND face_no=? AND show_flag=YES
```

---

## 4. 关键类逐个解析

### 4.1 🟢 P2: `LineDefectTypeServiceImpl` (76 行)

**核心方法**:
```java
private List<LineDefectTypePO> listByLine(faceNo, lineNo)
    // 私有方法：按产线查询所有缺陷（未被外部调用）

@Override Boolean addDefectTypeIfNotExist(LinePO line, List<DefectCountDTO> defects)
    // 核心：客户端上报数据时同步 line_defect_type

@Override List<LineDefectTypePO> listIfShowEnable(String lineNo, String faceNo)
    // 只返回 showFlag=YES 的缺陷，用于统计聚合
```

**`addDefectTypeIfNotExist` 详细逻辑**:
```java
public Boolean addDefectTypeIfNotExist(LinePO line, List<DefectCountDTO> defects) {
    boolean result = true;
    if (CollectionUtils.isNotEmpty(defects)) {
        Map<String, LineDefectTypePO> existDefectsOfLine = Maps.newHashMap();
        // 1. 加载产线已有缺陷到 map
        LambdaQueryWrapper qw = Wrappers.lambdaQuery()
            .eq(LineDefectTypePO::getLineNo, line.lineNo)
            .eq(LineDefectTypePO::getFaceNo, line.faceNo);
        this.list(qw).forEach(defect -> existDefectsOfLine.put(defect.getName(), defect));
        
        // 2. 客户端上传的缺陷逐个处理
        List<String> uploadDefectNames = Lists.newArrayList();
        defects.forEach(defect -> {
            uploadDefectNames.add(defect.getType());
            LineDefectTypePO value = existDefectsOfLine.getOrDefault(defect.getType(), new LineDefectTypePO());
            value.setShowFlag(defect.getShowFlag())
                 .setLineNo(line.getLineNo())
                 .setFaceNo(line.getFaceNo())
                 .setName(defect.getType());
            existDefectsOfLine.put(value.getName(), value);  // 覆盖或新增
        });
        
        // 3. saveOrUpdateBatch（已有 update + 新增 insert）
        result = this.saveOrUpdateBatch(existDefectsOfLine.values());
        
        // 4. 清理：上传列表中没有的 defect 删除（⚠️ BUG 风险）
        if (result) {
            List<Integer> needDelDefectId = Lists.newArrayList();
            existDefectsOfLine.forEach((name, defect) -> {
                if (!uploadDefectNames.contains(name)) {
                    needDelDefectId.add(defect.getId());
                }
            });
            if (CollectionUtils.isNotEmpty(needDelDefectId)) {
                result = this.removeBatchByIds(needDelDefectId);
            }
        }
    }
    return result;
}
```

**⚠️ 关键 BUG**: 第 4 步的清理逻辑**不正确**：
- `existDefectsOfLine` 已经包含"已存在 + 新增"的混合数据
- 遍历这个 map 找 `!uploadDefectNames.contains(name)`，会把"已存在但本次没上传"的 defect 当作"需要删除"
- 这意味着：如果客户端只上传 1 个 defect，PSM 会删除产线其他所有 defect

**修复建议**: 清理逻辑应该基于"数据库原始查询结果"，而不是更新后的 map：
```java
// 修复：基于数据库原始数据判断需要删除的
List<LineDefectTypePO> originalRecords = this.list(qw);  // 原始查询
List<Integer> needDelDefectId = originalRecords.stream()
    .filter(d -> !uploadDefectNames.contains(d.getName()))
    .map(LineDefectTypePO::getId)
    .toList();
```

### 4.2 ⚪ P3: 其他类

- `ChangeLineDefectResult` — 简单的 DTO，无方法（看名字是"变更产线缺陷结果"，但目前没有调用方）
- `DefectTypeEnum` — 3 大类（破损/脏污/其他）

---

## 5. 数据库交互

### 5.1 涉及表（1 张）

| 表 | 用途 | 字段 | retention |
|---|---|---|---|
| `line_defect_type` | 产线-缺陷绑定 | id/name/showFlag/lineNo/faceNo | 无 |

### 5.2 索引推断

- `line_defect_type(line_no, face_no)` — 复合查询索引
- `line_defect_type(name, line_no, face_no)` UNIQUE — 防重复

---

## 6. 与 EdgeHost 对照

### 6.1 已对齐部分

| PSM | EdgeHost | W-A |
|---|---|---|
| `line_defect_type` 表 | ✅ EdgeHost 已有 | W-A17 |
| `LineDefectTypeServiceImpl.addDefectTypeIfNotExist` | ⚠️ 部分（V1.19 复刻）| W-A17 |

### 6.2 缺口

| PSM | EdgeHost 状态 | 移植优先级 |
|---|---|---|
| `listByLine` (私有方法) | 不需要 | ⚪ N/A |
| BUG 修复（清理逻辑）| 待复刻 | 🟡 P1 |

### 6.3 移植建议

**W-A22+ 复刻 `addDefectTypeIfNotExist` 时务必修复 BUG**：
- EdgeHost 1:1 翻译时直接抄逻辑会产生同样的 BUG
- 建议直接用修复版本（基于原始查询结果清理）

---

## 7. 风险 / 注意点

### 7.1 ⚠️ P1 BUG：清理逻辑错误（已在 §4.1 详述）

### 7.2 ⚠️ DefectTypeEnum 与 DefectType 重复

- `defect.DefectTypeEnum`：3 大类（BREAKAGE/DIRTY/OTHER）
- `detect.DefectType`：7 个具体缺陷名（底面破损/侧面破损等）
- 命名相似但语义不同，注意区分

### 7.3 ⚠️ listByLine 私有方法未使用

```java
private List<LineDefectTypePO> listByLine(String faceNo, String lineNo) {
    LambdaQueryWrapper<LineDefectTypePO> qw = ...
    return this.list(qw);
}
```

**私有方法，但逻辑与 `addDefectTypeIfNotExist` 内部重复**。可能是死代码，或被删除后残留。

### 7.4 showFlag 来源

`defect.getShowFlag()` 来自客户端上传的 `DefectCountDTO`（detect 模块 DTO）。客户端负责决定哪些 defect 在大屏上显示，PSM 端只同步。

### 7.5 缺陷名唯一性

`line_defect_type.name` 是缺陷类型字符串（如 "底面破损"），由 detect 模块的 `DefectType` 枚举约束。**没有外键约束**到 detect 模块的枚举，仅靠字符串匹配。

---

## 8. 总结

defect 模块是 PSM 的产线-缺陷绑定，P2 关注点：
1. **`LineDefectTypeServiceImpl.addDefectTypeIfNotExist`**：动态同步（V1.17 关键）
2. **`listIfShowEnable`**：大屏可见性过滤

关键风险：
- ⚠️ **清理逻辑 BUG**（客户端部分上报时误删其他缺陷）
- DefectTypeEnum 与 DefectType 命名相似易混淆
- 私有方法 `listByLine` 死代码
