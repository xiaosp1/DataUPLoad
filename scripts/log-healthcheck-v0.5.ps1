# log-healthcheck-v0.5.ps1
# PM 13:30 升级：监控 v0.4 EdgeHost + v0.5 DataupLoad
# 2026-07-22 13:38 — v0.4 已让位给 DataupLoad，本脚本监控新项目

[CmdletBinding()]
param()

$root = Split-Path -Parent $PSScriptRoot
$issues = New-Object System.Collections.Generic.List[string]
$ok = $true

# 1. v0.4 EdgeHost 状态（13:15 PM 主动停掉让位给 DataupLoad，预期未运行）
Write-Host "`n=== 1. EdgeHost v0.4 状态 ==="
$edgeHost = Get-Process -Name 'IntcoEdge.EdgeHost' -ErrorAction SilentlyContinue
if ($edgeHost) {
    Write-Host ("[OK] EdgeHost PID=" + $edgeHost.Id + " running") -ForegroundColor Green
} else {
    Write-Host "[INFO] EdgeHost v0.4 未运行（13:15 PM 主动停掉让位给 DataupLoad，符合预期）" -ForegroundColor Yellow
}

# 2. v0.5 DataupLoad 状态（优先监控）
Write-Host "`n=== 2. DataupLoad v0.5 状态 ==="
$dataupLoad = Get-Process -Name 'hik-java' -ErrorAction SilentlyContinue | Where-Object { $_.MainModule.FileName -like '*DataupLoad*' -or $_.Path -like '*DataupLoad*' }
if ($dataupLoad) {
    Write-Host ("[OK] DataupLoad PID=" + $dataupLoad.Id + " running") -ForegroundColor Green
} else {
    Write-Host "[WARN] DataupLoad v0.5 未运行（预期内：18:00 前启动）" -ForegroundColor Yellow
    $issues.Add("[INFO] DataupLoad v0.5 not started yet (expected before 18:00)")
}

# 3. /health on :80
Write-Host "`n=== 3. /health probe ==="
try {
    $resp = Invoke-WebRequest -Uri 'http://127.0.0.1:80/health' -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Write-Host ("[OK] /health on :80 status=" + $resp.StatusCode) -ForegroundColor Green
} catch {
    Write-Host ("[CRIT] /health probe failed: " + $_.Exception.Message) -ForegroundColor Red
    $issues.Add("[CRIT] /health probe failed on :80 (DataupLoad not responding)")
    $ok = $false
}

# 4. PG 14 数据库大小
Write-Host "`n=== 4. PG 14 DB size ==="
$psql = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'
if (Test-Path $psql) {
    $env:PGPASSWORD = 'postgres'
    try {
        $sizeOut = & $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -tAc "SELECT pg_size_pretty(pg_database_size('intco'));" 2>&1
        Write-Host ("[OK] PG intco DB size = " + $sizeOut) -ForegroundColor Green
        if ($Verbose) {
            $rowCount = & $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -tAc "SELECT count(*) FROM defect_day_record;" 2>&1
            Write-Host ("[OK] defect_day_record rows = " + $rowCount)
            $rowCount2 = & $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -tAc "SELECT count(*) FROM alarm_record;" 2>&1
            Write-Host ("[OK] alarm_record rows = " + $rowCount2)
        }
    } catch {
        Write-Host ("[WARN] PG probe failed: " + $_.Exception.Message) -ForegroundColor Yellow
    }
}

# 5. 旧 EdgeHost DB 大小（保留观察）
Write-Host "`n=== 5. 旧 EdgeHost DB（保留观察）==="
$oldDb = Join-Path $root 'src/IntcoEdge.Db/data/intco.db'
if (Test-Path $oldDb) {
    $oldSize = (Get-Item $oldDb).Length / 1MB
    Write-Host ("[INFO] Old EdgeHost DB size = {0:N2} MB" -f $oldSize)
}

# 6. ERROR 日志（最近 30 分钟）
Write-Host "`n=== 6. ERROR 日志（最近 30 分钟）==="
$logRoot = 'E:\logs'
if (Test-Path $logRoot) {
    $errCount = (Get-ChildItem $logRoot -Recurse -Filter '*.log' -ErrorAction SilentlyContinue |
        Select-String -Pattern 'ERROR' -SimpleMatch |
        Where-Object { $_.Line -match '^\d{4}-\d{2}-\d{2}' -and $_.Line -match (Get-Date -Format 'yyyy-MM-dd HH:mm') }).Count
    if ($errCount -gt 0) {
        Write-Host ("[WARN] $errCount ERROR lines in logs") -ForegroundColor Yellow
    } else {
        Write-Host "[OK] no ERROR in logs" -ForegroundColor Green
    }
}

# 总结
Write-Host "`n=== 健康总结 ==="
if ($ok) {
    Write-Host "[DONE] HEALTH OK" -ForegroundColor Green
} else {
    Write-Host "[DONE] HEALTH ISSUES:" -ForegroundColor Red
    foreach ($i in $issues) { Write-Host "  - $i" }
    exit 1
}
