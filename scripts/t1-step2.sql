-- T1 Step 2: 验证 INSERT 后 3 条都在 + 全 UNSOLVED
SELECT id, uuid, line_no, face_no, solve, reason, create_time
FROM alarm_record
WHERE uuid LIKE 't1-disconn-%'
ORDER BY id;
