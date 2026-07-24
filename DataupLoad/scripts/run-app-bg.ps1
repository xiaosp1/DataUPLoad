$ErrorActionPreference = 'Stop'
Set-Location E:\DEMO\数据采集\DataupLoad

$libDir = Join-Path (Get-Location) 'lib'
$cpParts = @()
Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $cpParts += $_.FullName }
$cpParts += (Join-Path (Get-Location) 'target\classes')
$cp = [string]::Join(';', $cpParts)

# Kill any existing hik-java processes (clean state)
Get-Process -Name "hik-java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 1

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe'
$argList = @(
   '-cp', $cp,
   '-Dfile.encoding=UTF-8',
   '-Dspring.config.location=./config/',
   '-Dspring.config.name=application',
   '-Dserver.port=80',
   'com.hikrobotics.solution.Application'
)
$psi.Arguments = ($argList | ForEach-Object { '"' + $_ + '"' }) -join ' '
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.WorkingDirectory = (Get-Location).Path
$psi.WindowStyle = 'Hidden'

$p = [System.Diagnostics.Process]::Start($psi)
Write-Host "Started hik-java PID=$($p.Id)"
# Don't kill parent — let it run

# Wait for Tomcat to come up
$maxWait = 60
$elapsed = 0
while ($elapsed -lt $maxWait) {
   Start-Sleep 2
   $elapsed += 2
   $conn = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
   if ($conn) {
      Write-Host "Tomcat listening on port 80 after $elapsed seconds"
      exit 0
   }
}
Write-Host "Tomcat did not come up within $maxWait seconds"
exit 1
