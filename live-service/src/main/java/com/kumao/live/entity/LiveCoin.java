package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户酷猫币账户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_coin")
public class LiveCoin extends BaseEntity {

    private Long userId;
    private Integer balance;
}
