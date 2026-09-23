@echo off
chcp 65001 > nul
cd /d "%~dp0"
set "JAVA_BIN=java"
if exist "%~dp0..\runtime\bin\java.exe" set "JAVA_BIN=%~dp0..\runtime\bin\java.exe"
if exist "%~dp0runtime\bin\java.exe" set "JAVA_BIN=%~dp0runtime\bin\java.exe"
"%JAVA_BIN%" -cp "lib\Launcher.jar;lib\sqlite-jdbc.jar" ransomware.controller.DemoController
