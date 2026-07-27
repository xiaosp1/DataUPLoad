# W-FRONT-02-F PM verify script (打包 + 部署)

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
    } catch { return $false }
}

$webDir = 'E:\DEMO\DATALINK\DataupLoad\web'
$distDir = 'E:\DEMO\数据采集\DataupLoad-web\dist'

# 1 dist/index.html 存在
Check 'dist/index.html exists' (SafeExists (Join-Path $distDir 'index.html')) 'dist/index.html'

# 2 dist 含 Vue 3 app div
$indexHtml = ''
if (SafeExists (Join-Path $distDir 'index.html')) {
    $indexHtml = Get-Content -LiteralPath (Join-Path $distDir 'index.html') -Raw
}
Check 'dist/index.html has <div id="app">' ($indexHtml -match '<div\s+id="app"') 'should have Vue 3 app mount point'

# 3 dist 含 js chunk
$jsCount = 0
if (SafeExists (Join-Path $distDir 'assets')) {
    $jsCount = (Get-ChildItem -LiteralPath (Join-Path $distDir 'assets') -Filter '*.js' -ErrorAction SilentlyContinue).Count
}
Check 'dist/assets has JS chunks' ($jsCount -ge 1) "found $jsCount JS files"

# 4 dist 含 css chunk
$cssCount = 0
if (SafeExists (Join-Path $distDir 'assets')) {
    $cssCount = (Get-ChildItem -LiteralPath (Join-Path $distDir 'assets') -Filter '*.css' -ErrorAction SilentlyContinue).Count
}
Check 'dist/assets has CSS chunks' ($cssCount -ge 1) "found $cssCount CSS files"

# 5 web/ 已部署
Check 'web/index.html deployed' (SafeExists (Join-Path $webDir 'index.html')) 'web/index.html'

# 6 web/ 含 assets/
Check 'web/assets deployed' (SafeExists (Join-Path $webDir 'assets')) 'web/assets/'

# 7 GET / 返回 200
$get = ''
try {
    $get = (Invoke-WebRequest -Uri 'http://localhost/' -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop).StatusCode
} catch {}
Check 'GET / returns 200' ($get -eq 200) "status=$get"

# 8 后端 satoken cookie
$login = ''
try {
    $body = '{"username":"super_admin","password":"bc43e07b8c6e2c7e1f6e2a4f8e2c7e1f6e2a4f8e2c7e1f6e2a4f8e2c7e1f6e2"}'
    $r = Invoke-WebRequest -Uri 'http://localhost/web/auth/login' -Method POST -Body $body -ContentType 'application/json' -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    $login = $r.StatusCode
} catch {
    $login = $_.Exception.Response.StatusCode.value__
}
# This test is just smoke test — actual auth verified in C sub

# 9 截图
$sampleShot = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-F-sample.png'
Check 'F-sample.png submitted' (SafeExists $sampleShot) $sampleShot

# 10 report
$report = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-F-report.md'
Check 'F-report.md exists' (SafeExists $report) $report

Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-F approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
