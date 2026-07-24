$ErrorActionPreference = 'Stop'
$root = 'E:\DEMO\数据采集'
$ps1 = Join-Path $root 'scripts\log-healthcheck.ps1'
$b = [System.IO.File]::ReadAllBytes($ps1)
Write-Host ('Total bytes: {0}' -f $b.Length)
Write-Host ('First 4 bytes hex: {0:X2} {1:X2} {2:X2} {3:X2}' -f $b[0],$b[1],$b[2],$b[3])
Write-Host ('Has UTF-8 BOM: {0}' -f ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF))
$lines = Get-Content -LiteralPath $ps1
for ($i = 87; $i -lt 100; $i++) {
    $lineNum = $i + 1
    $line = $lines[$i]
    Write-Host ('{0,4} [{1} chars]: {2}' -f $lineNum, $line.Length, $line)
}
Write-Host '--- Parse check ---'
$errs = $null
$tokens = $null
[System.Management.Automation.Language.Parser]::ParseFile($ps1, [ref]$tokens, [ref]$errs) | Out-Null
if ($errs.Count -eq 0) {
    Write-Host 'PARSE OK'
} else {
    foreach ($e in $errs) {
        Write-Host ('PARSE ERR line {0} col {1}: {2}' -f $e.Extent.StartLineNumber, $e.Extent.StartColumnNumber, $e.Message)
    }
}
