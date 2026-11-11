package com.kumao.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_log")
public class ReviewLog extends BaseEntity {

    private String category;
    private String itemId;
    private String itemTitle;
    /** APPROVED / REJECTED */
    private String decision;
    private String adminReason;
    private String aiDecision;
    private Integer aiRiskScore;
    private String aiSource;
    private String aiReasons;
    private Long adminId;
    private String adminName;
}
