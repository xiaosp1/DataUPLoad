ALTER TABLE line_day_record add COLUMN remove_total int default 0 not null;

truncate table system_config;
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'设备报警音频', 'device_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46.417', '2025-04-15 15:27:46.417');
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'缺陷报警音频','defect_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46.422', '2025-04-15 15:27:46.422');
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'系统报警音频','system_alarm_sound_uri', '/data/sound/default.mp3', '2025-04-15 15:27:46.423', '2025-04-15 15:27:46.423');
INSERT INTO public.system_config(id, config_name,config_key, config_value, update_time, create_time)VALUES(default,'重复播报次数','sound_play_count', '1', '2025-04-15 15:27:46.423', '2025-04-15 15:27:46.423');

CREATE UNIQUE INDEX ignore_alarm_type_idx ON public.ignore_alarm ("type",line_no,face_no,defect_name);
