package com.kumao.vip.pay.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentCreateVO {

    private String orderNo;
    /** 实际使用的支付渠道：ALIPAY / WECHAT / MOCK */
    private String payType;
    private BigDecimal amount;
    private Integer days;
    private String packageName;
    /** true 表示渠道未配置，走本地模拟支付 */
    private boolean mock;
    /** 支付宝电脑网站支付页面地址（相对网关路径） */
    private String payUrl;
    /** 微信 Native 支付 code_url */
    private String codeUrl;
    /** 微信支付二维码（data:image/png;base64） */
    private String qrCode;
    private LocalDateTime payExpireTime;
}
