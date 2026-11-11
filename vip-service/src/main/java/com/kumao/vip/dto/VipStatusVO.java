package com.kumao.vip.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class VipStatusVO implements Serializable {

    private Long userId;
    private boolean vip;
    private String level;
    private LocalDateTime expireTime;
    private String danmakuColor;
    private String entranceEffect;
    private Integer remainDays;
    /** 今日是否已领取看广告赠送会员 */
    private boolean adRewardClaimedToday;
}
