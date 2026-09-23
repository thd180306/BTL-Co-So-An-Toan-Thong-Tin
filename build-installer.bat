@echo off
chcp 65001 > nul
title Building Pixel Chaser Installer

set "PROJECT_ROOT=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%build-installer.ps1"

echo.
pause
