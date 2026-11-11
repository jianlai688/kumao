package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 广告投放计划支付信息，供 vip-service 支付编排使用。
 */
@Data
public class AdPlanPaymentDTO implements Serializable {

    private Long planId;
    private Long userId;
    private BigDecimal amount;
    private String subject;
    /** UNPAID / PAID */
    private String paymentStatus;
    /** PENDING / RUNNING / PAUSED / FINISHED / REJECTED */
    private String status;
}
