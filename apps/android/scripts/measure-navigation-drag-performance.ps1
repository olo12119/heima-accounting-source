param(
    [string]$AdbPath = 'D:\AndroidDev\Sdk\platform-tools\adb.exe',
    [string]$PackageName = 'com.heima.accounting.dev',
    [string]$ActivityName = 'com.heima.accounting.MainActivity',
    [string]$MaterialMode = 'ON',
    [int]$Cycles = 8
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AdbPath)) { throw "adb not found: $AdbPath" }

# 1080 x 2400 project AVD. Gestures start on the current selection lens and
# deliberately cross the record slot to verify that dragging never opens it.
& $AdbPath shell am force-stop $PackageName | Out-Null
$startup = & $AdbPath shell am start -W -n "$PackageName/$ActivityName"
Start-Sleep -Milliseconds 1200
& $AdbPath shell input tap 108 2210 | Out-Null
Start-Sleep -Milliseconds 250
& $AdbPath shell dumpsys gfxinfo $PackageName reset | Out-Null

$clock = [Diagnostics.Stopwatch]::StartNew()
for ($cycle = 0; $cycle -lt $Cycles; $cycle++) {
    & $AdbPath shell input swipe 108 2210 324 2210 180 | Out-Null
    Start-Sleep -Milliseconds 120
    & $AdbPath shell input swipe 324 2210 756 2210 220 | Out-Null
    Start-Sleep -Milliseconds 120
    & $AdbPath shell input swipe 756 2210 972 2210 180 | Out-Null
    Start-Sleep -Milliseconds 120
    & $AdbPath shell input swipe 972 2210 108 2210 260 | Out-Null
    Start-Sleep -Milliseconds 140
}
$clock.Stop()

$gfx = & $AdbPath shell dumpsys gfxinfo $PackageName
$memory = & $AdbPath shell dumpsys meminfo $PackageName
$cpu = & $AdbPath shell dumpsys cpuinfo

"Scenario: Bottom lens drag and pager navigation; Liquid Glass $MaterialMode; Cycles: $Cycles; Wall: $($clock.ElapsedMilliseconds) ms"
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
