param([Parameter(Mandatory=$true)][string]$Path, [int]$Count = 16)
$bytes = [System.IO.File]::ReadAllBytes($Path)
$take = [Math]::Min($Count, $bytes.Length)
$hex = ''
for ($i = 0; $i -lt $take; $i++) {
    $hex += '{0:X2} ' -f $bytes[$i]
}
Write-Host ("First {0} bytes of {1} ({2} total): {3}" -f $take, $Path, $bytes.Length, $hex.TrimEnd())

if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    Write-Host "  WARN: file has UTF-8 BOM" -ForegroundColor Yellow
} else {
    Write-Host "  OK: no BOM" -ForegroundColor Green
}

try {
    [System.Text.Encoding]::UTF8.GetString($bytes) | Out-Null
    Write-Host "  OK: valid UTF-8" -ForegroundColor Green
} catch {
    Write-Host ("  FAIL: not valid UTF-8: " + $_.Exception.Message) -ForegroundColor Red
}
