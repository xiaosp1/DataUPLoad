# W-FRONT-02-C PM verify script (去 PSM 守卫 hack)

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

function SafeRead {
    param([string]$Path)
    try {
        $rp = Resolve-Path -LiteralPath $Path -ErrorAction Stop
        return Get-Content -LiteralPath $rp.Path -Raw -ErrorAction Stop
    } catch { return '' }
}

function GrepRecursive {
    param([string]$Pattern, [string]$Dir)
    if (-not (Test-Path -LiteralPath $Dir)) { return @() }
    Get-ChildItem -LiteralPath $Dir -Recurse -File -Include '*.ts','*.vue','*.js' -ErrorAction SilentlyContinue |
        ForEach-Object {
            $c = SafeRead $_.FullName
            if ($c -match $Pattern) { @{ Path = $_.FullName; Content = $c } }
        }
}

$root = 'E:\DEMO\数据采集\DataupLoad-web'
$apiDir = Join-Path $root 'src\api'
$routerFile = Join-Path $root 'src\router\index.ts'
$loginVue = Join-Path $root 'src\views\Login.vue'
$userStore = Join-Path $root 'src\stores\user.ts'
$interceptor = Join-Path $apiDir 'interceptor.ts'
$authApi = Join-Path $apiDir 'auth.ts'
$main = Join-Path $root 'src\main.js'

# 1-6 文件存在
Check 'src/api/auth.ts exists' (SafeExists $authApi) $authApi
Check 'src/api/interceptor.ts exists' (SafeExists $interceptor) $interceptor
Check 'src/router/index.ts exists' (SafeExists $routerFile) $routerFile
Check 'src/views/Login.vue exists' (SafeExists $loginVue) $loginVue
Check 'src/stores/user.ts exists' (SafeExists $userStore) $userStore

# 7-10 关键去 hack grep 验证（0 命中）
$tokenHackHits = GrepRecursive -Pattern 'document\.cookie\s*=\s*[''"]token' -Dir (Join-Path $root 'src')
Check 'NO document.cookie="token hack' ($tokenHackHits.Count -eq 0) 'should be 0 hits'

$syncHits = GrepRecursive -Pattern 'syncTokenToLocalStorage' -Dir (Join-Path $root 'src')
Check 'NO syncTokenToLocalStorage function' ($syncHits.Count -eq 0) 'should be 0 hits'

$getTokenHits = GrepRecursive -Pattern "getCookie\('token'\)" -Dir (Join-Path $root 'src')
Check 'NO getCookie(token) — must use satoken' ($getTokenHits.Count -eq 0) 'should be 0 hits'

# 11 router beforeEach 读 satoken
$routerContent = ''
if (SafeExists $routerFile) { $routerContent = SafeRead $routerFile }
Check 'router beforeEach reads satoken' ($routerContent -match "getCookie\('satoken'\)") 'should use getCookie(''satoken'')'

# 12 withCredentials
$authContent = ''
if (SafeExists $authApi) { $authContent = SafeRead $authApi }
Check 'auth.ts has withCredentials' ($authContent -match 'withCredentials') 'should set withCredentials: true'

# 13 Login.vue uses GlassCard
$loginContent = ''
if (SafeExists $loginVue) { $loginContent = SafeRead $loginVue }
Check 'Login.vue uses GlassCard' ($loginContent -match 'GlassCard') 'should import GlassCard'

# 14 interceptor.ts registered in main.js
$mainContent = ''
if (SafeExists $main) { $mainContent = SafeRead $main }
Check 'main.js imports interceptor' ($mainContent -match 'interceptor') 'should import interceptor'

# 15 截图提交
$sampleShot = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-C-sample.png'
Check 'C-sample.png submitted' (SafeExists $sampleShot) $sampleShot

# 16 dev server
$dev = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
Check 'dev server 5173 up' ($null -ne $dev) 'npm run dev should be on 5173'

Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-C approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
