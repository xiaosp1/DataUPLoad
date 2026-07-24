# watchdog-edge.ps1 - v0.4 EdgeHost 看门狗（PM 守护进程）
#
# 用途：cron 每 30 分钟跑一次 log-healthcheck。
#       这个脚本独立运行（PM 自己起），每 60s 检查 EdgeHost 进程 + :80 /health。
#       如果 EdgeHost 挂了 → 自动拉起 + 推 alarm。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File watchdog-edge.ps1
#
# 退出码：
#   0 = EdgeHost 健康退出
#   1 = 调用方式错

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$logFile = Join-Path $root 'logs/watchdog.log'

function Log($msg) {
    $ts = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $line = "[$ts] $msg"
    Write-Host $line
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

Log "=== Watchdog started (PID=$PID) ==="

$exe = Join-Path $root 'src/IntcoEdge.EdgeHost/bin/Debug/net8.0/IntcoEdge.EdgeHost.exe'

while ($true) {
    Start-Sleep -Seconds 60

    $edgeHost = Get-Process -Name 'IntcoEdge.EdgeHost' -ErrorAction SilentlyContinue
    $port80 = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue

    if (-not $edgeHost -or -not $port80) {
        Log "[ALERT] EdgeHost 挂了: process=$($edgeHost.Id -join ',') port80=$($port80.OwningProcess -join ',') → 自动拉起"

        # 杀残留
        if ($edgeHost) {
            $edgeHost | Stop-Process -Force
            Start-Sleep -Seconds 2
        }

        # 拉起
        if (Test-Path $exe) {
            $env:ASPNETCORE_URLS = 'http://0.0.0.0:80'
            $proc = Start-Process -FilePath $exe -PassThru -RedirectStandardOutput "$root/logs/edge-stdout.log" -RedirectStandardError "$root/logs/edge-stderr.log"
            Log "[FIX] EdgeHost re-launched PID=$($proc.Id)"

            # 验证
            Start-Sleep -Seconds 5
            try {
                $r = Invoke-WebRequest -Uri 'http://localhost:80/health' -UseBasicParsing -TimeoutSec 5
                Log "[VERIFY] /health -> $($r.StatusCode) $($r.Content)"
            } catch {
                Log "[VERIFY-FAIL] /health: $($_.Exception.Message)"
            }
        } else {
            Log "[ERROR] EdgeHost.exe 不存在: $exe"
        }
    }
}
