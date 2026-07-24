$ErrorActionPreference = 'Stop'
Set-Location E:\DEMO\数据采集\DataupLoad
$libDir = Join-Path (Get-Location) 'lib'
$cpParts = Get-ChildItem -Path $libDir -Filter '*.jar' | ForEach-Object { $_.FullName }
$cp = [string]::Join(';', $cpParts)

# Build fat jar
$fatJar = 'target\DataupLoad-fat.jar'
if (Test-Path $fatJar) { Remove-Item $fatJar -Force }

# Spring Boot loader jar - first in classpath (needs to be boot loader)
$bootLoader = Get-ChildItem -Path $libDir -Filter 'spring-boot-loader-*.jar' | Select-Object -First 1
$jarsToEmbed = Get-ChildItem -Path $libDir -Filter '*.jar' | Where-Object { $_.Name -notlike 'spring-boot-loader-*' }

# Create META-INF/MANIFEST with Start-Class
$manifestPath = 'target\MANIFEST.MF'
@"
Manifest-Version: 1.0
Start-Class: com.hikrobotics.solution.Application

"@ | Out-File -FilePath $manifestPath -Encoding ASCII -NoNewline

# Use jar c0mf for no-compression
& 'E:\DEMO\数据采集\DataupLoad\jdk\bin\jar.exe' cfm $fatJar $manifestPath -C target\classes . 2>&1 | Select-Object -First 5

# Append boot loader
& 'E:\DEMO\数据采集\DataupLoad\jdk\bin\jar.exe' uf $fatJar -C ($bootLoader.FullName -replace '\\[^\\]+$', '') ('org\springframework\boot\loader') 2>&1 | Select-Object -First 5

Write-Host "Manifest created. Classes packed."

# Use Spring Boot loader's layout: add BOOT-INF/lib and BOOT-INF/classes
# Simpler approach: build executable using -cp
Write-Host "Will run via -cp classpath approach instead of fat jar"
