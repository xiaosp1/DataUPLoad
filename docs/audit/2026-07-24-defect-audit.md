# defect 模块审计报告 (2026-07-24)

## 摘要
- F (功能完全对齐): **1** / 2 (DataupLoad 文件)
- P (实现 stub): **1** / 2 (LineDefectTypeService 实现简单)
- M (缺失): **4** / 6 (DAO/PO/Enum/ChangeLineDefectResult 缺)
- 真实对齐度: **30%** (此模块在 DataupLoad 不是主链路，仅基础 CRUD)

## 文件级判定

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| ILineDefectTypeService | F | 1:1 | — |
| LineDefectTypeServiceImpl | P | 实现简单 | listIfShowEnable 已对齐 PSM，但其他 CRUD 方法精简 |
| ~~ChangeLineDefectResult.java~~ | **M** | PSM 有 | 未复制 |
| ~~DefectTypeEnum.java~~ | **M** | PSM 有 | 未复制；DataupLoad 用 line 包下的 PlanStatusEnum 等枚举 |
| ~~LineDefectTypeDAO.java~~ | **M** | PSM 有 | 未复制；已用 Mapper 替代 |
| ~~LineDefectTypePO.java~~ | **M** | PSM 有 | 未复制；DataupLoad 用 line.entity.LineDefectType |

## 缺失的 PSM 类

| PSM 类 | 角色 | 影响 |
|---|---|---|
| `defect/dto/ChangeLineDefectResult.java` | 切换产线缺陷结果返回 | 当前未用，不阻塞 |
| `defect/constant/DefectTypeEnum.java` | 缺陷类型枚举 | detect 包下有 `DefectType.java` enum 已替代 |
| `defect/mapper/LineDefectTypeDAO.java` | MyBatis DAO | MyBatis-Plus BaseMapper 替代 |
| `defect/model/LineDefectTypePO.java` | 表映射 | DataupLoad 用 `line.entity.LineDefectType` 替代 |

## 重点问题 Top 3

1. **目录错位**：PSM 在 `module/defect/model/LineDefectTypePO`，DataupLoad 把 LineDefectType 放到了 `module/line/entity/`  
   **建议**：要么迁回 `module/defect/entity/`，要么在 ADR 里说明这是有意跨模块复用

2. **`LineDefectTypeServiceImpl` 实现不完整** — 只实现了 listIfShowEnable，缺少 save/update/delete  
   **风险**：如果前端调 CRUD 端点会失败  
   **建议**：补齐 4 个 CRUD 方法（基于 MyBatis-Plus ServiceImpl 即可）

3. **`ChangeLineDefectResult` 完全缺失** — 如果未来要支持"切换产线时换缺陷配置"会卡住  
   **建议**：暂不复制（当前不在二期内），但记录到 ADR-0006 跟踪

## 结论
defect 模块**当前不影响主链路**（line 服务可以引用 LineDefectType 完成查询），但**完整度仅 30%**。后续如果有产线切换/缺陷动态配置需求，需要补齐。
