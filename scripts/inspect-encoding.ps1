param([Parameter(Mandatory=$true)][string]$Path)
$bytes = [System.IO.File]::ReadAllBytes($Path)
$len = $bytes.Length
Write-Output "Total bytes: $len"
Write-Output "First 16 bytes hex:"
$head = ($bytes[0..15] | ForEach-Object { $_.ToString('X2') }) -join ' '
Write-Output "  $head"
Write-Output "BOM check (first 3):"
$bom = ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
Write-Output "  UTF-8 BOM present: $bom"
Write-Output ""
Write-Output "Decoded as UTF-8 (first 400 chars):"
Write-Output ([System.Text.Encoding]::UTF8.GetString($bytes, 0, [Math]::Min(400, $len)))
Write-Output ""
Write-Output "Decoded as GBK (first 400 chars):"
$gbk = [System.Text.Encoding]::GetEncoding('GB18030')
Write-Output ($gbk.GetString($bytes, 0, [Math]::Min(400, $len)))
