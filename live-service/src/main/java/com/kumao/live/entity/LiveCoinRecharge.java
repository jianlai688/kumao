package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 酷猫币充值入账记录，用于支付回调幂等。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_coin_recharge")
public class LiveCoinRecharge extends BaseEntity {

    private Long userId;
    private String orderNo;
    private Integer amount;
    private Integer balanceAfter;
}
