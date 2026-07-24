$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

Write-Host "===== alarm_record all rows ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, uuid, line_no, face_no, defect_name, reason, solve, create_time FROM alarm_record ORDER BY id;"

Write-Host "===== alarm_record distinct line_no/face_no ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT DISTINCT line_no, face_no FROM alarm_record;"
