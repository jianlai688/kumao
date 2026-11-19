#!/bin/bash
# 酷猫音乐平台 - 一键启动全部微服务
set -e
cd "$(dirname "$0")/.."

# 本地开发密钥（可选）：cp scripts/local-env.example.sh scripts/local-env.sh 后填值
# 该文件被 .gitignore 忽略，用于存放支付宝私钥等敏感凭据
if [ -f "$(dirname "$0")/local-env.sh" ]; then
  # shellcheck disable=SC1091
  . "$(dirname "$0")/local-env.sh"
fi

export NACOS_ADDR=${NACOS_ADDR:-127.0.0.1:8848}
export MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
export MYSQL_PORT=${MYSQL_PORT:-3306}
export MYSQL_USER=${MYSQL_USER:-root}
export MYSQL_PASSWORD=${MYSQL_PASSWORD:-kumao123456}
export REDIS_HOST=${REDIS_HOST:-127.0.0.1}
export REDIS_PORT=${REDIS_PORT:-6379}
export RABBITMQ_HOST=${RABBITMQ_HOST:-127.0.0.1}
export RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
export RABBITMQ_USER=${RABBITMQ_USER:-guest}
export RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}
export MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://127.0.0.1:9000}
export MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}
export MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin}
export ES_URIS=${ES_URIS:-http://127.0.0.1:9200}
export SRS_RTMP_HOST=${SRS_RTMP_HOST:-127.0.0.1}
export SRS_PLAY_HOST=${SRS_PLAY_HOST:-http://127.0.0.1:8088}
export SRS_PUSH_KEY=${SRS_PUSH_KEY:-kumao-srs-push-key}

# 以下两项仅为本地开发默认值，生产/公网部署必须通过环境变量覆盖
export JWT_SECRET=${JWT_SECRET:-kumao-local-dev-jwt-secret-change-me}
export AI_CONFIG_SECRET=${AI_CONFIG_SECRET:-kumao-local-dev-ai-config-secret}

mkdir -p logs
SERVICES=(user-service music-service playlist-service comment-service file-service live-service search-service vip-service musician-service ad-service review-service)

for svc in "${SERVICES[@]}"; do
  echo "[启动] $svc"
  nohup java -jar "$svc/target/$svc.jar" > "logs/$svc.log" 2>&1 &
  sleep 2
done

echo "[启动] kumao-gateway"
nohup java -jar kumao-gateway/target/kumao-gateway.jar > logs/kumao-gateway.log 2>&1 &

echo ""
echo "全部服务已启动，日志目录: logs/"
echo "网关地址: http://127.0.0.1:8080"
