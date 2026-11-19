package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_danmaku")
public class LiveDanmaku extends BaseEntity {

    private Long roomId;
    private Long userId;
    private String nickname;
    private String content;
    private String color;
    private Boolean vip;
    /** TEXT / GIFT / SYSTEM / ENTRANCE */
    private String type;
    /** 0 已通过 1 已驳回/屏蔽 2 待管理员复核 */
    private Integer status;
    /** PASS / REVIEW / REJECT */
    private String aiDecision;
    private Integer aiRiskScore;
    /** AI_MODEL / LOCAL_RULE / UNAVAILABLE */
    private String aiSource;
    private String aiReasons;
}
