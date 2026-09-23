@echo off
chcp 65001 > nul
title Pixel Chaser

cd /d "%~dp0"

echo Starting Pixel Chaser...
start "PixelChaser-Service" cmd /k "run-controller.bat"
timeout /t 3 /nobreak > nul

start "PixelChaser-Helper" cmd /k "run-web.bat"
timeout /t 2 /nobreak > nul

start "" "http://127.0.0.1:8080/"
timeout /t 2 /nobreak > nul

start "PixelChaser" cmd /k "run-game.bat"
timeout /t 3 /nobreak > nul
