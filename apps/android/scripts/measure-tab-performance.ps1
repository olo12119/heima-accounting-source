param(
    [string]$AdbPath = 'D:\AndroidDev\Sdk\platform-tools\adb.exe',
    [string]$PackageName = 'com.heima.accounting.dev',
    [string]$ActivityName = 'com.heima.accounting.MainActivity',
    [int]$Cycles = 8,
    [int]$PauseMilliseconds = 85
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

# Coordinates correspond to the project's 1080 x 2400 Android Studio AVD.
$destinations = @(
    @(324, 2210), # Statistics
    @(756, 2210), # Budget
    @(972, 2210), # Profile
    @(108, 2210)  # Home
)

& $AdbPath shell am force-stop $PackageName | Out-Null
& $AdbPath shell am start -W -n "$PackageName/$ActivityName" | Out-Null
Start-Sleep -Milliseconds 1200
& $AdbPath shell dumpsys gfxinfo $PackageName reset | Out-Null
for ($cycle = 0; $cycle -lt $Cycles; $cycle++) {
    foreach ($point in $destinations) {
        & $AdbPath shell input tap $point[0] $point[1] | Out-Null
        Start-Sleep -Milliseconds $PauseMilliseconds
    }
}

$metrics = & $AdbPath shell dumpsys gfxinfo $PackageName
$patterns = @(
    '^Total frames rendered:',
    '^Janky frames:',
    '^Janky frames \(legacy\):',
    '^50th percentile:',
    '^90th percentile:',
    '^95th percentile:',
    '^99th percentile:',
    '^Number Missed Vsync:',
    '^Number Slow UI thread:',
    '^Number Slow issue draw commands:',
    '^Number Frame deadline missed:',
    '^50th gpu percentile:',
    '^90th gpu percentile:',
    '^95th gpu percentile:',
    '^99th gpu percentile:'
)

$metrics | Select-String -Pattern $patterns | ForEach-Object { $_.Line.Trim() }
