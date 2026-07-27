# W-FRONT-02 — 前端 Vue 3 + Element Plus + Vite 重构（重派版）

- **状态**: 待派工
- **优先级**: P2（业务已用 gate-routing 方案 X-1 跑通，本工单不阻塞）
- **关联**: 
  - W-FRONT-01（已延后，参见 `docs/work-orders/W-FRONT-01-deferred.md`）
  - W-FRONT-X1（已完成方案 X-1，本工单是方案 Y 重启）
  - ADR-0016（前端对齐 PSM SPA）
  - ADR-0018（方案 X-1 临时过渡）
- **重派触发**: 2026-07-27 15:15 老板指令 — 清理孤儿文件后重新按规则派工
- **派工时间**: 2026-07-27 15:15

## 目标

把 `web/index.html`（PSM 老 SPA + 我写的 gate 补丁）替换成 **全新 Vue 3 + Element Plus + Vite SPA**，后端零改动，最终部署到 `web/` 目录覆盖。

## 与 W-FRONT-01 不同的关键点（PM 反思）

### W-FRONT-01 失败原因
1. 派工后 **worker 无 report 回执**，PM 没及时发现，浪费 30+ 分钟
2. **A→E 流水线无 PM 验收门控**——worker 跳级报告，PM 没看到中间产物
3. **没有 verify 脚本**——worker 自测 OK，PM 复测才发现问题
4. **没有 done 定义**——子单之间无明确边界

### W-FRONT-02 修正（PM 硬规则）

1. **每个子单必须有 report.md**——worker 完成后必须输出 `docs/work-orders/W-FRONT-02-X-report.md`，PM 不看到 report 不下下一单
2. **每个子单 PM 必须亲自 verify**——执行 `scripts/verify-w-front-02-{X}.ps1`（PM 自己写），不通过打回 worker
3. **done 定义明确**——每张子单 brief 含 3-5 条 done criteria，verify 脚本逐条对应
4. **派工 prompt 必含三件事**：
   - 明确 report 路径 + 格式
   - 明确 verify 脚本路径 + 调用方法
   - 明确 PM 等多久（默认 60 分钟无回执升级）

## 子单序列（A→E 流水线）

| 单号 | 任务 | 依赖 | done criteria | verify 脚本 |
|------|------|------|--------------|------------|
| **W-FRONT-02-A** | Vite+Vue3+ElementPlus+Router+Pinia 脚手架（DataupLoad-web/ 独立工程） | — | ① npm install 成功 ② npm run dev 起在 5173 ③ GET / 返回 Vue 3 默认页 ④ 控制台 0 error | `scripts/verify-w-front-02-A.ps1` |
| **W-FRONT-02-B** | Login.vue + 路由守卫 + satoken cookie + sha256 集成 | A | ① 登录表单 UI 完整 ② POST /web/auth/login 通 ③ cookie 自动带 ④ /web/account/current 通 ⑤ 路由守卫工作（未登录跳 /login） | `scripts/verify-w-front-02-B.ps1` |
| **W-FRONT-02-C** | 主界面路由表（/realTime、/alarm、/defect）+ Vuex/Pinia 状态管理 + axios interceptor | B | ① 路由跳转 OK ② axios 自动带 satoken ③ 路由守卫读 satoken 或 /web/account/current | `scripts/verify-w-front-02-C.ps1` |
| **W-FRONT-02-D** | 业务页面 stub（报警列表 / 缺陷列表 / 实时数据）+ i18n（zh-CN / en-US / id-ID） | C | ① 报警列表能拉后端数据 ② 缺陷列表能拉 ③ 实时数据 WS 联通 ④ i18n 切换工作 | `scripts/verify-w-front-02-D.ps1` |
| **W-FRONT-02-E** | vite build → 拷到 web/ + 重启服务 + 端到端 12 项验收 + verify 脚本归档 | D | ① build 产物 < 5MB（gzipped） ② 部署后 GET / 200 ③ 浏览器端到端 12 项全过 ④ verify 脚本归档 | `scripts/verify-w-front-02-E.ps1` |

## 派工模板（worker 必读）

每个子单派工 prompt 必须包含：

```text
【任务】W-FRONT-02-X
【必读】
- /brief: docs/work-orders/W-FRONT-02-{X}-brief.md
- /约束:  ADR-0016 + ADR-0018
【必产出】
1. docs/work-orders/W-FRONT-02-{X}-report.md（done criteria 逐条勾选）
2. scripts/verify-w-front-02-{X}.ps1（PM 验收脚本）
【验收门控】
- PM 收到 report.md 才视为完成
- PM 执行 verify 脚本不通过则打回
- 60 分钟无回执升级 PM 介入
【完成后回 PM】
"docs/work-orders/W-FRONT-02-{X}-report.md 已写，verify 脚本已就绪"
【禁止】
- 不许跨子单（A 没完成不能动 B）
- 不许跳过 PM verify
```

## 派工命令（codex exec）

```powershell
codex exec -C "E:\DEMO\数据采集\DataupLoad-web" --skip-git-repo-check -s workspace-write "<prompt 内容>"
```

## 备份策略

- 每次启动子单前备份 `web/index.html` → `backups/web-index-{timestamp}.html`
- 备份 `DataupLoad-web/` 在每次 build 前打 zip
- 备份位置: `E:\DEMO\数据采集\backups\w-front-02\`

## 风险

- ⚠️ Vue 3 + Element Plus + Vite 全新工程，可能踩坑（webpack vs vite 区别、Pinia vs Vuex 选择等）
- ⚠️ 后端接口契约没文档，需 worker 自己从 controller @RequestMapping 反推
- ⚠️ i18n key 体系庞大（PSM 老 SPA 有 200+ i18n keys），worker 可能只覆盖核心 30%
- ⚠️ **vue-router hash mode vs history mode** —— 直接用 hash mode（不需要 nginx 重写），简化部署

## 验收门控（PM 必做）

| 阶段 | PM 必做 |
|------|---------|
| 子单派工 | 写子单 brief（含 done criteria）+ 把 verify 脚本写好（哪怕空壳） |
| Worker 完工 | 收到 report.md 才视为完成 |
| PM 验收 | 执行 verify 脚本，截图 + 文字记录到 `docs/work-orders/W-FRONT-02-X-pm-verify.md` |
| 不通过 | 写打回单 `docs/work-orders/W-FRONT-02-X-reject.md`，worker 重做 |
| 子单完成 | commit + push 子单产物 + 下下一单 |

## 不在本工单范围

- 后端任何代码改动（业务接口契约不变）
- 数据库 schema 变更
- 旧 PSM 老 SPA 完全移除（保留 `web/index.psm-legacy.html` 作为回滚备份）
- C:\hik\ 调研产物清理（已完成，本次清理后保留反编译 jar 等）

## 开始时间

- 派工 A: 2026-07-27 15:15

