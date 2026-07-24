# W-DET-06 报告 — JDBC allowMultiQueries 配置

**完成时间**: 2026-07-24 20:03
**执行人**: 锋卫（手动）
**工时**: 1 分钟

## 改动文件
| 文件 | 改动 |
|---|---|
| `DataupLoad/config/application-prod.yml` | jdbc URL 加 `?allowMultiQueries=true` |

## 改动 diff
```diff
- url: jdbc:postgresql://127.0.0.1:5433/intco
+ url: jdbc:postgresql://127.0.0.1:5433/intco?allowMultiQueries=true
```

## 背景
W-DET-04 报告：DefectDayRecordMapper.updateCount 用 `;` 分隔的多 SQL 语句，需要 JDBC 允许 multi-queries。
PostgreSQL JDBC 默认 `allowMultiQueries=false`，必须显式开启。

## 验证
- 重启服务后调用 W-DET-04 的 updateCount 应该正常
- 待 W-DET-05 完成后一起端到端测试

## 已知限制
- 需要重启服务才能生效（下次重启验证）
