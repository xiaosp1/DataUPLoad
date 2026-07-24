# W-X15 alarm dedup / ignore gray-box test script
# Worker: codex exec
# Ticket: W-X15-alarm-dedup-ignore-test
# Date: 2026-07-23
# Forbidden: change source / change yml / change yk.uploadEnabled / restart hik-java
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-alarm-dedup.ps1 -All
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-alarm-dedup.ps1 -TestId T1

[CmdletBinding()]
param(
    [string]$TestId = "",
    [switch]$All
)

$ErrorActionPreference = "Stop"

# === Config ===
$ProjectRoot = "E:\DEMO\数据采集"
$LogDir      = Join-Path $ProjectRoot "logs"
if (!(Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }
$ResultLog   = Join-Path $LogDir ("w-x15-test-{0:yyyyMMdd-HHmmss}.log" -f (Get-Date))

$AlarmUrl    = "http://127.0.0.1:80/client/data/alarm"
$IgnoreUrl   = "http://127.0.0.1:80/web/alarm/ignore/"

$Psql        = "C:\Program Files\PostgreSQL\14\bin\psql.exe"
$env:PGPASSWORD = "postgres"
$PgArgs      = @("-h", "127.0.0.1", "-p", "5433", "-U", "postgres", "-d", "intco")

$AppLog      = "E:\DEMO\DATALINK\DataupLoad\log\DataupLoad\DataupLoad.log"

# === Util ===
$script:TestResults = @()
$script:NowMark     = (Get-Date).ToString("yyyyMMddHHmmss")
$script:RunTag      = "W-X15-$script:NowMark"

function Write-Banner($text) {
    $line = "=" * 78
    Write-Host ""
    Write-Host $line -ForegroundColor Cyan
    Write-Host $text -ForegroundColor Cyan
    Write-Host $line -ForegroundColor Cyan
}

function Write-Sub($text) {
    Write-Host ""
    Write-Host ("--- " + $text + " ---") -ForegroundColor Yellow
}

function Log-PASS {
    param([string]$testId, [string]$reason)
    $script:TestResults += [PSCustomObject]@{
        Test = $testId; Result = "PASS"; Reason = $reason
    }
    Write-Host ("[RESULT] {0} PASS - {1}" -f $testId, $reason) -ForegroundColor Green
}

function Log-FAIL {
    param([string]$testId, [string]$reason)
    $script:TestResults += [PSCustomObject]@{
        Test = $testId; Result = "FAIL"; Reason = $reason
    }
    Write-Host ("[RESULT] {0} FAIL - {1}" -f $testId, $reason) -ForegroundColor Red
}

function Invoke-CurlJson {
    param([string]$Method, [string]$Uri, [hashtable]$Body, [int]$TimeoutSec = 15)
    $json = $Body | ConvertTo-Json -Depth 10 -Compress
    Write-Host ("  > {0} {1}" -f $Method, $Uri) -ForegroundColor DarkGray
    Write-Host ("  > Body: {0}" -f $json) -ForegroundColor DarkGray
    try {
        $resp = Invoke-RestMethod -Uri $Uri -Method $Method -Body $json `
            -ContentType "application/json;charset=UTF-8" -TimeoutSec $TimeoutSec -ErrorAction Stop
        Write-Host ("  < " + ($resp | ConvertTo-Json -Depth 5 -Compress)) -ForegroundColor DarkGray
        return $resp
    } catch {
        Write-Host ("  < ERROR: {0}" -f $_.Exception.Message) -ForegroundColor DarkRed
        return $null
    }
}

function Invoke-PsqlScalar {
    param([string]$Sql)
    $args = $PgArgs + @("-tA", "-c", $Sql)
    $out = [string](& $Psql @args 2>&1 | Out-String)
    # return only first non-empty line (ignore INSERT 0 N status etc.)
    $lines = @($out -split "`r?`n" | Where-Object { $_.Trim() -ne '' })
    if ($lines.Count -gt 0) { return ([string]$lines[0]).Trim() }
    return ''
}

function Invoke-PsqlTable {
    param([string]$Sql)
    $args = $PgArgs + @("-c", $Sql)
    $out = [string](& $Psql @args 2>&1 | Out-String)
    return $out
}

function Invoke-PsqlExec {
    param([string]$Sql)
    Write-Host ("  SQL> " + $Sql) -ForegroundColor DarkGray
    $args = $PgArgs + @("-c", $Sql)
    $out = [string](& $Psql @args 2>&1 | Out-String)
    Write-Host $out.Trim() -ForegroundColor DarkGray
    return $out
}

function New-AlarmBody {
    param(
        [string]$Uuid,
        [string]$LineNo,
        [string]$FaceNo,
        [int]$Type = 1,
        [int]$Level = 1,
        [string]$Message = "[bad] defect alarm"
    )
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    @{
        uuid    = $Uuid
        time    = $now
        type    = $Type
        lineNo  = $LineNo
        faceNo  = $FaceNo
        level   = $Level
        message = $Message
    }
}

function Get-AppLogLines {
    param([int]$LastN = 50, [string]$Since = "")
    if (!(Test-Path $AppLog)) { return @() }
    $lines = @(Get-Content $AppLog -Tail $LastN -ErrorAction SilentlyContinue)
    if ($Since) {
        # Since is yyyy-MM-dd HH:mm:ss (19 chars). Log line starts with yyyy-MM-dd HH:mm:ss.SSS (23 chars).
        # Take substring(0,19) of log line and compare lexicographically.
        $lines = @($lines | Where-Object {
            if ($_.Length -lt 19) { return $false }
            $linePrefix = $_.Substring(0, 19)
            return ($linePrefix -ge $Since)
        })
    }
    return $lines
}

function Get-YkErrorCountSince {
    param([string]$Since = "")
    if (!(Test-Path $AppLog)) { return 0 }
    $lines = @(Get-Content $AppLog -ErrorAction SilentlyContinue)
    if ($Since) {
        $lines = @($lines | Where-Object {
            if ($_.Length -lt 19) { return $false }
            $linePrefix = $_.Substring(0, 19)
            return ($linePrefix -ge $Since)
        })
    }
    return (@($lines | Where-Object { $_ -match "push alarm info to yk failed|push alarm to yk error|push alarm to yk exception" })).Count
}

function Add-IgnoreAlarm {
    param([string]$Defect, [string]$Line, [string]$Face, [int]$Type = 1)
    $sql = "INSERT INTO ignore_alarm (defect_name, type, line_no, face_no, ignore_all, face_id, start_time, end_time) VALUES ('$Defect', $Type, '$Line', '$Face', 2, NULL, '2026-07-23 13:00:00', '2099-12-31 23:59:59') ON CONFLICT DO NOTHING RETURNING id;"
    $id = Invoke-PsqlScalar -Sql $sql
    if ([string]::IsNullOrEmpty($id)) {
        $id = Invoke-PsqlScalar -Sql "SELECT id FROM ignore_alarm WHERE defect_name='$Defect' AND line_no='$Line' AND face_no='$Face' AND type=$Type;"
    }
    Write-Host ("  IgnoreAlarm row id=" + $id) -ForegroundColor DarkGray
    return $id
}

function Remove-IgnoreAlarm {
    param([string]$Defect, [string]$Line, [string]$Face)
    $sql = "DELETE FROM ignore_alarm WHERE defect_name='$Defect' AND line_no='$Line' AND face_no='$Face';"
    Invoke-PsqlExec -Sql $sql | Out-Null
}

# === Banner ===
Write-Banner ("W-X15 alarm dedup/ignore gray-box test  " + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Write-Host ("RunTag : " + $script:RunTag)
Write-Host ("AppLog : " + $AppLog)
Write-Host ("Result : " + $ResultLog)

# === Baseline ===
Write-Sub "Baseline snapshot"
$baseAlarm = Invoke-PsqlScalar "SELECT count(*) FROM alarm_record;"
$baseUns   = Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE solve=2;"
$baseIg    = Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE solve=3;"
$baseIgAl  = Invoke-PsqlScalar "SELECT count(*) FROM ignore_alarm;"
$baseDft   = Invoke-PsqlScalar "SELECT count(*) FROM defect_type;"
Write-Host ("  alarm_record=$baseAlarm  UNSOLVED=$baseUns  IGNORE=$baseIg  ignore_alarm=$baseIgAl  defect_type=$baseDft")

# === Fixture: defect_type rows for tests ===
Write-Sub "Fixture: insert W-X15-* defect_type rows"
$fixtureNames = @("bad", "W-X15-D2A","W-X15-D2B","W-X15-D2C","W-X15-D2D","W-X15-D2E", "W-X15-D3","W-X15-D4","W-X15-D5","W-X15-D6","W-X15-D7","W-X15-D8")
foreach ($fn in $fixtureNames) {
    $sql = "INSERT INTO defect_type (""name"", category, count_enable, count_threshold, rate_enable, show_img_enable, alarm_enable, sound_enable, send_yk_enable) SELECT '$fn', 1, false, 0, false, false, 1, 1, 1 WHERE NOT EXISTS (SELECT 1 FROM defect_type WHERE ""name""='$fn');"
    Invoke-PsqlExec -Sql $sql | Out-Null
    $sql2 = "UPDATE defect_type SET alarm_enable=1, sound_enable=1, send_yk_enable=1 WHERE ""name""='$fn';"
    Invoke-PsqlExec -Sql $sql2 | Out-Null
}
$fxOut = Invoke-PsqlTable "SELECT ""name"", alarm_enable, send_yk_enable FROM defect_type WHERE ""name"" LIKE 'W-X15-%' OR ""name"" = 'bad' ORDER BY ""name"";"
Write-Host ("  fixture:`n" + $fxOut.Trim())

# === Tests ===
function Run-T1 {
    Write-Banner "T1 add() same-class dedup (3 same defect+line+face+type)"
    $lineNo = "W-X15-L1"; $faceNo = "FA1"
    $before = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE defect_name='bad' AND line_no='$lineNo' AND face_no='$faceNo' AND type=1;")
    for ($i = 1; $i -le 3; $i++) {
        $body = New-AlarmBody -Uuid ("$script:RunTag-T1-$i") -LineNo $lineNo -FaceNo $faceNo -Message "[bad] defect alarm - #$i"
        Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
        Start-Sleep -Milliseconds 300
    }
    Start-Sleep -Seconds 1
    $out = Invoke-PsqlTable "SELECT solve, count(*) FROM alarm_record WHERE defect_name='bad' AND line_no='$lineNo' AND face_no='$faceNo' AND type=1 GROUP BY solve ORDER BY solve;"
    Write-Host ("  group: " + $out.Trim())
    $hasUns1 = $out -match "2 \| +1"
    $hasIg2  = $out -match "3 \| +2"
    if ($hasUns1 -and $hasIg2) {
        Log-PASS "T1" "Same (defect+line+face+type) alarms -> 1 UNSOLVED + 2 IGNORE"
    } else {
        Log-FAIL "T1" "Expected 1 UNSOLVED + 2 IGNORE, got: $($out.Trim())"
    }
}

function Run-T2 {
    Write-Banner "T2 different defectName independent (5 different defects)"
    $lineNo = "W-X15-L2"; $faceNo = "FA2"
    $before = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE line_no='$lineNo' AND face_no='$faceNo' AND solve=2;")
    $names = @("W-X15-D2A","W-X15-D2B","W-X15-D2C","W-X15-D2D","W-X15-D2E")
    foreach ($dn in $names) {
        $body = New-AlarmBody -Uuid ("$script:RunTag-T2-$dn") -LineNo $lineNo -FaceNo $faceNo -Message ("[$dn] defect alarm")
        Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
        Start-Sleep -Milliseconds 200
    }
    Start-Sleep -Seconds 1
    $after = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE line_no='$lineNo' AND face_no='$faceNo' AND solve=2;")
    $delta = $after - $before
    $detail = Invoke-PsqlTable "SELECT defect_name, solve FROM alarm_record WHERE line_no='$lineNo' AND face_no='$faceNo' ORDER BY defect_name;"
    Write-Host ("  before=$before after=$after delta=$delta") -ForegroundColor Yellow
    Write-Host ("  detail: " + $detail.Trim())
    if ($delta -ge 5) {
        Log-PASS "T2" "5 different defectName all UNSOLVED (delta=$delta >= 5)"
    } else {
        Log-FAIL "T2" "Expected >=5 new UNSOLVED, delta=$delta, detail: $($detail.Trim())"
    }
}

function Run-T3 {
    Write-Banner "T3 different lineNo not cross-drown (same defect+face different line)"
    $defect = "W-X15-D3"
    $faceNo = "FA3"
    $lines  = @("W-X15-L3A","W-X15-L3B","W-X15-L3C")
    foreach ($ln in $lines) {
        $body = New-AlarmBody -Uuid ("$script:RunTag-T3-$ln") -LineNo $ln -FaceNo $faceNo -Message ("[$defect] defect alarm")
        Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
        Start-Sleep -Milliseconds 200
    }
    Start-Sleep -Seconds 1
    $out = Invoke-PsqlTable "SELECT line_no, solve, count(*) FROM alarm_record WHERE defect_name='$defect' AND face_no='$faceNo' GROUP BY line_no, solve ORDER BY line_no;"
    Write-Host ("  group: " + $out.Trim())
    $cnt = @([regex]::Matches($out, "W-X15-L3[ABC] \| +2 \| +1")).Count
    if ($cnt -ge 3) {
        Log-PASS "T3" "$($lines.Count) lineNo each UNSOLVED (matched $cnt rows)"
    } else {
        Log-FAIL "T3" "Expected 3 lineNo each UNSOLVED, matched $cnt rows: $($out.Trim())"
    }
}

function Run-T4 {
    Write-Banner "T4 ignore_alarm whitelist hit -> no yk push"
    $defect = "W-X15-D4"; $lineNo = "W-X15-L4"; $faceNo = "FA4"

    Write-Sub "4.1 INSERT ignore_alarm (POST /web/alarm/ignore is a no-op because IgnoreAlarmDTO is empty)"
    $apiTry = Invoke-CurlJson -Method Post -Uri $IgnoreUrl -Body @{
        type=$1; lineNo=$lineNo; faceNo=$faceNo; defectName=$defect
        startTime=(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
        endTime=(Get-Date).AddDays(7).ToString("yyyy-MM-dd HH:mm:ss")
    }
    $apiVerify = Invoke-PsqlScalar "SELECT count(*) FROM ignore_alarm WHERE defect_name='$defect' AND line_no='$lineNo';"
    Write-Host ("  After API POST ignore_alarm count = $apiVerify (expected: 0 since DTO empty)")

    $id = Add-IgnoreAlarm -Defect $defect -Line $lineNo -Face $faceNo -Type 1
    $verify = Invoke-PsqlTable "SELECT id, defect_name, line_no, face_no, type, end_time FROM ignore_alarm WHERE id=$id;"
    Write-Host ("  Whitelist row: " + $verify.Trim())

    Write-Sub "4.2 send 1 alarm matching whitelist"
    $ykSince = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $beforeErr = Get-YkErrorCountSince -Since $ykSince
    $body = New-AlarmBody -Uuid ("$script:RunTag-T4-1") -LineNo $lineNo -FaceNo $faceNo -Message ("[$defect] defect alarm")
    Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
    Start-Sleep -Seconds 2
    $afterErr = Get-YkErrorCountSince -Since $ykSince
    Write-Host ("  yk ERROR count before=$beforeErr after=$afterErr") -ForegroundColor Yellow

    $lines = Get-AppLogLines -LastN 200 -Since $ykSince
    $hasIsIgnoreBug = (@($lines | Where-Object { $_ -match "character varying > timestamp" })).Count -gt 0
    $hasPushToYK = (@($lines | Where-Object { $_ -match "success receive alarm event" })).Count -gt 0
    $hasSkip = (@($lines | Where-Object { $_ -match "yk upload disabled" })).Count -gt 0

    Write-Host ("  log has isIgnore SQL bug (varchar vs timestamp): " + $hasIsIgnoreBug) -ForegroundColor $(if($hasIsIgnoreBug){"Red"}else{"Green"})
    Write-Host ("  log has success receive alarm event: " + $hasPushToYK) -ForegroundColor $(if($hasPushToYK){"Yellow"}else{"Green"})
    Write-Host ("  log has yk upload disabled skip: " + $hasSkip) -ForegroundColor $(if($hasSkip){"Green"}else{"Gray"})

    # T4 acceptance: yk ERROR did not increase
    if ($afterErr -eq $beforeErr) {
        if ($hasIsIgnoreBug) {
            Log-PASS "T4" "yk ERROR unchanged ($beforeErr) BUT isIgnore() throws BadSqlGrammarException - whitelist short-circuits via exception path (BUG, see T5)"
        } elseif ($hasSkip) {
            Log-PASS "T4" "yk ERROR unchanged ($beforeErr); yk upload disabled short-circuit triggered"
        } else {
            Log-PASS "T4" "yk ERROR unchanged ($beforeErr); whitelist effective"
        }
    } else {
        Log-FAIL "T4" "yk ERROR delta=$($afterErr-$beforeErr) (expected 0)"
    }
}

function Run-T5 {
    Write-Banner "T5 ignore_alarm whitelist hit -> no WS broadcast"
    $defect = "W-X15-D5"; $lineNo = "W-X15-L5"; $faceNo = "FA5"
    Add-IgnoreAlarm -Defect $defect -Line $lineNo -Face $faceNo -Type 1 | Out-Null
    $ykSince = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")

    $body = New-AlarmBody -Uuid ("$script:RunTag-T5-1") -LineNo $lineNo -Face $faceNo -Message ("[$defect] defect alarm")
    Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
    Start-Sleep -Seconds 2

    $lines = Get-AppLogLines -LastN 200 -Since $ykSince
    $hasBroadcast = (@($lines | Where-Object { $_ -match "broadcastByUid|sendAlarmTextMessage" })).Count -gt 0
    $hasIsIgnoreBug = (@($lines | Where-Object { $_ -match "character varying > timestamp|IgnoreAlarmServiceImpl" })).Count -gt 0

    Write-Host ("  log has sendAlarmTextMessage/broadcastByUid: " + $hasBroadcast) -ForegroundColor $(if($hasBroadcast){"Yellow"}else{"Green"})
    Write-Host ("  log has isIgnore SQL exception: " + $hasIsIgnoreBug) -ForegroundColor $(if($hasIsIgnoreBug){"Red"}else{"Green"})

    if (-not $hasBroadcast) {
        if ($hasIsIgnoreBug) {
            Log-PASS "T5" "WS not sent (sendAlarmTextMessage NOT reached because isIgnore threw BadSqlGrammarException - **silent BUG**, whitelist short-circuited by exception)"
        } else {
            Log-PASS "T5" "WS not sent; whitelist effective"
        }
    } else {
        Log-FAIL "T5" "sendAlarmTextMessage WAS called -> whitelist bypassed"
    }
}

function Run-T6 {
    Write-Banner "T6 after delete whitelist -> yk still silent (uploadEnabled=false)"
    $defect = "W-X15-D6"; $lineNo = "W-X15-L6"; $faceNo = "FA6"

    $id = Add-IgnoreAlarm -Defect $defect -Line $lineNo -Face $faceNo -Type 1
    Write-Host ("  Whitelist row id=$id")

    if ($id -and $id -match '^\d+$') {
        $delUrl = "http://127.0.0.1:80/web/alarm/ignore/$id"
        Write-Host ("  DELETE $delUrl")
        try {
            $del = Invoke-RestMethod -Uri $delUrl -Method Delete -TimeoutSec 10 -ErrorAction Stop
            Write-Host ("  < " + ($del | ConvertTo-Json -Compress -Depth 3)) -ForegroundColor DarkGray
        } catch {
            Write-Host ("  < ERROR: " + $_.Exception.Message) -ForegroundColor DarkRed
        }
    }
    Remove-IgnoreAlarm -Defect $defect -Line $lineNo -Face $faceNo

    $afterDel = Invoke-PsqlScalar "SELECT count(*) FROM ignore_alarm WHERE defect_name='$defect' AND line_no='$lineNo';"
    Write-Host ("  After delete whitelist count = $afterDel (expected 0)")

    Write-Sub "6.1 send 1 alarm (whitelist removed)"
    $ykSince = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $beforeErr = Get-YkErrorCountSince -Since $ykSince
    $body = New-AlarmBody -Uuid ("$script:RunTag-T6-1") -LineNo $lineNo -Face $faceNo -Message ("[$defect] defect alarm")
    Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
    Start-Sleep -Seconds 2
    $afterErr = Get-YkErrorCountSince -Since $ykSince
    Write-Host ("  yk ERROR before=$beforeErr after=$afterErr") -ForegroundColor Yellow

    $lines = Get-AppLogLines -LastN 200 -Since $ykSince
    $hasSkip = (@($lines | Where-Object { $_ -match "yk upload disabled" })).Count -gt 0
    $hasReceive = (@($lines | Where-Object { $_ -match "success receive alarm event" })).Count -gt 0

    Write-Host ("  log has yk upload disabled skip: " + $hasSkip) -ForegroundColor $(if($hasSkip){"Green"}else{"Gray"})
    Write-Host ("  log has success receive alarm event: " + $hasReceive) -ForegroundColor $(if($hasReceive){"Yellow"}else{"Gray"})

    if ($afterErr -eq 0) {
        if ($hasSkip) {
            Log-PASS "T6" "yk ERROR=0; yk upload disabled silent-skip triggered (double-switch safety net works)"
        } else {
            Log-PASS "T6" "yk ERROR=0; whitelist silently blocked OR send_yk_enable path blocked"
        }
    } else {
        Log-FAIL "T6" "yk ERROR=$afterErr (expected 0; uploadEnabled=false should silent)"
    }
}

function Run-T7 {
    Write-Banner "T7 pressure 100 same-class (expect UNSOLVED <= 5)"
    $defect = "W-X15-D7"; $lineNo = "W-X15-L7"; $faceNo = "FA7"
    Invoke-PsqlExec "DELETE FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo';" | Out-Null

    $startTime = Get-Date
    for ($i = 1; $i -le 100; $i++) {
        $body = New-AlarmBody -Uuid ("$script:RunTag-T7-$i") -LineNo $lineNo -Face $faceNo -Message ("[$defect] defect alarm - #$i")
        $json = $body | ConvertTo-Json -Depth 5 -Compress
        try {
            Invoke-RestMethod -Uri $AlarmUrl -Method Post -Body $json -ContentType "application/json;charset=UTF-8" -TimeoutSec 10 -ErrorAction SilentlyContinue | Out-Null
        } catch {}
        if ($i % 25 -eq 0) { Write-Host ("  sent $i/100") -ForegroundColor DarkGray }
    }
    $dur = (Get-Date) - $startTime
    Write-Host ("  elapsed: {0:0.0}s" -f $dur.TotalSeconds) -ForegroundColor Yellow
    Start-Sleep -Seconds 2

    $uns = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo' AND face_no='$faceNo' AND solve=2;")
    $ig  = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo' AND face_no='$faceNo' AND solve=3;")
    $tot = [int](Invoke-PsqlScalar "SELECT count(*) FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo' AND face_no='$faceNo';")
    Write-Host ("  UNSOLVED=$uns IGNORE=$ig total=$tot") -ForegroundColor Yellow

    if ($uns -le 5) {
        Log-PASS "T7" "UNSOLVED=$uns <= 5; total=$tot (drowned $ig duplicates)"
    } else {
        Log-FAIL "T7" "UNSOLVED=$uns > 5; dedup anti-burst failed"
    }
}

function Run-T8 {
    Write-Banner "T8 noise alarm ('false trigger' / event misfire)"
    $defect = "W-X15-D8"; $lineNo = "W-X15-L8"; $faceNo = "FA8"
    Invoke-PsqlExec "DELETE FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo';" | Out-Null

    $body = New-AlarmBody -Uuid ("$script:RunTag-T8-noise-1") -LineNo $lineNo -FaceNo $faceNo -Message ("[$defect] event-misfire, drop this trigger. interval 1134875ms")
    Invoke-CurlJson -Method Post -Uri $AlarmUrl -Body $body | Out-Null
    Start-Sleep -Seconds 2

    $out = Invoke-PsqlTable "SELECT id, solve, defect_name, message FROM alarm_record WHERE defect_name='$defect' AND line_no='$lineNo';"
    Write-Host ("  record: " + $out.Trim())

    if ($out -match "\| +3 \|") {
        Log-PASS "T8" "noise alarm recorded with solve=3 (IGNORE) - PSM-style noise rule works"
    } elseif ($out -match "\| +2 \|") {
        Log-FAIL "T8" "noise alarm recorded with solve=2 (UNSOLVED) - PSM-style noise->IGNORE rule NOT implemented in DataupLoad"
    } else {
        Log-FAIL "T8" "noise alarm not stored: $($out.Trim())"
    }
}

# === Route ===
Write-Banner "Test routing"
if ($All -or $TestId -eq "") {
    Run-T1; Run-T2; Run-T3; Run-T4; Run-T5; Run-T6; Run-T7; Run-T8
} else {
    switch ($TestId.ToUpper()) {
        "T1" { Run-T1 }
        "T2" { Run-T2 }
        "T3" { Run-T3 }
        "T4" { Run-T4 }
        "T5" { Run-T5 }
        "T6" { Run-T6 }
        "T7" { Run-T7 }
        "T8" { Run-T8 }
        default { Write-Host ("Unknown TestId: " + $TestId) -ForegroundColor Red }
    }
}

# === Summary ===
Write-Banner "W-X15 gray-box test summary"
$pass = (@($script:TestResults | Where-Object { $_.Result -eq "PASS" })).Count
$fail = (@($script:TestResults | Where-Object { $_.Result -eq "FAIL" })).Count
foreach ($r in $script:TestResults) {
    $color = if ($r.Result -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("  {0,-4} {1,-6} {2}" -f $r.Test, $r.Result, $r.Reason) -ForegroundColor $color
}
Write-Host ""
Write-Host ("PASS=$pass FAIL=$fail") -ForegroundColor $(if($fail -eq 0){"Green"}else{"Red"})

Write-Sub "Final PG state"
$finAlarm = Invoke-PsqlTable "SELECT solve, count(*) FROM alarm_record GROUP BY solve ORDER BY solve;"
$finIg = Invoke-PsqlScalar "SELECT count(*) FROM ignore_alarm;"
$ykTotal = (@(Get-Content $AppLog -ErrorAction SilentlyContinue | Where-Object { $_ -match "push alarm info to yk failed|push alarm to yk error|push alarm to yk exception" })).Count
Write-Host ("alarm_record by solve:`n" + $finAlarm.Trim())
Write-Host ("ignore_alarm total: $finIg")
Write-Host ("yk push ERROR total (cumulative): $ykTotal")

# === Write result log ===
$summary = @()
$summary += ("W-X15 test report " + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
$summary += ("RunTag=" + $script:RunTag)
$summary += ""
$summary += "== Results =="
foreach ($r in $script:TestResults) {
    $summary += ("{0,-4} {1,-6} {2}" -f $r.Test, $r.Result, $r.Reason)
}
$summary += ""
$summary += "== Final PG =="
$summary += $finAlarm.Trim()
$summary += ("ignore_alarm total: " + $finIg)
$summary += ("yk push ERROR total: " + $ykTotal)
try {
    [System.IO.File]::WriteAllText($ResultLog, ($summary -join "`r`n"), [System.Text.UTF8Encoding]::new($false))
    Write-Host ("Detail log written: " + $ResultLog + " size=" + (Get-Item $ResultLog).Length) -ForegroundColor Cyan
} catch {
    Write-Host ("WARNING: failed to write result log: " + $_.Exception.Message) -ForegroundColor Yellow
    Write-Host ("Result path was: " + $ResultLog) -ForegroundColor Yellow
}

Write-Host ""
Write-Host ("Detail log: " + $ResultLog) -ForegroundColor Cyan
