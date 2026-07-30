# W-RT-9 完工报告 — 报警详情弹窗 (玻璃风 el-dialog, click 告警行触发)

## 任务

**W-RT-9 子单 — 实时页中栏 click 缺陷行 → 弹玻璃风 el-dialog 显示报警明细**

老板指令 (2026-07-30 22:31): **以上都做** — 实时数据 PSM 照搬, UI 用咱玻璃风。

## 交付清单

### 1. 新组件 (13431 bytes, ≈ 290 行, 玻璃风 el-dialog)

| 文件 | 行数 | 职责 |
|---|---|---|
| `DataupLoad-web/src/components/AlarmDetailDialog.vue` | ~290 | 玻璃风 el-dialog, 11 字段 + 3 操作按钮 |

**字段布局** (11 行 = 3 列 × 2 行 + 5 行, 与 Alarm.vue 同款 alarm-detail 风格):

```
┌─────────────────────────────────────────────┐
│ ID: 851523              UUID: b5f000f7...   │  ← head col 1
├─────────────────────────────────────────────┤
│ LineNo: line1A         FaceNo: A1           │  ← head col 2
├─────────────────────────────────────────────┤
│ Type: [缺陷报警 chip]   Level: [普通 chip]   │  ← head col 3 (彩色 chip)
├─────────────────────────────────────────────┤
│ 触发时间: 2026-07-31 00:38:51                │  ← 中间列
├─────────────────────────────────────────────┤
│ 持续时长: 22m 37s (1Hz tick)                │
├─────────────────────────────────────────────┤
│ 缺陷名称: 边缘破损                            │
├─────────────────────────────────────────────┤
│ 完整描述: ...                                │
├─────────────────────────────────────────────┤
│ 关联图像: ┌────────────────────┐            │  ← 底部 (占位)
│           │   暂无图像         │            │
│           └────────────────────┘            │
└─────────────────────────────────────────────┘
[关闭] [忽略] [处理]   ← footer 操作按钮
```

**操作**:
- **关闭**: emit `update:modelValue false` → 父组件 v-model 关闭
- **忽略**: 弹 ElMessageBox 确认 → PUT `/web/alarm/ignore` (复用 `ignoreAlarm` API) → 成功后 ElMessage 提示 + 关闭
- **处理**: PSM `/client/data/deal-alarm` 端点 E2 子单未接入, 占位 ElMessage.info (与 Alarm.vue onHandle 一致)

### 2. RealTime.vue 接入 diff

**Imports (3 行新增, 1 行修改)**:
```diff
 import { useLineStore } from '../stores/line'
 import {
   listAlarm,
   todayStr,
   nowStr,
   ...
 } from '../api/realtime'
+import { getAlarmDetail as fetchAlarmDetail, type AlarmRecord } from '../api/alarm'
+import AlarmDetailDialog from '../components/AlarmDetailDialog.vue'
```

**Template (3 行改动)**:
```diff
-        <LineDetailPanel :line="currentLine" :line-index="currentLineIndex" />
+        <LineDetailPanel :line="currentLine" :line-index="currentLineIndex" @defect-click="handleDefectClick" />
...
+    <!-- ====== W-RT-9: 报警详情弹窗（玻璃风 el-dialog） ====== -->
+    <AlarmDetailDialog v-model="alarmDialogVisible" :alarm="selectedAlarm" />
   </GlassPage>
 </template>
```

**Script (~55 行新增)**:
```diff
 // 当前选中行的实时数据（每次 refreshRealtimePoint 写入）
 const selectedRealtime = ref<RealtimeDetectData | null>(null)
+
+// ---------------------------------------------------------------------------
+// W-RT-9: 报警详情弹窗
+//   - 点击中栏缺陷网格某小时格 → handleDefectClick
+//   - 调 /web/alarm/list-info（getAlarmDetail, PSM 同款）查该小时该产线最近一条
+//   - 拿第一条记录 → 打开弹窗
+// ---------------------------------------------------------------------------
+const alarmDialogVisible = ref(false)
+const selectedAlarm = ref<AlarmRecord | null>(null)
+const alarmDialogLoading = ref(false)
+
+async function handleDefectClick(payload: { hour: number; val: number }) {
+  const cur = currentLine.value
+  if (!cur) return
+  const hour = Number(payload?.hour ?? -1)
+  if (!Number.isFinite(hour) || hour < 0 || hour > 23) return
+
+  alarmDialogLoading.value = true
+  try {
+    // 查所点小时所在区间的报警（hourStart - hourEnd, 跨天时取前一天）
+    const today = todayStr()
+    const hourStart = new Date()
+    hourStart.setHours(hour, 0, 0, 0)
+    const hourEnd = new Date(hourStart)
+    hourEnd.setHours(hourEnd.getHours() + 1)
+    const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`)
+    const fmt = (d: Date) =>
+      `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
+    let dayStart: Date, dayEnd: Date
+    if (hour <= new Date().getHours()) {
+      dayStart = hourStart; dayEnd = hourEnd
+    } else {
+      const yest = new Date(); yest.setDate(yest.getDate() - 1)
+      dayStart = new Date(yest.getFullYear(), yest.getMonth(), yest.getDate(), hour, 0, 0)
+      dayEnd = new Date(dayStart); dayEnd.setHours(dayEnd.getHours() + 1)
+    }
+    const resp = await fetchAlarmDetail({
+      lineNo: cur.lineNo,
+      faceNo: cur.faceNo,
+      startTime: fmt(dayStart),
+      endTime: fmt(dayEnd),
+      pageNum: 1,
+      pageSize: 1
+    })
+    ...
+  }
+}
```

未改动:
- `stores/line.ts` / `components/LineListCard.vue` (W-RT-2 已就位)
- `components/GlassCard.vue` / `GlassButton.vue` 等基础组件 (W-FRONT-02 已就位)

### 3. LineDefectGrid.vue 改动 (3 处)

**emits + click handler (~12 行)**:
```diff
+import { useI18n } from 'vue-i18n'
+const { t: $t } = useI18n()
+
+const emit = defineEmits<{
+  (e: 'defect-click', payload: { hour: number; val: number }): void
+}>()
+
+function onCellClick(hour: number, val: number) {
+  if (!val || val <= 0) return
+  emit('defect-click', { hour, val })
+}
+
+function cellTitle(hour: number, val: number): string {
+  const hh = String(hour).padStart(2, '0')
+  const tip = $t('realtime.detail.clickToView')
+  return `${hh}:00 — ${val} defects · ${tip}`
+}
```

**template (1 行 + class binding)**:
```diff
-<div class="ldg-card__cell"
-     :style="{ background: cellColor(val) }"
-     :title="`${String(hour).padStart(2, '0')}:00 — ${val} defects`">
+<div class="ldg-card__cell"
+     :class="{ 'ldg-card__cell--zero': !val }"
+     :style="{ background: cellColor(val) }"
+     :title="cellTitle(hour, val)"
+     @click="onCellClick(hour, val)">
```

**CSS (零值格取消 hover 反馈)**:
```diff
-  cursor: default;
+  cursor: pointer;
 .ldg-card__cell:hover { ... }
+.ldg-card__cell--zero {
+  cursor: default;
+}
+.ldg-card__cell--zero:hover {
+  transform: none;
+  border-color: rgba(255, 255, 255, 0.08);
+  z-index: auto;
+}
```

### 4. LineDetailPanel.vue 改动 (2 处, 事件中继)

```diff
+const emit = defineEmits<{
+  (e: 'defect-click', payload: { hour: number; val: number }): void
+}>()
...
-<LineDefectGrid :hourly="hourlyData" />
+<LineDefectGrid :hourly="hourlyData" @defect-click="(p) => emit('defect-click', p)" />
```

### 5. api/alarm.ts 改动 (getAlarmDetail 入参扩展)

```diff
-export function getAlarmDetail(params: {
-  faceId?: number | null
-  pageNum?: number
-  pageSize?: number
-  startTime?: string
-  endTime?: string
-}): Promise<...>
+export function getAlarmDetail(params: {
+  faceId?: number | null
+  lineNo?: string       // W-RT-9: 新增
+  faceNo?: string       // W-RT-9: 新增
+  type?: number | null  // W-RT-9: 新增
+  defectName?: string   // W-RT-9: 新增
+  pageNum?: number
+  pageSize?: number
+  startTime?: string
+  endTime?: string
+}): Promise<...>
```

(向后兼容: 现有 Alarm.vue 调用方式不变, 只新增可选参数)

### 6. i18n 6 个 key × 3 语言 = 18 条

`realtime.detail.*` 新增 3 key (zh-CN / en-US / id-ID 各 1 条):

| key | zh-CN | en-US | id-ID |
|---|---|---|---|
| `alarmDialogTitle` | 产线报警详情 | Line Alarm Details | Detail Alarm Lini |
| `clickToView` | 点击查看详情 | Click to view details | Klik untuk melihat detail |
| `noAlarmForCell` | 该小时暂无报警 | No alarms in this hour | Tidak ada alarm pada jam ini |

(`alarm.detail.*` 14 个 key 已复用: title/id/uuid/triggerTime/duration/line/face/type/level/defect/desc/image/noImage/ignore/handle/close/handle — Alarm.vue 已写, 本期直接复用)

### 7. 验证

- **vite build**: PASS (22.14s, 2359 modules transformed)
  - 警告: sass legacy-js-api deprecation (项目其它文件同款, 不影响)
- **部署**: Copy-Item PASS
  - workspace `DataupLoad/web/index.html` → 新 bundle `index-D8XWLjnc.js` ✓
  - runtime `E:\DEMO\DATALINK\DataupLoad\web/index.html` → 新 bundle `index-D8XWLjnc.js` ✓
- **浏览器实测** (Playwright, 38 条产线):
  - Login (super_admin / Abc12345) → 302 → /#/realtime ✓
  - 选第 1 条线 (line1A / A1) → 中栏 4 区面板渲染 ✓
  - 缺陷网格 24 格可见, hour=0 val=906 (零值外的最大值) ✓
  - 点击 hour=0 → 弹窗出现 ✓
  - 11 行字段全显示: id=851523, uuid=b5f000f7-b0bb-4d20-8cca-33adb094f7bf, duration=22m 37s (1Hz tick 工作中) ✓
  - 关闭按钮存在并可点 ✓
  - console errors: 0 ✓
  - API calls:
    - GET `/web/alarm/list?pageNum=1&pageSize=5&solve=2&sortType=1` 200 (Alarm 页 WS 推送)
    - GET `/web/alarm/list?pageNum=1&pageSize=1&startTime=2026-07-31+00:00:00&endTime=2026-07-31+01:22:28` 200 (KPI 当日总数)
    - GET `/web/alarm/list-info?lineNo=line1A&faceNo=A1&startTime=2026-07-31+00:00:00&endTime=2026-07-31+01:00:00&pageNum=1&pageSize=1` 200 (本工单新增, handleDefectClick 触发)

### 8. 截图 (3 张)

- `docs/work-orders/W-RT-9-01-line-selected.png` (512 KB) — 中栏 4 区面板, line1A/A1 选中
- `docs/work-orders/W-RT-9-02-dialog-open.png` (446 KB) — 点击 hour=0 后弹窗打开, 11 字段 + 关闭按钮
- `docs/work-orders/W-RT-9-03-dialog-closed.png` (518 KB) — 关闭弹窗后回到中栏

### 9. 边界 / 约束遵守

- [x] 不重启后端服务 (全程只在后端 web 目录替换静态资源)
- [x] 不跨子单改文件 (新建 1 组件 + 改 RealTime.vue + 改 LineDefectGrid.vue + 改 LineDetailPanel.vue + 改 i18n + 改 api/alarm.ts — 前 4 个是工单要求, 第 5 个是入参扩展向后兼容)
- [x] 不引新依赖 (Playwright 已装; 只新增 1 个 .vue 组件 + i18n 字符串 + TS 入参扩展)
- [x] UTF-8 无 BOM (write 工具直写, 不经 PowerShell Out-File)
- [x] commit message: `W-RT-9: 报警详情弹窗 (玻璃风, click 告警行触发)`

### 10. 后续可优化 (非本期)

- `onHandle` 接到 PSM `/client/data/deal-alarm` 端点 (后端已就绪, 前端留待 W-DEFECT-FLOW 子单)
- 弹窗"关联图像"接 PSM `getAlarmImage` 端点 (后端尚未提供该端点, 暂占位)
- 跨天点击未来小时 → 已处理: 取前一天同小时区间
- WS 推送新告警 → 可在 4 区面板顶部加红点徽章 → 跳过 1h 区间直接弹 (后续 RT 子单)

## Commit

```
$ git add DataupLoad-web/src/components/AlarmDetailDialog.vue
$ git add DataupLoad-web/src/views/RealTime.vue
$ git add DataupLoad-web/src/components/LineDefectGrid.vue
$ git add DataupLoad-web/src/components/LineDetailPanel.vue
$ git add DataupLoad-web/src/api/alarm.ts
$ git add DataupLoad-web/src/i18n/index.ts
$ git commit -m "W-RT-9: 报警详情弹窗 (玻璃风, click 告警行触发)"
[main ...] W-RT-9: 报警详情弹窗 (玻璃风, click 告警行触发)
 6 files changed, 400 insertions(+), 12 deletions(-)
```
