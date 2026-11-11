package com.kumao.vip.pay.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentStatusVO {

    private String orderNo;
    /** UNPAID / PAID / CLOSED */
    private String status;
    private String payType;
    private BigDecimal amount;
    private String packageName;
    private Integer days;
    private String transactionId;
    private boolean mock;
    private LocalDateTime payTime;
    private LocalDateTime payExpireTime;
    /** 会员到期时间（支付成功后才有值） */
    private LocalDateTime expireTime;
}
