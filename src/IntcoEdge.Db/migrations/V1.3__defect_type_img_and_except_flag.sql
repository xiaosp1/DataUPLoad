-- V1.3__defect_type_img_and_except_flag.sql
-- PSM Flyway V1.3:
--   alter table defect_type add column show_img_enable bool not null default false;
--   alter table defect_record add column except_flag int not null default 1;

ALTER TABLE defect_type  ADD COLUMN show_img_enable INTEGER NOT NULL DEFAULT 0;  -- 是否显示图片
ALTER TABLE defect_record ADD COLUMN except_flag    INTEGER NOT NULL DEFAULT 1;  -- 是否被剔除
