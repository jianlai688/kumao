package com.kumao.vip.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vip_order")
public class VipOrder extends BaseEntity {

    private String orderNo;
    private Long userId;
    private Long packageId;
    private String packageName;
    private Integer days;
    private BigDecimal amount;
    /** UNPAID / PAID / CLOSED */
    private String status;
    /** ALIPAY / WECHAT / MOCK / AD_REWARD */
    private String payType;
    /** 第三方交易号 */
    private String transactionId;
    private LocalDateTime payTime;
    /** 支付超时时间 */
    private LocalDateTime payExpireTime;
    /** 回调到达时间 */
    private LocalDateTime notifyTime;
    /** 会员到期时间（支付成功后写入） */
    private LocalDateTime expireTime;
    private String remark;
    private Integer deleted;
}
