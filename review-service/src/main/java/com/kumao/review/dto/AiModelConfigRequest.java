package com.kumao.review.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型配置保存/测试请求。
 *
 * <p>apiKey 留空表示保留已保存的密钥；clearApiKey=true 时显式清除。</p>
 */
@Data
public class AiModelConfigRequest {

    private String provider;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private boolean clearApiKey;
    private BigDecimal temperature;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Boolean enabled;
    private String remark;
}
