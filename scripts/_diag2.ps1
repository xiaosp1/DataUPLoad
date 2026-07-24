$path = $args[0]
$bytes = [System.IO.File]::ReadAllBytes($path)
$hex = ($bytes[0..3] | ForEach-Object { $_.ToString('X2') }) -join ' '
Write-Host ("First 4 bytes hex: " + $hex)
Write-Host ("Total bytes: " + $bytes.Length)
$content = [System.IO.File]::ReadAllText($path)
$lines = $content -split "`n"
Write-Host "Line 90-95:"
for ($i=89; $i -lt [Math]::Min(95, $lines.Count); $i++) { Write-Host ("{0,3}: {1}" -f ($i+1), $lines[$i]) }
