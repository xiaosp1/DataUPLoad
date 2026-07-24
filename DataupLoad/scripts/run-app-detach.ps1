$ErrorActionPreference = 'Stop'
Set-Location E:\DEMO\数据采集\DataupLoad

# kill any prior instance
Get-Process -Name "hik-java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 1

$libDir = Join-Path (Get-Location) 'lib'
$cpParts = @()
Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $cpParts += $_.FullName }
$cpParts += (Join-Path (Get-Location) 'target\classes')
$cp = [string]::Join(';', $cpParts)

# Use Start-Process with -PassThru so we return immediately; child detached
$si = New-Object System.Diagnostics.ProcessStartInfo
$si.FileName = 'E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe'
$argList = @(
   '-cp', $cp,
   '-Dfile.encoding=UTF-8',
   '-Dspring.config.location=./config/',
   '-Dspring.config.name=application',
   '-Dserver.port=80',
   'com.hikrobotics.solution.Application'
)
$si.Arguments = ($argList | ForEach-Object { '"' + $_ + '"' }) -join ' '
$si.UseShellExecute = $false
$si.RedirectStandardOutput = $false
$si.RedirectStandardError = $false
$si.WorkingDirectory = (Get-Location).Path
$si.WindowStyle = 'Hidden'
$si.CreateNoWindow = $true

$p = [System.Diagnostics.Process]::Start($si)
Write-Host "Started hik-java PID=$($p.Id) detached"
$p.Dispose()

# wait for Tomcat
$maxWait = 75
$elapsed = 0
while ($elapsed -lt $maxWait) {
   Start-Sleep 3
   $elapsed += 3
   $conn = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
   if ($conn) {
      Write-Host "Tomcat listening on port 80 after $elapsed seconds"
      exit 0
   }
   $stillRunning = Get-Process -Name "hik-java" -ErrorAction SilentlyContinue
   if (-not $stillRunning) {
      Write-Host "hik-java exited before listening (elapsed=${elapsed}s)"
      exit 2
   }
}
Write-Host "Tomcat did not come up within $maxWait seconds"
exit 1
