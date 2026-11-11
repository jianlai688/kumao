package com.kumao.comment.mq;

import com.kumao.comment.dto.PlayRecordMessage;
import com.kumao.comment.service.PlayRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 播放记录消费者：异步落库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayRecordConsumer {

    private final PlayRecordService playRecordService;

    @RabbitListener(queues = "${kumao.mq.play-record-queue:kumao.play.record}")
    public void onMessage(PlayRecordMessage message) {
        try {
            playRecordService.save(message);
        } catch (Exception e) {
            log.error("播放记录落库失败: {}", message, e);
        }
    }
}
