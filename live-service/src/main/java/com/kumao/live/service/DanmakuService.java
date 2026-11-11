package com.kumao.live.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kumao.common.dto.PageQuery;
import com.kumao.common.exception.BizException;
import com.kumao.common.result.PageResult;
import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.entity.LiveDanmaku;
import com.kumao.live.mapper.LiveDanmakuMapper;
import com.kumao.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final LiveDanmakuMapper danmakuMapper;
    private final LiveContentModerationService moderationService;
    private final RabbitTemplate rabbitTemplate;
    private final LiveWebSocketHandler webSocketHandler;

    @Value("${kumao.mq.danmaku-exchange:kumao.live.exchange}")
    private String exchange;
    @Value("${kumao.mq.danmaku-routing-key:live.danmaku}")
    private String routingKey;

    /**
     * 校验 -> 广播 -> MQ 持久化
     */
    public void broadcastAndPersist(DanmakuMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        if (message.getRoomId() == null) {
            throw new BizException("直播间ID不能为空");
        }
        if ("TEXT".equals(message.getType())) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                throw new BizException("弹幕内容不能为空");
            }
            if (message.getContent().length() > 100) {
                throw new BizException("弹幕内容不能超过100字");
            }
            moderationService.checkText(message.getUserId(), message.getRoomId(), message.getContent());
        }
        webSocketHandler.broadcast(message);
        if (!"SYSTEM".equals(message.getType())) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message);
            } catch (Exception e) {
                log.warn("弹幕投递 MQ 失败，降级同步落库: {}", e.getMessage());
                persist(message);
            }
        }
    }

    public void persist(DanmakuMessage message) {
        LiveDanmaku danmaku = new LiveDanmaku();
        danmaku.setRoomId(message.getRoomId());
        danmaku.setUserId(message.getUserId());
        danmaku.setNickname(message.getNickname());
        danmaku.setContent(message.getContent());
        danmaku.setColor(message.getColor());
        danmaku.setVip(Boolean.TRUE.equals(message.getVip()));
        danmaku.setType(message.getType());
        danmakuMapper.insert(danmaku);
    }

    public PageResult<LiveDanmaku> history(Long roomId, PageQuery query) {
        Page<LiveDanmaku> page = danmakuMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveDanmaku>()
                        .eq(LiveDanmaku::getRoomId, roomId)
                        .orderByDesc(LiveDanmaku::getCreateTime));
        return PageResult.of(page);
    }

    public PageResult<LiveDanmaku> adminPage(PageQuery query, Long roomId) {
        Page<LiveDanmaku> page = danmakuMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<LiveDanmaku>()
                        .eq(roomId != null, LiveDanmaku::getRoomId, roomId)
                        .orderByDesc(LiveDanmaku::getCreateTime));
        return PageResult.of(page);
    }

    public void adminDelete(Long id) {
        danmakuMapper.deleteById(id);
    }
}
