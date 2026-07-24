-- 由于新增页面，修改基础框架表中role的permission信息
update public.role set permission = '{user,log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}' where role = 'super_admin';
update public.role set permission = '{log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}' where role = 'admin';
update public.role set permission = '{log,real-time,data-view,glove-defect-records}' where role = 'operator';