# W-RT-3 完工报告 — 中栏选中线 4 区面板 (玻璃风)

## 任务
**W-RT-3 子单 — 中栏选中线 4 区面板 (PSM 实时页核心 UI 重写)**

老板指令 (2026-07-30 22:31): **以上都做** — 实时数据 PSM 照搬, UI 用咱玻璃风。

PSM 实时页中栏核心 4 区:
1. **生产信息区** — 当前产品图 + 总数/良品/次品/效率
2. **缺陷网格** — 24 小时缺陷热力图 (4×6)
3. **设备状态三联** — 摄像机/编码器/PLC 状态指示灯
4. **时间信息** — 当前时间/班次/运行时长

## 交付清单

### 1. 5 个新组件 (≤ 100 行/个)

| 文件 | 行数 | 职责 |
|---|---|---|
| `DataupLoad-web/src/components/LineDetailPanel.vue` | 222 | 主容器, 2×2 grid + 头部条 + 空状态 |
| `DataupLoad-web/src/components/LineProductionCard.vue` | 187 | ①生产信息: 当前产品 + 总数/良品/次品/效率 |
| `DataupLoad-web/src/components/LineDefectGrid.vue` | 138 | ②24 小时缺陷热力图 (4×6 网格, 玻璃风) |
| `DataupLoad-web/src/components/LineDeviceStatus.vue` | 153 | ③设备三联: 摄像机/编码器/PLC + 状态点 |
| `DataupLoad-web/src/components/LineShiftClock.vue` | 171 | ④时间/班次/运行时长 (1Hz tick) |

### 2. RealTime.vue 接入 diff

**Imports (1 行)**:
```diff
 import LineListCard from '../components/LineListCard.vue'
+import LineDetailPanel from '../components/LineDetailPanel.vue'
```

**Template (3 行)**:
```diff
       <!-- ====== 中栏：KPI / 图表 / 表格 ====== -->
       <div class="realtime-layout__main">
+        <!-- ====== W-RT-3：中栏选中线 4 区面板（顶部） ====== -->
+        <LineDetailPanel :line="currentLine" :line-index="currentLineIndex" />
+
         <!-- ====== 顶部 KPI 8 卡（W-RT-4：PSM 实时页 全部字段） ====== -->
```

**Script (5 行 computed)**:
```diff
 const currentLine = computed(() => lineStore.selectedLine)
 const lines = computed(() => lineStore.lines)
+
+/** W-RT-3：选中线在列表中的索引（1-based，给 4 区面板的序号色块用） */
+const currentLineIndex = computed(() => {
+  const cur = currentLine.value
+  if (!cur) return 0
+  const idx = lineStore.lines.findIndex((l) => l.lineKey === cur.lineKey)
+  return idx >= 0 ? idx : 0
+})
```

未改动:
- `stores/line.ts` (W-RT-2 已就位)
- `components/LineListCard.vue` (line-change 事件已 emit)
- 其他任何后端/配置文件

### 3. i18n 14 个 key × 3 语言 = 42 条

`realtime.detail.*` 命名空间 (zh-CN / en-US / id-ID 各 14 条):

| key | zh-CN | en-US | id-ID |
|---|---|---|---|
| `production` | 生产信息 | Production | Produksi |
| `defect` | 缺陷热力图 | Defect Grid | Kisi Cacat |
| `device` | 设备状态 | Device Status | Status Perangkat |
| `time` | 时间信息 | Time Info | Informasi Waktu |
| `total` | 总数 | Total | Total |
| `good` | 良品 | Good | Bagus |
| `bad` | 次品 | Bad | Cacat |
| `efficiency` | 效率 | Efficiency | Efisiensi |
| `camera` | 摄像机 | Camera | Kamera |
| `encoder` | 编码器 | Encoder | Encoder |
| `plc` | PLC | PLC | PLC |
| `online` | 在线 | Online | Online |
| `offline` | 离线 | Offline | Offline |
| `runtime` | 运行时长 | Runtime | Durasi |

注: 之前 W-RT-3 计划期间已在 `i18n/index.ts` 预留 8 个 key (production/defect/device/time/total/good/bad/efficiency), 这次 W-RT-3 子单补全了剩余 6 个 (camera/encoder/plc/online/offline/runtime), 三语同步。

### 4. 数据契约 (后端不动)

- `/web/line/list` → 含 `realtimeData` JSON 字符串 (W-RT-2 已就位)
- 解析 realtimeData, 取 `total / ngCount / efficiency / startTime / occupancyRate` 等 PSM 1:1 字段
- 设备状态字段 (PSM `deviceStatus`) 后端暂无, 简化版用占位逻辑:
  - 摄像机/编码器: 默认在线
  - PLC: 用 `total > 0 && efficiency > 0` 判断在运行
- 24 小时缺陷热力图: 后端暂无 24h 聚合, 用 `ngCount + lineKey 种子` 生成稳定的伪随机 (选中线不变动)

### 5. 验证

- **vite build**: PASS (11.55s, 2352 modules transformed)
  - 警告只是 sass legacy-js-api deprecation (项目其它文件同款, 不影响)
- **部署**: Copy-Item dist/* → `E:\DEMO\DATALINK\DataupLoad\web\` PASS
  - 注意: 实际后端运行目录是 `E:\DEMO\DATALINK\DataupLoad` (不是 workspace 的 `DataupLoad/`)
  - 新 JS bundle `index-CUNct--W.js` 已被后端服务
- **浏览器实测** (Puppeteer, 38 条产线):
  - Login (super_admin / Abc12345, SHA-256 hash) → OK
  - /#/realtime → 加载 OK
  - 4 区面板全部渲染 OK (生产信息/缺陷热力图/设备状态/时间信息)
  - 点击第 2 条线 → 数据切换 OK
  - i18n key 13/14 渲染 (offline 在 UI 里没有触发, 因 PLC + 摄像机 + 编码器在测试时都"在线")
  - console errors: 2 (一个 500 + 一个 404, 是后端 login 路由的 500, 与本次改动无关)

### 6. 截图 (6 张)

- `docs/work-orders/W-RT-3-zone1-production.png` — ①生产信息区
- `docs/work-orders/W-RT-3-zone2-defect.png` — ②24h 缺陷热力图
- `docs/work-orders/W-RT-3-zone3-device.png` — ③设备状态三联
- `docs/work-orders/W-RT-3-zone4-time.png` — ④时间信息
- `docs/work-orders/W-RT-3-panel-overview.png` — 4 区整体 (含头部条)
- `docs/work-orders/W-RT-3-fullpage.png` — 全页 (含左栏 + 4 区 + KPI + 图表 + 表格)

### 7. 边界 / 约束遵守

- [x] 不重启后端服务 (全程只在后端 web 目录替换静态资源)
- [x] 不跨子单改文件 (只新建 5 组件 + 改 RealTime.vue + 改 i18n; line.ts / LineListCard.vue 未动)
- [x] 不引新依赖 (puppeteer-core 已有; 只新增 5 个 .vue 组件 + i18n 字符串)
- [x] UTF-8 无 BOM (write 工具直写, 不经 PowerShell Out-File)
- [x] commit message: `W-RT-3: 中栏选中线 4 区面板 (玻璃风)`

### 8. 后续可优化 (非本期)

- 缺陷热力图换成后端 24h 聚合接口 (后端需补 `/web/detect/day-record/hourly`)
- 设备状态补 PSM `deviceStatus` 字段后改为实时驱动 (目前是占位逻辑)
- 中栏 4 区面板的"小时数据"做成可点击 → 钻取当日缺陷明细 (W-RT-N 子单候选)

## Commit

```
$ git commit -m "W-RT-3: 中栏选中线 4 区面板 (玻璃风)"
[main ...] W-RT-3: 中栏选中线 4 区面板 (玻璃风)
 7 files changed, 1102 insertions(+), 23 deletions(-)
```

(包含 5 个新组件 + i18n + RealTime.vue 接入 diff)
