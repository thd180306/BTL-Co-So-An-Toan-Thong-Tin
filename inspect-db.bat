@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo ============================================================
echo  Pixel Chaser — Database Live Inspector
echo ============================================================
java -cp "build\Launcher.jar;build\sqlite-jdbc.jar" ransomware.session.db.DbInspector
echo.
pause
