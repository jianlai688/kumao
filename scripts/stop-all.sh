#!/bin/bash
# 停止全部微服务
pkill -f 'user-service.jar' 2>/dev/null
pkill -f 'music-service.jar' 2>/dev/null
pkill -f 'playlist-service.jar' 2>/dev/null
pkill -f 'comment-service.jar' 2>/dev/null
pkill -f 'file-service.jar' 2>/dev/null
pkill -f 'live-service.jar' 2>/dev/null
pkill -f 'search-service.jar' 2>/dev/null
pkill -f 'vip-service.jar' 2>/dev/null
pkill -f 'musician-service.jar' 2>/dev/null
pkill -f 'ad-service.jar' 2>/dev/null
pkill -f 'review-service.jar' 2>/dev/null
pkill -f 'kumao-gateway.jar' 2>/dev/null
echo "已停止全部服务"
