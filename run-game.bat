@echo off
chcp 65001 > nul

set PROJECT_ROOT=%~dp0
set BUILD=%PROJECT_ROOT%build

if not exist "%BUILD%\PixelChaser.jar" (
    echo [ERROR] Build first: run build.bat
    pause
    exit /b 1
)

echo Starting Pixel Chaser...
java -jar "%BUILD%\PixelChaser.jar"

pause