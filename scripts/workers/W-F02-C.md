# W-F02-C：V1.20 SQL migration

## 范围（2 个动作）
1. 新增 `E:\DEMO\数据采集\DataupLoad\src\main\resources\sql\V1.20__ignore_alarm.sql`
2. 改 `E:\DEMO\数据采集\DataupLoad\config\application-prod.yml` 的 `baseline-version: 1.19` → `1.20`

## 任务
建 ignore_alarm 表，让 Flyway 在启动时建表。

## 依据
PSM `V1.20` SQL 不在反编译产物里（PM 之前看的是 1.0~1.19），需要按 IgnoreAlarmPO 字段自建。

## 实现要点

### V1.20__ignore_alarm.sql
```sql
-- 忽略报警配置表
drop table if exists public.ignore_alarm;
create table public.ignore_alarm (
    id serial primary key,
    ignore_all int default 2,
    face_id varchar(20),
    line_no varchar(20),
    face_no varchar(20),
    type int,
    defect_name varchar(50),
    start_time varchar(19),
    end_time varchar(19),
    create_time timestamp not null default current_timestamp,
    update_time timestamp not null default current_timestamp
);

create index idx_ignore_alarm_lookup on public.ignore_alarm(line_no, face_no, type, defect_name);

create trigger t_ignore_alarm before update on public.ignore_alarm for each row execute procedure upd_timestamp();

comment on table public.ignore_alarm is '忽略报警配置表';
comment on column public.ignore_alarm.id is '主键';
comment on column public.ignore_alarm.ignore_all is '是否全部忽略 1-是 2-否';
comment on column public.ignore_alarm.face_id is '线体ID';
comment on column public.ignore_alarm.line_no is '线体编号';
comment on column public.ignore_alarm.face_no is 'AB面编号';
comment on column public.ignore_alarm.type is '报警类型 1-缺陷 2-系统 3-设备';
comment on column public.ignore_alarm.defect_name is '缺陷名称';
comment on column public.ignore_alarm.start_time is '忽略开始时间';
comment on column public.ignore_alarm.end_time is '忽略结束时间';
```

### application-prod.yml 修改
```yaml
spring:
  flyway:
    baseline-version: 1.20  # 原来是 1.19
    baseline-description: 'PSM 19 versions + ignore_alarm 1.20 pre-existing in DB'
```

⚠️ 注意：baseline-version 必须改，否则 Flyway 不会跑 V1.20
⚠️ 不要在 prod 实际跑（hik-java 不能重启），只改文件

## 验证
- SQL 文件语法 OK（不实际跑）
- application-prod.yml 修改后看 git diff 确认

## 约束
- 不准重启 hik-java（baseline 升级会让 Flyway 报错）
- 不准删任何现有 SQL
- 改完 yml 后在群里说"已改 yml baseline，等老板 OK 再重启"

## 回报格式
```
W-F02-C 完成：[success/partial/blocked]
文件：[2 个相对路径]
改动摘要：[V1.20 SQL 内容 + yml baseline 改 1.19→1.20]
风险：[重启时 Flyway baseline 是否会冲突]
建议测试：[怎么验证]
```
