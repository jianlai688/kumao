@echo off
chcp 65001 >nul
echo ==========================================
echo   酷猫音乐平台 - 启动全部微服务
echo ==========================================
cd /d %~dp0\..

if not defined NACOS_ADDR set NACOS_ADDR=127.0.0.1:8848
if not defined MYSQL_HOST set MYSQL_HOST=127.0.0.1
if not defined MYSQL_PORT set MYSQL_PORT=3306
if not defined MYSQL_USER set MYSQL_USER=root
if not defined MYSQL_PASSWORD set MYSQL_PASSWORD=kumao123456
if not defined REDIS_HOST set REDIS_HOST=127.0.0.1
if not defined REDIS_PORT set REDIS_PORT=6379
if not defined RABBITMQ_HOST set RABBITMQ_HOST=127.0.0.1
if not defined RABBITMQ_PORT set RABBITMQ_PORT=5672
if not defined MINIO_ENDPOINT set MINIO_ENDPOINT=http://127.0.0.1:9000
if not defined ES_URIS set ES_URIS=http://127.0.0.1:9200
if not defined SRS_RTMP_HOST set SRS_RTMP_HOST=127.0.0.1
if not defined SRS_PLAY_HOST set SRS_PLAY_HOST=http://127.0.0.1:8088

for %%S in (user-service music-service playlist-service comment-service file-service live-service search-service vip-service musician-service ad-service review-service) do (
    echo [启动] %%S
    start "kumao-%%S" java -jar %%S\target\%%S.jar
    timeout /t 2 >nul
)

echo [启动] kumao-gateway
start "kumao-gateway" java -jar kumao-gateway\target\kumao-gateway.jar

echo.
echo 全服务已启动，网关: http://127.0.0.1:8080
echo 前端: cd kumao-web ^&^& npm run dev
pause
