$ErrorActionPreference = 'Stop'
Set-Location E:\DEMO\数据采集\DataupLoad
$libDir = Join-Path (Get-Location) 'lib'
$cpParts = Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $_.FullName }
$cpParts += (Join-Path (Get-Location) 'target\classes')
$cpParts += (Join-Path (Get-Location) 'config')
$cp = [string]::Join(';', $cpParts)

# Use hik-java.exe -cp to start Spring Boot app
$args = @(
   '-cp', $cp,
   '-Dfile.encoding=UTF-8',
   '-Dspring.config.location=./config/',
   '-Dspring.config.name=application',
   '-Dserver.port=80',
   '-Dlogging.level.root=INFO',
   'com.hikrobotics.solution.Application'
)
Write-Host "Starting app with classpath of $($cpParts.Count) entries..."
& 'E:\DEMO\数据采集\DataupLoad\jdk\bin\hik-java.exe' @args 2>&1
