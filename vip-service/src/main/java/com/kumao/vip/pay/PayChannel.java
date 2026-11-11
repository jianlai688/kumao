package com.kumao.vip.pay;

import com.kumao.common.exception.BizException;

/**
 * 支付渠道
 */
public enum PayChannel {

    ALIPAY,
    WECHAT,
    MOCK;

    public static PayChannel from(String value) {
        if (value == null || value.isBlank()) {
            return MOCK;
        }
        try {
            return PayChannel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("不支持的支付方式：" + value);
        }
    }
}
