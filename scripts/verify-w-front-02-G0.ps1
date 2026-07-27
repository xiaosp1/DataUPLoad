# W-FRONT-02-G0 PM verify script (PSM 100% 解耦清理)

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

function GrepHits {
    param([string]$Pattern, [string]$Dir)
    if (-not (Test-Path -LiteralPath $Dir)) { return 0 }
    $cnt = 0
    Get-ChildItem -LiteralPath $Dir -Recurse -File -ErrorAction SilentlyContinue |
        ForEach-Object {
            try {
                $c = Get-Content -LiteralPath $_.FullName -Raw -ErrorAction SilentlyContinue
                if ($c -match $Pattern) { $script:cnt++ }
            } catch {}
        }
    return $cnt
}

$webDir = 'E:\DEMO\DATALINK\DataupLoad\web'

# 1 老 SPA 文件应不存在
$legacyFiles = @(
  'index.psm-legacy.html',
  'js/index.f19ecd42-20260520160358.js',
  'js/index-legacy.0208e821-20260520160358.js',
  'js/polyfills-legacy.8d6c83c6-20260520160358.js',
  'js/browser.js',
  'js/AI.png',
  'js/vendor.89afe428-20260520160358.js',
  'js/vendor-legacy.cd362f1d-20260520160358.js',
  'login.html',
  'login.js'
)
$stillExists = 0
foreach ($f in $legacyFiles) {
    if (SafeExists (Join-Path $webDir $f)) { $stillExists++ }
}
Check 'PSM legacy files deleted' ($stillExists -eq 0) "$stillExists still exist"

# 2 web/index.html 是 Vue 3 SPA
$idx = ''
if (SafeExists (Join-Path $webDir 'index.html')) {
    $idx = Get-Content -LiteralPath (Join-Path $webDir 'index.html') -Raw
}
Check 'index.html is Vue 3 SPA' ($idx -match '<div\s+id="app"') 'should have app mount'
Check 'index.html NO gate-routing SHA256' ($idx -notmatch 'crypto\.subtle|syncTokenToLocalStorage') 'should NOT have gate-routing'
Check 'index.html NO bootMainStage' ($idx -notmatch 'bootMainStage') 'should NOT have bootMainStage'
Check 'index.html NO document.cookie token hack' ($idx -notmatch "document\.cookie.*=.*token") 'should NOT have cookie token hack'

# 3 grep 验证 0 命中
$patterns = @(
  @{ P = 'index\.f19ecd42'; N = 'index.f19ecd42' },
  @{ P = 'index-legacy'; N = 'index-legacy' },
  @{ P = 'vendor\.89afe428'; N = 'vendor.89afe428' },
  @{ P = 'polyfills-legacy'; N = 'polyfills-legacy' },
  @{ P = 'bootMainStage'; N = 'bootMainStage' },
  @{ P = 'syncTokenToLocalStorage'; N = 'syncTokenToLocalStorage' }
)
foreach ($pt in $patterns) {
    $hits = GrepHits -Pattern $pt.P -Dir $webDir
    Check ("NO " + $pt.P + " in web/") ($hits -eq 0) "found $hits hits"
}

# 4 curl 验证新前端独立工作
$getRoot = ''
try {
    $r = Invoke-WebRequest -Uri 'http://localhost/' -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    $getRoot = $r.StatusCode
} catch {}
Check 'GET / returns 200' ($getRoot -eq 200) "status=$getRoot"

# 5 ADR-0020
$adr = 'E:\DEMO\数据采集\docs\adr\0020-psm-100-percent-decoupled-20260727.md'
Check 'ADR-0020 exists' (SafeExists $adr) $adr

# 6 截图
$sampleShot = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-G0-sample.png'
Check 'G0-sample.png submitted' (SafeExists $sampleShot) $sampleShot

# 7 report
$report = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-G0-report.md'
Check 'G0-report.md exists' (SafeExists $report) $report

Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-G0 approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
