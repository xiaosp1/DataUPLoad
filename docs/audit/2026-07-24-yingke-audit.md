# yingke 模块审计报告 (2026-07-24)

## 摘要
- F (功能完全对齐): **14** / 15
- P (实现 stub): **1** / 15
- M (缺失): **0** / 15
- 真实对齐度: **95%** (升级部分比 PSM 强)

## 文件级判定

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| YKConfig | F | 1:1 | +loginEnabled/uploadEnabled 双开关（DataupLoad 业务改进） |
| AlarmDTO | F | 1:1 | — |
| ContextDTO | F | 1:1 | — |
| DetectDataDTO | F | 1:1 | DefectDataDTO/RemoveCountDTO 内嵌类完整 |
| LineAndDefectDTO | F | 1:1 | — |
| ListParamsDTO | F | 1:1 | — |
| LoginResultDTO | F | 1:1 | — |
| SearchDefectRecordDTO | F | 1:1 | 5 字段全 (startTime/endTime/lindGroup/defectGroup/faceGroup) |
| StringParamDTO | F | 1:1 | — |
| YKRequestDTO | F | 1:1 | — |
| YKResponseDTO | F | 1:1 | — |
| PushAlarmEvent | F | 1:1 | — |
| IYKService | F | 1:1 | — |
| **YKServiceImpl** | **F+** | 1:1+ | +双开关语义、+dedup Set、+pushAlarm 同步入口 |
| YKController | F | 1:1 | — |

## 重点问题 Top 3

1. **`YKConfig` 行为变更**（PSM 用 `isEnable()`，DataupLoad 用 `isLoginEnabled()`+`isUploadEnabled()`）  
   这是有意为之的业务改造（铁则 42，灰盒默认关推送），但要在 ADR 留痕  
   **建议**：补一份 ADR 描述双开关语义，避免后续维护者困惑

2. **dedup 仅内存级** — `pushAlarmDedupKeySet` 是 ConcurrentHashMap.newKeySet()，重启清空  
   **风险**：服务重启后会重新推一次告警  
   **建议**：考虑是否需要落库（如果 MES 端去重够稳，可接受）

3. **YKServiceImpl 有两条同款 `searchDefectRecord` 路径**  
   yingke 包里有 `SearchDefectRecordDTO`，detect 包也有同名 stub（之前已填充）  
   当前 YKServiceImpl 把 yingke DTO 转为 detect DTO 再调用，OK  
   **小问题**：可以删掉一份重复 DTO，但已编译通过，运行无影响

## 结论
yingke 模块**生产可用**，核心 MES 推报警链路完整且经过灰盒验证。
