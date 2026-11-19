package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 直播礼物结算单：一场直播结束后按平台抽成比例结算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_settlement")
public class LiveSettlement extends BaseEntity {

    private Long roomId;
    private Long anchorId;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    /** 本场礼物总额（酷币） */
    private Integer giftTotal;
    /** 平台抽成比例（百分比，默认 50） */
    private Integer platformRate;
    /** 平台抽成金额 */
    private Integer platformAmount;
    /** 主播应得金额 */
    private Integer anchorAmount;
    /** SETTLED 已结算 */
    private String status;
}
