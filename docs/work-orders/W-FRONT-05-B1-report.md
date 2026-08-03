# W-FRONT-05-B1 report — 上座率 API + 阈值配置

## 结论
**后端 0 代码改动**。现有 `LineController` + `SystemConfigController` 已覆盖全部需求。

## 数据摸底（重要修正）
- **老板说的"上座率" = `line.realtime_data` JSON 里的 `occupancyRate`**（百分比 0-100）
  - `occupancyRate` = 上座率
  - `occupancy` = 上座数量
  - `efficiency` = 效率（个/分，0-100）
- **昨 HEARTBEAT "realtime_data 表" 描述错误** —— 实际是 `public.line.realtime_data` 列（text），不是独立表
- **line 表结构**：`id/name/line_no/face_no/client_no/color/realtime_data(JSON text)/update_time/create_time`
- **line 表当前 38 条**（W-LINE-REG 补齐），`/web/line/list?pageSize=200` 返回 38 条 ✅

## 已插入 system_config（4 条，id 9-12）
| id | config_key | config_value | 说明 |
|----|-----------|--------------|------|
| 9  | occupancy.warn_threshold | 80 | 上座率黄色阈值（< 此值红） |
| 10 | occupancy.good_threshold | 95 | 上座率绿色阈值（>= 此值绿） |
| 11 | occupancy.refresh_interval | 5 | 前端刷新间隔（秒） |
| 12 | occupancy.show_value | true | 格子内是否显示真实值 |

## API 契约（前端直接复用，无需新 endpoint）
- `GET /web/line/list?pageNum=1&pageSize=200` → lines（每条 `realtimeData` JSON 字符串，含 `occupancyRate`）
- `GET /web/system-config` → 全部配置（含 4 条 occupancy.*）
- `PUT /web/system-config` → 批量更新整表（B4 阈值 UI 用；注意**行数必须一致**，否则 20601）

## curl 验证
- `GET /web/line/list?pageNum=1&pageSize=200` → 200, 78935 bytes, 38 records ✅
  - line1A faceNo=A2, `occupancyRate` 字段存在 ✅
- `GET /web/system-config` → 200, 8 条配置（含 4 条 occupancy.*）✅
- `POST /web/auth/login` → 200 + satoken cookie ✅

## 已知
- 当前所有 `occupancyRate` = 0（设备没在生产，看板全灰/红）
- `PUT /web/system-config` 行数必须与表一致（当前 8 行），B4 保存时需整表提交

## 后续（归 B4）
- B4 阈值 UI：读 GET /web/system-config → 展示 4 个 occupancy.* → PUT 整表回写
- 前端聚合 `/web/line/list` 的 occupancyRate + `/web/system-config` 的阈值，算红黄绿
