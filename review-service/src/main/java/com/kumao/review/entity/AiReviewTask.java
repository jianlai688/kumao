package com.kumao.review.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kumao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_task")
public class AiReviewTask extends BaseEntity {

    private String category;
    private String itemId;
    private String itemTitle;
    private String itemContent;
    /** PASS / REVIEW / REJECT */
    private String decision;
    private Integer riskScore;
    private String reasons;
    /** JSON 数组字符串 */
    private String signals;
    /** AI_MODEL / LOCAL_RULE */
    private String source;
    private String provider;
    private String model;
    /** PENDING_ADMIN / AUTO_APPROVED / APPROVAL_FAILED / RESOLVED */
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String approvalError;
    private Integer attempts;
}
