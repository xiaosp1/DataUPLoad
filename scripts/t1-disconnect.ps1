# T1: 断连报警 DealAlarmEvent 测试
# DataupLoad 现状（实测）：
#   1. 无 DealAlarmEvent 类、无 @EventListener 接收方
#   2. StatusRecordServiceImpl.receiveStatus() 不发布事件
#   3. AlarmRecord.buildClientAlarm() 构建器存在但无 caller
#   4. AlarmRecordServiceImpl.dealClientAlarm() 方法存在但永远不被调用
#   5. add() 只对 "interesting defect" 写库（defect_type 表里只有 TEST001），无法通过
#      POST /client/data/alarm 写入 reason=1（DISCONNECT）记录
#
# 因此本工单在 DataupLoad 上无法触发 DealAlarmEvent 事件链。
# 退而求其次：用 psql 直接 INSERT reason=1 记录 + 模拟 dealClientAlarm 的批量更新 SQL，
# 验证 schema 与清理逻辑工作正常。
$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== T1.1 模拟 3 次断连报警（直接 INSERT reason=1） ====="
# 清理前
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "DELETE FROM alarm_record WHERE uuid LIKE 't1-disconn-%';" 2>&1 | Out-Null

# 插入 3 条 DISCONNECT 报警（line1B/B1），模拟同一产线 3 次重连事件
$now = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c @"
INSERT INTO alarm_record (uuid, time, type, line_no, face_no, level, message, solve, reason, defect_name, create_time, update_time)
VALUES
 ('t1-disconn-001', '$now', 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [1]', 2, 1, '客户端', NOW() - INTERVAL '60 seconds', NOW() - INTERVAL '60 seconds'),
 ('t1-disconn-002', '$now', 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [2]', 2, 1, '客户端', NOW() - INTERVAL '30 seconds', NOW() - INTERVAL '30 seconds'),
 ('t1-disconn-003', '$now', 3, 'line1B', 'B1', 3, 'line1B-B1 客户端掉线 [3]', 2, 1, '客户端', NOW(),                              NOW());
"@

Write-Host ""
Write-Host "===== T1.2 INSERT 后 reason=1 行（应该 3 条，全 UNSOLVED） ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, uuid, line_no, face_no, solve, reason, create_time FROM alarm_record WHERE uuid LIKE 't1-disconn-%' ORDER BY id;"

Write-Host ""
Write-Host "===== T1.3 模拟 dealClientAlarm 的批量更新：除最新 1 条外全部置 SOLVED ====="
# dealClientAlarm SQL 等价（保持与 Java 代码同语义：按 id DESC 取第 1 条保留 UNSOLVED，其余置 SOLVED）
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c @"
WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
  FROM alarm_record
  WHERE line_no = 'line1B' AND face_no = 'B1' AND type = 3 AND solve = 2
)
UPDATE alarm_record SET solve = 1
WHERE id IN (SELECT id FROM ranked WHERE rn > 1)
RETURNING id, uuid, solve;
"@

Write-Host ""
Write-Host "===== T1.4 验证：仅最新 1 条保持 UNSOLVED(solve=2)，其余 SOLVED(solve=1) ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, uuid, solve, reason, create_time FROM alarm_record WHERE uuid LIKE 't1-disconn-%' ORDER BY id;"
