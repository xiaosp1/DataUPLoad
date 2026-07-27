$hik = "C:\hik"
$bak = "E:\DEMO\数据采集\backups\2026-07-27-cleanup\hik-debug-tools"
New-Item -ItemType Directory -Force -Path $bak | Out-Null

# 保留清单（PM 调研产物）
$keepDirs = @("psm-decompiled", "psm-extract", "starter-extract", "starter-check", "xml-out")
$keepFiles = @("psm-login.txt", "sha256-hits.txt", "locale-hits.txt", "framework-starter-debug.jar", "starter.jar")

# 删除清单：bcrypt 工具 + cmd/ps1 调试脚本 + 临时 class/java + 日志
$toRemove = @(
  "bcrypt-tool", "starter", "starter2", "verify",
  "BcryptCheck.class", "BcryptCheck.java",
  "CheckService.class", "CheckService.java",
  "DebugHash.class", "DebugHash.java",
  "GenCorrectHash.class", "GenCorrectHash.java",
  "GenCorrectHash2.class", "GenCorrectHash2.java",
  "GenPSMHash.class", "GenPSMHash.java",
  "T.class", "T.java", "T2.class", "T2.java",
  "TestBcryptNull.class", "TestBcryptNull.java",
  "TestLogin.class", "TestLogin.java",
  "UpdateHash2.class", "UpdateHash2.java",
  "VerifyHash2.class", "VerifyHash2.java",
  "VerifyLogin.class", "VerifyLogin.java",
  "db-pwd.txt", "cookies.txt", "dbhash.txt",
  "check-account.cmd", "check-account.sql",
  "check-db-hash.ps1", "compile-auth01.ps1",
  "compile-W-AUTH-01.cmd", "copy-mapper.ps1",
  "curl-correct.cmd", "curl-final.cmd",
  "dump.txt", "find-guard.cmd", "find-locale.cmd",
  "find-router.cmd", "find-router-createWebHashHistory.cmd",
  "find-sha256.cmd", "find-startup.cmd",
  "grep-router.ps1", "guard-hits.txt",
  "probe-*.ps1", "probe-*.cmd",
  "psm-login.txt",  # 备份后删
  "reset-password.sql", "restart-ps.ps1", "restart-ps3.ps1",
  "restart.bat", "restart.err", "restart.out", "restart.ps1",
  "router-file.txt", "router-hits.txt",
  "run-*.cmd", "run-*.bat",
  "scan-*.ps1", "sha256-hits.txt",
  "startup-err.log", "startup-out.log",
  "startup.err", "startup.log", "startup.txt",
  "update-db.sql"
)

# 先备份要删的全部
foreach ($item in $toRemove) {
  $src = Join-Path $hik $item
  if (Test-Path $src) {
    $dest = Join-Path $bak $item
    if ((Get-Item $src).PSIsContainer) {
      Copy-Item $src $dest -Recurse -Force -ErrorAction SilentlyContinue
    } else {
      Copy-Item $src $dest -Force -ErrorAction SilentlyContinue
    }
  }
}
# 也备份日志大文件（仅记录，不删）
$logFiles = @("after1.log", "app-snap.log", "app10.log", "app2-snap.log", "app3-snap.log", "app4-snap.log", "app5-snap.log", "app6.log", "app7.log", "app8.log", "app9.log", "err-snap.log", "index-app.js", "latest.log", "probe-log.log")
foreach ($f in $logFiles) {
  $src = Join-Path $hik $f
  if (Test-Path $src) {
    $dest = Join-Path "E:\DEMO\数据采集\backups\2026-07-27-cleanup\hik-old-logs" $f
    New-Item -ItemType Directory -Force -Path "E:\DEMO\数据采集\backups\2026-07-27-cleanup\hik-old-logs" | Out-Null
    Copy-Item $src $dest -Force -ErrorAction SilentlyContinue
  }
}

Write-Host ("Backed up to " + $bak)
Write-Host ""
Write-Host "Removing items..."

# 删除
$removed = 0
foreach ($item in $toRemove) {
  $src = Join-Path $hik $item
  if (Test-Path $src) {
    Remove-Item $src -Recurse -Force -ErrorAction SilentlyContinue
    $removed++
  }
}
Write-Host ("Removed " + $removed + " items")

# 也删 jar-extract（备份留）
$jarExtract = Join-Path $hik "jar-extract"
if (Test-Path $jarExtract) {
  Copy-Item $jarExtract $bak -Recurse -Force -ErrorAction SilentlyContinue
  Remove-Item $jarExtract -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "--- C:\hik after cleanup ---"
Get-ChildItem $hik -Force | Select-Object Name, Mode, Length | Format-Table -AutoSize
