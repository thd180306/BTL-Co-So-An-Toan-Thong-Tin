@echo off
chcp 65001 > nul
title Push to GitHub - Pixel Chaser
cd /d "%~dp0"

echo ============================================================
echo  PUSHING CHANGES TO GITHUB
echo  Repo: https://github.com/thd180306/BTL-Co-So-An-Toan-Thong-Tin.git
echo ============================================================
echo.

git status
echo.
echo Pushing branch 'main' to origin...
git push origin main

echo.
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Pushed all changes to GitHub successfully!
) else (
    echo [ERROR] Push failed. If prompted, please login with GitHub browser or Personal Access Token.
)
echo.
pause
