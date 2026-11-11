package com.kumao.review.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AiModelConfigView {

    /** REVIEW / TEXT / VISION */
    private String type;
    private String label;
    private String description;
    private String provider;
    private String providerLabel;
    private String modelName;
    private String baseUrl;
    /** 仅返回掩码，如 sk-****cdef */
    private String apiKeyMasked;
    private boolean apiKeyConfigured;
    private BigDecimal temperature;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private boolean enabled;
    /** enabled 且已配置密钥，当前会真正调用外部模型 */
    private boolean active;
    /** 环境变量兜底是否已配置（仅审核模型） */
    private boolean environmentConfigured;
    private String remark;
    private LocalDateTime updateTime;
    private String updateByName;
}
