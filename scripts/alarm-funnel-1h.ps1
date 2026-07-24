# 1h 报警漏斗统计 - PM 22:00 给老板的最新窗口
# 老板指令: "把数据 统计给我，一个小时之内产生了多少报警 被哪一关拦截了多少"
# 时间: 2026-07-23 21:56 GMT+8
$log = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log"
$errlog = "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\error.log"

# 给最近完整 1h 段：21:00-22:00 不完整 → 用 20:00-21:00
# 但老板可能更想看最新 1h（含 22:00 部分），给 2 个窗口对比

function Get-Funnel {
    param([string]$Label, [string]$Start, [string]$End)

    # 1. receive alarm
    $receive = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'receive alarm' }).Count

    # 2. not interesting defect（模板过滤）
    $notInt = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'not interesting defect' }).Count

    # 3. isIgnore 命中
    $isIgnore = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'isIgnore.*true|白名单命中|命中 ignore' }).Count

    # 4. yk push 调用
    $ykPush = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'pushAlarm2YK|YKServiceImpl.pushAlarm|yk.*push.*start' }).Count

    # 5. BadSqlGrammarException
    $badSql = (Get-Content $errlog -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'BadSqlGrammarException' }).Count

    # 6. ERROR 总数
    $errTotal = (Get-Content $errlog -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'ERROR' }).Count

    # 7. 同类去重 IGNORE (LambdaUpdateWrapper UPDATE → solve=3)
    $ignoreUpdate = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'solve.*=.*3|IGNORE.*更新|更新.*IGNORE|update.*set.*solve' }).Count

    # 8. alarm_record 入库成功
    $insertOk = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'INSERT INTO alarm_record|alarm.*save.*success|alarm.*insert.*ok' }).Count

    # 9. UNSOLVED (solve=2) / SOLVED (solve=1)
    $unsolved = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'solve.*2|UNSOLVED' }).Count
    $solved = (Get-Content $log -ErrorAction SilentlyContinue | Where-Object { $_ -ge $Start -and $_ -lt $End -and $_ -match 'solve.*1|SOLVED' }).Count

    Write-Host ""
    Write-Host "===== $Label ====="
    Write-Host "窗口: $Start ~ $End"
    Write-Host ""
    Write-Host "关卡                          数量            占比"
    Write-Host "─────────────────────────────────────────────"
    Write-Host ("① 收到报警 (receive alarm)   {0,8:N0}        100.00%" -f $receive)
    Write-Host ""
    Write-Host ("② 模板过滤 (not interesting) {0,8:N0}        {1,7:P2}" -f $notInt, ($(if($receive -gt 0){$notInt/$receive}else{0})))
    Write-Host ""
    Write-Host ("  剩余进入 isIgnore 检查      {0,8:N0}        {1,7:P2}" -f ($receive - $notInt), ($(if($receive -gt 0){($receive - $notInt)/$receive}else{0})))
    Write-Host ""
    Write-Host ("③ 同类去重 IGNORE (solve=3)  {0,8:N0}        {1,7:P2}" -f $ignoreUpdate, ($(if($receive -gt 0){$ignoreUpdate/$receive}else{0})))
    Write-Host ("  UNSOLVED 入库 (solve=2)     {0,8:N0}        {1,7:P2}" -f $unsolved, ($(if($receive -gt 0){$unsolved/$receive}else{0})))
    Write-Host ("  SOLVED 入库 (solve=1)       {0,8:N0}        {1,7:P2}" -f $solved, ($(if($receive -gt 0){$solved/$receive}else{0})))
    Write-Host ""
    Write-Host ("④ 白名单命中 (isIgnore true)  {0,8:N0}        {1,7:P2}" -f $isIgnore, ($(if($receive -gt 0){$isIgnore/$receive}else{0})))
    Write-Host ""
    Write-Host ("⑤ yk push 调用 (uploadEn=false应=0) {0,8:N0}        {1,7:P2}" -f $ykPush, ($(if($receive -gt 0){$ykPush/$receive}else{0})))
    Write-Host ""
    Write-Host ("⑥ ERROR 总数 (error.log)     {0,8:N0}        {1,7:P2}" -f $errTotal, ($(if($receive -gt 0){$errTotal/$receive}else{0})))
    Write-Host ("  BadSqlGrammarException      {0,8:N0}        {1,7:P2}" -f $badSql, ($(if($receive -gt 0){$badSql/$receive}else{0})))

    return [pscustomobject]@{
        Label = $Label
        Start = $Start
        End = $End
        Receive = $receive
        NotInteresting = $notInt
        IgnoreUpdate = $ignoreUpdate
        IsIgnore = $isIgnore
        YkPush = $ykPush
        ErrTotal = $errTotal
        BadSql = $badSql
        InsertOk = $insertOk
    }
}

# 窗口 1: 最近完整 1h (20:00-21:00)
$r1 = Get-Funnel "最近完整 1h (20:00-21:00)" "2026-07-23 20:00:00" "2026-07-23 21:00:00"

# 窗口 2: W-X22 1h 灰盒实测 (17:02:50-18:03) 对比
$r2 = Get-Funnel "W-X22 1h 灰盒 (17:02:50-18:03)" "2026-07-23 17:02:50" "2026-07-23 18:03:00"

Write-Host ""
Write-Host "===== 当前 PG alarm_record / ignore_alarm 状态 ====="
$env:PGPASSFILE = "$env:USERPROFILE\.pgpass"
& "C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT COUNT(*) AS alarm_record_total FROM alarm_record"
& "C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT COUNT(*) AS ignore_alarm_total FROM ignore_alarm"
& "C:\Program Files\PostgreSQL\14\bin\psql.exe" -h 127.0.0.1 -p 5433 -U postgres -d intco -c "SELECT id, type, line_no, face_no, level, solve, defect_name, time, create_time FROM alarm_record ORDER BY id DESC LIMIT 5"
