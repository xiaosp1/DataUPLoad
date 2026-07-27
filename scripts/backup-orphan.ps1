$bakRoot = "E:\DEMO\数据采集\backups\2026-07-27-cleanup"
New-Item -ItemType Directory -Force -Path $bakRoot | Out-Null

$bak1 = "$bakRoot\web-orphan-files"
New-Item -ItemType Directory -Force -Path $bak1 | Out-Null
Copy-Item "E:\DEMO\DATALINK\DataupLoad\web\login.html" $bak1 -Force
Copy-Item "E:\DEMO\DATALINK\DataupLoad\web\login.js" $bak1 -Force
Write-Host ("OK: backup login.html + login.js to " + $bak1)

$bak2 = "$bakRoot\application-prod-yml-bak"
New-Item -ItemType Directory -Force -Path $bak2 | Out-Null
if (Test-Path E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml.bak-0727) {
  Copy-Item E:\DEMO\DATALINK\DataupLoad\config\application-prod.yml.bak-0727 $bak2 -Force
  Write-Host ("OK: backup yml.bak-0727 to " + $bak2)
}

$hik = "C:\hik"
Write-Host ("--- " + $hik + " contents ---")
if (Test-Path $hik) {
  Get-ChildItem $hik -Force | Select-Object Name, Mode, Length, LastWriteTime | Format-Table -AutoSize
} else {
  Write-Host ($hik + " does not exist")
}
