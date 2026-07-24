$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location 'E:\DEMO\数据采集'
$lines = [System.IO.File]::ReadAllLines('.\scripts\log-healthcheck.ps1', [System.Text.Encoding]::UTF8)
$start = 85
$end = 105
for ($i = $start; $i -lt [Math]::Min($end, $lines.Length); $i++) {
    '{0:0000}: {1}' -f ($i + 1), $lines[$i]
}
