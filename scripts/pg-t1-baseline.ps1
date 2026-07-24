$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== T1.0 触发前基线 alarm_record reason=1 计数 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) AS disconnect_alarm_before FROM alarm_record WHERE reason=1;"
