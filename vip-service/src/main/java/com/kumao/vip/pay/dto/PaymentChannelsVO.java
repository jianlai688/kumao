package com.kumao.vip.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentChannelsVO {

    private boolean alipayEnabled;
    private boolean wechatEnabled;
    private boolean mockEnabled;
}
