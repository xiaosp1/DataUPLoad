# common 模块审计报告 (2026-07-24)

## 摘要
- F (功能完全对齐): **9** / 11
- P (实现 stub): **0** / 11
- M (缺失): **1** / 11 (DongleUtils ADR 跳过) + **1** 自加 (TxConfig)
- 真实对齐度: **90%** (排除 ADR 跳过项后 100%)

## 文件级判定

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| CommonMethod | F | 1:1 | 全部方法实现 |
| **CommonVariable** | F | 1:1 | PSM 仅 SERVICE_NAME 常量 |
| StateEnum | F | 1:1 | — |
| WsTypeEnum | F | 1:1 | — |
| **I18nConfig** | F | 1:1 | ResourceBundle getMessage |
| **JsonArrayTypeHandler** | F | 1:1 | PG JSON 数组 → 字符串（去掉 [ ] "） |
| **MathUtils** | F | 1:1 | round/divide/percent |
| ScheduleConfig | F | 1:1 | — |
| GlobalTaskManager | F | 1:1 | checkClientStatus + 大屏占位 |
| EnumUtil | F | 1:1 | — |
| ~~DongleUtils.java~~ | **M (ADR-0005)** | PSM 有 | **跳过**，无硬件加密狗 |
| **BaseResult / IdQuery** | DataupLoad 自有 | 不在 PSM | 在 `framework/common/base/`，框架提供 |

## 缺失类分析

| PSM 类 | 是否复制 | 原因 |
|---|---|---|
| `common/utils/DongleUtils.java` | ❌ | **ADR-0005**：项目无硬件加密狗，跳过 |

## DataupLoad 独有

| DataupLoad 类 | 来源 | 说明 |
|---|---|---|
| `framework/common/base/BaseResult.java` | 框架 | 统一响应格式 |
| `framework/common/query/IdQuery.java` | 框架 | ID 查询封装 |
| `config/TxConfig.java` | 业务 | 事务管理配置 |

## 重点问题 Top 3

1. **`CommonVariable` 仅有 SERVICE_NAME** — PSM 也只有这一个字段  
   不是缺陷，对齐 PSM 原版

2. **`JsonArrayTypeHandler` 简化实现** — PSM 同款，只剥 `[` `]` `"` 字符  
   **注意**：复杂嵌套 JSON 会出问题，但当前业务只用一维数组  
   **风险**：低，目前没有用到

3. **`ScheduleConfig` cron 表达式是 `0 0/5 * * * ?`** — PSM 同款  
   DataupLoad 跑起来后能正常 5 分钟跑一次，不是缺陷

## 结论
common 模块**完全对齐 PSM（除 ADR 跳过的 DongleUtils）**。框架内的 BaseResult/IdQuery 是 DataupLoad 业务框架的合理封装。
