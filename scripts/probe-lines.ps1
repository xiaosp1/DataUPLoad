Set-Location 'E:\DEMO\数据采集\scripts'
$path = '.\log-healthcheck.ps1'
$lines = Get-Content -LiteralPath $path
for ($i = 85; $i -lt 100; $i++) {
    Write-Host ("{0,4}: {1}" -f ($i+1), $lines[$i])
}
