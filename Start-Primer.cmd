@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-app.ps1" %*
if errorlevel 1 (
    echo.
    echo Primer failed to start. Review the message above.
    pause
    exit /b 1
)
