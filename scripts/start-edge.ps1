# start-edge.ps1 — 一键启动 v0.4 EdgeHost (PM 维护)
#
# 用途：cd 到项目根 → 跑这个脚本 → 1) 跑 DB migration → 2) 编译 → 3) 启动 EdgeHost
#
# 用法（PowerShell）:
#   .\scripts\start-edge.ps1                # 默认 migrate + 启
#   .\scripts\start-edge.ps1 -SkipBuild     # 不编译（开发调试用）
#   .\scripts\start-edge.ps1 -Clean         # 先删 DB 再 migrate + 启（⚠️ 会清空数据）
#   .\scripts\start-edge.ps1 -Background    # 后台启 EdgeHost，PM 用
#
# 退出码：
#   0 = 成功
#   1 = migration 失败
#   2 = build 失败
#   3 = EdgeHost 启动失败

param(
    [switch]$SkipBuild,
    [switch]$Clean,
    [switch]$Background,
    [int]$Port = 80
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $root

# 强制 PowerShell 输出 UTF-8（避免 cmd 启动时中文乱码）
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
    $PSDefaultParameterValues['*:Encoding'] = 'utf8'
} catch {}

Write-Host "=== EdgeHost 一键启动器 ===" -ForegroundColor Cyan
Write-Host ("项目根: " + $root)
Write-Host ("时间  : " + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Write-Host ""

# 0. 杀旧进程
$running = Get-Process -Name 'IntcoEdge.EdgeHost' -ErrorAction SilentlyContinue
if ($running) {
    Write-Host "[0/4] 终止旧 EdgeHost 进程 (PID=$($running.Id -join ','))" -ForegroundColor Yellow
    $running | Stop-Process -Force
    Start-Sleep -Seconds 2
}

# 0.5 (可选) 清 DB
if ($Clean) {
    $db1 = Join-Path $root 'src/IntcoEdge.Db/data/intco.db'
    $db2 = Join-Path $root 'src/IntcoEdge.EdgeHost/bin/Debug/net8.0/data/intco.db'
    foreach ($p in @($db1, $db2)) {
        foreach ($ext in @('', '-wal', '-shm')) {
            $full = "$p$ext"
            if (Test-Path $full) {
                Write-Host "[0.5] 清 DB: $full" -ForegroundColor Yellow
                Remove-Item $full -Force
            }
        }
    }
}

# 1. 跑 migration（从 src/IntcoEdge.Db 目录）
Write-Host "[1/4] 跑 migration..." -ForegroundColor Cyan
Push-Location 'src/IntcoEdge.Db'
try {
    & dotnet run -- migrate 2>&1 | Tee-Object -FilePath "$root/logs/migrate-last.log" | Select-Object -Last 5
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ migration 失败,退出码 $LASTEXITCODE" -ForegroundColor Red
        Pop-Location
        exit 1
    }
} finally {
    Pop-Location
}

# 2. 编译（可选）
if (-not $SkipBuild) {
    Write-Host "[2/4] 编译 EdgeHost..." -ForegroundColor Cyan
    & dotnet build src/IntcoEdge.EdgeHost --nologo -v quiet 2>&1 | Tee-Object -FilePath "$root/logs/build-last.log" | Select-Object -Last 5
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ build 失败,退出码 $LASTEXITCODE" -ForegroundColor Red
        exit 2
    }
}

# 3. 验证 DB 文件存在
$dbFinal = Join-Path $root 'src/IntcoEdge.Db/data/intco.db'
if (-not (Test-Path $dbFinal)) {
    Write-Host "❌ DB 文件不存在: $dbFinal" -ForegroundColor Red
    exit 1
}
Write-Host "✅ DB: $dbFinal ($([math]::Round((Get-Item $dbFinal).Length/1KB, 2)) KB)" -ForegroundColor Green

# 4. 启动 EdgeHost
$exe = Join-Path $root 'src/IntcoEdge.EdgeHost/bin/Debug/net8.0/IntcoEdge.EdgeHost.exe'

# 4.0 如果端口 < 1024，授权 urlacl + 加防火墙规则（admin）
if ($Port -lt 1024) {
    Write-Host "[3.5] 端口 $Port < 1024，配置 urlacl + 防火墙..." -ForegroundColor Cyan
    $urlaclExists = netsh http show urlacl 2>&1 | Select-String "http://\+\:$Port/"
    if (-not $urlaclExists) {
        netsh http add urlacl url=http://+:$Port/ user=Everyone 2>&1 | Out-Null
        Write-Host "  urlacl: http://+:$Port/ added" -ForegroundColor Yellow
    }
    $fwExists = Get-NetFirewallRule -DisplayName "EdgeHost v0.4 HTTP $Port" -ErrorAction SilentlyContinue
    if (-not $fwExists) {
        New-NetFirewallRule -DisplayName "EdgeHost v0.4 HTTP $Port" -Direction Inbound -Protocol TCP -LocalPort $Port -Action Allow -Profile Any 2>&1 | Out-Null
        Write-Host "  firewall: EdgeHost v0.4 HTTP $Port added" -ForegroundColor Yellow
    }
}
if (-not (Test-Path $exe)) {
    Write-Host "❌ EdgeHost.exe 不存在,请先编译: $exe" -ForegroundColor Red
    exit 2
}

if ($Background) {
    Write-Host "[3/4] 后台启动 EdgeHost on :$Port ..." -ForegroundColor Cyan
    $env:ASPNETCORE_URLS = "http://0.0.0.0:$Port"
    $proc = Start-Process -FilePath $exe -PassThru -RedirectStandardOutput "$root/logs/edge-stdout.log" -RedirectStandardError "$root/logs/edge-stderr.log"
    Write-Host "✅ EdgeHost PID=$($proc.Id) 后台运行 (port $Port)" -ForegroundColor Green
} else {
    Write-Host "[3/4] 前台启动 EdgeHost on :$Port (Ctrl+C 退出)..." -ForegroundColor Cyan
    $env:ASPNETCORE_URLS = "http://0.0.0.0:$Port"
    & $exe
}

Write-Host "=== 启动完成 ===" -ForegroundColor Cyan
