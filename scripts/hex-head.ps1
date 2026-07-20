# Hex-verify first 16 bytes of a file. Reports BOM presence (EF BB BF = UTF-8 BOM, unwanted).
param([Parameter(Mandatory=$true)][string]$Path)
if (-not (Test-Path -LiteralPath $Path)) { Write-Error "not found: $Path"; exit 2 }
$bytes = [System.IO.File]::ReadAllBytes($Path)
$len = $bytes.Length
$n = [Math]::Min(16, $len)
$head = ($bytes[0..($n-1)] | ForEach-Object { $_.ToString('X2') }) -join ' '
$bom = ($len -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
Write-Output ("{0} bytes={1} head16=[{2}] utf8BOM={3}" -f $Path, $len, $head, $bom)
if ($bom) { exit 3 }
