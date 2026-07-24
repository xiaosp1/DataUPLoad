--- 检测数据表
drop table if exists public.defect_record_backup;
create table public.defect_record_backup (
	id serial primary key,
    line_no varchar(20) not null,
	face_no varchar(20) not null,
	glove_no varchar(20) not null,
	result int not null,
	defect_type varchar(20) not null,
	img_list text not null,
	time timestamp not null ,
	update_time timestamp not null default current_timestamp,
  	create_time timestamp not null default current_timestamp,
  	except_flag  int not null  default 1
);

comment on table public.defect_record_backup is '检测数据表';
comment on column public.defect_record_backup.id is '主键id';
comment on column public.defect_record_backup.line_no is '线体编号';
comment on column public.defect_record_backup.face_no is 'AB面编号';
comment on column public.defect_record_backup.glove_no is '手套编号';
comment on column public.defect_record_backup.result is '检测结果 1-良品 2-次品';
comment on column public.defect_record_backup.defect_type is '缺陷类型';
comment on column public.defect_record_backup.time is '检测时间';
comment on column public.defect_record_backup.img_list is '检测图片信息';
comment on column public.defect_record_backup.update_time is '更新时间';
comment on column public.defect_record_backup.create_time is '创建时间';

create index index_join_backup on public.defect_record_backup(time,line_no,defect_type,result,face_no);
create trigger t_defect_record_backup before update on public.defect_record_backup for each row execute procedure upd_timestamp();