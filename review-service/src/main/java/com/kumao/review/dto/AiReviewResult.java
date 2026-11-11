package com.kumao.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResult {

    /** PASS / REVIEW / REJECT，仅为建议 */
    private String decision;
    private int riskScore;
    private String reasons;
    private List<String> signals;
    /** AI_MODEL / LOCAL_RULE */
    private String source;
    /** 模型服务商标识，如 deepseek */
    private String provider;
    private String model;
    private String fallbackReason;
    private long latencyMs;

    public static AiReviewResult local(String decision, int riskScore, String reasons,
                                       List<String> signals, String fallbackReason, long latencyMs) {
        return AiReviewResult.builder()
                .decision(decision)
                .riskScore(riskScore)
                .reasons(reasons)
                .signals(signals == null ? new ArrayList<>() : signals)
                .source("LOCAL_RULE")
                .model("local-rule-v1")
                .fallbackReason(fallbackReason)
                .latencyMs(latencyMs)
                .build();
    }
}
