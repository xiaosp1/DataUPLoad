# config 模块审计报告 (2026-07-24)

## 摘要
- F (功能完全对齐): **5** / 5 (核心)
- P (实现 stub): 0
- M (缺失): 0
- 真实对齐度: **100%**
- 另含 DataupLoad 自有 `TxConfig.java`（不在 PSM）

## 文件级判定

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| SystemConfigPO | F | 1:1 | PO→entity 命名 |
| SystemConfigMapper | F | DAO→Mapper | MyBatis-Plus BaseMapper |
| ISystemConfigService | F | 1:1 | — |
| SystemConfigServiceImpl | F | 1:1 | 实现完整 CRUD |
| SystemConfigController | F | 1:1 | `/web/system-config` GET/PUT |
| **TxConfig** (DataupLoad 自有) | — | 不在 PSM | DataupLoad 业务自定义的 @Configuration 类，事务管理 |

## 重点问题 Top 3

1. **`TxConfig.java` 不在 PSM** — DataupLoad 自加  
   建议补一行注释说明它是 DataupLoad 业务自加（PSM 未配事务管理类）

2. **`SystemConfigServiceImpl` 没看到 cache** — PSM 同款，无 cache  
   不是缺陷，符合 PSM 原版

3. **接口 `ISystemConfigService` 是否要加 `listConfigByKey`** — PSM 同款  
   当前实现已含 listAll，可满足前端调用需求

## 结论
config 模块**完全对齐 PSM**。`TxConfig` 是 DataupLoad 业务增强。
