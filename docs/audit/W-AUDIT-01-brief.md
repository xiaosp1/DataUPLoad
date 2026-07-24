# W-AUDIT-01 — DataupLoad 全模块审计工单

## 目标
对 DataupLoad 7 模块逐文件审计，对照 PSM 原版 169 个 Java 类，给出**真实功能对齐度**（不只是名字存在）。

## 现状扫描（命名层）
| 模块 | PSM 类 | DataupLoad 类 | 命名差异 | 需深入审计 |
|---|---|---|---|---|
| alarm | 35 | 39 | DAO→Mapper, PO→entity | ✅ |
| config | 5 | 5 | DAO→Mapper | ✅ |
| defect | 6 | 2 | DAO/PO/Enum 缺 | ✅ |
| detect | 37 | 33 | DAO→Mapper, PO→entity | ✅ |
| line | 54 | 61 | DAO→Mapper, PO→entity | ✅ |
| screen | 5 | 5 | — | ✅ |
| yingke | 15 | 15 | — | ✅ |
| common | 11 | 10 | — | ✅ |

## 审计输出格式

每个文件给出**一个判定**：

| 等级 | 含义 |
|---|---|
| ✅ **F** | 功能完全对齐 PSM（实现细节/字段一致） |
| 🟡 **P** | 文件存在但实现是 stub 或部分逻辑（未接业务） |
| ❌ **M** | 文件不存在或功能缺失 |
| ⚪ **N/A** | 不需审计（PO/DAO 命名差异已自动映射） |

## 审计结论示例
```markdown
## alarm/AlarmRecordService.java
- 等级：🟡 P
- 对比 PSM：AlarmRecordServiceImpl.java
- 缺失功能：
  - listClientStatus 没接
  - dealAlarm 没触发 ignore 规则
- 结论：服务跑起来能用，但逻辑只 60% 对齐
```

## 提交
每人一个 audit report，路径：
- `docs/audit/2026-07-24-alarm-audit.md`
- `docs/audit/2026-07-24-config-audit.md`
- `docs/audit/2026-07-24-defect-audit.md`
- `docs/audit/2026-07-24-detect-audit.md`
- `docs/audit/2026-07-24-line-audit.md`
- `docs/audit/2026-07-24-screen-audit.md`
- `docs/audit/2026-07-24-yingke-audit.md`
- `docs/audit/2026-07-24-common-audit.md`

最后汇总到 `docs/audit/2026-07-24-AUDIT-REPORT.md`。
