$ErrorActionPreference = 'Continue'
Set-Location E:\DEMO\数据采集\DataupLoad

# Kill any existing hik-java
Get-Process | Where-Object { $_.ProcessName -like "*hik-java*" } | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Build classpath
$libDir = '.\lib'
$cpParts = Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $_.FullName }
$cpParts += '.\target\classes'
$cpParts += '.\config'
$cp = [string]::Join(';', $cpParts)

# Clean log dir
if (Test-Path 'log\DataupLoad') {
    Get-ChildItem 'log\DataupLoad\*' -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path 'log\DataupLoad' -Force | Out-Null
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
$proc = Start-Process -FilePath '.\jdk\bin\hik-java.exe' -ArgumentList $args -RedirectStandardOutput '.\log\DataupLoad\app-startup.log' -RedirectStandardError '.\log\DataupLoad\app-startup.err.log' -PassThru -NoNewWindow
$myPid = $proc.Id
Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Started PID $myPid"

$list = $null; $l1 = $null; $nx = $null
for ($i=0; $i -lt 70; $i++) {
    Start-Sleep -Seconds 2
    $listen = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.OwningProcess -eq $myPid } | Select-Object -First 1
    if ($listen) {
        Write-Host "[$i] Bound to 80"
        $list = curl.exe -s -m 10 http://127.0.0.1:80/web/line/list
        $l1 = curl.exe -s -m 10 http://127.0.0.1:80/web/line/L1
        $nx = curl.exe -s -m 10 http://127.0.0.1:80/web/line/NONEXIST
        Write-Host "list: $list"
        Write-Host "L1: $l1"
        Write-Host "NX: $nx"
        break
    }
    if (-not (Get-Process -Id $myPid -ErrorAction SilentlyContinue)) {
        Write-Host "[$i] My PID died"
        break
    }
}

# Save curl results
if ($list) { $list | Out-File '.\log\DataupLoad\curl-list.txt' -Encoding UTF8 }
if ($l1) { $l1 | Out-File '.\log\DataupLoad\curl-L1.txt' -Encoding UTF8 }
if ($nx) { $nx | Out-File '.\log\DataupLoad\curl-nonexist.txt' -Encoding UTF8 }

# DB verify (if app was up at any point)
Write-Host ""
Write-Host "=== psql SELECT FROM line ==="
$env:PGPASSWORD = 'postgres'
& 'C:\Program Files\PostgreSQL\14\bin\psql.exe' -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, name, line_no, face_no, client_no, update_time, create_time FROM line ORDER BY id;" 2>&1

# Don't kill — leave for next agent / inspection
Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Done"
