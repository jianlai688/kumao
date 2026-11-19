@echo off
chcp 65001 >nul
setlocal
REM ============================================================
REM  Kumao Music - stop the public entry (nginx + backend jars)
REM  cpolar is a Windows service and is left running on purpose.
REM  Keep this file PURE ASCII + CRLF.
REM ============================================================

set "NGINX_HOME=D:\tools\nginx-1.30.4"
set "NGINX_CONF=kumao.conf"

echo Stopping nginx ...
if exist "%NGINX_HOME%\nginx.exe" (
    taskkill /F /IM nginx.exe >nul 2>&1
)
tasklist /FI "IMAGENAME eq nginx.exe" 2>nul | findstr /I "nginx.exe" >nul
if not errorlevel 1 taskkill /F /IM nginx.exe >nul 2>&1
echo   nginx stopped.

echo Stopping backend services ...
for %%P in (8080 8101 8102 8103 8104 8105 8106 8107 8108 8109 8110 8111) do call :killport %%P
echo   backend stopped.
echo.
echo Done. cpolar service is still running.
pause
endlocal
exit /b 0

:killport
for /f "tokens=5" %%A in ('netstat -ano ^| findstr /R /C:":%~1 " ^| findstr /I "LISTENING"') do (
    taskkill /F /PID %%A >nul 2>&1
    echo   [STOP ] port %~1 pid %%A
)
goto :eof
