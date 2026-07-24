$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== defect_type rows ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, category, name, alarm_enable, send_yk_enable FROM defect_type ORDER BY id;"
