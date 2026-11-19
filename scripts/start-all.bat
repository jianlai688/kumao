@echo off
chcp 65001 >nul
echo ==========================================
echo   酷猫音乐平台 - 启动全部微服务
echo ==========================================
cd /d %~dp0\..

REM 本地开发密钥（可选）：首次使用请复制 local-env.example.bat 为 local-env.bat
REM 该文件不入库，用于存放支付宝私钥等真正敏感的凭据
if exist "%~dp0local-env.bat" call "%~dp0local-env.bat"

if not defined NACOS_ADDR set NACOS_ADDR=127.0.0.1:8848
if not defined MYSQL_HOST set MYSQL_HOST=127.0.0.1
if not defined MYSQL_PORT set MYSQL_PORT=3306
if not defined MYSQL_USER set MYSQL_USER=root
if not defined MYSQL_PASSWORD set MYSQL_PASSWORD=kumao123456
if not defined REDIS_HOST set REDIS_HOST=127.0.0.1
if not defined REDIS_PORT set REDIS_PORT=6379
if not defined RABBITMQ_HOST set RABBITMQ_HOST=127.0.0.1
if not defined RABBITMQ_PORT set RABBITMQ_PORT=5672
if not defined RABBITMQ_USER set RABBITMQ_USER=guest
if not defined RABBITMQ_PASSWORD set RABBITMQ_PASSWORD=guest
if not defined MINIO_ENDPOINT set MINIO_ENDPOINT=http://127.0.0.1:9000
if not defined MINIO_ACCESS_KEY set MINIO_ACCESS_KEY=minioadmin
if not defined MINIO_SECRET_KEY set MINIO_SECRET_KEY=minioadmin
if not defined ES_URIS set ES_URIS=http://127.0.0.1:9200
if not defined SRS_RTMP_HOST set SRS_RTMP_HOST=127.0.0.1
if not defined SRS_PLAY_HOST set SRS_PLAY_HOST=http://127.0.0.1:8088
if not defined SRS_PUSH_KEY set SRS_PUSH_KEY=kumao-srs-push-key

REM 以下两项仅为本地开发默认值，生产/公网部署必须通过环境变量覆盖
if not defined JWT_SECRET set JWT_SECRET=kumao-local-dev-jwt-secret-change-me
if not defined AI_CONFIG_SECRET set AI_CONFIG_SECRET=kumao-local-dev-ai-config-secret

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
