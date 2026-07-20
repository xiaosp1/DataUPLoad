$root = $env:CHECK_ROOT
if (-not $root) { $root = 'E:\DEMO\数据采集\src\IntcoEdge.Desktop' }
Write-Host "Scanning: $root"
$bomCount = 0
$okCount = 0
Get-ChildItem -LiteralPath $root -File -Recurse -Include '*.cs','*.xaml','*.csproj','*.config','*.json' | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "BOM: $($_.FullName)"
        $bomCount++
    } else {
        $okCount++
    }
}
Write-Host "Summary: BOM=$bomCount OK=$okCount"
