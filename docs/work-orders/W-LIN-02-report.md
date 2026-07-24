# W-LIN-02 报告 — StateStatistic 3 派生方法

**完成时间**: 2026-07-24 17:45
**执行人**: 锋卫（手动）
**工时**: 实际 5 分钟

## 改动文件

| 文件 | 改动 |
|---|---|
| `DataupLoad/.../line/entity/StateStatistic.java` | 新增 3 个派生方法 |

## 新增方法

### `getWorkShift()` — 班次
- PSM 原版：`hours >= 8 && hours < 20 ? "A班" : "B班"`
- DataupLoad 同款（1:1）

### `getOkRate()` — 良品率(%)
- PSM 用 `MathUtils.div(long, long, int)` 返回 double，再 `DecimalFormat("0.0")` 格式化
- DataupLoad 用纯 Java: `(double) okTime / total * 100`，除零保护 `total == 0 → "0.0"`
- **差异**：DataupLoad 的 MathUtils.div 签名是 `divide(int, int)` 不支持 long+scale，所以用纯实现替代

### `getErrorRate()` — 异常率(%)
- 同 getOkRate 模式

## 编译验证

```
✅ javac 0 errors
✅ Spring Boot 启动成功 (PID 11824, port 80)
```

## 服务验证
待 worker 完成后一起跑冒烟测试

## 已知限制
- 无（3 方法简单，无副作用）
