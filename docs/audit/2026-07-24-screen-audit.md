# screen 模块审计报告 (2026-07-24)

## 摘要
- F (功能完全对齐): **4** / 5
- P (实现 stub): **1** / 5 (微差)
- M (缺失): **0** / 5
- 真实对齐度: **90%**

## 文件级判定

| 文件 | 等级 | 对比 PSM | 关键差异 |
|---|---|---|---|
| ClientStatusDTO | F | 1:1 | — |
| DefectNumberDTO | F | 1:1 | — |
| ScreenDataDTO | F | 1:1 | 含内嵌 DetectDataDTO |
| IScreenService | F | 1:1 | — |
| **ScreenServiceImpl** | **F (微调)** | 1:1 | PSM 包 `imp`，DataupLoad 包 `impl`；一行行为差异 `lineStatusMap.put(...)` → `putIfAbsent` |

## 重点问题 Top 3

1. **ScreenServiceImpl 行为差异** — `getCilentStatusList` 用了 `putIfAbsent` 而非 `put`  
   PSM 用 `put` 会被覆盖，DataupLoad 用 `putIfAbsent` 第一次写入后保留  
   **影响**：若同一 (lineNo+faceNo) 有多条 StatusRecord，DataupLoad 只保留第一条的 Map entry，PSM 保留最后一条  
   **建议**：验证是否真的需要这个差异（大概率是 bug，应改回 `put`）

2. **`getCilentStatusList` 拼 key 字符串拼接不一致**  
   PSM: `status.getLineNo() + ":" + status.getFaceNo()` → 用 lineNo+faceNo  
   DataupLoad: `status.getLineNo() + ":" + status.getFaceNo()` → 同样  
   **结论**：实际行为一致  

3. **`screen` 模块无 Controller/Service 入口** — 仅 Service  
   PSM 也没有 Controller，是通过 `GlobalTaskManager` 定时调 `sendScreenDataInfo()` 推 WS  
   DataupLoad 同款实现，**OK**（不是缺陷）

## 结论
screen 模块核心 WS 推送逻辑完整。**一个微调需关注**：putIfAbsent vs put 应复测。
