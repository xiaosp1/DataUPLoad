--- 设备状态变更表
drop table if exists public.state_change;
create table public.state_change (
	id serial primary key,
    line_id int not null,
	type int not null,
	change_time timestamp not null ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.state_change is '设备状态变更表';
comment on column public.state_change.id is '主键id';
comment on column public.state_change.line_id is '线';
comment on column public.state_change.type is '类型(0:下线，1:上线)';
comment on column public.state_change.change_time is '变更时间';
comment on column public.state_change.update_time is '更新时间';
comment on column public.state_change.create_time is '创建时间';

create trigger t_state_change before update on public.state_change for each row execute procedure upd_timestamp();

--- 设备状态统计
drop table if exists public.state_statistic;
create table public.state_statistic (
	id serial   primary key,
	line_id     int not null,
	ok_time     int8 not null default 0,
	error_time  int8 not null default 0,
	statistic_time  timestamp not null,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.state_statistic is '设备状态统计';
comment on column public.state_statistic.id is '主键id';
comment on column public.state_statistic.line_id is '面';
comment on column public.state_statistic.ok_time is 'OK时长';
comment on column public.state_statistic.error_time is '异常时长';
comment on column public.state_statistic.statistic_time is '统计时间';
comment on column public.state_statistic.update_time is '更新时间';
comment on column public.state_statistic.create_time is '创建时间';

create trigger t_state_statistic before update on public.state_statistic for each row execute procedure upd_timestamp();
CREATE UNIQUE INDEX statistic_idx ON public.state_statistic (line_id,statistic_time);

ALTER TABLE public.status_record ADD line_id int NULL;