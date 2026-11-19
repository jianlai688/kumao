package com.kumao.musician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 音乐人入驻资料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("musician")
public class Musician extends BaseEntity {

    private Long userId;
    /** 艺名 */
    private String stageName;
    private String realName;
    private String idCard;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String style;
    private String intro;
    private String avatar;
    private String representative;
    /** PENDING / APPROVED / REJECTED / REVOKED */
    private String status;
    private String rejectReason;
    private Long auditBy;
    private String auditTime;
    private Long fansCount;
    private Long totalPlayCount;
    private Long totalLikeCount;
    private Integer deleted;
}
