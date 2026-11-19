package com.kumao.live.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 弹幕发送结果：通过时已广播并落库，待复核时仅落库给管理员。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DanmakuSendResult {

    private Integer status;
    private boolean published;
    private String message;
    private String aiDecision;
    private Integer aiRiskScore;
}
