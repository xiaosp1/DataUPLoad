# 1h 报警漏斗统计 v2 - 修字符串比较
$log = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log"
$errlog = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log"

function Get-Funnel {
    param([string]$Label, [string]$Start, [string]$End)

    # 用正则截前 19 字符(到秒)再比较
    $receive = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and $_.Line -match 'receive alarm'
    }).Count

    $notInt = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and $_.Line -match 'not interesting defect'
    }).Count

    $isIgnore = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and $_.Line -match 'isIgnore'
    }).Count

    $ykPush = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and ($_.Line -match 'pushAlarm2YK' -or $_.Line -match 'yk push start' -or $_.Line -match 'YKServiceImpl.pushAlarm')
    }).Count

    $badSql = (Select-String -Path $errlog -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and $_.Line -match 'BadSqlGrammarException'
    }).Count

    $errTotal = (Select-String -Path $errlog -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and $_.Line -match 'ERROR'
    }).Count

    $ignoreUpdate = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts = $_.Line.Substring(0, 19)
        $ts -ge $Start -and $ts -lt $End -and ($_.Line -match 'update.*set.*solve' -or $_.Line -match 'similar alarm.*IGNORE' -or $_.Line -match 'same.*defect.*IGNORE')
    }).Count

    $interesting = $receive - $notInt

    Write-Host ""
    Write-Host "===== $Label ====="
    Write-Host "窗口: $Start ~ $End"
    Write-Host ""
    Write-Host "关卡                          数量        占比"
    Write-Host "---------------------------------------------------------------"
    Write-Host ("  收到报警 (receive alarm)         {0,8:N0}  {1,7:P2}" -f $receive, 1.0)
    Write-Host ""
    Write-Host ("① 模板过滤 (not interesting)     {0,8:N0}  {1,7:P2}" -f $notInt, ($(if($receive -gt 0){$notInt/$receive}else{0})))
    Write-Host ("  剩余到 isIgnore 检查            {0,8:N0}  {1,7:P2}" -f $interesting, ($(if($receive -gt 0){$interesting/$receive}else{0})))
    Write-Host ""
    Write-Host ("② 同类去重 IGNORE 更新            {0,8:N0}  {1,7:P2}" -f $ignoreUpdate, ($(if($receive -gt 0){$ignoreUpdate/$receive}else{0})))
    Write-Host ""
    Write-Host ("③ 白名单命中 isIgnore             {0,8:N0}  {1,7:P2}" -f $isIgnore, ($(if($receive -gt 0){$isIgnore/$receive}else{0})))
    Write-Host ""
    Write-Host ("④ yk push 调用                   {0,8:N0}  {1,7:P2}" -f $ykPush, ($(if($receive -gt 0){$ykPush/$receive}else{0})))
    Write-Host ""
    Write-Host ("⑤ error.log ERROR                {0,8:N0}  {1,7:P2}" -f $errTotal, ($(if($receive -gt 0){$errTotal/$receive}else{0})))
    Write-Host ("   BadSqlGrammarException          {0,8:N0}  {1,7:P2}" -f $badSql, ($(if($receive -gt 0){$badSql/$receive}else{0})))

    return [pscustomobject]@{
        Label = $Label; Start = $Start; End = $End
        Receive = $receive; NotInt = $notInt; Interesting = $interesting
        IgnoreUpdate = $ignoreUpdate; IsIgnore = $isIgnore
        YkPush = $ykPush; ErrTotal = $errTotal; BadSql = $badSql
    }
}

# 窗口 1: 最近完整 1h (20:00-21:00)
$r1 = Get-Funnel "最近完整 1h (20:00-21:00)" "2026-07-23 20:00:00" "2026-07-23 21:00:00"

# 窗口 2: W-X22 1h 灰盒实测
$r2 = Get-Funnel "W-X22 1h 灰盒实测 (17:02:50-18:03)" "2026-07-23 17:02:50" "2026-07-23 18:03:00"

# 窗口 3: 当前正在跑的 1h (21:00-22:00，未完)
$r3 = Get-Funnel "当前 1h (21:00-22:00，未完)" "2026-07-23 21:00:00" "2026-07-23 22:00:00"

Write-Host ""
Write-Host "===== 24h 各小时 receive alarm 分布（老板要趋势）====="
for ($h=0; $h -lt 24; $h++) {
    $ts = $h.ToString("00")
    $start = "2026-07-23 ${ts}:00:00"
    if ($h -eq 23) { $end = "2026-07-24 00:00:00" } else { $end = "2026-07-23 $($h.ToString('00')):59:59" }
    if ($h -eq 23) { $end = "2026-07-24 00:00:00" }
    $end = if ($h -eq 23) { "2026-07-24 00:00:00" } else { "2026-07-23 $($h.ToString('00')):59:59" }
    $cnt = (Select-String -Path $log -Pattern "^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" | Where-Object {
        $ts2 = $_.Line.Substring(0, 19)
        $ts2 -ge $start -and $ts2 -lt $end -and $_.Line -match 'receive alarm'
    }).Count
    Write-Host ("  ${ts}:00 - ${ts}:59  receive={0,6:N0}" -f $cnt)
}
