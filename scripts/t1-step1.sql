-- T1 Step 1: 清理 + 插入 3 条 DISCONNECT 报警（line1B/B1）
DELETE FROM alarm_record WHERE uuid LIKE 't1-disconn-%';

INSERT INTO alarm_record (uuid, time, type, line_no, face_no, level, message, solve, reason, defect_name, create_time, update_time)
VALUES
 ('t1-disconn-001', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [1]', 2, 1, '客户端', NOW() - INTERVAL '60 seconds', NOW() - INTERVAL '60 seconds'),
 ('t1-disconn-002', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [2]', 2, 1, '客户端', NOW() - INTERVAL '30 seconds', NOW() - INTERVAL '30 seconds'),
 ('t1-disconn-003', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [3]', 2, 1, '客户端', NOW(),                              NOW());
