@echo off
chcp 65001 >nul
setlocal
REM ============================================================
REM  Kumao Music - publish local stack to the internet
REM  backend micro-services -> nginx :80 -> cpolar tunnel
REM
REM  Keep this file PURE ASCII and CRLF (see deploy\README.md).
REM  Double-click it; do not paste its content into cmd.
REM ============================================================

set "ROOT=%~dp0.."
set "NGINX_HOME=D:\tools\nginx-1.30.4"
set "NGINX_CONF=kumao.conf"
set "JAVA_EXE=C:\Users\43030\.jdks\ms-17.0.17\bin\java.exe"
set "LOG_DIR=%ROOT%\logs\public"

echo ============================================================
echo   Kumao Music  -  start backend + nginx (public entry)
echo ============================================================
echo.

if not exist "%JAVA_EXE%" (
    echo [ERROR] JDK 17 not found: %JAVA_EXE%
    pause
    exit /b 1
)
if not exist "%NGINX_HOME%\nginx.exe" (
    echo [ERROR] nginx.exe not found: %NGINX_HOME%
    pause
    exit /b 1
)
if not exist "%ROOT%\kumao-web\dist\index.html" (
    echo [ERROR] frontend build missing: kumao-web\dist\index.html
    echo         run: cd kumao-web ^&^& npm run build
    pause
    exit /b 1
)
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM ---------- middleware connection settings of this machine ----------
set "NACOS_ADDR=127.0.0.1:8848"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3307"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD=kumao123456"
REM docker-published redis is reachable on IPv6 loopback only
REM (a local redis-server owns 127.0.0.1:6379)
set "REDIS_HOST=::1"
set "REDIS_PORT=6379"
set "RABBITMQ_HOST=127.0.0.1"
set "RABBITMQ_PORT=5672"
set "MINIO_ENDPOINT=http://127.0.0.1:9000"
set "ES_URIS=http://127.0.0.1:9200"
set "SRS_RTMP_HOST=127.0.0.1"
set "SRS_PLAY_HOST=/hls"
set "SRS_API_URL=http://127.0.0.1:1985"

echo [1/3] backend services
for %%S in (user-service:8101 music-service:8102 playlist-service:8103 comment-service:8104 file-service:8105 live-service:8106 search-service:8107 vip-service:8108 musician-service:8109 ad-service:8110 review-service:8111) do (
    for /f "tokens=1,2 delims=:" %%A in ("%%S") do call :svc %%B %%A
)
call :svc 8080 kumao-gateway

echo.
echo [2/3] waiting for gateway on 8080 ...
set /a WAIT=0
:waitloop
netstat -ano | findstr /R /C:":8080 " | findstr /I "LISTENING" >nul
if not errorlevel 1 goto :backend_ready
set /a WAIT+=1
if %WAIT% GEQ 60 goto :backend_timeout
ping -n 4 127.0.0.1 >nul
goto :waitloop

:backend_timeout
echo [WARN] gateway not listening after 180s. Check logs in %LOG_DIR%
goto :nginx_step

:backend_ready
echo       gateway is up.

:nginx_step
echo.
echo [3/3] nginx on port 80
tasklist /FI "IMAGENAME eq nginx.exe" 2>nul | findstr /I "nginx.exe" >nul
if not errorlevel 1 (
    echo       nginx already running - restarting to apply %NGINX_CONF%
    taskkill /F /IM nginx.exe >nul 2>&1
    ping -n 3 127.0.0.1 >nul
)

REM nginx cannot read config files under non-ASCII paths: sync to its conf dir
copy /Y "%~dp0nginx.conf" "%NGINX_HOME%\conf\%NGINX_CONF%" >nul
"%NGINX_HOME%\nginx.exe" -t -p "%NGINX_HOME%/" -c conf\%NGINX_CONF%
if errorlevel 1 (
    echo [ERROR] nginx config test failed, see messages above.
    pause
    exit /b 1
)
start "km-nginx" /d "%NGINX_HOME%" nginx.exe -p "%NGINX_HOME%/" -c conf\%NGINX_CONF%
ping -n 3 127.0.0.1 >nul

echo.
echo ============================================================
echo   Local entry : http://127.0.0.1/
echo   Public URL  : https://dashboard.cpolar.com  (Status page)
echo   Health check: curl --noproxy "*" http://127.0.0.1/api/music/banners
echo.
echo   cpolar runs as a Windows service and is NOT touched here.
echo ============================================================
pause
endlocal
exit /b 0

REM ---------- helper: start one service unless its port is taken ----------
:svc
netstat -ano | findstr /R /C:":%~1 " | findstr /I "LISTENING" >nul
if not errorlevel 1 goto :svc_running
if not exist "%ROOT%\%~2\target\%~2.jar" goto :svc_missing
echo   [START] %~2
start "km-%~2" /min "%JAVA_EXE%" -Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m -Dfile.encoding=UTF-8 -Dlogging.file.name=%LOG_DIR%\%~2.log -jar "%ROOT%\%~2\target\%~2.jar"
goto :eof

:svc_running
echo   [SKIP ] %~2 - port %~1 already busy
goto :eof

:svc_missing
echo   [ERROR] missing jar %~2\target\%~2.jar
goto :eof
