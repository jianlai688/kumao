package com.kumao.review.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kumao.ai.deepseek")
public class DeepSeekProperties {

    private boolean enabled = false;
    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 20;
    private int maxRetries = 2;
    private double temperature = 0.1;
}
