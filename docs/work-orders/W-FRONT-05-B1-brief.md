# W-FRONT-05-B1 上座率 API + system_config 阈值

## 背景
老板需求：把上座率单独拿出来放在主页醒目的位置 + 独立页面。38 条线 × 4 面 = 152 格热力图。**颜色 + 真实值双显**（老板 11:05 确认）。

数据底层已有：`public.line.realtime_data` JSON text，line_no + face_no 二维。

## 目标
1. 上座率数据 API（批量查实时）
2. system_config 加 3 个阈值配置（warnThreshold / goodThreshold + maxThreshold 可选）
3. 阈值 CRUD API

## 数据库

### 1. 摸清 realtime_data 表结构
```sql
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'realtime_data' ORDER BY ordinal_position;
```
记录字段，确认 `realtime_data`（JSON text）里有哪些 occupancy 相关 key：
- `occupancyRate` / `occupancy` / `efficiency`

### 2. 创建 system_config 表（如不存在）
```sql
CREATE TABLE IF NOT EXISTS public.system_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(64) UNIQUE NOT NULL,
  config_value VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入上座率阈值默认
INSERT INTO public.system_config (config_key, config_value, description) VALUES
  ('occupancy.warn_threshold', '80', '上座率黄色阈值（低于此值为红）'),
  ('occupancy.good_threshold', '95', '上座率绿色阈值（高于此值为绿）'),
  ('occupancy.refresh_interval', '5', '前端刷新间隔（秒）'),
  ('occupancy.show_value', 'true', '格子内是否显示真实值（true/false）')
ON CONFLICT (config_key) DO NOTHING;
```

## API 列表

### `GET /web/occupancy/snapshot`
- 参数：`lineNo`（可选，不过滤则返回全部）
- 返回：
```json
{
  "success": true,
  "data": {
    "lines": [
      {
        "lineNo": "line1A",
        "faces": [
          {"faceNo": "A1", "value": 99.8, "updateTime": "2026-08-01 10:30:00"},
          {"faceNo": "A2", "value": 99.5, ...},
          {"faceNo": "B1", "value": 100.0, ...},
          {"faceNo": "B2", "value": 99.7, ...}
        ],
        "avgValue": 99.75
      },
      ... 38 条
    ],
    "thresholds": {
      "warnThreshold": 80,
      "goodThreshold": 95,
      "refreshInterval": 5,
      "showValue": true
    },
    "summary": {
      "totalLines": 38,
      "redCount": 2,
      "yellowCount": 5,
      "greenCount": 145
    }
  }
}
```
- 字段说明：
  - `value`：上座率百分比（0-100，保留 1 位小数）
  - `color` 由前端根据 thresholds 计算（不在后端算，前端控制）
  - `summary` 颜色统计由前端算还是后端算？**后端算**（一次返回，避免前端遍历）

### `GET /web/occupancy/thresholds`
- 返回 4 个阈值（从 system_config 读）

### `POST /web/occupancy/thresholds`
- Body：`{"warnThreshold": 80, "goodThreshold": 95, "refreshInterval": 5, "showValue": true}`
- 写回 system_config
- 权限：super_admin / system_admin（用现有权限注解 @PreAuthorize）

## 实现要点
- **数据源**：从 `public.line.realtime_data` 读 JSON
- **line 表 38 条**（W-LINE-REG 已补齐）—— JOIN 拉全 38 条，没数据的填 0
- **列名映射**：参考 `src/main/java/.../mapper/StatusRecordMapper.java` 的 JSON 解析
- **阈值持久化**：复用现有 `system_config` mapper（如无则新建）

## 性能
- 152 格单次返回 < 100KB
- 加 5s 缓存（Caffeine 60s TTL），减轻 DB 压力

## 输出物
- 新表/迁移 SQL：`docs/work-orders/W-FRONT-05-B1-schema.sql`
- Service：`OccupancyService.java` + `OccupancyController.java` + `SystemConfigService.java`（如无）
- Mapper XML（如有）：`OccupancyMapper.xml` + `SystemConfigMapper.xml`
- 编译通过：`javac -d target/classes -cp '...' $(find src/main/java -name '*.java')`
- 端到端 curl 验证：
  - `GET /web/occupancy/snapshot` → 200 + 38 条 lines
  - `GET /web/occupancy/thresholds` → 200 + 4 个配置
  - `POST /web/occupancy/thresholds` → 200
- 报告：`docs/work-orders/W-FRONT-05-B1-report.md`

## 耗时上限
1.5 小时

## 边界
- **前端不写**（留给 B2/B3）
- **不上 WS**（留给 B4）
- 不动 realtime_data 表结构
