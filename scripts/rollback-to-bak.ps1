# rollback-to-bak.ps1
$ErrorActionPreference = 'Stop'
$bak = "E:\DataupLoad-final.jar.bak"
$dst = "E:\DataupLoad-final.jar"
$logFile = "E:\rollback-to-bak-$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

"=== rollback-to-bak.ps1 start $(Get-Date) ===" | Out-File $logFile -Append

# kill all java
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object {
    "killing java PID=$($_.Id)" | Out-File $logFile -Append
    Stop-Process -Id $_.Id -Force
}
Start-Sleep -Seconds 3

# restore bak
if (-not (Test-Path $bak)) {
    "FATAL: $bak missing" | Out-File $logFile -Append
    exit 1
}
Copy-Item -Path $bak -Destination $dst -Force
"restored bak to dst" | Out-File $logFile -Append

# revert yml
$yml = "E:\DEMO\数据采集\DataupLoad\config\application-prod.yml"
if (Test-Path $yml) {
    $content = Get-Content $yml -Raw
    if ($content -match 'baseline-version:\s*1\.20') {
        $content = $content -replace 'baseline-version:\s*1\.20', 'baseline-version: 0'
        Set-Content -Path $yml -Value $content -Encoding UTF8
        "reverted baseline-version" | Out-File $logFile -Append
    }
}

# start bak jar
$proc = Start-Process -FilePath "java" -ArgumentList @("-jar","-Dfile.encoding=UTF-8",$dst,"--spring.config.location=classpath:/,file:E:/DEMO/数据采集/DataupLoad/config/") -WorkingDirectory "E:\DEMO\数据采集" -RedirectStandardOutput "E:\DEMO\数据采集\logs\dataupload.out.log" -RedirectStandardError "E:\DEMO\数据采集\logs\dataupload.err.log" -PassThru -WindowStyle Hidden
"started java PID=$($proc.Id)" | Out-File $logFile -Append

Start-Sleep -Seconds 20
$port80 = netstat -ano | findstr ":80" | findstr LISTENING"
if ($port80) {
    "OK port80: $port80" | Out-File $logFile -Append
} else {
    "FAIL port80 not listening" | Out-File $logFile -Append
}
"=== rollback complete $(Get-Date) ===" | Out-File $logFile -Append
Write-Host "log: $logFile"
