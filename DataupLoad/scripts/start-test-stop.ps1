$ErrorActionPreference = 'Continue'
Set-Location E:\DEMO\数据采集\DataupLoad

# Build classpath
$libDir = '.\lib'
$cpParts = Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $_.FullName }
$cpParts += '.\target\classes'
$cpParts += '.\config'
$cp = [string]::Join(';', $cpParts)

# Clean log dir
if (Test-Path 'log\DataupLoad') {
    Get-ChildItem 'log\DataupLoad\*' -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}

# Start app
$args = @(
    '-cp', $cp,
    '-Dfile.encoding=UTF-8',
    '-Dspring.config.location=./config/',
    '-Dspring.config.name=application',
    '-Dserver.port=80',
    '-Dlogging.level.root=INFO',
    'com.hikrobotics.solution.Application'
)
$logFile = '.\log\DataupLoad\app-startup.log'
$errFile = '.\log\DataupLoad\app-startup.err.log'

$proc = Start-Process -FilePath '.\jdk\bin\hik-java.exe' -ArgumentList $args -RedirectStandardOutput $logFile -RedirectStandardError $errFile -PassThru -NoNewWindow
$myPid = $proc.Id
Write-Host "PID $myPid"

# Poll until my PID owns port 80 or it dies
$boundPid = $null
for ($i=0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 1
    $listen = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.OwningProcess -eq $myPid } | Select-Object -First 1
    if ($listen) {
        $boundPid = $myPid
        Write-Host "[$i] My PID $myPid listening on 80"
        break
    }
    if (-not (Get-Process -Id $myPid -ErrorAction SilentlyContinue)) {
        Write-Host "[$i] My PID $myPid died"
        break
    }
}

if ($boundPid -ne $myPid) {
    Write-Host "Could not bind port 80"
    Get-Content $logFile -Tail 30
    exit 1
}

# Run tests NOW
Write-Host "=== /web/line/list ==="
$list = curl.exe -s -m 10 http://127.0.0.1:80/web/line/list
Write-Host $list
Write-Host ""

Write-Host "=== /web/line/L1 ==="
$l1 = curl.exe -s -m 10 http://127.0.0.1:80/web/line/L1
Write-Host $l1
Write-Host ""

Write-Host "=== /web/line/NONEXIST ==="
$nx = curl.exe -s -m 10 http://127.0.0.1:80/web/line/NONEXIST
Write-Host $nx
Write-Host ""

# DB verify
Write-Host "=== psql ==="
$env:PGPASSWORD = 'postgres'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, name, line_no, face_no, client_no, update_time, create_time FROM line ORDER BY id;" 2>&1

# Save
$list | Out-File '.\log\DataupLoad\curl-list.txt' -Encoding UTF8
$l1 | Out-File '.\log\DataupLoad\curl-L1.txt' -Encoding UTF8
$nx | Out-File '.\log\DataupLoad\curl-nonexist.txt' -Encoding UTF8

# Stop app
Stop-Process -Id $myPid -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Write-Host "Done"
