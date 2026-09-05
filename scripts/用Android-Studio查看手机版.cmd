@echo off
chcp 65001 >nul

set "ANDROID_HOME=D:\AndroidDev\Sdk"
set "ANDROID_USER_HOME=D:\AndroidDev\AndroidUserHome"
set "ANDROID_AVD_HOME=D:\AndroidDev\Avd"
set "GRADLE_USER_HOME=D:\AndroidDev\GradleCache"
set "STUDIO_GRADLE_JDK=D:\AndroidDev\Jdk\jdk-17.0.20.1+1"
set "STUDIO_PROPERTIES=%~dp0apps\android\.studio\idea.properties"

set "HEIMA_STUDIO=D:\AndroidDev\AndroidStudio\bin\studio64.exe"
set "HEIMA_ANDROID_PROJECT=%~dp0apps\android"

if not exist "%HEIMA_STUDIO%" (
  echo Android Studio was not found at:
  echo %HEIMA_STUDIO%
  echo.
  pause
  exit /b 1
)

start "" "%HEIMA_STUDIO%" "%HEIMA_ANDROID_PROJECT%"

