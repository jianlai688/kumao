package com.kumao.comment.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 播放记录消息：前端播放歌曲时投递到 RabbitMQ
 */
@Data
public class PlayRecordMessage implements Serializable {

    private Long userId;
    private Long songId;
    private String device;
    private Integer playedSeconds;
    private Long timestamp;
}
