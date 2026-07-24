# W-X23 would_push_count.ps1
# 老板 2026-07-24 07:06 拍板："只统计有几条报警可以推送 MES"
# 逻辑: 按 PSM 规则（send_yk_enable=1）查询本应推英科但实际未推的报警数
#       yk.uploadEnabled=false 兜底（铁则 42），所以"实际未推"是真的

param(
    [string]$Start = "",
    [string]$End = "",
    [string]$LogFile = "E:\DEMO\数据采集\logs\would-push-count.log"
)

$ErrorActionPreference = "Stop"
$env:PGPASSWORD = "postgres"
$psql = "C:\Program Files\PostgreSQL\14\bin\psql.exe"

# 时间窗：默认最近 1h
if (-not $Start) {
    $End = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $Start = (Get-Date).AddHours(-1).ToString("yyyy-MM-dd HH:mm:ss")
}

# 把 SQL 写到临时文件，避免 PowerShell 转义地狱
$tmpSql1 = [System.IO.Path]::GetTempFileName()
@"
SELECT
    COUNT(*) FILTER (WHERE dt.send_yk_enable = 1 AND ar.solve = 2) AS would_push_unsolved,
    COUNT(*) FILTER (WHERE dt.send_yk_enable = 1) AS would_push_total,
    COUNT(*) AS alarm_record_total,
    COUNT(DISTINCT ar.defect_name) AS distinct_defect_names
  FROM alarm_record ar
  LEFT JOIN defect_type dt ON ar.defect_name = dt.name
 WHERE ar.create_time >= '$Start' AND ar.create_time < '$End';
"@ | Set-Content -Path $tmpSql1 -Encoding UTF8

$tmpSql2 = [System.IO.Path]::GetTempFileName()
@"
SELECT COALESCE(ar.defect_name, '<NULL>') AS defect_name,
       COUNT(*) FILTER (WHERE dt.send_yk_enable = 1 AND ar.solve = 2) AS would_push,
       COUNT(*) AS total
  FROM alarm_record ar
  LEFT JOIN defect_type dt ON ar.defect_name = dt.name
 WHERE ar.create_time >= '$Start' AND ar.create_time < '$End'
 GROUP BY ar.defect_name
 ORDER BY would_push DESC, total DESC
 LIMIT 10;
"@ | Set-Content -Path $tmpSql2 -Encoding UTF8

$result = & $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -A -t -f $tmpSql1 2>&1
$line1 = ($result | Select-String -Pattern '^\s*\d' | Select-Object -First 1).Line.Trim()
$parts = $line1 -split '\|'

$wouldPushUnsolved = $parts[0]
$wouldPushTotal    = $parts[1]
$total             = $parts[2]
$distinct          = $parts[3]

$byName = & $psql -h 127.0.0.1 -p 5433 -U postgres -d intco -A -t -f $tmpSql2 2>&1
$byNameLines = $byName | Select-String -Pattern '^\s*\S'

Remove-Item $tmpSql1, $tmpSql2 -Force -ErrorAction SilentlyContinue

$ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$line = "[$ts] window=$Start ~ $End  would_push_unsolved=$wouldPushUnsolved  would_push_total=$wouldPushTotal  alarm_record_total=$total  distinct_defect_names=$distinct"

Write-Host $line
Write-Host ""
Write-Host "--- top 10 by defect_name ---"
$byNameLines | ForEach-Object { Write-Host $_.Line.Trim() }
Write-Host ""

Add-Content -Path $LogFile -Value $line
Add-Content -Path $LogFile -Value "--- top 10 by defect_name ---"
$byNameLines | ForEach-Object { Add-Content -Path $LogFile -Value $_.Line.Trim() }
Add-Content -Path $LogFile -Value ""

Write-Host ""
Write-Host "[老板只关心这一个数] 本应推送 MES 的 UNSOLVED 报警数: $wouldPushUnsolved"
