$src = 'E:\DEMO\数据采集\scripts\log-healthcheck.ps1'
$txt = [System.IO.File]::ReadAllText($src)
# Write to TEMP as UTF-8
$tmp = Join-Path $env:TEMP 'log-healthcheck-utf8.ps1'
[System.IO.File]::WriteAllText($tmp, $txt, [System.Text.Encoding]::UTF8)

# Parse it
$tokens = $null
$errors = $null
$null = [System.Management.Automation.Language.Parser]::ParseFile($tmp, [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) {
    foreach ($e in $errors) {
        Write-Host ("LINE {0} COL {1}: {2}" -f $e.Extent.StartLineNumber, $e.Extent.StartColumnNumber, $e.Message)
    }
} else {
    Write-Host "No parse errors in script body"
    # Run it
    & $tmp -Verbose
}
