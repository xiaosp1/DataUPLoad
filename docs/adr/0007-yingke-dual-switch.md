# ADR-0007 — Yingke 双开关（loginEnabled / uploadEnabled）

**状态**: 已落地（代码已存在，ADR 留痕）
**日期**: 2026-07-24 19:18 (W-X28 W-YK-01 工单)
**决策人**: 锋卫

## 背景
PSM yingke 模块只有一个 `enabled` 总开关，控制所有 yk（MES）相关行为。
DataupLoad 在 W-X13d 工单时，引入双开关：
- `loginEnabled`: 是否调 MES AuthenticationController.Login 拿 ticket / 续约（凭证预热）
- `uploadEnabled`: 报警来了是否真推 yk 到 MES

代码位置：`DataupLoad/.../yingke/config/YKConfig.java`（已有 Javadoc 注释解释）

## 决策
**采用双开关**（已实现，W-X13d 落地），本 ADR 仅做正式留痕。

## 业务场景
| 场景 | loginEnabled | uploadEnabled | 用途 |
|------|--------------|---------------|------|
| **生产全开** | true | true | 默认正式环境，登录+推送 |
| **灰盒/测试** | true | false | 凭证预热OK但不推送（验证 ticket 拿得到） |
| **禁登录预热** | false | true | 复用已缓存的 ticket 但仍推数据 |
| **完全停用** | false | false | 调试期、压测期 |

## 兼容性
- 老字段 `enabled` getter 兼容语义：`loginEnabled || uploadEnabled` 任一为 true 即视为 true
- 老调用方代码不需修改
- 新代码请直接调 `isLoginEnabled()` / `isUploadEnabled()`

## 影响
- `YKServiceImpl.handleLineAndDefectSearch` 等方法改用 `isUploadEnabled()` 判断推送
- `YKServiceImpl.login()` 改用 `isLoginEnabled()` 判断凭证预热
- 灰盒默认 `loginEnabled=true, uploadEnabled=false`：ticket 预热、推送静默，正式上线改 `uploadEnabled=true`

## 配置文件位置
`DataupLoad/config/application.yml` 或 `application-prod.yml` 中 `hikrobotics.yk.*` 配置项

## 历史工单
- W-X13d: 引入双开关
- W-X28 W-YK-01: 正式 ADR 留痕（本文件）

## 备注
YKConfig.java 内的 Javadoc 已经写了完整解释，本 ADR 是从代码注释提取的正式版本。
未来任何人想简化回单开关，需要先讨论本 ADR。
