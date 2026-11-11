package com.kumao.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReviewLogRequest {

    @NotBlank(message = "审核类型不能为空")
    private String category;

    private String itemId;
    private String itemTitle;

    @NotBlank(message = "审核结论不能为空")
    @Pattern(regexp = "APPROVED|REJECTED", message = "审核结论只能是 APPROVED 或 REJECTED")
    private String decision;

    private String adminReason;
    private String aiDecision;
    private Integer aiRiskScore;
    private String aiSource;
    private String aiReasons;
}
