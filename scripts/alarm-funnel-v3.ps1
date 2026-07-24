# 1h 报警漏斗统计 v3 - 用 substring 字典序比较，简化结构
$log = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log"
$errlog = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log"

function Count-Pattern {
    param([string]$Path, [string]$Start, [string]$End, [string]$Pattern)
    $cnt = 0
    Get-Content $Path -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_.Length -lt 19) { return }
        $ts = $_.Substring(0, 19)
        if ($ts -ge $Start -and $ts -lt $End -and $_.Contains($Pattern)) {
            $cnt++
        }
    }
    return $cnt
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

    $pct = { param($n,$t) if($t -gt 0){$n/$t}else{0} }

    Write-Host ""
    Write-Host "===== $Label ====="
    Write-Host "窗口: $Start ~ $End"
    Write-Host ""
    Write-Host ("  收到报警 (receive alarm)        {0,8:N0}   {1,7:P2}" -f $receive, 1.0)
    Write-Host ""
    Write-Host ("① 模板过滤 (not interesting)     {0,8:N0}   {1,7:P2}" -f $notInt, (& $pct $notInt $receive))
    Write-Host ("  剩余进入 isIgnore 检查         {0,8:N0}   {1,7:P2}" -f $interest, (& $pct $interest $receive))
    Write-Host ""
    Write-Host ("② isIgnore 关键字命中             {0,8:N0}   {1,7:P2}" -f $isIgn, (& $pct $isIgn $receive))
    Write-Host ""
    Write-Host ("③ yk push 调用 (uploadEn=false 应=0)  {0,8:N0}   {1,7:P2}" -f $ykPush, (& $pct $ykPush $receive))
    Write-Host ""
    Write-Host ("④ error.log ERROR                {0,8:N0}   {1,7:P2}" -f $errTot, (& $pct $errTot $receive))
    Write-Host ("   BadSqlGrammarException          {0,8:N0}   {1,7:P2}" -f $badSql, (& $pct $badSql $receive))
}

# 三个窗口
Show-Funnel -Label "最近完整 1h (20:00-21:00)" -Start "2026-07-23 20:00:00" -End "2026-07-23 21:00:00"
Show-Funnel -Label "W-X22 1h 灰盒实测 (17:02:50-18:03)" -Start "2026-07-23 17:02:50" -End "2026-07-23 18:03:00"
Show-Funnel -Label "当前 1h (21:00-22:00，未完)" -Start "2026-07-23 21:00:00" -End "2026-07-23 22:00:00"

Write-Host ""
Write-Host "===== 24h receive alarm 分布（看趋势）====="
$day = "2026-07-23"
for ($h=0; $h -lt 24; $h++) {
    $hh = $h.ToString("00")
    $start = "$day ${hh}:00:00"
    $end   = if ($h -eq 23) { "2026-07-24 00:00:00" } else { "$day ${hh}:59:59" }
    $cnt = Count-Pattern -Path $log -Start $start -End $end -Pattern 'receive alarm'
    Write-Host ("  ${hh}:00 - ${hh}:59   {0,6:N0}" -f $cnt)
}
