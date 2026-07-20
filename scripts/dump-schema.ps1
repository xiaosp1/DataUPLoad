param([string]$WorkDir, [string]$OutFile)
Set-Location -LiteralPath $WorkDir
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
& dotnet run --no-build schema *>&1 | Out-File -LiteralPath $OutFile -Encoding utf8
Write-Output "Wrote $OutFile ($((Get-Item $OutFile).Length) bytes)"
