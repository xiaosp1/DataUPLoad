# W-FRONT-02-D PM verify script (主布局 + 8 路由 stub + i18n)

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

$root = 'E:\DEMO\数据采集\DataupLoad-web'
$layouts = Join-Path $root 'src\layouts'
$views = Join-Path $root 'src\views'
$mainLayout = Join-Path $layouts 'MainLayout.vue'
$sidebar = Join-Path $layouts 'Sidebar.vue'
$topbar = Join-Path $layouts 'Topbar.vue'
$router = Join-Path $root 'src\router\index.ts'
$i18n = Join-Path $root 'src\i18n\index.ts'
$permission = Join-Path $root 'src\stores\permission.ts'

# 1-6 文件存在
Check 'MainLayout.vue exists' (SafeExists $mainLayout) $mainLayout
Check 'Sidebar.vue exists' (SafeExists $sidebar) $sidebar
Check 'Topbar.vue exists' (SafeExists $topbar) $topbar

# 7 8 个 stub 页面存在
$stubs = @('RealTime.vue', 'Alarm.vue', 'Defect.vue', 'Account.vue', 'SystemConfig.vue', 'Log.vue')
$stubOk = $true
foreach ($s in $stubs) {
    if (-not (SafeExists (Join-Path $views $s))) { $stubOk = $false }
}
Check '6 stub pages exist (RealTime/Alarm/Defect/Account/SystemConfig/Log)' $stubOk 'should have 6+ stub views'

# 8 router 8 路由
$routerContent = ''
if (SafeExists $router) { $routerContent = SafeRead $router }
$routerCount = ([regex]::Matches($routerContent, "path:\s*'")).Count
Check 'router has 8+ routes' ($routerCount -ge 8) "found $routerCount path definitions"

# 9 i18n 三语
$i18nContent = ''
if (SafeExists $i18n) { $i18nContent = SafeRead $i18n }
Check 'i18n has zh-CN' ($i18nContent -match 'zh-CN|zh_CN') 'should have zh-CN'
Check 'i18n has en-US' ($i18nContent -match 'en-US|en_US') 'should have en-US'
Check 'i18n has id-ID' ($i18nContent -match 'id-ID|id_ID') 'should have id-ID'

# 10 permission store
Check 'permission store exists' (SafeExists $permission) $permission
$permContent = ''
if (SafeExists $permission) { $permContent = SafeRead $permission }
Check 'permission has has() function' ($permContent -match 'has\(') 'should have has() action'

# 11 路由守卫 permission 检查
Check 'router beforeEach has permission check' ($routerContent -match 'permission|has\(') 'should check permission'

# 12 Sidebar 用 GlassMenuItem
$sidebarContent = ''
if (SafeExists $sidebar) { $sidebarContent = SafeRead $sidebar }
Check 'Sidebar uses GlassMenuItem' ($sidebarContent -match 'GlassMenuItem') 'should import GlassMenuItem'

# 13 Topbar 三语切换
$topbarContent = ''
if (SafeExists $topbar) { $topbarContent = SafeRead $topbar }
Check 'Topbar has language switcher' ($topbarContent -match 'zh-CN|en-US|locale|i18n') 'should have locale switcher'

# 14 截图
$sampleShot = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-D-sample.png'
Check 'D-sample.png submitted' (SafeExists $sampleShot) $sampleShot

# 15 dev server
$dev = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
Check 'dev server 5173 up' ($null -ne $dev) 'should be on 5173'

Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-D approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
