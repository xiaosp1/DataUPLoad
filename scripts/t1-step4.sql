-- T1 Step 4: 断言：仅最新 1 条保持 UNSOLVED(solve=2)，其余 SOLVED(solve=1)
SELECT id, uuid, solve, reason, create_time
FROM alarm_record
WHERE uuid LIKE 't1-disconn-%'
ORDER BY id;
