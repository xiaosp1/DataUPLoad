# heartbeat-to-feishu.ps1
# 每 10 分钟在群里发当前进度

$log = Get-Content "E:\DEMO\数据采集\DataupLoad\log\DataupLoad\info.log" -Tail 5 -ErrorAction SilentlyContinue
$port = (Get-NetTCPConnection -State Listen -LocalPort 80 -ErrorAction SilentlyContinue).OwningProcess
$proc = if ($port) { Get-Process -Id $port -ErrorAction SilentlyContinue | Select-Object Id, ProcessName, @{N='Mem(MB)';E={[Math]::Round($_.WorkingSet64/1MB,0)}}, StartTime } else { "DOWN" }

$msg = @"
🏭 PM 心跳（$(Get-Date -Format 'HH:mm:ss')）
- hik-java 80 端口：$proc
- yk 推送：$(if (Select-String -Path 'E:\DEMO\数据采集\DataupLoad\config\application-prod.yml' -Pattern 'enable: true') {'⚠️ ON'} else {'✅ OFF（已熔断）'})
- 报警落 PG：$(Get-Process -Name "postgresql*" -ErrorAction SilentlyContinue | Measure-Object | Select-Object -ExpandProperty Count) 个 PG 进程
- 现场工控机推流：最近 5 条 log：
$log
"@

# 这里 PM 不直接调飞书 API，由 OpenClaw 处理
# 把 msg 写到文件，cron 触发时让 PM 转发到群
$msg | Out-File -FilePath "E:\DEMO\数据采集\heartbeat-current.txt" -Encoding UTF8