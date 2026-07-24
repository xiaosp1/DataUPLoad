-----缺陷类型
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

create index defect_day_record_time_idx on public.defect_day_record ("time");