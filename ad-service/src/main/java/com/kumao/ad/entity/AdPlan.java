package com.kumao.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_plan")
public class AdPlan extends BaseEntity {

    private Long advertiserId;
    private Long userId;
    private Long materialId;
    private String materialName;
    /** SPLASH 开屏 / HOME_BANNER 首页banner / LIVE 直播页 / PLAYER 播放页 / VIP_REWARD VIP 激励视频 */
    private String slot;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 目标人群：ALL / VIP / NEW_USER / MUSICIAN 等 */
    private String targetAudience;
    private Integer days;
    private BigDecimal dailyPrice;
    private BigDecimal amount;
    /** UNPAID / PAID */
    private String paymentStatus;
    private String paymentOrderNo;
    private LocalDateTime paidTime;
    /** PENDING / RUNNING / PAUSED / FINISHED / REJECTED */
    private String status;
    private String rejectReason;
    private Long exposureCount;
    private Long clickCount;
    private Integer deleted;
}
