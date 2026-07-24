$ErrorActionPreference = 'Stop'
Set-Location E:\DEMO\数据采集\DataupLoad
$libDir = Join-Path (Get-Location) 'lib'
$cpParts = Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $_.FullName }
$cp = [string]::Join(';', $cpParts)
$srcFiles = Get-ChildItem -Path 'src\main\java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
if (-not (Test-Path 'target\classes')) { New-Item -ItemType Directory -Path 'target\classes' | Out-Null }
$args = @('-d','target\classes','-cp',$cp,'-encoding','UTF-8','-source','17','-target','17','-parameters','-Xlint:none','-nowarn','-Xdiags:verbose') + $srcFiles
& 'E:\DEMO\数据采集\DataupLoad\jdk\bin\javac.exe' @args 2>&1
Write-Host "ExitCode: $LASTEXITCODE"
