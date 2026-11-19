package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_room")
public class LiveRoom extends BaseEntity {

    private String title;
    private String intro;
    private String coverUrl;
    private Long anchorId;
    private String anchorName;
    private String anchorAvatar;
    /** 弹唱 / 电台 / 翻唱 */
    private String category;
    /** INDOOR 室内 / OUTDOOR 户外 */
    private String scene;
    /** 户外直播地点 */
    private String location;
    /** NOT_STARTED / LIVING / ENDED / BANNED */
    private String status;
    /** 本场直播开始时间，用于真实时长和本场数据统计 */
    private LocalDateTime liveStartedAt;
    private String pushUrl;
    private String pullUrl;
    private String replayUrl;
    /** 开播前设置的直播歌单（歌曲 ID，逗号分隔，按顺序播放） */
    private String playlistIds;
    private Long onlineCount;
    private Long peakOnline;
    private Long likeCount;
    private Long giftIncome;
    private Integer banned;
    private String banReason;
    private Integer deleted;
}
