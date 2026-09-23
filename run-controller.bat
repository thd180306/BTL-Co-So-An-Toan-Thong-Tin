@echo off
chcp 65001 > nul

set PROJECT_ROOT=%~dp0
set BUILD=%PROJECT_ROOT%build

if not exist "%BUILD%\Launcher.jar" (
    echo [ERROR] Build first: run build.ps1
    pause
    exit /b 1
)

echo Starting Demo Controller on :5000...
java -cp "%BUILD%\Launcher.jar;%BUILD%\sqlite-jdbc.jar" ransomware.controller.DemoController

pause