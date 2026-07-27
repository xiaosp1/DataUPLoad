# STATUS.md

<!-- 项目当前状态快照（PM 维护，对老板汇报用） -->
<!-- 最近一次大修: 2026-07-27 W-FRONT-00 V1 验收（PM 误诊修正 + 诊断脚本交付，等老板 Console 回填）-->

## 🏭 当前状态：**PSM 99%+ 功能对齐，生产可用；前端登录排查中（W-FRONT-00 V1 PASS）**

> PID 9248（snapshot @W-X30 完成 22:43）| 端口 80 | 实时报警入库 | 186 文件 compile 0 error
> GitHub: 22 commits (main) | 最近: W-FIX-02c `5184380`
> 跳过项: DongleUtils 1 项（ADR-0005，无硬件加密狗）
>
> **W-FRONT-00**（老前端 bundle 登录排查）：V1 验收 PASS（11:35）。Worker 出诊断脚本 + ADR-0017，**未预先 patch bundle**（按 brief「根因确定后再改」）。**卡点**：等老板跑 Console 脚本回填根因 → Worker 回填 ADR + 出最小化 patch。

---

## ✅ 模块级完成状态

| 模块 | 对齐度 | 关键工单 |
|------|--------|---------|
| alarm 报警 | 100% | W-ALM-01~06（WS 推送 / 过滤 / 路径订正） |
| detect 检测 | 100% | W-DET-01~08（导出 1:1 PSM 返工） |
| line 产线 | 100% | W-LIN-01~06（plan/manage 实装） |
| yingke 英科 | 100% | W-YK-01（双开关登录） |
| defect 缺陷绑定 | 100% | W-DFT-01a/b |
| config 系统配置 | 100% | config 模块 |
| screen 大屏 | 100% | W-SCR-01（cache putIfAbsent） |
| common 公共 | 100% | W-FIX-01/02（CFR 清理 0 残留） |
| DB 表 | 100% | 27 张表全部就位 |
| **合计 (99 项)** | **99%+** | DongleUtils 1 项 ADR-0005 跳过 |

---

## 🧠 关键发现留痕（ADR 索引）

| ADR | 标题 | 关键决定 |
|-----|------|---------|
| 0005 | PG14 path correction | pg-client path 修正；DongleUtils 跳过 |
| 0006 | screen cache strategy | `putIfAbsent` 而非 `put`，避免覆盖竞态 |
| 0007 | yingke dual switch | login/heartbeat 双开关策略 |
| 0008 | LinePO 删除（v2 反转） | 别名层是技术债，W-CLEAN-03 删除 |
| 0009 | AlarmRecordService 扩展方法保留 | 兼容外部调用方 |
| 0010 | ChangeLineDefectResult 仅 DTO | 无 PO / 无 DB 表 |
| 0011 | alarm sound 服务端零节流 | SOUND_PLAY_INTERVAL 是前端约定（PSM 设计）|
| W-X23-defect_type-seed | 缺陷类型种子数据 | 与 PSM 字面一致 |
| 0014 | account add 双重 hash | salt+SHA-256，DB 列加 `salt` |
| 0015 | super_admin 密码回退 | 7/25 老明文回退，W-AUTH-02 重置走 GenHash |
| 0016 | 前端对齐 PSM SPA | 老 bundle 临时止血；长期 W-FRONT-01 子单重建 Vue3+Vite |
| 0017 | 前端 bundle PM 误诊修正 | 前端代码 100% 正确；根因在调用时（baseURL/CORS/cookie/Pinia 时序），等老板 Console 回填 |

**重大订正**: WS 推送路径 `/ws?uid=...&type=alarm`（**不是** `/webSocket/alarm`，工单 brief 写错）

---

## 🚀 端点列表（核心子集，全部已验证 200）

| 端点 | 方法 | 模块 |
|---|---|---|
| `/web/plan` | GET/POST/PUT/DELETE | line |
| `/web/plan-bind?lineId=` | GET | line |
| `/client/plan?lineNo=&faceNo=` | GET | line |
| `/web/system-config` | GET/PUT | config |
| `/web/defect` | GET/POST/PUT/DELETE | defect |
| `/web/alarm/query` | POST | alarm |
| `/web/alarm/num` | GET | alarm（WS 路径 B HTTP-driven） |
| `/client/yk/line-defect` | GET | yingke |
| `/client/yk/defect-record` | POST | yingke |
| `/web/line/state/statistic` | GET | line |
| `/web/line/state/change` | POST | line |
| `/ws?uid=web&type=alarm` | WS | alarm（路径 A event-driven） |

> 完整 50+ 端点覆盖 alarm/detect/line/yingke/defect/screen/config

---

## 🧹 跳过项（ADR / 框架替代，不影响生产）

| 类 | 原因 | ADR |
|---|---|---|
| DongleUtils | 无硬件加密狗 | ADR-0005 |
| detect/util/TimeRange | 已用 framework TimeRangeUtil | — |
| detect/excel/DataMergeStrategy | 已纳入 ExcelUtils 内部类（PSM 实际结构）| W-DET-08 |
| detect/util/ExcelUtils | 导出工具非核心（已被 PSM 1:1 实现覆盖）| W-DET-08 |

---

## 🏃 历史冲刺（W-X25~X30，六轮冲刺）

| 冲刺 | 时间窗 | 工单数 | 归档 |
|------|--------|--------|------|
| W-X25 一期 | 13:41-15:04 | 13 | `delivered/W-X25-一期冲刺验收.md` |
| W-X26 二期 | 15:05-16:22 | 16 | `delivered/W-X26-二期冲刺完成.md` |
| W-X27 P0 | 17:42-18:42 | 10 | `delivered/W-X27-P0-工单冲刺完成.md` |
| W-X28 P1 | 19:14-19:42 | 8  | `delivered/W-X28-P1-工单冲刺完成.md` |
| W-X29 P2 | 20:02-21:10 | 7  | `delivered/W-X29-P2-工单冲刺完成.md` |
| W-X30 清理验证 | 21:28-22:43 | 4  | `delivered/W-X30-清理验证冲刺完成.md` |

> W-X23 之前工单见 `delivered/` 目录（2026-07-23 系列）

---

## 📁 关键文档索引

| 文档 | 位置 |
|---|---|
| PSM 全功能偏差对照表 | `docs/PSM-vs-DataupLoad-TABLE.md` |
| 完整 docs 目录索引 | `docs/README.md` |
| ADR 全部记录 | `docs/adr/0005~0011.md` + `W-X23-defect_type-seed.md` |
| W-X25~X30 冲刺验收 | `docs/delivered/W-X{25-30}-*.md` |
| 工单报告 | `docs/work-orders/W-*-report.md` |
| 当前心跳 | `HEARTBEAT.md` |

---

## 🏁 STATUS 速读（3 句）

1. **做了什么**: W-X25~X30 六轮冲刺 + W-FIX-02c 补 commit，186 文件 compile 0 error，PID 9248 实时报警入库，PSM 99%+ 对齐（DongleUtils 1 项 ADR-0005 跳过）
2. **卡在哪**: 当前**无 P0/P1 阻塞**，生产可用；导出返工 1:1 PSM 后链路验证通过
3. **下一步**: （a）等老板跑 W-FRONT-00-report §A Console 脚本 → Worker 回填根因 + patch → 老板浏览器验收；（b）老板拍板 W-X31（dispatch 已就位），建议下一轮做端到端灰盒压测 + 1h 稳定性
