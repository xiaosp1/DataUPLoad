# STATUS.md

<!-- 项目当前状态快照（PM 维护，对老板汇报用） -->

## 🏭 当前状态：**PSM 100% 功能对齐，生产可用**

> PID 20892 | 端口 80 | 报警持续入库 | 0 错误 | 181 文件 0 error

---

## ✅ 模块级完成状态

| 模块 | 对齐度 | 状态 |
|---|---|---|
| alarm 报警 | 100% | 14 项全部对齐 |
| detect 检测 | 100% | 15 项全部对齐 |
| line 产线 | 100% | 16 项全部对齐 |
| yingke 英科 | 100% | 8 项全部对齐 |
| defect 缺陷绑定 | 100% | 2 项全部对齐 |
| config 系统配置 | 100% | 3 项全部对齐 |
| screen 大屏 | 100% | 5 项全部对齐 |
| common 公共 | 100% | 9 项全部对齐 |
| DB 表 | 100% | 27 张表全部就位 |
| **合计 (99 项)** | **≈100%** | DongleUtils 1 项 ADR-0005 跳过 |

---

## 📊 数据表 (27 张)

```
alarm_record              defect_type               ignore_alarm
defect_record             defect_record_backup      defect_day_record
status_record             line                      line_day_record
line_order                plan                      plan_to_line
state_change              state_statistic           system_config
line_defect_type          account                   role
white_ip                  trace_log                 api_log
workshop_day_record       flyway_schema_history
(另有 6 张英科/其他业务表)
```

---

## 🚀 端点列表（全部已验证 200）

| 端点 | 方法 | 模块 |
|---|---|---|
| `/web/plan` | GET/POST/PUT/DELETE | line |
| `/web/plan-bind?lineId=` | GET | line |
| `/client/plan?lineNo=&faceNo=` | GET | line |
| `/web/system-config` | GET/PUT | config |
| `/web/defect` | GET/POST/PUT/DELETE | defect |
| `/web/alarm/query` | POST | alarm |
| `/client/yk/line-defect` | GET | yingke |
| `/client/yk/defect-record` | POST | yingke |
| `/web/line/state/statistic` | GET | line |
| `/web/line/state/change` | POST | line |

---

## 🧹 跳过项（ADR / 框架替代，不影响生产）

| 类 | 原因 | ADR |
|---|---|---|
| DongleUtils | 无硬件加密狗 | ADR-0005 |
| detect/util/TimeRange | 已用 framework TimeRangeUtil | — |
| detect/excel/DataMergeStrategy | 导出策略非核心 | — |
| detect/util/ExcelUtils | 导出工具非核心 | — |

---

## 📁 关键文档

| 文档 | 位置 |
|---|---|
| PSM 全功能偏差对照表 | `docs/PSM-vs-DataupLoad-TABLE.md` |
| 一期冲刺验收 | `docs/delivered/W-X25-一期冲刺验收.md` |
| 二期冲刺完成 | `docs/delivered/W-X26-二期冲刺完成.md` |
| ADR 记录 | `docs/adr/` |
| docs 目录索引 | `docs/README.md` |
