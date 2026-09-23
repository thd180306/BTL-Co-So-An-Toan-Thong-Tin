@echo off
chcp 65001 > nul

set PROJECT_ROOT=%~dp0
set BUILD=%PROJECT_ROOT%build

if not exist "%BUILD%\Helper.jar" (
    echo [ERROR] Build first: run build.ps1
    pause
    exit /b 1
)

echo Starting Recovery Backend on :8080...
java -cp "%BUILD%\Helper.jar;%BUILD%\sqlite-jdbc.jar" ransomware.web.RecoveryBackend

pause