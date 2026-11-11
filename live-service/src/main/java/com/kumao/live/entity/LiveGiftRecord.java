package com.kumao.live.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("live_gift_record")
public class LiveGiftRecord extends BaseEntity {

    private Long roomId;
    private Long giftId;
    private String giftName;
    private String giftIcon;
    private Integer price;
    private Integer count;
    private Integer totalPrice;
    private Long senderId;
    private String senderName;
    private Long anchorId;
}
