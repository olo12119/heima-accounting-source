param(
    [string]$AdbPath = 'D:\AndroidDev\Sdk\platform-tools\adb.exe',
    [string]$PackageName = 'com.heima.accounting.dev',
    [string]$ActivityName = 'com.heima.accounting.MainActivity',
    [string]$MaterialMode = 'ON',
    [int]$Cycles = 12
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "adb not found: $AdbPath" }

& $AdbPath shell am force-stop $PackageName | Out-Null
$startup = & $AdbPath shell am start -W -n "$PackageName/$ActivityName"
Start-Sleep -Milliseconds 1200
& $AdbPath shell input tap 108 2210 | Out-Null
Start-Sleep -Milliseconds 300
& $AdbPath shell dumpsys gfxinfo $PackageName reset | Out-Null

$clock = [Diagnostics.Stopwatch]::StartNew()
for ($cycle = 0; $cycle -lt $Cycles; $cycle++) {
    & $AdbPath shell input tap 540 2210 | Out-Null
    Start-Sleep -Milliseconds 280
    & $AdbPath shell input keyevent 4 | Out-Null
    Start-Sleep -Milliseconds 220
}
$clock.Stop()

$gfx = & $AdbPath shell dumpsys gfxinfo $PackageName
$memory = & $AdbPath shell dumpsys meminfo $PackageName
$cpu = & $AdbPath shell dumpsys cpuinfo

"Scenario: Record Sheet open/close; Liquid Glass $MaterialMode; Cycles: $Cycles; Wall: $($clock.ElapsedMilliseconds) ms"
$startup | Select-String -Pattern 'ThisTime:|TotalTime:|WaitTime:' | ForEach-Object { $_.Line.Trim() }
$patterns = @(
    '^Total frames rendered:', '^Janky frames:', '^Janky frames \(legacy\):',
    '^50th percentile:', '^90th percentile:', '^95th percentile:', '^99th percentile:',
    '^Number Missed Vsync:', '^Number Slow UI thread:', '^Number Slow issue draw commands:',
    '^Number Frame deadline missed:', '^50th gpu percentile:', '^90th gpu percentile:',
    '^95th gpu percentile:', '^99th gpu percentile:'
)
$gfx | Select-String -Pattern $patterns | ForEach-Object { $_.Line.Trim() }
$memory | Select-String -Pattern '^\s*TOTAL\s+\d' | Select-Object -First 1 | ForEach-Object { "Memory: $($_.Line.Trim())" }
$cpu | Select-String -Pattern ("/" + [regex]::Escape($PackageName) + ":\s") | Select-Object -First 1 | ForEach-Object { "CPU sample: $($_.Line.Trim())" }
