-- V1.1__role_permission.sql
-- PSM Flyway V1.1: 更新 role.permission 字段。
-- 原始 SQL（PG）：update public.role set permission = '{...}' where role = 'super_admin';
-- SQLite 不支持 {...} 数组字面量；permission 字段实际是文本字符串 '{user,log,...}'，
-- 我们按原始字面量写入（保留花括号、逗号分隔的字符串格式）。
-- 这是预置数据；如 role 表为空 INSERT 会失败 — 但我们用 INSERT OR IGNORE 形式，
-- 确保幂等且不影响首次迁移历史。

INSERT OR IGNORE INTO role (id, "role", permission, create_time, update_time)
VALUES
    (1, 'super_admin', '{user,log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'admin',       '{log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'operator',    '{log,real-time,data-view,glove-defect-records}',                                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 即便 role 表已有行（被 V1.0 之前的预 Flyway 脚本写入），也用 UPDATE 同步成最新 permission。
UPDATE role SET permission = '{user,log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}',
                update_time = CURRENT_TIMESTAMP
 WHERE "role" = 'super_admin';

UPDATE role SET permission = '{log,real-time,data-view,glove-defect-records,client,solution,alarm,system-config}',
                update_time = CURRENT_TIMESTAMP
 WHERE "role" = 'admin';

UPDATE role SET permission = '{log,real-time,data-view,glove-defect-records}',
                update_time = CURRENT_TIMESTAMP
 WHERE "role" = 'operator';
