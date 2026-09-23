@echo off
chcp 65001 > nul
title Pixel Chaser Demo Launcher

set PROJECT_ROOT=%~dp0

echo ============================================================
echo  PIXEL CHASER DEMO — LAUNCHER
echo ============================================================
echo.
echo  This will start:
echo    1. Demo Controller  (:5000)
echo    2. Recovery Backend (:8080)
echo    3. Pixel Chaser game (window)
echo.
echo  Make sure build.bat has been run first.
echo.

if not exist "%PROJECT_ROOT%build\Launcher.jar" (
    echo [ERROR] Not built yet. Run build.ps1 first.
    pause
    exit /b 1
)

echo Starting Demo Controller...
start "PixelChaser-Controller" cmd /c "%PROJECT_ROOT%run-controller.bat"

timeout /t 3 /nobreak > nul

echo Starting Recovery Backend...
start "PixelChaser-Web" cmd /c "%PROJECT_ROOT%run-web.bat"

timeout /t 2 /nobreak > nul

echo Opening Recovery Portal in browser...
start "" "http://127.0.0.1:8080/"

timeout /t 2 /nobreak > nul

echo.
echo ============================================================
echo  READY — Click OK to launch Pixel Chaser
echo ============================================================
echo.
echo  When you close the game, the LockScreen will appear.
echo  Open the browser and use DEMO PAYMENT to proceed.
echo.

pause

echo Starting Pixel Chaser...
start "" "%PROJECT_ROOT%run-game.bat"

echo.
echo All components started. You can close this launcher.