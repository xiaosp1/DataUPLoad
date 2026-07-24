alter table defect_type add column show_img_enable  bool not null  default false;
comment on column defect_type.show_img_enable is '是否显示图片';

alter table defect_record add column except_flag  int not null  default 1;
comment on column defect_record.except_flag is '是否被剔除';
