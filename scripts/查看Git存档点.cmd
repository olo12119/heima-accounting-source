@echo off
chcp 65001 >nul
setlocal
title Heima Accounting - Git Save Points

echo ================================================
echo Git save points - newest save is at the top
echo ================================================
echo.

where git >nul 2>nul
if errorlevel 1 (
  echo Git was not found. Please show this message to Codex.
  echo.
  pause
  exit /b 1
)

pushd "%~dp0"
git log --oneline --decorate --graph --all
popd

echo.
echo The first 7 characters on each line are the save ID.
echo HEAD -^> main means this is the current save route.
echo This window is read-only and does not change project files.
echo.
pause
