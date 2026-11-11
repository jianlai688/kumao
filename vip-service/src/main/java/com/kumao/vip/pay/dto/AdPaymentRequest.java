package com.kumao.vip.pay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdPaymentRequest {

    @NotNull(message = "投放计划不能为空")
    private Long planId;

    /** ALIPAY / WECHAT / MOCK */
    private String payType = "ALIPAY";
}
