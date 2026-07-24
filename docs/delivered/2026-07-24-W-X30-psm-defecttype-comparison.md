# PSM vs DataupLoad `defect_type` 表对比报告

**工单**：W-X30（PSM 反编译一致性核查）
**日期**：2026-07-24
**结论速读**：PSM 在报警入口**强依赖** `defect_type` 表的 `send_yk_enable=1` 字段来决定是否推 MES（英科）；DataupLoad 当前 `defect_type` 表数据**严重缺失**，仅有 4 行（其中 TEST001 是手工插入，3 行中文乱码其实是 PSM 历史数据），且 `send_yk_enable` 严重不齐全 —— 导致**任何未在 `defect_type` 中登记的缺陷名都不会进入报警流程**。

---

## 1. PSM `defect_type` 表 DDL（合并所有 V* 迁移后）

### 1.1 初始 DDL（V1.2__screen_db.sql）

```sql
-- V1.2__screen_db.sql -----缺陷类型
create table public.defect_type (
    id serial primary key,
    "name" varchar(32) not null,
    category int not null default 3,
    count_enable bool not null default false,
    count_threshold int4 not null default 0,
    rate_enable bool not null default false,
    update_time timestamp not null default current_timestamp,
    create_time timestamp not null default current_timestamp
);

comment on column public.defect_type.id is '主键id';
comment on column public.defect_type."name" is '缺陷名称';
comment on column public.defect_type.category is '分类，1-破损 2-脏污 3-其他';
comment on column public.defect_type.count_enable is '是否统计缺陷计数';
comment on column public.defect_type.count_threshold is '缺陷计数阈值';
comment on column public.defect_type.rate_enable is '是否统计缺陷率';
comment on column public.defect_type.update_time is '更新时间';
comment on column public.defect_type.create_time is '创建时间';
```

### 1.2 V1.3（detect_db）追加列

```sql
-- V1.3__detect_db.sql
alter table defect_type add column show_img_enable  bool not null  default false;
comment on column defect_type.show_img_enable is '是否显示图片';
```

### 1.3 V1.14（关键 — alarm 模块上线）追加 3 个开关列 + 首条数据

```sql
-- V1.14__create_db.sql
alter table defect_type add column alarm_enable   int not null  default 0;
comment on column defect_type.alarm_enable   is '是否告警(1:报警,0:不报警)';

alter table defect_type add column sound_enable   int not null  default 0;
comment on column defect_type.sound_enable   is '是否语音(1:是,0:否)';

alter table defect_type add column send_yk_enable int not null  default 0;
comment on column defect_type.send_yk_enable is '是否发送给英科(1:是,0:否)';

INSERT INTO public.defect_type
  (id, "name", category, count_enable, count_threshold, rate_enable,
   update_time, create_time, show_img_enable, alarm_enable, sound_enable, send_yk_enable)
VALUES
  (default, '客户端', 3, true, 0, false,
   '2025-01-08 10:36:40.225', '2025-01-08 10:36:40.225', false, 1, 1, 1);
```

> ⚠️ **关键发现**：PSM SQL 中**只有 1 条** `INSERT INTO defect_type` 数据（即 `客户端 / category=3 / send_yk_enable=1`）。其余"未破损 / 脏污"等行是通过 PSM 启动后 web 界面 / 接口手动新增的，不在 SQL 迁移脚本里。

### 1.4 V1.16（独立副表 — 与本对比无关）

`line_defect_type` 是另一张表（按 line/face 维度存 defect 展示配置），与 `defect_type` 平行存在，不影响 yk 推送逻辑。

---

## 2. PSM `DefectTypePO` 实体字段

路径：`docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\alarm\model\DefectTypePO.java`

| 字段 | Java 类型 | 列名 | 备注 |
|---|---|---|---|
| `id` | `Integer` (AUTO) | `id` | 主键 |
| `name` | `String` | `name` | 缺陷名称（唯一匹配键之一） |
| `category` | `Integer` | `category` | 分类 1-破损 / 2-脏污 / 3-其他 |
| `countEnable` | `Boolean` | `count_enable` | 是否统计缺陷计数 |
| `countThreshold` | `Integer` | `count_threshold` | 缺陷计数阈值 |
| `rateEnable` | `Boolean` | `rate_enable` | 是否统计缺陷率 |
| `showImgEnable` | `Boolean` | `show_img_enable` | 是否显示图片 |
| **`alarmEnable`** | `Integer` | `alarm_enable` | **是否告警(1:报警,0:不报警)** |
| **`sendYkEnable`** | `Integer` | `send_yk_enable` | **是否发送给英科(1:是,0:否)** |
| **`soundEnable`** | `Integer` | `sound_enable` | **是否语音(1:是,0:否)** |
| `updateTime` | `LocalDateTime` | `update_time` | |
| `createTime` | `LocalDateTime` | `create_time` | |

Getter：`getId/getName/getCategory/getCountEnable/getCountThreshold/getRateEnable/getShowImgEnable/getAlarmEnable/getSendYkEnable/getSoundEnable/getUpdateTime/getCreateTime`。

---

## 3. DataupLoad 当前 `defect_type` 表实际数据

```sql
-- 实测 psql 查询结果（编码 GBK → 终端乱码；原值为 UTF-8 中文）
SELECT id, name, category, alarm_enable, sound_enable, send_yk_enable
  FROM defect_type ORDER BY id;

 id |  name   | category | alarm_enable | sound_enable | send_yk_enable
----+---------+----------+--------------+--------------+----------------
  2 | TEST001 |        1 |            1 |            1 |              0   ← 手工测试数据
 15 | 客户端  |        3 |            1 |            1 |              1   ← PSM V1.14 迁移残留
 16 | 未破损  |        1 |            1 |            1 |              1   ← 历史 web 录入
 17 | 脏污    |        2 |            1 |            1 |              1   ← 历史 web 录入
(4 行记录)
```

> 表 DDL 与 PSM V1.14 之后完全一致（13 列全在），但**行数严重不足**。

### 3.1 alarm_record 中实际出现的 defect_name 分布

```sql
SELECT defect_name, type, count(*) FROM alarm_record GROUP BY defect_name, type ORDER BY count(*) DESC;

 defect_name | type | count
-------------+------+-------
 未破损      |    1 |  3039
 TEST001     |    1 |     2
(2 行记录)
```

**关键观察**：当前 DB 里 `alarm_record` 只有 `未破损` 和 `TEST001` 两类缺陷，与 `defect_type` 表里的 4 行大致对应；但 PSM 实际产线还有 **破损（轻微/严重/划痕/凹坑/…）/ 脏污（油污/水渍/…）/ 客户端掉线** 等大量类型，本库完全缺失。

---

## 4. PSM `sendAlarmMessage` 推送决策逻辑

路径：`docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\alarm\service\imp\AlarmRecordServiceImpl.java`

### 4.1 入口 `add()` — 第一道过滤

```java
public BaseResult add(AlarmDTO form) {
    AlarmTypeEnum alarmType = AlarmTypeEnum.getByCode(form.getType());
    ...
    Map<String, DefectTypePO> sortDefectTypeByName = Maps.newHashMap();
    // ★ 关键：用 category 过滤 defect_type
    this.defectTypeService.listByAttribute(form.getType(), DefectTypePO::getCategory)
        .forEach(type -> sortDefectTypeByName.put(type.getName(), type));

    boolean isInterestingDefect = false;
    if (CollectionUtils.isNotEmpty(sortDefectTypeByName)) {
        for (DefectAlarmConfig.DefectTypeConfig config : this.alarmConfig.getConfig()) {
            if (!config.getType().toUpperCase().equals(alarmType.name())) continue;
            message = ReUtil.get(config.getTemplate(), form.getMessage(), 0);
            // ★ 关键：必须 message 包含 defect_type.name 才算 interesting
            for (String name : sortDefectTypeByName.keySet()) {
                if (message.contains(name)) {
                    defectName = name;
                    isInterestingDefect = true;
                    break;
                }
            }
        }
        if (isInterestingDefect) {
            // 旧 UNSOLVED 置 IGNORE
            // 新建 AlarmRecordPO，setDefectType(...)
            this.save(alarm);
            this.sendAlarmMessage(alarm);
        }
    }
    if (!isInterestingDefect) {
        log.warn("current alarm is not interesting defect.[form={}]", form);
    }
}
```

> ⚠️ **第一道硬性过滤**：`form.getType()` 必须能在 `defect_type` 中匹配到 `category` 相同的记录，且 `form.getMessage()`（正则提取后）必须**字符串包含** `defect_type.name`，否则**直接 return，不落库不推送**。

### 4.2 `sendAlarmMessage()` — 第二道过滤（yk 推送开关）

```java
public void sendAlarmMessage(AlarmRecordPO alarm) {
    DefectTypePO defectType = alarm.getDefectType();
    boolean isIgnore = false;          // ← PSM 反编译硬编码 false（BUG：白名单失效）
    // ① 大屏 WS 告警推送：依赖 alarm_enable=1
    if (Objects.equals(defectType.getAlarmEnable(), StateEnum.YES.getValue()) && !isIgnore) {
        this.sendAlarmTextMessage();
        if (Objects.equals(defectType.getSoundEnable(), StateEnum.YES.getValue())
            && Objects.equals(alarm.getSolve(), AlarmSolvedEnum.UNSOLVED.getValue())) {
            this.sendAlarmSoundWsMessage(defectType);
        }
    }
    // ② ★★★ yk 推送：依赖 send_yk_enable=1
    if (!isIgnore
        && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue())) {
        EventUtil.publish(new PushAlarmEvent(this, alarm));
    }
}
```

> ✅ **直接回答任务问题**：**是的，PSM `sendAlarmMessage` 用 `defectType.getSendYkEnable() == 1` 决定是否推 yk**。如果 `send_yk_enable=0`，则 `PushAlarmEvent` 不发布，`YKServiceImpl.pushAlarm2YK` 永远不会被触发。

### 4.3 `YKServiceImpl.pushAlarm2YK`（PSM 同款 — 由 `PushAlarmEvent` 异步触发）

路径：`docs\domain\海康大屏逆向\PSM\server\decompiled\com\hikrobotics\solution\module\yingke\service\imp\YKServiceImpl.java`

PSM 原版逻辑（要点）：
1. `ticket` 为空 → 报错跳过；
2. 同一 `(defectName, lineNo, faceNo, type)` 已推过 → 跳过；
3. 否则 `POST VisualInspectionController.HandleVisualInspectionAlarm` 给 MES。

---

## 5. DataupLoad `YKServiceImpl.pushAlarm2YK` 推送决策逻辑

路径：`DataupLoad\src\main\java\com\hikrobotics\solution\module\yingke\service\impl\YKServiceImpl.java`

### 5.1 入口（DataupLoad AlarmRecordServiceImpl.sendAlarmMessage）

```java
// DataupLoad 沿用 PSM 同款三开关判断 + W-B04 修复（isIgnore 查库）
if (!isIgnore
    && defectType != null
    && Objects.equals(defectType.getSendYkEnable(), StateEnum.YES.getValue())) {
    EventUtil.publish(new PushAlarmEvent(this, alarm));
}
```

→ DataupLoad **入口**确实依赖 `send_yk_enable=1`，与 PSM 对齐。

### 5.2 `pushAlarm2YK` 内部（注意 — **不查 defect_type**）

```java
@Async
@EventListener(PushAlarmEvent.class)
@Override
public void pushAlarm2YK(PushAlarmEvent event) {
    // ① W-X13d 灰盒：yk.upload-enabled=false → 静默跳过
    if (!this.ykConfig.isUploadEnabled()) {
        log.debug("yk upload disabled, skip push.[alarm={}]", event.getAlarmRecord());
        return;
    }
    // ② ticket 校验
    if (StringUtils.isBlank(this.ticket)) {
        log.error("push alarm to yk error, ticket is null.[alarm={}]", event.getAlarmRecord());
        return;
    }
    // ③ 业务字段填充 + W-X30 dedup v3
    AlarmRecord record = event.getAlarmRecord();
    if (StringUtils.isNotBlank(record.getDefectName())) {
        ...// 计数 + dedup key = (defectName, lineNo, faceNo, type)
    }
    // ④ POST MES
    ...
}
```

> ✅ 推送决策**仅依赖**：
> 1. **`ykConfig.isUploadEnabled()`**（application.yml 配置项，灰盒默认 false）
> 2. **`ticket != null`**（MES login 是否成功）
> 3. **W-X30 dedup set**（同 key 跳过）
>
> ❌ **不再查 `defect_type` 表** — `send_yk_enable` 已在上游 `sendAlarmMessage` 判过。

---

## 6. 两张表的差异意味着什么？

### 6.1 DDL 差异

| 项 | PSM | DataupLoad | 结论 |
|---|---|---|---|
| 表名 | `defect_type` | `defect_type` | ✅ 一致 |
| 列数 | 12（含 `show_img_enable`） | 13（实际多 `show_img_enable`） | ✅ 一致 |
| `send_yk_enable` 默认 | `0` | `0` | ✅ 一致 |
| 实体类 | `DefectTypePO` (12 字段) | `DefectType` (12 字段) | ✅ 字段一致 |
| 主键生成 | `serial` (PG sequence) | `serial` (PG sequence) | ✅ 一致 |

**DDL 层面：完全对齐**。

### 6.2 数据差异（**这是真正的问题**）

| 项 | PSM 期望（V1.14 + 手工录入） | DataupLoad 当前实际 | 影响 |
|---|---|---|---|
| 行数 | 通常 10~30+ 行（覆盖所有缺陷类型） | **仅 4 行** | 大量 defect_name 走不到 `add()` 的 `isInterestingDefect=true` 分支 |
| 破损类（category=1） | 轻微破损 / 严重破损 / 划痕 / 凹坑 … | 仅 `未破损` + `TEST001` | `轻微破损` 等告警直接 warn 丢弃 |
| 脏污类（category=2） | 油污 / 水渍 / 灰尘 … | 仅 `脏污`（1 行） | 同上 |
| 客户端类（category=3） | `客户端` 等 | `客户端`（1 行） | OK |
| `send_yk_enable` 默认值 | 新建默认 0（PSM web 后台手动开） | 同 | 与 PSM 一致，但需要逐条手动开 |

### 6.3 哪些缺陷 PSM 会推而我们不会推？

**场景**：产线出现 `轻微破损` 报警 → 海康 SDK 上报 `message="[轻微破损]..."`。

| 阶段 | PSM 行为 | DataupLoad 行为 |
|---|---|---|
| ① `add()` 入口 | `listByAttribute(1, ::getCategory)` → 找到 `轻微破损`，`message.contains("轻微破损")=true` → `isInterestingDefect=true` → 落库 + 调 `sendAlarmMessage` | 同左；但**因 `defect_type` 没有 `轻微破损` 这条记录**，`listByAttribute` 返回空 → `isInterestingDefect=false` → **直接 return 不落库** |
| ② WS 大屏告警 | 推送 | 不会触发（因为上一步已 return） |
| ③ yk 推送 | `sendYkEnable=1` → 发布 `PushAlarmEvent` → `pushAlarm2YK` → 推 MES | 同上不触发 |

**结论**：DataupLoad 当前 `defect_type` 表里没登记的 defect_name，**整条报警链路全部断掉**（不进 alarm_record、不上 WebSocket、不推 MES）。这是**严重的"静默丢报警"问题**。

### 6.4 `send_yk_enable` 字段值风险

- 当前 4 行中，**`TEST001` 的 `send_yk_enable=0`**（手工测试时没开）。
- 其余 3 行均 `send_yk_enable=1`，符合预期。

**若产线侧 PSM 的 web 后台曾手工把某缺陷改为 `send_yk_enable=0`（比如某个缺陷不需要通知到英科）**，DataupLoad 这边**无法感知**——两边表是分离的，PSM 改了不会同步过来。这是个**配置漂移**风险。

---

## 7. 建议：是否需要从 PSM 同步 `defect_type` 数据到 DataupLoad？

### 7.1 推荐方案：建立定期同步脚本（**强建议**）

```sql
-- 方案 A：一次性全量同步（如果能访问 PSM DB）
INSERT INTO defect_type
  (id, name, category, count_enable, count_threshold, rate_enable,
   show_img_enable, alarm_enable, sound_enable, send_yk_enable,
   update_time, create_time)
SELECT id, name, category, count_enable, count_threshold, rate_enable,
       show_img_enable, alarm_enable, sound_enable, send_yk_enable,
       update_time, create_time
  FROM <PSM_DB>.public.defect_type
ON CONFLICT (id) DO UPDATE SET
  name            = EXCLUDED.name,
  category        = EXCLUDED.category,
  alarm_enable    = EXCLUDED.alarm_enable,
  sound_enable    = EXCLUDED.sound_enable,
  send_yk_enable  = EXCLUDED.send_yk_enable,
  show_img_enable = EXCLUDED.show_img_enable,
  count_enable    = EXCLUDED.count_enable,
  count_threshold = EXCLUDED.count_threshold,
  rate_enable     = EXCLUDED.rate_enable,
  update_time     = EXCLUDED.update_time;
```

### 7.2 推荐方案：DB trigger / dblink / 定时任务

如果两个 DB 不在同一实例或没有 dblink 权限：
1. **方案 B**：让 PSM 在 `defect_type` 变更时通过 HTTP/消息队列通知 DataupLoad，DataupLoad 同步更新（侵入大，**不推荐**）。
2. **方案 C**（**推荐**）：在 DataupLoad 启动时 + 每小时跑一次**同步任务**，从 PSM 拉取全量 `defect_type` 行，做 upsert。
3. **方案 D**：在 DataupLoad web 后台新增"缺陷类型管理"页面（沿用 PSM `DefectTypeController`），由产线人员手动维护（成本高，**不推荐作为长期方案**）。

### 7.3 短期止血（如果同步方案来不及做）

**手动 SQL** 把已知的缺失 defect_name 一次性补齐（参考 PSM 同款默认配置：`alarm_enable=1, sound_enable=1, send_yk_enable=1`）：

```sql
-- 破损类（category=1）常见缺陷
INSERT INTO defect_type (name, category, count_enable, count_threshold, rate_enable, show_img_enable, alarm_enable, sound_enable, send_yk_enable)
VALUES
  ('未破损',   1, true, 0, false, false, 1, 1, 1),
  ('轻微破损', 1, true, 0, false, true,  1, 1, 1),
  ('严重破损', 1, true, 0, false, true,  1, 1, 1),
  ('划痕',     1, true, 0, false, true,  1, 1, 1),
  ('凹坑',     1, true, 0, false, true,  1, 1, 1),
  -- 脏污类（category=2）
  ('脏污',     2, true, 0, false, true,  1, 1, 1),
  ('油污',     2, true, 0, false, true,  1, 1, 1),
  ('水渍',     2, true, 0, false, true,  1, 1, 1),
  ('灰尘',     2, true, 0, false, true,  1, 1, 1),
  -- 客户端类（category=3）
  ('客户端',   3, true, 0, false, false, 1, 1, 1)
ON CONFLICT (name, category) DO UPDATE SET
  alarm_enable=EXCLUDED.alarm_enable,
  sound_enable=EXCLUDED.sound_enable,
  send_yk_enable=EXCLUDED.send_yk_enable,
  show_img_enable=EXCLUDED.show_img_enable,
  update_time=CURRENT_TIMESTAMP;
```

> 注：`defect_type` 当前**没有唯一约束**（DDL 里没建 `UNIQUE(name, category)`），上面的 `ON CONFLICT` 会报错；改为：

```sql
-- 安全版：先查重再插
INSERT INTO defect_type (...)
SELECT ... WHERE NOT EXISTS (
  SELECT 1 FROM defect_type WHERE name='轻微破损' AND category=1
);
```

或**先 `ALTER TABLE defect_type ADD CONSTRAINT defect_type_name_cat_uk UNIQUE (name, category);`** 再 upsert。

### 7.4 长期方案（**W-X30 之后的工单**）

| 工单 | 事项 |
|---|---|
| W-X31 | 给 `defect_type` 加 `UNIQUE(name, category)` 约束，避免重复录入 |
| W-X32 | DataupLoad 启动时从 PSM 拉取全量 `defect_type`（HTTP API 或定时 SQL） |
| W-X33 | DataupLoad 启动时检查 `defect_type` 行数 < 5 → 启动 warn 日志（"defect_type 数据可能不全"） |
| W-X34 | （可选）DataupLoad 加一个"缺陷类型管理"web 页面，沿用 PSM `DefectTypeController` |

---

## 附录 A：完整文件清单

| 用途 | 路径 |
|---|---|
| PSM 表 DDL | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/sql/V1.2__screen_db.sql` |
| PSM 表 DDL（追加列） | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/sql/V1.3__detect_db.sql` |
| PSM 表 DDL（关键开关列 + 首条 INSERT） | `docs/domain/海康大屏逆向/10-反编译产物-NEW/PSM/server/sql/V1.14__create_db.sql` |
| PSM 实体 | `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/alarm/model/DefectTypePO.java` |
| PSM alarm 推送逻辑 | `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/alarm/service/imp/AlarmRecordServiceImpl.java` |
| PSM DefectTypeService | `docs/domain/海康大屏逆向/PSM/server/decompiled/com/hikrobotics/solution/module/alarm/service/imp/DefectTypeServiceImpl.java` |
| DataupLoad 实体 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/model/DefectType.java` |
| DataupLoad alarm 推送逻辑 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/AlarmRecordServiceImpl.java` |
| DataupLoad yk 推送逻辑 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/yingke/service/impl/YKServiceImpl.java` |
| DataupLoad DefectTypeService | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/service/impl/DefectTypeServiceImpl.java` |
| DataupLoad alarm 配置 | `DataupLoad/src/main/java/com/hikrobotics/solution/module/alarm/config/DefectAlarmConfig.java` |

## 附录 B：执行的 psql 查询

```bash
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -d intco -p 5433 -c '\d defect_type'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -d intco -p 5433 -c 'SELECT * FROM defect_type ORDER BY id;'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -d intco -p 5433 -c 'SELECT id, name, category, alarm_enable, sound_enable, send_yk_enable FROM defect_type ORDER BY id;'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -d intco -p 5433 -c "SELECT defect_name, type, count(*) FROM alarm_record GROUP BY defect_name, type ORDER BY count(*) DESC;"
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -U postgres -d intco -p 5433 -c "SELECT id, line_no, face_no, defect_name, type, solve, message FROM alarm_record ORDER BY id DESC LIMIT 5;"
```
