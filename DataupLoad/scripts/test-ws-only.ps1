# WebSocket-only test - races in fast when port 80 appears
$logFile = 'E:\DEMO\数据采集\DataupLoad\log\DataupLoad\DataupLoad.log'
$maxWait = 200
$start = Get-Date
$wsResults = @()

Write-Host "[$(Get-Date)] Polling for port 80..."

while (((Get-Date) - $start).TotalSeconds -lt $maxWait) {
    $listener = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener -and (Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue).ProcessName -eq 'hik-java') {
        Write-Host "[$(Get-Date)] Port 80 up (PID $($listener.OwningProcess)). Running WS tests NOW..."

        $wsUrls = @('ws://127.0.0.1:80/ws/screen', 'ws://127.0.0.1:80/ws/alarm', 'ws://127.0.0.1:80/ws')
        foreach ($wsUrl in $wsUrls) {
            Write-Host "--- Connecting to $wsUrl ---"
            try {
                $ws = New-Object System.Net.WebSockets.ClientWebSocket
                $ct = [System.Threading.CancellationToken]::None
                $connectTask = $ws.ConnectAsync([Uri]::new($wsUrl), $ct)
                $completed = $connectTask.Wait(8000)
                if ($completed -and $ws.State -eq 'Open') {
                    Write-Host "OK  Connected! State=$($ws.State)"

                    # Use proper ArraySegment<byte> overload
                    $pingText = '{"type":"ping","clientType":"test"}'
                    $bytes = [System.Text.Encoding]::UTF8.GetBytes($pingText)
                    $seg = New-Object System.ArraySegment[byte] -ArgumentList (, $bytes)
                    $sendTask = $ws.SendAsync($seg, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $ct)
                    if ($sendTask.Wait(3000)) {
                        Write-Host "OK  Sent: $pingText"
                        $buf = [byte[]]::new(65536)
                        $recvSeg = New-Object System.ArraySegment[byte] -ArgumentList (, $buf)
                        $recvTask = $ws.ReceiveAsync($recvSeg, $ct)
                        if ($recvTask.Wait(5000)) {
                            $result = $recvTask.Result
                            $text = [System.Text.Encoding]::UTF8.GetString($buf, 0, $result.Count)
                            Write-Host "OK  Received: $text"
                        } else {
                            Write-Host "(no immediate reply within 5s)"
                        }
                    } else {
                        Write-Host "(send timeout)"
                    }

                    $ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, 'done', $ct).Wait(2000) | Out-Null
                    $ws.Dispose()
                    $wsResults += @{ url = $wsUrl; ok = $true; state = $ws.State }
                } else {
                    Write-Host "ERR Connect timeout / state=$($ws.State)"
                    $wsResults += @{ url = $wsUrl; ok = $false; state = $ws.State }
                }
            } catch {
                $msg = $_.Exception.Message
                if ($_.Exception.InnerException) { $msg += " | Inner: $($_.Exception.InnerException.Message)" }
                Write-Host "ERR $msg"
                $wsResults += @{ url = $wsUrl; ok = $false; error = $msg }
            }
            Start-Sleep -Milliseconds 500
        }
        break
    }
    Start-Sleep -Seconds 2
}

$wsResults | ConvertTo-Json -Depth 3 | Out-File 'E:\DEMO\数据采集\DataupLoad\logs\test-results-ws.json' -Encoding UTF8
Write-Host "Done."
