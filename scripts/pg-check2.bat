@echo off
set PGPASSWORD=***
"C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, defect_name, type, line_no, face_no, ignore_all, end_time FROM ignore_alarm ORDER BY id"
"C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, type, line_no, face_no, level, solve, defect_name, time FROM alarm_record ORDER BY id LIMIT 10"
