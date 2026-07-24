# W-B06 validation runner
# Wait for hik-java to come up, then test EVERYTHING in one shot (no waiting)

$logFile = 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log'
$maxWait = 200
$start = Get-Date
$results = @()
$wsResults = @()

function Test-Url([string]$url) {
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        return @{ url = $url; status = $resp.StatusCode; length = $resp.Content.Length; contentType = $resp.Headers['Content-Type']; ok = $true }
    } catch {
        $msg = ($_.Exception.Message -split "`n" | Select-Object -First 1) -as [string]
        return @{ url = $url; error = $msg; ok = $false }
    }
}

Write-Host "[$(Get-Date)] Polling for hik-java + port 80 listener..."

while (((Get-Date) - $start).TotalSeconds -lt $maxWait) {
    # Check listener exists
    $listener = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $hik = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        if ($hik -and $hik.ProcessName -eq 'hik-java') {
            Write-Host "[$(Get-Date)] Listener on 80 owned by hik-java PID $($listener.OwningProcess). Running ALL tests NOW..."

            # Test all static files
            $urls = @(
                'http://127.0.0.1:80/',
                'http://127.0.0.1:80/index.html',
                'http://127.0.0.1:80/version.json',
                'http://127.0.0.1:80/AI.png',
                'http://127.0.0.1:80/vite.svg',
                'http://127.0.0.1:80/browser.js',
                'http://127.0.0.1:80/assets/index-31885e80.css',
                'http://127.0.0.1:80/js/index.1a56f984-20260520160358.js',
                'http://127.0.0.1:80/js/polyfills-legacy.9c2b54b2-20260520160358.js'
            )
            foreach ($u in $urls) {
                $r = Test-Url $u
                if ($r.ok) {
                    Write-Host "OK  $($r.url.Replace('http://127.0.0.1:80','')) Status=$($r.status) Len=$($r.length) CT=$($r.contentType)"
                } else {
                    Write-Host "ERR $($u.Replace('http://127.0.0.1:80','')) - $($r.error)"
                }
                $results += $r
            }

            # Test WebSocket — try via .NET WebSocketClient
            Write-Host ""
            Write-Host "=== WebSocket tests ==="
            $wsUrls = @('ws://127.0.0.1:80/ws/screen', 'ws://127.0.0.1:80/ws/alarm')
            foreach ($wsUrl in $wsUrls) {
                Write-Host "--- Connecting to $wsUrl ---"
                try {
                    $ws = [System.Net.WebSockets.ClientWebSocket]::new()
                    $ct = [System.Threading.CancellationToken]::None
                    $connectTask = $ws.ConnectAsync([Uri]::new($wsUrl), $ct)
                    $completed = $connectTask.Wait(5000)
                    if ($completed -and $ws.State -eq 'Open') {
                        Write-Host "OK  Connected! State=$($ws.State)"
                        # Send ping
                        $pingMsg = [System.Text.Encoding]::UTF8.GetBytes('{"type":"ping"}')
                        $sendTask = $ws.SendAsync([System.Array]::CreateInstance([byte], $pingMsg.Length), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $ct)
                        $sent = $sendTask.Wait(3000)
                        if ($sent) {
                            Write-Host "OK  Sent ping"
                            # Try receive
                            $buf = [byte[]]::new(8192)
                            $recvTask = $ws.ReceiveAsync([System.Memory]::new($buf), $ct)
                            if ($recvTask.Wait(5000)) {
                                $result = $recvTask.Result
                                $text = [System.Text.Encoding]::UTF8.GetString($buf, 0, $result.Count)
                                Write-Host "OK  Received: $text"
                            } else {
                                Write-Host "WARN Receive timeout (no immediate reply)"
                            }
                        }
                        $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, 'done', $ct).Wait(2000) | Out-Null
                        $ws.Dispose()
                        $wsResults += @{ url = $wsUrl; ok = $true; state = $ws.State }
                    } else {
                        Write-Host "ERR Connect timeout or failed. State=$($ws.State)"
                        $wsResults += @{ url = $wsUrl; ok = $false; error = "Connect timeout / state=$($ws.State)" }
                    }
                } catch {
                    Write-Host "ERR $($_.Exception.Message)"
                    $wsResults += @{ url = $wsUrl; ok = $false; error = $_.Exception.Message }
                }
            }
            break
        }
    }
    Start-Sleep -Seconds 2
}

# Save results
$results | ConvertTo-Json -Depth 3 | Out-File 'E:\DEMO\数据采集\DataupLoad\logs\test-results-static.json' -Encoding UTF8
$wsResults | ConvertTo-Json -Depth 3 | Out-File 'E:\DEMO\数据采集\DataupLoad\logs\test-results-ws.json' -Encoding UTF8

Write-Host ""
Write-Host "Done. Test counts: static=$($results.Count), ws=$($wsResults.Count)"
