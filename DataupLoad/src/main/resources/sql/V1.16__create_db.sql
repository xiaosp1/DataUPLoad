-----缺陷类型
create table public.line_defect_type (
	id serial primary key,
	"name" varchar(32) not null,
	line_no varchar(128) not null,
	face_no varchar(128) not null,
	show_flag int not null default 0,
    update_time timestamp not null default current_timestamp,
    create_time timestamp not null default current_timestamp
);

comment on column public.line_defect_type.id is '主键id';
comment on column public.line_defect_type."name" is '缺陷名称';
comment on column public.line_defect_type.show_flag is '是否展示(0:不展示，1: 展示)';
comment on column public.line_defect_type.line_no is '线体名称';
comment on column public.line_defect_type.face_no is '面名称';
comment on column public.line_defect_type.update_time is '更新时间';
comment on column public.line_defect_type.create_time is '创建时间';