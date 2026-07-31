# 📚 docs/ — 项目文档目录

> 最后更新: 2026-07-25（W-DOC-01 文档对账）
> 项目: DataupLoad (PSM 反向工程重建)

---

## 📂 目录结构

```
docs/
├── README.md                          ← 本文件，目录导航
├── PSM-vs-DataupLoad-TABLE.md         ← PSM × DataupLoad 偏差对照表（最终版）
├── PSM-vs-DataupLoad-GAP.md           ← 差距分析（详细）
│
├── delivered/                         ← 交付验收记录
│   ├── W-X25-一期冲刺验收.md            2026-07-24 13:41-15:04
│   ├── W-X26-二期冲刺完成.md            2026-07-24 15:05-16:22
│   ├── W-X27-P0-工单冲刺完成.md         2026-07-24 17:42-18:42
│   ├── W-X28-P1-工单冲刺完成.md         2026-07-24 19:14-19:42
│   ├── W-X29-P2-工单冲刺完成.md         2026-07-24 20:02-21:10
│   ├── W-X30-清理验证冲刺完成.md        2026-07-24 21:28-22:43
│   └── 2026-07-23/                   ← W-X11~X24 历史交付
│
├── work-orders/                       ← 工单执行报告
│   ├── W-ALM-{01,02,03,05,06}-report.md
│   ├── W-DET-{01,02,03,04,05a,05b,05c,06,07,08}-report.md
│   ├── W-LIN-{01,02,03,04,05,06}-report.md
│   ├── W-DFT-{01a,01b}-report.md
│   ├── W-SCR-01-report.md
│   ├── W-YK-01-report.md
│   ├── W-FIX-{01,02}-report.md
│   └── W-CLEAN-{01,02,03}-report.md
│
├── dispatch/                          ← PM 派工记录
│   ├── W-X{27,28,29,30,31}-*-dispatch.md
│   └── W-{ALM,DET,LIN}-01-prompt.txt
│
├── adr/                               ← 架构决策记录（9 个）
│   ├── 0005-pg14-path-correction.md        ADR-0005
│   ├── 0005-psm-clone-new-project.md       项目基线决策
│   ├── 0006-screen-cache-strategy.md       ADR-0006
│   ├── 0007-yingke-dual-switch.md          ADR-0007
│   ├── 0008-line-po-alias-kept.md          ADR-0008（v2 反转）
│   ├── 0009-alarm-service-extra-methods.md ADR-0009
│   ├── 0010-change-line-defect-result-dto-only.md  ADR-0010
│   ├── 0011-alarm-sound-no-server-throttle.md      ADR-0011
│   └── W-X23-defect_type-seed.md           缺陷类型种子
│
├── audit/                             ← 审计报告
│   └── 2026-07-24/                   ← W-X24/X25 audit 产物
│
├── domain/                            ← 领域知识与逆向资料
│   ├── 海康大屏逆向/                   ← PSM 反编译产物（9000+ 文件）
│   ├── 海康视觉接口/                   ← 接口文档/原型
│   └── 英科医疗手套车间/                ← 业务场景文档
│       ├── 01-line-topology.md
│       ├── 02-data-flow.md
│       ├── 03-business-roles.md
│       └── README.md
│
├── psm-reference/                     ← PSM 模块级技术分析
├── tasks/                             ← 历史工单记录（早期）
├── SOP/                               ← 标准操作流程
└── _psm-read/                         ← PSM 阅读缓存（worker 临时）
```

---

## 🔑 核心文档

| 文档 | 定位 |
|---|---|
| `PSM-vs-DataupLoad-TABLE.md` | 最终对齐报告，**99%+**（DongleUtils ADR-0005 跳过）|
| `delivered/W-X25-一期冲刺验收.md` | 一期（13:41-15:04）|
| `delivered/W-X26-二期冲刺完成.md` | 二期（15:05-16:22）|
| `delivered/W-X27-P0-工单冲刺完成.md` | P0（17:42-18:42）|
| `delivered/W-X28-P1-工单冲刺完成.md` | P1（19:14-19:42）|
| `delivered/W-X29-P2-工单冲刺完成.md` | P2（20:02-21:10）|
| `delivered/W-X30-清理验证冲刺完成.md` | 清理验证（21:28-22:43）|
| `adr/0005~0011` | 架构决策记录（7 个 ADR + 2 个基线/种子）|
| `domain/英科医疗手套车间/` | 业务场景描述 |
| `psm-reference/` | 模块级逆向分析报告 |
| `audit/2026-07-24/` | W-X24/X25 全模块审计 |

---

## 📊 项目状态（snapshot @W-X30 完成 22:43）

```
编译:      186/186 .java 文件 ✅ (0 errors)
DB 表:     27/27 ✅
服务:      PID 9248, 端口 80, 0 ERROR
WS 推送:   /ws?uid=...&type=alarm 路径订正后正常
报警入库:   实时入库 ✅
Excel 导出: 1:1 PSM 返工完成（W-DET-08）
PSM 对齐:  99%+ (DongleUtils ADR-0005 跳过)
GitHub:    22 commits (main), 最新 5184380
```

**对齐度演变**:
- W-X28 后: 管理后台 95%+ 可用
- W-X29 后: 99%+，0 stub
- **W-X30 后**: 99%+，CFR 残留 0，导出 1:1 PSM ✅

---

## 🧠 ADR 一句话摘要

| ADR | 一句话 |
|-----|--------|
| 0005 | PG14 路径修正 + DongleUtils 跳过（无加密狗）|
| 0006 | screen cache 用 putIfAbsent 防覆盖竞态 |
| 0007 | yingke 登录/心跳双开关 |
| 0008 | LinePO 别名层删除（v2 反转 v1 保留决策）|
| 0009 | AlarmRecordService 扩展方法保留（兼容外部）|
| 0010 | ChangeLineDefectResult 仅 DTO，无 PO/表 |
| 0011 | alarm sound 服务端零节流（前端约定，PSM 设计）|
| W-X23 | defect_type 种子数据（PSM 字面）|

---

## 🏭 模块工单索引

| 模块 | 工单 |
|------|------|
| alarm | W-ALM-01/02/03/05/06 |
| detect | W-DET-01/02/03/04/05a/05b/05c/06/07/08 |
| line | W-LIN-01/02/03/04/05/06 |
| defect | W-DFT-01a/01b |
| screen | W-SCR-01 |
| yingke | W-YK-01 |
| common | W-FIX-01/02 + W-CLEAN-01/02/03 |

> 报告全部位于 `docs/work-orders/`

---

**联系人**: 锋卫 (AI PM) | **群组**: 数据采集重构 | **最近心跳**: 2026-07-24 22:43
