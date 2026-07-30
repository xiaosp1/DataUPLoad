# =============================================================================
# W-FRONT-02-G end-to-end verification script (12 checks)
#
# Backend: http://127.0.0.1:8080  (port 80 is held by Windows iphlpsvc)
# Login:   super_admin / Abc12345  (password must be SHA-256 hex before POST)
# Expected: 12/12 PASS
#
# Usage:
#   pwsh -NoProfile -File scripts/verify-w-front-02-G.ps1 `
#        | Tee-Object docs/work-orders/W-FRONT-02-G-verify-output.txt
#
# IMPORTANT:
# - All strings here are ASCII to avoid PowerShell 5.x ANSI/UTF-8 mojibake.
# - Cookie handling uses -WebSession (CookieContainer) instead of -Headers
#   because some PS versions strip the Cookie header in -Headers.
# - Browser-level checks (9-12) are read from
#   docs/work-orders/W-FRONT-02-G-browser-results.json which is written by
#   DataupLoad-web/test-g-e2e.mjs.
# =============================================================================

$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:8080"
$pass = 0
$fail = 0
$results = New-Object System.Collections.Generic.List[object]
$startedAt = Get-Date

# ASCII-only display names
$checkNames = @{
    1  = "GET / returns Vue 3 SPA entry"
    2  = "GET /assets/index-CndG5nFH.js (Vue 3 bundle)"
    3  = "GET /assets/index-B0hMWKcQ.css (Element Plus + glass CSS)"
    4  = "POST /web/auth/login super_admin/Abc12345 -> 200 + satoken"
    5  = "GET /web/account/current (satoken)"
    6  = "GET /web/account/list (page 1, size 10)"
    7  = "GET /web/alarm/list (page 1, size 10)"
    8  = "POST /web/detect/day-record/list-between (real path)"
    9  = "Browser: login page -> main (Vue 3 glass)"
    10 = "Browser: 8 business routes accessible"
    11 = "Browser: three-language switch (zh-CN / en-US / id-ID)"
    12 = "Browser: WS alarm push indicator"
}

# Build report paths using ASCII-only components; we'll substitute the
# project root explicitly to avoid Chinese chars in the script.
$projectRoot = $PSScriptRoot | Split-Path -Parent
$reportDir = Join-Path $projectRoot "docs\work-orders"
$browserLogPath = Join-Path $reportDir "W-FRONT-02-G-browser-results.json"
$summaryPath = Join-Path $reportDir "W-FRONT-02-G-verify-summary.json"
$outputPath = Join-Path $reportDir "W-FRONT-02-G-verify-output.txt"

function Check {
    param([int]$n, [scriptblock]$script)
    $name = $checkNames[$n]
    if (-not $name) { $name = "Check $n" }
    try {
        $ok = & $script
        if ($ok) {
            Write-Host ("[{0}] [PASS] {1}" -f $n, $name)
            $script:pass++
            $script:results.Add([PSCustomObject]@{ N = $n; Name = $name; OK = $true }) | Out-Null
        } else {
            Write-Host ("[{0}] [FAIL] {1}" -f $n, $name)
            $script:fail++
            $script:results.Add([PSCustomObject]@{ N = $n; Name = $name; OK = $false }) | Out-Null
        }
    } catch {
        Write-Host ("[{0}] [FAIL] {1} ({2})" -f $n, $name, $_)
        $script:fail++
        $script:results.Add([PSCustomObject]@{ N = $n; Name = $name; OK = $false }) | Out-Null
    }
}

Write-Host "=== W-FRONT-02-G end-to-end verification ==="
Write-Host ("Backend: {0}" -f $base)
Write-Host ("Started: {0}" -f $startedAt)
Write-Host ""

# Use a single WebSession for items 4-8 (cookie container)
$cookieJar = New-Object System.Net.CookieContainer
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$session.Cookies = $cookieJar

# -- 1: GET / returns Vue 3 SPA entry (contains #app) ----------------------
Check 1 {
    $r = Invoke-WebRequest -Uri "$base/" -UseBasicParsing -TimeoutSec 10
    ($r.StatusCode -eq 200) -and ($r.Content -match 'id="app"') -and ($r.Content -match 'index-CndG5nFH\.js')
}

# -- 2: GET /assets/index-CndG5nFH.js 200 -----------------------------------
Check 2 {
    $r = Invoke-WebRequest -Uri "$base/assets/index-CndG5nFH.js" -UseBasicParsing -TimeoutSec 30
    ($r.StatusCode -eq 200) -and ($r.RawContentLength -gt 1000000)
}

# -- 3: GET /assets/index-B0hMWKcQ.css 200 ----------------------------------
Check 3 {
    $r = Invoke-WebRequest -Uri "$base/assets/index-B0hMWKcQ.css" -UseBasicParsing -TimeoutSec 15
    ($r.StatusCode -eq 200) -and ($r.RawContentLength -gt 100000)
}

# -- 4: POST /web/auth/login super_admin / SHA256(Abc12345) -----------------
$pwd = "Abc12345"
$sha = [System.Security.Cryptography.SHA256]::Create().ComputeHash(
    [System.Text.Encoding]::UTF8.GetBytes($pwd)
)
$shaHex = -join ($sha | ForEach-Object { $_.ToString("x2") })
$loginBody = '{"username":"super_admin","password":"' + $shaHex + '"}'

$login = $null
$loginInfo = ""
try {
    $login = Invoke-WebRequest -Uri "$base/web/auth/login" -Method POST `
        -ContentType "application/json" -Body $loginBody `
        -UseBasicParsing -TimeoutSec 15 -WebSession $session
    $loginInfo = "status=$($login.StatusCode)"
} catch {
    $loginInfo = "$_"
}

Check 4 {
    if (-not $login) { return $false }
    if ($login.StatusCode -ne 200) { return $false }
    # Use WebSession's cookie container to verify satoken was stored
    $satoken = $cookieJar.GetCookies(([System.Uri]"${base}/")) | Where-Object { $_.Name -eq "satoken" } | Select-Object -First 1
    if (-not $satoken) { return $false }
    return ($login.Content -match '"success"\s*:\s*true') -or ($login.Content -match '"code"\s*:\s*0')
}

# -- 5: GET /web/account/current with WebSession ----------------------------
Check 5 {
    try {
        $r = Invoke-WebRequest -Uri "$base/web/account/current" `
            -WebSession $session -UseBasicParsing -TimeoutSec 10
        return ($r.StatusCode -eq 200) -and ($r.Content -match 'super_admin')
    } catch {
        return $false
    }
}

# -- 6: GET /web/account/list?pageNum=1&pageSize=10 -------------------------
Check 6 {
    try {
        $r = Invoke-WebRequest -Uri "$base/web/account/list?pageNum=1&pageSize=10" `
            -WebSession $session -UseBasicParsing -TimeoutSec 10
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

# -- 7: GET /web/alarm/list?pageNum=1&pageSize=10 ---------------------------
Check 7 {
    try {
        $r = Invoke-WebRequest -Uri "$base/web/alarm/list?pageNum=1&pageSize=10" `
            -WebSession $session -UseBasicParsing -TimeoutSec 10
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

# -- 8: POST /web/detect/day-record/list-between ----------------------------
Check 8 {
    try {
        $r = Invoke-WebRequest `
            -Uri "$base/web/detect/day-record/list-between?startTime=2026-07-23&endTime=2026-07-30" `
            -Method POST `
            -WebSession $session `
            -ContentType "application/json" -Body "{}" `
            -UseBasicParsing -TimeoutSec 15
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

# -- 9-12: browser-level checks ---------------------------------------------
$browserResults = $null
if (Test-Path $browserLogPath) {
    try {
        $browserResults = Get-Content -Raw -Path $browserLogPath -Encoding UTF8 | ConvertFrom-Json
        foreach ($n in 9..12) {
            $name = $checkNames[$n]
            $ok = $false
            $info = ""
            if ($browserResults.PSObject.Properties.Name -contains "checks") {
                $entry = $browserResults.checks | Where-Object { $_.n -eq $n } | Select-Object -First 1
                if ($entry) { $ok = $entry.ok; $info = $entry.info }
            }
            if ($ok) {
                Write-Host ("[{0}] [PASS] {1} -- {2}" -f $n, $name, $info)
                $script:pass++
                $script:results.Add([PSCustomObject]@{ N = $n; Name = $name; OK = $true }) | Out-Null
            } else {
                Write-Host ("[{0}] [FAIL] {1} -- {2}" -f $n, $name, $info)
                $script:fail++
                $script:results.Add([PSCustomObject]@{ N = $n; Name = $name; OK = $false }) | Out-Null
            }
        }
    } catch {
        Write-Host ("[9-12] [WARN] browser-results.json unreadable: {0}" -f $_)
        foreach ($n in 9..12) {
            $script:fail++
            $script:results.Add([PSCustomObject]@{ N = $n; Name = $checkNames[$n]; OK = $false }) | Out-Null
        }
    }
} else {
    Write-Host ("[9-12] [WARN] {0} not found -- run DataupLoad-web/test-g-e2e.mjs first" -f $browserLogPath)
    foreach ($n in 9..12) {
        $script:fail++
        $script:results.Add([PSCustomObject]@{ N = $n; Name = $checkNames[$n]; OK = $false }) | Out-Null
    }
}

$endedAt = Get-Date
$duration = $endedAt - $startedAt

Write-Host ""
Write-Host "=== SUMMARY ==="
Write-Host ("PASS: {0} / FAIL: {1} / TOTAL: {2}" -f $pass, $fail, ($pass + $fail))
Write-Host ("Duration: {0}" -f $duration)

# Persist JSON summary alongside the tee'd text output
$summary = [PSCustomObject]@{
    pass = $pass
    fail = $fail
    total = $pass + $fail
    startedAt = $startedAt.ToString("o")
    endedAt = $endedAt.ToString("o")
    durationSec = [int]$duration.TotalSeconds
    backend = $base
    loginInfo = $loginInfo
    results = $results
}
try {
    $summary | ConvertTo-Json -Depth 5 | Set-Content -Path $summaryPath -Encoding UTF8
    Write-Host ("Summary JSON: {0}" -f $summaryPath)
} catch {
    Write-Host ("Summary JSON write failed: {0}" -f $_)
}

if ($fail -gt 0) {
    exit 1
}
exit 0
