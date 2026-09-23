@echo off
chcp 65001 > nul
cd /d "%~dp0"

if not exist "build\PixelChaser.jar" (
    echo [ERROR] Build first: run build.ps1
    pause
    exit /b 1
)

echo Starting Pixel Chaser...
java -jar "build\PixelChaser.jar"

pause