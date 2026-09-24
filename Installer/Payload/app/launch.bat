@echo off
chcp 65001 > nul
title Pixel Chaser

cd /d "%~dp0"

echo Starting Pixel Chaser Services...
start "PixelChaser-Controller" cmd /c "run-controller.bat"
timeout /t 3 /nobreak > nul

start "PixelChaser-Web" cmd /c "run-web.bat"
timeout /t 2 /nobreak > nul

echo Opening Attacker C2 Dashboard in separate browser window...
if exist "open-portals.vbs" (
    cscript //nologo open-portals.vbs
) else (
    start "" "http://127.0.0.1:8080/c2.html"
)
timeout /t 2 /nobreak > nul

echo Launching Pixel Chaser Game...
start "PixelChaser" cmd /c "run-game.bat"
