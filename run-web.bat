@echo off
chcp 65001 > nul
cd /d "%~dp0"

if not exist "build\Helper.jar" (
    echo [ERROR] Build first: run build.ps1
    pause
    exit /b 1
)

echo Starting Recovery Backend on :8080...
java -cp "build\Helper.jar;build\sqlite-jdbc.jar" ransomware.web.RecoveryBackend

pause