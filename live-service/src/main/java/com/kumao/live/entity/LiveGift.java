package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_gift")
public class LiveGift extends BaseEntity {

    private String name;
    private String icon;
    private Integer price;
    private String effect;
    private Integer status;
    private Integer deleted;
}
