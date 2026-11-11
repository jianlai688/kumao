package com.kumao.musician.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 新歌发布通知：按音乐人分组推送
 * 连接：ws://host/ws/musician/notice?musicianId=13001
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewSongWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_MUSICIAN_ID = "musicianId";

    private final ObjectMapper objectMapper;
    /** musicianId -> sessions */
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long musicianId = musicianId(session);
        if (musicianId == null) {
            return;
        }
        sessions.computeIfAbsent(musicianId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("新歌通知连接 musicianId={} session={}", musicianId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long musicianId = musicianId(session);
        if (musicianId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(musicianId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(musicianId);
            }
        }
    }

    /**
     * 推送新歌发布通知给关注该音乐人的在线用户
     */
    public void notifyNewSong(Long musicianId, String songName) {
        Set<WebSocketSession> set = sessions.get(musicianId);
        if (set == null || set.isEmpty()) {
            return;
        }
        Map<String, Object> payload = Map.of(
                "type", "NEW_SONG",
                "musicianId", musicianId,
                "songName", songName == null ? "" : songName,
                "timestamp", System.currentTimeMillis());
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession s : set) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.debug("新歌通知推送失败: {}", e.getMessage());
                }
            }
        }
    }

    private Long musicianId(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_MUSICIAN_ID);
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }
}
