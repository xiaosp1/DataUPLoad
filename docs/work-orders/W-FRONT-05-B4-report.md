# W-FRONT-05-B4 report — 上座率阈值配置弹窗

## 状态
✅ 完工并部署

## 产出文件
- **新增** `DataupLoad-web/src/components/OccupancyThresholdDialog.vue`（~9.3KB）
  - 弹窗：红色阈值 / 绿色阈值 / 刷新间隔 三个滑杆 + 显示数值 复选框
  - **实时预览**：5 个样例色块（30/70/85/96/—）颜色随滑杆实时变化
  - 读数：`GET /web/system-config`；保存：改 4 个 `occupancy.*` → `PUT /web/system-config` 整表回写
  - 边界约束：warn < good（滑杆联动互锁）
- **修改** `src/components/OccupancyPanoramaBar.vue`
  - 顶部加 ⚙ 齿轮 → `openThreshold()` 打开弹窗
- **修改** `src/i18n/index.ts`
  - 三语 occupancy 加 6 keys（thresholdTitle / thresholdPreview / warnThreshold / goodThreshold / refreshInterval / thresholdSaved）

## 端到端验证（Python cookie-jar，绕过 PowerShell curl 编码坑）
```
current warn = 85   ←（上次 PowerShell 遗留）PUT revert → success:true → after=80
PUT 85 → success:true → PUT 80 → success:true → final warn=80 ✅
```
- 读取 → 改阈值 → 整表 PUT → DB 落库 → 回读确认，完整 cycle 通过
- 后端返回 `{"success":true,"code":0}`

## 部署验证
- `npm run build` → 通过（57.36s）
- HTTP 200：`GET /` + `GET /assets/index-DXFyfpNn.js`
- bundle 内 threshold 弹窗字符串在手（zh/en/id）

## 已知/边界
- **WS 增量刷新未接**（B4 原计划含，但已用 5s `lineStore.load(true)` 定时器实现，频率 config 可调 1-60s；WS 增量列为可选优化 B5）
- PowerShell 测试时的 `curl.exe` 中文 configName GBK mojibake 导致 ConvertFrom-Json 挂 —— **不影响浏览器前端**（浏览器 fetch/axios 输出正确 UTF-8，已用 Python 验证功能正确）
- PUT 要求整表行数一致（后端 20601），弹窗前端已整表提交

## 待办
- 老板浏览器实测：RealTime 顶部 ⚙ → 调阈值 → 保存 → 看板颜色变化（当前停产全灰，需设备生产才有色）
- 更新 HEARTBEAT + 一次性 git commit（B1/B2/B3/B4 + A 单全刷修复）

## 耗时
PM 手写 + 验证 ~1.5h
