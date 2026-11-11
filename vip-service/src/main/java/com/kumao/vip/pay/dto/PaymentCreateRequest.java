package com.kumao.vip.pay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    /** ALIPAY / WECHAT / MOCK */
    private String payType = "ALIPAY";
}
