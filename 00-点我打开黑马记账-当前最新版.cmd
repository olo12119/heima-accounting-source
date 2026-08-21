@echo off
chcp 65001 >nul
setlocal
title Heima Accounting 1.4.0 Preview
set "ELECTRON_RUN_AS_NODE="

set "APP_EXE="
for /d %%D in ("%~dp0*1.4.0*") do if exist "%%~fD\win-unpacked\HeimaAccounting.exe" set "APP_EXE=%%~fD\win-unpacked\HeimaAccounting.exe"

if "%HEIMA_LAUNCHER_CHECK%"=="1" (
  if defined APP_EXE echo FOUND=%APP_EXE%
  if not defined APP_EXE echo MISSING
  if defined APP_EXE exit /b 0
  exit /b 1
)

if defined APP_EXE (
  start "" "%APP_EXE%"
  exit /b 0
)

echo The unpacked preview was not found. Starting from source code.
echo Keep this window open until the app is closed.
echo.

if not exist "%~dp0node_modules\electron\dist\electron.exe" (
  echo Project dependencies are missing. Please show this message to Codex.
  echo.
  pause
  exit /b 1
)

pushd "%~dp0"
call npm.cmd run dev
set "START_RESULT=%ERRORLEVEL%"
popd

if not "%START_RESULT%"=="0" (
  echo.
  echo The app did not start correctly. Please show this message to Codex.
  pause
)
exit /b %START_RESULT%
