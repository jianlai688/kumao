package com.kumao.ad.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 广告制作人入驻资料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("advertiser")
public class Advertiser extends BaseEntity {

    private Long userId;
    /** 艺名 / 主体名称 */
    private String companyName;
    private String realName;
    private String idCard;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String adType;
    private String intro;
    private String avatar;
    private String representative;
    /** PENDING / APPROVED / REJECTED / REVOKED */
    private String status;
    private String rejectReason;
    private Long auditBy;
    private String auditTime;
    private Integer deleted;
}
