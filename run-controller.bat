@echo off
chcp 65001 > nul
cd /d "%~dp0"

if not exist "build\Launcher.jar" (
    echo [ERROR] Build first: run build.ps1
    pause
    exit /b 1
)

echo Starting Demo Controller on :5000...
java -cp "build\Launcher.jar;build\sqlite-jdbc.jar" ransomware.controller.DemoController

pause