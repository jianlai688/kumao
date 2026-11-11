package com.kumao.vip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenVipRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;
    /** 支付方式：ALIPAY / WECHAT / MOCK（未配置渠道时自动降级 MOCK） */
    private String payType = "MOCK";
}
