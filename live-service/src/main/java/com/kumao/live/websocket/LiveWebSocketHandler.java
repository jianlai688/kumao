package com.kumao.live.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.common.exception.BizException;
import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.mapper.LiveRoomMapper;
import com.kumao.live.service.LiveContentModerationService;
import com.kumao.live.service.OnlineCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播间弹幕 WebSocket：
 * - 进入房间更新 Redis 在线人数并向房间广播入场消息
 * - 收到文本弹幕 -> 敏感词校验 -> 房间内广播 -> 投递 MQ 异步落库
 * - 离开房间更新在线人数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_ROOM_ID = "roomId";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_NICKNAME = "nickname";
    public static final String ATTR_VIP = "vip";
    public static final String ATTR_COLOR = "color";
    public static final String ATTR_STUDIO = "studio";

    private final ObjectMapper objectMapper;
    private final OnlineCountService onlineCountService;
    private final LiveContentModerationService moderationService;
    private final RabbitTemplate rabbitTemplate;
    private final LiveRoomMapper roomMapper;
    /** roomId -> sessions */
    private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Value("${kumao.mq.danmaku-exchange:kumao.live.exchange}")
    private String danmakuExchange;
    @Value("${kumao.mq.danmaku-routing-key:live.danmaku}")
    private String danmakuRoutingKey;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = roomId(session);
        if (roomId == null) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        // 同一 session 可能被心跳、弹幕广播、指标广播等多个线程同时写入，
        // 必须用装饰器串行化发送，否则帧会交错导致对端 reset、消息随机丢失。
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 512 * 1024);
        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), safeSession);

        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        boolean studio = isStudio(session);
        if (userId != null && !studio) {
            onlineCountService.join(roomId, session.getId(), userId);
        }

        if (!studio) {
            DanmakuMessage join = DanmakuMessage.system(roomId,
                    session.getAttributes().getOrDefault(ATTR_NICKNAME, "游客") + " 进入了直播间");
            join.setType("ENTRANCE");
            join.setNickname(String.valueOf(session.getAttributes().getOrDefault(ATTR_NICKNAME, "游客")));
            join.setUserId(userId);
            join.setVip(Boolean.TRUE.equals(session.getAttributes().get(ATTR_VIP)));
            join.setColor((String) session.getAttributes().get(ATTR_COLOR));
            broadcast(join);
        }
        broadcastRoomMetrics(roomId);
        // 新连接可能错过广播时机的竞态，再定向发一份快照，保证首屏立即拿到真实状态。
        sendRoomMetrics(session);
        log.info("WebSocket 连接建立 room={} session={} 登录用户={} 工作室连接={} 当前在线={}",
                roomId, session.getId(), userId, studio, onlineCountService.count(roomId));
    }

    /**
     * 网关（Spring Cloud Gateway WebSocket 代理）会把客户端帧转成二进制帧转发，
     * 这里统一按 UTF-8 文本解析，保证手机端 / App 端发弹幕与浏览器一致可用。
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer buffer = message.getPayload();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String payload = new String(bytes, StandardCharsets.UTF_8);
        log.debug("收到二进制帧 room={} bytes={}", roomId(session), bytes.length);
        try {
            handleTextMessage(session, new TextMessage(payload));
        } catch (Exception e) {
            log.warn("处理二进制帧失败 room={}: {}", roomId(session), e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {        Long roomId = roomId(session);
        if (roomId == null) {
            return;
        }
        String payload = message.getPayload();
        // 心跳
        if ("ping".equalsIgnoreCase(payload.trim())) {
            onlineCountService.touch(roomId);
            session.sendMessage(new TextMessage("pong"));
            sendRoomMetrics(session);
            return;
        }
        DanmakuMessage in;
        try {
            in = objectMapper.readValue(payload, DanmakuMessage.class);
        } catch (Exception e) {
            send(session, DanmakuMessage.system(roomId, "消息格式错误"));
            return;
        }
        in.setRoomId(roomId);
        in.setUserId((Long) session.getAttributes().get(ATTR_USER_ID));
        in.setNickname(String.valueOf(session.getAttributes().getOrDefault(ATTR_NICKNAME, "游客")));
        in.setVip(Boolean.TRUE.equals(session.getAttributes().get(ATTR_VIP)));
        if (in.getColor() == null || !Boolean.TRUE.equals(in.getVip())) {
            in.setColor((String) session.getAttributes().getOrDefault(ATTR_COLOR, "#FFFFFF"));
        }
        in.setType("TEXT");
        in.setTimestamp(System.currentTimeMillis());
        if (in.getContent() == null || in.getContent().isBlank()) {
            send(session, DanmakuMessage.system(roomId, "弹幕内容不能为空"));
            return;
        }
        if (in.getContent().length() > 100) {
            send(session, DanmakuMessage.system(roomId, "弹幕内容不能超过100字"));
            return;
        }
        try {
            moderationService.checkText(in.getUserId(), roomId, in.getContent());
        } catch (BizException e) {
            send(session, DanmakuMessage.system(roomId, e.getMessage()));
            return;
        }
        broadcast(in);
        // 弹幕投递 RabbitMQ 异步持久化
        try {
            rabbitTemplate.convertAndSend(danmakuExchange, danmakuRoutingKey, in);
        } catch (Exception e) {
            log.warn("弹幕投递 MQ 失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = roomId(session);
        if (roomId == null) {
            return;
        }
        Map<String, WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                rooms.remove(roomId);
            }
        }
        onlineCountService.leave(roomId, session.getId());
        if (!isStudio(session)) {
            DanmakuMessage leave = DanmakuMessage.system(roomId,
                    session.getAttributes().getOrDefault(ATTR_NICKNAME, "游客") + " 离开了直播间");
            leave.setType("ENTRANCE");
            broadcast(leave);
        }
        broadcastRoomMetrics(roomId);
        log.info("WebSocket 断开 room={} session={} 当前在线={}", roomId, session.getId(), onlineCountService.count(roomId));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输异常: {}", exception.getMessage());
    }

    public void broadcast(DanmakuMessage message) {
        Map<String, WebSocketSession> sessions = rooms.get(message.getRoomId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        TextMessage text = new TextMessage(json);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(text);
                } catch (Exception e) {
                    log.debug("推送失败 session={}: {}", s.getId(), e.getMessage());
                }
            }
        }
    }

    /** 推流状态变化：通知房间内播放器立即接入或断开 HTTP-FLV/HLS。 */
    public void broadcastStreamStatus(Long roomId, boolean online) {
        DanmakuMessage message = DanmakuMessage.system(roomId, online ? "ONLINE" : "OFFLINE");
        message.setType("STREAM_STATUS");
        broadcast(message);
    }

    /** 广播真实房间指标：在线、累计观看、峰值、点赞、礼物和推流信息。 */
    public void broadcastRoomMetrics(Long roomId) {
        DanmakuMessage message = buildRoomMetrics(roomId);
        if (message != null) {
            broadcast(message);
        }
    }

    /** 给单个连接补发房间快照（连接建立、心跳时使用）。 */
    public void sendRoomMetrics(WebSocketSession session) {
        Long roomId = roomId(session);
        if (roomId == null || session == null || !session.isOpen()) {
            return;
        }
        DanmakuMessage message = buildRoomMetrics(roomId);
        if (message != null) {
            send(session, message);
        }
    }

    private DanmakuMessage buildRoomMetrics(Long roomId) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            return null;
        }
        long online = onlineCountService.count(roomId);
        long peak = Math.max(onlineCountService.peak(roomId), online);
        DanmakuMessage message = new DanmakuMessage();
        message.setRoomId(roomId);
        message.setType("ROOM_METRICS");
        message.setOnlineCount(online);
        message.setTotalViewers(onlineCountService.totalViewers(roomId));
        message.setPeakOnline(peak);
        message.setLikeCount(room.getLikeCount() == null ? 0L : room.getLikeCount());
        message.setGiftIncome(room.getGiftIncome() == null ? 0L : room.getGiftIncome());
        message.setRoomStatus(room.getStatus());
        message.setPullUrl(room.getPullUrl());
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    public long onlineCount(Long roomId) {
        return onlineCountService.count(roomId);
    }

    private boolean isStudio(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_STUDIO);
        return Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value))
                || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private void send(WebSocketSession session, DanmakuMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
        }
    }

    private Long roomId(WebSocketSession session) {
        Object roomId = session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(roomId));
    }
}
