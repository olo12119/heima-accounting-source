param(
    [string[]]$Tasks = @(
        ':app:assembleDebug',
        ':app:testDebugUnitTest',
        ':app:lintDebug'
    )
)

$ErrorActionPreference = 'Stop'

$androidProject = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $androidProject '..\..')).Path
$virtualDrive = 'H:'
$mappingCreated = $false
$markerValue = 'heima-accounting-android-build-root-v1'
$mappedMarker = "$virtualDrive\apps\android\.build-root-marker"

$existingMapping = @(& subst.exe) |
    Where-Object { $_ -match ('^' + [regex]::Escape($virtualDrive)) } |
    Select-Object -First 1
if ($existingMapping) {
    if (-not (Test-Path -LiteralPath $mappedMarker) -or
        (Get-Content -Raw -LiteralPath $mappedMarker -Encoding UTF8).Trim() -ne $markerValue) {
        throw "$virtualDrive is already used by another folder. Verification stopped safely."
    }
} else {
    & subst.exe $virtualDrive $projectRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create the temporary build drive $virtualDrive."
    }
    $mappingCreated = $true
}

try {
    $env:JAVA_HOME = 'D:\AndroidDev\Jdk\jdk-17.0.20.1+1'
    $env:ANDROID_HOME = 'D:\AndroidDev\Sdk'
    $env:ANDROID_SDK_ROOT = 'D:\AndroidDev\Sdk'
    $env:ANDROID_USER_HOME = 'D:\AndroidDev\AndroidUserHome'
    $env:GRADLE_USER_HOME = 'D:\AndroidDev\GradleCache'

    Push-Location "$virtualDrive\apps\android"
    try {
        & .\gradlew.bat @Tasks --no-configuration-cache
        if ($LASTEXITCODE -ne 0) {
            throw 'Android Studio verification failed.'
        }
    } finally {
        Pop-Location
    }

    Write-Host 'Android Studio verification passed. No standalone APK was copied.' -ForegroundColor Green
} finally {
    if ($mappingCreated) {
        & subst.exe $virtualDrive /D
    }
}
