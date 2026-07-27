# W-FRONT-02-A PM verify script (with Resolve-Path fix)

$ErrorActionPreference = 'Continue'
$pass = 0
$fail = 0

function Check {
    param([string]$Name, [bool]$Cond, [string]$Detail)
    if ($Cond) {
        Write-Host ('[OK]   ' + $Name + ' -- ' + $Detail) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ('[FAIL] ' + $Name + ' -- ' + $Detail) -ForegroundColor Red
        $script:fail++
    }
}

function SafeExists {
    param([string]$Path)
    try {
        $rp = Resolve-Path -LiteralPath $Path -ErrorAction Stop
        return [System.IO.File]::Exists($rp.Path) -or [System.IO.Directory]::Exists($rp.Path)
    } catch {
        return $false
    }
}

function SafeRead {
    param([string]$Path)
    try {
        $rp = Resolve-Path -LiteralPath $Path -ErrorAction Stop
        return Get-Content -LiteralPath $rp.Path -Raw -ErrorAction Stop
    } catch {
        return ''
    }
}

$pkgPath = 'E:\DEMO\数据采集\DataupLoad-web\package.json'
$vcPath = 'E:\DEMO\数据采集\DataupLoad-web\vite.config.js'
$mjPath = 'E:\DEMO\数据采集\DataupLoad-web\src\main.js'
$avPath = 'E:\DEMO\数据采集\DataupLoad-web\src\App.vue'
$nmPath = 'E:\DEMO\数据采集\DataupLoad-web\node_modules'
$devUrl = 'http://localhost:5173'

Check 'package.json exists' (SafeExists $pkgPath) $pkgPath

$hasPkg = SafeExists $pkgPath
if ($hasPkg) {
    $pkg = SafeRead $pkgPath
    Check 'has vite' ($pkg -match '"vite"\s*:\s*"') 'should have vite'
    Check 'has vue@^3' ($pkg -match '"vue"\s*:\s*"\^?3') 'vue 3.x'
    Check 'has element-plus' ($pkg -match '"element-plus"\s*:\s*"') 'element-plus UI lib'
    Check 'has vue-router' ($pkg -match '"vue-router"\s*:\s*"') 'vue-router 4.x'
    Check 'has pinia' ($pkg -match '"pinia"\s*:\s*"') 'pinia state mgmt'
    Check 'has axios' ($pkg -match '"axios"\s*:\s*"') 'axios HTTP client'
} else {
    for ($i = 1; $i -le 6; $i++) { $script:fail++ }
}

Check 'vite.config.js exists' (SafeExists $vcPath) $vcPath
Check 'src/main.js exists' (SafeExists $mjPath) $mjPath
Check 'src/App.vue exists' (SafeExists $avPath) $avPath
Check 'node_modules/ exists (npm install done)' (SafeExists $nmPath) $nmPath

$listen5173 = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
$devUp = $false
if ($listen5173) { $devUp = $true }
Check 'dev server 5173 listening' $devUp 'npm run dev should be on 5173'

if ($devUp) {
    try {
        $r = Invoke-WebRequest $devUrl -UseBasicParsing -TimeoutSec 5
        $code = $r.StatusCode
        $body = $r.Content
        Check 'GET / 200' ($code -eq 200) ('Status=' + $code)
        Check 'HTML has <div id=app>' ($body -match '<div\s+id=["'']app["'']') $body.Substring(0, [Math]::Min(200, $body.Length))
    } catch {
        Check 'GET / 200' $false $_.Exception.Message
        Check 'HTML has <div id=app>' $false 'dev unreachable'
    }
} else {
    Write-Host '[SKIP] dev server not up, skip 13-14' -ForegroundColor Yellow
    $script:fail += 2
}

$mjContent = ''
if (SafeExists $mjPath) { $mjContent = SafeRead $mjPath }
Check 'main.js imports element-plus' ($mjContent -match 'element-plus') $mjContent.Substring(0, [Math]::Min(300, $mjContent.Length))

Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-A approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
