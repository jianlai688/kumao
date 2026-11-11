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
    private String liveCategory;
    private String intro;
    private String coverUrl;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String rejectReason;
    private Long auditBy;
    private String auditTime;
}
