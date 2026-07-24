$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== alarm_record counts by reason ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT reason, count(*) FROM alarm_record GROUP BY reason ORDER BY reason;"

Write-Host "===== alarm_record counts by solve ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT solve, count(*) FROM alarm_record GROUP BY solve ORDER BY solve;"

Write-Host "===== ignore_alarm count ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) FROM ignore_alarm;"

Write-Host "===== oldest alarm_record ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT min(create_time), max(create_time), count(*) FROM alarm_record;"

Write-Host "===== current server time ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT NOW();"

Write-Host "===== active connections ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT count(*) FROM pg_stat_activity WHERE datname='intco';"
