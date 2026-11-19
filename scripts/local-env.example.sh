#!/bin/bash
# ============================================================
#  本地开发密钥模板（macOS / Linux）
#  1) cp scripts/local-env.example.sh scripts/local-env.sh
#  2) 填入真实值
#  3) local-env.sh 已被 .gitignore 忽略，不要提交
#
#  start-all.sh 存在该文件时会自动 source。
# ============================================================

: "${MYSQL_PASSWORD:=your-mysql-password}"
: "${RABBITMQ_USER:=guest}"
: "${RABBITMQ_PASSWORD:=your-rabbitmq-password}"
: "${MINIO_ACCESS_KEY:=your-minio-access-key}"
: "${MINIO_SECRET_KEY:=your-minio-secret-key}"
: "${JWT_SECRET:=your-32-bytes-random-jwt-secret}"
: "${AI_CONFIG_SECRET:=your-ai-config-secret}"
: "${SRS_PUSH_KEY:=your-srs-push-key}"
: "${ALIPAY_PRIVATE_KEY:=}"
: "${ALIPAY_PUBLIC_KEY:=}"
: "${WECHAT_APP_ID:=}"
: "${WECHAT_MCH_ID:=}"
: "${WECHAT_MERCHANT_SERIAL_NO:=}"
: "${WECHAT_API_V3_KEY:=}"
: "${WECHAT_PRIVATE_KEY_PATH:=}"

export MYSQL_PASSWORD RABBITMQ_USER RABBITMQ_PASSWORD
export MINIO_ACCESS_KEY MINIO_SECRET_KEY JWT_SECRET AI_CONFIG_SECRET SRS_PUSH_KEY
export ALIPAY_PRIVATE_KEY ALIPAY_PUBLIC_KEY
export WECHAT_APP_ID WECHAT_MCH_ID WECHAT_MERCHANT_SERIAL_NO WECHAT_API_V3_KEY WECHAT_PRIVATE_KEY_PATH
