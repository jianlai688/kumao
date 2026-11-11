package com.kumao.live.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.mapper.LiveRoomMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轮询 SRS 活跃流，并通过 WebSocket 通知房间播放器。
 *
 * <p>SRS 的 HTTP 回调在 Docker 网络下不一定能访问宿主机，因此使用 SRS HTTP API
 * 作为可靠的状态源；页面无需轮询，开播/断流状态由 WebSocket 实时下发。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamStatusMonitor {

    private final LiveRoomMapper roomMapper;
    private final LiveWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Value("${kumao.srs.api-url:http://127.0.0.1:1985}")
    private String srsApiUrl;

    /** 上一轮仍在推流的房间，用于识别断流。 */
    private final Map<Long, Boolean> onlineRooms = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${kumao.srs.status-interval-ms:3000}")
    public void poll() {
        Set<Long> current;
        try {
            current = fetchActiveRoomIds();
        } catch (Exception e) {
            // SRS 短暂不可用时不误判断流，等下一轮。
            log.debug("读取 SRS 活跃流失败: {}", e.getMessage());
            return;
        }

        for (Long roomId : current) {
            if (!onlineRooms.containsKey(roomId)) {
                onlineRooms.put(roomId, true);
                updateRoomStatus(roomId, "LIVING");
                webSocketHandler.broadcastStreamStatus(roomId, true);
            }
        }
        for (Long roomId : new HashSet<>(onlineRooms.keySet())) {
            if (!current.contains(roomId)) {
                onlineRooms.remove(roomId);
                updateRoomStatus(roomId, "ENDED");
                webSocketHandler.broadcastStreamStatus(roomId, false);
            }
        }
    }

    private Set<Long> fetchActiveRoomIds() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(1500);
        restTemplate.setRequestFactory(factory);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = restTemplate.exchange(
                srsApiUrl.replaceAll("/$", "") + "/api/v1/streams",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        Set<Long> result = new HashSet<>();
        JsonNode streams = root.path("streams");
        if (streams.isArray()) {
            for (JsonNode stream : streams) {
                String name = stream.path("name").asText("");
                if (name.startsWith("room_")) {
                    try {
                        result.add(Long.parseLong(name.substring(5)));
                    } catch (NumberFormatException ignored) {
                        // 忽略非房间流
                    }
                }
            }
        }
        return result;
    }

    private void updateRoomStatus(Long roomId, String status) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) return;
        room.setStatus(status);
        if ("LIVING".equals(status) && room.getLiveStartedAt() == null) {
            room.setLiveStartedAt(LocalDateTime.now());
        }
        if ("ENDED".equals(status)) room.setOnlineCount(0L);
        roomMapper.updateById(room);
    }
}
