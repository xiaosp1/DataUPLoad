# T1: 断连报警测试
# Approach: 直接 POST /client/data/alarm 模拟客户端掉线报警
# 由于 DataupLoad alarm.add() 只对 "interesting defect" 写库，DISCONNECT 类型的
# (type=3, defectName="客户端") 不在 defect_type 表里,无法通过 add() 触发
# 所以下面尝试用 buildClientAlarm 等价的 SQL 插入来模拟:
# - type=3 (DEVICE), reason=1 (DISCONNECT), solve=2 (UNSOLVED)
# 验证 alarm_record 表能正确存储此记录 + 验证 dealClientAlarm 的批量更新 SQL

$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== T1.1 POST /client/data/alarm 验证 端点可用性 ====="
# 测一下 endpoint 是否在线（用熟悉的 TEST001 消息）
$payload = '{"uuid":"t1-disconn-test-001","time":"2026-07-23 14:35:00","type":1,"lineNo":"line3B","faceNo":"B1","level":1,"message":"[TEST001] 缺陷报警"}'
try {
    $resp = Invoke-WebRequest -Method POST -Uri http://127.0.0.1:80/client/data/alarm -ContentType 'application/json' -Body $payload -UseBasicParsing -TimeoutSec 5
    Write-Host "HTTP $($resp.StatusCode): $($resp.Content)"
} catch {
    Write-Host "POST failed: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "===== T1.2 当前 alarm_record reason=1 计数 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) AS disconnect_alarm_before FROM alarm_record WHERE reason=1;"
