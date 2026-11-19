package com.kumao.music.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单曲购买记录（演示环境使用 MOCK 支付，下单即视为支付成功）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("song_purchase")
public class SongPurchase extends BaseEntity {

    private Long userId;
    private Long songId;
    private String orderNo;
    /** 成交金额（分） */
    private Integer amount;
    /** PAID / REFUNDED */
    private String status;
}
