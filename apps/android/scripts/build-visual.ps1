$ErrorActionPreference = 'Stop'

$androidProject = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $androidProject '..\..')).Path
$virtualDrive = 'H:'
$mappingCreated = $false
$markerValue = 'heima-accounting-android-build-root-v1'
$mappedMarker = "$virtualDrive\apps\android\.build-root-marker"
$deliveryNames = @(Get-Content -LiteralPath (Join-Path $PSScriptRoot 'delivery-names.txt') -Encoding UTF8)
if ($deliveryNames.Count -lt 2) {
    throw 'Delivery name configuration is incomplete.'
}

$existingMapping = @(& subst.exe) |
    Where-Object { $_ -match ('^' + [regex]::Escape($virtualDrive)) } |
    Select-Object -First 1
if ($existingMapping) {
    if (-not (Test-Path -LiteralPath $mappedMarker) -or
        (Get-Content -Raw -LiteralPath $mappedMarker -Encoding UTF8).Trim() -ne $markerValue) {
        throw "$virtualDrive is already used by another folder. Build stopped safely."
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
        & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-configuration-cache
        if ($LASTEXITCODE -ne 0) {
            throw 'Build or verification failed. The delivery APK was not updated.'
        }
    } finally {
        Pop-Location
    }

    $sourceApk = Join-Path $androidProject 'app\build\outputs\apk\debug\app-debug.apk'
    $deliveryFolder = Join-Path $projectRoot $deliveryNames[0]
    $deliveryApk = Join-Path $deliveryFolder $deliveryNames[1]
    New-Item -ItemType Directory -Path $deliveryFolder -Force | Out-Null
    Copy-Item -LiteralPath $sourceApk -Destination $deliveryApk -Force

    Write-Host 'All checks passed.' -ForegroundColor Green
    Write-Host "APK created: $deliveryApk"
} finally {
    if ($mappingCreated) {
        & subst.exe $virtualDrive /D
    }
}
