package com.kumao.live.mq;

import com.kumao.live.dto.DanmakuMessage;
import com.kumao.live.service.DanmakuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 弹幕异步持久化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DanmakuConsumer {

    private final DanmakuService danmakuService;

    @RabbitListener(queues = "${kumao.mq.danmaku-queue:kumao.live.danmaku}")
    public void onMessage(DanmakuMessage message) {
        try {
            danmakuService.persist(message);
        } catch (Exception e) {
            log.error("弹幕落库失败: {}", message, e);
        }
    }
}
