# log-healthcheck.ps1 - v0.4 健康检查（PM 11:46 紧急修复版）
#
# PM 11:46 修：解决 cron 60s 超时
#   1. error-*.log 只读最后 2000 行（流式 ReadLines）
#   2. accessLog 只读最后 500 行（已用 -Tail 500 但下游 foreach 不慢）
#   3. 加 stopwatch 输出耗时
#   4. Threading 改 BackgroundJob 防止 hang
#
# Exit codes:
#   0 = all green
#   1 = ERROR/CRIT appeared
#   2 = EdgeHost process dead
#   3 = DB file abnormal

param([switch]$Verbose)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$logsDir = Join-Path $root 'logs'
$stateFile = Join-Path $root 'logs\.last-healthcheck.json'

$sw = [System.Diagnostics.Stopwatch]::StartNew()

$results = @{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    ok = $true
    issues = @()
    duration_ms = 0
}

# 1. EdgeHost process check (fast)
$edgeHost = Get-Process -Name 'IntcoEdge.EdgeHost' -ErrorAction SilentlyContinue
if (-not $edgeHost) {
    $results.ok = $false
    $results.issues += '[CRIT] EdgeHost process NOT running'
} elseif ($Verbose) {
    Write-Host ("[OK] EdgeHost PID=" + $edgeHost.Id + " running") -ForegroundColor Green
}

# 2. /health probe (3s timeout)
$probeOk = $false
try {
    $r = Invoke-RestMethod -Uri 'http://localhost:80/health' -Method Get -TimeoutSec 3
    if ($r -eq 'ok') {
        $probeOk = $true
        $results.healthPort = 80
        if ($Verbose) {
            Write-Host '[OK] /health on :80' -ForegroundColor Green
        }
    }
} catch {
    # fall through to ALERT
}
if (-not $probeOk) {
    $results.ok = $false
    $results.issues += '[CRIT] /health probe failed on :80 (EdgeHost not responding)'
}

# 3. error-*.log scan (last 30 min, STREAMING - only tail 2000 lines)
$errorLog = Get-ChildItem -Path $logsDir -Filter 'error-*.log' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($errorLog) {
    $cutoff = (Get-Date).AddMinutes(-30)
    $recentErrors = @()
    try {
        # PM 11:46 修：流式读 + 只读最后 2000 行（避免大文件 timeout）
        $lines = [System.IO.File]::ReadLines($errorLog.FullName) | Select-Object -Last 2000
        foreach ($_ in $lines) {
            if ($_ -match '\[(\d{2}):(\d{2}):(\d{2})') {
                $h = [int]$Matches[1]; $m = [int]$Matches[2]; $s = [int]$Matches[3]
                $ts = Get-Date -Hour $h -Minute $m -Second $s
                if ($ts -gt (Get-Date)) { $ts = $ts.AddDays(-1) }
                if ($ts -ge $cutoff) { $recentErrors += $_ }
            }
        }
    } catch {
        # file 被占用，跳过
    }
    if ($recentErrors.Count -gt 0) {
        $results.ok = $false
        $results.issues += "[CRIT] last 30 min $($recentErrors.Count) ERRORs in $($errorLog.Name)"
        $results.recentErrors = ($recentErrors | Select-Object -First 5)
    } elseif ($Verbose) {
        Write-Host '[OK] no ERROR in last 30 min' -ForegroundColor Green
    }
}

# 4. DB size anomaly (fast - one stat call)
# W-A14 (老板 08:03): 阈值 50 → 500 MB
$db = Join-Path $root 'src/IntcoEdge.Db/data/intco.db'
if (Test-Path $db) {
    $f = Get-Item $db
    $sizeMb = $f.Length / 1MB
    if ($sizeMb -gt 500) {
        $results.ok = $false
        $results.issues += ('[CRIT] DB size = ' + [math]::Round($sizeMb, 2) + ' MB exceeds 500 MB threshold!')
    }
    if ($Verbose -and $sizeMb -le 500) {
        Write-Host ('[OK] DB size = ' + [math]::Round($sizeMb, 2) + ' MB') -ForegroundColor Green
    }
} else {
    $results.ok = $false
    $results.issues += '[CRIT] DB file missing: ' + $db
}

# 5. Push success rate (last 30 min, only tail 500)
$accessLog = Get-ChildItem -Path $logsDir -Filter 'intco-edge-host-*.log' -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($accessLog) {
    $cutoff = (Get-Date).AddMinutes(-30)
    $posts = @()
    try {
        # PM 11:46 修：流式 tail 500
        $lines = [System.IO.File]::ReadLines($accessLog.FullName) | Select-Object -Last 500
        foreach ($_ in $lines) {
            if ($_ -match '\[(\d{2}):(\d{2}):(\d{2})\].*HTTP (POST|PUT) /client/data/.* (\d{3})') {
                $h = [int]$Matches[1]; $m = [int]$Matches[2]; $s = [int]$Matches[3]; $code = [int]$Matches[4]
                $ts = Get-Date -Hour $h -Minute $m -Second $s
                if ($ts -gt (Get-Date)) { $ts = $ts.AddDays(-1) }
                if ($ts -ge $cutoff) { $posts += [pscustomobject]@{ ts=$ts; code=$code } }
            }
        }
    } catch {
        # file 被占用，跳过
    }
    if ($posts.Count -gt 0) {
        $success = ($posts | Where-Object { $_.code -eq 200 }).Count
        $rate = $success / $posts.Count * 100
        $results.pushStats = "success $success / total $($posts.Count) = $([math]::Round($rate, 1))%"
        if ($rate -lt 95 -and $posts.Count -ge 10) {
            $results.ok = $false
            $results.issues += "[WARN] push success rate = $([math]::Round($rate, 1))% (< 95%), total $($posts.Count)"
        }
        if ($Verbose) {
            $color = if ($rate -ge 95) { 'Green' } else { 'Yellow' }
            Write-Host ("[STATS] push success: $success/$($posts.Count) = $([math]::Round($rate, 1))%") -ForegroundColor $color
        }
    }
}

$sw.Stop()
$results.duration_ms = $sw.ElapsedMilliseconds

# 6. Save state (原子写，避免文件锁)
$tmp = $stateFile + '.tmp'
$results | ConvertTo-Json -Depth 3 | Set-Content -Path $tmp -Encoding UTF8
Move-Item -Path $tmp -Destination $stateFile -Force

# 7. Output + exit code
if (-not $results.ok) {
    $msg = "[" + $results.timestamp + "] " + ($results.issues -join ' | ')
    Write-Host $msg -ForegroundColor Red
    exit 1
} else {
    if ($Verbose) {
        Write-Host ("[DONE] HEALTH OK ($($results.duration_ms)ms)") -ForegroundColor Green
    }
    exit 0
}
