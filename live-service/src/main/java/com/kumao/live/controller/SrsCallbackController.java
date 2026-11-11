package com.kumao.live.controller;

import com.kumao.common.result.R;
import com.kumao.live.entity.LiveRoom;
import com.kumao.live.mapper.LiveRoomMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SRS 回调：录制完成生成回放地址
 */
@Slf4j
@RestController
@RequestMapping("/live/rooms")
@RequiredArgsConstructor
public class SrsCallbackController {

    private final LiveRoomMapper roomMapper;
    private final LiveWebSocketHandler webSocketHandler;

    @Value("${kumao.srs.play-host:http://127.0.0.1:8088}")
    private String playHost;

    /**
     * SRS on_publish / on_unpublish 回调。通过 WebSocket 实时通知房间内播放器。
     */
    @PostMapping("/publish-callback")
    public R<Void> publishCallback(@RequestBody Map<String, Object> body) {
        String action = String.valueOf(body.getOrDefault("action", ""));
        String stream = String.valueOf(body.getOrDefault("stream", ""));
        Long roomId = parseRoomId(stream);
        if (roomId == null) return R.ok();
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) return R.ok();
        boolean online = "on_publish".equals(action);
        if (online) {
            room.setStatus("LIVING");
        } else if ("on_unpublish".equals(action)) {
            room.setStatus("ENDED");
            room.setOnlineCount(0L);
        }
        roomMapper.updateById(room);
        webSocketHandler.broadcastStreamStatus(roomId, online);
        log.info("SRS 推流状态回调 room={} action={} online={}", roomId, action, online);
        return R.ok();
    }

    /**
     * SRS on_dvr 回调：{"stream":"room_1","file":"./objs/nginx/html/replay/live/room_1.123.mp4"}
     */
    @PostMapping("/dvr-callback")
    public R<Void> dvrCallback(@RequestBody Map<String, Object> body) {
        log.info("收到 SRS 录制回调: {}", body);
        String stream = String.valueOf(body.getOrDefault("stream", ""));
        String file = String.valueOf(body.getOrDefault("file", ""));
        Long roomId = parseRoomId(stream);
        if (roomId == null) {
            return R.ok();
        }
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            return R.ok();
        }
        int idx = file.indexOf("/replay/");
        String relative = idx >= 0 ? file.substring(idx + 1) : file;
        room.setReplayUrl(playHost.replaceAll("/$", "") + "/" + relative);
        if ("LIVING".equals(room.getStatus())) {
            room.setStatus("ENDED");
            room.setOnlineCount(0L);
        }
        roomMapper.updateById(room);
        return R.ok();
    }

    @PostMapping("/hls-callback")
    public R<Void> hlsCallback(@RequestBody Map<String, Object> body) {
        log.debug("SRS HLS 回调: {}", body);
        return R.ok();
    }

    private Long parseRoomId(String stream) {
        if (stream == null || !stream.startsWith("room_")) {
            return null;
        }
        try {
            return Long.parseLong(stream.substring(5));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
