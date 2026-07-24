if ($true) {
    if ($false) {
        Write-Host "x"
    } elseif ($true) {
        Write-Host ("[OK] DB size = " + [math]::Round(1.5, 2) + " MB") -ForegroundColor Green
    }
}
