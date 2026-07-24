$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== alarm_record schema ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "\d alarm_record"

Write-Host "===== ignore_alarm schema ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "\d ignore_alarm"

Write-Host "===== status_record schema ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "\d status_record"
