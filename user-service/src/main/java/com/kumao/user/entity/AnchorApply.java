package com.kumao.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主播入驻申请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("anchor_apply")
public class AnchorApply extends BaseEntity {

    private Long userId;
    private String realName;
    private String idCard;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String liveCategory;
    private String intro;
    private String coverUrl;
    /** 主播艺名 */
    private String anchorName;
    private String avatar;
    /** 收款方式：ALIPAY / WECHAT */
    private String payType;
    /** 收款账号（支付宝账号 / 微信号） */
    private String payAccount;
    /** 收款人姓名 */
    private String payName;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String rejectReason;
    private Long auditBy;
    private String auditTime;
}

