@echo off
chcp 65001 >nul
setlocal
for %%F in ("%~dp0*Windows*1.5.0.cmd") do (
  call "%%~fF"
  exit /b
)
echo The Windows launcher was not found.
pause
exit /b 1
