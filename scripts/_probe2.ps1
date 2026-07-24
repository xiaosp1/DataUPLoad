$path = Resolve-Path 'scripts/log-healthcheck.ps1'
Write-Host "Resolved: $path"
$lines = Get-Content $path
Write-Host ('Total lines: ' + $lines.Count)
Write-Host '--- Lines 80-100 ---'
for ($i = 79; $i -lt [Math]::Min($lines.Count, 100); $i++) {
    Write-Host (('{0,4}: {1}' -f ($i+1), $lines[$i]))
}
