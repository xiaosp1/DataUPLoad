$path = 'E:\DEMO\数据采集\scripts\log-healthcheck.ps1'
$bytes = [System.IO.File]::ReadAllBytes($path)
Write-Host ('Total bytes: ' + $bytes.Length)
Write-Host ('First 10 bytes hex: ' + (($bytes[0..9] | ForEach-Object { $_.ToString('X2') }) -join ' '))
$lines = [System.IO.File]::ReadAllLines($path)
Write-Host ('Total lines: ' + $lines.Length)
Write-Host '--- Lines 80-100 ---'
for ($i = 79; $i -lt [Math]::Min($lines.Length, 100); $i++) {
    Write-Host (('{0,4}: {1}' -f ($i+1), $lines[$i]))
}
