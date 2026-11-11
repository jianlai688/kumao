package com.kumao.live.websocket;

import com.kumao.api.client.VipClient;
import com.kumao.common.security.JwtUtils;
import com.kumao.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手：解析 token 与 roomId，写入 session 属性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    private final VipClient vipClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String roomId = params.getFirst("roomId");
        if (roomId == null || !roomId.matches("\\d+")) {
            return false;
        }
        attributes.put(LiveWebSocketHandler.ATTR_ROOM_ID, Long.valueOf(roomId));
        String studio = params.getFirst("studio");
        attributes.put(LiveWebSocketHandler.ATTR_STUDIO,
                "1".equals(studio) || "true".equalsIgnoreCase(studio));

        String token = params.getFirst("token");
        if (token != null && !token.isBlank()) {
            try {
                LoginUser user = jwtUtils.parseToLoginUser(token);
                attributes.put(LiveWebSocketHandler.ATTR_USER_ID, user.getUserId());
                attributes.put(LiveWebSocketHandler.ATTR_NICKNAME, user.getUsername());
                try {
                    var r = vipClient.status(user.getUserId());
                    if (r != null && r.getData() != null && r.getData().isVip()) {
                        attributes.put(LiveWebSocketHandler.ATTR_VIP, true);
                        attributes.put(LiveWebSocketHandler.ATTR_COLOR, r.getData().getDanmakuColor());
                    }
                } catch (Exception e) {
                    log.debug("WS 握手 VIP 查询失败: {}", e.getMessage());
                }
            } catch (Exception e) {
                // 游客也允许连接，仅不能发言
                attributes.put(LiveWebSocketHandler.ATTR_NICKNAME, "游客" + (int) (Math.random() * 10000));
            }
        } else {
            attributes.put(LiveWebSocketHandler.ATTR_NICKNAME, "游客" + (int) (Math.random() * 10000));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
