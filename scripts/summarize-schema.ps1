param([string]$DumpFile)
$content = [System.IO.File]::ReadAllText($DumpFile)
$tables  = ([regex]::Matches($content, 'CREATE TABLE\s+\S+', 'IgnoreCase')).Count
$idxs    = ([regex]::Matches($content, 'CREATE\s+(UNIQUE\s+)?INDEX\s+\S+', 'IgnoreCase')).Count
Write-Output ("Tables:  {0}" -f $tables)
Write-Output ("Indexes: {0}" -f $idxs)
Write-Output "---"
Write-Output "TABLES:"
[regex]::Matches($content, 'CREATE TABLE\s+(\S+)', 'IgnoreCase') | ForEach-Object {
    Write-Output ("  {0}" -f $_.Groups[1].Value)
}
Write-Output "---"
Write-Output "INDEXES:"
[regex]::Matches($content, 'CREATE\s+(UNIQUE\s+)?INDEX\s+(\S+)\s+ON\s+(\S+)', 'IgnoreCase') | ForEach-Object {
    Write-Output ("  {0,-32} on {1}" -f $_.Groups[2].Value, $_.Groups[3].Value)
}
