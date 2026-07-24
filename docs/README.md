# 📚 docs/ — 项目文档目录

> 最后更新: 2026-07-24 16:26  
> 项目: DataupLoad (PSM 反向工程重建)

---

## 📂 目录结构

```
docs/
├── README.md                     ← 本文件，目录导航
├── PSM-vs-DataupLoad-TABLE.md    ← PSM 全功能 × DataupLoad 偏差对照表（最终版）
│
├── delivered/                    ← 交付验收记录
│   ├── W-X25-一期冲刺验收.md      2026-07-24 一期 DB/FW/WEB/ALM/DET/CMN
│   └── W-X26-二期冲刺完成.md      2026-07-24 二期 line/config/screen/state/yk 全补齐
│
├── adr/                          ← 架构决策记录
│   ├── 0005-pg14-path-correction.md
│   ├── 0005-psm-clone-new-project.md
│   └── W-X23-defect_type-seed.md
│
├── domain/                       ← 领域知识与逆向资料
│   ├── 海康大屏逆向/              ← PSM 反编译产物（9000+ 文件）
│   │   └── PSM.rar               原版反编译 ZIP
│   ├── 英科医疗手套车间/           ← 业务场景文档
│   │   ├── 01-line-topology.md
│   │   ├── 02-data-flow.md
│   │   ├── 03-business-roles.md
│   │   └── README.md
│   └── ...
│
├── psm-reference/                ← PSM 模块级技术分析（7 模块 + 全量）
├── tasks/                        ← 历史工单记录
├── SOP/                          ← 标准操作流程
└── progress/                     ← （预留）进度追踪
```

---

## 🔑 核心文档

| 文档 | 定位 |
|---|---|
| `PSM-vs-DataupLoad-TABLE.md` | 最终对齐报告，99 项 100% |
| `delivered/W-X25-一期冲刺验收.md` | 一期（13:41-15:04）验收 |
| `delivered/W-X26-二期冲刺完成.md` | 二期（15:05-16:22）验收 |
| `adr/*.md` | 架构决策记录 |
| `domain/英科医疗手套车间/` | 业务场景描述 |
| `psm-reference/` | 模块级逆向分析报告 |

---

## 📊 项目状态

```
编译:      181/181 .java 文件 ✅
DB 表:     27/27 ✅
服务:      PID 20892, 端口 80, 0 错误
PSM 对齐:  ≈100%（DongleUtils ADR 跳过）
```

**联系人**: 锋卫 (AI PM) | **群组**: 数据采集重构
