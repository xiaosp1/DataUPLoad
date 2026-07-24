# heartbeat-to-feishu.ps1
# PM 2026-07-22 19:53 修：用 [Console]::OutputEncoding = UTF-8 + chcp 65001

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

$basePath = "E:\DEMO\数据采集"
$outFile = Join-Path $basePath "heartbeat-current.txt"
$ymlPath = Join-Path $basePath "DataupLoad\config\application-prod.yml"
$logPath = Join-Path $basePath "DataupLoad\log\DataupLoad\info.log"

$port = (Get-NetTCPConnection -State Listen -LocalPort 80 -ErrorAction SilentlyContinue).OwningProcess
$proc = if ($port) { 
    $p = Get-Process -Id $port -ErrorAction SilentlyContinue
    if ($p) { "PID=$($p.Id) Mem=$([Math]::Round($p.WorkingSet64/1MB,0))MB Started=$($p.StartTime.ToString('HH:mm:ss'))" } 
    else { "[DOWN]" }
} else { "[DOWN]" }

$ykStatus = if (Select-String -Path $ymlPath -Pattern 'enable: true' -Quiet -ErrorAction SilentlyContinue) { "[ON]" } else { "[OFF-MELTDOWN]" }
$workerCount = (Get-Process -Name "codex*" -ErrorAction SilentlyContinue | Measure-Object).Count
$pgPid = (Get-NetTCPConnection -State Listen -LocalPort 5433 -ErrorAction SilentlyContinue).OwningProcess

$alarmLog = ""
if (Test-Path $logPath) {
    $alarmLog = (Get-Content $logPath -Tail 30 -ErrorAction SilentlyContinue | Select-String -Pattern 'alarm|Alarm|YK' | Select-Object -First 5 -ExpandProperty Line) -join " | "
}

$msg = "[PM HEARTBEAT $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')]" +
       "`n- hik-java 80 port: $proc" +
       "`n- yk push: $ykStatus" +
       "`n- Worker procs: $workerCount" +
       "`n- PG 5433 PID: $pgPid" +
       "`n- last alarm log: $alarmLog"

# UTF-8 NO BOM
$utf8NoBom = New-Object System.Text.UTF8Encoding $False
[System.IO.File]::WriteAllText($outFile, $msg, $utf8NoBom)
Write-Host "Heartbeat written: $outFile"
Write-Host "----"
Write-Host $msg
