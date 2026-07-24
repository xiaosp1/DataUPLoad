# W-CLEAN-02 报告 — AlarmRecordService 死代码检查

**完成时间**: 2026-07-24 19:20
**执行人**: 锋卫（手动）
**工时**: 5 分钟

## 改动文件
| 文件 | 改动 |
|---|---|
| `docs/adr/0009-alarm-service-extra-methods.md` | 新建（澄清 ADR） |

## 检查结果
- 接口 `IAlarmRecordService` 方法数: **10**
- 实现 `AlarmRecordServiceImpl` 方法数: **12**
- 差 2 个方法

## 2 个扩展方法（不是死代码）
1. `dealClientAlarmListener(@EventListener DealAlarmEvent)` — W-X30b 工单引入，Spring 自动调用
2. `sendAlarmTextMessage(String)` — W-X30a 工单引入，Impl 内部 helper

## 决策
**2 个扩展方法是有意添加，无需清理**

## 不上接口的理由
- 是 Impl 内部使用（Spring 反射 / private helper）
- 上接口会污染接口契约

## 已知限制
- 无
