<#
.SYNOPSIS
  Hikvision IPC Probe Script v1.2 (READ-ONLY)
.DESCRIPTION
  Usage:
    .\probe-edge.ps1                          # will prompt for HikRoot
    .\probe-edge.ps1 -HikRoot "D:\PSM"        # specify Hik install root
  Output: probe-report-<HOST>-<yyyyMMdd-HHmmss>.md in <repoRoot>/reports/probe/.
  Read-only: no service restart, no config change, no file deletion.
  Runtime: ~10-20 seconds.
#>
param(
    [string]$HikRoot = ""
)
$ErrorActionPreference = "SilentlyContinue"

if (-not $HikRoot) {
    Write-Host "Please enter Hikvision install root (e.g. D:\PSM):" -ForegroundColor Yellow
    $HikRoot = Read-Host "HikRoot"
}
$HikRoot = $HikRoot.Trim().Trim('"')
if (-not (Test-Path $HikRoot)) {
    Write-Host ("Path not found: " + $HikRoot) -ForegroundColor Red
    Write-Host 'Usage: .\probe-edge.ps1 -HikRoot "D:\PSM"' -ForegroundColor Yellow
    exit 1
}
$HikRoot = (Resolve-Path $HikRoot).Path

$RunTime = Get-Date
$IsAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
$User    = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$HostNm  = $env:COMPUTERNAME
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) { $ScriptDir = (Get-Location).Path }

# 确定仓库根：向上查找含 .git 的目录；找不到则回退到脚本目录的上级
function Find-RepoRoot([string]$startDir) {
    $d = if ([string]::IsNullOrWhiteSpace($startDir)) { (Get-Location).Path } else { $startDir }
    for ($i = 0; $i -lt 8; $i++) {
        if (-not $d) { break }
        if (Test-Path -LiteralPath (Join-Path $d '.git')) { return $d }
        $p = Split-Path -Parent $d
        if (-not $p -or $p -eq $d) { break }
        $d = $p
    }
    return $null
}
$RepoRoot = Find-RepoRoot $ScriptDir
if (-not $RepoRoot) {
    $RepoRoot = Split-Path -Parent $ScriptDir
    if (-not $RepoRoot) { $RepoRoot = (Get-Location).Path }
}
$ReportDir = Join-Path $RepoRoot (Join-Path 'reports' 'probe')
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$Out = Join-Path $ReportDir ("probe-report-" + $HostNm + "-" + $RunTime.ToString("yyyyMMdd-HHmmss") + ".md")

$OK   = [char]0x2705   # check mark
$WARN = [char]0x26A0   # warning
$BAD  = [char]0x274C   # cross
$BT   = [string]([char]0x60) * 3
$NL   = [Environment]::NewLine
$sections = New-Object System.Collections.Generic.List[string]
$Stat = @{ok=0;wan=0;bad=0}

function Sec {
    param([string]$T,[string]$Lv,[string]$S,[string]$B,[string]$Tip="")
    switch($Lv){"OK"{$Stat.ok++;$ico=$OK} "WAN"{$Stat.wan++;$ico=$WARN} "BAD"{$Stat.bad++;$ico=$BAD}}
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("## " + $T)
    [void]$sb.AppendLine("**Result**: " + $ico + " " + $S)
    [void]$sb.AppendLine("")
    if($B){[void]$sb.AppendLine($B)}
    if($Tip){[void]$sb.AppendLine("");[void]$sb.AppendLine("> **Action**: " + $Tip)}
    [void]$sb.AppendLine("")
    $sections.Add($sb.ToString())
}
function Mask([string]$s){ if(-not $s){return ""}; if($s.Length -le 4){return "****"}; return $s.Substring(0,2)+"***"+$s.Substring($s.Length-2,2) }
function CB([string]$s){ return ($BT + $NL + $s + $NL + $BT) }
function Tbl([object[]]$rows,[string[]]$cols){
    if(-not $rows -or $rows.Count -eq 0){return "(no data)"}
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("| "+($cols -join " | ")+" |")
    [void]$sb.AppendLine("| "+(($cols|ForEach-Object{"---"}) -join " | ")+" |")
    foreach($r in $rows){
        $c=@(); foreach($k in $cols){
            $v = if($null -ne $r.$k){[string]$r.$k}else{""}
            $v = $v -replace "\|","/" -replace "[\r\n]+"," "
            if($v.Length -gt 200){$v=$v.Substring(0,200)+"..."}
            $c += $v
        }
        [void]$sb.AppendLine("| "+($c -join " | ")+" |")
    }
    return $sb.ToString()
}

Write-Host "[probe] HikRoot = $HikRoot"
Write-Host "[probe] Report  = $Out"

# ===== 1. Host basics =====
$B = New-Object System.Text.StringBuilder
$cs = Get-CimInstance Win32_ComputerSystem
$os = Get-CimInstance Win32_OperatingSystem
$cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1).Name
$cores = (Get-CimInstance Win32_Processor | Measure-Object NumberOfLogicalProcessors -Sum).Sum
$ramT = [math]::Round($cs.TotalPhysicalMemory/1GB,2)
$ramF = [math]::Round($os.FreePhysicalMemory/1MB,2)
$up = New-TimeSpan -Start $os.LastBootUpTime -End $RunTime
$upStr = "{0}d{1}h{2}m" -f $up.Days,$up.Hours,$up.Minutes
$drows=@(); Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" | ForEach-Object {
    $t=if($_.Size){[math]::Round($_.Size/1GB,1)}else{"-"}
    $f=if($_.FreeSpace){[math]::Round($_.FreeSpace/1GB,1)}else{"-"}
    $drows += [pscustomobject]@{Drive=$_.DeviceID;TotalGB=$t;FreeGB=$f;Label=$_.VolumeName}
}
$nrows=@(); Get-CimInstance Win32_NetworkAdapterConfiguration -Filter "IPEnabled=True" | ForEach-Object {
    $nrows += [pscustomobject]@{NIC=$_.Description;IP=(($_.IPAddress)-join "/");MAC=$_.MACAddress}
}
[void]$B.AppendLine((Tbl @(
    [pscustomobject]@{K="Host";V=$HostNm},
    [pscustomobject]@{K="OS";V=$os.Caption+" Build "+$os.BuildNumber},
    [pscustomobject]@{K="CPU";V=$cpu},
    [pscustomobject]@{K="Cores";V=$cores},
    [pscustomobject]@{K="RAM GB";V="$ramT total / $ramF free"},
    [pscustomobject]@{K="Uptime";V=$upStr},
    [pscustomobject]@{K="Admin";V=$IsAdmin}
) @("K","V")))
[void]$B.AppendLine(""); [void]$B.AppendLine("**Disks**"); [void]$B.AppendLine((Tbl $drows @("Drive","TotalGB","FreeGB","Label")))
[void]$B.AppendLine(""); [void]$B.AppendLine("**NIC/IP/MAC**"); [void]$B.AppendLine((Tbl $nrows @("NIC","IP","MAC")))
Sec "1. Host Basics" "OK" "Host info collected, free RAM ${ramF}GB." $B.ToString()

# ===== 2. Runtime / NTP / RDP =====
$B = New-Object System.Text.StringBuilder
$fw4="n/a"; try{$r=(Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full" -Name Release -EA 0).Release;$v=(Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full" -Name Version -EA 0).Version;$fw4="Release=$r Version=$v"}catch{}
$core=@(); foreach($p in @("${env:ProgramFiles}\dotnet\shared\Microsoft.NETCore.App","${env:ProgramFiles}\dotnet\shared\Microsoft.AspNetCore.App")){ if(Test-Path $p){ Get-ChildItem $p -Directory | ForEach-Object { $core += $_.Name } } }
$core = $core | Sort-Object -Unique
[void]$B.AppendLine("- .NET FX4: $fw4")
[void]$B.AppendLine("- .NET Core/.NET 5+: " + $(if($core.Count -gt 0){$core -join ", "}else{"not installed (self-contained OK)"}))
$tz = Get-TimeZone
[void]$B.AppendLine("- Timezone: " + $tz.Id + "  Local: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"))
$ntp=""; try{$cfg=w32tm /query /configuration 2>&1; $m=$cfg|Select-String "NtpServer\s*:\s*(\S+)"|Select-Object -First 1; if($m){$ntp=$m.Matches[0].Groups[1].Value}}catch{}
[void]$B.AppendLine("- NTP: " + $(if($ntp){$ntp}else{"<n/a>"}))
$rdpDeny=(Get-ItemProperty "HKLM:\System\CurrentControlSet\Control\Terminal Server" -Name fDenyTSConnections -EA 0).fDenyTSConnections
$rdpPort=(Get-ItemProperty "HKLM:\System\CurrentControlSet\Control\Terminal Server\WinStations\RDP-Tcp" -Name PortNumber -EA 0).PortNumber
[void]$B.AppendLine("- RDP: " + $(if($rdpDeny -eq 0){"Enabled port $rdpPort"}else{"Disabled"}))
Sec "2. Runtime / Time / RDP" "OK" ".NET, NTP, RDP probed." $B.ToString() "If clock drifts >5s from MES, fix NTP first. If .NET 8 cannot be installed, publish self-contained."

# ===== 3. Hik processes + watchdog =====
$B = New-Object System.Text.StringBuilder
$allProcs = Get-Process -EA 0
$tgt = $allProcs | Where-Object { $_.ProcessName -match "^hik|^postgres|^java|watch|dog|guard|psm|manager" }
$pRows=@(); foreach($p in $tgt){
    $cmd=""; $st="";
    $w=Get-CimInstance Win32_Process -Filter ("ProcessId="+$p.Id) -EA 0
    if($w){$cmd=$w.CommandLine}
    if($p.StartTime){$st=$p.StartTime.ToString("yyyy-MM-dd HH:mm")}
    $pRows += [pscustomobject]@{PID=$p.Id;Name=$p.ProcessName;CPU=[math]::Round($p.CPU,1);MemMB=[math]::Round($p.WorkingSet64/1MB,1);Start=$st;Cmd=$cmd}
}
$wdHints=@()
foreach($pp in $pRows){
    if($pp.Cmd -match "watch|dog|guard"){ $wdHints += "process-level: "+$pp.Name+" PID="+$pp.PID }
    if($pp.Name -match "watch|dog|guard"){ $wdHints += "by-name: "+$pp.Name+" PID="+$pp.PID }
}
if($pRows.Count -gt 0){ [void]$B.AppendLine((Tbl $pRows @("PID","Name","CPU","MemMB","Start","Cmd"))) }
else { [void]$B.AppendLine("No hik/java/postgres/watchdog-like process found. Hik may not be running.") }
[void]$B.AppendLine(""); [void]$B.AppendLine("**Watchdog hints**")
if($wdHints.Count -gt 0){ [void]$B.AppendLine((CB ($wdHints -join $NL))) } else { [void]$B.AppendLine("No obvious watchdog detected from process name/cmdline; still verify with services/tasks.") }
$pLv = if($pRows.Count -gt 0){"OK"}else{"BAD"}
Sec "3. Hik Processes and Watchdog" $pLv ("Relevant procs: "+$pRows.Count+"; watchdog hints: "+$wdHints.Count) $B.ToString() "Our process name must avoid 'hik-' prefix and either be whitelisted by the watchdog or installed as a service."

# ===== 4. Listening ports =====
$B = New-Object System.Text.StringBuilder
$listen = netstat -ano -p tcp 2>&1 | Select-String "LISTENING"
$pa=@(); foreach($l in $listen){
    $x = $l.Line -split '\s+'
    if($x.Count -lt 6){continue}
    $loc=$x[2]; $pidStr=$x[5]; $port=($loc-split":")[-1]; $addr=$loc.Substring(0,$loc.LastIndexOf(":"))
    $pn=""; $po=Get-Process -Id $pidStr -EA 0; if($po){$pn=$po.ProcessName}
    $tg=""; if(@("80","443","5432","8080","8443","3389") -contains $port){$tg="KEY"}
    if($pn -match "java|hik|postgres"){$tg=if($tg){"$tg/HIK"}else{"HIK"}}
    $pa += [pscustomobject]@{Addr=$addr;Port=$port;PID=$pidStr;Proc=$pn;Tag=$tg}
}
$hi = $pa | Where-Object { $_.Tag -ne "" } | Sort-Object {[int]$_.Port}
$pgBind = ($pa | Where-Object { $_.Port -eq "5432" } | Select-Object -First 1).Addr
[void]$B.AppendLine("**Key ports**"); [void]$B.AppendLine((Tbl $hi @("Addr","Port","PID","Proc","Tag")))
[void]$B.AppendLine(""); [void]$B.AppendLine("**All LISTENING**")
[void]$B.AppendLine((CB (($pa|Sort-Object {[int]$_.Port}|ForEach-Object{"{0,-22} :{1,-6} PID={2,-6} {3}" -f $_.Addr,$_.Port,$_.PID,$_.Proc}) -join $NL)))
Sec "4. Listening Ports" "OK" ("LISTENING "+$pa.Count+" total, "+$hi.Count+" key; PG5432 bind=" + $(if($pgBind){$pgBind}else{"<none>"})) $B.ToString() "Confirm if 5432 binds 127.0.0.1 or 0.0.0.0; note java high ports for Hik web/gateway."

# ===== 5. Config scan (MES URL + DB password hints) =====
$B = New-Object System.Text.StringBuilder
$cfFiles = New-Object System.Collections.Generic.HashSet[string]
Get-ChildItem -Path $HikRoot -Recurse -File -EA 0 -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -like "*.properties" -or $_.Name -like "*.yml" -or $_.Name -like "*.yaml" -or
    $_.Name -like "*.xml" -or $_.Name -like "*.json" -or $_.Name -like "*.conf" -or $_.Name -like "*.ini"
} | Select-Object -First 300 | ForEach-Object { [void]$cfFiles.Add($_.FullName) }
$kw = "mes|upload|gateway|dataportal|invoke|url|jdbc|postgres|password|datasource|api|token|appkey|secret|endpoint|proxy|server.port"
$hits=@(); $mesUrls=@(); $dbPw=@()
foreach($f in $cfFiles){
    try{
        $i=0; Get-Content $f -EA 0 | ForEach-Object {
            $i++; $line=$_
            if($line -match $kw){
                $c = $line -replace "(?i)(password|passwd|pwd)\s*[:=]\s*['`"]?([^'`"\s]+)['`"]?", { $_.Groups[1].Value+"="+(Mask $_.Groups[2].Value) }
                $hits += [pscustomobject]@{File=$f;Line=$i;Text=$c.Trim()}
                if($line -match "(?i)(mes|upload|gateway|dataportal|endpoint|url).*?(https?://[^\s'`"<>]+)"){ $mesUrls += $matches[2] }
                if($line -match "(?i)password\s*[:=]\s*['`"]?([^'`"\s]+)"){ $dbPw += $matches[1] }
            }
        }
    }catch{}
}
$mesUrls = $mesUrls | Sort-Object -Unique
[void]$B.AppendLine("Config files scanned: "+$cfFiles.Count+"; keyword hits: "+$hits.Count)
if($mesUrls.Count -gt 0){
    [void]$B.AppendLine(""); [void]$B.AppendLine("**Suspected MES / gateway endpoints**"); [void]$B.AppendLine((CB ($mesUrls -join $NL)))
} else {
    [void]$B.AppendLine(""); [void]$B.AppendLine("> No http(s) MES endpoint parsed from config; confirm MES target via Hik admin UI.")
}
if($hits.Count -gt 0){
    [void]$B.AppendLine(""); [void]$B.AppendLine("**Key config hits (passwords masked, first 80)**")
    [void]$B.AppendLine((CB (($hits|Select-Object -First 80|ForEach-Object{"["+$_.File+":"+$_.Line+"] "+$_.Text}) -join $NL)))
}
$cLv = if($hits.Count -gt 0){"OK"}else{"WAN"}
Sec "5. Config Scan" $cLv ("Files "+$cfFiles.Count+"; hits "+$hits.Count+"; MES URLs "+$mesUrls.Count) $B.ToString() "Share real MES credentials/password via secure channel only, never in report or chat."
$MesTgts=@()
foreach($m in $mesUrls){
    if($m -match "https?://([^:/\s]+)(:(\d+))?"){
        $h2=$matches[1]; $p2=if($matches[3]){[int]$matches[3]}elseif($m -match "^https"){443}else{80}
        $MesTgts += [pscustomobject]@{Host=$h2;Port=$p2;Url=$m}
    }
}

# ===== 6. PostgreSQL probe =====
$B = New-Object System.Text.StringBuilder
$psqlCand = New-Object System.Collections.Generic.HashSet[string]
Get-ChildItem -Path $HikRoot -Filter "psql.exe" -Recurse -Depth 6 -EA 0 | ForEach-Object { [void]$psqlCand.Add($_.FullName) }
foreach($alt in @("C:\Program Files\PostgreSQL","C:\Program Files (x86)\PostgreSQL","C:\PostgreSQL")){
    if(Test-Path $alt){ Get-ChildItem -Path $alt -Filter "psql.exe" -Recurse -Depth 4 -EA 0 | ForEach-Object { [void]$psqlCand.Add($_.FullName) } }
}
$pgProc = $allProcs | Where-Object { $_.ProcessName -match "^postgres" } | Select-Object -First 1
$p5432 = $pa | Where-Object { $_.Port -eq "5432" } | Select-Object -First 1
$dbStat=""; $dbLv="WAN"; $a2ok=$false
$pgBind = if($p5432){$p5432.Addr}else{"<none>"}
[void]$B.AppendLine("- postgres process : "+$(if($pgProc){"PID="+$pgProc.Id+" "+$pgProc.ProcessName}else{"<not running>"}))
[void]$B.AppendLine("- 5432 bind addr   : "+$pgBind)
if($psqlCand.Count -gt 0){
    [void]$B.AppendLine("- bundled psql.exe :"); [void]$B.AppendLine((CB ($psqlCand -join $NL)))
} else {
    [void]$B.AppendLine("- bundled psql.exe : <not found; A2 will use Npgsql directly>")
}
if(-not $pgProc){
    $dbStat="postgres is NOT running; A2 unavailable, verify Hik platform is up."; $dbLv="BAD"
} elseif(-not $p5432){
    $dbStat="postgres is up but 5432 not listening; port may have been changed, manual check required."; $dbLv="BAD"
} else {
    $dbStat = "postgres running, 5432 binds "+$pgBind+". "
    if($psqlCand.Count -gt 0){
        $psql = $psqlCand | Select-Object -First 1
        $tryPwd = @("postgres","Abc12345","hik12345","hikvision","123456","postgres123")
        if($dbPw.Count -gt 0){ $tryPwd = @($dbPw[0]) + $tryPwd }
        $connected = $false
        foreach($pwd in $tryPwd){
            $env:PGPASSWORD = ***
            $t = & $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -w -c "select version();" 2>&1
            if($LASTEXITCODE -eq 0 -and ($t -join " ") -match "PostgreSQL"){
                [void]$B.AppendLine(""); [void]$B.AppendLine("- postgres/"+(Mask $pwd)+" connected -> A2 directly usable")
                $dbs = & $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -w -c "SELECT datname, pg_size_pretty(pg_database_size(datname)) as size FROM pg_database WHERE datistemplate=false ORDER BY pg_database_size(datname) DESC;" 2>&1
                [void]$B.AppendLine(""); [void]$B.AppendLine("**Database list**"); [void]$B.AppendLine((CB ($dbs -join $NL)))
                $biz = ((& $psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -w -t -c "SELECT datname FROM pg_database WHERE datistemplate=false AND datname<>'postgres' ORDER BY pg_database_size(datname) DESC LIMIT 1;" 2>&1) -join "").Trim()
                if($biz -and $biz -notmatch "ERROR|FATAL"){
                    $tbls = & $psql -h 127.0.0.1 -p 5432 -U postgres -d $biz -w -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name LIMIT 80;" 2>&1
                    [void]$B.AppendLine(""); [void]$B.AppendLine("**Business DB ["+$biz+"] tables (first 80)**"); [void]$B.AppendLine((CB ($tbls -join $NL)))
                }
                $connected=$true; $a2ok=$true; $dbLv="OK"; $dbStat+="Default password works, A2 is ready."
                Remove-Item Env:PGPASSWORD -EA 0
                break
            }
            Remove-Item Env:PGPASSWORD -EA 0
        }
        if(-not $connected){
            $dbStat += "Default passwords failed; need read-only account from Hik vendor."; $dbLv="WAN"
            [void]$B.AppendLine(""); [void]$B.AppendLine("- Default passwords all failed (tried: "+(($tryPwd|ForEach-Object{Mask $_}) -join ", ")+")")
        }
    } else {
        $dbStat += "No psql.exe found; will verify connectivity with Npgsql at deploy time."; $dbLv="OK"; $a2ok=$true
    }
}
Sec "6. PostgreSQL Probe" $dbLv $dbStat $B.ToString() "If default password fails, request read-only account from Hik; A2 will be our primary path."

# ===== 7. Network connectivity =====
$B = New-Object System.Text.StringBuilder
$gw = (Get-CimInstance Win32_NetworkAdapterConfiguration -Filter "IPEnabled=True"|Where-Object{$_.DefaultIPGateway}|Select-Object -First 1).DefaultIPGateway|Select-Object -First 1
$tgs = @(@{h="8.8.8.8";p=53;n="Google DNS"})
if($gw){$tgs += @{h=$gw;p=0;n="Gateway(ICMP)"}}
foreach($m in $MesTgts){$tgs += @{h=$m.Host;p=$m.Port;n="MES: "+$m.Url}}
$ping = New-Object System.Net.NetworkInformation.Ping
function TcpT([string]$h,[int]$p,[int]$to=2500){
    try{$c=New-Object System.Net.Sockets.TcpClient;$iar=$c.BeginConnect($h,$p,$null,$null);$ok=$iar.AsyncWaitHandle.WaitOne($to,$false)
        if($ok -and $c.Connected){$c.EndConnect($iar);$c.Close();return "TCP OK <${to}ms"} else {$c.Close();return "TCP timeout/fail"}
    }catch{return "TCP err: "+$_.Exception.Message}
}
$cRows=@(); $mesReachable=$false
foreach($t in $tgs){
    if($t.p -eq 0){
        $pr=$ping.Send($t.h,1500)
        $st=if($pr.Status -eq "Success"){"ICMP OK "+$pr.RoundtripTime+"ms"}else{"ICMP fail ("+$pr.Status+")"}
        $cRows += [pscustomobject]@{Target=$t.n;Host=$t.h;Port="-";Result=$st}
    } else {
        $r = TcpT $t.h $t.p
        $cRows += [pscustomobject]@{Target=$t.n;Host=$t.h;Port=$t.p;Result=$r}
        if($t.n -match "^MES:" -and $r -match "OK"){ $mesReachable=$true }
    }
}
[void]$B.AppendLine((Tbl $cRows @("Target","Host","Port","Result")))
$cnLv = if(($cRows|Where-Object{$_.Result -match "fail|err|timeout"}).Count -eq 0){"OK"}else{"WAN"}
Sec "7. Network Connectivity" $cnLv "ICMP/TCP probes to gateway/MES done." $B.ToString() "If MES is unreachable, confirm proxy/whitelist/gateway rules before deploying A1."

# ===== 8. Recent logs (24h) =====
$B = New-Object System.Text.StringBuilder
$logHits=@(); $logFiles=0
Get-ChildItem -Path $HikRoot -Include "*.log","*.log.*","*.txt" -Recurse -File -EA 0 -Force |
    Where-Object{$_.LastWriteTime -gt (Get-Date).AddHours(-24) -and $_.Name -match "SourceManager|upload|mes|gateway|manager|platform|hik|error"} |
    Select-Object -First 20 | ForEach-Object {
    $logFiles++
    try{
        $n=0; Get-Content $_.FullName -EA 0 | Where-Object{$_ -match "error|exception|fail|timeout|mes|upload|POST"} | ForEach-Object{
            $n++; if($n -le 10){ $logHits += "["+$_.Name+"] "+$_ }
        }
    }catch{}
}
[void]$B.AppendLine("Log files (last 24h, matching): "+$logFiles)
if($logHits.Count -gt 0){
    [void]$B.AppendLine(""); [void]$B.AppendLine("**Error/upload keyword samples (first 60)**")
    [void]$B.AppendLine((CB (($logHits|Select-Object -First 60) -join $NL)))
} else {
    [void]$B.AppendLine("No recent error/exception/fail sampled; existing chain looks stable.")
}
Sec "8. Recent Logs (24h)" "OK" ("Scanned "+$logFiles+" logs, "+$logHits.Count+" key lines.") $B.ToString() "Frequent errors/timeouts indicate instability; investigate before enabling A1 proxy."

# ===== 9. Resource snapshot =====
$B = New-Object System.Text.StringBuilder
$tc = $allProcs|Sort-Object CPU -Descending|Select-Object -First 8
$tm = $allProcs|Sort-Object WorkingSet64 -Descending|Select-Object -First 8
[void]$B.AppendLine("**Top 8 CPU**"); [void]$B.AppendLine((Tbl ($tc|ForEach-Object{[pscustomobject]@{PID=$_.Id;Name=$_.ProcessName;CPU=[math]::Round($_.CPU,1);MemMB=[math]::Round($_.WorkingSet64/1MB,1)}}) @("PID","Name","CPU","MemMB")))
[void]$B.AppendLine(""); [void]$B.AppendLine("**Top 8 Memory**"); [void]$B.AppendLine((Tbl ($tm|ForEach-Object{[pscustomobject]@{PID=$_.Id;Name=$_.ProcessName;CPU=[math]::Round($_.CPU,1);MemMB=[math]::Round($_.WorkingSet64/1MB,1)}}) @("PID","Name","CPU","MemMB")))
$load = (Get-CimInstance Win32_Processor|Measure-Object LoadPercentage -Average).Average
[void]$B.AppendLine(""); [void]$B.AppendLine("Current CPU utilization ~ "+$load+"%")
Sec "9. Resource Snapshot" "OK" "CPU/Memory top processes captured." $B.ToString() "If java+postgres already use >80% RAM, verify headroom or lower our batch frequency."

# ===== FINAL VERDICT =====
$B = New-Object System.Text.StringBuilder
[void]$B.AppendLine("| Item | Value |")
[void]$B.AppendLine("| --- | --- |")
[void]$B.AppendLine("| HikRoot | " + $HikRoot + " |")
[void]$B.AppendLine("| MES endpoint (from config) | " + $(if($MesTgts.Count -gt 0){($MesTgts|ForEach-Object{$_.Url}) -join "; "}else{"<not found>"}) + " |")
[void]$B.AppendLine("| MES TCP reachable | " + $(if($mesReachable){"YES"}elseif($MesTgts.Count -eq 0){"<not configured/parsed>"}else{"NO"}) + " |")
[void]$B.AppendLine("| PostgreSQL 5432 bind | " + $pgBind + " |")
[void]$B.AppendLine("| A2 (PG direct-read) available | " + $(if($a2ok){"YES"}else{"NO"}) + " |")
[void]$B.AppendLine("| Free RAM | ${ramF}GB / ${ramT}GB |")
[void]$B.AppendLine("| Watchdog hints | " + $wdHints.Count + " |")
[void]$B.AppendLine("")

$a1feasible = ($MesTgts.Count -gt 0 -and $mesReachable)
$verdict=""; $reason=@(); $overall=""
if($a2ok -and $a1feasible){
    $verdict = $OK+" DUAL (A2 primary + A1 backup/validation)"
    $reason = @(
        "A2 local PG direct read works, zero-intrusion; use as primary.",
        "MES endpoint parsed and reachable; after Hik/IT approval, redirect MES target to local proxy as validation/backup channel.",
        "Rollout: A2 first, then add A1 for dual validation."
    )
    $overall = $OK+" GREEN: environment clear, deploy DUAL."
}elseif($a2ok){
    $verdict = $OK+" A2 = direct-read Hik PostgreSQL (RECOMMENDED)"
    $reason = @(
        "A2 local PG access works, zero-intrusion, lowest risk, fastest delivery.",
        "A1 not ready (MES URL not parsed or unreachable); ship MVP on A2, add A1 later when needed."
    )
    $overall = $OK+" GREEN: start with A2."
}elseif($a1feasible){
    $verdict = $WARN+" A1 = proxy-intercept MES upload (best available)"
    $reason = @(
        "PG direct-read not yet working (bad default password/port bind/process issue); request read-only PG account from Hik.",
        "MES endpoint parsed and reachable; A1 is feasible, but requires redirecting Hik MES target to 127.0.0.1 (needs Hik/IT approval).",
        "Load-test the proxy in staging first; avoid breaking the original upload."
    )
    $overall = $WARN+" YELLOW: A1 feasible but config change required; push Hik for PG read-only account so we can move to A2."
}else{
    $verdict = $BAD+" Cannot auto-decide; need manual input"
    $reason = @(
        "Neither A2 nor A1 meets auto-decision criteria.",
        "Manual items required: (1) MES target URL from Hik admin UI; (2) PostgreSQL port/credentials from Hik; (3) re-run script as Administrator for full data.",
        "Re-run after providing the missing items; script will output verdict automatically."
    )
    $overall = $BAD+" RED: key info missing; supply MES URL/PG credentials."
}
[void]$B.AppendLine("**Recommended capture method**: " + $verdict)
[void]$B.AppendLine("")
foreach($r in $reason){ [void]$B.AppendLine("- " + $r) }
[void]$B.AppendLine("")
[void]$B.AppendLine("> Send this .md report back to dev to confirm deploy plan. No manual note-taking needed.")
Sec "FINAL VERDICT" "OK" $overall $B.ToString()

# ===== Assemble report =====
$H = New-Object System.Text.StringBuilder
[void]$H.AppendLine("# Hik IPC Probe Report")
[void]$H.AppendLine("")
[void]$H.AppendLine("- Script v1.2")
[void]$H.AppendLine("- HikRoot : " + $HikRoot)
[void]$H.AppendLine("- Runtime : " + $RunTime.ToString("yyyy-MM-dd HH:mm:ss zzz"))
[void]$H.AppendLine("- User    : " + $User)
[void]$H.AppendLine("- IsAdmin : " + $IsAdmin)
[void]$H.AppendLine("- Host    : " + $HostNm)
[void]$H.AppendLine("")
[void]$H.AppendLine("> **Overall**: " + $overall)
[void]$H.AppendLine("> Sections : " + $OK+" "+$Stat.ok+" / "+$WARN+" "+$Stat.wan+" / "+$BAD+" "+$Stat.bad)
[void]$H.AppendLine("")
[void]$H.AppendLine("Passwords are masked. Share real credentials only via secure channel.")
[void]$H.AppendLine("Script is read-only: no service restarted, no config modified, no file deleted.")
[void]$H.AppendLine("")
$all = $H.ToString() + ($sections -join $NL)
[System.IO.File]::WriteAllText($Out, $all, (New-Object System.Text.UTF8Encoding($false)))
Write-Host ""
Write-Host ("[probe] DONE -> " + $Out) -ForegroundColor Green
Write-Host "[probe] Send this .md back to dev."
