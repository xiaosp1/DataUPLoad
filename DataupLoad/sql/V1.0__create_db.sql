--- 检测数据表
drop table if exists public.defect_record;
create table public.defect_record (
	id serial primary key,
    line_no varchar(20) not null,
	face_no varchar(20) not null,
	glove_no varchar(20) not null,
	result int not null,
	defect_type varchar(20) not null,
	img_list text not null,
	time timestamp not null ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.defect_record is '检测数据表';
comment on column public.defect_record.id is '主键id';
comment on column public.defect_record.line_no is '线体编号';
comment on column public.defect_record.face_no is 'AB面编号';
comment on column public.defect_record.glove_no is '手套编号';
comment on column public.defect_record.result is '检测结果 1-良品 2-次品';
comment on column public.defect_record.defect_type is '缺陷类型';
comment on column public.defect_record.time is '检测时间';
comment on column public.defect_record.img_list is '检测图片信息';
comment on column public.defect_record.update_time is '更新时间';
comment on column public.defect_record.create_time is '创建时间';

create index index_join on public.defect_record(time,line_no,defect_type,result,face_no);
create trigger t_defect_record before update on public.defect_record for each row execute procedure upd_timestamp();

--- 车间单日检测数据汇总表
drop table if exists public.workshop_day_record;
create table public.workshop_day_record (
	id serial primary key,
	right_count int default 0,
	error_count int default 0,
	need_count int default 0,
	time varchar(19) not null ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.workshop_day_record is '车间单日检测数据汇总表';
comment on column public.workshop_day_record.id is '主键id';
comment on column public.workshop_day_record.right_count is '良品数量';
comment on column public.workshop_day_record.error_count is '次品数量';
comment on column public.workshop_day_record.need_count is '需达成数量';
comment on column public.workshop_day_record.time is '汇总日期';
comment on column public.workshop_day_record.update_time is '更新时间';
comment on column public.workshop_day_record.create_time is '创建时间';

create trigger t_workshop_day_record before update on public.workshop_day_record for each row execute procedure upd_timestamp();

--- 生产线单日检测数据汇总表
drop table if exists public.line_day_record;
create table public.line_day_record (
	id serial primary key,
	right_count int default 0,
    error_count int default 0,
	line_no varchar(20) not null,
    time varchar(19) not null ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.line_day_record is '生产线单日检测数据汇总表';
comment on column public.line_day_record.id is '主键id';
comment on column public.line_day_record.right_count is '良品数量';
comment on column public.line_day_record.error_count is '次品数量';
comment on column public.line_day_record.line_no is '线体编号';
comment on column public.line_day_record.time is '汇总日期';
comment on column public.line_day_record.update_time is '更新时间';
comment on column public.line_day_record.create_time is '创建时间';

create trigger t_line_day_record before update on public.line_day_record for each row execute procedure upd_timestamp();

--- 每日缺陷数量汇总表
drop table if exists public.defect_day_record;
create table public.defect_day_record (
	id serial primary key,
	count int default 0,
	time varchar(19) not null ,
	line_no varchar(20) not null,
	type varchar(20) not null,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.defect_day_record is '每日缺陷数量汇总表';
comment on column public.defect_day_record.id is '主键id';
comment on column public.defect_day_record.count is '数量';
comment on column public.defect_day_record.time is '汇总日期';
comment on column public.defect_day_record.line_no is '线体编号';
comment on column public.defect_day_record.type is '缺陷类型';
comment on column public.defect_day_record.update_time is '更新时间';
comment on column public.defect_day_record.create_time is '创建时间';

create trigger t_defect_day_record before update on public.defect_day_record for each row execute procedure upd_timestamp();

--- 报警记录表
drop table if exists public.alarm_record;
create table public.alarm_record (
	id serial primary key,
	uuid varchar(36) not null,
	time varchar(19) not null ,
	type int not null,
	line_no varchar(20) not null,
	face_no varchar(5) not null,
	level int not null,
	message varchar(50) not null,
	solve int default 2,
	reason int ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.alarm_record is '报警记录表';
comment on column public.alarm_record.id is '主键';
comment on column public.alarm_record.uuid is '报警唯一标识';
comment on column public.alarm_record.time is '报警时间';
comment on column public.alarm_record.type is '报警类型 1-缺陷 2-系统 3-设备';
comment on column public.alarm_record.line_no is '线体编号';
comment on column public.alarm_record.level is '报警级别 1-一般 2-严重';
comment on column public.alarm_record.message is '报警消息';
comment on column public.alarm_record.solve is '是否解决 1-是 2-否';
comment on column public.alarm_record.reason is '报警原因 1-客户端掉线';
comment on column public.alarm_record.update_time is '更新时间';
comment on column public.alarm_record.create_time is '创建时间';

create trigger t_alarm_record before update on public.alarm_record for each row execute procedure upd_timestamp();

--- 设备状态记录表
drop table if exists public.status_record;
create table public.status_record (
    id serial primary key,
    time varchar(19) not null ,
    type int not null,
    line_no varchar(20) not null,
    face_no varchar(20) not null,
    status int not null default 1,
    device_no varchar(20) not null,
    update_time timestamp not null default current_timestamp,
    create_time timestamp not null default current_timestamp
);

comment on table public.status_record is '设备状态记录表';
comment on column public.status_record.id is '主键';
comment on column public.status_record.time is '上报时间';
comment on column public.status_record.type is '设备类型 1-相机 2-剔除机 3-客户端';
comment on column public.status_record.line_no is '线体编号';
comment on column public.status_record.face_no is 'AB面编号';
comment on column public.status_record.status is '硬件状态 1-在线2-掉线';
comment on column public.status_record.device_no is '设备编号';
comment on column public.status_record.update_time is '更新时间';
comment on column public.status_record.create_time is '创建时间';

create trigger t_status_record before update on public.status_record for each row execute procedure upd_timestamp();

--- 方案表
drop table if exists public.plan;
create table public.plan (
	id serial primary key,
	name varchar(20) not null,
	uri varchar(50) not null,
	description varchar(200) ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.plan is '方案表';
comment on column public.plan.id is '主键';
comment on column public.plan.name is '方案名称';
comment on column public.plan.uri is '方案存储路径';
comment on column public.plan.description is '方案描述';
comment on column public.plan.update_time is '更新时间';
comment on column public.plan.create_time is '创建时间';

create trigger t_plan before update on public.plan for each row execute procedure upd_timestamp();

--- 生产线表
drop table if exists public.line;
create table public.line (
	id serial primary key,
	name varchar(20) not null,
	line_no varchar(20) not null,
	face_no varchar(20) not null,
	client_no varchar(20) not null,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.line is '生产线表';
comment on column public.line.id is '主键';
comment on column public.line.name is '生产线名称';
comment on column public.line.line_no is '生产线编号';
comment on column public.line.face_no is 'AB面编号';
comment on column public.line.client_no is '客户端编号';
comment on column public.line.update_time is '更新时间';
comment on column public.line.create_time is '创建时间';

create trigger t_line before update on public.line for each row execute procedure upd_timestamp();

--- 方案与生产线关联表
drop table if exists public.plan_to_line;
create table public.plan_to_line (
	id serial primary key,
	line_id int not null,
	plan_id int not null,
	status int default 2,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.plan_to_line is '方案与生产线关联表';
comment on column public.plan_to_line.id is '主键';
comment on column public.plan_to_line.line_id is '生产线Id';
comment on column public.plan_to_line.plan_id is '方案Id';
comment on column public.plan_to_line.status is '是否启用 1-是 2-否';
comment on column public.plan_to_line.update_time is '更新时间';
comment on column public.plan_to_line.create_time is '创建时间';

create trigger t_plan_to_line before update on public.plan_to_line for each row execute procedure upd_timestamp();