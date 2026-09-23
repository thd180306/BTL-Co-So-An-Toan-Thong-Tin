@echo off
chcp 65001 > nul

set PROJECT_ROOT=%~dp0
set LAB=%PROJECT_ROOT%Lab_sandbox

echo ============================================================
echo  Reset Demo State
echo ============================================================
echo.
echo  This will delete:
echo    - sessions.db
echo    - Lab_sandbox\working\* contents
echo    - Lab_sandbox\restored\* contents
echo.
echo  Baseline will NOT be touched.
echo.

set /p CONFIRM="Type YES to confirm reset: "
if defined CONFIRM set CONFIRM=%CONFIRM: =%
if /i not "%CONFIRM%"=="YES" (
    echo Aborted.
    pause
    exit /b 0
)

echo.
echo [1/3] Deleting sessions.db...
set DB_FOUND=0
if exist "%PROJECT_ROOT%Ransomware\sessions.db" (
    del /F /Q "%PROJECT_ROOT%Ransomware\sessions.db"
    set DB_FOUND=1
)
if exist "%PROJECT_ROOT%build\sessions.db" (
    del /F /Q "%PROJECT_ROOT%build\sessions.db"
    set DB_FOUND=1
)
if exist "%PROJECT_ROOT%sessions.db" (
    del /F /Q "%PROJECT_ROOT%sessions.db"
    set DB_FOUND=1
)
if "%DB_FOUND%"=="1" (
    echo   [OK]
) else (
    echo   [SKIP] Not found
)

echo [2/3] Clearing Lab_sandbox\working\...
if exist "%LAB%\working" (
    for /d %%D in ("%LAB%\working\*") do rmdir /S /Q "%%D" 2>nul
    echo   [OK]
)

echo [3/3] Clearing Lab_sandbox\restored\...
if exist "%LAB%\restored" (
    for /d %%D in ("%LAB%\restored\*") do rmdir /S /Q "%%D" 2>nul
    echo   [OK]
)

echo.
echo ============================================================
echo  RESET COMPLETE
echo ============================================================
echo.
echo  Baseline is intact at: %LAB%\baseline
dir /B "%LAB%\baseline"
echo.

pause