# ADR-0009 — AlarmRecordServiceImpl 扩展方法保留

**状态**: 已完成检查
**日期**: 2026-07-24 19:20 (W-X28 W-CLEAN-02 工单)
**决策人**: 锋卫

## 背景
W-AUDIT-01 审计报告 §四 清理工单提到 W-CLEAN-02: 检查 AlarmRecordService 死代码遗留。

实际检查结果：
- `IAlarmRecordService` 接口方法数: **10**
- `AlarmRecordServiceImpl` 实现方法数: **12**
- 差 2 个方法

## 决策
**2 个扩展方法是有意添加，不属于死代码，无需清理。**

## Impl 多出的 2 个方法
1. `public void dealClientAlarmListener(@EventListener DealAlarmEvent event)`
   - Spring `@EventListener` 异步监听器
   - 来源：W-X30b DealAlarmEvent 接入工单
   - 作用：去重逻辑（同一 reason 的报警只处理一次）

2. `public void sendAlarmTextMessage(String alarmText)`
   - WS 文本推送工具方法
   - 来源：W-X30a Controller 双推工单
   - 作用：把文本告警推送到大屏

## 接口差异处理
- 这 2 个方法**未在接口中声明**
- 因为它们是 Impl 内部使用（@EventListener 由 Spring 反射调用，sendAlarmTextMessage 是 private helper）
- 如果加到接口，会污染接口契约（业务无关）

## 验证
```bash
grep -rn "dealClientAlarmListener\|sendAlarmTextMessage" DataupLoad/src/main/java/
```
- `dealClientAlarmListener`: Spring 自动调用，0 处显式调用（正常）
- `sendAlarmTextMessage`: 在 Impl 内部 2 处调用，0 处外部调用（正常）

## 历史工单
- W-X30a: Controller 双推 — 引入 sendAlarmTextMessage
- W-X30b: DealAlarmEvent 接入 — 引入 dealClientAlarmListener
- W-X28 W-CLEAN-02: 检查确认无死代码（本文件留痕）
