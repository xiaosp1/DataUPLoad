# W-FRONT-02-B PM verify script

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

$root = 'E:\DEMO\数据采集\DataupLoad-web'
$stylesDir = Join-Path $root 'src\styles'
$compDir = Join-Path $root 'src\components'
$tokens = Join-Path $stylesDir 'tokens.scss'
$elOverride = Join-Path $stylesDir 'element-overrides.scss'
$global = Join-Path $stylesDir 'global.scss'
$main = Join-Path $root 'src\main.js'

# 1-3 styles
Check 'tokens.scss exists' (SafeExists $tokens) $tokens
Check 'element-overrides.scss exists' (SafeExists $elOverride) $elOverride
Check 'global.scss exists' (SafeExists $global) $global

# 4-7 components
$glassCard = Join-Path $compDir 'GlassCard.vue'
$glassBtn = Join-Path $compDir 'GlassButton.vue'
$glassMenu = Join-Path $compDir 'GlassMenuItem.vue'
$glassTable = Join-Path $compDir 'GlassTable.vue'
$glassPage = Join-Path $compDir 'GlassPage.vue'
$compIdx = Join-Path $compDir 'index.ts'

Check 'GlassCard.vue exists' (SafeExists $glassCard) $glassCard
Check 'GlassButton.vue exists' (SafeExists $glassBtn) $glassBtn
Check 'GlassMenuItem.vue exists' (SafeExists $glassMenu) $glassMenu
Check 'GlassTable.vue exists' (SafeExists $glassTable) $glassTable
Check 'GlassPage.vue exists' (SafeExists $glassPage) $glassPage
Check 'components/index.ts exists' (SafeExists $compIdx) $compIdx

# 8 tokens content check
$tokensContent = ''
if (SafeExists $tokens) { $tokensContent = SafeRead $tokens }
Check 'tokens has --accent color' ($tokensContent -match '\-\-accent\s*:\s*#5ce1ff') 'should have --accent: #5ce1ff'
Check 'tokens has --glass-bg' ($tokensContent -match '\-\-glass-bg\s*:') 'should have --glass-bg'

# 9 element-overrides content
$elContent = ''
if (SafeExists $elOverride) { $elContent = SafeRead $elOverride }
Check 'element-overrides has --el-color-primary' ($elContent -match '\-\-el-color-primary') 'should override Element Plus primary'

# 10 global.scss imported in main.js
$mainContent = ''
if (SafeExists $main) { $mainContent = SafeRead $main }
Check 'main.js imports global.scss' ($mainContent -match "import.*global\.scss") 'main.js should import global styles'

# 11 components registered in main.js
Check 'main.js registers Glass components' ($mainContent -match 'GlassCard|GlassButton') 'main.js should register glass components'

# 12 dev server
$listen5173 = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
$devUp = $false
if ($listen5173) { $devUp = $true }
Check 'dev server 5173 listening' $devUp 'npm run dev should be on 5173'

# 13 sample screenshot
$sampleShot = 'E:\DEMO\数据采集\docs\work-orders\W-FRONT-02-B-sample.png'
$sampleExists = SafeExists $sampleShot
Check 'B-sample.png screenshot submitted' $sampleExists $sampleShot

# Summary
Write-Host ''
Write-Host '=================================' -ForegroundColor Cyan
Write-Host ('PASS: ' + $pass + '  FAIL: ' + $fail)
Write-Host '=================================' -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host 'ALL CHECKS PASSED -- W-FRONT-02-B approved' -ForegroundColor Green
    exit 0
} else {
    Write-Host ('FAIL ' + $fail + ' items, reject worker') -ForegroundColor Red
    exit 1
}
