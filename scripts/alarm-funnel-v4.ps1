# 1h 报警漏斗统计 v4 - 用 Select-String + Pattern
$log = "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\DataupLoad.log"
$errlog = "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\error.log"

# 测试 log 能读
Write-Host "log size: $((Get-Item $log).Length)"
Write-Host "errlog size: $((Get-Item $errlog).Length)"

function Count-Pattern {
    param([string]$Path, [string]$Start, [string]$End, [string]$Pattern)
    $matched = Select-String -Path $Path -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        ($ts -ge $Start) -and ($ts -lt $End) -and ($_.Line.Contains($Pattern))
    }
    return $matched.Count
}

function Show-Funnel {
    param([string]$Label, [string]$Start, [string]$End)

    $receive = Count-Pattern -Path $log -Start $Start -End $End -Pattern 'receive alarm'
    $notInt  = Count-Pattern -Path $log -Start $Start -End $End -Pattern 'not interesting defect'
    $isIgn   = Count-Pattern -Path $log -Start $Start -End $End -Pattern 'isIgnore'
    $ykPush  = Count-Pattern -Path $log -Start $Start -End $End -Pattern 'pushAlarm2YK'
    $badSql  = Count-Pattern -Path $errlog -Start $Start -End $End -Pattern 'BadSqlGrammarException'
    $errTot  = Count-Pattern -Path $errlog -Start $Start -End $End -Pattern 'ERROR'
    $interest = $receive - $notInt

    Write-Host ""
    Write-Host ("===== {0} =====" -f $Label)
    Write-Host ("window: {0} ~ {1}" -f $Start, $End)
    Write-Host ""
    Write-Host ("  receive alarm                {0,8:N0}" -f $receive)
    Write-Host ""
    Write-Host ("(1) not interesting template   {0,8:N0}" -f $notInt)
    Write-Host ("    remaining to isIgnore      {0,8:N0}" -f $interest)
    Write-Host ""
    Write-Host ("(2) isIgnore keyword hit       {0,8:N0}" -f $isIgn)
    Write-Host ""
    Write-Host ("(3) yk push called             {0,8:N0}" -f $ykPush)
    Write-Host ""
    Write-Host ("(4) error.log ERROR            {0,8:N0}" -f $errTot)
    Write-Host ("    BadSqlGrammarException     {0,8:N0}" -f $badSql)
}

Show-Funnel -Label "最近完整 1h (20:00-21:00)" -Start "2026-07-23 20:00:00" -End "2026-07-23 21:00:00"
Show-Funnel -Label "W-X22 1h 灰盒实测 (17:02:50-18:03)" -Start "2026-07-23 17:02:50" -End "2026-07-23 18:03:00"
Show-Funnel -Label "当前 1h (21:00-22:00，未完)" -Start "2026-07-23 21:00:00" -End "2026-07-23 22:00:00"

Write-Host ""
Write-Host "===== 24h receive alarm 分布 ====="
$day = "2026-07-23"
for ($h=0; $h -lt 24; $h++) {
    $hh = $h.ToString("00")
    $start = "$day ${hh}:00:00"
    $end   = if ($h -eq 23) { "2026-07-24 00:00:00" } else { "$day ${hh}:59:59" }
    $cnt = Count-Pattern -Path $log -Start $start -End $end -Pattern 'receive alarm'
    Write-Host ("  {0}:00 - {0}:59   {1,6:N0}" -f $hh, $cnt)
}
