#!/bin/bash
# 酷猫音乐平台 - 一键启动全部微服务
set -e
cd "$(dirname "$0")/.."

export NACOS_ADDR=${NACOS_ADDR:-127.0.0.1:8848}
export MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
export MYSQL_PORT=${MYSQL_PORT:-3306}
export MYSQL_USER=${MYSQL_USER:-root}
export MYSQL_PASSWORD=${MYSQL_PASSWORD:-kumao123456}
export REDIS_HOST=${REDIS_HOST:-127.0.0.1}
export REDIS_PORT=${REDIS_PORT:-6379}
export RABBITMQ_HOST=${RABBITMQ_HOST:-127.0.0.1}
export MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://127.0.0.1:9000}
export ES_URIS=${ES_URIS:-http://127.0.0.1:9200}
export SRS_RTMP_HOST=${SRS_RTMP_HOST:-127.0.0.1}
export SRS_PLAY_HOST=${SRS_PLAY_HOST:-http://127.0.0.1:8088}

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
