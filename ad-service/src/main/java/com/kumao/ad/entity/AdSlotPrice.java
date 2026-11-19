package com.kumao.ad.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 广告位日单价配置。
 *
 * <p>历史计划在创建时已经固化 dailyPrice 与 amount，本表只影响新创建的投流计划。</p>
 */
@Data
@TableName("ad_slot_price")
public class AdSlotPrice {

    /** SPLASH / HOME_BANNER / LIVE / PLAYER / VIP_REWARD */
    @TableId(type = IdType.INPUT)
    private String slot;

    private String name;

    private BigDecimal dailyPrice;

    private LocalDateTime updateTime;
}
