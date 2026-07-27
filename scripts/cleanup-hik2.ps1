$hik = "C:\hik"
$bakLog = "E:\DEMO\数据采集\backups\2026-07-27-cleanup\hik-old-logs"
New-Item -ItemType Directory -Force -Path $bakLog | Out-Null

$logFiles = @("after1.log", "app-snap.log", "app10.log", "app2-snap.log", "app3-snap.log", "app4-snap.log", "app5-snap.log", "app6.log", "app7.log", "app8.log", "app9.log", "err-snap.log", "latest.log", "probe-log.log", "probe.ps1", "run.cmd", "verify-hash.ps1")

# 备份
foreach ($f in $logFiles) {
  $src = Join-Path $hik $f
  if (Test-Path $src) {
    $dest = Join-Path $bakLog $f
    Copy-Item $src $dest -Force -ErrorAction SilentlyContinue
  }
}

# 删除（保留 jar / 反编译目录 / hit 文本）
$removed = 0
foreach ($f in $logFiles) {
  $src = Join-Path $hik $f
  if (Test-Path $src) {
    Remove-Item $src -Force -ErrorAction SilentlyContinue
    $removed++
  }
}

# index-app.js 是 PSM 老 SPA 的 modern chunk，保留备份
$ia = Join-Path $hik "index-app.js"
if (Test-Path $ia) {
  Copy-Item $ia (Join-Path $bakLog "index-app.js") -Force
  Remove-Item $ia -Force -ErrorAction SilentlyContinue
  $removed++
}

Write-Host ("Removed " + $removed + " more items")
Write-Host ""
Get-ChildItem $hik -Force | Select-Object Name, Mode, Length | Format-Table -AutoSize
