@echo off
chcp 65001 > nul
cd /d "%~dp0"
java -cp "lib\Launcher.jar;lib\sqlite-jdbc.jar" ransomware.controller.DemoController
pause
