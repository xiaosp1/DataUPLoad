# verify-encoding.ps1
# 验证指定文件：UTF-8 无 BOM，中文不乱码
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\verify-encoding.ps1 -Paths @("file1.cs","file2.xaml")
#   powershell -ExecutionPolicy Bypass -File scripts\verify-encoding.ps1 -Paths @("file1.cs") -ShowHex
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string[]]$Paths,
    [switch]$ShowHex
)

$ErrorActionPreference = 'Stop'
$failed = $false

foreach ($p in $Paths) {
    if (-not (Test-Path -LiteralPath $p)) {
        Write-Host "[MISS] $p" -ForegroundColor Red
        $failed = $true
        continue
    }
    $bytes = [System.IO.File]::ReadAllBytes($p)
    if ($bytes.Length -lt 3) {
        Write-Host "[OK-SHORT] $p (len=$($bytes.Length))" -ForegroundColor DarkGray
        continue
    }
    $bom = $bytes[0..2] -join ','
    if ($bom -eq '239,187,191') {
        Write-Host "[BOM!] $p — UTF-8 BOM detected (must be UTF-8 NO BOM)" -ForegroundColor Red
        $failed = $true
    }
    else {
        Write-Host "[OK]   $p — UTF-8 no BOM" -ForegroundColor Green
    }
    if ($ShowHex) {
        $len = [Math]::Min(16, $bytes.Length)
        $hex = ($bytes[0..($len - 1)] | ForEach-Object { $_.ToString('X2') }) -join ' '
        Write-Host "       first ${len} bytes: $hex"
    }
}

if ($failed) {
    exit 1
}
else {
    Write-Host "All files OK." -ForegroundColor Green
    exit 0
}
