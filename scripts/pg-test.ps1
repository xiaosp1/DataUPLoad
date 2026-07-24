$env:PGPASSWORD = 'postgres'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT version();"
