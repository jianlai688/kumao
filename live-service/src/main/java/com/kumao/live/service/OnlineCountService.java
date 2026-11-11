package com.kumao.live.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 直播间观看统计：
 * - 在线人数只统计已登录用户，且同一用户多开只算一人
 * - 累计观看按本场直播去重后的登录用户统计
 * - 峰值在线只记录本场真实峰值
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineCountService {

    private static final String ONLINE_KEY = "kumao:live:online:";
    private static final String VIEWERS_KEY = "kumao:live:viewers:";
    private static final String PEAK_KEY = "kumao:live:peak:";
    private static final Duration ONLINE_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    /**
     * 服务重启后旧 WebSocket session 已全部失效，清理残留在线连接，避免虚高。
     */
    @PostConstruct
    public void clearStaleOnlineSessions() {
        try {
            Set<String> keys = redisTemplate.keys(ONLINE_KEY + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清理 OBS/直播服务重启前的在线连接缓存 {} 个", keys.size());
            }
        } catch (Exception e) {
            log.warn("清理在线连接缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 记录一个已登录用户的观看连接。
     * Hash 的 field 是 WebSocket sessionId，value 是 userId，便于同一用户多开时去重。
     */
    public void join(Long roomId, String sessionId, Long userId) {
        if (roomId == null || sessionId == null || userId == null) {
            return;
        }
        String onlineKey = ONLINE_KEY + roomId;
        redisTemplate.opsForHash().put(onlineKey, sessionId, String.valueOf(userId));
        redisTemplate.expire(onlineKey, ONLINE_TTL);
        redisTemplate.opsForSet().add(VIEWERS_KEY + roomId, String.valueOf(userId));

        long current = count(roomId);
        if (current > peak(roomId)) {
            redisTemplate.opsForValue().set(PEAK_KEY + roomId, String.valueOf(current));
        }
    }

    /** 断开某个观看连接；同一用户的其他连接仍会保留在线。 */
    public void leave(Long roomId, String sessionId) {
        if (roomId == null || sessionId == null) {
            return;
        }
        redisTemplate.opsForHash().delete(ONLINE_KEY + roomId, sessionId);
    }

    /** 心跳时续期，避免异常断线留下长期脏数据。 */
    public void touch(Long roomId) {
        if (roomId != null) {
            redisTemplate.expire(ONLINE_KEY + roomId, ONLINE_TTL);
        }
    }

    /** 当前在线人数：按登录 userId 去重。 */
    public long count(Long roomId) {
        if (roomId == null) {
            return 0;
        }
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(ONLINE_KEY + roomId);
        return entries.values().stream()
                .map(String::valueOf)
                .distinct()
                .count();
    }

    /** 本场累计观看人数：按登录 userId 去重。 */
    public long totalViewers(Long roomId) {
        if (roomId == null) {
            return 0;
        }
        Long size = redisTemplate.opsForSet().size(VIEWERS_KEY + roomId);
        return size == null ? 0 : size;
    }

    /** 本场峰值在线。 */
    public long peak(Long roomId) {
        if (roomId == null) {
            return 0;
        }
        String value = redisTemplate.opsForValue().get(PEAK_KEY + roomId);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 开播/下播时清空本场统计。 */
    public void clear(Long roomId) {
        if (roomId != null) {
            redisTemplate.delete(java.util.List.of(
                    ONLINE_KEY + roomId,
                    VIEWERS_KEY + roomId,
                    PEAK_KEY + roomId
            ));
        }
    }
}
