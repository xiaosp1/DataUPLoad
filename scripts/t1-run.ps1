$env:PGPASSWORD = 'postgres'
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

# 用短路径避免中文路径被 psql 当成 ASCII 字节误解析
$dir = (Get-Item 'E:\DEMO\数据采集\scripts').FullName
$shortDir = (New-Object -ComObject Scripting.FileSystemObject).GetFolder("$dir").ShortPath
Write-Host "shortDir = $shortDir"

Write-Host ""
Write-Host "===== T1.1 INSERT 3 条 DISCONNECT 报警 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -f "$shortDir\t1-step1.sql"

Write-Host ""
Write-Host "===== T1.2 INSERT 后状态 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -f "$shortDir\t1-step2.sql"

Write-Host ""
Write-Host "===== T1.3 dealClientAlarm 批量更新 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -f "$shortDir\t1-step3.sql"

Write-Host ""
Write-Host "===== T1.4 最终断言 ====="
& $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -f "$shortDir\t1-step4.sql"
