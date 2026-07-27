# W-FRONT-02-A PM verify script
# 用途：PM 在收到 worker report.md 后执行此脚本验收
# 预期：15 项 check 全部 OK

$ErrorActionPreference = "Continue"
$pass = 0
$fail = 0
$root = "E:\DEMO\数据采集\DataupLoad-web"
$devUrl = "http://localhost:5173"

function Check {
    param([string]$Name, [bool]$Cond, [string]$Detail)
    if ($Cond) {
        Write-Host ("[OK] " + $Name + " — " + $Detail) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("[FAIL] " + $Name + " — " + $Detail) -ForegroundColor Red
        $script:fail++
    }
}

# ---- 1-7: package.json ----
$pkgPath = Join-Path $root "package.json"
$hasPkg = Test-Path $pkgPath
Check "package.json 存在" $hasPkg $pkgPath

if ($hasPkg) {
    $pkg = Get-Content $pkgPath -Raw
    Check "含 vite" ($pkg -match '"vite"\s*:\s*"') "node_modules 应该有 vite"
    Check "含 vue@^3" ($pkg -match '"vue"\s*:\s*"\^?3') "vue 必须是 3.x"
    Check "含 element-plus" ($pkg -match '"element-plus"\s*:\s*"') "element-plus 是 UI 库"
    Check "含 vue-router" ($pkg -match '"vue-router"\s*:\s*"') "vue-router 4.x"
    Check "含 pinia" ($pkg -match '"pinia"\s*:\s*"') "pinia 状态管理"
    Check "含 axios" ($pkg -match '"axios"\s*:\s*"') "axios HTTP 客户端"
} else {
    for ($i = 1; $i -le 6; $i++) { $script:fail++ }
}

# ---- 8-10: 关键文件 ----
$vc = Join-Path $root "vite.config.js"
$mj = Join-Path $root "src\main.js"
$av = Join-Path $root "src\App.vue"
$ri = Join-Path $root "src\router\index.js"
Check "vite.config.js 存在" (Test-Path $vc) $vc
Check "src/main.js 存在" (Test-Path $mj) $mj
Check "src/App.vue 存在" (Test-Path $av) $av

# ---- 11: node_modules 已 install ----
$nm = Join-Path $root "node_modules"
Check "node_modules/ 存在（npm install 完成）" (Test-Path $nm) $nm

# ---- 12: dev server 在 5173 监听 ----
$listen5173 = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
$devUp = $false
if ($listen5173) { $devUp = $true }
Check "dev server 5173 在跑" $devUp "npm run dev 应启动到 5173"

# ---- 13-14: GET / 返回 Vue 3 HTML ----
if ($devUp) {
    try {
        $r = Invoke-WebRequest $devUrl -UseBasicParsing -TimeoutSec 5
        $code = $r.StatusCode
        $body = $r.Content
        Check "GET / 200" ($code -eq 200) "Status=$code"
        Check "HTML 含 <div id=app>" ($body -match '<div\s+id=["'']app["'']') $body.Substring(0, [Math]::Min(200, $body.Length))
    } catch {
        Check "GET / 200" $false $_.Exception.Message
        Check "HTML 含 <div id=app>" $false "dev 不可达"
    }
} else {
    Write-Host "[SKIP] dev server 没起，跳过 13-14" -ForegroundColor Yellow
    $script:fail += 2
}

# ---- 15: Element Plus CSS ----
$mjContent = ""
if (Test-Path $mj) { $mjContent = Get-Content $mj -Raw }
Check "main.js 引用 element-plus" ($mjContent -match 'element-plus') $mjContent.Substring(0, [Math]::Min(300, $mjContent.Length))

# ---- 总结 ----
Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ("PASS: " + $pass + "  FAIL: " + $fail)
Write-Host "=================================" -ForegroundColor Cyan
if ($fail -eq 0) {
    Write-Host "ALL CHECKS PASSED — W-FRONT-02-A 验收通过" -ForegroundColor Green
    exit 0
} else {
    Write-Host ("FAIL " + $fail + " 项，打回 worker 重做") -ForegroundColor Red
    exit 1
}
