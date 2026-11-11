package com.kumao.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 内容审核结果。PASS 表示放行，REVIEW / REJECT 为风险建议。
 */
@Data
public class ModerationResultDTO implements Serializable {

    /** PASS / REVIEW / REJECT */
    private String decision;
    private int riskScore;
    private String reasons;
    private List<String> signals = new ArrayList<>();
    private String source;
    private String provider;
    private String model;
}
