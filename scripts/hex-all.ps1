param([Parameter(Mandatory=$true)][string]$Dir, [string]$Pattern='*')
$bad = 0
$count = 0
Get-ChildItem -LiteralPath $Dir -Filter $Pattern -File -Recurse | Sort-Object FullName | ForEach-Object {
    $count++
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $len = $bytes.Length
    $n = [Math]::Min(16, $len)
    $head = ($bytes[0..($n-1)] | ForEach-Object { $_.ToString('X2') }) -join ' '
    $bom = ($len -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
    $tag = if ($bom) { 'BOM-PRESENT' } else { 'ok' }
    if ($bom) { $bad++ }
    Write-Output ("[{0}] bytes={1} head16=[{2}] {3}" -f $_.FullName, $len, $head, $tag)
}
Write-Output ("--- scanned {0} file(s), {1} with UTF-8 BOM ---" -f $count, $bad)
exit $bad
