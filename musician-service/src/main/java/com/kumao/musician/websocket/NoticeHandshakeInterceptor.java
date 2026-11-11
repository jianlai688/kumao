package com.kumao.musician.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 新歌通知握手：解析 musicianId 写入 session
 */
@Component
public class NoticeHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String musicianId = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("musicianId");
        if (musicianId == null || !musicianId.matches("\\d+")) {
            return false;
        }
        attributes.put(NewSongWebSocketHandler.ATTR_MUSICIAN_ID, Long.valueOf(musicianId));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
