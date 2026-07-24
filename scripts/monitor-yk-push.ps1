# =============================================================================
# monitor-yk-push.ps1 — W-X18 灰盒期 24h 监控（7 信号扩展版）
# =============================================================================
# 派工单：W-X18 灰盒期 24h 监控扩展
# 监控 7 个 status=ALERT 触发条件：
#   ┌─ 原有 3 个（W-X14 SOP 三信号）
#   │  1. yk 调用日志        — 1min 内 ticket success AND uploadEnabled=true → ALERT（铁则 36 + 铁则 42 双开关）
#   │  2. PG alarm_record 增长 — 5min 内 > 10 行/min → ALERT
#   │  3. yk 配置            — uploadEnabled=true → ALERT（loginEnabled=true 灰盒合法，uploadEnabled 必须 false）
#   └─ 新增 4 个（W-X18）
#      4. ticket 续期监控    — 最近 50min 内至少 1 次 `success to get ticket from yk` → OK；否则 ALERT（loginEnabled=true 时）
#      5. alarm_record 对比  — 5min 内 PG count vs DataupLoad.log receive alarm 数，差 > 5% → ALERT
#      6. ignore_alarm 变更  — 1min 内 ignore_alarm 表 count 变化 → ALERT（白名单被改）
#      7. hik-java CPU user  — 10min 内累计 UserModeTime 增量 > 200s → ALERT
#
# 输出格式（每行）：
#   [YYYY-MM-DD HH:MM:SS] status=OK | signal1=... | signal2=... | ... | signal7=...
#   [YYYY-MM-DD HH:MM:SS] status=ALERT | why=<失败的信号+原因>
#
# 输出文件：E:\DEMO\数据采集\logs\monitor-yk-push.log（追加，UTF-8 无 BOM）
#
# 跑法（每 60s 一次，cron / Task Scheduler）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File E:\DEMO\数据采集\scripts\monitor-yk-push.ps1
#
# 严禁：
#   - ❌ 不改 hik-java 代码
#   - ❌ 不改 yml
#   - ❌ 不重启 hik-java
#   - ❌ 不改 uploadEnabled（铁则 36 + 铁则 42）
#   - ❌ 不改 enable / loginEnabled（只读）
# =============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'

# -----------------------------------------------------------------------------
# 常量（路径与阈值）
# -----------------------------------------------------------------------------
$Script:YkConfigPath      = 'E:\DEMO\数据采集\DataupLoad\config\application-prod.yml'
$Script:LogDir            = 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad'
$Script:LogFile           = 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log'
$Script:MonitorLogPath    = 'E:\DEMO\数据采集\logs\monitor-yk-push.log'
$Script:StateFile         = Join-Path $env:TEMP 'monitor-yk-push-state.json'

$Script:PgHost            = '127.0.0.1'
$Script:PgPort            = 5433
$Script:PgDb              = 'intco'
$Script:PgUser            = 'postgres'
$Script:PsqlPath          = 'C:\Program Files\PostgreSQL\14\bin\psql.exe'

# 阈值（按工单定义）
$Script:AlarmGrowthPerMin = 10          # 信号2：alarm_record 5min > 10 行/min
$Script:TicketRenewMin    = 50          # 信号4：ticket 续期窗口（分钟）
$Script:AlarmLogWindowMin = 5           # 信号5：alarm_record 对比窗口（分钟）
$Script:AlarmDeltaPct     = 0.05        # 信号5：> 5% 差异 ALERT
$Script:IgnorePollSec     = 60          # 信号6：ignore_alarm 1min 一次 count（脚本默认 60s 跑一次，刚好一致）
$Script:CpuUserWindowSec  = 600         # 信号7：CPU user 累计窗口（秒）
$Script:CpuUserThresholdSec = 200       # 信号7：> 200s/10min ALERT

# -----------------------------------------------------------------------------
# PGPASSWORD 自愈（W-X12b Step 0 沿用）：从 application-prod.yml 解析
# -----------------------------------------------------------------------------
$Script:PgPassword = $null
if ($env:PGPASSWORD) {
    $Script:PgPassword = $env:PGPASSWORD
} else {
    $pgYamlCandidates = @(
        'E:\DEMO\数据采集\DataupLoad\config\application-prod.yml',
        'E:\DEMO\数据采集\DataupLoad\src\main\resources\application-prod.yml'
    )
    foreach ($pgYaml in $pgYamlCandidates) {
        if (-not (Test-Path $pgYaml)) { continue }
        try {
            $lines = Get-Content -Path $pgYaml -Encoding UTF8 -ErrorAction Stop
            $inDs = $false
            foreach ($ln in $lines) {
                $trim = $ln.Trim()
                if ($trim -match '^datasource\s*:\s*$') { $inDs = $true; continue }
                if ($inDs) {
                    if ($ln -notmatch '^\s{2,}\S' -and $trim -match '^[A-Za-z_][A-Za-z0-9_-]*\s*:') { break }
                    if ($trim -match '^password\s*:\s*(.+?)\s*$') {
                        $val = $Matches[1].Trim().Trim('"').Trim("'")
                        if ($val.Length -gt 0) {
                            $Script:PgPassword = $val
                            break
                        }
                    }
                }
            }
        } catch { }
        if ($Script:PgPassword) { break }
    }
}

# -----------------------------------------------------------------------------
# 工具：东八区本地时间戳
# -----------------------------------------------------------------------------
function Get-BeijingNow {
    return (Get-Date).ToUniversalTime().AddHours(8).ToString("yyyy-MM-ddTHH:mm:ss")
}

# -----------------------------------------------------------------------------
# 工具：PG 查询小工具（返回单个 int，失败返回 $null + reason）
# -----------------------------------------------------------------------------
function Invoke-PgScalarInt {
    param(
        [Parameter(Mandatory)][string]$Query
    )
    if (-not $Script:PgPassword) {
        return [pscustomobject]@{ ok = $false; value = $null; reason = 'PG credentials unavailable' }
    }
    if (-not (Test-Path $Script:PsqlPath)) {
        # 备用：常见安装位
        $candidates = @(
            'C:\Program Files\PostgreSQL\14\bin\psql.exe',
            'C:\Program Files\PostgreSQL\13\bin\psql.exe',
            'C:\Program Files\PostgreSQL\15\bin\psql.exe',
            'C:\Program Files\PostgreSQL\16\bin\psql.exe'
        )
        foreach ($c in $candidates) {
            if (Test-Path $c) { $Script:PsqlPath = $c; break }
        }
    }
    if (-not (Test-Path $Script:PsqlPath)) {
        return [pscustomobject]@{ ok = $false; value = $null; reason = 'psql.exe not found' }
    }
    $prevPw = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $Script:PgPassword
        $out = & $Script:PsqlPath -h $Script:PgHost -p $Script:PgPort -U $Script:PgUser -d $Script:PgDb -tA -c $Query 2>&1
        $stderrText = ($out | Where-Object { $_ -is [string] }) -join ' '
        if ($LASTEXITCODE -ne 0) {
            return [pscustomobject]@{
                ok     = $false
                value  = $null
                reason = "PG error: $($stderrText.Substring(0, [Math]::Min(160, $stderrText.Length)))"
            }
        }
        $first = ($out | Select-Object -First 1)
        if ($null -eq $first) { return [pscustomobject]@{ ok = $false; value = $null; reason = 'PG empty result' } }
        return [pscustomobject]@{ ok = $true; value = [int]$first; reason = 'ok' }
    } catch {
        return [pscustomobject]@{ ok = $false; value = $null; reason = "PG exception: $($_.Exception.Message)" }
    } finally {
        # 恢复原 env（而不是删）
        if ($null -ne $prevPw) {
            $env:PGPASSWORD = $prevPw
        } else {
            Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        }
    }
}

# -----------------------------------------------------------------------------
# 信号 3：yk 配置（W-X14 适配双开关）
#   旧版 yk.enable 已废弃（保留兼容），新版 yk.loginEnabled + yk.uploadEnabled
#   灰盒规则（铁则 36 + 铁则 42）：
#     - uploadEnabled 必须是 false（真推开关），true 即 ALERT
#     - loginEnabled 灰盒期 true 是合法的（凭证预热），不算告警
# -----------------------------------------------------------------------------
function Test-YkEnable {
    if (-not (Test-Path $Script:YkConfigPath)) {
        return [pscustomobject]@{
            ok              = $false
            login_enabled   = $null
            upload_enabled  = $null
            raw_line        = "config not found: $($Script:YkConfigPath)"
            reason          = 'yk config file missing'
        }
    }
    $lines = Get-Content -Path $Script:YkConfigPath -Encoding UTF8
    $inYkBlock = $false
    $loginEnabled = $null
    $uploadEnabled = $null
    $loginRaw = ''
    $uploadRaw = ''

    foreach ($ln in $lines) {
        $trim = $ln.Trim()
        # 进入 yk: 顶层段（必须是 yk 单独成行；hik-security/oauth/... 都不算）
        if ($trim -match '^yk\s*:\s*$') { $inYkBlock = $true; continue }
        if ($inYkBlock) {
            # 离开 yk: 段：遇到 yk 段以外的下一个顶层 key（缩进 < 2 空格 + 以字母开头 + 冒号）
            # yk: 下字段缩进是 2 空格（如 `  loginEnabled:`）；hik-security 是 0 空格 + 顶层
            if (-not $ln.StartsWith('  ') -and $trim -match '^[A-Za-z_][A-Za-z0-9_-]*\s*:') { break }
            # 跳过纯注释行
            if ($trim.StartsWith('#')) { continue }
            # 去掉尾部 `# 注释`
            $valuePart = $trim -replace '\s*#.*$', ''
            # 双开关解析（允许尾部注释和多余空格）
            if ($valuePart -match '^loginEnabled\s*:\s*(true|false)\s*$') {
                $loginEnabled = ($Matches[1] -eq 'true')
                $loginRaw = $trim
                continue
            }
            if ($valuePart -match '^uploadEnabled\s*:\s*(true|false)\s*$') {
                $uploadEnabled = ($Matches[1] -eq 'true')
                $uploadRaw = $trim
                continue
            }
            # 兼容旧 yk.enable: false（DEPRECATED 注释行会被注释掉，跳过）
            if ($valuePart -match '^enable\s*:') { continue }
        }
    }

    # 必须解析出 uploadEnabled（缺字段即 ALERT）
    if ($null -eq $uploadEnabled) {
        return [pscustomobject]@{
            ok              = $false
            login_enabled   = $loginEnabled
            upload_enabled  = $null
            raw_line        = ($loginRaw + ' | ' + $uploadRaw)
            reason          = 'yk.uploadEnabled field not found in config (need loginEnabled + uploadEnabled)'
        }
    }
    # uploadEnabled=true 即 ALERT（铁则 42：灰盒期 uploadEnabled 必须 false）
    if ($uploadEnabled) {
        return [pscustomobject]@{
            ok              = $false
            login_enabled   = $loginEnabled
            upload_enabled  = $uploadEnabled
            raw_line        = $uploadRaw
            reason          = 'yk.uploadEnabled=true (forbidden by rule 36/42 in graybox)'
        }
    }
    return [pscustomobject]@{
        ok              = $true
        login_enabled   = $loginEnabled
        upload_enabled  = $uploadEnabled
        raw_line        = $uploadRaw
        reason          = 'ok'
    }
}

# -----------------------------------------------------------------------------
# 信号 1：yk 调用日志（W-X14 适配）
#   1min 内 log 中是否出现 `success to get ticket from yk` 或 `updateTicket:8[0-9] success`
#   当 yk.uploadEnabled=false 且 loginEnabled=true 时，ticket success 是合法预热 → OK
#   当 uploadEnabled=true 时，ticket success 也意味着真推路径被允许 → ALERT（叠加信号 3）
#   当 loginEnabled=false 时，ticket success 不应出现 → ALERT
# -----------------------------------------------------------------------------
function Test-YkTicketCall {
    if (-not (Test-Path $Script:LogFile)) {
        return [pscustomobject]@{
            ok          = $false
            count_1m    = -1
            samples     = @()
            log_file    = $Script:LogFile
            reason      = 'log file missing'
        }
    }
    $cutoff = (Get-Date).AddMinutes(-1)
    $hits = @()
    try {
        # FileShare.ReadWrite 共享读（hik-java 正在写 DataupLoad.log）
        $fs = [System.IO.File]::Open($Script:LogFile, 'Open', 'Read', 'ReadWrite')
        $reader = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
        try {
            while ($null -ne ($line = $reader.ReadLine())) {
                # 提取时间戳（DataupLoad.log 格式 `2026-07-23 14:23:42.933`）
                if ($line -match '^(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})') {
                    $tsStr = $Matches[1]
                    $ts = [DateTime]::ParseExact($tsStr, 'yyyy-MM-dd HH:mm:ss', $null)
                    if ($ts -lt $cutoff) { continue }
                }
                if ($line -match 'success to get ticket from yk' -or $line -match 'updateTicket:8[0-9]\s+success') {
                    $hits += [pscustomobject]@{
                        line = $line.Substring(0, [Math]::Min(200, $line.Length))
                    }
                }
            }
        } finally { $reader.Close(); $fs.Close() }
    } catch {
        return [pscustomobject]@{
            ok       = $false
            count_1m = -1
            samples  = @()
            log_file = $Script:LogFile
            reason   = "log read error: $($_.Exception.Message)"
        }
    }
    $count = $hits.Count
    return [pscustomobject]@{
        ok       = $true  # 该信号在双开关下不再单独判 ok/false；状态由 信号 1 配套语义在 Invoke-Monitor 里聚合
        count_1m = $count
        samples  = ($hits | Select-Object -First 3)
        log_file = $Script:LogFile
        reason   = if ($count -gt 0) { "yk ticket call detected ($count in 1m)" } else { 'no ticket call in 1m' }
    }
}

# -----------------------------------------------------------------------------
# 信号 2：PG alarm_record 增长（W-X14 沿用）
#   5min 内 alarm_record 涨速 > $Script:AlarmGrowthPerMin 行/min 报警
# -----------------------------------------------------------------------------
function Test-PgAlarmGrowth {
    $now = Get-Date
    if (-not (Test-Path $Script:StateFile)) {
        $state = [pscustomobject]@{ samples = @() }
    } else {
        try {
            $state = Get-Content -Path $Script:StateFile -Raw -Encoding UTF8 | ConvertFrom-Json
        } catch {
            $state = [pscustomobject]@{ samples = @() }
        }
    }
    $nowCount = $null
    $ok = $true
    $reason = 'ok'
    $growthPerMin = 0
    $baseline = $null
    $baselineAgeMin = 0

    $q = "SELECT COUNT(*) FROM public.alarm_record;"
    $r = Invoke-PgScalarInt -Query $q
    if (-not $r.ok) {
        # PG 硬失败/软失败都不直接 ALERT（与 W-X12b 一致：软失败→WARN；硬失败→ALERT）
        $hardFailPatterns = @('could not connect', 'connection refused', 'timeout expired',
                              'no route to host', 'server closed the connection unexpectedly',
                              'Is the server running')
        $isHard = $false
        foreach ($p in $hardFailPatterns) { if ($r.reason -match [Regex]::Escape($p)) { $isHard = $true; break } }
        if ($isHard) {
            $ok = $false
            $reason = "PG unreachable: $($r.reason)"
        } else {
            $ok = $true
            $reason = "PG degraded: $($r.reason)"
        }
    } else {
        $nowCount = $r.value
        # 找 4-6 min 容忍窗的基线样本
        if ($state -and $state.samples) {
            $candidates = @($state.samples | Where-Object {
                $_.ts -and [DateTime]::TryParse($_.ts, [ref]([DateTime]::MinValue)) -and
                (((Get-Date) - [DateTime]::Parse($_.ts)).TotalMinutes -ge 4) -and
                (((Get-Date) - [DateTime]::Parse($_.ts)).TotalMinutes -le 6)
            })
            if ($candidates.Count -gt 0) {
                $best = $candidates | Sort-Object { [DateTime]::Parse($_.ts) } | Select-Object -Last 1
                $baseline = [int]$best.count
                $baselineAgeMin = [Math]::Round(((Get-Date) - [DateTime]::Parse($best.ts)).TotalMinutes, 2)
                $growthPerMin = [Math]::Round(($nowCount - $baseline) / [Math]::Max($baselineAgeMin, 0.1), 2)
                if ($growthPerMin -gt $Script:AlarmGrowthPerMin) {
                    $ok = $false
                    $reason = "alarm_record growth too fast: $growthPerMin rows/min (threshold $($Script:AlarmGrowthPerMin))"
                }
            }
        }
    }

    # 写回 state（保留 30 min）
    $samples = @()
    if ($state -and $state.samples) { $samples += @($state.samples) }
    if ($null -ne $nowCount) {
        $samples += [pscustomobject]@{ ts = $now.ToString("yyyy-MM-ddTHH:mm:ss"); count = $nowCount }
    }
    $cutoff = (Get-Date).AddMinutes(-30)
    $samples = @($samples | Where-Object {
        [DateTime]::TryParse($_.ts, [ref]([DateTime]::MinValue)) -and
        ([DateTime]::Parse($_.ts) -ge $cutoff)
    })
    try {
        ($samples | ConvertTo-Json -Compress -Depth 3) | Set-Content -Path $Script:StateFile -Encoding UTF8
    } catch { }

    return [pscustomobject]@{
        ok              = $ok
        current_count   = $nowCount
        baseline_count  = $baseline
        baseline_age_min= $baselineAgeMin
        growth_per_min  = $growthPerMin
        threshold       = $Script:AlarmGrowthPerMin
        reason          = $reason
    }
}

# -----------------------------------------------------------------------------
# 信号 4：ticket 续期监控（W-X18 新增）
#   当 yk.loginEnabled=true 时（灰盒默认），最近 50min 内 DataupLoad.log
#   至少应出现 1 次 `success to get ticket from yk`；缺则 ALERT（ticket 续期死了）
#   当 loginEnabled=false 时不检查
# -----------------------------------------------------------------------------
function Test-TicketRenewal {
    param(
        [Parameter(Mandatory)][bool]$LoginEnabled
    )
    if (-not $LoginEnabled) {
        return [pscustomobject]@{
            ok       = $true
            count    = -1
            last_ts  = $null
            reason   = 'loginEnabled=false, skip check'
        }
    }
    if (-not (Test-Path $Script:LogFile)) {
        return [pscustomobject]@{
            ok       = $false
            count    = -1
            last_ts  = $null
            reason   = 'log file missing'
        }
    }
    $cutoff = (Get-Date).AddMinutes(-1 * $Script:TicketRenewMin)
    $count = 0
    $lastTs = $null
    try {
        $fs = [System.IO.File]::Open($Script:LogFile, 'Open', 'Read', 'ReadWrite')
        $reader = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
        try {
            while ($null -ne ($line = $reader.ReadLine())) {
                if ($line -match 'success to get ticket from yk') {
                    if ($line -match '^(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})') {
                        $ts = [DateTime]::ParseExact($Matches[1], 'yyyy-MM-dd HH:mm:ss', $null)
                        if ($ts -ge $cutoff) {
                            $count += 1
                            if ($null -eq $lastTs -or $ts -gt $lastTs) { $lastTs = $ts }
                        }
                    } else {
                        # 无时间戳：保守计 1
                        $count += 1
                    }
                }
            }
        } finally { $reader.Close(); $fs.Close() }
    } catch {
        return [pscustomobject]@{
            ok       = $false
            count    = -1
            last_ts  = $null
            reason   = "log read error: $($_.Exception.Message)"
        }
    }

    if ($count -gt 0) {
        return [pscustomobject]@{
            ok       = $true
            count    = $count
            last_ts  = $lastTs.ToString("yyyy-MM-dd HH:mm:ss")
            reason   = "ok ($count ticket renewals in last $($Script:TicketRenewMin)m)"
        }
    }
    return [pscustomobject]@{
        ok       = $false
        count    = 0
        last_ts  = $null
        reason   = "no ticket renewal in last $($Script:TicketRenewMin)m (loginEnabled=true, expected ≥1)"
    }
}

# -----------------------------------------------------------------------------
# 信号 5：alarm_record 入库数对比（W-X18 新增）
#   5min 内 PG count 增量 vs DataupLoad.log 中 `receive alarm` 行数增量
#   差 > 5%（相对 log 接收数）→ ALERT
#   （说明：receive alarm 是 HTTP 入口打点；alarm_record 是去重/ignore/同化后的落库数，
#    灰盒正常情况下差应远小于 5%；> 5% 意味着去重逻辑或入库管道异常）
# -----------------------------------------------------------------------------
function Test-AlarmRecordCompare {
    if (-not (Test-Path $Script:LogFile)) {
        return [pscustomobject]@{
            ok           = $false
            pg_count     = $null
            log_count    = -1
            delta_pct    = $null
            reason       = 'log file missing'
        }
    }
    $now = Get-Date
    $cutoff = $now.AddMinutes(-1 * $Script:AlarmLogWindowMin)

    # 1) PG alarm_record 增量（5min 内）
    $pgRes = Invoke-PgScalarInt -Query "SELECT COUNT(*) FROM public.alarm_record WHERE create_time >= NOW() - INTERVAL '$($Script:AlarmLogWindowMin) minutes';"
    $pgCount = if ($pgRes.ok) { $pgRes.value } else { $null }

    # 2) log 内 receive alarm 行数（5min 内）
    $logCount = 0
    try {
        $fs = [System.IO.File]::Open($Script:LogFile, 'Open', 'Read', 'ReadWrite')
        $reader = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
        try {
            while ($null -ne ($line = $reader.ReadLine())) {
                if ($line -match 'AlarmRecordController\.addAlarmData:36\]\s+receive alarm') {
                    if ($line -match '^(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})') {
                        $ts = [DateTime]::ParseExact($Matches[1], 'yyyy-MM-dd HH:mm:ss', $null)
                        if ($ts -ge $cutoff) { $logCount += 1 }
                    } else {
                        $logCount += 1
                    }
                }
            }
        } finally { $reader.Close(); $fs.Close() }
    } catch {
        return [pscustomobject]@{
            ok        = $false
            pg_count  = $pgCount
            log_count = -1
            delta_pct = $null
            reason    = "log read error: $($_.Exception.Message)"
        }
    }

    # 3) 差值 = |pg - log| / log
    # logCount = 0 且 pgCount = 0 → OK（无业务）
    # logCount = 0 且 pgCount > 0 → ALERT（log 无 receive 但 PG 有入库，可能代码路径绕过 log）
    # logCount > 0 → 差 > 5% ALERT
    if ($logCount -eq 0 -and $null -eq $pgCount) {
        return [pscustomobject]@{
            ok        = $true
            pg_count  = $null
            log_count = 0
            delta_pct = $null
            reason    = "PG degraded ($($pgRes.reason)) + log=0"
        }
    }
    if ($logCount -eq 0 -and $pgCount -eq 0) {
        return [pscustomobject]@{
            ok        = $true
            pg_count  = 0
            log_count = 0
            delta_pct = 0
            reason    = "no alarm traffic in last $($Script:AlarmLogWindowMin)m"
        }
    }
    if ($logCount -eq 0 -and $pgCount -gt 0) {
        return [pscustomobject]@{
            ok        = $false
            pg_count  = $pgCount
            log_count = 0
            delta_pct = $null
            reason    = "PG has $pgCount inserts in $($Script:AlarmLogWindowMin)m but log shows 0 receive alarm (bypass detection?)"
        }
    }

    $delta = [Math]::Abs(($pgCount - $logCount))
    $deltaPct = [Math]::Round($delta / $logCount, 4)
    $deltaPctStr = ('{0:P2}' -f $deltaPct)
    if ($deltaPct -gt $Script:AlarmDeltaPct) {
        return [pscustomobject]@{
            ok        = $false
            pg_count  = $pgCount
            log_count = $logCount
            delta_pct = $deltaPct
            reason    = "alarm_record delta $deltaPctStr > {0:P0} (pg=$pgCount log=$logCount in $($Script:AlarmLogWindowMin)m)" -f $Script:AlarmDeltaPct
        }
    }
    return [pscustomobject]@{
        ok        = $true
        pg_count  = $pgCount
        log_count = $logCount
        delta_pct = $deltaPct
        reason    = "ok (delta=$deltaPct, pg=$pgCount log=$logCount)"
    }
}

# -----------------------------------------------------------------------------
# 信号 6：ignore_alarm 表变更监控（W-X18 新增）
#   每 1min 一次 count，变化即 ALERT（白名单被改）
#   state 文件记录上次 count + ts；本次比较
# -----------------------------------------------------------------------------
function Test-IgnoreAlarmChange {
    $ignoreStateFile = Join-Path $env:TEMP 'monitor-yk-push-ignore-state.json'
    $res = Invoke-PgScalarInt -Query "SELECT COUNT(*) FROM public.ignore_alarm;"
    if (-not $res.ok) {
        return [pscustomobject]@{
            ok        = $true
            count     = $null
            prev      = $null
            reason    = "PG degraded: $($res.reason)"
        }
    }
    $cur = $res.value
    $prev = $null
    if (Test-Path $ignoreStateFile) {
        try {
            $st = Get-Content -Path $ignoreStateFile -Raw -Encoding UTF8 | ConvertFrom-Json
            $prev = [int]$st.count
        } catch { }
    }
    # 写回
    try {
        (@{ count = $cur; ts = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss") } | ConvertTo-Json -Compress) |
            Set-Content -Path $ignoreStateFile -Encoding UTF8
    } catch { }

    if ($null -ne $prev -and $cur -ne $prev) {
        return [pscustomobject]@{
            ok       = $false
            count    = $cur
            prev     = $prev
            reason   = "ignore_alarm count changed: $prev -> $cur (whitelist modified!)"
        }
    }
    return [pscustomobject]@{
        ok       = $true
        count    = $cur
        prev     = $prev
        reason   = if ($null -eq $prev) { "baseline set: $cur" } else { "ok (count=$cur, stable)" }
    }
}

# -----------------------------------------------------------------------------
# 信号 7：hik-java CPU user 模式异常告警（W-X18 新增）
#   10min 滚动窗口内 hik-java.exe 累计 UserModeTime 增量 > 200s → ALERT
#   state 文件记录上次采样的 (ts, userTime100ns)
#   注：wmic UserModeTime 单位是 100ns
# -----------------------------------------------------------------------------
function Test-HikJavaCpu {
    $cpuStateFile = Join-Path $env:TEMP 'monitor-yk-push-cpu-state.json'
    $proc = Get-CimInstance Win32_Process -Filter "Name='hik-java.exe'" -ErrorAction SilentlyContinue
    if (-not $proc) {
        return [pscustomobject]@{
            ok        = $false
            pid       = $null
            delta_sec = $null
            window_sec= $null
            reason    = 'hik-java.exe not running'
        }
    }
    $pidVal = [int]$proc.ProcessId
    $user100ns = [int64]$proc.UserModeTime
    $now = Get-Date

    $prevTs = $null
    $prevUser = $null
    if (Test-Path $cpuStateFile) {
        try {
            $st = Get-Content -Path $cpuStateFile -Raw -Encoding UTF8 | ConvertFrom-Json
            $prevTs = [DateTime]::Parse($st.ts)
            $prevUser = [int64]$st.user100ns
        } catch { }
    }

    # 写回本次
    try {
        (@{ ts = $now.ToString("yyyy-MM-ddTHH:mm:ss"); user100ns = $user100ns } | ConvertTo-Json -Compress) |
            Set-Content -Path $cpuStateFile -Encoding UTF8
    } catch { }

    if ($null -eq $prevTs -or $null -eq $prevUser) {
        return [pscustomobject]@{
            ok        = $true
            pid       = $pidVal
            delta_sec = $null
            window_sec= $null
            reason    = 'baseline set'
        }
    }

    $windowSec = ($now - $prevTs).TotalSeconds
    # 仅评估窗口在 1-15min 之间（太短/太长数据失真，跳过本次）
    if ($windowSec -lt 30 -or $windowSec -gt 900) {
        return [pscustomobject]@{
            ok        = $true
            pid       = $pidVal
            delta_sec = $null
            window_sec= [Math]::Round($windowSec, 1)
            reason    = "window out of range ($([Math]::Round($windowSec,1))s), skip"
        }
    }
    $delta100ns = $user100ns - $prevUser
    if ($delta100ns -lt 0) { $delta100ns = 0 }  # 重启后 userTime 重置
    $deltaSec = [Math]::Round($delta100ns / 10000000.0, 1)
    if ($deltaSec -gt $Script:CpuUserThresholdSec) {
        return [pscustomobject]@{
            ok        = $false
            pid       = $pidVal
            delta_sec = $deltaSec
            window_sec= [Math]::Round($windowSec, 1)
            reason    = "hik-java CPU user=$deltaSec s in $([Math]::Round($windowSec,1))s window > $($Script:CpuUserThresholdSec)s"
        }
    }
    return [pscustomobject]@{
        ok        = $true
        pid       = $pidVal
        delta_sec = $deltaSec
        window_sec= [Math]::Round($windowSec, 1)
        reason    = "ok (cpu_user=$deltaSec s in $([Math]::Round($windowSec,1))s)"
    }
}

# -----------------------------------------------------------------------------
# 前置条件：hik-java 进程 + 80 端口（铁则 38 沿用）
#   不在 7 个 ALERT 计数里；不满足则进程级直接报硬失败
# -----------------------------------------------------------------------------
function Test-HikJavaAlive {
    $proc = Get-CimInstance Win32_Process -Filter "Name='hik-java.exe'" -ErrorAction SilentlyContinue
    if (-not $proc) {
        return [pscustomobject]@{ ok = $false; pid = $null; port_80_listen = $false; reason = 'hik-java.exe not running' }
    }
    $pidVal = [int]$proc.ProcessId
    $listen = $false
    $listeners = @(Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue |
                   Where-Object { [int]$_.OwningProcess -eq $pidVal })
    if ($listeners.Count -gt 0) { $listen = $true }
    $ok = $listen
    return [pscustomobject]@{
        ok            = $ok
        pid           = $pidVal
        port_80_listen= $listen
        reason        = if ($ok) { 'ok' } else { "hik-java pid=$pidVal running but port 80 not LISTEN" }
    }
}

# -----------------------------------------------------------------------------
# Invoke-Monitor — 调 7 信号 + 汇总单行格式
# -----------------------------------------------------------------------------
function Invoke-Monitor {
    $ykEnable  = Test-YkEnable
    $hikAlive  = Test-HikJavaAlive

    # 前置失败：进程级硬错，但仍尽量跑（状态以"前置失败"标记）
    $precheckFailed = -not $hikAlive.ok

    $ykTicket  = Test-YkTicketCall
    $pgAlarm   = Test-PgAlarmGrowth
    $ticketRenewal = Test-TicketRenewal -LoginEnabled ([bool]$ykEnable.login_enabled)
    $alarmCmp  = Test-AlarmRecordCompare
    $ignoreChg = Test-IgnoreAlarmChange
    $cpuUser   = Test-HikJavaCpu

    # === 7 个 status=ALERT 触发条件 ===
    # 信号 1：yk 调用日志（双开关语义）
    #   当 uploadEnabled=true 时 ticket success = ALERT（信号 1 失败）
    #   当 loginEnabled=false 时 ticket success 也算异常（不应出现）
    $sig1_ok = $true
    $sig1_reason = 'ok'
    if ($ykEnable.upload_enabled -eq $true) {
        $sig1_ok = $false
        $sig1_reason = "yk.uploadEnabled=true AND ticket_call=$($ykTicket.count_1m)/1m"
    } elseif (-not $ykEnable.login_enabled -and $ykTicket.count_1m -gt 0) {
        $sig1_ok = $false
        $sig1_reason = "yk.loginEnabled=false but ticket_call=$($ykTicket.count_1m)/1m (unexpected)"
    } else {
        $sig1_reason = "ticket_call=$($ykTicket.count_1m)/1m, loginEnabled=$($ykEnable.login_enabled), uploadEnabled=$($ykEnable.upload_enabled)"
    }

    # 信号 2：PG alarm_record 增长
    $sig2_ok = $pgAlarm.ok
    $sig2_reason = "growth=$($pgAlarm.growth_per_min)/min, current=$($pgAlarm.current_count), baseline=$($pgAlarm.baseline_count)"

    # 信号 3：yk 配置（uploadEnabled 必须 false）
    $sig3_ok = $ykEnable.ok
    $sig3_reason = if (-not $sig3_ok) { $ykEnable.reason } else { "uploadEnabled=false, loginEnabled=$($ykEnable.login_enabled)" }

    # 信号 4：ticket 续期
    $sig4_ok = $ticketRenewal.ok
    $sig4_reason = $ticketRenewal.reason

    # 信号 5：alarm_record 对比
    $sig5_ok = $alarmCmp.ok
    $sig5_reason = $alarmCmp.reason

    # 信号 6：ignore_alarm 变更
    $sig6_ok = $ignoreChg.ok
    $sig6_reason = $ignoreChg.reason

    # 信号 7：hik-java CPU user
    $sig7_ok = $cpuUser.ok
    $sig7_reason = $cpuUser.reason

    # 汇总
    $signals = @(
        [pscustomobject]@{ n = 1; ok = $sig1_ok; r = $sig1_reason },
        [pscustomobject]@{ n = 2; ok = $sig2_ok; r = $sig2_reason },
        [pscustomobject]@{ n = 3; ok = $sig3_ok; r = $sig3_reason },
        [pscustomobject]@{ n = 4; ok = $sig4_ok; r = $sig4_reason },
        [pscustomobject]@{ n = 5; ok = $sig5_ok; r = $sig5_reason },
        [pscustomobject]@{ n = 6; ok = $sig6_ok; r = $sig6_reason },
        [pscustomobject]@{ n = 7; ok = $sig7_ok; r = $sig7_reason }
    )

    $alertReasons = @()
    foreach ($s in $signals) {
        if (-not $s.ok) { $alertReasons += "sig$($s.n): $($s.r)" }
    }
    if ($precheckFailed) { $alertReasons += "precheck: $($hikAlive.reason)" }

    $status = if ($alertReasons.Count -gt 0) { 'ALERT' } else { 'OK' }

    # 单行格式
    $ts = (Get-Date).ToUniversalTime().AddHours(8).ToString("yyyy-MM-dd HH:mm:ss")
    $parts = @()
    $parts += "signal1=$($ykTicket.count_1m)"
    $parts += "signal2=$($pgAlarm.growth_per_min)/min"
    $parts += "signal3=login=$($ykEnable.login_enabled)/upload=$($ykEnable.upload_enabled)"
    $parts += "signal4=$($ticketRenewal.count)"
    $pgStr = if ($null -eq $alarmCmp.pg_count) { 'N/A' } else { [string]$alarmCmp.pg_count }
    $logStr = if ($null -eq $alarmCmp.log_count) { 'N/A' } else { [string]$alarmCmp.log_count }
    $parts += "signal5=pg$pgStr/log$logStr"
    $ignoreStr = if ($null -eq $ignoreChg.count) { 'N/A' } else { [string]$ignoreChg.count }
    $parts += "signal6=$ignoreStr"
    $cpuStr = if ($null -eq $cpuUser.delta_sec) { 'N/A' } else { ([string]$cpuUser.delta_sec + 's') }
    $parts += "signal7=$cpuStr"
    $parts += "pid=$($hikAlive.pid)"
    $parts += "p80=$($hikAlive.port_80_listen)"

    if ($status -eq 'ALERT') {
        $line = "[$ts] status=ALERT | why=$($alertReasons -join ' | ') | $($parts -join ' | ')"
    } else {
        $line = "[$ts] status=OK | $($parts -join ' | ')"
    }
    return [pscustomobject]@{
        line    = $line
        status  = $status
        reasons = $alertReasons
    }
}

# -----------------------------------------------------------------------------
# 入口
# -----------------------------------------------------------------------------
try {
    $r = Invoke-Monitor
    # 写日志（追加，UTF-8 无 BOM）
    $logDir = Split-Path -Parent $Script:MonitorLogPath
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
    # Add-Content + UTF8 编码在 PS 5.1 上会带 BOM，改用 .NET 写
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::AppendAllText($Script:MonitorLogPath, ($r.line + "`r`n"), $utf8NoBom)
    # 屏幕同步输出（方便 tail）
    Write-Output $r.line
} catch {
    # 兜底：脚本本身崩了也要把异常写日志
    $ts = (Get-Date).ToUniversalTime().AddHours(8).ToString("yyyy-MM-dd HH:mm:ss")
    $errLine = "[$ts] status=ALERT | why=monitor exception: $($_.Exception.Message) | signal1=ERR | signal2=ERR | signal3=ERR | signal4=ERR | signal5=ERR | signal6=ERR | signal7=ERR | pid=? | p80=?"
    try {
        $logDir = Split-Path -Parent $Script:MonitorLogPath
        if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::AppendAllText($Script:MonitorLogPath, ($errLine + "`r`n"), $utf8NoBom)
    } catch { }
    Write-Output $errLine
    exit 1
}
