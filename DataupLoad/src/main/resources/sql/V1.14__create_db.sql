alter table defect_type add column alarm_enable  int not null  default 0;
comment on column defect_type.alarm_enable is '是否告警(1:报警,0:不报警)';

alter table defect_type add column sound_enable  int not null  default 0;
comment on column defect_type.sound_enable is '是否语音(1:是,0:否)';

alter table defect_type add column send_yk_enable  int not null  default 0;
comment on column defect_type.send_yk_enable is '是否发送给英科(1:是,0:否)';

alter table line add column color  varchar(20);
comment on column line.color is '线体颜色';

INSERT INTO public.defect_type (id, "name", category, count_enable, count_threshold, rate_enable, update_time, create_time, show_img_enable,alarm_enable,sound_enable,send_yk_enable) VALUES(default, '客户端', 3, true, 0, false, '2025-01-08 10:36:40.225', '2025-01-08 10:36:40.225',false, 1,1,1);

--- 忽略报警记录表
drop table if exists public.ignore_alarm;
create table public.ignore_alarm (
	id serial primary key,
	defect_name varchar(128) not null,
	type int not null,
	line_no varchar(20) not null,
	face_no varchar(20) not null,
	ignore_time timestamp not null,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.ignore_alarm is '忽略报警类型';
comment on column public.ignore_alarm.id is '主键';
comment on column public.ignore_alarm.defect_name is '缺陷名称';
comment on column public.ignore_alarm.line_no is '生产线编号';
comment on column public.ignore_alarm.face_no is 'AB面编号';
comment on column public.ignore_alarm.type is '类别';
comment on column public.ignore_alarm.ignore_time is '忽略时长';
comment on column public.ignore_alarm.update_time is '更新时间';
comment on column public.ignore_alarm.create_time is '创建时间';

--- 系统配置表
drop table if exists public.system_config;
create table public.system_config (
	id serial primary key,
	config_name varchar(128) not null,
	config_key varchar(128) not null,
	config_value varchar(128) not null,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp
);

comment on table public.system_config is '系统配置表';
comment on column public.system_config.id is '主键';
comment on column public.system_config.config_key is '配置标识';
comment on column public.system_config.config_name is '配置名称';
comment on column public.system_config.config_value is '配置取值';
comment on column public.system_config.update_time is '更新时间';
comment on column public.system_config.create_time is '创建时间';

INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'设备报警音频', 'device_alarm_sound_uri', '/data/default.mp3', '2025-04-15 15:27:46.417', '2025-04-15 15:27:46.417');
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'缺陷报警音频','defect_alarm_sound_uri', '/data/default.mp3', '2025-04-15 15:27:46.422', '2025-04-15 15:27:46.422');
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'系统报警音频','system_alarm_sound_uri', '/data/default.mp3', '2025-04-15 15:27:46.423', '2025-04-15 15:27:46.423');
