$ErrorActionPreference = 'Stop'
Set-Location "E:\DEMO\数据采集\DataupLoad"

Get-Process -Name "hik-java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 2

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe'
$psi.Arguments = '-cp "lib\*;target\classes" -Dfile.encoding=UTF-8 -Dspring.config.location=./config/ -Dspring.config.name=application -Dserver.port=8080 com.hikrobotics.solution.Application'
$psi.UseShellExecute = $false
$psi.WorkingDirectory = (Get-Location).Path
$psi.WindowStyle = 'Hidden'
$psi.CreateNoWindow = $true
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.StandardInputEncoding = [System.Text.Encoding]::UTF8
$psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
$psi.StandardErrorEncoding = [System.Text.Encoding]::UTF8

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
$proc.Start() | Out-Null
Write-Host "Started PID=$($proc.Id)"
$proc.StandardInput.Close()
# Don't read stdout/stderr - it would block
$proc.Dispose()

Start-Sleep 35
$proc2 = Get-Process -Name "hik-java" -ErrorAction SilentlyContinue
if ($proc2) {
   Write-Host "Running: $($proc2.Id)"
} else {
   Write-Host "Process not running"
}
$listen = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue
if ($listen) {
   Write-Host "Port 80 listening: $($listen.OwningProcess)"
} else {
   Write-Host "Port 80 not listening"
}
