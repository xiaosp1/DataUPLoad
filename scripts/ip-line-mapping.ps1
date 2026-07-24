$log = Get-ChildItem -LiteralPath 'E:\DEMO\数据采集\logs' -Filter 'intco-edge-host-*.log' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Write-Host "Log: $($log.FullName) ($([math]::Round($log.Length/1MB,2)) MB)"

# 1) 抓 IP（ConnectionId + RemoteIp）
$ipMap = @{}
Get-Content -LiteralPath $log.FullName -Tail 10000 -ErrorAction SilentlyContinue |
    ForEach-Object {
        if ($_ -match '"ConnectionId":"(?<cid>[^"]+)".*?"RemoteIp":"(?<ip>[^"]+)"') {
            if (-not $ipMap.ContainsKey($Matches.cid)) { $ipMap[$Matches.cid] = $Matches.ip }
        }
        elseif ($_ -match '"RemoteIp":"(?<ip>[^"]+)".*?"ConnectionId":"(?<cid>[^"]+)"') {
            if (-not $ipMap.ContainsKey($Matches.cid)) { $ipMap[$Matches.cid] = $Matches.ip }
        }
    }

# 2) 抓 lineNo/faceNo（ConnectionId + HandleDetectDataAsync line/status）
$lineMap = @{}
Get-Content -LiteralPath $log.FullName -Tail 10000 -ErrorAction SilentlyContinue |
    Where-Object { $_ -match 'HandleDetectDataAsync line/status' } |
    ForEach-Object {
        $cid = $null; $line = $null; $face = $null
        if ($_ -match '"ConnectionId":"(?<cid>[^"]+)".*?lineNo=(?<line>\S+)\s+faceNo=(?<face>\S+)') {
            $cid = $Matches.cid; $line = $Matches.line; $face = $Matches.face
        }
        elseif ($_ -match 'lineNo=(?<line>\S+)\s+faceNo=(?<face>\S+).*?"ConnectionId":"(?<cid>[^"]+)"') {
            $cid = $Matches.cid; $line = $Matches.line; $face = $Matches.face
        }
        if ($cid) { $lineMap[$cid] = @{ Line = $line; Face = $face } }
    }

Write-Host "IP map: $($ipMap.Count) entries"
Write-Host "Line map: $($lineMap.Count) entries"

# 3) 关联 IP ↔ lineNo/faceNo（每 IP 累计次数）
$ipLine = @{}
foreach ($cid in $lineMap.Keys) {
    if ($ipMap.ContainsKey($cid)) {
        $ip = $ipMap[$cid]
        $lf = $lineMap[$cid]
        $key = "$($lf.Line)/$($lf.Face)"
        if (-not $ipLine.ContainsKey($ip)) {
            $ipLine[$ip] = @{ LineFace = $key; Hits = 0 }
        }
        $ipLine[$ip].Hits++
    }
}

Write-Host ""
Write-Host "=== IP ↔ LineNo/FaceNo 对应表 ==="
$ipLine.GetEnumerator() | Sort-Object Key | ForEach-Object {
    "{0,-18} -> {1,-12} (hits={2})" -f $_.Key, $_.Value.LineFace, $_.Value.Hits
}
