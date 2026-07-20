# 06. PSM 数据库 Schema

> 本文是从 PSM Flyway 迁移脚本（V1.0 ~ V1.19）反向梳理出来的 PSM PostgreSQL 表结构。
> 数据库：`jdbc:postgresql://127.0.0.1:5432/intco`
>
> ⚠️ **注意**：SQL 脚本里的中文注释**全是乱码**（UTF-8 字节被当 GBK 解码了），这是 PSM 自带的问题，**不影响字段名/类型**。

---

## 📊 表清单（22 张）

### 核心业务表

| 表名 | 用途 | 来源脚本 |
|---|---|---|
| `defect_record` | 缺陷记录（每次检测的缺陷数据） | V1.0 |
| `defect_record_backup` | 缺陷记录备份 | V1.6 |
| `workshop_day_record` | 车间单日检测数据汇总 | V1.0 |
| `line_day_record` | 产线单日检测数据汇总 | V1.0 |
| `defect_day_record` | 每日缺陷数量汇总 | V1.0 |
| `alarm_record` | 报警记录 | V1.0 |
| `status_record` | 设备状态记录 | V1.0 |
| `plan` | 方案/配方表 | V1.0 |
| `plan_to_line` | 方案与产线关联表 | V1.0 |
| `line` | 产线表 | V1.0 |
| `line_order` | 产线顺序表 | V1.4 |
| `defect_type` | 缺陷类型定义 | V1.2 |
| `line_defect_type` | 产线缺陷类型 | V1.16 |
| `ignore_alarm` | 忽略报警 | V1.14 |
| `system_config` | 系统配置 | V1.14 |
| `state_change` | 设备状态变更 | V1.19 |
| `state_statistic` | 设备状态统计 | V1.19 |

### 权限/系统表（Flyway 之前的脚本建的，SQL 没看到）

- `role` — 角色（V1.1 update 语句里出现过）
- `user` — 用户
- `white_ip` — IP 白名单（V1.7 INSERT 过）

---

## 📋 核心表字段详单

### 1. `defect_record` — 缺陷记录表（最大，核心）

```sql
create table public.defect_record (
    id          serial primary key,        -- 主键
    line_no     varchar(20) not null,      -- 产线编号
    face_no     varchar(20) not null,      -- AB面编号
    glove_no    varchar(20) not null,      -- 手套编号
    result      int not null,              -- 检测结果 1=良品 2=次品
    defect_type varchar(20) not null,      -- 缺陷类型
    img_list    text not null,             -- 检测图片信息（JSON 字符串）
    time        timestamp not null,        -- 检测时间
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp,
    except_flag int not null default 1     -- 是否被剔除（V1.3）
);

create index index_join on public.defect_record(time, line_no, defect_type, result, face_no);
```

**说明**：这是 PSM 接收设备推过来的**原始缺陷数据**，每条记录代表一次检测。`img_list` 是 JSON 字符串存图片路径。

---

### 2. `alarm_record` — 报警记录表（★ 关键）

```sql
create table public.alarm_record (
    id          serial primary key,
    uuid        varchar(36) not null,      -- 报警唯一标识（去重用）
    time        varchar(19) not null,      -- 报警时间 'yyyy-MM-dd HH:mm:ss'
    type        int not null,              -- 报警类型 1=缺陷 2=系统 3=设备
    line_no     varchar(20) not null,      -- 产线编号
    face_no     varchar(5) not null,       -- AB面编号
    level       int not null,              -- 报警级别 1=一般 2=严重
    message     varchar(1000) not null,    -- 报警消息（V1.5 扩到 1000）
    solve       int default 2,             -- 是否解决 1=是 2=否
    reason      int,                       -- 报警原因 1=客户端掉线
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp,
    defect_name varchar(128)               -- V1.11 新增：缺陷名称（关联 defect_type.name）
);

create index alarm_record_defect_name_idx on public.alarm_record(defect_name, solve, line_no, face_no);
```

**说明**：
- `uuid` 是 PSM 去重关键字段，**我们 EdgeHost 推报警时必须生成稳定 UUID**
- `type` 枚举：**1=缺陷，2=系统，3=设备**
- `level` 枚举：**1=一般，2=严重**
- `solve` 枚举：**1=已解决，2=未解决**
- `defect_name` 是冗余字段（V1.11 后才加），方便按缺陷名搜索

---

### 3. `line` — 产线表

```sql
create table public.line (
    id          serial primary key,
    name        varchar(20) not null,      -- 产线名称
    line_no     varchar(20) not null,      -- 产线编号
    face_no     varchar(20) not null,      -- AB面编号
    client_no   varchar(20) not null,      -- 客户端编号
    realtime_data text,                    -- V1.9 新增：实时数据（JSON 字符串）
    color       varchar(20),               -- V1.14 新增：产线颜色
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);
```

**说明**：`realtime_data` 是 V1.9 后加的 JSON 字段，存产线实时数据。

---

### 4. `plan` + `plan_to_line` — 方案/配方

```sql
-- 方案
create table public.plan (
    id          serial primary key,
    name        varchar(20) not null,      -- 方案名称
    uri         varchar(50) not null,      -- 方案存储路径
    description varchar(200),              -- 方案描述
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);

-- 方案与产线关联
create table public.plan_to_line (
    id          serial primary key,
    line_id     int not null,              -- 产线 ID
    plan_id     int not null,              -- 方案 ID
    status      int default 2,             -- 是否启用 1=是 2=否
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);
```

---

### 5. `defect_type` — 缺陷类型定义（★ 字典核心）

```sql
create table public.defect_type (
    id              serial primary key,
    name            varchar(32) not null,         -- 缺陷名称
    category        int not null default 3,       -- 分类 1=破损 2=脏污 3=其他
    count_enable    bool not null default false,  -- 是否统计缺陷计数
    count_threshold int4 not null default 0,      -- 缺陷计数阈值
    rate_enable     bool not null default false,   -- 是否统计缺陷率
    show_img_enable bool not null default false,  -- V1.3 是否显示图片
    alarm_enable    int not null default 0,       -- V1.14 是否报警 (1:报警, 0:不报警)
    sound_enable    int not null default 0,       -- V1.14 是否播音 (1:是, 0:否)
    send_yk_enable  int not null default 0,       -- V1.14 是否发送给英科 (1:是, 0:否)
    update_time     timestamp default current_timestamp,
    create_time     timestamp default current_timestamp
);
```

**说明**：
- V1.14 后 defect_type 加了**报警/声音/英科推送**三个开关，**这就是 PSM 的"是否推英科"配置**
- "是否发送给英科" 的逻辑：**某 defect_type 的 send_yk_enable=1**，PSM 收到对应报警就推给英科

---

### 6. `line_defect_type` — 产线-缺陷关联（V1.16）

```sql
create table public.line_defect_type (
    id          serial primary key,
    name        varchar(32) not null,      -- 缺陷名称
    line_no     varchar(128) not null,     -- 产线名称
    face_no     varchar(128) not null,     -- 面名称
    show_flag   int not null default 0,    -- 是否展示(0:不展示, 1:展示)
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);
```

**说明**：把 defect_type 按产线+面分组，决定哪个产线展示哪些缺陷。

---

### 7. `status_record` — 设备状态记录

```sql
create table public.status_record (
    id          serial primary key,
    time        varchar(19) not null,      -- 上报时间 'yyyy-MM-dd HH:mm:ss'
    type        int not null,              -- 设备类型 1=相机 2=剔除机 3=客户端
    line_no     varchar(20) not null,      -- 产线编号
    face_no     varchar(20) not null,      -- AB面编号
    status      int not null default 1,    -- 硬件状态 1=在线 2=掉线
    device_no   varchar(20) not null,      -- 设备编号
    device_name varchar(128),              -- V1.10 新增：设备名称
    line_id     int,                       -- V1.19 新增：产线 ID
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);
```

---

### 8. `defect_day_record` + `line_day_record` — 每日统计

```sql
-- 每日缺陷数量汇总
create table public.defect_day_record (
    id          serial primary key,
    count       int default 0,
    time        varchar(19) not null,      -- 汇总日期
    line_no     varchar(20) not null,
    type        varchar(20) not null,
    face_no     varchar(100),              -- V1.8 新增
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);

-- 产线单日检测数据汇总
create table public.line_day_record (
    id                 serial primary key,
    right_count        int default 0,
    error_count        int default 0,
    line_no            varchar(20) not null,
    time               varchar(19) not null,
    face_no            varchar(100),              -- V1.8 新增
    remove_total       int default 0,             -- V1.17 新增
    upload_remove_total int default 0,            -- V1.18 新增
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);
```

---

### 9. `ignore_alarm` — 忽略报警（V1.14）

```sql
create table public.ignore_alarm (
    id          serial primary key,
    defect_name varchar(128) not null,
    type        int not null,
    line_no     varchar(20) not null,
    face_no     varchar(20) not null,
    ignore_time timestamp not null,
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);

-- V1.17 加了唯一索引
CREATE UNIQUE INDEX ignore_alarm_type_idx ON public.ignore_alarm(type, line_no, face_no, defect_name);
```

---

### 10. `system_config` — 系统配置（V1.14）

```sql
create table public.system_config (
    id           serial primary key,
    config_name  varchar(128) not null,    -- 配置名称
    config_key   varchar(128) not null,    -- 配置标识
    config_value varchar(128) not null,    -- 配置取值
    update_time  timestamp default current_timestamp,
    create_time  timestamp default current_timestamp
);

-- 默认配置（V1.17）
INSERT INTO public.system_config(config_name, config_key, config_value) VALUES
('设备报警音频', 'device_alarm_sound_uri', '/data/sound/default.mp3'),
('缺陷报警音频', 'defect_alarm_sound_uri', '/data/sound/default.mp3'),
('系统报警音频', 'system_alarm_sound_uri', '/data/sound/default.mp3'),
('重复播放次数', 'sound_play_count', '1');
```

---

### 11. `state_change` + `state_statistic` — 状态变更/统计（V1.19）

```sql
create table public.state_change (
    id          serial primary key,
    line_id     int not null,
    type        int not null,               -- 类型(0:下线, 1:上线)
    change_time timestamp not null,
    update_time timestamp default current_timestamp,
    create_time timestamp default current_timestamp
);

create table public.state_statistic (
    id             serial primary key,
    line_id        int not null,
    ok_time        int8 not null default 0,  -- OK 时长
    error_time     int8 not null default 0,  -- 异常时长
    statistic_time timestamp not null,
    update_time    timestamp default current_timestamp,
    create_time    timestamp default current_timestamp
);

CREATE UNIQUE INDEX statistic_idx ON public.state_statistic(line_id, statistic_time);
```

---

## 📅 迁移脚本时序（按 Flyway 版本号）

| 版本 | 改动 | 重要性 |
|---|---|---|
| **V1.0** | 创建 10 张核心表（defect_record, alarm_record, line, plan...） | ⭐ |
| V1.1 | role 表权限更新 | - |
| V1.2 | defect_type 表 | - |
| V1.3 | defect_type.show_img_enable + defect_record.except_flag | - |
| V1.4 | line_order 表 | - |
| V1.5 | alarm_record.message 字段扩到 1000 | - |
| V1.6 | defect_record_backup 表 | - |
| V1.7 | white_ip 默认 '*.*.*.*' | - |
| V1.8 | defect_day_record + line_day_record 加 face_no | - |
| V1.9 | line 加 realtime_data | - |
| V1.10 | status_record 加 device_name | - |
| V1.11 | alarm_record 加 defect_name | - |
| V1.13 | alarm_record_defect_name_idx 索引 | - |
| **V1.14** | defect_type 加 alarm/sound/send_yk 三个开关 + ignore_alarm + system_config | ⭐ |
| V1.15 | system_config 加 sound-play-count | - |
| V1.16 | line_defect_type 表 | - |
| V1.17 | line_day_record 加 remove_total + system_config 重建 + ignore_alarm 唯一索引 | - |
| V1.18 | line_day_record 加 upload_remove_total | - |
| **V1.19** | state_change + state_statistic 表 + status_record 加 line_id | ⭐ |

---

## 🔍 现场 EdgeHost 表对照

**EdgeHost（SQLite, D:\IntcoEdge\data\intco.db）**：

| EdgeHost 表 | PSM 对应表 | 差异 |
|---|---|---|
| `defect_record` | `defect_record` | ✅ 字段基本一致（EdgeHost 多一些 MES 推送相关字段） |
| `defect_day_record` | `defect_day_record` | ✅ 一致 |
| `alarm_record` | `alarm_record` | ✅ EdgeHost 多 uuid 字段 |
| `line` | `line` | ✅ 一致 |
| `defect_type` | `defect_type` | ✅ 一致 |
| `line_defect_type` | `line_defect_type` | ✅ 一致 |
| `mes_alarm_push_log` | （无对应） | EdgeHost 自己的推送日志 |

**结论**：EdgeHost 的 SQLite schema 跟 PSM 的 PostgreSQL schema **基本 1:1 对应**。这意味着：

1. EdgeHost 可以做"PSM 数据本地镜像"，方便 MES 查询不需要实时打 PSM
2. EdgeHost 的 sqlite.db → PSM 的 postgres 同步是**可能的双向同步场景**
3. 我们的 DTO 字段可以两边都兼容

---

## ⚠️ 已知问题

1. **SQL 注释乱码**：PSM 自带 PG 初始化时编码可能是 GBK 而不是 UTF-8，导致 SQL 注释里的中文全是乱码。**字段名/类型/默认值 不受影响**。
2. **schema 不完整**：PSM 2.1.9 的最新字段可能比 V1.19 多（service 实现里 @TableField 注解可能加了新字段，反编译看不到）。需要连 PSM PG 实例 `\d table` 验证。
3. **没有看到 system_config 完整配置项**：V1.14/V1.17 只初始化了 4 个，PSM 实际可能更多（运行中动态写入）。

## 🔌 验证方式

要拿到 PSM 完整 schema，需要连 PSM 的 PG 实例执行 `\d`：

```bash
psql -h 127.0.0.1 -p 5432 -U postgres -d intco
\dt                  -- 列出所有表
\d defect_record     -- 列出表字段
\d alarm_record
```
