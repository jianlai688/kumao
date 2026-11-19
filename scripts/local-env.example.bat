@echo off
REM ============================================================
REM  Local development secrets template
REM  1) copy to scripts\local-env.bat
REM  2) fill in real values
REM  3) local-env.bat is git-ignored, never commit it
REM
REM  start-all.bat calls this file automatically when it exists.
REM ============================================================

REM MySQL / Redis / RabbitMQ（本地中间件）
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=your-mysql-password"
if not defined RABBITMQ_USER set "RABBITMQ_USER=guest"
if not defined RABBITMQ_PASSWORD set "RABBITMQ_PASSWORD=your-rabbitmq-password"

REM MinIO
if not defined MINIO_ACCESS_KEY set "MINIO_ACCESS_KEY=your-minio-access-key"
if not defined MINIO_SECRET_KEY set "MINIO_SECRET_KEY=your-minio-secret-key"

REM 网关/各服务共用的 JWT 签名密钥（生产环境务必使用 32 位以上随机串）
if not defined JWT_SECRET set "JWT_SECRET=your-32-bytes-random-jwt-secret"

REM review-service 加密存储 AI 模型配置的密钥
if not defined AI_CONFIG_SECRET set "AI_CONFIG_SECRET=your-ai-config-secret"

REM 直播间推流鉴权
if not defined SRS_PUSH_KEY set "SRS_PUSH_KEY=your-srs-push-key"

REM 支付宝沙箱应用私钥（PKCS8，不带 BEGIN/END 头）
if not defined ALIPAY_PRIVATE_KEY set "ALIPAY_PRIVATE_KEY="
REM 支付宝沙箱应用公钥（用于回调验签）
if not defined ALIPAY_PUBLIC_KEY set "ALIPAY_PUBLIC_KEY="

REM 微信支付（无沙箱，未配置时自动降级为模拟支付）
if not defined WECHAT_APP_ID set "WECHAT_APP_ID="
if not defined WECHAT_MCH_ID set "WECHAT_MCH_ID="
if not defined WECHAT_MERCHANT_SERIAL_NO set "WECHAT_MERCHANT_SERIAL_NO="
if not defined WECHAT_API_V3_KEY set "WECHAT_API_V3_KEY="
if not defined WECHAT_PRIVATE_KEY_PATH set "WECHAT_PRIVATE_KEY_PATH="
