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
