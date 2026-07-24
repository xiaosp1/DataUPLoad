# ADR-0005 修订：PG 14 路径修正（23:58 PM 体检）

| 字段 | 值 |
|---|---|
| 状态 | **Accepted**（23:58 PM 体检发现 + 老板 23:54 #7874 第 2 条指令确认）|
| 日期 | 2026-07-22 23:58 |
| 决策者 | PM 体检 + 老板拍板 |
| 影响范围 | 修正 ADR-0005 第 5 件事 + TODO.md + STATUS.md |
| 关系 | **修订 ADR-0005** |

---

## 修正前

ADR-0005 老板 13:13 拍板第 5 件事：
> **PG 12 安装位置：E:\PostgreSQL\12\**

TODO.md / STATUS.md / 今晚计划文档全部沿用此决议。

---

## 修正后

**实测真实安装（23:58 PM 体检）**：

| 项 | 老板拍板 | **实测真实** | 偏差 |
|---|---|---|---|
| 版本 | PostgreSQL **12** | **PostgreSQL 14.23** | 🔴 版本错（装的是 PG 14）|
| 安装路径 | `E:\PostgreSQL\12\` | **`C:\Program Files\PostgreSQL\14\`** | 🔴 路径错（C 盘，不是 E 盘）|
| 端口 | (未指定) | **5433**（不是 PG 默认 5432）| 🆕 需说明 |
| 服务名 | (未指定) | `postgresql-x64-14` Running Automatic | 🆕 |
| 数据库 | intco | intco ✅ | OK |
| 用户 | postgres / postgres | postgres / postgres ✅ | OK |

PM 用 psql 验证：
```
$ psql -U postgres -h 127.0.0.1 -p 5433 -d intco -c 'SELECT version();'
                           version
-------------------------------------------------------------
 PostgreSQL 14.23, compiled by Visual C++ build 1944, 64-bit
```

---

## 老板 23:54 #7874 第 2 条指令

> "那就修改下 按照pg14来"

PM 立即执行：
- ✅ TODO.md 改 PG 12 → PG 14（路径改 C:\Program Files\PostgreSQL\14\）
- ✅ STATUS.md 改 PG 12 → PG 14
- ✅ ADR-0005 补本份修订（0005-pg14-path-correction.md）
- ✅ INDEX.md 引用本份修订

---

## PM 体检反思（铁则 39 自查）

| 失职 | 后果 | 自评 |
|---|---|---|
| 13:13 老板拍"PG 12 装 E:\PostgreSQL\12\" PM 没问清楚现场实际版本 | 11h 后 PM 体检发现实际装的是 PG 14 + C 盘，ADR 文档与现实脱节 | 🔴 PM 没现场核 |
| TODO.md / 今晚计划文档沿用老板拍板但没写明"已实测"还是"待实施" | 后续 W-B02 工单 PG 部署段可能误以为装了 PG 12 | 🟡 文档语义不清 |

**改进**：
- 派工单 DoD 加 "PM 体检：到现场 `Get-Service` + `psql --version` 双验证后再写完成"
- 老板拍"装 X"类工单 PM 必须先 `where / find` 现场是否已装，避免重装/覆盖

---

## 关联文档

- ADR-0005 原版（PG 12 拍板）→ `docs/adr/0005-psm-clone-new-project.md`
- 老板 23:54 #7874 第 2 条指令 → 改 PG 14
- 今晚计划文档（TODO 顶部）→ 已更新
- 真名清单（同步反推自 PG 14）→ `docs/delivered/2026-07-22-vision-registry-auto.md`
